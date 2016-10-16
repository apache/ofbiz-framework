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
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.ofbiz.base.component.ComponentConfig;
import org.apache.ofbiz.base.component.ComponentException;
import org.apache.ofbiz.base.component.ComponentLoaderConfig;
import org.apache.ofbiz.base.start.Classpath;
import org.apache.ofbiz.base.start.NativeLibClassLoader;
import org.apache.ofbiz.base.start.StartupCommand;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.FileUtil;
import org.apache.ofbiz.base.util.UtilValidate;

/**
 * ComponentContainer - StartupContainer implementation for Components
 * <p/>
 * Example ofbiz-container.xml configuration:
 * <pre>
 *   <container name="component-container" class="org.apache.ofbiz.base.component.ComponentContainer"/>
 * </pre>
 */
public class ComponentContainer implements Container {

    public static final String module = ComponentContainer.class.getName();

    protected String configFileLocation = null;
    private String name;
    private final AtomicBoolean loaded = new AtomicBoolean(false);

    @Override
    public void init(List<StartupCommand> ofbizCommands, String name, String configFile) throws ContainerException {
        if (!loaded.compareAndSet(false, true)) {
            throw new ContainerException("Components already loaded, cannot start");
        }
        this.name = name;
        this.configFileLocation = configFile;

        // get the config for this container
        ContainerConfig.Configuration cc = ContainerConfig.getConfiguration(name, configFileLocation);

        // check for an override loader config
        String loaderConfig = null;
        if (cc.getProperty("loader-config") != null) {
            loaderConfig = cc.getProperty("loader-config").value;
        }

        // load the components
        try {
            loadComponents(loaderConfig);
        } catch (ComponentException e) {
            throw new ContainerException(e);
        }
    }

    /**
     * @see org.apache.ofbiz.base.container.Container#start()
     */
    public boolean start() throws ContainerException {
        return loaded.get();
    }

    public void loadComponents(String loaderConfig) throws ComponentException {
        // get the components to load
        List<ComponentLoaderConfig.ComponentDef> components = ComponentLoaderConfig.getRootComponents(loaderConfig);
        String parentPath;
        try {
            parentPath = FileUtil.getFile(System.getProperty("ofbiz.home")).getCanonicalFile().toString().replaceAll("\\\\", "/");
            // load each component
            if (components != null) {
                for (ComponentLoaderConfig.ComponentDef def: components) {
                    loadComponentFromConfig(parentPath, def);
                }
            }
        } catch (MalformedURLException e) {
            throw new ComponentException(e.getMessage(), e);
        } catch (IOException e) {
            throw new ComponentException(e.getMessage(), e);
        }
        Debug.logInfo("All components loaded", module);
    }

    private void loadComponentFromConfig(String parentPath, ComponentLoaderConfig.ComponentDef def) throws IOException {
        String location;
        if (def.location.startsWith("/")) {
            location = def.location;
        } else {
            location = parentPath + "/" + def.location;
        }
        if (def.type == ComponentLoaderConfig.SINGLE_COMPONENT) {
            ComponentConfig config = null;
            try {
                config = ComponentConfig.getComponentConfig(def.name, location);
                if (UtilValidate.isEmpty(def.name)) {
                    def.name = config.getGlobalName();
                }
            } catch (ComponentException e) {
                Debug.logError("Cannot load component : " + def.name + " @ " + def.location + " : " + e.getMessage(), module);
            }
            if (config == null) {
                Debug.logError("Cannot load component : " + def.name + " @ " + def.location, module);
            } else {
                this.loadComponent(config);
            }
        } else if (def.type == ComponentLoaderConfig.COMPONENT_DIRECTORY) {
            this.loadComponentDirectory(location);
        }
    }

