package org.jboss.as.cli.impl;

import org.jboss.as.cli.CommandSettings;

import java.io.InputStream;
import java.io.OutputStream;

public class CommandSettingsBuilder {
    private InputStream input;
    private OutputStream output;
    private boolean test;
    private String username;
    private String password;
    private String defaultController;
    private boolean disableLocalAuth;
    private boolean initConsole;
    private int connectionTimeout;

    public CommandSettingsBuilder() {
    }

    public CommandSettingsBuilder input(InputStream input) {
        this.input = input;
        return this;
    }

    public CommandSettingsBuilder output(OutputStream output) {
        this.output = output;
        return this;
    }

    public CommandSettingsBuilder test(boolean test) {
        this.test = test;
        return this;
    }

    public CommandSettingsBuilder username(String username) {
        this.username = username;
        return this;
    }

    public CommandSettingsBuilder password(String password) {
        this.password = password;
        return this;
    }

    public CommandSettingsBuilder defaultController(String defaultController) {
        this.defaultController = defaultController;
        return this;
    }

    public CommandSettingsBuilder disableLocalAuth(boolean disableLocalAuth) {
        this.disableLocalAuth = disableLocalAuth;
        return this;
    }

    public CommandSettingsBuilder initConsole(boolean initConsole) {
        this.initConsole = initConsole;
        return this;
    }

    public CommandSettingsBuilder connectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
        return this;
    }

    public CommandSettings create() {
        return new CommandSettingsImpl(input, output, username, password,
                defaultController, disableLocalAuth, initConsole, connectionTimeout, test);
    }
}
