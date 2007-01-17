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

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.ofbiz.base.container.ContainerLoader;
import org.ofbiz.base.start.StartupException;
import org.ofbiz.base.util.CachedClassLoader;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.StringUtil;
import org.ofbiz.base.util.UtilHttp;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.UtilObject;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.security.Security;
import org.ofbiz.security.SecurityConfigurationException;
import org.ofbiz.security.SecurityFactory;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.WebAppDispatcher;

/**
 * ContextFilter - Restricts access to raw files and configures servlet objects.
 */
public class ContextFilter implements Filter {

    public static final String module = ContextFilter.class.getName();
    public static final String CONTAINER_CONFIG = "limited-containers.xml";
    public static final String FORWARDED_FROM_SERVLET = "_FORWARDED_FROM_SERVLET_";

    protected ClassLoader localCachedClassLoader = null;
    protected FilterConfig config = null;
    protected boolean debug = false;

    /**
     * @see javax.servlet.Filter#init(javax.servlet.FilterConfig)
     */
    public void init(FilterConfig config) throws ServletException {
        this.config = config;

        // puts all init-parameters in ServletContext attributes for easier parameterization without code changes
        this.putAllInitParametersInAttributes();

        // initialize the cached class loader for this application
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        localCachedClassLoader = new CachedClassLoader(loader, (String) config.getServletContext().getAttribute("webSiteId"));

        // set debug
        this.debug = "true".equalsIgnoreCase(config.getInitParameter("debug"));
        if (!debug) {
            debug = Debug.verboseOn();
        }

        // load the containers
        getContainers();
        // check the serverId
        getServerId();
        // initialize the delegator
        getDelegator();
        // initialize security
        getSecurity();
        // initialize the services dispatcher
        getDispatcher();

        // this will speed up the initial sessionId generation
        new java.security.SecureRandom().nextLong();
    }

    /**
     * @see javax.servlet.Filter#doFilter(javax.servlet.ServletRequest, javax.servlet.ServletResponse, javax.servlet.FilterChain)
     */
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponseWrapper wrapper = new HttpServletResponseWrapper((HttpServletResponse) response);

        // Debug.logInfo("Running ContextFilter.doFilter", module);

        // ----- Servlet Object Setup -----
        // set the cached class loader for more speedy running in this thread
        Thread.currentThread().setContextClassLoader(localCachedClassLoader);

        // set the webSiteId in the session
        httpRequest.getSession().setAttribute("webSiteId", config.getServletContext().getAttribute("webSiteId"));

        // set the ServletContext in the request for future use
        request.setAttribute("servletContext", config.getServletContext());

        // set the filesystem path of context root.
        request.setAttribute("_CONTEXT_ROOT_", config.getServletContext().getRealPath("/"));

        // set the server root url
        StringBuffer serverRootUrl = UtilHttp.getServerRootUrl(httpRequest);
        request.setAttribute("_SERVER_ROOT_URL_", serverRootUrl.toString());

        // request attributes from redirect call
        String reqAttrMapHex = (String) httpRequest.getSession().getAttribute("_REQ_ATTR_MAP_");
        if (UtilValidate.isNotEmpty(reqAttrMapHex)) {
            byte[] reqAttrMapBytes = StringUtil.fromHexString(reqAttrMapHex);
            Map reqAttrMap = (Map) UtilObject.getObject(reqAttrMapBytes);
            if (reqAttrMap != null) {
                Iterator i = reqAttrMap.keySet().iterator();
                while (i.hasNext()) {
                    String key = (String) i.next();
                    request.setAttribute(key, reqAttrMap.get(key));
                }
            }
            httpRequest.getSession().removeAttribute("_REQ_ATTR_MAP_");
        }

        // ----- Context Security -----
        // check if we are disabled
        String disableSecurity = config.getInitParameter("disableContextSecurity");
        if (disableSecurity != null && "Y".equals(disableSecurity)) {
            chain.doFilter(request, response);
            return;
        }

