package org.jocean.j2se.unit;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.springframework.core.io.Resource;

import com.google.common.collect.Maps;

public class LocalRef implements UnitKeeperAware {

    public void setLocation(final Resource location) {
        this._location = location;
    }

    public void stop() {
        if (null != this._unitKeeper) {
            this._unitKeeper.deleteUnit("#");
        }
    }
    
    private static String[] genSourceFrom(final Properties properties) {
        final String value = properties.getProperty(SPRING_XML_KEY);
        properties.remove(SPRING_XML_KEY);
        return null!=value ? value.split(",") : null;
    }

    @Override
    public void setUnitKeeper(final UnitKeeper keeper) {
        this._unitKeeper = keeper;
        try (final InputStream is = this._location.getInputStream()) {
            final Properties props = new Properties();
            props.load(is);
            final String[] source = genSourceFrom(props);
            this._unitKeeper.createOrUpdateUnit("#", source, Maps.fromProperties(props));
        } catch (IOException e) {
        }
    }
    
    private Resource _location;
    
    private UnitKeeper _unitKeeper;
    
    private static final String SPRING_XML_KEY = "__spring.xml";
}
