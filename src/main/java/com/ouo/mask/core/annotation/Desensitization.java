package com.ouo.mask.core.annotation;

import java.lang.annotation.*;

/***********************************************************
 * TODO:     脱敏注解
 *  只针对于WEB场景启用或停用，以防止与第二或三方交易响应时脱敏数据
 *
 * Author:   刘春
 * Date:     2023/1/29
 ***********************************************************/
@Documented
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface Desensitization {
    //todo: 是否启用脱敏，只针对于WEB场景
    boolean enabled() default true;
}
