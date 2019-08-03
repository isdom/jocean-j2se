package org.jocean.j2se.prometheus;

import org.springframework.beans.factory.annotation.Value;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.jmx.BuildInfoCollector;
import io.prometheus.jmx.JmxCollector;

public class JmxExporter {
    public void start() throws Exception {
        this._buildInfoCollector = new BuildInfoCollector().register(CollectorRegistry.defaultRegistry);

        // JVM MBean Collector
        this._jmxCollector = new JmxCollector(_config).register(CollectorRegistry.defaultRegistry);
    }

    public void stop() throws Exception {
        if (null != this._jmxCollector) {
            CollectorRegistry.defaultRegistry.unregister(this._jmxCollector);
            this._jmxCollector = null;
        }
        if (null != _buildInfoCollector) {
            CollectorRegistry.defaultRegistry.unregister(this._buildInfoCollector);
            this._buildInfoCollector = null;
        }
    }

    private JmxCollector _jmxCollector = null;

    private BuildInfoCollector _buildInfoCollector = null;

    @Value("${config}")
    String _config;
}
