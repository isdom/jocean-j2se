package org.jocean.ext.util;

import java.util.UUID;

public interface MutableIdentifyable extends Identifyable {
    public void setIdentification(UUID id);
}
