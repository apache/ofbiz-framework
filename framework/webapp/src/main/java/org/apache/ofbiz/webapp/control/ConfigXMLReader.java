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
package org.apache.ofbiz.webapp.control;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import javax.servlet.ServletContext;

import org.apache.ofbiz.base.component.ComponentConfig.WebappInfo;
import org.apache.ofbiz.base.location.FlexibleLocation;
import org.apache.ofbiz.base.metrics.Metrics;
import org.apache.ofbiz.base.metrics.MetricsFactory;
import org.apache.ofbiz.base.util.Assert;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.FileUtil;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.StringUtil;
import org.apache.ofbiz.base.util.UtilHttp;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.UtilXml;
import org.apache.ofbiz.base.util.cache.UtilCache;
import org.apache.ofbiz.base.util.collections.MapContext;
import org.apache.ofbiz.base.util.collections.MultivaluedMapContext;
import org.apache.ofbiz.base.util.collections.MultivaluedMapContextAdapter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * ConfigXMLReader.java - Reads and parses the XML site config files.
 */
public class ConfigXMLReader {

    public static final String module = ConfigXMLReader.class.getName();
    public static final String controllerXmlFileName = "/WEB-INF/controller.xml";
    private static final UtilCache<URL, ControllerConfig> controllerCache = UtilCache.createUtilCache("webapp.ControllerConfig");
    private static final UtilCache<String, List<ControllerConfig>> controllerSearchResultsCache = UtilCache.createUtilCache("webapp.ControllerSearchResults");
    public static final RequestResponse emptyNoneRequestResponse = RequestResponse.createEmptyNoneRequestResponse();

    public static Set<String> findControllerFilesWithRequest(String requestUri, String controllerPartialPath) throws GeneralException {
        Set<String> allControllerRequestSet = new HashSet<>();
        if (UtilValidate.isEmpty(requestUri)) {
            return allControllerRequestSet;
        }
        String cacheId = controllerPartialPath != null ? controllerPartialPath : "NOPARTIALPATH";
        List<ControllerConfig> controllerConfigs = controllerSearchResultsCache.get(cacheId);
        if (controllerConfigs == null) {
            try {
                // find controller.xml file with webappMountPoint + "/WEB-INF" in the path
                List<File> controllerFiles = FileUtil.findXmlFiles(null, controllerPartialPath, "site-conf", "site-conf.xsd");
                controllerConfigs = new LinkedList<>();
                for (File controllerFile : controllerFiles) {
                    URL controllerUrl = null;
                    try {
                        controllerUrl = controllerFile.toURI().toURL();
                    } catch (MalformedURLException mue) {
                        throw new GeneralException(mue);
                    }
                    ControllerConfig cc = ConfigXMLReader.getControllerConfig(controllerUrl);
                    controllerConfigs.add(cc);
                }
                controllerConfigs = controllerSearchResultsCache.putIfAbsentAndGet(cacheId, controllerConfigs);
            } catch (IOException e) {
                throw new GeneralException("Error finding controller XML files to lookup request references: " + e.toString(), e);
            }
        }
        if (controllerConfigs != null) {
            for (ControllerConfig cc : controllerConfigs) {
                // make sure it has the named request in it
                if (cc.requestMapMap.get(requestUri) != null) {
                    String requestUniqueId = cc.url.toExternalForm() + "#" + requestUri;
                    allControllerRequestSet.add(requestUniqueId);
                    // Debug.logInfo("========== In findControllerFilesWithRequest found controller with request here [" + requestUniqueId + "]", module);
                }
            }
        }
        return allControllerRequestSet;
    }

    public static Set<String> findControllerRequestUniqueForTargetType(String target, String urlMode) throws GeneralException {
        if (UtilValidate.isEmpty(urlMode)) {
            urlMode = "intra-app";
        }
        int indexOfDollarSignCurlyBrace = target.indexOf("${");
        int indexOfQuestionMark = target.indexOf("?");
        if (indexOfDollarSignCurlyBrace >= 0 && (indexOfQuestionMark < 0 || indexOfQuestionMark > indexOfDollarSignCurlyBrace)) {
            // we have an expanded string in the requestUri part of the target, not much we can do about that...
            return null;
        }
        if ("intra-app".equals(urlMode)) {
            // look through all controller.xml files and find those with the request-uri referred to by the target
            String requestUri = UtilHttp.getRequestUriFromTarget(target);
            Set<String> controllerLocAndRequestSet = ConfigXMLReader.findControllerFilesWithRequest(requestUri, null);
            // if (controllerLocAndRequestSet.size() > 0) Debug.logInfo("============== In findRequestNamesLinkedtoInWidget, controllerLocAndRequestSet: " + controllerLocAndRequestSet, module);
            return controllerLocAndRequestSet;
        } else if ("inter-app".equals(urlMode)) {
            String webappMountPoint = UtilHttp.getWebappMountPointFromTarget(target);
            if (webappMountPoint != null)
                webappMountPoint += "/WEB-INF";
            String requestUri = UtilHttp.getRequestUriFromTarget(target);

            Set<String> controllerLocAndRequestSet = ConfigXMLReader.findControllerFilesWithRequest(requestUri, webappMountPoint);
            // if (controllerLocAndRequestSet.size() > 0) Debug.logInfo("============== In findRequestNamesLinkedtoInWidget, controllerLocAndRequestSet: " + controllerLocAndRequestSet, module);
            return controllerLocAndRequestSet;
        } else {
            return new HashSet<>();
        }
    }

