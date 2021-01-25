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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

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
import org.apache.ofbiz.security.CsrfUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * ConfigXMLReader.java - Reads and parses the XML site config files.
 */
public final class ConfigXMLReader {

    private static final String MODULE = ConfigXMLReader.class.getName();
    private static final Path CONTROLLERXMLFILENAME = Paths.get("WEB-INF", "controller.xml");
    private static final UtilCache<URL, ControllerConfig> CONTROLLERCACHE = UtilCache
            .createUtilCache("webapp.ControllerConfig");
    private static final UtilCache<String, List<ControllerConfig>> CONTROLLERSEARCHRESULTSCACHE = UtilCache
            .createUtilCache("webapp.ControllerSearchResults");
    private static final RequestResponse EMPTY_NONE_REQ_RES = RequestResponse.createEmptyNoneRequestResponse();

    /**
     * Instantiates a new Config xml reader.
     */
    protected ConfigXMLReader() { }

    /**
     * Gets empty none request response.
     *
     * @return the empty none request response
     */
    public static RequestResponse getEmptyNoneRequestResponse() {
        return EMPTY_NONE_REQ_RES;
    }

    /**
     * Find controller files with request set.
     *
     * @param requestUri            the request uri
     * @param controllerPartialPath the controller partial path
     * @return the set
     * @throws GeneralException the general exception
     */
    public static Set<String> findControllerFilesWithRequest(String requestUri, String controllerPartialPath)
            throws GeneralException {
        Set<String> allControllerRequestSet = new HashSet<>();
        if (UtilValidate.isEmpty(requestUri)) {
            return allControllerRequestSet;
        }
        String cacheId = controllerPartialPath != null ? controllerPartialPath : "NOPARTIALPATH";
        List<ControllerConfig> controllerConfigs = CONTROLLERSEARCHRESULTSCACHE.get(cacheId);
        if (controllerConfigs == null) {
            try {
                // find controller.xml file with webappMountPoint + "/WEB-INF" in the path
                List<File> controllerFiles = FileUtil.findXmlFiles(null, controllerPartialPath, "site-conf",
                        "site-conf.xsd");
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
                controllerConfigs = CONTROLLERSEARCHRESULTSCACHE.putIfAbsentAndGet(cacheId, controllerConfigs);
            } catch (IOException e) {
                throw new GeneralException(
                        "Error finding controller XML files to lookup request references: " + e.toString(), e);
            }
        }
        if (controllerConfigs != null) {
            for (ControllerConfig cc : controllerConfigs) {
                // make sure it has the named request in it
                if (cc.requestMapMap.get(requestUri) != null) {
                    String requestUniqueId = cc.url.toExternalForm() + "#" + requestUri;
                    allControllerRequestSet.add(requestUniqueId);
                    // Debug.logInfo("========== In findControllerFilesWithRequest found controller with request here ["
                    // + requestUniqueId + "]", MODULE);
                }
            }
        }
        return allControllerRequestSet;
    }

    /**
     * Find controller request unique for target type set.
     *
     * @param target  the target
     * @param urlMode the url mode
     * @return the set
     * @throws GeneralException the general exception
     */
    public static Set<String> findControllerRequestUniqueForTargetType(String target, String urlMode)
            throws GeneralException {
        if (UtilValidate.isEmpty(urlMode)) {
            urlMode = "intra-app";
        }
        int indexOfDollarSignCurlyBrace = target.indexOf("${");
        int indexOfQuestionMark = target.indexOf("?");
        if (indexOfDollarSignCurlyBrace >= 0
                && (indexOfQuestionMark < 0 || indexOfQuestionMark > indexOfDollarSignCurlyBrace)) {
            // we have an expanded string in the requestUri part of the target, not much we can do about that...
            return null;
        }
        if ("intra-app".equals(urlMode)) {
            // look through all controller.xml files and find those with the request-uri referred to by the target
            String requestUri = UtilHttp.getRequestUriFromTarget(target);
            Set<String> controllerLocAndRequestSet = ConfigXMLReader.findControllerFilesWithRequest(requestUri, null);
            // if (controllerLocAndRequestSet.size() > 0) Debug.logInfo("============== In
            // findRequestNamesLinkedtoInWidget, controllerLocAndRequestSet: " + controllerLocAndRequestSet, MODULE);
            return controllerLocAndRequestSet;
        } else if ("inter-app".equals(urlMode)) {
            String webappMountPoint = UtilHttp.getWebappMountPointFromTarget(target);
            if (webappMountPoint != null) webappMountPoint += "/WEB-INF";
            String requestUri = UtilHttp.getRequestUriFromTarget(target);

            Set<String> controllerLocAndRequestSet = ConfigXMLReader.findControllerFilesWithRequest(requestUri,
                    webappMountPoint);
            // if (controllerLocAndRequestSet.size() > 0) Debug.logInfo("============== In
            // findRequestNamesLinkedtoInWidget, controllerLocAndRequestSet: " + controllerLocAndRequestSet, MODULE);
            return controllerLocAndRequestSet;
        } else {
            return new HashSet<>();
        }
    }

