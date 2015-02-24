/**
 * 
 */
package org.jocean.j2se.stats;

import java.util.ArrayList;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.DynamicMBean;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.ReflectionException;
import javax.management.openmbean.OpenMBeanAttributeInfoSupport;
import javax.management.openmbean.OpenMBeanConstructorInfoSupport;
import javax.management.openmbean.OpenMBeanInfoSupport;
import javax.management.openmbean.OpenMBeanParameterInfoSupport;
import javax.management.openmbean.SimpleType;

import org.jocean.idiom.ExceptionUtils;
import org.jocean.idiom.stats.BizMemoImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author isdom
 *
 */
public abstract class BizMemoSupportMBean<IMPL extends BizMemoImpl<IMPL,STEP,RESULT>, 
    STEP extends Enum<STEP>, RESULT extends Enum<RESULT>> 
    extends BizMemoImpl<IMPL,STEP,RESULT> {
    
    private static final Logger LOG = LoggerFactory
            .getLogger(BizMemoSupportMBean.class);
    
    public BizMemoSupportMBean(final Class<STEP> clsStep, final Class<RESULT> clsResult) {
        super(clsStep, clsResult);
    }
    
    public DynamicMBean createMBean() {
        return new DynamicMBean() {

            @Override
            public Object getAttribute(final String attribute)
                    throws AttributeNotFoundException, MBeanException,
                    ReflectionException {
                final String splited[] = attribute.split(":");
                final String type = splited[0];
                final String name = splited[1];
                return name2Integer(type, name).get();
            }

            @Override
            public void setAttribute(final Attribute attribute)
                    throws AttributeNotFoundException,
                    InvalidAttributeValueException, MBeanException,
                    ReflectionException {
            }

            @Override
            public AttributeList getAttributes(final String[] attributes) {
                return new AttributeList() {
                    private static final long serialVersionUID = 1L;
                    {
                        for ( String attrname : attributes ) {
                            try {
                                this.add( new Attribute(attrname, getAttribute(attrname)));
                            } catch (Exception e) {
                                LOG.warn("exception when create Attribute({}), detail:{}", 
                                        attrname, ExceptionUtils.exception2detail(e));
                            }
                        }
                    }
                };
            }

            @Override
            public AttributeList setAttributes(final AttributeList attributes) {
                return null;
            }

            @Override
            public Object invoke(final String actionName, final Object[] params,
                    final String[] signature) throws MBeanException,
                    ReflectionException {
                return null;
            }

            @Override
            public MBeanInfo getMBeanInfo() {
                final OpenMBeanAttributeInfoSupport[] attributes = new ArrayList<OpenMBeanAttributeInfoSupport>() {
                    private static final long serialVersionUID = 1L;
                {
                    for ( STEP step : _steps ) {
                        this.add(createCountAttribute(step));
                    }
                    for ( RESULT result : _results ) {
                        this.add(createCountAttribute(result));
                    }
                }
                /**
                 * @param stepOrResult
                 * @return
                 */
                private OpenMBeanAttributeInfoSupport createCountAttribute(
                        Enum<?> stepOrResult) {
                    return new OpenMBeanAttributeInfoSupport(
                        stepOrResult.getClass().getSimpleName() + ":" + stepOrResult.name(), 
                        stepOrResult.getClass().getCanonicalName() + "." +stepOrResult.name(), 
                        SimpleType.INTEGER, true, false, 
                        false);
                }}.toArray(new OpenMBeanAttributeInfoSupport[0]);
             
            //No arg constructor     
                final OpenMBeanConstructorInfoSupport[] constructors = new OpenMBeanConstructorInfoSupport[]{
                    new OpenMBeanConstructorInfoSupport("BizMemo", "Constructs a BizMemo instance.", 
                            new OpenMBeanParameterInfoSupport[0])
                };
             
            //Build the info 
                return new OpenMBeanInfoSupport(BizMemoImpl.class.getName(), 
                            "BizMemo - Open MBean", attributes, constructors, 
                            null, null);
            }};
    }
}