    public static ControllerConfig getControllerConfig(WebappInfo webAppInfo) throws WebAppConfigurationException, MalformedURLException {
        Assert.notNull("webAppInfo", webAppInfo);
        String filePath = webAppInfo.getLocation().concat(controllerXmlFileName);
        File configFile = new File(filePath);
        return getControllerConfig(configFile.toURI().toURL());
    }

    public static ControllerConfig getControllerConfig(URL url) throws WebAppConfigurationException {
        ControllerConfig controllerConfig = controllerCache.get(url);
        if (controllerConfig == null) {
            controllerConfig = controllerCache.putIfAbsentAndGet(url, new ControllerConfig(url));
        }
        return controllerConfig;
    }

    public static URL getControllerConfigURL(ServletContext context) {
        try {
            return context.getResource(controllerXmlFileName);
        } catch (MalformedURLException e) {
            Debug.logError(e, "Error Finding XML Config File: " + controllerXmlFileName, module);
            return null;
        }
    }

    /** Loads the XML file and returns the root element 
     * @throws WebAppConfigurationException */
    private static Element loadDocument(URL location) throws WebAppConfigurationException {
        try {
            Document document = UtilXml.readXmlDocument(location, true);
            Element rootElement = document.getDocumentElement();
            if (!"site-conf".equalsIgnoreCase(rootElement.getTagName())) {
                rootElement = UtilXml.firstChildElement(rootElement, "site-conf");
            }
            if (Debug.verboseOn()) {
                 Debug.logVerbose("Loaded XML Config - " + location, module);
            }
            return rootElement;
        } catch (Exception e) {
            Debug.logError("When read " + (location != null? location.toString(): "empty location (!)") + " threw " + e.toString(), module);
            throw new WebAppConfigurationException(e);
        }
    }

    public static class ControllerConfig {
        public URL url;
        private String errorpage;
        private String protectView;
        private String owner;
        private String securityClass;
        private String defaultRequest;
        private String statusCode;
        private List<URL> includes = new ArrayList<>();
        private final Map<String, Event> firstVisitEventList = new LinkedHashMap<>();
        private final Map<String, Event> preprocessorEventList = new LinkedHashMap<>();
        private final Map<String, Event> postprocessorEventList = new LinkedHashMap<>();
        private final Map<String, Event> afterLoginEventList = new LinkedHashMap<>();
        private final Map<String, Event> beforeLogoutEventList = new LinkedHashMap<>();
        private final Map<String, String> eventHandlerMap = new HashMap<>();
        private final Map<String, String> viewHandlerMap = new HashMap<>();
        private MultivaluedMapContext<String, RequestMap> requestMapMap = new MultivaluedMapContext<>();
        private Map<String, ViewMap> viewMapMap = new HashMap<>();

        public ControllerConfig(URL url) throws WebAppConfigurationException {
            this.url = url;
            Element rootElement = loadDocument(url);
            if (rootElement != null) {
                long startTime = System.currentTimeMillis();
                loadIncludes(rootElement);
                loadGeneralConfig(rootElement);
                loadHandlerMap(rootElement);
                loadRequestMap(rootElement);
                loadViewMap(rootElement);
                if (Debug.infoOn()) {
                    double totalSeconds = (System.currentTimeMillis() - startTime) / 1000.0;
                    String locString = this.url.toExternalForm();
                    Debug.logInfo("controller loaded: " + totalSeconds + "s, " + this.requestMapMap.size() + " requests, " + this.viewMapMap.size() + " views in " + locString, module);
                }
            }
        }

