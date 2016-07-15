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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.ofbiz.base.start.Start.ServerState;

/**
 * The StartupControlPanel controls OFBiz by executing high level
 * functions such as init, start and stop on the server. The bulk
 * of the startup sequence logic resides in this class.
 */
final class StartupControlPanel {

    /**
     * Initialize OFBiz by:
     * - setting high level JVM and OFBiz system properties
     * - creating a Config object holding startup configuration parameters
     *
     * @param ofbizCommands: commands passed by the user to OFBiz on start
     * @return config: OFBiz configuration
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

        List<StartupLoader> loaders = new ArrayList<StartupLoader>();
        List<String> loaderArgs = StartupCommandUtil.adaptStartupCommandsToLoaderArgs(ofbizCommands);
        Thread adminServer = createAdminServer(config, serverState, loaders);
        Classpath classPath = createClassPath(config);
        NativeLibClassLoader classLoader = createAndSetContextClassLoader(config, classPath);

        createLogDirectoryIfMissing(config);
        createRuntimeShutdownHook(config, loaders, serverState);
        loadStartupLoaders(config, loaders, loaderArgs, serverState, classLoader);
        startStartupLoaders(loaders, serverState);
        executeShutdownAfterLoadIfConfigured(config, loaders, serverState, adminServer);
    }

    /**
     * Shutdown the OFBiz server. This method is invoked in one of the
     * following ways:
     *
     * - Manually if requested by the client AdminClient
     * - Automatically if Config.shutdownAfterLoad is set to true
     */
    static void stop(List<StartupLoader> loaders, AtomicReference<ServerState> serverState, Thread adminServer) {
        shutdownServer(loaders, serverState, adminServer);
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
     * @param e: The startup exception that cannot / should not be handled
     *   except by terminating the system
     */
    static void fullyTerminateSystem(StartupException e) {
        e.printStackTrace();
        System.exit(1);
    }

    private static void shutdownServer(List<StartupLoader> loaders, AtomicReference<ServerState> serverState, Thread adminServer) {
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
        if (adminServer != null && adminServer.isAlive()) {
            adminServer.interrupt();
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

    private static Thread createAdminServer(
            Config config,
            AtomicReference<ServerState> serverState,
            List<StartupLoader> loaders) throws StartupException {

        Thread adminServer = null;
        if (config.adminPort > 0) {
            adminServer = new AdminServer(loaders, serverState, config);
            adminServer.start();
        } else {
            System.out.println("Admin socket not configured; set to port 0");
        }
        return adminServer;
    }

    private static Classpath createClassPath(Config config) throws StartupException {
        Classpath classPath = new Classpath();
        try {
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
        } catch (IOException e) {
            throw new StartupException("Cannot create classpath", e);
        }
        return classPath;
    }

    private static NativeLibClassLoader createAndSetContextClassLoader(Config config, Classpath classPath) throws StartupException {
        ClassLoader parent = Thread.currentThread().getContextClassLoader();
        NativeLibClassLoader classloader = null;
        try {
            classloader = new NativeLibClassLoader(classPath.getUrls(), parent);
            classloader.addNativeClassPath(System.getProperty("java.library.path"));
            for (File folder : classPath.getNativeFolders()) {
                classloader.addNativeClassPath(folder);
            }
        } catch (IOException e) {
            throw new StartupException("Couldn't create NativeLibClassLoader", e);
        }
        Thread.currentThread().setContextClassLoader(classloader);
        return classloader;
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
            List<StartupLoader> loaders,
            AtomicReference<ServerState> serverState) {

        if (config.useShutdownHook) {
            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    shutdownServer(loaders, serverState, this);
                }
            });
        } else {
            System.out.println("Shutdown hook disabled");
        }
    }

    private static void loadStartupLoaders(Config config, 
            List<StartupLoader> loaders,
            List<String> loaderArgs,
            AtomicReference<ServerState> serverState,
            NativeLibClassLoader classloader) throws StartupException {

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

    private static void executeShutdownAfterLoadIfConfigured(
            Config config,
            List<StartupLoader> loaders,
            AtomicReference<ServerState> serverState,
            Thread adminServer) {

        if (config.shutdownAfterLoad) {
            stop(loaders, serverState, adminServer);
        }
    }
}
