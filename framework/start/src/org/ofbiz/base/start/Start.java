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
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * OFBiz startup class.
 *
 * This class implements a thread-safe state machine. The design is critical
 * for reliable starting and stopping of the server.
 *
 * The machine's current state and state changes must be encapsulated in this
 * class. Client code may query the current state, but it may not change it.
 *
 * This class uses a singleton pattern to guarantee that only one server instance
 * is running in the VM. Client code retrieves the instance by using the getInstance()
 * static method.
 *
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
     * @throws StartupException: propagates to the servlet container
     */
    public static void main(String[] args) throws StartupException {
        Command command = evaluateOfbizCommand(args);
        if (command == Command.HELP) {
            help(System.out);
            return;
        } else if (command == Command.HELP_ERROR) {
            help(System.err);
            System.exit(1);
        }
        instance.init(args, command == Command.COMMAND);
        try {
            if (command == Command.STATUS) {
                System.out.println("Current Status : " + instance.status());
            } else if (command == Command.SHUTDOWN) {
                System.out.println("Shutting down server : " + instance.shutdown());
            } else {
                // general start
                instance.start();
            }
        } catch (Exception e) {
            throw new StartupException(e);
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

    void init(String[] args, boolean fullInit) throws StartupException {
        String globalSystemPropsFileName = System.getProperty("ofbiz.system.props");
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
        try {
            this.config = new Config(args);
        } catch (IOException e) {
            throw new StartupException("Couldn't not fetch config instance", e);
        }
        // parse the startup arguments
        if (args.length > 1) {
            this.loaderArgs.addAll(Arrays.asList(args).subList(1, args.length));
            // Needed when portoffset is used with these commands
            try {
                if ("status".equals(args[0])) {
                    System.out.println("Current Status : " + instance.status());
                } else if ("stop".equals(args[0])) {
                    System.out.println("Shutting down server : " + instance.shutdown());
                }
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(99);
            }
        }
        if (!fullInit) {
            return;
        }
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
    }

    void start() throws Exception {
        if (!startStartLoaders()) {
            if (this.serverState.get() == ServerState.STOPPING) {
                return;
            } else {
                throw new Exception("Error during start.");
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

    private static Command evaluateOfbizCommand(String[] args) {
        Command command = null;
        for (String arg : args) {
            if (arg.equals("-help") || arg.equals("-?")) {
                command = checkCommand(command, Command.HELP);
            } else if (arg.equals("-status")) {
                command = checkCommand(command, Command.STATUS);
            } else if (arg.equals("-shutdown")) {
                command = checkCommand(command, Command.SHUTDOWN);
            } else if (arg.startsWith("-")) {
                if (!arg.contains("portoffset")) {
                    command = checkCommand(command, Command.COMMAND);
                }
            } else {
                command = checkCommand(command, Command.COMMAND);
                if (command == Command.COMMAND) {
                } else {
                    command = Command.HELP_ERROR;
                }
            }
        }
        if (command == null) {
            command = Command.COMMAND;
        }
        return command;
    }

    private static Command checkCommand(Command command, Command wanted) {
        if (wanted == Command.HELP || wanted.equals(command)) {
            return wanted;
        } else if (command == null) {
            return wanted;
        } else {
            System.err.println("Duplicate command detected(was " + command + ", wanted " + wanted);
            return Command.HELP_ERROR;
        }
    }

    private static void help(PrintStream out) {
        // Currently some commands have no dash, see OFBIZ-5872
        out.println("");
        out.println("Usage: java -jar ofbiz.jar [command] [arguments]");
        out.println("both    -----> Runs simultaneously the POS (Point of Sales) application and OFBiz standard");
        out.println("-help, -? ----> This screen");
        out.println("load-data -----> Creates tables/load data, eg: load-data -readers=seed,demo,ext -timeout=7200 -delegator=default -group=org.ofbiz. Or: load-data -file=/tmp/dataload.xml");
        out.println("pos     -----> Runs the POS (Point of Sales) application");
        out.println("start -------> Starts the server");
        out.println("-status ------> Gives the status of the server");
        out.println("-shutdown ----> Shutdowns the server");
        out.println("test --------> Runs the JUnit test script");
        out.println("[no config] --> Uses default config");
        out.println("[no command] -> Starts the server with default config");
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

    /**
     * Creates a new <code>NativeLibClassLoader</code> instance.
     *
     * @return A new <code>NativeLibClassLoader</code> instance
     * @throws IOException
     */
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

    private String shutdown() throws IOException {
        return sendSocketCommand(Control.SHUTDOWN);
    }

    /**
     * Returns <code>true</code> if all loaders were started.
     *
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

    private String status() throws IOException {
        try {
            return sendSocketCommand(Control.STATUS);
        } catch (ConnectException e) {
            return "Not Running";
        } catch (IOException e) {
            throw e;
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

    private enum Command {
        HELP, HELP_ERROR, STATUS, SHUTDOWN, COMMAND
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
