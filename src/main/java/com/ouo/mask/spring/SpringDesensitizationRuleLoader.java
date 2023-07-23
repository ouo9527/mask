package com.ouo.mask.spring;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.ouo.mask.core.DesensitizationRuleLoader;
import com.ouo.mask.core.ModeEnum;
import com.ouo.mask.core.property.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.PropertySources;
import pl.jalokim.propertiestojson.util.PropertiesToJsonConverter;

import java.util.*;

/***********************************************************
 * TODO:     脱敏规则加载
 * Author:   刘春
 * Date:     2023/1/18
 ***********************************************************/
@Slf4j
public class SpringDesensitizationRuleLoader extends PropertySourcesPlaceholderConfigurer implements DesensitizationRuleLoader {
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
    private List<DesensitizationRule> desensitizationRules = new ArrayList<>();

    private static Object getJson(Properties properties) {
        JSONObject mapRules = JSON.parseObject(new PropertiesToJsonConverter().convertToJson(properties, PREFIX));
        if (CollUtil.isEmpty(mapRules)) return null;
        Object obj = mapRules;
        for (String k : StrUtil.split(PREFIX, '.')) {
            if (obj instanceof JSONObject) obj = ((JSONObject) obj).get(k);
            else return null;
        }
        return obj;
    }

    @Override
    public List<DesensitizationRule> load() {
        return this.desensitizationRules;
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        super.postProcessBeanFactory(beanFactory);
        PropertySources propertySources = this.getAppliedPropertySources();
        Object localProperties = propertySources.stream()
                .filter(ps -> LOCAL_PROPERTIES_PROPERTY_SOURCE_NAME.equals(ps.getName()))
                .findFirst()
                .get().getSource();
        if (!(localProperties instanceof Properties)) return;
        try {
            Object rules = getJson((Properties) localProperties);
            if (!(rules instanceof List)) return;
            for (int i = 0; i < ((List) rules).size(); i++) {
                Object rule = ((List) rules).get(i);
                if (!(rule instanceof Map)) {
                    log.debug("{}[{}]: It is not an object", PREFIX, i);
                    continue;
                }
                String mode = "";
                String field = "";
                if (null != rule) {
                    mode = StrUtil.toStringOrNull(((Map) rule).get("mode"));
                    field = StrUtil.toStringOrNull(((Map) rule).get("field"));
                }
                if (StrUtil.isBlank(mode) || StrUtil.isBlank(field)) {
                    log.debug("{}[{}]: field={}, mode={} .There is a blank value", PREFIX, i, field, mode);
                    continue;
                }
                if (StrUtil.equalsIgnoreCase(mode, ModeEnum.EMPTY.name())) {//todo：置空
                    this.desensitizationRules.add(JSON.to(EmptyDesensitizationRule.class, rule));
                } else if (StrUtil.equalsIgnoreCase(mode, ModeEnum.HASH.name())) {//todo：哈希
                    this.desensitizationRules.add(JSON.to(HashDesensitizationRule.class, rule));
                } else if (StrUtil.equalsIgnoreCase(mode, ModeEnum.REGEX.name())) {//todo：正则
                    this.desensitizationRules.add(JSON.to(RegexDesensitizationRule.class, rule));
                } else if (StrUtil.equalsIgnoreCase(mode, ModeEnum.REPLACE.name())) {//todo：替换
                    Object posns = ((Map) rule).get("posns");
                    //todo：对于springboot yml转properties时，若多层数组嵌套时，会被转成LinkedHashMap
                    if (posns instanceof Map) ((Map) rule).put("posns", CollUtil.newArrayList(((Map) posns).values()));
                    this.desensitizationRules.add(JSON.to(ReplaceDesensitizationRule.class, rule));
                } else if (StrUtil.equalsIgnoreCase(mode, ModeEnum.MASK.name())) {//todo：掩盖
                    Object obj = ((Map) rule).get("posns");
                    List<?> posns = Collections.EMPTY_LIST;
                    //todo：对于springboot yml转properties时，若多层数组嵌套时，会被转成LinkedHashMap
                    if (obj instanceof Map)
                        ((Map) rule).put("posns", posns = CollUtil.newArrayList(((Map) obj).values()));
                    else if (obj instanceof List) posns = (List<?>) obj;
                    ReplaceDesensitizationRule r = JSON.to(ReplaceDesensitizationRule.class, rule);
                    ReplaceDesensitizationRule.PosnProperty surplus = new ReplaceDesensitizationRule.PosnProperty();
                    surplus.setFixed(true);
                    surplus.setRv(Convert.convert(Boolean.class, ObjUtil.defaultIfNull(((Map) rule).get("surplusHide"),
                            ((Map) rule).get("surplus-hide"))
                            , false) ? "*" : "");
                    r.setSurplus(surplus);
                    List<ReplaceDesensitizationRule.PosnProperty> ps = new ArrayList<>(posns.size());
                    for (int j = 0; j < posns.size(); j++) {
                        Map<String, Object> posn = JSON.to(Map.class, posns.get(j));
                        if (CollUtil.isEmpty(posn) || !NumberUtil.isInteger(StrUtil.toStringOrNull(posn.get("i"))))
                            continue;
                        ReplaceDesensitizationRule.PosnProperty p = new ReplaceDesensitizationRule.PosnProperty();
                        p.setI(Convert.convert(Integer.class, posn.get("i")));
                        p.setFixed(true);
                        p.setRv(Convert.convert(Boolean.class, posn.get("hide"), false) ? "*" : "");
                        ps.add(p);
                    }
                    r.setPosns(ps);
                    this.desensitizationRules.add(r);
                } else {
                    log.debug("{}[{}]: field={}, mode={} is not within the range of [empty,hash,regex,replace,mask]", PREFIX, i, field, mode);
                    continue;
                }
            }
        } catch (RuntimeException e) {
            log.error("[{}]: configuration parsing failed: ", PREFIX, e);
        }
    }
}
