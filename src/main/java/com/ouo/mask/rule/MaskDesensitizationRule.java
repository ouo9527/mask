package com.ouo.mask.rule;

import com.ouo.mask.enums.SensitiveTypeEnum;
import lombok.Getter;
import lombok.Setter;

/***********************************************************
 * TODO:     掩盖脱敏规则
 * Author:   刘春
 * Date:     2023/1/29
 ***********************************************************/
@Getter
@Setter
public class MaskDesensitizationRule extends DesensitizationRule {
    // TODO：敏感类型
    private SensitiveTypeEnum type;
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
    private CustomShow show;

    //TODO：自定义显示
    @Setter
    @Getter
    public static class CustomShow {
        // TODO：前几位显示
        private int pre;
        // TODO：后几位显示
        private int suf;
    }
}
