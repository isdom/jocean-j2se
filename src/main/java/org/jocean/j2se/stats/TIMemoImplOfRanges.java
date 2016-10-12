/**
 * 
 */
package org.jocean.j2se.stats;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

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
import org.jocean.idiom.stats.TimeIntervalMemo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Range;

/**
 * @author isdom
 *
 */
public class TIMemoImplOfRanges implements TimeIntervalMemo {

    private static final Logger LOG = LoggerFactory
            .getLogger(TIMemoImplOfRanges.class);

    public TIMemoImplOfRanges(final String[] rangeNames, final Range<Long>[] ranges) {
        if ( rangeNames.length != ranges.length ) {
            throw new RuntimeException("mismatch range names & ranges count.");
        }
        this._names = rangeNames;
        this._ranges = ranges;
        this._counters = new AtomicInteger[ranges.length];
        for ( int idx = 0; idx < this._counters.length; idx++) {
            this._counters[idx] = new AtomicInteger(0);
        }
    }
    
    @Override
    public void recordInterval(final long interval) {
        final int idx = interval2idx(interval);
        if ( idx >= 0) {
            this._counters[idx].incrementAndGet();
        }
    }
    
    public DynamicMBean createMBean() {
        return new DynamicMBean() {

            @Override
            public Object getAttribute(final String attribute)
                    throws AttributeNotFoundException, MBeanException,
                    ReflectionException {
                return name2Integer(attribute).get();
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
                    for ( int idx = 0; idx < _names.length; idx++ ) {
                        this.add(createCountAttribute(idx));
                    }
                }
                /**
                 * @param stepOrResult
                 * @return
                 */
                private OpenMBeanAttributeInfoSupport createCountAttribute(
                        final int idx ) {
                    return new OpenMBeanAttributeInfoSupport(
                        _names[idx], 
                        _names[idx], 
                        SimpleType.INTEGER, 
                        true, 
                        false, 
                        false);
                }}.toArray(new OpenMBeanAttributeInfoSupport[0]);
             
            //No arg constructor     
                final OpenMBeanConstructorInfoSupport[] constructors = new OpenMBeanConstructorInfoSupport[]{
                    new OpenMBeanConstructorInfoSupport("TIMemo", "Constructs a TimeIntervalMemo instance.", 
                            new OpenMBeanParameterInfoSupport[0])
                };
             
            //Build the info 
                return new OpenMBeanInfoSupport(TIMemoImplOfRanges.class.getName(), 
                            "TimeIntervalMemo - Open MBean", attributes, constructors, 
                            null, null);
            }};
    }
    
    private int interval2idx(final long interval) {
        int idx = 0;
        for (Range<Long> r : this._ranges) {
            if ( r.contains(interval) ) {
                return idx;
            }
            idx++;
        }
        return -1;
    }
    
    private AtomicInteger name2Integer(final String name) {
        return this._counters[Integer.parseInt( name.substring(0, name.indexOf('_')))];
    }
    
    protected final String[] _names;
    protected final Range<Long>[] _ranges;
    protected final AtomicInteger[] _counters;
}
