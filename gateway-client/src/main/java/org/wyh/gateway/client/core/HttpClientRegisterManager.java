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
 * @Description: http服务的注册管理器，负责http服务（准确来说是spring mvc服务）的自动化注册。
                 （在spring mvc中，可以认为一个Controller类对应一个服务）
                 该类主要是实现了onApplicationEvent方法，确保在spring mvc应用启动时，
                 完成服务定义和实例的构建，然后调用父类的doRegister方法完成服务注册。
                 注意：在实际使用中，客户端模块会被作为外部jar包引入到spring boot应用中。
                 通常来说，http服务会采用Spring MVC的web框架，因此这里默认服务采用了Spring MVC框架。
                 ApplicationContext就是应用的Spring IOC容器
                 ApplicationContextAware实现类的setApplicationContext方法会被框架自动调用，传入该应用的Spring IOC容器
                 ApplicationListener实现类的onApplicationEvent方法会在事件发生时被spring框架自动调用。
 */
@Slf4j
public class HttpClientRegisterManager extends AbstractClientRegisterManager
        implements ApplicationContextAware, ApplicationListener<ApplicationEvent> {
    //该应用对应的Spring IOC容器（Spring上下文）
    private ApplicationContext applicationContext;
    //ServerProperties类的bean实例由Spring框架自动注册，它可以获取服务器的配置参数信息
    @Autowired
    private ServerProperties serverProperties;
    //保存已经注册过的服务的bean。理论上来说，一个应用可以对应多个服务类。但实际上为了解耦合，一个应用通常只包含一个服务。
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
        //该方法由spring框架自动调用，用于给该类传递ioc容器对象
        this.applicationContext = applicationContext;
    }
    @Override
    public void onApplicationEvent(ApplicationEvent applicationEvent) {
        /*
         * 应用启动时，完成http服务的自动注册。
         * （应用启动时，spring会发布ApplicationStartedEvent事件，然后调用onApplicationEvent进行处理）
         */
        if(applicationEvent instanceof ApplicationStartedEvent){
            try {
                //将具体的注册逻辑委托给doRegister方法
                register();
            } catch (Exception e) {
                log.error("http服务注册异常", e);
                throw new RuntimeException(e);
            }
            log.info("http服务启动");
        }
    }
    /**
     * @date: 2024-01-29 10:56
     * @description: 实现具体的自动注册流程
     * @return: void
     */
    private void register(){
        /*
         * 这是一个固定写法，不必细究，只需要知道下面一系列操作就是在获取应用的所有handler方法。
         * BeanFactoryUtils.beansOfTypeIncludingAncestors方法的作用是获取指定类型及其子类型的所有bean对象
         * 四个参数分别是：bean factory；要获取的bean的类型；是否包含非单例的bean；是否允许预初始化
         * RequestMappingHandlerMapping会扫描所有Controller类中的所有RequestMapping方法（即handler方法），并保存对应的信息
         * 也就是说，RequestMappingHandlerMapping的bean中保存了该应用所有Controller类的所有handler方法的信息。
         */
        Map<String, RequestMappingHandlerMapping> allRequestMappings = BeanFactoryUtils.beansOfTypeIncludingAncestors(applicationContext,
                RequestMappingHandlerMapping.class, true, false);
        //通常来说，一个Spring MVC应用中只有一个RequestMappingHandlerMapping类型的bean
        for(RequestMappingHandlerMapping handlerMapping : allRequestMappings.values()){
            /*
             * RequestMappingHandlerMapping.getHandlerMethods方法会返回一个map，其中保存了所有handler方法的相关信息
             * RequestMappingInfo封装了@RequestMapping注解中的信息，例如url，请求方法等。
             * HandlerMethod封装了（处理http请求的）handler方法及其所属Controller类的相关信息
             */
            Map<RequestMappingInfo, HandlerMethod> handlerMethods = handlerMapping.getHandlerMethods();
            for (Map.Entry<RequestMappingInfo, HandlerMethod> method : handlerMethods.entrySet()) {
                HandlerMethod handlerMethod = method.getValue();
                //以下两句代码的作用就是获取handler方法所属Controller类的bean对象
                //先获取handler方法所属Controller类的class对象
                Class<?> clazz = handlerMethod.getBeanType();
                //再根据class对象获取对应类型的bean实例
                Object bean = applicationContext.getBean(clazz);
                //如果该服务/Controller类（一个Controller类对应一个服务）已经注册过，则跳过
                if(set.contains(bean)){
                    continue;
                }
                //调用注解扫描器，扫描服务类上的@ApiService和@ApiInvoker注解，并返回相应的服务定义
                ServiceDefinition serviceDefinition = ApiAnnotationScanner.getInstance().scanner(bean);
                if(serviceDefinition == null){
                    continue;
                }
                //根据服务的配置类实例，设置服务定义的部署环境
                serviceDefinition.setEnvType(super.getApiProperties().getEnv());
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
                // TODO: 2024-05-22 这里将所有的服务定义都设为默认值，是否不合理。是否可以在注解中添加权重信息。
                // TODO: 2024-05-22 warmUpTime属性未设置
                serviceInstance.setWeight(GatewayConst.DEFAULT_WEIGHT);
                //根据api的配置信息判断是否为灰度服务，然后设置服务实例的gray属性
                if(getApiProperties().isGray()){
                    serviceInstance.setGray(true);
                }
                //调用父类方法，真正将该服务注册到注册中心
                super.doRegister(serviceDefinition, serviceInstance);
                //将已经注册过的服务bean加入set中，防止重复注册
                set.add(bean);
            }
        }
    }
}
