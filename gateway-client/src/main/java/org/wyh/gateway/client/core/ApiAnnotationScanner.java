package org.wyh.gateway.client.core;

import org.wyh.gateway.client.support.ApiInvoker;
import org.wyh.gateway.client.support.ApiProtocol;
import org.wyh.gateway.client.support.ApiService;
import org.wyh.gateway.common.config.HttpServiceInvoker;
import org.wyh.gateway.common.config.ServiceDefinition;
import org.wyh.gateway.common.config.ServiceInvoker;
import org.wyh.gateway.common.constant.BasicConst;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * @BelongsProject: my-api-gateway
 * @BelongsPackage: org.wyh.gateway.client.core
 * @Author: wyh
 * @Date: 2024-01-25 14:50
 * @Description: 注解扫描类。用于扫描后台服务类/接口上的@ApiInvoker和@ApiService注解，然后构建相应的服务定义。
                 注意：在实际使用中，客户端模块会被作为外部jar包引入到spring boot应用中。
 */
public class ApiAnnotationScanner {
    /**
     * @BelongsProject: my-api-gateway
     * @BelongsPackage: org.wyh.gateway.client.core
     * @Author: wyh
     * @Date: 2024-01-25 15:02
     * @Description: 静态内部类，用于实现单例模式
     */
    private static class SingletonHolder {
        static final ApiAnnotationScanner INSTANCE = new ApiAnnotationScanner();
    }
    /**
     * @date: 2024-01-25 15:03
     * @description: private修饰的无参构造器，用于实现单例模式
     * @return: null
     */
    private ApiAnnotationScanner() {
    }
    /**
     * @date: 2024-01-25 15:04
     * @description: 获取该类的单例对象
     * @return: org.wyh.gateway.client.core.ApiAnnotationScanner
     */
    public static ApiAnnotationScanner getInstance() {
        return SingletonHolder.INSTANCE;
    }
    /**
     * @date: 2024-01-25 15:56
     * @description: 扫描传入的服务对象，返回相应的服务定义
     * @Param bean: 服务对应的bean对象
     * @Param args:
     * @return: org.wyh.common.config.ServiceDefinition
     */
    public ServiceDefinition scanner(Object bean, Object... args){
        //获取服务bean类型的class对象
        Class<?> aClass = bean.getClass();
        //如果该类上没有@ApiService注解，则说明它不是一个需要注册的服务
        if (!aClass.isAnnotationPresent(ApiService.class)) {
            return null;
        }
        //获取注解对象，并从中获取服务的相关信息
        ApiService apiService = aClass.getAnnotation(ApiService.class);
        String serviceId = apiService.serviceId();
        ApiProtocol protocol = apiService.protocol();
        String patternPath = apiService.patternPath();
        String version = apiService.version();
        String serviceDesc = apiService.desc();

        ServiceDefinition serviceDefinition = new ServiceDefinition();
        Map<String, ServiceInvoker> invokerMap = new HashMap<>();
        //获取服务类中的所有方法
        Method[] methods = aClass.getMethods();
        if(methods != null && methods.length > 0){
            //构造服务的方法调用集合Map<String, ServiceInvoker>
            for(Method method : methods){
                //如果该方法上没有@ApiInvoker对象，则说明它不是一个向外暴露的方法
                ApiInvoker invokerAnnotation = method.getAnnotation(ApiInvoker.class);
                if(invokerAnnotation == null){
                    continue;
                }
                String path = invokerAnnotation.path();
                //根据服务采用的具体协议，构造对应的ServiceInvoker对象。
                switch (protocol) {
                    case HTTP:
                        HttpServiceInvoker httpServiceInvoker = createHttpServiceInvoker(invokerAnnotation);
                        invokerMap.put(path, httpServiceInvoker);
                        break;
                    default:
                        //目前只支持http协议的服务
                        break;
                }
            }
            //设置服务定义的相关信息
            serviceDefinition.setUniqueId(serviceId + BasicConst.COLON_SEPARATOR + version);
            serviceDefinition.setServiceId(serviceId);
            serviceDefinition.setVersion(version);
            serviceDefinition.setProtocol(protocol.getCode());
            serviceDefinition.setPatternPath(patternPath);
            serviceDefinition.setEnable(true);
            serviceDefinition.setDesc(serviceDesc);
            serviceDefinition.setInvokerMap(invokerMap);

            return serviceDefinition;
        }
        //如果服务类的方法调用为空，则该服务没有意义，返回null
        return null;
    }
    /**
     * @date: 2024-01-25 15:09
     * @description: 构建HttpServiceInvoker对象
     * @Param invokerAnnotation:
     * @return: org.wyh.common.config.HttpServiceInvoker
     */
    private HttpServiceInvoker createHttpServiceInvoker(ApiInvoker invokerAnnotation){
        HttpServiceInvoker httpServiceInvoker = new HttpServiceInvoker();
        httpServiceInvoker.setInvokerPath(invokerAnnotation.path());
        httpServiceInvoker.setTimeout(invokerAnnotation.timeout());
        httpServiceInvoker.setDesc(invokerAnnotation.desc());
        return httpServiceInvoker;
    }
}
