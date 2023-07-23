package com.ouo;

import cn.hutool.core.util.ReUtil;
import com.alibaba.fastjson2.JSON;
import com.ouo.mask.config.DesensitizationAutoConfiguration;
import com.ouo.mask.core.DefaultDesensitizationHandler;
import com.ouo.mask.core.annotation.*;
import com.ouo.mask.core.property.DesensitizationRule;
import com.ouo.mask.core.property.EmptyDesensitizationRule;
import com.ouo.mask.core.property.ReplaceDesensitizationRule;
import com.ouo.mask.spring.SpringDesensitizationRuleLoader;
import com.ouo.mask.spring.SpringUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.beans.factory.config.YamlMapFactoryBean;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;*/

@Slf4j
//todo：Junit4需要@RunWith(SpringRunner.class)+@SpringBootTest配置合；而Junit5不需要@RunWith
//@RunWith(SpringRunner.class)
@SpringBootTest(classes = DesensitizationAutoConfiguration.class /* properties = {"classpath*:application.properties"}*/)
public class UnitTest {

    //@Bean
    public SpringDesensitizationRuleLoader placeholderConfigurer() {
        SpringDesensitizationRuleLoader desensitizationRuleLoader = new SpringDesensitizationRuleLoader();
        desensitizationRuleLoader.setLocation(new ClassPathResource("application.properties"));
        return desensitizationRuleLoader;
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
        SpringDesensitizationRuleLoader loader = SpringUtil.getBean(SpringDesensitizationRuleLoader.class);
        System.out.println("所加载到脱敏规则：" + JSON.toJSONString(loader.load()));
    }

    @Test
    public void desensitized() {
        List<DesensitizationRule> rules = new ArrayList<>();
        ReplaceDesensitizationRule rule = new ReplaceDesensitizationRule();
        //rule.setScene(SceneEnum.WEB);
        rule.setField("phone");
        List<ReplaceDesensitizationRule.PosnProperty> posns = new ArrayList<>();
        ReplaceDesensitizationRule.PosnProperty posn3 = new ReplaceDesensitizationRule.PosnProperty();
        posn3.setI(3);
        posns.add(posn3);
        ReplaceDesensitizationRule.PosnProperty posn8 = new ReplaceDesensitizationRule.PosnProperty();
        posn8.setI(8);
        posn8.setRv("*");
        //posn8.setFixed(false);
        posns.add(posn8);
        rule.setPosns(posns);
        rules.add(rule);
        DefaultDesensitizationHandler handler = new DefaultDesensitizationHandler(null, rules);


        Map<String, Object> map = new HashMap<>();
        map.put("phone", "17722855144");
        map.put("name", "[1,2]");

        System.out.println(
                handler.desensitized(map));

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

        System.out.println(JSON.toJSONString(handler.desensitized(user)));
    }


    @Setter
    @Getter
    static class User {
        @MaskDesensitization(posns = {@MaskDesensitization.Posn(i = 1)}, surplusHide = true)
        String name;
        @ReplaceDesensitization(posns = {@ReplaceDesensitization.Posn(i = 3), @ReplaceDesensitization.Posn(i = 8, rv = "#")})
        String extra;
        @ReplaceDesensitization(posns = {@ReplaceDesensitization.Posn(i = 3), @ReplaceDesensitization.Posn(i = 8, rv = "#?12$%34")})
        String tel;
        @MaskDesensitization(posns = {@MaskDesensitization.Posn(i = 3), @MaskDesensitization.Posn(i = 8, hide = true)})
        Long phone;

        @EmptyDesensitization
        Attach attach;

        @Setter
        @Getter
        static class Attach {
            @EmptyDesensitization
            String[] hobbies;
            @RegexDesensitization(pattern = "(\\w{3})\\w+(@qq.com)", rv = "$1***$2")
            String email;
            @HashDesensitization(algorithm = HashDesensitization.AlgorithmEnum.MD5, salt = "ws@4q#")
            String card;
        }
    }
}
