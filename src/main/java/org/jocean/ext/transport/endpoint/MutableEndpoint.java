package org.jocean.ext.transport.endpoint;

import java.util.concurrent.BlockingQueue;

import org.jocean.ext.transport.protocol.endpoint.Endpoint;

import io.netty.channel.Channel;

public interface MutableEndpoint<I> extends Endpoint<I> {
	public void start();
	public void setChannel(Channel channel);
	public void setSendQueue(BlockingQueue<I> queue);
}
