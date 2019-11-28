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
package org.apache.ofbiz.webapp;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;

import org.apache.ofbiz.base.component.ComponentConfig;
import org.apache.ofbiz.base.component.ComponentConfig.WebappInfo;
import org.apache.ofbiz.base.util.Assert;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.UtilXml.LocalErrorHandler;
import org.apache.ofbiz.base.util.UtilXml.LocalResolver;
import org.apache.ofbiz.base.util.cache.UtilCache;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.DelegatorFactory;
import org.apache.ofbiz.security.Security;
import org.apache.ofbiz.security.SecurityConfigurationException;
import org.apache.ofbiz.security.SecurityFactory;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceContainer;
import org.apache.ofbiz.webapp.event.RequestBodyMapHandlerFactory;
import org.apache.tomcat.util.descriptor.DigesterFactory;
import org.apache.tomcat.util.descriptor.web.ServletDef;
import org.apache.tomcat.util.descriptor.web.WebRuleSet;
import org.apache.tomcat.util.descriptor.web.WebXml;
import org.apache.tomcat.util.digester.Digester;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Web application utilities.
 * <p>This class reuses some of the Tomcat/Catalina classes for convenience, but
 * OFBiz does not need to be running on Tomcat for this to work.</p>
 */
public final class WebAppUtil {

    public static final String module = WebAppUtil.class.getName();
    public static final String CONTROL_MOUNT_POINT = "control";
    private static final String webAppFileName = "/WEB-INF/web.xml";
    private static final UtilCache<String, WebXml> webXmlCache = UtilCache.createUtilCache("webapp.WebXml");

    /**
     * Returns the control servlet path. The path consists of the web application's mount-point
     * specified in the <code>ofbiz-component.xml</code> file and the servlet mapping specified
     * in the web application's <code>web.xml</code> file.
     * 
     * @param webAppInfo
     * @throws IOException
     * @throws SAXException
     */
    public static String getControlServletPath(WebappInfo webAppInfo) throws IOException, SAXException {
        Assert.notNull("webAppInfo", webAppInfo);
        String servletMapping = null;
        WebXml webXml = getWebXml(webAppInfo);
        for (ServletDef servletDef : webXml.getServlets().values()) {
            if ("org.apache.ofbiz.webapp.control.ControlServlet".equals(servletDef.getServletClass()) || "org.apache.ofbiz.product.category.SeoControlServlet".equals(servletDef.getServletClass())) {
                String servletName = servletDef.getServletName();
                // Catalina servlet mappings: key = url-pattern, value = servlet-name.
                for (Entry<String, String> entry : webXml.getServletMappings().entrySet()) {
                    if (servletName.equals(entry.getValue())) {
                        servletMapping = entry.getKey();
                        break;
                    }
                }
                break;
            }
        }
        if (servletMapping == null) {
            throw new IllegalArgumentException("org.apache.ofbiz.webapp.control.ControlServlet mapping not found in " + webAppInfo.getLocation() + webAppFileName);
        }
        servletMapping = servletMapping.replace("*", "");
        String servletPath = webAppInfo.contextRoot.concat(servletMapping);
        return servletPath;
    }

    public static boolean isDistributable(WebappInfo appinfo) throws IOException, SAXException {
        WebXml webxml = getWebXml(appinfo);
        return webxml.isDistributable();
    }

    /**
     * Returns the <code>WebappInfo</code> instance associated to the specified web site ID.
     * Throws <code>IllegalArgumentException</code> if the web site ID was not found.
     * 
     * @param webSiteId
     * @throws IOException
     * @throws SAXException
     */
    public static WebappInfo getWebappInfoFromWebsiteId(String webSiteId) throws IOException, SAXException {
        Assert.notNull("webSiteId", webSiteId);
        for (WebappInfo webAppInfo : ComponentConfig.getAllWebappResourceInfos()) {
            if (webSiteId.equals(WebAppUtil.getWebSiteId(webAppInfo))) {
                return webAppInfo;
            }
        }
        throw new IllegalArgumentException("Web site ID '" + webSiteId + "' not found.");
    }

    /**
     * Returns the web site ID - as configured in the web application's <code>web.xml</code> file,
     * or <code>null</code> if no web site ID was found.
     * 
     * @param webAppInfo
     * @throws IOException
     * @throws SAXException
     */
    public static String getWebSiteId(WebappInfo webAppInfo) throws IOException, SAXException {
        Assert.notNull("webAppInfo", webAppInfo);
        WebXml webXml = getWebXml(webAppInfo);
        return webXml.getContextParams().get("webSiteId");
    }

    public static LocalDispatcher getDispatcher(ServletContext servletContext) {
        LocalDispatcher dispatcher = (LocalDispatcher) servletContext.getAttribute("dispatcher");
        if (dispatcher == null) {
            Delegator delegator = getDelegator(servletContext);
            dispatcher = makeWebappDispatcher(servletContext, delegator);
            servletContext.setAttribute("dispatcher", dispatcher);
        }
        return dispatcher;
    }

