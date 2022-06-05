package org.apache.skywalking.apm.agent.core.cat;

import com.dianping.cat.Cat;
import java.util.HashMap;
import java.util.Map;

public  class CatContext implements Cat.Context {

    private Map<String, String> properties = new HashMap<>(16);
    @Override
    public void addProperty(String key, String value) {
        properties.put(key,value);
    }

    @Override
    public String getProperty(String key) {
        return properties.get(key);
    }
}