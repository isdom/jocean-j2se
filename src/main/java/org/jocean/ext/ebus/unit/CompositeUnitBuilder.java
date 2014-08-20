package org.jocean.ext.ebus.unit;

import org.jocean.idiom.Pair;

import java.util.HashMap;
import java.util.Map;

public class CompositeUnitBuilder implements UnitBuilder {

    private Map<String, UnitBuilder> builders = new HashMap<>();


    public void setBuilders(Map<String, UnitBuilder> builders) {
        this.builders.clear();

        for (Map.Entry<String, UnitBuilder> entry : builders.entrySet()) {
            if (null != entry.getValue()) {
                this.builders.put(entry.getKey(), entry.getValue());
            }
        }
    }

    public Pair<EventUnit, String> createUnit(UnitSource source) {
        if (null == source) {
            return Pair.of(null, "unit source is null");
        }
        UnitBuilder builder = builders.get(source.getType());
        if (null == builder) {
            return Pair.of(null, "can't found unit builder for type [" + source.getType() + "]");
        }
        return builder.createUnit(source);
    }
}
