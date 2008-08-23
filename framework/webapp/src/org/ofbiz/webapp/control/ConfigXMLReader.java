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
package org.ofbiz.webapp.control;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javolution.util.FastList;
import javolution.util.FastMap;
import javolution.util.FastSet;

import org.ofbiz.base.location.FlexibleLocation;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.FileUtil;
import org.ofbiz.base.util.GeneralException;
import static org.ofbiz.base.util.UtilGenerics.checkMap;
import org.ofbiz.base.util.UtilHttp;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.UtilXml;
import org.ofbiz.base.util.cache.UtilCache;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * ConfigXMLReader.java - Reads and parses the XML site config files.
 */
public class ConfigXMLReader {

    public static final String module = ConfigXMLReader.class.getName();
    public static UtilCache<URL, ControllerConfig> controllerCache = new UtilCache<URL, ControllerConfig>("webapp.ControllerConfig");
    public static UtilCache<String, List<ControllerConfig>> controllerSearchResultsCache = new UtilCache<String, List<ControllerConfig>>("webapp.ControllerSearchResults");

    public static ControllerConfig getControllerConfig(URL url) {
        ControllerConfig controllerConfig = controllerCache.get(url);
        if (controllerConfig == null) { // don't want to block here
            synchronized (ConfigXMLReader.class) {
                // must check if null again as one of the blocked threads can still enter
                controllerConfig = controllerCache.get(url);
                if (controllerConfig == null) {
                    controllerConfig = new ControllerConfig(url);
                    controllerCache.put(url, controllerConfig);
                }
            }
        }
        return controllerConfig;
    }
    
    public static class ControllerConfig {
        public URL url;
        
        public Map<String, Object> configMap = FastMap.newInstance();
        public Map<String, Map<String, String>> handlerMap = FastMap.newInstance();
        public Map<String, Map<String, Object>> requestMap = FastMap.newInstance();
        public Map<String, Map<String, String>> viewMap = FastMap.newInstance();
        public String defaultRequest = null;

        public ControllerConfig(URL url) {
            this.url = url;
            
            Element rootElement = loadDocument(url);
            if (rootElement != null) {
                this.configMap = loadConfigMap(rootElement, url);
                this.handlerMap = loadHandlerMap(rootElement, url);
                this.requestMap = loadRequestMap(rootElement, url);
                this.viewMap = loadViewMap(rootElement, url);
                this.defaultRequest = loadDefaultRequest(rootElement, url);
            }
        }
    }
    
