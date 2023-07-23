package com.ouo.mask.core.annotation;

import java.lang.annotation.*;

/***********************************************************
 * TODO:     掩盖脱敏(属于固定值替换一种)注解
 *  目前只对CharSequence类型字段有效，若其值为JSON字符串时，该注解失效
 * Author:   刘春
 * Date:     2023/1/29
 ***********************************************************/
@Documented
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface MaskDesensitization {
    // TODO：场景，默认：WEB和LOG
    SceneEnum scene() default SceneEnum.ALL;

    // TODO：位置，按照数组顺序从左往右
    Posn[] posns();

    // TODO：剩余是否隐藏，若为true等价于@ReplaceDesensitization(surplus=@ReplaceDesensitization.Posn(rv="*"))
    boolean surplusHide() default false;

    //TODO：位置
    @Target({})
    @Retention(RetentionPolicy.RUNTIME)
    @interface Posn {
        // TODO：第几位（用于posns属性时必须大于0，否则无效；而用于surplus属性，无效）
        int i() default 0;

        // TODO：是否隐藏
        boolean hide() default false;
    }
}
