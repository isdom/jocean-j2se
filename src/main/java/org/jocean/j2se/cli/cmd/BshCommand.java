package org.jocean.j2se.cli.cmd;

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

public class BshCommand implements CliCommand<AppCliContext>, ApplicationContextAware {

    private static final Logger LOG = LoggerFactory.getLogger(BshCommand.class);

    @Override
    public String execute(final AppCliContext ctx, final String... args) throws Exception {
        if (args.length < 1) {
            return "FAILED: missing script params\n" + getHelp();
        }

        final Interpreter inter = new Interpreter();
//      inter.setOut(new PrintStream(outputStream));

        // TODO, 注入 CliContext 环境上下文，可以在 bsh 脚本中与 命令行 环境进行交互，实现类似 stopapp；exitapp 的功能
        try {
//            Interpreter i = new Interpreter();  // Construct an interpreter
//            i.set("foo", 5);                    // Set variables
//            i.set("date", new Date() );
            inter.set("clictx", ctx);
            inter.set("appctx", this._applicationContext);
            return inter.eval( new StringReader(new String(BaseEncoding.base64().decode(args[0]), Charsets.UTF_8)) ).toString();
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
        return "bsh [script's content encode as base64]";
    }

    @Override
    public void setApplicationContext(final ApplicationContext applicationContext) throws BeansException {
        this._applicationContext = (ConfigurableApplicationContext)applicationContext;
    }

    ConfigurableApplicationContext _applicationContext;
}

