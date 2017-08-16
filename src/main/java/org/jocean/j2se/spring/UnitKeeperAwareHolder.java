package org.jocean.j2se.spring;

import org.jocean.j2se.unit.UnitKeeperAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

public class UnitKeeperAwareHolder implements BeanPostProcessor {

    private static final Logger LOG = 
            LoggerFactory.getLogger(UnitKeeperAwareHolder.class);

    @Override
    public Object postProcessBeforeInitialization(final Object bean, final String beanName)
            throws BeansException {
        if (null!=bean) {
            if (bean instanceof UnitKeeperAware) {
                _unitKeeperAware = (UnitKeeperAware)bean;
            }
        }
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(final Object bean, final String beanName)
            throws BeansException {
        return bean;
    }

    public UnitKeeperAware getUnitKeeperAware() {
        return this._unitKeeperAware;
    }

    private UnitKeeperAware _unitKeeperAware;
}
