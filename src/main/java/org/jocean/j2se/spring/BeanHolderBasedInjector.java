package org.jocean.j2se.spring;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

import javax.inject.Inject;
import javax.inject.Named;

import org.jocean.idiom.BeanHolder;
import org.jocean.idiom.ExceptionUtils;
import org.jocean.idiom.ReflectUtils;
import org.jocean.j2se.util.BeanHolders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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
            injectFields(bean, beanName, Inject.class, Named.class);
            injectFields(bean, beanName, Autowired.class, Qualifier.class);
        }
        return bean;
    }

    private void injectFields(final Object bean, final String beanName, final Class<? extends Annotation> injectCls,
            final Class<? extends Annotation> namedCls) {
        final Field[] fields = ReflectUtils.getAnnotationFieldsOf(bean.getClass(), injectCls);
        for (Field field : fields) {
            try {
                if (null == field.get(bean)) {
                    final Object value = BeanHolders.getBean(this._beanHoler, field.getType(),
                            field.getAnnotation(namedCls));
                    if (null != value) {
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
                LOG.warn("exception when postProcessBeforeInitialization for bean({}), detail: {}", beanName,
                        ExceptionUtils.exception2detail(e));
            }
        }
    }

    @Override
    public Object postProcessAfterInitialization(final Object bean, final String beanName)
            throws BeansException {
        return bean;
    }

    private final BeanHolder _beanHoler;
}
