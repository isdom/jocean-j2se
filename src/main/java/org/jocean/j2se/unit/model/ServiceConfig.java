package org.jocean.j2se.unit.model;

import java.util.Arrays;

public class ServiceConfig {


    @Override
    public String toString() {
        final int maxLen = 10;
        final StringBuilder builder = new StringBuilder();
        builder.append("ServiceConfig [host=").append(_host).append(", conf=")
                .append(_conf != null ? Arrays.asList(_conf).subList(0, Math.min(_conf.length, maxLen)) : null)
                .append("]");
        return builder.toString();
    }

    public void setHost(final String host) {
        this._host = host;
    }

    public String getHost() {
        return this._host;
    }

    public void setConf(final UnitDescription[] conf) {
        this._conf = conf;
    }

    public UnitDescription[] getConf() {
        return this._conf;
    }

    private String _host;
    private UnitDescription[] _conf;
}
