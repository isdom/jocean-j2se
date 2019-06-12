package org.jocean.j2se.cli;

import java.io.OutputStream;
import java.io.Reader;

import org.jocean.cli.CliContext;

public interface AppCliContext extends CliContext {
    public CliController getCliController();
    public void enableSendbackLOG();
    public void disableSendbackLOG();
    public OutputStream outputStream();
    public Reader redirectInput();
}
