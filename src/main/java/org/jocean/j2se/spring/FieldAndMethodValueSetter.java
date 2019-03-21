package org.jocean.j2se.spring;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import org.jocean.idiom.Beans;
import org.jocean.idiom.ExceptionUtils;
import org.jocean.idiom.ReflectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.util.StringValueResolver;

public class FieldAndMethodValueSetter implements BeanPostProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(FieldAndMethodValueSetter.class);

    public FieldAndMethodValueSetter(final StringValueResolver stringValueResolver) {
        this._stringValueResolver = stringValueResolver;
    }

    @Override
    public Object postProcessBeforeInitialization(final Object bean, final String beanName)
            throws BeansException {
        if (null!=bean) {
            assignAnnotatedFields(bean, beanName);
            assignAnnotatedMethods(bean, beanName);
        }
        return bean;
    }

    private void assignAnnotatedFields(final Object bean, final String beanName) {
        final Field[] fields = ReflectUtils.getAnnotationFieldsOf(bean.getClass(), Value.class);
        for (final Field field : fields) {
            try {
                final Value v = field.getAnnotation(Value.class);
                final String value = resolveValue(v.value());

                if (null!=value) {
                    field.set(bean, Beans.fromString(value, field.getType()));
                    if (LOG.isInfoEnabled()) {
                        LOG.info("set value {} to bean({})'s field({}), it's original is {}", value, beanName, field, v.value());
                    }
                } else {
                    LOG.warn("NOT resolve value {}, unable auto set bean({})'s field({})!", v.value(), beanName, field);
                }
            } catch (final Exception e) {
                LOG.warn("exception when FieldAndMethodValueSetter.postProcessBeforeInitialization for bean({}), detail: {}",
                        beanName, ExceptionUtils.exception2detail(e));
            }
        }
    }

    private void assignAnnotatedMethods(final Object bean, final String beanName) {
        final Method[] methods = ReflectUtils.getAnnotationMethodsOf(bean.getClass(), Value.class);
        for (final Method method : methods) {
            if (method.getParameterCount()!=1) {
                LOG.warn("method {}'s parameter count != 1, ignore invoke by @Value key", method);
                continue;
            }

            try {
                final Value v = method.getAnnotation(Value.class);
                final String value = resolveValue(v.value());

                if (null!=value) {
                    final Parameter p1st = method.getParameters()[0];
                    method.invoke(bean, Beans.fromString(value, p1st.getType()));
                    if (LOG.isInfoEnabled()) {
                        LOG.info("invoke bean({})'s method {} with value {}, which original is {}", beanName, method, value, v.value());
                    }
                } else {
                    LOG.warn("NOT resolve value {}, unable invoke bean({})'s method({})!", v.value(), beanName, method);
                }
            } catch (final Exception e) {
                LOG.warn("exception when FieldAndMethodValueSetter.postProcessBeforeInitialization for bean({}), detail: {}",
                        beanName, ExceptionUtils.exception2detail(e));
            }
        }
    }

    @Override
    public Object postProcessAfterInitialization(final Object bean, final String beanName)
            throws BeansException {
        return bean;
    }

    private String resolveValue(final String strVal) {
        try {
            return _stringValueResolver.resolveStringValue(strVal);
        } catch (final Exception e) {
            return null;
        }
    }

    private final StringValueResolver _stringValueResolver;
}
