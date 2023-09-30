package com.ouo.mask.util;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SmUtil;
import cn.hutool.crypto.digest.DigestAlgorithm;
import cn.hutool.crypto.digest.Digester;
import cn.hutool.crypto.digest.MD5;
import com.ouo.mask.annotation.*;
import com.ouo.mask.enums.SceneEnum;
import com.ouo.mask.rule.*;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

@Slf4j
public abstract class DesensitizedUtil {

    /**
     * 置空模式(脱敏后不等长)：根据场景匹配规则脱敏该字段的数据
     *
     * @param scene     场景
     * @param rule      规则
     * @param fieldName 字段名
     * @param data      数据
     * @return
     */
    public static String emptyDesensitized(SceneEnum scene, EmptyDesensitizationRule rule, String fieldName, String data) {
        return notDesensitization(scene, rule, fieldName, data) ? data : "";
    }

    /**
     * 置空模式(脱敏后不等长)：根据场景匹配注解中规则脱敏该字段的数据
     *
     * @param scene      场景
     * @param annotation 规则
     * @param fieldName  字段名
     * @param data       数据
     * @return
     */
    public static String emptyDesensitized(SceneEnum scene, Empty annotation, String fieldName, String data) {
        if (null == annotation) return data;
        EmptyDesensitizationRule rule = new EmptyDesensitizationRule();
        rule.setScene(annotation.scene());
        rule.setField(fieldName);

        return emptyDesensitized(scene, rule, fieldName, data);
    }

    /**
     * Hash模式(脱敏后不等长)：根据场景匹配规则脱敏该字段的数据
     *
     * @param scene     场景
     * @param rule      规则
     * @param fieldName 字段名
     * @param data      数据
     * @return
     */
    public static String hashDesensitized(SceneEnum scene, HashDesensitizationRule rule, String fieldName, String data) {
        if (notDesensitization(scene, rule, fieldName, data)) return data;
        switch (rule.getAlgorithm()) {
            case HASH256:
                return new Digester(DigestAlgorithm.SHA256)
                        .setSalt(StrUtil.trimToEmpty(rule.getSalt()).getBytes())
                        .digestHex(data);
            case MD5:
                return new MD5()
                        .setSalt(StrUtil.trimToEmpty(rule.getSalt()).getBytes())
                        .digestHex(data);
            case SM3:
            default:
                return SmUtil.sm3WithSalt(StrUtil.trimToEmpty(rule.getSalt()).getBytes())
                        .digestHex(data);
        }
    }

    /**
     * Hash模式(脱敏后不等长)：根据场景匹配Hash注解中规则脱敏该字段的数据
     *
     * @param scene      场景
     * @param annotation 规则
     * @param fieldName  字段名
     * @param data       数据
     * @return
     */
    public static String hashDesensitized(SceneEnum scene, Hash annotation, String fieldName, String data) {
        if (null == annotation) return data;
        HashDesensitizationRule rule = new HashDesensitizationRule();
        rule.setScene(annotation.scene());
        rule.setField(fieldName);
        rule.setAlgorithm(annotation.algorithm());
        rule.setSalt(annotation.salt());

        return desensitized(scene, CollUtil.newArrayList(rule), fieldName, data);
    }

    /**
     * 正则模式(脱敏后可能不等长)：根据场景匹配规则脱敏该字段的数据
     *
     * @param scene     场景
     * @param rule      规则
     * @param fieldName 字段名
     * @param data      数据
     * @return
     */
    public static String regexDesensitized(SceneEnum scene, RegexDesensitizationRule rule, String fieldName, String data) {
        if (notDesensitization(scene, rule, fieldName, data)) return data;
        //todo: Java正则特殊符号必须使用2个反斜杠，如：(\\d{3})\\d{4}(\\d{4})
        //todo: 替换值：$1####$2，如：17788485848脱敏后177####5848
        if (StrUtil.isNotBlank(rule.getPattern())) {
            return ReUtil.replaceAll(data, rule.getPattern(), rule.getRv());
        }
        return data;
    }

