package org.jocean.j2se.zk;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.TreeCache;
import org.apache.curator.framework.recipes.cache.TreeCacheEvent;
import org.apache.curator.framework.recipes.cache.TreeCacheListener;
import org.jocean.idiom.COWCompositeSupport;
import org.jocean.idiom.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import rx.functions.Action0;
import rx.functions.Action1;

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
        public void onAdded(final ZKAgent agent, String path, byte[] data)
                throws Exception {
        }
        @Override
        public void onUpdated(final ZKAgent agent, String path, byte[] data)
                throws Exception {
        }
        @Override
        public void onRemoved(final ZKAgent agent, String path) throws Exception {
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
    
    private static final Logger LOG = LoggerFactory
            .getLogger(ZKAgent.class);

    public ZKAgent(
            final CuratorFramework client, 
            final String root) throws Exception {
        this._client = client;
        this._root = root;
        this._executor = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder()
            .setNameFormat("Curator-TreeCache-%d")
            .setDaemon(false)
            .build());
        // wait for thread running
        this._executor.submit(new Runnable() {
            @Override
            public void run() {
                LOG.info("ZKUpdater Thread has running.");
            }}).get();
        this._treecache = TreeCache.newBuilder(client, root)
            .setCacheData(false)
            .setExecutor(this._executor)
            .build();
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
    
    public void start() {
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
        } catch (Exception e) {
            LOG.error("exception when TreeCache({})'s start, detail:{}", 
                    this._treecache, ExceptionUtils.exception2detail(e));
        }
    }
    
    public void stop() {
        if (this._isActive.compareAndSet(true, false)) {
            this._executor.submit(new Runnable() {
                @Override
                public void run() {
                    _listenerSupport.foreachComponent(new Action1<Listener>() {
                        @Override
                        public void call(final Listener listener) {
                            syncNodesOnRemoved(listener);
                        }});
                    _listenerSupport.clear();
                    _treecache.close();
                }});
        } else {
            LOG.warn("ZKAgent has already stopped.");
        }
    }

    public Action0 addListener(final Listener listener) {
        final DisabledListener disabledListener = new DisabledListener(listener);
        try {
            return new Action0() {
                @Override
                public void call() {
                    disabledListener.disable();
                    removeListener(disabledListener);
                }};
        } finally {
            syncTreeAndAddListener(disabledListener);
        }
    }
    
    private void removeListener(final Listener listener) {
        this._executor.submit(new Runnable() {
            @Override
            public void run() {
                _listenerSupport.removeComponent(listener);
                syncNodesOnRemoved(listener);
                LOG.info("remove listener: {} ", listener);
            }});
    }

    private void syncTreeAndAddListener(final Listener listener) {
        this._executor.submit(new Runnable() {
            @Override
            public void run() {
                syncNodesOnAdded(listener);
                _listenerSupport.addComponent(listener);
            }});
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
        for (Map.Entry<String, byte[]> node : nodes) {
            try {
                LOG.info("sync node({}) onAdded ", node.getKey());
                listener.onAdded(this, node.getKey(), node.getValue());
            } catch (Exception e) {
                LOG.warn("exception when onAdded for {}/{}, detail: {}",
                        node.getKey(), node.getValue(), ExceptionUtils.exception2detail(e));
            }
        }
    }
    
    private void syncNodesOnRemoved(final Listener listener) {
        final List<Map.Entry<String, byte[]>> nodes = Lists.newArrayList(this._nodes.entrySet());
        nodes.sort(DESC_BY_PATH);
        for (Map.Entry<String, byte[]> node : nodes) {
            try {
                LOG.info("sync node({}) onRemoved ", node.getKey());
                listener.onRemoved(this, node.getKey());
            } catch (Exception e) {
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
            this._listenerSupport.foreachComponent(new Action1<Listener> () {
                @Override
                public void call(final Listener listener) {
                    try {
                        listener.onAdded(ZKAgent.this,
                                event.getData().getPath(), 
                                event.getData().getData());
                    } catch (Exception e) {
                        LOG.warn("exception when doAdd for event({}), detail:{}",
                                event, ExceptionUtils.exception2detail(e));
                    }
                }});
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
            this._listenerSupport.foreachComponent(new Action1<Listener> () {
                @Override
                public void call(final Listener listener) {
                    try {
                        listener.onRemoved(ZKAgent.this, event.getData().getPath());
                    } catch (Exception e) {
                        LOG.warn("exception when doRemoved for event({}), detail:{}",
                                event, ExceptionUtils.exception2detail(e));
                    }
                }});
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
            this._listenerSupport.foreachComponent(new Action1<Listener> () {
                @Override
                public void call(final Listener listener) {
                    try {
                        listener.onUpdated(ZKAgent.this,
                                event.getData().getPath(), 
                                event.getData().getData());
                    } catch (Exception e) {
                        LOG.warn("exception when doUpdate for event({}), detail:{}",
                                event, ExceptionUtils.exception2detail(e));
                    }
                }});
        }
    }
    
    private final AtomicBoolean _isActive = new AtomicBoolean(true);
    private final CuratorFramework _client;
    private final ExecutorService _executor;
    private final String _root;
    private final TreeCache _treecache;
    private final Map<String, byte[]> _nodes = new HashMap<>();
    private final COWCompositeSupport<Listener> _listenerSupport
        = new COWCompositeSupport<Listener>();
}
