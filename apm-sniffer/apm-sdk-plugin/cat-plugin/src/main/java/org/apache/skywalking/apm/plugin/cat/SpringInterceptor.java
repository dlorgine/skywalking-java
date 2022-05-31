package org.apache.skywalking.apm.plugin.cat;

import java.lang.reflect.Method;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import org.apache.skywalking.apm.util.StringUtil;
import org.springframework.core.env.ConfigurableEnvironment;

public class SpringInterceptor implements InstanceMethodsAroundInterceptor {
    @Override
    public void beforeMethod(
            EnhancedInstance objInst,
            Method method,
            Object[] allArguments,
            Class<?>[] argumentsTypes,
            MethodInterceptResult result
    ) throws Throwable {

    }

    @Override
    public Object afterMethod(
            EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, Object ret
    ) throws Throwable {
        ConfigurableEnvironment environment=(ConfigurableEnvironment)ret;
        String domainName=environment.getProperty("app.name", environment.getProperty("spring.application.name"));
        String catIps=environment.getProperty("cat.server.ips");
        if(StringUtil.isNotEmpty(domainName)){
          System.setProperty("spring.application.name", domainName);
        }
        if(StringUtil.isNotEmpty(catIps)){
          System.setProperty("cat.server.ips",catIps);
        }
        System.setProperty("cat.port",environment.getProperty("cat.port","2280"));
        System.setProperty("cat.http.port",environment.getProperty("cat.http.port","8080"));
        return ret;
    }

    @Override
    public void handleMethodException(
            EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, Throwable t
    ) {

    }
}
