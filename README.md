[TOC]

# 敏感信息无感脱敏

    1、注解侵入式：属于局部或精确脱敏，常用于相同字段需要不同脱敏规则的场景，其中注解如下：
        - @EmptyDesensitization：    置空（将敏感字段值取空，非等长）
        - @HashDesensitization：     哈希（将敏感字段值取Hash，非等长）
        - @RegexDesensitization：    正则（将敏感字段值通过正则表达式进行替换，非等长）
        - @ReplaceDesensitization：  替换（将敏感字段值根据所指定位置进行替换，等长）
        - @MaskDesensitization：     掩盖（将敏感字段值根据所指定位置进行*替换，等长，且属于特殊替换模式）
        
    2、配置非侵入式：属于全局，常用于与注解侵入式相反的场景，可同时用，若局部匹配不到，则会采用全局匹配，因此最好配置全局规则
    
    3、对于日志脱敏，只支持Logback、Log4j/Log4j2；且需满足如下日志输出格式：
        格式-传数组或可变参数值：log.info("模板：{key1}、{key2}...", val1, val2);
        
        日志框架有以下组合：
        1）slf4j + logback： slf4j-api.jar + logback-classic.jar + logback-core.jar
        2）slf4j + log4j： slf4j-api.jar + slf4j-log412.jar + log4j.jar
        3）slf4j + jul： slf4j-api.jar + slf4j-jdk14.jar
        4）也可以只用slf4j无日志实现：slf4j-api.jar + slf4j-nop.jar
        注log4j2配合需要导入log4j2的log4j-api.jar、log4j-core.jar和桥接包log4j-slf4j-impl.jar。
        所谓的桥接包，就是实现StaticLoggerBinder类，用来连接slf4j和日志框架。因为log4j和log4j2刚开始没有StaticLoggerBinder这个类，
        为了不改变程序结构，只能重新写一个新的jar来实现StaticLoggerBinder。而logback出现slf4j之后，于是在logback本身的jar中实现了StaticLoggerBinder，所以就不需要桥接包
    
    4、对于网页脱敏，支持@ResponseBody修饰或JSON响应    
    
## 注解侵入式案列

    1、电话号码177*****144：@MaskDesensitization(posns = {@MaskDesensitization.Posn(i = 3), @MaskDesensitization.Posn(i = 8, hide = true)})
    2、电子邮箱133***@qq.com：@RegexDesensitization(pattern = "(\\w{3})\\w+(@qq.com)", rv = "$1***$2")
    3、身份证：@HashDesensitization(algorithm = HashDesensitization.AlgorithmEnum.MD5, salt = "ws@4q#")
    4、地址：@ReplaceDesensitization(posns = {@ReplaceDesensitization.Posn(i = 3), @ReplaceDesensitization.Posn(i = 8, rv = "#?12$%34")})

## 配置非侵入式案列

    ouo:
      desensitization:
        #脱敏规则列表
        rules:
          #字段名
          - field: name
            #脱敏模式：置空empty、哈希hash、正则regex、替换replace、掩盖mask
            mode: mask
            #脱敏场景：日志log、网页web、全部all，默认all
            scene: log
            #位置，默认显示
            posns[0]:
              i: 3
            posns[1]:
              i: 8
              hide: true
          - field: phone
            mode: replace
            scene: all
            posns:
              - i: 3
              - i: 8
                #是否固定值，默认true，false为随机值
                fixed: true
                #替换值
                rv: "#"
            #剩余位置    
            surplus:
              fixed: false
          - field: id_card
            mode: hash
            scene: web
            #算法
            algorithm: sm3
            #盐
            salt: grvyw$2
          - field: addr
            mode: empty
          - field: ip
            mode: tel
            #正则表达式
            pattern: (\\d{3})\\d{4}(\\d{4})
            rv: $1####$2
