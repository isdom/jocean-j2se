package org.jocean.j2se.cli.cmd;

import org.jocean.cli.CliCommand;
import org.jocean.cli.CliContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BshCommand implements CliCommand<CliContext> {

    private static final Logger LOG = LoggerFactory.getLogger(BshCommand.class);

    @Override
    public String execute(final CliContext ctx, final String... args) throws Exception {
        return "OK";
    }

    @Override
    public String getAction() {
        return "bsh";
    }

    @Override
    public String getHelp() {
        return null;
    }
}

