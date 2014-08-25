/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.as.cli.command;

import org.jboss.aesh.cl.CommandDefinition;
import org.jboss.aesh.cl.Option;
import org.jboss.aesh.cl.validator.CommandValidator;
import org.jboss.aesh.cl.validator.CommandValidatorException;
import org.jboss.aesh.console.command.Command;
import org.jboss.aesh.console.command.CommandResult;
import org.jboss.as.cli.completer.ModuleNameCompleter;

import java.io.IOException;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
@CommandDefinition(name = "remove", description = "", validator = ModuleRemove.ModuleRemoveValidator.class)
public class ModuleRemove implements Command<CliCommandInvocation> {

    @Option(completer = ModuleNameCompleter.class, validator = ModuleAdd.ModuleNameValidator.class)
    private String name;

    @Option
    private String slot;

    @Override
    public CommandResult execute(CliCommandInvocation commandInvocation) throws IOException, InterruptedException {
        return null;
    }

     public class ModuleRemoveValidator implements CommandValidator<ModuleRemove> {
        @Override
        public void validate(ModuleRemove command) throws CommandValidatorException {
            if(command.name == null || command.name.length() == 0)
                throw new CommandValidatorException("Name must be set to remove a module");
        }
    }
}
