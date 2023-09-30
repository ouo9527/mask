package com.ouo.mask.annotation;

import com.ouo.mask.enums.SceneEnum;
import com.ouo.mask.enums.SensitiveTypeEnum;

import java.lang.annotation.*;

/***********************************************************
 * TODO:     掩盖脱敏(属于固定值替换一种)注解
 *  目前只对CharSequence类型字段有效，若其值为JSON字符串时，该注解失效
 * Author:   刘春
 * Date:     2023/1/29
 ***********************************************************/
@Documented
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface Mask {
    // TODO：场景，默认：WEB和LOG
    SceneEnum scene() default SceneEnum.ALL;

    // TODO：敏感类型
    SensitiveTypeEnum type();

    /**
     * TODO：自定义显示，格式：前,后，如前3后4显示（3,4）、前3显示（3）、后4显示（,4）
     * 1、姓名：默认自动根据字符长度显示，当长度小于等于2，则显示第1个字符，否则显示前2个字符
     * 2、手机号：大陆-11位、台湾-10位、香港澳门-8位。默认自动根据字符长度显示大陆-前3后4、台湾-前3后3、香港澳门-前2后2
     * 3、固话：由3～4位区号+7～8位固定数字组成。默认自动根据字符长度显示，当区号小于等于3，则显示前3后2，否则前4后2
     * 4、身份证号：由6位地址码+8位出生日期+3位顺序码+1位校验码，有15位或18位。默认显示前3后4
     * 5、地址：默认自动根据字符长度显示，当长度大于6，则显示前6，则显示前7
     * 6、电子邮件：默认自动根据@前字符长度显示，且@后字符显示，当@前字符长度小于3，则@前字符全显示，否则显示前三位及@后
     * 7、日期：默认显示年份（采用正则处理，如：(.+年)?(.+月)?(.+日)?.+
     * 8、中国大陆车牌：由1个汉字+1个字母+5～6字母和数字组成。默认显示前2后2
     * 9、银行卡：默认显示前6后4
     * 10、护照：由1位字母（护照类型）+8位数字组成。默认显示前1后3
     * 11、数值：默认显示第1位
     */
    String custom() default "";

    /**
     * todo: 常用脱敏选项，用于自定义显示
     */
    interface CommonMaskOptions {
        // TODO：前1～9显示
        String PRE_1 = "1";
        String PRE_2 = "2";
        String PRE_3 = "3";
        String PRE_4 = "4";
        String PRE_5 = "5";
        String PRE_6 = "6";
        String PRE_7 = "7";
        String PRE_8 = "8";
        String PRE_9 = "9";

        // TODO：前1～9后1～9显示
        String PRE_1_SUF_1 = "1,1";
        String PRE_1_SUF_2 = "1,2";
        String PRE_1_SUF_3 = "1,3";
        String PRE_2_SUF_2 = "2,2";
        String PRE_3_SUF_1 = "3,1";
        String PRE_3_SUF_3 = "3,3";
        String PRE_3_SUF_4 = "3,4";
        String PRE_4_SUF_2 = "4,2";
        String PRE_4_SUF_4 = "4,4";
        String PRE_6_SUF_3 = "6,3";
        String PRE_6_SUF_4 = "6,4";

        // TODO：后1～9显示
        String SUF_1 = ",1";
        String SUF_2 = ",2";
        String SUF_3 = ",3";
        String SUF_4 = ",4";
        String SUF_5 = ",5";
        String SUF_6 = ",6";
        String SUF_7 = ",7";
        String SUF_8 = ",8";
        String SUF_9 = ",9";
    }
}
