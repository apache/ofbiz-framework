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

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
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

    private static final String MODULE = WebAppUtil.class.getName();
    public static final String CONTROL_MOUNT_POINT = "control";
    private static final Path WEB_APP_FILE_NAME = Paths.get("WEB-INF", "web.xml");
    private static final UtilCache<Path, WebXml> WEB_XML_CACHE = UtilCache.createUtilCache("webapp.WebXml");

    /**
     * Returns the control servlet path. The path consists of the web application's mount-point
     * specified in the <code>ofbiz-component.xml</code> file and the servlet mapping specified
     * in the web application's <code>web.xml</code> file.
     * @param webAppInfo
     * @throws IOException
     * @throws SAXException
     */
    public static String getControlServletPath(WebappInfo webAppInfo) throws IOException, SAXException {
        Assert.notNull("webAppInfo", webAppInfo);
        String servletMapping = null;
        WebXml webXml = getWebXml(webAppInfo);
        for (ServletDef servletDef : webXml.getServlets().values()) {
            if ("org.apache.ofbiz.webapp.control.ControlServlet".equals(servletDef.getServletClass())
                    || "org.apache.ofbiz.product.category.SeoControlServlet".equals(servletDef.getServletClass())) {
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
            Debug.logWarning("org.apache.ofbiz.webapp.control.ControlServlet mapping not found in "
                    + webAppInfo.location().resolve(WEB_APP_FILE_NAME), MODULE);
            return "";
        }
        servletMapping = servletMapping.replace("*", "");
        String servletPath = webAppInfo.getContextRoot().concat(servletMapping);
        return servletPath;
    }

    public static boolean isDistributable(WebappInfo appinfo) throws IOException, SAXException {
        WebXml webxml = getWebXml(appinfo);
        return webxml.isDistributable();
    }

    /**
     * Returns the <code>WebappInfo</code> instance associated to the specified web site ID.
     * Throws <code>IllegalArgumentException</code> if the web site ID was not found.
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
            Debug.logWarning(ioe, MODULE);
        }
        if (requestBodyMap != null) {
            Set<String> parameterNames = requestBodyMap.keySet();
            for (String parameterName: parameterNames) {
                request.setAttribute(parameterName, requestBodyMap.get(parameterName));
            }
        }
    }

    /** This method only sets up a dispatcher for the current webapp and passed in delegator, it does not save it to the ServletContext
     * or anywhere else, just returns it */
    public static LocalDispatcher makeWebappDispatcher(ServletContext servletContext, Delegator delegator) {
        if (delegator == null) {
            Debug.logError("[ContextFilter.init] ERROR: delegator not defined.", MODULE);
            return null;
        }
        // get the unique name of this dispatcher
        String dispatcherName = servletContext.getInitParameter("localDispatcherName");

        if (dispatcherName == null) {
            Debug.logError("No localDispatcherName specified in the web.xml file", MODULE);
            dispatcherName = delegator.getDelegatorName();
        }

        LocalDispatcher dispatcher = ServiceContainer.getLocalDispatcher(dispatcherName, delegator);
        if (dispatcher == null) {
            Debug.logError("[ContextFilter.init] ERROR: dispatcher could not be initialized.", MODULE);
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
            if (Debug.verboseOn()) {
                Debug.logVerbose("Setup Entity Engine Delegator with name " + delegatorName, MODULE);
            }
            delegator = DelegatorFactory.getDelegator(delegatorName);
            servletContext.setAttribute("delegator", delegator);
            if (delegator == null) {
                Debug.logError("[ContextFilter.init] ERROR: delegator factory returned null for delegatorName \"" + delegatorName + "\"", MODULE);
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
                    Debug.logError(e, "Unable to obtain an instance of the security object.", MODULE);
                }
            }
            servletContext.setAttribute("security", security);
            if (security == null) {
                Debug.logError("An invalid (null) Security object has been set in the servlet context.", MODULE);
            }
        }
        return security;
    }

    /**
     * Returns a <code>WebXml</code> instance that models the web application's <code>web.xml</code> file.
     * @param webAppInfo
     * @throws IOException
     * @throws SAXException
     */
    private static WebXml getWebXml(WebappInfo webAppInfo) throws IOException, SAXException {
        Assert.notNull("webAppInfo", webAppInfo);
        Path webXmlFileLocation = webAppInfo.location().resolve(WEB_APP_FILE_NAME);
        return parseWebXmlFile(webXmlFileLocation, true);
    }

    /**
     * Parses the specified <code>web.xml</code> file into a <code>WebXml</code> instance.
     * @param webXmlLocation
     * @param validate
     * @throws IOException
     * @throws SAXException
     */
    private static WebXml parseWebXmlFile(Path webXmlLocation, boolean validate) throws IOException, SAXException {
        Objects.requireNonNull(webXmlLocation, "webXmlFileLocation");
        WebXml result = WEB_XML_CACHE.get(webXmlLocation);
        if (result == null) {
            if (Files.notExists(webXmlLocation)) {
                throw new IllegalArgumentException(webXmlLocation + " does not exist.");
            }

            boolean namespaceAware = true;
            result = new WebXml();
            LocalResolver lr = new LocalResolver(new DefaultHandler());
            ErrorHandler handler = new LocalErrorHandler(webXmlLocation.toString(), lr);
            Digester digester = DigesterFactory.newDigester(validate, namespaceAware, new WebRuleSet(), false);
            digester.push(result);
            digester.setErrorHandler(handler);
            try (InputStream is = Files.newInputStream(webXmlLocation)) {
                InputSource iso = new InputSource(is);
                iso.setSystemId(webXmlLocation.toString());
                digester.parse(iso);
            } finally {
                digester.reset();
            }
            result = WEB_XML_CACHE.putIfAbsentAndGet(webXmlLocation, result);
        }
        return result;
    }

    private WebAppUtil() { }
}
