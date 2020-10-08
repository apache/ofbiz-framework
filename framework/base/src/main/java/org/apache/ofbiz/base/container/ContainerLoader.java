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
package org.apache.ofbiz.base.container;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

import org.apache.ofbiz.base.component.ComponentConfig;
import org.apache.ofbiz.base.start.Config;
import org.apache.ofbiz.base.start.StartupCommand;
import org.apache.ofbiz.base.start.StartupException;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilValidate;

/**
 * An object that loads containers (background processes).
 *
 * Normally, instances of this class are created by OFBiz startup code, and
 * client code should not create instances of this class. Client code is
 * responsible for making sure containers are shut down properly.
 * <p>
 * When OFBiz starts, the main thread will create the {@code ContainerLoader} instance and
 * then call the loader's {@code load} method.
 * After this instance has been created and initialized, the main thread will call the
 * {@code start} method. When OFBiz shuts down, a separate shutdown thread will call
 * the {@code unload} method.
 */
public class ContainerLoader {

    private static final String MODULE = ContainerLoader.class.getName();

    private final Deque<Container> loadedContainers = new LinkedList<>();

    /**
     * Starts the containers.
     * @param config Startup config
     * @param ofbizCommands Command-line arguments
     * @throws StartupException If an error was encountered. Throwing this exception
     * will halt loader loading, so it should be thrown only when OFBiz can't
     * operate without it.
     */
    public synchronized void load(Config config, List<StartupCommand> ofbizCommands) throws StartupException {
        // Load mandatory container providing access to containers from components.
        try {
            ComponentContainer cc = new ComponentContainer();
            cc.init(ofbizCommands, "component-container", null);
            loadedContainers.add(cc);
        } catch (ContainerException e) {
            throw new StartupException("Cannot init() component-container", e);
        }

        // Load containers defined in components.
        Debug.logInfo("[Startup] Loading containers...", MODULE);
        loadedContainers.addAll(loadContainersFromConfigurations(config.getLoaders(), ofbizCommands));

        // Start all containers loaded from above steps
        startLoadedContainers();
    }

    /**
     * Checks if two collections have an intersection or are both empty.
     * @param a the first collection which can be {@code null}
     * @param b the second collection which can be {@code null}
     * @return {@code true} if {@code a} and {@code b} have an intersection or are both empty.
     */
    private static boolean intersects(Collection<?> a, Collection<?> b) {
        return UtilValidate.isEmpty(a) && UtilValidate.isEmpty(b)
                || !Collections.disjoint(a, b);
    }

    /**
     * Loads the available containers which are matching the configured loaders.
     * @param loaders  the collection of loaders to match
     * @param ofbizCommands  the parsed commands line arguments used by the containers
     * @return a list of loaded containers.
     * @throws StartupException when a container fails to load.
     */
    private static List<Container> loadContainersFromConfigurations(Collection<String> loaders,
            List<StartupCommand> ofbizCommands) throws StartupException {
        List<Container> loadContainers = new ArrayList<>();
        for (ContainerConfig.Configuration containerCfg : ComponentConfig.getAllConfigurations()) {
            if (intersects(containerCfg.loaders(), loaders)) {
                Debug.logInfo("Loading container: " + containerCfg.name(), MODULE);
                Container tmpContainer = loadContainer(containerCfg, ofbizCommands);
                loadContainers.add(tmpContainer);
                Debug.logInfo("Loaded container: " + containerCfg.name(), MODULE);
            }
        }
        return loadContainers;
    }


    private static Container loadContainer(ContainerConfig.Configuration containerCfg, List<StartupCommand> ofbizCommands)
            throws StartupException {
        // load the container class
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        Class<?> containerClass;
        try {
            containerClass = loader.loadClass(containerCfg.className());
        } catch (ClassNotFoundException e) {
            throw new StartupException("Cannot locate container class", e);
        }
        if (containerClass == null) {
            throw new StartupException("Component container class not loaded");
        }

        // create a new instance of the container object
        Container containerObj;
        try {
            containerObj = (Container) containerClass.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new StartupException("Cannot create " + containerCfg.name(), e);
        }
        if (containerObj == null) {
            throw new StartupException("Unable to create instance of component container");
        }

        // initialize the container object
        try {
            containerObj.init(ofbizCommands, containerCfg.name(), null);
        } catch (ContainerException e) {
            throw new StartupException("Cannot init() " + containerCfg.name(), e);
        }

        return containerObj;
    }

    private void startLoadedContainers() throws StartupException {
        Debug.logInfo("[Startup] Starting containers...", MODULE);
        for (Container container: loadedContainers) {
            Debug.logInfo("Starting container " + container.getName(), MODULE);
            try {
                container.start();
            } catch (ContainerException e) {
                throw new StartupException("Cannot start() " + container.getClass().getName(), e);
            }
            Debug.logInfo("Started container " + container.getName(), MODULE);
        }
    }

    /**
     * Stops the containers.
     */
    public synchronized void unload() {
        Debug.logInfo("Shutting down containers", MODULE);
        loadedContainers.descendingIterator().forEachRemaining(loadedContainer -> {
            Debug.logInfo("Stopping container " + loadedContainer.getName(), MODULE);
            try {
                loadedContainer.stop();
            } catch (ContainerException e) {
                Debug.logError(e, MODULE);
            }
            Debug.logInfo("Stopped container " + loadedContainer.getName(), MODULE);
        });
    }
}
