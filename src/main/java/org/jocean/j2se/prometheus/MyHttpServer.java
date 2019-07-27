package org.jocean.j2se.prometheus;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ThreadFactory;
import java.util.zip.GZIPOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.common.TextFormat;

/**
 * Expose Prometheus metrics using a plain Java HttpServer.
 * <p>
 * Example Usage:
 * <pre>
 * {@code
 * HTTPServer server = new HTTPServer(1234);
 * }
 * </pre>
 * */
public class MyHttpServer {

    private static final Logger LOG = LoggerFactory.getLogger(MyHttpServer.class);

    private static class LocalByteArray extends ThreadLocal<ByteArrayOutputStream> {
        @Override
        protected ByteArrayOutputStream initialValue()
        {
            return new ByteArrayOutputStream(1 << 20);
        }
    }

    static class HTTPMetricHandler implements HttpHandler {
        private final CollectorRegistry registry;
        private final LocalByteArray response = new LocalByteArray();

        HTTPMetricHandler(final CollectorRegistry registry) {
          this.registry = registry;
        }


        @Override
        public void handle(final HttpExchange t) throws IOException {
            final String query = t.getRequestURI().getRawQuery();

            final ByteArrayOutputStream response = this.response.get();
            response.reset();

            final long start = System.currentTimeMillis();
            final OutputStreamWriter osw = new OutputStreamWriter(response);
            TextFormat.write004(osw,
                    registry.filteredMetricFamilySamples(parseQuery(query)));
            osw.flush();
            osw.close();
            response.flush();
            response.close();
            LOG.info("sun httpserver /metrics's TextFormat.write004 cost: {}", System.currentTimeMillis() - start);

            t.getResponseHeaders().set("Content-Type",
                    TextFormat.CONTENT_TYPE_004);
            if (shouldUseCompression(t)) {
                t.getResponseHeaders().set("Content-Encoding", "gzip");
                t.sendResponseHeaders(HttpURLConnection.HTTP_OK, 0);
                final GZIPOutputStream os = new GZIPOutputStream(t.getResponseBody());
                response.writeTo(os);
                os.close();
            } else {
                t.getResponseHeaders().set("Content-Length",
                        String.valueOf(response.size()));
                t.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.size());
                response.writeTo(t.getResponseBody());
            }
            t.close();
        }

    }

    protected static boolean shouldUseCompression(final HttpExchange exchange) {
        final List<String> encodingHeaders = exchange.getRequestHeaders().get("Accept-Encoding");
        if (encodingHeaders == null) return false;

        for (final String encodingHeader : encodingHeaders) {
            final String[] encodings = encodingHeader.split(",");
            for (final String encoding : encodings) {
                if (encoding.trim().toLowerCase().equals("gzip")) {
                    return true;
                }
            }
        }
        return false;
    }

    protected static Set<String> parseQuery(final String query) throws IOException {
        final Set<String> names = new HashSet<String>();
        if (query != null) {
            final String[] pairs = query.split("&");
            for (final String pair : pairs) {
                final int idx = pair.indexOf("=");
                if (idx != -1 && URLDecoder.decode(pair.substring(0, idx), "UTF-8").equals("name[]")) {
                    names.add(URLDecoder.decode(pair.substring(idx + 1), "UTF-8"));
                }
            }
        }
        return names;
    }


    static class DaemonThreadFactory implements ThreadFactory {
        private final ThreadFactory delegate;
        private final boolean daemon;

        DaemonThreadFactory(final ThreadFactory delegate, final boolean daemon) {
            this.delegate = delegate;
            this.daemon = daemon;
        }

        @Override
        public Thread newThread(final Runnable r) {
            final Thread t = delegate.newThread(r);
            t.setDaemon(daemon);
            return t;
        }

        static ThreadFactory defaultThreadFactory(final boolean daemon) {
            return new DaemonThreadFactory(Executors.defaultThreadFactory(), daemon);
        }
    }

    protected final HttpServer server;
    protected final ExecutorService executorService;


    /**
     * Start a HTTP server serving Prometheus metrics from the given registry.
     */
    public MyHttpServer(final InetSocketAddress addr, final CollectorRegistry registry, final boolean daemon) throws IOException {
        server = HttpServer.create();
        server.bind(addr, 3);
        final HttpHandler mHandler = new HTTPMetricHandler(registry);
        server.createContext("/", mHandler);
        server.createContext("/metrics", mHandler);
        executorService = Executors.newFixedThreadPool(5, DaemonThreadFactory.defaultThreadFactory(daemon));
        server.setExecutor(executorService);
        start(daemon);
    }

    /**
     * Start a HTTP server serving Prometheus metrics from the given registry using non-daemon threads.
     */
    public MyHttpServer(final InetSocketAddress addr, final CollectorRegistry registry) throws IOException {
        this(addr, registry, false);
    }

    /**
     * Start a HTTP server serving the default Prometheus registry.
     */
    public MyHttpServer(final int port, final boolean daemon) throws IOException {
        this(new InetSocketAddress(port), CollectorRegistry.defaultRegistry, daemon);
    }

    /**
     * Start a HTTP server serving the default Prometheus registry using non-daemon threads.
     */
    public MyHttpServer(final int port) throws IOException {
        this(port, false);
    }

    /**
     * Start a HTTP server serving the default Prometheus registry.
     */
    public MyHttpServer(final String host, final int port, final boolean daemon) throws IOException {
        this(new InetSocketAddress(host, port), CollectorRegistry.defaultRegistry, daemon);
    }

    /**
     * Start a HTTP server serving the default Prometheus registry using non-daemon threads.
     */
    public MyHttpServer(final String host, final int port) throws IOException {
        this(new InetSocketAddress(host, port), CollectorRegistry.defaultRegistry, false);
    }

    /**
     * Start a HTTP server by making sure that its background thread inherit proper daemon flag.
     */
    private void start(final boolean daemon) {
        if (daemon == Thread.currentThread().isDaemon()) {
            server.start();
        } else {
            final FutureTask<Void> startTask = new FutureTask<Void>(new Runnable() {
                @Override
                public void run() {
                    server.start();
                }
            }, null);
            DaemonThreadFactory.defaultThreadFactory(daemon).newThread(startTask).start();
            try {
                startTask.get();
            } catch (final ExecutionException e) {
                throw new RuntimeException("Unexpected exception on starting HTTPSever", e);
            } catch (final InterruptedException e) {
                // This is possible only if the current tread has been interrupted,
                // but in real use cases this should not happen.
                // In any case, there is nothing to do, except to propagate interrupted flag.
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Stop the HTTP server.
     */
    public void stop() {
        server.stop(0);
        executorService.shutdown(); // Free any (parked/idle) threads in pool
    }

    /**
     * Gets the port number.
     */
    public int getPort() {
        return server.getAddress().getPort();
    }
}

