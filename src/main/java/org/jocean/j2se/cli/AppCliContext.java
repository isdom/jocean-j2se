package org.jocean.j2se.cli;

import org.jocean.cli.CliContext;

import rx.functions.Action1;

public interface AppCliContext extends CliContext {
    public CliController getCliController();
    public Action1<Object> logger();
}