        private <K, V> Map<K, V> pushIncludes(Function<ControllerConfig, Map<K, V>> f) throws WebAppConfigurationException {
            MapContext<K, V> res = new MapContext<>();
            for (URL include : includes) {
                res.push(getControllerConfig(include).pushIncludes(f));
            }
            res.push(f.apply(this));
            return res;
        }

        private String getIncludes(Function<ControllerConfig, String> f) throws WebAppConfigurationException {
            String val = f.apply(this);
            if (val != null) {
                return val;
            }
            for (URL include : includes) {
                String inc = getControllerConfig(include).getIncludes(f);
                if (inc != null) {
                    return inc;
                }
            }
            return null;
        }

        public Map<String, Event> getAfterLoginEventList() throws WebAppConfigurationException {
            return pushIncludes(ccfg -> ccfg.afterLoginEventList);
        }

        public Map<String, Event> getBeforeLogoutEventList() throws WebAppConfigurationException {
            return pushIncludes(ccfg -> ccfg.beforeLogoutEventList);
        }

        public String getDefaultRequest() throws WebAppConfigurationException {
            return getIncludes(ccfg -> ccfg.defaultRequest);
        }

        public String getErrorpage() throws WebAppConfigurationException {
            return getIncludes(ccfg -> ccfg.errorpage);
        }

        public Map<String, String> getEventHandlerMap() throws WebAppConfigurationException {
            return pushIncludes(ccfg -> ccfg.eventHandlerMap);
        }

        public Map<String, Event> getFirstVisitEventList() throws WebAppConfigurationException {
            return pushIncludes(ccfg -> ccfg.firstVisitEventList);
        }

        public String getOwner() throws WebAppConfigurationException {
            return getIncludes(ccfg -> ccfg.owner);
        }

        public Map<String, Event> getPostprocessorEventList() throws WebAppConfigurationException {
            return pushIncludes(ccfg -> ccfg.postprocessorEventList);
        }

        public Map<String, Event> getPreprocessorEventList() throws WebAppConfigurationException {
            return pushIncludes(ccfg -> ccfg.preprocessorEventList);
        }

        public String getProtectView() throws WebAppConfigurationException {
            return getIncludes(ccfg -> ccfg.protectView);
        }

        // XXX: Keep it for backward compatibility until moving everything to 鈥榞etRequestMapMultiMap鈥�.
        public Map<String, RequestMap> getRequestMapMap() throws WebAppConfigurationException {
            return new MultivaluedMapContextAdapter<>(getRequestMapMultiMap());
        }

        public MultivaluedMapContext<String, RequestMap> getRequestMapMultiMap() throws WebAppConfigurationException {
            MultivaluedMapContext<String, RequestMap> result = new MultivaluedMapContext<>();
            for (URL includeLocation : includes) {
                ControllerConfig controllerConfig = getControllerConfig(includeLocation);
                result.push(controllerConfig.getRequestMapMultiMap());
            }
            result.push(requestMapMap);
            return result;
        }

        public String getSecurityClass() throws WebAppConfigurationException {
            return getIncludes(ccfg -> ccfg.securityClass);
        }

        public String getStatusCode() throws WebAppConfigurationException {
            return getIncludes(ccfg -> ccfg.statusCode);
        }

        public Map<String, String> getViewHandlerMap() throws WebAppConfigurationException {
            return pushIncludes(ccfg -> ccfg.viewHandlerMap);
        }

        public Map<String, ViewMap> getViewMapMap() throws WebAppConfigurationException {
            return pushIncludes(ccfg -> ccfg.viewMapMap);
        }

        /**
         * Computes the name of an XML element.
         *
         * @param el  the element containing "type" and/or "name" attributes
         * @return the derived name.
         * @throws NullPointerException when {@code el} is {@code null}
         */
        private static String elementToName(Element el) {
            String eventName = el.getAttribute("name");
            return eventName.isEmpty()
                    ? el.getAttribute("type") + "::" + el.getAttribute("path") + "::" + el.getAttribute("invoke")
                    : eventName;
        }

        /**
         * Collects some events defined in an XML tree.
         *
         * @param root  the root of the XML tree
         * @param childName  the name of the element inside {@code root} containing the events
         * @return a map associating element derived names to an event objects.
         */
        private static void collectEvents(Element root, String childName, Map<String, Event> coll) {
            Element child = UtilXml.firstChildElement(root, childName);
            if (child != null) {
                UtilXml.childElementList(child, "event").stream()
                       .forEachOrdered(ev -> coll.put(elementToName(ev), new Event(ev)));
            }
        }