        // check if we are told to redirect everthing
        String redirectAllTo = config.getInitParameter("forceRedirectAll");
        if (redirectAllTo != null && redirectAllTo.length() > 0) {
            // little trick here so we don't loop on ourself
            if (httpRequest.getSession().getAttribute("_FORCE_REDIRECT_") == null) {
                httpRequest.getSession().setAttribute("_FORCE_REDIRECT_", "true");
                Debug.logWarning("Redirecting user to: " + redirectAllTo, module);

                if (!redirectAllTo.toLowerCase().startsWith("http")) {
                    redirectAllTo = httpRequest.getContextPath() + redirectAllTo;
                }
                wrapper.sendRedirect(redirectAllTo);
                return;
            } else {
                httpRequest.getSession().removeAttribute("_FORCE_REDIRECT_");
                chain.doFilter(request, response);
                return;
            }
        }

        // test to see if we have come through the control servlet already, if not do the processing
        if (request.getAttribute(ContextFilter.FORWARDED_FROM_SERVLET) == null) {
            // Debug.logInfo("In ContextFilter.doFilter, FORWARDED_FROM_SERVLET is NOT set", module);
            String allowedPath = config.getInitParameter("allowedPaths");
            String redirectPath = config.getInitParameter("redirectPath");
            String errorCode = config.getInitParameter("errorCode");

            List allowList = StringUtil.split(allowedPath, ":");
            allowList.add("/");  // No path is allowed.
            allowList.add("");   // No path is allowed.

            if (debug) Debug.log("[Request]: " + httpRequest.getRequestURI(), module);

            String requestPath = httpRequest.getServletPath();
            if (requestPath == null) requestPath = "";
            if (requestPath.lastIndexOf("/") > 0) {
                if (requestPath.indexOf("/") == 0) {
                    requestPath = "/" + requestPath.substring(1, requestPath.indexOf("/", 1));
                } else {
                    requestPath = requestPath.substring(1, requestPath.indexOf("/"));
                }
            }

            String requestInfo = httpRequest.getServletPath();
            if (requestInfo == null) requestInfo = "";
            if (requestInfo.lastIndexOf("/") >= 0) {
                requestInfo = requestInfo.substring(0, requestInfo.lastIndexOf("/")) + "/*";
            }

            StringBuffer contextUriBuffer = new StringBuffer();
            if (httpRequest.getContextPath() != null) {
                contextUriBuffer.append(httpRequest.getContextPath());
            }
            if (httpRequest.getServletPath() != null) {
                contextUriBuffer.append(httpRequest.getServletPath());
            }
            if (httpRequest.getPathInfo() != null) {
                contextUriBuffer.append(httpRequest.getPathInfo());
            }
            String contextUri = contextUriBuffer.toString();

            // Verbose Debugging
            if (Debug.verboseOn()) {
                for (int i = 0; i < allowList.size(); i++) {
                    Debug.logVerbose("[Allow]: " + allowList.get(i), module);
                }
                Debug.logVerbose("[Request path]: " + requestPath, module);
                Debug.logVerbose("[Request info]: " + requestInfo, module);
                Debug.logVerbose("[Servlet path]: " + httpRequest.getServletPath(), module);
            }

            // check to make sure the requested url is allowed
            if (!allowList.contains(requestPath) && !allowList.contains(requestInfo) && !allowList.contains(httpRequest.getServletPath())) {
                String filterMessage = "[Filtered request]: " + contextUri;

                if (redirectPath == null) {
                    int error = 404;
                    try {
                        error = Integer.parseInt(errorCode);
                    } catch (NumberFormatException nfe) {
                        Debug.logWarning(nfe, "Error code specified would not parse to Integer : " + errorCode, module);
                    }
                    filterMessage = filterMessage + " (" + error + ")";
                    wrapper.sendError(error, contextUri);
                } else {
                    filterMessage = filterMessage + " (" + redirectPath + ")";
                    if (!redirectPath.toLowerCase().startsWith("http")) {
                        redirectPath = httpRequest.getContextPath() + redirectPath;
                    }
                    wrapper.sendRedirect(redirectPath);
                }
                Debug.logWarning(filterMessage, module);
                return;
            }
        }

