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

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.apache.ofbiz.base.container.ContainerLoader;

/**
 * OFBiz startup class.
 *
 * <p>
 * This class implements a thread-safe state machine. The design is critical
 * for reliable starting and stopping of the server.
 * </p>
 * <p>
 * The machine's current state and state changes must be encapsulated in this
 * class. Client code may query the current state, but it may not change it.
 * </p>
 * <p>
 * This class uses a singleton pattern to guarantee that only one server instance
 * is running in the VM. Client code retrieves the instance by using the
 * {@code getInstance()} static method.
 * </p>
 */
public final class Start {

    private Config config = null;
    private ContainerLoader loader = new ContainerLoader();
    private final AtomicReference<ServerState> serverState = new AtomicReference<>(ServerState.STARTING);

    // Singleton, do not change
    private static final Start INSTANCE = new Start();
    private Start() {
    }

    /**
     * main is the entry point to execute high level OFBiz commands
     * such as starting, stopping or checking the status of the server.
     *
     * @param args The commands for OFBiz
     */
    public static void main(String[] args) {
        List<StartupCommand> ofbizCommands = null;
        try {
            ofbizCommands = StartupCommandUtil.parseOfbizCommands(args);
        } catch (StartupException e) {
            // incorrect arguments passed to the command line
            StartupCommandUtil.highlightAndPrintErrorMessage(e.getMessage());
            StartupCommandUtil.printOfbizStartupHelp(System.err);
            System.exit(1);
        }

        CommandType commandType = CommandType.valueOf(ofbizCommands);
        if (!commandType.equals(CommandType.HELP)) {
            INSTANCE.config = StartupControlPanel.init(ofbizCommands);
        }
        switch (commandType) {
        case HELP:
            StartupCommandUtil.printOfbizStartupHelp(System.out);
            break;
        case STATUS:
            System.out.println("Current Status : " + AdminClient.requestStatus(INSTANCE.config));
            break;
        case SHUTDOWN:
            System.out.println("Shutting down server : " + AdminClient.requestShutdown(INSTANCE.config));
            break;
        case START:
            try {
                StartupControlPanel.start(INSTANCE.config, INSTANCE.serverState, ofbizCommands, INSTANCE.loader);
            } catch (StartupException e) {
                StartupControlPanel.fullyTerminateSystem(e);
            }
            break;
        }
    }

    /**
     * Returns the <code>Start</code> instance.
     */
    public static Start getInstance() {
        return INSTANCE;
    }

    /**
     * Returns the server's main configuration.
     */
    public Config getConfig() {
        return this.config;
    }

    /**
     * Updates the server's main configuration.
     *
     * @param c  the new configuration
     */
    public void setConfig(Config c) {
        this.config = c;
    }

    /**
     * Returns the server's current state.
     */
    public ServerState getCurrentState() {
        return serverState.get();
    }

    public void stop() {
        StartupControlPanel.shutdownServer(loader, serverState);
        System.exit(0);
    }

    /**
     * This enum contains the possible OFBiz server states.
     */
    public enum ServerState {
        STARTING, RUNNING, STOPPING;

        @Override
        public String toString() {
            return name().charAt(0) + name().substring(1).toLowerCase(Locale.getDefault());
        }
    }

    /**
     * The type of command that allow dispatching to various startup behavior.
     */
    private enum CommandType {
        HELP, STATUS, SHUTDOWN, START;

        /**
         * Determines the type of command from a list of command-line commands
         * @param ofbizCommands  the list of parsed command-line arguments which cannot be {@code null}
         * @return the corresponding command type.
         */
        static CommandType valueOf(List<StartupCommand> ofbizCommands) {
            Set<String> commandNames = ofbizCommands.stream().map(StartupCommand::getName).collect(Collectors.toSet());
            if (commandNames.contains(StartupCommandUtil.StartupOption.HELP.getName())) {
                return CommandType.HELP;
            } else if (commandNames.contains(StartupCommandUtil.StartupOption.STATUS.getName())) {
                return CommandType.STATUS;
            } else if (commandNames.contains(StartupCommandUtil.StartupOption.SHUTDOWN.getName())) {
                return CommandType.SHUTDOWN;
            } else {
                return CommandType.START;
            }
        }
    }
}
