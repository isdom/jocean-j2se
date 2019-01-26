/**
 *
 */
package org.jocean.j2se.booter;

import java.util.concurrent.atomic.AtomicReference;

import org.jocean.j2se.cli.CliController;
import org.jocean.j2se.cli.cmd.ExitSrvCommand;
import org.jocean.j2se.cli.cmd.StartAppCommand;
import org.jocean.j2se.cli.cmd.StopAppCommand;
import org.springframework.context.ConfigurableApplicationContext;


/**
 * @author isdom
 *
 */
public class CliMain {
    /**
     * @param args
     * @param extJars
     * @throws Exception
     */
    public static void main(final String[] args, final String[] libs) throws Exception {
        final CliController cli = new CliController(System.getProperty("user.dir", "~") + "/");
        final AtomicReference<ConfigurableApplicationContext> ref = new AtomicReference<>();
        cli.cmdsRepo().addCommand(new StartAppCommand(ref, libs))
            .addCommand(new StopAppCommand(ref))
            .addCommand(new ExitSrvCommand(cli))
            ;
        cli.start();

        cli.await();
    }
}
