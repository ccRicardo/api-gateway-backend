package org.wyh.gateway.client.core;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.wyh.gateway.client.support.ApiProperties;

import javax.servlet.Servlet;

/**
 * @BelongsProject: my-api-gateway
 * @BelongsPackage: org.wyh.gateway.client.core
 * @Author: wyh
 * @Date: 2024-01-30 10:03
 * @Description: 服务接入模块（客户端）的自动配置类，
                 主要用途是将配置文件中的相应属性映射到配置类的bean实例上，并根据配置信息创建相应的注册管理器bean实例。
                 注意：在实际使用中，客户端模块会被作为外部jar包引入到spring boot应用中。
                 该类启用了spring boot的自动装配机制（在文件resources/META-INF/spring.factories中开启）
                 SpringBoot应用的@SpringBootApplication会开启自动配置
                 然后扫描外部引用jar包中的META-INF/spring.factories文件，
                 将其中配置的类注册到spring ioc中，然后注册配置类的bean实例，最后调用@Bean方法，实例化注册管理器的bean对象
                 本类中使用了条件注解，进而能够根据应用使用的协议，来实例化相应的注册管理器bean
                 当发生相应事件时，注册管理器bean的onApplicationEvent会被调用，从而完成服务的自动注册

 */
/*
 * @Configuration用于标识配置类。配置类中的@Bean方法可以将返回对象注册为一个bean实例。
 * @EnableConfigurationProperties的作用是使指定的（@ConfigurationProperties注解的）配置类生效，即：
 * 注册指定配置类的bean实例，并将spring boot配置文件中带有指定前缀的属性值绑定到该bean实例的相应字段上。
 * @ConditionalOnProperty的作用是控制配置类是否生效，或者控制某个bean是否被创建
 * 例如，下面这句代码的意思就是：只有当配置文件中存在名为api.registerAddress的属性,并且不为null时，该配置类才会生效
 */
@Configuration
@EnableConfigurationProperties(ApiProperties.class)
@ConditionalOnProperty(prefix = ApiProperties.API_PREFIX, name = {"registerAddress"})
public class ApiClientAutoConfiguration {
    //服务配置类实例。@EnableConfigurationProperties注解已经将该类实例注册到了spring ioc中。
    @Autowired
    private ApiProperties apiProperties;
    /*
     * @ConditionalOnClass和@ConditionalOnMissingBean都是条件注解
     * @ConditionalOnClass的作用是根据指定的类是否（在类路径中）存在，来决定是否加载该bean
     * @ConditionalOnMissingBean的作用是，当spring ioc中不存在指定类型的bean时，该类的bean才会注册
     * 具体来说，该注解一般用于确保指定类型在spring ioc中只有一个bean实例。
     */

    /**
     * @date: 2024-01-30 14:15
     * @description: 注册HttpClientRegisterManager的bean实例
     * @return: org.wyh.gateway.client.core.HttpClientRegisterManager
     */
    @Bean
    //该注解是在判断服务是否使用http协议
    @ConditionalOnClass({Servlet.class, DispatcherServlet.class, WebMvcConfigurer.class})
    @ConditionalOnMissingBean(HttpClientRegisterManager.class)
    public HttpClientRegisterManager httpClientRegisterManager(){
        return new HttpClientRegisterManager(apiProperties);
    }
    /**
     * @date: 2024-01-30 14:18
     * @description: 注册DubboClientRegisterManager的bean实例
     * @return: org.wyh.gateway.client.core.DubboClientRegisterManager
     */
}
