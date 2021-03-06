package org.jocean.j2se.unit;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.io.FilenameUtils;
import org.jocean.idiom.BeanHolder;
import org.jocean.idiom.COWCompositeSupport;
import org.jocean.idiom.ExceptionUtils;
import org.jocean.idiom.jmx.MBeanRegister;
import org.jocean.idiom.jmx.MBeanRegisterAware;
import org.jocean.j2se.annotation.Updatable;
import org.jocean.j2se.jmx.MBeanRegisterSetter;
import org.jocean.j2se.jmx.MBeanRegisterSupport;
import org.jocean.j2se.spring.BeanHolderBasedFieldInjector;
import org.jocean.j2se.spring.BeanHolderBasedMethodInjector;
import org.jocean.j2se.spring.BeanHolderSetter;
import org.jocean.j2se.spring.FieldValueSetter;
import org.jocean.j2se.spring.MethodValueSetter;
import org.jocean.j2se.spring.PropertiesResourceSetter;
import org.jocean.j2se.spring.PropertyPlaceholderConfigurerSetter;
import org.jocean.j2se.spring.SpringBeanHolder;
import org.jocean.j2se.spring.UnitAgentAware;
import org.jocean.j2se.spring.UnitKeeperAwareHolder;
import org.jocean.j2se.util.PackageUtils;
import org.jocean.j2se.util.SelectorUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ApplicationContextEvent;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringValueResolver;

public class UnitAgent implements MBeanRegisterAware, UnitAgentMXBean, ApplicationContextAware, SpringBeanHolder {

    private static final Node[] EMPTY_NODE = new Node[0];

    private final static String[] _DEFAULT_SOURCE_PATTERNS = new String[]{"**/units/**.xml"};

    private static final class StopInitCtxException extends RuntimeException {

        private static final long serialVersionUID = 1L;
    }

    private static final Logger LOG = LoggerFactory.getLogger(UnitAgent.class);

    public UnitAgent() {
        this._sourcePatterns = _DEFAULT_SOURCE_PATTERNS;
        this._sourcesRegister = new MBeanRegisterSupport("org.jocean:type=unitSource", null);
        //this._unitsRegister = new MBeanRegisterSupport("org.jocean:type=units", null);
    }

    public void addUnitListener(final UnitListener listener) {
        if ( null == listener ) {
            LOG.warn("addUnitListener: listener is null, just ignore");
        }
        else {
            if ( !this._unitListenerSupport.addComponent(listener) ) {
                LOG.warn("addUnitListener: listener {} has already added", listener);
            }
        }
    }

    public void removeUnitListener(final UnitListener listener) {
        if ( null == listener ) {
            LOG.warn("removeUnitListener: listener is null, just ignore");
        }
        else {
            this._unitListenerSupport.removeComponent(listener);
        }
    }

    @Override
    public void setApplicationContext(final ApplicationContext applicationContext)
            throws BeansException {
        this._rootApplicationContext = applicationContext;
        //设置工程全局配置文件
        final PropertyPlaceholderConfigurer rootConfigurer =
                null != this._rootApplicationContext ? getRootConfigurer() : null;
        if (rootConfigurer != null) {
            final Field field = ReflectionUtils.findField(rootConfigurer.getClass(), "locations");
            field.setAccessible(true);
            this._rootPropertyFiles = (Resource[]) ReflectionUtils.getField(field, rootConfigurer);
        } else {
            this._rootPropertyFiles = null;
        }
        this._beanFinder = this._rootApplicationContext.getBean(DefaultBeanFinder.class);
    }

    private PropertyPlaceholderConfigurer getRootConfigurer() {
        try {
            return this._rootApplicationContext.getBean(PropertyPlaceholderConfigurer.class);
        } catch (final Exception e) {
            return null;
        }
    }

    @Override
    public <T> T getBean(final Class<T> requiredType) {
        T bean = null;
        if (null != this._rootApplicationContext) {
            bean = getBeanOf(this._rootApplicationContext, requiredType);
        }
        if (null != bean) {
            return bean;
        }
        for ( final Node node : this._units.values()) {
            if (null != node._applicationContext) {
                bean = getBeanOf(node._applicationContext, requiredType);
                if (null != bean) {
                    return bean;
                }
            }
            if (null != node._unitAgent) {
                bean = node._unitAgent.getBean(requiredType);
                if (null != bean) {
                    return bean;
                }
            }
        }
        return null;
    }

    @Override
    public <T> T getBean(final String name, final Class<T> requiredType) {
        T bean = null;
        if (null != this._rootApplicationContext) {
            bean = getBeanOf(this._rootApplicationContext, name, requiredType);
        }
        if (null != bean) {
            return bean;
        }
        for ( final Node node : this._units.values()) {
            if (null != node._applicationContext) {
                bean = getBeanOf(node._applicationContext, name, requiredType);
                if (null != bean) {
                    return bean;
                }
            }
            if (null != node._unitAgent) {
                bean = node._unitAgent.getBean(name, requiredType);
                if (null != bean) {
                    return bean;
                }
            }
        }
        return null;
    }

