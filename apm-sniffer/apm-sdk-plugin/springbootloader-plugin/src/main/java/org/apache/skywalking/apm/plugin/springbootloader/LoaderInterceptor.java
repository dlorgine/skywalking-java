/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 */

package org.apache.skywalking.apm.plugin.springbootloader;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.skywalking.apm.agent.core.boot.AgentPackagePath;
import org.apache.skywalking.apm.agent.core.conf.Config;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;

public class LoaderInterceptor implements InstanceMethodsAroundInterceptor {

    @Override
    public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes,
        MethodInterceptResult result) throws Throwable {
        //Thread.sleep(10_000L);
        if(allArguments[0] instanceof URL[]){
            URL[] urls=(URL[])allArguments[0]  ;
            List<URL> newLst= new ArrayList<URL>(Arrays.asList(urls));
            String[] arr = Config.Agent.CLASS_LOADER_NAME.split(",");
            File file = new File(AgentPackagePath.getPath(), "cat");
            boolean flag=false;
            if (file.exists() && file.isDirectory()) {
                String[] jarFileNames = file.list((dir, name) -> name.endsWith(".jar"));
                for (String temp : jarFileNames) {
                    File file1 = new File(file.getPath(), temp);
                    newLst.add(new URL("jar:file:"+file1.getPath()+"!/"));
                    System.out.println("add cat1 jar " + temp);
                    flag=true;
                }
            }
            if(flag){
                URL[] urls1=new URL[newLst.size()];
                allArguments[0]=newLst.toArray(urls1);
            }
        }
    }

    @Override
    public Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes,
        Object ret) throws Throwable {
        return ret;
    }

    @Override
    public void handleMethodException(EnhancedInstance objInst, Method method, Object[] allArguments,
        Class<?>[] argumentsTypes, Throwable t) {

    }

}