    /**
     * 正则模式(脱敏后不等长)：根据场景匹配正则注解中规则脱敏该字段的数据
     *
     * @param scene      场景
     * @param annotation 规则
     * @param fieldName  字段名
     * @param data       数据
     * @return
     */
    public static String regexDesensitized(SceneEnum scene, Regex annotation, String fieldName, String data) {
        if (null == annotation) return data;
        RegexDesensitizationRule rule = new RegexDesensitizationRule();
        rule.setScene(annotation.scene());
        rule.setField(fieldName);
        rule.setPattern(annotation.pattern());
        rule.setRv(annotation.rv());

        return desensitized(scene, CollUtil.newArrayList(rule), fieldName, data);
    }

    /**
     * 替换模式(脱敏后等长)：根据场景匹配规则脱敏该字段的数据
     *
     * @param scene     场景
     * @param rule      规则
     * @param fieldName 字段名
     * @param data      数据
     * @return
     */
    public static String replDesensitized(SceneEnum scene, ReplDesensitizationRule rule, String fieldName, String data) {
        if (notDesensitization(scene, rule, fieldName, data)) return data;
        int index = 0;
        String val = data;
        //todo：前几位
        List<ReplDesensitizationRule.Posn> posns = rule.getPosns();
        if (null != posns) {
            for (ReplDesensitizationRule.Posn posn : posns) {
                if (index >= val.length()) break;
                if (null == posn) continue;
                int i = posn.getI();
                val = rpv(posn, val, index, false);
                index = i;
            }
        }
        //todo: 剩余位
        ReplDesensitizationRule.Posn surplus = rule.getSurplus();
        if (null != surplus && index < val.length()) {
            return rpv(surplus, val, index, true);
        }
        return val;
    }

    /**
     * 替换模式(脱敏后等长)：根据场景匹配替换注解中规则脱敏该字段的数据
     *
     * @param scene      场景
     * @param annotation 规则
     * @param fieldName  字段名
     * @param data       数据
     * @return
     */
    public static String replDesensitized(SceneEnum scene, Repl annotation, String fieldName, String data) {
        if (null == annotation) return data;
        ReplDesensitizationRule rule = new ReplDesensitizationRule();
        rule.setScene(annotation.scene());
        rule.setField(fieldName);

        ReplDesensitizationRule.Posn surplus = new ReplDesensitizationRule.Posn();
        if (null != annotation.surplus()) {
            surplus.setFixed(annotation.surplus().fixed());
            surplus.setRv(annotation.surplus().rv());
            rule.setSurplus(surplus);
        }

        List<ReplDesensitizationRule.Posn> posns = new ArrayList<>();
        if (null != annotation.posns()) {
            for (Repl.Posn p : annotation.posns()) {
                if (null == p) continue;
                ReplDesensitizationRule.Posn posn = new ReplDesensitizationRule.Posn();
                posn.setI(p.i());
                posn.setFixed(p.fixed());
                posn.setRv(p.rv());

                posns.add(posn);
            }
        }
        rule.setPosns(posns);

        return replDesensitized(scene, rule, fieldName, data);
    }

