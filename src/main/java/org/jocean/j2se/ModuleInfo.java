package org.jocean.j2se;

import java.util.jar.Attributes;
import java.util.jar.JarFile;

public class ModuleInfo {
    private String _moduleBuildNo;
    private String _specificationTitle;
    private String _specificationVersion;
    private String _specificationVendor;
    private String _implementationTitle;
    private String _implementationVersion;
    private String _implementationVendor;

    public ModuleInfo(final String location) {
        try (final JarFile jar = new JarFile(location)) {
            final Attributes attrs = jar.getManifest().getMainAttributes();
            
            this._moduleBuildNo = attrs.getValue(Attributes.Name.IMPLEMENTATION_VERSION);
            this._specificationTitle = attrs.getValue(Attributes.Name.SPECIFICATION_TITLE);
            this._specificationVersion = attrs.getValue(Attributes.Name.SPECIFICATION_VERSION);
            this._specificationVendor = attrs.getValue(Attributes.Name.SPECIFICATION_VENDOR);
            this._implementationTitle = attrs.getValue(Attributes.Name.IMPLEMENTATION_TITLE);
            this._implementationVersion = attrs.getValue(Attributes.Name.IMPLEMENTATION_VERSION);
            this._implementationVendor = attrs.getValue(Attributes.Name.IMPLEMENTATION_VENDOR);
        } catch (Exception e) {
            //  just ignore
        }
    }

    /**
     * @return the moduleBuildNo
     */
    public String getModuleBuildNo() {
        return null != this._moduleBuildNo ? this._moduleBuildNo : "UNKNOWN";
    }

    /**
     * @return the specificationTitle
     */
    public String getSpecificationTitle() {
        return _specificationTitle;
    }

    /**
     * @return the specificationVersion
     */
    public String getSpecificationVersion() {
        return _specificationVersion;
    }

    /**
     * @return the specificationVendor
     */
    public String getSpecificationVendor() {
        return _specificationVendor;
    }

    /**
     * @return the implementationTitle
     */
    public String getImplementationTitle() {
        return _implementationTitle;
    }

    /**
     * @return the implementationVersion
     */
    public String getImplementationVersion() {
        return _implementationVersion;
    }

    /**
     * @return the implementationVendor
     */
    public String getImplementationVendor() {
        return _implementationVendor;
    }
}
