/**
 * 
 */
package org.jocean.j2se.unit;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.commons.io.FilenameUtils;
import org.jocean.j2se.unit.UnitAgentMXBean.UnitMXBean;
import org.jocean.j2se.zk.ZKAgent.Listener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;

/**
 * @author isdom
 *
 */
public class ZKUnitUpdater implements Listener {

    private static final Logger LOG = LoggerFactory
            .getLogger(ZKUnitUpdater.class);
    
    private static final String SPRING_XML_KEY = "__spring.xml";
    
    public ZKUnitUpdater(final UnitAgent unitAgent) {
        this._unitAgent = unitAgent;
    }
    
    public void setRoot(final String root) {
        this._rootPath = root;
    }
    
    /**
     * @param data
     * @throws IOException
     */
    private Properties loadProperties(final byte[] data) throws IOException {
        try (
            final InputStream is = null != data
                    ? new ByteArrayInputStream(data) 
                    : null;
        ) {
            return new Properties() {
                private static final long serialVersionUID = 1L;
            {
                if (null != is) {
                    this.load( is );
                }
            }};
        }
    }
    
    @Override
    public void onAdded(
            final String path,
            final byte[] data)
            throws Exception {
        final String pathName = parseSourceFromPath(path);
        if ( null != pathName ) {
            if ( LOG.isDebugEnabled()) {
                LOG.debug("creating unit named {}", pathName);
            }
            final String template = getTemplateFromFullPathName(pathName);
            final Properties properties = loadProperties(data);
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
            
        }
    }

    private static String[] genSourceFrom(final Properties properties) {
        final String value = properties.getProperty(SPRING_XML_KEY);
        properties.remove(SPRING_XML_KEY);
        return null!=value ? value.split(",") : null;
    }

    @Override
    public void onUpdated(
            final String path,
            final byte[] data)
            throws Exception {
        final String pathName = parseSourceFromPath(path);
        if (null != pathName) {
            if ( LOG.isDebugEnabled()) {
                LOG.debug("updating unit named {}", pathName);
            }
            final Properties properties = loadProperties(data);
            final String[] source = genSourceFrom(properties);
            
            UnitMXBean unit = null;
            if (null != source ) {
                unit = this._unitAgent.updateUnitWithSource(
                        pathName,
                        source,
                        Maps.fromProperties(properties));
            } else {
                unit = this._unitAgent.updateUnit(
                        pathName,
                        Maps.fromProperties(properties));
            }
            if (null == unit) {
                LOG.info("update unit {} failed.", pathName);
            } else {
                LOG.info("update unit {} success with active status:{}", pathName, unit.isActive());
            }
        }
    }
    
    @Override
    public void onRemoved(
            final String path)
            throws Exception {
        final String pathName = parseSourceFromPath(path);
        if (null != pathName) {
            if ( LOG.isDebugEnabled()) {
                LOG.debug("removing unit for {}", pathName);
            }
            if ( this._unitAgent.deleteUnit(pathName) ) {
                LOG.info("remove unit {} success", pathName);
            }
            else {
                LOG.info("remove unit {} failure", pathName);
            }
        }
    }

    private String parseSourceFromPath(final String path) {
        if (path.length() <= this._rootPath.length() ) {
            return null;
        }
        return path.substring(this._rootPath.length() + ( !this._rootPath.endsWith("/") ? 1 : 0 ));
    }

    private String getTemplateFromFullPathName(final String fullPathName) {
//        a/b/c.txt --> c.txt
//        a.txt     --> a.txt
//        a/b/c     --> c
//        a/b/c/    --> ""
        // for return value, eg:
        //  and gateway.1.2.3.4 --> gateway
        final String  template = FilenameUtils.getName(fullPathName);
        final int idx = template.indexOf('.');
        return ( -1 != idx ) ? template.substring(0, idx) : template;
    }
    
    private final UnitAgent _unitAgent;
    private String _rootPath;
}
