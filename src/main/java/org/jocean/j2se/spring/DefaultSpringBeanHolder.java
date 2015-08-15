package org.jocean.j2se.spring;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

public class DefaultSpringBeanHolder implements SpringBeanHolder, BeanFactoryAware {

    @Override
    public void setBeanFactory(final BeanFactory beanFactory) throws BeansException {
        this._beanFactory = beanFactory;
    }

    @Override
    public <T> T getBean(final Class<T> requiredType) {
        return this._beanFactory.getBean(requiredType);
    }

    @Override
    public <T> T getBean(final String name, final Class<T> requiredType) {
        return this._beanFactory.getBean(name, requiredType);
    }

    @Override
    public ConfigurableListableBeanFactory[] allBeanFactory() {
        return ( this._beanFactory instanceof ConfigurableListableBeanFactory)
                ? new ConfigurableListableBeanFactory[]{(ConfigurableListableBeanFactory)this._beanFactory}
                : new ConfigurableListableBeanFactory[0]
        ;
    }
    
    private BeanFactory _beanFactory;
}
