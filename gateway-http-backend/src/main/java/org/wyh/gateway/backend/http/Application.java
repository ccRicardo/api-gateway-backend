package org.wyh.gateway.backend.http;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @BelongsProject: my-api-gateway
 * @BelongsPackage: org.wyh.backend.http.server
 * @Author: wyh
 * @Date: 2024-01-19 14:58
 * @Description: http后台服务,用于网关测试
                 该应用引入了网关系统的客户端模块依赖。
                 SpringBootApplication注解开启了自动装配，
                 因此会扫描org.wyh.gateway.client下的META-INF/spring.factories文件
                 将ApiClientAutoConfiguration注册到spring ioc中，
                 并调用httpClientRegisterManager方法，注册HttpClientRegisterManager的bean
                 当此应用启动时，HttpClientRegisterManager的onApplicationEvent会被调用，
                 从而完成该服务的自动注册
 */
@SpringBootApplication(scanBasePackages="org.wyh.gateway.backend.http")
public class Application {
    /**
     * @date: 2024-01-19 15:00
     * @description: springboot应用启动方法
     * @Param args:
     * @return: void
     */
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
