package org.jocean.j2se.eventbus;

import com.google.common.eventbus.EventBus;

public interface EventBusAware {
    public void setEventBus(final EventBus eventbus);
}