    /**
     * 掩盖模式(脱敏后等长)：根据场景匹配掩盖注解中规则脱敏该字段的数据
     *  1、姓名：默认自动根据字符长度显示，当长度小于等于2，则显示第1个字符，否则显示前2个字符
     *  2、手机号：大陆-11位、台湾-10位、香港澳门-8位。默认自动根据字符长度显示大陆-前3后4、台湾-前3后3、香港澳门-前2后2
     *  3、固话：由3～4位区号+7～8位固定数字组成。默认自动根据字符长度显示，当区号小于等于3，则显示前3后2，否则前4后2
     *  4、身份证号：由6位地址码+8位出生日期+3位顺序码+1位校验码，有15位或18位。默认显示前3后4
     *  5、地址：默认自动根据正则：(.+省)?(.+市)?(.+自治区)?(.+行政区)?(.+县)?(.+区)?.+  进行对省以下进行如：广西壮族自治区**显
     *  6、电子邮件：默认自动根据@前字符长度显示，且@后字符显示，当@前字符长度小于3，则@前字符全显示，否则显示前三位及@后
     *  7、中国大陆车牌：由1个汉字+1个字母+5～6字母和数字组成。默认显示前2后2
     *  8、银行卡：默认显示前6后4
     *  9、护照：由1位字母（护照类型）+8位数字组成。默认显示前1后3
     *  10、数值：默认显示第1位
     *
     * @param scene      场景
     * @param annotation 规则
     * @param fieldName  字段名
     * @param data       数据
     * @return
     */
    public static String maskDesensitized(SceneEnum scene, Mask annotation, String fieldName, String data) {
        if (null == annotation) return data;
        int len = StrUtil.length(data);
        if (StrUtil.isBlank(annotation.custom())) {
            switch (annotation.type()) {
                case FULL_NAME:
                    return StrUtil.hide(data, 2 < len ? 2 : 1, len);
                case MOBILE_PHONE: {
                    int pre = 0;
                    int suf = 0;
                    if (8 >= len) {
                        pre = suf = 2;
                    } else if (10 >= len) {
                        pre = suf = 3;
                    } else {
                        pre = 3;
                        suf = 4;
                    }
                    return StrUtil.hide(data, pre, len - suf);
                }
                case FIXED_PHONE: {
                    List<String> newData = StrUtil.splitTrim(data, '-');
                    int pre = 0;
                    int suf = 2;
                    if (2 == newData.size()) {
                        pre = StrUtil.length(newData.get(0)) + 1;
                    } else if (10 >= len) {
                        pre = 3;
                    } else {
                        pre = 4;
                    }
                    return StrUtil.hide(data, pre, len - suf);
                }
                case ID_CARD:
                    return StrUtil.hide(data, 3, len - 4);
                case ADDRESS: {
                    List<String> addrs = ReUtil.getAllGroups(Pattern.compile("(.+省)?(.+市)?(.+自治区)?(.+行政区)?(.+县)?(.+区)?.+"), data, false);
                    if (CollUtil.isEmpty(addrs)) return data;
                    StringBuilder newData = new StringBuilder();
                    int i = 0;
                    if (StrUtil.isNotBlank(addrs.get(0))) {
                        newData.append(addrs.get(0));
                        i += addrs.get(0).length();
                    }
                    if (StrUtil.isNotBlank(addrs.get(1))) {
                        newData.append(StrUtil.hide(addrs.get(1), -1, addrs.get(1).length() - 1));
                        i += addrs.get(1).length();
                    }
                    if (StrUtil.isNotBlank(addrs.get(2))) {
                        newData.append(StrUtil.hide(addrs.get(2), -1, addrs.get(2).length() - 3));
                        i += addrs.get(2).length();
                    }
                    if (StrUtil.isNotBlank(addrs.get(3))) {
                        newData.append(StrUtil.hide(addrs.get(3), -1, addrs.get(3).length() - 3));
                        i += addrs.get(3).length();
                    }
                    if (StrUtil.isNotBlank(addrs.get(4))) {
                        newData.append(StrUtil.hide(addrs.get(4), -1, addrs.get(4).length() - 1));
                        i += addrs.get(4).length();
                    }
                    if (StrUtil.isNotBlank(addrs.get(5))) {
                        newData.append(StrUtil.hide(addrs.get(5), -1, addrs.get(5).length() - 1));
                        i += addrs.get(5).length();
                    }
                    return 0 == i ? StrUtil.hide(data, 7, len)
                            : newData.append(StrUtil.repeat('*', len - i)).toString();
                }
                case EMAIL: {
                    List<String> email = StrUtil.splitTrim(data, '@');
                    if (2 == email.size()) {
                        StringBuilder newData = new StringBuilder();
                        return newData
                                .append(StrUtil.hide(email.get(0), 3, StrUtil.length(email.get(0))))
                                .append('@')
                                .append(email.get(1))
                                .toString();
                    } else return data;
                }
                case CAR_LICENSE:
                    return StrUtil.hide(data, 2, len - 2);
                case BANK_CARD:
                    return StrUtil.hide(data, 6, len - 4);
                case PASSPORT:
                    return StrUtil.hide(data, 1, len - 3);
                case NUMBER:
                    return StrUtil.hide(data, 1, len);
                default:
                    return data;
            }
        } else {
            List<String> custom = StrUtil.split(annotation.custom(), ',');
            if (1 == custom.size()) {
                return StrUtil.hide(data, NumberUtil.parseInt(StrUtil.trim(custom.get(0))), len);
            }
            return StrUtil.hide(data, NumberUtil.parseInt(StrUtil.trim(custom.get(0))),
                    len - NumberUtil.parseInt(StrUtil.trim(custom.get(1))));
        }

    }

