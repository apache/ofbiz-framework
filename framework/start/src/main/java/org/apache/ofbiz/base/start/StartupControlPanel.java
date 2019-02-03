/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *******************************************************************************/
package org.apache.ofbiz.base.start;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.ofbiz.base.container.ContainerLoader;
import org.apache.ofbiz.base.start.Start.ServerState;

/**
 * The StartupControlPanel controls OFBiz by executing high level
 * functions such as init, start and stop on the server. The bulk
 * of the startup sequence logic resides in this class.
 */
final class StartupControlPanel {

    public static final String module = StartupControlPanel.class.getName();

    /**
     * Initialize OFBiz by:
     * - setting high level JVM and OFBiz system properties
     * - creating a Config object holding startup configuration parameters
     *
     * @param ofbizCommands commands passed by the user to OFBiz on start
     * @return config OFBiz configuration
     */
    static Config init(List<StartupCommand> ofbizCommands) {
        Config config = null;
        try {
            loadGlobalOfbizSystemProperties("ofbiz.system.props");
            config =  new Config(ofbizCommands);
        } catch (StartupException e) {
            fullyTerminateSystem(e);
        }
        return config;
    }

    /**
     * Execute the startup sequence for OFBiz
     */
    static void start(Config config,
            AtomicReference<ServerState> serverState,
            List<StartupCommand> ofbizCommands) throws StartupException {

        ContainerLoader loader = new ContainerLoader();
        Thread adminServer = createAdminServer(config, serverState, loader);

        createLogDirectoryIfMissing(config);
        createRuntimeShutdownHook(config, loader, serverState);
        loadContainers(config, loader, ofbizCommands, serverState);
        printStartupMessage(config);
        executeShutdownAfterLoadIfConfigured(config, loader, serverState, adminServer);
    }

    /**
     * Print OFBiz startup message only if the OFBiz server is not scheduled for shutdown.
     * @param config contains parameters for system startup
     */
    private static void printStartupMessage(Config config) {
        if (!config.shutdownAfterLoad) {
            String lineSeparator = System.lineSeparator();
            System.out.println(lineSeparator + "   ____  __________  _" +
                               lineSeparator + "  / __ \\/ ____/ __ )(_)___" +
                               lineSeparator + " / / / / /_  / __  / /_  /" +
                               lineSeparator + "/ /_/ / __/ / /_/ / / / /_" +
                               lineSeparator + "\\____/_/   /_____/_/ /___/  is started and ready." +
                               lineSeparator);
        }
    }

    /**
     * Shutdown the OFBiz server. This method is invoked in one of the
     * following ways:
     *
     * - Manually if requested by the client AdminClient
     * - Automatically if Config.shutdownAfterLoad is set to true
     */
    static void stop(ContainerLoader loader, AtomicReference<ServerState> serverState, Thread adminServer) {
        shutdownServer(loader, serverState, adminServer);
        System.exit(0);
    }

    /**
     * Properly exit from the system when a StartupException cannot or
     * should not be handled except by exiting the system.
     *
     * A proper system exit is achieved by:
     *
     * - Printing the stack trace for users to see what happened
     * - Executing the shutdown hooks (if existing) through System.exit
     * - Terminating any lingering threads (if existing) through System.exit
     * - Providing an exit code that is not 0 to signal to the build system
     *   or user of failure to execute.
     *
     * @param e The startup exception that cannot / should not be handled
     *   except by terminating the system
     */
    static void fullyTerminateSystem(StartupException e) {
        e.printStackTrace();
        System.exit(1);
    }

    private static void shutdownServer(ContainerLoader loader, AtomicReference<ServerState> serverState, Thread adminServer) {
        ServerState currentState;
        do {
            currentState = serverState.get();
            if (currentState == ServerState.STOPPING) {
                return;
            }
        } while (!serverState.compareAndSet(currentState, ServerState.STOPPING));
        try {
            loader.unload();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (adminServer != null && adminServer.isAlive()) {
            adminServer.interrupt();
        }
    }

    private static void loadGlobalOfbizSystemProperties(String globalOfbizPropertiesFileName) throws StartupException {
        String systemProperties = System.getProperty(globalOfbizPropertiesFileName);
        if (systemProperties != null) {
            try (FileInputStream  stream = new FileInputStream(systemProperties)) {
            System.getProperties().load(stream);
            } catch (IOException e) {
                throw new StartupException("Couldn't load global system props", e);
            }
        }
    }

    private static Thread createAdminServer(
            Config config,
            AtomicReference<ServerState> serverState,
            ContainerLoader loader) throws StartupException {

        Thread adminServer = null;
        if (config.adminPort > 0) {
            adminServer = new AdminServer(loader, serverState, config);
            adminServer.start();
        } else {
            System.out.println("Admin socket not configured; set to port 0");
        }
        return adminServer;
    }

    private static void createLogDirectoryIfMissing(Config config) {
        File logDir = new File(config.logDir);
        if (!logDir.exists()) {
            if (logDir.mkdir()) {
                System.out.println("Created OFBiz log dir [" + logDir.getAbsolutePath() + "]");
            }
        }
    }

    private static void createRuntimeShutdownHook(
            Config config,
            ContainerLoader loader,
            AtomicReference<ServerState> serverState) {

        if (config.useShutdownHook) {
            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    shutdownServer(loader, serverState, this);
                }
            });
        } else {
            System.out.println("Shutdown hook disabled");
        }
    }

    private static void loadContainers(Config config,
            ContainerLoader loader,
            List<StartupCommand> ofbizCommands,
            AtomicReference<ServerState> serverState) throws StartupException {
        synchronized (StartupControlPanel.class) {
            if (serverState.get() == ServerState.STOPPING) {
                return;
            }
            loader.load(config, ofbizCommands);
        }
        serverState.compareAndSet(ServerState.STARTING, ServerState.RUNNING);
    }

    private static void executeShutdownAfterLoadIfConfigured(
            Config config,
            ContainerLoader loader,
            AtomicReference<ServerState> serverState,
            Thread adminServer) {

        if (config.shutdownAfterLoad) {
            stop(loader, serverState, adminServer);
        }
    }
}
