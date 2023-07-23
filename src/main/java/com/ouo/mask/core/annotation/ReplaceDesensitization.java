package com.ouo.mask.core.annotation;

import java.lang.annotation.*;

/***********************************************************
 * TODO:     替换脱敏注解
 *  目前只对CharSequence类型字段有效，若其值为JSON字符串时，该注解失效
 * Author:   刘春
 * Date:     2023/1/29
 ***********************************************************/
@Documented
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface ReplaceDesensitization {
    // TODO：场景，默认：WEB和LOG
    SceneEnum scene() default SceneEnum.ALL;

    // TODO：位置，按照数组顺序从左往右进行替换
    Posn[] posns();

    // TODO：剩余位置替换
    Posn surplus() default @Posn;

    //TODO：位置
    @Target({})
    @Retention(RetentionPolicy.RUNTIME)
    @interface Posn {
        // TODO：随机还是固定值替换，默认：固定值
        boolean fixed() default true;

        // TODO：第几位（用于posns属性时必须大于0，否则无效；而用于surplus属性，无效）
        int i() default 0;

        // TODO：替换值，若随机，则该属性无效；若固定值，其为空时，保持原样
        String rv() default "";
    }
}
