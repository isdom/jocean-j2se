package org.jocean.j2se.stats;

import org.jocean.idiom.stats.TimeIntervalMemo;

import com.google.common.collect.Range;

import rx.functions.Action1;
import rx.functions.Action2;

public class TIMemos {
    public interface OnCounter extends Action2<String,Integer> {}
    
    public interface CounterableTIMemo extends TimeIntervalMemo, Action1<OnCounter> {
    }
    
    private static class CounterableTIMemoImpl extends TIMemoImplOfRanges 
        implements CounterableTIMemo {

        public CounterableTIMemoImpl(final String[] rangeNames, final Range<Long>[] ranges) {
            super(rangeNames, ranges);
        }
        
        @Override
        public void call(final OnCounter receptor) {
            for ( int idx = 0; idx < this._names.length; idx++ ) {
                receptor.call(this._names[idx], this._counters[idx].get());
            }
        }
    }
    
    private static final Range<Long> MT30S = Range.atLeast(30000L);
    private static final Range<Long> BT10_30S = Range.closedOpen(10000L, 30000L);
    private static final Range<Long> BT5S_10S = Range.closedOpen(5000L, 10000L);
    private static final Range<Long> BT1S_5S = Range.closedOpen(1000L, 5000L);
    private static final Range<Long> BT500MS_1S = Range.closedOpen(500L, 1000L);
    private static final Range<Long> BT100MS_500MS = Range.closedOpen(100L, 500L);
    private static final Range<Long> BT10MS_100MS = Range.closedOpen(10L, 100L);
    private static final Range<Long> LT10MS = Range.closedOpen(0L, 10L);
    
    private static final Range<Long> BT5MS_10MS = Range.closedOpen(5L, 10L);
    private static final Range<Long> EQ4MS = Range.singleton(4L);
    private static final Range<Long> EQ3MS = Range.singleton(3L);
    private static final Range<Long> EQ2MS = Range.singleton(2L);
    private static final Range<Long> EQ1MS = Range.singleton(1L);
    private static final Range<Long> EQ0MS = Range.singleton(0L);
    
    @SuppressWarnings("unchecked")
    public static CounterableTIMemo memo_10ms_30S() {
        return new CounterableTIMemoImpl(new String[]{
                "1_lt10ms",
                "2_lt100ms",
                "3_lt500ms",
                "4_lt1s",
                "5_lt5s",
                "6_lt10s",
                "7_lt30s",
                "8_mt30s",
                },
                new Range[]{
                LT10MS,
                BT10MS_100MS,
                BT100MS_500MS,
                BT500MS_1S,
                BT1S_5S,
                BT5S_10S,
                BT10_30S,
                MT30S});
    }
    
    @SuppressWarnings("unchecked")
    public static CounterableTIMemo memo_0ms_30S() {
        return new CounterableTIMemoImpl(new String[]{
                "01_eq0ms",
                "02_eq1ms",
                "03_eq2ms",
                "04_eq3ms",
                "05_eq4ms",
                "06_5_10ms",
                "07_10_100ms",
                "08_100_500ms",
                "09_500ms_1s",
                "10_1_5s",
                "11_5_10s",
                "12_10_30s",
                "13_mt30s",
                },
                new Range[]{
                EQ0MS,
                EQ1MS,
                EQ2MS,
                EQ3MS,
                EQ4MS,
                BT5MS_10MS,
                BT10MS_100MS,
                BT100MS_500MS,
                BT500MS_1S,
                BT1S_5S,
                BT5S_10S,
                BT10_30S,
                MT30S});
    }
}
