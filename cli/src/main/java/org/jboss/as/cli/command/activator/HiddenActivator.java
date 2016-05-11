/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jboss.as.cli.command.activator;

import org.jboss.aesh.cl.activation.OptionActivator;
import org.jboss.aesh.cl.internal.ProcessedCommand;

/**
 *
 * @author jfdenise
 */
public class HiddenActivator implements OptionActivator {

    private final OptionActivator activator;
    private final boolean hidden;

    public HiddenActivator() {
        this(true, null);
    }

    public HiddenActivator(boolean hidden, OptionActivator activator) {
        this.hidden = hidden;
        this.activator = activator;
    }

    @Override
    public boolean isActivated(ProcessedCommand processedCommand) {
        if (hidden) {
            return false;
        }
        return activator.isActivated(processedCommand);
    }

}