        private void loadGeneralConfig(Element rootElement) {
            this.errorpage = UtilXml.childElementValue(rootElement, "errorpage");
            this.statusCode = UtilXml.childElementValue(rootElement, "status-code");
            Element protectElement = UtilXml.firstChildElement(rootElement, "protect");
            if (protectElement != null) {
                this.protectView = protectElement.getAttribute("view");
            }
            this.owner = UtilXml.childElementValue(rootElement, "owner");
            this.securityClass = UtilXml.childElementValue(rootElement, "security-class");
            Element defaultRequestElement = UtilXml.firstChildElement(rootElement, "default-request");
            if (defaultRequestElement != null) {
                this.defaultRequest = defaultRequestElement.getAttribute("request-uri");
            }
            collectEvents(rootElement, "firstvisit", firstVisitEventList);
            collectEvents(rootElement, "preprocessor", preprocessorEventList);
            collectEvents(rootElement, "postprocessor", postprocessorEventList);
            collectEvents(rootElement, "after-login", afterLoginEventList);
            collectEvents(rootElement, "before-logout", beforeLogoutEventList);
        }

        private void loadHandlerMap(Element rootElement) {
            Map<Boolean, Map<String, String>> handlers = UtilXml.childElementList(rootElement, "handler").stream()
                    .collect(Collectors.partitioningBy(el -> "view".equals(el.getAttribute("type")),
                            Collectors.toMap(el -> el.getAttribute("name"), el -> el.getAttribute("className"))));
            viewHandlerMap.putAll(handlers.get(true));
            eventHandlerMap.putAll(handlers.get(false));
        }

        protected void loadIncludes(Element rootElement) {
            for (Element includeElement : UtilXml.childElementList(rootElement, "include")) {
                String includeLocation = includeElement.getAttribute("location");
                if (!includeLocation.isEmpty()) {
                    try {
                        URL urlLocation = FlexibleLocation.resolveLocation(includeLocation);
                        includes.add(urlLocation);
                    } catch (MalformedURLException mue) {
                        Debug.logError(mue, "Error processing include at [" + includeLocation + "]:" + mue.toString(), module);
                    }
                }
            }
        }

        private void loadRequestMap(Element root) {
            for (Element requestMapElement : UtilXml.childElementList(root, "request-map")) {
                RequestMap requestMap = new RequestMap(requestMapElement);
                this.requestMapMap.add(requestMap.uri, requestMap);
            }
        }

        private void loadViewMap(Element rootElement) {
            for (Element viewMapElement : UtilXml.childElementList(rootElement, "view-map")) {
                ViewMap viewMap = new ViewMap(viewMapElement);
                this.viewMapMap.put(viewMap.name, viewMap);
            }
        }

    }

    public static class Event {
        public String type;
        public String path;
        public String invoke;
        public boolean globalTransaction = true;
        public int transactionTimeout;
        public Metrics metrics = null;

        public Event(Element eventElement) {
            this.type = eventElement.getAttribute("type");
            this.path = eventElement.getAttribute("path");
            this.invoke = eventElement.getAttribute("invoke");
            this.globalTransaction = !"false".equals(eventElement.getAttribute("global-transaction"));
            String tt = eventElement.getAttribute("transaction-timeout");
            if(!tt.isEmpty()) {
                this.transactionTimeout = Integer.valueOf(tt);
            }
            // Get metrics.
            Element metricsElement = UtilXml.firstChildElement(eventElement, "metric");
            if (metricsElement != null) {
                this.metrics = MetricsFactory.getInstance(metricsElement);
            }
        }

        public Event(String type, String path, String invoke, boolean globalTransaction) {
            this.type = type;
            this.path = path;
            this.invoke = invoke;
            this.globalTransaction = globalTransaction;
        }
    }

    public static class RequestMap {
        public String uri;
        public String method;
        public boolean edit = true;
        public boolean trackVisit = true;
        public boolean trackServerHit = true;
        public String description;
        public Event event;
        public boolean securityHttps = true;
        public boolean securityAuth = false;
        public boolean securityCert = false;
        public boolean securityExternalView = true;
        public boolean securityDirectRequest = true;
        public Map<String, RequestResponse> requestResponseMap = new HashMap<>();
        public Metrics metrics = null;

