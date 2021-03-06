package org.jocean.j2se.unit;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.NodeCache;
import org.apache.curator.framework.recipes.cache.NodeCacheListener;
import org.jocean.idiom.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;

public class ZKNodeRef implements UnitKeeperAware {
    private static final Logger LOG = LoggerFactory.getLogger(ZKNodeRef.class);

    public ZKNodeRef(final String connName) {
        this.connName = connName;
    }

    @Inject
    @Named("this.connName")
    public void setClient(final CuratorFramework client) {
        LOG.info("inject zkconn named[{}]/CuratorFramework {}", this.connName, client);
        this._client = client;
    }

    public void setPath(final String path) {
        this._path = path;
    }

    private void start() {
        this._cache = new NodeCache(this._client, this._path);
        this._cache.getListenable().addListener(new NodeCacheListener() {
            @Override
            public void nodeChanged() throws Exception {
                processUnitByData();
            }});
        try {
            this._cache.start(true);
        } catch (final Exception e) {
            LOG.warn("exception when NodeCache.start for path {}, detail: {}",
                    this._path,
                    ExceptionUtils.exception2detail(e));
        }
        LOG.debug("ZKNodeRef {} start", this);
        processUnitByData();
    }

    public void stop() {
        try {
            LOG.debug("ZKNodeRef {} closing", this);
            this._cache.close();
            LOG.debug("ZKNodeRef {} closed", this);
        } catch (final IOException e) {
        }
    }

    private void processUnitByData() {
        final ChildData data = this._cache.getCurrentData();
        LOG.debug("process unit with current data {}", data);
        if (null != data) {
            onUpdated(data.getData());
        } else {
            onRemoved();
        }
    }

    private void onUpdated(final byte[] data) {
        final Properties properties = loadProperties(data);
        final String[] source = genSourceFrom(properties);
        this._unitKeeper.createOrUpdateUnit("#", source, Maps.fromProperties(properties));
    }

    private void onRemoved() {
        this._unitKeeper.deleteUnit("#");
    }

    private Properties loadProperties(final byte[] data) {
        try (
            final InputStream is = null != data
                    ? new ByteArrayInputStream(data)
                    : null;
        ) {
            return new Properties() {
                private static final long serialVersionUID = 1L;
            {
                if (null != is) {
                    this.load( is );
                }
            }};
        } catch (final Exception e) {
            return null;
        }
    }

    private static String[] genSourceFrom(final Properties properties) {
        final String value = properties.getProperty(SPRING_XML_KEY);
        properties.remove(SPRING_XML_KEY);
        return null!=value ? value.split(",") : null;
    }

    @Override
    public void setUnitKeeper(final UnitKeeper keeper) {
        this._unitKeeper = keeper;
        start();
    }

    private final String connName;
    private CuratorFramework _client;

    private NodeCache _cache;
    private String _path;

    private UnitKeeper _unitKeeper;

    private static final String SPRING_XML_KEY = "__spring.xml";
}
