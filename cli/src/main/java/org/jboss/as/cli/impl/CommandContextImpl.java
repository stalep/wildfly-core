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
package org.jboss.as.cli.impl;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ServiceLoader;

import javax.net.ssl.SSLException;
import javax.security.sasl.SaslException;

import org.jboss.as.cli.CliConfig;
import org.jboss.as.cli.CliEvent;
import org.jboss.as.cli.CliEventListener;
import org.jboss.as.cli.CliInitializationException;
import org.jboss.as.cli.CommandCompleter;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.CommandHandler;
import org.jboss.as.cli.CommandHandlerProvider;
import org.jboss.as.cli.CommandHistory;
import org.jboss.as.cli.CommandLineCompleter;
import org.jboss.as.cli.CommandLineException;
import org.jboss.as.cli.CommandLineRedirection;
import org.jboss.as.cli.CommandRegistry;
import org.jboss.as.cli.ControllerAddress;
import org.jboss.as.cli.ControllerAddressResolver;
import org.jboss.as.cli.OperationCommand;
import org.jboss.as.cli.Util;
import org.jboss.as.cli.batch.Batch;
import org.jboss.as.cli.batch.BatchManager;
import org.jboss.as.cli.batch.BatchedCommand;
import org.jboss.as.cli.batch.impl.DefaultBatchManager;
import org.jboss.as.cli.batch.impl.DefaultBatchedCommand;
import org.jboss.as.cli.connection.CliSSLContext;
import org.jboss.as.cli.handlers.ArchiveHandler;
import org.jboss.as.cli.handlers.ClearScreenHandler;
import org.jboss.as.cli.handlers.CommandCommandHandler;
import org.jboss.as.cli.handlers.ConnectHandler;
import org.jboss.as.cli.handlers.DeployHandler;
import org.jboss.as.cli.handlers.DeploymentInfoHandler;
import org.jboss.as.cli.handlers.DeploymentOverlayHandler;
import org.jboss.as.cli.handlers.EchoDMRHandler;
import org.jboss.as.cli.handlers.EchoVariableHandler;
import org.jboss.as.cli.handlers.GenericTypeOperationHandler;
import org.jboss.as.cli.handlers.HelpHandler;
import org.jboss.as.cli.handlers.HistoryHandler;
import org.jboss.as.cli.handlers.LsHandler;
import org.jboss.as.cli.handlers.OperationRequestHandler;
import org.jboss.as.cli.handlers.PrefixHandler;
import org.jboss.as.cli.handlers.PrintWorkingNodeHandler;
import org.jboss.as.cli.handlers.QuitHandler;
import org.jboss.as.cli.handlers.ReadAttributeHandler;
import org.jboss.as.cli.handlers.ReadOperationHandler;
import org.jboss.as.cli.handlers.ReloadHandler;
import org.jboss.as.cli.handlers.SetVariableHandler;
import org.jboss.as.cli.handlers.ShutdownHandler;
import org.jboss.as.cli.handlers.UndeployHandler;
import org.jboss.as.cli.handlers.UnsetVariableHandler;
import org.jboss.as.cli.handlers.VersionHandler;
import org.jboss.as.cli.handlers.batch.BatchClearHandler;
import org.jboss.as.cli.handlers.batch.BatchDiscardHandler;
import org.jboss.as.cli.handlers.batch.BatchEditLineHandler;
import org.jboss.as.cli.handlers.batch.BatchHandler;
import org.jboss.as.cli.handlers.batch.BatchHoldbackHandler;
import org.jboss.as.cli.handlers.batch.BatchListHandler;
import org.jboss.as.cli.handlers.batch.BatchMoveLineHandler;
import org.jboss.as.cli.handlers.batch.BatchRemoveLineHandler;
import org.jboss.as.cli.handlers.batch.BatchRunHandler;
import org.jboss.as.cli.handlers.ifelse.ElseHandler;
import org.jboss.as.cli.handlers.ifelse.EndIfHandler;
import org.jboss.as.cli.handlers.ifelse.IfHandler;
import org.jboss.as.cli.handlers.jca.JDBCDriverInfoHandler;
import org.jboss.as.cli.handlers.jca.JDBCDriverNameProvider;
import org.jboss.as.cli.handlers.jca.XADataSourceAddCompositeHandler;
import org.jboss.as.cli.handlers.jms.CreateJmsResourceHandler;
import org.jboss.as.cli.handlers.jms.DeleteJmsResourceHandler;
import org.jboss.as.cli.handlers.module.ASModuleHandler;
import org.jboss.as.cli.handlers.trycatch.CatchHandler;
import org.jboss.as.cli.handlers.trycatch.EndTryHandler;
import org.jboss.as.cli.handlers.trycatch.FinallyHandler;
import org.jboss.as.cli.handlers.trycatch.TryHandler;
import org.jboss.as.cli.operation.CommandLineParser;
import org.jboss.as.cli.operation.NodePathFormatter;
import org.jboss.as.cli.operation.OperationCandidatesProvider;
import org.jboss.as.cli.operation.OperationFormatException;
import org.jboss.as.cli.operation.OperationRequestAddress;
import org.jboss.as.cli.operation.ParsedCommandLine;
import org.jboss.as.cli.operation.impl.DefaultCallbackHandler;
import org.jboss.as.cli.operation.impl.DefaultOperationCandidatesProvider;
import org.jboss.as.cli.operation.impl.DefaultOperationRequestAddress;
import org.jboss.as.cli.operation.impl.DefaultOperationRequestBuilder;
import org.jboss.as.cli.operation.impl.DefaultOperationRequestParser;
import org.jboss.as.cli.operation.impl.DefaultPrefixFormatter;
import org.jboss.as.cli.operation.impl.RolloutPlanCompleter;
import org.jboss.as.cli.parsing.operation.OperationFormat;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.protocol.GeneralTimeoutHandler;
import org.jboss.as.protocol.StreamUtils;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.logging.Logger.Level;
import org.jboss.sasl.util.HexConverter;
import org.xnio.http.RedirectException;

