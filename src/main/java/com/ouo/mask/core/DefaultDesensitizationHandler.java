package com.ouo.mask.core;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.ouo.mask.core.annotation.*;
import com.ouo.mask.core.util.DesensitizedUtil;
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
public class DefaultDesensitizationHandler extends DesensitizationHandler {

    public DefaultDesensitizationHandler(DesensitizationRuleLoader loader) {
        super(loader);
    }

    @Override
    public Object desensitized(String context, SceneEnum scene, Object obj) {
        if (CollUtil.isNotEmpty(loader.getDesensitizationRules()) &&
                (CollUtil.isEmpty(loader.getScan()) || StrUtil.startWithAny(context,
                        ArrayUtil.toArray(loader.getScan(), String.class)))) { //todo：对类过滤处理，避免不必要的脱敏
            return desensitizedFromAnnotation(scene, null, null, obj);
        }
        return obj;
    }

    /**
     * todo:    从注解中脱敏处理
     *  注：对于待脱敏对象非基本(包含其包装)类型、数组/集合类型、迭代器和枚举类型外，则满足以下规则：
     *  1）必须提供public set/get 方法
     *  2）可以使用scan属性来扫描所需要待脱敏的对象
     *  目的都是降低递归所造成栈溢出
     * @param scene       场景
     * @param fieldName   字段名
     * @param annotations 字段注解(@EmptyDesensitization、@HashDesensitization、@MaskDesensitization、@RegexDesensitization、@ReplaceDesensitization)
     * @param obj         待脱敏数据对象
     * @return
     */
    private <T> T desensitizedFromAnnotation(SceneEnum scene, String fieldName, Annotation[] annotations, T obj) {
        //todo：处理注解（局部）
        if (ArrayUtil.isArray(obj)) {
            Object[] objs = new Object[ArrayUtil.length(obj)];
            for (int i = 0; i < objs.length; i++) {
                objs[i] = this.desensitizedFromAnnotation(scene, null, null, ArrayUtil.get(obj, i));
            }
            return (T) objs;
        } else if (obj instanceof Collection) {
            final List<Object> objs = new ArrayList<>(((Collection) obj).size());
            ((Collection) obj).forEach(o -> objs.add(this.desensitizedFromAnnotation(scene, null, null, o)));
            return (T) objs;
        } else if (obj instanceof Iterator) {
            final List<Object> objs = new ArrayList<>();
            final Iterator<?> iter = (Iterator<?>) obj;
            while (iter.hasNext()) {
                objs.add(this.desensitizedFromAnnotation(scene, null, null, iter.next()));
            }
            return (T) objs;
        } else if (obj instanceof Map) {
            final Map<Object, Object> objMap = new HashMap<>(((Map) obj).size());
            ((Map) obj).forEach((k, v) -> objMap.put(k, this.desensitizedFromAnnotation(scene, StrUtil.toStringOrNull(k), null, v)));
            return (T) objMap;
        } else if (obj instanceof CharSequence) {
            String val = Convert.convert(String.class, obj);
            if (JSON.isValidObject(val) || JSON.isValidArray(val)) {
                try {
                    return (T) JSON.toJSONString(this.desensitizedFromJson(scene, null, JSON.parse(val)));
                } catch (Exception e) {
                    log.debug("脱敏异常：", e);
                    return obj;
                }
            }
            boolean globalSearch = true;

            if (ArrayUtil.isNotEmpty(annotations)) {
                for (Annotation annotation : annotations) {
                    if (null == annotation) continue;
                    if (annotation instanceof Empty) {
                        globalSearch = false;
                        return (T) DesensitizedUtil.emptyDesensitized(scene, (Empty) annotation,
                                fieldName, val);
                    }
                    if (annotation instanceof Hash) {
                        globalSearch = false;
                        return (T) DesensitizedUtil.hashDesensitized(scene, (Hash) annotation,
                                fieldName, val);
                    }
                    if (annotation instanceof Regex) {
                        globalSearch = false;
                        return (T) DesensitizedUtil.regexDesensitized(scene, (Regex) annotation,
                                fieldName, val);
                    }
                    if (annotation instanceof Replace) {
                        globalSearch = false;
                        return (T) DesensitizedUtil.replaceDesensitized(scene, (Replace) annotation,
                                fieldName, val);
                    }
                    if (annotation instanceof Mask) {
                        globalSearch = false;
                        return (T) DesensitizedUtil.maskDesensitized(scene, (Mask) annotation,
                                fieldName, val);
                    }
                }
            }
            return globalSearch ? (T) DesensitizedUtil.desensitized(scene, loader.getDesensitizationRules(), fieldName, val) : obj;
        } else if (null == obj || ObjectUtil.isBasicType(obj) || obj instanceof Enumeration || obj instanceof Number) {//todo：基础类型、枚举、数值
            return obj;
        } else {
            Field[] fields = ReflectUtil.getFields(obj.getClass());
            if (null == fields) return obj;
            for (Field field : fields) {
                try {
                    //todo：必须提供public set/get 方法；
                    Method publicGet = ReflectUtil.getPublicMethod(obj.getClass(), StrUtil.genGetter(field.getName()));
                    if (null == publicGet) continue;
                    Object val = this.desensitizedFromAnnotation(scene, field.getName(), field.getAnnotations(),
                            ReflectUtil.invoke(obj, publicGet));
                    //todo: 会造成private final 无法获取和修改
                    //field.setAccessible(true);
                    //field.set(obj, Convert.convert(field.getType(), val));
                    Method publicSet = ReflectUtil.getPublicMethod(obj.getClass(), StrUtil.genSetter(field.getName()), field.getType());
                    if (null != publicSet) ReflectUtil.invoke(obj, publicSet, val);
                } catch (Exception e) {
                    log.debug("脱敏异常：", e);
                }
            }
            return obj;
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
            return DesensitizedUtil.desensitized(scene, loader.getDesensitizationRules(), key, Convert.convert(String.class, json, ""));
        }
    }
}
