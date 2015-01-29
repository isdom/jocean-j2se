/**
 * 
 */
package org.jocean.j2se.spring;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.FactoryBean;

/**
 * @author isdom
 *
 */
public class BeanProxy<T> implements FactoryBean<T> {

    private static final Logger LOG = 
    	LoggerFactory.getLogger(BeanProxy.class);
    
	@SuppressWarnings("unchecked")
    public BeanProxy(final Class<T> intf) {
		this._intf = new Class[]{intf};
		this._proxy = buildProxy();
	}

	@SuppressWarnings("unchecked")
	public T buildProxy() {
		return (T)Proxy.newProxyInstance(
		        Thread.currentThread().getContextClassLoader(), 
		        this._intf, 
				new InvocationHandler() {
                    @Override
                    public Object invoke(
                            final Object proxy, 
                            final Method method,
                            final Object[] args) throws Throwable {
                        final T impl = _ref.get();
                        if ( null != impl ) {
                            return method.invoke(impl, args);
                        }
                        throw new RuntimeException("implementation Object !NOT! set yet.");
                    }} );
	}

	public void setImpl(final T impl) {
		if ( !this._ref.compareAndSet(null, impl) ) {
		    LOG.warn("BeanProxy already set implementation, ignore {}", impl);
		}
	}

    public void setImplForced(final T impl) {
        this._ref.set(impl);
    }
    
	@Override
	public String toString() {
		final T impl = this._ref.get();
		
		if ( null != impl ) {
			return impl.toString();
		}
		else {
		    return	"BeanProxy with null impl!";
		}
	}

    @Override
    public T getObject() throws Exception {
        return this._proxy;
    }

    @Override
    public Class<?> getObjectType() {
        return this._intf[0];
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    private final AtomicReference<T> _ref = new AtomicReference<T>(null);
    private final Class<T>[]  _intf;
    private final T _proxy;
    
}
