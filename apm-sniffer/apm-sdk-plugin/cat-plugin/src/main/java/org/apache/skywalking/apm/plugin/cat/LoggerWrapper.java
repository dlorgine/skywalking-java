package org.apache.skywalking.apm.plugin.cat;

import com.dianping.cat.Cat;
import org.apache.skywalking.apm.util.StringUtil;
import org.slf4j.MDC;

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
        if (!SpringInterceptor.CAT_INIT)
        {
            return method.invoke(target, args);
        }
        String methodName = method.getName();
        try {
            //代理过程中插入其他操作
            if ("error".equalsIgnoreCase(methodName) || "warn".equalsIgnoreCase(methodName) || "info".equalsIgnoreCase(methodName) || "debug".equalsIgnoreCase(methodName)) {
                //MDC.put("catTraceId", Cat.getCurrentMessageId());
                String traceId = Cat.getCurrentMessageId();
                if (StringUtil.isNotEmpty(traceId)) {
                    String logType = MDC.get("logType");
                    if (args[0] instanceof String) {
                        if (StringUtil.isNotEmpty(logType) && args[0].toString().indexOf("[" + logType + "]:") != -1) {
                            String target = "[" + logType + "]: ";
                            String json = args[0].toString().substring(args[0].toString().indexOf(target) + target.length());
                            args[0] = target + "{\"catTraceId\":\"" + Cat.getCurrentMessageId() + "\"," + json.substring(json.indexOf("{") + 1);
                        } else {
                            args[0] = "[catTraceId:" + Cat.getCurrentMessageId() + "]-" + args[0].toString();
                        }
                    } else if (args.length > 1 && args[1] instanceof String) {
                        if (StringUtil.isNotEmpty(logType) && args[1].toString().indexOf("[" + logType + "]:") != -1) {
                            String target = "[" + logType + "]: ";
                            String json = args[1].toString().substring(args[1].toString().indexOf(target) + target.length());
                            args[1] = target + "{\"catTraceId\":\"" + Cat.getCurrentMessageId() + "\"," + json.substring(json.indexOf("{") + 1);
                        } else {
                            args[1] = "[catTraceId:" + Cat.getCurrentMessageId() + "]-" + args[1].toString();
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
            }
        } catch (Throwable e) {
            //e.printStackTrace();
        }
        Object result = method.invoke(target, args);
        return result;
    }

}
