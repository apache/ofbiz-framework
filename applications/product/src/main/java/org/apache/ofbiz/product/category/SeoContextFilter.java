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
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.StringUtil;
import org.apache.ofbiz.base.util.UtilHttp;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.webapp.control.ConfigXMLReader;
import org.apache.ofbiz.webapp.control.ConfigXMLReader.ControllerConfig;
import org.apache.ofbiz.webapp.control.ControlFilter;
import org.apache.ofbiz.webapp.control.WebAppConfigurationException;
import org.apache.oro.text.regex.Pattern;
import org.apache.oro.text.regex.Perl5Matcher;

/**
 * SeoContextFilter - Restricts access to raw files and configures servlet objects.
 */
public class SeoContextFilter implements Filter {

    public static final String module = SeoContextFilter.class.getName();

    protected Set<String> webServlets = new HashSet<>();
    private FilterConfig config;
    private String allowedPaths = "";
    private String redirectPath = "";
    private String errorCode = "";
    private List<String> allowedPathList = new ArrayList<>();

    public void init(FilterConfig config) throws ServletException {
        this.config = config;
        allowedPaths = config.getInitParameter("allowedPaths");
        redirectPath = config.getInitParameter("redirectPath");
        errorCode = config.getInitParameter("errorCode");
        if (UtilValidate.isNotEmpty(allowedPaths)) {
            allowedPathList = StringUtil.split(allowedPaths, ":");
        }

        Map<String, ? extends ServletRegistration> servletRegistrations = config.getServletContext().getServletRegistrations();
        for (Entry<String, ? extends ServletRegistration> entry : servletRegistrations.entrySet()) {
            Collection<String> servlets = entry.getValue().getMappings();
            for (String servlet : servlets) {
                if (servlet.endsWith("/*")) {
                    servlet = servlet.substring(0, servlet.length() - 2);
                    if (UtilValidate.isNotEmpty(servlet) && !webServlets.contains(servlet)) {
                        webServlets.add(servlet);
                    }
                }
            }
        }
    }

    /**
     * @see javax.servlet.Filter#doFilter(javax.servlet.ServletRequest, javax.servlet.ServletResponse, javax.servlet.FilterChain)
     */
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String uri = httpRequest.getRequestURI();
        boolean forwarded = forwardUri(httpResponse, uri);
        if (forwarded) {
            return;
        }

        URL controllerConfigURL = ConfigXMLReader.getControllerConfigURL(config.getServletContext());
        ControllerConfig controllerConfig = null;
        Map<String, ConfigXMLReader.RequestMap> requestMaps = null;
        try {
            controllerConfig = ConfigXMLReader.getControllerConfig(controllerConfigURL);
            requestMaps = controllerConfig.getRequestMapMap();
        } catch (WebAppConfigurationException e) {
            Debug.logError(e, "Exception thrown while parsing controller.xml file: ", module);
            throw new ServletException(e);
        }
        Set<String> uris = requestMaps.keySet();

