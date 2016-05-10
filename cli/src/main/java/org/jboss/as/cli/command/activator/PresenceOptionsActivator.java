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
package org.jboss.as.cli.command.activator;

import java.util.Set;
import org.jboss.aesh.cl.activation.OptionActivator;
import org.jboss.aesh.cl.internal.ProcessedCommand;

/**
 * Search for a set of options, activate the option if all expected options are
 * found and all not expected are not found.
 *
 * @author jdenise@redhat.com
 */
public class PresenceOptionsActivator implements OptionActivator {

    private final ExpectedOptionsActivator exist;
    private final NotExpectedOptionsActivator notExist;

    public PresenceOptionsActivator(Set<String> expected, Set<String> notExpected) {
        this.exist = new ExpectedOptionsActivator(expected);
        this.notExist = new NotExpectedOptionsActivator(notExpected);
    }

    @Override
    public boolean isActivated(ProcessedCommand processedCommand) {
        return exist.isActivated(processedCommand)
                && notExist.isActivated(processedCommand);
    }
}