    public static Set<String> findControllerFilesWithRequest(String requestUri, String controllerPartialPath) throws GeneralException {
        Set<String> allControllerRequestSet = FastSet.newInstance();
        
        if (UtilValidate.isEmpty(requestUri)) {
            return allControllerRequestSet;
        }
        
        String cacheId = controllerPartialPath != null ? controllerPartialPath : "NOPARTIALPATH";
        List<ControllerConfig> controllerConfigs = (List<ControllerConfig>) controllerSearchResultsCache.get(cacheId);
        
        if (controllerConfigs == null) {
            try {
                // find controller.xml file with webappMountPoint + "/WEB-INF" in the path
                List<File> controllerFiles = FileUtil.findXmlFiles(null, controllerPartialPath, "site-conf", "site-conf.xsd");
                
                controllerConfigs = FastList.newInstance();
                for (File controllerFile: controllerFiles) {
                    URL controllerUrl = null;
                    try {
                        controllerUrl = controllerFile.toURL();
                    } catch(MalformedURLException mue) {
                        throw new GeneralException(mue);
                    }
                    ControllerConfig cc = ConfigXMLReader.getControllerConfig(controllerUrl);
                    controllerConfigs.add(cc);
                }
                
                controllerSearchResultsCache.put(cacheId, controllerConfigs);
            } catch (IOException e) {
                throw new GeneralException("Error finding controller XML files to lookup request references: " + e.toString(), e);
            }
        }
        
        if (controllerConfigs != null) {
            for (ControllerConfig cc: controllerConfigs) {
                // make sure it has the named request in it
                if (cc.requestMap.get(requestUri) != null) {
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
            if (webappMountPoint != null) webappMountPoint += "/WEB-INF";
            String requestUri = UtilHttp.getRequestUriFromTarget(target);
            
            Set<String> controllerLocAndRequestSet = ConfigXMLReader.findControllerFilesWithRequest(requestUri, webappMountPoint);
            // if (controllerLocAndRequestSet.size() > 0) Debug.logInfo("============== In findRequestNamesLinkedtoInWidget, controllerLocAndRequestSet: " + controllerLocAndRequestSet, module);
            return controllerLocAndRequestSet;
        } else {
            return FastSet.newInstance();
        }
    }

    /** Site Config Variables */
    public static final String DEFAULT_ERROR_PAGE = "errorpage";
    public static final String SITE_OWNER = "owner";
    public static final String SECURITY_CLASS = "security-class";
    public static final String FIRSTVISIT = "firstvisit";
    public static final String PREPROCESSOR = "preprocessor";
    public static final String POSTPROCESSOR = "postprocessor";

    /** URI Config Variables */
    public static final String INCLUDE = "include";
    public static final String INCLUDE_LOCATION = "location";

    public static final String DEFAULT_REQUEST = "default-request";
    public static final String REQUEST_MAPPING = "request-map";
    public static final String REQUEST_URI = "uri";
    public static final String REQUEST_EDIT = "edit";
    public static final String REQUEST_TRACK_STATS = "track-serverhit";
    public static final String REQUEST_TRACK_VISIT = "track-visit";

    public static final String REQUEST_DESCRIPTION = "description";
    public static final String ERROR_PAGE_DEFAULT = "error";
    public static final String NEXT_PAGE_DEFAULT = "success";

    public static final String SECURITY = "security";
    public static final String SECURITY_HTTPS = "https";
    public static final String SECURITY_AUTH = "auth";
    public static final String SECURITY_CERT = "cert";    
    public static final String SECURITY_EXTVIEW = "external-view";
    public static final String SECURITY_DIRECT = "direct-request";

    public static final String EVENT = "event";
    public static final String EVENT_PATH = "path";
    public static final String EVENT_TYPE = "type";
    public static final String EVENT_METHOD = "invoke";
    public static final String EVENT_GLOBAL_TRANSACTION = "global-transaction";

    public static final String RESPONSE = "response";
    public static final String RESPONSE_NAME = "name";
    public static final String RESPONSE_TYPE = "type";
    public static final String RESPONSE_VALUE = "value";
    public static final String RESPONSE_MAP = "response-map";

    /** View Config Variables */
    public static final String VIEW_MAPPING = "view-map";
    public static final String VIEW_NAME = "name";
    public static final String VIEW_PAGE = "page";
    public static final String VIEW_TYPE = "type";
    public static final String VIEW_INFO = "info";
    public static final String VIEW_CONTENT_TYPE = "content-type";
    public static final String VIEW_ENCODING = "encoding";
    public static final String VIEW_DESCRIPTION = "description";

    /** Handler Config Variables */
    public static final String HANDLER = "handler";
    public static final String HANDLER_NAME = "name";
    public static final String HANDLER_TYPE = "type";
    public static final String HANDLER_CLASS = "class";
    
    /** Loads the XML file and returns the root element */
    public static Element loadDocument(URL location) {
        Document document;
        try {
            document = UtilXml.readXmlDocument(location, true);
            Element rootElement = document.getDocumentElement();
            // rootElement.normalize();
            if (Debug.verboseOn()) Debug.logVerbose("Loaded XML Config - " + location, module);
            return rootElement;
        } catch (Exception e) {
            Debug.logError(e, module);
        }
        return null;
    }

    /** Gets a Map of request mappings. */
    public static Map<String, Map<String, Object>> getRequestMap(URL xml) {
        ControllerConfig controllerConfig = getControllerConfig(xml);
        return controllerConfig != null ? controllerConfig.requestMap : null;
    }

    /** Gets a FastMap of request mappings. */
    public static Map<String, Map<String, Object>> loadRequestMap(Element root, URL xml) {
        long startTime = System.currentTimeMillis();
        Map<String, Map<String, Object>> map = FastMap.newInstance();
        if (root == null) {
            root = loadDocument(xml);
        }

        if (root == null) return map;

        for (Element includeElement: UtilXml.childElementList(root, INCLUDE)) {
            String includeLocation = includeElement.getAttribute(INCLUDE_LOCATION);
            if ((includeLocation != null) && (includeLocation.length() > 0)) {
                try {
                    Map<String, Map<String, Object>> subMap = loadRequestMap(null, FlexibleLocation.resolveLocation(includeLocation));
                    map.putAll(subMap);
                } catch (MalformedURLException mue) {
                    Debug.logError(mue, "Error processing include at [" + includeLocation + "]:" + mue.toString(), module);
                }
            }
        }

        for (Element requestMapElement: UtilXml.childElementList(root, REQUEST_MAPPING)) {
            
            // Create a URI-MAP for each element found.
            Map<String, Object> uriMap = FastMap.newInstance();

            // Get the URI info.
            String uri = requestMapElement.getAttribute(REQUEST_URI);
            String edit = requestMapElement.getAttribute(REQUEST_EDIT);
            String stats = requestMapElement.getAttribute(REQUEST_TRACK_STATS);
            String visit = requestMapElement.getAttribute(REQUEST_TRACK_VISIT);

            if (edit == null || edit.equals(""))
                edit = "true";
            if (uri != null) {
                uriMap.put(REQUEST_URI, uri);
                uriMap.put(REQUEST_EDIT, edit);
                uriMap.put(REQUEST_TRACK_STATS, stats);
                uriMap.put(REQUEST_TRACK_VISIT, visit);
            }

            // Check for security.
            Element securityElement = UtilXml.firstChildElement(requestMapElement, SECURITY);
            if (securityElement != null) {
                String securityHttps = securityElement.getAttribute(SECURITY_HTTPS);
                String securityAuth = securityElement.getAttribute(SECURITY_AUTH);
                String securityCert = securityElement.getAttribute(SECURITY_CERT);
                String securityExtView = securityElement.getAttribute(SECURITY_EXTVIEW);
                String securityDirectRequest = securityElement.getAttribute(SECURITY_DIRECT);

                // if x509 is required so is https
                if ("true".equalsIgnoreCase(securityCert)) {
                    securityHttps = "true";
                }
                
                uriMap.put(SECURITY_HTTPS, securityHttps);
                uriMap.put(SECURITY_AUTH, securityAuth);
                uriMap.put(SECURITY_CERT, securityCert);               
                uriMap.put(SECURITY_EXTVIEW, securityExtView);
                uriMap.put(SECURITY_DIRECT, securityDirectRequest);
            }

            // Check for an event.
            Element eventElement = UtilXml.firstChildElement(requestMapElement, EVENT);
            if (eventElement != null) {
                String type = eventElement.getAttribute(EVENT_TYPE);
                String path = eventElement.getAttribute(EVENT_PATH);
                String invoke = eventElement.getAttribute(EVENT_METHOD);

                uriMap.put(EVENT_TYPE, type);
                uriMap.put(EVENT_PATH, path);
                uriMap.put(EVENT_METHOD, invoke);
                
                // Check for a global-transaction attribute - default to true
                uriMap.put(EVENT_GLOBAL_TRANSACTION, eventElement.hasAttribute(EVENT_GLOBAL_TRANSACTION) ? eventElement.getAttribute(EVENT_GLOBAL_TRANSACTION) : "true");
            }

            // Check for a description.
            String description = UtilXml.childElementValue(requestMapElement, REQUEST_DESCRIPTION);
            uriMap.put(REQUEST_DESCRIPTION, UtilValidate.isNotEmpty(description) ? description : "");

            // Get the response(s).
            Map<String, String> responseMap = FastMap.newInstance();
            uriMap.put(RESPONSE_MAP, responseMap);
            
            for (Element responseElement: UtilXml.childElementList(requestMapElement, RESPONSE)) {
                String name = responseElement.getAttribute(RESPONSE_NAME);
                String type = responseElement.getAttribute(RESPONSE_TYPE);
                String value = responseElement.getAttribute(RESPONSE_VALUE);
                
                responseMap.put(name, type + ":" + value);
            }

            if (uri != null) {
                map.put(uri, uriMap);
            }
        }

        /* Debugging */
        if (Debug.verboseOn()) {
            Debug.logVerbose("-------- Request Mappings --------", module);
            //FastMap debugMap = map;
            Set<String> debugSet = map.keySet();
            Iterator<String> i = debugSet.iterator();
            while (i.hasNext()) {
                Object o = i.next();
                String request = (String) o;
                Map<String, Object> thisURI = map.get(o);

                StringBuilder verboseMessageBuffer = new StringBuilder();

                Iterator<String> debugIter = thisURI.keySet().iterator();
                while (debugIter.hasNext()) {
                    Object lo = debugIter.next();
                    String name = (String) lo;
                    String value = thisURI.get(lo).toString();

                    verboseMessageBuffer.append("[").append(name).append("=>").append(value).append("]");
                }
                Debug.logVerbose(request + " :: " + verboseMessageBuffer.toString(), module);
            }
            Debug.logVerbose("------ End Request Mappings ------", module);
        }
        /* End Debugging */

        double totalSeconds = (System.currentTimeMillis() - startTime)/1000.0;
        if (Debug.infoOn()) Debug.logInfo("RequestMap Created: (" + map.size() + ") records in " + totalSeconds + "s", module);
        return map;
    }

    /** Gets a FastMap of view mappings. */
    public static Map<String, Map<String, String>> getViewMap(URL xml) {
        ControllerConfig controllerConfig = getControllerConfig(xml);
        return controllerConfig != null ? controllerConfig.viewMap : null;
    }

    /** Gets a FastMap of view mappings. */
    public static Map<String, Map<String, String>> loadViewMap(Element root, URL xml) {
        long startTime = System.currentTimeMillis();
        Map<String, Map<String, String>> map = FastMap.newInstance();
        if (root == null) {
            root = loadDocument(xml);
        }

        if (root == null) {
            return map;
        }

        for (Element includeElement: UtilXml.childElementList(root, INCLUDE)) {
            String includeLocation = includeElement.getAttribute(INCLUDE_LOCATION);
            if ((includeLocation != null) && (includeLocation.length() > 0)) {
                try {
                    Map<String, Map<String, String>> subMap = loadViewMap(null, FlexibleLocation.resolveLocation(includeLocation));
                    map.putAll(subMap);
                } catch (MalformedURLException mue) {
                    Debug.logError(mue, "Error processing include at [" + includeLocation + "]:" + mue.toString(), module);
                }
            }
        }

        for (Element viewMapElement: UtilXml.childElementList(root, VIEW_MAPPING)) {
            // Create a URI-MAP for each element found.
            Map<String, String> uriMap = FastMap.newInstance();

            // Get the view info.
            String name = viewMapElement.getAttribute(VIEW_NAME);
            String page = viewMapElement.getAttribute(VIEW_PAGE);
            if (page == null || page.length() == 0) {
                page = name;
            }

            uriMap.put(VIEW_NAME, name);
            uriMap.put(VIEW_PAGE, page);
            uriMap.put(VIEW_TYPE, viewMapElement.getAttribute(VIEW_TYPE));
            uriMap.put(VIEW_INFO, viewMapElement.getAttribute(VIEW_INFO));
            uriMap.put(VIEW_CONTENT_TYPE, viewMapElement.getAttribute(VIEW_CONTENT_TYPE));
            uriMap.put(VIEW_ENCODING, viewMapElement.getAttribute(VIEW_ENCODING));

            // Check for a description.
            String description = UtilXml.childElementValue(viewMapElement, VIEW_DESCRIPTION);
            uriMap.put(VIEW_DESCRIPTION, UtilValidate.isNotEmpty(description) ? description : "");

            if (name != null) map.put(name, uriMap);
        }

        /* Debugging */
        if (Debug.verboseOn()) {
            Debug.logVerbose("-------- View Mappings --------", module);
            //FastMap debugMap = map;
            Set debugSet = map.keySet();
            Iterator i = debugSet.iterator();
    
            while (i.hasNext()) {
                Object o = i.next();
                String request = (String) o;
                Map thisURI = (Map) map.get(o);
    
                StringBuilder verboseMessageBuffer = new StringBuilder();
    
                Iterator debugIter = thisURI.keySet().iterator();
                while (debugIter.hasNext()) {
                    Object lo = debugIter.next();
                    String name = (String) lo;
                    String value = (String) thisURI.get(lo);
    
                    verboseMessageBuffer.append("[").append(name).append("=>").append(value).append("]");
                }
                Debug.logVerbose(request + " :: " + verboseMessageBuffer.toString(), module);
            }
            Debug.logVerbose("------ End View Mappings ------", module);
        }
        /* End Debugging */

        double totalSeconds = (System.currentTimeMillis() - startTime)/1000.0;
        if (Debug.infoOn()) Debug.logInfo("ViewMap Created: (" + map.size() + ") records in " + totalSeconds + "s", module);
        return map;
    }

    /** Gets a FastMap of site configuration variables. */
    public static Map<String, Object> getConfigMap(URL xml) {
        ControllerConfig controllerConfig = getControllerConfig(xml);
        return controllerConfig != null ? controllerConfig.configMap : null;
    }

    /** Gets a FastMap of site configuration variables. */
    public static Map<String, Object> loadConfigMap(Element root, URL xml) {
        long startTime = System.currentTimeMillis();
        FastMap<String, Object> map = FastMap.newInstance();
        if (root == null) {
            root = loadDocument(xml);
        }
        
        if (root == null) {
            return map;
        }

        for (Element includeElement: UtilXml.childElementList(root, INCLUDE)) {
            String includeLocation = includeElement.getAttribute(INCLUDE_LOCATION);
            if ((includeLocation != null) && (includeLocation.length() > 0)) {
                try {
                    Map<String, Object> subMap = loadConfigMap(null, FlexibleLocation.resolveLocation(includeLocation));
                    map.putAll(subMap);
                } catch (MalformedURLException mue) {
                    Debug.logError(mue, "Error processing include at [" + includeLocation + "]:" + mue.toString(), module);
                }
            }
        }

        // default error page
        String errorpage = UtilXml.childElementValue(root, DEFAULT_ERROR_PAGE);
        if (UtilValidate.isNotEmpty(errorpage)) map.put(DEFAULT_ERROR_PAGE, errorpage);

        // site owner
        String owner = UtilXml.childElementValue(root, SITE_OWNER);
        if (UtilValidate.isNotEmpty(owner)) map.put(SITE_OWNER, owner);

        // security class
        String securityClass = UtilXml.childElementValue(root, SECURITY_CLASS);
        if (UtilValidate.isNotEmpty(securityClass)) map.put(SECURITY_CLASS, securityClass);

        // first visit event
        Element firstvisitElement = UtilXml.firstChildElement(root, FIRSTVISIT);
        if (firstvisitElement != null) {
            List<Map<String, String>> eventList = FastList.newInstance();
            for (Element eventElement: UtilXml.childElementList(firstvisitElement, EVENT)) {
                Map<String, String> eventMap = FastMap.newInstance();
                eventMap.put(EVENT_TYPE, eventElement.getAttribute(EVENT_TYPE));
                eventMap.put(EVENT_PATH, eventElement.getAttribute(EVENT_PATH));
                eventMap.put(EVENT_METHOD, eventElement.getAttribute(EVENT_METHOD));
            
                // Check for a global-transaction attribute - default to true
                eventMap.put(EVENT_GLOBAL_TRANSACTION, eventElement.hasAttribute(EVENT_GLOBAL_TRANSACTION) ? eventElement.getAttribute(EVENT_GLOBAL_TRANSACTION) : "true");
                eventList.add(eventMap);
            }
            map.put(FIRSTVISIT, eventList);
        }

        // preprocessor events
        Element preprocessorElement = UtilXml.firstChildElement(root, PREPROCESSOR);
        if (preprocessorElement != null) {
            List<Map<String, String>> eventList = FastList.newInstance();
            for (Element eventElement: UtilXml.childElementList(preprocessorElement, EVENT)) {
                Map<String, String> eventMap = FastMap.newInstance();
                eventMap.put(EVENT_TYPE, eventElement.getAttribute(EVENT_TYPE));
                eventMap.put(EVENT_PATH, eventElement.getAttribute(EVENT_PATH));
                eventMap.put(EVENT_METHOD, eventElement.getAttribute(EVENT_METHOD));
            
                // Check for a global-transaction attribute - default to true
                eventMap.put(EVENT_GLOBAL_TRANSACTION, eventElement.hasAttribute(EVENT_GLOBAL_TRANSACTION) ? eventElement.getAttribute(EVENT_GLOBAL_TRANSACTION) : "true");
                eventList.add(eventMap);
            }
            map.put(PREPROCESSOR, eventList);
        }

        // postprocessor events
        Element postprocessorElement = UtilXml.firstChildElement(root, POSTPROCESSOR);
        if (postprocessorElement != null) {
            List<Map<String, String>> eventList = FastList.newInstance();
            for (Element eventElement: UtilXml.childElementList(postprocessorElement, EVENT)) {
                Map<String, String> eventMap = FastMap.newInstance();
                eventMap.put(EVENT_TYPE, eventElement.getAttribute(EVENT_TYPE));
                eventMap.put(EVENT_PATH, eventElement.getAttribute(EVENT_PATH));
                eventMap.put(EVENT_METHOD, eventElement.getAttribute(EVENT_METHOD));
            
                // Check for a global-transaction attribute - default to true
                eventMap.put(EVENT_GLOBAL_TRANSACTION, eventElement.hasAttribute(EVENT_GLOBAL_TRANSACTION) ? eventElement.getAttribute(EVENT_GLOBAL_TRANSACTION) : "true");
                eventList.add(eventMap);
            }
            map.put(POSTPROCESSOR, eventList);
        }

        // after-login events
        Element afterLoginElement = UtilXml.firstChildElement(root, "after-login");
        if (afterLoginElement != null) {
            List<Map<String, String>> eventList = FastList.newInstance();
            for (Element eventElement: UtilXml.childElementList(afterLoginElement, EVENT)) {
                Map<String, String> eventMap = FastMap.newInstance();
                eventMap.put(EVENT_TYPE, eventElement.getAttribute(EVENT_TYPE));
                eventMap.put(EVENT_PATH, eventElement.getAttribute(EVENT_PATH));
                eventMap.put(EVENT_METHOD, eventElement.getAttribute(EVENT_METHOD));
            
                // Check for a global-transaction attribute - default to true
                eventMap.put(EVENT_GLOBAL_TRANSACTION, eventElement.hasAttribute(EVENT_GLOBAL_TRANSACTION) ? eventElement.getAttribute(EVENT_GLOBAL_TRANSACTION) : "true");
                eventList.add(eventMap);
            }
            map.put("after-login", eventList);
        }

        // before-logout events
        Element beforeLogoutElement = UtilXml.firstChildElement(root, "before-logout");
        if (beforeLogoutElement != null) {
            List<Map<String, String>> eventList = FastList.newInstance();
            List<? extends Element> eventElementList = UtilXml.childElementList(beforeLogoutElement, EVENT);
            for (Element eventElement: UtilXml.childElementList(beforeLogoutElement, EVENT)) {
                Map<String, String> eventMap = FastMap.newInstance();
                eventMap.put(EVENT_TYPE, eventElement.getAttribute(EVENT_TYPE));
                eventMap.put(EVENT_PATH, eventElement.getAttribute(EVENT_PATH));
                eventMap.put(EVENT_METHOD, eventElement.getAttribute(EVENT_METHOD));
            
                // Check for a global-transaction attribute - default to true
                eventMap.put(EVENT_GLOBAL_TRANSACTION, eventElement.hasAttribute(EVENT_GLOBAL_TRANSACTION) ? eventElement.getAttribute(EVENT_GLOBAL_TRANSACTION) : "true");
                eventList.add(eventMap);
            }
            map.put("before-logout", eventList);
        }

        /* Debugging */
        /*
         Debug.logVerbose("-------- Config Mappings --------", module);
         FastMap debugMap = map;
         Set debugSet = debugMap.keySet();
         Iterator i = debugSet.iterator();
         while (i.hasNext()) {
         Object o = i.next();
         String request = (String) o;
         FastMap thisURI = (FastMap) debugMap.get(o);
         Debug.logVerbose(request, module);
         Iterator debugIter = ((Set) thisURI.keySet()).iterator();
         while (debugIter.hasNext()) {
         Object lo = debugIter.next();
         String name = (String) lo;
         String value = (String) thisURI.get(lo);
         if (Debug.verboseOn()) Debug.logVerbose("\t" + name + " -> " + value, module);
         }
         }
         Debug.logVerbose("------ End Config Mappings ------", module);
         */
        /* End Debugging */

        double totalSeconds = (System.currentTimeMillis() - startTime)/1000.0;
        if (Debug.infoOn()) Debug.logInfo("ConfigMap Created: (" + map.size() + ") records in " + totalSeconds + "s", module);
        return map;
    }

    /** Gets the default-request from the configuration */
    public static String getDefaultRequest(URL xml) {
        ControllerConfig controllerConfig = getControllerConfig(xml);
        return controllerConfig != null ? controllerConfig.defaultRequest : null;
    }

    public static String loadDefaultRequest(Element root, URL xml) {
        if (root == null) {
            root = loadDocument(xml);
        }
        if (root == null) {
            return null;
        }

        // holder for the default-request
        String defaultRequest = null;

        for (Element includeElement: UtilXml.childElementList(root, INCLUDE)) {
            String includeLocation = includeElement.getAttribute(INCLUDE_LOCATION);
            if ((includeLocation != null) && (includeLocation.length() > 0)) {
                try {
                    defaultRequest = loadDefaultRequest(null, FlexibleLocation.resolveLocation(includeLocation));
                } catch (MalformedURLException mue) {
                    Debug.logError(mue, "Error processing include at [" + includeLocation + "]:" + mue.toString(), module);
                }
            }
        }

        Element e = UtilXml.firstChildElement(root, "default-request");
        if (e != null) {
            defaultRequest = e.getAttribute("request-uri");
        }
        return defaultRequest;
    }

    /** Gets a FastMap of handler mappings. */
    public static Map<String, Map<String, String>> getHandlerMap(URL xml) {
        ControllerConfig controllerConfig = getControllerConfig(xml);
        return controllerConfig != null ? controllerConfig.handlerMap : null;
    }

    public static Map<String, Map<String, String>> loadHandlerMap(Element root, URL xml) {
        long startTime = System.currentTimeMillis();
        Map<String, Map<String, String>> map = FastMap.newInstance();
        if (root == null) {
            root = loadDocument(xml);
        }
        if (root == null) {
            return map;
        }

        for (Element includeElement: UtilXml.childElementList(root, INCLUDE)) {
            String includeLocation = includeElement.getAttribute(INCLUDE_LOCATION);
            if ((includeLocation != null) && (includeLocation.length() > 0)) {
                try {
                    Map<String, Map<String, String>> subMap = loadHandlerMap(null, FlexibleLocation.resolveLocation(includeLocation));

                    Map<String, String> newViewHandlerMap = checkMap(subMap.get("view"), String.class, String.class);
                    Map<String, String> viewHandlerMap = checkMap(map.get("view"), String.class, String.class);
                    if (viewHandlerMap == null) {
                        map.put("view", newViewHandlerMap);
                    } else {
                        if (newViewHandlerMap != null) {
                            viewHandlerMap.putAll(newViewHandlerMap);
                        }
                    }

                    Map<String, String> newEventHandlerMap = checkMap(subMap.get("event"), String.class, String.class);
                    Map<String, String> eventHandlerMap = checkMap(map.get("event"), String.class, String.class);
                    if (eventHandlerMap == null) {
                        map.put("event", newEventHandlerMap);
                    } else {
                        if (newEventHandlerMap != null) {
                            eventHandlerMap.putAll(newEventHandlerMap);
                        }
                    }
                } catch (MalformedURLException mue) {
                    Debug.logError(mue, "Error processing include at [" + includeLocation + "]:" + mue.toString(), module);
                }
            }
        }

        Map<String, String> eventMap = FastMap.newInstance();
        Map<String, String> viewMap = FastMap.newInstance();

        for (Element handlerElement: UtilXml.childElementList(root, HANDLER)) {
            String hName = checkEmpty(handlerElement.getAttribute(HANDLER_NAME));
            String hClass = checkEmpty(handlerElement.getAttribute(HANDLER_CLASS));
            String hType = checkEmpty(handlerElement.getAttribute(HANDLER_TYPE));
            if (hType.equals("view")) {
                viewMap.put(hName, hClass);
            } else {
                eventMap.put(hName, hClass);
            }
        }

        Map<String, String> viewHandlerMap = checkMap(map.get("view"), String.class, String.class);
        if (viewHandlerMap == null) {
            map.put("view", viewMap);
        } else {
            if (viewMap != null) {
                viewHandlerMap.putAll(viewMap);
            }
        }
        Map<String, String> eventHandlerMap = checkMap(map.get("event"), String.class, String.class);
        if (eventHandlerMap == null) {
            map.put("event", eventMap);
        } else {
            if (eventMap != null) {
                eventHandlerMap.putAll(eventMap);
            }
        }

        /* Debugging */
        if (Debug.verboseOn()) {
            Debug.logVerbose("-------- Handler Mappings --------", module);
            Map<String, String> debugMap = checkMap(map.get("event"), String.class, String.class);

            if (UtilValidate.isNotEmpty(debugMap)) {
                Debug.logVerbose("-------------- EVENT -------------", module);
                for (Map.Entry<String, String> entry: debugMap.entrySet()) {
                    String handlerName = entry.getKey();
                    String className = entry.getValue();
                    Debug.logVerbose("[EH] : " + handlerName + " => " + className, module);
                }
            }
            debugMap = checkMap(map.get("view"), String.class, String.class);
            if (UtilValidate.isNotEmpty(debugMap)) {
                Debug.logVerbose("-------------- VIEW --------------", module);
                for (Map.Entry<String, String> entry: debugMap.entrySet()) {
                    String handlerName = entry.getKey();
                    String className = entry.getValue();
                    Debug.logVerbose("[VH] : " + handlerName + " => " + className, module);
                }
            }
            Debug.logVerbose("------ End Handler Mappings ------", module);
        }

        double totalSeconds = (System.currentTimeMillis() - startTime)/1000.0;
        if (Debug.infoOn()) Debug.logInfo("HandlerMap Created: (" + ((Map) map.get("view")).size() + ") view handlers and (" + ((Map) map.get("event")).size() + ") request/event handlers in " + totalSeconds + "s", module);
        return map;
    }

    private static String checkEmpty(String string) {
        if (string != null && string.length() > 0)
            return string;
        else
            return "";
    }
}
