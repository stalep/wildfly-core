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

import org.jboss.aesh.cl.Arguments;
import org.jboss.aesh.cl.CommandDefinition;
import org.jboss.aesh.console.Prompt;
import org.jboss.aesh.console.command.Command;
import org.jboss.aesh.console.command.CommandResult;
import org.jboss.as.cli.CommandLineException;
import org.jboss.as.cli.ControllerAddress;
import org.jboss.as.cli.ControllerAddressResolver;
import org.jboss.as.cli.Util;
import org.jboss.as.cli.connection.AuthenticationCallbackHandler;
import org.jboss.as.cli.connection.CliSSLContext;
import org.jboss.as.cli.impl.ModelControllerClientFactory;
import org.jboss.as.cli.operation.impl.DefaultOperationRequestBuilder;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.protocol.GeneralTimeoutHandler;
import org.jboss.as.protocol.StreamUtils;
import org.jboss.logging.Logger;
import org.xnio.http.RedirectException;

import javax.net.ssl.SSLException;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.sasl.SaslException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@CommandDefinition(name = "connect", description = "Connect to a JBoss Server")
public class Connect implements Command<CliCommandInvocation>, CallbackHandler {

    private static final Logger log = Logger.getLogger(Command.class);
    private final GeneralTimeoutHandler timeoutHandler = new GeneralTimeoutHandler();

    @Arguments(defaultValue = {"localhost"})
    private List<String> host;

    @Override
    public CommandResult execute(CliCommandInvocation commandInvocation) throws IOException, InterruptedException {
        try {
            if(commandInvocation.getCommandContext().getModelControllerClient() == null) {
                //TODO: clean this up!
                connectController(
                        commandInvocation.getCommandContext().getControllerAddressResolver(),
                        commandInvocation.getCommandContext().getCliSSLContext(),
                        commandInvocation.getCommandContext().getControllerHost(),
                        commandInvocation.getCommandContext().getContextConfig().isDisableLocalAuth(),
                        commandInvocation.getCommandContext().getContextConfig().getConnectionTimeout(),
                        commandInvocation.getCommandContext().getContextConfig().getUsername(),
                        commandInvocation.getCommandContext().getContextConfig().getPassword(),
                        commandInvocation.getCommandContext().getContextConfig().getClientBindAddress(),
                        commandInvocation);

                if(commandInvocation.getCommandContext().getModelControllerClient() != null)
                    return CommandResult.SUCCESS;
                else
                    return CommandResult.FAILURE;
            }
            else {
                commandInvocation.getShell().out().println("Already connected to a session, log out before connecting to a new session.");
                return CommandResult.SUCCESS;
            }
        }
        catch (CommandLineException e) {
            e.printStackTrace();
            return CommandResult.FAILURE;
        }
    }

    private void connectController(ControllerAddressResolver addressResolver,
                                   CliSSLContext cliSSLContext,
                                   String controller,
                                   boolean disableLocalAuth,
                                   int connectionTimeout,
                                   String username,
                                   char[] password,
                                   String clientBindAddress,
                                   CliCommandInvocation commandInvocation) throws CommandLineException {

        ControllerAddress address = addressResolver.resolveAddress(controller);

        // In case the alias mappings cause us to enter some form of loop or a badly
        // configured server does the same,
        Set<ControllerAddress> visited = new HashSet<>();
        visited.add(address);
        boolean retry;
        do {
            try {
                CallbackHandler cbh = new AuthenticationCallbackHandler(username, password, commandInvocation);
                if (log.isDebugEnabled()) {
                    log.debug("connecting to " + address.getHost() + ':' + address.getPort() + " as " + username);
                }
                ModelControllerClient tempClient =
                        ModelControllerClientFactory.CUSTOM.getClient(address, cbh,
                        disableLocalAuth, cliSSLContext.getSslContext(), connectionTimeout, null,
                                timeoutHandler, clientBindAddress);
                retry = false;
                tryConnection(tempClient, address);
                //TODO: need to replace null with ConnectionInfoBean
                commandInvocation.getCommandContext().initNewClient(tempClient, address, null);
                commandInvocation.setPrompt(new Prompt(
                        commandInvocation.getCommandContext().getPrompt()));
            }
            catch (RedirectException re) {
                try {
                    URI location = new URI(re.getLocation());
                    if ("http-remoting".equals(address.getProtocol()) && "https".equals(location.getScheme())) {
                        int port = location.getPort();
                        if (port < 0) {
                            port = 443;
                        }
                        address = addressResolver.resolveAddress(new URI("https-remoting", null, location.getHost(), port,
                                null, null, null).toString());
                        if (visited.add(address) == false) {
                            throw new CommandLineException("Redirect to address already tried encountered Address="
                                    + address.toString());
                        }
                        retry = true;
                    } else if (address.getHost().equals(location.getHost()) && address.getPort() == location.getPort()
                            && location.getPath() != null && location.getPath().length() > 1) {
                        throw new CommandLineException("Server at " + address.getHost() + ":" + address.getPort()
                                + " does not support " + address.getProtocol());
                    } else {
                        throw new CommandLineException("Unsupported redirect received.", re);
                    }
                } catch (URISyntaxException e) {
                    throw new CommandLineException("Bad redirect location '" + re.getLocation() + "' received.", e);
                }
            } catch (IOException e) {
                throw new CommandLineException("Failed to resolve host '" + address.getHost() + "'", e);
            }
        } while (retry);
    }

    /**
     * Used to make a call to the server to verify that it is possible to connect.
     */
    private void tryConnection(final ModelControllerClient client, ControllerAddress address) throws CommandLineException, RedirectException {
        try {
            DefaultOperationRequestBuilder builder = new DefaultOperationRequestBuilder();
            builder.setOperationName(Util.READ_ATTRIBUTE);
            builder.addProperty(Util.NAME, Util.NAME);

            client.execute(builder.buildRequest());
            // We don't actually care what the response is we just want to be sure the ModelControllerClient
            // does not throw an Exception.
        } catch (Exception e) {
            try {
                Throwable current = e;
                while (current != null) {
                    if (current instanceof SaslException) {
                        throw new CommandLineException("Unable to authenticate against controller at " + address.getHost() + ":" + address.getPort(), current);
                    }
                    if (current instanceof SSLException) {
                        throw new CommandLineException("Unable to negotiate SSL connection with controller at "+ address.getHost() + ":" + address.getPort());
                    }
                    if (current instanceof RedirectException) {
                        throw (RedirectException) current;
                    }
                    if (current instanceof CommandLineException) {
                        throw (CommandLineException) current;
                    }
                    current = current.getCause();
                }

                // We don't know what happened, most likely a timeout.
                throw new CommandLineException("The controller is not available at " + address.getHost() + ":" + address.getPort(), e);
            } finally {
                StreamUtils.safeClose(client);
            }
        }
    }

    @Override
    public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {

    }
}
