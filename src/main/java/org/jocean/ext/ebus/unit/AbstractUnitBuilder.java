package org.jocean.ext.ebus.unit;

import org.jocean.idiom.Pair;

public abstract class AbstractUnitBuilder implements UnitBuilder {

    private String name;

    public AbstractUnitBuilder(String name) {
        this.name = name;
    }

    public Pair<EventUnit, String> createUnit(UnitSource source) {
        EventUnit unit = createUnitImpl();
        if (null == unit) {
            return Pair.of(null, "[" + name + "] : can't instantiated unit source { "
                    + source.getName() + "}");
        }
        if (unit instanceof UnitSourceAware) {
            ((UnitSourceAware) unit).setUnitSource(source);
        }
        return Pair.of(unit, null);
    }

    protected abstract EventUnit createUnitImpl();
}
