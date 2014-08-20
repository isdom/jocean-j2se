package org.jocean.ext.ebus.unit;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class UnitContext {

    public String getGroup() {
        return _group;
    }

    public UnitContext setGroup(final String group) {
        this._group = group;
        return this;
    }

    public String getName() {
        return _name;
    }

    public UnitContext setName(final String name) {
        this._name = name;
        return this;
    }

    public String getSource() {
        return _source;
    }

    public UnitContext setSource(final String source) {
        this._source = source;
        return this;
    }

    public Map<String, String> getParams() {
        return Collections.unmodifiableMap(this._params);
    }

    public UnitContext setParams(final Map<String, String> params) {
        this._params.clear();
        this._params.putAll(params);
        return this;
    }

    public Map<String, String> getPlaceholders() {
        return Collections.unmodifiableMap(this._placeholders);
    }

    public UnitContext setPlaceholders(final Map<String, String> placeholders) {
        this._placeholders.clear();
        this._placeholders.putAll(placeholders);
        return this;
    }

    public int getOrder() {
        return this._order;
    }

    public UnitContext setOrder(int order) {
        this._order = order;
        return this;
    }


    @Override
    public String toString() {
        return "UnitContext [_group=" + _group + ", _name=" + _name
                + ", _source=" + _source + ", _params=" + _params
                + ", _placeholders=" + _placeholders + ", _order=" + _order
                + "]";
    }


    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((_group == null) ? 0 : _group.hashCode());
        result = prime * result + ((_name == null) ? 0 : _name.hashCode());
        result = prime * result + _order;
        result = prime * result + ((_params == null) ? 0 : _params.hashCode());
        result = prime * result + ((_source == null) ? 0 : _source.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        UnitContext other = (UnitContext) obj;
        if (_group == null) {
            if (other._group != null)
                return false;
        } else if (!_group.equals(other._group))
            return false;
        if (_name == null) {
            if (other._name != null)
                return false;
        } else if (!_name.equals(other._name))
            return false;
        if (_order != other._order)
            return false;
        if (_params == null) {
            if (other._params != null)
                return false;
        } else if (!_params.equals(other._params))
            return false;
        if (_source == null) {
            if (other._source != null)
                return false;
        } else if (!_source.equals(other._source))
            return false;
        return true;
    }

    private String _group;

    private String _name;

    private String _source;

    private Map<String, String> _params = new HashMap<>();

    private Map<String, String> _placeholders = new HashMap<>();

    private int _order;
}
