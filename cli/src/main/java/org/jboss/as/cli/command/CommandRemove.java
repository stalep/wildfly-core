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
package org.jboss.as.cli.command;

import org.jboss.aesh.cl.CommandDefinition;
import org.jboss.aesh.console.command.Command;
import org.jboss.aesh.console.command.CommandResult;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.jboss.aesh.cl.Arguments;
import org.jboss.aesh.cl.completer.OptionCompleter;
import org.jboss.aesh.console.command.CommandNotFoundException;
import org.jboss.aesh.console.command.container.CommandContainer;
import org.jboss.aesh.console.command.registry.MutableCommandRegistry;
import org.jboss.as.cli.command.generic.MainCommandParser;
import org.jboss.as.cli.provider.CliCompleterInvocation;

/**
 * Remove added command
 *
 * @author jdenise@redhat.com
 */
@CommandDefinition(name = "remove", description = "")
public class CommandRemove implements Command<CliCommandInvocation> {

    @Arguments(completer = DynamicCommandCompleter.class)
    private List<String> commandName;

    @Override
    public CommandResult execute(CliCommandInvocation commandInvocation) throws IOException, InterruptedException {
        MutableCommandRegistry reg = (MutableCommandRegistry) commandInvocation.getCommandRegistry();
        // Should be handled at the aesh level.
        if (commandName == null || commandName.isEmpty()) {
            return null;
        }
        try {
            CommandContainer container = reg.getCommand(commandName.get(0), null);
            if (container != null && container.getParser() instanceof MainCommandParser) {
                if (((MainCommandParser) container.getParser()).isRemovable()) {
                    reg.removeCommand(commandName.get(0));
                } else {
                    throw new RuntimeException(commandName + " can't be removed");
                }
            } else {
                throw new RuntimeException("Invalid command " + commandName);
            }
        } catch (CommandNotFoundException ex) {
            throw new RuntimeException(ex);
        }
        return null;
    }

    public class DynamicCommandCompleter implements OptionCompleter<CliCompleterInvocation> {

        @Override
        public void complete(CliCompleterInvocation cliCompleterInvocation) {
            MutableCommandRegistry reg
                    = (MutableCommandRegistry) cliCompleterInvocation.getCommandRegistry();
            List<String> candidates = new ArrayList<>();
            int pos = 0;
            if (cliCompleterInvocation.getGivenCompleteValue() != null) {
                pos = cliCompleterInvocation.getGivenCompleteValue().length();
            }
            for (String name : reg.getAllCommandNames()) {
                CommandContainer container;
                try {
                    container = reg.getCommand(name, null);
                } catch (CommandNotFoundException ex) {
                    continue;
                }
                if (container != null && container.getParser() instanceof MainCommandParser) {
                    if (((MainCommandParser) container.getParser()).isRemovable()) {
                        candidates.add(name);
                    }
                }
            }
            cliCompleterInvocation.addAllCompleterValues(candidates);
            cliCompleterInvocation.setAppendSpace(false);
        }
    }

}
