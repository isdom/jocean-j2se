package org.jocean.j2se.unit;

import org.jocean.idiom.BeanFinder;
import org.jocean.idiom.BeanHolder;
import org.jocean.idiom.BeanHolderAware;

import rx.Observable;

public class DefaultBeanFinder implements BeanFinder, BeanHolderAware {

    @Override
    public <T> Observable<T> find(final Class<T> requiredType) {
        return Observable.unsafeCreate(subscriber -> {
            if (!subscriber.isUnsubscribed()) {
                final T bean = this._holder.getBean(requiredType);
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
    public <T> Observable<T> find(String name, Class<T> requiredType) {
        return Observable.unsafeCreate(subscriber -> {
            if (!subscriber.isUnsubscribed()) {
                final T bean = this._holder.getBean(name, requiredType);
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

    private BeanHolder _holder;
}
