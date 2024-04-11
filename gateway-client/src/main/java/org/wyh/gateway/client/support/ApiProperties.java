package org.wyh.gateway.client.support;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @BelongsProject: my-api-gateway
 * @BelongsPackage: org.wyh.gateway.client.support
 * @Author: wyh
 * @Date: 2024-01-26 14:28
 * @Description: api服务的配置类，用于封装注册中心地址，服务的部署环境信息，以及是否为灰度服务。
                 注意：在实际使用中，客户端模块会被作为外部jar包引入到spring boot应用中。
 */
/*
 * @ConfigurationProperties的作用是将配置文件中的属性值绑定/映射到该Java类的相应字段上。
 * prefix用于指定配置文件中需要绑定/映射属性的前缀名
 */

@ConfigurationProperties(prefix="api")
@Data
public class ApiProperties {
    //注册中心地址
    private String registerAddress;
    //服务部署环境
    private String env = "dev";
    //标识是否为灰度服务
    private boolean gray;
}
