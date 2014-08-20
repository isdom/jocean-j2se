package org.jocean.ext.util;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class SimpleRegistry {
    private ConcurrentMap<String, Object> registry = new ConcurrentHashMap<>();

    public Object getOrCreateEntry(String key, Object defaultValue) {
        Object value = registry.putIfAbsent(key, defaultValue);

        return null == value ? defaultValue : value;
    }

    public Object getEntry(String key) {
        return registry.get(key);
    }

    public Runnable createEntryRemover(final String key) {
        return new Runnable() {

            public void run() {
                registry.remove(key);
            }
        };
    }

    public Map<String, String> getAllEntries() {
        Map<String, String> ret = new HashMap<>();
        for (Map.Entry<String, Object> entry : this.registry.entrySet()) {
            ret.put(entry.getKey(), entry.getValue().toString());
        }

        return ret;
    }

    @SuppressWarnings("unchecked")
    public Object getProxyOf(String key, Class<?>[] intf) {
        ProxyBuilder builder = (ProxyBuilder) getOrCreateEntry(key, new ProxyBuilder(intf));

        return builder.buildProxy();
    }

    @SuppressWarnings("unchecked")
    public Object setImplToProxy(String key, Class<?>[] intf, Object impl) {
        ProxyBuilder builder = (ProxyBuilder) getOrCreateEntry(key, new ProxyBuilder(intf));

        builder.setImpl(impl);
        return impl;
    }

    @SuppressWarnings("unchecked")
    public Object getProxyOf(String key, Class<?> intf) {
        ProxyBuilder builder = (ProxyBuilder) getOrCreateEntry(key, new ProxyBuilder(intf));

        return builder.buildProxy();
    }

    @SuppressWarnings("unchecked")
    public Object setImplToProxy(String key, Class<?> intf, Object impl) {
        ProxyBuilder builder = (ProxyBuilder) getOrCreateEntry(key, new ProxyBuilder(intf));

        builder.setImpl(impl);
        return impl;
    }
}
