package org.jocean.j2se.stats;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.jocean.idiom.Pair;
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

public class FlowStats implements FlowsMXBean, MBeanRegisterAware {
    
     private static final String FLOWS_OBJECTNAME_SUFFIX = "name=flows";

    private static final Comparator<String> DESC_COMPARATOR = new Comparator<String>() {
        @Override
        public int compare(final String o1, final String o2) {
            return o2.compareTo(o1);
        }};
        
    @Override
    public void setMBeanRegister(final MBeanRegister register) {
        register.registerMBean(FLOWS_OBJECTNAME_SUFFIX, this);
    }
        
    public void addFlows(final String path, final String method, final Class<Object> flowcls) {
        final Collection<Pair<String, Class<Object>>> methodAndFlows = this._apis.get(path);
        if ( !methodAndFlows.contains(Pair.of(method, flowcls)) ) {
            this._apis.put(path, Pair.of(method, flowcls));
        }
    }
    
    @Override
    public String[] getFlowsAsText() {
        final List<String> flows = new ArrayList<>();
        for ( Map.Entry<String, Collection<Pair<String, Class<Object>>>> entry 
                : this._apis.asMap().entrySet()) {
            final StringBuilder sb = new StringBuilder();
            final Class<Object> cls = entry.getValue().iterator().next().getSecond();
            sb.append("[");
            sb.append(getExecutedCount(cls));
            sb.append("]");
            sb.append(entry.getKey());
            sb.append("-->");
            for (Pair<String, Class<Object>> pair : entry.getValue()) {
                sb.append(pair.getFirst());
                sb.append("/");
            }
            sb.append(cls);
            final AtomicInteger idx = new AtomicInteger(1);
            fetchExecutedInterval(cls, new Action2<String, Action1<OnCounter>>() {
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
            flows.add(sb.toString());
        }
        
        final String[] flowsAsArray = flows.toArray(new String[0]);
        Arrays.sort(flowsAsArray, DESC_COMPARATOR);
        return flowsAsArray;
    }
    
    @Override
    public Map<String, Object> getFlows() {
        final Map<String, Object> flows = Maps.newHashMap();
        for ( Map.Entry<String, Collection<Pair<String, Class<Object>>>> entry 
                : this._apis.asMap().entrySet()) {
            final Class<Object> cls = entry.getValue().iterator().next().getSecond();
            final Map<String, Object> counters = Maps.newHashMap();
            flows.put(entry.getKey(), counters);
            
            //  total count
            final StringBuilder sb = new StringBuilder();
            sb.append(cls);
            for (Pair<String, Class<Object>> pair : entry.getValue()) {
                sb.append("/");
                sb.append(pair.getFirst());
            }
            counters.put(sb.toString(), getExecutedCount(cls));
            
            fetchExecutedInterval(cls, new Action2<String, Action1<OnCounter>>() {
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
        
        return flows;
    }
    
    public int incExecutedCount(final Class<?> cls) {
        return this._executedCounters.get(cls).incrementAndGet();
    }
    
    private int getExecutedCount(final Class<?> cls) {
        return this._executedCounters.get(cls).get();
    }
    
    public void recordExecutedInterval(final Class<?> cls, final String endreason, final long interval) {
        this._executedTIMemos.recordInterval(interval, cls, endreason);
    }
    
    private void fetchExecutedInterval(final Class<?> cls, 
            final Action2<String, Action1<OnCounter>> receptor) {
        final Map<String, CounterableTIMemo> snapshot = 
                this._executedTIMemos.fetchStatsSnapshot(cls);
        for (Map.Entry<String, CounterableTIMemo> entry : snapshot.entrySet()) {
            receptor.call(entry.getKey(), entry.getValue());
        }
    }

    private final SimpleCache<Class<?>, AtomicInteger> _executedCounters = new SimpleCache<>(
            new Func1<Class<?>, AtomicInteger>() {
        @Override
        public AtomicInteger call(final Class<?> input) {
            return new AtomicInteger(0);
        }});
    
    private final MultilevelStats _executedTIMemos = MultilevelStats.Util.buildStats(2);
    private final Multimap<String, Pair<String,Class<Object>>> _apis = ArrayListMultimap.create(); 
}
