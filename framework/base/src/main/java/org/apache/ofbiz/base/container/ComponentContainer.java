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

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.ofbiz.base.component.ComponentConfig;
import org.apache.ofbiz.base.component.ComponentException;
import org.apache.ofbiz.base.component.ComponentLoaderConfig;
import org.apache.ofbiz.base.start.Classpath;
import org.apache.ofbiz.base.start.Start;
import org.apache.ofbiz.base.start.StartupCommand;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.FileUtil;
import org.apache.ofbiz.base.util.UtilValidate;

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
    private final List<Classpath> componentsClassPath = new ArrayList<>();
    private static Map<String, List<ComponentConfig.DependsOnInfo>> toBeLoadedComponents = new HashMap<>();

    @Override
    public void init(List<StartupCommand> ofbizCommands, String name, String configFile) throws ContainerException {
        if (!loaded.compareAndSet(false, true)) {
            throw new ContainerException("Components already loaded, cannot start");
        }
        this.name = name;

        // load the components from framework/base/config/component-load.xml (root components)
        try {
            for (ComponentLoaderConfig.ComponentDef def: ComponentLoaderConfig.getRootComponents()) {
                loadComponentFromConfig(Start.getInstance().getConfig().ofbizHome, def);
            }
        } catch (IOException | ComponentException e) {
            throw new ContainerException(e);
        }
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
        for(Classpath classPath : componentsClassPath) {
            try {
                allComponentUrls.addAll(Arrays.asList(classPath.getUrls()));
            } catch (MalformedURLException e) {
                Debug.logError("Unable to load component classpath" + classPath.toString(), module);
                Debug.logError(e.getMessage(), module);
            }
        }
        URL[] componentURLs = allComponentUrls.toArray(new URL[allComponentUrls.size()]);
        URLClassLoader classLoader = new URLClassLoader(componentURLs, Thread.currentThread().getContextClassLoader());
        Thread.currentThread().setContextClassLoader(classLoader);
    }

    /**
     * Checks if <code>ComponentDef.type</code> is a directory or a single component.
     * If it is a directory, load the directory, otherwise load a single component
     *
     * @param parentPath the parent path of what is being loaded
     * @param def the component or directory loader definition
     * @throws IOException
     * @throws ContainerException
     * @throws ComponentException
     */
    private void loadComponentFromConfig(String parentPath, ComponentLoaderConfig.ComponentDef def) throws IOException, ContainerException, ComponentException {
        String location = def.location.startsWith("/") ? def.location : parentPath + "/" + def.location;

        if (def.type.equals(ComponentLoaderConfig.ComponentType.COMPONENT_DIRECTORY)) {
            loadComponentDirectory(location);
        } else if (def.type.equals(ComponentLoaderConfig.ComponentType.SINGLE_COMPONENT)) {
            ComponentConfig config = retrieveComponentConfig(def.name, location);
            if (config != null) {
                loadComponent(config);
            }
        }
    }

    /**
     * Checks to see if the directory contains a load file (component-load.xml) and
     * then delegates loading to the appropriate method
     *
     * @param directoryName the name of component directory to load
     * @throws IOException
     * @throws ContainerException
     * @throws ComponentException
     */
    private void loadComponentDirectory(String directoryName) throws IOException, ContainerException, ComponentException {
        Debug.logInfo("Auto-Loading component directory : [" + directoryName + "]", module);

        File directoryPath = FileUtil.getFile(directoryName);
        if (directoryPath.exists() && directoryPath.isDirectory()) {
            File componentLoadFile = new File(directoryPath, ComponentLoaderConfig.COMPONENT_LOAD_XML_FILENAME);
            if (componentLoadFile.exists()) {
                loadComponentsInDirectoryUsingLoadFile(directoryPath, componentLoadFile);
            } else {
                loadComponentsInDirectory(directoryPath);
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
     * @throws ContainerException
     */
    private void loadComponentsInDirectoryUsingLoadFile(File directoryPath, File componentLoadFile) throws IOException, ContainerException {
        URL configUrl = null;
        try {
            configUrl = componentLoadFile.toURI().toURL();
            List<ComponentLoaderConfig.ComponentDef> componentsToLoad = ComponentLoaderConfig.getComponentsFromConfig(configUrl);
            for (ComponentLoaderConfig.ComponentDef def: componentsToLoad) {
                loadComponentFromConfig(directoryPath.toString(), def);
            }
        } catch (MalformedURLException e) {
            Debug.logError(e, "Unable to locate URL for component loading file: " + componentLoadFile.getAbsolutePath(), module);
        } catch (ComponentException e) {
            Debug.logError(e, "Unable to load components from URL: " + configUrl.toExternalForm(), module);
        }
    }

    /**
     * Load all components in a directory because it does not contain
     * a load-components.xml file. The components are sorted alphabetically
     * for loading purposes
     *
     * @param directoryPath the absolute path of the directory
     * @throws IOException
     * @throws ComponentException
     */
    private void loadComponentsInDirectory(File directoryPath) throws IOException, ComponentException {
        String[] sortedComponentNames = directoryPath.list();
        List<ComponentConfig> componentConfigs = new ArrayList<>();
        if (sortedComponentNames == null) {
            throw new IllegalArgumentException("sortedComponentNames is null, directory path is invalid " + directoryPath.getPath());
        }
        Arrays.sort(sortedComponentNames);

        for (String componentName: sortedComponentNames) {
            File componentPath = FileUtil.getFile(directoryPath.getCanonicalPath() + File.separator + componentName);
            String componentLocation = componentPath.getCanonicalPath();
            File configFile = FileUtil.getFile(componentLocation.concat(File.separator).concat(ComponentConfig.OFBIZ_COMPONENT_XML_FILENAME));

            if (componentPath.isDirectory() && !componentName.startsWith(".") && configFile.exists()) {
                ComponentConfig config = retrieveComponentConfig(null, componentLocation);
                componentConfigs.add(config);
            }
        }
        for (ComponentConfig componentConfig : componentConfigs) {
            if (componentConfig != null) {
                loadComponent(componentConfig);
            }
        }
        loadComponentWithDependency();
    }

    /**
     * Checks dependency for unloaded components and add them into
     * componentsClassPath
     *
     * @throws IOException
     * @throws ComponentException
     */
    private void loadComponentWithDependency() throws IOException, ComponentException {
        while (true) {
            if (UtilValidate.isEmpty(toBeLoadedComponents)) {
                return;
            } else {
                for (Map.Entry<String, List<ComponentConfig.DependsOnInfo>> entries : toBeLoadedComponents.entrySet()) {
                    ComponentConfig config = retrieveComponentConfig(entries.getKey(), null);
                    if (config.enabled()) {
                        List<ComponentConfig.DependsOnInfo> dependencyList = checkDependencyForComponent(config);
                        if (UtilValidate.isNotEmpty(dependencyList)) {
                            toBeLoadedComponents.replace(config.getComponentName(), dependencyList);
                            String msg = "Not loading component [" + config.getComponentName() + "] because it's dependent Component is not loaded [ " + dependencyList + "]";
                            Debug.logInfo(msg, module);
                        }
                        if (UtilValidate.isEmpty(dependencyList)) {
                            componentsClassPath.add(buildClasspathFromComponentConfig(config));
                            toBeLoadedComponents.replace(config.getComponentName(), dependencyList);
                            Debug.logInfo("Added class path for component : [" + config.getComponentName() + "]", module);
                        }
                    } else {
                        Debug.logInfo("Not loading component [" + config.getComponentName() + "] because it's disabled", module);
                    }
                }
            }
        }
    }

    /**
     * Fetch the <code>ComponentConfig</code> for a certain component
     *
     * @param name component name
     * @param location directory location of the component
     * @return The component configuration
     */
    private static ComponentConfig retrieveComponentConfig(String name, String location) {
        ComponentConfig config = null;
        try {
            config = ComponentConfig.getComponentConfig(name, location);
        } catch (ComponentException e) {
            Debug.logError("Cannot load component : " + name + " @ " + location + " : " + e.getMessage(), module);
        }
        if (config == null) {
            Debug.logError("Cannot load component : " + name + " @ " + location, module);
        }
        return config;
    }

    /**
     * Load a single component by adding all its classpath entries to
     * the list of classpaths to be loaded
     *
     * @param config the component configuration
     * @throws IOException
     * @throws ComponentException
     */
    private void loadComponent(ComponentConfig config) throws IOException, ComponentException {
        if (config.enabled()) {
            List<ComponentConfig.DependsOnInfo> dependencyList = checkDependencyForComponent(config);
            if (UtilValidate.isEmpty(dependencyList)) {
                componentsClassPath.add(buildClasspathFromComponentConfig(config));
                Debug.logInfo("Added class path for component : [" + config.getComponentName() + "]", module);
            }
        } else {
            Debug.logInfo("Not loading component [" + config.getComponentName() + "] because it's disabled", module);
        }
    }

    /**
     * Check for components loaded and Removes loaded components dependency
     * from list of unloaded components
     *
     * @param config the component configuration
     * @throws IOException
     * @throws ComponentException
     *
     */
    private List<ComponentConfig.DependsOnInfo> checkDependencyForComponent(ComponentConfig config) throws IOException, ComponentException {
        List<ComponentConfig.DependsOnInfo> dependencyList = new ArrayList<>(config.getDependsOn());
        if (UtilValidate.isNotEmpty(dependencyList)) {
            Set<ComponentConfig.DependsOnInfo> resolvedDependencyList = new HashSet<>();
            for (ComponentConfig.DependsOnInfo dependency : dependencyList) {
                Debug.logInfo("Component : " + config.getComponentName() + " is Dependent on  " + dependency.componentName, module);
                ComponentConfig componentConfig = ComponentConfig.getComponentConfig(String.valueOf(dependency.componentName));
                Classpath dependentComponentClasspath = buildClasspathFromComponentConfig(componentConfig);
                componentsClassPath.forEach(componentClassPath -> {
                    if (Arrays.equals(componentClassPath.toString().split(":"), dependentComponentClasspath.toString().split(":"))) {
                        resolvedDependencyList.add(dependency);
                    }
                });
            }
            resolvedDependencyList.forEach(resolvedDependency -> Debug.logInfo("Resolved : " + resolvedDependency.componentName + " Dependency for Component " + config.getComponentName(), module));
            dependencyList.removeAll(resolvedDependencyList);
            if (UtilValidate.isEmpty(dependencyList)) {
                toBeLoadedComponents.remove(config.getComponentName());
            } else {
                toBeLoadedComponents.put(config.getComponentName(), dependencyList);
            }
        }
        return dependencyList;
    }

    /**
     * Construct a <code>Classpath</code> object for a certain component based
     * on its configuration defined in <code>ComponentConfig</code>
     *
     * @param config the component configuration
     * @return the constructed classpath
     * @throws IOException
     */
    private static Classpath buildClasspathFromComponentConfig(ComponentConfig config) throws IOException {
        Classpath classPath = new Classpath();
        String configRoot = config.getRootLocation().replace('\\', '/');
        configRoot = configRoot.endsWith("/") ? configRoot : configRoot + "/";
        List<ComponentConfig.ClasspathInfo> classpathInfos = config.getClasspathInfos();

        for (ComponentConfig.ClasspathInfo cp: classpathInfos) {
            String location = cp.location.replace('\\', '/');
            if (!"jar".equals(cp.type) && !"dir".equals(cp.type)) {
                Debug.logError("Classpath type '" + cp.type + "' is not supported; '" + location + "' not loaded", module);
                continue;
            }

            location = location.startsWith("/") ? location.substring(1) : location;
            String dirLoc = location.endsWith("/*") ? location.substring(0, location.length() - 2) : location;
            File path = FileUtil.getFile(configRoot + dirLoc);

            if (path.exists()) {
                classPath.addComponent(configRoot + location);
                if (path.isDirectory() && "dir".equals(cp.type)) {
                    classPath.addFilesFromPath(path);
                }
            } else {
                Debug.logWarning("Location '" + configRoot + dirLoc + "' does not exist", module);
            }
        }
        return classPath;
    }

    @Override
    public void stop() {
    }

    @Override
    public String getName() {
        return name;
    }

}
