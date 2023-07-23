package com.ouo.mask.core.util;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SmUtil;
import cn.hutool.crypto.digest.DigestAlgorithm;
import cn.hutool.crypto.digest.Digester;
import cn.hutool.crypto.digest.MD5;
import com.ouo.mask.core.annotation.*;
import com.ouo.mask.core.property.*;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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
    public static String emptyDesensitized(SceneEnum scene, EmptyDesensitization annotation, String fieldName, String data) {
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
    public static String hashDesensitized(SceneEnum scene, HashDesensitization annotation, String fieldName, String data) {
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
    public static String regexDesensitized(SceneEnum scene, RegexDesensitization annotation, String fieldName, String data) {
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
    public static String replaceDesensitized(SceneEnum scene, ReplaceDesensitizationRule rule, String fieldName, String data) {
        if (notDesensitization(scene, rule, fieldName, data)) return data;
        int index = 0;
        String val = data;
        //todo：前几位
        List<ReplaceDesensitizationRule.PosnProperty> posns = rule.getPosns();
        if (null != posns) {
            for (ReplaceDesensitizationRule.PosnProperty posn : posns) {
                if (index >= val.length()) break;
                if (null == posn) continue;
                int i = posn.getI();
                val = rpv(posn, val, index, false);
                index = i;
            }
        }
        //todo: 剩余位
        ReplaceDesensitizationRule.PosnProperty surplus = rule.getSurplus();
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
    public static String replaceDesensitized(SceneEnum scene, ReplaceDesensitization annotation, String fieldName, String data) {
        if (null == annotation) return data;
        ReplaceDesensitizationRule rule = new ReplaceDesensitizationRule();
        rule.setScene(annotation.scene());
        rule.setField(fieldName);

        ReplaceDesensitizationRule.PosnProperty surplus = new ReplaceDesensitizationRule.PosnProperty();
        if (null != annotation.surplus()) {
            surplus.setFixed(annotation.surplus().fixed());
            surplus.setRv(annotation.surplus().rv());
            rule.setSurplus(surplus);
        }

        List<ReplaceDesensitizationRule.PosnProperty> posns = new ArrayList<>();
        if (null != annotation.posns()) {
            for (ReplaceDesensitization.Posn replacePosn : annotation.posns()) {
                if (null == replacePosn) continue;
                ReplaceDesensitizationRule.PosnProperty posn = new ReplaceDesensitizationRule.PosnProperty();
                posn.setI(replacePosn.i());
                posn.setFixed(replacePosn.fixed());
                posn.setRv(replacePosn.rv());

                posns.add(posn);
            }
        }
        rule.setPosns(posns);

        return replaceDesensitized(scene, rule, fieldName, data);
    }

    /**
     * 掩盖模式(脱敏后等长)：根据场景匹配掩盖注解中规则脱敏该字段的数据
     *
     * @param scene      场景
     * @param annotation 规则
     * @param fieldName  字段名
     * @param data       数据
     * @return
     */
    public static String maskDesensitized(SceneEnum scene, MaskDesensitization annotation, String fieldName, String data) {
        if (null == annotation) return data;
        ReplaceDesensitizationRule rule = new ReplaceDesensitizationRule();
        rule.setScene(annotation.scene());
        rule.setField(fieldName);

        ReplaceDesensitizationRule.PosnProperty surplus = new ReplaceDesensitizationRule.PosnProperty();
        surplus.setFixed(true);
        surplus.setRv(annotation.surplusHide() ? "*" : "");
        rule.setSurplus(surplus);

        List<ReplaceDesensitizationRule.PosnProperty> posns = new ArrayList<>();
        if (null != annotation.posns()) {
            for (MaskDesensitization.Posn maskPosn : annotation.posns()) {
                if (null == maskPosn) continue;
                ReplaceDesensitizationRule.PosnProperty posn = new ReplaceDesensitizationRule.PosnProperty();
                posn.setI(maskPosn.i());
                posn.setFixed(true);
                posn.setRv(maskPosn.hide() ? "*" : "");

                posns.add(posn);
            }
        }
        rule.setPosns(posns);

        return replaceDesensitized(scene, rule, fieldName, data);
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
            if (rule instanceof ReplaceDesensitizationRule) {//todo: 替换(脱敏后等长)
                return replaceDesensitized(scene, (ReplaceDesensitizationRule) rule, fieldName, data);
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
    private static String rpv(ReplaceDesensitizationRule.PosnProperty posn, String val, int index, boolean surplus) {
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