    /**
     * Gets controller config.
     *
     * @param webAppInfo the web app info
     * @return the controller config
     * @throws WebAppConfigurationException the web app configuration exception
     * @throws MalformedURLException        the malformed url exception
     */
    public static ControllerConfig getControllerConfig(WebappInfo webAppInfo)
            throws WebAppConfigurationException, MalformedURLException {
        Assert.notNull("webAppInfo", webAppInfo);
        Path filePath = webAppInfo.location().resolve(CONTROLLERXMLFILENAME);
        return getControllerConfig(filePath.toUri().toURL());
    }

    /**
     * Gets controller config.
     *
     * @param url the url
     * @return the controller config
     * @throws WebAppConfigurationException the web app configuration exception
     */
    public static ControllerConfig getControllerConfig(URL url) throws WebAppConfigurationException {
        ControllerConfig controllerConfig = CONTROLLERCACHE.get(url);
        if (controllerConfig == null) {
            controllerConfig = CONTROLLERCACHE.putIfAbsentAndGet(url, new ControllerConfig(url));
        }
        return controllerConfig;
    }

    /**
     * Gets controller config url.
     * @param context the context
     * @return the controller config url
     */
    public static URL getControllerConfigURL(ServletContext context) {
        try {
            return context.getResource("/" + CONTROLLERXMLFILENAME);
        } catch (MalformedURLException e) {
            Debug.logError(e, "Error Finding XML Config File: " + CONTROLLERXMLFILENAME, MODULE);
            return null;
        }
    }

    /**
     * Loads the XML file and returns the root element
     * @throws WebAppConfigurationException
     */
    private static Element loadDocument(URL location) throws WebAppConfigurationException {
        try {
            Document document = UtilXml.readXmlDocument(location, true);
            Element rootElement = document.getDocumentElement();
            if (!"site-conf".equalsIgnoreCase(rootElement.getTagName())) {
                rootElement = UtilXml.firstChildElement(rootElement, "site-conf");
            }
            if (Debug.verboseOn()) {
                Debug.logVerbose("Loaded XML Config - " + location, MODULE);
            }
            return rootElement;
        } catch (Exception e) {
            Debug.logError("When read " + (location != null ? location.toString() : "empty location (!)") + " threw "
                    + e.toString(), MODULE);
            throw new WebAppConfigurationException(e);
        }
    }

    /**
     * The type Controller config.
     */
    public static class ControllerConfig {
        private static final String DEFAULT_REDIRECT_STATUS_CODE = UtilProperties.getPropertyValue("requestHandler",
                "status-code", "302");

        private URL url;
        private String errorpage;
        private String protectView;
        private String owner;
        private String securityClass;
        private String defaultRequest;
        private String statusCode;
        private List<ControllerConfig> includes = new ArrayList<>();
        private final Map<String, Event> firstVisitEventList = new LinkedHashMap<>();
        private final Map<String, Event> preprocessorEventList = new LinkedHashMap<>();
        private final Map<String, Event> postprocessorEventList = new LinkedHashMap<>();
        private final Map<String, Event> afterLoginEventList = new LinkedHashMap<>();
        private final Map<String, Event> beforeLogoutEventList = new LinkedHashMap<>();
        private final Map<String, String> eventHandlerMap = new HashMap<>();
        private final Map<String, String> viewHandlerMap = new HashMap<>();
        private MultivaluedMapContext<String, RequestMap> requestMapMap = new MultivaluedMapContext<>();
        private Map<String, ViewMap> viewMapMap = new HashMap<>();