        public RequestMap(Element requestMapElement) {
            // Get the URI info
            this.uri = requestMapElement.getAttribute("uri");
            this.method = requestMapElement.getAttribute("method");
            this.edit = !"false".equals(requestMapElement.getAttribute("edit"));
            this.trackServerHit = !"false".equals(requestMapElement.getAttribute("track-serverhit"));
            this.trackVisit = !"false".equals(requestMapElement.getAttribute("track-visit"));
            // Check for security
            Element securityElement = UtilXml.firstChildElement(requestMapElement, "security");
            if (securityElement != null) {
                if (!UtilProperties.propertyValueEqualsIgnoreCase("url", "no.http", "Y")) {
                    this.securityHttps = "true".equals(securityElement.getAttribute("https"));
                } else {
                    String httpRequestMapList = UtilProperties.getPropertyValue("url", "http.request-map.list");
                    if (UtilValidate.isNotEmpty(httpRequestMapList)) {
                        List<String> reqList = StringUtil.split(httpRequestMapList, ",");
                        if (reqList.contains(this.uri)) {
                            this.securityHttps = "true".equals(securityElement.getAttribute("https"));
                        }
                    }
                }
                this.securityAuth = "true".equals(securityElement.getAttribute("auth"));
                this.securityCert = "true".equals(securityElement.getAttribute("cert"));
                this.securityExternalView = !"false".equals(securityElement.getAttribute("external-view"));
                this.securityDirectRequest = !"false".equals(securityElement.getAttribute("direct-request"));
            }
            // Check for event
            Element eventElement = UtilXml.firstChildElement(requestMapElement, "event");
            if (eventElement != null) {
                this.event = new Event(eventElement);
            }
            // Check for description
            this.description = UtilXml.childElementValue(requestMapElement, "description");
            // Get the response(s)
            for (Element responseElement : UtilXml.childElementList(requestMapElement, "response")) {
                RequestResponse response = new RequestResponse(responseElement);
                requestResponseMap.put(response.name, response);
            }
            // Get metrics.
            Element metricsElement = UtilXml.firstChildElement(requestMapElement, "metric");
            if (metricsElement != null) {
                this.metrics = MetricsFactory.getInstance(metricsElement);
            }
        }
    }

    public static class RequestResponse {

        public static RequestResponse createEmptyNoneRequestResponse() {
            RequestResponse requestResponse = new RequestResponse();
            requestResponse.name = "empty-none";
            requestResponse.type = "none";
            requestResponse.value = null;
            return requestResponse;
        }

        public String name;
        public String type;
        public String value;
        public String statusCode;
        public boolean saveLastView = false;
        public boolean saveCurrentView = false;
        public boolean saveHomeView = false;
        public Map<String, String> redirectParameterMap = new HashMap<>();
        public Map<String, String> redirectParameterValueMap = new HashMap<>();

        public RequestResponse() {
        }

        public RequestResponse(Element responseElement) {
            this.name = responseElement.getAttribute("name");
            this.type = responseElement.getAttribute("type");
            this.value = responseElement.getAttribute("value");
            this.statusCode = responseElement.getAttribute("status-code");
            this.saveLastView = "true".equals(responseElement.getAttribute("save-last-view"));
            this.saveCurrentView = "true".equals(responseElement.getAttribute("save-current-view"));
            this.saveHomeView = "true".equals(responseElement.getAttribute("save-home-view"));
            for (Element redirectParameterElement : UtilXml.childElementList(responseElement, "redirect-parameter")) {
                if (UtilValidate.isNotEmpty(redirectParameterElement.getAttribute("value"))) {
                    this.redirectParameterValueMap.put(redirectParameterElement.getAttribute("name"), redirectParameterElement.getAttribute("value"));
                } else {
                    String from = redirectParameterElement.getAttribute("from");
                    if (from.isEmpty())
                        from = redirectParameterElement.getAttribute("name");
                    this.redirectParameterMap.put(redirectParameterElement.getAttribute("name"), from);
                }
            }
        }
    }

    public static class ViewMap {
        public String viewMap;
        public String name;
        public String page;
        public String type;
        public String info;
        public String contentType;
        public String encoding;
        public String xFrameOption;
        public String strictTransportSecurity;
        public String description;
        public boolean noCache = false;

        public ViewMap(Element viewMapElement) {
            this.name = viewMapElement.getAttribute("name");
            this.page = viewMapElement.getAttribute("page");
            this.type = viewMapElement.getAttribute("type");
            this.info = viewMapElement.getAttribute("info");
            this.contentType = viewMapElement.getAttribute("content-type");
            this.noCache = "true".equals(viewMapElement.getAttribute("no-cache"));
            this.encoding = viewMapElement.getAttribute("encoding");
            this.xFrameOption = viewMapElement.getAttribute("x-frame-options");
            this.strictTransportSecurity = viewMapElement.getAttribute("strict-transport-security");
            this.description = UtilXml.childElementValue(viewMapElement, "description");
            if (UtilValidate.isEmpty(this.page)) {
                this.page = this.name;
            }
        }
    }
}
