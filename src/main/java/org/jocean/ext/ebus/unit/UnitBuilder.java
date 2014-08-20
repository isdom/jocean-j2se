package org.jocean.ext.ebus.unit;

import org.jocean.idiom.Pair;

public interface UnitBuilder {
    public Pair<EventUnit, String> createUnit(UnitSource source);
}
