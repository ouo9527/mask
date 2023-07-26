package com.ouo.mask.core;

import com.ouo.mask.core.annotation.SceneEnum;

/***********************************************************
 * TODO:     脱敏处理器
 * Author:   刘春
 * Date:     2023/1/29
 ***********************************************************/
public abstract class DesensitizationHandler {
    //todo：脱敏规则加载器
    protected DesensitizationRuleLoader loader;

    protected DesensitizationHandler(DesensitizationRuleLoader loader) {
        this.loader = loader;
    }

    /**
     * todo： 根据所加载的脱敏规则进行脱敏数据
     *
     * @param scene 场景
     * @param obj 待脱敏对象
     * @return 返回已脱敏对象
     */
    public abstract Object desensitized(SceneEnum scene, Object obj);
}
