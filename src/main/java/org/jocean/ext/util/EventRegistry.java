package org.jocean.ext.util;

import org.jocean.event.api.EventReceiver;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class EventRegistry {
    private ConcurrentMap<UUID, EventReceiver> _flows;
    private volatile static EventRegistry instance;

    private EventRegistry() {
    }

    public static EventRegistry getInstance() {
        if (instance == null) {
            synchronized (EventRegistry.class) {
                if (instance == null) {
                    instance = new EventRegistry();
                    instance._flows = new ConcurrentHashMap<>();
                }
            }
        }
        return instance;
    }

    public EventReceiver getReceiver(UUID key) {
        return _flows.get(key);
    }

    public EventReceiver addEventReceiver(UUID key, EventReceiver eventReceiver) {
        return _flows.putIfAbsent(key, eventReceiver);
    }

    public EventReceiver remove(UUID key) {
        return _flows.remove(key);
    }
}
