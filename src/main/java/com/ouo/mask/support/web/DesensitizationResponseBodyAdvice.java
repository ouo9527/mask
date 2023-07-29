package com.ouo.mask.support.web;

import cn.hutool.core.annotation.AnnotationUtil;
import com.ouo.mask.annotation.Desensitization;
import com.ouo.mask.enums.SceneEnum;
import com.ouo.mask.handler.DesensitizationHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/***********************************************************
 * TODO:     脱敏处理AOP处理{@link org.springframework.web.bind.annotation.ResponseBody}
 * Author:   刘春
 * Date:     2022/12/1
 ***********************************************************/
@Order(Ordered.HIGHEST_PRECEDENCE)
@ControllerAdvice
@Slf4j
public class DesensitizationResponseBodyAdvice implements ResponseBodyAdvice<Object> {

    private DesensitizationHandler handler;

    public DesensitizationResponseBodyAdvice(DesensitizationHandler handler) {
        this.handler = handler;
    }

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        if (null != handler && null != returnType && !Void.TYPE.equals(returnType.getMethod().getReturnType())) {
            Boolean enabled = AnnotationUtil.getAnnotationValue(returnType.getMethod(), Desensitization.class,
                    "enabled");
            return null == enabled || enabled;
        }
        return false;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {

        return handler.desensitized(returnType.getDeclaringClass().getName(), SceneEnum.WEB, body);
    }
}
