package com.ouo.mask.core;

import com.ouo.mask.core.annotation.SceneEnum;
import com.ouo.mask.core.property.DesensitizationRule;

import java.util.Collection;

/***********************************************************
 * TODO:     脱敏处理器
 * Author:   刘春
 * Date:     2023/1/29
 ***********************************************************/
public abstract class DesensitizationHandler {

    // TODO：场景
    protected SceneEnum scene;
    // TODO：脱敏规则
    protected Collection<DesensitizationRule> rules;

    protected DesensitizationHandler(SceneEnum scene, Collection<DesensitizationRule> rules) {
        this.scene = null == scene ? SceneEnum.ALL : scene;
        this.rules = rules;
    }

    /**
     * todo： 根据所加载的脱敏规则进行脱敏数据
     *
     * @param obj 待脱敏对象
     * @return 返回已脱敏对象
     */
    public abstract Object desensitized(Object obj);
}
