package org.jocean.j2se.util;

import java.lang.annotation.Annotation;

import javax.inject.Named;

import org.jocean.idiom.BeanHolder;
import org.springframework.beans.factory.annotation.Qualifier;

public class BeanHolders {
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
}
