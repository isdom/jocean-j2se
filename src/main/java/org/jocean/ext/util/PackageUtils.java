package org.jocean.ext.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;

public class PackageUtils {

    private static final Logger logger = LoggerFactory
            .getLogger(PackageUtils.class);

    private static final List<String> EMPTY_LIST = new ArrayList<>();

    public static Map<URL, String[]> getAllCPResourceAsPathlike() throws IOException {
        final Map<URL, String[]> vResult = new HashMap<>();
        final ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (logger.isDebugEnabled()) {
            logger.debug("using classloader: " + cl);
        }
        {
            //	for file classpath
            final Enumeration<URL> dirs = cl.getResources("");
            if (logger.isDebugEnabled()) {
                logger.debug("PackageUtils: get file resources: " + dirs
                        + ", hasMoreElements:" + dirs.hasMoreElements());
            }
            while (dirs.hasMoreElements()) {
                final URL url = dirs.nextElement();
                final String protocol = url.getProtocol();
                if (logger.isDebugEnabled()) {
                    logger.debug("PackageUtils: url: " + url);
                }

                if ("file".equals(protocol)) {
                    final List<String> resources = new ArrayList<>();
                    getAllFileCPResourceAsPathlike(
                            "",
                            URLDecoder.decode(url.getFile(), "UTF-8"),
                            resources);
                    vResult.put(url, resources.toArray(new String[resources.size()]));
                }
            }
        }

        {
            //	for jar classpath
            final Enumeration<URL> jars = cl.getResources("META-INF");
            while (jars.hasMoreElements()) {
                final URL url = jars.nextElement();
                final String protocol = url.getProtocol();
                if (logger.isDebugEnabled()) {
                    logger.debug("PackageUtils: url: " + url);
                }

                if ("jar".equals(protocol)) {
                    final List<String> resources = new ArrayList<>();
                    final JarFile jar = ((JarURLConnection) url.openConnection())
                            .getJarFile();
                    final Enumeration<JarEntry> entries = jar.entries();
                    while (entries.hasMoreElements()) {
                        final JarEntry entry = entries.nextElement();
                        String name = entry.getName();
                        if (name.charAt(0) == '/') {
                            name = name.substring(1);
                        }
                        if (!entry.isDirectory()) {
                            resources.add(name);
                        }
                    }
                    vResult.put(url, resources.toArray(new String[resources.size()]));
                }
            }
        }
        return vResult;
    }

    private static void getAllFileCPResourceAsPathlike(
            final String basePath,
            final String filePath,
            final List<String> resources) {
        final File dir = new File(filePath);

        if (!dir.exists() || !dir.isDirectory()) {
            return;
        }

        final File[] dirfiles = dir.listFiles();

        for (File file : dirfiles) {
            if (file.isDirectory()) {
                getAllFileCPResourceAsPathlike(
                        basePath + file.getName() + "/",
                        file.getAbsolutePath(),
                        resources);
            } else {
                resources.add(basePath + file.getName());
            }
        }
    }

