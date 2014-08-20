package org.jocean.ext.ebus.unit;

import org.jocean.ext.ebus.EventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;

public class FsUnitSourceGroupManager {

    private static final Logger logger =
            LoggerFactory.getLogger(FsUnitSourceGroupManager.class);

    private Collection<String> pathes;
    private EventBus eventBus;
    private String eventUnitsChanged = "org.jocean.event.unit";
    private String prefix = null;
    private Collection<FilesystemUnitSourceGroup> groups =
            new ArrayList<>();
    private long timeout;

    private final boolean _enabled;

    public FsUnitSourceGroupManager(final boolean enabled) {
        this._enabled = enabled;
    }

    /**
     * @return the pathes
     */
    public Collection<String> getPathes() {
        return pathes;
    }

    /**
     * @param pathes the pathes to set
     */
    public void setPathes(Collection<String> pathes) {
        this.pathes = pathes;
    }

    /**
     * @return the eventBus
     */
    public EventBus getEventBus() {
        return eventBus;
    }

    /**
     * @param eventBus the eventBus to set
     */
    public void setEventBus(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    /**
     * @return the eventUnitsChanged
     */
    public String getEventUnitsChanged() {
        return eventUnitsChanged;
    }

    /**
     * @param eventUnitsChanged the eventUnitsChanged to set
     */
    public void setEventUnitsChanged(String eventUnitsChanged) {
        this.eventUnitsChanged = eventUnitsChanged;
    }

    /**
     * @return the prefix
     */
    public String getPrefix() {
        return prefix;
    }

    /**
     * @param prefix the prefix to set
     */
    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public void start() {
        if (this._enabled) {
            for (String path : pathes) {
                logger.debug("start {}", path);
                FilesystemUnitSourceGroup group = null;

                try {
                    group = new FilesystemUnitSourceGroup();

                    group.setCheckTimeout(timeout);
                    group.setDir(path);
                    group.setEventBus(eventBus);
                    group.setEventUnitsChanged(eventUnitsChanged);
                    group.setPrefix(prefix);
                    group.start();
                    groups.add(group);
                    // 	主动延时 10 毫秒， 在加载 下一个 unit 文件前，可基本认为当前 unit 已经初始化完成
                    //	可在一般情况下（unit文件初始化内容不太多时），保障 unit 按加载顺序进行先后初始化
                    //Thread.sleep(10);
                } catch (Exception e) {
                    logger.warn("failed to start fs unit group for path {}, reason {}", path, e);
                }
            }
        }
    }

    public void stop() {
        for (FilesystemUnitSourceGroup group : groups) {
            try {
                group.stop();
                if (logger.isDebugEnabled()) {
                    logger.debug("stop group {} succeed.", group.getDir());
                }
            } catch (Exception e) {
                logger.warn("failed to stop fs unit group for path {}", group.getDir());
            }
        }
        groups.clear();
    }

    /**
     * @return the timeout
     */
    public long getTimeout() {
        return timeout;
    }

    /**
     * @param timeout the timeout to set
     */
    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }
}
