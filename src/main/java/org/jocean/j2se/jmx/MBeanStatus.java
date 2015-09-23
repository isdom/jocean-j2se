package org.jocean.j2se.jmx;

import javax.management.ObjectName;

public interface MBeanStatus {
    public ObjectName mbeanName();
    
    public boolean isRegistered();
    
    public boolean isUnregistered();
    
    public Object getValue(final String attributeOrMethodName);
}
