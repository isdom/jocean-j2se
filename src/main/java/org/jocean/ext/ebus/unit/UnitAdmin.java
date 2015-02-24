package org.jocean.ext.ebus.unit;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.PropertyFilter;
import com.alibaba.fastjson.serializer.SerializerFeature;
import org.jocean.ext.unit.PropertyConfigurerFactory;
import org.jocean.ext.unit.ValueAwarePlaceholderConfigurer;
import org.jocean.ext.util.PackageUtils;
import org.jocean.ext.util.ant.SelectorUtils;
import org.jocean.idiom.ExceptionUtils;
import org.jocean.idiom.Pair;
import org.jocean.idiom.Triple;
import org.jocean.j2se.jmx.MBeanRegisterSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.util.ReflectionUtils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class UnitAdmin implements UnitAdminMXBean {

    private final static Comparator<UnitContext> UNITCTX_ASCEND_COMPARATOR =
            new Comparator<UnitContext>() {
                @Override
                public int compare(final UnitContext o1, final UnitContext o2) {
                    return o1.getOrder() - o2.getOrder();
                }
            };

    private static final String _DEFAULT_GROUP = "default";

    private final static String[] _DEFAULT_SOURCE_PATTERNS = new String[]{
            "**/bizflow/**.xml",
            "jocean/template/transport/**.xml",
            "unitrepo/**/protocol/**.xml"};

    public static interface UnitMXBean {

        public String getUnitId();

        public String getGroup();

        public void setGroup(final String group);

        public String getName();

        public void setName(final String name);

        public String getSource();

        public Map<String, String> getParameters();

        public Map<String, String> getPlaceholders();

        public String getCreateTimestamp();

        public void setOrder(final int order);

        public int getOrder();

        public void close();
    }

    public static interface SourceMXBean {

        public Map<String, String> getPlaceholders();

    }

    private static final class StopInitCtxException extends RuntimeException {

        private static final long serialVersionUID = 1L;
    }

    private static final Logger LOG =
            LoggerFactory.getLogger(UnitAdmin.class);

    public UnitAdmin(final ApplicationContext root) {
        this._root = root;
        this._sourcePatterns = _DEFAULT_SOURCE_PATTERNS;
        this._sourcesRegister = new MBeanRegisterSupport("org.jocean:type=unitSource", null);
        this._unitsRegister = new MBeanRegisterSupport("org.jocean:type=units", null);
        //设置工程全局配置文件
        PropertyPlaceholderConfigurer rootConfigurer = this._root.getBean(PropertyPlaceholderConfigurer.class);
        if (rootConfigurer != null) {
            Field field = ReflectionUtils.findField(rootConfigurer.getClass(), "locations");
            field.setAccessible(true);
            this._rootPropertyFiles = (Resource[]) ReflectionUtils.getField(field, rootConfigurer);
        } else {
            this._rootPropertyFiles = null;
        }
    }


    public void init() {
        refreshSources();
    }

    private void refreshSources() {
        //this._unitsRegister.destroy();
        final Map<String, Map<String, String>> infos = getSourceInfo(this._sourcePatterns);
        for (Map.Entry<String, Map<String, String>> entry : infos.entrySet()) {
            final Map<String, String> placeholders = entry.getValue();
            final String suffix = "name=" + entry.getKey();
            if (!this._sourcesRegister.isRegistered(suffix)) {
                this._sourcesRegister.registerMBean(suffix, new SourceMXBean() {

                    @Override
                    public Map<String, String> getPlaceholders() {
                        return placeholders;
                    }
                });
            }
        }
    }

    public String[] getSourcePatterns() {
        return _sourcePatterns;
    }

    public void setSourcePatterns(final String[] sourcePatterns) {
        this._sourcePatterns = sourcePatterns;
    }

    @Override
    public void newUnit(final String name, final String pattern, final String[] params)
            throws Exception {
        newUnit(name, pattern, new HashMap<String, String>() {
            private static final long serialVersionUID = 1L;

            {
                //	确保偶数
                for (int idx = 0; idx < (params.length / 2) * 2; idx += 2) {
                    this.put(params[idx], params[idx + 1]);
                }
            }
        });
    }

    @Override
    public void newUnit(final String name, final String pattern, final Map<String, String> params)
            throws Exception {
        newUnit(_DEFAULT_GROUP, name, pattern, params);
    }

    @Override
    public void newUnit(final String group, final String name, final String pattern, final Map<String, String> params)
            throws Exception {
        createUnit(group, name, pattern, params, false);
    }

    public UnitMXBean createUnit(
            final String group,
            final String name,
            final String pattern,
            final Map<String, String> params,
            final boolean usingFirstWhenMatchedMultiSource) throws Exception {

        final int index = this._logidx.incrementAndGet();
        final String now = new Date().toString();

        final String[] sources = searchUnitSourceOf(new String[]{pattern});

        if (null == sources) {
            LOG.warn("can't found unit source matched {}, newUnit {}/{} failed", pattern, group, name);
            addLog(Integer.toString(index), now + ": newUnit(" + group + "/" + name
                    + ") failed for can't found source matched ("
                    + pattern + ")");
            return null;
        } else if (sources.length > 1) {
            if (usingFirstWhenMatchedMultiSource) {
                LOG.warn("found unit source more than one matched {}, using first one to create unit {}/{}",
                        pattern, group, name);
            } else {
                LOG.warn("found unit source more than one matched {}, failed to create unit {}/{}", pattern, group, name);
                addLog(Integer.toString(index), now + ": newUnit(" + group + "/" + name
                        + ") failed for found unit source > 1 "
                        + pattern);
                return null;
            }
        }

        final String objectNameSuffix = genUnitSuffix(group, name);
        final Object mock = newMockUnitMXBean(group, name);
        if (!reserveRegistration(objectNameSuffix, mock)) {
            addLog(Integer.toString(index), now + ": newUnit("
                    + group + "/" + name
                    + ") failed for can't reserve "
                    + objectNameSuffix);
            throw new RuntimeException("can't reserve " + objectNameSuffix + ", may be already registered");
        }

        try {
            final ValueAwarePlaceholderConfigurer configurer =
                    new ValueAwarePlaceholderConfigurer() {
                        {
                            this.setProperties(new Properties() {
                                private static final long serialVersionUID = 1L;

                                {
                                    this.putAll(params);
                                }
                            });

                        }
                    };

            if (this._rootPropertyFiles != null) {
                configurer.setLocations(this._rootPropertyFiles);
                configurer.setLocalOverride(true);//params(功能单元中的配置)覆盖全局配置
            }

            final AbstractApplicationContext ctx =
                    createConfigurableApplicationContext(sources[0], configurer);

            final UUID uid = UUID.randomUUID();
            final UnitMXBean unit =
                    newUnitMXBean(
                            uid,
                            this._order.getAndAdd(10),
                            group,
                            name,
                            sources[0],
                            now,
                            params,
                            configurer.getTextedResolvedPlaceholders());

            this._units.put(uid, Triple.of(group, name, ctx));

            this._unitsRegister.replaceRegisteredMBean(objectNameSuffix, mock, unit);

            addLog(Integer.toString(index), now + ": newUnit(" + group + "/" + name + ") succeed.");

            return unit;
        } catch (Exception e) {
            this._unitsRegister.unregisterMBean(objectNameSuffix);
            addLog(Integer.toString(index), now + ": newUnit(" + group + "/" + name + ") failed for "
                    + ExceptionUtils.exception2detail(e));
            throw e;
        }
    }

    /**
     * @param uid
     * @param order
     * @param group
     * @param name
     * @param source
     * @param now
     * @param params
     * @param placeholders
     * @return
     */
    private UnitMXBean newUnitMXBean(
            final UUID uid,
            final int order,
            final String group,
            final String name,
            final String source,
            final String now,
            final Map<String, String> params,
            final Map<String, String> placeholders) {
        return new UnitMXBean() {

            @Override
            public String getSource() {
                return source;
            }

            @Override
            public Map<String, String> getParameters() {
                return params;
            }

            @Override
            public Map<String, String> getPlaceholders() {
                return placeholders;
            }

            @Override
            public String getCreateTimestamp() {
                return now;
            }

            @Override
            public void close() {
                deleteUnit(uid);
            }

            @Override
            public int getOrder() {
                return order;
            }

            @Override
            public String getGroup() {
                return group;
            }

            @Override
            public String getName() {
                return name;
            }

            @Override
            public void setGroup(final String _group) {
                if (!_group.equals(group)) {
                    if (_unitsRegister.registerMBean(
                            genUnitSuffix(_group, name),
                            newUnitMXBean(
                                    uid,
                                    order,
                                    _group,
                                    name,
                                    source,
                                    now,
                                    params,
                                    placeholders))) {
                        _units.put(uid, Triple.of(_group, name, uid2AbstractApplicationContext(uid)));
                        _unitsRegister.unregisterMBean(genUnitSuffix(group, name));
                    }
                }
            }

            @Override
            public void setName(final String _name) {
                if (!_name.equals(name)) {
                    if (_unitsRegister.registerMBean(
                            genUnitSuffix(group, _name),
                            newUnitMXBean(
                                    uid,
                                    order,
                                    group,
                                    _name,
                                    source,
                                    now,
                                    params,
                                    placeholders))) {
                        _units.put(uid, Triple.of(group, _name, uid2AbstractApplicationContext(uid)));
                        _unitsRegister.unregisterMBean(genUnitSuffix(group, name));
                    }
                }
            }

            @Override
            public void setOrder(final int _order) {
                if (_order != order) {
                    _unitsRegister.replaceRegisteredMBean(
                            genUnitSuffix(group, name), this,
                            newUnitMXBean(
                                    uid,
                                    _order,
                                    group,
                                    name,
                                    source,
                                    now,
                                    params,
                                    placeholders));
                }
            }

            @Override
            public String getUnitId() {
                return uid.toString();
            }
        };
    }

    private AbstractApplicationContext uid2AbstractApplicationContext(final UUID uid) {
        final Triple<String, String, AbstractApplicationContext> triple = this._units.get(uid);
        return (null != triple ? triple.getThird() : null);
    }

    private Pair<String, String> uid2GroupAndName(final UUID uid) {
        final Triple<String, String, AbstractApplicationContext> triple = this._units.get(uid);
        return (null != triple ? Pair.of(triple.getFirst(), triple.getSecond()) : null);
    }

    private UUID groupAndName2Uid(final String group, final String name) {
        for (Map.Entry<UUID, Triple<String, String, AbstractApplicationContext>> entry
                : this._units.entrySet()) {
            if (entry.getValue().getFirst().equals(group)
                    && entry.getValue().getSecond().equals(name)) {
                return entry.getKey();
            }
        }
        LOG.warn("can't found unit id matched group={},name={}", group, name);
        return null;
    }

    /**
     * @param group
     * @param name
     * @return
     */
    private String genUnitSuffix(final String group, final String name) {
        return "group=" + group + ",name=" + name;
    }

    private boolean reserveRegistration(final String objectNameSuffix, final Object mock) {
        return this._unitsRegister.registerMBean(objectNameSuffix, mock);
    }

    private Object newMockUnitMXBean(final String group, final String name) {
        return new UnitMXBean() {

            @Override
            public String getSource() {
                return null;
            }

            @Override
            public Map<String, String> getParameters() {
                return null;
            }

            @Override
            public Map<String, String> getPlaceholders() {
                return null;
            }

            @Override
            public String getCreateTimestamp() {
                return null;
            }

            @Override
            public int getOrder() {
                return 0;
            }

            @Override
            public void close() {
            }

            @Override
            public String getGroup() {
                return group;
            }

            @Override
            public String getName() {
                return name;
            }

            @Override
            public void setGroup(final String group) {
            }

            @Override
            public void setName(final String name) {
            }

            @Override
            public void setOrder(int seq) {
            }

            @Override
            public String getUnitId() {
                return null;
            }
        };
    }

    public void deleteUnit(final UUID uid) {
        final Pair<String, String> pair = uid2GroupAndName(uid);
        final int index = this._logidx.incrementAndGet();
        if (null != pair) {
            this._unitsRegister.unregisterMBean(genUnitSuffix(pair.getFirst(), pair.getSecond()));
            final Triple<String, String, AbstractApplicationContext> triple = this._units.remove(uid);

            if (null != triple) {
                final AbstractApplicationContext ctx = triple.getThird();
                if (null != ctx) {
                    ctx.close();
                    addLog(Integer.toString(index),
                            new Date().toString() + ": deleteUnit(group=" + pair.getFirst()
                                    + ",name=" + pair.getSecond()
                                    + ") succeed.)");
                } else {
                    LOG.warn("unit id {}'s AbstractApplicationContext is null, unit maybe deleted already", uid);
                    addLog(Integer.toString(index),
                            new Date().toString() + ": deleteUnit(group=" + pair.getFirst()
                                    + ",name=" + pair.getSecond()
                                    + ") failed for AbstractApplicationContext is null.)");
                }
            } else {
                LOG.warn("can't found AbstractApplicationContext matched unit id {}, unit maybe deleted already", uid);
                addLog(Integer.toString(index),
                        new Date().toString() + ": deleteUnit(group=" + pair.getFirst()
                                + ",name=" + pair.getSecond()
                                + ") failed for can't found AbstractApplicationContext matched unit id " + uid + ".)");
            }
        } else {
            LOG.warn("can't found group and name matched unit id {}, unit maybe deleted already", uid);
            addLog(Integer.toString(index),
                    new Date().toString() + ": deleteUnit(" + uid
                            + ") failed for can't found group and name matched unit id " + uid + ".)");
        }
    }

    @Override
    public void deleteUnit(final String name) {
        deleteUnit(_DEFAULT_GROUP, name);
    }

    @Override
    public void deleteUnit(final String group, final String name) {
        deleteUnit(groupAndName2Uid(group, name));
    }

    @Override
    public void deleteAllUnit() {
        //	remove and close all unit
        while (!this._units.isEmpty()) {
            final UUID uid = this._units.keySet().iterator().next();
            deleteUnit(uid);
        }
    }

    @Override
    public Map<String, Map<String, String>> getSourceInfo(final String sourcePattern) {
        return getSourceInfo(new String[]{sourcePattern});
    }

    public Map<String, Map<String, String>> getSourceInfo(final String[] sourcePatterns) {
        final String[] sources = searchUnitSourceOf(sourcePatterns);

        final Map<String, Map<String, String>> infos = new HashMap<>();

        if (null == sources) {
            LOG.warn("can't found unit source matched {}, getSourcesInfo failed", Arrays.toString(sourcePatterns));
            return infos;
        }

        for (String source : sources) {
            final String unitSource = source;
            final ValueAwarePlaceholderConfigurer configurer =
                    new ValueAwarePlaceholderConfigurer() {
                        {
                            this.setIgnoreUnresolvablePlaceholders(true);
                        }

                        @Override
                        protected void processProperties(ConfigurableListableBeanFactory beanFactoryToProcess, Properties props)
                                throws BeansException {
                            super.processProperties(beanFactoryToProcess, props);
                            if (LOG.isDebugEnabled()) {
                                LOG.debug("after processProperties for source {}", unitSource);
                            }
                            throw new StopInitCtxException();
                        }
                    };

            try {
                createConfigurableApplicationContext(source, configurer);
            } catch (StopInitCtxException ignored) {
            }
            infos.put(unitSource, configurer.getTextedResolvedPlaceholders());
        }

        return infos;
    }

    @Override
    public String[] getLogs() {
        return this._logs.toArray(new String[this._logs.size()]);
    }

    @Override
    public void resetLogs() {
        this._logs.clear();
    }

    private void addLog(String id, String msg) {
        this._logs.add(id + ":" + msg + "\r\n");
    }

    /**
     * @param unitCtxs
     */
    private Map<String, List<UnitContext>> units2UnitContexts(
            final Map<String, List<UnitContext>> unitCtxs) {
        for (Triple<String, String, AbstractApplicationContext> triple : this._units.values()) {
            final UnitMXBean unit = (UnitMXBean) _unitsRegister.getMBean(
                    genUnitSuffix(triple.getFirst(), triple.getSecond()));

            if (null != unit) {
                final List<UnitContext> ctxs = getUnitContexts(unitCtxs, unit.getGroup());

                ctxs.add(new UnitContext()
                                //.setGroup(unit.getGroup())
                                .setName(unit.getName())
                                .setParams(unit.getParameters())
                                .setPlaceholders(unit.getPlaceholders())
                                .setSource(unit.getSource())
                                .setOrder(unit.getOrder())
                );
            }
        }

        return unitCtxs;
    }

    private List<UnitContext> getUnitContexts(
            final Map<String, List<UnitContext>> unitCtxs, final String group) {
        List<UnitContext> ret = unitCtxs.get(group);
        if (null == ret) {
            ret = new ArrayList<>();
            unitCtxs.put(group, ret);
        }
        return ret;
    }

    private int createUnitsByContexts(final List<UnitContext> allctx) {
        int unitsCreated = 0;
        for (UnitContext ctx : allctx) {
            try {
                if (null != createUnit(ctx.getGroup(), ctx.getName(), ctx.getSource(), ctx.getParams(), true)) {
                    unitsCreated++;
                }
            } catch (Exception e) {
                LOG.error("exception when createUnit for {}, detail:{}",
                        ctx, ExceptionUtils.exception2detail(e));
            }
        }

        return unitsCreated;
    }

    private String genGroupString(final File cfg) {
        if (cfg.getName().endsWith(".json")) {
            return cfg.getName().substring(0, cfg.getName().length() - 5);
        } else {
            return cfg.getName();
        }
    }

    @Override
    public String genUnitsOrderAsJson() {

        final List<UnitContext> ctxs = new ArrayList<>();

        for (Triple<String, String, AbstractApplicationContext> triple : this._units.values()) {
            final UnitMXBean unit = (UnitMXBean) _unitsRegister.getMBean(
                    genUnitSuffix(triple.getFirst(), triple.getSecond()));

            if (null != unit) {
                ctxs.add(new UnitContext()
                                .setGroup(unit.getGroup())
                                .setName(unit.getName())
                                .setOrder(unit.getOrder())
                );
            }
        }

        Collections.sort(ctxs, UNITCTX_ASCEND_COMPARATOR);

        return JSON.toJSONString(ctxs, new PropertyFilter() {

            @Override
            public boolean apply(final Object source, final String name, final Object value) {
                return "group".equals(name) || "name".equals(name);
            }
        }, SerializerFeature.PrettyFormat);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void resetUnitsOrderByJson(final String json) {

        final UnitContext[] ctxs = JSON.parseObject(json, UnitContext[].class);

        final Object[] triples = this._units.values().toArray();

        //	reserved new unit order
        this._order.set(triples.length * 10);

        final List<Pair<String, String>> orgs = new ArrayList<Pair<String, String>>() {
            private static final long serialVersionUID = 1L;

            {
                for (Object o : triples) {
                    final Triple<String, String, AbstractApplicationContext> triple =
                            (Triple<String, String, AbstractApplicationContext>) o;

                    this.add(Pair.of(triple.getFirst(), triple.getSecond()));
                }
            }
        };


        int order = 0;

        for (UnitContext ctx : ctxs) {
            order = resetUnitOrder(order, ctx.getGroup(), ctx.getName());

            orgs.remove(Pair.of(ctx.getGroup(), ctx.getName()));
        }

        //	deal with left group and name pair
        for (Pair<String, String> pair : orgs) {
            order = resetUnitOrder(order, pair.getFirst(), pair.getSecond());
        }
    }

    /**
     * @param order
     * @param group
     * @return
     */
    private int resetUnitOrder(final int order, final String group, final String name) {
        final UnitMXBean unit = (UnitMXBean) this._unitsRegister.getMBean(genUnitSuffix(group, name));
        if (null != unit) {
            unit.setOrder(order);
            return order + 10;
        } else {
            LOG.warn("can't found unit with group={},name={}, reset unit's order failed", group, name);
            return order;
        }
    }


    @Override
    public String genUnitContextsAsJson() {

        final Map<String, List<UnitContext>> unitCtxs =
                units2UnitContexts(new HashMap<String, List<UnitContext>>());

        for (Map.Entry<String, List<UnitContext>> entry : unitCtxs.entrySet()) {

            Collections.sort(entry.getValue(), UNITCTX_ASCEND_COMPARATOR);
        }

        return JSON.toJSONString(unitCtxs, SerializerFeature.PrettyFormat);
    }

    @Override
    public void createUnitsFromJson(final String json) {

        final Map<String, UnitContext[]> unitCtxs =
                JSON.parseObject(json, new ParameterizedType() {

                    @Override
                    public Type[] getActualTypeArguments() {
                        //	key, value
                        return new Type[]{String.class, UnitContext[].class};
                    }

                    @Override
                    public Type getRawType() {
                        return HashMap.class;
                    }

                    @Override
                    public Type getOwnerType() {
                        return null;
                    }
                });

        final int count = createUnitsByContexts(
                new ArrayList<UnitContext>() {
                    private static final long serialVersionUID = 1L;

                    {
                        //	fill group field
                        for (Map.Entry<String, UnitContext[]> entry : unitCtxs.entrySet()) {
                            for (UnitContext ctx : entry.getValue()) {
                                ctx.setGroup(entry.getKey());
                                this.add(ctx);
                            }
                        }
                        Collections.sort(this, UNITCTX_ASCEND_COMPARATOR);
                    }
                }
        );
        if (LOG.isInfoEnabled()) {
            LOG.info("create {} units from json.", count);
        }
    }

    /**
     * @param unitSource
     * @param configurer
     * @return
     */
    private AbstractApplicationContext createConfigurableApplicationContext(
            final String unitSource,
            final PropertyPlaceholderConfigurer configurer) {
        final AbstractApplicationContext topCtx =
                new ClassPathXmlApplicationContext(
                        new String[]{"org/jocean/ext/ebus/spring/unitParent.xml"}, this._root);

        final PropertyConfigurerFactory factory =
                topCtx.getBean(PropertyConfigurerFactory.class);

        factory.setConfigurer(configurer);

        final AbstractApplicationContext ctx =
                new ClassPathXmlApplicationContext(
                        new String[]{
                                "org/jocean/ext/ebus/spring/Configurable.xml",
                                unitSource},
                        topCtx);
        return ctx;
    }

    private String[] searchUnitSourceOf(final String[] sourcePatterns) {

        if (LOG.isDebugEnabled()) {
            LOG.debug("try to match pattern: {}", Arrays.toString(sourcePatterns));
        }
        final List<String> sources = new ArrayList<>();
        try {
            final Map<URL, String[]> allRes = PackageUtils.getAllCPResourceAsPathlike();
            for (Map.Entry<URL, String[]> entry : allRes.entrySet()) {
                for (String res : entry.getValue()) {
                    if (LOG.isTraceEnabled()) {
                        LOG.trace("found resource: {}", res);
                    }
                    for (String pattern : sourcePatterns) {
                        if (SelectorUtils.match(pattern, res)) {
                            if (LOG.isDebugEnabled()) {
                                LOG.debug("found matched unit source: {}", res);
                            }
                            sources.add(res);
                        }
                    }
                }
            }
        } catch (IOException e) {
            LOG.error("exception when search pattern {}, detail : {}", Arrays.toString(sourcePatterns),
                    ExceptionUtils.exception2detail(e));
        }

        return sources.isEmpty() ? null : sources.toArray(new String[sources.size()]);
    }

    private String[] _sourcePatterns;

    private final Resource[] _rootPropertyFiles;

    private final MBeanRegisterSupport _sourcesRegister;

    private final ApplicationContext _root;

    private final MBeanRegisterSupport _unitsRegister;

    private final Map<UUID, Triple<String, String, AbstractApplicationContext>> _units = new ConcurrentHashMap<>();

    private final AtomicInteger _logidx = new AtomicInteger(0);

    private final AtomicInteger _order = new AtomicInteger(0);

    private final Queue<String> _logs = new ConcurrentLinkedQueue<>();
}
