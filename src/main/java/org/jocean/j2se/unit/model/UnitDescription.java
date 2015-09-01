/**
 * 
 */
package org.jocean.j2se.unit.model;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Properties;

import org.jocean.idiom.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;

/**
 * @author isdom
 *
 */
public class UnitDescription {
    
    private static final Logger LOG = LoggerFactory
            .getLogger(UnitDescription.class);

    private static final UnitDescription[] EMPTY_DESCRIPTIONS = 
            new UnitDescription[0];
    
    public void setName(final String name) {
        this._name = name;
    }
    
    public void setChildren(final UnitDescription[] children) {
        this._children = null != children ? children : EMPTY_DESCRIPTIONS;
    }
    
    public void setParameters(final String parameters) {
        this._parameters = parameters;
    }
    
    public String getName() {
        return this._name;
    }

    public UnitDescription[] getChildren() {
        return this._children;
    }

    public String getParameters() {
        return this._parameters;
    }
    
    public Properties parametersAsProperties() {
        final Properties properties = new Properties();
        try (
            final InputStream is = null != _parameters
                    ? new ByteArrayInputStream(_parameters.getBytes(Charsets.UTF_8)) 
                    : null;
        ) {
            if (null != is) {
                try {
                    properties.load( is );
                } catch (IOException e) {
                    LOG.warn("exception when load properties, detail: {}",
                            ExceptionUtils.exception2detail(e));
                }
            }
        } catch (IOException e1) {
            // just ignore close throw Exception
        }
        return properties;
    }
    
    @Override
    public String toString() {
        return "UnitDescription [name=" + _name + ", parameters="
                + _parameters + ", children=" + Arrays.toString(_children) + "]";
    }

    private String _name;
    private String _parameters;
    private UnitDescription[] _children = EMPTY_DESCRIPTIONS;
}
