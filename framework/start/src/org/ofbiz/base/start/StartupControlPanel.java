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
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.ofbiz.base.start.Start.ServerState;

final class StartupControlPanel {

    enum OfbizSocketCommand {
        SHUTDOWN, STATUS, FAIL
    }

    static Config init(List<StartupCommand> ofbizCommands) throws StartupException {
        loadGlobalOfbizSystemProperties("ofbiz.system.props");
        try {
            return new Config(ofbizCommands);
        } catch (StartupException e) {
            throw new StartupException("Could not fetch config instance", e);
        }
    }

    static String status(Config config) throws StartupException {
        try {
            return sendSocketCommand(OfbizSocketCommand.STATUS, config);
        } catch (ConnectException e) {
            return "Not Running";
        } catch (IOException e) {
            throw new StartupException(e);
        }
    }

    static String shutdown(Config config) throws StartupException {
        try {
            return sendSocketCommand(OfbizSocketCommand.SHUTDOWN, config);
        } catch (Exception e) {
            throw new StartupException(e);
        }
    }

    static void start(Config config,
            AtomicReference<ServerState> serverState,
            List<StartupCommand> ofbizCommands) throws StartupException {
        
        List<String> loaderArgs = StartupCommandUtil.adaptStartupCommandsToLoaderArgs(ofbizCommands);
        ArrayList<StartupLoader> loaders = new ArrayList<StartupLoader>();
        Thread adminPortThread = null;

        //create log dir
        File logDir = new File(config.logDir);
        if (!logDir.exists()) {
            if (logDir.mkdir()) {
                System.out.println("Created OFBiz log dir [" + logDir.getAbsolutePath() + "]");
            }
        }

        // create the listener thread
        if (config.adminPort > 0) {
            adminPortThread = new AdminPortThread(loaders, serverState, config);
            adminPortThread.start();
        } else {
            System.out.println("Admin socket not configured; set to port 0");
        }

        // set the shutdown hook
        if (config.useShutdownHook) {
            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    StartupControlPanel.shutdownServer(loaders, serverState, this);
                }
            });
        } else {
            System.out.println("Shutdown hook disabled");
        }

        // load and start the startup loaders
        loadStartupLoaders(config, loaderArgs, loaders, serverState);
        startStartupLoaders(loaders, serverState);

        // execute shutdown if applicable
        if (config.shutdownAfterLoad) {
            StartupControlPanel.stopServer(loaders, serverState, adminPortThread);
        }
    }

    static void stopServer(ArrayList<StartupLoader> loaders, AtomicReference<ServerState> serverState, Thread adminPortThread) {
        shutdownServer(loaders, serverState, adminPortThread);
        System.exit(0);
    }

    private static void shutdownServer(ArrayList<StartupLoader> loaders, AtomicReference<ServerState> serverState, Thread adminPortThread) {
        ServerState currentState;
        do {
            currentState = serverState.get();
            if (currentState == ServerState.STOPPING) {
                return;
            }
        } while (!serverState.compareAndSet(currentState, ServerState.STOPPING));
        // The current thread was the one that successfully changed the state;
        // continue with further processing.
        synchronized (loaders) {
            // Unload in reverse order
            for (int i = loaders.size(); i > 0; i--) {
                StartupLoader loader = loaders.get(i - 1);
                try {
                    loader.unload();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        if (adminPortThread != null && adminPortThread.isAlive()) {
            adminPortThread.interrupt();
        }
    }

    private static void loadGlobalOfbizSystemProperties(String globalOfbizPropertiesFileName) throws StartupException {
        String systemProperties = System.getProperty(globalOfbizPropertiesFileName);
        if (systemProperties != null) {
            FileInputStream stream = null;
            try {
                stream = new FileInputStream(systemProperties);
                System.getProperties().load(stream);
                stream.close();
            } catch (IOException e) {
                throw new StartupException("Couldn't load global system props", e);
            }
        }
    }

    private static String sendSocketCommand(OfbizSocketCommand socketCommand, Config config) throws IOException {
        String response = "OFBiz is Down";
        try {
            Socket socket = new Socket(config.adminAddress, config.adminPort);
            // send the command
            PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
            writer.println(config.adminKey + ":" + socketCommand);
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

    private static NativeLibClassLoader createClassLoader(Config config) throws IOException {
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

    private static void loadStartupLoaders(Config config, 
            List<String> loaderArgs, ArrayList<StartupLoader> loaders,
            AtomicReference<ServerState> serverState) throws StartupException {

        NativeLibClassLoader classloader = null;
        try {
            classloader = createClassLoader(config);
        } catch (IOException e) {
            throw new StartupException("Couldn't create NativeLibClassLoader", e);
        }
        Thread.currentThread().setContextClassLoader(classloader);
        String[] argsArray = loaderArgs.toArray(new String[loaderArgs.size()]);
        synchronized (loaders) {
            for (Map<String, String> loaderMap : config.loaders) {
                if (serverState.get() == ServerState.STOPPING) {
                    return;
                }
                try {
                    String loaderClassName = loaderMap.get("class");
                    Class<?> loaderClass = classloader.loadClass(loaderClassName);
                    StartupLoader loader = (StartupLoader) loaderClass.newInstance();
                    loaders.add(loader); // add before loading, so unload can occur if error during loading
                    loader.load(config, argsArray);
                } catch (ReflectiveOperationException e) {
                    throw new StartupException(e.getMessage(), e);
                }
            }
            loaders.trimToSize();
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

    private static void startStartupLoaders(List<StartupLoader> loaders, 
            AtomicReference<ServerState> serverState) throws StartupException {

        synchronized (loaders) {
            // start the loaders
            for (StartupLoader loader : loaders) {
                if (serverState.get() == ServerState.STOPPING) {
                    return;
                } else {
                    loader.start();
                }
            }
        }
        if(!serverState.compareAndSet(ServerState.STARTING, ServerState.RUNNING)) {
            throw new StartupException("Error during start");
        }
    }
}
