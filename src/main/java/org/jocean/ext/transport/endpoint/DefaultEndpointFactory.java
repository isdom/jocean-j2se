package org.jocean.ext.transport.endpoint;

import io.netty.channel.Channel;
import org.jocean.ext.transport.protocol.endpoint.Endpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public abstract class DefaultEndpointFactory implements EndpointFactory {
    private static final Logger logger =
    	LoggerFactory.getLogger(DefaultEndpointFactory.class);

    private BlockingQueue<Object>	pendings = null;

    public DefaultEndpointFactory(boolean shareSendQueue, int cachedMessageCount) {
    	if ( shareSendQueue ) {
    		this.pendings = new LinkedBlockingQueue<>(cachedMessageCount);
    	}
    	else {
    		this.pendings = null;
    	}
    }

	public Endpoint createEndpoint(Channel channel) {
		MutableEndpoint endpoint = createEndpoint();
		endpoint.setChannel(channel);
		if ( null != pendings ) {
			endpoint.setSendQueue(pendings);
		}
		endpoint.start();
		return endpoint;
	}
	
	public void stop() {
		if ( null != this.pendings ) {
			this.pendings.clear();
		}
	}
	
	public void send(Object bean) {
		if ( null != this.pendings ) {
			if ( logger.isTraceEnabled()) {
				logger.trace("send bean:" + bean);
			}
			pendings.add(bean);
		}
		else {
			throw new RuntimeException("Unsupport Operation for send.");
		}
	}

	public int getPendingCount() {
		if ( null != this.pendings ) {
			return	this.pendings.size();
		}
		else {
			return	-1;
		}
	}
	
    protected abstract MutableEndpoint createEndpoint();
}
