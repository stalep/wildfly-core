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
import org.jboss.aesh.cl.Option;
import org.jboss.aesh.cl.activation.OptionActivator;
import org.jboss.aesh.cl.internal.ProcessedCommand;
import org.jboss.aesh.cl.internal.ProcessedOption;
import org.jboss.aesh.console.command.Command;
import org.jboss.aesh.console.command.CommandResult;
import java.io.IOException;
import org.jboss.aesh.cl.parser.CommandLineParserException;
import org.jboss.aesh.console.command.container.AeshCommandContainer;
import org.jboss.aesh.console.command.registry.MutableCommandRegistry;
import org.jboss.as.cli.command.generic.MainCommandParser;
import org.jboss.as.cli.command.generic.NodeType;
import org.jboss.as.cli.completer.PathOptionCompleter;
import org.jboss.as.cli.completer.PropertyCompleter;

/**
 * A Command to add new commands.
 *
 * @author jdenise@redhat.com
 */
@CommandDefinition(name = "add", description = "")
public class CommandAdd implements Command<CliCommandInvocation> {

    @Option(name = "node-type", completer = PathOptionCompleter.class)
    private String nodeType;

    @Option(name = "command-name", activator = NodeTypeActivator.class)
    private String commandName;

    @Option(name = "property-id", activator = NodeTypeActivator.class,
            completer = PropertyCompleter.class)
    private String propertyId;

    @Override
    public CommandResult execute(CliCommandInvocation commandInvocation)
            throws IOException, InterruptedException {
        MutableCommandRegistry reg
                = (MutableCommandRegistry) commandInvocation.getCommandRegistry();

        try {
            reg.addCommand(new AeshCommandContainer(new MainCommandParser(commandName,
                    new NodeType(nodeType),
                    propertyId,
                    commandInvocation.getCommandContext(),
                    true)));
        } catch (CommandLineParserException ex) {
            throw new RuntimeException(ex);
        }
        return null;
    }

    public class NodeTypeActivator implements OptionActivator {

        @Override
        public boolean isActivated(ProcessedCommand processedCommand) {
            ProcessedOption processedOption = processedCommand.findLongOption("node-type");
            return processedOption != null && processedOption.getValue() != null;
        }
    }

}
