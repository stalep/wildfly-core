/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.server.operations;

import static org.jboss.as.server.mgmt.HttpManagementResourceDefinition.INTERFACE;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.server.logging.ServerLogger;
import org.jboss.as.server.mgmt.HttpManagementResourceDefinition;
import org.jboss.dmr.ModelNode;

/**
 * {@code OperationStepHandler} for changing attributes on the http management interface.
 *
 * @author Emanuel Muckenhuber
 */
public class HttpManagementWriteAttributeHandler extends ReloadRequiredWriteAttributeHandler{

    public static final OperationStepHandler INSTANCE = new HttpManagementWriteAttributeHandler();

    private HttpManagementWriteAttributeHandler() {
        super(HttpManagementResourceDefinition.ATTRIBUTE_DEFINITIONS);
    }

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        // Add a Stage.MODEL handler to validate that when this step and all other steps that may be part of
        // a containing composite operation are done, the model doesn't violate the requirement that "interface"
        // is an alternative to "socket-binding"
        context.addStep(new OperationStepHandler() {
            @Override
            public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                final ModelNode model = context.readResource(PathAddress.EMPTY_ADDRESS).getModel();
                if (model.hasDefined(INTERFACE.getName())
                        && (model.hasDefined(HttpManagementResourceDefinition.SOCKET_BINDING.getName())
                        || model.hasDefined(HttpManagementResourceDefinition.SECURE_SOCKET_BINDING.getName())
                )) {
                    throw ServerLogger.ROOT_LOGGER.illegalCombinationOfHttpManagementInterfaceConfigurations(
                            INTERFACE.getName(),
                            HttpManagementResourceDefinition.SOCKET_BINDING.getName(),
                            HttpManagementResourceDefinition.SECURE_SOCKET_BINDING.getName());
                }
                context.stepCompleted();
            }
        }, OperationContext.Stage.MODEL);

        super.execute(context, operation);
    }

}