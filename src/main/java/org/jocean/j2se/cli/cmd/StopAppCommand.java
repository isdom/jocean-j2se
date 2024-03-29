package org.jocean.j2se.cli.cmd;

import org.jocean.cli.CliCommand;
import org.jocean.j2se.cli.AppCliContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;

public class StopAppCommand implements CliCommand<AppCliContext>, ApplicationContextAware {

    @SuppressWarnings("unused")
    private static final Logger LOG = LoggerFactory.getLogger(StopAppCommand.class);

    ConfigurableApplicationContext _applicationContext;

    @Override
    public void setApplicationContext(final ApplicationContext applicationContext) throws BeansException {
        this._applicationContext = (ConfigurableApplicationContext)applicationContext;
    }

    @Override
    public String execute(final AppCliContext ctx, final String... args) throws Exception {
        LOG.info("exxecute StopAppCommand");
        
        if (this._applicationContext == null) {
            return "FAILED: already stopped";
        } else {
            ctx.enableSendbackLOG();

            try {
                this._applicationContext.close();
            } finally {
                this._applicationContext = null;
            }
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

