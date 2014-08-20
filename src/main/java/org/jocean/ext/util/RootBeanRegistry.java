package org.jocean.ext.util;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class RootBeanRegistry {
    private ConcurrentMap<String, Object> registry = new ConcurrentHashMap<>();

    public Object getBean(String beanName) {
        return registry.get(beanName);
    }

    public Object setBean(String beanName, Object impl) {
        return registry.put(beanName,impl);
    }
}
