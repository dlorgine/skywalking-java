package org.apache.skywalking.apm.plugin.cat;

import static org.apache.skywalking.apm.plugin.cat.CatConfig.keyCatEnabled;
import static org.apache.skywalking.apm.plugin.cat.CatConfig.keyHttpPort;
import static org.apache.skywalking.apm.plugin.cat.CatConfig.keyIps;
import static org.apache.skywalking.apm.plugin.cat.CatConfig.keyTcpPort;

import com.dianping.cat.Cat;
import java.io.File;
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
        ConfigurableEnvironment environment = (ConfigurableEnvironment) ret;
        String domainName = environment.getProperty("cat.app.name", environment.getProperty("spring.application.name"));
        System.setProperty("spring.application.name", domainName);
        String catIps = environment.getProperty(keyIps);
        String tcpPort = environment.getProperty(keyTcpPort, "2280");
        String httpPort = environment.getProperty(keyHttpPort, "8080");

        if ("true".equalsIgnoreCase(System.getProperty(CatConfig.keyCatEnabled, "true"))
                && StringUtil.isNotEmpty(catIps)) {
            System.out.println("Trying to initialize CAT with " + keyCatEnabled);
            initialize(domainName, tcpPort, httpPort, catIps);
        } else {
            System.out.println("CAT is DISABLED explicitly. use ${cat.enabled} to turn on.");
        }

        return ret;
    }


    private void initialize(String domain, String tcpPort, String httpPort, String catIps) {

        if (StringUtil.isNotEmpty(catIps)) {
            // bring CAT up
            String userHome = System.getProperty("user.home");
            File catHome = null;
            try {
                catHome = new File(userHome, ".cat");
                catHome.mkdirs();
                System.setProperty("CAT_HOME", catHome.getAbsolutePath());
            } catch (Throwable e) {
                try {
                    catHome = new File("/tmp/.cat");
                    catHome.mkdirs();
                    System.setProperty("CAT_HOME", catHome.getAbsolutePath());
                } catch (Throwable ee) {}
            }


            Cat.getBootstrap()
                    .initializeByDomain(domain,
                            Integer.parseInt(tcpPort),
                            Integer.parseInt(httpPort),
                            catIps.trim().split(",")
                    );
        }
    }

    @Override
    public void handleMethodException(
            EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, Throwable t
    ) {

    }
}
