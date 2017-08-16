package org.jocean.j2se.unit;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import javax.inject.Inject;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.NodeCache;
import org.apache.curator.framework.recipes.cache.NodeCacheListener;

import com.google.common.collect.Maps;

public class ZKNodeRef implements UnitKeeperAware {

    public void setPath(final String path) {
        this._path = path;
    }

    public void start() {
        this._cache = new NodeCache(this._client, this._path);
        this._cache.getListenable().addListener(new NodeCacheListener() {
            @Override
            public void nodeChanged() throws Exception {
                final ChildData data = _cache.getCurrentData();
                if (null != data) {
                    onUpdated(data.getData());
                } else {
                    onRemoved();
                }
            }});
    }
    
    public void stop() {
        try {
            this._cache.close();
        } catch (IOException e) {
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
        } catch (Exception e) {
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
    }
    
    @Inject
    private CuratorFramework _client;
    
    private NodeCache _cache;
    private String _path;
    
    private UnitKeeper _unitKeeper;
    
    private static final String SPRING_XML_KEY = "__spring.xml";
}
