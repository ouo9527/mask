package com.ouo.mask;

import cn.hutool.core.bean.BeanPath;
import cn.hutool.core.comparator.CompareUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.lang.Dict;
import cn.hutool.core.util.ReUtil;
import com.alibaba.fastjson2.JSON;
import com.ouo.mask.annotation.*;
import com.ouo.mask.config.DesensitizationAutoConfiguration;
import com.ouo.mask.config.DesensitizationProperties;
import com.ouo.mask.config.SpringDesensitizationPlaceholderConfigurer;
import com.ouo.mask.enums.SceneEnum;
import com.ouo.mask.handler.DesensitizationHandler;
import com.ouo.mask.rule.DesensitizationRule;
import com.ouo.mask.rule.EmptyDesensitizationRule;
import com.ouo.mask.rule.ReplDesensitizationRule;
import com.ouo.mask.util.SpringUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.beans.factory.config.YamlMapFactoryBean;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.*;

/*import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;*/

@Slf4j
//todo：Junit4需要@RunWith(SpringRunner.class)+@SpringBootTest配置合；而Junit5不需要@RunWith
//@RunWith(SpringRunner.class)
@SpringBootTest(classes = DesensitizationAutoConfiguration.class/*, properties = {"classpath*:application.yml"}*/)
public class UnitTest {

    @Autowired
    private DesensitizationHandler handler;

    @Test
    public void properties() throws IOException {
        //todo: 仅有单层的Map
        Properties properties = new Properties();
        properties.load(new ClassPathResource("application.properties").getInputStream());

        //todo：具体层级的Map
        Dict dict = Dict.create();
        properties.entrySet().stream()
                .sorted((e1, e2) ->
                        CompareUtil.compare(Convert.toStr(e1.getKey()).length(), Convert.toStr(e2.getKey()).length()))
                .forEach(entry -> {
                    BeanPath.create(entry.getKey().toString()).set(dict, entry.getValue());
                });
        System.out.println(JSON.toJSONString(dict));
        DesensitizationProperties dp = new DesensitizationProperties();
        dp.setRules(properties);
        System.out.println(JSON.toJSONString(dp));
    }

    @Test
    public void yml() throws IOException {
        /*Yaml yaml = new Yaml();
        Object obj = yaml.load(ClassUtils.getDefaultClassLoader().getResourceAsStream("application.properties"));*/
        //todo：解决占位符
        /*PropertySourcesPlaceholderConfigurer configurer
                = new PropertySourcesPlaceholderConfigurer();
        YamlPropertiesFactoryBean yamlProFb
                = new YamlPropertiesFactoryBean();
        yamlProFb.setResources(new ClassPathResource("application.properties"));
        configurer.setProperties(yamlProFb.getObject());*/
        YamlMapFactoryBean yamlMapFb = new YamlMapFactoryBean();
        yamlMapFb.setResources(new ClassPathResource("application.yml"));
        PropertiesFactoryBean factoryBean = new PropertiesFactoryBean();
        factoryBean.setLocations(new ClassPathResource("application.properties"));
        factoryBean.setSingleton(false);

        //springboot2.0以下，@ConfigurationProperties映射对象原理由PropertiesConfigurationFactory；2.0后由ConfigurationPropertiesBindingPostProcessor
        MapConfigurationPropertySource source = new MapConfigurationPropertySource();
        source.putAll(factoryBean.getObject());
        Binder binder = new Binder(source);
        Map<?, ?> map = binder.bind("ouo.desensitization.rules", Map.class).get();

        //properties->yml
        /*DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        options.setIndent(2);

        System.out.println(new PropertiesToJsonConverter().convertToJson(factoryBean.getObject()));
        Yaml yaml = new Yaml(options);
        String str = yaml.dump(factoryBean.getObject());
        System.out.println(str);*/
    }

    @Test
    public void hutool() {
        String DEFAULT_PATTERN = "\\{(\\w+)\\}";
        // TODO: 格式-传数组或可变参数值：log.info("模板：{key1}、{key2}...", val1, val2);
        List<String> fields = ReUtil.findAllGroup1(DEFAULT_PATTERN, "模板：{key1}、{key2}...");
        System.out.println(JSON.toJSONString(fields));
        /*Gson gson = new Gson();
        System.out.println(gson.toJson(fields));*/
        Map<String, Object> rule = new HashMap<>();
        rule.put("field", "12");
        rule.put("scene", "log");
        System.out.println(JSON.to(EmptyDesensitizationRule.class, rule));
    }

    @Test
    public void loadRule() {
        SpringDesensitizationPlaceholderConfigurer loader = SpringUtil.getBean(SpringDesensitizationPlaceholderConfigurer.class);
        //System.out.println("所加载到脱敏规则：" + JSON.toJSONString());
    }

    @Test
    public void desensitized() {
        List<DesensitizationRule> rules = new ArrayList<>();
        ReplDesensitizationRule rule = new ReplDesensitizationRule();
        //rule.setScene(SceneEnum.WEB);
        rule.setField("phone");
        List<ReplDesensitizationRule.Posn> posns = new ArrayList<>();
        ReplDesensitizationRule.Posn posn3 = new ReplDesensitizationRule.Posn();
        posn3.setI(3);
        posns.add(posn3);
        ReplDesensitizationRule.Posn posn8 = new ReplDesensitizationRule.Posn();
        posn8.setI(8);
        posn8.setRv("*");
        //posn8.setFixed(false);
        posns.add(posn8);
        rule.setPosns(posns);
        rules.add(rule);

        Map<String, Object> map = new HashMap<>();
        map.put("phone", "17722855144");
        map.put("name", "[1,2]");

        System.out.println(
                handler.desensitized(SceneEnum.ALL, map));

        User user = new User();
        user.setName("张王四");
        user.setExtra("{\"phone\":17722855144}");
        user.setTel("17722855144");
        user.setPhone(17722855144L);
        User.Attach attach = new User.Attach();
        attach.setHobbies(new String[]{"打球"});
        attach.setEmail("128756@qq.com");
        attach.setCard("532128199510286631");
        user.setAttach(attach);

        System.out.println(JSON.toJSONString(handler.desensitized(SceneEnum.ALL, user)));
    }


    @Setter
    @Getter
    static class User {
        @Mask(posns = {@Mask.Posn(i = 1)}, surplusHide = true)
        String name;
        @Repl(posns = {@Repl.Posn(i = 3), @Repl.Posn(i = 8, rv = "#")})
        String extra;
        @Repl(posns = {@Repl.Posn(i = 3), @Repl.Posn(i = 8, rv = "#?12$%34")})
        String tel;
        @Mask(posns = {@Mask.Posn(i = 3), @Mask.Posn(i = 8, hide = true)})
        Long phone;

        @Empty
        Attach attach;

        @Setter
        @Getter
        static class Attach {
            @Empty
            String[] hobbies;
            @Regex(pattern = "(\\w{3})\\w+(@qq.com)", rv = "$1***$2")
            String email;
            @Hash(algorithm = Hash.AlgorithmEnum.MD5, salt = "ws@4q#")
            String card;
        }
    }
}
