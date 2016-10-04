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

import static org.apache.ofbiz.base.util.UtilGenerics.checkMap;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Map;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.StringUtil;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilHttp;
import org.apache.ofbiz.base.util.UtilObject;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.DelegatorFactory;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.EntityUtil;
import org.apache.ofbiz.security.Security;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.webapp.WebAppUtil;
import org.apache.ofbiz.webapp.website.WebSiteWorker;

/**
 * ContextFilter - Configures objects for OFBiz applications
 */
public class ContextFilter implements Filter {

    private static final String module = ContextFilter.class.getName();

    protected FilterConfig config = null;

    // default charset used to decode requests body data if no encoding is specified in the request
    private String defaultCharacterEncoding;
    private boolean isMultitenant;

    /**
     * @see javax.servlet.Filter#init(javax.servlet.FilterConfig)
     */
    public void init(FilterConfig config) throws ServletException {
        this.config = config;

        // puts all init-parameters in ServletContext attributes for easier parametrization without code changes
        this.putAllInitParametersInAttributes();

        defaultCharacterEncoding = config.getServletContext().getInitParameter("charset");
        if (UtilValidate.isEmpty(defaultCharacterEncoding)) {
            defaultCharacterEncoding = "UTF-8";
        }
        // check the serverId
        getServerId();
        // initialize the delegator
        WebAppUtil.getDelegator(config.getServletContext());
        // initialize security
        WebAppUtil.getSecurity(config.getServletContext());
        // initialize the services dispatcher
        WebAppUtil.getDispatcher(config.getServletContext());

        // check if multi tenant is enabled
        isMultitenant = EntityUtil.isMultiTenantEnabled();

        // this will speed up the initial sessionId generation
        new java.security.SecureRandom().nextLong();
    }

    /**
     * @see javax.servlet.Filter#doFilter(javax.servlet.ServletRequest, javax.servlet.ServletResponse, javax.servlet.FilterChain)
     */
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // ----- Servlet Object Setup -----

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

        if (request.getCharacterEncoding() == null) {
            request.setCharacterEncoding(defaultCharacterEncoding);
        }

        WebAppUtil.setAttributesFromRequestBody(request);

        request.setAttribute("delegator", config.getServletContext().getAttribute("delegator"));
        request.setAttribute("dispatcher", config.getServletContext().getAttribute("dispatcher"));
        request.setAttribute("security", config.getServletContext().getAttribute("security"));

        if (isMultitenant) {
            // get tenant delegator by domain name
            String serverName = httpRequest.getServerName();
            try {
                // if tenant was specified, replace delegator with the new per-tenant delegator and set tenantId to session attribute
                Delegator delegator = WebAppUtil.getDelegator(config.getServletContext());

                //Use base delegator for fetching data from entity of entityGroup org.apache.ofbiz.tenant
                Delegator baseDelegator = DelegatorFactory.getDelegator(delegator.getDelegatorBaseName());
                GenericValue tenantDomainName = EntityQuery.use(baseDelegator).from("TenantDomainName").where("domainName", serverName).queryOne();
                String tenantId = null;
                if(UtilValidate.isNotEmpty(tenantDomainName)) {
                    tenantId = tenantDomainName.getString("tenantId");
                }
                
                if(UtilValidate.isEmpty(tenantId)) {
                    tenantId = (String) httpRequest.getAttribute("userTenantId");
                }
                if(UtilValidate.isEmpty(tenantId)) {
                    tenantId = httpRequest.getParameter("userTenantId");
                }
                if (UtilValidate.isNotEmpty(tenantId)) {
                    // if the request path is a root mount then redirect to the initial path
                    if ("".equals(httpRequest.getContextPath()) && "".equals(httpRequest.getServletPath())) {
                        GenericValue tenant = EntityQuery.use(baseDelegator).from("Tenant").where("tenantId", tenantId).queryOne();
                        String initialPath = tenant.getString("initialPath");
                        if (UtilValidate.isNotEmpty(initialPath) && !"/".equals(initialPath)) {
                            ((HttpServletResponse)response).sendRedirect(initialPath);
                            return;
                        }
                    }

                    // make that tenant active, setup a new delegator and a new dispatcher
                    String tenantDelegatorName = delegator.getDelegatorBaseName() + "#" + tenantId;
                    httpRequest.getSession().setAttribute("delegatorName", tenantDelegatorName);

                    // after this line the delegator is replaced with the new per-tenant delegator
                    delegator = DelegatorFactory.getDelegator(tenantDelegatorName);
                    config.getServletContext().setAttribute("delegator", delegator);

                    // clear web context objects
                    config.getServletContext().setAttribute("security", null);
                    config.getServletContext().setAttribute("dispatcher", null);

                    // initialize security
                    Security security = WebAppUtil.getSecurity(config.getServletContext());
                    // initialize the services dispatcher
                    LocalDispatcher dispatcher = WebAppUtil.getDispatcher(config.getServletContext());

                    // set web context objects
                    request.setAttribute("delegator", delegator);
                    request.setAttribute("dispatcher", dispatcher);
                    request.setAttribute("security", security);
                    
                    request.setAttribute("userTenantId", tenantId);
                }

                // NOTE DEJ20101130: do NOT always put the delegator name in the user's session because the user may 
                // have logged in and specified a tenant, and even if no Tenant record with a matching domainName field 
                // is found this will change the user's delegator back to the base one instead of the one for the 
                // tenant specified on login 
                // httpRequest.getSession().setAttribute("delegatorName", delegator.getDelegatorName());
            } catch (GenericEntityException e) {
                Debug.logWarning(e, "Unable to get Tenant", module);
            }
        }

        // we're done checking; continue on
        chain.doFilter(request, httpResponse);
    }

    /**
     * @see javax.servlet.Filter#destroy()
     */
    public void destroy() {
        WebAppUtil.getDispatcher(config.getServletContext()).deregister();
        config = null;
    }

    private void putAllInitParametersInAttributes() {
        Enumeration<String> initParamEnum = UtilGenerics.cast(config.getServletContext().getInitParameterNames());
        while (initParamEnum.hasMoreElements()) {
            String initParamName = initParamEnum.nextElement();
            String initParamValue = config.getServletContext().getInitParameter(initParamName);
            if (Debug.verboseOn()) Debug.logVerbose("Adding web.xml context-param to application attribute with name [" + initParamName + "] and value [" + initParamValue + "]", module);
            config.getServletContext().setAttribute(initParamName, initParamValue);
        }
    }

    private String getServerId() {
        String serverId = (String) config.getServletContext().getAttribute("_serverId");
        if (serverId == null) {
            serverId = config.getServletContext().getInitParameter("ofbizServerName");
            config.getServletContext().setAttribute("_serverId", serverId);
        }
        return serverId;
    }
}
