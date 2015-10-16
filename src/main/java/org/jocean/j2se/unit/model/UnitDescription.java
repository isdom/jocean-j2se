/**
 * 
 */
package org.jocean.j2se.unit.model;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Comparator;
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
    
    private final Comparator<UnitDescription> DESC_COMPARATOR = 
        new Comparator<UnitDescription>() {
            @Override
            public int compare(final UnitDescription o1, final UnitDescription o2) {
                return o1._name.compareTo(o2._name);
            }};
    
    public void setName(final String name) {
        this._name = name;
    }
    
    public void setChildren(final UnitDescription[] children) {
        this._children = null != children 
                ? children
                : EMPTY_DESCRIPTIONS;
        
        Arrays.sort(this._children, DESC_COMPARATOR);
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

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(_children);
        result = prime * result + ((_name == null) ? 0 : _name.hashCode());
        result = prime * result
                + ((_parameters == null) ? 0 : _parameters.hashCode());
        return result;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        UnitDescription other = (UnitDescription) obj;
        if (!Arrays.equals(_children, other._children))
            return false;
        if (_name == null) {
            if (other._name != null)
                return false;
        } else if (!_name.equals(other._name))
            return false;
        if (_parameters == null) {
            if (other._parameters != null)
                return false;
        } else if (!_parameters.equals(other._parameters))
            return false;
        return true;
    }

    private String _name;
    private String _parameters;
    private UnitDescription[] _children = EMPTY_DESCRIPTIONS;
}
