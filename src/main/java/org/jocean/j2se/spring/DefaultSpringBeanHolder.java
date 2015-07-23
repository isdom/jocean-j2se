package org.jocean.j2se.spring;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.context.ConfigurableApplicationContext;

public class DefaultSpringBeanHolder implements SpringBeanHolder, BeanFactoryAware {

    @Override
    public void setBeanFactory(final BeanFactory beanFactory) throws BeansException {
        this._beanFactory = beanFactory;
    }

    @Override
    public <T> T getBean(Class<T> requiredType) {
        return this._beanFactory.getBean(requiredType);
    }

    @Override
    public ConfigurableApplicationContext[] allApplicationContext() {
        return ( this._beanFactory instanceof ConfigurableApplicationContext)
                ? new ConfigurableApplicationContext[]{(ConfigurableApplicationContext)this._beanFactory}
                : new ConfigurableApplicationContext[0]
        ;
    }
    
    private BeanFactory _beanFactory;

}
