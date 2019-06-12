package org.jocean.j2se.cli;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

import org.jocean.cli.CliContext;
import org.jocean.cli.CliShell;
import org.jocean.cli.CommandRepository;
import org.jocean.j2se.logback.BytesAppender;
import org.jocean.j2se.logback.OutputBytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.AttributeKey;
import rx.Observable;
import rx.schedulers.Schedulers;

@Sharable
public class CliHandler extends ChannelInboundHandlerAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(CliHandler.class);

    private static AttributeKey<OutputBytes> OUTPUT = AttributeKey.valueOf("LOG");

    @Override
    public void channelActive(final ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
    }

    private void enableOutput(final ChannelHandlerContext ctx) {
        if (null == ctx.channel().attr(OUTPUT).get()) {
            final OutputBytes output = bytes -> {
                if (null != bytes && ctx.channel().isActive()) {
                    ctx.write(Unpooled.wrappedBuffer(bytes));
                    ctx.flush();
                }
            };
            ctx.channel().attr(OUTPUT).set(output);

            BytesAppender.addToRoot(ctx.channel().id().toString(), output);

            LOG.info("append BytesAppender instance named:{}", ctx.channel().id().toString());
        }
    }

    @Override
    public void channelInactive(final ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);

        disableOutput(ctx);
    }

    private void disableOutput(final ChannelHandlerContext ctx) {
        final OutputBytes output = ctx.channel().attr(OUTPUT).get();
        if (null != output) {
            LOG.info("detach BytesAppender instance named:{}", ctx.channel().id().toString());
            BytesAppender.detachFromRoot(ctx.channel().id().toString());
        }
    }

    abstract class AppContextSupport implements AppCliContext {

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
        }
        @Override
        public CliController getCliController() {
            return _ctrl;
        }
        @Override
        public CommandRepository getCommandRepository() {
            return _repo;
        }
    }

    CliHandler(final CliShell<AppCliContext> shell, final CommandRepository commandRepository, final CliController cliController) {
        this._shell = shell;
        this._ctrl = cliController;
        this._repo = commandRepository;
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, final Object msg) {
        Observable.<String>unsafeCreate(subscriber -> {
            final String result = this._shell.execute(new AppContextSupport() {
                @Override
                public void enableSendbackLOG() {
                    enableOutput(ctx);
                }

                @Override
                public void disableSendbackLOG() {
                    disableOutput(ctx);
                }

                @Override
                public OutputStream outputStream() {
                    return new OutputStream() {
                        @Override
                        public void write(final int b) throws IOException {
                            ctx.write(ctx.alloc().buffer(1).writeByte(b));
//                            ctx.write(Unpooled.wrappedBuffer(new byte[]{(byte)b}));
                        }

                        @Override
                        public void write(final byte[] b, final int off, final int len) throws IOException {
                            ctx.write(Unpooled.wrappedBuffer(b, off, len));
//                            if (off == 0 && b.length == len) {
//                                ctx.write(b);
//                            }
//                            else {
//                                ctx.write(Arrays.copyOfRange(b, off, off + len));
//                            }
                        }

                        @Override
                        public void flush() throws IOException {
                            ctx.flush();
                        }};
                }}, (String) msg);
            subscriber.onNext(result);
            subscriber.onCompleted();
        }).subscribeOn(Schedulers.computation())
        .subscribe(result -> {
            if ( null != result ) {
                ctx.write(Unpooled.wrappedBuffer(result.getBytes(Charsets.UTF_8)));
                ctx.flush();
            }
        }, e -> {}, () -> ctx.close());
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

    private final CliShell<AppCliContext> _shell;
    private final CliController _ctrl;
    private final CommandRepository _repo;
}