package org.jocean.j2se.cli;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.jocean.cli.CliContext;
import org.jocean.cli.CliShell;
import org.jocean.cli.CommandRepository;
import org.jocean.j2se.logback.BytesAppender;
import org.jocean.j2se.logback.OutputBytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;

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
                    ctx.write(ctx.alloc().buffer(bytes.length).writeBytes(bytes));
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

    Reader buildReader(final BlockingQueue<String> queue, final ChannelHandlerContext ctx) {
        final AtomicReference<StringReader> _currentStringReader = new AtomicReference<>(null);

        return new Reader() {
            @Override
            public int read(final char[] cbuf, final int off, final int len) throws IOException {
                final StringReader stringReader = _currentStringReader.get();
                if (null != stringReader) {
                    final int readed = stringReader.read(cbuf, off, len);
                    if (readed > -1) {
                        return readed;
                    }
                    _currentStringReader.set(null);
                }

                do {
                    try {
                        final String ss = queue.poll(100, TimeUnit.MILLISECONDS);
                        if (null != ss) {
                            _currentStringReader.set(new StringReader(ss));
                            return _currentStringReader.get().read(cbuf, off, len);
                        }
                    } catch (final InterruptedException e) {
                    }
                } while (ctx.channel().isActive());

                return -1;
            }

            @Override
            public void close() throws IOException {
                ctx.close();
            }};
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, final Object msg) {
        if (_queue != null) {
            if (msg.equals("quit")) {
                ctx.close();
            }
            else {
                _queue.offer((String)msg + "\n");
            }
        }
        else {
            executeCommand(ctx, (String)msg);
        }
    }

    private void executeCommand(final ChannelHandlerContext ctx, final String cmd) {
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
                        }

                        @Override
                        public void write(final byte[] b, final int off, final int len) throws IOException {
                            ctx.write(ctx.alloc().buffer(len).writeBytes(b, off, len));
                        }

                        @Override
                        public void flush() throws IOException {
                            ctx.flush();
                        }};
                }

                @Override
                public Reader redirectInput() {
                    _queue = new LinkedBlockingQueue<String>(100);
                    return buildReader(_queue, ctx);
                }}, cmd);
            if (null != result) {
                subscriber.onNext(result);
            }
            if (null == _queue) {
                subscriber.onCompleted();
            }
        }).subscribeOn(Schedulers.computation())
        .subscribe(result -> {
            if ( null != result ) {
                final byte[] bytes = result.getBytes(Charsets.UTF_8);
                ctx.write(ctx.alloc().buffer(bytes.length).writeBytes(bytes));
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
    private BlockingQueue<String> _queue;
}