package org.jocean.j2se.zk;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicReference;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.TreeCache;
import org.apache.curator.framework.recipes.cache.TreeCacheEvent;
import org.apache.curator.framework.recipes.cache.TreeCacheListener;
import org.jocean.idiom.COWCompositeSupport;
import org.jocean.idiom.ExceptionUtils;
import org.jocean.idiom.InterfaceSelector;
import org.jocean.j2se.unit.InitializationMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import rx.functions.Action0;
import rx.functions.ActionN;

public class ZKAgent {

    public interface Listener {
        public void onAdded(final ZKAgent agent, final String path, final byte[] data)
                throws Exception;

        public void onUpdated(final ZKAgent agent, final String path, final byte[] data)
                throws Exception;

        public void onRemoved(final ZKAgent agent, final String path)
                throws Exception;
    }

    private static final Listener NOP_LISTENER = new Listener() {
        @Override
        public void onAdded(final ZKAgent agent, final String path, final byte[] data)
                throws Exception {
        }
        @Override
        public void onUpdated(final ZKAgent agent, final String path, final byte[] data)
                throws Exception {
        }
        @Override
        public void onRemoved(final ZKAgent agent, final String path) throws Exception {
        }};

    private static class DisabledListener implements Listener {

        DisabledListener(final Listener listener) {
            this._listenerRef.set(listener);
        }

        @Override
        public void onAdded(final ZKAgent agent, final String path, final byte[] data) throws Exception {
            this._listenerRef.get().onAdded(agent, path, data);
        }

        @Override
        public void onUpdated(final ZKAgent agent, final String path, final byte[] data) throws Exception {
            this._listenerRef.get().onUpdated(agent, path, data);
        }

        @Override
        public void onRemoved(final ZKAgent agent, final String path) throws Exception {
            this._listenerRef.get().onRemoved(agent, path);
        }

        void disable() {
            LOG.info("disable listener: {} ", this._listenerRef.get());
            this._listenerRef.set(NOP_LISTENER);
        }

        private final AtomicReference<Listener> _listenerRef =
                new AtomicReference<>();
    }

    private static final Logger LOG = LoggerFactory.getLogger(ZKAgent.class);

    @Inject
    @Named("this.connName")
    public void setClient(final CuratorFramework client) {
        LOG.info("inject zkconn named[{}]/CuratorFramework {}", this.connName, client);
        this._client = client;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("ZKAgent [root=").append(_root).append(", zkconn=").append(this.connName).append("]");
        return builder.toString();
    }

    public ZKAgent(final String connName) {
        this.connName = connName;
    }

    /**
     * @return the CuratorFramework client
     */
    public CuratorFramework client() {
        return this._client;
    }

    public String root() {
        return this._root;
    }

    public ExecutorService executor() {
    	return this._executor;
    }

