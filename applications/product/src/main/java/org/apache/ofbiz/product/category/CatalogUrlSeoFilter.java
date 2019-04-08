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
package org.apache.ofbiz.product.category;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.common.UrlServletHelper;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.product.category.ftl.CatalogUrlSeoTransform;

public class CatalogUrlSeoFilter extends CatalogUrlFilter {

    public final static String module = CatalogUrlSeoFilter.class.getName();

    protected static String defaultLocaleString = null;
    protected static String redirectUrl = null;

    /**
     * @see javax.servlet.Filter#doFilter(javax.servlet.ServletRequest, javax.servlet.ServletResponse, javax.servlet.FilterChain)
     */
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        Delegator delegator = (Delegator) httpRequest.getSession().getServletContext().getAttribute("delegator");

        // Get ServletContext
        ServletContext servletContext = config.getServletContext();

        // Set request attribute and session
        UrlServletHelper.setRequestAttributes(request, delegator, servletContext);

        // set initial parameters
        String initDefaultLocalesString = config.getInitParameter("defaultLocaleString");
        String initRedirectUrl = config.getInitParameter("redirectUrl");
        defaultLocaleString = UtilValidate.isNotEmpty(initDefaultLocalesString) ? initDefaultLocalesString : "";
        redirectUrl = UtilValidate.isNotEmpty(initRedirectUrl) ? initRedirectUrl : "";

        // set the ServletContext in the request for future use
        httpRequest.setAttribute("servletContext", config.getServletContext());
        if (CatalogUrlSeoTransform.forwardUri(httpRequest, httpResponse, delegator, ControlServlet.controlServlet)) {
            return;
        }
        super.doFilter(httpRequest, httpResponse, chain);
    }

}
