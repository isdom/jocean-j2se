package org.jocean.j2se.zk;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.TreeCache;
import org.apache.curator.framework.recipes.cache.TreeCacheEvent;
import org.apache.curator.framework.recipes.cache.TreeCacheListener;
import org.apache.curator.utils.CloseableExecutorService;
import org.jocean.idiom.COWCompositeSupport;
import org.jocean.idiom.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import rx.functions.Action1;

public class ZKAgent {
    private static final Runnable NOP = new Runnable() {
        @Override
        public void run() {
        }};
        
    public interface Listener {
        public void onAdded(final int version, final String path, final byte[] data) 
                throws Exception;
        
        public void onUpdated(final int version, final String path, final byte[] data) 
                throws Exception;
        
        public void onRemoved(final int version, final String path) 
                throws Exception;
    }
    
    private static final Logger LOG = LoggerFactory
            .getLogger(ZKAgent.class);

    public ZKAgent(
            final CuratorFramework client, 
            final String root) throws Exception {
        this._root = root;
        this._executor = 
            new CloseableExecutorService(
                Executors.newSingleThreadExecutor(new ThreadFactoryBuilder()
                    .setNameFormat("Curator-TreeCache-%d")
                    .setDaemon(false)
                    .build()));
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
    
    public String root() {
        return this._root;
    }
    
    public CloseableExecutorService executor() {
    	return this._executor;
    }
    
    public void start() {
        this._zkCache.getListenable().addListener(new TreeCacheListener() {
            @Override
            public void childEvent(final CuratorFramework client, final TreeCacheEvent event)
                    throws Exception {
                _cacheVersion++;
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
    
    public Runnable addListener(final Listener listener) {
        try {
            return this._executor.submit(new Callable<Runnable>() {
                @Override
                public Runnable call() throws Exception {
                    return syncTreeAndAddListener(listener);
                }}).get();
        } catch (Exception e) {
            LOG.error("exception when syncCurrentTree {}, detail:{}", 
                    this._root, ExceptionUtils.exception2detail(e));
            return NOP;
        }
    }
    
    private Runnable syncTreeAndAddListener(final Listener listener) {
        final WrappedListener wrappedListener = new WrappedListener(listener);
        enumSubtreeOf(this._root, wrappedListener);
        try {
            wrappedListener.onAdded(this._cacheVersion, null, null);
        } catch (Exception e) {
            LOG.warn("exception when onAdded for sync cache version {}, detail: {}",
                    this._cacheVersion, ExceptionUtils.exception2detail(e));
        }
        this._listenerSupport.addComponent(wrappedListener);
        return new Runnable() {
            @Override
            public void run() {
                wrappedListener.disable();
                _listenerSupport.removeComponent(wrappedListener);
            }};
    }
    
    private class WrappedListener implements Listener {

        WrappedListener(final Listener listener) {
            this._listenerRef.set(listener);
        }
        
        @Override
        public void onAdded(int version, String path, byte[] data) throws Exception {
            final Listener impl = this._listenerRef.get();
            if (null!=impl) {
                impl.onAdded(version, path, data);
            }
        }

        @Override
        public void onUpdated(int version, String path, byte[] data) throws Exception {
            final Listener impl = this._listenerRef.get();
            if (null!=impl) {
                impl.onUpdated(version, path, data);
            }
        }

        @Override
        public void onRemoved(int version, String path) throws Exception {
            final Listener impl = this._listenerRef.get();
            if (null!=impl) {
                impl.onRemoved(version, path);
            }
        }
        
        void disable() {
            this._listenerRef.set(null);
        }
        
        private final AtomicReference<Listener> _listenerRef = 
                new AtomicReference<>();
    }
    
    private void enumSubtreeOf(final String parent, final Listener listener) {
    	final Map<String, ChildData> children = 
    			this._zkCache.getCurrentChildren(parent);
    	if (null!= children) {
        	for (Map.Entry<String, ChildData> entry : children.entrySet()) {
        	    final String path= entry.getKey();
        	    final ChildData data = entry.getValue();
        	    try {
        	        listener.onAdded(-1, path, data.getData());
        	    } catch (Exception e) {
        	        LOG.warn("exception when onAdded for {}/{}, detail: {}",
        	                path, data.getData(), ExceptionUtils.exception2detail(e));
        	    }
        	    enumSubtreeOf(path, listener);
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
                        listener.onAdded(_cacheVersion, 
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
                        listener.onRemoved(_cacheVersion, 
                                event.getData().getPath());
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
                        listener.onUpdated(_cacheVersion, 
                                event.getData().getPath(), 
                                event.getData().getData());
                    } catch (Exception e) {
                        LOG.warn("exception when doUpdate for event({}), detail:{}",
                                event, ExceptionUtils.exception2detail(e));
                    }
                }});
        }
    }
    
    private final CloseableExecutorService _executor;
    private final String _root;
    private final TreeCache _zkCache;
    private int _cacheVersion = 0;
//    private final Listener _operator;
    private final COWCompositeSupport<Listener> _listenerSupport
        = new COWCompositeSupport<Listener>();
}
