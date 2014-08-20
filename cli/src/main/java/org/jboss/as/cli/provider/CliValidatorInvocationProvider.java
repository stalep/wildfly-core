/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.as.cli.provider;

import org.jboss.aesh.console.command.validator.ValidatorInvocation;
import org.jboss.aesh.console.command.validator.ValidatorInvocationProvider;
import org.jboss.as.cli.CommandContext;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class CliValidatorInvocationProvider implements ValidatorInvocationProvider<CliValidatorInvocation> {

    private final CommandContext context;

    public CliValidatorInvocationProvider(CommandContext context) {
        this.context = context;
    }

    @Override
    public CliValidatorInvocation enhanceValidatorInvocation(ValidatorInvocation validatorInvocation) {
        return new CliValidatorInvocationImpl(context, validatorInvocation.getValue(),
                validatorInvocation.getAeshContext(), validatorInvocation.getCommand());
    }
}
