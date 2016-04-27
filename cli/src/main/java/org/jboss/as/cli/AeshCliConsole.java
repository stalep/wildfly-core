/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.cli;

import org.jboss.aesh.console.AeshConsole;
import org.jboss.aesh.console.AeshConsoleBuilder;
import org.jboss.aesh.console.Prompt;
import org.jboss.aesh.console.command.invocation.CommandInvocationServices;
import org.jboss.aesh.console.command.registry.AeshCommandRegistryBuilder;
import org.jboss.aesh.console.command.registry.CommandRegistry;
import org.jboss.aesh.console.settings.FileAccessPermission;
import org.jboss.aesh.console.settings.Settings;
import org.jboss.aesh.console.settings.SettingsBuilder;
import org.jboss.as.cli.command.Cd;
import org.jboss.as.cli.command.Clear;
import org.jboss.as.cli.command.Connect;
import org.jboss.as.cli.command.Exit;
import org.jboss.as.cli.command.Logout;
import org.jboss.as.cli.command.Ls;
import org.jboss.as.cli.command.Module;
import org.jboss.as.cli.command.Quit;
import org.jboss.as.cli.provider.CliCommandInvocationProvider;
import org.jboss.as.cli.provider.CliCompleterInvocationProvider;
import org.jboss.as.cli.provider.CliConverterInvocationProvider;
import org.jboss.as.cli.provider.CliManProvider;
import org.jboss.as.cli.provider.CliOptionActivatorProvider;
import org.jboss.as.cli.provider.CliValidatorInvocationProvider;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class AeshCliConsole {

    private AeshConsole console;
    private CommandContext commandContext;
    private CommandRegistry commandRegistry;
    private static final String PROVIDER = "JBOSS_CLI";

    public AeshCliConsole(CommandContext commandContext) {
        this.commandContext = commandContext;

        setupConsole(new SettingsBuilder().create());
    }

    public void startConsole() {
        if(console != null)
            console.start();
    }

    public AeshCliConsole(CommandContext commandContext,
                          InputStream consoleInput, OutputStream consoleOutput) {
        this.commandContext = commandContext;


        SettingsBuilder settingsBuilder = new SettingsBuilder();
        if(consoleInput != null)
            settingsBuilder.inputStream(consoleInput);
        if(consoleOutput != null)
            settingsBuilder.outputStream(new PrintStream(consoleOutput));

        settingsBuilder
                .enableExport(false)
                .disableHistory(!commandContext.getCliConfig().isHistoryEnabled())
                .historyFile(new File(commandContext.getCliConfig().getHistoryFileDir(),
                        commandContext.getCliConfig().getHistoryFileName()))
                .logging(true)
                .readInputrc(false)
                .enableMan(true);
        // Modify Default History File Permissions
        FileAccessPermission permissions = new FileAccessPermission();
        permissions.setReadableOwnerOnly(true);
        permissions.setWritableOwnerOnly(true);
        settingsBuilder.historyFilePermission(permissions);

        //TODO: need to fix this
        settingsBuilder.interruptHook(
                (console1, action) -> commandContext.terminateSession());

        setupConsole(settingsBuilder.create());
    }

    public AeshCliConsole(CommandContext commandContext, Settings settings) {
        this.commandContext = commandContext;
        setupConsole(settings);
    }

    private void setupConsole(Settings settings) {

        CommandInvocationServices services = new CommandInvocationServices();
        services.registerProvider(PROVIDER, new CliCommandInvocationProvider(commandContext));

        commandRegistry = createCommandRegistry();

        CliOptionActivatorProvider activatorProvider = new CliOptionActivatorProvider(commandContext);

        console = new AeshConsoleBuilder()
                .commandRegistry(commandRegistry)
                .settings(settings)
                .commandInvocationProvider(services)
                .completerInvocationProvider(new CliCompleterInvocationProvider(commandContext))
                .commandNotFoundHandler(new CliCommandNotFound())
                .converterInvocationProvider(new CliConverterInvocationProvider(commandContext))
                .optionActivatorProvider(activatorProvider)
                .validatorInvocationProvider(new CliValidatorInvocationProvider(commandContext))
                .manProvider(new CliManProvider())
                .prompt(new Prompt("[disconnected /] "))
                .create();

        console.setCurrentCommandInvocationProvider(PROVIDER);

    }

    private CommandRegistry createCommandRegistry() {
        return new AeshCommandRegistryBuilder()
                .command(Quit.class)
                .command(Exit.class)
                .command(Ls.class)
                .command(Connect.class)
                .command(Cd.class)
                .command(Clear.class)
                .command(Logout.class)
                .command(Module.class)
                .create();
    }
}
