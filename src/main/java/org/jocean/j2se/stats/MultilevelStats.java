package org.jocean.j2se.stats;

import java.util.Map;

public interface MultilevelStats {
    
    public void recordInterval(final long interval, final Object ...levels);
    
    public <K,V> Map<K,V> fetchStatsSnapshot(final Object ...levels);
}
