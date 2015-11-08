package org.jocean.j2se.stats;

import java.util.Map;

public interface MultilevelStats {
    
    public void recordInterval(final long interval, final Object ...levels);
    
    public <K,V> Map<K,V> fetchStatsSnapshot(final Object ...levels);
    
    public static class Util {
        public static MultilevelStats buildStats(final int levels) {
            return new MultilevelStatsImpl(levels);
        }
    }
}
