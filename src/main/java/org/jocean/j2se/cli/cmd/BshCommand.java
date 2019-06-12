package org.jocean.j2se.cli.cmd;

import java.io.PrintStream;
import java.io.Reader;
import java.util.concurrent.Executors;

import org.jocean.cli.CliCommand;
import org.jocean.idiom.ExceptionUtils;
import org.jocean.j2se.cli.AppCliContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;

import bsh.Interpreter;

public class BshCommand implements CliCommand<AppCliContext>, ApplicationContextAware {

    private static final Logger LOG = LoggerFactory.getLogger(BshCommand.class);

    @Override
    public String execute(final AppCliContext ctx, final String... args) throws Exception {
        return doBsh(ctx, args);
    }

    private String doBsh(final AppCliContext ctx, final String... args) {
        final Reader in = ctx.redirectInput();
        final PrintStream out = new PrintStream(ctx.outputStream());

        final Interpreter inter = new Interpreter(in, out, out, true);

        inter.setExitOnEOF(false);

        // 注入 CliContext 环境上下文，可以在 bsh 脚本中与 命令行 环境进行交互，实现类似 stopapp；exitapp 的功能
        try {
            inter.set("_LOG", LOG);
            inter.set("_clictx", ctx);
            inter.set("_appctx", this._applicationContext);
            Executors.newFixedThreadPool(1).submit(() -> {
                inter.run();
            });
            return "bsh started";
        }
        catch (final Exception e) {
            LOG.error("exception when inter.eval, detail: {}", ExceptionUtils.exception2detail(e));
            return "Error: " + ExceptionUtils.exception2detail(e);
        }
    }

    @Override
    public String getAction() {
        return "bsh";
    }

    @Override
    public String getHelp() {
        return "bsh";
    }

    @Override
    public void setApplicationContext(final ApplicationContext applicationContext) throws BeansException {
        this._applicationContext = (ConfigurableApplicationContext)applicationContext;
    }

    ConfigurableApplicationContext _applicationContext;
}