    /**
     * 根据场景匹配规则脱敏该字段的数据
     *
     * @param scene     场景
     * @param rules     规则
     * @param fieldName 字段名
     * @param data      数据
     * @return
     */
    public static String desensitized(SceneEnum scene, Collection<DesensitizationRule> rules, String fieldName, String data) {
        if (null == scene || CollUtil.isEmpty(rules) || StrUtil.isBlank(fieldName) || StrUtil.isBlank(data)) {
            return data;
        }
        //todo: 脱敏规则
        for (DesensitizationRule rule : rules) {
            if ((null == rule || !StrUtil.equals(fieldName, rule.getField()) || null == scene) ||
                    (SceneEnum.ALL != scene && SceneEnum.ALL != rule.getScene() &&
                            null != rule.getScene() && scene != rule.getScene())) continue;
            if (rule instanceof EmptyDesensitizationRule) {//todo: 置空(脱敏后不等长)
                return emptyDesensitized(scene, (EmptyDesensitizationRule) rule, fieldName, data);
            }
            if (rule instanceof HashDesensitizationRule) {//todo: HASH(脱敏后不等长)
                return hashDesensitized(scene, (HashDesensitizationRule) rule, fieldName, data);
            }
            if (rule instanceof RegexDesensitizationRule) {//todo: 正则(脱敏后可能不等长)
                return regexDesensitized(scene, (RegexDesensitizationRule) rule, fieldName, data);
            }
            if (rule instanceof ReplDesensitizationRule) {//todo: 替换(脱敏后等长)
                return replDesensitized(scene, (ReplDesensitizationRule) rule, fieldName, data);
            }
        }
        return data;
    }

    /**
     * todo：位置所对应的值替换
     *
     * @param posn
     * @param val
     * @param index
     * @param surplus
     * @return
     */
    private static String rpv(ReplDesensitizationRule.Posn posn, String val, int index, boolean surplus) {
        if (null == posn || StrUtil.isEmpty(val)) return val;
        int i = surplus ? val.length() : posn.getI();
        int span = i - index >= val.length() ? val.length() - index : i - index;
        if (posn.isFixed()) {//todo: 固定值
            String rv = posn.getRv();
            if (StrUtil.isNotEmpty(rv)) {//todo：若替换值为空保持原值
                if (span >= rv.length()) //todo：替换值长度小于所要填充位置时，需填充
                    val = StrUtil.replace(val, index, i, StrUtil.repeatByLength(rv, span));
                else val = StrUtil.replace(val, index, i, StrUtil.subPre(rv, span));
            }
        } else {//todo: 随机值
            val = StrUtil.replace(val, index, i, RandomUtil.randomString(span));
        }
        return val;
    }

    /**
     * 校验是否不能脱敏
     *
     * @param scene
     * @param rule
     * @param fieldName
     * @param data
     * @return
     */
    private static boolean notDesensitization(SceneEnum scene, DesensitizationRule rule, String fieldName, String data) {
        log.debug("校验是否不能脱敏：场景={}, 脱敏规则={}, 待脱敏字段={}", scene, rule, fieldName);
        return ((null == rule || !StrUtil.equals(fieldName, rule.getField()) || null == scene) ||
                (SceneEnum.ALL != scene && SceneEnum.ALL != rule.getScene() &&
                        null != rule.getScene() && scene != rule.getScene()));
    }
}
