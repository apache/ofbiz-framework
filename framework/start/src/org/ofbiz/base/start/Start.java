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
package org.ofbiz.base.start;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

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
 * <tt>getInstance()</tt> static method.
 * </p>
 */
public final class Start {

    private Config config = null;
    private final List<String> loaderArgs = new ArrayList<String>();
    private final ArrayList<StartupLoader> loaders = new ArrayList<StartupLoader>();
    private final AtomicReference<ServerState> serverState = new AtomicReference<ServerState>(ServerState.STARTING);
    private Thread adminPortThread = null;

    // Singleton, do not change
    private static final Start instance = new Start();
    private Start() {
    }

    /**
     * main is the entry point to execute high level ofbiz commands 
     * such as starting, stopping or checking the status of the server.
     * 
     * @param args: The commands for ofbiz
     * @throws StartupException: terminates ofbiz or propagates to caller
     */
    public static void main(String[] args) throws StartupException {
        List<StartupCommand> ofbizCommands = null;
        try {
            ofbizCommands = StartupCommandUtil.parseOfbizCommands(args);
        } catch (StartupException e) {
            // incorrect arguments passed to the command line
            StartupCommandUtil.printAndHighlightMessage(e.getMessage());
            StartupCommandUtil.printOfbizStartupHelp(System.err);
            System.exit(1);
        }

        CommandType commandType = evaluateOfbizCommands(ofbizCommands);
        if(commandType != CommandType.HELP) {
            instance.init(ofbizCommands);
        }
        switch (commandType) {
            case HELP:
                StartupCommandUtil.printOfbizStartupHelp(System.out);
                break;
            case STATUS:
                System.out.println("Current Status : " + instance.status());
                break;
            case SHUTDOWN:
                System.out.println("Shutting down server : " + instance.shutdown());
                break;
            case START:
                populateLoaderArgs(ofbizCommands);
                instance.start();
                break;
        }
    }

    /**
     * Returns the <code>Start</code> instance.
     */
    public static Start getInstance() {
        return instance;
    }

    /**
     * Returns the server's main configuration.
     */
    public Config getConfig() {
        return this.config;
    }

    /**
     * Returns the server's current state.
     */
    public ServerState getCurrentState() {
        return serverState.get();
    }

    /**
     * This enum contains the possible OFBiz server states.
     */
    public enum ServerState {
        STARTING, RUNNING, STOPPING;

        @Override
        public String toString() {
            return name().charAt(0) + name().substring(1).toLowerCase();
        }
    }

    void init(List<StartupCommand> ofbizCommands) throws StartupException {
        loadGlobalOfbizSystemProperties("ofbiz.system.props");
        try {
            this.config = new Config(ofbizCommands);
        } catch (StartupException e) {
            throw new StartupException("Could not fetch config instance", e);
        }
    }

