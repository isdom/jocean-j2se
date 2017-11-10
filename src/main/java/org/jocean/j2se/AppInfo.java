package org.jocean.j2se;

import java.io.IOException;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;

public class AppInfo {
    final private String _buildNo;

    public AppInfo() {
        this._buildNo = getVersion();
    }

    private String getVersion() {
        try {
            return Resources.toString(getClass().getResource("/version.txt"), Charsets.UTF_8);
        } catch (IOException e) {
            return "UNKNOWN";
        }
    }

    public String getBuildNo() {
        return this._buildNo;
    }
}
