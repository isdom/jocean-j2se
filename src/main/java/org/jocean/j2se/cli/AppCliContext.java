package org.jocean.j2se.cli;

import org.jocean.cli.CliContext;

public interface AppCliContext extends CliContext {
    public CliController getCliController();
}
