/**
 *
 */
package org.jocean.j2se.booter;

import org.jocean.j2se.AppInfo;
import org.jocean.j2se.ModuleInfo;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;


/**
 * @author isdom
 *
 */
public class ZKMain {
    /**
     * @param args
     * @param extJars
     * @throws Exception
     */
    public static void main(final String[] args, final String[] libs) throws Exception {
        @SuppressWarnings({ "resource"})
        final AbstractApplicationContext ctx = new ClassPathXmlApplicationContext("unit/zkbooter.xml");
        final AppInfo app = ctx.getBean("appinfo", AppInfo.class);
        if (null != app) {
            for (final String lib : libs) {
                app.getModules().put(lib, new ModuleInfo(lib));
            }
        }
    }
}
