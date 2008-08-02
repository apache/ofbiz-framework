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

import java.io.Serializable;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.Collection;
import java.util.Map;
import java.util.List;
import javax.servlet.ServletContext;

import javolution.util.FastList;

import org.ofbiz.base.util.Debug;
import static org.ofbiz.base.util.UtilGenerics.checkMap;
import org.ofbiz.base.util.UtilValidate;

/**
 * RequestManager - Manages request, config and view mappings.
 */
public class RequestManager implements Serializable {

    public static final String module = RequestManager.class.getName();
    public static final int VIEW_HANDLER_KEY = 1;
    public static final int EVENT_HANDLER_KEY = 0;

    private URL configFileUrl;
    private URL webInfUrl;

    public RequestManager(ServletContext context) {

        /** Loads the site configuration from servlet context parameter. */
        try {
            configFileUrl = context.getResource("/WEB-INF/controller.xml");
            webInfUrl = context.getResource("/WEB-INF");
        } catch (Exception e) {
            Debug.logError(e, "[RequestManager.constructor] Error Finding XML Config File: " +
                "/WEB-INF/controller.xml", module);
        }
        // do quick inits:
        ConfigXMLReader.getConfigMap(configFileUrl);
        ConfigXMLReader.getHandlerMap(configFileUrl);
        ConfigXMLReader.getRequestMap(configFileUrl);
        ConfigXMLReader.getViewMap(configFileUrl);
    }

    /** Gets the entire handler mapping */
    public Map<String, Map<String, String>> getHandlerMap() {
        return ConfigXMLReader.getHandlerMap(configFileUrl);
    }

    /** Gets the class name of the named handler */
    public String getHandlerClass(String name, int type) {
        Map<String, Map<String, String>> map = getHandlerMap();
        Map<String, String> hMap;

        if (type == RequestManager.VIEW_HANDLER_KEY) {
            hMap = checkMap(map.get("view"), String.class, String.class);
        } else {
            hMap = checkMap(map.get("event"), String.class, String.class);
        }

        if (!hMap.containsKey(name)) {
            return null;
        } else {
            return hMap.get(name);
        }
    }

    public List<String> getHandlerKeys(int type) {
        Map<String, Map<String, String>> map = getHandlerMap();
        Map<String, String> hMap;

        if (type == RequestManager.VIEW_HANDLER_KEY) {
            hMap = checkMap(map.get("view"), String.class, String.class);
        } else {
            hMap = checkMap(map.get("event"), String.class, String.class);
        }

        if (hMap != null) {
            List<String> result = FastList.newInstance();
            result.addAll(hMap.keySet());
            return result;
        } else {
            return null;
        }
    }

    public Map<String, Object> getRequestMapMap(String uriStr) {
        if (UtilValidate.isNotEmpty(uriStr)) {
            return ConfigXMLReader.getRequestMap(configFileUrl).get(uriStr);
        } else {
            return null;
        }
    }

    public String getRequestAttribute(String uriStr, String attribute) {
        Map<String, Object> uri = getRequestMapMap(uriStr);

        if (uri != null && attribute != null) {
            String value = (String) uri.get(attribute);
            if (value == null) {
                // not found, try the response Map (though better to hit that directly when desired)
                Map<String, String> responseMap = checkMap(uri.get(ConfigXMLReader.RESPONSE_MAP), String.class, String.class);
                value = responseMap.get(attribute);
            }
            return value;
        } else {
            Debug.logInfo("[RequestManager.getRequestAttribute] Value for attribute \"" + attribute +
                "\" of uri \"" + uriStr + "\" not found", module);
            return null;
        }
    }
    
    public String getRequestResponseValue(String uriStr, String responseName) {
        if (responseName == null) return null;
        
        Map<String, Object> uri = getRequestMapMap(uriStr);
        if (uri == null) return null;
        Map<String, String> responseMap = checkMap(uri.get(ConfigXMLReader.RESPONSE_MAP), String.class, String.class);
        if (responseMap == null) return null;
        
        return responseMap.get(responseName);
    }

    /** Gets the event class from the requestMap */
    public String getEventPath(String uriStr) {
        Map<String, Object> uri = getRequestMapMap(uriStr);

        if (uri != null)
            return (String) uri.get(ConfigXMLReader.EVENT_PATH);
        else {
            Debug.logWarning("[RequestManager.getEventPath] Path of event for request \"" + uriStr +
                "\" not found", module);
            return null;
        }
    }

