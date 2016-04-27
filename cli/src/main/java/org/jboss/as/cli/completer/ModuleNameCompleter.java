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
package org.jboss.as.cli.completer;

import org.jboss.aesh.cl.completer.OptionCompleter;
import org.jboss.aesh.complete.CompleteOperation;
import org.jboss.aesh.io.filter.AllResourceFilter;
import org.jboss.aesh.io.filter.ResourceFilter;
import org.jboss.aesh.terminal.TerminalString;
import org.jboss.aesh.util.FileLister;
import org.jboss.as.cli.provider.CliCompleterInvocation;
import org.wildfly.security.manager.WildFlySecurityManager;

import java.io.File;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class ModuleNameCompleter implements OptionCompleter<CliCompleterInvocation> {

    private File modulesDir;

    private static final String JBOSS_HOME = "JBOSS_HOME";

    private final ResourceFilter filter = new AllResourceFilter();

    public void complete(CliCompleterInvocation completerInvocation) {

        CompleteOperation completeOperation =
                new CompleteOperation(completerInvocation.getAeshContext(),
                        completerInvocation.getGivenCompleteValue().replace('.', File.separatorChar), 0);
        if (completerInvocation.getGivenCompleteValue() == null)
            new FileLister("",
                    completerInvocation.getAeshContext().getCurrentWorkingDirectory().newInstance(
                      getModulesDir().getAbsolutePath() + File.separatorChar), filter)
                    .findMatchingDirectories(completeOperation);
        else
            new FileLister(completerInvocation.getGivenCompleteValue().replace('.', File.separatorChar),
                    completerInvocation.getAeshContext().getCurrentWorkingDirectory().newInstance(
                            getModulesDir().getAbsolutePath() + File.separatorChar), filter)
                    .findMatchingDirectories(completeOperation);

        if (completeOperation.getCompletionCandidates().size() > 1) {
            completeOperation.removeEscapedSpacesFromCompletionCandidates();
        }

        for(TerminalString completion : completeOperation.getCompletionCandidates()) {
            completion.setCharacters( completion.getCharacters().replace(File.separatorChar, '.'));
            completerInvocation.addCompleterValueTerminalString(completion);
        }

        //completerInvocation.setCompleterValuesTerminalString(completeOperation.getCompletionCandidates());
        if (completerInvocation.getGivenCompleteValue() != null && completerInvocation.getCompleterValues().size() == 1) {
            completerInvocation.setAppendSpace(completeOperation.hasAppendSeparator());
        }

        if(completeOperation.doIgnoreOffset())
            completerInvocation.setIgnoreOffset(completeOperation.doIgnoreOffset());

        completerInvocation.setIgnoreStartsWith(true);

    }

    protected File getModulesDir() {
        if(modulesDir != null) {
            return modulesDir;
        }
        final String modulesDirStr = WildFlySecurityManager.getEnvPropertyPrivileged(JBOSS_HOME, null);
        if(modulesDirStr == null) {
            return new File("");
        }
        modulesDir = new File(modulesDirStr, "modules");
        if(!modulesDir.exists()) {
            return new File("");
        }
        return modulesDir;
    }

}
