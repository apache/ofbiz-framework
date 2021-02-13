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
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.validator.routines.UrlValidator;
import org.apache.logging.log4j.ThreadContext;
import org.apache.ofbiz.base.util.Debug;

/**
 * A Filter used to specify an allowlist of allowed paths to the OFBiz application.
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
 *   - the filter skips the check against the allowlist of allowed paths if a request attribute
 *     with name _FORWARDED_FROM_SERVLET_ is present; this attribute is typically set by the ControlServlet to indicate
 *     that the request path is safe and should not be checked again
 */
@SuppressWarnings("serial")
public class ControlFilter extends HttpFilter {
    public static final String FORWARDED_FROM_SERVLET = "_FORWARDED_FROM_SERVLET_";
    public static final int DEFAULT_HTTP_ERROR_CODE = 403;
    private static final String MODULE = ControlFilter.class.getName();

    /** The path used for redirection. */
    private String redirectPath;
    /** True when all traffic must be redirected to {@code redirectPath}. */
    private boolean redirectAll;
    /** True when redirectPath is an absolute URI. */
    private boolean redirectPathIsUrl;
    /** The error code used when current path is not allowed and {@code redirectPath} is null. */
    private int errorCode;
    /** The list of all path prefixes that are allowed. */
    private Set<String> allowedPaths;

    @Override
    public void init(FilterConfig conf) throws ServletException {
        redirectPath = conf.getInitParameter("redirectPath");
        redirectPathIsUrl = UrlValidator.getInstance().isValid(redirectPath);
        errorCode = readErrorCode(conf.getInitParameter("errorCode"));
        allowedPaths = readAllowedPaths(conf.getInitParameter("allowedPaths"));
        redirectAll = (redirectPath != null)
                && BooleanUtils.toBoolean(conf.getInitParameter("forceRedirectAll"));

        // Ensure that the path used for local redirections is allowed.
        if (redirectPath != null && !redirectPathIsUrl) {
            allowedPaths.add(redirectPath);
        }
    }

    /**
     * Converts {@code code} string to an integer.  If conversion fails, Return
     * {@code DEFAULT_HTTP_ERROR_STATUS} instead.
     * @param code an arbitrary string which can be {@code null}
     * @return the integer matching {@code code}
     */
    private static int readErrorCode(String code) {
        try {
            return (code == null) ? DEFAULT_HTTP_ERROR_CODE : Integer.parseInt(code);
        } catch (NumberFormatException err) {
            Debug.logWarning(err, "Error code specified would not parse to Integer: " + code, MODULE);
            Debug.logWarning(err, "The default error code will be used: " + DEFAULT_HTTP_ERROR_CODE, MODULE);
            return DEFAULT_HTTP_ERROR_CODE;
        }
    }

    /**
     * Splits the paths defined by {@code paths}.
     * @param paths a string which can be either {@code null} or a list of
     * paths separated by ':'.
     * @return a set of string
     */
    private static Set<String> readAllowedPaths(String paths) {
        return (paths == null) ? Collections.emptySet()
                               : Arrays.stream(paths.split(":")).collect(Collectors.toSet());
    }

    /**
     * Makes allowed paths pass through while redirecting the others to a fix location.
     */
    @Override
    public void doFilter(HttpServletRequest req, HttpServletResponse resp, FilterChain chain)
            throws IOException, ServletException {
        String context = req.getContextPath();
        HttpSession session = req.getSession();

        // Check if we are told to redirect everything.
        if (redirectAll) {
            // little trick here so we don't loop on ourselves
            if (session.getAttribute("_FORCE_REDIRECT_") == null) {
                session.setAttribute("_FORCE_REDIRECT_", "true");
                Debug.logWarning("Redirecting user to: " + redirectPath, MODULE);
                redirect(resp, context);
            } else {
                session.removeAttribute("_FORCE_REDIRECT_");
                chain.doFilter(req, resp);
            }
        } else if (req.getAttribute(FORWARDED_FROM_SERVLET) == null
                && !allowedPaths.isEmpty()) {
            // Get the request URI without the webapp mount point.
            String uriWithContext = req.getRequestURI();
            String uri = uriWithContext.substring(context.length());

            // Check if the requested URI is allowed.
            if (allowedPaths.stream().anyMatch(uri::startsWith)) {
                try {
                    // support OFBizDynamicThresholdFilter in log4j2.xml
                    ThreadContext.put("uri", uri);

                    chain.doFilter(req, resp);
                } finally {
                    ThreadContext.remove("uri");
                }
            } else {
                if (redirectPath == null) {
                    resp.sendError(errorCode, uriWithContext);
                } else {
                    redirect(resp, context);
                }
                if (Debug.infoOn()) {
                    Debug.logInfo("[Filtered request]: " + uriWithContext + " --> "
                                    + (redirectPath == null ? errorCode : redirectPath), MODULE);
                }
            }
        }
    }

    /**
     * Sends an HTTP response redirecting to {@code redirectPath}.
     * @param resp The response to send
     * @param contextPath the prefix to add to the redirection when
     * {@code redirectPath} is a relative URI.
     * @throws IOException when redirection has not been properly sent.
     */
    private void redirect(HttpServletResponse resp, String contextPath) throws IOException {
        resp.sendRedirect(redirectPathIsUrl ? redirectPath : (contextPath + redirectPath));
    }
}