    /** Gets the event type from the requestMap */
    public String getEventType(String uriStr) {
        Map uri = getRequestMapMap(uriStr);

        if (uri != null)
            return (String) uri.get(ConfigXMLReader.EVENT_TYPE);
        else {
            Debug.logWarning("[RequestManager.getEventType] Type of event for request \"" + uriStr +
                "\" not found", module);
            return null;
        }
    }

    /** Gets the event method from the requestMap */
    public String getEventMethod(String uriStr) {
        Map<String, Object> uri = getRequestMapMap(uriStr);

        if (uri != null) {
            return (String) uri.get(ConfigXMLReader.EVENT_METHOD);
        } else {
            Debug.logWarning("[RequestManager.getEventMethod] Method of event for request \"" +
                uriStr + "\" not found", module);
            return null;
        }
    }

    /** Gets the event global-transaction from the requestMap */
    public boolean getEventGlobalTransaction(String uriStr) {
        Map<String, Object> uri = getRequestMapMap(uriStr);

        if (uri != null) {
            return Boolean.valueOf((String) uri.get(ConfigXMLReader.EVENT_GLOBAL_TRANSACTION)).booleanValue();
        } else {
            if (Debug.verboseOn()) {
                Debug.logWarning("[RequestManager.getEventGlobalTransaction] Global-transaction of event for request \"" +
                    uriStr + "\" not found, defaulting to true", module);
            }
            return false;
        }
    }

    /** Gets the view name from the requestMap */
    public String getViewName(String uriStr) {
        Map<String, Object> uri = getRequestMapMap(uriStr);

        if (uri != null)
            return checkMap(uri.get(ConfigXMLReader.RESPONSE_MAP), String.class, String.class).get(ConfigXMLReader.NEXT_PAGE_DEFAULT);
        else {
            Debug.logWarning("[RequestManager.getViewName] View name for uri \"" + uriStr + "\" not found", module);
            return null;
        }
    }

    /** Gets the next page (jsp) from the viewMap */
    public String getViewPage(String viewStr) {
        if (viewStr != null && viewStr.startsWith("view:")) viewStr = viewStr.substring(viewStr.indexOf(':') + 1);
        Map<String, String> page = ConfigXMLReader.getViewMap(configFileUrl).get(viewStr);

        if (page != null) {
            return page.get(ConfigXMLReader.VIEW_PAGE);
        } else {
            Debug.logWarning("[RequestManager.getViewPage] View with name \"" + viewStr + "\" not found", module);
            return null;
        }
    }

    /** Gets the type of this view */
    public String getViewType(String viewStr) {
        Map<String, String> view = ConfigXMLReader.getViewMap(configFileUrl).get(viewStr);

        if (view != null) {
            return view.get(ConfigXMLReader.VIEW_TYPE);
        } else {
            Debug.logWarning("[RequestManager.getViewType] View with name \"" + viewStr + "\" not found", module);
            return null;
        }
    }

    /** Gets the info of this view */
    public String getViewInfo(String viewStr) {
        Map<String, String> view = ConfigXMLReader.getViewMap(configFileUrl).get(viewStr);

        if (view != null) {
            return view.get(ConfigXMLReader.VIEW_INFO);
        } else {
            Debug.logWarning("[RequestManager.getViewInfo] View with name \"" + viewStr + "\" not found", module);
            return null;
        }
    }
    
    /** Gets the content-type of this view */
    public String getViewContentType(String viewStr) {
        Map<String, String> view = ConfigXMLReader.getViewMap(configFileUrl).get(viewStr);

        if (view != null) {
            return view.get(ConfigXMLReader.VIEW_CONTENT_TYPE);
        } else {
            Debug.logWarning("[RequestManager.getViewInfo] View with name \"" + viewStr + "\" not found", module);
            return null;
        }
    }    
    
    /** Gets the content-type of this view */
    public String getViewEncoding(String viewStr) {
        Map view = (Map) ConfigXMLReader.getViewMap(configFileUrl).get(viewStr);

        if (view != null) {
            return (String) view.get(ConfigXMLReader.VIEW_ENCODING);
        } else {
            Debug.logWarning("[RequestManager.getViewInfo] View with name \"" + viewStr + "\" not found", module);
            return null;
        }
    }        

