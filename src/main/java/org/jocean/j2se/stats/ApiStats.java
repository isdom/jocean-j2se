package org.jocean.j2se.stats;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.jocean.idiom.SimpleCache;
import org.jocean.j2se.jmx.MBeanRegister;
import org.jocean.j2se.jmx.MBeanRegisterAware;
import org.jocean.j2se.stats.TIMemos.CounterableTIMemo;
import org.jocean.j2se.stats.TIMemos.OnCounter;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

import rx.functions.Action1;
import rx.functions.Action2;
import rx.functions.Func1;

public class ApiStats implements ApisMXBean, MBeanRegisterAware {
    
     private static final String FLOWS_OBJECTNAME_SUFFIX = "name=apis";

    private static final Comparator<String> DESC_COMPARATOR = new Comparator<String>() {
        @Override
        public int compare(final String o1, final String o2) {
            return o2.compareTo(o1);
        }};
        
    @Override
    public void setMBeanRegister(final MBeanRegister register) {
        register.registerMBean(FLOWS_OBJECTNAME_SUFFIX, this);
    }
        
    public void addApi(final String path, final String method) {
        this._apis.put(path, method);
    }
    
    @Override
    public String[] getApisAsText() {
        final List<String> apis = new ArrayList<>();
        for ( Map.Entry<String, Collection<String>> entry 
                : this._apis.asMap().entrySet()) {
            final StringBuilder sb = new StringBuilder();
            final String path = entry.getKey();
            sb.append("[");
            sb.append(getExecutedCount(path));
            sb.append("]");
            sb.append(entry.getKey());
            sb.append("-->");
            for (String method : entry.getValue()) {
                sb.append(method);
                sb.append("/");
            }
            final AtomicInteger idx = new AtomicInteger(1);
            fetchExecutedInterval(path, new Action2<String, Action1<OnCounter>>() {
                @Override
                public void call(final String reason, Action1<OnCounter> memo) {
                    sb.append('\n');
                    sb.append('\t');
                    sb.append("(" + Integer.toString(idx.getAndIncrement()) + ")." + reason + ":");
                    memo.call(new OnCounter() {
                        @Override
                        public void call(final String name, final Integer counter) {
                            sb.append('\n');
                            sb.append("\t\t");
                            sb.append(name);
                            sb.append(':');
                            sb.append(counter);
                        }});
                }});
            apis.add(sb.toString());
        }
        
        final String[] apisAsArray = apis.toArray(new String[0]);
        Arrays.sort(apisAsArray, DESC_COMPARATOR);
        return apisAsArray;
    }
    
    @Override
    public Map<String, Map<String,Map<String, Integer>>> getApis() {
        final Map<String, Map<String,Map<String, Integer>>> apis = Maps.newHashMap();
        for ( Map.Entry<String, Collection<String>> entry 
                : this._apis.asMap().entrySet()) {
            final String path = entry.getKey();
            final Map<String,Map<String, Integer>> counters = Maps.newHashMap();
            apis.put(path, counters);
            
            //  total count
            final StringBuilder sb = new StringBuilder();
            for (String method : entry.getValue()) {
                sb.append("/");
                sb.append(method);
            }
            final Map<String, Integer> total = Maps.newHashMap();
            total.put(sb.toString(), getExecutedCount(path));
            counters.put("_TOTAL_", total);
            
            fetchExecutedInterval(path, new Action2<String, Action1<OnCounter>>() {
                @Override
                public void call(final String reason, final Action1<OnCounter> memo) {
                    final Map<String, Integer> ttlCounters = Maps.newHashMap();
                    counters.put(reason, ttlCounters);
                    memo.call(new OnCounter() {
                        @Override
                        public void call(final String name, final Integer counter) {
                            ttlCounters.put(name, counter);
                        }});
                }});
        }
        
        return apis;
    }
    
    public int incExecutedCount(final String path) {
        return this._executedCounters.get(path).incrementAndGet();
    }
    
    private int getExecutedCount(final String path) {
        return this._executedCounters.get(path).get();
    }
    
    public void recordExecutedInterval(final String path, final String endreason, final long interval) {
        this._executedTIMemos.recordInterval(interval, path, endreason);
    }
    
    private void fetchExecutedInterval(final String path, 
            final Action2<String, Action1<OnCounter>> receptor) {
        final Map<String, CounterableTIMemo> snapshot = 
                this._executedTIMemos.fetchStatsSnapshot(path);
        for (Map.Entry<String, CounterableTIMemo> entry : snapshot.entrySet()) {
            receptor.call(entry.getKey(), entry.getValue());
        }
    }

    private final SimpleCache<String, AtomicInteger> _executedCounters = new SimpleCache<>(
            new Func1<String, AtomicInteger>() {
        @Override
        public AtomicInteger call(final String path) {
            return new AtomicInteger(0);
        }});
    
    private final MultilevelStats _executedTIMemos = MultilevelStats.Util.buildStats(2);
    private final Multimap<String, String> _apis = ArrayListMultimap.create(); 
}
