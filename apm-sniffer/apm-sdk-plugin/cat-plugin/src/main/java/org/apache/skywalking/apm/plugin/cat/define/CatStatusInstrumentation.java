package org.apache.skywalking.apm.plugin.cat.define;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesGenericArgument;
import static org.apache.skywalking.apm.agent.core.plugin.match.NameMatch.byName;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.ConstructorInterceptPoint;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.InstanceMethodsInterceptPoint;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.ClassInstanceMethodsEnhancePluginDefine;
import org.apache.skywalking.apm.agent.core.plugin.match.ClassMatch;

public class CatStatusInstrumentation extends ClassInstanceMethodsEnhancePluginDefine {

    /**
     * Enhance class.
     */
    private static final String ENHANCE_CLASS = "com.dianping.cat.message.internal.DefaultTransaction";

    /**
     * The intercept class for "invoke" method in the class "org.apache.catalina.core.StandardHostValve"
     */
    private static final String INVOKE_INTERCEPT_CLASS = "org.apache.skywalking.apm.plugin.cat.CatStatusInterceptor";

    @Override
    protected ClassMatch enhanceClass() {
        if("true".equalsIgnoreCase(System.getProperty("cat.status.error","true"))){
            return byName(ENHANCE_CLASS);
        }else{
            return null;
        }
    }

    @Override
    public ConstructorInterceptPoint[] getConstructorsInterceptPoints() {
        return null;
    }

    @Override
    public InstanceMethodsInterceptPoint[] getInstanceMethodsInterceptPoints() {
        return new InstanceMethodsInterceptPoint[] {
                new InstanceMethodsInterceptPoint() {
                    @Override
                    public ElementMatcher<MethodDescription> getMethodsMatcher() {
                        return   named("setStatus").and(takesGenericArgument(0,Throwable.class));
                    }

                    @Override
                    public String getMethodsInterceptor() {
                        return INVOKE_INTERCEPT_CLASS;
                    }

                    @Override
                    public boolean isOverrideArgs() {
                        return true;
                    }
                }
        };
    }
}
