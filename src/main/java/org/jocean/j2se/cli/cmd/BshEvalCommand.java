package org.jocean.j2se.cli.cmd;

import java.io.PrintStream;
import java.io.StringReader;

import org.jocean.cli.CliCommand;
import org.jocean.idiom.ExceptionUtils;
import org.jocean.j2se.cli.AppCliContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;

import com.google.common.base.Charsets;
import com.google.common.io.BaseEncoding;

import bsh.Interpreter;

public class BshEvalCommand implements CliCommand<AppCliContext>, ApplicationContextAware {

    private static final Logger LOG = LoggerFactory.getLogger(BshEvalCommand.class);

    @Override
    public String execute(final AppCliContext ctx, final String... args) throws Exception {
        if (args.length < 1) {
            return "FAILED: missing script params\n" + getHelp();
        }

        LOG.info("BshEvalCommand.execute()");
        return doBsh(ctx, args);
    }

    private String doBsh(final AppCliContext ctx, final String... args) {
        final Interpreter inter = new Interpreter();
        inter.setOut(new PrintStream(ctx.outputStream()));

        // 注入 CliContext 环境上下文，可以在 bsh 脚本中与 命令行 环境进行交互，实现类似 stopapp；exitapp 的功能
        try {
            inter.set("cli.log", LOG);
            inter.set("cli.ctx", ctx);
            inter.set("spring.root", this._applicationContext);
            for (int idx = 1; idx < args.length; idx += 2) {
                if (idx + 1 < args.length) {
                    //  inject extern args for bsh env
                    inter.set(args[idx], args[idx + 1]);
                }
            }
            final Object ret = inter.eval( new StringReader(new String(BaseEncoding.base64().decode(args[0]), Charsets.UTF_8)) );
            return null != ret ? ret.toString() : null;
        }
        catch (final Exception e) {
            LOG.error("exception when inter.eval, detail: {}", ExceptionUtils.exception2detail(e));
            return "Error: " + ExceptionUtils.exception2detail(e);
        }
    }

    @Override
    public String getAction() {
        return "bsheval";
    }

    @Override
    public String getHelp() {
        return "bsheval [script's content encode as base64] [arg1 name] [value1] [arg2 name] [value2] ...";
    }

    @Override
    public void setApplicationContext(final ApplicationContext applicationContext) throws BeansException {
        this._applicationContext = (ConfigurableApplicationContext)applicationContext;
    }

    ConfigurableApplicationContext _applicationContext;
}

