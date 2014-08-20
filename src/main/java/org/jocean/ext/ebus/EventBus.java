package org.jocean.ext.ebus;

import org.jocean.ext.util.ClosureEx;

import java.util.concurrent.Executor;

public interface EventBus {

    public Runnable registerObserver(Executor exec, String event, ClosureEx closure);

    public Runnable registerObserver(Executor exec, String event, Object target, String methodName);

    public void fireEvent(String event, Object... args);
}
