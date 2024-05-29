package org.wyh.gateway.backend.http.controller;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.wyh.gateway.client.support.ApiInvoker;
import org.wyh.gateway.client.support.ApiProperties;
import org.wyh.gateway.client.support.ApiProtocol;
import org.wyh.gateway.client.support.ApiService;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;

/**
 * @BelongsProject: my-api-gateway
 * @BelongsPackage: org.wyh.backend.http.server.controller
 * @Author: wyh
 * @Date: 2024-02-01 10:36
 * @Description:
 */
@Slf4j
@RestController
@ApiService(serviceId = "http-service", protocol = ApiProtocol.HTTP, patternPath = "/http-service/**", desc="test")
public class TestController {
    @Autowired
    private ApiProperties apiProperties;
    /**
     * @date: 2024-03-12 9:27
     * @description: http标准测试服务，用于测试网关系统的绝大部分功能
                     注意：请求头中必须带有uniqueId，下同
     * @return: java.lang.String
     */
    @ApiInvoker(path="/http-service/test", desc="test-svc", ruleId="1")
    @GetMapping("/http-service/test")
    public String test() throws InterruptedException{
        Thread.sleep(50);
        return "this is normal test http service";
    }
    /**
     * @date: 2024-03-12 9:32
     * @description: http登录服务，用于测试网关系统的用户鉴权功能
                     注意：请求参数中必须带有phoneNumber和code
                     该服务的响应结果中会包含user-jwt cookie
     * @Param phoneNumber:
     * @Param code:
     * @Param response: 响应对象
     * @return: java.lang.String
     */
    @ApiInvoker(path="/http-service/login", desc="login-svc", ruleId="2")
    @GetMapping("/http-service/login")
    public String login(@RequestParam("phoneNumber") String phoneNumber,
                        @RequestParam("code") String code,
                        HttpServletResponse response){
        //密钥
        String SECRETKEY = "amknvqo390j0oinxbhw9u10jlg3nikbn";
        //存储jwt token的cookie的名称
        String COOKIE_NAME = "user-jwt";
        //构建jwt token
        var jwt = Jwts.builder()
                //设置用户id
                .setSubject("20240312")
                //设置该jwt的创建时间
                .setIssuedAt(new Date())
                //设置密钥和签名算法
                .signWith(SignatureAlgorithm.HS256, SECRETKEY).compact();
        //设置cookie返回jwt信息
        response.addCookie(new Cookie(COOKIE_NAME, jwt));
        return "login success: " + phoneNumber + " " + code;
    }
    /**
     * @date: 2024-03-12 9:58
     * @description: http用户服务，配合上述的http登录服务，测试网关系统的用户鉴权功能
                     注意：请求中必须带有user-jwt cookie
     * @Param userId:
     * @return: java.lang.String
     */
    @ApiInvoker(path="/http-service/user", desc="user-svc", ruleId="3")
    @GetMapping("/http-service/user")
    public String getUserInfo(@RequestHeader("userId") String userId){
        return "userId: " + userId;
    }
}
