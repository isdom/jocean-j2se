package org.jocean.j2se.zk;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.TreeCache;
import org.apache.curator.framework.recipes.cache.TreeCacheEvent;
import org.apache.curator.framework.recipes.cache.TreeCacheListener;
import org.jocean.idiom.COWCompositeSupport;
import org.jocean.idiom.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        this._zkCache = TreeCache.newBuilder(client, root)
            .setCacheData(true)
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
        this._zkCache.getListenable().addListener(new TreeCacheListener() {
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
            this._zkCache.start();
        } catch (Exception e) {
            LOG.error("exception when TreeCache({})'s start, detail:{}", 
                    this._zkCache, ExceptionUtils.exception2detail(e));
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
                LOG.info("remove listener: {} ", listener);
            }});
    }

    private void syncTreeAndAddListener(final Listener listener) {
        this._executor.submit(new Runnable() {
            @Override
            public void run() {
                enumSubtreeOf(_root, listener);
                _listenerSupport.addComponent(listener);
            }});
    }

    private void enumSubtreeOf(final String parent, final Listener listener) {
        final ChildData data = this._zkCache.getCurrentData(parent);
        if (null != data) {
            try {
                listener.onAdded(this, data.getPath(), data.getData());
            } catch (Exception e) {
                LOG.warn("exception when onAdded for {}/{}, detail: {}",
                        data.getPath(), data.getData(), ExceptionUtils.exception2detail(e));
            }
            final Map<String, ChildData> children = 
                    this._zkCache.getCurrentChildren(parent);
            if (null!= children) {
                final List<String> childrenPaths = new ArrayList<>();
                for (Map.Entry<String, ChildData> entry : children.entrySet()) {
                    childrenPaths.add(entry.getValue().getPath());
                }
                Collections.sort(childrenPaths);
                for (String path : childrenPaths) {
                    enumSubtreeOf(path, listener);
                }
            }
        }
    }
    
    public void stop() {
        this._zkCache.close();
    }

    private void nodeAdded(final TreeCacheEvent event) throws Exception {
        if (LOG.isDebugEnabled()) {
            LOG.debug("handle event ({}), try to add or update operator", 
                    event);
        }
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
    
    private final CuratorFramework _client;
    private final ExecutorService _executor;
    private final String _root;
    private final TreeCache _zkCache;
    private final COWCompositeSupport<Listener> _listenerSupport
        = new COWCompositeSupport<Listener>();
}
