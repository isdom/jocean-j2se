package org.jocean.j2se.booter;


public class CliBooter {

    /**
     * @param args
     */
    public static void main(final String[] args) {
        final String[] extJars = JVMUtil.addAllJarsToClassPath(System.getProperty("user.dir") + "/lib");
        for (final String jarName : extJars) {
            System.out.println("add path [" + jarName + "]");
        }

        try {
            CliMain.main(args, extJars);
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

}
