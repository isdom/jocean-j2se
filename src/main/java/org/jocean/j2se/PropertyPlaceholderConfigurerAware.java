package org.jocean.j2se;

import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;

public interface PropertyPlaceholderConfigurerAware {
    public void setPropertyPlaceholderConfigurer(final PropertyPlaceholderConfigurer configurer);

}
