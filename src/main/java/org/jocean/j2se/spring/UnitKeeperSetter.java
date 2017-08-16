package org.jocean.j2se.spring;

import org.jocean.idiom.ExceptionUtils;
import org.jocean.j2se.unit.UnitKeeper;
import org.jocean.j2se.unit.UnitKeeperAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

public class UnitKeeperSetter implements BeanPostProcessor {

    private static final Logger LOG = 
            LoggerFactory.getLogger(UnitKeeperSetter.class);

    public UnitKeeperSetter(final UnitKeeper unitKeeper) {
        this._unitKeeper = unitKeeper;
    }
    
    @Override
    public Object postProcessBeforeInitialization(final Object bean, final String beanName)
            throws BeansException {
        if (null!=bean) {
            if (bean instanceof UnitKeeperAware) {
                try {
                    ((UnitKeeperAware)bean).setUnitKeeper(this._unitKeeper);
                } catch (Exception e) {
                    LOG.warn("exception when setUnitKeeper for bean({}), detail: {}",
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

    private final UnitKeeper _unitKeeper;
}
