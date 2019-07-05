package org.jocean.j2se.prometheus;

import java.net.InetSocketAddress;

import org.springframework.beans.factory.annotation.Value;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.HTTPServer;
import io.prometheus.jmx.BuildInfoCollector;
import io.prometheus.jmx.JmxCollector;

public class JmxExporter {
    public void start() throws Exception {
        if (null == _buildInfoCollector) {
            _buildInfoCollector = new BuildInfoCollector().register();
        }

        if (null == _jmxCollector) {
            _jmxCollector = new JmxCollector(_config).register();
        }

        _httpserver = new HTTPServer(new InetSocketAddress(_port), CollectorRegistry.defaultRegistry);
    }

    public void stop() {
        if (null != _httpserver) {
            _httpserver.stop();
        }
    }

    private HTTPServer _httpserver;
    static private JmxCollector _jmxCollector = null;
    static private BuildInfoCollector _buildInfoCollector = null;

    @Value("${config}")
    String _config;

    @Value("${port}")
    int _port = 0;
}
