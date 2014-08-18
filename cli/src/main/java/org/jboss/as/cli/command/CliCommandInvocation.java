/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.as.cli.command;

import org.jboss.aesh.console.AeshContext;
import org.jboss.aesh.console.Prompt;
import org.jboss.aesh.console.command.CommandOperation;
import org.jboss.aesh.console.command.invocation.CommandInvocation;
import org.jboss.aesh.console.command.registry.CommandRegistry;
import org.jboss.aesh.console.operator.ControlOperator;
import org.jboss.aesh.terminal.Shell;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.operation.OperationRequestAddress;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class CliCommandInvocation implements CommandInvocation {

    private final CommandInvocation commandInvocation;
    private final CommandContext ctx;

    public CliCommandInvocation(final CommandContext ctx, CommandInvocation commandInvocation) {
        this.ctx = ctx;
        this.commandInvocation = commandInvocation;
    }

    public final CommandContext getCommandContext() {
        return ctx;
    }

    @Override
    public ControlOperator getControlOperator() {
        return commandInvocation.getControlOperator();
    }

    @Override
    public CommandRegistry getCommandRegistry() {
        return commandInvocation.getCommandRegistry();
    }

    @Override
    public Shell getShell() {
        return commandInvocation.getShell();
    }

    @Override
    public void setPrompt(Prompt prompt) {
        commandInvocation.setPrompt(prompt);
    }

    @Override
    public Prompt getPrompt() {
        return commandInvocation.getPrompt();
    }

    @Override
    public String getHelpInfo(String s) {
        return commandInvocation.getHelpInfo(s);
    }

    @Override
    public void stop() {
        commandInvocation.stop();
    }

    @Override
    public AeshContext getAeshContext() {
        return commandInvocation.getAeshContext();
    }

    @Override
    public CommandOperation getInput() throws InterruptedException {
        return commandInvocation.getInput();
    }

    @Override
    public int getPid() {
        return commandInvocation.getPid();
    }

    @Override
    public void putProcessInBackground() {
        commandInvocation.putProcessInBackground();
    }

    @Override
    public void putProcessInForeground() {
        commandInvocation.putProcessInForeground();
    }

    @Override
    public void executeCommand(String input) {
        commandInvocation.executeCommand(input);
    }

    public void updatePrompt() {
        StringBuilder builder = new StringBuilder();

        builder.append('[');
        String controllerHost = ctx.getControllerHost();
        if(controllerHost != null) {
            if(ctx.isDomainMode())
                builder.append("domain@");
            else
                builder.append("standalone@");

            builder.append(controllerHost).append(':').append(ctx.getControllerPort()).append(' ');
        }
        else
            builder.append("disconnected ");

        OperationRequestAddress prefix = ctx.getCurrentNodePath();
        if(prefix == null || prefix.isEmpty())
            builder.append('/');
        else {
            builder.append(prefix.getNodeType());
            final String nodeName = prefix.getNodeName();
            if (nodeName != null)
                builder.append('=').append(nodeName);
        }

        if(ctx.isBatchMode())
            builder.append(" #");
        builder.append("] ");

        commandInvocation.setPrompt(new Prompt(builder.toString()));
    }

}
