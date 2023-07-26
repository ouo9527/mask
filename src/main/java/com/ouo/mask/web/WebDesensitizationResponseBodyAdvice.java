package com.ouo.mask.web;

import com.ouo.mask.core.DesensitizationHandler;
import com.ouo.mask.core.annotation.SceneEnum;
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
public class WebDesensitizationResponseBodyAdvice implements ResponseBodyAdvice<Object> {

    private DesensitizationHandler handler;

    public WebDesensitizationResponseBodyAdvice(DesensitizationHandler handler) {
        this.handler = handler;
    }

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return null != handler && null != returnType && !Void.TYPE.equals(returnType.getMethod().getReturnType());
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {

        return handler.desensitized(SceneEnum.WEB, body);
    }
}
