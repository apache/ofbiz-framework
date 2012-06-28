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
import java.net.URL;
import java.util.Collection;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.ofbiz.base.component.ComponentConfig;
import org.ofbiz.base.start.Config;
import org.ofbiz.base.start.StartupException;
import org.ofbiz.base.start.StartupLoader;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilValidate;

/**
 * An object that loads containers (background processes).
 * 
 * <p>Normally, instances of this class are created by OFBiz startup code, and
 * client code should not create instances of this class. Client code is
 * responsible for making sure containers are shut down properly. </p>
 *
 */
public class ContainerLoader implements StartupLoader {

    public static final String module = ContainerLoader.class.getName();
    private static Map<String, Container> containerMap = new ConcurrentHashMap<String, Container>();

    /**
     * Returns a <code>Container</code> that has the specified name. Returns
     * <code>null</code> if the specified container was not loaded. If more than one
     * instance of the container was loaded, then the last instance that was loaded is
     * returned. The returned <code>Container</code> will be initialized, but there is no
     * guarantee that it will be in the running state.
     * 
     * @param containerName
     *            The name of the container.
     * @return A <code>Container</code> that has the specified name.
     */
    public static Container getContainer(String containerName) {
        return containerMap.get(containerName);
    }

    private String configFile = null;
    private final List<Container> loadedContainers = new LinkedList<Container>();
    private boolean unloading = false;
    private boolean loaded = false;

    /**
     * @see org.ofbiz.base.start.StartupLoader#load(Config, String[])
     */
    public synchronized void load(Config config, String args[]) throws StartupException {
        if (this.loaded || this.unloading) {
            return;
        }
        Debug.logInfo("[Startup] Loading containers...", module);
        this.loadedContainers.clear();
        // get this loader's configuration file
        this.configFile = config.containerConfig;
        Collection<ContainerConfig.Container> containers = null;
        try {
            containers = ContainerConfig.getContainers(configFile);
            if (UtilValidate.isEmpty(containers)) {
                throw new StartupException("No containers loaded; problem with configuration");
            }
        } catch (ContainerException e) {
            throw new StartupException(e);
        }
        for (ContainerConfig.Container containerCfg : containers) {
            if (this.unloading) {
                return;
            }
            Container tmpContainer = loadContainer(containerCfg, args);
            this.loadedContainers.add(tmpContainer);
            containerMap.put(containerCfg.name, tmpContainer);

            // TODO: Put container-specific code in the container.
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
                    }
                } catch (ContainerException e) {
                    throw new StartupException("Cannot start() " + tmpContainer.getClass().getName(), e);
                } catch (java.lang.AbstractMethodError e) {
                    throw new StartupException("Cannot start() " + tmpContainer.getClass().getName(), e);
                }
            }
        }
        if (this.unloading) {
            return;
        }
        List<ContainerConfig.Container> containersDefinedInComponents = ComponentConfig.getAllContainers();
        for (ContainerConfig.Container containerCfg: containersDefinedInComponents) {
            Container tmpContainer = loadContainer(containerCfg, args);
            this.loadedContainers.add(tmpContainer);
            containerMap.put(containerCfg.name, tmpContainer);
        }
        // Get hot-deploy container configuration files
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        Enumeration<URL> resources;
        try {
            resources = loader.getResources("hot-deploy-containers.xml");
            while (resources.hasMoreElements() && !this.unloading) {
                URL xmlUrl = resources.nextElement();
                Debug.logInfo("Loading hot-deploy containers from " + xmlUrl, module);
                Collection<ContainerConfig.Container> hotDeployContainers = ContainerConfig.getContainers(xmlUrl);
                for (ContainerConfig.Container containerCfg : hotDeployContainers) {
                    if (this.unloading) {
                        return;
                    }
                    Container tmpContainer = loadContainer(containerCfg, args);
                    this.loadedContainers.add(tmpContainer);
                    containerMap.put(containerCfg.name, tmpContainer);
                }
            }
        } catch (Exception e) {
            Debug.logError(e, "Could not load hot-deploy-containers.xml", module);
            throw new StartupException(e);
        }
        loaded = true;
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
        Debug.logInfo(writer.toString(), module);
    }

    /**
     * @see org.ofbiz.base.start.StartupLoader#start()
     */
    public synchronized void start() throws StartupException {
        if (!this.loaded || this.unloading) {
            throw new IllegalStateException("start() called on unloaded containers");
        }
        Debug.logInfo("[Startup] Starting containers...", module);
        // start each container object
        for (Container container: this.loadedContainers) {
            if (this.unloading) {
                return;
            }
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
        if (!this.unloading) {
            this.unloading = true;
            synchronized (this) {
                Debug.logInfo("Shutting down containers", module);
                if (Debug.verboseOn()) {
                    printThreadDump();
                }
                // shutting down in reverse order
                for (int i = this.loadedContainers.size(); i > 0; i--) {
                    Container container = this.loadedContainers.get(i-1);
                    try {
                        container.stop();
                    } catch (ContainerException e) {
                        Debug.logError(e, module);
                    }
                }
            }
        }
    }
}
