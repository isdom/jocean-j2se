package org.jocean.ext.transport;

public interface Sender<I> {
    boolean send(I msg);
}
