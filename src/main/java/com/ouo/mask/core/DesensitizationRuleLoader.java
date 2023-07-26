package com.ouo.mask.core;

import com.ouo.mask.core.property.DesensitizationRule;

import java.util.List;

/***********************************************************
 * TODO:     脱敏规则加载器
 * Author:   刘春
 * Date:     2023/1/29
 ***********************************************************/
public interface DesensitizationRuleLoader {
    /**
     * todo: 规则加载
     *
     * @return
     */
    void load();

    /**
     * todo: 获取脱敏规则
     *
     * @return
     */
    List<DesensitizationRule> getDesensitizationRules();

    /**
     * todo: 获取脱敏范围即配置包路径，多个值时以英文逗号隔开；
     * 为了减少不必要的数据脱敏，否则会影响系统性能，因此推荐设置。默认：空，当为空时，会扫描全部类
     * 注：对于待脱敏对象非基本(包含其包装)类型、数组/集合类型、迭代器和枚举类型外，则满足以下规则：
     * 1）必须提供public set/get 方法
     * 2）可以使用scan属性来扫描所需要待脱敏的对象
     * 目的都是降低递归所造成栈溢出
     *
     * @return
     */
    List<String> getScan();
}
