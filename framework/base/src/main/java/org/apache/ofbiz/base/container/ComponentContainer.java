/*
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
 */
package org.apache.ofbiz.base.container;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.ofbiz.base.component.ComponentConfig;
import org.apache.ofbiz.base.component.ComponentException;
import org.apache.ofbiz.base.component.ComponentLoaderConfig;
import org.apache.ofbiz.base.component.ComponentLoaderConfig.ComponentDef;
import org.apache.ofbiz.base.start.Start;
import org.apache.ofbiz.base.start.StartupCommand;
import org.apache.ofbiz.base.util.Debug;

/**
 * ComponentContainer - StartupContainer implementation for Components
 *
 * The purpose of this container is to load the classpath for all components
 * defined in OFBiz. This container must run before any other containers to
 * allow components to access any necessary resources. Furthermore, the
 * ComponentContainer also builds up the <code>ComponentConfigCache</code>
 * defined in <code>ComponentConfig</code> to keep track of loaded components
 *
 */
public class ComponentContainer implements Container {

    public static final String module = ComponentContainer.class.getName();

    private String name;
    private final AtomicBoolean loaded = new AtomicBoolean(false);

    @Override
    public void init(List<StartupCommand> ofbizCommands, String name, String configFile) throws ContainerException {
        init(name, Start.getInstance().getConfig().ofbizHome);
    }

    /**
     * Loads components found in a directory.
     *
     * @param name  the name of this container
     * @param ofbizHome  the directory where to search for components
     * @throws ContainerException when components are already loaded or when failing to load them.
     */
    void init(String name, Path ofbizHome) throws ContainerException {
        if (!loaded.compareAndSet(false, true)) {
            throw new ContainerException("Components already loaded, cannot start");
        }
        this.name = name;

        // load the components from framework/base/config/component-load.xml (root components)
        try {
            for (ComponentDef def: ComponentLoaderConfig.getRootComponents()) {
                loadComponent(ofbizHome, def);
            }
            ComponentConfig.sortDependencies();
        } catch (IOException | ComponentException e) {
            throw new ContainerException(e);
        }
        String fmt = "Added class path for component : [%s]";
        List<Classpath> componentsClassPath = ComponentConfig.components()
                .peek(cmpnt -> Debug.logInfo(String.format(fmt, cmpnt.getComponentName()), module))
                .map(cmpnt -> buildClasspathFromComponentConfig(cmpnt))
                .collect(Collectors.toList());
        loadClassPathForAllComponents(componentsClassPath);
        Debug.logInfo("All components loaded", module);
    }

    @Override
    public boolean start() {
        return loaded.get();
    }

    /**
     * Iterate over all the components and load their classpath URLs into the classloader
     * and set the classloader as the context classloader
     *
     * @param componentsClassPath a list of classpaths for all components
     */
    private static void loadClassPathForAllComponents(List<Classpath> componentsClassPath) {
        List<URL> allComponentUrls = new ArrayList<>();
        for (Classpath classPath : componentsClassPath) {
            try {
                for (URI uri : classPath.toUris()) {
                    allComponentUrls.add(uri.toURL());
                }
            } catch (MalformedURLException e) {
                Debug.logError(e, "Unable to load component classpath %s", module, classPath);
            }
        }
        URL[] componentURLs = allComponentUrls.toArray(new URL[allComponentUrls.size()]);
        URLClassLoader classLoader = new URLClassLoader(componentURLs, Thread.currentThread().getContextClassLoader());
        Thread.currentThread().setContextClassLoader(classLoader);
    }

    /**
     * Loads any kind of component definition.
     *
     * @param dir  the location where the component should be loaded
     * @param component  a single component or a component directory definition
     * @throws IOException when component directory loading fails.
     */
    private void loadComponent(Path dir, ComponentDef component) throws IOException {
        Path location = component.location.isAbsolute() ? component.location : dir.resolve(component.location);
        switch (component.type) {
        case COMPONENT_DIRECTORY:
            loadComponentDirectory(location);
            break;
        case SINGLE_COMPONENT:
            retrieveComponentConfig(null, location);
            break;
        }
    }

