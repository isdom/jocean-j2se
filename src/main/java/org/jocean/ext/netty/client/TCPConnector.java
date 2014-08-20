package org.jocean.ext.netty.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.jocean.ext.netty.initializer.TcpClientInitializer;
import org.jocean.ext.transport.TransportUtils;
import org.jocean.ext.transport.endpoint.EndpointFactory;
import org.jocean.ext.transport.protocol.endpoint.Endpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 非线程安全
 */
public class TCPConnector {
    private static final Logger logger = LoggerFactory.getLogger(TCPConnector.class);

    private ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();
    private ScheduledExecutorService senderService = Executors.newSingleThreadScheduledExecutor();

    private String name = "connector";
    private String destIp = null;
    private int destPort = -1;
    private String localAddress = "0.0.0.0";
    private int localPort;
    private Channel channel = null;
    private TcpClientInitializer tcpClientInitializer;
    private Bootstrap bootstrap = new Bootstrap();
    private NioEventLoopGroup eventLoopGroup = new NioEventLoopGroup();
    private EndpointFactory endpointFactory = null;
    private BlockingQueue<Object> sendQueue = null;
    private long retryTimeout = 1000;
    private long waitTimeout = 1000;
    private boolean needHoldMessage = true;//消息无法发送 默认保存到下次通道建立后再发送

    private final AtomicReference<Endpoint> _endpointRef = new AtomicReference<>(null);

    private class IOHandler extends ChannelInboundHandlerAdapter {

        @Override
        @SuppressWarnings("unchecked")
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            Endpoint endpoint = TransportUtils.getEndpointOfSession(ctx);
            if (null != endpoint) {
                endpoint.messageReceived(ctx, msg);
            } else {
                if (logger.isWarnEnabled())
                    logger.warn("{} missing endpoint, ignore incoming msg:{}", getDesc(), msg);
            }
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            logger.info("channelActive:{}", ctx.channel());
            try {
                channel = ctx.channel();
                //	create endpoint
                Endpoint endpoint = endpointFactory.createEndpoint(channel);
                if (null != endpoint) {
                    TransportUtils.attachEndpointToSession(ctx, endpoint);
                    _endpointRef.set(endpoint);
                }
            } catch (Exception ex) {
                logger.warn(" createEndpoint:", ex);
                channel = null;
                ctx.channel().close();
            }
        }

        @Override
        public void channelInactive(final ChannelHandlerContext ctx) throws Exception {
            _endpointRef.set(null);
            Endpoint endpoint = TransportUtils.getEndpointOfSession(ctx);
            if (null != endpoint) {
                endpoint.stop();
            }
            if (!exec.isShutdown()) {
                exec.submit(new Runnable() {

                    public void run() {
                        onChannelDisconnected(ctx.channel());
                    }
                });
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            logger.error(getDesc() + " TCPConnector:", cause);
        }
    }

    private void doConnect() {
        if (null == destIp || destIp.equals("")) {
            logger.warn(getDesc() + " destIp is null, disable this connector.");
            return;
        }

        List<ChannelHandler> handlers = new ArrayList<ChannelHandler>();
        handlers.add(new IOHandler());
        this.tcpClientInitializer.setOtherHandlers(handlers);

        ChannelFuture future = bootstrap.connect();

        future.addListener(new ChannelFutureListener() {

            public void operationComplete(final ChannelFuture future) throws Exception {
                exec.submit(new Runnable() {
                    public void run() {
                        onConnectComplete(future);
                    }
                });
            }
        });
    }

    private void onConnectComplete(ChannelFuture future) {
        if (!future.isSuccess()) {
            if (logger.isInfoEnabled()) {
                logger.info(getDesc() + " connect [" + this.destIp + ":" + this.destPort + "] failed, retry...");
            }
            exec.schedule(new Runnable() {

                              public void run() {
                                  doConnect();
                              }
                          },
                    retryTimeout, TimeUnit.MILLISECONDS);
        } else {
            //连接成功
            doSendPending();
            InetSocketAddress address = (InetSocketAddress) future.channel().localAddress();
            localAddress = address.getAddress().getHostAddress();
            localPort = address.getPort();
        }
    }