    public static void setAttributesFromRequestBody(ServletRequest request) {
        // read the body (for JSON requests) and set the parameters as attributes:
        Map<String, Object> requestBodyMap = null;
        try {
            requestBodyMap = RequestBodyMapHandlerFactory.extractMapFromRequestBody(request);
        } catch (IOException ioe) {
            Debug.logWarning(ioe, module);
        }
        if (requestBodyMap != null) {
            Set<String> parameterNames = requestBodyMap.keySet();
            for (String parameterName: parameterNames) {
                request.setAttribute(parameterName, requestBodyMap.get(parameterName));
            }
        }
    }

    /** This method only sets up a dispatcher for the current webapp and passed in delegator, it does not save it to the ServletContext or anywhere else, just returns it */
    public static LocalDispatcher makeWebappDispatcher(ServletContext servletContext, Delegator delegator) {
        if (delegator == null) {
            Debug.logError("[ContextFilter.init] ERROR: delegator not defined.", module);
            return null;
        }
        // get the unique name of this dispatcher
        String dispatcherName = servletContext.getInitParameter("localDispatcherName");

        if (dispatcherName == null) {
            Debug.logError("No localDispatcherName specified in the web.xml file", module);
            dispatcherName = delegator.getDelegatorName();
        }

        LocalDispatcher dispatcher = ServiceContainer.getLocalDispatcher(dispatcherName, delegator);
        if (dispatcher == null) {
            Debug.logError("[ContextFilter.init] ERROR: dispatcher could not be initialized.", module);
        }

        return dispatcher;
    }

    public static Delegator getDelegator(ServletContext servletContext) {
        Delegator delegator = (Delegator) servletContext.getAttribute("delegator");
        if (delegator == null) {
            String delegatorName = servletContext.getInitParameter("entityDelegatorName");

            if (UtilValidate.isEmpty(delegatorName)) {
                delegatorName = "default";
            }
            if (Debug.verboseOn()) Debug.logVerbose("Setup Entity Engine Delegator with name " + delegatorName, module);
            delegator = DelegatorFactory.getDelegator(delegatorName);
            servletContext.setAttribute("delegator", delegator);
            if (delegator == null) {
                Debug.logError("[ContextFilter.init] ERROR: delegator factory returned null for delegatorName \"" + delegatorName + "\"", module);
            }
        }
        return delegator;
    }

    public static Security getSecurity(ServletContext servletContext) {
        Security security = (Security) servletContext.getAttribute("security");
        if (security == null) {
            Delegator delegator = (Delegator) servletContext.getAttribute("delegator");

            if (delegator != null) {
                try {
                    security = SecurityFactory.getInstance(delegator);
                } catch (SecurityConfigurationException e) {
                    Debug.logError(e, "Unable to obtain an instance of the security object.", module);
                }
            }
            servletContext.setAttribute("security", security);
            if (security == null) {
                Debug.logError("An invalid (null) Security object has been set in the servlet context.", module);
            }
        }
        return security;
    }

    /**
     * Returns a <code>WebXml</code> instance that models the web application's <code>web.xml</code> file.
     * 
     * @param webAppInfo
     * @throws IOException
     * @throws SAXException
     */
    private static WebXml getWebXml(WebappInfo webAppInfo) throws IOException, SAXException {
        Assert.notNull("webAppInfo", webAppInfo);
        String webXmlFileLocation = webAppInfo.getLocation().concat(webAppFileName);
        return parseWebXmlFile(webXmlFileLocation, true);
    }

    /**
     * Parses the specified <code>web.xml</code> file into a <code>WebXml</code> instance.
     * 
     * @param webXmlFileLocation
     * @param validate
     * @throws IOException
     * @throws SAXException
     */
    private static WebXml parseWebXmlFile(String webXmlFileLocation, boolean validate) throws IOException, SAXException {
        Assert.notEmpty("webXmlFileLocation", webXmlFileLocation);
        WebXml result = webXmlCache.get(webXmlFileLocation);
        if (result == null) {
            File file = new File(webXmlFileLocation);
            if (!file.exists()) {
                throw new IllegalArgumentException(webXmlFileLocation + " does not exist.");
            }
            boolean namespaceAware = true;
            result = new WebXml();
            LocalResolver lr = new LocalResolver(new DefaultHandler());
            ErrorHandler handler = new LocalErrorHandler(webXmlFileLocation, lr);
            Digester digester = DigesterFactory.newDigester(validate, namespaceAware, new WebRuleSet(), false);
            digester.push(result);
            digester.setErrorHandler(handler);
            try (InputStream is = new FileInputStream(file)) {
                InputSource iso = new InputSource(is);
                iso.setSystemId(file.getAbsolutePath());
                digester.parse(iso);
            } finally {
                digester.reset();
            }
            result = webXmlCache.putIfAbsentAndGet(webXmlFileLocation, result);
        }
        return result;
    }

    private WebAppUtil() {}
}
