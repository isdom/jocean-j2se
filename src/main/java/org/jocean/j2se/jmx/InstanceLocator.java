/**
 * 
 */
package org.jocean.j2se.jmx;

import java.lang.management.ManagementFactory;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.jocean.idiom.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author isdom
 *
 */
public class InstanceLocator {
    
    private static final String ATTR_INSTANCE_NAME = "Instance";

    private static final String DEFAULT_INSTANCE_PREFIX = "instance.locator:type=";
    
    private static final Logger LOG = 
            LoggerFactory.getLogger(InstanceLocator.class);
    
    public static interface LocatorMBean {
        public Object getInstance();
    }
    
    private static class Locator implements LocatorMBean {
        
        @Override
        public Object getInstance() {
            return  this._obj;
        }
        
        Locator(final Object obj) {
            this._obj = obj;
        }
        
        private final Object _obj;
    }
    
    /**
     * @param instanceType
     * @param instanceName
     * @return
     * @throws Exception 
     * @throws  
     */
    private static ObjectName objectNameOf(
            final String instanceType,
            final String instanceName) {
        try {
            return ObjectName.getInstance( DEFAULT_INSTANCE_PREFIX 
                    + instanceType + ",name=" + instanceName);
        } catch (Exception e) {
            LOG.error("failed to gen objectname for {}/{}, detail: {}",
                    new Object[]{instanceType, instanceName, ExceptionUtils.exception2detail(e) });
            return  null;
        }
    }
    
    public static void registerInstance(
            final String instanceType, 
            final String instanceName, 
            final Object instance) {
        final MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();
        
        try {
            mbeanServer.registerMBean(
                    new Locator(instance),
                    objectNameOf(instanceType, instanceName) );
        } catch (Exception e) {
            LOG.error("failed to registerInstance for {}, detail : {}", 
                    instance, ExceptionUtils.exception2detail(e));
        } 
    }

    public static void unregisterInstance(
            final String instanceType, 
            final String instanceName ) {
        final MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();
        
        try {
            mbeanServer.unregisterMBean( objectNameOf(instanceType, instanceName) );
        } catch (Exception e) {
            LOG.error("failed to unregisterInstance for {}, detail : {}", 
                    instanceName, ExceptionUtils.exception2detail(e));
        } 
    }

    @SuppressWarnings("unchecked")
    public static <RET> RET locateInstance(
            final String instanceType, 
            final String instanceName ) {
        final MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();
        
        final ObjectName on = objectNameOf(instanceType, instanceName);
        
        if ( mbeanServer.isRegistered( on ) ) {
            try {
                return (RET)mbeanServer.getAttribute( objectNameOf(instanceType, instanceName), ATTR_INSTANCE_NAME);
            } catch (Exception e) {
                LOG.error("failed to locateInstance for {}, detail : {}", 
                        instanceName, ExceptionUtils.exception2detail(e));
                return  null;
            }
        }
        else {
            return  null;
        }
    }
}
