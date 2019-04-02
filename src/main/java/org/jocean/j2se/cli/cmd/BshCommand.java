package org.jocean.j2se.cli.cmd;

import java.io.StringReader;

import org.jocean.cli.CliCommand;
import org.jocean.cli.CliContext;
import org.jocean.idiom.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.google.common.io.BaseEncoding;

import bsh.Interpreter;

public class BshCommand implements CliCommand<CliContext> {

    private static final Logger LOG = LoggerFactory.getLogger(BshCommand.class);

    @Override
    public String execute(final CliContext ctx, final String... args) throws Exception {
        final Interpreter inter = new Interpreter();
//        inter.setOut(new PrintStream(outputStream));

        if (args.length < 1) {
            return "FAILED: missing script params\n" + getHelp();
        }

        try {
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
}

