package org.jocean.ext.ebus.unit;

import org.jocean.ext.ebus.EventBus;
import org.jocean.ext.util.SimpleRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class UnitGroupManager {

    private static final Logger logger = LoggerFactory.getLogger(UnitGroupManager.class);

    private final EventBus eventBus;
    private final SimpleRegistry registry;
    private String[] cfgXmlLocations = new String[]{"buildinUnits/"};

    static private class Tuple {
        public EventUnit unit;
        public Object ctx;
        public long lastModified;

        public Tuple(EventUnit u, Object c, long l) {
            unit = u;
            ctx = c;
            lastModified = l;
        }
    }

    private Map<String, Map<String, Tuple>> groups = new HashMap<>();

    private void doAddUnit(Map<String, Tuple> newer, String key, EventUnit unit, long l) {
        newer.put(key, new Tuple(unit, unit.init(eventBus), l));
    }

    private void addUnit(Map<String, Tuple> newer, UnitSource source, ApplicationContext rootAppCtx) {

        if (null != newer.get(source.getName())) {
            logger.error("addUnit: key [" + source.getName() + "] already exist.");
            return;
        }

        if (source.getName().endsWith(".cfg")) {
            try {
                doAddUnit(newer, source.getName(),
                        new PropertiesEventUnitImpl(source, cfgXmlLocations)
                                .setUnitAdmin((UnitAdmin) this.registry.getEntry("__unit_admin")),
                        source.lastModified());
            } catch (Exception e) {
                logger.error("addUnit:", e);
            }
        } else {
            logger.error(source + " must be .cfg(properties file)");
        }
    }

    private Map<String, Tuple> getUnitGroup(String groupName) {
        Map<String, Tuple> group = groups.get(groupName);
        if (null == group) {
            group = new HashMap<>();
            groups.put(groupName, group);
        }
        return group;
    }

    private void replaceUnitGroup(String groupName, Map<String, Tuple> newer) {
        groups.put(groupName, newer);
    }

    public void doUnitsChanged(String groupName, Iterator<UnitSource> itr, ApplicationContext rootAppCtx) {
        Map<String, Tuple> units = getUnitGroup(groupName);

        Map<String, Tuple> newer = new HashMap<>();
        while (itr.hasNext()) {
            UnitSource source = itr.next();

            Tuple tuple = units.get(source.getName());
            if (null != tuple) {
                if (tuple.lastModified != source.lastModified()) {
                    try {
                        tuple.unit.destroy(tuple.ctx);
                    } catch (Exception e) {
                        logger.error("failed to destroy unit {}, bcs of {}",
                                tuple.unit.getDescription(),
                                e);
                    }
                    addUnit(newer, source, rootAppCtx);
                    if (logger.isDebugEnabled()) {
                        logger.debug("Unit:" + groupName + "." + source.getName() + " updated.");
                    }
                } else {
                    newer.put(source.getName(), tuple);
                    if (logger.isDebugEnabled()) {
                        logger.debug("Unit:" + groupName + "." + source.getName() + " !NOT! changed.");
                    }
                }
                units.remove(source.getName());
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug("Unit:" + groupName + "." + source.getName() + " createing.");
                }
                addUnit(newer, source, rootAppCtx);
                if (logger.isDebugEnabled()) {
                    logger.debug("Unit:" + groupName + "." + source.getName() + " created.");
                }
            }
        }
        for (Map.Entry<String, Tuple> entry : units.entrySet()) {
            Tuple tuple = entry.getValue();
            try {
                tuple.unit.destroy(tuple.ctx);
            } catch (Exception e) {
                logger.error("failed to destroy unit {}, bcs of {}",
                        tuple.unit.getDescription(),
                        e);
            }
            if (logger.isDebugEnabled()) {
                logger.debug("Unit:" + groupName + "." + entry.getKey() + " has been removed, so destroy.");
            }
        }
        units.clear();
        replaceUnitGroup(groupName, newer);
    }

    public UnitGroupManager(final EventBus eventBus, final SimpleRegistry registry) {
        this.eventBus = eventBus;
        this.registry = registry;
    }

    /**
     * @return the cfgXmlpath
     */
    public String[] getCfgXmlLocations() {
        return cfgXmlLocations;
    }

    /**
     * @param cfgXmlLocations the cfgXmlpath to set
     */
    public void setCfgXmlLocations(String[] cfgXmlLocations) {
        this.cfgXmlLocations = cfgXmlLocations;
    }

    public void destroy() {
        for (Map.Entry<String, Map<String, Tuple>> entry : groups.entrySet()) {
            if (logger.isDebugEnabled()) {
                logger.debug("group name is {}", entry.getKey());
            }
            Map<String, Tuple> units = entry.getValue();
            for (Map.Entry<String, Tuple> itr : units.entrySet()) {
                if (logger.isDebugEnabled()) {
                    logger.debug("try remove unit {}", itr.getKey());
                }
                Tuple tuple = itr.getValue();

                try {
                    tuple.unit.destroy(tuple.ctx);
                } catch (Exception e) {
                    logger.error("failed to destroy unit {}, reason {}", itr.getKey(), e);
                }
            }
            units.clear();
        }

        groups.clear();
    }

    public Map<String, String> getAllUnitGroup() {

        Map<String, String> ret = new HashMap<>();

        for (Map.Entry<String, Map<String, Tuple>> entry : groups.entrySet()) {

            Map<String, Tuple> units = entry.getValue();
            for (Map.Entry<String, Tuple> itr : units.entrySet()) {
                Tuple tuple = itr.getValue();

                ret.put(entry.getKey() + "-" + itr.getKey(),
                        tuple.unit.getDescription() + ":" + new Date(tuple.lastModified));
            }
        }

        return ret;
    }
}