    @Override
    public Object getBean(final String name) {
        Object bean = null;
        if (null != this._rootApplicationContext) {
            bean = getBeanOf(this._rootApplicationContext, name);
        }
        if (null != bean) {
            return bean;
        }
        for ( final Node node : this._units.values()) {
            if (null != node._applicationContext) {
                bean = getBeanOf(node._applicationContext, name);
                if (null != bean) {
                    return bean;
                }
            }
            if (null != node._unitAgent) {
                bean = node._unitAgent.getBean(name);
                if (null != bean) {
                    return bean;
                }
            }
        }
        return null;
    }

    @Override
    public Object getBean(final String name, final Object... args) {
        Object bean = null;
        if (null != this._rootApplicationContext) {
            bean = getBeanOf(this._rootApplicationContext, name, args);
        }
        if (null != bean) {
            return bean;
        }
        for ( final Node node : this._units.values()) {
            if (null != node._applicationContext) {
                bean = getBeanOf(node._applicationContext, name, args);
                if (null != bean) {
                    return bean;
                }
            }
            if (null != node._unitAgent) {
                bean = node._unitAgent.getBean(name, args);
                if (null != bean) {
                    return bean;
                }
            }
        }
        return null;
    }

    @Override
    public <T> T getBean(final Class<T> requiredType, final Object... args) {
        T bean = null;
        if (null != this._rootApplicationContext) {
            bean = getBeanOf(this._rootApplicationContext, requiredType, args);
        }
        if (null != bean) {
            return bean;
        }
        for ( final Node node : this._units.values()) {
            if (null != node._applicationContext) {
                bean = getBeanOf(node._applicationContext, requiredType, args);
                if (null != bean) {
                    return bean;
                }
            }
            if (null != node._unitAgent) {
                bean = node._unitAgent.getBean(requiredType, args);
                if (null != bean) {
                    return bean;
                }
            }
        }
        return null;
    }

    private static <T> T getBeanOf(final BeanFactory factory, final Class<T> requiredType) {
        try {
            return factory.getBean(requiredType);
        } catch (final Exception e) {
            if (!(e instanceof NoSuchBeanDefinitionException)) {
                LOG.warn("exception when get ({}) bean from ({}), detail:{}",
                        requiredType, factory, ExceptionUtils.exception2detail(e));
            }
            return null;
        }
    }

    private static <T> T getBeanOf(final BeanFactory factory, final String name, final Class<T> requiredType) {
        try {
            return factory.getBean(name, requiredType);
        } catch (final Exception e) {
            if (!(e instanceof NoSuchBeanDefinitionException)) {
                LOG.warn("exception when get ({}/{}) bean from ({}), detail:{}",
                        name, requiredType, factory, ExceptionUtils.exception2detail(e));
            }
            return null;
        }
    }

    private static Object getBeanOf(final BeanFactory factory, final String name) {
        try {
            return factory.getBean(name);
        } catch (final Exception e) {
            if (!(e instanceof NoSuchBeanDefinitionException)) {
                LOG.warn("exception when get ({}) bean from ({}), detail:{}",
                        name, factory, ExceptionUtils.exception2detail(e));
            }
            return null;
        }
    }

    private static Object getBeanOf(final BeanFactory factory, final String name, final Object... args) {
        try {
            return factory.getBean(name, args);
        } catch (final Exception e) {
            if (!(e instanceof NoSuchBeanDefinitionException)) {
                LOG.warn("exception when get ({}) bean with args from ({}), detail:{}",
                        name, factory, ExceptionUtils.exception2detail(e));
            }
            return null;
        }
    }

    private static <T> T getBeanOf(final BeanFactory factory, final Class<T> requiredType, final Object... args) {
        try {
            return factory.getBean(requiredType, args);
        } catch (final Exception e) {
            if (!(e instanceof NoSuchBeanDefinitionException)) {
                LOG.warn("exception when get ({}) bean with args from ({}), detail:{}",
                        requiredType, factory, ExceptionUtils.exception2detail(e));
            }
            return null;
        }
    }

    public String findUnitFullpathByName(final String name) {
        for (final String fullpath : this._units.keySet()) {
            if (fullpath.endsWith("/" + name) || fullpath.equals(name)) {
                return fullpath;
            }
        }
        return null;
    }

