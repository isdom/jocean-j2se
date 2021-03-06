package org.jocean.j2se.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

import javax.inject.Named;

import org.jocean.idiom.BeanHolder;
import org.jocean.idiom.ExceptionUtils;
import org.jocean.idiom.ReflectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.util.StringValueResolver;

public class BeanHolders {

    private static final Logger LOG = LoggerFactory.getLogger(BeanHolders.class);

    private BeanHolders() {
        throw new IllegalStateException("No instances!");
    }

    public static Object getBean(final BeanHolder holder, final Class<?> type, final Annotation named) {
        if (named != null) {
            final String name = getName(named);
            if (null != name && !name.equals("")) {
                return holder.getBean(name, type);
            }
        }
        return holder.getBean(type);
    }

    private static String getName(final Annotation named) {
        return named instanceof Named ? ((Named) named).value()
                : named instanceof Qualifier ? ((Qualifier) named).value() : null;
    }

    public static Object getBean(final BeanHolder holder,
            final Class<?> type,
            final Annotation named,
            final Object owner,
            final StringValueResolver stringValueResolver) {
        if (named != null) {
            String name = getName(named);
            if (null != name && !name.equals("")) {
                if (null!=owner && name.startsWith("this.")) {
                    name = getNameByExpression(owner, name.substring(5));
                    if (null == name) {
                        LOG.warn("invalid expression {}, can't found matched field or field is null", getName(named));
                        return null;
                    }
                }
                return holder.getBean(resolveValue(stringValueResolver, name), type);
            }
        }
        return holder.getBean(type);
    }

    private static String resolveValue(final StringValueResolver stringValueResolver, final String strVal) {
        if (null == stringValueResolver) {
            LOG.debug("getBean: stringValueResolver is null, use input {} as resolved", strVal);
            return strVal;
        }
        try {
            final String resolvedStringValue = stringValueResolver.resolveStringValue(strVal);
            LOG.debug("getBean: resolveValue for input {} --> resolved: {}", strVal, resolvedStringValue);
            return resolvedStringValue;
        } catch (final Exception e) {
            return null;
        }
    }

    private static String getNameByExpression(final Object owner, final String expression) {
        try {
            final Field field = ReflectUtils.getFieldNamedDeep(owner.getClass(), expression);
            if (null != field) {
                field.setAccessible(true);
                final Object value = field.get(owner);
                if (null != value) {
                    return value.toString();
                }
            }
        } catch (final Exception e) {
            LOG.warn("exception when getNameByExpression for ({}) with exp ({}), detail: {}",
                    owner, expression, ExceptionUtils.exception2detail(e));
        }
        return null;
    }
}
