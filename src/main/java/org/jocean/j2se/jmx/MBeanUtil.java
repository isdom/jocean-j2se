/**
 * 
 */
package org.jocean.j2se.jmx;

import javax.management.JMException;
import javax.management.MBeanException;
import javax.management.ObjectName;
import javax.management.modelmbean.ModelMBean;
import javax.management.modelmbean.ModelMBeanInfo;
import javax.management.modelmbean.RequiredModelMBean;

import org.jocean.idiom.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jmx.export.MBeanExportException;
import org.springframework.jmx.export.assembler.MBeanInfoAssembler;
import org.springframework.jmx.export.assembler.SimpleReflectiveMBeanInfoAssembler;

/**
 * @author isdom
 *
 */
public class MBeanUtil {
    
    private static final Logger LOG = 
            LoggerFactory.getLogger(MBeanUtil.class);
    
    private MBeanUtil() {
        throw new IllegalStateException("No instances!");
    }
    
    public static ObjectName safeGetObjectName(final String objname) {
        try {
            return ObjectName.getInstance( objname );
        } catch (Exception e) {
            LOG.error("exception when ObjectName.getInstance {}, detail: {}", 
                    objname, ExceptionUtils.exception2detail(e));
            return  null;
        }
    }
    
    private final static MBeanInfoAssembler _ASSEMBLER = 
            new SimpleReflectiveMBeanInfoAssembler();
        
    private static ModelMBeanInfo getMBeanInfo(final Object managedBean, final String beanKey) 
            throws JMException {
        final ModelMBeanInfo info = _ASSEMBLER.getMBeanInfo(managedBean, beanKey);
//        if (logger.isWarnEnabled() && ObjectUtils.isEmpty(info.getAttributes()) &&
//                ObjectUtils.isEmpty(info.getOperations())) {
//            LOG.warn("Bean with key '" + beanKey +
//                    "' has been registered as an MBean but has no exposed attributes or operations");
//        }
        return info;
    }
        
    private static ModelMBean createModelMBean() throws MBeanException {
        return new RequiredModelMBean();
//        return (this.exposeManagedResourceClassLoader ? new SpringModelMBean() : new RequiredModelMBean());
    }
    
    public static ModelMBean createAndConfigureMBean(final Object managedResource, String beanKey)
            throws MBeanExportException {
        try {
            final ModelMBean mbean = createModelMBean();
            mbean.setModelMBeanInfo(getMBeanInfo(managedResource, beanKey));
            mbean.setManagedResource(managedResource, "ObjectReference");
            return mbean;
        }
        catch (Exception ex) {
            throw new MBeanExportException("Could not create ModelMBean for managed resource [" +
                    managedResource + "] with key '" + beanKey + "'", ex);
        }
    }

    public static ModelMBean createAndConfigureMBean(final Object managedResource)
            throws MBeanExportException {
        return createAndConfigureMBean(managedResource, managedResource.getClass().getName());
    }
}
