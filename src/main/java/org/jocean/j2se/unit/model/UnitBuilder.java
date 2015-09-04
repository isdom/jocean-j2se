/**
 * 
 */
package org.jocean.j2se.unit.model;

import java.io.InputStream;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;

import org.jocean.idiom.ExceptionUtils;
import org.jocean.j2se.unit.UnitAgent;
import org.jocean.j2se.unit.UnitAgentMXBean.UnitMXBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import com.google.common.collect.Maps;

/**
 * @author isdom
 *
 */
public class UnitBuilder {
    
    private static final Logger LOG = LoggerFactory
            .getLogger(UnitBuilder.class);
    
    private static final String SPRING_XML_KEY = "__spring.xml";
    
    public UnitBuilder(final UnitAgent unitAgent) {
        this._unitAgent = unitAgent;
    }
    
    public void setLocation(final Resource location) {
        this._location = location;
    }
    
    public void start() {
        scheduleRebuild(0L);
    }
    
    private void scheduleRebuild(final long delay) {
        this._timer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    rebuildUnits();
                } catch (Exception e) {
                    LOG.warn("exception when rebuildUnits, detail:{}",
                            ExceptionUtils.exception2detail(e));
                }
            }}, delay);
    }
    
    private void rebuildUnits() throws Exception {
        try {
            if ( this._locationLastModified != this._location.lastModified() ) {
                this._locationLastModified = this._location.lastModified();
                
                final Yaml yaml = new Yaml(new Constructor(UnitDescription.class));
            
                try (final InputStream is = this._location.getInputStream()) {
                    final UnitDescription desc = (UnitDescription)yaml.load(is);
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("load unit description from location[{}]\n{}", 
                                this._location, desc);
                    }
                    if (null!=this._rootDesc) {
                        this._unitAgent.deleteUnit(this._rootDesc.getName());
                    }
                    this._rootDesc = desc;
                    build(desc, null);
                }
            }
        } finally {
            scheduleRebuild(1000L);
        }
    }
    
    public void stop() {
        this._timer.cancel();
    }
    
    private void build(final UnitDescription desc, final String parentPath) {
        final String pathName = null != parentPath 
                ? parentPath + desc.getName() 
                : desc.getName();
        final String template = getTemplateFromName(desc.getName());
        final Properties properties = desc.parametersAsProperties();
        final String[] source = genSourceFrom(properties);
        
        UnitMXBean unit = null;
        if (null != source ) {
            unit = this._unitAgent.createUnitWithSource(
                    pathName,
                    source,
                    Maps.fromProperties(properties));
        } else {
            unit = this._unitAgent.createUnit(
                    pathName,
                    new String[]{"**/"+ template + ".xml", template + ".xml"},
                    Maps.fromProperties(properties),
                    true);
        }
        if (null == unit) {
            LOG.info("create unit {} failed.", pathName);
        } else {
            LOG.info("create unit {} success with active status:{}", pathName, unit.isActive());
        }
        for ( UnitDescription child : desc.getChildren()) {
            build(child, pathName + "/");
        }
    }
    
    private static String[] genSourceFrom(final Properties properties) {
        final String value = properties.getProperty(SPRING_XML_KEY);
        properties.remove(SPRING_XML_KEY);
        return null!=value ? value.split(",") : null;
    }
    
    private String getTemplateFromName(final String name) {
      // for return value, eg:
      //  and gateway.1.2.3.4 --> gateway
      final int idx = name.indexOf('.');
      return ( idx > -1 ) ? name.substring(0, idx) : name;
    }
    
    private final UnitAgent _unitAgent;
    private UnitDescription _rootDesc = null;
    private Resource _location;
    private long _locationLastModified = 0;
    
    //false means the associated thread should !NOT! run as a daemon.
    private final Timer _timer = new Timer(false);
}
