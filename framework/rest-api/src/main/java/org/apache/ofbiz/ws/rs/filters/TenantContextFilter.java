/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ofbiz.ws.rs.filters;

import java.io.IOException;

import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.DelegatorFactory;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.EntityUtil;
import org.apache.ofbiz.security.Security;
import org.apache.ofbiz.security.SecurityConfigurationException;
import org.apache.ofbiz.security.SecurityFactory;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceContainer;
import org.apache.ofbiz.webapp.WebAppUtil;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Resolves the REST API tenant from a configured TenantDomainName entry.
 * Unlike ContextFilter, this filter does not mutate servlet-wide delegator,
 * dispatcher, or security attributes, which are shared across requests.
 *
 * <p>Tenant selection intentionally does not accept query parameters or
 * client-supplied tenant headers. Unmapped domains retain the existing
 * base-delegator behavior; registered invalid domains are rejected.</p>
 */
public class TenantContextFilter implements Filter {

    private jakarta.servlet.ServletContext servletContext;

    @Override
    public void init(FilterConfig filterConfig) {
        servletContext = filterConfig.getServletContext();
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (!isMultiTenantEnabled()) {
            chain.doFilter(request, response);
            return;
        }
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        Delegator baseDelegator = (Delegator) servletContext.getAttribute("delegator");
        if (baseDelegator == null) {
            httpResponse.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            return;
        }

        try {
            String tenantId = findTenantId(baseDelegator, httpRequest.getServerName());
            if (tenantId == null) {
                chain.doFilter(request, response);
                return;
            }
            if (!isValidTenant(baseDelegator, tenantId)) {
                httpResponse.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            Delegator tenantDelegator = getTenantDelegator(baseDelegator, tenantId);
            if (tenantDelegator == null) {
                httpResponse.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                return;
            }
            LocalDispatcher dispatcher = getTenantDispatcher(tenantDelegator);
            Security security = getTenantSecurity(tenantDelegator);
            if (dispatcher == null || security == null) {
                httpResponse.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                return;
            }

            request.setAttribute("delegator", tenantDelegator);
            request.setAttribute("dispatcher", dispatcher);
            request.setAttribute("security", security);
            request.setAttribute("userTenantId", tenantId);
            chain.doFilter(request, response);
        } catch (GenericEntityException | SecurityConfigurationException e) {
            // Never fall back to the base delegator on a tenant resolution error.
            throw new ServletException("REST API tenant context initialization failed", e);
        }
    }

    /** Extracted for isolated unit tests; production uses the OFBiz global multitenancy setting. */
    protected boolean isMultiTenantEnabled() {
        return EntityUtil.isMultiTenantEnabled();
    }

    /** Only domains registered in the base TenantDomainName table select a tenant. */
    protected String findTenantId(Delegator baseDelegator, String serverName) throws GenericEntityException {
        if (serverName == null || serverName.isBlank()) {
            return null;
        }
        GenericValue domain = EntityQuery.use(baseDelegator)
                .from("TenantDomainName")
                .where("domainName", serverName)
                .queryOne();
        return domain == null ? null : domain.getString("tenantId");
    }

    /**
     * Checks whether a domain-mapped tenant is currently valid.
     * <p>Testing subclasses may override this hook. Production overrides must
     * still reject unknown or disabled tenants before a request is dispatched.</p>
     * @param baseDelegator base delegator containing the tenant registry
     * @param tenantId tenant ID resolved from a registered domain
     * @return true only if the tenant is valid
     */
    protected boolean isValidTenant(Delegator baseDelegator, String tenantId) {
        return WebAppUtil.isValidTenantId(baseDelegator, tenantId);
    }

    /**
     * Obtains the delegator belonging to the validated tenant.
     * <p>Overrides must not substitute another tenant's delegator or the base
     * delegator, since this would violate request-level tenant isolation.</p>
     * @param baseDelegator base delegator used to form the tenant delegator name
     * @param tenantId validated tenant ID
     * @return the tenant-specific delegator
     */
    protected Delegator getTenantDelegator(Delegator baseDelegator, String tenantId) {
        return DelegatorFactory.getDelegator(baseDelegator.getDelegatorBaseName() + "#" + tenantId);
    }

    /**
     * Obtains the dispatcher bound to the current request's tenant delegator.
     * <p>Overrides must preserve the delegator-dispatcher association; sharing
     * a dispatcher across tenants could execute services in the wrong tenant.</p>
     * @param tenantDelegator delegator for the resolved tenant
     * @return a dispatcher bound to that delegator
     */
    protected LocalDispatcher getTenantDispatcher(Delegator tenantDelegator) {
        return ServiceContainer.getLocalDispatcher(
                servletContext.getInitParameter("localDispatcherName"), tenantDelegator);
    }

    /**
     * Obtains tenant-specific security services for the request.
     * <p>Overrides must preserve the tenant binding and must not fall back to
     * permissions from another tenant or from the base delegator.</p>
     * @param tenantDelegator delegator for the resolved tenant
     * @return security services bound to the tenant
     * @throws SecurityConfigurationException if tenant security cannot be initialized
     */
    protected Security getTenantSecurity(Delegator tenantDelegator) throws SecurityConfigurationException {
        return SecurityFactory.getInstance(tenantDelegator);
    }
}