/**
 *
 * @author Alexey Loubyansky
 */
class CommandContextImpl implements CommandContext, ModelControllerClientFactory.ConnectionCloseHandler {

    private static final Logger log = Logger.getLogger(CommandContext.class);

    /** the cli configuration */
    private final CliConfig config;
    private final ControllerAddressResolver addressResolver;

    private final CommandRegistry cmdRegistry = new CommandRegistry();

    private Console console;

    /** whether the session should be terminated */
    private boolean terminate;

    /** current command line */
    private String cmdLine;
    /** parsed command arguments */
    private DefaultCallbackHandler parsedCmd = new DefaultCallbackHandler(true);

    /** domain or standalone mode */
    private boolean domainMode;
    /** the controller client */
    private ModelControllerClient client;

    /** the address of the current controller */
    private ControllerAddress currentAddress;
    /** the command line specified username */
    private final String username;
    /** the command line specified password */
    private final char[] password;
    /** flag to disable the local authentication mechanism */
    private final boolean disableLocalAuth;
    /** the time to connect to a controller */
    private final int connectionTimeout;
    /** various key/value pairs */
    private Map<String, Object> map = new HashMap<String, Object>();
    /** operation request address prefix */
    private OperationRequestAddress prefix = new DefaultOperationRequestAddress();
    /** the prefix formatter */
    private final NodePathFormatter prefixFormatter = DefaultPrefixFormatter.INSTANCE;
    /** provider of operation request candidates for tab-completion */
    private final OperationCandidatesProvider operationCandidatesProvider;
    /** operation request handler */
    private final OperationRequestHandler operationHandler;
    /** batches */
    private BatchManager batchManager = new DefaultBatchManager();
    /** the default command completer */
    private final CommandCompleter cmdCompleter;
    /** the timeout handler */
    private final GeneralTimeoutHandler timeoutHandler = new GeneralTimeoutHandler();

    /** output target */
    private BufferedWriter outputTarget;

    private List<CliEventListener> listeners = new ArrayList<CliEventListener>();

    /** the value of this variable will be used as the exit code of the vm, it is reset by every command/operation executed */
    private int exitCode;

    private File currentDir = new File("");

    /** whether to resolve system properties passed in as values of operation parameters*/
    private boolean resolveParameterValues;

    /** whether to write messages to the terminal output */
    private boolean silent;

    private Map<String, String> variables;

    private CliShutdownHook.Handler shutdownHook;

    /** command line handling redirection */
    private CommandLineRedirectionRegistration redirection;
    /** SSLContext and TrustManager */
    private CliSSLContext cliSSLContext;

    /**
     * Version mode - only used when --version is called from the command line.
     *
     * @throws CliInitializationException
     */
    CommandContextImpl() throws CliInitializationException {
        this.console = null;
        this.operationCandidatesProvider = null;
        this.cmdCompleter = null;
        operationHandler = new OperationRequestHandler();
        initCommands();
        config = CliConfigImpl.load(this);
        addressResolver = ControllerAddressResolver.newInstance(config, null);
        resolveParameterValues = config.isResolveParameterValues();
        this.connectionTimeout = config.getConnectionTimeout();
        silent = config.isSilent();
        username = null;
        password = null;
        disableLocalAuth = false;
        initSSLContext();
        addShutdownHook();
        CliLauncher.runcom(this);
    }

    CommandContextImpl(String username, char[] password, boolean disableLocalAuth) throws CliInitializationException {
        this(null, username, password, disableLocalAuth, false, -1);
    }

    /**
     * Default constructor used for both interactive and non-interactive mode.
     *
     */
    CommandContextImpl(String defaultController, String username, char[] password, boolean disableLocalAuth, boolean initConsole, final int connectionTimeout)
            throws CliInitializationException {

        config = CliConfigImpl.load(this);
        addressResolver = ControllerAddressResolver.newInstance(config, defaultController);

        operationHandler = new OperationRequestHandler();

        this.username = username;
        this.password = password;
        this.disableLocalAuth = disableLocalAuth;
        this.connectionTimeout = connectionTimeout != -1 ? connectionTimeout : config.getConnectionTimeout();

        resolveParameterValues = config.isResolveParameterValues();
        silent = config.isSilent();
        initCommands();

        initSSLContext();

        if (initConsole) {
            cmdCompleter = new CommandCompleter(cmdRegistry);
            //initBasicConsole(null, null);
            //console.addCompleter(cmdCompleter);
            this.operationCandidatesProvider = new DefaultOperationCandidatesProvider();
        } else {
            this.cmdCompleter = null;
            this.operationCandidatesProvider = null;
        }

        addShutdownHook();
        CliLauncher.runcom(this);
    }