    @Override
    public ConfigurableListableBeanFactory[] allBeanFactory() {
        final List<ConfigurableListableBeanFactory> factorys = new ArrayList<>();
        for (final Node node : this._units.values() ) {
            if (null!=node._applicationContext && null!=node._applicationContext.getBeanFactory())
                factorys.add(node._applicationContext.getBeanFactory());
        }
        return factorys.toArray(new ConfigurableListableBeanFactory[0]);
    }

    public void init() {
        refreshSources();
    }

    public void stop() {
        deleteAllUnit();
    }

    private void refreshSources() {
        final Map<String, String[]> infos = getSourceInfo(this._sourcePatterns);
        for (final Map.Entry<String, String[]> entry : infos.entrySet()) {
            final String[] placeholders = entry.getValue();
            final String suffix = "name=" + entry.getKey();
            if (!this._sourcesRegister.isRegistered(suffix)) {
                this._sourcesRegister.registerMBean(suffix, new SourceMXBean() {

                    @Override
                    public String[] getPlaceholders() {
                        return placeholders;
                    }
                });
            }
        }
    }

    public String[] getSourcePatterns() {
        return this._sourcePatterns;
    }

    public void setSourcePatterns(final String[] sourcePatterns) {
        this._sourcePatterns = sourcePatterns;
    }

    @Override
    public boolean newUnit(
            final String unitName,
            final String pattern,
            final String[] unitParameters) {
        return newUnit(unitName, pattern, new HashMap<String, String>() {
            private static final long serialVersionUID = 1L;
            {
                //  确保偶数
                for (int idx = 0; idx < (unitParameters.length / 2) * 2; idx += 2) {
                    this.put(unitParameters[idx], unitParameters[idx + 1]);
                }
            }
        });
    }

    @Override
    public boolean newUnit(
            final String unitName,
            final String pattern,
            final Map<String, String> unitParameters) {
        return null != createUnit(unitName, new String[]{pattern}, unitParameters, false);
    }

    public UnitMXBean createUnitWithSource(
            final String unitName,
            final String[] source,
            final Map<String, String> unitParameters) {
        return doCreateUnit(unitName, source, unitParameters);
    }

    public UnitMXBean createUnit(
            final String unitName,
            final String[] patterns,
            final Map<String, String> unitParameters,
            final boolean usingFirstWhenMatchedMultiSource) {
        final String[] sources = searchUnitSourceOf(patterns);

        if (null == sources) {
            LOG.warn("can't found unit source matched {}, newUnit {} failed",
                    Arrays.toString(patterns), unitName);
            addLog(" newUnit(" + unitName
                    + ") failed for can't found source matched ("
                    + Arrays.toString(patterns) + ")");
            return null;
        } else if (sources.length > 1) {
            if (!usingFirstWhenMatchedMultiSource) {
                LOG.warn("found unit source more than one matched {}, failed to create unit {}/{}",
                        Arrays.toString(patterns), unitName);
                addLog(" newUnit(" + unitName
                        + ") failed for found unit source > 1 "
                        + Arrays.toString(patterns));
                return null;
            } else {
                LOG.warn("found unit source more than one matched {}, using first one to create unit {}",
                        Arrays.toString(patterns), unitName);
            }
        }

        return doCreateUnit(unitName, new String[]{sources[0]}, unitParameters);
    }

