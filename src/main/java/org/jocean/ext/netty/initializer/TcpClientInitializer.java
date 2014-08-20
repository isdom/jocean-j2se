package org.jocean.ext.netty.initializer;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.MessageToByteEncoder;

import java.util.List;

public class TcpClientInitializer extends BaseInitializer {
    private String tcpResponseDecoder;
    private MessageToByteEncoder tcpRequestEncoder;
    private List<ChannelHandler> otherHandlers;

    @Override
    protected void addCodecHandler(ChannelPipeline pipeline) throws Exception {
        pipeline.addLast("tcpResponseDecoder", applicationContext.getBean(tcpResponseDecoder, ChannelHandler.class))
                .addLast("tcpRequestEncoder", tcpRequestEncoder);
    }

    @Override
    protected void addBusinessHandler(ChannelPipeline pipeline) throws Exception {
        //注意Codec的线程安全
        for (ChannelHandler handler : otherHandlers) {
            pipeline.addLast(handler.toString(), handler);
        }
    }

    public void setTcpRequestEncoder(MessageToByteEncoder tcpRequestEncoder) {
        this.tcpRequestEncoder = tcpRequestEncoder;
    }

    /**
     * 注意tcpResponseDecoder必须确保是线程安全的 ,否则可能会出现解码时的未知异常
     *
     * @param tcpResponseDecoder tcpResponseDecoder的beanName
     */
    public void setTcpResponseDecoder(String tcpResponseDecoder) {
        this.tcpResponseDecoder = tcpResponseDecoder;
    }

    public List<ChannelHandler> getOtherHandlers() {
        return otherHandlers;
    }

    public void setOtherHandlers(List<ChannelHandler> otherHandlers) {
        this.otherHandlers = otherHandlers;
    }
}
