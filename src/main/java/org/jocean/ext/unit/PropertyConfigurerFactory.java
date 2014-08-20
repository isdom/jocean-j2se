package org.jocean.ext.unit;

import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;

public class PropertyConfigurerFactory {

    public void setConfigurer(final PropertyPlaceholderConfigurer configurer) {
        this._configurer = configurer;
    }

    public PropertyPlaceholderConfigurer createConfigurer() throws Exception {
        return (null != this._configurer)
                ? this._configurer
                : new PropertyPlaceholderConfigurer();
    }

    private PropertyPlaceholderConfigurer _configurer = null;
}
