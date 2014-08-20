package org.jocean.ext.util;

import java.util.UUID;

public class AbstractIdentifyable implements MutableIdentifyable {

    private UUID uuid = UUID.randomUUID();

    public void setIdentification(UUID id) {
        this.uuid = id;
    }

    public UUID getIdentification() {
        return uuid;
    }
}