    private UnitMXBean doCreateUnit(
            final String unitName,
            final String[] unitSource,
            final Map<String, String> unitParameters) {

        final String objectNameSuffix = genUnitSuffix(unitName);
        final Object mock = newMockUnitMXBean(unitName);
        if (!reserveRegistration(objectNameSuffix, mock)) {
            addLog(" newUnit("
                    + unitName
                    + ") failed for can't reserve "
                    + objectNameSuffix);
            throw new RuntimeException("can't reserve " + objectNameSuffix + ", may be already registered");
        }

        final Properties properties = new Properties() {
            private static final long serialVersionUID = 1L;

            {
                this.putAll(unitParameters);
            }
        };

        final ValueAwarePlaceholderConfigurer configurer = new ValueAwarePlaceholderConfigurer() {
                    {
                        this.setProperties(properties);
                    }
                };

        if (this._rootPropertyFiles != null) {
            configurer.setLocations(this._rootPropertyFiles);
            configurer.setLocalOverride(true);//params(功能单元中的配置)覆盖全局配置
        }

        final Node parentNode = getParentNode(unitName);
        final ApplicationContext parentCtx = null != parentNode ? parentNode.rootApplicationContext()
                    : this._rootApplicationContext;

        if (null == parentCtx) {
            registerUnactiveUnit(
                    unitName,
                    unitSource,
                    unitParameters,
                    objectNameSuffix,
                    parentNode,
                    mock,
                    "newUnit(" + unitName + ") failed for unactive parent("+parentNode._unitName + ").");
            addLog(" newUnit(" + unitName + ") failed for unactive parent("+parentNode._unitName + ").");
            return null;
        }
        try {
            final UnitAgentAware unitAgentAware = new UnitAgentAware();
            final UnitKeeperAwareHolder unitKeeprAwareHolder = new UnitKeeperAwareHolder();
            final ClassPathXmlApplicationContext ctx =
                    createConfigurableApplicationContext(
                            parentCtx,
                            genFullObjectNameOfUnit(unitName),
                            unitSource,
                            configurer,
                            properties,
                            unitAgentAware,
                            unitKeeprAwareHolder);
            ctx.setDisplayName(unitName);
            if ( null != parentNode) {
                parentNode.addChild(unitName);
            }
            final Node node = new Node(ctx, unitName, unitSource, unitParameters, unitAgentAware.getUnitAgent());
            this._units.put(unitName, node);

            final UnitMXBean unit =
                    newUnitMXBean(
                            node,
                            null!=ctx,
                            unitName,
                            unitSource,
                            new Date().toString(),
                            map2StringArray(unitParameters),
                            configurer.getTextedResolvedPlaceholdersAsStringArray(),
                            node.childrenUnits(),
                            "newUnit(" + unitName + ") succeed.");

            this._unitsRegister.replaceRegisteredMBean(objectNameSuffix, mock, unit);

            addLog("newUnit(" + unitName + ") succeed.");

            if (!this._unitListenerSupport.isEmpty()) {
                this._unitListenerSupport.foreachComponent(listener -> listener.postUnitCreated(unitName, ctx));
            }

            if (null != unitKeeprAwareHolder.getUnitKeeperAware()) {
                unitKeeprAwareHolder.getUnitKeeperAware().setUnitKeeper(buildUnitKeeper(unitName));
            }

            return unit;
        } catch (final Exception e) {
            registerUnactiveUnit(
                    unitName,
                    unitSource,
                    unitParameters,
                    objectNameSuffix,
                    parentNode,
                    mock,
                    "newUnit(" + unitName + ") failed for "+ ExceptionUtils.exception2detail(e));
            LOG.warn("exception when createUnit for {}, detail:{}", unitName, ExceptionUtils.exception2detail(e));
            addLog(" newUnit(" + unitName + ") failed for "
                    + ExceptionUtils.exception2detail(e));
            return null;
        }
    }

    private UnitKeeper buildUnitKeeper(final String parentName) {
        return new UnitKeeper() {
            @Override
            public void createOrUpdateUnit(
                    final String implname,
                    final String[] source,
                    final Map<String, String> unitParameters) {
                final String fullname = parentName + "/" + implname;
                createOrUpdateUnitWithSource(fullname, source, unitParameters);
                final Node parent = name2node(parentName);
                final Node impl = name2node(fullname);
                if (null != parent && null != impl) {
                    parent._implApplicationContext = impl._applicationContext;
                    updateDescendantUnitsOf(parentName, fullname);
                }
            }

            @Override
            public void deleteUnit(final String implname) {
                final Node parent = name2node(parentName);
                if (null != parent) {
                    parent._implApplicationContext = null;
                }
                UnitAgent.this.deleteUnit(parentName + "/" + implname);
            }};
    }

    /**
     * @param unitName
     * @return
     */
    private Node getParentNode(final String unitName) {
        final String parentPath = FilenameUtils.getPathNoEndSeparator(unitName);
        final Node parentNode = "".equals(parentPath) ? null : name2node(parentPath);
        if (null != parentNode) {
            LOG.debug("found parent node {} for path {} ", parentNode, parentPath);
        }
        else {
            LOG.debug("can not found parent node for path {} ", parentPath);
        }
        return parentNode;
    }

    /**
     * @param unitName
     * @param unitSource
     * @param unitParameters
     * @param objectNameSuffix
     * @param parentNode
     * @param mock
     */
    private void registerUnactiveUnit(
            final String unitName,
            final String[] unitSource,
            final Map<String, String> unitParameters,
            final String objectNameSuffix,
            final Node parentNode,
            final Object mock,
            final String unactiveReason) {
        if ( null != parentNode) {
            parentNode.addChild(unitName);
        }
        final Node node = new Node(null, unitName, unitSource, unitParameters, null);
        this._units.put(unitName, node);

        final UnitMXBean unit =
                newUnitMXBean(
                        node,
                        false,
                        unitName,
                        unitSource,
                        new Date().toString(),
                        map2StringArray(unitParameters),
                        null,
                        node.childrenUnits(),
                        unactiveReason);

        this._unitsRegister.replaceRegisteredMBean(objectNameSuffix, mock, unit);
    }

