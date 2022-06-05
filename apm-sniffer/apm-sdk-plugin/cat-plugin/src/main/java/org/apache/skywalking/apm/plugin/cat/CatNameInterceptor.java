package org.apache.skywalking.apm.plugin.cat;

import java.lang.reflect.Method;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import org.apache.skywalking.apm.util.StringUtil;
import org.springframework.core.env.ConfigurableEnvironment;

public class CatNameInterceptor implements InstanceMethodsAroundInterceptor {
    @Override
    public void beforeMethod(
            EnhancedInstance objInst,
            Method method,
            Object[] allArguments,
            Class<?>[] argumentsTypes,
            MethodInterceptResult result
    ) throws Throwable {
        String appName= System.getProperty("spring.application.name");
        if(StringUtil.isNotEmpty(appName)){
            result.defineReturnValue(appName);
        }
    }

    @Override
    public Object afterMethod(
            EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, Object ret
    ) throws Throwable {
        return ret;
    }

    @Override
    public void handleMethodException(
            EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, Throwable t
    ) {

    }
}
