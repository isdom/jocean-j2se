package org.jocean.j2se.spring;

import java.util.Properties;

import org.jocean.idiom.ExceptionUtils;
import org.jocean.j2se.PropertiesResourceAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

public class PropertiesResourceSetter implements BeanPostProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(PropertiesResourceSetter.class);

    public PropertiesResourceSetter(final Properties properties) {
        this._properties = properties;
    }

    @Override
    public Object postProcessBeforeInitialization(final Object bean, final String beanName)
            throws BeansException {
        if (null!=bean) {
            if (bean instanceof PropertiesResourceAware) {
                try {
                    ((PropertiesResourceAware)bean).setPropertiesResource(this._properties);
                } catch (final Exception e) {
                    LOG.warn("exception when setPropertiesResource for bean({}), detail: {}",
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

    private final Properties _properties;
}
