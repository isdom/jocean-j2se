package org.jocean.ext.transport.endpoint;

import io.netty.channel.Channel;
import org.jocean.ext.transport.protocol.endpoint.Endpoint;

public interface EndpointFactory {
	public Endpoint createEndpoint(Channel channel);
}
