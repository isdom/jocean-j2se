/**
 * 
 */
package org.jocean.j2se.jmx;

import java.lang.management.ManagementFactory;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.jocean.idiom.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author isdom
 *
 */
public class MBeanRegisterSupport implements MBeanRegister {
    
    private static final Logger LOG = 
            LoggerFactory.getLogger(MBeanRegisterSupport.class);
    
    public MBeanRegisterSupport(final String objectNamePrefix, final MBeanServer mbeanServer) {
        this._mbeanServer = (null != mbeanServer) ? mbeanServer 
                : ManagementFactory.getPlatformMBeanServer();
        this._objectNamePrefix = objectNamePrefix;
        if ( null == this._objectNamePrefix ) {
            LOG.warn("objectName prefix is null, registerMBean use suffix as full objectName.");
        }
    }
    
    private String createObjectName(final String suffix) {
        return  (null != _objectNamePrefix) ? (_objectNamePrefix + "," + suffix ) : suffix;
    }
    
    public  boolean registerMBean(final String suffix, final Object mbean) {
        
        if ( null == mbean ) {
            LOG.error("registerMBean with suffix({}) failed, mbean is null",  suffix);
            return  false;
        }
        
        this._rlock.lock();
        
        try {
            final ObjectName objectName = 
                    ObjectName.getInstance(createObjectName(suffix));
            if ( null != this._objectNames.putIfAbsent(objectName, mbean) ) {
                //  该ObjectName 已经或正在注册 mbean
                LOG.error("registerMBean {}/{} failed, ObjectName({}) already used",  
                        new Object[]{suffix, mbean, objectName});
                return  false;
            }
            
            try {
                this._mbeanServer.registerMBean(mbean, objectName);
            }
            catch (Exception e) {
                this._objectNames.remove(objectName);
                throw e;
            }
            
            if ( LOG.isDebugEnabled() ) {
                LOG.debug("register mbean {} with objectname {} succeed.", mbean, objectName);
            }
            
            return  true;
            
        } catch (Exception e) {
            LOG.error("exception when registerMBean {}/{}, detail: {}", 
                    new Object[]{suffix, mbean, ExceptionUtils.exception2detail(e)});
            return  false;
        }
        finally {
            this._rlock.unlock();
        }
    }
    
    public  void unregisterMBean(final String suffix) {
        this._rlock.lock();
        
        try {
            final ObjectName objectName = 
                    ObjectName.getInstance(createObjectName(suffix));
            if ( this._objectNames.containsKey(objectName) ) {
                this._mbeanServer.unregisterMBean(objectName);
                this._objectNames.remove(objectName);
                if ( LOG.isDebugEnabled() ) {
                    LOG.debug("unregister mbean by objectname {} succeed.", objectName);
                }
            }
            else {
                LOG.error("unregister mbean failed, bcs objectname({}) not exist or not register via this MBeanRegisterSupport.", 
                        objectName);
            }
        } catch (Exception e) {
            LOG.error("exception when unregisterMBean, detail: {}", 
                    ExceptionUtils.exception2detail(e));
        }
        finally {
            this._rlock.unlock();
        }
    }
    
    public boolean isRegistered(final String suffix) {
        try {
            final ObjectName objectName = ObjectName.getInstance(createObjectName(suffix));
            
            return (this._objectNames.containsKey(objectName) 
                    || this._mbeanServer.isRegistered( objectName ) );
        } catch (Exception e) {
            LOG.error("exception when test suffix({}) isRegistered, detail:{}", 
                    suffix, ExceptionUtils.exception2detail(e));
        }
        return  false;
    }

    public boolean replaceRegisteredMBean(final String suffix, final Object oldMBean, final Object newMBean) {
        this._rlock.lock();
        
        try {
            final ObjectName objectName = 
                    ObjectName.getInstance(createObjectName(suffix));
            if ( this._objectNames.containsKey(objectName) ) {
                if ( this._objectNames.replace(objectName, oldMBean, newMBean)) {
                    try {
                        this._mbeanServer.unregisterMBean(objectName);
                        this._mbeanServer.registerMBean(newMBean, objectName);
                        if ( LOG.isDebugEnabled() ) {
                            LOG.debug("replaceRegisteredMBean objectname {} succeed.", objectName);
                        }
                        return  true;
                    } catch (Exception e) {
                        this._objectNames.remove(objectName);
                        throw e;
                    }
                }
                else {
                    LOG.error("replaceRegisteredMBean mbean failed, bcs objectname({}) 's oldMBean not match.", 
                            objectName);
                }
            }
            else {
                LOG.error("replaceRegisteredMBean mbean failed, bcs objectname({}) not exist or not register via this MBeanRegisterSupport.", 
                        objectName);
            }
            return  false;
        } catch (Exception e) {
            LOG.error("exception when replaceRegisteredMBean, detail: {}", 
                    ExceptionUtils.exception2detail(e));
            return  false;
        }
        finally {
            this._rlock.unlock();
        }
    }
    
    public  Object  getMBean(final String suffix) {
        try {
            return this._objectNames.get(  ObjectName.getInstance(createObjectName(suffix) ));
        } catch (Exception e) {
            LOG.error("exception when getMBean for suffix {}, detail: {}",
                    suffix, ExceptionUtils.exception2detail(e));
            return  null;
        }
    }

    public  void unregisterAllMBeans() {
        this._wlock.lock();
        
        try {
            for ( Map.Entry<ObjectName, Object> entry : this._objectNames.entrySet() ) {
                try {
                    this._mbeanServer.unregisterMBean(entry.getKey());
                    if ( LOG.isDebugEnabled() ) {
                        LOG.debug("unregisterAllMBeans: unregister mbean with objectname {} succeed.", entry.getKey());
                    }
                } catch (Exception e) {
                    LOG.error("exception when (unregisterAllMBean|destroy)'s unregisterMBean({}), detail: {}", 
                            entry.getKey(), ExceptionUtils.exception2detail(e));
                }
            }
            _objectNames.clear();
        }
        finally {
            this._wlock.unlock();
        }
    }
    
    public  void destroy() {
        unregisterAllMBeans();
    }

    public String getObjectNamePrefix() {
        return  this._objectNamePrefix;
    }
    
    //  JMX support
    private final MBeanServer   _mbeanServer;
    
    //  FSM JMX's prefix
    private final String        _objectNamePrefix;

    private final ConcurrentMap<ObjectName, Object> _objectNames = 
            new ConcurrentHashMap<ObjectName, Object>();
    
    private final ReadWriteLock _lock = new ReentrantReadWriteLock();
    
    private final Lock  _rlock = _lock.readLock();
    
    private final Lock  _wlock = _lock.writeLock();
}
