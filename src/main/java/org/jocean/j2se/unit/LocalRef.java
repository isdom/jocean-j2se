package org.jocean.j2se.unit;

import java.io.InputStream;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;

import com.google.common.collect.Maps;

public class LocalRef implements UnitKeeperAware {
    private static final Logger LOG =
            LoggerFactory.getLogger(LocalRef.class);

    public void setLocation(final Resource location) {
        this._location = location;
    }

    public void start() throws Exception {
        try (final InputStream is = this._location.getInputStream()) {
            final Properties props = new Properties();
            props.load(is);
            final String[] source = genSourceFrom(props);
            this._unitKeeper.createOrUpdateUnit("#", source, Maps.fromProperties(props));
        }
    }
    
    public void stop() {
        this._unitKeeper.deleteUnit("#");
    }
    
    private static String[] genSourceFrom(final Properties properties) {
        final String value = properties.getProperty(SPRING_XML_KEY);
        properties.remove(SPRING_XML_KEY);
        return null!=value ? value.split(",") : null;
    }

    @Override
    public void setUnitKeeper(final UnitKeeper keeper) {
        this._unitKeeper = keeper;
    }
    
    private Resource _location;
    
    private UnitKeeper _unitKeeper;
    
    private static final String SPRING_XML_KEY = "__spring.xml";
}
