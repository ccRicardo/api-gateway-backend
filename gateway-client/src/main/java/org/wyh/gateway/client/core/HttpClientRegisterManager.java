package org.wyh.gateway.client.core;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.wyh.gateway.common.config.ServiceDefinition;
import org.wyh.gateway.common.config.ServiceInstance;
import org.wyh.gateway.common.constant.BasicConst;
import org.wyh.gateway.common.constant.GatewayConst;
import org.wyh.gateway.common.utils.NetUtils;
import org.wyh.gateway.common.utils.TimeUtil;
import org.wyh.gateway.client.support.ApiProperties;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @BelongsProject: my-api-gateway
 * @BelongsPackage: org.wyh.gateway.client.core
 * @Author: wyh
 * @Date: 2024-01-29 9:48
 * @Description: http服务的注册管理器，负责http服务的自动化注册。
                 注意：在实际使用中，客户端模块会被作为外部jar包引入到spring boot应用中。
                 通常来说，http服务会采用Spring MVC的web框架，因此这里默认服务采用了Spring MVC框架。
                 ApplicationContext就是应用的Spring IOC容器
                 ApplicationContextAware实现类的setApplicationContext方法会被框架自动调用，传入该应用的Spring IOC容器
                 ApplicationListener实现类的onApplicationEvent方法会在事件发生时被spring框架自动调用。
 */
@Slf4j
public class HttpClientRegisterManager extends AbstractClientRegisterManager
        implements ApplicationContextAware, ApplicationListener<ApplicationEvent> {
    //该应用的Spring IOC容器（Spring上下文）
    private ApplicationContext applicationContext;
    //ServerProperties类的bean由Spring框架提供，它可以获取服务器的配置参数信息
    @Autowired
    private ServerProperties serverProperties;
    //记录已经注册过的服务的bean。理论上来说，一个应用可以对应多个服务类。但实际上为了解耦合，一个应用通常只包含一个服务。
    private Set<Object> set= new HashSet<>();
    /**
     * @date: 2024-01-29 10:36
     * @description: 有参构造器，需要传入服务的配置类
     * @Param apiProperties:
     * @return: null
     */
    protected HttpClientRegisterManager(ApiProperties apiProperties){
        super(apiProperties);
    }
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
    @Override
    public void onApplicationEvent(ApplicationEvent applicationEvent) {
        //Spring MVC应用启动时，完成http服务的自动注册（ApplicationStartedEvent事件会在应用启动时被发布）
        if(applicationEvent instanceof ApplicationStartedEvent){
            try {
                //将具体的注册逻辑委托给doRegisterHttp方法
                doRegisterHttp();
            } catch (Exception e) {
                log.error("【服务接入模块】http服务注册异常", e);
                throw new RuntimeException(e);
            }
            log.info("【服务接入模块】http服务api启动");
        }
    }
    /**
     * @date: 2024-01-29 10:56
     * @description: 实现具体的自动注册流程
     * @return: void
     */
    private void doRegisterHttp(){
        /*
         * BeanFactoryUtils.beansOfTypeIncludingAncestors方法的作用是获取指定类型及其子类型的所有bean对象
         * 四个参数分别是：bean factory；要获取的bean的类型；是否包含非单例的bean；是否允许预初始化
         * RequestMappingHandlerMapping会扫描所有@Controller类中的@RequestMapping方法（即handler方法），并保存对应的信息
         * 也就是说，RequestMappingHandlerMapping的bean保存了该应用所有handler方法的信息。
         */
        Map<String, RequestMappingHandlerMapping> allRequestMappings = BeanFactoryUtils.beansOfTypeIncludingAncestors(applicationContext,
                RequestMappingHandlerMapping.class, true, false);
        //通常来说，一个Spring MVC应用中只有一个RequestMappingHandlerMapping类型的bean
        for(RequestMappingHandlerMapping handlerMapping : allRequestMappings.values()){
            /*
             * RequestMappingHandlerMapping.getHandlerMethods方法会返回一个map，其中保存了所有handler方法的相关信息
             * RequestMappingInfo封装了@RequestMapping注解中的信息，例如url，请求方法等。
             * HandlerMethod封装了处理http请求的handler方法及其所在类的bean
             */
            Map<RequestMappingInfo, HandlerMethod> handlerMethods = handlerMapping.getHandlerMethods();
            for (Map.Entry<RequestMappingInfo, HandlerMethod> me : handlerMethods.entrySet()) {
                HandlerMethod handlerMethod = me.getValue();
                //以下两句代码的作用就是获取handler方法所在类的bean对象
                //先获取handler方法所在类的class对象
                Class<?> clazz = handlerMethod.getBeanType();
                //再根据class对象获取对应类型的bean
                Object bean = applicationContext.getBean(clazz);
                //如果该服务已经注册过，则跳过
                if(set.contains(bean)){
                    continue;
                }
                //调用注解扫描器，扫描服务类上的@ApiService和@ApiInvoker注解，并返回相应的服务定义
                ServiceDefinition serviceDefinition = ApiAnnotationScanner.getInstance().scanner(bean);
                if(serviceDefinition == null){
                    continue;
                }
                //根据服务配置对象，设置服务定义的部署环境
                serviceDefinition.setEnvType(getApiProperties().getEnv());
                //构造对应的服务实例
                ServiceInstance serviceInstance = new ServiceInstance();
                //由于SpringMVC应用是部署在服务器上的，所以这里获取的是服务器ip
                String localIp = NetUtils.getLocalIp();
                int port = serverProperties.getPort();
                String serviceInstanceId = localIp + BasicConst.COLON_SEPARATOR + port;
                String uniqueId = serviceDefinition.getUniqueId();
                String version = serviceDefinition.getVersion();
                serviceInstance.setServiceInstanceId(serviceInstanceId);
                serviceInstance.setUniqueId(uniqueId);
                serviceInstance.setIp(localIp);
                serviceInstance.setPort(port);
                serviceInstance.setRegisterTime(TimeUtil.currentTimeMillis());
                serviceInstance.setVersion(version);
                serviceInstance.setWeight(GatewayConst.DEFAULT_WEIGHT);
                //根据api的配置信息判断是否为灰度服务，然后设置服务实例的gray属性
                if(getApiProperties().isGray()){
                    serviceInstance.setGray(true);
                }
                //将该服务注册到注册中心
                register(serviceDefinition, serviceInstance);
                //将已经注册过的服务bean加入set中，防止重复注册
                set.add(bean);
            }
        }
    }
}
