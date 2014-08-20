package org.jocean.ext.ebus.actors;

import org.jocean.ext.ebus.EventBus;
import org.jocean.ext.util.ClosureEx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;

public class AbstractEventActor {
    private static final Logger logger = LoggerFactory.getLogger(AbstractEventActor.class);

    protected ExecutorService exec = null;

    private EventBus ebus;

    /**
     * @param ebus the ebus to set
     */
    public AbstractEventActor(EventBus ebus, final String name) {
        exec = Executors.newFixedThreadPool(1, new ThreadFactory() {

            public Thread newThread(Runnable r) {
                ClassLoader cl = Thread.currentThread().getContextClassLoader();
                if (logger.isDebugEnabled()) {
                    logger.debug("{} try create thread, current thread's classloader: {}", name, cl);
                }
                return new Thread(r, name);
            }
        });

        Future<?> future = exec.submit(new Runnable() {

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

        this.ebus = ebus;
    }

    protected Runnable registerObserver(String event, Object target, String methodName) {
        return ebus.registerObserver(exec, event, target, methodName);
    }

    protected Runnable registerObserver(String event, ClosureEx closure) {
        return ebus.registerObserver(exec, event, closure);
    }

    public void destroy() {
        exec.shutdownNow();
    }

    protected EventBus getEventBus() {
        return ebus;
    }

    public int getPendingTaskCount() {
        if (exec instanceof ThreadPoolExecutor) {
            BlockingQueue<Runnable> queue = ((ThreadPoolExecutor) exec).getQueue();
            return queue.size();
        } else {
            throw new RuntimeException("Internal Error : exec is !NOT! ThreadPoolExecutor class");
        }
    }
}
