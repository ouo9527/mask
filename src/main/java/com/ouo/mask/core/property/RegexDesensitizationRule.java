package com.ouo.mask.core.property;

import lombok.Getter;
import lombok.Setter;

/***********************************************************
 * TODO:     正则脱敏规则
 * Author:   刘春
 * Date:     2023/1/29
 ***********************************************************/
@Setter
@Getter
public class RegexDesensitizationRule extends DesensitizationRule {
    // TODO：正则表达式
    private String pattern;
    // TODO：替换值
    private String rv;
}
