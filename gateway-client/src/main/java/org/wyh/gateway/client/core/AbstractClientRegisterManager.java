package org.wyh.gateway.client.core;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.wyh.gateway.common.config.ServiceDefinition;
import org.wyh.gateway.common.config.ServiceInstance;
import org.wyh.gateway.client.support.ApiProperties;
import org.wyh.gateway.register.api.RegisterCenter;

import java.util.ServiceLoader;

/**
 * @BelongsProject: my-api-gateway
 * @BelongsPackage: org.wyh.gateway.client.core
 * @Author: wyh
 * @Date: 2024-01-26 14:23
 * @Description: 注册管理抽象类，用于初始化注册中心实例，并提供doRegister方法来真正完成服务注册。
                 注意：在实际使用中，客户端模块会被作为外部jar包引入到spring boot应用中。
 */
@Slf4j
@Getter
public abstract class AbstractClientRegisterManager {
    //服务配置类对象，主要包含注册中心地址和服务部署环境信息
    private ApiProperties apiProperties;
    //注册中心实例
    private RegisterCenter registerCenter;
    /**
     * @date: 2024-01-26 14:37
     * @description: 有参构造器，用于初始化apiProperties和registerCenter属性。
     * @Param apiProperties:
     * @return: null
     */
    protected AbstractClientRegisterManager(ApiProperties apiProperties){
        this.apiProperties = apiProperties;
        /*
         * 以下这段代码的作用是通过java SPI机制来加载/构建注册中心实例
         * SPI是JDK内置的一种服务提供发现机制，可以动态获取/发现接口的实现类
         * 具体来说，服务提供者在提供了一种接口实现后，
         * 需要在resources/META-INF/services目录下创建一个以接口（全类名）命名的文件
         * 文件的内容就是该接口具体实现类的全类名
         * 之后通过java.util.ServiceLoader，就可以根据文件中的实现类全类名，来构建/加载相应的实例
         * ServiceLoader.findFirst方法返回的是第一个（种）实现类的实例。
         */
        ServiceLoader<RegisterCenter> serviceLoader = ServiceLoader.load(RegisterCenter.class);
        registerCenter = serviceLoader.findFirst().orElseThrow(()->{
            //如果没找到实现类，则执行以下lambda表达式，抛出异常
            log.error("未发现注册中心实例");
            return new RuntimeException("未发现注册中心实例");
        });
        registerCenter.init(apiProperties.getRegisterAddress(), apiProperties.getEnv());
    }
    /**
     * @date: 2024-01-26 15:10
     * @description: 调用注册中心实例的注册方法，真正完成注册
     * @Param serviceDefinition:
     * @Param serviceInstance:
     * @return: void
     */
    protected void doRegister(ServiceDefinition serviceDefinition, ServiceInstance serviceInstance){
        registerCenter.register(serviceDefinition, serviceInstance);
    }
}
