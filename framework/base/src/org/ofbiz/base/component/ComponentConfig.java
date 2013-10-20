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
package org.ofbiz.base.component;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.ofbiz.base.container.ContainerConfig;
import org.ofbiz.base.container.ContainerException;
import org.ofbiz.base.location.FlexibleLocation;
import org.ofbiz.base.util.Assert;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.KeyStoreUtil;
import org.ofbiz.base.util.UtilURL;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.UtilXml;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * ComponentConfig - Component configuration class for ofbiz-container.xml
 *
 */
public class ComponentConfig {

    public static final String module = ComponentConfig.class.getName();
    public static final String OFBIZ_COMPONENT_XML_FILENAME = "ofbiz-component.xml";
    /* Note: These Maps are not UtilCache instances because there is no strategy or implementation for reloading components.
     * Also, we are using LinkedHashMap to maintain insertion order - which client code depends on. This means
     * we will need to use synchronization code because there is no concurrent implementation of LinkedHashMap.
     */
    private static final ComponentConfigCache componentConfigCache = new ComponentConfigCache();
    private static final Map<String, List<WebappInfo>> serverWebApps = new LinkedHashMap<String, List<WebappInfo>>();

    public static Boolean componentExists(String componentName) {
        Assert.notEmpty("componentName", componentName);
        return componentConfigCache.fromGlobalName(componentName) != null;
    }

    public static List<ClasspathInfo> getAllClasspathInfos() {
        return getAllClasspathInfos(null);
    }

    public static List<ClasspathInfo> getAllClasspathInfos(String componentName) {
        List<ClasspathInfo> classpaths = new ArrayList<ClasspathInfo>();
        for (ComponentConfig cc : getAllComponents()) {
            if (componentName == null || componentName.equals(cc.getComponentName())) {
                classpaths.addAll(cc.getClasspathInfos());
            }
        }
        return classpaths;
    }

    public static Collection<ComponentConfig> getAllComponents() {
        return componentConfigCache.values();
    }

    public static List<ContainerConfig.Container> getAllContainers() {
        return getAllContainers(null);
    }

    public static List<ContainerConfig.Container> getAllContainers(String componentName) {
        List<ContainerConfig.Container> containers = new ArrayList<ContainerConfig.Container>();
        for (ComponentConfig cc : getAllComponents()) {
            if (componentName == null || componentName.equals(cc.getComponentName())) {
                containers.addAll(cc.getContainers());
            }
        }
        return containers;
    }

    public static List<EntityResourceInfo> getAllEntityResourceInfos(String type) {
        return getAllEntityResourceInfos(type, null);
    }

    public static List<EntityResourceInfo> getAllEntityResourceInfos(String type, String componentName) {
        List<EntityResourceInfo> entityInfos = new ArrayList<EntityResourceInfo>();
        for (ComponentConfig cc : getAllComponents()) {
            if (componentName == null || componentName.equals(cc.getComponentName())) {
                List<EntityResourceInfo> ccEntityInfoList = cc.getEntityResourceInfos();
                if (UtilValidate.isEmpty(type)) {
                    entityInfos.addAll(ccEntityInfoList);
                } else {
                    for (EntityResourceInfo entityResourceInfo : ccEntityInfoList) {
                        if (type.equals(entityResourceInfo.type)) {
                            entityInfos.add(entityResourceInfo);
                        }
                    }
                }
            }
        }
        return entityInfos;
    }

    public static List<KeystoreInfo> getAllKeystoreInfos() {
        return getAllKeystoreInfos(null);
    }

    public static List<KeystoreInfo> getAllKeystoreInfos(String componentName) {
        List<KeystoreInfo> keystoreInfos = new ArrayList<KeystoreInfo>();
        for (ComponentConfig cc : getAllComponents()) {
            if (componentName == null || componentName.equals(cc.getComponentName())) {
                keystoreInfos.addAll(cc.getKeystoreInfos());
            }
        }
        return keystoreInfos;
    }

    public static List<ServiceResourceInfo> getAllServiceResourceInfos(String type) {
        return getAllServiceResourceInfos(type, null);
    }

