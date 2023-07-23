package com.ouo.mask.config;

import com.ouo.mask.core.DesensitizationRuleLoader;
import com.ouo.mask.spring.SpringDesensitizationRuleLoader;
import com.ouo.mask.spring.SpringUtil;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;

import java.util.Map;
import java.util.Properties;

/***********************************************************
 * TODO:     数据脱敏配置
 * Author:   刘春
 * Date:     2023/1/17
 ***********************************************************/
@Configuration
@Import({SpringUtil.class})
//@RefreshScope //springcloud刷新@Value注解属性
public class DesensitizationAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(value = {DesensitizationRuleLoader.class, SpringDesensitizationRuleLoader.class})
    public SpringDesensitizationRuleLoader propertySourcesPlaceholderConfigurer(Environment environment) {
        SpringDesensitizationRuleLoader desensitizationRuleLoader = new SpringDesensitizationRuleLoader();
        //desensitizationRuleLoader.setEnvironment(environment);
        //todo：Binder会将配置转化成LinkedHashMap
        final Properties properties = Binder.get(environment)
                .bind(DesensitizationRuleLoader.PREFIX, Map.class)
                .map(p -> {
                    final Properties prop = new Properties();
                    p.forEach((k, v) -> prop.put(DesensitizationRuleLoader.PREFIX + "[" + k + "]", v));
                    return prop;
                }).get();
        desensitizationRuleLoader.setProperties(properties);
        return desensitizationRuleLoader;
    }
}
