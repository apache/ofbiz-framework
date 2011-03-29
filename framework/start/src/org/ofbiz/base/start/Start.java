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

/**
 * Start - OFBiz Container(s) Startup Class
 *
 */
public class Start {

    private static final String SHUTDOWN_COMMAND = "SHUTDOWN";
    private static final String STATUS_COMMAND = "STATUS";

    public static void main(String[] args) throws IOException {
        String firstArg = args.length > 0 ? args[0] : "";
        Start start = new Start();

        if (firstArg.equals("-help") || firstArg.equals("-?")) {
            System.out.println("");
            System.out.println("Usage: java -jar ofbiz.jar [command] [arguments]");
            System.out.println("-both    -----> Run simultaneously the POS (Point of Sales) application and OFBiz standard");
            System.out.println("-help, -? ----> This screen");
            System.out.println("-install -----> Run install (create tables/load data)");
            System.out.println("-pos     -----> Run the POS (Point of Sales) application");
            System.out.println("-setup -------> Run external application server setup");
            System.out.println("-start -------> Start the server");
            System.out.println("-status ------> Status of the server");
            System.out.println("-shutdown ----> Shutdown the server");
            System.out.println("-test --------> Run the JUnit test script");
            System.out.println("[no config] --> Use default config");
            System.out.println("[no command] -> Start the server w/ default config");
        } else {
            // hack for the status and shutdown commands
            if (firstArg.equals("-status")) {
                start.init(args, false);
                System.out.println("Current Status : " + start.status());
            } else if (firstArg.equals("-shutdown")) {
                start.init(args, false);
                System.out.println("Shutting down server : " + start.shutdown());
            } else {
                // general start
                start.init(args, true);
                start.start();
            }
        }
    }

    private Config config = null;
    private String[] loaderArgs = null;
    private final ArrayList<StartupLoader> loaders = new ArrayList<StartupLoader>();
    private boolean serverStarted = false;
    private boolean serverStopping = false;
    private Thread adminPortThread = null;

    private void createListenerThread() throws IOException {
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

    public void init(String[] args) throws IOException {
        init(args, true);
    }

    public void init(String[] args, boolean fullInit) throws IOException {
        String globalSystemPropsFileName = System.getProperty("ofbiz.system.props");
        if (globalSystemPropsFileName != null) {
            try {
                System.getProperties().load(new FileInputStream(globalSystemPropsFileName));
            } catch (IOException e) {
                throw (IOException) new IOException("Couldn't load global system props").initCause(e);
            }
        }
        this.config = Config.getInstance(args);

        // parse the startup arguments
        if (args.length > 0) {
            this.loaderArgs = new String[args.length];
            System.arraycopy(args, 0, this.loaderArgs, 0, this.loaderArgs.length);
        }

        if (fullInit) {
            // initialize the classpath
            initClasspath();

            // create the log directory
            createLogDirectory();

            // create the listener thread
            createListenerThread();

            // set the shutdown hook
            if (config.useShutdownHook) {
                Runtime.getRuntime().addShutdownHook(new Thread() { public void run() { shutdownServer(); } });
            } else {
                System.out.println("Shutdown hook disabled");
            }

            // initialize the startup loaders
            if (!initStartLoaders()) {
                System.exit(99);
            }
        }
    }

    private void initClasspath() throws IOException {
        Classpath classPath = new Classpath(System.getProperty("java.class.path"));
        this.config.initClasspath(classPath);
        // Set the classpath/classloader
        System.setProperty("java.class.path", classPath.toString());
        ClassLoader classloader = classPath.getClassLoader();
        Thread.currentThread().setContextClassLoader(classloader);
        if (System.getProperty("DEBUG") != null) {
            System.out.println("Startup Classloader: " + classloader.toString());
            System.out.println("Startup Classpath: " + classPath.toString());
        }
    }

    private boolean initStartLoaders() {
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        synchronized (this.loaders) {
            // initialize the loaders
            for (String loaderClassName: config.loaders) {
                if (this.serverStopping) {
                    return false;
                }
                try {
                    Class<?> loaderClass = classloader.loadClass(loaderClassName);
                    StartupLoader loader = (StartupLoader) loaderClass.newInstance();
                    loader.load(config, loaderArgs);
                    this.loaders.add(loader);
                } catch (Exception e) {
                    e.printStackTrace();
                    return false;
                }
            }
            this.loaders.trimToSize();
        }
        return true;
    }

    private String sendSocketCommand(String command) throws IOException, ConnectException {
        Socket socket = new Socket(config.adminAddress, config.adminPort);

        // send the command
        PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
        writer.println(config.adminKey + ":" + command);
        writer.flush();

        // read the reply
        BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        String response = reader.readLine();

        reader.close();

        // close the socket
        writer.close();
        socket.close();

        return response;
    }

    public String shutdown() throws IOException {
        return sendSocketCommand(Start.SHUTDOWN_COMMAND);
    }

    private void shutdownServer() {
        if (serverStopping) return;
        serverStopping = true;
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

    public void start() {
        if (!startStartLoaders()) {
            System.exit(99);
        }
        if (config.shutdownAfterLoad) {
            stopServer();
        }
    }

    /**
     * Returns <code>true</code> if all loaders were started.
     * 
     * @return <code>true</code> if all loaders were started.
     */
    private boolean startStartLoaders() {
        synchronized (this.loaders) {
            // start the loaders
            for (StartupLoader loader: this.loaders) {
                if (this.serverStopping) {
                    return false;
                }
                try {
                    loader.start();
                } catch (StartupException e) {
                    e.printStackTrace();
                    return false;
                }
            }
            serverStarted = true;
        }
        return true;
    }

    public String status() throws IOException {
        String status = null;
        try {
            status = sendSocketCommand(Start.STATUS_COMMAND);
        } catch (ConnectException e) {
            return "Not Running";
        } catch (IOException e) {
            throw e;
        }
        return status;
    }

    public void stopServer() {
        shutdownServer();
        System.exit(0);
    }

    private class AdminPortThread extends Thread {
        private ServerSocket serverSocket = null;

        AdminPortThread() throws IOException {
            super("AdminPortThread");
            this.serverSocket = new ServerSocket(config.adminPort, 1, config.adminAddress);
            setDaemon(false);
        }

        private void processClientRequest(Socket client) throws IOException {
            BufferedReader reader = null;
            PrintWriter writer = null;
            try {
                reader = new BufferedReader(new InputStreamReader(client.getInputStream()));
                String request = reader.readLine();
                writer = new PrintWriter(client.getOutputStream(), true);
                if (request != null && !request.isEmpty() && request.contains(":")) {
                    String key = request.substring(0, request.indexOf(':'));
                    String command = request.substring(request.indexOf(':') + 1);
                    if (key.equals(config.adminKey)) {
                        if (command.equals(Start.SHUTDOWN_COMMAND)) {
                            if (serverStopping) {
                                writer.println("IN-PROGRESS");
                            } else {
                                writer.println("OK");
                                writer.flush();
                                stopServer();
                            }
                            return;
                        } else if (command.equals(Start.STATUS_COMMAND)) {
                            writer.println(serverStopping ? "Stopping" : serverStarted ? "Running" : "Starting");
                            return;
                        }
                    }
                }
                writer.println("FAIL");
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
                    System.out.println("Received connection from - " + clientSocket.getInetAddress() + " : " + clientSocket.getPort());
                    processClientRequest(clientSocket);
                    clientSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