    CommandContextImpl(String defaultController,
            String username, char[] password, boolean disableLocalAuth,
            InputStream consoleInput, OutputStream consoleOutput)
            throws CliInitializationException {

        config = CliConfigImpl.load(this);
        addressResolver = ControllerAddressResolver.newInstance(config, defaultController);

        operationHandler = new OperationRequestHandler();

        this.username = username;
        this.password = password;
        this.disableLocalAuth = disableLocalAuth;
        this.connectionTimeout = config.getConnectionTimeout();

        resolveParameterValues = config.isResolveParameterValues();
        silent = config.isSilent();
        initCommands();

        initSSLContext();

        cmdCompleter = new CommandCompleter(cmdRegistry);
        //initBasicConsole(consoleInput, consoleOutput);
        //console.addCompleter(cmdCompleter);
        this.operationCandidatesProvider = new DefaultOperationCandidatesProvider();

        addShutdownHook();
        CliLauncher.runcom(this);
    }

    protected void addShutdownHook() {
        shutdownHook = new CliShutdownHook.Handler() {
            @Override
            public void shutdown() {
                terminateSession();
            }};
        CliShutdownHook.add(shutdownHook);
    }

    /*
    protected void initBasicConsole(InputStream consoleInput, OutputStream consoleOutput) throws CliInitializationException {
        copyConfigSettingsToConsole(consoleInput, consoleOutput);
        this.console = Console.Factory.getConsole(this);
    }

    private void copyConfigSettingsToConsole(InputStream consoleInput, OutputStream consoleOutput) {
        if(consoleInput != null)
            Settings.getInstance().setInputStream(consoleInput);
        if(consoleOutput != null)
            Settings.getInstance().setStdOut(consoleOutput);

        Settings.getInstance().setHistoryDisabled(!config.isHistoryEnabled());
        Settings.getInstance().setHistoryFile(new File(config.getHistoryFileDir(), config.getHistoryFileName()));
        Settings.getInstance().setHistorySize(config.getHistoryMaxSize());
        Settings.getInstance().setEnablePipelineAndRedirectionParser(false);
    }
    */

    private void initCommands() {
        cmdRegistry.registerHandler(new PrefixHandler(), "cd", "cn");
        cmdRegistry.registerHandler(new ClearScreenHandler(), "clear", "cls");
        cmdRegistry.registerHandler(new CommandCommandHandler(cmdRegistry), "command");
        cmdRegistry.registerHandler(new ConnectHandler(), "connect");
        cmdRegistry.registerHandler(new EchoDMRHandler(), "echo-dmr");
        cmdRegistry.registerHandler(new HelpHandler(cmdRegistry), "help", "h");
        cmdRegistry.registerHandler(new HistoryHandler(), "history");
        cmdRegistry.registerHandler(new LsHandler(), "ls");
        cmdRegistry.registerHandler(new ASModuleHandler(this), "module");
        cmdRegistry.registerHandler(new PrintWorkingNodeHandler(), "pwd", "pwn");
        cmdRegistry.registerHandler(new QuitHandler(), "quit", "q", "exit");
        cmdRegistry.registerHandler(new ReadAttributeHandler(this), "read-attribute");
        cmdRegistry.registerHandler(new ReadOperationHandler(this), "read-operation");
        cmdRegistry.registerHandler(new ReloadHandler(this), "reload");
        cmdRegistry.registerHandler(new ShutdownHandler(this), "shutdown");
        cmdRegistry.registerHandler(new VersionHandler(), "version");

        // variables
        cmdRegistry.registerHandler(new SetVariableHandler(), "set");
        cmdRegistry.registerHandler(new EchoVariableHandler(), "echo");
        cmdRegistry.registerHandler(new UnsetVariableHandler(), "unset");

        // deployment
        cmdRegistry.registerHandler(new DeployHandler(this), "deploy");
        cmdRegistry.registerHandler(new UndeployHandler(this), "undeploy");
        cmdRegistry.registerHandler(new DeploymentInfoHandler(this), "deployment-info");
        cmdRegistry.registerHandler(new DeploymentOverlayHandler(this), "deployment-overlay");

        // batch commands
        cmdRegistry.registerHandler(new BatchHandler(this), "batch");
        cmdRegistry.registerHandler(new BatchDiscardHandler(), "discard-batch");
        cmdRegistry.registerHandler(new BatchListHandler(), "list-batch");
        cmdRegistry.registerHandler(new BatchHoldbackHandler(), "holdback-batch");
        cmdRegistry.registerHandler(new BatchRunHandler(this), "run-batch");
        cmdRegistry.registerHandler(new BatchClearHandler(), "clear-batch");
        cmdRegistry.registerHandler(new BatchRemoveLineHandler(), "remove-batch-line");
        cmdRegistry.registerHandler(new BatchMoveLineHandler(), "move-batch-line");
        cmdRegistry.registerHandler(new BatchEditLineHandler(), "edit-batch-line");

        // try-catch
        cmdRegistry.registerHandler(new TryHandler(), "try");
        cmdRegistry.registerHandler(new CatchHandler(), "catch");
        cmdRegistry.registerHandler(new FinallyHandler(), "finally");
        cmdRegistry.registerHandler(new EndTryHandler(), "end-try");

        // if else
        cmdRegistry.registerHandler(new IfHandler(), "if");
        cmdRegistry.registerHandler(new ElseHandler(), "else");
        cmdRegistry.registerHandler(new EndIfHandler(), "end-if");

        // data-source
        GenericTypeOperationHandler dsHandler = new GenericTypeOperationHandler(this, "/subsystem=datasources/data-source", null);
        final DefaultCompleter driverNameCompleter = new DefaultCompleter(JDBCDriverNameProvider.INSTANCE);
        dsHandler.addValueCompleter(Util.DRIVER_NAME, driverNameCompleter);
        cmdRegistry.registerHandler(dsHandler, "data-source");
        GenericTypeOperationHandler xaDsHandler = new GenericTypeOperationHandler(this, "/subsystem=datasources/xa-data-source", null);
        xaDsHandler.addValueCompleter(Util.DRIVER_NAME, driverNameCompleter);
        // override the add operation with the handler that accepts xa props
        final XADataSourceAddCompositeHandler xaDsAddHandler = new XADataSourceAddCompositeHandler(this, "/subsystem=datasources/xa-data-source");
        xaDsAddHandler.addValueCompleter(Util.DRIVER_NAME, driverNameCompleter);
        xaDsHandler.addHandler("add", xaDsAddHandler);
        cmdRegistry.registerHandler(xaDsHandler, "xa-data-source");
        cmdRegistry.registerHandler(new JDBCDriverInfoHandler(this), "jdbc-driver-info");

        // JMS
        cmdRegistry.registerHandler(new GenericTypeOperationHandler(this, "/subsystem=messaging/hornetq-server=default/jms-queue", "queue-address"), "jms-queue");
        cmdRegistry.registerHandler(new GenericTypeOperationHandler(this, "/subsystem=messaging/hornetq-server=default/jms-topic", "topic-address"), "jms-topic");
        cmdRegistry.registerHandler(new GenericTypeOperationHandler(this, "/subsystem=messaging/hornetq-server=default/connection-factory", null), "connection-factory");
        // these are used for the cts setup
        cmdRegistry.registerHandler(new CreateJmsResourceHandler(this), false, "create-jms-resource");
        cmdRegistry.registerHandler(new DeleteJmsResourceHandler(this), false, "delete-jms-resource");

        // rollout plan
        final GenericTypeOperationHandler rolloutPlan = new GenericTypeOperationHandler(this, "/management-client-content=rollout-plans/rollout-plan", null);
        rolloutPlan.addValueConverter("content", HeadersArgumentValueConverter.INSTANCE);
        rolloutPlan.addValueCompleter("content", RolloutPlanCompleter.INSTANCE);
        cmdRegistry.registerHandler(rolloutPlan, "rollout-plan");

        // supported but hidden from tab-completion until stable implementation
        cmdRegistry.registerHandler(new ArchiveHandler(this), false, "archive");

        registerExtraHandlers();
    }

