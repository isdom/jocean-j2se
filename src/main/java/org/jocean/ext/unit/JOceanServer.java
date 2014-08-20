package org.jocean.ext.unit;

import org.jocean.ext.ebus.unit.UnitGroupBooter;
import org.jocean.ext.util.AppInfo;
import org.jocean.ext.util.SimpleRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.jmx.export.MBeanExporter;

import javax.management.ObjectName;

public class JOceanServer {
    private static final Logger LOG = LoggerFactory.getLogger(JOceanServer.class);

    private AbstractApplicationContext rootCtx;

    public JOceanServer() {
        try {
            final AbstractApplicationContext root = new ClassPathXmlApplicationContext("unitrepo/jocean/ext/main/joceanRoot.xml");

            final UnitGroupBooter booter = root.getBean(UnitGroupBooter.class);

            if (null != booter) {
                booter.setRootAppCtx(root);
            }

            final SimpleRegistry rootRegistry = root.getBean(SimpleRegistry.class);

            if (null != rootRegistry) {
                rootRegistry.getOrCreateEntry("__root_spring_ctx", root);
            }

            final AbstractApplicationContext appCtxLauncher =
                    new ClassPathXmlApplicationContext(
                            new String[]{"unitrepo/jocean/ext/main/appCtxLauncher.xml"}, root);

            final AppContextInitor initor = appCtxLauncher.getBean(AppContextInitor.class);
            if (null != initor) {
                final ApplicationContext appCtx = initor.getApplicationContext();
                if (null != booter) {
                    booter.setRootAppCtx(appCtx);
                }
                if (null != rootRegistry) {
                    rootRegistry.createEntryRemover("__root_spring_ctx").run();
                    rootRegistry.getOrCreateEntry("__root_spring_ctx", appCtx);
                }
            }

            final AbstractApplicationContext stcServerCtx =
                    new ClassPathXmlApplicationContext(
                            new String[]{"unitrepo/jocean/ext/main/joceanServer.xml"}, root);

            final AppInfo info = stcServerCtx.getBean(AppInfo.class);

            final MBeanExporter exporter = (MBeanExporter) stcServerCtx.getBean("booterExporter");

            exporter.registerManagedResource(stcServerCtx, new ObjectName("org.jocean:name=rootCtx"));
            exporter.registerManagedResource(this, new ObjectName("org.jocean:name=jOceanServer"));

            LOG.info(info.getSpecificationTitle() + " Server [" + info.getAppVersion() + "] Started... ");

            this.rootCtx = stcServerCtx;
        } catch (Exception e) {
            LOG.error("Server Start Error: ", e);
            System.exit(-1);
        }
    }

    public void exit() {
        if (null != this.rootCtx) {
            this.rootCtx.close();
        }
        System.exit(0);
    }

    public static void main(String[] args) {
        new JOceanServer();
    }
}
