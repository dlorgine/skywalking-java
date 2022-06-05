package org.apache.skywalking.apm.plugin.cat;

import com.dianping.cat.Cat;
import java.lang.reflect.Method;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import org.apache.skywalking.apm.util.StringUtil;

public class CatStatusInterceptor implements InstanceMethodsAroundInterceptor {
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
        Cat.logError((Throwable) allArguments[0]);
        return ret;
    }

    @Override
    public void handleMethodException(
            EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, Throwable t
    ) {

    }
}
