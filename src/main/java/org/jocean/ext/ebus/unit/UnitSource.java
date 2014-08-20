package org.jocean.ext.ebus.unit;

import java.io.InputStream;

public interface UnitSource {

    String getName();

    InputStream getInputStream();

    long lastModified();

    String getType();
}
