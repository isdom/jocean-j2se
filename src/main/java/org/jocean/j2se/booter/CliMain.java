/**
 *
 */
package org.jocean.j2se.booter;

import org.jocean.j2se.AppInfo;
import org.jocean.j2se.ModuleInfo;
import org.jocean.j2se.cli.CliController;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;


/**
 * @author isdom
 *
 */
public class CliMain {
    /**
     * @param args
     * @param extJars
     * @throws Exception
     */
    public static void main(final String[] args, final String[] libs) throws Exception {
        @SuppressWarnings({ "resource"})
        final AbstractApplicationContext appctx = new ClassPathXmlApplicationContext("unit/clibooter.xml");
        final AppInfo app = appctx.getBean("appinfo", AppInfo.class);
        if (null != app) {
            for (final String lib : libs) {
                app.getModules().put(lib, new ModuleInfo(lib));
            }
        }

        appctx.getBean(CliController.class).await();
    }
}
