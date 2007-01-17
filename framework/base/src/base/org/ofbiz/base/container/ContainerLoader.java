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

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.ofbiz.base.start.StartupException;
import org.ofbiz.base.start.StartupLoader;
import org.ofbiz.base.start.Start;
import org.ofbiz.base.util.Debug;

/**
 * ContainerLoader - StartupLoader for the container
 *
 */
public class ContainerLoader implements StartupLoader {
    
    public static final String module = ContainerLoader.class.getName();
    public static final String CONTAINER_CONFIG = "ofbiz-containers.xml";
    private static boolean loaded = false;

    protected List loadedContainers = new LinkedList();
    protected String configFile = null;

    /**
     * @see org.ofbiz.base.start.StartupLoader#load(Start.Config, String[])
     */
    public void load(Start.Config config, String args[]) throws StartupException {
        Debug.logInfo("[Startup] Loading containers...", module);
        loaded = true;
        
        // get the master container configuration file
        this.configFile = config.containerConfig;
        
        Collection containers = null;
        try {
            containers = ContainerConfig.getContainers(configFile);
        } catch (ContainerException e) {            
            throw new StartupException(e);
        }

        if (containers != null) {
            Iterator i = containers.iterator();
            while (i.hasNext()) {
                ContainerConfig.Container containerCfg = (ContainerConfig.Container) i.next();                
                loadedContainers.add(loadContainer(containerCfg, args));
            }
        }
    }

    /**
     * @see org.ofbiz.base.start.StartupLoader#start()
     */
    public void start() throws StartupException {
        Debug.logInfo("[Startup] Starting containers...", module);

        // start each container object
        for (int i = 0; i < loadedContainers.size(); i++) {
            Container container = (Container) loadedContainers.get(i);
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

        // shutting down in reverse order
        for (int i = loadedContainers.size(); i > 0; i--) {
            Container container = (Container) loadedContainers.get(i-1);
            try {
                container.stop();
            } catch (ContainerException e) {
                Debug.logError(e, module);
            }
        }
    }

    private Container loadContainer(ContainerConfig.Container containerCfg, String[] args) throws StartupException {
        // load the container class
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if (loader == null) {
            Debug.logWarning("Unable to get context classloader; using system", module);
            loader = ClassLoader.getSystemClassLoader();
        }
        Class containerClass = null;
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

    public static synchronized boolean loadContainers(String config, String[] args) throws StartupException {
        if (!loaded) {
            ContainerLoader loader = new ContainerLoader();
            Start.Config cfg = new Start.Config();
            cfg.containerConfig = config == null ? "limited-containers.xml" : config;
            loader.load(cfg, args);
            return true;
        }
        return false;
    }
}
