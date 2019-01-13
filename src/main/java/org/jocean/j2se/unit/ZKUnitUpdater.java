/**
 *
 */
package org.jocean.j2se.unit;

import java.io.ByteArrayInputStream;
import java.io.DataInput;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.io.FilenameUtils;
import org.jocean.j2se.unit.UnitAgentMXBean.UnitMXBean;
import org.jocean.j2se.zk.ZKAgent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import com.google.common.collect.Maps;
import com.google.common.io.ByteStreams;

/**
 * @author isdom
 *
 */
public class ZKUnitUpdater implements ZKAgent.Listener {

    private static final Logger LOG = LoggerFactory.getLogger(ZKUnitUpdater.class);

    private static final String SPRING_XML_KEY = "__spring.xml";

    public ZKUnitUpdater(final UnitAgent unitAgent) {
        this._unitAgent = unitAgent;
    }

    /**
     * @param data
     * @throws IOException
     */
    private Properties loadProperties(final byte[] data) throws IOException {
        try (final InputStream is = null != data ? new ByteArrayInputStream(data) : null) {
            return new Properties() {
                private static final long serialVersionUID = 1L;
            {
                if (null != is) {
                    this.load( is );
                }
            }};
        }
    }

    private Map<String, String> data2props(final byte[] data) throws IOException {
        if (null == data) {
            return Collections.emptyMap();
        }
        final DataInput di = ByteStreams.newDataInput(data);
        final String _1_line = di.readLine();
        if (null != _1_line && _1_line.startsWith("## yaml")) {
            LOG.debug("parse as yaml");
            // read as yaml
            final Yaml yaml = new Yaml();
            final Map<String, String> props = (Map<String, String>)yaml.loadAs(new ByteArrayInputStream(data), Map.class);
            LOG.debug("parse result: {}", props);
            return props;
        } else {
            return Maps.fromProperties(loadProperties(data));
        }
    }

    @Override
    public void onAdded(
            final ZKAgent agent,
            final String path,
            final byte[] data)
            throws Exception {
        final String pathName = parseSourceFromPath(agent.root(), path);
        if ( null != pathName ) {
            if ( LOG.isDebugEnabled()) {
                LOG.debug("creating unit named {}", pathName);
            }
            final String template = getTemplateFromFullPathName(pathName);
            final Map<String, String> props = data2props(data);
            final String[] source = genSourceFrom(props);
            UnitMXBean unit = null;
            if (null != source ) {
                unit = this._unitAgent.createUnitWithSource(pathName, source, props);
            } else {
                unit = this._unitAgent.createUnit(pathName,
                        new String[]{"**/"+ template + ".xml", template + ".xml"},
                        props,
                        true);
            }
            if (null == unit) {
                LOG.info("create unit {} failed.", pathName);
            } else {
                LOG.info("create unit {} success with active status:{}", pathName, unit.isActive());
            }

        }
    }

    @Override
    public void onUpdated(
            final ZKAgent agent,
            final String path,
            final byte[] data)
            throws Exception {
        final String pathName = parseSourceFromPath(agent.root(), path);
        if (null != pathName) {
            if ( LOG.isDebugEnabled()) {
                LOG.debug("updating unit named {}", pathName);
            }
            final Map<String, String> props = data2props(data);
            final String[] source = genSourceFrom(props);

            UnitMXBean unit = null;
            if (null != source ) {
                unit = this._unitAgent.updateUnitWithSource(pathName, source, props);
            } else {
                unit = this._unitAgent.updateUnit(pathName, props);
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
            final ZKAgent agent,
            final String path)
            throws Exception {
        final String pathName = parseSourceFromPath(agent.root(), path);
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

    private static String[] genSourceFrom(final Map<String, String> props) {
        final String value = props.get(SPRING_XML_KEY);
//        properties.remove(SPRING_XML_KEY);
        return null!=value ? value.split(",") : null;
    }

    private String parseSourceFromPath(final String root, final String path) {
        if (path.equals(root)) {
            return null;
        }
        return path.substring(root.length() + ( !root.endsWith("/") ? 1 : 0 ));
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
}
