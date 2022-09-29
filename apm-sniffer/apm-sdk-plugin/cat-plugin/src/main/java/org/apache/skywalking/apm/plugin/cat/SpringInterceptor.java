package org.apache.skywalking.apm.plugin.cat;

import static org.apache.skywalking.apm.plugin.cat.CatConfig.keyCatEnabled;
import static org.apache.skywalking.apm.plugin.cat.CatConfig.keyHttpPort;
import static org.apache.skywalking.apm.plugin.cat.CatConfig.keyIps;
import static org.apache.skywalking.apm.plugin.cat.CatConfig.keyTcpPort;

import com.dianping.cat.Cat;
import java.io.File;
import java.lang.reflect.Method;

import com.dianping.cat.configuration.client.entity.ClientConfig;
import com.dianping.cat.configuration.client.entity.Server;
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
        String domainName = environment.getProperty("cat.app.name", environment.getProperty("spring.application.name",environment.getProperty("application.name")));
        System.setProperty("spring.application.name", domainName);
        String catIps = environment.getProperty(keyIps);
        String tcpPort = environment.getProperty(keyTcpPort, "2280");
        String httpPort = environment.getProperty(keyHttpPort, "8080");

        if ("true".equalsIgnoreCase(System.getProperty(CatConfig.keyCatEnabled, "true"))
                && StringUtil.isNotEmpty(catIps)) {
            System.out.println("Trying to initialize CAT with " + keyCatEnabled);
            initialize(domainName, tcpPort, httpPort, catIps);
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
            String template="<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                    "<config mode=\"client\">\n" +
                    "    <servers>\n" +
                    "        serverLst\n" +
                    "    </servers>\n" +
                    "</config>";
            String serverTemplte="        <server ip=\"%s\" port=\"%s\" http-port=\"%s\"/>";
            StringBuilder rs=new StringBuilder();
            for(String p: catIps.trim().split(",")){
                rs.append(String.format(serverTemplte,p,tcpPort,httpPort));
            }
            template=template.replace("client",domain).replace("serverLst",rs.toString());
            System.setProperty(Cat.CLIENT_CONFIG,template);
            //ClientConfig clientConfig=new ClientConfigBuilder().build(domain,Integer.parseInt(tcpPort),Integer.parseInt(httpPort),catIps.trim().split(","));


        }
    }

    @Override
    public void handleMethodException(
            EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, Throwable t
    ) {

    }

    private static class ClientConfigBuilder {
        public ClientConfig build(String domain, int tcpPort, int httpPort, String[] servers) {
            ClientConfig config = new ClientConfig();

            if (domain != null) {
                config.setDomain(domain);
            }

            for (String server : servers) {
                config.addServer(new Server(server).setPort(tcpPort).setHttpPort(httpPort));
            }

            return config;
        }
    }
}
