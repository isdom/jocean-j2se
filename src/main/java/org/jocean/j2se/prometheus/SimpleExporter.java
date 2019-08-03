package org.jocean.j2se.prometheus;

import java.net.InetSocketAddress;

import javax.inject.Inject;
import javax.management.ObjectName;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jmx.export.MBeanExporter;

import io.prometheus.client.CollectorRegistry;

public class SimpleExporter {
    public void start() throws Exception {
        this._httpserver = new MyHttpServer(new InetSocketAddress(_port), CollectorRegistry.defaultRegistry,
                "app_build",    System.getProperty("service.buildno"),
                "application",  System.getProperty("app.name"),
                "hostname",     System.getenv("HOSTNAME")
                );
                //new HTTPServer(new InetSocketAddress(_port), CollectorRegistry.defaultRegistry);

        this._mbeanExporter.registerManagedResource(this._httpserver, ObjectName.getInstance("prometheus:type=simple_exporter"));
    }

    public void stop() throws Exception {
        if (null != this._httpserver) {
            this._httpserver.stop();
            this._mbeanExporter.unregisterManagedResource(ObjectName.getInstance("prometheus:type=simple_exporter"));
        }
    }

    private /*HTTPServer*/ MyHttpServer _httpserver;
    @Inject
    MBeanExporter _mbeanExporter;

    @Value("${port}")
    int _port = 0;
}
