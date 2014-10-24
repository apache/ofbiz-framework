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

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import org.ofbiz.base.component.AlreadyLoadedException;
import org.ofbiz.base.component.ComponentConfig;
import org.ofbiz.base.component.ComponentException;
import org.ofbiz.base.component.ComponentLoaderConfig;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.FileUtil;
import org.ofbiz.base.util.UtilValidate;

/**
 * ComponentContainer - StartupContainer implementation for Components
 * <p/>
 * Example ofbiz-container.xml configuration:
 * <pre>
 *   <container name="component-container" class="org.ofbiz.base.component.ComponentContainer"/>
 * </pre>
 */
public class ComponentContainer implements Container {

    public static final String module = ComponentContainer.class.getName();

    protected String configFileLocation = null;
    private String name;
    private boolean loaded = false;

    @Override
    public void init(String[] args, String name, String configFile) throws ContainerException {
        this.name = name;
        this.configFileLocation = configFile;

        // get the config for this container
        ContainerConfig.Container cc = ContainerConfig.getContainer(name, configFileLocation);

        // check for an override loader config
        String loaderConfig = null;
        if (cc.getProperty("loader-config") != null) {
            loaderConfig = cc.getProperty("loader-config").value;
        }

        // load the components
        try {
            loadComponents(loaderConfig);
        } catch (AlreadyLoadedException e) {
            throw new ContainerException(e);
        } catch (ComponentException e) {
            throw new ContainerException(e);
        }
    }

    /**
     * @see org.ofbiz.base.container.Container#start()
     */
    public boolean start() throws ContainerException {
        return true;
    }

    public synchronized void loadComponents(String loaderConfig) throws AlreadyLoadedException, ComponentException {
        // set the loaded list; and fail if already loaded
        if (!loaded) {
            loaded = true;
        } else {
            throw new AlreadyLoadedException("Components already loaded, cannot start");
        }

        // get the components to load
        List<ComponentLoaderConfig.ComponentDef> components = ComponentLoaderConfig.getRootComponents(loaderConfig);

        String parentPath;
        try {
            parentPath = FileUtil.getFile(System.getProperty("ofbiz.home")).getCanonicalFile().toString().replaceAll("\\\\", "/");
        } catch (MalformedURLException e) {
            throw new ComponentException(e.getMessage(), e);
        } catch (IOException e) {
            throw new ComponentException(e.getMessage(), e);
        }
        // load each component
        if (components != null) {
            for (ComponentLoaderConfig.ComponentDef def: components) {
                this.loadComponentFromConfig(parentPath, def);
            }
        }
        Debug.logInfo("All components loaded", module);
    }

    private void loadComponentFromConfig(String parentPath, ComponentLoaderConfig.ComponentDef def) {
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

    private void loadComponentDirectory(String directoryName) {
        Debug.logInfo("Auto-Loading component directory : [" + directoryName + "]", module);
        File parentPath = FileUtil.getFile(directoryName);
        if (!parentPath.exists() || !parentPath.isDirectory()) {
            Debug.logError("Auto-Load Component directory not found : " + directoryName, module);
        } else {
            File componentLoadConfig = new File(parentPath, "component-load.xml");
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
                        File componentPath = FileUtil.getFile(parentPath.getCanonicalPath() + "/" + sub);
                        if (componentPath.isDirectory() && !sub.equals("CVS") && !sub.equals(".svn")) {
                            // make sure we have a component configuration file
                            String componentLocation = componentPath.getCanonicalPath();
                            File configFile = FileUtil.getFile(componentLocation + "/ofbiz-component.xml");
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

    private void loadComponent(ComponentConfig config) {
        // make sure the component is enabled
        if (!config.enabled()) {
            Debug.logInfo("Not Loaded component : [" + config.getComponentName() + "] (disabled)", module);
            return;
        }
        Debug.logInfo("Loaded component : [" + config.getComponentName() + "]", module);
    }

    /**
     * @see org.ofbiz.base.container.Container#stop()
     */
    public void stop() throws ContainerException {
    }

    public String getName() {
        return name;
    }

}
