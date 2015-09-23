/**
 * 
 */
package org.jocean.j2se.jmx;

import javax.management.ObjectName;

import org.jocean.idiom.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author isdom
 *
 */
public class MBeanUtil {
    
    private static final Logger LOG = 
            LoggerFactory.getLogger(MBeanUtil.class);
    
    public static ObjectName safeGetObjectName(final String objname) {
        try {
            return ObjectName.getInstance( objname );
        } catch (Exception e) {
            LOG.error("exception when ObjectName.getInstance {}, detail: {}", 
                    objname, ExceptionUtils.exception2detail(e));
            return  null;
        }
    }
    

}
