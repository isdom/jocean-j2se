package org.jocean.j2se.spring;

import org.jocean.idiom.BeanHolder;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;

public class SpringBeanHolder implements BeanHolder, BeanFactoryAware {

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this._beanFactory = beanFactory;
    }

    @Override
    public <T> T getBean(Class<T> requiredType) {
        return this._beanFactory.getBean(requiredType);
    }

    private BeanFactory _beanFactory;
}
