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
package org.apache.ofbiz.cmssite.multisite;

import java.io.IOException;
import java.util.Locale;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilHttp;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.DelegatorFactory;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.order.shoppingcart.CartItemModifyException;
import org.apache.ofbiz.order.shoppingcart.ShoppingCart;
import org.apache.ofbiz.security.Security;
import org.apache.ofbiz.security.SecurityConfigurationException;
import org.apache.ofbiz.security.SecurityFactory;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.webapp.control.RequestHandler;
import org.apache.ofbiz.webapp.stats.VisitHandler;

// Used to filter website on the basis of hosted pathAlias.
public class WebSiteFilter implements Filter {

    public static final String MODULE = WebSiteFilter.class.getName();

    protected FilterConfig m_config = null;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        m_config = filterConfig;
        m_config.getServletContext().setAttribute("MULTI_SITE_ENABLED", true);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        HttpSession session = httpRequest.getSession();

        String webSiteId = (String) m_config.getServletContext().getAttribute("webSiteId");
        String pathInfo = httpRequest.getPathInfo();
        // get the WebSite id segment, cheat here and use existing logic
        String webSiteAlias = RequestHandler.getRequestUri(pathInfo);
        Delegator delegator = (Delegator) httpRequest.getSession().getServletContext().getAttribute("delegator");
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");

        setWebContextObjects(httpRequest, httpResponse, delegator, dispatcher);

        GenericValue webSite = null;
        try {
            if (UtilValidate.isNotEmpty(webSiteAlias) && webSite == null) {
                webSite = EntityQuery.use(delegator).from("WebSite").where("hostedPathAlias", webSiteAlias).cache().queryFirst();
            }
            if (UtilValidate.isEmpty(webSite)) {
                webSite = EntityQuery.use(delegator).from("WebSite").where("isDefault", "Y").cache().queryFirst();
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
        }
        if (webSite != null) {
            webSiteId = webSite.getString("webSiteId");
            GenericValue productStore = null;
            try {
                productStore = webSite.getRelatedOne("ProductStore", false);
            } catch (GenericEntityException e) {
                Debug.logError(e, MODULE);
            }

            String newLocale = request.getParameter("newLocale");
            if (productStore != null && newLocale == null && session.getAttribute("locale") == null) {
                newLocale = productStore.getString("defaultLocaleString");
            } else if (newLocale == null && session.getAttribute("locale") != null) {
                newLocale = session.getAttribute("locale").toString();
            }

            if (newLocale == null)
                newLocale = UtilHttp.getLocale(httpRequest).toString();
            // If the webSiteId has changed then invalidate the existing session
            if (!webSiteId.equals(session.getAttribute("webSiteId"))) {
                ShoppingCart cart = (ShoppingCart) session.getAttribute("shoppingCart");
                if (cart != null && !(webSite.getString("productStoreId").equals(cart.getProductStoreId())) ) {
                    // clearing cart items from previous store 
                    cart.clear();
                    // Put product Store for this webSite in cart
                    cart.setProductStoreId(webSite.getString("productStoreId"));
                }
                if (cart != null && productStore != null) {
                    Locale localeObj = UtilMisc.parseLocale(newLocale);
                    cart.setLocale(localeObj);
                    try {
                        cart.setCurrency(dispatcher, productStore.getString("defaultCurrencyUomId"));
                    } catch (CartItemModifyException e) {
                        Debug.logError(e, MODULE);
                    }
                }
                session.removeAttribute("webSiteId");
                session.setAttribute("webSiteId", webSiteId);
                session.setAttribute("displayMaintenancePage", webSite.getString("displayMaintenancePage"));
            }
            request.setAttribute("webSiteId", webSiteId);
            session.setAttribute("displayMaintenancePage", webSite.getString("displayMaintenancePage"));
            if(UtilValidate.isEmpty(webSite.getString("hostedPathAlias"))) {
                request.setAttribute("removePathAlias", false);
            } else {
                request.setAttribute("removePathAlias", true);
            }
            httpRequest = new MultiSiteRequestWrapper(httpRequest);
            UtilHttp.setLocale(httpRequest, newLocale);
        }
        if (webSiteId != null) {
            request.setAttribute("webSiteId", webSiteId);
        }
        chain.doFilter(httpRequest, response);
    }

    private static void setWebContextObjects(HttpServletRequest request, HttpServletResponse response, Delegator delegator, LocalDispatcher dispatcher) {
        HttpSession session = request.getSession();
        Security security = null;
        try {
            security = SecurityFactory.getInstance(delegator);
        } catch (SecurityConfigurationException e) {
            Debug.logError(e, MODULE);
        }
        request.setAttribute("delegator", delegator);
        request.setAttribute("dispatcher", dispatcher);
        request.setAttribute("security", security);

        session.setAttribute("delegatorName", delegator.getDelegatorName());
        session.setAttribute("delegator", delegator);
        session.setAttribute("dispatcher", dispatcher);
        session.setAttribute("security", security);
        session.setAttribute("_WEBAPP_NAME_", UtilHttp.getApplicationName(request));
        
        // get rid of the visit info since it was pointing to the previous database, and get a new one
        session.removeAttribute("visitor");
        session.removeAttribute("visit");
        VisitHandler.getVisitor(request, response);
        VisitHandler.getVisit(session);
    }
    @Override
    public void destroy() {
    }
}