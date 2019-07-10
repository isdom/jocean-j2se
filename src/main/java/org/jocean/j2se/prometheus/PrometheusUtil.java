package org.jocean.j2se.prometheus;

import java.io.File;

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

public class PrometheusUtil {
    public static int addPrometheusMeterRegistry(final CompositeMeterRegistry compositeMeterRegistry) {
        final PrometheusMeterRegistry prometheusRegistry = new PrometheusMeterRegistry(
                PrometheusConfig.DEFAULT, CollectorRegistry.defaultRegistry, Clock.SYSTEM);

        compositeMeterRegistry.add(prometheusRegistry);

        prometheusRegistry.config().commonTags(
                "application", System.getProperty("app.name"),
                "hostname", System.getenv("HOSTNAME"));

        new ClassLoaderMetrics().bindTo(compositeMeterRegistry);
        new JvmMemoryMetrics().bindTo(compositeMeterRegistry);
        new JvmGcMetrics().bindTo(compositeMeterRegistry);
        new JvmThreadMetrics().bindTo(compositeMeterRegistry);
        new DiskSpaceMetrics(new File(System.getProperty("user.home"))).bindTo(compositeMeterRegistry);

        new UptimeMetrics().bindTo(compositeMeterRegistry);
        new FileDescriptorMetrics().bindTo(compositeMeterRegistry);
        new ProcessorMetrics().bindTo(compositeMeterRegistry);

        // logback metrics
        new LogbackMetrics().bindTo(compositeMeterRegistry);

        new ProcessMemoryMetrics().bindTo(compositeMeterRegistry);
        new ProcessThreadMetrics().bindTo(compositeMeterRegistry);

        return 0;
    }
}