    public static List<ServiceResourceInfo> getAllServiceResourceInfos(String type, String componentName) {
        List<ServiceResourceInfo> serviceInfos = new ArrayList<ServiceResourceInfo>();
        for (ComponentConfig cc : getAllComponents()) {
            if (componentName == null || componentName.equals(cc.getComponentName())) {
                List<ServiceResourceInfo> ccServiceInfoList = cc.getServiceResourceInfos();
                if (UtilValidate.isEmpty(type)) {
                    serviceInfos.addAll(ccServiceInfoList);
                } else {
                    for (ServiceResourceInfo serviceResourceInfo : ccServiceInfoList) {
                        if (type.equals(serviceResourceInfo.type)) {
                            serviceInfos.add(serviceResourceInfo);
                        }
                    }
                }
            }
        }
        return serviceInfos;
    }

    public static List<TestSuiteInfo> getAllTestSuiteInfos() {
        return getAllTestSuiteInfos(null);
    }

    public static List<TestSuiteInfo> getAllTestSuiteInfos(String componentName) {
        List<TestSuiteInfo> testSuiteInfos = new ArrayList<TestSuiteInfo>();
        for (ComponentConfig cc : getAllComponents()) {
            if (componentName == null || componentName.equals(cc.getComponentName())) {
                testSuiteInfos.addAll(cc.getTestSuiteInfos());
            }
        }
        return testSuiteInfos;
    }

    public static List<WebappInfo> getAllWebappResourceInfos() {
        return getAllWebappResourceInfos(null);
    }

    public static List<WebappInfo> getAllWebappResourceInfos(String componentName) {
        List<WebappInfo> webappInfos = new ArrayList<WebappInfo>();
        for (ComponentConfig cc : getAllComponents()) {
            if (componentName == null || componentName.equals(cc.getComponentName())) {
                webappInfos.addAll(cc.getWebappInfos());
            }
        }
        return webappInfos;
    }

    public static List<WebappInfo> getAppBarWebInfos(String serverName) {
        return ComponentConfig.getAppBarWebInfos(serverName, null, null);
    }

    public static List<WebappInfo> getAppBarWebInfos(String serverName, Comparator<? super String> comp, String menuName) {
        String serverWebAppsKey = serverName + menuName;
        List<WebappInfo> webInfos = null;
        synchronized (serverWebApps) {
            webInfos = serverWebApps.get(serverWebAppsKey);
        }
        if (webInfos == null) {
            Map<String, WebappInfo> tm = null;
            // use a TreeMap to sort the components alpha by title
            if (comp != null) {
                tm = new TreeMap<String, WebappInfo>(comp);
            } else {
                tm = new TreeMap<String, WebappInfo>();
            }
            for (ComponentConfig cc : getAllComponents()) {
                for (WebappInfo wInfo : cc.getWebappInfos()) {
                    String key = UtilValidate.isNotEmpty(wInfo.position) ? wInfo.position : wInfo.title;
                    if (serverName.equals(wInfo.server) && wInfo.appBarDisplay) {
                        if (UtilValidate.isNotEmpty(menuName)) {
                            if (menuName.equals(wInfo.menuName)) {
                                tm.put(key, wInfo);
                            }
                        } else {
                            tm.put(key, wInfo);
                        }
                    }
                }
            }
            webInfos = new ArrayList<WebappInfo>(tm.size());
            webInfos.addAll(tm.values());
            webInfos = Collections.unmodifiableList(webInfos);
            synchronized (serverWebApps) {
                // We are only preventing concurrent modification, we are not guaranteeing a singleton.
                serverWebApps.put(serverWebAppsKey, webInfos);
            }
        }
        return webInfos;
    }

    public static List<WebappInfo> getAppBarWebInfos(String serverName, String menuName) {
        return getAppBarWebInfos(serverName, null, menuName);
    }

    public static ComponentConfig getComponentConfig(String globalName) throws ComponentException {
        // TODO: we need to look up the rootLocation from the container config, or this will blow up
        return getComponentConfig(globalName, null);
    }

    public static ComponentConfig getComponentConfig(String globalName, String rootLocation) throws ComponentException {
        ComponentConfig componentConfig = null;
        if (globalName != null && !globalName.isEmpty()) {
            componentConfig = componentConfigCache.fromGlobalName(globalName);
            if (componentConfig != null) {
                return componentConfig;
            }
        }
        if (rootLocation != null && !rootLocation.isEmpty()) {
            componentConfig = componentConfigCache.fromRootLocation(rootLocation);
            if (componentConfig != null) {
                return componentConfig;
            }
        }
        if (rootLocation != null) {
            componentConfig = new ComponentConfig(globalName, rootLocation);
            if (componentConfig.enabled()) {
                componentConfigCache.put(componentConfig);
            }
            return componentConfig;
        } else {
            // Do we really need to do this?
            throw new ComponentException("No component found named : " + globalName);
        }
    }

