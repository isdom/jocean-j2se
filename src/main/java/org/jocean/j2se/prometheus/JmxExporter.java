package org.jocean.j2se.prometheus;

import java.net.InetSocketAddress;

import javax.inject.Inject;
import javax.management.ObjectName;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jmx.export.MBeanExporter;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.HTTPServer;
import io.prometheus.jmx.BuildInfoCollector;
import io.prometheus.jmx.JmxCollector;

public class JmxExporter {
    public void start() throws Exception {
        if (null == _buildInfoCollector) {
            _buildInfoCollector = new BuildInfoCollector().register();
        }

        // JVM MBean Collector
        this._jmxCollector = new JmxCollector(_config).register(CollectorRegistry.defaultRegistry);

        _httpserver = new HTTPServer(new InetSocketAddress(_port), CollectorRegistry.defaultRegistry);

        _mbeanExporter.registerManagedResource(this, ObjectName.getInstance("prometheus:type=exporter"));
        _mbeanExporter.registerManagedResource(_httpserver, ObjectName.getInstance("prometheus:type=httpendpoint"));
    }

    public void stop() throws Exception {
        if (null != _httpserver) {
            _httpserver.stop();
            _mbeanExporter.unregisterManagedResource(ObjectName.getInstance("prometheus:type=httpendpoint"));
        }
        _mbeanExporter.unregisterManagedResource(ObjectName.getInstance("prometheus:type=exporter"));

        if (null != this._jmxCollector) {
            CollectorRegistry.defaultRegistry.unregister(this._jmxCollector);
            this._jmxCollector = null;
        }
    }

    private HTTPServer _httpserver;
    private JmxCollector _jmxCollector = null;

    static private BuildInfoCollector _buildInfoCollector = null;

    @Inject
    MBeanExporter _mbeanExporter;

//    @Inject
//    CompositeMeterRegistry _compositeMeterRegistry;

    @Value("${config}")
    String _config;

    @Value("${port}")
    int _port = 0;
}
