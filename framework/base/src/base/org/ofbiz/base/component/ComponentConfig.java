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
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.security.KeyStore;
import javax.xml.parsers.ParserConfigurationException;

import javolution.util.FastList;
import javolution.util.FastMap;
import org.ofbiz.base.util.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 * ComponentConfig - Component configuration class for ofbiz-container.xml
 *
 */
public class ComponentConfig {

    public static final String module = ComponentConfig.class.getName();
    public static final String OFBIZ_COMPONENT_XML_FILENAME = "ofbiz-component.xml";

    // this is not a UtilCache because reloading may cause problems
    protected static Map componentConfigs = FastMap.newInstance();
    protected static Map serverWebApps = FastMap.newInstance();

    public static ComponentConfig getComponentConfig(String globalName) throws ComponentException {
        // TODO: we need to look up the rootLocation from the container config, or this will blow up
        return getComponentConfig(globalName, null);
    }

    public static ComponentConfig getComponentConfig(String globalName, String rootLocation) throws ComponentException {
        ComponentConfig componentConfig = null;
        if (UtilValidate.isNotEmpty(globalName)) {
            componentConfig = (ComponentConfig) componentConfigs.get(globalName);
        }
        if (componentConfig == null) {
            if (rootLocation != null) {
                synchronized (ComponentConfig.class) {
                    if (UtilValidate.isNotEmpty(globalName)) {
                        componentConfig = (ComponentConfig) componentConfigs.get(globalName);
                    }
                    if (componentConfig == null) {
                        componentConfig = new ComponentConfig(globalName, rootLocation);
                        if (componentConfigs.containsKey(componentConfig.getGlobalName())) {
                            Debug.logWarning("WARNING: Loading ofbiz-component using a global name that already exists, will over-write: " + componentConfig.getGlobalName(), module);
                        }
                        if (componentConfig.enabled()) {
                            componentConfigs.put(componentConfig.getGlobalName(), componentConfig);
                        }
                    }
                }
            } else {
                throw new ComponentException("No component found named : " + globalName);
            }
        }
        return componentConfig;
    }

    public static Collection getAllComponents() {
        Collection values = componentConfigs.values();
        if (values != null) {
            return values;
        } else {
            Debug.logWarning("No components were found, something is probably missing or incorrect in the component-load setup.", module);
            return FastList.newInstance();
        }
    }

    public static List getAllClasspathInfos() {
        return getAllClasspathInfos(null);
    }

    public static List getAllClasspathInfos(String componentName) {
        List classpaths = FastList.newInstance();
        Iterator i = getAllComponents().iterator();
        while (i.hasNext()) {
            ComponentConfig cc = (ComponentConfig) i.next();
            if (componentName == null || componentName.equals(cc.getComponentName())) {
                classpaths.addAll(cc.getClasspathInfos());
            }
        }
        return classpaths;
    }

    public static List getAllEntityResourceInfos(String type) {
        return getAllEntityResourceInfos(type, null);
    }

    public static List getAllEntityResourceInfos(String type, String componentName) {
        List entityInfos = FastList.newInstance();
        Iterator i = getAllComponents().iterator();
        while (i.hasNext()) {
            ComponentConfig cc = (ComponentConfig) i.next();
            if (componentName == null || componentName.equals(cc.getComponentName())) {
                List ccEntityInfoList = cc.getEntityResourceInfos();
                if (UtilValidate.isEmpty(type)) {
                    entityInfos.addAll(ccEntityInfoList);
                } else {
                    Iterator ccEntityInfoIter = ccEntityInfoList.iterator();
                    while (ccEntityInfoIter.hasNext()) {
                        EntityResourceInfo entityResourceInfo = (EntityResourceInfo) ccEntityInfoIter.next();
                        if (type.equals(entityResourceInfo.type)) {
                            entityInfos.add(entityResourceInfo);
                        }
                    }
                }
            }
        }
        return entityInfos;
    }

    public static List getAllServiceResourceInfos(String type) {
        return getAllServiceResourceInfos(type, null);
    }

