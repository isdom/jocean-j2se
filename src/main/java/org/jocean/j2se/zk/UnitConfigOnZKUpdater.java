package org.jocean.j2se.zk;

import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.CreateMode;
import org.jocean.idiom.ExceptionUtils;
import org.jocean.idiom.PropertyPlaceholderHelper;
import org.jocean.idiom.PropertyPlaceholderHelper.PlaceholderResolver;
import org.jocean.j2se.jmx.MBeanStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rx.Subscriber;

import com.google.common.base.Charsets;

public class UnitConfigOnZKUpdater extends Subscriber<MBeanStatus> {
    
    private static final Logger LOG = 
            LoggerFactory.getLogger(UnitConfigOnZKUpdater.class);

    public UnitConfigOnZKUpdater(final CuratorFramework curator) {
        this._curator = curator;
    }
    
    @Override
    public void onCompleted() {
        LOG.info("Subscriber MBeanStatus for path {} onCompleted", this._path);
        removeZKPath();
    }

    @Override
    public void onError(final Throwable e) {
        LOG.warn("exception when subscriber MBeanStatus for path {}, detail:{}",
                this._path, ExceptionUtils.exception2detail(e));
    }

    @Override
    public void onNext(final MBeanStatus mbeanStatus) {
        if ( mbeanStatus.isRegistered() ) {
            final PlaceholderResolver placeholderResolver = new PlaceholderResolver() {
                @Override
                public String resolvePlaceholder(final Object resolveContext,
                        final String placeholderName) {
                    final Object value = mbeanStatus.getValue(placeholderName);
                    return null != value ? value.toString() : "";
                }};
            final PropertyPlaceholderHelper placeholderReplacer = new PropertyPlaceholderHelper("{", "}");
            final String config = placeholderReplacer.replacePlaceholders(null, this._template, placeholderResolver, null);
            
            try {
                this._curator.create()
                    .creatingParentsIfNeeded()
                    .withMode(CreateMode.EPHEMERAL_SEQUENTIAL)
                    .forPath(this._path, config.getBytes(Charsets.UTF_8));
                if (LOG.isDebugEnabled()) {
                    LOG.debug("create config for path {}, config\n{}", this._path, config);
                }
            } catch (Exception e) {
                LOG.warn("exception when create config for path {}, detail:{}",
                        this._path, ExceptionUtils.exception2detail(e));
            }
            
        } else if (mbeanStatus.isUnregistered()) {
            removeZKPath();
        }
    }

    private void removeZKPath() {
        try {
            this._curator.delete()
                .deletingChildrenIfNeeded()
                .forPath(this._path);
            if (LOG.isDebugEnabled()) {
                LOG.debug("delete config for path {}", this._path);
            }
        } catch (Exception e) {
            LOG.warn("exception when delete config for path {}, detail:{}",
                    this._path, ExceptionUtils.exception2detail(e));
        }
    }

    
    /**
     * @param template the _template to set
     */
    public void setTemplate(final String template) {
        this._template = template;
    }
    
    /**
     * @param path the _path to set
     */
    public void setPath(final String path) {
        this._path = path;
    }
    
    private String _path;
    private String _template;
    private final CuratorFramework _curator;
}
