package com.ouo;

import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.annotation.JSONField;
import com.ouo.mask.core.annotation.Hash;
import com.ouo.mask.core.property.DesensitizationRule;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/***********************************************************
 * TODO:     脱敏规则配置
 * Author:   刘春
 * Date:     2023/1/29
 ***********************************************************/
@Getter
@Setter
public class DesensitizationRuleProperties extends DesensitizationRule {
    /**************************** mode：替换 *******************************/
    //todo：位置，按照数组顺序从左往右进行替换
    private List<PosnProperty> posns;
    //todo：剩余位置替换
    private PosnProperty surplus;
    /**************************** mode：掩盖 *******************************/
    //todo：剩余位置是否隐藏，用于mode为掩盖时
    private boolean surplusHide;
    /**************************** mode：哈希 *******************************/
    //todo：Hash算法
    @JSONField(serializeFeatures = JSONWriter.Feature.WriteEnumsUsingName)
    private Hash.AlgorithmEnum algorithm;
    //todo：盐
    private String salt;
    /**************************** mode：正则 *******************************/
    //todo：正则表达式
    private String pattern;
    //todo：替换值
    private String rv;

    //todo：位置属性
    @Setter
    @Getter
    public static class PosnProperty {
        //todo：随机还是固定值替换，默认：固定值
        private boolean fixed = true;
        //todo：第几位（用于posns属性时必须大于0，否则无效；而用于surplus属性，无效）
        private int i;
        //todo：替换值，若随机，则该属性无效；若固定值，其为空时，保持原样
        private String rv;
        /**************************** mode：掩盖 *******************************/
        //todo：是否隐藏，用于mode为掩盖时
        private boolean hide;
    }
}