    public static String getFullLocation(String componentName, String resourceLoaderName, String location) throws ComponentException {
        ComponentConfig cc = getComponentConfig(componentName, null);
        if (cc == null) {
            throw new ComponentException("Could not find component with name: " + componentName);
        }
        return cc.getFullLocation(resourceLoaderName, location);
    }

    public static KeystoreInfo getKeystoreInfo(String componentName, String keystoreName) {
        for (ComponentConfig cc : getAllComponents()) {
            if (componentName != null && componentName.equals(cc.getComponentName())) {
                for (KeystoreInfo ks : cc.getKeystoreInfos()) {
                    if (keystoreName != null && keystoreName.equals(ks.getName())) {
                        return ks;
                    }
                }
            }
        }
        return null;
    }

    public static String getRootLocation(String componentName) throws ComponentException {
        ComponentConfig cc = getComponentConfig(componentName);
        if (cc == null) {
            throw new ComponentException("Could not find component with name: " + componentName);
        }
        return cc.getRootLocation();
    }

    public static InputStream getStream(String componentName, String resourceLoaderName, String location) throws ComponentException {
        ComponentConfig cc = getComponentConfig(componentName);
        if (cc == null) {
            throw new ComponentException("Could not find component with name: " + componentName);
        }
        return cc.getStream(resourceLoaderName, location);
    }

    public static URL getURL(String componentName, String resourceLoaderName, String location) throws ComponentException {
        ComponentConfig cc = getComponentConfig(componentName);
        if (cc == null) {
            throw new ComponentException("Could not find component with name: " + componentName);
        }
        return cc.getURL(resourceLoaderName, location);
    }

    public static WebappInfo getWebAppInfo(String serverName, String contextRoot) {
        if (serverName == null || contextRoot == null) {
            return null;
        }
        ComponentConfig.WebappInfo info = null;
        for (ComponentConfig cc : getAllComponents()) {
            for (WebappInfo wInfo : cc.getWebappInfos()) {
                if (serverName.equals(wInfo.server) && contextRoot.equals(wInfo.getContextRoot())) {
                    info = wInfo;
                }
            }
        }
        return info;
    }

    public static boolean isFileResourceLoader(String componentName, String resourceLoaderName) throws ComponentException {
        ComponentConfig cc = getComponentConfig(componentName);
        if (cc == null) {
            throw new ComponentException("Could not find component with name: " + componentName);
        }
        return cc.isFileResourceLoader(resourceLoaderName);
    }

    // ========== ComponentConfig instance ==========

    protected String globalName = null;
    protected String rootLocation = null;
    protected String componentName = null;
    protected boolean enabled = true;

    protected Map<String, ResourceLoaderInfo> resourceLoaderInfos = new LinkedHashMap<String, ResourceLoaderInfo>();
    protected List<ClasspathInfo> classpathInfos = new ArrayList<ClasspathInfo>();
    protected List<EntityResourceInfo> entityResourceInfos = new ArrayList<EntityResourceInfo>();
    protected List<ServiceResourceInfo> serviceResourceInfos = new ArrayList<ServiceResourceInfo>();
    protected List<TestSuiteInfo> testSuiteInfos = new ArrayList<TestSuiteInfo>();
    protected List<KeystoreInfo> keystoreInfos = new ArrayList<KeystoreInfo>();
    protected List<WebappInfo> webappInfos = new ArrayList<WebappInfo>();
    protected List<ContainerConfig.Container> containers = new ArrayList<ContainerConfig.Container>();

    protected ComponentConfig() {
    }