    private void registerExtraHandlers() {
        ServiceLoader<CommandHandlerProvider> loader = ServiceLoader.load(CommandHandlerProvider.class);
        for (CommandHandlerProvider provider : loader) {
            cmdRegistry.registerHandler(provider.createCommandHandler(this), provider.isTabComplete(), provider.getNames());
        }
    }

    public int getExitCode() {
        return exitCode;
    }

    /**
     * Initialise the SSLContext and associated TrustManager for this CommandContext.
     *
     * If no configuration is specified the default mode of operation will be to use a lazily initialised TrustManager with no
     * KeyManager.
     */
    private void initSSLContext() throws CliInitializationException {
        cliSSLContext = new CliSSLContext(config.getSslConfig(), timeoutHandler);
    }

    @Override
    public boolean isTerminated() {
        return terminate;
    }

    private StringBuilder lineBuffer;

    @Override
    public void handle(String line) throws CommandLineException {
        if (line.isEmpty() || line.charAt(0) == '#') {
            return; // ignore comments
        }

        int i = line.length() - 1;
        while(i > 0 && line.charAt(i) <= ' ') {
            if(line.charAt(--i) == '\\') {
                break;
            }
        }
        if(line.charAt(i) == '\\') {
            if(lineBuffer == null) {
                lineBuffer = new StringBuilder();
            }
            lineBuffer.append(line, 0, i);
            lineBuffer.append(' ');
            return;
        } else if(lineBuffer != null) {
            lineBuffer.append(line);
            line = lineBuffer.toString();
            lineBuffer = null;
        }

        resetArgs(line);
        try {
            if(redirection != null) {
                redirection.target.handle(this);
            } else if (parsedCmd.getFormat() == OperationFormat.INSTANCE) {
                final ModelNode request = parsedCmd.toOperationRequest(this);

                if (isBatchMode()) {
                    StringBuilder op = new StringBuilder();
                    op.append(getNodePathFormatter().format(parsedCmd.getAddress()));
                    op.append(line.substring(line.indexOf(':')));
                    DefaultBatchedCommand batchedCmd = new DefaultBatchedCommand(op.toString(), request);
                    Batch batch = getBatchManager().getActiveBatch();
                    batch.add(batchedCmd);
                } else {
                    set("OP_REQ", request);
                    try {
                        operationHandler.handle(this);
                    } finally {
                        set("OP_REQ", null);
                    }
                }
            } else {
                final String cmdName = parsedCmd.getOperationName();
                CommandHandler handler = cmdRegistry.getCommandHandler(cmdName.toLowerCase());
                if (handler != null) {
                    if (isBatchMode() && handler.isBatchMode(this)) {
                        if (!(handler instanceof OperationCommand)) {
                            throw new CommandLineException("The command is not allowed in a batch.");
                        } else {
                            try {
                                ModelNode request = ((OperationCommand) handler).buildRequest(this);
                                BatchedCommand batchedCmd = new DefaultBatchedCommand(line, request);
                                Batch batch = getBatchManager().getActiveBatch();
                                batch.add(batchedCmd);
                            } catch (CommandFormatException e) {
                                throw new CommandFormatException("Failed to add to batch '" + line + "'", e);
                            }
                        }
                    } else {
                        handler.handle(this);
                    }
                } else {
                    throw new CommandLineException("Unexpected command '" + line + "'. Type 'help --commands' for the list of supported commands.");
                }
            }
        } catch(CommandLineException e) {
            throw e;
        } catch(Throwable t) {
            throw new CommandLineException("Failed to handle '" + line + "'", t);
        } finally {
            // so that getArgumentsString() doesn't return this line
            // during the tab-completion of the next command
            cmdLine = null;
        }
    }

