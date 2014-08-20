package org.jocean.ext.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executor;

public class FunctorAsync implements ClosureEx {

    private static final Logger logger = LoggerFactory.getLogger(FunctorAsync.class);
    private Executor executor;
    private ClosureEx impl;

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return null != impl ? impl.toString() : "functorAsync(null)";
    }

    public void execute(final Object... args) {
        executor.execute(new Runnable() {

            public void run() {
                try {
                    impl.execute(args);
                } catch (Exception e) {
                    logger.error("execute:", e);
                }
            }
        });
    }

    public Executor getExecutor() {
        return executor;
    }

    public void setExecutor(Executor executor) {
        this.executor = executor;
    }

    public ClosureEx getImpl() {
        return impl;
    }

    public void setImpl(ClosureEx impl) {
        this.impl = impl;
    }

    public void setCanceled(boolean canceled) {
        this.impl.setCanceled(canceled);
    }
}
