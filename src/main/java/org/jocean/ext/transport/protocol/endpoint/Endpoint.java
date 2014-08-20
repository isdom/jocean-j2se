package org.jocean.ext.transport.protocol.endpoint;

import org.jocean.ext.transport.Receiver;
import org.jocean.ext.transport.Sender;

public interface Endpoint<I> extends Sender<I>, Receiver<I> {
	public void stop();
}