    /** Gets the error page from the requestMap, if none uses the default */
    public String getErrorPage(String uriStr) {
        //Debug.logInfo("uriStr is: " + uriStr, module);
        Map<String, Object> uri = getRequestMapMap(uriStr);
        //Debug.logInfo("RequestMapMap is: " + uri, module);

        if (uri != null) {
            String errorViewUri = checkMap(uri.get(ConfigXMLReader.RESPONSE_MAP), String.class, String.class).get(ConfigXMLReader.ERROR_PAGE_DEFAULT);
            //Debug.logInfo("errorViewUri is: " + errorViewUri, module);
            String returnPage = getViewPage(errorViewUri);
            //Debug.logInfo("Got returnPage for ErrorPage: " + returnPage, module);

            if (returnPage != null) {
                return returnPage;
            } else {
                return getDefaultErrorPage();
            }
        } else {
            return getDefaultErrorPage();
        }
    }

    /** Gets the default error page from the configMap or static site default */
    public String getDefaultErrorPage() {
        String errorPage;
        errorPage = (String) ConfigXMLReader.getConfigMap(configFileUrl).get(ConfigXMLReader.DEFAULT_ERROR_PAGE);
        //Debug.logInfo("For DefaultErrorPage got errorPage: " + errorPage, module);
        if (errorPage != null) return errorPage;
        return "/error/error.jsp";
    }

    public boolean requiresAuth(String uriStr) {
        Map<String, Object> uri = getRequestMapMap(uriStr);

        if (uri != null) {
            String value = (String) uri.get(ConfigXMLReader.SECURITY_AUTH);

            //if (Debug.verboseOn()) Debug.logVerbose("Require Auth: " + value, module);
            return "true".equalsIgnoreCase(value);
        } else
            return false;
    }

    public boolean requiresHttps(String uriStr) {
        Map<String, Object> uri = getRequestMapMap(uriStr);

        if (uri != null) {
            String value = (String) uri.get(ConfigXMLReader.SECURITY_HTTPS);

            //if (Debug.verboseOn()) Debug.logVerbose("Requires HTTPS: " + value, module);
            return "true".equalsIgnoreCase(value);
        } else
            return false;
    }

    public boolean requiresHttpsClientCert(String uriStr) {
        Map uri = getRequestMapMap(uriStr);

        if (uri != null) {
            String value = (String) uri.get(ConfigXMLReader.SECURITY_CERT);

            //if (Debug.verboseOn()) Debug.logVerbose("Requires x.509 Cert: " + value, module);
            return "true".equalsIgnoreCase(value);
        } else
            return false;

    }
       
    public boolean allowExtView(String uriStr) {
        Map<String, Object> uri = getRequestMapMap(uriStr);

        if (uri != null) {
            String value = (String) uri.get(ConfigXMLReader.SECURITY_EXTVIEW);

            //if (Debug.verboseOn()) Debug.logVerbose("Allow External View: " + value, module);
            return !"false".equalsIgnoreCase(value);
        } else
            return true;
    }

    public boolean allowDirectRequest(String uriStr) {
        Map<String, Object> uri = getRequestMapMap(uriStr);

        if (uri != null) {
            String value = (String) uri.get(ConfigXMLReader.SECURITY_DIRECT);

            //if (Debug.verboseOn()) Debug.logVerbose("Allow Direct Request: " + value, module);
            return !"false".equalsIgnoreCase(value);
        } else
            return false;
    }

    public String getDefaultRequest() {
        return ConfigXMLReader.getDefaultRequest(configFileUrl);
    }

    @SuppressWarnings("unchecked")
    public Collection<Map<String, String>> getFirstVisitEvents() {
        return (Collection<Map<String, String>>) ConfigXMLReader.getConfigMap(configFileUrl).get(ConfigXMLReader.FIRSTVISIT);
    }

    @SuppressWarnings("unchecked")
    public Collection<Map<String, String>> getPreProcessor() {
        return (Collection<Map<String, String>>) ConfigXMLReader.getConfigMap(configFileUrl).get(ConfigXMLReader.PREPROCESSOR);
    }

    @SuppressWarnings("unchecked")
    public Collection<Map<String, String>> getPostProcessor() {
        return (Collection<Map<String, String>>) ConfigXMLReader.getConfigMap(configFileUrl).get(ConfigXMLReader.POSTPROCESSOR);
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, String>> getAfterLoginEventList() {
        return (List<Map<String, String>>) ConfigXMLReader.getConfigMap(configFileUrl).get("after-login");
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, String>> getBeforeLogoutEventList() {
        return (List<Map<String, String>>) ConfigXMLReader.getConfigMap(configFileUrl).get("before-logout");        
    }
}
