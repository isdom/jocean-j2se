package org.jocean.j2se.unit;

import java.util.Map;

public interface UnitKeeper {
    public void createOrUpdateUnit(
            final String implname,
            final String[] source,
            final Map<String, String> unitParameters);
    
    public void deleteUnit(final String subname);
}
