package com.ouo.mask.core;

import com.ouo.mask.core.property.DesensitizationRule;

import java.util.List;

/***********************************************************
 * TODO:     脱敏规则加载器
 * Author:   刘春
 * Date:     2023/1/29
 ***********************************************************/
public interface DesensitizationRuleLoader {

    String PREFIX = "ouo.desensitization.rules";

    /**
     * todo： 规则加载
     *
     * @return
     */
    List<DesensitizationRule> load();
}