    public UnitMXBean createOrUpdateUnitWithSource(
            final String unitName,
            final String[] newSource,
            final Map<String, String> newUnitParameters) {
        if (null == name2node(unitName)) {
            return createUnitWithSource(unitName, newSource, newUnitParameters);
        } else {
            return updateUnitWithSource(unitName, newSource, newUnitParameters);
        }
    }

    public UnitMXBean createOrUpdateUnit(
            final String unitName,
            final String[] patterns,
            final Map<String, String> newUnitParameters,
            final boolean usingFirstWhenMatchedMultiSource) {
        if (null == name2node(unitName)) {
            return createUnit(unitName, patterns, newUnitParameters, usingFirstWhenMatchedMultiSource);
        } else {
            return updateUnit(unitName, newUnitParameters);
        }
    }

    public UnitMXBean updateUnitWithSource(
            final String unitName,
            final String[] newSource,
            final Map<String, String> newUnitParameters) {
        if (null == name2node(unitName)) {
            LOG.debug("can't found unit named {}, update failed.", unitName);
            addLog(" can't found unit named "+ unitName + ", update failed.");
            return null;
        }
        final Node[] nodes = doDeleteUnit(unitName).toArray(EMPTY_NODE);
        final UnitMXBean mbean = doCreateUnit(nodes[0]._unitName, newSource, newUnitParameters);
        for (int idx = 1; idx < nodes.length; idx++) {
            final Node node = nodes[idx];
            doCreateUnit(node._unitName, node._unitSource, node._unitParameters);
        }
        return mbean;
    }

    public UnitMXBean updateUnit(final String unitName, final Map<String, String> newUnitParameters) {
        final Node self = name2node(unitName);
        if (null == self) {
            LOG.debug("can't found unit named {}, update failed.", unitName);
            addLog(" can't found unit named "+ unitName + ", update failed.");
            return null;
        }
        else if (self._isUpdatable && null!=self._applicationContext) {
            final Properties properties = new Properties();
            properties.putAll(newUnitParameters);
            final ValueAwarePlaceholderConfigurer configurer = new ValueAwarePlaceholderConfigurer();
            configurer.setProperties(properties);

            final BeanPostProcessor processor = new FieldValueSetter(configurer.buildStringValueResolver());
            final Map<String, Object> beans = self._applicationContext.getBeansWithAnnotation(Updatable.class);
            for (final Map.Entry<String, Object> entry : beans.entrySet()) {
                processor.postProcessBeforeInitialization(entry.getValue(), entry.getKey());
            }
            final UnitMXBean mbean =
                    newUnitMXBean(
                            self,
                            true,
                            unitName,
                            self._unitSource,
                            new Date().toString(),
                            map2StringArray(newUnitParameters),
                            configurer.getTextedResolvedPlaceholdersAsStringArray(),
                            self.childrenUnits(),
                            "upateUnit(" + unitName + ") succeed.");

            final String objectNameSuffix = genUnitSuffix(unitName);
            this._unitsRegister.replaceRegisteredMBean(objectNameSuffix, this._unitsRegister.getMBean(objectNameSuffix), mbean);
            return mbean;
        }
        else {
            final Node[] nodes = doDeleteUnit(unitName).toArray(EMPTY_NODE);
            final UnitMXBean mbean = doCreateUnit(nodes[0]._unitName, nodes[0]._unitSource, newUnitParameters);
            for (int idx = 1; idx < nodes.length; idx++) {
                final Node node = nodes[idx];
                doCreateUnit(node._unitName, node._unitSource, node._unitParameters);
            }
            return mbean;
        }
    }

    private void updateDescendantUnitsOf(final String unitName, final String skipName) {
        final Node parent = name2node(unitName);
        if (null != parent) {
            final List<Node> descendants = new ArrayList<>();
            for (final String child : parent.childrenUnitsAsArray()) {
                if (!child.equals(skipName)) {
                    final List<Node> nodesDeleted = doDeleteUnit(child);
                    if (null != nodesDeleted) {
                        descendants.addAll(nodesDeleted);
                    }
                } else {
                    LOG.info("skip child unit {}, DO NOT update.", child);
                }
            }
            for (final Node node : descendants) {
                doCreateUnit(node._unitName, node._unitSource, node._unitParameters);
            }
        }
    }

    private Node name2node(final String unitName) {
        return this._units.get(unitName);
    }

    /**
     * @param params
     * @return
     */
    private String[] map2StringArray(final Map<String, String> params) {
        return new ArrayList<String>() {
            private static final long serialVersionUID = 1L;
        {
            for (final Map.Entry<String, String> entry : params.entrySet()) {
                this.add(entry.getKey() + "<--" + entry.getValue());
            }
        }}.toArray(new String[0]);
    }

