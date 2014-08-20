package org.jocean.ext.ebus.unit;


import org.jocean.ext.ebus.EventBus;
import org.jocean.ext.ebus.actors.AbstractEventActor;
import org.jocean.ext.util.SimpleRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Callable;

public class UnitGroupBooter extends AbstractEventActor {

    private static final Logger logger =
            LoggerFactory.getLogger(UnitGroupBooter.class);

    private Runnable canceler;
    private UnitGroupManager manager;
    private ApplicationContext rootAppCtx = null;
    private String eventUnitsChanged = "org.jocean.event.unit";

    public void setCfgXmlPath(String cfgXmlPath) {
        manager.setCfgXmlLocations(new String[]{cfgXmlPath});
    }

    public void setCfgXmlLocations(String[] locations) {
        manager.setCfgXmlLocations(locations);
    }

    /**
     * @return the rootAppCtx
     */
    public ApplicationContext getRootAppCtx() {
        return rootAppCtx;
    }

    /**
     * @param rootAppCtx the rootAppCtx to set
     */
    public void setRootAppCtx(ApplicationContext rootAppCtx) {
        this.rootAppCtx = rootAppCtx;
    }

    public UnitGroupBooter(final EventBus ebus, final SimpleRegistry registry) {
        super(ebus, "ebus booter");
        manager = new UnitGroupManager(ebus, registry);
    }

    public UnitGroupBooter(EventBus ebus, String name, final SimpleRegistry registry) {
        super(ebus, "ebus booter " + name);
        manager = new UnitGroupManager(ebus, registry);
    }

    @SuppressWarnings("unused")
    private void onEventGroup(String groupName, Iterator<UnitSource> itr) {
        if (logger.isDebugEnabled()) {
            logger.debug(groupName + " changed.");
        }
        manager.doUnitsChanged(groupName, itr, rootAppCtx);
    }

    public void start() {
        canceler = registerObserver(eventUnitsChanged, this, "onEventGroup");
    }

    @Override
    public void destroy() {
        try {
            canceler.run();
            manager.destroy();
        } catch (Exception e) {
            logger.error("destroy:", e);
        }

        super.destroy();
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

    public Map<String, String> getAllUnitGroup() throws Exception {

        return exec.submit(new Callable<Map<String, String>>() {

            public Map<String, String> call() throws Exception {
                return manager.getAllUnitGroup();
            }
        }).get();
    }
}
