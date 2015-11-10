package org.jocean.ext.transport.endpoint;

import org.jocean.ext.transport.protocol.endpoint.Endpoint;

import io.netty.channel.Channel;

public interface EndpointFactory {
	public Endpoint createEndpoint(Channel channel);
}