        // test to see if we have come through the control servlet already, if not do the processing
        String requestPath = null;
        String contextUri = null;
        if (httpRequest.getAttribute(ControlFilter.FORWARDED_FROM_SERVLET) == null) {
            requestPath = httpRequest.getServletPath();
            if (requestPath == null) requestPath = "";
            if (requestPath.lastIndexOf('/') > 0) {
                if (requestPath.indexOf('/') == 0) {
                    requestPath = '/' + requestPath.substring(1, requestPath.indexOf('/', 1));
                } else {
                    requestPath = requestPath.substring(1, requestPath.indexOf('/'));
                }
            }

            String requestInfo = httpRequest.getServletPath();
            if (requestInfo == null) requestInfo = "";
            if (requestInfo.lastIndexOf('/') >= 0) {
                requestInfo = requestInfo.substring(0, requestInfo.lastIndexOf('/')) + "/*";
            }

            StringBuilder contextUriBuffer = new StringBuilder();
            if (httpRequest.getContextPath() != null) {
                contextUriBuffer.append(httpRequest.getContextPath());
            }
            if (httpRequest.getServletPath() != null) {
                contextUriBuffer.append(httpRequest.getServletPath());
            }
            if (httpRequest.getPathInfo() != null) {
                contextUriBuffer.append(httpRequest.getPathInfo());
            }
            contextUri = contextUriBuffer.toString();

            List<String> pathItemList = StringUtil.split(httpRequest.getPathInfo(), "/");
            String viewName = "";
            if (pathItemList != null) {
                viewName = pathItemList.get(0);
            }
            
            String requestUri = UtilHttp.getRequestUriFromTarget(httpRequest.getRequestURI());

            // check to make sure the requested url is allowed
            if (!allowedPathList.contains(requestPath) && !allowedPathList.contains(requestInfo) && !allowedPathList.contains(httpRequest.getServletPath())
                    && !allowedPathList.contains(requestUri) && !allowedPathList.contains("/" + viewName)
                    && (UtilValidate.isEmpty(requestPath) && UtilValidate.isEmpty(httpRequest.getServletPath()) && !uris.contains(viewName))) {
                String filterMessage = "[Filtered request]: " + contextUri;

                if (redirectPath == null) {
                    if (UtilValidate.isEmpty(viewName)) {
                        // redirect without any url change in browser
                        RequestDispatcher rd = request.getRequestDispatcher(SeoControlServlet.getDefaultPage());
                        rd.forward(request, response);
                    } else {
                        int error = 404;
                        if (UtilValidate.isNotEmpty(errorCode)) {
                            try {
                                error = Integer.parseInt(errorCode);
                            } catch (NumberFormatException nfe) {
                                Debug.logWarning(nfe, "Error code specified would not parse to Integer : " + errorCode, module);
                            }
                        }
                        filterMessage = filterMessage + " (" + error + ")";
                        httpResponse.sendError(error, contextUri);
                        request.setAttribute("filterRequestUriError", contextUri);
                    }
                } else {
                    filterMessage = filterMessage + " (" + redirectPath + ")";
                    if (!redirectPath.toLowerCase(Locale.getDefault()).startsWith("http")) {
                        redirectPath = httpRequest.getContextPath() + redirectPath;
                    }
                    // httpResponse.sendRedirect(redirectPath);
                    if ("".equals(uri) || "/".equals(uri)) {
                        // redirect without any url change in browser
                        RequestDispatcher rd = request.getRequestDispatcher(redirectPath);
                        rd.forward(request, response);
                    } else {
                        // redirect with url change in browser
                        httpResponse.setStatus(SeoConfigUtil.getDefaultResponseCode());
                        httpResponse.setHeader("Location", redirectPath);
                    }
                }
                Debug.logWarning(filterMessage, module);
                return;
            } else if ((allowedPathList.contains(requestPath) || allowedPathList.contains(requestInfo) || allowedPathList.contains(httpRequest.getServletPath())
                    || allowedPathList.contains(requestUri) || allowedPathList.contains("/" + viewName))
                    && !webServlets.contains(httpRequest.getServletPath())) {
                request.setAttribute(SeoControlServlet.REQUEST_IN_ALLOW_LIST, Boolean.TRUE);
            }
        }


        // we're done checking; continue on
        chain.doFilter(httpRequest, httpResponse);
    }

    @Override
    public void destroy() {

    }

    /**
     * Forward a uri according to forward pattern regular expressions. Note: this is developed for Filter usage.
     * 
     * @param uri String to reverse transform
     * @return String
     */
    protected static boolean forwardUri(HttpServletResponse response, String uri) {
        Perl5Matcher matcher = new Perl5Matcher();
        boolean foundMatch = false;
        Integer responseCodeInt = null;

        if (SeoConfigUtil.checkUseUrlRegexp() && SeoConfigUtil.getSeoPatterns() != null && SeoConfigUtil.getForwardReplacements() != null) {
            Iterator<String> keys = SeoConfigUtil.getSeoPatterns().keySet().iterator();
            while (keys.hasNext()) {
                String key = keys.next();
                Pattern pattern = SeoConfigUtil.getSeoPatterns().get(key);
                String replacement = SeoConfigUtil.getForwardReplacements().get(key);
                if (matcher.matches(uri, pattern)) {
                    for (int i = matcher.getMatch().groups(); i > 0; i--) {
                        replacement = replacement.replaceAll("\\$" + i, matcher.getMatch().group(i));
                    }
                    uri = replacement;
                    responseCodeInt = SeoConfigUtil.getForwardResponseCodes().get(key);
                    foundMatch = true;
                    // be careful, we don't break after finding a match
                }
            }
        }

        if (foundMatch) {
            if (responseCodeInt == null) {
                response.setStatus(SeoConfigUtil.getDefaultResponseCode());
            } else {
                response.setStatus(responseCodeInt);
            }
            response.setHeader("Location", uri);
        } else {
            Debug.logInfo("Can NOT forward this url: " + uri, module);
        }
        return foundMatch;
    }
}
