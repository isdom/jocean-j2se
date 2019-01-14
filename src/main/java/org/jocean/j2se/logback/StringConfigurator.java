package org.jocean.j2se.logback;

import java.io.ByteArrayInputStream;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import com.google.common.base.Charsets;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.util.StatusPrinter;

public class StringConfigurator {

    public void loadConfig() throws Exception {
        final LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();

        final JoranConfigurator configurator = new JoranConfigurator();
        configurator.setContext(lc);
        lc.reset();
        configurator.doConfigure(new ByteArrayInputStream(this._xmlAsString.getBytes(Charsets.UTF_8)));
        StatusPrinter.printInCaseOfErrorsOrWarnings(lc);
    }

    @Value("${logback.xml}")
    String _xmlAsString;
}
