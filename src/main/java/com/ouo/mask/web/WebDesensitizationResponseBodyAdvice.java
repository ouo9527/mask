package com.ouo.mask.web;

import cn.hutool.core.collection.CollUtil;
import com.ouo.mask.core.DefaultDesensitizationHandler;
import com.ouo.mask.core.DesensitizationRuleLoader;
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

    private DesensitizationRuleLoader loader;

    public WebDesensitizationResponseBodyAdvice(DesensitizationRuleLoader loader) {
        this.loader = loader;
    }

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return CollUtil.isNotEmpty(loader.load()) && !Void.TYPE.equals(returnType.getMethod().getReturnType());
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {

        return new DefaultDesensitizationHandler(SceneEnum.WEB, loader.load()).desensitized(body);
    }

}
