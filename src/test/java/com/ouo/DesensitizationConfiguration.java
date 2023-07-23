package com.ouo;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/***********************************************************
 * TODO:     数据脱敏配置
 * Author:   刘春
 * Date:     2023/1/17
 ***********************************************************/
@Configuration
@ConfigurationProperties(prefix = "ouo.desensitization")
@Getter
@Setter
public class DesensitizationConfiguration {

    private static String test;
    //todo: 映射复杂数据类型
    //private List<Map> rules;
    //todo：映射properties配置文件中数组类型
    //private Map<String, DesensitizationRuleProperties> rules;
    //todo：映射yml配置文件中数组类型
    private List<DesensitizationRuleProperties> rules;

    public String getTest() {
        return test;
    }

    public void setTest(String test) {
        DesensitizationConfiguration.test = test;
    }
}