    protected ComponentConfig(String globalName, String rootLocation) throws ComponentException {
        this.globalName = globalName;
        if (!rootLocation.endsWith("/")) {
            rootLocation = rootLocation + "/";
        }
        this.rootLocation = rootLocation.replace('\\', '/');
        File rootLocationDir = new File(rootLocation);
        if (!rootLocationDir.exists()) {
            throw new ComponentException("The component root location does not exist: " + rootLocation);
        }
        if (!rootLocationDir.isDirectory()) {
            throw new ComponentException("The component root location is not a directory: " + rootLocation);
        }
        String xmlFilename = rootLocation + "/" + OFBIZ_COMPONENT_XML_FILENAME;
        URL xmlUrl = UtilURL.fromFilename(xmlFilename);
        if (xmlUrl == null) {
            throw new ComponentException("Could not find the " + OFBIZ_COMPONENT_XML_FILENAME + " configuration file in the component root location: " + rootLocation);
        }
        Document ofbizComponentDocument = null;
        try {
            ofbizComponentDocument = UtilXml.readXmlDocument(xmlUrl, true);
        } catch (Exception e) {
            throw new ComponentException("Error reading the component config file: " + xmlUrl, e);
        }
        Element ofbizComponentElement = ofbizComponentDocument.getDocumentElement();
        this.componentName = ofbizComponentElement.getAttribute("name");
        this.enabled = "true".equalsIgnoreCase(ofbizComponentElement.getAttribute("enabled"));
        if (UtilValidate.isEmpty(this.globalName)) {
            this.globalName = this.componentName;
        }
        // resource-loader - resourceLoaderInfos
        for (Element curElement : UtilXml.childElementList(ofbizComponentElement, "resource-loader")) {
            ResourceLoaderInfo resourceLoaderInfo = new ResourceLoaderInfo(curElement);
            this.resourceLoaderInfos.put(resourceLoaderInfo.name, resourceLoaderInfo);
        }
        // classpath - classpathInfos
        for (Element curElement : UtilXml.childElementList(ofbizComponentElement, "classpath")) {
            ClasspathInfo classpathInfo = new ClasspathInfo(this, curElement);
            this.classpathInfos.add(classpathInfo);
        }
        // entity-resource - entityResourceInfos
        for (Element curElement : UtilXml.childElementList(ofbizComponentElement, "entity-resource")) {
            EntityResourceInfo entityResourceInfo = new EntityResourceInfo(this, curElement);
            this.entityResourceInfos.add(entityResourceInfo);
        }
        // service-resource - serviceResourceInfos
        for (Element curElement : UtilXml.childElementList(ofbizComponentElement, "service-resource")) {
            ServiceResourceInfo serviceResourceInfo = new ServiceResourceInfo(this, curElement);
            this.serviceResourceInfos.add(serviceResourceInfo);
        }
        // test-suite - serviceResourceInfos
        for (Element curElement : UtilXml.childElementList(ofbizComponentElement, "test-suite")) {
            TestSuiteInfo testSuiteInfo = new TestSuiteInfo(this, curElement);
            this.testSuiteInfos.add(testSuiteInfo);
        }
        // keystore - (cert/trust store infos)
        for (Element curElement : UtilXml.childElementList(ofbizComponentElement, "keystore")) {
            KeystoreInfo keystoreInfo = new KeystoreInfo(this, curElement);
            this.keystoreInfos.add(keystoreInfo);
        }
        // webapp - webappInfos
        for (Element curElement : UtilXml.childElementList(ofbizComponentElement, "webapp")) {
            WebappInfo webappInfo = new WebappInfo(this, curElement);
            this.webappInfos.add(webappInfo);
        }
        // containers
        try {
            this.containers.addAll(ContainerConfig.getContainers(xmlUrl));
        } catch (ContainerException ce) {
            throw new ComponentException("Error reading containers for component: " + this.globalName, ce);
        }
        if (Debug.verboseOn())
            Debug.logVerbose("Read component config : [" + rootLocation + "]", module);
    }

    public boolean enabled() {
        return this.enabled;
    }

    public List<ClasspathInfo> getClasspathInfos() {
        return this.classpathInfos;
    }

    public String getComponentName() {
        return this.componentName;
    }

    public List<ContainerConfig.Container> getContainers() {
        return this.containers;
    }

    public List<EntityResourceInfo> getEntityResourceInfos() {
        return this.entityResourceInfos;
    }

