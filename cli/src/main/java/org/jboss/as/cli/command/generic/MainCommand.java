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
package org.jboss.as.cli.command.generic;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jboss.aesh.cl.internal.ProcessedCommand;
import org.jboss.aesh.cl.internal.ProcessedOptionBuilder;
import org.jboss.aesh.cl.parser.CommandLineParserException;
import org.jboss.aesh.console.command.CommandResult;
import org.jboss.aesh.console.command.map.MapCommand;
import org.jboss.aesh.console.command.map.MapProcessedCommandBuilder;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.CommandLineException;
import org.jboss.as.cli.command.CliCommandInvocation;
import org.jboss.as.cli.command.activator.PresenceOptionsActivatorBuilder;
import org.jboss.dmr.ModelNode;

/**
 *
 * @author jfdenise
 */
class MainCommand extends MapCommand<CliCommandInvocation> {

    private final NodeType nodeType;
    private final String propertyId;
    private final String commandName;
    private final Set<String> excludedOps;

    MainCommand(String commandName, NodeType nodeType, String propertyId, String... excludedOps) {
        this.nodeType = nodeType;
        this.propertyId = propertyId;
        this.commandName = commandName;
        this.excludedOps = new HashSet<>(Arrays.asList(excludedOps));
    }

    @Override
    public CommandResult execute(CliCommandInvocation commandInvocation) throws IOException, InterruptedException {
        if (contains("help")) {
            try {
                printHelp(commandInvocation.getCommandContext(), getValues());
            } catch (CommandLineException ex) {
                throw new RuntimeException(ex);
            }
        }
        return null;
    }

    private void printHelp(CommandContext ctx,
            Map<String, Object> values) throws CommandLineException {

        if (values.containsKey("properties")) {
            org.jboss.as.cli.command.generic.Util.printProperties(ctx, propertyId,
                    org.jboss.as.cli.command.generic.Util.
                    getAttributeDescriptions(ctx, nodeType));
            return;
        }

        if (values.containsKey("commands")) {
            printSupportedCommands(ctx);
            return;
        }

        printNodeDescription(ctx);
    }

