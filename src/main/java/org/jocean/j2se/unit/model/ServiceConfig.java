package org.jocean.j2se.unit.model;

import java.util.Arrays;

public class ServiceConfig {


    @Override
    public String toString() {
        final int maxLen = 10;
        final StringBuilder builder = new StringBuilder();
        builder.append("ServiceConfig [host=").append(_host).append(", confs=")
                .append(_confs != null ? Arrays.asList(_confs).subList(0, Math.min(_confs.length, maxLen)) : null)
                .append("]");
        return builder.toString();
    }

    public void setHost(final String host) {
        this._host = host;
    }

    public String getHost() {
        return this._host;
    }

    public void setConfs(final UnitDescription[] confs) {
        this._confs = confs;
    }

    public UnitDescription[] getConfs() {
        return this._confs;
    }

    private String _host;
    private UnitDescription[] _confs;
}
