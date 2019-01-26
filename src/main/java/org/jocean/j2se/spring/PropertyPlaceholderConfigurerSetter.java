package org.jocean.j2se.spring;

import org.jocean.idiom.ExceptionUtils;
import org.jocean.j2se.PropertyPlaceholderConfigurerAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;

public class PropertyPlaceholderConfigurerSetter implements BeanPostProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(PropertyPlaceholderConfigurerSetter.class);

    public PropertyPlaceholderConfigurerSetter(final PropertyPlaceholderConfigurer configurer) {
        this._configurer = configurer;
    }

    @Override
    public Object postProcessBeforeInitialization(final Object bean, final String beanName)
            throws BeansException {
        if (null!=bean) {
            if (bean instanceof PropertyPlaceholderConfigurerAware) {
                try {
                    ((PropertyPlaceholderConfigurerAware)bean).setPropertyPlaceholderConfigurer(this._configurer);
                } catch (final Exception e) {
                    LOG.warn("exception when setPropertyPlaceholderConfigurer for bean({}), detail: {}",
                            beanName, ExceptionUtils.exception2detail(e));
                }
            }
        }
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(final Object bean, final String beanName)
            throws BeansException {
        return bean;
    }

    private final PropertyPlaceholderConfigurer _configurer;
}
