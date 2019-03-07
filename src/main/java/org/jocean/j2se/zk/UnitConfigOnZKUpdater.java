package org.jocean.j2se.zk;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;
import javax.management.ObjectName;

import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.CreateMode;
import org.jocean.idiom.ExceptionUtils;
import org.jocean.idiom.PropertyPlaceholderHelper;
import org.jocean.idiom.PropertyPlaceholderHelper.PlaceholderResolver;
import org.jocean.idiom.jmx.MBeanRegister;
import org.jocean.idiom.jmx.MBeanRegisterAware;
import org.jocean.j2se.jmx.MBeanStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;

import rx.Observer;

public class UnitConfigOnZKUpdater implements Observer<MBeanStatus>, MBeanRegisterAware {

    private static final Logger LOG = LoggerFactory.getLogger(UnitConfigOnZKUpdater.class);

    @Override
    public void setMBeanRegister(final MBeanRegister register) {
        register.registerMBean("type=zkupdater", new ZKUpdaterMXBean() {
            @Override
            public String[] getCreatedPaths() {
                return _createdPaths.values().toArray(new String[0]);
            }

            @Override
            public void removePaths() {
                removeAllZKPath();
            }
        });
    }

    @Override
    public void onCompleted() {
        LOG.info("Subscriber MBeanStatus for path {} onCompleted", this._path);
        removeAllZKPath();
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
                    final String propertyValue = System.getProperty(placeholderName);
                    if (null!=propertyValue) {
                        return propertyValue;
                    }
                    final String envValue = System.getenv(placeholderName);
                    if (null!=envValue) {
                        return envValue;
                    }
                    final Object value = mbeanStatus.getValue(placeholderName);
                    return null != value ? value.toString() : "";
                }};
            final PropertyPlaceholderHelper placeholderReplacer = new PropertyPlaceholderHelper("{", "}");
            final String config = placeholderReplacer.replacePlaceholders(null, this._template, placeholderResolver, null);

            try {
                addZKPath(mbeanStatus.mbeanName(),
                    this._curator.create()
                        .creatingParentsIfNeeded()
                        .withMode(CreateMode.EPHEMERAL_SEQUENTIAL)
                        .forPath(placeholderReplacer.replacePlaceholders(null, this._path, placeholderResolver, null),
                            config.getBytes(Charsets.UTF_8)));
                if (LOG.isDebugEnabled()) {
                    LOG.debug("create config for path {}, config\n{}", this._path, config);
                }
            } catch (final Exception e) {
                LOG.warn("exception when create config for path {}, detail:{}",
                        this._path, ExceptionUtils.exception2detail(e));
            }

        } else if (mbeanStatus.isUnregistered()) {
            removeZKPath(mbeanStatus.mbeanName());
        }
    }

    private void addZKPath(final ObjectName mbeanName, final String createdPath) {
        this._createdPaths.put(mbeanName, createdPath);
    }

    private void removeZKPath(final ObjectName mbeanName) {
        final String path = this._createdPaths.remove(mbeanName);
        if (null!=path) {
            try {
                this._curator.delete()
                    .deletingChildrenIfNeeded()
                    .forPath(path);
                if (LOG.isDebugEnabled()) {
                    LOG.debug("delete config for path {}", path);
                }
            } catch (final Exception e) {
                LOG.warn("exception when delete config for path {}, detail:{}",
                        path, ExceptionUtils.exception2detail(e));
            }
        }
    }

    private void removeAllZKPath() {
        while (!this._createdPaths.keySet().isEmpty()) {
            removeZKPath(this._createdPaths.keySet().iterator().next());
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
        this._path = path + _PATH_SUFFIX;
    }

    static {
        String hostname = "unknown";
        try {
            hostname = InetAddress.getLocalHost().getHostName();
        } catch (final UnknownHostException e) {
        }
        _PATH_SUFFIX = "." + hostname
                + "." + System.getProperty("user.name")
                + "." + System.getProperty("app.name")
                + ".";
    }

    private static final String _PATH_SUFFIX;

    @Inject
    private CuratorFramework _curator;

    private String _path;
    private String _template;

    private final Map<ObjectName,String> _createdPaths = new ConcurrentHashMap<>();
}
