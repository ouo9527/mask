package com.ouo.mask.config;

import com.ouo.mask.handler.DefaultDesensitizationHandler;
import com.ouo.mask.handler.DesensitizationHandler;
import com.ouo.mask.support.web.DesensitizationResponseBodyAdvice;
import com.ouo.mask.util.SpringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/***********************************************************
 * TODO:     数据脱敏配置
 * Author:   刘春
 * Date:     2023/1/17
 ***********************************************************/
@Configuration
@ConditionalOnProperty(prefix = "ouo.desensitization", name = "enabled", havingValue = "true")
@Import({SpringUtil.class, DesensitizationResponseBodyAdvice.class})
@EnableConfigurationProperties({DesensitizationProperties.class})
//@RefreshScope //springcloud刷新@Value注解属性
@Slf4j
public class DesensitizationAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(value = {DesensitizationHandler.class})
    public DesensitizationHandler desensitizationHandler(DesensitizationProperties properties) {
        DesensitizationHandler handler = new DefaultDesensitizationHandler();
        ((DefaultDesensitizationHandler) handler).setProperties(properties);
        return handler;
    }
}