    public static List getAllServiceResourceInfos(String type, String componentName) {
        List serviceInfos = FastList.newInstance();
        Iterator i = getAllComponents().iterator();
        while (i.hasNext()) {
            ComponentConfig cc = (ComponentConfig) i.next();
            if (componentName == null || componentName.equals(cc.getComponentName())) {
                List ccServiceInfoList = cc.getServiceResourceInfos();
                if (UtilValidate.isEmpty(type)) {
                    serviceInfos.addAll(ccServiceInfoList);
                } else {
                    Iterator ccServiceInfoIter = ccServiceInfoList.iterator();
                    while (ccServiceInfoIter.hasNext()) {
                        ServiceResourceInfo serviceResourceInfo = (ServiceResourceInfo) ccServiceInfoIter.next();
                        if (type.equals(serviceResourceInfo.type)) {
                            serviceInfos.add(serviceResourceInfo);
                        }
                    }
                }
            }
        }
        return serviceInfos;
    }

    public static List getAllTestSuiteInfos() {
        return getAllTestSuiteInfos(null);
    }

    public static List getAllTestSuiteInfos(String componentName) {
        List testSuiteInfos = FastList.newInstance();
        Iterator i = getAllComponents().iterator();
        while (i.hasNext()) {
            ComponentConfig cc = (ComponentConfig) i.next();
            if (componentName == null || componentName.equals(cc.getComponentName())) {
                testSuiteInfos.addAll(cc.getTestSuiteInfos());
            }
        }
        return testSuiteInfos;
    }

    public static List getAllKeystoreInfos() {
        return getAllKeystoreInfos(null);
    }

    public static List getAllKeystoreInfos(String componentName) {
        List keystoreInfos = FastList.newInstance();
        Iterator i = getAllComponents().iterator();
        while (i.hasNext()) {
            ComponentConfig cc = (ComponentConfig) i.next();
            if (componentName == null || componentName.equals(cc.getComponentName())) {
                keystoreInfos.addAll(cc.getKeystoreInfos());
            }
        }
        return keystoreInfos;
    }

    public static KeystoreInfo getKeystoreInfo(String componentName, String keystoreName) {
        Iterator i = getAllComponents().iterator();
        while (i.hasNext()) {
            ComponentConfig cc = (ComponentConfig) i.next();
            if (componentName != null && componentName.equals(cc.getComponentName())) {
                Iterator ki = cc.getKeystoreInfos().iterator();
                while (ki.hasNext()) {
                    KeystoreInfo ks = (KeystoreInfo) ki.next();
                    if (keystoreName != null && keystoreName.equals(ks.getName())) {
                        return ks;
                    }
                }
            }
        }

        return null;
    }

    public static List getAllWebappResourceInfos() {
        return getAllWebappResourceInfos(null);
    }

    public static List getAllWebappResourceInfos(String componentName) {
        List webappInfos = FastList.newInstance();
        Iterator i = getAllComponents().iterator();
        while (i.hasNext()) {
            ComponentConfig cc = (ComponentConfig) i.next();
            if (componentName == null || componentName.equals(cc.getComponentName())) {
                webappInfos.addAll(cc.getWebappInfos());
            }
        }
        return webappInfos;
    }

    public static boolean isFileResourceLoader(String componentName, String resourceLoaderName) throws ComponentException {
        ComponentConfig cc = ComponentConfig.getComponentConfig(componentName);
        if (cc == null) {
            throw new ComponentException("Could not find component with name: " + componentName);
        }
        return cc.isFileResourceLoader(resourceLoaderName);
    }

    public static InputStream getStream(String componentName, String resourceLoaderName, String location) throws ComponentException {
        ComponentConfig cc = ComponentConfig.getComponentConfig(componentName);
        if (cc == null) {
            throw new ComponentException("Could not find component with name: " + componentName);
        }
        return cc.getStream(resourceLoaderName, location);
    }

    public static URL getURL(String componentName, String resourceLoaderName, String location) throws ComponentException {
        ComponentConfig cc = ComponentConfig.getComponentConfig(componentName);
        if (cc == null) {
            throw new ComponentException("Could not find component with name: " + componentName);
        }
        return cc.getURL(resourceLoaderName, location);
    }

    public static String getFullLocation(String componentName, String resourceLoaderName, String location) throws ComponentException {
        ComponentConfig cc = ComponentConfig.getComponentConfig(componentName);
        if (cc == null) {
            throw new ComponentException("Could not find component with name: " + componentName);
        }
        return cc.getFullLocation(resourceLoaderName, location);
    }

    public static String getRootLocation(String componentName) throws ComponentException {
        ComponentConfig cc = ComponentConfig.getComponentConfig(componentName);
        if (cc == null) {
            throw new ComponentException("Could not find component with name: " + componentName);
        }
        return cc.getRootLocation();
    }

    public static List getAppBarWebInfos(String serverName) {
        return ComponentConfig.getAppBarWebInfos(serverName, null);
    }

