package org.jboss.as.cli;

import java.io.InputStream;
import java.io.OutputStream;

public interface CommandSettings {


    InputStream getInputStream();

    OutputStream getOutputStream();

    boolean isTest();

    String getUsername();

    String getPassword();

    String getDefaultController();

    boolean isLocalAuthDisabled();

    boolean doInitConsole();

    int getConnectionTimeout();
}
