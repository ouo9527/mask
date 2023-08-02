package com.ouo.mask.support.log;

import ch.qos.logback.classic.pattern.MessageConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import com.ouo.mask.enums.SceneEnum;
import com.ouo.mask.handler.DesensitizationHandler;
import com.ouo.mask.util.SpringUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/***********************************************************
 * TODO:     基于logback提供MessageConverter，在message打印之前，
 *  允许对“参数格式化之后的message”（formattedMessage）进行转换，
 *  最终logger打印的实际内容是converter返回的整形后的结果。
 *  需要在logback.xml配置文件，增加如下配置：
 *  <configuration>
 *      ……
 *      <conversionRule conversionWord="m" converterClass="com.ouo.mask.log.LogbackDesensitizeConverter"/>
 *      ……
 *  </configuration>
 *  注：conversionRule标签中conversionWord即定义日志输出格式中的信息参数，默认%msg，若如上自定义为m，则%m
 *
 *  例如：log.info("模板：{key1}、{key2}...", val1, val2)
 * Author:   刘春
 * Date:     2022/11/18
 ***********************************************************/
public class LogbackDesensitizeConverter extends MessageConverter {
    // TODO: 匹配模板中key=vale的正则表达式(已弃用)
    //Pattern DEFAULT_PATTERN = Pattern.compile("([\\u4e00-\\u9fa5]*\\w*\\s*=\\s*\\{\\})");
    // TODO: 检测模板是否符合{key}表达式
    private final String DEFAULT_PATTERN = "\\{(\\w+)\\}";

    @Override
    public String convert(ILoggingEvent event) {
        try {
            DesensitizationHandler handler = SpringUtil.getBean(DesensitizationHandler.class);
            // TODO: 格式-传数组或可变参数值：log.info("模板：{key1}、{key2}...", val1, val2);
            List<String> fields = ReUtil.findAllGroup1(DEFAULT_PATTERN, event.getMessage());
            if (ArrayUtil.isNotEmpty(event.getArgumentArray()) && CollUtil.isNotEmpty(fields)) { //ReUtil.isMatch(DEFAULT_PATTERN, event.getMessage())
                Map<String, Object> data = new HashMap<>(fields.size());
                for (int i = 0; i < fields.size(); i++) {
                    if (i < event.getArgumentArray().length) data.put(fields.get(i), event.getArgumentArray()[i]);
                    else break;
                }
                return StrUtil.format(event.getMessage(), handler.desensitized(event.getLoggerName(), SceneEnum.LOG, data));
            }
        } catch (RuntimeException e) {
            //todo: 会引发死循环，从而造成栈溢出
            //log.error("从Sping容器中加载DesensitizationRule脱敏规则异常：", e);
        }

        return super.convert(event);
    }
}
