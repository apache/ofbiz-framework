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

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilHttp;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.common.UrlServletHelper;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;

public class SeoContentUrlFilter implements Filter {
    private static final String MODULE = SeoContentUrlFilter.class.getName();
    protected static final String DEFAULT_LOCALE_STRING = null;
    protected static final String REDIRECT_URL = null;
    private FilterConfig config;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        this.config = filterConfig;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        Delegator delegator = (Delegator) httpRequest.getSession().getServletContext().getAttribute("delegator");

        String pathInfo = UtilHttp.getFullRequestUrl(httpRequest);
        if (UtilValidate.isNotEmpty(pathInfo) && pathInfo.endsWith("-content")) {
            String alternativeUrl = pathInfo.substring(pathInfo.lastIndexOf('/') + 1);
            GenericValue urlContent = null;
            try {
                urlContent = EntityQuery.use(delegator).from("ContentAssocDataResourceViewTo")
                    .where("caContentAssocTypeId", "ALTERNATIVE_URL",
                            "caThruDate", null,
                            "drObjectInfo", alternativeUrl)
                        .orderBy("-createdDate")
                        .cache()
                        .queryFirst();
            } catch (Exception e) {
                Debug.logWarning(e, MODULE);
            }
            if (urlContent != null) {
                StringBuilder urlBuilder = new StringBuilder();
                if (UtilValidate.isNotEmpty(SeoControlServlet.getControlServlet())) {
                    urlBuilder.append("/" + SeoControlServlet.getControlServlet());
                }
                urlBuilder.append("/" + config.getInitParameter("viewRequest") + "?contentId=" + urlContent.getString("contentIdStart"));

                // Set view query parameters
                UrlServletHelper.setViewQueryParameters(request, urlBuilder);
                if (Debug.infoOn()) Debug.logInfo("[Filtered request]: " + pathInfo + " (" + urlBuilder + ")", MODULE);
                RequestDispatcher dispatch = request.getRequestDispatcher(urlBuilder.toString());
                dispatch.forward(request, response);
                return;
            }

            // Check path alias
            UrlServletHelper.checkPathAlias(request, httpResponse, delegator, pathInfo);
        }
        // we're done checking; continue on
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {

    }
}
