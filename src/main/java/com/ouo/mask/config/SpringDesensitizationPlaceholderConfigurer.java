package com.ouo.mask.config;

import com.ouo.mask.handler.DefaultDesensitizationHandler;
import com.ouo.mask.handler.DesensitizationHandler;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.PropertySources;

import java.util.Iterator;
import java.util.Properties;

/***********************************************************
 * TODO:     脱敏规则加载（用于Spring环境，而Springboot环境采用{@link DesensitizationAutoConfiguration}自动加载）
 * Author:   刘春
 * Date:     2023/1/18
 ***********************************************************/
@Slf4j
public class SpringDesensitizationPlaceholderConfigurer extends PropertySourcesPlaceholderConfigurer {
    @Setter
    private DesensitizationHandler handler;

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        super.postProcessBeanFactory(beanFactory);
        this.convert();
    }

    private void convert() {
        PropertySources propertySources = this.getAppliedPropertySources();
        /*Object localProperties = propertySources.stream()
                .filter(ps -> LOCAL_PROPERTIES_PROPERTY_SOURCE_NAME.equals(ps.getName()))
                .findFirst()
                .get().getSource();*/
        //todo: 仅有单层的Map
        Object localProperties = null;
        Iterator<PropertySource<?>> it = propertySources.iterator();
        while (it.hasNext()) {
            PropertySource<?> propertySource = it.next();
            if (null == propertySource || !LOCAL_PROPERTIES_PROPERTY_SOURCE_NAME.equals(propertySource.getName()))
                continue;
            else {
                localProperties = propertySource.getSource();
                break;
            }
        }
        if (handler instanceof DefaultDesensitizationHandler) {
            DesensitizationProperties properties = new DesensitizationProperties();
            if (localProperties instanceof Properties) properties.setRules((Properties) localProperties);
            ((DefaultDesensitizationHandler) handler).setProperties(properties);
        }
    }
}
