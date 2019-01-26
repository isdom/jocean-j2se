package org.jocean.j2se.cli;

import org.jocean.cli.CliContext;
import org.jocean.cli.CliShell;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

@Sharable
public class CliHandler extends ChannelInboundHandlerAdapter {

    CliHandler(final CliShell<? extends CliContext> shell) {
        this._shell = shell;
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, final Object msg) {
        final String result = this._shell.execute((String) msg);
        if ( null != result ) {
            ctx.write(result);
            ctx.flush();
        }
    }

    @Override
    public void channelReadComplete(final ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) {
        // Close the connection when an exception is raised.
        cause.printStackTrace();
        ctx.close();
    }

    private final CliShell<? extends CliContext> _shell;
}