    public String getFullLocation(String resourceLoaderName, String location) throws ComponentException {
        ResourceLoaderInfo resourceLoaderInfo = resourceLoaderInfos.get(resourceLoaderName);
        if (resourceLoaderInfo == null) {
            throw new ComponentException("Could not find resource-loader named: " + resourceLoaderName);
        }
        StringBuilder buf = new StringBuilder();
        // pre-pend component root location if this is a type component resource-loader
        if ("component".equals(resourceLoaderInfo.type)) {
            buf.append(rootLocation);
        }

        if (UtilValidate.isNotEmpty(resourceLoaderInfo.prependEnv)) {
            String propValue = System.getProperty(resourceLoaderInfo.prependEnv);
            if (propValue == null) {
                String errMsg = "The Java environment (-Dxxx=yyy) variable with name " + resourceLoaderInfo.prependEnv + " is not set, cannot load resource.";
                Debug.logError(errMsg, module);
                throw new IllegalArgumentException(errMsg);
            }
            buf.append(propValue);
        }
        if (UtilValidate.isNotEmpty(resourceLoaderInfo.prefix)) {
            buf.append(resourceLoaderInfo.prefix);
        }
        buf.append(location);
        return buf.toString();
    }

    public String getGlobalName() {
        return this.globalName;
    }

    public List<KeystoreInfo> getKeystoreInfos() {
        return this.keystoreInfos;
    }

    public Map<String, ResourceLoaderInfo> getResourceLoaderInfos() {
        return this.resourceLoaderInfos;
    }

    public String getRootLocation() {
        return this.rootLocation;
    }

    public List<ServiceResourceInfo> getServiceResourceInfos() {
        return this.serviceResourceInfos;
    }

    public InputStream getStream(String resourceLoaderName, String location) throws ComponentException {
        URL url = getURL(resourceLoaderName, location);
        try {
            return url.openStream();
        } catch (java.io.IOException e) {
            throw new ComponentException("Error opening resource at location [" + url.toExternalForm() + "]", e);
        }
    }

    public List<TestSuiteInfo> getTestSuiteInfos() {
        return this.testSuiteInfos;
    }

    public URL getURL(String resourceLoaderName, String location) throws ComponentException {
        ResourceLoaderInfo resourceLoaderInfo = resourceLoaderInfos.get(resourceLoaderName);
        if (resourceLoaderInfo == null) {
            throw new ComponentException("Could not find resource-loader named: " + resourceLoaderName);
        }
        if ("component".equals(resourceLoaderInfo.type) || "file".equals(resourceLoaderInfo.type)) {
            String fullLocation = getFullLocation(resourceLoaderName, location);
            URL fileUrl = UtilURL.fromFilename(fullLocation);
            if (fileUrl == null) {
                throw new ComponentException("File Resource not found: " + fullLocation);
            }
            return fileUrl;
        } else if ("classpath".equals(resourceLoaderInfo.type)) {
            String fullLocation = getFullLocation(resourceLoaderName, location);
            URL url = UtilURL.fromResource(fullLocation);
            if (url == null) {
                throw new ComponentException("Classpath Resource not found: " + fullLocation);
            }
            return url;
        } else if ("url".equals(resourceLoaderInfo.type)) {
            String fullLocation = getFullLocation(resourceLoaderName, location);
            URL url = null;
            try {
                url = FlexibleLocation.resolveLocation(location);
            } catch (java.net.MalformedURLException e) {
                throw new ComponentException("Error with malformed URL while trying to load URL resource at location [" + fullLocation + "]", e);
            }
            if (url == null) {
                throw new ComponentException("URL Resource not found: " + fullLocation);
            }
            return url;
        } else {
            throw new ComponentException("The resource-loader type is not recognized: " + resourceLoaderInfo.type);
        }
    }

    public List<WebappInfo> getWebappInfos() {
        return this.webappInfos;
    }

    public boolean isFileResource(ResourceInfo resourceInfo) throws ComponentException {
        return isFileResourceLoader(resourceInfo.loader);
    }

    public boolean isFileResourceLoader(String resourceLoaderName) throws ComponentException {
        ResourceLoaderInfo resourceLoaderInfo = resourceLoaderInfos.get(resourceLoaderName);
        if (resourceLoaderInfo == null) {
            throw new ComponentException("Could not find resource-loader named: " + resourceLoaderName);
        }
        return "file".equals(resourceLoaderInfo.type) || "component".equals(resourceLoaderInfo.type);
    }

