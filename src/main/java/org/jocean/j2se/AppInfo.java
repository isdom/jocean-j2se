package org.jocean.j2se;

import java.util.HashMap;
import java.util.Map;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;

public class AppInfo {
    public AppInfo() {
        this._version = fetchVersionFromResource();
    }

    private String fetchVersionFromResource() {
        try {
            return Resources.toString(getClass().getResource("/version.txt"), Charsets.UTF_8);
        } catch (final Exception e) {
            return "UNKNOWN";
        }
    }

    public String getVersion() {
        return this._version;
    }

    public int getBuild() {
        final String snapshotStr = "SNAPSHOT-";

        final int snapshotIdx = this._version.indexOf(snapshotStr);

        if (snapshotIdx > -1) {
            final String versionWithDate = this._version.substring(snapshotIdx + snapshotStr.length());
            final int dateIdx = versionWithDate.indexOf('-');
            if (dateIdx > -1) {
                return Integer.parseInt(versionWithDate.substring(0, dateIdx));
            }
        }
        return -1;
    }

    public Map<String, ModuleInfo> getModules() {
        return this._modules;
    }

    final private String _version;
    final private Map<String, ModuleInfo> _modules = new HashMap<>();
}
