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
import org.ofbiz.base.start.Classpath;
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

    //protected static List loadedComponents2 = null;
    protected Classpath classPath = new Classpath(System.getProperty("java.class.path"));
    protected String configFileLocation = null;
    private boolean loaded = false;
    private String instrumenterClassName;
    private String instrumenterFile;

    /**
     * @see org.ofbiz.base.container.Container#init(java.lang.String[], java.lang.String)
     */
    public void init(String[] args, String configFile) throws ContainerException {
        this.configFileLocation = configFile;

        // get the config for this container
        ContainerConfig.Container cc = ContainerConfig.getContainer("component-container", configFileLocation);

        // check for an override loader config
        String loaderConfig = null;
        if (cc.getProperty("loader-config") != null) {
            loaderConfig = cc.getProperty("loader-config").value;
        }

        // check for en override update classpath
        boolean updateClassPath = true;
        if (cc.getProperty("update-classpath") != null) {
            updateClassPath = "true".equalsIgnoreCase(cc.getProperty("update-classpath").value);
        }
        String instrumenterClassName;
        if (cc.getProperty("ofbiz.instrumenterClassName") != null) {
            instrumenterClassName = cc.getProperty("ofbiz.instrumenterClassName").value;
        } else {
            instrumenterClassName = null;
        }
        String instrumenterFile;
        if (cc.getProperty("ofbiz.instrumenterFile") != null) {
            instrumenterFile = cc.getProperty("ofbiz.instrumenterFile").value;
        } else {
            instrumenterFile = null;
        }

        // load the components
        try {
            loadComponents(loaderConfig, updateClassPath, instrumenterClassName, instrumenterFile);
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

    public synchronized void loadComponents(String loaderConfig, boolean updateClasspath) throws AlreadyLoadedException, ComponentException {
        loadComponents(loaderConfig, updateClasspath, null, null);
    }

    public synchronized void loadComponents(String loaderConfig, boolean updateClasspath, String instrumenterClassName, String instrumenterFile) throws AlreadyLoadedException, ComponentException {
        // set the loaded list; and fail if already loaded
        //if (loadedComponents == null) {
        //    loadedComponents = new LinkedList();
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

        // set the new classloader/classpath on the current thread
        if (updateClasspath) {
            classPath.instrument(instrumenterFile, instrumenterClassName);
            System.setProperty("java.class.path", classPath.toString());
            ClassLoader cl = classPath.getClassLoader();
            Thread.currentThread().setContextClassLoader(cl);
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
                            // make sure we have a component configuraton file
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
            Debug.logInfo("Not Loading component : [" + config.getComponentName() + "] (disabled)", module);
            return;
        }

        Debug.logInfo("Loading component : [" + config.getComponentName() + "]", module);
        List<ComponentConfig.ClasspathInfo> classpathInfos = config.getClasspathInfos();
        String configRoot = config.getRootLocation();
        configRoot = configRoot.replace('\\', '/');
        // set the root to have a trailing slash
        if (!configRoot.endsWith("/")) {
            configRoot = configRoot + "/";
        }
        if (classpathInfos != null) {
            for (ComponentConfig.ClasspathInfo cp: classpathInfos) {
                String location = cp.location.replace('\\', '/');
                // set the location to not have a leading slash
                if (location.startsWith("/")) {
                    location = location.substring(1);
                }
                if ("dir".equals(cp.type)) {
                    classPath.addComponent(configRoot + location);
                } else if ("jar".equals(cp.type)) {
                    String dirLoc = location;
                    if (dirLoc.endsWith("/*")) {
                        // strip off the slash splat
                        dirLoc = location.substring(0, location.length() - 2);
                    }
                    File path = FileUtil.getFile(configRoot + dirLoc);
                    if (path.exists()) {
                        if (path.isDirectory()) {
                            // load all .jar and .zip files in this directory
                            for (File file: path.listFiles()) {
                                String fileName = file.getName();
                                if (fileName.endsWith(".jar") || fileName.endsWith(".zip")) {
                                    classPath.addComponent(file);
                                }
                            }
                        } else {
                            // add a single file
                            classPath.addComponent(configRoot + location);
                        }
                    } else {
                        Debug.logWarning("Location '" + configRoot + dirLoc + "' does not exist", module);
                    }
                } else {
                    Debug.logError("Classpath type '" + cp.type + "' is not supported; '" + location + "' not loaded", module);
                }
            }
        }
    }

    /**
     * @see org.ofbiz.base.container.Container#stop()
     */
    public void stop() throws ContainerException {
    }

    /**
     * Static method for easy loading of components for use when the container system is not.
     *
     * @param updateClasspath Tells the component loader to update the classpath, and thread classloader
     * @throws AlreadyLoadedException
     * @throws ComponentException
     */
    public static synchronized void loadComponents(boolean updateClasspath) throws AlreadyLoadedException, ComponentException {
        ComponentContainer cc = new ComponentContainer();
        cc.loadComponents(null, updateClasspath);
        cc = null;
    }
}
