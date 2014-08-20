package org.jocean.ext.ebus.unit;

import org.jocean.ext.ebus.EventBus;

public interface EventUnit {

    public Object init(EventBus eventBus);

    public void destroy(Object ctx);

    public String getDescription();
}
