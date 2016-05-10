/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jboss.as.cli.command.generic.custom;

import org.jboss.aesh.cl.internal.ProcessedCommand;
import org.jboss.aesh.cl.parser.CommandLineParserException;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.command.generic.AbstractOperationSubCommand;
import org.jboss.as.cli.command.generic.NodeType;

/**
 *
 * @author jfdenise
 */
public class DataSourceAddCompositeSubCommand extends AbstractOperationSubCommand {

    public DataSourceAddCompositeSubCommand(String operationName, NodeType nodeType, String propertyId) {
        super(operationName, nodeType, propertyId);
    }

    @Override
    public ProcessedCommand getProcessedCommand(CommandContext commandContext)
            throws CommandLineParserException {
        // XXX JFDENISE TODO
        return null;
    }

}
