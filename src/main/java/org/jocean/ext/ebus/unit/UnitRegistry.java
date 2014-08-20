package org.jocean.ext.ebus.unit;

import org.apache.commons.io.IOUtils;
import org.jocean.ext.ebus.EventBus;
import org.jocean.idiom.Pair;
import org.jocean.idiom.Triple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

public class UnitRegistry {

    private static final Logger logger =
            LoggerFactory.getLogger(UnitRegistry.class);

    private UnitBuilder unitBuilder;

    private Map<String, Triple<EventUnit, Object, UnitSource>> units = new HashMap<>();

    private ExecutorService executorService;
    private EventBus eventBus;

    public UnitRegistry(final String name) {
        executorService = Executors.newFixedThreadPool(1, new ThreadFactory() {

            public Thread newThread(Runnable r) {
                ClassLoader cl = Thread.currentThread().getContextClassLoader();
                if (logger.isDebugEnabled()) {
                    logger.debug("{} try create thread, current thread's classloader: {}", name, cl);
                }
                return new Thread(r, name);
            }
        });

        Future<?> future = executorService.submit(new Runnable() {

            public void run() {
                if (logger.isDebugEnabled()) {
                    logger.debug("{} init thread call", name);
                }
            }
        });

        try {
            future.get();
        } catch (InterruptedException | ExecutionException e) {
            logger.error("failed to init {} thread for {}", name, e);
        }
    }

    private Pair<Boolean, String> doCreateAndRegisterUnit(UnitSource source) {
        if (null != units.get(source.getName())) {
            return Pair.of(false, "unit source named {" + source.getName() + "} already created.");
        }

        Pair<EventUnit, String> ret = unitBuilder.createUnit(source);

        if (null == ret.getFirst()) {
            //	not get unit
            return Pair.of(false, ret.getSecond());
        }

        //	try init unit
        EventUnit unit = ret.getFirst();

        Object ctx = null;
        try {
            ctx = unit.init(eventBus);
        } catch (Exception e) {
            return Pair.of(false, "failed to init unit : " + e.toString());
        }

        if (null == ctx) {
            return Pair.of(false, "failed to init unit.");
        }
        units.put(source.getName(), Triple.of(ret.getFirst(), ctx, source));

        return Pair.of(true, null);

    }

    private static void doDestroyUnit(Triple<EventUnit, Object, UnitSource> tuple) {
        tuple.getFirst().destroy(tuple.getSecond());
    }

    private Pair<Boolean, String> doDestroyAndUnregisterUnit(final String name) {
        Triple<EventUnit, Object, UnitSource> tuple = units.get(name);
        if (null == tuple) {
            return Pair.of(false, "unit named {" + name + "} not created.");
        }

        //	unregister
        units.remove(name);

        try {
            doDestroyUnit(tuple);
        } catch (Exception e) {
            return Pair.of(false, "failed to destroy unit {" + name + "}: " + e.toString());
        }

        return Pair.of(true, null);
    }

    public Pair<Boolean, String> createAndRegisterUnit(final UnitSource source) throws Exception {
        return executorService.submit(new Callable<Pair<Boolean, String>>() {

            public Pair<Boolean, String> call() throws Exception {
                return doCreateAndRegisterUnit(source);
            }
        }).get();
    }

    public Pair<Boolean, String> destroyAndUnregisterUnit(final String name) throws Exception {
        return executorService.submit(new Callable<Pair<Boolean, String>>() {

            public Pair<Boolean, String> call() throws Exception {
                return doDestroyAndUnregisterUnit(name);
            }
        }).get();
    }

    public void doDestroyAllUnits() {
        for (Map.Entry<String, Triple<EventUnit, Object, UnitSource>> itr : units.entrySet()) {
            if (logger.isDebugEnabled()) {
                logger.debug("try remove unit {}", itr.getKey());
            }
            Triple<EventUnit, Object, UnitSource> tuple = itr.getValue();

            try {
                doDestroyUnit(tuple);
            } catch (Exception e) {
                logger.error("failed to destroy unit {}, reason {}", itr.getKey(), e);
            }
        }

        units.clear();
    }

    public void destroy() throws Exception {
        try {
            executorService.submit(new Runnable() {
                public void run() {
                    doDestroyAllUnits();
                }
            }).get();
        } finally {
            executorService.shutdownNow();
        }
    }

    public String[] getAllUnits() throws Exception {
        return executorService.submit(new Callable<String[]>() {

            public String[] call() throws Exception {
                return units.keySet().toArray(new String[0]);
            }
        }).get();
    }

    private String doDumpSourceOfUnit(String name) throws IOException {
        Triple<EventUnit, Object, UnitSource> tuple = units.get(name);
        if (null == tuple) {
            return null;
        }

        return IOUtils.toString(tuple.getThird().getInputStream(), "UTF-8");
    }

    public String dumpSourceOfUnit(final String name) throws Exception {
        return executorService.submit(new Callable<String>() {

            public String call() throws Exception {
                return doDumpSourceOfUnit(name);
            }
        }).get();
    }

    public boolean isUnitExist(final String name) throws Exception {
        return executorService.submit(new Callable<Boolean>() {

            public Boolean call() throws Exception {
                return (null != units.get(name));
            }
        }).get();
    }

    /**
     * @return the unitBuilder
     */
    public UnitBuilder getUnitBuilder() {
        return unitBuilder;
    }

    /**
     * @param unitBuilder the unitBuilder to set
     */
    public void setUnitBuilder(UnitBuilder unitBuilder) {
        this.unitBuilder = unitBuilder;
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
}
