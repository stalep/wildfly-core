/*
 *
 *  JBoss, Home of Professional Open Source.
 *  Copyright 2014, Red Hat, Inc., and individual contributors
 *  as indicated by the @author tags. See the copyright.txt file in the
 *  distribution for a full listing of individual contributors.
 *
 *  This is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU Lesser General Public License as
 *  published by the Free Software Foundation; either version 2.1 of
 *  the License, or (at your option) any later version.
 *
 *  This software is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this software; if not, write to the Free
 *  Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 *  02110-1301 USA, or see the FSF site: http://www.fsf.org.
 * /
 */

package org.wildfly.extension.requestcontroller;

import java.util.List;
import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.server.deployment.Phase;
import org.jboss.as.server.suspend.SuspendController;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;


/**
 * Handler responsible for adding the subsystem resource to the model
 *
 * @author Stuart Douglas
 */
class RequestControllerSubsystemAdd extends AbstractBoottimeAddStepHandler {

    private final RequestController requestController;

    RequestControllerSubsystemAdd(RequestController requestController) {
        this.requestController = requestController;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {

        for (AttributeDefinition attr : RequestControllerRootDefinition.ATTRIBUTES) {
            attr.validateAndSet(operation, model);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void performBoottime(OperationContext context, ModelNode operation, final ModelNode model,
                                ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers)
            throws OperationFailedException {

        context.addStep(new AbstractDeploymentChainStep() {
            @Override
            protected void execute(DeploymentProcessorTarget processorTarget) {

                processorTarget.addDeploymentProcessor(RequestControllerExtension.SUBSYSTEM_NAME, Phase.STRUCTURE, Phase.STRUCTURE_GLOBAL_REQUEST_CONTROLLER, new RequestControllerDeploymentUnitProcessor());
 }
        }, OperationContext.Stage.RUNTIME);

    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) throws OperationFailedException {
        super.performRuntime(context, operation, model, verificationHandler, newControllers);

        int maxRequests = RequestControllerRootDefinition.MAX_REQUESTS.resolveModelAttribute(context, model).asInt();
        requestController.setMaxRequestCount(maxRequests);

        context.getServiceTarget().addService(RequestController.SERVICE_NAME, requestController)
                .addDependency(SuspendController.SERVICE_NAME, SuspendController.class, requestController.getShutdownControllerInjectedValue())
                .install();


    }
}
