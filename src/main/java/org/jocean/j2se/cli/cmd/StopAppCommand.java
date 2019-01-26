package org.jocean.j2se.cli.cmd;

import java.util.concurrent.atomic.AtomicReference;

import org.jocean.cli.CliCommand;
import org.jocean.cli.CliContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ConfigurableApplicationContext;

public class StopAppCommand implements CliCommand<CliContext> {

    @SuppressWarnings("unused")
    private static final Logger LOG = LoggerFactory.getLogger(StopAppCommand.class);

    final AtomicReference<ConfigurableApplicationContext> _ctxRef;

    public StopAppCommand(final AtomicReference<ConfigurableApplicationContext> ctxRef) {
        this._ctxRef = ctxRef;
    }

    @Override
    public String execute(final CliContext ctx, final String... args) throws Exception {
        if (this._ctxRef.get() == null) {
            return "FAILED: already stopped";
        }

        final ConfigurableApplicationContext appctx = this._ctxRef.getAndSet(null);

        if (null != appctx) {
            appctx.close();
        } else {
            return "FAILED: already stopped";
        }

        return "OK";
    }

    @Override
    public String getAction() {
        return "stopapp";
    }

    @Override
    public String getHelp() {
        return "stop app\r\n\tUsage: stopapp";
    }
}

