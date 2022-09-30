package org.apache.skywalking.apm.plugin.cat;

import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.StaticMethodsAroundInterceptor;
import org.apache.skywalking.apm.util.StringUtil;
import org.slf4j.Logger;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class LogslfjInterceptor implements StaticMethodsAroundInterceptor {
    @Override
    public void beforeMethod(Class clazz, Method method, Object[] allArguments, Class<?>[] parameterTypes, MethodInterceptResult result) {

    }

    String ignoreClass="";
    String[] ignoreNames=null;
    private String[] getName(){
        if(ignoreClass.equalsIgnoreCase(System.getProperty("cat.log.igonre",ignoreClass))){
            ignoreClass=System.getProperty("cat.log.igonre",ignoreClass);
            ignoreNames=ignoreClass.split(",");
        }
        return ignoreNames;
    }
    private boolean ignoreLog(String name){
        if(name.startsWith("org.")){
            return true;
        }
        if(name.startsWith("com.dianping.cat")){
            return true;
        }
        String[] names=getName();
        if(names==null||names.length==0){
            return false;
        }
        for (String temp:names){
            if(StringUtil.isNotEmpty(temp) && name.startsWith(temp)){
                return true;
            }
        }
        return false;
    }
    @Override
    public Object afterMethod(Class clazz, Method method, Object[] allArguments, Class<?>[] parameterTypes, Object ret) {
        try {
            String name=((Logger) ret).getName();
            if (!ignoreLog(name)) {
                InvocationHandler retHandler = new LoggerWrapper<Logger>((Logger) ret);
                Logger loggerProxy = (Logger) Proxy.newProxyInstance(ret.getClass().getClassLoader(), new Class<?>[]{Logger.class}, retHandler);
                return loggerProxy;
            }
        } catch (Throwable ex) {
            ex.printStackTrace();
            return ret;
        }
        return ret;
    }

    @Override
    public void handleMethodException(Class clazz, Method method, Object[] allArguments, Class<?>[] parameterTypes, Throwable t) {

    }
}
