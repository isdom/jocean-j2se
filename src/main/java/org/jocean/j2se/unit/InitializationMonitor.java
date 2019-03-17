package org.jocean.j2se.unit;

import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InitializationMonitor {

    private static final Logger LOG = LoggerFactory.getLogger(InitializationMonitor.class);

    public void beginInitialize(final Object agent) {
        final int count = this._counter.incrementAndGet();
        LOG.info("{}: {} beginInitialize", count, agent);
    }

    public void endInitialize(final Object agent) {
        final int count = this._counter.decrementAndGet();
        LOG.info("{}: {} endInitialize", count, agent);
        if (count == 0) {
            synchronized(this) {
                if (this._counter.get() == 0) {
                    this.notifyAll();
                }
            }
        }
    }

    public void await() throws InterruptedException {
        if (this._counter.get() > 0) {
            synchronized(this) {
                if (this._counter.get() > 0) {
                    this.wait();
                }
            }
        }
    }

    final AtomicInteger _counter = new AtomicInteger(0);
}
