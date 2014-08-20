package org.jocean.ext.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

public class AppInfo {
    private static final Logger logger = LoggerFactory.getLogger(AppInfo.class);

    private String appVersion;
    private String specificationTitle;
    private String specificationVersion;
    private String specificationVendor;
    private String implementationTitle;
    private String implementationVersion;
    private String implementationVendor;

    public void setJarLocation(String location) {
        JarFile jar;
        try {
            jar = new JarFile(location);
            Manifest man = jar.getManifest();
            Attributes attrs = man.getMainAttributes();
            appVersion = attrs.getValue(Attributes.Name.IMPLEMENTATION_VERSION);
            this.specificationTitle = attrs.getValue(Attributes.Name.SPECIFICATION_TITLE);
            this.specificationVersion = attrs.getValue(Attributes.Name.SPECIFICATION_VERSION);
            this.specificationVendor = attrs.getValue(Attributes.Name.SPECIFICATION_VENDOR);
            this.implementationTitle = attrs.getValue(Attributes.Name.IMPLEMENTATION_TITLE);
            this.implementationVersion = attrs.getValue(Attributes.Name.IMPLEMENTATION_VERSION);
            this.implementationVendor = attrs.getValue(Attributes.Name.IMPLEMENTATION_VENDOR);
        } catch (IOException e) {
            logger.error("setJarLocation:", e);
        }

    }

    /**
     * @return the appVersion
     */
    public String getAppVersion() {
        return appVersion;
    }

    /**
     * @return the logger
     */
    public static Logger getLogger() {
        return logger;
    }

    /**
     * @return the specificationTitle
     */
    public String getSpecificationTitle() {
        return specificationTitle;
    }

    /**
     * @return the specificationVersion
     */
    public String getSpecificationVersion() {
        return specificationVersion;
    }

    /**
     * @return the specificationVendor
     */
    public String getSpecificationVendor() {
        return specificationVendor;
    }

    /**
     * @return the implementationTitle
     */
    public String getImplementationTitle() {
        return implementationTitle;
    }

    /**
     * @return the implementationVersion
     */
    public String getImplementationVersion() {
        return implementationVersion;
    }

    /**
     * @return the implementationVendor
     */
    public String getImplementationVendor() {
        return implementationVendor;
    }
}
