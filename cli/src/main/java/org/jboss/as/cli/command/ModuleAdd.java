/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.as.cli.command;

import org.jboss.aesh.cl.CommandDefinition;
import org.jboss.aesh.cl.Option;
import org.jboss.aesh.cl.activation.OptionActivator;
import org.jboss.aesh.cl.internal.ProcessedCommand;
import org.jboss.aesh.cl.internal.ProcessedOption;
import org.jboss.aesh.cl.validator.OptionValidator;
import org.jboss.aesh.cl.validator.OptionValidatorException;
import org.jboss.aesh.console.command.Command;
import org.jboss.aesh.console.command.CommandResult;
import org.jboss.aesh.io.Resource;
import org.jboss.as.cli.completer.ModuleNameCompleter;
import org.jboss.as.cli.provider.CliValidatorInvocationImpl;
import org.wildfly.security.manager.WildFlySecurityManager;

import java.io.File;
import java.io.IOException;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
@CommandDefinition(name = "add", description = "")
public class ModuleAdd implements Command<CliCommandInvocation> {

    private static final String JBOSS_HOME = "JBOSS_HOME";

    @Option(completer = ModuleNameCompleter.class, validator = ModuleNameValidator.class)
    private String name;

    @Option(activator = NameActivator.class)
    private String dependencies;

    @Option(name = "main-class", activator = NameActivator.class)
    private String mainClass;

    @Option(name = "module-xml")
    private String moduleXml;

    @Option(activator = NameActivator.class)
    private String properties;

    @Option(activator = NameActivator.class)
    private Resource resources;

    @Option
    private String slot;

    @Override
    public CommandResult execute(CliCommandInvocation commandInvocation) throws IOException, InterruptedException {
        return null;
    }

    public String getName() {
        return name;
    }

    public class ModuleNameValidator implements OptionValidator<CliValidatorInvocationImpl> {

        @Override
        public void validate(CliValidatorInvocationImpl validatorInvocation) throws OptionValidatorException {
            final String modulesDirStr = WildFlySecurityManager.getEnvPropertyPrivileged(JBOSS_HOME, null);
            if( modulesDirStr == null || !new File(modulesDirStr, "modules").exists())
                throw new OptionValidatorException("JBOSS_HOME must be set");
        }
    }

    public class NameActivator implements OptionActivator {

        @Override
        public boolean isActivated(ProcessedCommand processedCommand) {
            ProcessedOption processedOption = processedCommand.findLongOption("name");
            return processedOption != null && processedOption.getValue() != null;
        }
    }
}
