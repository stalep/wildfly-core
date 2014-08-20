/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
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
