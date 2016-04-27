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
package org.jboss.as.cli.command;

import org.jboss.aesh.cl.CommandDefinition;
import org.jboss.aesh.cl.Option;
import org.jboss.aesh.cl.OptionGroup;
import org.jboss.aesh.cl.OptionList;
import org.jboss.aesh.cl.activation.OptionActivator;
import org.jboss.aesh.cl.internal.ProcessedCommand;
import org.jboss.aesh.cl.internal.ProcessedOption;
import org.jboss.aesh.cl.validator.CommandValidator;
import org.jboss.aesh.cl.validator.CommandValidatorException;
import org.jboss.aesh.cl.validator.OptionValidator;
import org.jboss.aesh.cl.validator.OptionValidatorException;
import org.jboss.aesh.console.Config;
import org.jboss.aesh.console.command.Command;
import org.jboss.aesh.console.command.CommandResult;
import org.jboss.aesh.io.Resource;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandLineException;
import org.jboss.as.cli.completer.ModuleNameCompleter;
import org.jboss.as.cli.handlers.module.ModuleConfigImpl;
import org.jboss.as.cli.handlers.module.ModuleDependency;
import org.jboss.as.cli.handlers.module.ResourceRoot;
import org.jboss.as.cli.provider.CliValidatorInvocationImpl;
import org.jboss.staxmapper.XMLExtendedStreamWriter;
import org.wildfly.security.manager.WildFlySecurityManager;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.List;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
@CommandDefinition(name = "add", description = "", validator = ModuleAdd.ModuleAddValidator.class)
public class ModuleAdd implements Command<CliCommandInvocation> {

    private static final String JBOSS_HOME = "JBOSS_HOME";
    private File modulesDir;

    @Option(completer = ModuleNameCompleter.class, validator = ModuleNameValidator.class)
    private String name;

    @OptionList(activator = NameActivator.class, completer = ModuleNameCompleter.class)
    private List<String> dependencies;

    @Option(name = "main-class", activator = NameActivator.class)
    private String mainClass;

    @Option(name = "module-xml", activator = NameActivator.class)
    private Resource moduleXml;

    @OptionGroup(shortName = 'P', activator = NameActivator.class)
    private HashMap<String,String> properties;

    @OptionList(activator = NameActivator.class)
    private List<Resource> resources;

    @Option
    private String slot;

    @Override
    public CommandResult execute(CliCommandInvocation commandInvocation) throws IOException, InterruptedException {
        return null;
    }

    public String getName() {
        return name;
    }


    protected void addModule(CommandContext ctx ) throws CommandLineException {

        // resources required only if we are generating module.xml
        File[] resourceFiles;

        if(resources != null) {
            resourceFiles = new File[resources.size()];
            for (int i = 0; i < resources.size(); ++i) {
                final File f = new File(resources.get(i).getAbsolutePath());
                if (!f.exists()) {
                    throw new CommandLineException("Failed to locate " + f.getAbsolutePath());
                }
                resourceFiles[i] = f;
            }
        }

        final File moduleDir = getModulePath(getModulesDir(), name, slot);
        if(moduleDir.exists()) {
            throw new CommandLineException("Module " + name + " already exists at " + moduleDir.getAbsolutePath());
        }

        if(!moduleDir.mkdirs()) {
            throw new CommandLineException("Failed to create directory " + moduleDir.getAbsolutePath());
        }

        final ModuleConfigImpl config;
        if(moduleXml != null) {
            config = null;
           // final File source = new File(moduleXml.getAbsolutePath());
            if(!moduleXml.exists()) {
                throw new CommandLineException("Failed to locate the file on the filesystem: " + moduleXml.getAbsolutePath());
            }
            try {
                moduleXml.copy(moduleXml.newInstance(moduleDir.getAbsolutePath() + Config.getPathSeparator() + "module.xml"));
            }
            catch(IOException ioe) {
                ioe.printStackTrace();
            }
        }
        else {
            config = new ModuleConfigImpl(name);
        }

        for(Resource f : resources) {
            try {
                f.copy(f.newInstance(moduleDir + Config.getPathSeparator() + f.getName()));
            }
            catch(IOException ioe) {
                ioe.printStackTrace();
            }
            if(config != null) {
                config.addResource(new ResourceRoot(f.getName()));
            }
        }

        if(config != null) {

            for(String dep : dependencies) {
                // TODO validate dependencies
                config.addDependency(new ModuleDependency(dep));
            }

            for(String name : properties.keySet()) {
                config.setProperty(name, properties.get(name));
            }

            if(slot != null)
                config.setSlot(slot);

            if(mainClass != null) {
                config.setMainClass(mainClass);
            }

            FileWriter moduleWriter = null;
            final File moduleFile = new File(moduleDir, "module.xml");
            try {
                moduleWriter = new FileWriter(moduleFile);
                XMLExtendedStreamWriter xmlWriter = create(XMLOutputFactory.newInstance().createXMLStreamWriter(moduleWriter));
                config.writeContent(xmlWriter, null);
                xmlWriter.flush();
            } catch (IOException e) {
                throw new CommandLineException("Failed to create file " + moduleFile.getAbsolutePath(), e);
            } catch (XMLStreamException e) {
                throw new CommandLineException("Failed to write to " + moduleFile.getAbsolutePath(), e);
            } finally {
                if(moduleWriter != null) {
                    try {
                        moduleWriter.close();
                    } catch (IOException e) {}
                }
            }
        }
    }

    public static XMLExtendedStreamWriter create(XMLStreamWriter writer) throws CommandLineException {
        try {
            // Use reflection to access package protected class FormattingXMLStreamWriter
            // TODO: at some point the staxmapper API could be enhanced to make this unnecessary
            Class<?> clazz = Class.forName("org.jboss.staxmapper.FormattingXMLStreamWriter");
            Constructor<?> ctr = clazz.getConstructor( XMLStreamWriter.class );
            ctr.setAccessible(true);
            return (XMLExtendedStreamWriter)ctr.newInstance(new Object[]{writer});
        } catch (Exception e) {
            throw new CommandLineException("Failed to create xml stream writer.", e);
        }
    }

    protected File getModulePath(File modulesDir, final String moduleName, String slot) throws CommandLineException {
        return new File(modulesDir, moduleName.replace('.', File.separatorChar) + File.separatorChar + (slot == null ? "main" : slot));
    }

    protected File getModulesDir() throws CommandLineException {
        if(modulesDir != null) {
            return modulesDir;
        }
        final String modulesDirStr = WildFlySecurityManager.getEnvPropertyPrivileged(JBOSS_HOME, null);
        if(modulesDirStr == null) {
            throw new CommandLineException(JBOSS_HOME + " environment variable is not set.");
        }
        modulesDir = new File(modulesDirStr, "modules");
        if(!modulesDir.exists()) {
            throw new CommandLineException("Failed to locate the modules dir on the filesystem: " + modulesDir.getAbsolutePath());
        }
        return modulesDir;
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

    public class ModuleAddValidator implements CommandValidator<ModuleAdd> {

        @Override
        public void validate(ModuleAdd command) throws CommandValidatorException {
            if(command.name == null || command.name.length() == 0)
                throw new CommandValidatorException("Name must be defined to create a new module");
        }
    }
}
