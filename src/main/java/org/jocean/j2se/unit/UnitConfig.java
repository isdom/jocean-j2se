/**
 * 
 */
package org.jocean.j2se.unit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson.annotation.JSONField;

/**
 * @author isdom
 *
 */
public class UnitConfig {
    
    @JSONField(name="name")
    public void setName(final String name) {
        this._name = name;
    }
    
    @JSONField(name="children")
    public void setChildren(final List<UnitConfig> children) {
        this._children.clear();
        this._children.addAll(children);
    }
    
    @JSONField(name="parameters")
    public void setParameters(final Map<String, String> parameters) {
        this._parameters.clear();
        this._parameters.putAll(parameters);
    }
    
    private String _name;
    private final List<UnitConfig> _children = new ArrayList<>();
    private final Map<String, String> _parameters = new HashMap<>();
}
