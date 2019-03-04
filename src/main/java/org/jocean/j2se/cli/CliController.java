package org.jocean.j2se.cli;

import java.util.Map;

import org.jocean.cli.AbstractCliContext;
import org.jocean.cli.CliContext;
import org.jocean.cli.CliShell;
import org.jocean.cli.CommandRepository;
import org.jocean.j2se.os.OSUtil;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerDomainSocketChannel;
import io.netty.channel.unix.DomainSocketAddress;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.CharsetUtil;
import io.netty.util.concurrent.DefaultThreadFactory;

public class CliController {

    static final EventLoopGroup EPOLL_BOSS_GROUP =
            new EpollEventLoopGroup(1, new DefaultThreadFactory("cli-boss", true));

    static final EventLoopGroup EPOLL_WORKER_GROUP =
            new EpollEventLoopGroup(1, new DefaultThreadFactory("cli-worker", true));

    static final ByteBuf _SEMICOLON = Unpooled.wrappedBuffer(";".getBytes());

    Channel _bindChannel;

    CliShell<CliContext> _shell = new CliShell<>();

    final String _socketPath;

    public CliController(final String socketPath) {
        this._socketPath = socketPath;
    }

    public void setCommandRepository(final CommandRepository commandRepository) {
        final AbstractCliContext ctx = new AbstractCliContext() {
            @Override
            public <V> V getProperty(final String key) {
                return null;
            }
            @Override
            public <V> CliContext setProperty(final String key, final V obj) {
                return null;
            }
            @Override
            public Map<String, Object> getProperties() {
                return null;
            }};

        ctx.setCommandRepository(commandRepository);

        _shell.setCommandContext(ctx);
    }

    public void start() throws InterruptedException {
        final CliHandler cliHandler = new CliHandler(this._shell);

        final ServerBootstrap sb = new ServerBootstrap().group(EPOLL_BOSS_GROUP, EPOLL_WORKER_GROUP)
                        .channel(EpollServerDomainSocketChannel.class)
                        .option(ChannelOption.SO_BACKLOG, 100)
                        .handler(new LoggingHandler(LogLevel.INFO))
                        .childHandler(new ChannelInitializer<Channel>() {
                            @Override
                            protected void initChannel(final Channel channel) throws Exception {
                                final ChannelPipeline p = channel.pipeline();
                                p.addLast(new LoggingHandler(LogLevel.INFO));
                                p.addLast(new DelimiterBasedFrameDecoder(2048, _SEMICOLON));
                                p.addLast(new StringDecoder(CharsetUtil.UTF_8));
                                p.addLast(new StringEncoder(CharsetUtil.UTF_8));
                                p.addLast(cliHandler);
                            }});
        final DomainSocketAddress localAddress = new DomainSocketAddress(this._socketPath + OSUtil.getCurrentPid() + ".socket");

        final ChannelFuture future = sb.bind(localAddress);
        future.sync();

        _bindChannel = future.channel();
    }

    public void stop() throws InterruptedException {
        if (_bindChannel != null) {
            _bindChannel.close().sync();
        }
    }

    public void await() throws InterruptedException {
        EPOLL_BOSS_GROUP.terminationFuture().await();
    }
}
