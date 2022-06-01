package com.example.demo;

import com.dianping.cat.proxy.Tracer;
import com.dianping.cat.proxy.spi.Transaction;
import java.lang.reflect.Method;
import java.net.URL;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.util.ClassUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
@RestController
public class DemoApplication {

    //-javaagent:/Users/chenyanli/Documents/airwallex/github/skywalking-java/skywalking-agent/skywalking-agent.jar
    @RequestMapping("/test")
    public String test() {
        Transaction t = Tracer.newTransaction("URL", "pageName");
		try {
			Thread.sleep(100);
			t.setStatus(Transaction.SUCCESS);
		}catch (Throwable e){
			t.setStatus(e);
		}finally {
			t.complete();
		}
        return "test";
    }

    @RequestMapping("/error1")
    public String error1() {
        for(int i=0;i<100;i++){
        Transaction t = Tracer.newTransaction("test", "t");
        try {
            RuntimeException r=new RuntimeException("ttt");
            t.setStatus(r);
            //Tracer.logError(r);
        }catch (Throwable e){
            t.setStatus(e);
        }finally {
            t.complete();
        }}
        return "test";
    }

    @RequestMapping("/error3")
    public String error2() {
        RuntimeException r=new RuntimeException("ttt");
        throw r;
    }

    RestTemplate restTemplate=new RestTemplate();
    @RequestMapping("/a2")
    public String a() {
        return restTemplate.getForObject("http://localhost:8081/b2",String.class);
    }

    @RequestMapping("/b2")
    public String b() throws InterruptedException {
        Transaction transaction=Tracer.newTransaction("test","test");
        transaction.setStatus(Transaction.SUCCESS);
        Thread.sleep(10L);
        transaction.complete();
        return "b";
    }
    private static Method getMethod(ClassLoader classLoader) {
        Method method = null;
        try {
            //Thread.sleep(10_000L);
            Class<?> class1= Class.forName("java.net.URLClassLoader",false,classLoader);
            method = class1.getDeclaredMethod("addURL", URL.class);
            //System.setProperty("illegal-access", "permit");
            method.setAccessible(true);
        } catch (Throwable e1) {
            e1.printStackTrace();
        }
        return method;
    }
    public static void main(String[] args) throws InterruptedException {
       // Thread.sleep(20_000L);
        //Method method=getMethod(Thread.currentThread().getContextClassLoader());
		SpringApplication.run(DemoApplication.class, args);
    }

    private static String getPath(String path) {
        ClassLoader cl = ClassUtils.getDefaultClassLoader();
        URL url = cl != null ? cl.getResource(path) : ClassLoader.getSystemResource(path);
        return url.getPath();
    }
}