    /**
     * Checks to see if the directory contains a load file (component-load.xml) and
     * then delegates loading to the appropriate method
     *
     * @param directoryName the name of component directory to load
     * @throws IOException
     */
    private void loadComponentDirectory(Path directoryName) throws IOException {
        Debug.logInfo("Auto-Loading component directory : [" + directoryName + "]", module);
        if (Files.exists(directoryName) && Files.isDirectory(directoryName)) {
            Path componentLoad = directoryName.resolve(ComponentLoaderConfig.COMPONENT_LOAD_XML_FILENAME);

            if (Files.exists(componentLoad)) {
                loadComponentsInDirectoryUsingLoadFile(directoryName, componentLoad);
            } else {
                loadComponentsInDirectory(directoryName);
            }
        } else {
            Debug.logError("Auto-Load Component directory not found : " + directoryName, module);
        }

    }

    /**
     * load components residing in a directory only if they exist in the component
     * load file (component-load.xml) and they are sorted in order from top to bottom
     * in the load file
     *
     * @param directoryPath the absolute path of the directory
     * @param componentLoadFile the name of the load file (i.e. component-load.xml)
     * @throws IOException
     */
    private void loadComponentsInDirectoryUsingLoadFile(Path directoryPath, Path componentLoadFile) throws IOException {
        URL configUrl = null;
        try {
            configUrl = componentLoadFile.toUri().toURL();
            List<ComponentDef> componentsToLoad = ComponentLoaderConfig.getComponentsFromConfig(configUrl);
            for (ComponentDef def: componentsToLoad) {
                loadComponent(directoryPath, def);
            }
        } catch (MalformedURLException e) {
            Debug.logError(e, "Unable to locate URL for component loading file: " + componentLoadFile.toAbsolutePath(), module);
        } catch (ComponentException e) {
            Debug.logError(e, "Unable to load components from URL: " + configUrl.toExternalForm(), module);
        }
    }

    /**
     * Load all components in a directory because it does not contain
     * a load-components.xml file. The components are sorted alphabetically
     * for loading purposes
     *
     * @param directoryPath a valid absolute path of a component directory
     * @throws IOException if an I/O error occurs when opening the directory
     */
    private static void loadComponentsInDirectory(Path directoryPath) throws IOException {
        try (Stream<Path> paths = Files.list(directoryPath)) {
            paths.sorted()
                    .map(cmpnt -> directoryPath.resolve(cmpnt).toAbsolutePath().normalize())
                    .filter(Files::isDirectory)
                    .filter(dir -> Files.exists(dir.resolve(ComponentConfig.OFBIZ_COMPONENT_XML_FILENAME)))
                    .forEach(componentDir -> retrieveComponentConfig(null, componentDir));
        }
    }

    /**
     * Fetch the <code>ComponentConfig</code> for a certain component
     *
     * @param name component name
     * @param location directory location of the component which can be {@code null}
     * @return The component configuration
     */
    private static ComponentConfig retrieveComponentConfig(String name, Path location) {
        ComponentConfig config = null;
        try {
            config = ComponentConfig.getComponentConfig(name, (location == null) ? null : location.toString());
        } catch (ComponentException e) {
            Debug.logError("Cannot load component : " + name + " @ " + location + " : " + e.getMessage(), module);
        }
        if (config == null) {
            Debug.logError("Cannot load component : " + name + " @ " + location, module);
        }
        return config;
    }

    /**
     * Constructs a {@code Classpath} object for a specific component definition.
     *
     * @param config  the component configuration
     * @return the associated class path information
     * @see ComponentConfig
     */
    private static Classpath buildClasspathFromComponentConfig(ComponentConfig config) {
        Classpath res = new Classpath();
        config.getClasspathInfos().forEach(res::add);
        return res;
    }

    @Override
    public void stop() {
    }

    @Override
    public String getName() {
        return name;
    }
}
