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
package org.ofbiz.base.container;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.ofbiz.base.start.Start;
import org.ofbiz.base.start.StartupException;
import org.ofbiz.base.start.StartupLoader;
import org.ofbiz.base.util.Debug;

/**
 * ContainerLoader - StartupLoader for the container
 *
 */
public class ContainerLoader implements StartupLoader {

    public static final String module = ContainerLoader.class.getName();
    public static final String CONTAINER_CONFIG = "ofbiz-containers.xml";
    private static boolean loaded = false;

    protected List<Container> loadedContainers = new LinkedList<Container>();
    protected String configFile = null;
    public static Container rmiLoadedContainer = null; // used in Geronimo/WASCE to allow to deregister

    /**
     * @see org.ofbiz.base.start.StartupLoader#load(Start.Config, String[])
     */
    public void load(Start.Config config, String args[]) throws StartupException {
        Debug.logInfo("[Startup] Loading containers...", module);
        loaded = true;

        // get the master container configuration file
        this.configFile = config.containerConfig;

        Collection<ContainerConfig.Container> containers = null;
        try {
            containers = ContainerConfig.getContainers(configFile);
        } catch (ContainerException e) {
            throw new StartupException(e);
        }

        if (containers != null) {
            for (ContainerConfig.Container containerCfg: containers) {
                Container tmpContainer = loadContainer(containerCfg, args);
                loadedContainers.add(tmpContainer);

                // This is only used in case of OFBiz running in Geronimo or WASCE. It allows to use the RMIDispatcher
                if (containerCfg.name.equals("rmi-dispatcher") && configFile.equals("limited-containers.xml")) {
                    try {
                        ContainerConfig.Container.Property initialCtxProp = containerCfg.getProperty("use-initial-context");
                        String useCtx = initialCtxProp == null || initialCtxProp.value == null ? "false" : initialCtxProp.value;
                        if (!useCtx.equalsIgnoreCase("true")) {
                            //system.setProperty("java.security.policy", "client.policy"); maybe used if needed...
                            if (System.getSecurityManager() == null) { // needed by WASCE with a client.policy file.
                                System.setSecurityManager(new java.rmi.RMISecurityManager());
                            }
                            tmpContainer.start();
                            rmiLoadedContainer = tmpContainer; // used in Geronimo/WASCE to allow to deregister
                        }
                    } catch (ContainerException e) {
                        throw new StartupException("Cannot start() " + tmpContainer.getClass().getName(), e);
                    } catch (java.lang.AbstractMethodError e) {
                        throw new StartupException("Cannot start() " + tmpContainer.getClass().getName(), e);
                    }
                }
            }
        }
    }

    /**
     * @see org.ofbiz.base.start.StartupLoader#start()
     */
    public void start() throws StartupException {
        Debug.logInfo("[Startup] Starting containers...", module);

        // start each container object
        for (Container container: loadedContainers) {
            try {
                container.start();
            } catch (ContainerException e) {
                throw new StartupException("Cannot start() " + container.getClass().getName(), e);
            } catch (java.lang.AbstractMethodError e) {
                throw new StartupException("Cannot start() " + container.getClass().getName(), e);
            }
        }
    }

    /**
     * @see org.ofbiz.base.start.StartupLoader#unload()
     */
    public void unload() throws StartupException {
        Debug.logInfo("Shutting down containers", module);
        if (Debug.verboseOn())
            printThreadDump();

        // shutting down in reverse order
        for (int i = loadedContainers.size(); i > 0; i--) {
            Container container = loadedContainers.get(i-1);
            try {
                container.stop();
            } catch (ContainerException e) {
                Debug.logError(e, module);
            }
        }
    }

    private void printThreadDump() {
        Thread currentThread = Thread.currentThread();
        ThreadGroup group = currentThread.getThreadGroup();
        while (group.getParent() != null) {
            group = group.getParent();
        }
        Thread threadArr[] = new Thread[1000];
        group.enumerate(threadArr);

        StringWriter writer = new StringWriter();
        PrintWriter out = new PrintWriter(writer);
        out.println("Thread dump:");
        for (Thread t: threadArr) {
            if (t != null) {
                ThreadGroup g = t.getThreadGroup();
                out.println("Thread: " + t.getName() + " [" + t.getId() + "] @ " + (g != null ? g.getName() : "[none]") + " : " + t.getPriority() + " [" + t.getState().name() + "]");
                out.println("--- Alive: " + t.isAlive() + " Daemon: " + t.isDaemon());
                for (StackTraceElement stack: t.getStackTrace()) {
                    out.println("### " + stack.toString());
                }
            }
        }
        Debug.log(writer.toString(), module);
    }

    private Container loadContainer(ContainerConfig.Container containerCfg, String[] args) throws StartupException {
        // load the container class
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if (loader == null) {
            Debug.logWarning("Unable to get context classloader; using system", module);
            loader = ClassLoader.getSystemClassLoader();
        }
        Class<?> containerClass = null;
        try {
            containerClass = loader.loadClass(containerCfg.className);
        } catch (ClassNotFoundException e) {
            throw new StartupException("Cannot locate container class", e);
        }
        if (containerClass == null) {
            throw new StartupException("Component container class not loaded");
        }

        // create a new instance of the container object
        Container containerObj = null;
        try {
            containerObj = (Container) containerClass.newInstance();
        } catch (InstantiationException e) {
            throw new StartupException("Cannot create " + containerCfg.name, e);
        } catch (IllegalAccessException e) {
            throw new StartupException("Cannot create " + containerCfg.name, e);
        } catch (ClassCastException e) {
            throw new StartupException("Cannot create " + containerCfg.name, e);
        }

        if (containerObj == null) {
            throw new StartupException("Unable to create instance of component container");
        }

        // initialize the container object
        try {
            containerObj.init(args, configFile);
        } catch (ContainerException e) {
            throw new StartupException("Cannot init() " + containerCfg.name, e);
        } catch (java.lang.AbstractMethodError e) {
            throw new StartupException("Cannot init() " + containerCfg.name, e);
        }

        return containerObj;
    }

    public static synchronized Container loadContainers(String config, String[] args) throws StartupException {
        if (!loaded) {
            ContainerLoader loader = new ContainerLoader();
            Start.Config cfg = new Start.Config();
            cfg.containerConfig = config == null ? "limited-containers.xml" : config;
            loader.load(cfg, args);
            if (rmiLoadedContainer != null) { // used in Geronimo/WASCE to allow to deregister
                return rmiLoadedContainer;
            }
        }
        return null;
    }
}
