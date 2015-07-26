package org.jocean.j2se.spring;

import java.lang.reflect.Field;

import javax.inject.Inject;

import org.jocean.idiom.BeanHolder;
import org.jocean.idiom.ExceptionUtils;
import org.jocean.idiom.ReflectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

public class BeanHolderBasedInjector implements BeanPostProcessor {

    private static final Logger LOG = 
            LoggerFactory.getLogger(BeanHolderBasedInjector.class);

    public BeanHolderBasedInjector(final BeanHolder beanHolder) {
        this._beanHoler = beanHolder;
    }
    
    @Override
    public Object postProcessBeforeInitialization(final Object bean, final String beanName)
            throws BeansException {
        if (null!=bean) {
            final Field[] fields = ReflectUtils.getAnnotationFieldsOf(bean.getClass(), Inject.class);
            for (Field field : fields) {
                try {
                    if (null==field.get(bean)) {
                        final Object value = this._beanHoler.getBean(field.getType());
                        if (null!=value) {
                            field.set(bean, value);
                            if (LOG.isDebugEnabled()) {
                                LOG.debug("inject {} to bean({})'s field({})", value, beanName, field);
                            }
                        }
                    }
                } catch (Exception e) {
                    LOG.warn("exception when postProcessBeforeInitialization for bean({}), detail: {}",
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
