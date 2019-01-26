package org.jocean.j2se.cli.cmd;

import org.jocean.cli.CliCommand;
import org.jocean.cli.CliContext;
import org.jocean.j2se.cli.CliController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExitSrvCommand implements CliCommand<CliContext> {

    @SuppressWarnings("unused")
    private static final Logger LOG = LoggerFactory.getLogger(ExitSrvCommand.class);

    public ExitSrvCommand(final CliController cli) {
        this._cli = cli;
    }

    @Override
    public String execute(final CliContext ctx, final String... args) throws Exception {
        this._cli.stop();
        System.exit(0);

        return "OK";
    }

    @Override
    public String getAction() {
        return "exitsrv";
    }

    @Override
    public String getHelp() {
        return null;
    }

    final CliController _cli;
}

