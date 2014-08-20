package org.jocean.ext.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicReference;

public class ProxyBuilder<T> {
    private static final Logger logger =
            LoggerFactory.getLogger(ProxyBuilder.class);

    private AtomicReference<T> ref = new AtomicReference<>();
    private Class<T>[] intf;

    private class Handler implements InvocationHandler {

        public Object invoke(Object obj, Method method, Object[] args)
                throws Throwable {
            T impl = ref.get();
            if (null != impl) {
                return method.invoke(impl, args);
            }

            throw new RuntimeException("implementation Object !NOT! set yet.");
        }

    }

    private Handler handler = new Handler();

    @SuppressWarnings("unchecked")
    public ProxyBuilder(Class<T> intf) {
        this.intf = new Class[]{intf};
    }

    public ProxyBuilder(Class<T>[] intf) {
        this.intf = intf;
    }

    @SuppressWarnings("unchecked")
    public T buildProxy() {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (logger.isDebugEnabled()) {
            logger.debug("using ClassLoader {} to newProxyInstance", cl);
        }
        return (T) Proxy.newProxyInstance(
                cl, intf, handler);
    }

    public void setImpl(T impl) {
        this.ref.set(impl);
    }

    public String toString() {
        T impl = this.ref.get();

        if (null != impl) {
            return impl.toString();
        }

        return "ProxyBuilder with null impl";
    }
}
