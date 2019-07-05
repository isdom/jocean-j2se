package org.jocean.j2se.prometheus;

import java.net.InetSocketAddress;

import org.springframework.beans.factory.annotation.Value;

import io.github.mweirauch.micrometer.jvm.extras.ProcessMemoryMetrics;
import io.github.mweirauch.micrometer.jvm.extras.ProcessThreadMetrics;
import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
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
        if (null == _jmxCollector) {
            _jmxCollector = new JmxCollector(_config).register();
        }

        // MicroMeter Collector
        if (null == _prometheusRegistry) {
            _prometheusRegistry = new PrometheusMeterRegistry(
                    PrometheusConfig.DEFAULT, CollectorRegistry.defaultRegistry, Clock.SYSTEM);
            new ClassLoaderMetrics().bindTo(_prometheusRegistry);
            new JvmMemoryMetrics().bindTo(_prometheusRegistry);
            new JvmGcMetrics().bindTo(_prometheusRegistry);
            new ProcessorMetrics().bindTo(_prometheusRegistry);
            new JvmThreadMetrics().bindTo(_prometheusRegistry);

            new ProcessMemoryMetrics().bindTo(_prometheusRegistry);
            new ProcessThreadMetrics().bindTo(_prometheusRegistry);
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
    static private PrometheusMeterRegistry _prometheusRegistry = null;

    @Value("${config}")
    String _config;

    @Value("${port}")
    int _port = 0;
}