    public static String[] getClasspathResourceAsPathlike(final String path) throws IOException {
        String packageOnly = path;
        boolean recursive = false;
        if (path.endsWith(".*")) {
            packageOnly = path.substring(0, path.lastIndexOf(".*"));
            recursive = true;
        }

        if (packageOnly.endsWith("/")) {
            packageOnly = packageOnly.substring(0, path.length() - 1);
        }

        final List<String> vResult = new ArrayList<>();
        final String packageDirName = packageOnly.replace('.', '/');
        final ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (logger.isDebugEnabled()) {
            logger.debug("using classloader: " + cl);
        }
        final Enumeration<URL> dirs = cl.getResources(packageDirName);
        if (logger.isDebugEnabled()) {
            logger.debug("PackageUtils: getResources: " + dirs
                    + ", hasMoreElements:" + dirs.hasMoreElements());
        }
        while (dirs.hasMoreElements()) {
            final URL url = dirs.nextElement();
            final String protocol = url.getProtocol();
            if (logger.isDebugEnabled()) {
                logger.debug("PackageUtils: url: " + url);
            }

            if ("file".equals(protocol)) {
                getDirCPResourceAsPathlike(
                        packageOnly,
                        URLDecoder.decode(url.getFile(), "UTF-8"),
                        recursive,
                        vResult);
            } else if ("jar".equals(protocol)) {
                final JarFile jar = ((JarURLConnection) url.openConnection())
                        .getJarFile();
                final Enumeration<JarEntry> entries = jar.entries();
                while (entries.hasMoreElements()) {
                    final JarEntry entry = entries.nextElement();
                    String name = entry.getName();
                    if (name.charAt(0) == '/') {
                        name = name.substring(1);
                    }
                    if (name.startsWith(packageDirName)
                            && !entry.isDirectory()) {
                        vResult.add(name);
                    }
                }
            } else {
                final String urlpath = url.getPath();
                if (urlpath.startsWith(packageDirName)) {
                    vResult.add(urlpath);
                }
            }
        }

        return vResult.toArray(new String[vResult.size()]);
    }

    private static void getDirCPResourceAsPathlike(
            final String packageName,
            final String packagePath,
            final boolean recursive,
            final List<String> classes) {
        final File dir = new File(packagePath);

        if (!dir.exists() || !dir.isDirectory()) {
            return;
        }

        final File[] dirfiles = dir.listFiles();

        for (File file : dirfiles) {
            if (file.isDirectory()) {
                getDirCPResourceAsPathlike(
                        packageName + "/" + file.getName(),
                        file.getAbsolutePath(), recursive,
                        classes);
            } else {
                classes.add(packageName + "/" + file.getName());
            }
        }
    }

    //	此处实现 对于 非recursive 情况没有处理，在"xxx.yyy"情况下均会获取包括子包下的所有resource
    //	与参数为"xxx.yyy.*"无区别
    //	TODO fix bug
    public static String[] getResourceInPackage(String packageName) throws IOException {
        String packageOnly = packageName;
        boolean recursive = false;
        if (packageName.endsWith(".*")) {
            packageOnly = packageName.substring(0, packageName
                    .lastIndexOf(".*"));
            recursive = true;
        }

        if (packageOnly.endsWith("/")) {
            packageOnly = packageOnly.substring(0, packageName.length() - 1);
        }

        List<String> vResult = new ArrayList<>();
        String packageDirName = packageOnly.replace('.', '/');
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (logger.isDebugEnabled()) {
            logger.debug("using classloader: " + cl);
        }
        Enumeration<URL> dirs = cl.getResources(packageDirName);
        if (logger.isDebugEnabled()) {
            logger.debug("PackageUtils: getResources: " + dirs
                    + ", hasMoreElements:" + dirs.hasMoreElements());
        }
        while (dirs.hasMoreElements()) {
            URL url = dirs.nextElement();
            String protocol = url.getProtocol();
            if (logger.isDebugEnabled()) {
                logger.debug("PackageUtils: url: " + url);
            }

            if ("file".equals(protocol)) {
                getResourceInDirPackage(packageOnly,
                        URLDecoder.decode(url.getFile(), "UTF-8"), recursive,
                        vResult);
            } else if ("jar".equals(protocol)) {
                JarFile jar = ((JarURLConnection) url.openConnection())
                        .getJarFile();
                Enumeration<JarEntry> entries = jar.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    String name = entry.getName();
                    if (name.charAt(0) == '/') {
                        name = name.substring(1);
                    }
                    if (name.startsWith(packageDirName)) {
                        int idx = name.lastIndexOf('/');
                        if (idx != -1) {
                            packageName = name.substring(0, idx).replace('/',
                                    '.');
                        }

                        if (logger.isDebugEnabled()) {
                            logger.debug("PackageUtils: Package name is " + packageName);
                        }

                        if ((idx != -1) || recursive) {
                            // it's not inside a deeper dir
                            if (!entry.isDirectory()) {
                                String resName = name.substring(packageName
                                        .length() + 1);
                                vResult.add(packageName + "." + resName);
                            }
                        }
                    }
                }
            } else {
                String path = url.getPath();
                if (path.startsWith(packageDirName)) {
                    vResult.add(path.replace('/', '.'));
                }
            }
        }

