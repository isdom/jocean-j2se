package org.jocean.ext.ebus.unit;

import org.jocean.ext.ebus.EventBus;
import org.jocean.ext.io.DirChangeListener;
import org.jocean.ext.io.DirMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.Iterator;

public class FilesystemUnitSourceGroup {

    private static final Logger logger = LoggerFactory.getLogger(FilesystemUnitSourceGroup.class);

    private DirMonitor monitor = new DirMonitor();
    private EventBus eventBus;
    private String eventUnitsChanged = "org.jocean.event.unit";

    private long timeout;

    private String path;

    private String prefix = null;

    private class InnerChange implements DirChangeListener {

        public void onDirChanged(String dirPath) {
            doDirChanged();
        }

    }

    private InnerChange innerChange = new InnerChange();

    private static class InnerIterator implements Iterator<UnitSource> {

        private File[] files;
        private int idx;

        InnerIterator(File[] files) {
            this.files = files.clone();
            idx = 0;
        }

        public boolean hasNext() {
            return idx < files.length;
        }

        public UnitSource next() {
            return new FilesystemUnitSource(files[idx++]);
        }

        public void remove() {
        }

    }

    public FilesystemUnitSourceGroup() {
        monitor.setDir("c:/eventUnits");
        monitor.setIncludeSuffixs(Arrays.asList(new String[]{
                ".bsh", ".xml", ".cfg", ".jar", ".zip"}));
        monitor.setListener(innerChange);
    }

    public EventBus getEventBus() {
        return eventBus;
    }

    public void setEventBus(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    public void setDir(String dirName) {
        this.path = dirName;
    }

    public String getDir() {
        return this.path;
    }

    public void setCheckTimeout(long checkTimeout) {
        this.timeout = checkTimeout;
    }

    public long getCheckTimeout() {
        return this.timeout;
    }

    public void start() {
        if ("".equals(this.path)) {
            if (logger.isInfoEnabled()) {
                logger.info("dir is empty, disable this UnitSource Group.");
            }
        } else {
            monitor.setCheckTimeout(this.timeout);
            monitor.setDir(null != this.prefix ? (this.prefix + this.path) : this.path);
            monitor.start();
        }
    }

    public void stop() {
        if (monitor.isRunning()) {
            monitor.stop();

            //	remove all files in directory
            eventBus.fireEvent(eventUnitsChanged, monitor.getDir(),
                    new InnerIterator(new File[0]));
        }
    }

    public boolean isRunning() {
        return monitor.isRunning();
    }

    private void doDirChanged() {
        File[] unitFiles = new File(monitor.getDir()).listFiles(new FilenameFilter() {

            public boolean accept(File dir, String name) {
                return (name.endsWith(".bsh")
                        || name.endsWith(".xml")
                        || name.endsWith(".cfg")
                        || name.endsWith(".jar")
                        || name.endsWith(".zip")
                );
            }
        });

        eventBus.fireEvent(eventUnitsChanged, monitor.getDir(), new InnerIterator(unitFiles));
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
}
