package com.ouo.mask.core.annotation;

import lombok.Getter;

import java.lang.annotation.*;

/***********************************************************
 * TODO:     Hash脱敏注解
 *  目前只对CharSequence类型字段有效，若其值为JSON字符串时，该注解失效
 * Author:   刘春
 * Date:     2023/1/29
 ***********************************************************/
@Documented
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface Hash {
    // TODO：场景，默认：WEB和LOG
    SceneEnum scene() default SceneEnum.ALL;

    //TODO：Hash算法
    AlgorithmEnum algorithm() default AlgorithmEnum.SM3;

    //TODO：盐
    String salt() default "";

    //TODO：Hash算法
    @Getter
    enum AlgorithmEnum {
        SM3(10),
        MD5(20),
        HASH256(30);

        private int code;

        AlgorithmEnum(int code) {
            this.code = code;
        }

        @Override
        public String toString() {
            return this.name().toLowerCase();
        }
    }
}