    public void handleSafe(String line) {
        exitCode = 0;
        try {
            handle(line);
        } catch(Throwable t) {
            final StringBuilder buf = new StringBuilder();
            buf.append(t.getLocalizedMessage());
            Throwable t1 = t.getCause();
            while(t1 != null) {
                if(t1.getLocalizedMessage() != null) {
                    buf.append(": ").append(t1.getLocalizedMessage());
                } else {
                    buf.append(": ").append(t1.getClass().getName());
                }
                t1 = t1.getCause();
            }
            error(buf.toString());
        }
    }

    @Override
    public String getArgumentsString() {
        // a little hack to support tab-completion of commands and ops spread across multiple lines
        if(lineBuffer != null) {
            return lineBuffer.toString();
        }
        if (cmdLine != null && parsedCmd.getOperationName() != null) {
            int cmdNameLength = parsedCmd.getOperationName().length();
            if (cmdLine.length() == cmdNameLength) {
                return null;
            } else {
                return cmdLine.substring(cmdNameLength + 1);
            }
        }
        return null;
    }

    @Override
    public void terminateSession() {
        if(!terminate) {
            terminate = true;
            disconnectController();
            if (shutdownHook != null) {
                CliShutdownHook.remove(shutdownHook);
            }
        }
    }

    @Override
    public void printLine(String message) {
        final Level logLevel;
        if(exitCode != 0) {
            logLevel = Level.ERROR;
        } else {
            logLevel = Level.INFO;
        }
        if(log.isEnabled(logLevel)) {
            log.log(logLevel, message);
        }

        if (outputTarget != null) {
            try {
                outputTarget.append(message);
                outputTarget.newLine();
                outputTarget.flush();
            } catch (IOException e) {
                System.err.println("Failed to print '" + message + "' to the output target: " + e.getLocalizedMessage());
            }
            return;
        }

        if(!silent) {
            if (console != null) {
                console.print(message);
                console.printNewLine();
            } else { // non-interactive mode
                System.out.println(message);
            }
        }
    }

    /**
     * Set the exit code of the process to indicate an error and output the error message.
     *
     * WARNING This method should only be called for unrecoverable errors as once the exit code is set subsequent operations may
     * not be possible.
     *
     * @param message The message to display.
     */
    protected void error(String message) {
        this.exitCode = 1;
        printLine(message);
    }

    private String readLine(String prompt, boolean password, boolean disableHistory) throws CommandLineException {
        if (console == null) {
            //initBasicConsole(null, null);
        }

        boolean useHistory = console.isUseHistory();
        if (useHistory && disableHistory) {
            console.setUseHistory(false);
        }
        try {
            if (password) {
                return console.readLine(prompt, (char) 0x00);
            } else {
                return console.readLine(prompt);
            }

        } finally {
            if (disableHistory && useHistory) {
                console.setUseHistory(true);
            }
        }
    }

    @Override
    public void printColumns(Collection<String> col) {
        if(log.isInfoEnabled()) {
            log.info(col);
        }
        if (outputTarget != null) {
            try {
                for (String item : col) {
                    outputTarget.append(item);
                    outputTarget.newLine();
                }
            } catch (IOException e) {
                System.err.println("Failed to print columns '" + col + "' to the console: " + e.getLocalizedMessage());
            }
            return;
        }

        if(!silent) {
            if (console != null) {
                console.printColumns(col);
            } else { // non interactive mode
                for (String item : col) {
                    System.out.println(item);
                }
            }
        }
    }

    @Override
    public void set(String key, Object value) {
        map.put(key, value);
    }

    @Override
    public Object get(String key) {
        return map.get(key);
    }

    @Override
    public Object remove(String key) {
        return map.remove(key);
    }

    @Override
    public ModelControllerClient getModelControllerClient() {
        return client;
    }

    @Override
    public CommandLineParser getCommandLineParser() {
        return DefaultOperationRequestParser.INSTANCE;
    }

    @Override
    public OperationRequestAddress getCurrentNodePath() {
        return prefix;
    }

    @Override
    public NodePathFormatter getNodePathFormatter() {

        return prefixFormatter;
    }

    @Override
    public OperationCandidatesProvider getOperationCandidatesProvider() {
        return operationCandidatesProvider;
    }

    @Override
    public void connectController() throws CommandLineException {
        connectController(null);
    }

    @Override
    public void connectController(String controller) throws CommandLineException {


    }

