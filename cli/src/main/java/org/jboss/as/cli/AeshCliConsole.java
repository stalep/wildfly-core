/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.as.cli;

import org.jboss.aesh.console.AeshConsole;
import org.jboss.aesh.console.AeshConsoleBuilder;
import org.jboss.aesh.console.Prompt;
import org.jboss.aesh.console.command.invocation.CommandInvocationServices;
import org.jboss.aesh.console.command.registry.AeshCommandRegistryBuilder;
import org.jboss.aesh.console.command.registry.CommandRegistry;
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

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import org.jboss.aesh.cl.parser.CommandLineParserException;
import org.jboss.aesh.console.command.container.AeshCommandContainer;
import org.jboss.aesh.console.command.registry.MutableCommandRegistry;
import org.jboss.as.cli.command.CommandCommand;
import org.jboss.as.cli.command.generic.MainCommandParser;
import org.jboss.as.cli.command.generic.NodeType;
import org.jboss.as.cli.completer.RolloutPlanCompleter;
import org.jboss.as.cli.converter.HeadersConverter;
import org.jboss.as.cli.handlers.jca.JDBCDriverNameProvider;
import org.jboss.as.cli.impl.DefaultCompleter;

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
                .logging(true)
                .readInputrc(false)
                .enableMan(true);

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
        MutableCommandRegistry mutableReg = (MutableCommandRegistry) commandRegistry;
        try {
            // Add some Generic commands
            MainCommandParser dataSourceParser = new MainCommandParser("data-source",
                    new NodeType("/subsystem=datasources/data-source"),
                    null,
                    commandContext,
                    false);
            final DefaultCompleter driverNameCompleter = new DefaultCompleter(JDBCDriverNameProvider.INSTANCE);
            dataSourceParser.addCustomCompleter(Util.DRIVER_NAME,
                    new org.jboss.as.cli.completer.DefaultCompleter(driverNameCompleter));
            // XXX JFDENISE TODO
            //dataSourceParser.addCustomSubCommand(new DataSourceAddCompositeSubCommand(Util.ADD,
            //        new NodeType("/subsystem=datasources/data-source"), null));
            mutableReg.addCommand(new AeshCommandContainer(dataSourceParser));

            MainCommandParser xdataSourceParser = new MainCommandParser("xa-data-source",
                    new NodeType("/subsystem=datasources/xa-data-source"),
                    null,
                    commandContext,
                    false);
            xdataSourceParser.addCustomCompleter(Util.DRIVER_NAME,
                    new org.jboss.as.cli.completer.DefaultCompleter(driverNameCompleter));
            // XXX JFDENISE TODO
            //dataSourceParser.addCustomSubCommand(new XADataSourceAddCompositeSubCommand(Util.ADD,
            //        new NodeType("/subsystem=datasources/data-source"), null));
            mutableReg.addCommand(new AeshCommandContainer(xdataSourceParser));

            MainCommandParser rolloutParser = new MainCommandParser("rollout-plan",
                    new NodeType("/management-client-content=rollout-plans/rollout-plan"),
                    null,
                    commandContext,
                    false);
            rolloutParser.addCustomConverter("content", HeadersConverter.INSTANCE);
            rolloutParser.addCustomCompleter("content", RolloutPlanCompleter.INSTANCE);
            mutableReg.addCommand(new AeshCommandContainer(rolloutParser));

        } catch (CommandLineParserException ex) {
            throw new RuntimeException(ex);
        }

        CliOptionActivatorProvider activatorProvider = new CliOptionActivatorProvider(commandContext);

        console = new AeshConsoleBuilder()
                .commandRegistry(commandRegistry)
                .settings(settings)
                .commandInvocationProvider(services)
                .completerInvocationProvider(new CliCompleterInvocationProvider(commandContext, commandRegistry))
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
                .command(CommandCommand.class)
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
