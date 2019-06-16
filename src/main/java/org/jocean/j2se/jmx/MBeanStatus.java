package org.jocean.j2se.jmx;

import javax.management.ObjectName;

public interface MBeanStatus {
    final static int MS_REGISTERED = 1;
    final static int MS_UNREGISTERED = 2;
    final static int MS_CHANGED = 3;

    public ObjectName mbeanName();

    public int status();

    public Object getValue(final String attributeOrMethodName);
}
