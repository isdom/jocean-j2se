package org.jocean.ext.unit;

import org.jocean.ext.util.JVMUtil;

public class JOceanBooter {

    public static void main(String[] args) {
        String[] extJars = JVMUtil.addAllJarsToClassPath(System.getProperty("user.dir") + "/lib");
        for (String jarName : extJars) {
            System.out.println("add path [" + jarName + "]");
        }

        JOceanServer.main(args);
    }
}
