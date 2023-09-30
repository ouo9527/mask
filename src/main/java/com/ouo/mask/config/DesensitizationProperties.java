package com.ouo.mask.config;

import cn.hutool.core.bean.BeanPath;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Dict;
import cn.hutool.core.map.MapUtil;
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
@ConfigurationProperties(prefix = "ouo.desensitization", ignoreInvalidFields = true)
public class DesensitizationProperties {
    public static final String PREFIX = "ouo.desensitization";
    public static final String RULES = PREFIX + ".rules";
    public static final String STRATEGY = PREFIX + ".strategy";

    //todo: 脱敏策略
    @Setter
    @Getter
    private DesensitizationStrategy strategy;

    //todo: 脱敏规则，key：字段，value：规则集
    private Map<String, List<DesensitizationRule>> rules;

    public Map<String, List<DesensitizationRule>> getRules() {
        return MapUtil.defaultIfEmpty(rules, Collections.EMPTY_MAP);
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

    public void setRules(Properties properties) {
        //todo：具体层级的Map
        Dict dict = Dict.create();
        CollUtil.forEach(properties, (k, v, i) -> {
            BeanPath.create(StrUtil.toStringOrNull(k)).set(dict, v);
        });
        this.setStrategy(dict.getByPath(DesensitizationProperties.STRATEGY, DesensitizationStrategy.class));
        this.setRules(dict.getByPath(DesensitizationProperties.RULES, Map.class));
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
                dr = JSON.to(MaskDesensitizationRule.class, r);
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
