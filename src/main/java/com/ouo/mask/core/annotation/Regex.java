package com.ouo.mask.core.annotation;

import java.lang.annotation.*;

/***********************************************************
 * TODO:     正则脱敏注解
 *  目前只对CharSequence类型字段有效，若其值为JSON字符串时，该注解失效
 *
 * Author:   刘春
 * Date:     2023/1/29
 ***********************************************************/
@Documented
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface Regex {
    // TODO：场景，默认：WEB和LOG
    SceneEnum scene() default SceneEnum.ALL;

    // TODO：正则表达式，Java正则特殊符号必须使用2个反斜杠，如：(\\d{3})\\d{4}(\\d{4})
    String pattern();

    // TODO：替换值，如：$1####$2，如：17788485848脱敏后177####5848
    String rv();
}
