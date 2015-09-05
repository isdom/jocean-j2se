package org.jocean.j2se.zk;

import java.util.concurrent.Executors;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.TreeCache;
import org.apache.curator.framework.recipes.cache.TreeCacheEvent;
import org.apache.curator.framework.recipes.cache.TreeCacheListener;
import org.apache.curator.utils.CloseableExecutorService;
import org.jocean.idiom.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

public class ZKUpdater{
    public interface Operator {
        public void doAdd(final String root, final TreeCacheEvent event) 
                throws Exception;
        
        public void doUpdate(final String root, final TreeCacheEvent event) 
                throws Exception;
        
        public void doRemove(final String root, final TreeCacheEvent event) 
                throws Exception;
    }
    
    private static final Logger LOG = LoggerFactory
            .getLogger(ZKUpdater.class);

    public ZKUpdater(
            final CuratorFramework client, 
            final String root, 
            final Operator operator) throws Exception {
        this._operator = operator;
        this._root = root;
        final CloseableExecutorService executor = 
            new CloseableExecutorService(
                Executors.newSingleThreadExecutor(new ThreadFactoryBuilder()
                    .setNameFormat("Curator-TreeCache-%d")
                    .setDaemon(false)
                    .build()));
        // wait for thread running
        executor.submit(new Runnable() {
            @Override
            public void run() {
                LOG.info("ZKUpdater Thread has running.");
            }}).get();
        this._zkCache = TreeCache.newBuilder(client, root)
            .setCacheData(true)
            .setExecutor(executor)
            .build();
    }
    
    public void start() {
        this._zkCache.getListenable().addListener(new TreeCacheListener() {

            @Override
            public void childEvent(CuratorFramework client, TreeCacheEvent event)
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
    
    public void stop() {
        this._zkCache.close();
    }

    private void nodeAdded(final TreeCacheEvent event) throws Exception {
        if (LOG.isDebugEnabled()) {
            LOG.debug("handle event ({}), try to add or update operator", 
                    event);
        }
        try {
            this._operator.doAdd(this._root, event);
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
            this._operator.doRemove(this._root, event);
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
            this._operator.doUpdate(this._root, event);
        } catch (Exception e) {
            LOG.warn("exception when doUpdate for event({}), detail:{}",
                    event, ExceptionUtils.exception2detail(e));
        }
    }
    
    private final String _root;
    private final TreeCache _zkCache;
    private final Operator _operator;
}
