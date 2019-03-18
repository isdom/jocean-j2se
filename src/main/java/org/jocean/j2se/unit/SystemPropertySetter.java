package org.jocean.j2se.unit;

import java.io.IOException;
import java.io.StringReader;

import org.springframework.beans.factory.annotation.Value;

public class SystemPropertySetter {

    @Value("${properties}")
    void setProperties(final String propertiesAsString) throws IOException {
        System.getProperties().load(new StringReader(propertiesAsString));
    }
}
