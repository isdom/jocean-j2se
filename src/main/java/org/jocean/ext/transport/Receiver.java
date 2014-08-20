package org.jocean.ext.transport;

import io.netty.channel.ChannelHandlerContext;

public interface Receiver<I> {
    void messageReceived(ChannelHandlerContext ctx, I msg);
}