    public static List getAppBarWebInfos(String serverName,  Comparator comp) {
        List webInfos = (List) serverWebApps.get(serverName);
        if (webInfos == null) {
            synchronized (ComponentConfig.class) {
                if (webInfos == null) {
                    Map tm = null;
                    Iterator i = getAllComponents().iterator();

                    // use a TreeMap to sort the components alpha by title
                    if (comp != null) {
                        tm = new TreeMap(comp);
                    } else {
                        tm = new TreeMap();
                    }

                    while (i.hasNext()) {
                        ComponentConfig cc = (ComponentConfig) i.next();
                        Iterator wi = cc.getWebappInfos().iterator();
                        while (wi.hasNext()) {
                            ComponentConfig.WebappInfo wInfo = (ComponentConfig.WebappInfo) wi.next();
                            if (serverName.equals(wInfo.server) && wInfo.appBarDisplay) {
                                tm.put(wInfo.title, wInfo);
                            }
                        }
                    }
                    List webInfoList = FastList.newInstance();
                    webInfoList.addAll(tm.values());
                    serverWebApps.put(serverName, webInfoList);
                    return webInfoList;
                }
            }
        }
        return webInfos;
    }

    public static WebappInfo getWebAppInfo(String serverName, String contextRoot) {
        ComponentConfig.WebappInfo info = null;
        if (serverName == null || contextRoot == null) {
            return info;
        }

        Iterator i = getAllComponents().iterator();
        while (i.hasNext() && info == null) {
            ComponentConfig cc = (ComponentConfig) i.next();
            Iterator wi = cc.getWebappInfos().iterator();
            while (wi.hasNext()) {
                ComponentConfig.WebappInfo wInfo = (ComponentConfig.WebappInfo) wi.next();
                if (serverName.equals(wInfo.server) && contextRoot.equals(wInfo.getContextRoot())) {
                    info = wInfo;
                }
            }
        }
        return info;
    }

    // ========== component info fields ==========
    protected String globalName = null;
    protected String rootLocation = null;
    protected String componentName = null;
    protected boolean enabled = true;

    protected Map resourceLoaderInfos = FastMap.newInstance();
    protected List classpathInfos = FastList.newInstance();
    protected List entityResourceInfos = FastList.newInstance();
    protected List serviceResourceInfos = FastList.newInstance();
    protected List testSuiteInfos = FastList.newInstance();
    protected List keystoreInfos = FastList.newInstance();
    protected List webappInfos = FastList.newInstance();

    protected ComponentConfig() {}

