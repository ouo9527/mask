package com.ouo.mask.config;

import com.ouo.mask.core.DefaultDesensitizationHandler;
import com.ouo.mask.core.DesensitizationHandler;
import com.ouo.mask.core.DesensitizationRuleLoader;
import com.ouo.mask.spring.SpringDesensitizationRuleLoader;
import com.ouo.mask.spring.SpringUtil;
import com.ouo.mask.web.WebDesensitizationResponseBodyAdvice;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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
@ConditionalOnProperty(prefix = "ouo.desensitization", name = "enabled", havingValue = "true")
@Import({SpringUtil.class, WebDesensitizationResponseBodyAdvice.class})
//@RefreshScope //springcloud刷新@Value注解属性
@Slf4j
public class DesensitizationAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(value = {DesensitizationRuleLoader.class, SpringDesensitizationRuleLoader.class})
    public SpringDesensitizationRuleLoader propertySourcesPlaceholderConfigurer(Environment environment) {
        SpringDesensitizationRuleLoader desensitizationRuleLoader = new SpringDesensitizationRuleLoader();
        //desensitizationRuleLoader.setEnvironment(environment);
        //todo：Binder会将配置转化成LinkedHashMap
        final Properties properties = Binder.get(environment)
                .bind(SpringDesensitizationRuleLoader.RULES, Map.class)
                .map(p -> {
                    final Properties prop = new Properties();
                    p.forEach((k, v) -> prop.put(SpringDesensitizationRuleLoader.RULES + "[" + k + "]", v));
                    return prop;
                }).orElse(null);
        properties.put(SpringDesensitizationRuleLoader.SCAN,
                Binder.get(environment).bind(SpringDesensitizationRuleLoader.SCAN, String.class).orElse(""));
        desensitizationRuleLoader.setProperties(properties);

        return desensitizationRuleLoader;
    }

    @Bean
    @ConditionalOnMissingBean(value = {DesensitizationHandler.class})
    public DesensitizationHandler desensitizationHandler(DesensitizationRuleLoader loader) {
        return new DefaultDesensitizationHandler(loader);
    }
}
