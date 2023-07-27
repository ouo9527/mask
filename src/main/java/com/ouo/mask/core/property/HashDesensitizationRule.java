package com.ouo.mask.core.property;

import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.annotation.JSONField;
import com.ouo.mask.core.annotation.Hash;
import lombok.Getter;
import lombok.Setter;

/***********************************************************
 * TODO:     Hash脱敏规则
 * Author:   刘春
 * Date:     2023/1/29
 ***********************************************************/
@Getter
@Setter
public class HashDesensitizationRule extends DesensitizationRule {
    //TODO：Hash算法
    @JSONField(serializeFeatures = JSONWriter.Feature.WriteEnumsUsingName)
    private Hash.AlgorithmEnum algorithm;
    //TODO：盐
    private String salt;
}