    @Override
    @Deprecated
    public void connectController(String host, int port) throws CommandLineException {
        try {
            connectController(new URI(null, null, host, port, null, null, null).toString().substring(2));
        } catch (URISyntaxException e) {
            throw new CommandLineException("Unable to construct URI for connection.", e);
        }
    }

    @Override
    public void bindClient(ModelControllerClient newClient) {
        initNewClient(newClient, null);
    }

    @Override
    public void initNewClient(ModelControllerClient newClient, ControllerAddress address) {
        if (newClient != null) {
            if (this.client != null) {
                disconnectController();
            }

            client = newClient;
            this.currentAddress = address;

            List<String> nodeTypes = Util.getNodeTypes(newClient, new DefaultOperationRequestAddress());
            domainMode = nodeTypes.contains(Util.SERVER_GROUP);
        }
    }

    @Override
    public File getCurrentDir() {
        return currentDir;
    }

    @Override
    public void setCurrentDir(File dir) {
        if(dir == null) {
            throw new IllegalArgumentException("dir is null");
        }
        this.currentDir = dir;
    }

    @Override
    public void registerRedirection(CommandLineRedirection redirection) throws CommandLineException {
        if(this.redirection != null) {
            throw new CommandLineException("Another redirection is currently active.");
        }
        this.redirection = new CommandLineRedirectionRegistration(redirection);
        redirection.set(this.redirection);
    }

    /**
     * Handle the last SSL failure, prompting the user to accept or reject the certificate of the remote server.
     *
     * @return true if the certificate validation should be retried.
     */
    private boolean handleSSLFailure(Certificate[] lastChain) throws CommandLineException {
        printLine("Unable to connect due to unrecognised server certificate");
        for (Certificate current : lastChain) {
            if (current instanceof X509Certificate) {
                X509Certificate x509Current = (X509Certificate) current;
                Map<String, String> fingerprints = generateFingerprints(x509Current);
                printLine("Subject    - " + x509Current.getSubjectX500Principal().getName());
                printLine("Issuer     - " + x509Current.getIssuerDN().getName());
                printLine("Valid From - " + x509Current.getNotBefore());
                printLine("Valid To   - " + x509Current.getNotAfter());
                for (String alg : fingerprints.keySet()) {
                    printLine(alg + " : " + fingerprints.get(alg));
                }
                printLine("");
            }
        }

        for (;;) {
            String response;
            if (cliSSLContext.getTrustManager().isModifyTrustStore()) {
                response = readLine("Accept certificate? [N]o, [T]emporarily, [P]ermenantly : ", false, true);
            } else {
                response = readLine("Accept certificate? [N]o, [T]emporarily : ", false, true);
            }

            if (response != null && response.length() == 1) {
                switch (response.toLowerCase(Locale.ENGLISH).charAt(0)) {
                    case 'n':
                        return false;
                    case 't':
                        cliSSLContext.getTrustManager().storeChainTemporarily(lastChain);
                        return true;
                    case 'p':
                        if (cliSSLContext.getTrustManager().isModifyTrustStore()) {
                            cliSSLContext.getTrustManager().storeChainPermenantly(lastChain);
                            return true;
                        }
                }
            }
        }
    }

    private static final String[] FINGERPRINT_ALGORITHMS = new String[] { "MD5", "SHA1" };

    private Map<String, String> generateFingerprints(final X509Certificate cert) throws CommandLineException  {
        Map<String, String> fingerprints = new HashMap<String, String>(FINGERPRINT_ALGORITHMS.length);
        for (String current : FINGERPRINT_ALGORITHMS) {
            try {
                fingerprints.put(current, generateFingerPrint(current, cert.getEncoded()));
            } catch (GeneralSecurityException e) {
                throw new CommandLineException("Unable to generate fingerprint", e);
            }
        }

        return fingerprints;
    }