    private UnitMXBean newUnitMXBean(
            final Node node,
            final boolean isActive,
            final String name,
            final String[] source,
            final String now,
            final String[] params,
            final String[] placeholders,
            final List<String> childrenUnits,
            final String unactiveReason) {
        return new UnitMXBean() {

            @Override
            public boolean isActive() {
                return isActive;
            }

            @Override
            public String getSource() {
                return Arrays.toString(source);
            }

            @Override
            public String[] getParameters() {
                return params;
            }

            @Override
            public String[] getPlaceholders() {
                return placeholders;
            }

            @Override
            public String getCreateTimestamp() {
                return now;
            }

            @Override
            public void close() {
                deleteUnit(name);
            }

            @Override
            public String getName() {
                return name;
            }

            @Override
            public String[] getChildrenUnits() {
                return childrenUnits.toArray(new String[0]);
            }

            @Override
            public String getUnactiveReason() {
                return unactiveReason;
            }

            @Override
            public boolean isUpdatable() {
                return node._isUpdatable;
            }

            @Override
            public void setUpdatable(final boolean updatable) {
                node.setUpdatable(updatable);
            }
        };
    }

    private String genUnitSuffix(final String unitName) {
        return this._rootApplicationContext.getDisplayName() + "=" + unitName;
    }

    private String genFullObjectNameOfUnit(final String unitName) {
        return _unitsRegister.getObjectNamePrefix() + "," + genUnitSuffix(unitName);
    }

    private boolean reserveRegistration(final String objectNameSuffix, final Object mock) {
        return this._unitsRegister.registerMBean(objectNameSuffix, mock);
    }

    private Object newMockUnitMXBean(final String name) {
        return new UnitMXBean() {

            @Override
            public boolean isActive() {
                return false;
            }

            @Override
            public String getSource() {
                return null;
            }

            @Override
            public String[] getParameters() {
                return null;
            }

            @Override
            public String[] getPlaceholders() {
                return null;
            }

            @Override
            public String getCreateTimestamp() {
                return null;
            }

            @Override
            public void close() {
            }

            @Override
            public String getName() {
                return name;
            }

            @Override
            public String[] getChildrenUnits() {
                return null;
            }

            @Override
            public String getUnactiveReason() {
                return null;
            }

            @Override
            public boolean isUpdatable() {
                return false;
            }

            @Override
            public void setUpdatable(final boolean updatable) {
            }
        };
    }

    @Override
    public boolean deleteUnit(final String unitName) {
        return null != doDeleteUnit(unitName);
    }

    public List<Node> doDeleteUnit(final String unitName) {
        this._unitsRegister.unregisterMBean(genUnitSuffix(unitName));
        final Node node = this._units.remove(unitName);
        if (null != node) {
            final List<Node> nodesDeleted = new ArrayList<>();
            nodesDeleted.add(node);
            for (final String child : node.childrenUnitsAsArray()) {
                final List<Node> nodes = doDeleteUnit(child);
                if (null != nodes) {
                    nodesDeleted.addAll(nodes);
                }
            }
            final ConfigurableApplicationContext ctx = node._applicationContext;
            if (null != ctx && !this._unitListenerSupport.isEmpty()) {
                this._unitListenerSupport.foreachComponent(listener -> listener.beforeUnitClosed(unitName, ctx));
            }
            node.closeApplicationContext();
            final Node parentNode = getParentNode(unitName);
            if (null!=parentNode) {
                parentNode.removeChild(unitName);
            }
            addLog(" deleteUnit(name=" + unitName + ") success.)");
            return nodesDeleted;
        } else {
            LOG.warn("can't found unit named {}, maybe deleted already", unitName);
            addLog(" deleteUnit(name=" + unitName + ") failure.)");
            return null;
        }
    }

    @Override
    public void deleteAllUnit() {
        //  remove and close all unit
        while (!this._units.isEmpty()) {
            deleteUnit(this._units.keySet().iterator().next());
        }
    }

    @Override
    public Map<String, String[]> getSourceInfo(final String sourcePattern) {
        return getSourceInfo(new String[]{sourcePattern});
    }

