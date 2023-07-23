package com.ouo.mask.log;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import com.ouo.mask.core.DefaultDesensitizationHandler;
import com.ouo.mask.core.DesensitizationRuleLoader;
import com.ouo.mask.core.annotation.SceneEnum;
import com.ouo.mask.core.property.DesensitizationRule;
import com.ouo.mask.spring.SpringUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.impl.LogEventFactory;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.SimpleMessage;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/***********************************************************
 * TODO:      基于log4j RewritePolicy实现自定义日志格式（
 *  msg模板（{}）替换之前，即只是此时日志总进口，有利于脱敏）
 *  注：1）在类路径（如：resources目录）下创建log4j2.component.properties文件，
 *  并在改文件中加入Log4jLogEventFactory=xx.xx.Log4jDesensitizedLogEventFactory,会被log4j框架自动加载
 *  2）log4j.xml文件【无需配置】
 *  注意Marker的使用：
 *  private static final Marker SENSITIVE_DATA_MARKER = MarkerFactory.getMarker("SENSITIVE_DATA_MARKER");
 *  logger.warn(SENSITIVE_DATA_MARKER, cardNo);
 * Author:   刘春
 * Date:     2022/12/3
 ***********************************************************/
@Slf4j
public class Log4jDesensitizedLogEventFactory implements LogEventFactory {

    // TODO: 检测模板是否符合{key}表达式
    private final String DEFAULT_PATTERN = "\\{(\\w+)\\}";

    @Override
    public LogEvent createEvent(String loggerName, Marker marker,
                                String fqcn, Level level, Message message,
                                List<Property> properties, Throwable t) {

        List<DesensitizationRule> rules = Collections.EMPTY_LIST;
        try {
            DesensitizationRuleLoader loader = SpringUtil.getBean(DesensitizationRuleLoader.class);
            if (null != loader) rules = loader.load();
        } catch (RuntimeException e) {
            log.error("从Sping容器中加载DesensitizationRule脱敏规则异常：", e);
        }
        Message mesg = message;
        // TODO: 格式-传数组或可变参数值：log.info("模板：{key1}、{key2}...", val1, val2);
        List<String> fields = ReUtil.findAllGroup1(DEFAULT_PATTERN, message.getFormat());
        if (CollUtil.isNotEmpty(rules) && ArrayUtil.isNotEmpty(message.getParameters()) && CollUtil.isNotEmpty(fields)) { //ReUtil.isMatch(DEFAULT_PATTERN, message.getFormat())
            Map<String, Object> data = new HashMap<>(fields.size());
            for (int i = 0; i < fields.size(); i++) {
                if (i < message.getParameters().length) data.put(fields.get(i), message.getParameters()[i]);
                else break;
            }
            mesg = new SimpleMessage(StrUtil.format(message.getFormat(),
                    new DefaultDesensitizationHandler(SceneEnum.LOG, rules).desensitized(data)));
        }
        // message.getFormattedMessage() 为null
        return new Log4jLogEvent(loggerName, marker, fqcn, level, mesg, properties, t);
    }
}
