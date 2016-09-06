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
package org.apache.ofbiz.solr.webapp;

import static org.apache.ofbiz.base.util.UtilGenerics.checkMap;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.solr.common.SolrException;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.NodeConfig;
import org.apache.solr.servlet.SolrDispatchFilter;
import org.apache.ofbiz.base.conversion.ConversionException;
import org.apache.ofbiz.base.conversion.JSONConverters.MapToJSON;
import org.apache.ofbiz.base.lang.JSON;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.StringUtil;
import org.apache.ofbiz.base.util.UtilHttp;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilObject;
import org.apache.ofbiz.base.util.UtilTimer;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.security.Security;
import org.apache.ofbiz.webapp.WebAppUtil;
import org.apache.ofbiz.webapp.control.ContextFilter;
import org.apache.ofbiz.webapp.control.LoginWorker;
import org.apache.ofbiz.webapp.website.WebSiteWorker;

/**
 * OFBizSolrContextFilter - Restricts access to solr urls.
 */
public class OFBizSolrContextFilter extends SolrDispatchFilter {

    public static final String module = OFBizSolrContextFilter.class.getName();

    protected ContextFilter contextFilter = null;
    protected FilterConfig config = null;
    
    /**
     * @see javax.servlet.Filter#init(javax.servlet.FilterConfig)
     */
    public void init(FilterConfig config) throws ServletException {
        super.init(config);
        this.config = config;
        contextFilter = new ContextFilter();
        contextFilter.init(config);
    }

    /**
     * @see javax.servlet.Filter#doFilter(javax.servlet.ServletRequest, javax.servlet.ServletResponse, javax.servlet.FilterChain)
     */
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        // set the ServletContext in the request for future use
        httpRequest.setAttribute("servletContext", config.getServletContext());

        // set the webSiteId in the session
        if (UtilValidate.isEmpty(httpRequest.getSession().getAttribute("webSiteId"))){
            httpRequest.getSession().setAttribute("webSiteId", WebSiteWorker.getWebSiteId(httpRequest));
        }

        // set the filesystem path of context root.
        httpRequest.setAttribute("_CONTEXT_ROOT_", config.getServletContext().getRealPath("/"));

        // set the server root url
        httpRequest.setAttribute("_SERVER_ROOT_URL_", UtilHttp.getServerRootUrl(httpRequest));

        // request attributes from redirect call
        String reqAttrMapHex = (String) httpRequest.getSession().getAttribute("_REQ_ATTR_MAP_");
        if (UtilValidate.isNotEmpty(reqAttrMapHex)) {
            byte[] reqAttrMapBytes = StringUtil.fromHexString(reqAttrMapHex);
            Map<String, Object> reqAttrMap = checkMap(UtilObject.getObject(reqAttrMapBytes), String.class, Object.class);
            if (reqAttrMap != null) {
                for (Map.Entry<String, Object> entry: reqAttrMap.entrySet()) {
                    httpRequest.setAttribute(entry.getKey(), entry.getValue());
                }
            }
            httpRequest.getSession().removeAttribute("_REQ_ATTR_MAP_");
        }

        // ----- Context Security -----
        // check if we are disabled
        String disableSecurity = config.getInitParameter("disableContextSecurity");
        if (disableSecurity != null && "Y".equalsIgnoreCase(disableSecurity)) {
            chain.doFilter(httpRequest, httpResponse);
            return;
        }

        // check if we are told to redirect everthing
        String redirectAllTo = config.getInitParameter("forceRedirectAll");
        if (UtilValidate.isNotEmpty(redirectAllTo)) {
            // little trick here so we don't loop on ourself
            if (httpRequest.getSession().getAttribute("_FORCE_REDIRECT_") == null) {
                httpRequest.getSession().setAttribute("_FORCE_REDIRECT_", "true");
                Debug.logWarning("Redirecting user to: " + redirectAllTo, module);

                if (!redirectAllTo.toLowerCase().startsWith("http")) {
                    redirectAllTo = httpRequest.getContextPath() + redirectAllTo;
                }
                httpResponse.sendRedirect(redirectAllTo);
                return;
            } else {
                httpRequest.getSession().removeAttribute("_FORCE_REDIRECT_");
                chain.doFilter(httpRequest, httpResponse);
                return;
            }
        }

