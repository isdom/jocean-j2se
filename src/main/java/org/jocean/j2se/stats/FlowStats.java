package org.jocean.j2se.stats;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import javax.management.JMException;
import javax.management.MBeanException;
import javax.management.modelmbean.ModelMBean;
import javax.management.modelmbean.ModelMBeanInfo;
import javax.management.modelmbean.RequiredModelMBean;

import org.jocean.idiom.Pair;
import org.jocean.idiom.SimpleCache;
import org.jocean.idiom.stats.TimeIntervalMemo;
import org.jocean.j2se.jmx.MBeanRegister;
import org.jocean.j2se.jmx.MBeanRegisterAware;
import org.jocean.j2se.stats.TIMemos.CounterableTIMemo;
import org.jocean.j2se.stats.TIMemos.OnCounter;
import org.springframework.jmx.export.MBeanExportException;
import org.springframework.jmx.export.assembler.MBeanInfoAssembler;
import org.springframework.jmx.export.assembler.SimpleReflectiveMBeanInfoAssembler;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

import rx.functions.Action1;
import rx.functions.Action2;
import rx.functions.Func1;

public class FlowStats implements FlowsMBean, MBeanRegisterAware {
    
    private static final String FLOWS_OBJECTNAME_SUFFIX = "name=flows";

    private final static MBeanInfoAssembler _ASSEMBLER = 
            new SimpleReflectiveMBeanInfoAssembler();
    
    private static final Comparator<String> DESC_COMPARATOR = new Comparator<String>() {
        @Override
        public int compare(final String o1, final String o2) {
            return o2.compareTo(o1);
        }};
        
    private static ModelMBeanInfo getMBeanInfo(final Object managedBean, final String beanKey) 
            throws JMException {
        final ModelMBeanInfo info = _ASSEMBLER.getMBeanInfo(managedBean, beanKey);
//        if (logger.isWarnEnabled() && ObjectUtils.isEmpty(info.getAttributes()) &&
//                ObjectUtils.isEmpty(info.getOperations())) {
//            logger.warn("Bean with key '" + beanKey +
//                    "' has been registered as an MBean but has no exposed attributes or operations");
//        }
        return info;
    }
        
    private static ModelMBean createModelMBean() throws MBeanException {
        return new RequiredModelMBean();
//        return (this.exposeManagedResourceClassLoader ? new SpringModelMBean() : new RequiredModelMBean());
    }
    
    private static ModelMBean createAndConfigureMBean(final Object managedResource, String beanKey)
            throws MBeanExportException {
        try {
            final ModelMBean mbean = createModelMBean();
            mbean.setModelMBeanInfo(getMBeanInfo(managedResource, beanKey));
            mbean.setManagedResource(managedResource, "ObjectReference");
            return mbean;
        }
        catch (Exception ex) {
            throw new MBeanExportException("Could not create ModelMBean for managed resource [" +
                    managedResource + "] with key '" + beanKey + "'", ex);
        }
    }
    
    @Override
    public void setMBeanRegister(final MBeanRegister register) {
        register.registerMBean(FLOWS_OBJECTNAME_SUFFIX, 
                createAndConfigureMBean(this, this.getClass().getName()));
        this._register = register;
    }
        
    public void addFlows(final String path, final String method, final Class<Object> flowcls) {
        final Collection<Pair<String, Class<Object>>> methodAndFlows = this._apis.get(path);
        if ( !methodAndFlows.contains(Pair.of(method, flowcls)) ) {
            this._apis.put(path, Pair.of(method, flowcls));
        }
        this._cls2path.putIfAbsent(flowcls, path);
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
    public Map<String, String> getFlowInfo() {
        final Map<String, String> infos = Maps.newHashMap();
        
        for ( Map.Entry<String, Collection<Pair<String, Class<Object>>>> entry 
                : this._apis.asMap().entrySet()) {
            final Class<Object> cls = entry.getValue().iterator().next().getSecond();
            
            final StringBuilder sb = new StringBuilder();
            sb.append(cls.getName());
            for (Pair<String, Class<Object>> pair : entry.getValue()) {
                sb.append("/");
                sb.append(pair.getFirst());
            }
            infos.put(entry.getKey(), sb.toString());
        }
        
        return infos;
    }
    
    @Override
    public Map<String, Map<String,Object>> getFlowStats() {
        final Map<String, Map<String,Object>> flows = Maps.newHashMap();
        for ( Map.Entry<String, Collection<Pair<String, Class<Object>>>> entry 
                : this._apis.asMap().entrySet()) {
            final Class<Object> cls = entry.getValue().iterator().next().getSecond();
            final Map<String, Object> counters = Maps.newHashMap();
            flows.put(entry.getKey(), counters);
            
            counters.put("_TOTAL_", getExecutedCount(cls));
            
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

    private MBeanRegister _register;
    private final ConcurrentMap<Class<?>,String>  _cls2path = Maps.newConcurrentMap();
    
    private final SimpleCache<Class<?>, AtomicInteger> _executedCounters = new SimpleCache<>(
            new Func1<Class<?>, AtomicInteger>() {
        @Override
        public AtomicInteger call(final Class<?> input) {
            return new AtomicInteger(0);
        }});
    
    private final MultilevelStats _executedTIMemos = MultilevelStats.Util.buildStats(2, 
        new Action2<Object, Object>() {
            @Override
            public void call(final Object key, final Object cacheOrTIMemo) {
                if (cacheOrTIMemo instanceof TimeIntervalMemo) {
                    if ( cacheOrTIMemo instanceof TIMemoImplOfRanges) {
                        _register.registerMBean(FLOWS_OBJECTNAME_SUFFIX 
                                + ",path=" + _cls2path.get(CURRENT_FLOWCLS.get())
                                + ",reason=" + key, 
                            ((TIMemoImplOfRanges)cacheOrTIMemo).createMBean());
                    }
                } else {
                    CURRENT_FLOWCLS.set((Class<?>)key);
                }
            }});
    private final Multimap<String, Pair<String,Class<Object>>> _apis = ArrayListMultimap.create(); 
    
    private static final ThreadLocal<Class<?>> CURRENT_FLOWCLS = new ThreadLocal<Class<?>>();
}