    private void onChannelDisconnected(Channel channel) {
        if (logger.isInfoEnabled()) {
            logger.info(getDesc() + " channel : " + channel + "closed, retry connect...");
        }
        exec.schedule(new Runnable() {

                          public void run() {
                              doConnect();
                          }
                      },
                retryTimeout, TimeUnit.MILLISECONDS);
    }

    private void doSendPending() {
        senderService.submit(new Runnable() {

            public void run() {
                sendPending();
            }
        });
    }

    private void sendPending() {
        if (sendQueue == null) {
            throw new RuntimeException("sendQueue is null,please set it first!");
        }
        try {
            if (null == channel || !channel.isActive() || !channel.isWritable()) {
                if (!needHoldMessage) {
                    sendQueue.clear();
                }
                Thread.sleep(waitTimeout);    // sleep 1s
            } else {
                Object bean = sendQueue.poll(waitTimeout, TimeUnit.MILLISECONDS);
                if (null != bean) {
                    channel.writeAndFlush(bean);
                }
            }
        } catch (InterruptedException e) {
            logger.error("sendPending", e);
        } finally {
            doSendPending();
        }
    }

    public TCPConnector() {
    }

    public TCPConnector(String name) {
        this.name = name;
    }

    public String getDestIp() {
        return destIp;
    }

    public void setDestIp(String destIp) {
        this.destIp = destIp;
    }

    public int getDestPort() {
        return destPort;
    }

    public void setDestPort(int destPort) {
        this.destPort = destPort;
    }

    public TcpClientInitializer getTcpClientInitializer() {
        return tcpClientInitializer;
    }

    public void setTcpClientInitializer(TcpClientInitializer tcpClientInitializer) {
        this.tcpClientInitializer = tcpClientInitializer;
    }

    public EndpointFactory getEndpointFactory() {
        return endpointFactory;
    }

    public void setEndpointFactory(EndpointFactory endpointFactory) {
        this.endpointFactory = endpointFactory;
    }

    public void start() {
        bootstrap.group(eventLoopGroup)
                .channel(NioSocketChannel.class)
                .localAddress(localAddress, localPort)
                .remoteAddress(destIp, destPort);
        bootstrap.handler(tcpClientInitializer);
        doConnect();
    }

    public void stop() {
        this.exec.shutdownNow();
        this.eventLoopGroup.shutdownGracefully();
        if (this.channel != null) {
            this.channel.close();
        }
    }

    private String getDesc() {
        return "[" + name + "]";
    }

    public long getRetryTimeout() {
        return retryTimeout;
    }

    public void setRetryTimeout(long retryTimeout) {
        this.retryTimeout = retryTimeout;
    }

    public boolean isActive() {
        return channel != null && channel.isActive();
    }

    public void setSendQueue(BlockingQueue<Object> sendQueue) {
        this.sendQueue = sendQueue;
    }

    public int getPendingSendCount() {
        return sendQueue == null ? 0 : sendQueue.size();
    }

    public int getSendQueueRemainingCapacity() {
        return sendQueue == null ? 0 : sendQueue.remainingCapacity();
    }

    public String getLocalAddress() {
        return localAddress;
    }

    public void setLocalAddress(String localAddress) {
        this.localAddress = localAddress;
    }

    public int getLocalPort() {
        return localPort;
    }

    public void setLocalPort(int localPort) {
        this.localPort = localPort;
    }

    public long getWaitTimeout() {
        return waitTimeout;
    }

    public boolean isNeedHoldMessage() {
        return needHoldMessage;
    }

    public void setNeedHoldMessage(boolean needHoldMessage) {
        this.needHoldMessage = needHoldMessage;
    }
}
