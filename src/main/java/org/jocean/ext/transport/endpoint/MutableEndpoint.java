package org.jocean.ext.transport.endpoint;

import io.netty.channel.Channel;
import org.jocean.ext.transport.protocol.endpoint.Endpoint;

import java.util.concurrent.BlockingQueue;

public interface MutableEndpoint<I> extends Endpoint<I> {
	public void start();
	public void setChannel(Channel channel);
	public void setSendQueue(BlockingQueue<I> queue);
}
