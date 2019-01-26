package org.jocean.j2se.cli.cmd;

import java.lang.management.ManagementFactory;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import javax.management.JMX;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.jocean.cli.CliCommand;
import org.jocean.cli.CliContext;
import org.jocean.http.server.mbean.HttpServerMXBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TradeCountCommand implements CliCommand<CliContext> {

    private static final Logger LOG = LoggerFactory.getLogger(TradeCountCommand.class);

    @Override
    public String execute(final CliContext ctx, final String... args) throws Exception {
        final MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();

        final Set<ObjectName> names = mbeanServer.queryNames(ObjectName.getInstance("org.jocean:*,name=hsb"), null);
        final AtomicInteger cnt = new AtomicInteger(0);

        for (final ObjectName hsbname : names) {
            LOG.info("try to count {}.NumActiveTrades.", hsbname);
            final HttpServerMXBean hsm = JMX.newMXBeanProxy(mbeanServer, hsbname, HttpServerMXBean.class);
            cnt.addAndGet(hsm.getNumActiveTrades());
        }

        return Integer.toString(cnt.get());
    }

    @Override
    public String getAction() {
        return "tradecnt";
    }

    @Override
    public String getHelp() {
        return null;
    }
}

