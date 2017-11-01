package org.jocean.j2se.spring;

import java.lang.reflect.Field;
import java.util.Properties;

import org.jocean.idiom.Beans;
import org.jocean.idiom.ExceptionUtils;
import org.jocean.idiom.ReflectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanPostProcessor;

public class FieldValueSetter implements BeanPostProcessor {

    private static final Logger LOG = 
            LoggerFactory.getLogger(FieldValueSetter.class);

    public FieldValueSetter(final Properties properties) {
        this._properties = properties;
    }
    
    @Override
    public Object postProcessBeforeInitialization(final Object bean, final String beanName)
            throws BeansException {
        if (null!=bean) {
            final Field[] fields = ReflectUtils.getAnnotationFieldsOf(bean.getClass(), Value.class);
            for (Field field : fields) {
                try {
                    final Value v = field.getAnnotation(Value.class);
                    final String key = v.value().replace("${", "").replace("}", "");
                    final String value = this._properties.getProperty(key);
                    
                    if (null!=value) {
                        field.set(bean, Beans.fromString(value, field.getType()));
                        if (LOG.isInfoEnabled()) {
                            LOG.info("set value {} to bean({})'s field({}) by key {}", value, beanName, field, key);
                        }
                    } else {
                        LOG.warn("NOT Found value for key {}, unable auto set bean({})'s field({})!", 
                                key, beanName, field);
                    }
                } catch (Exception e) {
                    LOG.warn("exception when FieldValueSetter.postProcessBeforeInitialization for bean({}), detail: {}",
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

    private Properties _properties;
}
