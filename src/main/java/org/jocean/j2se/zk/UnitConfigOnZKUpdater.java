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
import org.jocean.j2se.jmx.MBeanPublisher;
import org.jocean.j2se.jmx.MBeanStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import com.google.common.base.Charsets;

import rx.Subscription;

public class UnitConfigOnZKUpdater implements MBeanRegisterAware {

    private static final Logger LOG = LoggerFactory.getLogger(UnitConfigOnZKUpdater.class);

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("UnitConfigOnZKUpdater [objectName=").append(_objectName)
                .append(", notificationType=") .append(_notificationType)
                .append(", path=").append(_path)
                .append(", template=").append(_template)
                .append("]");
        return builder.toString();
    }

    public Subscription start() {
        return _publisher.watch(this._objectName, this._notificationType).subscribe(mbeanStatus -> processStatus(mbeanStatus),
                e -> LOG.warn("exception when subscriber MBeanStatus for {}, detail:{}",
                        this, ExceptionUtils.exception2detail(e)),
                () -> {
                    LOG.info("Subscriber MBeanStatus for path {} onCompleted", this._path);
                    removeAllZKPath();
                });
    }

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

    private void processStatus(final MBeanStatus mbeanStatus) {
        if ( mbeanStatus.status() == MBeanStatus.MS_REGISTERED ) {
            createZKPath(mbeanStatus);
        } else if (mbeanStatus.status() == MBeanStatus.MS_CHANGED) {
            updateZKPath(mbeanStatus);
        } else if (mbeanStatus.status() == MBeanStatus.MS_UNREGISTERED) {
            removeZKPath(mbeanStatus.mbeanName());
        }
    }

    private void createZKPath(final MBeanStatus mbeanStatus) {
        final PlaceholderResolver placeholderResolver = buildResolver(mbeanStatus);
        final PropertyPlaceholderHelper placeholderReplacer = new PropertyPlaceholderHelper("{", "}");
        String config = placeholderReplacer.replacePlaceholders(null, this._template, placeholderResolver, null);

        // for generate JSON format config
        if (config.isEmpty()) {
            config = new PropertyPlaceholderHelper("${", "}").replacePlaceholders(null, this._template, placeholderResolver, null);
        }

        try {
            final String createdPath = this._curator.create()
                .creatingParentsIfNeeded()
                .withMode(CreateMode.EPHEMERAL_SEQUENTIAL)
                .forPath(placeholderReplacer.replacePlaceholders(null, this._path, placeholderResolver, null),
                    config.getBytes(Charsets.UTF_8));
            addZKPath(mbeanStatus.mbeanName(), createdPath);
            LOG.info("create config for path {}, config\n{}", createdPath, config);
        } catch (final Exception e) {
            LOG.warn("exception when create config for {}, detail:{}", this, ExceptionUtils.exception2detail(e));
        }
    }

    private void updateZKPath(final MBeanStatus mbeanStatus) {
        final PlaceholderResolver placeholderResolver = buildResolver(mbeanStatus);
        final PropertyPlaceholderHelper placeholderReplacer = new PropertyPlaceholderHelper("{", "}");
        String config = placeholderReplacer.replacePlaceholders(null, this._template, placeholderResolver, null);

        // for generate JSON format config
        if (config.isEmpty()) {
            config = new PropertyPlaceholderHelper("${", "}").replacePlaceholders(null, this._template, placeholderResolver, null);
        }

        try {
            final String path = this._createdPaths.get(mbeanStatus.mbeanName());
            if (null!=path) {
                this._curator.setData().forPath(path, config.getBytes(Charsets.UTF_8));
            }
            LOG.info("update config for path {}, config\n{}", path, config);
        } catch (final Exception e) {
            LOG.warn("exception when update config for {}, detail:{}", this, ExceptionUtils.exception2detail(e));
        }
    }

    private void addZKPath(final ObjectName mbeanName, final String createdPath) {
        this._createdPaths.put(mbeanName, createdPath);
        LOG.info("add path:{} for mbeanName {}", createdPath, mbeanName);
    }

    private void removeZKPath(final ObjectName mbeanName) {
        final String path = this._createdPaths.remove(mbeanName);
        if (null!=path) {
            try {
                this._curator.delete()
                    .deletingChildrenIfNeeded()
                    .forPath(path);
                LOG.info("delete config for path {}", path);
            } catch (final Exception e) {
                LOG.warn("exception when delete config for path {}, detail:{}", path, ExceptionUtils.exception2detail(e));
            }
        }
    }

    private void removeAllZKPath() {
        while (!this._createdPaths.keySet().isEmpty()) {
            removeZKPath(this._createdPaths.keySet().iterator().next());
        }
    }

    private PlaceholderResolver buildResolver(final MBeanStatus mbeanStatus) {
        return new PlaceholderResolver() {
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
    private MBeanPublisher _publisher;

    @Inject
    private CuratorFramework _curator;

    @Value("${mbean.name}")
    void setObjectName(final String on) throws Exception {
        this._objectName = ObjectName.getInstance(on);
    }

    @Value("${path}")
    public void setPath(final String path) {
        this._path = path + _PATH_SUFFIX;
    }

    ObjectName _objectName;

    @Value("${notification.type}")
    String _notificationType = null;

    String _path;

    @Value("${template}")
    String _template;

    private final Map<ObjectName,String> _createdPaths = new ConcurrentHashMap<>();
}