    public static class ClasspathInfo {
        public ComponentConfig componentConfig;
        public String type;
        public String location;

        public ClasspathInfo(ComponentConfig componentConfig, Element element) {
            this.componentConfig = componentConfig;
            this.type = element.getAttribute("type");
            this.location = element.getAttribute("location");
        }
    }

    // ComponentConfig instances need to be looked up by their global name and root location,
    // so this class encapsulates the Maps and synchronization code required to do that.
    private static final class ComponentConfigCache {
        // Key is the global name.
        private final Map<String, ComponentConfig> componentConfigs = new LinkedHashMap<String, ComponentConfig>();
        // Root location mapped to global name.
        private final Map<String, String> componentLocations = new HashMap<String, String>();
        
        private synchronized ComponentConfig fromGlobalName(String globalName) {
            return componentConfigs.get(globalName);
        }

        private synchronized ComponentConfig fromRootLocation(String rootLocation) {
            String globalName = componentLocations.get(rootLocation);
            if (globalName == null) {
                return null;
            }
            return componentConfigs.get(globalName);
        }

        private synchronized ComponentConfig put(ComponentConfig config) {
            String globalName = config.getGlobalName();
            String fileLocation = config.getRootLocation();
            componentLocations.put(fileLocation, globalName);
            return componentConfigs.put(globalName, config);
        }

        private synchronized Collection<ComponentConfig> values() {
            return Collections.unmodifiableList(new ArrayList<ComponentConfig>(componentConfigs.values()));
        }
    }

    public static class EntityResourceInfo extends ResourceInfo {
        public String type;
        public String readerName;

        public EntityResourceInfo(ComponentConfig componentConfig, Element element) {
            super(componentConfig, element);
            this.type = element.getAttribute("type");
            this.readerName = element.getAttribute("reader-name");
        }
    }

    public static class KeystoreInfo extends ResourceInfo {
        public String name;
        public String type;
        public String password;
        public boolean isCertStore;
        public boolean isTrustStore;

        public KeystoreInfo(ComponentConfig componentConfig, Element element) {
            super(componentConfig, element);
            this.name = element.getAttribute("name");
            this.type = element.getAttribute("type");
            this.password = element.getAttribute("password");
            this.isCertStore = "true".equalsIgnoreCase(element.getAttribute("is-certstore"));
            this.isTrustStore = "true".equalsIgnoreCase(element.getAttribute("is-truststore"));
        }

        public KeyStore getKeyStore() {
            ComponentResourceHandler rh = this.createResourceHandler();
            if (rh != null) {
                try {
                    return KeyStoreUtil.getStore(rh.getURL(), this.getPassword(), this.getType());
                } catch (Exception e) {
                    Debug.logWarning(e, module);
                }
            }
            return null;
        }

        public String getName() {
            return name;
        }

        public String getPassword() {
            return password;
        }

        public String getType() {
            return type;
        }

        public boolean isCertStore() {
            return isCertStore;
        }

        public boolean isTrustStore() {
            return isTrustStore;
        }
    }

    public static class ResourceInfo {
        public ComponentConfig componentConfig;
        public String loader;
        public String location;

        public ResourceInfo(ComponentConfig componentConfig, Element element) {
            this.componentConfig = componentConfig;
            this.loader = element.getAttribute("loader");
            this.location = element.getAttribute("location");
        }

        public ComponentResourceHandler createResourceHandler() {
            return new ComponentResourceHandler(componentConfig.getGlobalName(), loader, location);
        }

        public String getLocation() {
            return location;
        }
    }

    public static class ResourceLoaderInfo {
        public String name;
        public String type;
        public String prependEnv;
        public String prefix;

        public ResourceLoaderInfo(Element element) {
            this.name = element.getAttribute("name");
            this.type = element.getAttribute("type");
            this.prependEnv = element.getAttribute("prepend-env");
            this.prefix = element.getAttribute("prefix");
        }
    }

    public static class ServiceResourceInfo extends ResourceInfo {
        public String type;

        public ServiceResourceInfo(ComponentConfig componentConfig, Element element) {
            super(componentConfig, element);
            this.type = element.getAttribute("type");
        }
    }

    public static class TestSuiteInfo extends ResourceInfo {
        public TestSuiteInfo(ComponentConfig componentConfig, Element element) {
            super(componentConfig, element);
        }
    }

