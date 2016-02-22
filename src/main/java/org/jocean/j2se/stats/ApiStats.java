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
import org.jocean.j2se.stats.TIMemos.EmitableTIMemo;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import rx.functions.Action1;
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
    public String[] getApis() {
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
            fetchExecutedInterval(path, new Action1<String>() {
                @Override
                public void call(final String ttl) {
                    sb.append('\n');
                    sb.append('\t');
                    sb.append(ttl);
                }});
            apis.add(sb.toString());
        }
        
        final String[] apisAsArray = apis.toArray(new String[0]);
        Arrays.sort(apisAsArray, DESC_COMPARATOR);
        return apisAsArray;
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
    
    private void fetchExecutedInterval(final String path, final Action1<String> receptor) {
        final Map<String, EmitableTIMemo> snapshot = this._executedTIMemos.fetchStatsSnapshot(path);
        int idx = 1;
        for (Map.Entry<String, EmitableTIMemo> entry : snapshot.entrySet()) {
            receptor.call( "(" + Integer.toString(idx++) + ")." + entry.getKey() + ":");
            entry.getValue().emit(receptor);
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
