package com.ouo.mask.config;

import cn.hutool.core.bean.BeanPath;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.lang.Dict;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.text.StrSplitter;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.ouo.mask.enums.ModeEnum;
import com.ouo.mask.rule.*;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.*;

/**
 * @Value失效场景： 1、PropertySourcesPlaceholderConfigurer类及其子类使用，由于配置还未加载并解析
 * 2、使用static或者final修饰
 * 3、用该注解的类没有被spring管理
 * 4、在Bean初始化时构造方法中引用被@Value修饰的变量
 * @ConfigurationProperties失效场景： 1、所修饰的类，其类中属性不存在非静态set方法或者不存在无参构造方法
 * 2、若使用@Bean+@ConfigurationProperties自定义bean或（@Component 或者 @Configuration等）bean注解+@ConfigurationProperties，
 * 则需要使用EnableAutoConfiguration修饰在入口类，如配置类、启动类上等
 * 3、若只使用@ConfigurationProperties，则需要使用@EnableConfigurationProperties修饰在入口类，如启配置类、动类上等
 * <p>
 * 注意：1）在spring-boot 2.2 之前版本,必须使用 @Component 或者 @Configuration 声明成Spring Bean；
 * 而2.2.0 新增一个 @ConfigurationPropertiesScan 的注解，默认是开启的扫描 main 启动类所在的包路径的所有ConfigurationProperties
 * 或使用@EnableConfigurationProperties注解加载相应配置类，所以可以不用再加 @Component 或者 @Configuration ；
 * Spring boot 2.2.1 默认关闭此功能，需要显式指定此注解，实际在使用过程中会发现 @Profile 和这个注解的兼容问题,
 * @ConfigurationPropertiesScan not compatible with @Profile @ConfigurationProperties，所以 Spring Boot 2.2.1 默认关闭了这个功能；
 * <p>
 * 2）@ConfigurationProperties虽可以处理复杂的数据类型，且会将复杂数据类型（无论数组或列表）转成LinkedHashMap，然后再做实际映射处理，但是对于properties和yml文件
 * 中复杂类型做映射处理时，前者无法映射成数组，而后者可以
 * 3）在spring-boot2.0以下，@ConfigurationProperties映射对象原理由PropertiesConfigurationFactory；2.0后由ConfigurationPropertiesBindingPostProcessor
 */
@Slf4j
@Getter
@ConfigurationProperties("ouo.desensitization")
public class DesensitizationProperties {
    public static final String PREFIX = "ouo.desensitization";
    public static final String RULES = PREFIX + ".rules";
    public static final String SCOPES = PREFIX + ".scopes";

    //todo: 脱敏范围即配置包路径，多个值时以英文逗号隔开；为了减少不必要的数据脱敏，否则会影响系统性能，因此推荐设置。若为空则所有路径生效
    @Setter
    private String[] scopes;

    //todo: 脱敏规则，key：字段，value：规则集
    private Map<String, List<DesensitizationRule>> rules;

    public void setRules(Properties properties) {
        //todo：具体层级的Map
        Dict dict = Dict.create();
        CollUtil.forEach(properties, (k, v, i) -> {
            BeanPath.create(StrUtil.toStringOrNull(k)).set(dict, v);
        });
        this.setScopes(StrSplitter.splitToArray(dict.getByPath(DesensitizationProperties.SCOPES, String.class),
                ',', -1, true, true));
        this.setRules(dict.getByPath(DesensitizationProperties.RULES, Map.class));
    }

    public void setRules(Map rules) {
        this.rules = new HashMap<>();
        CollUtil.forEach(rules, (k, v, i) -> {
            final String field = StrUtil.toStringOrNull(k);
            if (v instanceof List) {
                this.rules.put(field, convert(field, (List) v));
            } else if (v instanceof Map) {
                this.rules.put(field, convert(field, CollUtil.newArrayList(((Map) v).values())));
            } else log.debug("{}.{}: This does not comply with the desensitization rules.",
                    DesensitizationProperties.RULES, field);
        });
    }

    private List<DesensitizationRule> convert(String field, List<?> rules) {
        final List<DesensitizationRule> drs = new ArrayList<>();
        CollUtil.forEach(rules, ((r, i) -> {
            String mode = r instanceof Map ? MapUtil.getStr((Map) r, "mode", "") : "";
            DesensitizationRule dr = null;

            if (StrUtil.equalsIgnoreCase(mode, ModeEnum.EMPTY.name())) {//todo：置空
                dr = JSON.to(EmptyDesensitizationRule.class, r);
            } else if (StrUtil.equalsIgnoreCase(mode, ModeEnum.HASH.name())) {//todo：哈希
                dr = JSON.to(HashDesensitizationRule.class, r);
            } else if (StrUtil.equalsIgnoreCase(mode, ModeEnum.REGEX.name())) {//todo：正则
                dr = JSON.to(RegexDesensitizationRule.class, r);
            } else if (StrUtil.equalsIgnoreCase(mode, ModeEnum.REPL.name())) {//todo：替换
                Object posns = ((Map) r).get("posns");
                //todo：对于springboot yml转properties时，若多层数组嵌套时，会被转成LinkedHashMap
                if (posns instanceof Map) ((Map) r).put("posns", CollUtil.newArrayList(((Map) posns).values()));
                dr = JSON.to(ReplDesensitizationRule.class, r);
            } else if (StrUtil.equalsIgnoreCase(mode, ModeEnum.MASK.name())) {//todo：掩盖
                Object obj = ((Map) r).get("posns");
                List<?> posns = Collections.EMPTY_LIST;
                //todo：对于springboot yml转properties时，若多层数组嵌套时，会被转成LinkedHashMap
                if (obj instanceof Map)
                    ((Map) r).put("posns", posns = CollUtil.newArrayList(((Map) obj).values()));
                else if (obj instanceof List) posns = (List<?>) obj;
                ReplDesensitizationRule rdr = JSON.to(ReplDesensitizationRule.class, r);
                ReplDesensitizationRule.Posn surplus = new ReplDesensitizationRule.Posn();
                surplus.setFixed(true);
                surplus.setRv(Convert.convert(Boolean.class, ObjUtil.defaultIfNull(((Map) r).get("surplusHide"),
                        ((Map) r).get("surplus-hide")), false) ? "*" : "");
                rdr.setSurplus(surplus);
                List<ReplDesensitizationRule.Posn> ps = new ArrayList<>(posns.size());
                for (int j = 0; j < posns.size(); j++) {
                    Map<String, Object> posn = JSON.to(Map.class, posns.get(j));
                    if (CollUtil.isEmpty(posn) || !NumberUtil.isInteger(StrUtil.toStringOrNull(posn.get("i"))))
                        continue;
                    ReplDesensitizationRule.Posn p = new ReplDesensitizationRule.Posn();
                    p.setI(Convert.convert(Integer.class, posn.get("i")));
                    p.setFixed(true);
                    p.setRv(Convert.convert(Boolean.class, posn.get("hide"), false) ? "*" : "");
                    ps.add(p);
                }
                rdr.setPosns(ps);
                dr = rdr;
            } else {
                log.debug("{}.{}[{}]: mode={} is not within the range of [empty,hash,regex,replace,mask]",
                        DesensitizationProperties.RULES, field, i, mode);
            }

            if (null != dr) {
                dr.setField(field);
                drs.add(dr);
            }
        }));

        return drs;
    }
}
