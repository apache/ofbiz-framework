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
package org.apache.ofbiz.base.component;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import org.apache.ofbiz.base.container.ContainerConfig;
import org.apache.ofbiz.base.container.ContainerConfig.Configuration;
import org.apache.ofbiz.base.container.ContainerException;
import org.apache.ofbiz.base.location.FlexibleLocation;
import org.apache.ofbiz.base.util.Assert;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.KeyStoreUtil;
import org.apache.ofbiz.base.util.UtilURL;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.UtilXml;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * An object that models the <code>&lt;ofbiz-component&gt;</code> element.
 *
 * @see <code>ofbiz-component.xsd</code>
 *
 */
public final class ComponentConfig {

    public static final String module = ComponentConfig.class.getName();
    public static final String OFBIZ_COMPONENT_XML_FILENAME = "ofbiz-component.xml";
    // This map is not a UtilCache instance because there is no strategy or implementation for reloading components.
    private static final ComponentConfigCache componentConfigCache = new ComponentConfigCache();

    public static Boolean componentExists(String componentName) {
        Assert.notEmpty("componentName", componentName);
        return componentConfigCache.fromGlobalName(componentName) != null;
    }

    /**
     * Provides the list of all the classpath information available in components.
     *
     * @return a list of classpath information
     */
    public static List<ClasspathInfo> getAllClasspathInfos() {
        return getAllComponents().stream()
                .flatMap(cc -> cc.getClasspathInfos().stream())
                .collect(Collectors.toList());
    }

    public static Collection<ComponentConfig> getAllComponents() {
        return componentConfigCache.values();
    }

    /**
     * Provides the list of all the container configuration elements available in components.
     *
     * @return a list of container configuration elements
     */
    public static List<ContainerConfig.Configuration> getAllConfigurations() {
        return getAllComponents().stream()
                .flatMap(cc -> cc.getConfigurations().stream())
                .collect(Collectors.toList());
    }

    public static List<EntityResourceInfo> getAllEntityResourceInfos(String type) {
        return getAllEntityResourceInfos(type, null);
    }

