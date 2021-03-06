package org.jocean.j2se.spring;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

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
import org.springframework.util.StringValueResolver;

public class BeanHolderBasedMethodInjector implements BeanPostProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(BeanHolderBasedMethodInjector.class);

    public BeanHolderBasedMethodInjector(final BeanHolder beanHolder, final StringValueResolver stringValueResolver) {
        this._beanHolder = beanHolder;
        this._stringValueResolver = stringValueResolver;
    }

    @Override
    public Object postProcessBeforeInitialization(final Object bean, final String beanName)
            throws BeansException {
        if (null!=bean) {
            LOG.info("BeanHolderBasedMethodInjector: handle ({})/{}", beanName, bean);

            injectAnnotatedMethods(bean, beanName, Inject.class, Named.class);
            injectAnnotatedMethods(bean, beanName, Autowired.class, Qualifier.class);
        }
        return bean;
    }

    private void injectAnnotatedMethods(final Object bean, final String beanName,
            final Class<? extends Annotation> injectCls, final Class<? extends Annotation> namedCls) {
        final Method[] methods = ReflectUtils.getAnnotationMethodsOf(bean.getClass(), injectCls);
        for (final Method method : methods) {
            if (method.getParameterCount()!=1) {
                LOG.warn("method {}'s parameter count != 1, ignore inject", method);
                continue;
            }

            try {
                final Parameter p1st = method.getParameters()[0];
                final Object value = BeanHolders.getBean(this._beanHolder,
                        p1st.getType(), method.getAnnotation(namedCls), bean, this._stringValueResolver);

                if (null != value) {
                    method.invoke(bean, value);
                    LOG.info("inject {} to bean({})'s method({})", value, beanName, method);
                } else {
                    LOG.info("NOT Found global bean for type {}, unable auto inject bean({})'s method({})!",
                            p1st.getType(), beanName, method);
                }
            } catch (final Exception e) {
                LOG.warn("exception when postProcessBeforeInitialization for bean({}), detail: {}",
                        beanName, ExceptionUtils.exception2detail(e));
            }
        }
    }

    @Override
    public Object postProcessAfterInitialization(final Object bean, final String beanName)
            throws BeansException {
        return bean;
    }

    private final BeanHolder _beanHolder;
    private final StringValueResolver _stringValueResolver;
}
