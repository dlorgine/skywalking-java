package org.apache.skywalking.apm.plugin.cat;

import com.dianping.cat.Cat;
import org.slf4j.MDC;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

public class LoggerWrapper<T> implements InvocationHandler {
    //被代理类的对象
    private T target;

    public LoggerWrapper(T target) {
        this.target = target;
    }

    public Object invoke(Object proxy, Method method, Object[] args)
            throws Throwable {
        try {
            //代理过程中插入其他操作
            if (method.getName().indexOf("error") != -1||method.getName().indexOf("info")!=-1||method.getName().indexOf("warn")!=-1){
                MDC.put("catTraceId", Cat.getCurrentMessageId());
                if (method.getName().indexOf("error") != -1) {
                    if (args.length > 1) {
                        if (args[0] instanceof String && args[1] instanceof Throwable) {
                            Cat.logError(args[0].toString(), (Throwable) args[1]);
                        }
                    } else if (args.length == 1 && args[0] instanceof Throwable) {
                        Cat.logError((Throwable) args[0]);
                    }
                }
            }
        }catch (Throwable e){
            e.printStackTrace();
        }

        Object result = method.invoke(target, args);
        return result;
    }

}
