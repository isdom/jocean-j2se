package org.jocean.j2se.prometheus;

import java.io.File;
import java.net.InetSocketAddress;

import javax.inject.Inject;
import javax.management.ObjectName;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jmx.export.MBeanExporter;

import io.github.mweirauch.micrometer.jvm.extras.ProcessMemoryMetrics;
import io.github.mweirauch.micrometer.jvm.extras.ProcessThreadMetrics;
import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.DiskSpaceMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.logging.LogbackMetrics;
import io.micrometer.core.instrument.binder.system.FileDescriptorMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.core.instrument.binder.system.UptimeMetrics;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
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

            _compositeMeterRegistry.add(_prometheusRegistry);

//            _prometheusRegistry.config().commonTags("application", System.getProperty("app.name"));

            new ClassLoaderMetrics().bindTo(_prometheusRegistry);
            new JvmMemoryMetrics().bindTo(_prometheusRegistry);
            new JvmGcMetrics().bindTo(_prometheusRegistry);
            new JvmThreadMetrics().bindTo(_prometheusRegistry);
            new DiskSpaceMetrics(new File(System.getProperty("user.home"))).bindTo(_prometheusRegistry);
//            new ExecutorServiceMetrics().bindTo(_prometheusRegistry);

            new UptimeMetrics().bindTo(_prometheusRegistry);
            new FileDescriptorMetrics().bindTo(_prometheusRegistry);
            new ProcessorMetrics().bindTo(_prometheusRegistry);

            // logback metrics
            new LogbackMetrics().bindTo(_prometheusRegistry);

            new ProcessMemoryMetrics().bindTo(_prometheusRegistry);
            new ProcessThreadMetrics().bindTo(_prometheusRegistry);
        }

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
    }

    private HTTPServer _httpserver;
    static private JmxCollector _jmxCollector = null;
    static private BuildInfoCollector _buildInfoCollector = null;
    static private PrometheusMeterRegistry _prometheusRegistry = null;

    @Inject
    MBeanExporter _mbeanExporter;

    @Inject
    CompositeMeterRegistry _compositeMeterRegistry;

    @Value("${config}")
    String _config;

    @Value("${port}")
    int _port = 0;
}
