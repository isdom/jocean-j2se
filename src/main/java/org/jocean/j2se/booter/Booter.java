package org.jocean.j2se.booter;


public class Booter {

    /**
     * @param args
     */
    public static void main(String[] args) {
        final String[] extJars = JVMUtil.addAllJarsToClassPath(System.getProperty("user.dir") + "/lib");
        for (String jarName : extJars) {
            System.out.println("add path [" + jarName + "]");
        }

        try {
            ZKMain.main(args, extJars);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