        String[] result = vResult.toArray(new String[vResult.size()]);
        return result;
    }

    private static void getResourceInDirPackage(String packageName, String packagePath,
                                                final boolean recursive, List<String> classes) {
        File dir = new File(packagePath);

        if (!dir.exists() || !dir.isDirectory()) {
            return;
        }

        File[] dirfiles = dir.listFiles();

        for (File file : dirfiles) {
            if (file.isDirectory()) {
                getResourceInDirPackage(packageName + "." + file.getName(),
                        file.getAbsolutePath(), recursive,
                        classes);
            } else {
                classes.add(packageName + "." + file.getName());
            }
        }
    }

    public static String[] findClassesInPackage(String packageName) throws IOException {
        return findClassesInPackage(packageName, Collections.<String>emptyList(), Collections.<String>emptyList(), true);
    }

    /**
     * @param packageName
     * @return The list of all the classes inside this package
     * @throws java.io.IOException
     */
    public static String[] findClassesInPackage(String packageName,
                                                List<String> included, List<String> excluded, boolean recursive) throws IOException {
        String packageOnly = packageName;
        List<String> vResult = new ArrayList<>();
        String packageDirName = packageOnly.replace('.', '/');
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (logger.isDebugEnabled()) {
            logger.debug("using classloader: " + cl);
        }
        Enumeration<URL> dirs = cl.getResources(packageDirName);
        if (logger.isDebugEnabled()) {
            logger.debug("PackageUtils: getResources: " + dirs
                    + ", hasMoreElements:" + dirs.hasMoreElements());
        }
        while (dirs.hasMoreElements()) {
            URL url = dirs.nextElement();
            String protocol = url.getProtocol();
            // if(!matchTestClasspath(url, packageDirName, recursive)) {
            // continue;
            // }
            if (logger.isDebugEnabled()) {
                logger.debug("PackageUtils: url: " + url);
            }

            if ("file".equals(protocol)) {
                findClassesInDirPackage(packageOnly, included, excluded,
                        URLDecoder.decode(url.getFile(), "UTF-8"), recursive,
                        vResult);
            } else if ("jar".equals(protocol)) {
                JarFile jar = ((JarURLConnection) url.openConnection())
                        .getJarFile();
                Enumeration<JarEntry> entries = jar.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    String name = entry.getName();
                    if (name.charAt(0) == '/') {
                        name = name.substring(1);
                    }
                    if (name.startsWith(packageDirName)) {
                        int idx = name.lastIndexOf('/');
                        if (idx != -1) {
                            packageName = name.substring(0, idx).replace('/',
                                    '.');
                        }

                        if (logger.isDebugEnabled()) {
                            logger.debug("PackageUtils: Package name is " + packageName);
                        }

                        // Utils.log("PackageUtils", 4, "Package name is " +
                        // packageName);
                        if ((idx != -1) || recursive) {
                            // it's not inside a deeper dir
                            if (name.endsWith(".class") && !entry.isDirectory()) {
                                String className = name.substring(packageName
                                        .length() + 1, name.length() - 6);

                                if (logger.isDebugEnabled()) {
                                    logger.debug("PackageUtils: Found class "
                                            + className
                                            + ", seeing it if it's included or excluded");
                                }
                                includeOrExcludeClass(packageName, className,
                                        included, excluded, vResult);
                            }
                        }
                    }
                }
            } else {
                String path = url.getPath();
                if (path.startsWith(packageDirName) && path.endsWith(".class")) {
                    String pkg = "";
                    int idx = path.lastIndexOf('/');
                    if (idx != -1) {
                        pkg = path.substring(0, idx).replace('/', '.');
                    }
                    String className = path.substring(pkg.length() + 1, path.length() - 6);
                    includeOrExcludeClass(pkg, className, included, excluded, vResult);
                }
            }
        }

        String[] result = vResult.toArray(new String[vResult.size()]);
        return result;
    }

    public static List<Class<?>> findClassesInPackage(String packageName, Class<? extends Annotation> annotationClass) {
        List<Class<?>> found = new ArrayList<>();
        try {
            for (String clsName : findClassesInPackage(packageName)) {
                try {
                    ClassLoader cl = Thread.currentThread().getContextClassLoader();
                    if (logger.isDebugEnabled()) {
                        logger.debug("using ClassLoader {} to load Class {}", cl, clsName);
                    }
                    Class<?> cls = cl.loadClass(clsName);
                    if (cls.isAnnotationPresent(annotationClass)) {
                        found.add(cls);
                    }
                } catch (ClassNotFoundException e) {
                    logger.error("", e);
                }
            }
        } catch (IOException e) {
            logger.error("", e);
        }
        return found;
    }

    private static void findClassesInDirPackage(String packageName,
                                                List<String> included, List<String> excluded, String packagePath,
                                                final boolean recursive, List<String> classes) {
        File dir = new File(packagePath);

        if (!dir.exists() || !dir.isDirectory()) {
            return;
        }

        File[] dirfiles = dir.listFiles(new FileFilter() {
            public boolean accept(File file) {
                return (recursive && file.isDirectory())
                        || (file.getName().endsWith(".class"));
            }
        });

        if (logger.isDebugEnabled()) {
            logger.debug("PackageUtils: Looking for test classes in the directory: "
                    + dir);
        }
        for (File file : dirfiles) {
            if (file.isDirectory()) {
                findClassesInDirPackage(packageName + "." + file.getName(),
                        included, excluded, file.getAbsolutePath(), recursive,
                        classes);
            } else {
                String className = file.getName().substring(0,
                        file.getName().length() - 6);

                if (logger.isDebugEnabled()) {
                    logger.debug("PackageUtils: Found class " + className
                            + ", seeing it if it's included or excluded");
                }
                includeOrExcludeClass(packageName, className, included,
                        excluded, classes);
            }
        }
    }

    private static void includeOrExcludeClass(String packageName,
                                              String className, List<String> included, List<String> excluded,
                                              List<String> classes) {
        if (isIncluded(className, included, excluded)) {
            if (logger.isDebugEnabled()) {
                logger.debug("PackageUtils: ... Including class " + className);
            }
            classes.add(packageName + '.' + className);
        } else {
            if (logger.isDebugEnabled()) {
                logger.debug("PackageUtils: ... Excluding class " + className);
            }
        }
    }

    /**
     * @return true if name should be included.
     */
    private static boolean isIncluded(String name, List<String> included,
                                      List<String> excluded) {
        boolean result = false;

        //
        // If no includes nor excludes were specified, return true.
        //
        if (null == included) {
            included = EMPTY_LIST;
        }

        if (null == excluded) {
            excluded = EMPTY_LIST;
        }

        if (included.size() == 0 && excluded.size() == 0) {
            result = true;
        } else {
            boolean isIncluded = PackageUtils.find(name, included);
            boolean isExcluded = PackageUtils.find(name, excluded);
            if (isIncluded && !isExcluded) {
                result = true;
            } else if (isExcluded) {
                result = false;
            } else {
                result = included.size() == 0;
            }
        }
        return result;
    }

    private static boolean find(String name, List<String> list) {
        for (String regexpStr : list) {
            if (Pattern.matches(regexpStr, name))
                return true;
        }
        return false;
    }
}