        // we're done checking; continue on
        chain.doFilter(request, response);
    }

    /**
     * @see javax.servlet.Filter#destroy()
     */
    public void destroy() {
        getDispatcher().deregister();
        config = null;
    }

    protected LocalDispatcher getDispatcher() {
        LocalDispatcher dispatcher = (LocalDispatcher) config.getServletContext().getAttribute("dispatcher");
        if (dispatcher == null) {
            GenericDelegator delegator = getDelegator();

            if (delegator == null) {
                Debug.logError("[ContextFilter.init] ERROR: delegator not defined.", module);
                return null;
            }
            Collection readers = null;
            String readerFiles = config.getServletContext().getInitParameter("serviceReaderUrls");

            if (readerFiles != null) {
                readers = new ArrayList();
                List readerList = StringUtil.split(readerFiles, ";");
                Iterator i = readerList.iterator();

                while (i.hasNext()) {
                    try {
                        String name = (String) i.next();
                        URL readerURL = config.getServletContext().getResource(name);

                        if (readerURL != null)
                            readers.add(readerURL);
                    } catch (NullPointerException npe) {
                        Debug.logInfo(npe, "[ContextFilter.init] ERROR: Null pointer exception thrown.", module);
                    } catch (MalformedURLException e) {
                        Debug.logError(e, "[ContextFilter.init] ERROR: cannot get URL from String.", module);
                    }
                }
            }
            // get the unique name of this dispatcher
            String dispatcherName = config.getServletContext().getInitParameter("localDispatcherName");

            if (dispatcherName == null)
                Debug.logError("No localDispatcherName specified in the web.xml file", module);
            dispatcher = new WebAppDispatcher(dispatcherName, delegator, readers);
            config.getServletContext().setAttribute("dispatcher", dispatcher);
            if (dispatcher == null)
                Debug.logError("[ContextFilter.init] ERROR: dispatcher could not be initialized.", module);
        }
        return dispatcher;
    }

    protected GenericDelegator getDelegator() {
        GenericDelegator delegator = (GenericDelegator) config.getServletContext().getAttribute("delegator");
        if (delegator == null) {
            String delegatorName = config.getServletContext().getInitParameter("entityDelegatorName");

            if (delegatorName == null || delegatorName.length() <= 0) {
                delegatorName = "default";
            }
            if (Debug.infoOn()) Debug.logInfo("[ContextFilter.init] Getting Entity Engine Delegator with delegator name " + delegatorName, module);
            delegator = GenericDelegator.getGenericDelegator(delegatorName);
            config.getServletContext().setAttribute("delegator", delegator);
            if (delegator == null) {
                Debug.logError("[ContextFilter.init] ERROR: delegator factory returned null for delegatorName \"" + delegatorName + "\"", module);
            }
        }
        return delegator;
    }

    protected Security getSecurity() {
        Security security = (Security) config.getServletContext().getAttribute("security");
        if (security == null) {
            GenericDelegator delegator = (GenericDelegator) config.getServletContext().getAttribute("delegator");

            if (delegator != null) {
                try {
                    security = SecurityFactory.getInstance(delegator);
                } catch (SecurityConfigurationException e) {
                    Debug.logError(e, "[ServiceDispatcher.init] : No instance of security imeplemtation found.", module);
                }
            }
            config.getServletContext().setAttribute("security", security);
            if (security == null) {
                Debug.logError("[ContextFilter.init] ERROR: security create failed.", module);
            }
        }
        return security;
    }

    protected void putAllInitParametersInAttributes() {
        Enumeration initParamEnum = config.getServletContext().getInitParameterNames();
        while (initParamEnum.hasMoreElements()) {
            String initParamName = (String) initParamEnum.nextElement();
            String initParamValue = config.getServletContext().getInitParameter(initParamName);
            if (Debug.infoOn()) Debug.logInfo("Adding web.xml context-param to application attribute with name [" + initParamName + "] and value [" + initParamValue + "]", module);
            config.getServletContext().setAttribute(initParamName, initParamValue);
        }
    }

    protected String getServerId() {
        String serverId = (String) config.getServletContext().getAttribute("_serverId");
        if (serverId == null) {
            serverId = config.getServletContext().getInitParameter("ofbizServerName");
            config.getServletContext().setAttribute("_serverId", serverId);
        }
        return serverId;
    }

    protected boolean getContainers() throws ServletException {
        try {
            ContainerLoader.loadContainers(CONTAINER_CONFIG, null);
        } catch (StartupException e) {
            Debug.logError(e, module);
            throw new ServletException("Unable to load containers; cannot start ContextFilter");
        }
        return true;
    }
}
