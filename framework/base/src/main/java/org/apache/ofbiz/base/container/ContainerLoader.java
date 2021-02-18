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
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.ofbiz.base.component.ComponentConfig;
import org.apache.ofbiz.base.start.Config;
import org.apache.ofbiz.base.start.StartupCommand;
import org.apache.ofbiz.base.start.StartupException;
import org.apache.ofbiz.base.start.StartupLoader;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilValidate;


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

    private final List<Container> loadedContainers = new LinkedList<>();

    /**
     * @see org.apache.ofbiz.base.start.StartupLoader#load(Config, List)
     */
    @Override
    public synchronized void load(Config config, List<StartupCommand> ofbizCommands) throws StartupException {

        // loaders defined in startup (e.g. main, test, load-data, etc ...)
        List<String> loaders = config.loaders;

        // load containers defined in ofbiz-containers.xml
        Debug.logInfo("[Startup] Loading containers...", module);
        List<ContainerConfig.Configuration> ofbizContainerConfigs = filterContainersHavingMatchingLoaders(
                loaders, retrieveOfbizContainers(config.containerConfig));
        loadedContainers.addAll(loadContainersFromConfigurations(ofbizContainerConfigs, config, ofbizCommands));

        // load containers defined in components
        Debug.logInfo("[Startup] Loading component containers...", module);
        List<ContainerConfig.Configuration> componentContainerConfigs = filterContainersHavingMatchingLoaders(
                loaders, ComponentConfig.getAllConfigurations());
        loadedContainers.addAll(loadContainersFromConfigurations(componentContainerConfigs, config, ofbizCommands));

        // Start all containers loaded from above steps
        startLoadedContainers();
    }

    private Collection<ContainerConfig.Configuration> retrieveOfbizContainers(String configFile) throws StartupException {
        try {
            return ContainerConfig.getConfigurations(configFile);
        } catch (ContainerException e) {
            throw new StartupException(e);
        }
    }

    private List<ContainerConfig.Configuration> filterContainersHavingMatchingLoaders(List<String> loaders,
            Collection<ContainerConfig.Configuration> containerConfigs) {
        return containerConfigs.stream()
                .filter(containerCfg ->
                    UtilValidate.isEmpty(containerCfg.loaders) &&
                    UtilValidate.isEmpty(loaders) ||
                    containerCfg.loaders.stream().anyMatch(loader -> loaders.contains(loader)))
                .collect(Collectors.toList());
    }

    private List<Container> loadContainersFromConfigurations(List<ContainerConfig.Configuration> containerConfigs,
            Config config, List<StartupCommand> ofbizCommands) throws StartupException {

        List<Container> loadContainers = new ArrayList<>();
        for (ContainerConfig.Configuration containerCfg : containerConfigs) {
            Debug.logInfo("Loading container: " + containerCfg.name, module);
            Container tmpContainer = loadContainer(config.containerConfig, containerCfg, ofbizCommands);
            loadContainers.add(tmpContainer);
            Debug.logInfo("Loaded container: " + containerCfg.name, module);
        }
        return loadContainers;
    }

    private Container loadContainer(String configFile,
            ContainerConfig.Configuration containerCfg,
            List<StartupCommand> ofbizCommands) throws StartupException {
        // load the container class
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        Class<?> containerClass;
        try {
            containerClass = loader.loadClass(containerCfg.className);
        } catch (ClassNotFoundException e) {
            throw new StartupException("Cannot locate container class", e);
        }
        if (containerClass == null) {
            throw new StartupException("Component container class not loaded");
        }

        // create a new instance of the container object
        Container containerObj;
        try {
            containerObj = (Container) containerClass.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new StartupException("Cannot create " + containerCfg.name, e);
        }
        if (containerObj == null) {
            throw new StartupException("Unable to create instance of component container");
        }

        // initialize the container object
        try {
            containerObj.init(ofbizCommands, containerCfg.name, configFile);
        } catch (ContainerException e) {
            throw new StartupException("Cannot init() " + containerCfg.name, e);
        }

        return containerObj;
    }

    private void startLoadedContainers() throws StartupException {
        Debug.logInfo("[Startup] Starting containers...", module);
        for (Container container: loadedContainers) {
            Debug.logInfo("Starting container " + container.getName(), module);
            try {
                container.start();
            } catch (ContainerException e) {
                throw new StartupException("Cannot start() " + container.getClass().getName(), e);
            }
            Debug.logInfo("Started container " + container.getName(), module);
        }
    }

    /**
     * @see org.apache.ofbiz.base.start.StartupLoader#unload()
     */
    @Override
    public synchronized void unload() throws StartupException {
        Debug.logInfo("Shutting down containers", module);

        List<Container> reversedContainerList = new ArrayList<>(loadedContainers);
        Collections.reverse(reversedContainerList);

        for(Container loadedContainer : reversedContainerList) {
            Debug.logInfo("Stopping container " + loadedContainer.getName(), module);
            try {
                loadedContainer.stop();
            } catch (ContainerException e) {
                Debug.logError(e, module);
            }
            Debug.logInfo("Stopped container " + loadedContainer.getName(), module);
        }
    }
}
