package org.jocean.j2se;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;

public class AppInfo {
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
    
    public Map<String, ModuleInfo> getModules() {
        return this._modules;
    }
    
    final private String _buildNo;
    final private Map<String, ModuleInfo> _modules = new HashMap<>();
}
