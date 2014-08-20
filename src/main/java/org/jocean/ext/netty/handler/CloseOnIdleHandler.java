package org.jocean.ext.netty.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * 空闲时关闭Channel
 */
public class CloseOnIdleHandler extends IdleStateHandler {
    private static final Logger logger = LoggerFactory.getLogger(CloseOnIdleHandler.class);

    public CloseOnIdleHandler(int readerIdleTimeSeconds, int writerIdleTimeSeconds, int allIdleTimeSeconds) {
        super(readerIdleTimeSeconds, writerIdleTimeSeconds, allIdleTimeSeconds);
    }

    public CloseOnIdleHandler(long readerIdleTime, long writerIdleTime, long allIdleTime, TimeUnit unit) {
        super(readerIdleTime, writerIdleTime, allIdleTime, unit);
    }

    @Override
    protected void channelIdle(ChannelHandlerContext ctx, IdleStateEvent evt) throws Exception {
        if (logger.isInfoEnabled()) {
            logger.info("channelIdle: " + evt.state().name() + " , close channel[" + ctx.channel().remoteAddress() + "]");
        }
        ctx.channel().close();
    }
}
