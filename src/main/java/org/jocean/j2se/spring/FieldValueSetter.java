package org.jocean.j2se.spring;

import java.lang.reflect.Field;

import org.jocean.idiom.Beans;
import org.jocean.idiom.ExceptionUtils;
import org.jocean.idiom.ReflectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.util.StringValueResolver;

public class FieldValueSetter implements BeanPostProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(FieldValueSetter.class);

    public FieldValueSetter(final StringValueResolver stringValueResolver) {
        this._stringValueResolver = stringValueResolver;
    }

    @Override
    public Object postProcessBeforeInitialization(final Object bean, final String beanName)
            throws BeansException {
        if (null!=bean) {
            LOG.info("FieldValueSetter: handle ({})/{}", beanName, bean);

            assignAnnotatedFields(bean, beanName);
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
                LOG.warn("exception when FieldValueSetter.postProcessBeforeInitialization for bean({}), detail: {}",
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
