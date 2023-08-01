package com.ouo.mask.handler;

import com.ouo.mask.enums.SceneEnum;

/***********************************************************
 * TODO:     脱敏处理器
 * Author:   刘春
 * Date:     2023/1/29
 ***********************************************************/
public interface DesensitizationHandler {

    /**
     * todo： 在上下文内按照场景进行数据脱敏
     *
     * @param scene   场景
     * @param data    待脱敏的数据
     * @return 返回已脱敏数据
     */
    <T> T desensitized(SceneEnum scene, T data);

    /**
     * todo: 根据脱敏策略验证是否脱敏
     *
     * @param context 待脱敏对象所被使用的上下文即在那个类中使用
     * @param roleId  角色ID
     * @return
     */
    boolean isValid(String context, String[] roleId);
}
