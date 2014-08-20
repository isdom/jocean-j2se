package org.jocean.ext.unit;

import org.jocean.idiom.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericXmlApplicationContext;
import org.springframework.core.io.Resource;

import java.util.Arrays;

public class AppContextInitor {

    private static final Logger LOG
            = LoggerFactory.getLogger(AppContextInitor.class);

    public AppContextInitor(final ApplicationContext root) {
        this._root = root;
        this._appctx = root;
    }

    public void setLocations(final Resource[] locations) {
        this._locations = locations;
    }

    public void init() {
        if (null != _locations && (_locations.length > 0)) {
            final GenericXmlApplicationContext ctx =
                    new GenericXmlApplicationContext();
            try {
                ctx.setParent(this._root);
                ctx.load(this._locations);
                ctx.refresh();
                this._appctx = ctx;
                if (LOG.isDebugEnabled()) {
                    LOG.debug("create Application Context {} succeed.", Arrays.toString(this._locations));
                }
            } catch (Exception e) {
                LOG.error("exception when create application context {}, detail: {}",
                        Arrays.toString(this._locations),
                        ExceptionUtils.exception2detail(e));
            }
        } else {
            if (LOG.isDebugEnabled()) {
                LOG.debug("_location is null, no application context create.");
            }
        }
    }

    public ApplicationContext getApplicationContext() {
        return this._appctx;
    }

    private final ApplicationContext _root;
    private Resource[] _locations = null;
    private ApplicationContext _appctx = null;
}
