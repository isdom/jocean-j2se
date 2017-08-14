package org.jocean.j2se.spring;

import org.jocean.j2se.unit.UnitAgent;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

public class UnitAgentAware implements BeanPostProcessor {

    @Override
    public Object postProcessBeforeInitialization(final Object bean, final String beanName)
            throws BeansException {
        if (null!=bean) {
            if (bean instanceof UnitAgent) {
                this._unitAgent = (UnitAgent) bean;
            }
        }
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(final Object bean, final String beanName)
            throws BeansException {
        return bean;
    }
    
    public UnitAgent getUnitAgent() {
        return this._unitAgent;
    }

    private UnitAgent _unitAgent = null;
}