    void start() throws StartupException {
        // create the log directory
        createLogDirectory();
        // create the listener thread
        createListenerThread();
        // set the shutdown hook
        if (config.useShutdownHook) {
            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    shutdownServer();
                }
            });
        } else {
            System.out.println("Shutdown hook disabled");
        }

        // initialize the startup loaders
        initStartLoaders();
        if (!startStartLoaders()) {
            if (this.serverState.get() == ServerState.STOPPING) {
                return;
            } else {
                throw new StartupException("Error during start.");
            }
        }
        if (config.shutdownAfterLoad) {
            stopServer();
        }
    }

    void shutdownServer() {
        ServerState currentState;
        do {
            currentState = this.serverState.get();
            if (currentState == ServerState.STOPPING) {
                return;
            }
        } while (!this.serverState.compareAndSet(currentState, ServerState.STOPPING));
        // The current thread was the one that successfully changed the state;
        // continue with further processing.
        synchronized (this.loaders) {
            // Unload in reverse order
            for (int i = this.loaders.size(); i > 0; i--) {
                StartupLoader loader = this.loaders.get(i - 1);
                try {
                    loader.unload();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        if (this.adminPortThread != null && this.adminPortThread.isAlive()) {
            this.adminPortThread.interrupt();
        }
    }

    void stopServer() {
        shutdownServer();
        System.exit(0);
    }

    private static CommandType evaluateOfbizCommands(List<StartupCommand> ofbizCommands) {
        if (ofbizCommands.stream().anyMatch(
                command -> command.getName().equals(StartupCommandUtil.StartupOption.HELP.getName()))) {
            return CommandType.HELP;
        } else if (ofbizCommands.stream().anyMatch(
                command -> command.getName().equals(StartupCommandUtil.StartupOption.STATUS.getName()))) {
            return CommandType.STATUS;
        } else if (ofbizCommands.stream().anyMatch(
                command -> command.getName().equals(StartupCommandUtil.StartupOption.SHUTDOWN.getName()))) {
            return CommandType.SHUTDOWN;
        } else {
            return CommandType.START;
        }
    }

    private enum CommandType {
        HELP, STATUS, SHUTDOWN, START
    }

    private void loadGlobalOfbizSystemProperties(String globalOfbizPropertiesFileName) throws StartupException {
        String globalSystemPropsFileName = System.getProperty(globalOfbizPropertiesFileName);
        if (globalSystemPropsFileName != null) {
            FileInputStream stream = null;
            try {
                stream = new FileInputStream(globalSystemPropsFileName);
                System.getProperties().load(stream);
            } catch (IOException e) {
                throw new StartupException("Couldn't load global system props", e);
            } finally {
                if (stream != null) {
                    try {
                        stream.close();
                    } catch (IOException e) {
                        throw new StartupException("Couldn't close stream", e);
                    }
                }
            }
        }
    }

    /**
     * populates the loaderArgs with arguments as expected by the
     * containers that will receive them. 
     * 
     * TODO A better solution is to change the signature of all 
     * containers to receive a <tt>List</tt> of <tt>StartupCommand</tt>s
     * instead and delete the methods populateLoaderArgs, commandExistsInList
     * and retrieveCommandArgumentEntries along with the loaderArgs list.
     */
    private static void populateLoaderArgs(List<StartupCommand> ofbizCommands) {
        final String LOAD_DATA = StartupCommandUtil.StartupOption.LOAD_DATA.getName();
        final String TEST = StartupCommandUtil.StartupOption.TEST.getName();
        final String TEST_LIST = StartupCommandUtil.StartupOption.TEST_LIST.getName();
        
        if(commandExistsInList(ofbizCommands, LOAD_DATA)) {
            retrieveCommandArguments(ofbizCommands, LOAD_DATA).entrySet().stream().forEach(entry -> 
            instance.loaderArgs.add("-" + entry.getKey() + "=" + entry.getValue()));
        } else if(commandExistsInList(ofbizCommands, TEST)) {
            retrieveCommandArguments(ofbizCommands, TEST).entrySet().stream().forEach(entry -> 
            instance.loaderArgs.add("-" + entry.getKey() + "=" + entry.getValue()));
        } else if(commandExistsInList(ofbizCommands, TEST_LIST)) {
            Map<String,String> testListArgs = retrieveCommandArguments(ofbizCommands, TEST_LIST);
            instance.loaderArgs.add(testListArgs.get("file"));
            instance.loaderArgs.add("-" + testListArgs.get("mode"));
        }
    }

    private static boolean commandExistsInList(List<StartupCommand> ofbizCommands, String commandName) {
        return ofbizCommands.stream().anyMatch(command -> command.getName().equals(commandName));
    }

    private static Map<String,String> retrieveCommandArguments(List<StartupCommand> ofbizCommands, String commandName) {
        return ofbizCommands.stream()
                .filter(option-> option.getName().equals(commandName))
                .collect(Collectors.toList()).get(0).getProperties();
    }

    private void createListenerThread() throws StartupException {
        if (config.adminPort > 0) {
            this.adminPortThread = new AdminPortThread();
            this.adminPortThread.start();
        } else {
            System.out.println("Admin socket not configured; set to port 0");
        }
    }

    private void createLogDirectory() {
        File logDir = new File(config.logDir);
        if (!logDir.exists()) {
            if (logDir.mkdir()) {
                System.out.println("Created OFBiz log dir [" + logDir.getAbsolutePath() + "]");
            }
        }
    }

    private NativeLibClassLoader createClassLoader() throws IOException {
        ClassLoader parent = Thread.currentThread().getContextClassLoader();
        if (parent instanceof NativeLibClassLoader) {
            parent = parent.getParent();
        }
        if (parent == null) {
            parent = Start.class.getClassLoader();
            if (parent == null) {
                parent = ClassLoader.getSystemClassLoader();
            }
        }
        Classpath classPath = new Classpath();
        /*
         * Class paths needed to get StartupLoaders to work.
         */
        classPath.addComponent(config.ofbizHome);
        String ofbizHomeTmp = config.ofbizHome;
        if (!ofbizHomeTmp.isEmpty() && !ofbizHomeTmp.endsWith("/")) {
            ofbizHomeTmp = ofbizHomeTmp.concat("/");
        }
        if (config.classpathAddComponent != null) {
            String[] components = config.classpathAddComponent.split(",");
            for (String component : components) {
                classPath.addComponent(ofbizHomeTmp.concat(component.trim()));
            }
        }
        if (config.classpathAddFilesFromPath != null) {
            String[] paths = config.classpathAddFilesFromPath.split(",");
            for (String path : paths) {
                classPath.addFilesFromPath(new File(ofbizHomeTmp.concat(path.trim())));
            }
        }
        NativeLibClassLoader classloader = new NativeLibClassLoader(classPath.getUrls(), parent);
        if (config.instrumenterFile != null && config.instrumenterClassName != null) {
            try {
                classloader = new InstrumentingClassLoader(classPath.getUrls(), parent, config.instrumenterFile,
                        config.instrumenterClassName);
            } catch (Exception e) {
                System.out.println("Instrumenter not enabled - " + e);
            }
        }
        classloader.addNativeClassPath(System.getProperty("java.library.path"));
        for (File folder : classPath.getNativeFolders()) {
            classloader.addNativeClassPath(folder);
        }
        return classloader;
    }

    private void initStartLoaders() throws StartupException {
        NativeLibClassLoader classloader = null;
        try {
            classloader = createClassLoader();
        } catch (IOException e) {
            throw new StartupException("Couldn't create NativeLibClassLoader", e);
        }
        Thread.currentThread().setContextClassLoader(classloader);
        String[] argsArray = loaderArgs.toArray(new String[loaderArgs.size()]);
        synchronized (this.loaders) {
            for (Map<String, String> loaderMap : config.loaders) {
                if (this.serverState.get() == ServerState.STOPPING) {
                    return;
                }
                try {
                    String loaderClassName = loaderMap.get("class");
                    Class<?> loaderClass = classloader.loadClass(loaderClassName);
                    StartupLoader loader = (StartupLoader) loaderClass.newInstance();
                    loaders.add(loader); // add before loading, so unload can occur if error during loading
                    loader.load(config, argsArray);
                } catch (ClassNotFoundException e) {
                    throw new StartupException(e.getMessage(), e);
                } catch (InstantiationException e) {
                    throw new StartupException(e.getMessage(), e);
                } catch (IllegalAccessException e) {
                    throw new StartupException(e.getMessage(), e);
                }
            }
            this.loaders.trimToSize();
        }
        if (classloader instanceof InstrumentingClassLoader) {
            try {
                ((InstrumentingClassLoader)classloader).closeInstrumenter();
            } catch (IOException e) {
                throw new StartupException(e.getMessage(), e);
            }
        }
        StringBuilder sb = new StringBuilder();
        for (String path : classloader.getNativeLibPaths()) {
            if (sb.length() > 0) {
                sb.append(File.pathSeparator);
            }
            sb.append(path);
        }
        System.setProperty("java.library.path", sb.toString());
    }

    private String sendSocketCommand(Control control) throws IOException, ConnectException {
        String response = "OFBiz is Down";
        try {
            Socket socket = new Socket(config.adminAddress, config.adminPort);
            // send the command
            PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
            writer.println(config.adminKey + ":" + control);
            writer.flush();
            // read the reply
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            response = reader.readLine();
            reader.close();
            // close the socket
            writer.close();
            socket.close();

        } catch (ConnectException e) {
            System.out.println("Could not connect to " + config.adminAddress + ":" + config.adminPort);
        }
        return response;
    }

    private String shutdown() throws StartupException {
        try {
            return sendSocketCommand(Control.SHUTDOWN);
        } catch (Exception e) {
            throw new StartupException(e);
        }
    }

    /**
     * @return <code>true</code> if all loaders were started.
     */
    private boolean startStartLoaders() {
        synchronized (this.loaders) {
            // start the loaders
            for (StartupLoader loader : this.loaders) {
                if (this.serverState.get() == ServerState.STOPPING) {
                    return false;
                }
                try {
                    loader.start();
                } catch (StartupException e) {
                    e.printStackTrace();
                    return false;
                }
            }
        }
        return this.serverState.compareAndSet(ServerState.STARTING, ServerState.RUNNING);
    }

    private String status() throws StartupException {
        try {
            return sendSocketCommand(Control.STATUS);
        } catch (ConnectException e) {
            return "Not Running";
        } catch (IOException e) {
            throw new StartupException(e);
        }
    }

    private class AdminPortThread extends Thread {
        private ServerSocket serverSocket = null;

        AdminPortThread() throws StartupException {
            super("OFBiz-AdminPortThread");
            try {
                this.serverSocket = new ServerSocket(config.adminPort, 1, config.adminAddress);
            } catch (IOException e) {
                throw new StartupException("Couldn't create server socket(" + config.adminAddress + ":" + config.adminPort + ")",
                        e);
            }
            setDaemon(false);
        }

        private void processClientRequest(Socket client) throws IOException {
            BufferedReader reader = null;
            PrintWriter writer = null;
            try {
                reader = new BufferedReader(new InputStreamReader(client.getInputStream()));
                String request = reader.readLine();
                writer = new PrintWriter(client.getOutputStream(), true);
                Control control;
                if (request != null && !request.isEmpty() && request.contains(":")) {
                    String key = request.substring(0, request.indexOf(':'));
                    if (key.equals(config.adminKey)) {
                        control = Control.valueOf(request.substring(request.indexOf(':') + 1));
                        if (control == null) {
                            control = Control.FAIL;
                        }
                    } else {
                        control = Control.FAIL;
                    }
                } else {
                    control = Control.FAIL;
                }
                control.processRequest(Start.this, writer);
            } finally {
                if (reader != null) {
                    reader.close();
                }
                if (writer != null) {
                    writer.flush();
                    writer.close();
                }
            }
        }

        @Override
        public void run() {
            System.out.println("Admin socket configured on - " + config.adminAddress + ":" + config.adminPort);
            while (!Thread.interrupted()) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("Received connection from - " + clientSocket.getInetAddress() + " : "
                            + clientSocket.getPort());
                    processClientRequest(clientSocket);
                    clientSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private enum Control {
        SHUTDOWN {
            @Override
            void processRequest(Start start, PrintWriter writer) {
                if (start.serverState.get() == ServerState.STOPPING) {
                    writer.println("IN-PROGRESS");
                } else {
                    writer.println("OK");
                    writer.flush();
                    start.stopServer();
                }
            }
        },
        STATUS {
            @Override
            void processRequest(Start start, PrintWriter writer) {
                writer.println(start.serverState.get());
            }
        },
        FAIL {
            @Override
            void processRequest(Start start, PrintWriter writer) {
                writer.println("FAIL");
            }
        };

        abstract void processRequest(Start start, PrintWriter writer);
    }
}
