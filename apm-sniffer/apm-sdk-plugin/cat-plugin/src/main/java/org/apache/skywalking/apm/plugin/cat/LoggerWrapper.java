package org.apache.skywalking.apm.plugin.cat;

import com.dianping.cat.Cat;
import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.RuntimeContext;
import org.apache.skywalking.apm.util.StringUtil;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

public class LoggerWrapper<T> implements InvocationHandler {
    //被代理类的对象
    private T target;
    private final static String IS_LOG_ENABLED = "isLogEnabled";

    public LoggerWrapper(T target) {
        this.target = target;
    }

    public Object invoke(Object proxy, Method method, Object[] args)
            throws Throwable {
        RuntimeContext runtimeContext = ContextManager.getRuntimeContext();
        if (!SpringInterceptor.CAT_INIT || runtimeContext == null || (runtimeContext.get(IS_LOG_ENABLED) != null && "false".equalsIgnoreCase(runtimeContext.get(IS_LOG_ENABLED).toString()))) {
            return method.invoke(target, args);
        }
        String methodName = method.getName();
        try {
            //代理过程中插入其他操作
            if ("error".equalsIgnoreCase(methodName)||"warn".equalsIgnoreCase(methodName)||"info".equalsIgnoreCase(methodName)||"debug".equalsIgnoreCase(methodName)) {
                //MDC.put("catTraceId", Cat.getCurrentMessageId());
                String traceId = Cat.getCurrentMessageId();
                if (StringUtil.isNotEmpty(traceId)) {
                    if (args[0] instanceof String) {
                        args[0] = "catTraceId:" + Cat.getCurrentMessageId() + "," + args[0].toString();
                    } else if (args.length > 1 && args[1] instanceof String) {
                        args[1] = "catTraceId:" + Cat.getCurrentMessageId() + "," + args[1].toString();
                    }
                }
                if ("error".equalsIgnoreCase(methodName)) {
                    if (args.length > 1) {
                        if (args[0] instanceof String && args[1] instanceof Throwable) {
                            Cat.logError(args[0].toString(), (Throwable) args[1]);
                        }
                    } else if (args.length == 1 && args[0] instanceof Throwable) {
                        Cat.logError((Throwable) args[0]);
                    }
                }
            }
        } catch (Throwable e) {
            //e.printStackTrace();
        }
        Object result = method.invoke(target, args);
        if (methodName.startsWith("is") && methodName.endsWith("Enabled")) {
            runtimeContext.put(IS_LOG_ENABLED, result);
        }
        return result;
    }

}
