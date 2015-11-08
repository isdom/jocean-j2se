package org.jocean.j2se.stats;

import java.util.Map;

import org.jocean.idiom.Function;
import org.jocean.idiom.SimpleCache;
import org.jocean.idiom.stats.TimeIntervalMemo;

public class MultilevelStatsImpl implements MultilevelStats{
    
    MultilevelStatsImpl(final int levels) {
        if (levels <= 0) {
            throw new RuntimeException("invalid cache level, must >= 1");
        }
        
        this._cache = new SimpleCache<Object, Object>(genIfAbsent(levels-1));
    }
    
    private Function<Object, Object> genIfAbsent(final int levels) {
        return new Function<Object, Object>() {
            @Override
            public Object apply(final Object input) {
                if ( 0 == levels ) {
                    return TIMemos.memo_10ms_30S();
                } else {
                    return new SimpleCache<Object, Object>(genIfAbsent(levels-1));
                }
            }};
    }

    @SuppressWarnings("unchecked")
    public void recordInterval(final long interval, final Object ...levels) {
        int idx = 0;
        SimpleCache<Object, Object> cache = this._cache;
        while ( idx < levels.length - 1 ) {
            cache = (SimpleCache<Object, Object>)cache.get(levels[idx++]);
        }
        final TimeIntervalMemo memo = (TimeIntervalMemo)cache.get(levels[idx]);
        memo.recordInterval(interval);
    }
    
    public <K,V> Map<K,V> fetchStatsSnapshot(final Object ...levels) {
        return null;
    }
    
    private final SimpleCache<Object, Object> _cache;
}
