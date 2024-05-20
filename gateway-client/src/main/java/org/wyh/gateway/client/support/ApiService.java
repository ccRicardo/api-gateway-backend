package org.wyh.gateway.client.support;

import java.lang.annotation.*;

/**
 * @BelongsProject: my-api-gateway
 * @BelongsPackage: org.wyh.gateway.client.core
 * @Author: wyh
 * @Date: 2024-01-25 10:41
 * @Description: 服务注解。在服务类/接口上使用，用于标识要注册的服务。
                 注意：在实际使用中，客户端模块会被作为外部jar包引入到spring boot应用中。
 */
@Target(ElementType.TYPE)           //定义注解的作用范围（此处为类/接口/枚举/注解）
@Retention(RetentionPolicy.RUNTIME) //定义注解的保留策略（此处为运行时仍然保留注解信息）
@Documented
public @interface ApiService {
    //服务id（名称）
    String serviceId();
    //服务版本号
    String version() default "1.0.0";
    //服务使用的协议
    ApiProtocol protocol();
    //服务的（ANT风格的）匹配规则
    String patternPath();
    //服务的描述信息（自己添加的字段）
    String desc();
}
