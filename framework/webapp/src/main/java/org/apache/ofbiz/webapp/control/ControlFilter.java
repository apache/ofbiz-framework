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

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.ofbiz.base.util.Debug;

/*
 * A Filter used to specify a whitelist of allowed paths to the OFBiz application.
 * Requests that do not match any of the paths listed in allowedPaths are redirected to redirectPath, or an error code
 * is returned (the error code can be set in errorCode, the default value is 403).
 * If forceRedirectAll is set to Y then allowedPaths is ignored and all requests are redirected to redirectPath; note
 * that forceRedirectAll is ignored if redirectPath is not set.
 *
 * Init parameters:
 *   - forceRedirectAll: when set to Y, and redirectPath is set, then redirects all traffic to redirectPath
 *   - allowedPaths: a colon separated list of URL or URI that are allowed;
 *     non matching request paths are redirected, or an error code is returned,
 *     according to the setup of redirectPath and errorCode
 *   - redirectPath: if the path requested is not in the allowedPaths, or forceRedirectAll is set to Y,
 *     specifies the the path to which the request is redirected to;
 *   - errorCode: the error code set in the response if the path requested is not in the allowedPaths
 *     and redirectPath is not set; defaults to 403
 *
 * Interaction with the context:
 *   - for its internal logic (to avoid an infinite loop of redirections when forceRedirectAll is set) the filter sets
 *     a session parameter (_FORCE_REDIRECT_=true) before the first redirection; the parameter is removed during the
 *     second pass before the request is forwarded to the next filter in the chain
 *   - the filter skips the check against the whitelist of allowed paths if a request attribute
 *     with name _FORWARDED_FROM_SERVLET_ is present; this attribute is typically set by the ControlServlet to indicate
 *     that the request path is safe and should not be checked again
 */


public class ControlFilter implements Filter {
    public static final String FORWARDED_FROM_SERVLET = "_FORWARDED_FROM_SERVLET_";

    private static final String module = ControlFilter.class.getName();
    private boolean redirectAll;
    private boolean redirectPathIsUrl;
    private String redirectPath;
    protected int errorCode;
    private Set<String> allowedPaths = new HashSet<>();

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        redirectPath = filterConfig.getInitParameter("redirectPath");
        redirectPathIsUrl = (redirectPath != null && redirectPath.toLowerCase().startsWith("http"));
        String redirectAllString = filterConfig.getInitParameter("forceRedirectAll");
        redirectAll = (redirectPath != null && redirectAllString != null && "Y".equalsIgnoreCase(redirectAllString));
        String errorCodeString = filterConfig.getInitParameter("errorCode");
        errorCode = 403;
        if (errorCodeString != null) {
            try {
                errorCode = Integer.parseInt(errorCodeString);
            } catch (NumberFormatException nfe) {
                Debug.logWarning(nfe, "Error code specified would not parse to Integer: " + errorCodeString, module);
                Debug.logWarning(nfe, "The default error code will be used: " + errorCode, module);
            }
        }
        String allowedPathsString = filterConfig.getInitParameter("allowedPaths");
        if (allowedPathsString != null) {
            String[] result = allowedPathsString.split(":");
            for (int x = 0; x < result.length; x++) {
                allowedPaths.add(result[x]);
            }
            // if an URI is specified in the redirectPath parameter, it is added to the allowed list
            if (redirectPath != null && !redirectPathIsUrl) {
                allowedPaths.add(redirectPath);
            }

        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // check if we are told to redirect everything
        if (redirectAll) {
            // little trick here so we don't loop on ourselves
            if (httpRequest.getSession().getAttribute("_FORCE_REDIRECT_") == null) {
                httpRequest.getSession().setAttribute("_FORCE_REDIRECT_", "true");
                Debug.logWarning("Redirecting user to: " + redirectPath, module);
                if (redirectPathIsUrl) {
                    httpResponse.sendRedirect(redirectPath);
                } else {
                    httpResponse.sendRedirect(httpRequest.getContextPath() + redirectPath);
                }
                return;
            } else {
                httpRequest.getSession().removeAttribute("_FORCE_REDIRECT_");
                chain.doFilter(httpRequest, httpResponse);
                return;
            }
        }

        if (httpRequest.getAttribute(FORWARDED_FROM_SERVLET) == null && !allowedPaths.isEmpty()) {
            // check to make sure the requested url is allowed
            // get the request URI without the webapp mount point
            String requestUri = httpRequest.getRequestURI().substring(httpRequest.getContextPath().length());
            int offset = requestUri.indexOf("/", 1);
            if (offset == -1) {
                offset = requestUri.length();
            }
            while (!allowedPaths.contains(requestUri.substring(0, offset))) {
                offset = requestUri.indexOf("/", offset + 1);
                if (offset == -1) {
                    if (allowedPaths.contains(requestUri)) {
                        break;
                    }
                    // path not allowed
                    if (redirectPath == null) {
                        httpResponse.sendError(errorCode, httpRequest.getRequestURI());
                    } else {
                        if (redirectPathIsUrl) {
                            httpResponse.sendRedirect(redirectPath);
                        } else {
                            httpResponse.sendRedirect(httpRequest.getContextPath() + redirectPath);
                        }
                    }
                    if (Debug.infoOn()) {
                        Debug.logInfo("[Filtered request]: " + httpRequest.getRequestURI() + " --> " + (redirectPath == null? errorCode: redirectPath), module);
                    }
                    return;
                }
            }
            chain.doFilter(request, httpResponse);
        }
    }

    @Override
    public void destroy() {

    }
}
