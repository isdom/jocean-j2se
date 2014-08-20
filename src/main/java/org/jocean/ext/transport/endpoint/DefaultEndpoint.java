package org.jocean.ext.transport.endpoint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author hp
 *
 */
public class DefaultEndpoint extends AbstractEndpoint implements MutableEndpoint {
    private static final Logger logger = LoggerFactory.getLogger(DefaultEndpoint.class);

    private ExecutorService exec = 
        Executors.newSingleThreadExecutor();
    
    private long waitTimeout = 1000;
    
    private void doSendPending() {
		exec.submit(new Runnable() {

			public void run() {
				sendPending();
			}} );
    }
    
    private	void sendPending() {
        if(pendings == null){
            throw new RuntimeException("pendings is null,please set it first!");
        }
    	try {
	    	if ( null == channel ) {
	    		Thread.sleep(waitTimeout);	// sleep 1s 
	    	}
	    	else {
	    		Object bean = pendings.poll(waitTimeout, TimeUnit.MILLISECONDS);
	    		if ( null != bean ) {
	    			channel.writeAndFlush(bean);
	    		}
	    	}
    	} catch (InterruptedException e) {
    		logger.warn("sendPending interrupted", e);
		}
    	finally {
    		doSendPending();
    	}
    }
    
	public long getWaitTimeout() {
		return waitTimeout;
	}

	public void setWaitTimeout(long waitTimeout) {
		this.waitTimeout = waitTimeout;
	}
	
	public void start() {
		doSendPending();
	}

	public void stop() {
		super.stop();
		
        this.exec.shutdownNow();
	}
}
