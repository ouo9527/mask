package com.ouo.mask.core;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.ouo.mask.core.annotation.*;
import com.ouo.mask.core.property.DesensitizationRule;
import com.ouo.mask.core.util.DesensitizedUtil;
import lombok.extern.slf4j.Slf4j;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.*;


/***********************************************************
 * TODO:     数据脱敏处理器
 * Author:   刘春
 * Date:     2022/12/3
 ***********************************************************/
@Slf4j
public class DefaultDesensitizationHandler extends DesensitizationHandler {

    public DefaultDesensitizationHandler(SceneEnum scene, Collection<DesensitizationRule> rules) {
        super(scene, rules);
    }

    @Override
    public Object desensitized(Object obj) {
        return desensitizedFromAnnotation(null, null, obj);
    }

    /**
     * todo:    从JSON中脱敏处理
     *
     * @param key  字段
     * @param json 待脱敏数据
     * @return
     */
    private Object desensitizedFromJson(String key, Object json) {
        if (json instanceof List) {
            final List result = new ArrayList<>();
            ((List) json).forEach((t) -> result.add(desensitizedFromJson(null, t)));
            return result;
        } else if (json instanceof Map) {
            Map<String, Object> result = new LinkedHashMap();
            ((Map<String, Object>) json).forEach((k, v) -> result.put(k, desensitizedFromJson(k, v)));
            return result;
        } else {
            //todo：对key-value脱敏处理
            return DesensitizedUtil.desensitized(scene, rules, key, Convert.convert(String.class, json, ""));
        }
    }

    /**
     * todo:    从注解中脱敏处理
     *
     * @param fieldName   字段名
     * @param annotations 字段注解(@EmptyDesensitization、@HashDesensitization、@MaskDesensitization、@RegexDesensitization、@ReplaceDesensitization)
     * @param obj         待脱敏数据对象
     * @return
     */
    private Object desensitizedFromAnnotation(String fieldName, Annotation[] annotations, Object obj) {
        if (null == obj) return obj;
        //todo：处理注解（局部）
        if (ArrayUtil.isArray(obj)) {
            Object[] objs = new Object[ArrayUtil.length(obj)];
            for (int i = 0; i < objs.length; i++) {
                objs[i] = desensitizedFromAnnotation(null, null, ArrayUtil.get(obj, i));
            }
            return objs;
        } else if (obj instanceof Collection) {
            final List<Object> objs = new ArrayList<>(((Collection) obj).size());
            ((Collection) obj).forEach(o -> objs.add(desensitizedFromAnnotation(null, null, o)));
            return objs;
        } else if (obj instanceof Iterator) {
            final List<Object> objs = new ArrayList<>();
            final Iterator<?> iter = (Iterator<?>) obj;
            while (iter.hasNext()) {
                objs.add(desensitizedFromAnnotation(null, null, iter.next()));
            }
            return objs;
        } else if (obj instanceof Map) {
            final Map<Object, Object> objMap = new HashMap<>(((Map) obj).size());
            ((Map) obj).forEach((k, v) -> objMap.put(k, this.desensitizedFromAnnotation(StrUtil.toStringOrNull(k), null, v)));
            return objMap;
        } else if (obj instanceof CharSequence) {
            String val = Convert.convert(String.class, obj);
            if (JSON.isValidObject(val) || JSON.isValidArray(val)) {
                try {
                    return this.desensitizedFromJson(null, JSON.parse(val));
                } catch (Exception e) {
                    log.error("脱敏异常：", e);
                    return obj;
                }
            }
            boolean globalSearch = true;

            if (ArrayUtil.isNotEmpty(annotations)) {
                for (Annotation annotation : annotations) {
                    if (null == annotation) continue;
                    if (annotation instanceof EmptyDesensitization) {
                        globalSearch = false;
                        return DesensitizedUtil.emptyDesensitized(scene, (EmptyDesensitization) annotation,
                                fieldName, val);
                    }
                    if (annotation instanceof HashDesensitization) {
                        globalSearch = false;
                        return DesensitizedUtil.hashDesensitized(scene, (HashDesensitization) annotation,
                                fieldName, val);
                    }
                    if (annotation instanceof RegexDesensitization) {
                        globalSearch = false;
                        return DesensitizedUtil.regexDesensitized(scene, (RegexDesensitization) annotation,
                                fieldName, val);
                    }
                    if (annotation instanceof ReplaceDesensitization) {
                        globalSearch = false;
                        return DesensitizedUtil.replaceDesensitized(scene, (ReplaceDesensitization) annotation,
                                fieldName, val);
                    }
                    if (annotation instanceof MaskDesensitization) {
                        globalSearch = false;
                        return DesensitizedUtil.maskDesensitized(scene, (MaskDesensitization) annotation,
                                fieldName, val);
                    }
                }
            }
            return globalSearch ? DesensitizedUtil.desensitized(scene, rules, fieldName, val) : obj;
        } else if (ObjectUtil.isBasicType(obj) || obj instanceof Enumeration || obj instanceof Number) {//todo：基础类型、枚举、数值
            return obj;
        } else {
            Field[] fields = ReflectUtil.getFields(obj.getClass());
            if (null == fields) return obj;
            for (Field field : fields) {
                try {
                    field.setAccessible(true);
                    field.set(obj, Convert.convert(field.getType(), desensitizedFromAnnotation(field.getName(), field.getAnnotations(), field.get(obj))));
                } catch (Exception e) {
                    log.error("脱敏异常：", e);
                }
            }
            return obj;
        }
    }
}