    private void loadComponentDirectory(String directoryName) throws IOException {
        Debug.logInfo("Auto-Loading component directory : [" + directoryName + "]", module);
        File parentPath = FileUtil.getFile(directoryName);
        if (!parentPath.exists() || !parentPath.isDirectory()) {
            Debug.logError("Auto-Load Component directory not found : " + directoryName, module);
        } else {
            File componentLoadConfig = new File(parentPath, ComponentLoaderConfig.COMPONENT_LOAD_XML_FILENAME);
            if (componentLoadConfig != null && componentLoadConfig.exists()) {
                URL configUrl = null;
                try {
                    configUrl = componentLoadConfig.toURI().toURL();
                    List<ComponentLoaderConfig.ComponentDef> componentsToLoad = ComponentLoaderConfig.getComponentsFromConfig(configUrl);
                    if (componentsToLoad != null) {
                        for (ComponentLoaderConfig.ComponentDef def: componentsToLoad) {
                            this.loadComponentFromConfig(parentPath.toString(), def);
                        }
                    }
                } catch (MalformedURLException e) {
                    Debug.logError(e, "Unable to locate URL for component loading file: " + componentLoadConfig.getAbsolutePath(), module);
                } catch (ComponentException e) {
                    Debug.logError(e, "Unable to load components from URL: " + configUrl.toExternalForm(), module);
                }
            } else {
                String[] fileNames = parentPath.list();
                Arrays.sort(fileNames);
                for (String sub: fileNames) {
                    try {
                        File componentPath = FileUtil.getFile(parentPath.getCanonicalPath() + File.separator + sub);
                        if (componentPath.isDirectory() && !sub.equals("CVS") && !sub.equals(".svn")) {
                            // make sure we have a component configuration file
                            String componentLocation = componentPath.getCanonicalPath();
                            File configFile = FileUtil.getFile(componentLocation.concat(File.separator).concat(ComponentConfig.OFBIZ_COMPONENT_XML_FILENAME));
                            if (configFile.exists()) {
                                ComponentConfig config = null;
                                try {
                                    // pass null for the name, will default to the internal component name
                                    config = ComponentConfig.getComponentConfig(null, componentLocation);
                                } catch (ComponentException e) {
                                    Debug.logError(e, "Cannot load component : " + componentPath.getName() + " @ " + componentLocation + " : " + e.getMessage(), module);
                                }
                                if (config == null) {
                                    Debug.logError("Cannot load component : " + componentPath.getName() + " @ " + componentLocation, module);
                                } else {
                                    loadComponent(config);
                                }
                            }
                        }
                    } catch (IOException ioe) {
                        Debug.logError(ioe, module);
                    }
                }
            }
        }
    }

    private void loadComponent(ComponentConfig config) throws IOException {
        // make sure the component is enabled
        if (!config.enabled()) {
            Debug.logInfo("Not loading component [" + config.getComponentName() + "] because it is disabled", module);
            return;
        }
        List<ComponentConfig.ClasspathInfo> classpathInfos = config.getClasspathInfos();
        String configRoot = config.getRootLocation();
        configRoot = configRoot.replace('\\', '/');
        // set the root to have a trailing slash
        if (!configRoot.endsWith("/")) {
            configRoot = configRoot + "/";
        }
        if (classpathInfos != null) {
            Classpath classPath = new Classpath();
            // TODO: If any components change the class loader, then this will need to be changed.
            NativeLibClassLoader classloader = (NativeLibClassLoader) Thread.currentThread().getContextClassLoader();
            for (ComponentConfig.ClasspathInfo cp: classpathInfos) {
                String location = cp.location.replace('\\', '/');
                // set the location to not have a leading slash
                if (location.startsWith("/")) {
                    location = location.substring(1);
                }
                if (!"jar".equals(cp.type) && !"dir".equals(cp.type)) {
                    Debug.logError("Classpath type '" + cp.type + "' is not supported; '" + location + "' not loaded", module);
                    continue;
                }
                String dirLoc = location;
                if (dirLoc.endsWith("/*")) {
                    // strip off the slash splat
                    dirLoc = location.substring(0, location.length() - 2);
                }
                File path = FileUtil.getFile(configRoot + dirLoc);
                if (path.exists()) {
                    if (path.isDirectory()) {
                        if ("dir".equals(cp.type)) {
                            classPath.addComponent(configRoot + location);
                        }
                        classPath.addFilesFromPath(path);
                    } else {
                        // add a single file
                        classPath.addComponent(configRoot + location);
                    }
                } else {
                    Debug.logWarning("Location '" + configRoot + dirLoc + "' does not exist", module);
                }
            }
            for (URL url : classPath.getUrls()) {
                classloader.addURL(url);
            }
            for (File folder : classPath.getNativeFolders()) {
                classloader.addNativeClassPath(folder);
            }
        }
         Debug.logInfo("Loaded component : [" + config.getComponentName() + "]", module);
    }

    /**
     * @see org.apache.ofbiz.base.container.Container#stop()
     */
    public void stop() throws ContainerException {
    }

    public String getName() {
        return name;
    }

}
