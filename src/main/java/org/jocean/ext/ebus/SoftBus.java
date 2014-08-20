package org.jocean.ext.ebus;

import org.jocean.ext.util.ClosureEx;
import org.jocean.ext.util.Functor;
import org.jocean.ext.util.FunctorAsync;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

public class SoftBus implements EventBus {
    private static final Logger logger = LoggerFactory.getLogger(SoftBus.class);

    private ExecutorService mainExecutor = null;

    private static class ClosureExSet {
        private Map<UUID, ClosureEx> closures =
                new HashMap<>();

        public void add(UUID uuid, ClosureEx closure) {
            closures.put(uuid, closure);
        }

        public void remove(UUID uuid) {
            closures.remove(uuid);
        }

        public void execute(Object... args) {
            for (ClosureEx closure : closures.values()) {
                closure.execute(args);
            }
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            for (ClosureEx closure : closures.values()) {
                sb.append(closure.toString());
                sb.append(";");
            }

            return sb.toString();
        }
    }

    private Map<String, ClosureExSet> closureSet = new HashMap<>();

    public SoftBus() {
        mainExecutor =
                Executors.newFixedThreadPool(1, new ThreadFactory() {

                    public Thread newThread(Runnable r) {
                        return new Thread(r, "ebus main threads");
                    }
                });
    }

    private ClosureExSet getOrCreateClosureExSet(String event) {
        ClosureExSet set = closureSet.get(event);

        if (null == set) {
            set = new ClosureExSet();
            closureSet.put(event, set);
        }

        return set;
    }

    private ClosureExSet getClosureExSet(String event) {
        return closureSet.get(event);
    }

    private void doRegisterObserver(
            final String event, final UUID id, final ClosureEx closure) {
        getOrCreateClosureExSet(event).add(id, closure);
    }

    private void doRemoveObserver(final String event, final UUID id) {
        ClosureExSet set = getClosureExSet(event);

        if (null != set) {
            set.remove(id);
        }
    }

    public Runnable registerObserver(final Executor exec,
                                     final String event, final ClosureEx closure) {

        final FunctorAsync async = new FunctorAsync();
        async.setExecutor(exec);
        async.setImpl(closure);

        final UUID id = UUID.randomUUID();

        this.mainExecutor.submit(new Runnable() {

            public void run() {
                doRegisterObserver(event, id, async);
            }
        });

        return new Runnable() {

            public void run() {
                //	set canceled flag
                closure.setCanceled(true);
                //	and remove registered observer
                mainExecutor.submit(new Runnable() {

                    public void run() {
                        doRemoveObserver(event, id);
                    }
                });
            }
        };
    }

    public Runnable registerObserver(final Executor exec,
                                     final String event, final Object target, final String methodName) {

        return registerObserver(exec, event, new Functor(target, methodName));
    }

    private void doFireEvent(final String event, final Object... args) {
        ClosureExSet set = this.getClosureExSet(event);
        if (null != set) {
            set.execute(args);
        } else {
            if (logger.isDebugEnabled()) {
                logger.debug("event [" + event + "] not found any matched closure!");
            }
        }
    }

    public void fireEvent(final String event, final Object... args) {
        this.mainExecutor.submit(new Runnable() {

            public void run() {
                doFireEvent(event, args);
            }
        });
    }

    public int getPendingTaskCount() {
        if (this.mainExecutor instanceof ThreadPoolExecutor) {
            BlockingQueue<Runnable> queue = ((ThreadPoolExecutor) mainExecutor).getQueue();
            return queue.size();
        } else {
            throw new RuntimeException("Internal Erro : mainExecutor is !NOT! ThreadPoolExecutor class");
        }
    }

    private Map<String, String> doGetAllEvents() {
        Map<String, String> ret = new HashMap<>();
        for (Map.Entry<String, ClosureExSet> entry : this.closureSet.entrySet()) {
            ret.put(entry.getKey(), entry.getValue().toString());
        }

        return ret;
    }

    public Map<String, String> getAllEvents() throws InterruptedException, ExecutionException {
        return this.mainExecutor.submit(new Callable<Map<String, String>>() {

            public Map<String, String> call() throws Exception {
                return doGetAllEvents();
            }
        }).get();
    }
}