    private void printNodeDescription(CommandContext ctx) throws CommandFormatException {

        int offset = 2;

        ctx.printLine("\nSYNOPSIS\n");

        final StringBuilder buf = new StringBuilder();
        buf.append("  ").append(commandName).append(" --help [--properties | --commands] |\n");
        if (nodeType.dependsOnProfile() && ctx.isDomainMode()) {
            for (int i = 0; i <= commandName.length() + offset; ++i) {
                buf.append(' ');
            }
            buf.append("--profile=<profile_name>\n");
        }
        for (int i = 0; i <= commandName.length() + offset; ++i) {
            buf.append(' ');
        }
        buf.append('(').append(propertyId).append("=<resource_id> (--<property>=<value>)*) |\n");
        for (int i = 0; i <= commandName.length() + offset; ++i) {
            buf.append(' ');
        }
        buf.append("(<command> ").append(propertyId).append("=<resource_id> (--<parameter>=<value>)*)");

        buf.append('\n');
        for (int i = 0; i <= commandName.length() + offset; ++i) {
            buf.append(' ');
        }
        buf.append("[--headers={<operation_header> (;<operation_header>)*}]");
        ctx.printLine(buf.toString());

        ctx.printLine("\n\nDESCRIPTION\n");

        buf.setLength(0);
        buf.append("The command is used to manage resources of type ");
        buf.append(this.nodeType);
        buf.append(".");
        org.jboss.as.cli.command.generic.Util.formatText(ctx, buf, offset);

        ctx.printLine("\n\nRESOURCE DESCRIPTION\n");

        if (nodeType.dependsOnProfile() && ctx.isDomainMode() && getValue("profile") == null) {
            buf.setLength(0);
            buf.append("(Execute '");
            buf.append(commandName).append(" --profile=<profile_name> --help' to include the resource description here.)");
            org.jboss.as.cli.command.generic.Util.formatText(ctx, buf, offset);
        } else if (ctx.getModelControllerClient() == null) {
            buf.setLength(0);
            buf.append("(Connection to the controller is required to be able to load the resource description)");
            org.jboss.as.cli.command.generic.Util.formatText(ctx, buf, offset);
        } else {
            ModelNode request = org.jboss.as.cli.command.generic.Util.initRequest(ctx, nodeType);
            if (request == null) {
                return;
            }
            request.get(org.jboss.as.cli.Util.OPERATION).set(org.jboss.as.cli.Util.READ_RESOURCE_DESCRIPTION);
            ModelNode result = null;
            try {
                result = ctx.getModelControllerClient().execute(request);
                if (!result.hasDefined(org.jboss.as.cli.Util.RESULT)) {
                    throw new CommandFormatException("Node description is not available.");
                }
                result = result.get(org.jboss.as.cli.Util.RESULT);
                if (!result.hasDefined(org.jboss.as.cli.Util.DESCRIPTION)) {
                    throw new CommandFormatException("Node description is not available.");
                }
            } catch (IOException | CommandFormatException e) {
            }

            buf.setLength(0);
            if (result != null) {
                buf.append(result.get(org.jboss.as.cli.Util.DESCRIPTION).asString());
            } else {
                buf.append("N/A. Please, open a jira issue at https://issues.jboss.org/browse/WFLY to get this fixed. Thanks!");
            }
            org.jboss.as.cli.command.generic.Util.formatText(ctx, buf, offset);
        }

        ctx.printLine("\n\nARGUMENTS\n");

        org.jboss.as.cli.command.generic.Util.formatProperty(ctx, "--help", "prints this content.");

        org.jboss.as.cli.command.generic.Util.formatProperty(ctx, "--help --properties",
                "prints the list of the resource properties including their access-type "
                + "(read/write/metric), value type, and the description.");

        org.jboss.as.cli.command.generic.Util.formatProperty(ctx, "--help --commands",
                "prints the list of the commands available for the resource."
                + " To get the complete description of a specific command (including its parameters, "
                + "their types and descriptions), execute " + commandName + " <command> --help.");

        if (nodeType.dependsOnProfile() && ctx.isDomainMode()) {
            org.jboss.as.cli.command.generic.Util.formatProperty(ctx, "--profile", "the name of the profile the target resource belongs to.");
        }

        buf.setLength(0);
        if (propertyId.equals("name")) {
            buf.append("is the name of the resource that completes the path ").append(nodeType).append(" and ");
        } else {
            buf.append("corresponds to a property of the resource which ");
        }
        buf.append("is used to identify the resource against which the command should be executed.");
        org.jboss.as.cli.command.generic.Util.formatProperty(ctx, propertyId, buf);

        org.jboss.as.cli.command.generic.Util.formatProperty(ctx, "<property>",
                "property name of the resource whose value should be updated. "
                + "For a complete list of available property names, their types and descriptions, execute "
                + commandName + " --help --properties.");

        org.jboss.as.cli.command.generic.Util.formatProperty(ctx, "<command>",
                "command name provided by the resource. For a complete list of available commands execute "
                + commandName + " --help --commands.");

        org.jboss.as.cli.command.generic.Util.formatProperty(ctx, "<parameter>",
                "parameter name of the <command> provided by the resource. "
                + "For a complete list of available parameter names of a specific <command>, "
                + "their types and descriptions execute " + commandName + " <command> --help.");

        org.jboss.as.cli.command.generic.Util.formatProperty(ctx, "--headers",
                "a list of operation headers separated by a semicolon. For the list of supported "
                + "headers, please, refer to the domain management documentation or use tab-completion.");
    }

    public ProcessedCommand getProcessedCommand(final CommandContext commandContext) throws CommandLineParserException {
        return new MapProcessedCommandBuilder().
                name(commandName).
                addOption(new ProcessedOptionBuilder().name("help").
                        hasValue(false).
                        type(String.class).
                        create()).
                addOption(new ProcessedOptionBuilder().name("properties").
                        hasValue(false).
                        type(String.class).
                        activator(new PresenceOptionsActivatorBuilder().
                                expected("help").
                                notExpected("commands").
                                create()).
                        create()
                ).
                addOption(new ProcessedOptionBuilder().name("commands").
                        hasValue(false).
                        type(String.class).
                        activator(new PresenceOptionsActivatorBuilder().
                                expected("help").
                                notExpected("properties").create()).create()).
                command(this).create();
    }

    private void printSupportedCommands(CommandContext ctx) throws CommandLineException {
        final List<String> list
                = org.jboss.as.cli.command.generic.Util.
                getSupportedCommands(ctx, nodeType, excludedOps, null);
        list.add("write-attributes");
        list.add("To read the description of a specific command execute '"
                + commandName + " command_name --help'.");
        for (String name : list) {
            ctx.printLine(name);
        }
    }

}
