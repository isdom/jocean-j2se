package org.jocean.j2se.zk;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.TreeCache;
import org.apache.curator.framework.recipes.cache.TreeCacheEvent;
import org.apache.curator.framework.recipes.cache.TreeCacheListener;
import org.apache.curator.utils.CloseableExecutorService;
import org.jocean.idiom.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import rx.functions.Action2;

public class ZKAgent {
    public interface Listener {
        public void onAdded(final String root, final int version, final TreeCacheEvent event) 
                throws Exception;
        
        public void onUpdated(final String root, final int version, final TreeCacheEvent event) 
                throws Exception;
        
        public void onRemoved(final String root, final int version, final TreeCacheEvent event) 
                throws Exception;
    }
    
    private static final Logger LOG = LoggerFactory
            .getLogger(ZKAgent.class);

    public ZKAgent(
            final CuratorFramework client, 
            final String root, 
            final Listener operator) throws Exception {
        this._operator = operator;
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
    
    public int enumCurrentTree(final Action2<String, byte[]> onNode) {
        try {
            return this._executor.submit(new Callable<Integer>() {
                @Override
                public Integer call() throws Exception {
                    enumChildren(_root, onNode);
                    return _cacheVersion;
                }}).get();
        } catch (Exception e) {
            LOG.error("exception when enumCurrentTree {}, detail:{}", 
                    this._root, ExceptionUtils.exception2detail(e));
            return -1;
        }
    }
    
    private void enumChildren(final String root, final Action2<String, byte[]> onNode) {
    	final Map<String, ChildData> children = 
    			this._zkCache.getCurrentChildren(root);
    	if (null!= children) {
        	for (Map.Entry<String, ChildData> entry : children.entrySet()) {
        	    final String path= entry.getKey();
        	    final ChildData data = entry.getValue();
        	    try {
        	        onNode.call(path, data.getData());
        	    } catch (Exception e) {
        	        LOG.warn("exception when onNode for {}/{}, detail: {}",
        	                path, data.getData(), ExceptionUtils.exception2detail(e));
        	    }
        	    enumChildren(path, onNode);
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
        try {
            this._operator.onAdded(this._root, this._cacheVersion, event);
        } catch (Exception e) {
            LOG.warn("exception when doAdd for event({}), detail:{}",
                    event, ExceptionUtils.exception2detail(e));
        }
    }
    
    private void nodeRemoved(final TreeCacheEvent event) throws Exception {
        if (LOG.isDebugEnabled()) {
            LOG.debug("handle event ({}), try to remove operator", 
                    event);
        }
        try {
            this._operator.onRemoved(this._root, this._cacheVersion, event);
        } catch (Exception e) {
            LOG.warn("exception when doRemove for event({}), detail:{}",
                    event, ExceptionUtils.exception2detail(e));
        }
    }
    
    private void nodeUpdated(final TreeCacheEvent event) throws Exception {
        if (LOG.isDebugEnabled()) {
            LOG.debug("handle event ({}), try to update operator", 
                    event);
        }
        try {
            this._operator.onUpdated(this._root, this._cacheVersion, event);
        } catch (Exception e) {
            LOG.warn("exception when doUpdate for event({}), detail:{}",
                    event, ExceptionUtils.exception2detail(e));
        }
    }
    
    private final CloseableExecutorService _executor;
    private final String _root;
    private final TreeCache _zkCache;
    private int _cacheVersion = 0;
    private final Listener _operator;
}