    protected ComponentConfig(String globalName, String rootLocation) throws ComponentException {
        this.globalName = globalName;
        if (!rootLocation.endsWith("/")) {
            rootLocation = rootLocation + "/";
        }
        this.rootLocation = rootLocation.replace('\\', '/');

        File rootLocationDir = new File(rootLocation);
        if (rootLocationDir == null) {
            throw new ComponentException("The given component root location is does not exist: " + rootLocation);
        }
        if (!rootLocationDir.isDirectory()) {
            throw new ComponentException("The given component root location is not a directory: " + rootLocation);
        }

        String xmlFilename = rootLocation + "/" + OFBIZ_COMPONENT_XML_FILENAME;
        URL xmlUrl = UtilURL.fromFilename(xmlFilename);
        if (xmlUrl == null) {
            throw new ComponentException("Could not find the " + OFBIZ_COMPONENT_XML_FILENAME + " configuration file in the component root location: " + rootLocation);
        }

        Document ofbizComponentDocument = null;
        try {
            ofbizComponentDocument = UtilXml.readXmlDocument(xmlUrl, true);
        } catch (SAXException e) {
            throw new ComponentException("Error reading the component config file: " + xmlUrl, e);
        } catch (ParserConfigurationException e) {
            throw new ComponentException("Error reading the component config file: " + xmlUrl, e);
        } catch (IOException e) {
            throw new ComponentException("Error reading the component config file: " + xmlUrl, e);
        }

        Element ofbizComponentElement = ofbizComponentDocument.getDocumentElement();
        this.componentName = ofbizComponentElement.getAttribute("name");
        this.enabled = "true".equalsIgnoreCase(ofbizComponentElement.getAttribute("enabled"));
        if (UtilValidate.isEmpty(this.globalName)) {
            this.globalName = this.componentName;
        }
        Iterator elementIter = null;

        // resource-loader - resourceLoaderInfos
        elementIter = UtilXml.childElementList(ofbizComponentElement, "resource-loader").iterator();
        while (elementIter.hasNext()) {
            Element curElement = (Element) elementIter.next();
            ResourceLoaderInfo resourceLoaderInfo = new ResourceLoaderInfo(curElement);
            this.resourceLoaderInfos.put(resourceLoaderInfo.name, resourceLoaderInfo);
        }

        // classpath - classpathInfos
        elementIter = UtilXml.childElementList(ofbizComponentElement, "classpath").iterator();
        while (elementIter.hasNext()) {
            Element curElement = (Element) elementIter.next();
            ClasspathInfo classpathInfo = new ClasspathInfo(this, curElement);
            this.classpathInfos.add(classpathInfo);
        }

        // entity-resource - entityResourceInfos
        elementIter = UtilXml.childElementList(ofbizComponentElement, "entity-resource").iterator();
        while (elementIter.hasNext()) {
            Element curElement = (Element) elementIter.next();
            EntityResourceInfo entityResourceInfo = new EntityResourceInfo(this, curElement);
            this.entityResourceInfos.add(entityResourceInfo);
        }

        // service-resource - serviceResourceInfos
        elementIter = UtilXml.childElementList(ofbizComponentElement, "service-resource").iterator();
        while (elementIter.hasNext()) {
            Element curElement = (Element) elementIter.next();
            ServiceResourceInfo serviceResourceInfo = new ServiceResourceInfo(this, curElement);
            this.serviceResourceInfos.add(serviceResourceInfo);
        }

        // test-suite - serviceResourceInfos
        elementIter = UtilXml.childElementList(ofbizComponentElement, "test-suite").iterator();
        while (elementIter.hasNext()) {
            Element curElement = (Element) elementIter.next();
            TestSuiteInfo testSuiteInfo = new TestSuiteInfo(this, curElement);
            this.testSuiteInfos.add(testSuiteInfo);
        }

        // keystore - (cert/trust store infos)
        elementIter = UtilXml.childElementList(ofbizComponentElement, "keystore").iterator();
        while (elementIter.hasNext()) {
            Element curElement = (Element) elementIter.next();
            KeystoreInfo keystoreInfo = new KeystoreInfo(this, curElement);
            this.keystoreInfos.add(keystoreInfo);
        }

        // webapp - webappInfos
        elementIter = UtilXml.childElementList(ofbizComponentElement, "webapp").iterator();
        while (elementIter.hasNext()) {
            Element curElement = (Element) elementIter.next();
            WebappInfo webappInfo = new WebappInfo(this, curElement);
            this.webappInfos.add(webappInfo);
        }

        if (Debug.verboseOn()) Debug.logVerbose("Read component config : [" + rootLocation + "]", module);
    }

    public boolean isFileResource(ResourceInfo resourceInfo) throws ComponentException {
        return isFileResourceLoader(resourceInfo.loader);
    }
    public boolean isFileResourceLoader(String resourceLoaderName) throws ComponentException {
        ResourceLoaderInfo resourceLoaderInfo = (ResourceLoaderInfo) resourceLoaderInfos.get(resourceLoaderName);
        if (resourceLoaderInfo == null) {
            throw new ComponentException("Could not find resource-loader named: " + resourceLoaderName);
        }
        return "file".equals(resourceLoaderInfo.type) || "component".equals(resourceLoaderInfo.type);
    }

    public InputStream getStream(String resourceLoaderName, String location) throws ComponentException {
        URL url = getURL(resourceLoaderName, location);
        try {
            return url.openStream();
        } catch (java.io.IOException e) {
            throw new ComponentException("Error opening resource at location [" + url.toExternalForm() + "]", e);
        }
    }

