/**
 * 
 */
package org.jocean.j2se.booter;

import java.util.Map;

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
        final AbstractApplicationContext ctx = 
                new ClassPathXmlApplicationContext("unit/zkbooter.xml");
        @SuppressWarnings("unchecked")
        final Map<String, ModuleInfo> modules = (Map<String, ModuleInfo>)ctx.getBean("libs", Map.class);
        if (null != modules) {
            for (String lib : libs) {
                modules.put(lib, new ModuleInfo(lib));
            }
        }
    }
}
