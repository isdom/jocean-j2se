package org.jocean.ext.netty.handler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.jocean.ext.netty.closure.Closure;
import org.jocean.ext.transport.TransportUtils;
import org.jocean.ext.transport.Validateable;
import org.jocean.ext.transport.endpoint.EndpointFactory;
import org.jocean.ext.transport.protocol.endpoint.Endpoint;
import org.jocean.ext.util.Identifyable;
import org.jocean.j2se.jmx.MBeanRegisterSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.UUID;

@ChannelHandler.Sharable
public class TcpRequestHandler extends ChannelInboundHandlerAdapter {

    public static interface ChannelMXBean {

        public String getId();

        public boolean isConnected();

        public String getRemoteIp();

        public int getRemotePort();

        public String getLocalIp();

        public int getLocalPort();
    }

    private static Logger logger = LoggerFactory.getLogger(TcpRequestHandler.class);

    private Map<Object, Closure> uuidMap;

    private EndpointFactory endpointFactory = null;

    private final MBeanRegisterSupport _mbeanSupport;

    public TcpRequestHandler() {
        this("public:class=transport.tcp.netty.server,name=inbound");
    }

    public TcpRequestHandler(final String objectNamePrefix) {
        this._mbeanSupport = new MBeanRegisterSupport(objectNamePrefix, null);
    }

    public void destroy() {
        this._mbeanSupport.destroy();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
            throws Exception {
        if (!(cause instanceof IOException)) {
            logger.error("exceptionCaught: ", cause);
        } else {
            logger.debug("exceptionCaught: ", cause);
        }
        ctx.channel().close();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        logger.debug("messageReceived {}", msg);
        if (msg instanceof Validateable) {
            Object result = ((Validateable) msg).validate();
            if (result != null) {
                ctx.channel().writeAndFlush(result);
                logger.info("request {} validate failed,return resp {}", msg, result);
                return;
            }
        }
        Endpoint endpoint = TransportUtils.getEndpointOfSession(ctx);
        if (null != endpoint) {
            endpoint.messageReceived(ctx, msg);
        }
        if (uuidMap != null) {
            if (msg instanceof Identifyable) {
                UUID uuid = ((Identifyable) msg).getIdentification();
                if (uuidMap.containsKey(uuid)) {
                    uuidMap.remove(uuid).execute(msg);
                } else {
                    logger.info("can't find matched uuid {}", uuid);
                }
            }
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if (logger.isInfoEnabled()) {
            logger.info("channelInactive: [{}]", ctx.channel().remoteAddress());
        }
        unregisterCtxMBean(ctx);
        Endpoint endpoint = TransportUtils.getEndpointOfSession(ctx);
        if (null != endpoint) {
            endpoint.stop();
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        logger.info("channelActive:{}", ctx.channel());
        try {
            //	create endpoint
            Endpoint endpoint = endpointFactory.createEndpoint(ctx.channel());
            if (null != endpoint) {
                TransportUtils.attachEndpointToSession(ctx, endpoint);
            }

            //	register mbean for ctx
            registerCtxMBean(ctx);
        } catch (Exception ex) {
            logger.warn(" createEndpoint:", ex);
            ctx.channel().close();
        }
    }

    private void registerCtxMBean(final ChannelHandlerContext ctx) {
        final Channel channel = ctx.channel();

        this._mbeanSupport.registerMBean(genChannelSuffix(channel), new ChannelMXBean() {

            @Override
            public String getId() {
                return channel.toString();
            }

            @Override
            public boolean isConnected() {
                return channel.isActive();
            }

            @Override
            public String getRemoteIp() {
                return ((InetSocketAddress) channel.remoteAddress()).getAddress().getHostAddress();
            }

            @Override
            public int getRemotePort() {
                return ((InetSocketAddress) channel.remoteAddress()).getPort();
            }

            @Override
            public String getLocalIp() {
                return ((InetSocketAddress) channel.localAddress()).getAddress().getHostAddress();
            }

            @Override
            public int getLocalPort() {
                return ((InetSocketAddress) channel.localAddress()).getPort();
            }
        });

    }

    private void unregisterCtxMBean(final ChannelHandlerContext ctx) {
        final Channel channel = ctx.channel();

        this._mbeanSupport.unregisterMBean(genChannelSuffix(channel));
    }

    /**
     * @param channel
     * @return
     */
    private static String genChannelSuffix(final Channel channel) {
        InetSocketAddress dstAddr = (InetSocketAddress) channel.remoteAddress();
        return "id=" + dstAddr.getAddress().getHostAddress() + "_" + dstAddr.getPort();
    }

    public Map<Object, Closure> getUuidMap() {
        return uuidMap;
    }

    /**
     * 必须为线程安全的,内存安全的Map,建议使用guava的Maps类创建
     *
     * @param uuidMap
     */
    public void setUuidMap(Map<Object, Closure> uuidMap) {
        this.uuidMap = uuidMap;
    }

    public void setEndpointFactory(EndpointFactory endpointFactory) {
        this.endpointFactory = endpointFactory;
    }
}
