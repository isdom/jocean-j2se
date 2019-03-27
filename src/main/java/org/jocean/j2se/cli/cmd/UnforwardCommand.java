package org.jocean.j2se.cli.cmd;

import java.lang.management.ManagementFactory;
import java.util.Set;

import javax.management.JMX;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.jocean.cli.CliCommand;
import org.jocean.cli.CliContext;
import org.jocean.j2se.zk.ZKUpdaterMXBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UnforwardCommand implements CliCommand<CliContext> {

    private static final Logger LOG = LoggerFactory.getLogger(UnforwardCommand.class);

    @Override
    public String execute(final CliContext ctx, final String... args) throws Exception {
        final MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();

        final Set<ObjectName> names = mbeanServer.queryNames(ObjectName.getInstance("org.jocean:*,type=zkupdater"), null);

        if (names.size() > 0) {
            for (final ObjectName forwardObjectName : names) {
                LOG.info("try to invoke {}.removePaths.", forwardObjectName);

                final ZKUpdaterMXBean updaterMXBean = JMX.newMXBeanProxy(mbeanServer, forwardObjectName, ZKUpdaterMXBean.class);

                updaterMXBean.removePaths();
            }
        } else {
            return "failed: !NO! ANY matched zkupdater instance";
        }

        return "OK";
    }

    @Override
    public String getAction() {
        return "unfwd";
    }

    @Override
    public String getHelp() {
        return null;
    }
}