        /**
         * Instantiates a new Controller config.
         *
         * @param url the url
         * @throws WebAppConfigurationException the web app configuration exception
         */
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
                    Debug.logInfo("controller loaded: " + totalSeconds + "s, " + this.requestMapMap.size()
                            + " requests, " + this.viewMapMap.size() + " views in " + locString, MODULE);
                }
            }
        }

        private <K, V> Map<K, V> pushIncludes(Function<ControllerConfig, Map<K, V>> f) {
            MapContext<K, V> res = new MapContext<>();
            for (ControllerConfig include : includes) {
                res.push(include.pushIncludes(f));
            }
            res.push(f.apply(this));
            return res;
        }

        private String getIncludes(Function<ControllerConfig, String> f) {
            String val = f.apply(this);
            if (val != null) {
                return val;
            }
            for (ControllerConfig include : includes) {
                String inc = include.getIncludes(f);
                if (inc != null) {
                    return inc;
                }
            }
            return null;
        }

        /**
         * Gets after login event list.
         *
         * @return the after login event list
         */
        public Map<String, Event> getAfterLoginEventList() {
            return pushIncludes(ccfg -> ccfg.afterLoginEventList);
        }

        /**
         * Gets before logout event list.
         *
         * @return the before logout event list
         */
        public Map<String, Event> getBeforeLogoutEventList() {
            return pushIncludes(ccfg -> ccfg.beforeLogoutEventList);
        }

        /**
         * Gets default request.
         *
         * @return the default request
         */
        public String getDefaultRequest() {
            return getIncludes(ccfg -> ccfg.defaultRequest);
        }

        /**
         * Gets errorpage.
         *
         * @return the errorpage
         */
        public String getErrorpage() {
            return getIncludes(ccfg -> ccfg.errorpage);
        }

        /**
         * Gets event handler map.
         *
         * @return the event handler map
         */
        public Map<String, String> getEventHandlerMap() {
            return pushIncludes(ccfg -> ccfg.eventHandlerMap);
        }

        /**
         * Gets first visit event list.
         * @return the first visit event list
         */
        public Map<String, Event> getFirstVisitEventList() {
            return pushIncludes(ccfg -> ccfg.firstVisitEventList);
        }

        /**
         * Gets owner.
         * @return the owner
         */
        public String getOwner() {
            return getIncludes(ccfg -> ccfg.owner);
        }

        /**
         * Gets postprocessor event list.
         * @return the postprocessor event list
         */
        public Map<String, Event> getPostprocessorEventList() {
            return pushIncludes(ccfg -> ccfg.postprocessorEventList);
        }

        /**
         * Gets preprocessor event list.
         * @return the preprocessor event list
         */
        public Map<String, Event> getPreprocessorEventList() {
            return pushIncludes(ccfg -> ccfg.preprocessorEventList);
        }

        /**
         * Gets protect view.
         * @return the protect view
         */
        public String getProtectView() {
            return getIncludes(ccfg -> ccfg.protectView);
        }

        /**
         * XXX: Keep it for backward compatibility until moving everything to 鈥榞etRequestMapMultiMap鈥�.  @return the request map map
         */
        public Map<String, RequestMap> getRequestMapMap() {
            return new MultivaluedMapContextAdapter<>(getRequestMapMultiMap());
        }

        /**
         * Gets request map multi map.
         *
         * @return the request map multi map
         */
        public MultivaluedMapContext<String, RequestMap> getRequestMapMultiMap() {
            MultivaluedMapContext<String, RequestMap> result = new MultivaluedMapContext<>();
            for (ControllerConfig include : includes) {
                result.push(include.getRequestMapMultiMap());
            }
            result.push(requestMapMap);
            return result;
        }

        /**
         * Gets security class.
         *
         * @return the security class
         */
        public String getSecurityClass() {
            return getIncludes(ccfg -> ccfg.securityClass);
        }

        /**
         * Provides the status code that should be used when redirecting an HTTP client.
         *
         * @return an HTTP response status code.
         */
        public String getStatusCode() {
            String status = getIncludes(ccfg -> ccfg.statusCode);
            return UtilValidate.isEmpty(status) ? DEFAULT_REDIRECT_STATUS_CODE : status;
        }

        /**
         * Gets view handler map.
         *
         * @return the view handler map
         */
        public Map<String, String> getViewHandlerMap() {
            return pushIncludes(ccfg -> ccfg.viewHandlerMap);
        }

        /**
         * Gets view map map.
         *
         * @return the view map map
         */
        public Map<String, ViewMap> getViewMapMap() {
            return pushIncludes(ccfg -> ccfg.viewMapMap);
        }

        /**
         * Computes the name of an XML element.
         * @param el
         *            the element containing "type" and/or "name" attributes
         * @return the derived name.
         * @throws NullPointerException
         *             when {@code el} is {@code null}
         */
        private static String elementToName(Element el) {
            String eventName = el.getAttribute("name");
            return eventName.isEmpty()
                    ? el.getAttribute("type") + "::" + el.getAttribute("path") + "::" + el.getAttribute("invoke")
                    : eventName;
        }

        /**
         * Collects some events defined in an XML tree.
         * @param root
         *            the root of the XML tree
         * @param childName
         *            the name of the element inside {@code root} containing the events
         * @param coll
         *            the map associating element derived names to an event objects to populate.
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
                            Collectors.toMap(el -> el.getAttribute("name"), el -> el.getAttribute("class"))));
            viewHandlerMap.putAll(handlers.get(true));
            eventHandlerMap.putAll(handlers.get(false));
        }

        /**
         * Load includes.
         * @param rootElement the root element
         * @throws WebAppConfigurationException the web app configuration exception
         */
        protected void loadIncludes(Element rootElement) throws WebAppConfigurationException {
            for (Element includeElement : UtilXml.childElementList(rootElement, "include")) {
                String includeLocation = includeElement.getAttribute("location");
                if (!includeLocation.isEmpty()) {
                    try {
                        URL urlLocation = FlexibleLocation.resolveLocation(includeLocation);
                        ControllerConfig includedController = getControllerConfig(urlLocation);
                        includes.add(includedController);
                    } catch (MalformedURLException mue) {
                        Debug.logError(mue, "Error processing include at [" + includeLocation + "]:" + mue.toString(),
                                MODULE);
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
        private String type;
        private String path;
        private String invoke;
        private boolean globalTransaction = true;
        private int transactionTimeout;
        private Metrics metrics = null;

        /**
         * Gets metrics.
         * @return the metrics
         */
        public Metrics getMetrics() {
            return metrics;
        }
        /**
         * Is global transaction boolean.
         * @return the boolean
         */
        public boolean isGlobalTransaction() {
            return globalTransaction;
        }
        /**
         * Gets transaction timeout.
         * @return the transaction timeout
         */
        public int getTransactionTimeout() {
            return transactionTimeout;
        }

        /**
         * Gets type.
         * @return the type
         */
        public String getType() {
            return type;
        }

        /**
         * Gets path.
         * @return the path
         */
        public String getPath() {
            return path;
        }

        /**
         * Gets invoke.
         * @return the invoke
         */
        public String getInvoke() {
            return invoke;
        }

        /**
         * Instantiates a new Event.
         * @param eventElement the event element
         */
        public Event(Element eventElement) {
            this.type = eventElement.getAttribute("type");
            this.path = eventElement.getAttribute("path");
            this.invoke = eventElement.getAttribute("invoke");
            this.globalTransaction = !"false".equals(eventElement.getAttribute("global-transaction"));
            String tt = eventElement.getAttribute("transaction-timeout");
            if (!tt.isEmpty()) {
                this.transactionTimeout = Integer.valueOf(tt);
            }
            // Get metrics.
            Element metricsElement = UtilXml.firstChildElement(eventElement, "metric");
            if (metricsElement != null) {
                this.metrics = MetricsFactory.getInstance(metricsElement);
            }
        }

        /**
         * Instantiates a new Event.
         * @param type              the type
         * @param path              the path
         * @param invoke            the invoke
         * @param globalTransaction the global transaction
         */
        public Event(String type, String path, String invoke, boolean globalTransaction) {
            this.type = type;
            this.path = path;
            this.invoke = invoke;
            this.globalTransaction = globalTransaction;
        }
    }

    public static class RequestMap {
        private String uri;
        private String method;
        private boolean edit = true;
        private boolean trackVisit = true;
        private boolean trackServerHit = true;
        private String description;
        private Event event;
        private boolean securityHttps = true;
        private boolean securityAuth = false;
        private boolean securityCsrfToken = true;
        private boolean securityCert = false;
        private boolean securityExternalView = true;
        private boolean securityDirectRequest = true;
        private Map<String, RequestResponse> requestResponseMap = new HashMap<>();
        private Metrics metrics = null;

        /**
         * Sets method.
         * @param method the method
         */
        public void setMethod(String method) {
            this.method = method;
        }

        /**
         * Is track visit boolean.
         * @return the boolean
         */
        public boolean isTrackVisit() {
            return trackVisit;
        }

        /**
         * Is track server hit boolean.
         * @return the boolean
         */
        public boolean isTrackServerHit() {
            return trackServerHit;
        }

        /**
         * Gets method.
         * @return the method
         */
        public String getMethod() {
            return method;
        }

        /**
         * Is security auth boolean.
         * @return the boolean
         */
        public boolean isSecurityAuth() {
            return securityAuth;
        }

        /**
         * Is security csrf token boolean.
         * @return the boolean
         */
        public boolean isSecurityCsrfToken() {
            return securityCsrfToken;
        }

        /**
         * Is security cert boolean.
         * @return the boolean
         */
        public boolean isSecurityCert() {
            return securityCert;
        }

        /**
         * Is security direct request boolean.
         * @return the boolean
         */
        public boolean isSecurityDirectRequest() {
            return securityDirectRequest;
        }

        /**
         * Gets metrics.
         * @return the metrics
         */
        public Metrics getMetrics() {
            return metrics;
        }

        /**
         * Gets event.
         * @return the event
         */
        public Event getEvent() {
            return event;
        }

        /**
         * Is security external view boolean.
         * @return the boolean
         */
        public boolean isSecurityExternalView() {
            return securityExternalView;
        }

        /**
         * Gets uri.
         * @return the uri
         */
        public String getUri() {
            return uri;
        }

        /**
         * Is security https boolean.
         * @return the boolean
         */
        public boolean isSecurityHttps() {
            return securityHttps;
        }

        /**
         * Gets request response map.
         * @return the request response map
         */
        public Map<String, RequestResponse> getRequestResponseMap() {
            return requestResponseMap;
        }

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
                this.securityCsrfToken = CsrfUtil.getStrategy().modifySecurityCsrfToken(this.uri, this.method,
                        securityElement.getAttribute("csrf-token"));
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

        private String name;
        private String type;
        private String value;
        private String statusCode;
        private boolean saveLastView = false;
        private boolean saveCurrentView = false;

        /**
         * Gets name.
         * @return the name
         */
        public String getName() {
            return name;
        }

        /**
         * Is save last view boolean.
         * @return the boolean
         */
        public boolean isSaveLastView() {
            return saveLastView;
        }

        /**
         * Is save current view boolean.
         * @return the boolean
         */
        public boolean isSaveCurrentView() {
            return saveCurrentView;
        }

        /**
         * Is save home view boolean.
         * @return the boolean
         */
        public boolean isSaveHomeView() {
            return saveHomeView;
        }

        private boolean saveHomeView = false;
        private Map<String, String> redirectParameterMap = new HashMap<>();
        private Map<String, String> redirectParameterValueMap = new HashMap<>();

        /**
         * Gets status code.
         * @return the status code
         */
        public String getStatusCode() {
            return statusCode;
        }

        /**
         * Gets redirect parameter map.
         * @return the redirect parameter map
         */
        public Map<String, String> getRedirectParameterMap() {
            return redirectParameterMap;
        }

        /**
         * Gets redirect parameter value map.
         * @return the redirect parameter value map
         */
        public Map<String, String> getRedirectParameterValueMap() {
            return redirectParameterValueMap;
        }

        /**
         * Gets type.
         * @return the type
         */
        public String getType() {
            return type;
        }

        /**
         * Gets value.
         * @return the value
         */
        public String getValue() {
            return value;
        }

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
                    this.redirectParameterValueMap.put(redirectParameterElement.getAttribute("name"),
                            redirectParameterElement.getAttribute("value"));
                } else {
                    String from = redirectParameterElement.getAttribute("from");
                    if (from.isEmpty()) from = redirectParameterElement.getAttribute("name");
                    this.redirectParameterMap.put(redirectParameterElement.getAttribute("name"), from);
                }
            }
        }
    }

    public static class ViewMap {
        private String viewMap;
        private String name;
        private String page;
        private String type;
        private String info;
        private String contentType;
        private String encoding;
        private String xFrameOption;
        private String strictTransportSecurity;
        private String description;
        private boolean noCache = false;

        /**
         * Gets view map.
         * @return the view map
         */
        public String getViewMap() {
            return viewMap;
        }

        /**
         * Gets name.
         *
         * @return the name
         */
        public String getName() {
            return name;
        }

        /**
         * Gets frame option.
         * @return the frame option
         */
        public String getxFrameOption() {
            return xFrameOption;
        }

        /**
         * Gets strict transport security.
         * @return the strict transport security
         */
        public String getStrictTransportSecurity() {
            return strictTransportSecurity;
        }

        /**
         * Gets description.
         * @return the description
         */
        public String getDescription() {
            return description;
        }

        /**
         * Gets info.
         * @return the info
         */
        public String getInfo() {
            return info;
        }

        /**
         * Gets page.
         *
         * @return the page
         */
        public String getPage() {
            return page;
        }

        /**
         * Is no cache boolean.
         *
         * @return the boolean
         */
        public boolean isNoCache() {
            return noCache;
        }

        /**
         * Gets type.
         * @return the type
         */
        public String getType() {
            return type;
        }

        /**
         * Gets content type.
         * @return the content type
         */
        public String getContentType() {
            return contentType;
        }

        /**
         * Gets encoding.
         * @return the encoding
         */
        public String getEncoding() {
            return encoding;
        }

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
