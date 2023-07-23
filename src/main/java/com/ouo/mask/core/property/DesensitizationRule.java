package com.ouo.mask.core.property;

import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.annotation.JSONField;
import com.ouo.mask.core.ModeEnum;
import com.ouo.mask.core.annotation.SceneEnum;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/***********************************************************
 * TODO:     脱敏规则
 * Author:   刘春
 * Date:     2023/1/29
 ***********************************************************/
@Setter
@Getter
@ToString
public abstract class DesensitizationRule {
    //todo：场景
    @JSONField(serializeFeatures = JSONWriter.Feature.WriteEnumsUsingName)
    protected SceneEnum scene;
    //todo：字段
    protected String field;
    //todo：模式
    @JSONField(serializeFeatures = JSONWriter.Feature.WriteEnumsUsingName)
    protected ModeEnum mode;
}
