package org.jocean.j2se.unit;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;

import org.jocean.idiom.BeanFinder;
import org.jocean.idiom.BeanHolder;
import org.jocean.idiom.BeanHolderAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rx.Observable;

public class DefaultBeanFinder implements BeanFinder, BeanHolderAware {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultBeanFinder.class);

    @Override
    public <T> Observable<T> find(final Class<T> requiredType) {
        return Observable.unsafeCreate(subscriber -> {
            if (!subscriber.isUnsubscribed()) {
                final ConcurrentMap<String, Object> cache = this._cached.get();
                @SuppressWarnings("unchecked")
                T bean = (T)cache.get(requiredType.getName());
                if (null == bean) {
                    bean = this._holder.getBean(requiredType);
                    if (null != bean) {
                        cache.putIfAbsent(requiredType.getName(), bean);
                    }
                }
                if (null != bean) {
                    subscriber.onNext(bean);
                    subscriber.onCompleted();
                } else {
                    subscriber.onError(new RuntimeException("no bean with type " + requiredType + " available"));
                }
            }
        });
    }

    @Override
    public <T> Observable<T> find(final String name, final Class<T> requiredType) {
        return Observable.unsafeCreate(subscriber -> {
            if (!subscriber.isUnsubscribed()) {
                final ConcurrentMap<String, Object> cache = this._cached.get();
                final String key = requiredType.getName() + ":" + name;
                @SuppressWarnings("unchecked")
                T bean = (T)cache.get(key);
                if (null == bean) {
                    bean = this._holder.getBean(name, requiredType);
                    if (null != bean) {
                        cache.putIfAbsent(key, bean);
                    }
                }
                if (null != bean) {
                    subscriber.onNext(bean);
                    subscriber.onCompleted();
                } else {
                    subscriber.onError(new RuntimeException(
                            "no bean with name(" + name + ")/type(" + requiredType + ") available"));
                }
            }
        });
    }

    @Override
    public void setBeanHolder(final BeanHolder beanHolder) {
        this._holder = beanHolder;
    }

    public void resetCache() {
        final ConcurrentMap<String, Object> old = this._cached.getAndSet(new ConcurrentHashMap<>());
        LOG.info("reset beans cache");
        if (null!=old) {
            old.clear();
        }
    }

    private BeanHolder _holder;
    private final AtomicReference<ConcurrentMap<String, Object>> _cached = new AtomicReference<>(new ConcurrentHashMap<>());
}
