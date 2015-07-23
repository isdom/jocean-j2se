package org.jocean.j2se.spring;

import org.jocean.idiom.BeanHolder;
import org.springframework.context.ConfigurableApplicationContext;

public interface SpringBeanHolder extends BeanHolder {

    public ConfigurableApplicationContext[] allApplicationContext();
    
}
