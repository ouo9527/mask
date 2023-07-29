package com.ouo.mask.annotation;

import com.ouo.mask.enums.SceneEnum;

import java.lang.annotation.*;

/***********************************************************
 * TODO:     置空脱敏注解
 *  目前只对CharSequence类型字段有效，若其值为JSON字符串时，该注解失效
 * Author:   刘春
 * Date:     2023/1/29
 ***********************************************************/
@Documented
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface Empty {
    // TODO：场景，默认：WEB和LOG
    SceneEnum scene() default SceneEnum.ALL;
}
