package org.jocean.j2se.spring;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.support.AbstractApplicationContext;

public class ApplicationContextNameSetter implements ApplicationContextAware {

    @Override
    public void setApplicationContext(final ApplicationContext applicationContext)
            throws BeansException {
        if (applicationContext instanceof AbstractApplicationContext
            && null != this._displayName) {
            ((AbstractApplicationContext)applicationContext).setDisplayName(this._displayName);
        }
    }
    
    public void setDisplayName(final String displayName) {
        this._displayName = displayName;
    }

    private String _displayName;
}
