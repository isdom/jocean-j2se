/**
 *
 */
package org.jocean.j2se.os;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author isdom
 *
 */
public class OSUtil {

    private static final Logger LOG = LoggerFactory.getLogger(OSUtil.class);

    public static int getCurrentPid() {
        final RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
        final String name = runtime.getName(); // format: "pid@hostname"
        try {
            return Integer.parseInt(name.substring(0, name.indexOf('@')));
        } catch (final Exception e) {
            return -1;
        }
    }

    public static String getLocalHostname() {
        final RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
        final String name = runtime.getName(); // format: "pid@hostname"
        try {
            return name.substring(name.indexOf('@') + 1, name.length() );
        } catch (final Exception e) {
            return "";
        }
    }

    public static boolean isHostRunningWindows() {
		final String osname = System.getProperties().getProperty("os.name");
		return ( osname.startsWith("win") || osname.startsWith("Win") );
    }

    public static Map<String, String>	readUnixProcStatus(final int pid) {
    	final File statusfile = new File("/proc/" + Integer.toString(pid) + "/status"  );

    	if ( !statusfile.exists() ) {
    		return Collections.emptyMap();
    	}

    	final Map<String, String> props = new HashMap<String, String>();
    	BufferedReader in = null;

    	try {
    		in = new BufferedReader(new FileReader(statusfile));
    		String line = null;
    		do {
    			line = in.readLine();
    			if ( null != line ) {
    				final String[] pieces = line.split(":");
    				if ( pieces.length >= 2 ) {
    					props.put(pieces[0].trim(), pieces[1].trim());
    				}
    			}
    		} while( null != line );
    	} catch (final IOException e) {
    		LOG.error("failed to readLine for " + statusfile.getAbsolutePath(), e);
		}
    	finally {
    		if ( null != in ) {
    			try {
					in.close();
				} catch (final IOException e) {
					//	just ignore close exception
				}
    		}
    	}

    	return	props;
    }
}
