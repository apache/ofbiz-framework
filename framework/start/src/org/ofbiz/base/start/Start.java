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
import java.lang.reflect.Method;
import java.net.ConnectException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * Start - OFBiz Container(s) Startup Class
 *
 */
public class Start implements Runnable {

    private static final String SHUTDOWN_COMMAND = "SHUTDOWN";
    private static final String STATUS_COMMAND = "STATUS";

    private static String getConfigFileName(String command) {
        // default command is "start"
        if (command == null || command.trim().length() == 0) {
            command = "start";
        }
        // strip off the leading dash
        if (command.startsWith("-")) {
            command = command.substring(1);
        }
        // shutdown & status hack
        if (command.equalsIgnoreCase("shutdown")) {
            command = "start";
        } else if (command.equalsIgnoreCase("status")) {
            command = "start";
        }
        return "org/ofbiz/base/start/" + command + ".properties";
    }

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

    private ClassLoader classloader = null;
    private Classpath classPath = new Classpath(System.getProperty("java.class.path"));
    private Config config = null;
    private String[] loaderArgs = null;
    private List<StartupLoader> loaders = new ArrayList<StartupLoader>();
    private boolean serverRunning = true;
    private ServerSocket serverSocket = null;
    private boolean serverStarted = false;
    private boolean serverStopping = false;
    private Thread serverThread = null;

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
        String firstArg = args.length > 0 ? args[0] : "";
        String cfgFile = Start.getConfigFileName(firstArg);

        this.config = new Config();

        // read the default properties first
        config.readConfig(cfgFile);

        // parse the startup arguments
        if (args.length > 0) {
            this.loaderArgs = new String[args.length];
            System.arraycopy(args, 0, this.loaderArgs, 0, this.loaderArgs.length);
        }

        if (fullInit) {
            // initialize the classpath
            initClasspath();

            // initialize the log directory
            initLogDirectory();

            // initialize the listener thread
            initListenerThread();

            // set the shutdown hook
            if (config.useShutdownHook) {
                setShutdownHook();
            } else {
                System.out.println("Shutdown hook disabled");
            }

            // initialize the startup loaders
            initStartLoaders();
        }
    }

    private void initClasspath() throws IOException {
        // load tools.jar
        if (config.toolsJar != null) {
            classPath.addComponent(config.toolsJar);
        }

        // load comm.jar
        if (config.commJar != null) {
            classPath.addComponent(config.commJar);
        }

        // add OFBIZ_HOME to CP & load libs
        classPath.addClasspath(config.ofbizHome);
        loadLibs(config.ofbizHome, false);

        // load the lib directory
        if (config.baseLib != null) {
            loadLibs(config.baseLib, true);
        }

        // load the ofbiz-base.jar
        if (config.baseJar != null) {
            classPath.addComponent(config.baseJar);
        }

        // load the base schema directory
        if (config.baseDtd != null) {
            classPath.addComponent(config.baseDtd);
        }

        // load the config directory
        if (config.baseConfig != null) {
            classPath.addComponent(config.baseConfig);
        }

        classPath.instrument(config.instrumenterFile, config.instrumenterClassName);
        // set the classpath/classloader
        System.setProperty("java.class.path", classPath.toString());
        this.classloader = classPath.getClassLoader();
        Thread.currentThread().setContextClassLoader(classloader);
        if (System.getProperty("DEBUG") != null) {
            System.out.println("Startup Classloader: " + classloader.toString());
            System.out.println("Startup Classpath: " + classPath.toString());
        }
    }

    private void initListenerThread() throws IOException {
        if (config.adminPort > 0) {
            this.serverSocket = new ServerSocket(config.adminPort, 1, config.adminAddress);
            this.serverThread = new Thread(this, this.toString());
            this.serverThread.setDaemon(false);
            System.out.println("Admin socket configured on - " + config.adminAddress + ":" + config.adminPort);
            this.serverThread.start();
        } else {
            System.out.println("Admin socket not configured; set to port 0");
        }
    }

    private void initLogDirectory() {
        // stat the log directory
        boolean createdDir = false;
        File logDir = new File(config.logDir);
        if (!logDir.exists()) {
            logDir.mkdir();
            createdDir = true;
        }

        if (createdDir) {
            System.out.println("Created OFBiz log dir [" + logDir.getAbsolutePath() + "]");
        }
    }

    private void initStartLoaders() {
        synchronized (this.loaders) {
            // initialize the loaders
            for (String loaderClassName: config.loaders) {
                if (this.serverStopping) {
                    return;
                }
                try {
                    Class<?> loaderClass = classloader.loadClass(loaderClassName);
                    StartupLoader loader = (StartupLoader) loaderClass.newInstance();
                    loader.load(config, loaderArgs);
                    this.loaders.add(loader);
                } catch (Exception e) {
                    e.printStackTrace();
                    System.exit(99);
                }
            }
        }
    }

    private void loadLibs(String path, boolean recurse) throws IOException {
        File libDir = new File(path);
        if (libDir.exists()) {
            File files[] = libDir.listFiles();
            for (File file: files) {
                String fileName = file.getName();
                // FIXME: filter out other files?
                if (file.isDirectory() && !"CVS".equals(fileName) && !".svn".equals(fileName) && recurse) {
                    loadLibs(file.getCanonicalPath(), recurse);
                } else if (fileName.endsWith(".jar") || fileName.endsWith(".zip")) {
                    classPath.addComponent(file);
                }
            }
        }
    }

    private void processClientRequest(Socket client) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream()));
        String request = reader.readLine();

        PrintWriter writer = new PrintWriter(client.getOutputStream(), true);
        writer.println(processRequest(request, client));
        writer.flush();

        writer.close();
        reader.close();
    }

    private String processRequest(String request, Socket client) {
        if (request != null) {
            String key = request.substring(0, request.indexOf(':'));
            String command = request.substring(request.indexOf(':') + 1);
            if (!key.equals(config.adminKey)) {
                return "FAIL";
            } else {
                if (command.equals(Start.SHUTDOWN_COMMAND)) {
                    if (serverStopping) return "IN-PROGRESS";
                    Thread t = new Thread() {
                        @Override
                        public void run() {
                            shutdownServer();
                        }
                    };
                    t.start();
                    return "OK";
                } else if (command.equals(Start.STATUS_COMMAND)) {
                    return serverStopping ? "Stopping" : serverStarted ? "Running" : "Starting";
                }
                return "FAIL";
            }
        } else {
            return "FAIL";
        }
    }

    public void run() {
        while (serverRunning) {
            try {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Received connection from - " + clientSocket.getInetAddress() + " : " + clientSocket.getPort());
                processClientRequest(clientSocket);
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        shutdownServer();
        System.exit(0);
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

    private void setShutdownHook() {
        try {
            Method shutdownHook = java.lang.Runtime.class.getMethod("addShutdownHook", new Class[]{java.lang.Thread.class});
            Thread hook = new Thread() {
                @Override
                public void run() {
                    setName("OFBiz_Shutdown_Hook");
                    shutdownServer();
                    // Try to avoid JVM crash
                    try {
                        Thread.sleep(1000);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            };

            shutdownHook.invoke(Runtime.getRuntime(), new Object[]{hook});
        } catch (Exception e) {
            // VM Does not support shutdown hook
            e.printStackTrace();
        }
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
        serverRunning = false;
    }

    public void start() {
        startServer();
        if (config.shutdownAfterLoad) {
            shutdownServer();
            System.exit(0);
        }
    }

    private void startServer() {
        // start the startup loaders
        if (!startStartLoaders()) {
            System.exit(99);
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

    public void stop() {
        shutdownServer();
    }
}