    public static class WebappInfo {
        public ComponentConfig componentConfig;
        public List<String> virtualHosts = new ArrayList<String>();
        public Map<String, String> initParameters = new LinkedHashMap<String, String>();
        public String name;
        public String title;
        public String description;
        public String menuName;
        public String server;
        public String mountPoint;
        public String location;
        public String[] basePermission;
        public String position;
        public boolean appBarDisplay;
        public boolean sessionCookieAccepted;
        public boolean privileged;

        public WebappInfo(ComponentConfig componentConfig, Element element) {
            this.componentConfig = componentConfig;
            this.name = element.getAttribute("name");
            this.title = element.getAttribute("title");
            this.description = element.getAttribute("description");
            this.server = element.getAttribute("server");
            this.mountPoint = element.getAttribute("mount-point");
            this.location = element.getAttribute("location");
            this.appBarDisplay = !"false".equals(element.getAttribute("app-bar-display"));
            this.sessionCookieAccepted = !"false".equals(element.getAttribute("session-cookie-accepted"));
            this.privileged = !"false".equals(element.getAttribute("privileged"));
            String basePermStr = element.getAttribute("base-permission");
            if (UtilValidate.isNotEmpty(basePermStr)) {
                this.basePermission = basePermStr.split(",");
            } else {
                // default base permission is NONE
                this.basePermission = new String[] { "NONE" };
            }
            // trim the permissions (remove spaces)
            for (int i = 0; i < this.basePermission.length; i++) {
                this.basePermission[i] = this.basePermission[i].trim();
                if (this.basePermission[i].indexOf('_') != -1) {
                    this.basePermission[i] = this.basePermission[i].substring(0, this.basePermission[i].indexOf('_'));
                }
            }
            // default title is name w/ upper-cased first letter
            if (UtilValidate.isEmpty(this.title)) {
                this.title = Character.toUpperCase(name.charAt(0)) + name.substring(1).toLowerCase();
            }
            if (UtilValidate.isEmpty(this.description)) {
                this.description = this.title;
            }
            String menuNameStr = element.getAttribute("menu-name");
            if (UtilValidate.isNotEmpty(menuNameStr)) {
                this.menuName = menuNameStr;
            } else {
                this.menuName = "main";
            }
            this.position = element.getAttribute("position");
            // default mount point is name if none specified
            if (UtilValidate.isEmpty(this.mountPoint)) {
                this.mountPoint = this.name;
            }
            // check the mount point and make sure it is properly formatted
            if (!"/".equals(this.mountPoint)) {
                if (!this.mountPoint.startsWith("/")) {
                    this.mountPoint = "/" + this.mountPoint;
                }
                if (!this.mountPoint.endsWith("/*")) {
                    if (!this.mountPoint.endsWith("/")) {
                        this.mountPoint = this.mountPoint + "/";
                    }
                    this.mountPoint = this.mountPoint + "*";
                }
            }
            // load the virtual hosts
            List<? extends Element> virtHostList = UtilXml.childElementList(element, "virtual-host");
            if (UtilValidate.isNotEmpty(virtHostList)) {
                for (Element e : virtHostList) {
                    virtualHosts.add(e.getAttribute("host-name"));
                }
            }
            // load the init parameters
            List<? extends Element> initParamList = UtilXml.childElementList(element, "init-param");
            if (UtilValidate.isNotEmpty(initParamList)) {
                for (Element e : initParamList) {
                    this.initParameters.put(e.getAttribute("name"), e.getAttribute("value"));
                }
            }
        }

        public String[] getBasePermission() {
            return this.basePermission;
        }

        public String getContextRoot() {
            if (mountPoint.endsWith("/*")) {
                return mountPoint.substring(0, mountPoint.length() - 2);
            }
            return mountPoint;
        }

        public String getDescription() {
            return description;
        }

        public Map<String, String> getInitParameters() {
            return initParameters;
        }

        public String getLocation() {
            return componentConfig.getRootLocation() + location;
        }

        public String getName() {
            return name;
        }

        public String getTitle() {
            return title;
        }

        public List<String> getVirtualHosts() {
            return virtualHosts;
        }

        public boolean isSessionCookieAccepted() {
            return sessionCookieAccepted;
        }
    }
}