    private String generateFingerPrint(final String algorithm, final byte[] cert) throws GeneralSecurityException {
        StringBuilder sb = new StringBuilder();

        MessageDigest md = MessageDigest.getInstance(algorithm);
        byte[] digested = md.digest(cert);
        String hex = HexConverter.convertToHexString(digested);
        boolean started = false;
        for (int i = 0; i < hex.length() - 1; i += 2) {
            if (started) {
                sb.append(":");
            } else {
                started = true;
            }
            sb.append(hex.substring(i, i + 2));
        }

        return sb.toString();
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
    public void disconnectController() {
        if (this.client != null) {
            StreamUtils.safeClose(client);
            // if(loggingEnabled) {
            // printLine("Closed connection to " + this.controllerHost + ':' +
            // this.controllerPort);
            // }
            client = null;
            this.currentAddress = null;
            domainMode = false;
            notifyListeners(CliEvent.DISCONNECTED);
        }
        promptConnectPart = null;
    }

    @Override
    @Deprecated
    public String getDefaultControllerHost() {
        return config.getDefaultControllerHost();
    }

    @Override
    @Deprecated
    public int getDefaultControllerPort() {
        return config.getDefaultControllerPort();
    }

    @Override
    public ControllerAddress getDefaultControllerAddress() {
        return config.getDefaultControllerAddress();
    }

    @Override
    public String getControllerHost() {
        return currentAddress != null ? currentAddress.getHost() : null;
    }

    @Override
    public int getControllerPort() {
        return currentAddress != null ? currentAddress.getPort() : -1;
    }

    @Override
    public void clearScreen() {
        if(console != null) {
            console.clearScreen();
        }
    }

    String promptConnectPart;

    @Override
    public String getPrompt() {
        if(lineBuffer != null) {
            return "> ";
        }
        StringBuilder buffer = new StringBuilder();
        if (promptConnectPart == null) {
            buffer.append('[');
            String controllerHost = getControllerHost();
            if (controllerHost != null) {
                if (domainMode) {
                    buffer.append("domain@");
                } else {
                    buffer.append("standalone@");
                }
                buffer.append(controllerHost).append(':').append(getControllerPort()).append(' ');
                promptConnectPart = buffer.toString();
            } else {
                buffer.append("disconnected ");
            }
        } else {
            buffer.append(promptConnectPart);
        }

        if (prefix.isEmpty()) {
            buffer.append('/');
        } else {
            buffer.append(prefix.getNodeType());
            final String nodeName = prefix.getNodeName();
            if (nodeName != null) {
                buffer.append('=').append(nodeName);
            }
        }

        if (isBatchMode()) {
            buffer.append(" #");
        }
        buffer.append("] ");
        return buffer.toString();
    }

    @Override
    public CommandHistory getHistory() {
        if(console == null) {
            /*
            try {
                initBasicConsole(null, null);
            } catch (CliInitializationException e) {
                throw new IllegalStateException("Failed to initialize console.", e);
            }
            */
        }
        return console.getHistory();
    }

    private void resetArgs(String cmdLine) throws CommandFormatException {
        if (cmdLine != null) {
            parsedCmd.parse(prefix, cmdLine, this);
            setOutputTarget(parsedCmd.getOutputTarget());
        }
        this.cmdLine = cmdLine;
    }

    @Override
    public boolean isBatchMode() {
        return batchManager.isBatchActive();
    }

    @Override
    public BatchManager getBatchManager() {
        return batchManager;
    }

    @Override
    public BatchedCommand toBatchedCommand(String line) throws CommandFormatException {
        return new DefaultBatchedCommand(line, buildRequest(line, true));
    }

    @Override
    public ModelNode buildRequest(String line) throws CommandFormatException {
        return buildRequest(line, false);
    }

    protected ModelNode buildRequest(String line, boolean batchMode) throws CommandFormatException {

        if (line == null || line.isEmpty()) {
            throw new OperationFormatException("The line is null or empty.");
        }

        final DefaultCallbackHandler originalParsedArguments = this.parsedCmd;
        final String originalCmdLine = this.cmdLine;
        try {
            this.parsedCmd = new DefaultCallbackHandler();
            resetArgs(line);

            if (parsedCmd.getFormat() == OperationFormat.INSTANCE) {
                final ModelNode request = this.parsedCmd.toOperationRequest(this);
                StringBuilder op = new StringBuilder();
                op.append(prefixFormatter.format(parsedCmd.getAddress()));
                op.append(line.substring(line.indexOf(':')));
                return request;
            }

            final CommandHandler handler = cmdRegistry.getCommandHandler(parsedCmd.getOperationName());
            if (handler == null) {
                throw new OperationFormatException("No command handler for '" + parsedCmd.getOperationName() + "'.");
            }
            if(batchMode) {
                if(!handler.isBatchMode(this)) {
                    throw new OperationFormatException("The command is not allowed in a batch.");
                }
            } else if (!(handler instanceof OperationCommand)) {
                throw new OperationFormatException("The command does not translate to an operation request.");
            }

            return ((OperationCommand) handler).buildRequest(this);
        } finally {
            this.parsedCmd = originalParsedArguments;
            this.cmdLine = originalCmdLine;
        }
    }

    @Override
    public CommandLineCompleter getDefaultCommandCompleter() {
        return cmdCompleter;
    }

    @Override
    public ParsedCommandLine getParsedCommandLine() {
        return parsedCmd;
    }

    @Override
    public boolean isDomainMode() {
        return domainMode;
    }

    @Override
    public void addEventListener(CliEventListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("Listener is null.");
        }
        listeners.add(listener);
    }

    @Override
    public CliConfig getConfig() {
        return config;
    }

    protected void setOutputTarget(String filePath) {
        if (filePath == null) {
            this.outputTarget = null;
            return;
        }
        FileWriter writer;
        try {
            writer = new FileWriter(filePath, false);
        } catch (IOException e) {
            error(e.getLocalizedMessage());
            return;
        }
        this.outputTarget = new BufferedWriter(writer);
    }

    protected void notifyListeners(CliEvent event) {
        for (CliEventListener listener : listeners) {
            listener.cliEvent(event, this);
        }
    }