    public void start() throws Exception {
        if ( this._initializationMonitor != null) {
            this._initializationMonitor.beginInitialize(this);
        }
        this._executor = Executors.newSingleThreadExecutor(DEFAULT_THREAD_FACTORY);
        // wait for thread running
        this._executor.submit(() -> LOG.info("ZKAgent Thread has running.")).get();
        this._treecache = TreeCache.newBuilder(this._client, this._root)
            .setCacheData(false)
            .setExecutor(this._executor)
            .build();
        this._treecache.getListenable().addListener(new TreeCacheListener() {
            @Override
            public void childEvent(final CuratorFramework client, final TreeCacheEvent event)
                    throws Exception {
                switch (event.getType()) {
                case NODE_ADDED:
                    nodeAdded(event);
                    break;
                case NODE_REMOVED:
                    nodeRemoved(event);
                    break;
                case NODE_UPDATED:
                    nodeUpdated(event);
                case INITIALIZED:
                    if ( !_initialized ) {
                        _initialized = true;
                        if ( _initializationMonitor != null) {
                            _initializationMonitor.endInitialize(ZKAgent.this);
                        }
                    }
                default:
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("unhandle event ({}), just ignore.",
                                event);
                    }
                    break;
                }
            }});
        try {
            this._treecache.start();
        } catch (final Exception e) {
            LOG.error("exception when TreeCache({})'s start, detail:{}",
                    this._treecache, ExceptionUtils.exception2detail(e));
        }
    }

    public void stop() {
        this._op.stop(this);
    }

    //  TODO, simplify these code
    private static final ActionN DO_STOP = new ActionN() {
        @Override
        public void call(final Object... args) {
            ((ZKAgent)args[0]).stopWhenActive();
        }};

    private void stopWhenActive() {
        this._executor.submit(() -> {
                // close tree cache first and no more task accepted by executor
                _treecache.close();

                _listenerSupport.foreachComponent(listener -> syncNodesOnRemoved(listener));
                _listenerSupport.clear();
            });
    }

    public Action0 addListener(final Listener listener) {
        return this._op.addListener(this, listener);
    }

    private Action0 addListenerWhenActive(final Listener listener) {
        final DisabledListener disabledListener = new DisabledListener(listener);
        try {
            return () -> {
                    disabledListener.disable();
                    _executor.submit(() -> _op.removeListenerAndRemoveTree(ZKAgent.this, disabledListener));
                };
        } finally {
            this._executor.submit(() -> _op.addTreeAndAddListener(ZKAgent.this, disabledListener));
        }
    }

    private void doAddTreeAndAddListener(final Listener listener) {
        syncNodesOnAdded(listener);
        _listenerSupport.addComponent(listener);
        LOG.info("add listener: {} ", listener);
    }

    private void doRemoveListenerAndRemoveTree(final Listener listener) {
        _listenerSupport.removeComponent(listener);
        syncNodesOnRemoved(listener);
        LOG.info("remove listener: {} ", listener);
    }

    private static final Comparator<Map.Entry<String, byte[]>> ASC_BY_PATH =
            new Comparator<Map.Entry<String, byte[]>>() {
        @Override
        public int compare(final Map.Entry<String, byte[]> o1, final Map.Entry<String, byte[]> o2) {
            return o1.getKey().compareTo(o2.getKey());
        }};

    private static final Comparator<Map.Entry<String, byte[]>> DESC_BY_PATH =
            new Comparator<Map.Entry<String, byte[]>>() {
        @Override
        public int compare(final Map.Entry<String, byte[]> o1, final Map.Entry<String, byte[]> o2) {
            return o2.getKey().compareTo(o1.getKey());
        }};

    private void syncNodesOnAdded(final Listener listener) {
        final List<Map.Entry<String, byte[]>> nodes = Lists.newArrayList(this._nodes.entrySet());
        nodes.sort(ASC_BY_PATH);
        for (final Map.Entry<String, byte[]> node : nodes) {
            try {
                LOG.info("sync node({}) onAdded ", node.getKey());
                listener.onAdded(this, node.getKey(), node.getValue());
            } catch (final Exception e) {
                LOG.warn("exception when onAdded for {}/{}, detail: {}",
                        node.getKey(), node.getValue(), ExceptionUtils.exception2detail(e));
            }
        }
    }

    private void syncNodesOnRemoved(final Listener listener) {
        final List<Map.Entry<String, byte[]>> nodes = Lists.newArrayList(this._nodes.entrySet());
        nodes.sort(DESC_BY_PATH);
        for (final Map.Entry<String, byte[]> node : nodes) {
            try {
                LOG.info("sync node({}) onRemoved ", node.getKey());
                listener.onRemoved(this, node.getKey());
            } catch (final Exception e) {
                LOG.warn("exception when onRemoved for {}, detail: {}",
                        node.getKey(), ExceptionUtils.exception2detail(e));
            }
        }
    }

    private void nodeAdded(final TreeCacheEvent event) throws Exception {
        if (LOG.isDebugEnabled()) {
            LOG.debug("handle event ({}), try to add or update operator",
                    event);
        }
        // update local cached nodes
        this._nodes.put(event.getData().getPath(), event.getData().getData());

        if (!this._listenerSupport.isEmpty()) {
            this._listenerSupport.foreachComponent(listener -> {
                    try {
                        listener.onAdded(ZKAgent.this,
                                event.getData().getPath(),
                                event.getData().getData());
                    } catch (final Exception e) {
                        LOG.warn("exception when doAdd for event({}), detail:{}",
                                event, ExceptionUtils.exception2detail(e));
                    }
                });
        }
    }

    private void nodeRemoved(final TreeCacheEvent event) throws Exception {
        if (LOG.isDebugEnabled()) {
            LOG.debug("handle event ({}), try to remove operator",
                    event);
        }
        // update local cached nodes
        this._nodes.remove(event.getData().getPath());

        if (!this._listenerSupport.isEmpty()) {
            this._listenerSupport.foreachComponent(listener -> {
                    try {
                        listener.onRemoved(ZKAgent.this, event.getData().getPath());
                    } catch (final Exception e) {
                        LOG.warn("exception when doRemoved for event({}), detail:{}",
                                event, ExceptionUtils.exception2detail(e));
                    }
                });
        }
    }

    private void nodeUpdated(final TreeCacheEvent event) throws Exception {
        if (LOG.isDebugEnabled()) {
            LOG.debug("handle event ({}), try to update operator",
                    event);
        }
        // update local cached nodes
        this._nodes.put(event.getData().getPath(), event.getData().getData());

        if (!this._listenerSupport.isEmpty()) {
            this._listenerSupport.foreachComponent(listener -> {
                    try {
                        listener.onUpdated(ZKAgent.this,
                                event.getData().getPath(),
                                event.getData().getData());
                    } catch (final Exception e) {
                        LOG.warn("exception when doUpdate for event({}), detail:{}",
                                event, ExceptionUtils.exception2detail(e));
                    }
                });
        }
    }

    protected interface Op {
        public Action0 addListener(final ZKAgent zka, final Listener listener);
        public void addTreeAndAddListener(final ZKAgent zka, final Listener listener);
        public void removeListenerAndRemoveTree(final ZKAgent zka, final Listener listener);
        public void stop(final ZKAgent zka);
    }

    private static final Op OP_ACTIVE = new Op() {
        @Override
        public Action0 addListener(final ZKAgent zka, final Listener listener) {
            return zka.addListenerWhenActive(listener);
        }
        @Override
        public void addTreeAndAddListener(final ZKAgent zka, final Listener listener) {
            zka.doAddTreeAndAddListener(listener);
        }
        @Override
        public void removeListenerAndRemoveTree(final ZKAgent zka, final Listener listener) {
            zka.doRemoveListenerAndRemoveTree(listener);
        }
        @Override
        public void stop(final ZKAgent zka) {
            zka._selector.destroyAndSubmit(DO_STOP, zka);
        }
    };

    private static final Op OP_UNACTIVE = new Op() {

        @Override
        public Action0 addListener(final ZKAgent zka, final Listener listener) {
            throw new RuntimeException("ZKAgent({}) has stopped, can't addListener");
        }
        @Override
        public void addTreeAndAddListener(final ZKAgent zka, final Listener listener) {
        }
        @Override
        public void removeListenerAndRemoveTree(final ZKAgent zka, final Listener listener) {
        }
        @Override
        public void stop(final ZKAgent zka) {
            LOG.warn("ZKAgent({}) has already stopped.", zka);
        }
    };

    public void setRoot(final String root) {
        this._root = root;
    }

    boolean _initialized = false;

    private final String connName;

    @Inject
    InitializationMonitor _initializationMonitor;

    private final InterfaceSelector _selector = new InterfaceSelector();
    private final Op _op = this._selector.build(Op.class, OP_ACTIVE, OP_UNACTIVE);

    @Value("${zk.units.path}")
    String _root;

    private CuratorFramework _client;
    private ExecutorService _executor;
    private TreeCache _treecache;

    private final Map<String, byte[]> _nodes = new HashMap<>();
    private final COWCompositeSupport<Listener> _listenerSupport = new COWCompositeSupport<Listener>();

    private static final ThreadFactory DEFAULT_THREAD_FACTORY = new ThreadFactoryBuilder()
            .setNameFormat("Curator-TreeCache-%d")
            .setDaemon(false)
            .build();
}