    public static List<EntityResourceInfo> getAllEntityResourceInfos(String type, String componentName) {
        List<EntityResourceInfo> entityInfos = new ArrayList<>();
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

    /**
     * Provides the list of all the keystore information available in components.
     *
     * @return a list of keystore information
     */
    public static List<KeystoreInfo> getAllKeystoreInfos() {
        return getAllComponents().stream()
                .flatMap(cc -> cc.getKeystoreInfos().stream())
                .collect(Collectors.toList());
    }

    /**
     * Provides the list of all the service resource information matching a type.
     *
     * @param type  the service resource type to match
     * @return a list of service resource information
     */
    public static List<ServiceResourceInfo> getAllServiceResourceInfos(String type) {
        return getAllComponents().stream()
                .flatMap(cc -> cc.getServiceResourceInfos().stream())
                .filter(sri -> UtilValidate.isEmpty(type) || type.equals(sri.type))
                .collect(Collectors.toList());
    }

    /**
     * Provides the list of all the test-suite information matching a component name.
     *
     * @param name  the name of the component to match where {@code null} means "any"
     * @return a list of test-suite information
     */
    public static List<TestSuiteInfo> getAllTestSuiteInfos(String name) {
        return getAllComponents().stream()
                .filter(cc -> name == null || name.equals(cc.getComponentName()))
                .flatMap(cc -> cc.getTestSuiteInfos().stream())
                .collect(Collectors.toList());
    }

    /**
     * Provides the list of all the web-app information in components
     *
     * @return a list of web-app information
     */
    public static List<WebappInfo> getAllWebappResourceInfos() {
        return getAllComponents().stream()
                .flatMap(cc -> cc.getWebappInfos().stream())
                .collect(Collectors.toList());
    }

    public static ComponentConfig getComponentConfig(String globalName) throws ComponentException {
        // TODO: we need to look up the rootLocation from the container config, or this will blow up
        return getComponentConfig(globalName, null);
    }

    public static ComponentConfig getComponentConfig(String globalName, String rootLocation) throws ComponentException {
        ComponentConfig componentConfig;
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
        if (rootLocation == null) {
            // Do we really need to do this?
            throw new ComponentException("No component found named : " + globalName);
        }
        componentConfig = new ComponentConfig(globalName, rootLocation);
        if (componentConfig.enabled()) {
            componentConfigCache.put(componentConfig);
        }
        return componentConfig;
    }

    public static String getFullLocation(String componentName, String resourceLoaderName, String location) throws ComponentException {
        ComponentConfig cc = getComponentConfig(componentName, null);
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
        return cc.getRootLocation();
    }

    public static InputStream getStream(String componentName, String resourceLoaderName, String location) throws ComponentException {
        ComponentConfig cc = getComponentConfig(componentName);
        return cc.getStream(resourceLoaderName, location);
    }

    public static URL getURL(String componentName, String resourceLoaderName, String location) throws ComponentException {
        ComponentConfig cc = getComponentConfig(componentName);
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
    
    // ========== ComponentConfig instance ==========

    private final String globalName;
    private final String rootLocation;
    private final String componentName;
    private final boolean enabled;
    private final Map<String, ResourceLoaderInfo> resourceLoaderInfos;
    private final List<ClasspathInfo> classpathInfos;
    private final List<DependsOnInfo> dependsOnInfos;
    private final List<EntityResourceInfo> entityResourceInfos;
    private final List<ServiceResourceInfo> serviceResourceInfos;
    private final List<TestSuiteInfo> testSuiteInfos;
    private final List<KeystoreInfo> keystoreInfos;
    private final List<WebappInfo> webappInfos;
    private final List<ContainerConfig.Configuration> configurations;

    /**
     * Instantiates a component configuration from a {@link ComponentConfig.Builder builder} object.
     *
     * This allows instantiating component configuration without an XML entity object,
     * which is useful for example when writing unit tests.
     *
     * @param b the component configuration builder
     */
    private ComponentConfig(Builder b) {
        this.globalName = b.globalName;
        this.rootLocation = b.rootLocation;
        this.componentName = b.componentName;
        this.enabled = b.enabled;
        this.resourceLoaderInfos = b.resourceLoaderInfos;
        this.classpathInfos = b.classpathInfos;
        this.dependsOnInfos = b.dependsOnInfos;
        this.entityResourceInfos = b.entityResourceInfos;
        this.serviceResourceInfos = b.serviceResourceInfos;
        this.testSuiteInfos = b.testSuiteInfos;
        this.keystoreInfos = b.keystoreInfos;
        this.webappInfos = b.webappInfos;
        this.configurations = b.configurations;
    }

    /**
     * Builder for component configuration.
     */
    public static final class Builder {
        private String globalName;
        private String rootLocation;
        private String componentName;
        private boolean enabled = true;
        private Map<String, ResourceLoaderInfo> resourceLoaderInfos;
        private List<ClasspathInfo> classpathInfos;
        private List<DependsOnInfo> dependsOnInfos;
        private List<EntityResourceInfo> entityResourceInfos;
        private List<ServiceResourceInfo> serviceResourceInfos;
        private List<TestSuiteInfo> testSuiteInfos;
        private List<KeystoreInfo> keystoreInfos;
        private List<WebappInfo> webappInfos;
        private List<ContainerConfig.Configuration> configurations;

        public Builder globalName(String name) {
            this.globalName = name;
            return this;
        }

        public Builder rootLocation(String rootLocation) {
            this.rootLocation = rootLocation;
            return this;
        }

        public Builder componentName(String componentName) {
            this.componentName = componentName;
            return this;
        }

        public Builder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Builder resourceLoaderInfos(Map<String, ResourceLoaderInfo> resourceLoaderInfos) {
            this.resourceLoaderInfos = resourceLoaderInfos;
            return this;
        }

        public Builder classpathInfos(List<ClasspathInfo> classpathInfos) {
            this.classpathInfos = classpathInfos;
            return this;
        }

        public Builder dependsOnInfos(List<DependsOnInfo> dependsOnInfos) {
            this.dependsOnInfos = dependsOnInfos;
            return this;
        }

        public Builder entityResourceInfos(List<EntityResourceInfo> entityResourceInfos) {
            this.entityResourceInfos = entityResourceInfos;
            return this;
        }

        public Builder serviceResourceInfos(List<ServiceResourceInfo> serviceResourceInfos) {
            this.serviceResourceInfos = serviceResourceInfos;
            return this;
        }

        public Builder testSuiteInfos(List<TestSuiteInfo> testSuiteInfos) {
            this.testSuiteInfos = testSuiteInfos;
            return this;
        }

        public Builder keystoreInfos(List<KeystoreInfo> keystoreInfos) {
            this.keystoreInfos = keystoreInfos;
            return this;
        }

        public Builder webappInfos(List<WebappInfo> webappInfos) {
            this.webappInfos = webappInfos;
            return this;
        }

        public Builder configurations(List<ContainerConfig.Configuration> configurations) {
            this.configurations = configurations;
            return this;
        }

        public ComponentConfig create() {
           return new ComponentConfig(this);
        }
    }

    /**
     * Instantiates a component config from a component name and a root location.
     *
     * @param globalName  the global name of the component which can be {@code null}
     * @param rootLocation  the root location of the component
     * @throws ComponentException when component directory does not exist or if the
     *         {@code ofbiz-component.xml} file of that component is not properly defined
     * @throws NullPointerException when {@code rootLocation} is {@code null}
     */
    private ComponentConfig(String globalName, String rootLocation) throws ComponentException {
        if (!rootLocation.endsWith("/")) {
            rootLocation = rootLocation + "/";
        }
        this.rootLocation = rootLocation.replace('\\', '/');
        File rootLocationDir = new File(rootLocation);
        if (!rootLocationDir.exists()) {
            throw new ComponentException("The component root location does not exist: " + rootLocation);
        } else if (!rootLocationDir.isDirectory()) {
            throw new ComponentException("The component root location is not a directory: " + rootLocation);
        }
        String xmlFilename = this.rootLocation + "/" + OFBIZ_COMPONENT_XML_FILENAME;
        URL xmlUrl = UtilURL.fromFilename(xmlFilename);
        if (xmlUrl == null) {
            throw new ComponentException("Could not find the " + OFBIZ_COMPONENT_XML_FILENAME
                    + " configuration file in the component root location: " + rootLocation);
        }
        Element componentElement = null;
        try {
            Document ofbizComponentDocument = UtilXml.readXmlDocument(xmlUrl, true);
            componentElement = ofbizComponentDocument.getDocumentElement();
        } catch (Exception e) {
            throw new ComponentException("Error reading the component config file: " + xmlUrl, e);
        }

        componentName = componentElement.getAttribute("name");
        enabled = "true".equalsIgnoreCase(componentElement.getAttribute("enabled"));
        this.globalName = UtilValidate.isEmpty(globalName) ? componentName : globalName;
        dependsOnInfos = collectElements(componentElement, "depends-on", DependsOnInfo::new);
        classpathInfos = collectElements(componentElement, "classpath", ClasspathInfo::new);
        entityResourceInfos = collectElements(componentElement, "entity-resource", EntityResourceInfo::new);
        serviceResourceInfos = collectElements(componentElement, "service-resource", ServiceResourceInfo::new);
        testSuiteInfos = collectElements(componentElement, "test-suite", TestSuiteInfo::new);
        keystoreInfos = collectElements(componentElement, "keystore", KeystoreInfo::new);
        webappInfos = collectElements(componentElement, "webapp", WebappInfo::new);
        resourceLoaderInfos = UtilXml.childElementList(componentElement, "resource-loader").stream()
                .map(ResourceLoaderInfo::new)
                .collect(Collectors.collectingAndThen(
                        Collectors.toMap(rli -> rli.name, rli -> rli),
                        Collections::unmodifiableMap));
        try {
            Collection<Configuration> configurations = ContainerConfig.getConfigurations(xmlUrl);
            this.configurations = Collections.unmodifiableList(new ArrayList<>(configurations));
        } catch (ContainerException ce) {
            throw new ComponentException("Error reading container configurations for component: " + this.globalName, ce);
        }
        if (Debug.verboseOn()) {
            Debug.logVerbose("Read component config : [" + rootLocation + "]", module);
        }
    }

    /**
     * Constructs an immutable list of objects from the the childs of an XML element.
     *
     * @param ofbizComponentElement  the XML element containing the childs
     * @param elemName  the name of the child elements to collect
     * @param mapper  the constructor use to map child elements to objects
     * @return an immutable list of objects corresponding to {@code mapper}
     */
    private <T> List<T> collectElements(Element ofbizComponentElement, String elemName,
            BiFunction<ComponentConfig, Element, T> mapper) {
        return UtilXml.childElementList(ofbizComponentElement, elemName).stream()
                .map(element -> mapper.apply(this, element))
                .collect(Collectors.collectingAndThen(Collectors.toList(), Collections::unmodifiableList));
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

    public List<ContainerConfig.Configuration> getConfigurations() {
        return this.configurations;
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

    public List<DependsOnInfo> getDependsOn() {
        return this.dependsOnInfos;
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

    /**
     * An object that models the <code>&lt;classpath&gt;</code> element.
     *
     * @see <code>ofbiz-component.xsd</code>
     *
     */
    public static final class ClasspathInfo {
        public final ComponentConfig componentConfig;
        public final String type;
        public final String location;

        private ClasspathInfo(ComponentConfig componentConfig, Element element) {
            this.componentConfig = componentConfig;
            this.type = element.getAttribute("type");
            this.location = element.getAttribute("location");
        }
    }

    // ComponentConfig instances need to be looked up by their global name and root location,
    // so this class encapsulates the Maps and synchronization code required to do that.
    private static final class ComponentConfigCache {
        // Key is the global name.
        private final Map<String, ComponentConfig> componentConfigs = new LinkedHashMap<>();
        // Root location mapped to global name.
        private final Map<String, String> componentLocations = new HashMap<>();

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
            return Collections.unmodifiableList(new ArrayList<>(componentConfigs.values()));
        }
    }

    /**
     * An object that models the <code>&lt;entity-resource&gt;</code> element.
     *
     * @see <code>ofbiz-component.xsd</code>
     *
     */
    public static final class EntityResourceInfo extends ResourceInfo {
        public final String type;
        public final String readerName;

        private EntityResourceInfo(ComponentConfig componentConfig, Element element) {
            super(componentConfig, element);
            this.type = element.getAttribute("type");
            this.readerName = element.getAttribute("reader-name");
        }
    }

    public static final class DependsOnInfo extends ResourceInfo {
        public final String componentName;

        private DependsOnInfo(ComponentConfig componentConfig, Element element) {
            super(componentConfig, element);
            this.componentName = element.getAttribute("component-name");
        }
    }

    /**
     * An object that models the <code>&lt;keystore&gt;</code> element.
     *
     * @see <code>ofbiz-component.xsd</code>
     *
     */
    public static final class KeystoreInfo extends ResourceInfo {
        private final String name;
        private final String type;
        private final String password;
        private final boolean isCertStore;
        private final boolean isTrustStore;

        private KeystoreInfo(ComponentConfig componentConfig, Element element) {
            super(componentConfig, element);
            this.name = element.getAttribute("name");
            this.type = element.getAttribute("type");
            this.password = element.getAttribute("password");
            this.isCertStore = "true".equalsIgnoreCase(element.getAttribute("is-certstore"));
            this.isTrustStore = "true".equalsIgnoreCase(element.getAttribute("is-truststore"));
        }

        public KeyStore getKeyStore() {
            ComponentResourceHandler rh = this.createResourceHandler();
            try {
                return KeyStoreUtil.getStore(rh.getURL(), this.getPassword(), this.getType());
            } catch (Exception e) {
                Debug.logWarning(e, module);
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

    public static abstract class ResourceInfo {
        private final ComponentConfig componentConfig;
        private final String loader;
        private final String location;

        protected ResourceInfo(ComponentConfig componentConfig, Element element) {
            this.componentConfig = componentConfig;
            this.loader = element.getAttribute("loader");
            this.location = element.getAttribute("location");
        }

        public ComponentResourceHandler createResourceHandler() {
            return new ComponentResourceHandler(componentConfig.getGlobalName(), loader, location);
        }

        public ComponentConfig getComponentConfig() {
            return componentConfig;
        }

        public String getLocation() {
            return location;
        }
    }

    /**
     * An object that models the <code>&lt;resource-loader&gt;</code> element.
     *
     * @see <code>ofbiz-component.xsd</code>
     *
     */
    public static final class ResourceLoaderInfo {
        public final String name;
        public final String type;
        public final String prependEnv;
        public final String prefix;

        private ResourceLoaderInfo(Element element) {
            this.name = element.getAttribute("name");
            this.type = element.getAttribute("type");
            this.prependEnv = element.getAttribute("prepend-env");
            this.prefix = element.getAttribute("prefix");
        }
    }

    /**
     * An object that models the <code>&lt;service-resource&gt;</code> element.
     *
     * @see <code>ofbiz-component.xsd</code>
     *
     */
    public static final class ServiceResourceInfo extends ResourceInfo {
        public final String type;

        private ServiceResourceInfo(ComponentConfig componentConfig, Element element) {
            super(componentConfig, element);
            this.type = element.getAttribute("type");
        }
    }

    /**
     * An object that models the <code>&lt;test-suite&gt;</code> element.
     *
     * @see <code>ofbiz-component.xsd</code>
     *
     */
    public static final class TestSuiteInfo extends ResourceInfo {
        public TestSuiteInfo(ComponentConfig componentConfig, Element element) {
            super(componentConfig, element);
        }
    }

    /**
     * An object that models the <code>&lt;webapp&gt;</code> element.
     *
     * @see <code>ofbiz-component.xsd</code>
     *
     */
    public static final class WebappInfo {
        // FIXME: These fields should be private - since we have accessors - but
        // client code accesses the fields directly.
        public final ComponentConfig componentConfig;
        public final List<String> virtualHosts;
        public final Map<String, String> initParameters;
        public final String name;
        public final String title;
        public final String description;
        public final String menuName;
        public final String server;
        public final String mountPoint;
        public final String contextRoot;
        public final String location;
        public final String[] basePermission;
        public final String position;
        public final boolean privileged;
        // CatalinaContainer modifies this field.
        private volatile boolean appBarDisplay;
        private final String accessPermission;
        private final boolean useAutologinCookie;

        /**
         * Instantiates a webapp information from a {@link WebappInfo.Builder builder} object.
         *
         * This allows instantiating webapp information without an XML entity object,
         * which is useful for example when writing unit tests.
         *
         * @param b the webapp information builder
         */
        private WebappInfo(Builder b) {
            this.componentConfig = b.componentConfig;
            this.virtualHosts = b.virtualHosts;
            this.initParameters = b.initParameters;
            this.name = b.name;
            this.title = b.title;
            this.description = b.description;
            this.menuName = b.menuName;
            this.server = b.server;
            this.mountPoint = b.mountPoint;
            this.contextRoot = b.contextRoot;
            this.location = b.location;
            this.basePermission = b.basePermissions;
            this.position = b.position;
            this.privileged = b.privileged;
            this.appBarDisplay = b.appBarDisplay;
            this.accessPermission = b.accessPermission;
            this.useAutologinCookie = b.useAutologinCookie;
        }

        /**
         * Builder for webapp information.
         */
        public static class Builder {
            private ComponentConfig componentConfig;
            private List<String> virtualHosts;
            private Map<String, String> initParameters;
            private String name;
            private String title;
            private String description;
            private String menuName;
            private String server;
            private String mountPoint = "";
            private String contextRoot;
            private String location;
            private String[] basePermissions;
            private String position;
            private boolean privileged = false;
            private boolean appBarDisplay = true;
            private String accessPermission;
            private boolean useAutologinCookie;

            public Builder componentConfig(ComponentConfig componentConfig) {
                this.componentConfig = componentConfig;
                return this;
            }

            public Builder virtualHosts(List<String> virtualHosts) {
                this.virtualHosts = virtualHosts;
                return this;
            }

            public Builder initParameters(Map<String, String> initParameters) {
                this.initParameters = initParameters;
                return this;
            }

            public Builder name(String name) {
                this.name = name;
                return this;
            }

            public Builder title(String title) {
                this.title = title;
                return this;
            }

            public Builder description(String description) {
                this.description = description;
                return this;
            }

            public Builder menuName(String menuName) {
                this.menuName = menuName;
                return this;
            }

            public Builder server(String server) {
                this.server = server;
                return this;
            }

            public Builder mountPoint(String mountPoint) {
                this.mountPoint = mountPoint;
                return this;
            }

            public Builder contextRoot(String contextRoot) {
                this.contextRoot = contextRoot;
                return this;
            }

            public Builder location(String location) {
                this.location = location;
                return this;
            }

            public Builder basePermissions(String[] basePermissions) {
                this.basePermissions = basePermissions;
                return this;
            }

            public Builder position(String position) {
                this.position = position;
                return this;
            }

            public Builder privileged(boolean privileged) {
                this.privileged = privileged;
                return this;
            }

            public Builder appBarDisplay(boolean appBarDisplay) {
                this.appBarDisplay = appBarDisplay;
                return this;
            }

            public Builder accessPermission(String accessPermission) {
                this.accessPermission = accessPermission;
                return this;
            }

            public Builder useAutologinCookie(boolean useAutologinCookie) {
                this.useAutologinCookie = useAutologinCookie;
                return this;
            }

            public WebappInfo create() {
                return new WebappInfo(this);
            }
        }

        private WebappInfo(ComponentConfig componentConfig, Element element) {
            this.componentConfig = componentConfig;
            this.name = element.getAttribute("name");
            String title = element.getAttribute("title");
            if (title.isEmpty()) {
                // default title is name w/ upper-cased first letter
                title = Character.toUpperCase(name.charAt(0)) + name.substring(1).toLowerCase();
            }
            this.title = title;
            String description = element.getAttribute("description");
            if (description.isEmpty()) {
                description = this.title;
            }
            this.description = description;
            this.server = element.getAttribute("server");
            String mountPoint = element.getAttribute("mount-point");
            // check the mount point and make sure it is properly formatted
            if (!mountPoint.isEmpty()) {
                if (!mountPoint.startsWith("/")) {
                    mountPoint = "/" + mountPoint;
                }
                if (!mountPoint.endsWith("/*")) {
                    if (!mountPoint.endsWith("/")) {
                        mountPoint = mountPoint + "/";
                    }
                    mountPoint = mountPoint + "*";
                }
            }
            this.mountPoint = mountPoint;
            if (this.mountPoint.endsWith("/*")) {
                this.contextRoot = this.mountPoint.substring(0, this.mountPoint.length() - 2);
            } else {
                this.contextRoot = this.mountPoint;
            }
            this.location = element.getAttribute("location");
            this.appBarDisplay = !"false".equals(element.getAttribute("app-bar-display"));
            this.privileged = !"false".equals(element.getAttribute("privileged"));
            this.accessPermission = element.getAttribute("access-permission");
            this.useAutologinCookie = !"false".equals(element.getAttribute("use-autologin-cookie"));
            String basePermStr = element.getAttribute("base-permission");
            if (!basePermStr.isEmpty()) {
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
            String menuNameStr = element.getAttribute("menu-name");
            if (UtilValidate.isNotEmpty(menuNameStr)) {
                this.menuName = menuNameStr;
            } else {
                this.menuName = "main";
            }
            this.position = element.getAttribute("position");
            // load the virtual hosts
            List<? extends Element> virtHostList = UtilXml.childElementList(element, "virtual-host");
            if (!virtHostList.isEmpty()) {
                List<String> virtualHosts = new ArrayList<>(virtHostList.size());
                for (Element e : virtHostList) {
                    virtualHosts.add(e.getAttribute("host-name"));
                }
                this.virtualHosts = Collections.unmodifiableList(virtualHosts);
            } else {
                this.virtualHosts = Collections.emptyList();
            }
            // load the init parameters
            List<? extends Element> initParamList = UtilXml.childElementList(element, "init-param");
            if (!initParamList.isEmpty()) {
                Map<String, String> initParameters = new LinkedHashMap<>();
                for (Element e : initParamList) {
                    initParameters.put(e.getAttribute("name"), e.getAttribute("value"));
                }
                this.initParameters = Collections.unmodifiableMap(initParameters);
            } else {
                this.initParameters = Collections.emptyMap();
            }
        }

        public synchronized boolean getAppBarDisplay() {
            return this.appBarDisplay;
        }

        public String getAccessPermission() {
            return this.accessPermission;
        }

        public String[] getBasePermission() {
            return this.basePermission.clone();
        }

        public String getContextRoot() {
            return contextRoot;
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

        public String getMountPoint() {
            return mountPoint;
        }

        public String getTitle() {
            return title;
        }

        public List<String> getVirtualHosts() {
            return virtualHosts;
        }

        public boolean isAutologinCookieUsed() {
            return useAutologinCookie;
        }

        public synchronized void setAppBarDisplay(boolean appBarDisplay) {
            this.appBarDisplay = appBarDisplay;
        }
    }
}
