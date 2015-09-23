package org.jocean.j2se.stats;

import org.jocean.idiom.Emitter;
import org.jocean.idiom.stats.TimeIntervalMemo;

import rx.functions.Action1;

import com.google.common.collect.Range;

public class TIMemos {
    public interface EmitableTIMemo extends TimeIntervalMemo, Emitter<String> {
    }
    
    private static class EmitableTIMemoImpl extends TIMemoImplOfRanges 
        implements EmitableTIMemo {

        public EmitableTIMemoImpl(final String[] rangeNames, final Range<Long>[] ranges) {
            super(rangeNames, ranges);
        }
        
        @Override
        public void emit(final Action1<String> receptor) {
            for ( int idx = 0; idx < this._names.length; idx++ ) {
                receptor.call(this._names[idx] +":"+ this._counters[idx].get());
            }
        }
    }
    
    private static final Range<Long> MT30S = Range.atLeast(30000L);
    private static final Range<Long> LT30S = Range.closedOpen(10000L, 30000L);
    private static final Range<Long> LT10S = Range.closedOpen(5000L, 10000L);
    private static final Range<Long> LT5S = Range.closedOpen(1000L, 5000L);
    private static final Range<Long> LT1S = Range.closedOpen(500L, 1000L);
    private static final Range<Long> LT500MS = Range.closedOpen(100L, 500L);
    private static final Range<Long> LS100MS = Range.closedOpen(10L, 100L);
    private static final Range<Long> LT10MS = Range.closedOpen(0L, 10L);
    
    @SuppressWarnings("unchecked")
    public static EmitableTIMemo memo_10ms_30S() {
        return new EmitableTIMemoImpl(new String[]{
                "lt10ms",
                "lt100ms",
                "lt500ms",
                "lt1s",
                "lt5s",
                "lt10s",
                "lt30s",
                "mt30s",
                },
                new Range[]{
                LT10MS,
                LS100MS,
                LT500MS,
                LT1S,
                LT5S,
                LT10S,
                LT30S,
                MT30S});
    }
    
}