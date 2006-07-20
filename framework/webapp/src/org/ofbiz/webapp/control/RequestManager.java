/*
 * Copyright 2001-2006 The Apache Software Foundation
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package org.ofbiz.webapp.control;

import java.io.Serializable;
import java.net.URL;
import java.util.Collection;
import java.util.Map;
import java.util.List;
import java.util.LinkedList;
import javax.servlet.ServletContext;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilValidate;

/**
 * RequestManager - Manages request, config and view mappings.
 *
 * @author     <a href="mailto:jaz@ofbiz.org">Andy Zeneski</a>
 * @version    $Rev$
 * @since      2.0
 */
public class RequestManager implements Serializable {

    public static final String module = RequestManager.class.getName();
    public static final int VIEW_HANDLER_KEY = 1;
    public static final int EVENT_HANDLER_KEY = 0;

    private URL configFileUrl;

    public RequestManager(ServletContext context) {

        /** Loads the site configuration from servlet context parameter. */
        try {
            configFileUrl = context.getResource("/WEB-INF/controller.xml");
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
    public Map getHandlerMap() {
        return (Map) ConfigXMLReader.getHandlerMap(configFileUrl);
    }

    /** Gets the class name of the named handler */
    public String getHandlerClass(String name, int type) {
        Map map = getHandlerMap();
        Map hMap = null;

        if (type == 1) {
            hMap = (Map) map.get("view");
        } else {
            hMap = (Map) map.get("event");
        }

        if (!hMap.containsKey(name)) {
            return null;
        } else {
            return (String) hMap.get(name);
        }
    }

    public List getHandlerKeys(int type) {
        Map map = getHandlerMap();
        Map hMap = null;

        if (type == 1) {
            hMap = (Map) map.get("view");
        } else {
            hMap = (Map) map.get("event");
        }

        if (hMap != null) {
            return new LinkedList(hMap.keySet());
        } else {
            return null;
        }
    }

    public Map getRequestMapMap(String uriStr) {
        if (UtilValidate.isNotEmpty(uriStr)) {
            return (Map) ConfigXMLReader.getRequestMap(configFileUrl).get(uriStr);
        } else {
            return null;
        }
    }

    public String getRequestAttribute(String uriStr, String attribute) {
        Map uri = getRequestMapMap(uriStr);

        if (uri != null && attribute != null) {
            return (String) uri.get(attribute);
        } else {
            Debug.logInfo("[RequestManager.getRequestAttribute] Value for attribute \"" + attribute +
                "\" of uri \"" + uriStr + "\" not found", module);
            return null;
        }
    }

    /** Gets the event class from the requestMap */
    public String getEventPath(String uriStr) {
        Map uri = getRequestMapMap(uriStr);

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
        Map uri = getRequestMapMap(uriStr);

        if (uri != null) {
            return (String) uri.get(ConfigXMLReader.EVENT_METHOD);
        } else {
            Debug.logWarning("[RequestManager.getEventMethod] Method of event for request \"" +
                uriStr + "\" not found", module);
            return null;
        }
    }

    /** Gets the view name from the requestMap */
    public String getViewName(String uriStr) {
        Map uri = getRequestMapMap(uriStr);

        if (uri != null)
            return (String) uri.get(ConfigXMLReader.NEXT_PAGE);
        else {
            Debug.logWarning("[RequestManager.getViewName] View name for uri \"" + uriStr + "\" not found", module);
            return null;
        }
    }

    /** Gets the next page (jsp) from the viewMap */
    public String getViewPage(String viewStr) {
        if (viewStr != null && viewStr.startsWith("view:")) viewStr = viewStr.substring(viewStr.indexOf(':') + 1);
        Map page = (Map) ConfigXMLReader.getViewMap(configFileUrl).get(viewStr);

        if (page != null) {
            return (String) page.get(ConfigXMLReader.VIEW_PAGE);
        } else {
            Debug.logWarning("[RequestManager.getViewPage] View with name \"" + viewStr + "\" not found", module);
            return null;
        }
    }

    /** Gets the type of this view */
    public String getViewType(String viewStr) {
        Map view = (Map) ConfigXMLReader.getViewMap(configFileUrl).get(viewStr);

        if (view != null) {
            return (String) view.get(ConfigXMLReader.VIEW_TYPE);
        } else {
            Debug.logWarning("[RequestManager.getViewType] View with name \"" + viewStr + "\" not found", module);
            return null;
        }
    }

    /** Gets the info of this view */
    public String getViewInfo(String viewStr) {
        Map view = (Map) ConfigXMLReader.getViewMap(configFileUrl).get(viewStr);

        if (view != null) {
            return (String) view.get(ConfigXMLReader.VIEW_INFO);
        } else {
            Debug.logWarning("[RequestManager.getViewInfo] View with name \"" + viewStr + "\" not found", module);
            return null;
        }
    }
    
    /** Gets the content-type of this view */
    public String getViewContentType(String viewStr) {
        Map view = (Map) ConfigXMLReader.getViewMap(configFileUrl).get(viewStr);

        if (view != null) {
            return (String) view.get(ConfigXMLReader.VIEW_CONTENT_TYPE);
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
        Map uri = getRequestMapMap(uriStr);
        //Debug.logInfo("RequestMapMap is: " + uri, module);

        if (uri != null) {
            String errorViewUri = (String) uri.get(ConfigXMLReader.ERROR_PAGE);
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
        String errorPage = null;
        errorPage = (String) ConfigXMLReader.getConfigMap(configFileUrl).get(ConfigXMLReader.DEFAULT_ERROR_PAGE);
        //Debug.logInfo("For DefaultErrorPage got errorPage: " + errorPage, module);
        if (errorPage != null) return errorPage;
        return "/error/error.jsp";
    }

    public boolean requiresAuth(String uriStr) {
        Map uri = getRequestMapMap(uriStr);

        if (uri != null) {
            String value = (String) uri.get(ConfigXMLReader.SECURITY_AUTH);

            //if (Debug.verboseOn()) Debug.logVerbose("Require Auth: " + value, module);
            if ("true".equalsIgnoreCase(value))
                return true;
            else
                return false;
        } else
            return false;
    }

    public boolean requiresHttps(String uriStr) {
        Map uri = getRequestMapMap(uriStr);

        if (uri != null) {
            String value = (String) uri.get(ConfigXMLReader.SECURITY_HTTPS);

            //if (Debug.verboseOn()) Debug.logVerbose("Requires HTTPS: " + value, module);
            if ("true".equalsIgnoreCase(value))
                return true;
            else
                return false;
        } else
            return false;
    }

    public boolean allowExtView(String uriStr) {
        Map uri = getRequestMapMap(uriStr);

        if (uri != null) {
            String value = (String) uri.get(ConfigXMLReader.SECURITY_EXTVIEW);

            //if (Debug.verboseOn()) Debug.logVerbose("Allow External View: " + value, module);
            if ("false".equalsIgnoreCase(value))
                return false;
            else
                return true;
        } else
            return true;
    }

    public boolean allowDirectRequest(String uriStr) {
        Map uri = getRequestMapMap(uriStr);

        if (uri != null) {
            String value = (String) uri.get(ConfigXMLReader.SECURITY_DIRECT);

            //if (Debug.verboseOn()) Debug.logVerbose("Allow Direct Request: " + value, module);
            if ("false".equalsIgnoreCase(value))
                return false;
            else
                return true;
        } else
            return false;
    }

    public Collection getFirstVisitEvents() {
        Collection c = (Collection) ConfigXMLReader.getConfigMap(configFileUrl).get(ConfigXMLReader.FIRSTVISIT);
        return c;
    }

    public Collection getPreProcessor() {
        Collection c = (Collection) ConfigXMLReader.getConfigMap(configFileUrl).get(ConfigXMLReader.PREPROCESSOR);
        return c;
    }

    public Collection getPostProcessor() {
        Collection c = (Collection) ConfigXMLReader.getConfigMap(configFileUrl).get(ConfigXMLReader.POSTPROCESSOR);
        return c;
    }

    public List getAfterLoginEventList() {
        List lst = (List) ConfigXMLReader.getConfigMap(configFileUrl).get("after-login");
        return lst;
    }

    public List getBeforeLogoutEventList() {
        List lst = (List) ConfigXMLReader.getConfigMap(configFileUrl).get("before-logout");
        return lst;
    }
}
