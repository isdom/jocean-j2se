package org.jocean.j2se.spring;

import org.jocean.idiom.BeanHolder;
import org.jocean.idiom.BeanHolderAware;
import org.jocean.idiom.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

public class BeanHolderSetter implements BeanPostProcessor {

    private static final Logger LOG = 
            LoggerFactory.getLogger(BeanHolderSetter.class);

    public BeanHolderSetter(final BeanHolder beanHolder) {
        this._beanHoler = beanHolder;
    }
    
    @Override
    public Object postProcessBeforeInitialization(final Object bean, final String beanName)
            throws BeansException {
        if (null!=bean) {
            if (bean instanceof BeanHolderAware) {
                try {
                    ((BeanHolderAware)bean).setBeanHolder(_beanHoler);
                } catch (Exception e) {
                    LOG.warn("exception when setBeanHolder for bean({}), detail: {}",
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

    private final BeanHolder _beanHoler;
}
