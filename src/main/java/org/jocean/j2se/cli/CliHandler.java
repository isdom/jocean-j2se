package org.jocean.j2se.cli;

import java.util.Map;

import org.jocean.cli.CliContext;
import org.jocean.cli.CliShell;
import org.jocean.cli.CommandRepository;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import rx.Observable;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

@Sharable
public class CliHandler extends ChannelInboundHandlerAdapter {

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
        Observable.unsafeCreate(subscriber -> {
            final String result = this._shell.execute(new AppContextSupport() {
                @Override
                public Action1<Object> logger() {
                    return obj -> {
                        if (null != obj) {
                            ctx.write(obj.toString());
                            ctx.flush();
                        }
                    };
                }}, (String) msg);
            subscriber.onNext(result);
        }).subscribeOn(Schedulers.computation())
        .subscribe(result -> {
            if ( null != result ) {
                ctx.write(result);
                ctx.flush();
            }
        });
    }

    @Override
    public void channelReadComplete(final ChannelHandlerContext ctx) {
//        ctx.flush();
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