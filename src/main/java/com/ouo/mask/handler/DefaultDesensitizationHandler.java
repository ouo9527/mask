package com.ouo.mask.handler;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.ouo.mask.annotation.*;
import com.ouo.mask.config.DesensitizationProperties;
import com.ouo.mask.enums.SceneEnum;
import com.ouo.mask.util.DesensitizedUtil;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;


/***********************************************************
 * TODO:     数据脱敏处理器
 * Author:   刘春
 * Date:     2022/12/3
 ***********************************************************/
@Slf4j
public class DefaultDesensitizationHandler implements DesensitizationHandler {

    @Setter
    private DesensitizationProperties properties;

    @Override
    public <T> T desensitized(SceneEnum scene, T data) {
        return desensitizedFromAnnotation(scene, null, null, data);
    }

    /**
     * todo:    从注解中脱敏处理
     *  注：对于待脱敏对象非基本(包含其包装)类型、数组/集合类型、迭代器和枚举类型外，则满足以下规则：
     *  1）必须提供public set/get 方法
     *  2）可以使用scan属性来扫描所需要待脱敏的对象
     *  目的都是降低递归所造成栈溢出
     * @param scene       场景
     * @param fieldName   字段名
     * @param annotations 字段注解(@Empty、@Hash、@Mask、@Regex、@Repl)
     * @param data        待脱敏数据对象
     * @return
     */
    private <T> T desensitizedFromAnnotation(SceneEnum scene, String fieldName, Annotation[] annotations, T data) {
        //todo：处理注解（局部）
        if (ArrayUtil.isArray(data)) {
            Object[] objs = new Object[ArrayUtil.length(data)];
            for (int i = 0; i < objs.length; i++) {
                objs[i] = this.desensitizedFromAnnotation(scene, null, null, ArrayUtil.get(data, i));
            }
            return (T) objs;
        } else if (data instanceof Collection) {
            final List<Object> objs = new ArrayList<>(((Collection) data).size());
            ((Collection) data).forEach(o -> objs.add(this.desensitizedFromAnnotation(scene, null, null, o)));
            return (T) objs;
        } else if (data instanceof Iterator) {
            final List<Object> objs = new ArrayList<>();
            final Iterator<?> iter = (Iterator<?>) data;
            while (iter.hasNext()) {
                objs.add(this.desensitizedFromAnnotation(scene, null, null, iter.next()));
            }
            return (T) objs;
        } else if (data instanceof Map) {
            final Map<Object, Object> objMap = new HashMap<>(((Map) data).size());
            ((Map) data).forEach((k, v) -> objMap.put(k, this.desensitizedFromAnnotation(scene, StrUtil.toStringOrNull(k), null, v)));
            return (T) objMap;
        } else if (data instanceof CharSequence) {
            String val = Convert.convert(String.class, data);
            if (JSON.isValidObject(val) || JSON.isValidArray(val)) {
                try {
                    return (T) JSON.toJSONString(this.desensitizedFromJson(scene, null, JSON.parse(val)));
                } catch (Exception e) {
                    log.debug("脱敏异常：", e);
                    return data;
                }
            }
            //优先全局（即配置文件）匹配
            T t = (T) DesensitizedUtil.desensitized(scene, properties.getRules().get(fieldName), fieldName, val);

            if (data.equals(t) && ArrayUtil.isNotEmpty(annotations)) {
                for (Annotation annotation : annotations) {
                    if (null == annotation) continue;
                    if (annotation instanceof Empty)
                        return (T) DesensitizedUtil.emptyDesensitized(scene, (Empty) annotation,
                                fieldName, val);
                    if (annotation instanceof Hash)
                        return (T) DesensitizedUtil.hashDesensitized(scene, (Hash) annotation,
                                fieldName, val);
                    if (annotation instanceof Regex)
                        return (T) DesensitizedUtil.regexDesensitized(scene, (Regex) annotation,
                                fieldName, val);
                    if (annotation instanceof Repl)
                        return (T) DesensitizedUtil.replDesensitized(scene, (Repl) annotation,
                                fieldName, val);
                    if (annotation instanceof Mask)
                        return (T) DesensitizedUtil.maskDesensitized(scene, (Mask) annotation,
                                fieldName, val);
                }
            }
            return t;
        } else if (null == data || ObjectUtil.isBasicType(data) || data instanceof Enumeration || data instanceof Number) {//todo：基础类型、枚举、数值
            return data;
        } else {
            Field[] fields = ReflectUtil.getFields(data.getClass());
            if (null == fields) return data;
            for (Field field : fields) {
                try {
                    //todo：必须提供public set/get 方法；
                    Method publicGet = ReflectUtil.getPublicMethod(data.getClass(), StrUtil.genGetter(field.getName()));
                    if (null == publicGet) continue;
                    Object val = this.desensitizedFromAnnotation(scene, field.getName(), field.getAnnotations(),
                            ReflectUtil.invoke(data, publicGet));
                    //todo: 会造成private final 无法获取和修改
                    //field.setAccessible(true);
                    //field.set(obj, Convert.convert(field.getType(), val));
                    Method publicSet = ReflectUtil.getPublicMethod(data.getClass(), StrUtil.genSetter(field.getName()), field.getType());
                    if (null != publicSet) ReflectUtil.invoke(data, publicSet, val);
                } catch (Exception e) {
                    log.debug("脱敏异常：", e);
                }
            }
            return data;
        }
    }

    /**
     * todo:    从JSON中脱敏处理
     *
     * @param scene 场景
     * @param key   字段
     * @param json  待脱敏数据
     * @return
     */
    private Object desensitizedFromJson(SceneEnum scene, String key, Object json) {
        if (json instanceof List) {
            final List result = new ArrayList<>();
            ((List) json).forEach((t) -> result.add(this.desensitizedFromJson(scene, null, t)));
            return result;
        } else if (json instanceof Map) {
            Map<String, Object> result = new LinkedHashMap();
            ((Map<String, Object>) json).forEach((k, v) -> result.put(k, this.desensitizedFromJson(scene, k, v)));
            return result;
        } else {
            //todo：对key-value脱敏处理
            return DesensitizedUtil.desensitized(scene, properties.getRules().get(key), key, Convert.convert(String.class, json, ""));
        }
    }

    @Override
    public boolean isValid(String context, String[] roleId) {
        //todo: 无需验证策略，即需要脱敏
        if (null == properties.getStrategy()) return true;
        //todo：验证脱敏范围即配置包路径，多个值时以英文逗号隔开；为了减少不必要的数据脱敏，否则会影响系统性能，因此推荐设置。若为空则所有路径生效
        if (ArrayUtil.isNotEmpty(properties.getStrategy().getPackages()) &&
                !StrUtil.startWithAny(context, properties.getStrategy().getPackages())) return false;
        //todo：验证角色集中是否存在非脱敏角色
        if (ArrayUtil.isNotEmpty(roleId) && ArrayUtil.containsAny(roleId,
                properties.getStrategy().getNonMaskRule())) return false;
        //todo：验证脱敏有效期
        Date effectDate = properties.getStrategy().getEffectDate();
        Date expiryDate = properties.getStrategy().getExpiryDate();
        return !new Date().before(null == effectDate ? new Date() : effectDate) &&
                !new Date().after(null == expiryDate ? new Date() : expiryDate);
    }
}
