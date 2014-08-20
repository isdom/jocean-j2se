package org.jocean.ext.ebus.unit;

import org.apache.commons.io.FilenameUtils;
import org.jocean.ext.ebus.EventBus;
import org.jocean.ext.util.PackageUtils;
import org.jocean.idiom.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class PropertiesEventUnitImpl implements EventUnit, UnitSourceAware {

    private static final Logger LOG =
    		 LoggerFactory.getLogger(PropertiesEventUnitImpl.class);

        private UnitSource source;
        private String[]					xmlLocations;
        private UnitAdmin unitAdmin;

        public PropertiesEventUnitImpl(
                UnitSource source,
                String[] xmlLocations) {
        	this.source = source;
        	this.xmlLocations = xmlLocations;
        }

        public PropertiesEventUnitImpl setUnitAdmin(final UnitAdmin unitAdmin) {
    		this.unitAdmin = unitAdmin;
    		return	this;
    	}

        @Override
    	public Object init(final EventBus eventBus) {
        	if ( LOG.isDebugEnabled() ) {
        		LOG.debug("current thread contextClassLoader {}", Thread.currentThread().getContextClassLoader());
        	}

    		final String springXml = searchSpringXml(getTemplateFromSourceName(source.getName()));

    		if ( null == springXml ) {
    			LOG.error("!CAN'T! found matched spring config xml, failed to create unit {}",
    					source.getName());
    			return	null;
    		}

    		if ( null != this.unitAdmin ) {
    			try {
    				final Properties props =  new Properties();
    				props.load( source.getInputStream() );
    				return this.unitAdmin.createUnit(
    						"default",
    						new File(source.getName()).getName(),
    						springXml,
    						new HashMap<String, String>() {
    							private static final long serialVersionUID = 1L;

    						{
    							for ( Map.Entry<Object,Object> entry : props.entrySet() ) {
    								this.put(entry.getKey().toString(), entry.getValue().toString());
    							}
    						}},
    						true);
    			} catch (Exception e) {
    				LOG.error("exception when createUnit for source {}, detail: {}",
    						source.getName(), ExceptionUtils.exception2detail(e));
    			}
    		}

    		return	null;
        }

    	/**
    	 * @return
    	 */
    	private String getTemplateFromSourceName(final String sourceName) {
    		String	template = FilenameUtils.getBaseName(sourceName);
    		int idx = template.lastIndexOf(".");
    		if ( -1 != idx ) {
    			template = template.substring(idx + 1);
    		}
    		return template;
    	}

        @Override
        public void destroy(Object ctx) {
        	if ( null != ctx ) {
        		((UnitAdmin.UnitMXBean)ctx).close();
        	}
        }

        @Override
        public String getDescription() {
            return source.getName();
        }

    	public void setUnitSource(UnitSource source) {
    		this.source = source;
    	}

    	private String searchSpringXml(final String template) {
        	for (String location : this.xmlLocations) {
                try {
    				if ( LOG.isTraceEnabled() ) {
    					LOG.trace("start search resource in {}", location );
    				}
    				String[] allRes = PackageUtils.getResourceInPackage(location);
    				for ( String res : allRes ) {
    					if ( LOG.isTraceEnabled() ) {
    						LOG.trace("found resource: {}", res);
    					}
    					if ( res.endsWith("." + template + ".xml") ) {
    						if ( LOG.isDebugEnabled() ) {
    							LOG.debug("found matched spring config: {}", res);
    						}

    						return res.substring(0, res.length() - 4).replace('.', '/') + ".xml";
    					}
    				}
        		} catch (IOException e) {
        			LOG.error("invalid xmlPath {}", location, e);
        		}
        	}

        	return	null;
    	}
}