    @Override
    public void interact() {
        if(cmdCompleter == null) {
            throw new IllegalStateException("The console hasn't been initialized at construction time.");
        }

        if (this.client == null) {
            printLine("You are disconnected at the moment. Type 'connect' to connect to the server or"
                    + " 'help' for the list of supported commands.");
        }

        try {
            while (!isTerminated()) {
                final String line = console.readLine(getPrompt());
                if (line == null) {
                    terminateSession();
                } else {
                    handleSafe(line.trim());
                }
            }
            printLine("");
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    @Override
    public boolean isResolveParameterValues() {
        return resolveParameterValues;
    }

    @Override
    public void setResolveParameterValues(boolean resolve) {
        this.resolveParameterValues = resolve;
    }

    @Override
    public void handleClose() {
        if(parsedCmd.getFormat().equals(OperationFormat.INSTANCE) && "shutdown".equals(parsedCmd.getOperationName())) {
            final String restart = parsedCmd.getPropertyValue("restart");
            if(restart == null || !Util.TRUE.equals(restart)) {
                disconnectController();
                printLine("");
                printLine("The connection to the controller has been closed as the result of the shutdown operation.");
                printLine("(Although the command prompt will wrongly indicate connection until the next line is entered)");
            } // else maybe still notify the listeners that the connection has been closed
        }
    }

    @Override
    public boolean isSilent() {
        return this.silent;
    }

    @Override
    public void setSilent(boolean silent) {
        this.silent = silent;
    }

    @Override
    public int getTerminalWidth() {
        if(console == null) {
            /*
            try {
                this.initBasicConsole(null, null);
            } catch (CliInitializationException e) {
                this.error("Failed to initialize the console: " + e.getLocalizedMessage());
                return 80;
            }
            */
        }
        return console.getTerminalWidth();
    }

    @Override
    public int getTerminalHeight() {
        if(console == null) {
            /*
            try {
                this.initBasicConsole(null, null);
            } catch (CliInitializationException e) {
                this.error("Failed to initialize the console: " + e.getLocalizedMessage());
                return 24;
            }
            */
        }
        return console.getTerminalHeight();
    }

    @Override
    public void setVariable(String name, String value) throws CommandLineException {
        if(name == null || name.isEmpty()) {
            throw new CommandLineException("Variable name can't be null or an empty string");
        }
        if(!Character.isJavaIdentifierStart(name.charAt(0))) {
            throw new CommandLineException("Variable name must be a valid Java identifier (and not contain '$'): '" + name + "'");
        }
        for(int i = 1; i < name.length(); ++i) {
            final char c = name.charAt(i);
            if(!Character.isJavaIdentifierPart(c) || c == '$') {
                throw new CommandLineException("Variable name must be a valid Java identifier (and not contain '$'): '" + name + "'");
            }
        }

        if(value == null) {
            if(variables == null) {
                return;
            }
            variables.remove(name);
        } else {
            if(variables == null) {
                variables = new HashMap<String,String>();
            }
            variables.put(name, value);
        }
    }

    @Override
    public String getVariable(String name) {
        return variables == null ? null : variables.get(name);
    }

    @Override
    public Collection<String> getVariables() {
        return variables == null ? Collections.<String>emptySet() : variables.keySet();
    }

    @Override
    public ControllerAddressResolver getControllerAddressResolver() {
        return addressResolver;
    }

    @Override
    public CliSSLContext getCliSSLContext() {
        return cliSSLContext;
    }

    @Override
    public void setCurrentNodePath(OperationRequestAddress address) {
        prefix = address;
    }

    class CommandLineRedirectionRegistration implements CommandLineRedirection.Registration {

        CommandLineRedirection target;

        CommandLineRedirectionRegistration(CommandLineRedirection redirection) {
            if(redirection == null) {
                throw new IllegalArgumentException("Redirection is null");
            }
            this.target = redirection;
        }

        @Override
        public void unregister() throws CommandLineException {
            ensureActive();
            CommandContextImpl.this.redirection = null;
        }

        @Override
        public boolean isActive() {
            return CommandContextImpl.this.redirection == this;
        }

        @Override
        public void handle(ParsedCommandLine parsedLine) throws CommandLineException {

            ensureActive();

            final String line = parsedLine.getSubstitutedLine();
            if (parsedLine.getFormat() == OperationFormat.INSTANCE) {
                final ModelNode request = Util.toOperationRequest(CommandContextImpl.this, parsedLine);
                if (isBatchMode()) {
                    StringBuilder op = new StringBuilder();
                    op.append(getNodePathFormatter().format(parsedCmd.getAddress()));
                    op.append(line.substring(line.indexOf(':')));
                    DefaultBatchedCommand batchedCmd = new DefaultBatchedCommand(op.toString(), request);
                    Batch batch = getBatchManager().getActiveBatch();
                    batch.add(batchedCmd);
                } else {
                    set("OP_REQ", request);
                    try {
                        operationHandler.handle(CommandContextImpl.this);
                    } finally {
                        set("OP_REQ", null);
                    }
                }
            } else {
                final String cmdName = parsedCmd.getOperationName();
                CommandHandler handler = cmdRegistry.getCommandHandler(cmdName.toLowerCase());
                if (handler != null) {
                    if (isBatchMode() && handler.isBatchMode(CommandContextImpl.this)) {
                        if (!(handler instanceof OperationCommand)) {
                            throw new CommandLineException("The command is not allowed in a batch.");
                        } else {
                            try {
                                ModelNode request = ((OperationCommand) handler).buildRequest(CommandContextImpl.this);
                                BatchedCommand batchedCmd = new DefaultBatchedCommand(line, request);
                                Batch batch = getBatchManager().getActiveBatch();
                                batch.add(batchedCmd);
                            } catch (CommandFormatException e) {
                                throw new CommandFormatException("Failed to add to batch '" + line + "'", e);
                            }
                        }
                    } else {
                        handler.handle(CommandContextImpl.this);
                    }
                } else {
                    throw new CommandLineException("Unexpected command '" + line + "'. Type 'help --commands' for the list of supported commands.");
                }
            }
        }

        private void ensureActive() throws CommandLineException {
            if(!isActive()) {
                throw new CommandLineException("The redirection is not registered any more.");
            }
        }
    }
}