    public Map<String, String[]> getSourceInfo(final String[] sourcePatterns) {
        final String[] sources = searchUnitSourceOf(sourcePatterns);

        final Map<String, String[]> infos = new HashMap<>();

        if (null == sources) {
            LOG.warn("can't found unit source matched {}, getSourcesInfo failed", Arrays.toString(sourcePatterns));
            return infos;
        }

        for (final String source : sources) {
            final String unitSource = source;
            final ValueAwarePlaceholderConfigurer configurer =
                    new ValueAwarePlaceholderConfigurer() {
                        {
                            this.setIgnoreUnresolvablePlaceholders(true);
                        }

                        @Override
                        protected void processProperties(final ConfigurableListableBeanFactory beanFactoryToProcess, final Properties props)
                                throws BeansException {
                            super.processProperties(beanFactoryToProcess, props);
                            LOG.debug("after processProperties for source {}", unitSource);
                            throw new StopInitCtxException();
                        }
                    };

            try {
                createConfigurableApplicationContext(null, "", new String[]{source}, configurer, null, null, null);
            } catch (final StopInitCtxException ignored) {
            }
            infos.put(unitSource, configurer.getTextedResolvedPlaceholdersAsStringArray());
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

    private void addLog(final String msg) {
        this._logs.add(this._logidx.incrementAndGet()
                + ":" + new Date().toString()
                + ":" + msg + "\r\n");
    }

    /**
     * @param unitSource
     * @param unitSource2
     * @param configurer
     * @param properties
     * @param unitKeeprAwareHolder
     * @return
     */
    private ClassPathXmlApplicationContext createConfigurableApplicationContext(
            final ApplicationContext parentCtx,
            final String objectNameSuffix,
            final String[] unitSource,
            final ValueAwarePlaceholderConfigurer configurer,
            final Properties properties,
            final UnitAgentAware unitAgentAware,
            final UnitKeeperAwareHolder unitKeeprAwareHolder) {
        final MBeanRegister register = new MBeanRegisterSupport(objectNameSuffix, null);

        final ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(
                    unitSource,
                    false,
                    parentCtx);

        ctx.addBeanFactoryPostProcessor(configurer);
        ctx.addBeanFactoryPostProcessor(new BeanFactoryPostProcessor() {
            @Override
            public void postProcessBeanFactory(final ConfigurableListableBeanFactory beanFactory) throws BeansException {
                if (null != properties) {
                    beanFactory.addBeanPostProcessor(new PropertiesResourceSetter(properties));
                }
                beanFactory.addBeanPostProcessor(new PropertyPlaceholderConfigurerSetter(configurer));
                beanFactory.addBeanPostProcessor(new MBeanRegisterSetter(register));
                beanFactory.addBeanPostProcessor(new BeanHolderSetter(UnitAgent.this));

                final StringValueResolver stringValueResolver = configurer.buildStringValueResolver();
                final BeanHolder localHolder = new BeanHolder(){
                    @Override
                    public <T> T getBean(final Class<T> requiredType) {
                        try {
                            return ctx.getBean(requiredType);
                        } catch (final Exception e) {
                            LOG.debug("can't found {} locally, try find global.", requiredType);
                        }
                        final T bean = UnitAgent.this.getBean(requiredType);
                        if (null != bean) {
                            return bean;
                        }
                        // TODO, fix for real async find
                        return _beanFinder.find(requiredType).toBlocking().single();
                    }

                    @Override
                    public <T> T getBean(final String name, final Class<T> requiredType) {
                        try {
                            return ctx.getBean(name, requiredType);
                        } catch (final Exception e) {
                            LOG.debug("can't found {}/{} locally, try find global.", name, requiredType);
                        }
                        final T bean = UnitAgent.this.getBean(name, requiredType);
                        if (null != bean) {
                            return bean;
                        }
                        // TODO, fix for real async find
                        return _beanFinder.find(name, requiredType).toBlocking().single();
                    }

                    @Override
                    public Object getBean(final String name) {
                        try {
                            return ctx.getBean(name);
                        } catch (final Exception e) {
                            LOG.debug("can't found {} locally, try find global.", name);
                        }
                        final Object bean = UnitAgent.this.getBean(name);
                        if (null != bean) {
                            return bean;
                        }
                        // TODO, fix for real async find
                        return _beanFinder.find(name, Object.class).toBlocking().single();
                    }

                    @Override
                    public Object getBean(final String name, final Object... args) {
                        try {
                            return ctx.getBean(name, args);
                        } catch (final Exception e) {
                            LOG.debug("can't found {} with args locally, try find global.", name);
                        }
                        final Object bean = UnitAgent.this.getBean(name, args);
                        if (null != bean) {
                            return bean;
                        }
                        // TODO, fix for real async find
                        return _beanFinder.find(name, args).toBlocking().single();
                    }

                    @Override
                    public <T> T getBean(final Class<T> requiredType, final Object... args) {
                        try {
                            return ctx.getBean(requiredType, args);
                        } catch (final Exception e) {
                            LOG.debug("can't found {} with args locally, try find global.", requiredType);
                        }
                        final T bean = UnitAgent.this.getBean(requiredType, args);
                        if (null != bean) {
                            return bean;
                        }
                        // TODO, fix for real async find
                        return _beanFinder.find(requiredType, args).toBlocking().single();
                    }};

                beanFactory.addBeanPostProcessor(new FieldValueSetter(stringValueResolver));
                beanFactory.addBeanPostProcessor(new BeanHolderBasedFieldInjector(localHolder, stringValueResolver));

                beanFactory.addBeanPostProcessor(new MethodValueSetter(stringValueResolver));
                beanFactory.addBeanPostProcessor(new BeanHolderBasedMethodInjector(localHolder, stringValueResolver));

                if (null!=unitAgentAware) {
                    beanFactory.addBeanPostProcessor(unitAgentAware);
                }
                if (null!=unitKeeprAwareHolder) {
                    beanFactory.addBeanPostProcessor(unitKeeprAwareHolder);
                }
            }});
        ctx.addApplicationListener(new ApplicationListener<ApplicationContextEvent>() {
            @Override
            public void onApplicationEvent(final ApplicationContextEvent event) {
                if (event instanceof ContextClosedEvent) {
                    if (event.getApplicationContext() == ctx) {
                        register.destroy();
                        LOG.info("application {} closed, so destroy it's MBeanRegister {}", ctx, register);
                    } else {
                        LOG.debug("application {} raised close event, and received by parent application {}, just ignore.",
                                event.getApplicationContext(), ctx);
                    }
                }
            }});

        ctx.refresh();
        // when new applicationContext added, then reset finder's beans cache
        this._beanFinder.resetCache();
        return ctx;
    }

    private String[] searchUnitSourceOf(final String[] sourcePatterns) {

        LOG.debug("try to match pattern: {}", Arrays.toString(sourcePatterns));
        final List<String> sources = new ArrayList<>();
        try {
            final Map<URL, String[]> allRes = PackageUtils.getAllCPResourceAsPathlike();
            for (final Map.Entry<URL, String[]> entry : allRes.entrySet()) {
                for (final String res : entry.getValue()) {
                    if (LOG.isTraceEnabled()) {
                        LOG.trace("found resource: {}", res);
                    }
                    for (final String pattern : sourcePatterns) {
                        if (SelectorUtils.match(pattern, res)) {
                            LOG.debug("found matched unit source: {}", res);
                            sources.add(res);
                        }
                    }
                }
            }
        } catch (final IOException e) {
            LOG.error("exception when search pattern {}, detail : {}", Arrays.toString(sourcePatterns),
                    ExceptionUtils.exception2detail(e));
        }

        return sources.isEmpty() ? null : sources.toArray(new String[sources.size()]);
    }

    private static class Node {
        Node(final ConfigurableApplicationContext applicationContext,
            final String unitName,
            final String[] unitSource,
            final Map<String, String> unitParameters,
            final UnitAgent unitAgent
            ) {
            this._applicationContext = applicationContext;
            this._unitName = unitName;
            this._unitSource = unitSource;
            this._unitParameters = Collections.unmodifiableMap(unitParameters);
            this._unitAgent = unitAgent;
        }

        void setUpdatable(final boolean updatable) {
            this._isUpdatable = updatable;
        }

        ApplicationContext rootApplicationContext() {
            return null != this._implApplicationContext
                    ? this._implApplicationContext
                    : this._applicationContext;
        }

        ConfigurableApplicationContext closeApplicationContext() {
            if (null != this._applicationContext) {
                this._applicationContext.close();
            }
            return this._applicationContext;
        }

        void addChild(final String child) {
            this._children.add(child);
        }

        void removeChild(final String child) {
            this._children.remove(child);
        }

        List<String> childrenUnits() {
            return this._children;
        }

        String[] childrenUnitsAsArray() {
            return this._children.toArray(new String[0]);
        }

        @Override
        public String toString() {
            return "Node [unitName=" + _unitName + ", applicationContext="
                    + _applicationContext + ", unitSource=" + Arrays.toString(_unitSource)
                    + ", children's count=" + _children.size() + "]";
        }

        private final List<String> _children = new ArrayList<>();
        private final ConfigurableApplicationContext _applicationContext;
        private ConfigurableApplicationContext _implApplicationContext = null;
        private final String _unitName;
        private final String[] _unitSource;
        private final Map<String, String>   _unitParameters;
        private final UnitAgent _unitAgent;
        private boolean _isUpdatable = false;
    }

    @Override
    public void setMBeanRegister(final MBeanRegister register) {
        register.registerMBean("name=unitAgent", this);
        this._unitsRegister = register;
    }

    private String[] _sourcePatterns;

    private ApplicationContext _rootApplicationContext = null;

    private DefaultBeanFinder _beanFinder = null;

    private Resource[] _rootPropertyFiles = null;

    private final MBeanRegister _sourcesRegister;

    private MBeanRegister _unitsRegister;

    private final Map<String, Node> _units = new ConcurrentHashMap<>();

    private final AtomicInteger _logidx = new AtomicInteger(0);

    private final Queue<String> _logs = new ConcurrentLinkedQueue<>();

    private final COWCompositeSupport<UnitListener> _unitListenerSupport = new COWCompositeSupport<UnitListener>();
}