        String servletPath = httpRequest.getServletPath();
        if (UtilValidate.isNotEmpty(servletPath) && servletPath.equals("/control")) {
        	contextFilter.doFilter(httpRequest, httpResponse, chain);
            // we're done checking; continue on
            chain.doFilter(request, response);
        } else {
            // check if the request is from an authorized user
            if (UtilValidate.isNotEmpty(servletPath) && (servletPath.startsWith("/admin/") || servletPath.endsWith("/update") 
                    || servletPath.endsWith("/update/json") || servletPath.endsWith("/update/csv") || servletPath.endsWith("/update/extract")
                    || servletPath.endsWith("/replication") || servletPath.endsWith("/file") || servletPath.endsWith("/file/"))) {
                HttpSession session = httpRequest.getSession();
                GenericValue userLogin = (GenericValue) session.getAttribute("userLogin");
                Security security = (Security) request.getAttribute("security");
                if (security == null) {
                    security = (Security) httpRequest.getServletContext().getAttribute("security");
                    if (security != null) {
                        request.setAttribute("security", security);
                    }
                }
                if (security == null) {
                    security = WebAppUtil.getSecurity(httpRequest.getServletContext());
                    if (security != null) {
                        request.setAttribute("security", security);
                    }
                }
                if (servletPath.startsWith("/admin/") && (UtilValidate.isEmpty(userLogin) || !LoginWorker.hasBasePermission(userLogin, httpRequest))) {
                    response.setContentType("application/json");
                    MapToJSON mapToJson = new MapToJSON();
                    JSON json;
                    OutputStream os = null;
                    try {
                        json = mapToJson.convert(UtilMisc.toMap("ofbizLogin", (Object) "true"));
                        os = response.getOutputStream();
                        os.write(json.toString().getBytes());
                        os.flush();
                        String message = "";
                        if (UtilValidate.isEmpty(userLogin)) {
                            message = "To manage Solr in OFBiz, you have to login first and have the permission to do so.";
                        } else {
                            message = "To manage Solr in OFBiz, you have to the permission to do so.";
                        }
                        Debug.logInfo("[" + httpRequest.getRequestURI().substring(1) + "(Domain:" + request.getScheme() + "://" + request.getServerName() + ")] Request error: " + message, module);
                    } catch (ConversionException e) {
                        Debug.logError("Error while converting Solr ofbizLogin map to JSON.", module);
                    } finally {
                        if (os != null) {
                            os.close();
                        }
                    }
                    return;
                } else if (servletPath.endsWith("/update") || servletPath.endsWith("/update/json") || servletPath.endsWith("/update/csv") || servletPath.endsWith("/update/extract")) {
                    // NOTE: the update requests are defined in an index's solrconfig.xml
                    // get the Solr index name from the request
                    if (UtilValidate.isEmpty(userLogin) || !LoginWorker.hasBasePermission(userLogin, httpRequest)) {
                        httpResponse.setContentType("application/json");
                        MapToJSON mapToJson = new MapToJSON();
                        Map<String, Object> responseHeader = new HashMap<String, Object>();
                        JSON json;
                        String message = "";
                        OutputStream os = null;
                        try {
                            os = httpResponse.getOutputStream();
                            if (UtilValidate.isEmpty(userLogin)) {
                                httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                                responseHeader.put("status", HttpServletResponse.SC_UNAUTHORIZED);
                                message = "To update a Solr index in OFBiz, you have to login first and have the permission to do so.";
                                responseHeader.put("message", message);
                            } else {
                                httpResponse.setStatus(HttpServletResponse.SC_FORBIDDEN);
                                responseHeader.put("status", HttpServletResponse.SC_FORBIDDEN);
                                message = "To update a Solr index in OFBiz, you have to have the permission to do so.";
                                responseHeader.put("message", message);
                            }
                            json = mapToJson.convert(UtilMisc.toMap("responseHeader", (Object) responseHeader));
                            os.write(json.toString().getBytes());
                            os.flush();
                            Debug.logInfo("[" + httpRequest.getRequestURI().substring(1) + "(Domain:" + request.getScheme() + "://" + request.getServerName() + ")] Request error: " + message, module);
                        } catch (ConversionException e) {
                            Debug.logError("Error while converting responseHeader map to JSON.", module);
                        } finally {
                            if (os != null) {
                                os.close();
                            }
                        }
                        return;
                    }
                } else if (servletPath.endsWith("/replication")) {
                    // get the Solr index name from the request
                    if (UtilValidate.isEmpty(userLogin) || !LoginWorker.hasBasePermission(userLogin, httpRequest)) {
                        httpResponse.setContentType("application/json");
                        MapToJSON mapToJson = new MapToJSON();
                        Map<String, Object> responseHeader = new HashMap<String, Object>();
                        JSON json;
                        String message = "";
                        OutputStream os = null;
                        try {
                            os = httpResponse.getOutputStream();
                            if (UtilValidate.isEmpty(userLogin)) {
                                httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                                responseHeader.put("status", HttpServletResponse.SC_UNAUTHORIZED);
                                message = "To enable/disable replication of a Solr index in OFBiz, you have to login first and have the permission to do so.";
                                responseHeader.put("message", message);
                            } else {
                                httpResponse.setStatus(HttpServletResponse.SC_FORBIDDEN);
                                responseHeader.put("status", HttpServletResponse.SC_FORBIDDEN);
                                message = "To enable/disable replication of a Solr index in OFBiz, you have to have the permission to do so.";
                                responseHeader.put("message", message);
                            }
                            json = mapToJson.convert(UtilMisc.toMap("responseHeader", (Object) responseHeader));
                            os.write(json.toString().getBytes());
                            os.flush();
                            Debug.logInfo("[" + httpRequest.getRequestURI().substring(1) + "(Domain:" + request.getScheme() + "://" + request.getServerName() + ")] Request error: " + message, module);
                        } catch (ConversionException e) {
                            Debug.logError("Error while converting responseHeader map to JSON.", module);
                        } finally {
                            if (os != null) {
                                os.close();
                            }
                        }
                        return;
                    }
                } else if (servletPath.endsWith("/file") || servletPath.endsWith("/file/")) {
                    // get the Solr index name from the request
                    if (UtilValidate.isEmpty(userLogin) || !LoginWorker.hasBasePermission(userLogin, httpRequest)) {
                        httpResponse.setContentType("application/json");
                        MapToJSON mapToJson = new MapToJSON();
                        Map<String, Object> responseHeader = new HashMap<String, Object>();
                        JSON json;
                        String message = "";
                        OutputStream os = null;
                        try {
                            os = httpResponse.getOutputStream();
                            if (UtilValidate.isEmpty(userLogin)) {
                                httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                                responseHeader.put("status", HttpServletResponse.SC_UNAUTHORIZED);
                                message = "To view files of a Solr index in OFBiz, you have to login first and have the permission to do so.";
                                responseHeader.put("message", message);
                            } else {
                                httpResponse.setStatus(HttpServletResponse.SC_FORBIDDEN);
                                responseHeader.put("status", HttpServletResponse.SC_FORBIDDEN);
                                message = "To view files of a Solr index in OFBiz, you have to have the permission to do so.";
                                responseHeader.put("message", message);
                            }
                            json = mapToJson.convert(UtilMisc.toMap("responseHeader", (Object) responseHeader));
                            os.write(json.toString().getBytes());
                            os.flush();
                            Debug.logInfo("[" + httpRequest.getRequestURI().substring(1) + "(Domain:" + request.getScheme() + "://" + request.getServerName() + ")] Request error: " + message, module);
                        } catch (ConversionException e) {
                            Debug.logError("Error while converting responseHeader map to JSON.", module);
                        } finally {
                            if (os != null) {
                                os.close();
                            }
                        }
                        return;
                    }
                }
            }
            
            String charset = request.getCharacterEncoding();
            String rname = null;
            if (httpRequest.getRequestURI() != null) {
                rname = httpRequest.getRequestURI().substring(1);
            }
            if (rname != null && (rname.endsWith(".css") || rname.endsWith(".js") || rname.endsWith(".ico") || rname.endsWith(".html") || rname.endsWith(".png") || rname.endsWith(".jpg") || rname.endsWith(".gif"))) {
                rname = null;
            }
            UtilTimer timer = null;
            if (Debug.timingOn() && rname != null) {
                timer = new UtilTimer();
                timer.setLog(true);
                timer.timerString("[" + rname + "(Domain:" + request.getScheme() + "://" + request.getServerName() + ")] Request Begun, encoding=[" + charset + "]", module);
            }
            // NOTE: there's a chain.doFilter in SolrDispatchFilter's doFilter
            super.doFilter(request, response, chain);
            if (Debug.timingOn() && rname != null) timer.timerString("[" + rname + "(Domain:" + request.getScheme() + "://" + request.getServerName() + ")] Request Done", module);
        }
    }

    /**
     * @see javax.servlet.Filter#destroy()
     */
    public void destroy() {
        super.destroy();
        contextFilter.destroy();
        config = null;
    }

    /**
     * Override this to change CoreContainer initialization
     * @return a CoreContainer to hold this server's cores
     */
    protected CoreContainer createCoreContainer(String solrHome, Properties extraProperties) {
        NodeConfig nodeConfig = null;
        try {
            nodeConfig = loadNodeConfig(solrHome, extraProperties);
        } catch (SolrException e) {
            nodeConfig = loadNodeConfig("specialpurpose/solr/home", extraProperties);
        }
        cores = new CoreContainer(nodeConfig, extraProperties, true);
        cores.load();
        return cores;
    }
}

