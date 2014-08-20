package org.jocean.ext.ebus.unit;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

public class FilesystemUnitSource implements UnitSource {
    private static final Logger logger =
            LoggerFactory.getLogger(FilesystemUnitSource.class);
    private File file;
    private String name;
    private String type;

    public FilesystemUnitSource(File file) {
        this.file = file;
        try {
            name = file.getCanonicalPath();
        } catch (IOException e) {
            logger.error("FilesystemUnitSource.init file.getCanonicalPath", e);
        }
        if (null != name) {
            type = FilenameUtils.getExtension(name);
        }
    }

    public InputStream getInputStream() {
        try {
            return new FileInputStream(file);
        } catch (FileNotFoundException e) {
            logger.error("getInputStream", e);
        }
        return null;
    }

    public String getName() {
        return name;
    }

    public long lastModified() {
        return file.lastModified();
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this,
                ToStringStyle.MULTI_LINE_STYLE);
    }

    public String getType() {
        return type;
    }

    /**
     * @param type the type to set
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }
}
