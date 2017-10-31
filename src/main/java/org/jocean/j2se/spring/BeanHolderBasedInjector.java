package org.jocean.j2se.spring;

import java.lang.reflect.Field;

import javax.inject.Inject;
import javax.inject.Named;

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
                        final Object value = getValue(field);
                        if (null!=value) {
                            field.set(bean, value);
                            if (LOG.isInfoEnabled()) {
                                LOG.info("inject {} to bean({})'s field({})", value, beanName, field);
                            }
                        } else {
                            LOG.warn("NOT Found global bean for type {}, unable auto inject bean({})'s field({})!", 
                                    field.getType(), beanName, field);
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

    private Object getValue(final Field field) {
        final Named named = field.getAnnotation(Named.class);
        if (named != null && !named.value().equals("")) {
            return this._beanHoler.getBean(named.value(), field.getType());
        }
        return this._beanHoler.getBean(field.getType());
    }

    @Override
    public Object postProcessAfterInitialization(final Object bean, final String beanName)
            throws BeansException {
        return bean;
    }

    private final BeanHolder _beanHoler;
}
