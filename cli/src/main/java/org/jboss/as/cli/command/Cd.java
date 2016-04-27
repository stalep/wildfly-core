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

import org.jboss.aesh.cl.Arguments;
import org.jboss.aesh.cl.CommandDefinition;
import org.jboss.aesh.cl.Option;
import org.jboss.aesh.console.command.Command;
import org.jboss.aesh.console.command.CommandResult;
import org.jboss.as.cli.completer.PathOptionCompleter;
import org.jboss.as.cli.converter.OperationRequestAddressConverter;
import org.jboss.as.cli.operation.OperationRequestAddress;
import org.jboss.as.cli.validator.ChangeNodeValidator;

import java.io.IOException;
import java.util.List;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
@CommandDefinition(name = "cd", description = " [node_path]%n Changes the current node path to the argument.")
public class Cd implements Command<CliCommandInvocation> {

    @Arguments(completer = PathOptionCompleter.class,
            converter = OperationRequestAddressConverter.class,
            validator = ChangeNodeValidator.class)
    private List<OperationRequestAddress> arguments;

    @Option(hasValue = false)
    private boolean help;

    @Override
    public CommandResult execute(CliCommandInvocation cliCommandInvocation) throws IOException {
        if(help) {
            cliCommandInvocation.getShell().out().print(cliCommandInvocation.getHelpInfo("cd"));
        }
        else {
            if(arguments != null &&arguments.size() > 0) {
                cliCommandInvocation.getCommandContext().setCurrentNodePath(arguments.get(0));

                cliCommandInvocation.updatePrompt();
            }
        }
        return CommandResult.SUCCESS;
    }

}
