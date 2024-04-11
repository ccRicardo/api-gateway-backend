package org.wyh.gateway.client.support;

/**
 * @BelongsProject: my-api-gateway
 * @BelongsPackage: org.wyh.gateway.client.core
 * @Author: wyh
 * @Date: 2024-01-25 10:35
 * @Description: 后台服务使用的协议
                 注意：在实际使用中，客户端模块会被作为外部jar包引入到spring boot应用中。
 */
public enum ApiProtocol {
    //目前只支持http协议
    HTTP("http", "http协议");
    //协议代码
    private String code;
    //协议描述
    private String desc;
    /**
     * @date: 2024-01-25 10:38
     * @description: 有参构造器，由于是枚举类，所以并不对外开放
     * @Param code:
     * @Param desc:
     * @return: null
     */
    ApiProtocol(String code, String desc){
        this.code = code;
        this.desc = desc;
    }
    /**
     * @date: 2024-01-25 10:39
     * @description: 获取协议的代码信息
     * @return: java.lang.String
     */
    public String getCode(){
        return code;
    }
    /**
     * @date: 2024-01-25 10:39
     * @description: 获取协议的描述信息
     * @return: java.lang.String
     */
    public String getDesc(){
        return desc;
    }
}
