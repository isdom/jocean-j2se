package org.jocean.j2se.cli.cmd;

import javax.inject.Inject;

import org.jocean.cli.CliCommand;
import org.jocean.j2se.cli.AppCliContext;
import org.jocean.j2se.cli.CliController;
import org.jocean.j2se.os.OSUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExitSrvCommand implements CliCommand<AppCliContext> {

    private static final Logger LOG = LoggerFactory.getLogger(ExitSrvCommand.class);

    @Override
    public String execute(final AppCliContext ctx, final String... args) throws Exception {
        ctx.enableSendbackLOG();
        LOG.warn("execute exit command, will stop JVM process {}", OSUtil.getCurrentPid());
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

    @Inject
    CliController _cli;
}
