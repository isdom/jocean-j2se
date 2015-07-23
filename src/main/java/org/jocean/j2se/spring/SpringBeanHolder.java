package org.jocean.j2se.spring;

import org.jocean.idiom.BeanHolder;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

public interface SpringBeanHolder extends BeanHolder {

    public ConfigurableListableBeanFactory[] allBeanFactory();
    
}
