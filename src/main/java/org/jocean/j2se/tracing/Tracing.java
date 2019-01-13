package org.jocean.j2se.tracing;

public interface Tracing {

    public interface Scope extends AutoCloseable {
        @Override
        void close();
    }

    public Scope activate();
}
