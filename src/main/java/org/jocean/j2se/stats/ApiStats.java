package org.jocean.j2se.stats;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.jocean.idiom.SimpleCache;
import org.jocean.idiom.jmx.MBeanRegister;
import org.jocean.idiom.jmx.MBeanRegisterAware;
import org.jocean.j2se.jmx.MBeanUtil;
import org.jocean.j2se.stats.TIMemos.CounterableTIMemo;
import org.jocean.j2se.stats.TIMemos.OnCounter;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

import rx.functions.Action1;
import rx.functions.Action2;

public class ApiStats implements ApisMBean, MBeanRegisterAware {

     private static final String FLOWS_OBJECTNAME_SUFFIX = "name=apis";

    private static final Comparator<String> DESC_COMPARATOR = new Comparator<String>() {
        @Override
        public int compare(final String o1, final String o2) {
            return o2.compareTo(o1);
        }};

    @Override
    public void setMBeanRegister(final MBeanRegister register) {
        register.registerMBean(FLOWS_OBJECTNAME_SUFFIX,  MBeanUtil.createAndConfigureMBean(this));
        this._register = register;
    }

    public void addApi(final String path, final String method) {
        this._apis.put(path, method);
    }

    @Override
    public String[] getApisAsText() {
        final List<String> apis = new ArrayList<>();
        for ( final Map.Entry<String, Collection<String>> entry
                : this._apis.asMap().entrySet()) {
            final StringBuilder sb = new StringBuilder();
            final String path = entry.getKey();
            sb.append("[");
            sb.append(getExecutedCount(path));
            sb.append("]");
            sb.append(entry.getKey());
            sb.append("-->");
            for (final String method : entry.getValue()) {
                sb.append(method);
                sb.append("/");
            }
            final AtomicInteger idx = new AtomicInteger(1);
            fetchExecutedInterval(path, new Action2<String, Action1<OnCounter>>() {
                @Override
                public void call(final String reason, final Action1<OnCounter> memo) {
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
    public Map<String, String> getApiInfo() {
        final Map<String, String> infos = Maps.newHashMap();

        for ( final Map.Entry<String, Collection<String>> entry
                : this._apis.asMap().entrySet()) {

            final StringBuilder sb = new StringBuilder();
            for (final String method : entry.getValue()) {
                sb.append("/");
                sb.append(method);
            }
            infos.put(entry.getKey(), sb.toString());
        }

        return infos;
    }

    @Override
    public Map<String, Map<String,Object>> getApiStats() {
        final Map<String, Map<String,Object>> apis = Maps.newHashMap();
        for ( final Map.Entry<String, Collection<String>> entry
                : this._apis.asMap().entrySet()) {
            final String path = entry.getKey();
            final Map<String,Object> counters = Maps.newHashMap();
            apis.put(path, counters);

            counters.put("_TOTAL_", getExecutedCount(path));

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
        CURRENT_RECORDPATH.set(path);
        this._executedTIMemos.recordInterval(interval, path, endreason);
    }

    private void fetchExecutedInterval(final String path,
            final Action2<String, Action1<OnCounter>> receptor) {
        final Map<String, CounterableTIMemo> snapshot =
                this._executedTIMemos.fetchStatsSnapshot(path);
        for (final Map.Entry<String, CounterableTIMemo> entry : snapshot.entrySet()) {
            receptor.call(entry.getKey(), entry.getValue());
        }
    }

    private MBeanRegister _register;
    private final SimpleCache<String, AtomicInteger> _executedCounters = new SimpleCache<>( path-> new AtomicInteger(0));

    private final MultilevelStats _executedTIMemos = MultilevelStats.Util.buildStats(2, (key, cacheOrTIMemo) -> {
            if (cacheOrTIMemo instanceof TIMemoImplOfRanges) {
                _register.registerMBean(FLOWS_OBJECTNAME_SUFFIX
                        + ",path=" + CURRENT_RECORDPATH.get()
                        + ",reason=" + key,
                    ((TIMemoImplOfRanges)cacheOrTIMemo).createMBean());
            }
        });
    private final Multimap<String, String> _apis = ArrayListMultimap.create();
    private static final ThreadLocal<String> CURRENT_RECORDPATH = new ThreadLocal<>();
}
