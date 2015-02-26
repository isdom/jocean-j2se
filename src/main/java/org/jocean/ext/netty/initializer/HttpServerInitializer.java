package org.jocean.ext.netty.initializer;

import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.ssl.SslHandler;
//import org.jocean.restful.ssl.SecureChatSslContextFactory;

import javax.net.ssl.SSLEngine;

public abstract class HttpServerInitializer extends BaseInitializer {
    //一个HTTP请求提交的最大数据量,默认为1M
    private int maxContentLength = 1024 * 1024;
    private boolean enableSSL;

    @Override
    public void addCodecHandler(ChannelPipeline pipeline) throws Exception {
        //  TODO adjust SecureChatSslContextFactory's package
//        if (this.enableSSL) {
//            final SSLEngine engine = SecureChatSslContextFactory.getServerContext().createSSLEngine();
//            engine.setUseClientMode(false);
//            pipeline.addFirst("ssl", new SslHandler(engine));
//        }
        //HttpServerCodec是非线程安全的,不能所有Channel使用同一个
        HttpServerCodec httpServerCodec = new HttpServerCodec();
        pipeline.addLast("codec", httpServerCodec)
                .addLast("aggregator", new HttpObjectAggregator(maxContentLength));
    }

    @Override
    protected void addBusinessHandler(ChannelPipeline pipeline) throws Exception {
        pipeline.addLast("handler", getHttpRequestHandler());
    }

    public abstract ChannelInboundHandler getHttpRequestHandler();

    public int getMaxContentLength() {
        return maxContentLength;
    }

    public void setMaxContentLength(int maxContentLength) {
        this.maxContentLength = maxContentLength;
    }

    public boolean isEnableSSL() {
        return enableSSL;
    }

    public void setEnableSSL(boolean enableSSL) {
        this.enableSSL = enableSSL;
    }
}
