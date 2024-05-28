package org.wyh.gateway.client.support;

import java.lang.annotation.*;

/**
 * @BelongsProject: my-api-gateway
 * @BelongsPackage: org.wyh.gateway.client.core
 * @Author: wyh
 * @Date: 2024-01-25 10:44
 * @Description: 服务的方法调用注解。在服务的方法调用上使用，用于标识服务要提供/暴露的方法调用。
                 注意：在实际使用中，客户端模块会被作为外部jar包引入到spring boot应用中。
 */
@Target(ElementType.METHOD)         //定义注解的作用范围（此处为方法）
@Retention(RetentionPolicy.RUNTIME) //定义注解的保留策略（此处为运行时仍然保留注解信息）
@Documented
public @interface ApiInvoker {
    //方法调用的路径
    String path();
    //方法调用使用的规则id
    String ruleId();
    //方法的描述信息（自己定义的字段）
    String desc() default " ";
    //方法调用的超时时间
    int timeout() default 5000;
}
