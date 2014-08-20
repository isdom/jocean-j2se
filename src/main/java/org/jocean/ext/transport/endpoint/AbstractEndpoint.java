package org.jocean.ext.transport.endpoint;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import org.jocean.ext.netty.closure.Closure;
import org.jocean.ext.transport.Receiver;
import org.jocean.ext.transport.TransportUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jmx.export.MBeanExporter;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.util.concurrent.BlockingQueue;

public abstract class AbstractEndpoint<I> implements MutableEndpoint<I> {

    private static final Logger logger =
            LoggerFactory.getLogger(AbstractEndpoint.class);

    protected Closure<I> messageClosure;
    protected Receiver<I> nextReceiver;
    protected Channel channel = null;

    protected BlockingQueue<I> pendings = null;

    protected MBeanExporter mbeanExport = null;

    protected String objectNamePrefix;

    protected ObjectName objectName = null;

    /**
     * @return the objectNamePrefix
     */
    public String getObjectNamePrefix() {
        return objectNamePrefix;
    }

    /**
     * @param objectNamePrefix the objectNamePrefix to set
     */
    public void setObjectNamePrefix(String objectNamePrefix) {
        this.objectNamePrefix = objectNamePrefix;
    }

    /**
     * @return the mbeanExport
     */
    public MBeanExporter getMbeanExport() {
        return mbeanExport;
    }

    /**
     * @param mbeanExport the mbeanExport to set
     */
    public void setMbeanExport(MBeanExporter mbeanExport) {
        this.mbeanExport = mbeanExport;
    }

    @Override
    public boolean send(I bean) {
        return pendings.add(bean);
    }

    public void messageReceived(ChannelHandlerContext ctx, I msg) {
        if (null != messageClosure) {
            TransportUtils.attachSender(msg, this);
            this.messageClosure.execute(msg);
        }
        if (null != nextReceiver) {
            this.nextReceiver.messageReceived(ctx, msg);
        }
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
        if (null != objectName && null != mbeanExport) {
            try {
                mbeanExport.getServer().unregisterMBean(objectName);
            } catch (MBeanRegistrationException e) {
                logger.error("setChannel:", e);
                e.printStackTrace();
            } catch (InstanceNotFoundException e) {
                logger.error("setChannel:", e);
            }
        }
        if (null != channel && null != mbeanExport) {
            try {
                objectName = new ObjectName(objectNamePrefix + ",channel=" +
                        channel
                                .toString().replaceAll(":", "/")
                                .replaceAll(",", "")
                                .replaceAll("=>", "-")
                );
                mbeanExport.registerManagedResource(this, objectName);
            } catch (MalformedObjectNameException | NullPointerException e) {
                logger.error("setChannel:", e);
            }
        }
    }

    /* (non-Javadoc)
      * @see com.skymobi.transport.Endpoint#stop()
      */
    public void stop() {
        if (null != objectName && null != mbeanExport) {
            try {
                mbeanExport.getServer().unregisterMBean(objectName);
            } catch (MBeanRegistrationException e) {
                logger.error("setIoSession:", e);
                e.printStackTrace();
            } catch (InstanceNotFoundException e) {
                logger.error("setIoSession:", e);
            }
        }
    }

    public void setMessageClosure(Closure<I> closure) {
        this.messageClosure = closure;
    }

    public void setSendQueue(BlockingQueue<I> queue) {
        this.pendings = queue;
    }

    /**
     * @param nextReceiver the nextReceiver to set
     */
    public void setNextReceiver(Receiver<I> nextReceiver) {
        this.nextReceiver = nextReceiver;
    }

    public int getPendingCount() {
        if (null != this.pendings) {
            return this.pendings.size();
        } else {
            return -1;
        }
    }
}