    public URL getURL(String resourceLoaderName, String location) throws ComponentException {
        ResourceLoaderInfo resourceLoaderInfo = (ResourceLoaderInfo) resourceLoaderInfos.get(resourceLoaderName);
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
                url = new URL(fullLocation);
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

    public String getFullLocation(String resourceLoaderName, String location) throws ComponentException {
        ResourceLoaderInfo resourceLoaderInfo = (ResourceLoaderInfo) resourceLoaderInfos.get(resourceLoaderName);
        if (resourceLoaderInfo == null) {
            throw new ComponentException("Could not find resource-loader named: " + resourceLoaderName);
        }

        StringBuffer buf = new StringBuffer();

        // pre-pend component root location if this is a type component resource-loader
        if ("component".equals(resourceLoaderInfo.type)) {
            buf.append(rootLocation);
        }

        if (resourceLoaderInfo.prependEnv != null && resourceLoaderInfo.prependEnv.length() > 0) {
            String propValue = System.getProperty(resourceLoaderInfo.prependEnv);
            if (propValue == null) {
                String errMsg = "The Java environment (-Dxxx=yyy) variable with name " + resourceLoaderInfo.prependEnv + " is not set, cannot load resource.";
                Debug.logError(errMsg, module);
                throw new IllegalArgumentException(errMsg);
            }
            buf.append(propValue);
        }
        if (resourceLoaderInfo.prefix != null && resourceLoaderInfo.prefix.length() > 0) {
            buf.append(resourceLoaderInfo.prefix);
        }
        buf.append(location);
        return buf.toString();
    }

    public List getClasspathInfos() {
        return this.classpathInfos;
    }

    public String getComponentName() {
        return this.componentName;
    }

    public List getEntityResourceInfos() {
        return this.entityResourceInfos;
    }

    public String getGlobalName() {
        return this.globalName;
    }

    public Map getResourceLoaderInfos() {
        return this.resourceLoaderInfos;
    }

    public String getRootLocation() {
        return this.rootLocation;
    }

    public List getServiceResourceInfos() {
        return this.serviceResourceInfos;
    }

    public List getTestSuiteInfos() {
        return this.testSuiteInfos;
    }

    public List getKeystoreInfos() {
        return this.keystoreInfos;
    }
    
    public List getWebappInfos() {
        return this.webappInfos;
    }

    public boolean enabled() {
        return this.enabled;
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

    public static class EntityResourceInfo extends ResourceInfo {
        public String type;
        public String readerName;

        public EntityResourceInfo(ComponentConfig componentConfig, Element element) {
            super(componentConfig, element);
            this.type = element.getAttribute("type");
            this.readerName = element.getAttribute("reader-name");
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
        
        public String getType() {
            return type;
        }

        public String getPassword() {
            return password;
        }

        public boolean isCertStore() {
            return isCertStore;
        }

        public boolean isTrustStore() {
            return isTrustStore;
        }
    }

    public static class WebappInfo {
        public ComponentConfig componentConfig;
        public List virtualHosts;
        public Map initParameters;
        public String name;
        public String title;
        public String server;
        public String mountPoint;
        public String location;
        public String[] basePermission;
        public boolean appBarDisplay;

        public WebappInfo(ComponentConfig componentConfig, Element element) {
        	this.virtualHosts = FastList.newInstance();
            this.initParameters = FastMap.newInstance();
            this.componentConfig = componentConfig;
            this.name = element.getAttribute("name");
            this.title = element.getAttribute("title");
            this.server = element.getAttribute("server");
            this.mountPoint = element.getAttribute("mount-point");
            this.location = element.getAttribute("location");
            this.appBarDisplay = !"false".equals(element.getAttribute("app-bar-display"));
            String basePermStr = element.getAttribute("base-permission");
            if (UtilValidate.isNotEmpty(basePermStr)) {
                this.basePermission = basePermStr.split(",");
            } else {
                // default base permission is NONE
                this.basePermission = new String[] { "NONE" };
            }

            // trim the permussions (remove spaces)
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
            List virtHostList = UtilXml.childElementList(element, "virtual-host");
            if (virtHostList != null && virtHostList.size() > 0) {
                Iterator elementIter = virtHostList.iterator();
                while (elementIter.hasNext()) {
                    Element e = (Element) elementIter.next();
                    virtualHosts.add(e.getAttribute("host-name"));
                }
            }

            // load the init parameters
            List initParamList = UtilXml.childElementList(element, "init-param");
            if (initParamList != null && initParamList.size() > 0) {
                Iterator elementIter = initParamList.iterator();
                while (elementIter.hasNext()) {
                    Element e = (Element) elementIter.next();
                    this.initParameters.put(e.getAttribute("name"), e.getAttribute("value"));
                }
            }
        }

        public String getContextRoot() {
            if (mountPoint.endsWith("/*")) {
                return mountPoint.substring(0, mountPoint.length() - 2);
            }
            return mountPoint;
        }

        public String[] getBasePermission() {
            return this.basePermission;
        }

        public String getName() {
            return name;
        }

        public String getLocation() {
            return componentConfig.getRootLocation() + location;
        }

        public String getTitle() {
            return title;
        }

        public List getVirtualHosts() {
        	return virtualHosts;
        }

        public Map getInitParameters() {
            return initParameters;
        }
    }
}
