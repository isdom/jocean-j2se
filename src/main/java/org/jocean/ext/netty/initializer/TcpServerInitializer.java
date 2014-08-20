package org.jocean.ext.netty.initializer;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;

public abstract class TcpServerInitializer extends BaseInitializer {

    private ChannelHandler tcpRequestHandler;
    private ChannelHandler tcpResponseEncoder;

    @Override
    protected void addCodecHandler(ChannelPipeline pipeline) throws Exception {
        pipeline.addLast("tcpRequestDecoder", getTcpRequestDecoder())
                .addLast("tcpResponseEncoder", tcpResponseEncoder);
    }

    @Override
    protected void addBusinessHandler(ChannelPipeline pipeline) throws Exception {
        pipeline.addLast("handler", tcpRequestHandler);
    }

    public void setTcpRequestHandler(ChannelHandler tcpRequestHandler) {
        this.tcpRequestHandler = tcpRequestHandler;
    }

    public void setTcpResponseEncoder(ChannelHandler tcpResponseEncoder) {
        this.tcpResponseEncoder = tcpResponseEncoder;
    }

    /**
     * 注意tcpRequestDecoder必须确保是线程安全的 ,否则可能会出现解码时的未知异常
     */
    public abstract ChannelHandler getTcpRequestDecoder();
}
