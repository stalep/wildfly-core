package org.jboss.as.cli.impl;

import org.jboss.as.cli.CommandSettings;

import java.io.InputStream;
import java.io.OutputStream;

public class CommandSettingsImpl implements CommandSettings {

    private InputStream input;
    private OutputStream output;
    private boolean test;
    private String username;
    private String password;
    private String defaultController;
    private boolean disableLocalAuth;
    private boolean initConsole;

    public CommandSettingsImpl(InputStream input, OutputStream output, String username, String password, String defaultController, boolean disableLocalAuth, boolean initConsole, int connectionTimeout, boolean test) {
        this.input = input;
        this.output = output;
        this.username = username;
        this.password = password;
        this.defaultController = defaultController;
        this.disableLocalAuth = disableLocalAuth;
        this.initConsole = initConsole;
        this.test = test;
    }

    @Override
    public InputStream getInputStream() {
        return input;
    }

    @Override
    public OutputStream getOutputStream() {
        return output;
    }

    @Override
    public boolean isTest() {
        return test;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getDefaultController() {
        return defaultController;
    }

    @Override
    public boolean isLocalAuthDisabled() {
        return disableLocalAuth;
    }

    @Override
    public boolean doInitConsole() {
        return initConsole;
    }

    @Override
    public int getConnectionTimeout() {
        return 0;
    }

    public void setInput(InputStream input) {
        this.input = input;
    }

    public void setOutput(OutputStream output) {
        this.output = output;
    }

    public void setTest(boolean test) {
        this.test = test;
    }
}
