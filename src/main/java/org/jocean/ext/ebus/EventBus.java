package org.jocean.ext.ebus;

import java.util.concurrent.Executor;

import org.jocean.ext.util.ClosureEx;

public interface EventBus {

    public Runnable registerObserver(Executor exec, String event, ClosureEx closure);

    public Runnable registerObserver(Executor exec, String event, Object target, String methodName);

    public void fireEvent(String event, Object... args);
}
