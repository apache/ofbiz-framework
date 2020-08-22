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
package org.apache.ofbiz.security;

import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.core.MultivaluedHashMap;

import org.apache.commons.lang.StringUtils;
import org.apache.cxf.jaxrs.model.URITemplate;
import org.apache.ofbiz.base.component.ComponentConfig;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.webapp.control.ConfigXMLReader;
import org.apache.ofbiz.webapp.control.RequestHandler;
import org.apache.ofbiz.webapp.control.RequestHandlerException;
import org.apache.ofbiz.webapp.control.RequestHandlerExceptionAllowExternalRequests;
import org.apache.ofbiz.webapp.control.WebAppConfigurationException;

public final class CsrfUtil {

    private static final String MODULE = CsrfUtil.class.getName();
    private static String tokenNameNonAjax = UtilProperties.getPropertyValue("security", "csrf.tokenName.nonAjax",
            "csrf");
    private static ICsrfDefenseStrategy strategy;
    private static String strategyCanonicalName;
    private static int cacheSize = (int) Long
            .parseLong(UtilProperties.getPropertyValue("security", "csrf.cache.size", "5000"));
    private static LinkedHashMap<String, Map<String, Map<String, String>>> csrfTokenCache =
            new LinkedHashMap<String, Map<String, Map<String, String>>>() {
        private static final long serialVersionUID = 1L;

        protected boolean removeEldestEntry(Map.Entry<String, Map<String, Map<String, String>>> eldest) {
            return size() > cacheSize; // TODO use also csrf.cache.size here?
        }
    };

    private CsrfUtil() {
    }

    static {
        try {
            String className = UtilProperties.getPropertyValue("security", "csrf.defense.strategy",
                    NoCsrfDefenseStrategy.class.getCanonicalName());
            Class<?> c = Class.forName(className);
            strategyCanonicalName = c.getCanonicalName();
            setStrategy((ICsrfDefenseStrategy) c.newInstance());
        } catch (Exception e) {
            Debug.logError(e, MODULE);
            setStrategy(new NoCsrfDefenseStrategy());
        }
    }

    public static Map<String, String> getTokenMap(HttpServletRequest request, String targetContextPath) {

        HttpSession session = request.getSession();
        GenericValue userLogin = (GenericValue) session.getAttribute("userLogin");
        String partyId = null;
        if (userLogin != null && userLogin.get("partyId") != null) {
            partyId = userLogin.getString("partyId");
        }

        Map<String, String> tokenMap = null;
        if (UtilValidate.isNotEmpty(partyId)) {
            Map<String, Map<String, String>> partyTokenMap = csrfTokenCache.get(partyId);
            if (partyTokenMap == null) {
                partyTokenMap = new HashMap<>();
                csrfTokenCache.put(partyId, partyTokenMap);
            }

            tokenMap = partyTokenMap.get(targetContextPath);
            if (tokenMap == null) {
                tokenMap = new LinkedHashMap<String, String>() {
                    private static final long serialVersionUID = 1L;

                    protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
                        return size() > cacheSize;
                    }
                };
                partyTokenMap.put(targetContextPath, tokenMap);
            }
        } else {
            tokenMap = UtilGenerics.cast(session.getAttribute("CSRF-Token"));
            if (tokenMap == null) {
                tokenMap = new LinkedHashMap<String, String>() {
                    private static final long serialVersionUID = 1L;

                    protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
                        return size() > cacheSize;
                    }
                };
                session.setAttribute("CSRF-Token", tokenMap);
            }
        }
        return tokenMap;
    }

    private static String generateToken() {
        return getStrategy().generateToken();
    }

    /**
     * Reduce number of subfolder from request uri, if needed, before using it to generate CSRF token.
     * @param requestUri
     * @return
     */
    static String getRequestUriWithSubFolderLimit(String requestUri) {
        int limit = CsrfUtil.getStrategy().maxSubFolderInRequestUrlForTokenMapLookup(requestUri);
        if (limit < 1) {
            return requestUri;
        }
        while (StringUtils.countMatches(requestUri, "/") + 1 > limit) {
            requestUri = requestUri.substring(0, requestUri.lastIndexOf("/"));
        }
        return requestUri;
    }

    static String getRequestUriFromPath(String pathOrRequestUri) {
        String requestUri = pathOrRequestUri;
        // remove any query string
        if (requestUri.contains("?")) {
            // e.g. "/viewprofile?partyId=Company" to "/viewprofile"
            requestUri = requestUri.substring(0, requestUri.indexOf("?"));
        }
        String controlServletPart = "/control/"; // TODO remove with OFBIZ-11229
        if (requestUri.contains(controlServletPart)) {
            // e.g. "/partymgr/control/viewprofile" to "viewprofile"
            requestUri = requestUri.substring(requestUri.indexOf(controlServletPart) + controlServletPart.length());
        }
        if (requestUri.startsWith("/")) {
            // e.g. "/viewprofile" to "viewprofile"
            requestUri = requestUri.substring(1);
        }
        if (requestUri.contains("#")) {
            // e.g. "view/entityref_main#org.apache.ofbiz.accounting.budget" to "view/entityref_main"
            requestUri = requestUri.substring(0, requestUri.indexOf("#"));
        }
        return requestUri;
    }

    /**
     * Generate CSRF token for non-ajax request if required and add it as key to token map in session When token map
     * size limit is reached, the eldest entry will be deleted each time a new entry is added. Token only generated for
     * up to 3 subfolders in the path so 'entity/find/Budget/0001' and 'entity/find/Budget/0002' should share the same
     * CSRF token.
     * @param request
     * @param pathOrRequestUri
     * @return csrf token
     */
    public static String generateTokenForNonAjax(HttpServletRequest request, String pathOrRequestUri) {
        if (UtilValidate.isEmpty(pathOrRequestUri) || pathOrRequestUri.startsWith("javascript")
                || pathOrRequestUri.startsWith("#")) {
            return "";
        }

        if (pathOrRequestUri.contains("&#x2f;")) {
            pathOrRequestUri = pathOrRequestUri.replaceAll("&#x2f;", "/");
        }

        String requestUri = getRequestUriWithSubFolderLimit(getRequestUriFromPath(pathOrRequestUri));

        Map<String, String> tokenMap = null;

        ConfigXMLReader.RequestMap requestMap = null;
        // TODO when OFBIZ-11354 will be done this will need to be removed even if it should be OK as is
        if (pathOrRequestUri.contains("/control/")) {
            tokenMap = getTokenMap(request, "/" + RequestHandler.getRequestUri(pathOrRequestUri));
            requestMap = findRequestMap(pathOrRequestUri);
        } else {
            tokenMap = getTokenMap(request, request.getContextPath());
            Map<String, ConfigXMLReader.RequestMap> requestMapMap = UtilGenerics
                    .cast(request.getAttribute("requestMapMap"));
            requestMap = findRequestMap(requestMapMap, pathOrRequestUri);
        }
        if (requestMap == null) {
            if (!"org.apache.ofbiz.security.NoCsrfDefenseStrategy".equals(strategyCanonicalName)) {
                Debug.logWarning("Cannot find the corresponding request map for path: " + pathOrRequestUri, MODULE);
            }
        }
        String tokenValue = "";
        if (requestMap != null && requestMap.isSecurityCsrfToken()) {
            if (tokenMap.containsKey(requestUri)) {
                tokenValue = tokenMap.get(requestUri);
            } else {
                tokenValue = generateToken();
                tokenMap.put(requestUri, tokenValue);
            }
        }
        return tokenValue;
    }

    static ConfigXMLReader.RequestMap findRequestMap(String urlWithControlPath) {

        String requestUri = getRequestUriFromPath(urlWithControlPath);

        List<ComponentConfig.WebappInfo> webappInfos = ComponentConfig.getAllWebappResourceInfos().stream()
                .filter(line -> line.getContextRoot().contains(RequestHandler.getRequestUri(urlWithControlPath)))
                .collect(Collectors.toList());

        ConfigXMLReader.RequestMap requestMap = null;
        if (UtilValidate.isNotEmpty(webappInfos)) {
            try {
                if (StringUtils.countMatches(requestUri, "/") == 1) {
                    requestMap = ConfigXMLReader.getControllerConfig(webappInfos.get(0)).getRequestMapMap()
                            .get(requestUri.substring(0, requestUri.indexOf("/")));
                } else {
                    requestMap = ConfigXMLReader.getControllerConfig(webappInfos.get(0)).getRequestMapMap()
                            .get(requestUri);
                }
            } catch (WebAppConfigurationException | MalformedURLException e) {
                Debug.logError(e, MODULE);
            }
        }
        return requestMap;
    }

    static ConfigXMLReader.RequestMap findRequestMap(Map<String, ConfigXMLReader.RequestMap> requestMapMap,
            String urlWithoutControlPath) {
        String path = urlWithoutControlPath;
        if (urlWithoutControlPath.startsWith("/")) {
            path = urlWithoutControlPath.substring(1);
        }
        int charPos = path.indexOf("?");
        if (charPos != -1) {
            path = path.substring(0, charPos);
        }
        MultivaluedHashMap<String, String> vars = new MultivaluedHashMap<>();
        for (Map.Entry<String, ConfigXMLReader.RequestMap> entry : requestMapMap.entrySet()) {
            URITemplate uriTemplate = URITemplate.createExactTemplate(entry.getKey());
            // Check if current path the URI template exactly.
            if (uriTemplate.match(path, vars) && vars.getFirst(URITemplate.FINAL_MATCH_GROUP).equals("/")) {
                return entry.getValue();
            }
        }
        // the path could be request uri with orderride
        if (path.contains("/")) {
            return requestMapMap.get(path.substring(0, path.indexOf("/")));
        }
        return null;
    }

    /**
     * generate csrf token for AJAX and add it as value to token cache
     * @param request
     * @return csrf token
     */
    public static String generateTokenForAjax(HttpServletRequest request) {
        HttpSession session = request.getSession();
        String tokenValue = (String) session.getAttribute("X-CSRF-Token");
        if (tokenValue == null) {
            tokenValue = generateToken();
            session.setAttribute("X-CSRF-Token", tokenValue);
        }
        return tokenValue;
    }

    /**
     * get csrf token for AJAX
     * @param session
     * @return csrf token
     */
    public static String getTokenForAjax(HttpSession session) {
        return (String) session.getAttribute("X-CSRF-Token");
    }

    public static String addOrUpdateTokenInUrl(String link, String csrfToken) {
        if (link.contains(CsrfUtil.getTokenNameNonAjax())) {
            return link.replaceFirst("\\b" + CsrfUtil.getTokenNameNonAjax() + "=.*?(&|$)",
                    CsrfUtil.getTokenNameNonAjax() + "=" + csrfToken + "$1");
        } else if (!"".equals(csrfToken)) {
            if (link.contains("?")) {
                return link + "&" + CsrfUtil.getTokenNameNonAjax() + "=" + csrfToken;
            } else {
                return link + "?" + CsrfUtil.getTokenNameNonAjax() + "=" + csrfToken;
            }
        }
        return link;
    }

    public static String addOrUpdateTokenInQueryString(String link, String csrfToken) {
        if (UtilValidate.isNotEmpty(link)) {
            if (link.contains(CsrfUtil.getTokenNameNonAjax())) {
                return link.replaceFirst("\\b" + CsrfUtil.getTokenNameNonAjax() + "=.*?(&|$)",
                        CsrfUtil.getTokenNameNonAjax() + "=" + csrfToken + "$1");
            } else {
                if (UtilValidate.isNotEmpty(csrfToken)) {
                    return link + "&" + CsrfUtil.getTokenNameNonAjax() + "=" + csrfToken;
                } else {
                    return link;
                }
            }
        } else {
            return CsrfUtil.getTokenNameNonAjax() + "=" + csrfToken;
        }
    }

    public static void checkToken(HttpServletRequest request, String path)
            throws RequestHandlerException, RequestHandlerExceptionAllowExternalRequests {
        String csrfUtilPath = path;
        if (path.startsWith("/")) {
            csrfUtilPath = path.substring(1);
        }
        if ("XMLHttpRequest".equals(request.getHeader("X-Requested-With")) && !"GET".equals(request.getMethod())) {
            String csrfToken = request.getHeader("X-CSRF-Token");
            HttpSession session = request.getSession();
            if ((UtilValidate.isEmpty(csrfToken) || !csrfToken.equals(CsrfUtil.getTokenForAjax(session)))
                    && !"/SetTimeZoneFromBrowser".equals(request.getPathInfo())) { // TODO maybe this can be improved...
                throw new RequestHandlerException(
                        "Invalid or missing CSRF token for AJAX call to path '" + request.getPathInfo() + "'");
            }
        } else {
            Map<String, String> tokenMap = CsrfUtil.getTokenMap(request, request.getContextPath());
            String csrfToken = request.getParameter(CsrfUtil.getTokenNameNonAjax());
            String limitPath = getRequestUriWithSubFolderLimit(csrfUtilPath);
            if (UtilValidate.isNotEmpty(csrfToken) && tokenMap.containsKey(limitPath)
                    && csrfToken.equals(tokenMap.get(limitPath))) {
                if (!CsrfUtil.getStrategy().keepTokenAfterUse(csrfUtilPath, request.getMethod())) {
                    tokenMap.remove(limitPath);
                }
            } else {
                CsrfUtil.getStrategy().invalidTokenResponse(csrfUtilPath, request);
            }
        }
    }

    public static void cleanupTokenMap(HttpSession session) {
        GenericValue userLogin = (GenericValue) session.getAttribute("userLogin");
        String partyId = null;
        if (userLogin != null && userLogin.get("partyId") != null) {
            partyId = userLogin.getString("partyId");
            Map<String, Map<String, String>> partyTokenMap = csrfTokenCache.get(partyId);
            if (partyTokenMap != null) {
                String contextPath = session.getServletContext().getContextPath();
                partyTokenMap.remove(contextPath);
                if (partyTokenMap.isEmpty()) {
                    csrfTokenCache.remove(partyId);
                }
            }
        }
    }

    /**
     * @return the tokenNameNonAjax
     */
    public static String getTokenNameNonAjax() {
        return tokenNameNonAjax;
    }

    /**
     * @param tokenNameNonAjax the tokenNameNonAjax to set
     */
    public static void setTokenNameNonAjax(String tokenNameNonAjax) {
        CsrfUtil.tokenNameNonAjax = tokenNameNonAjax;
    }

    /**
     * @return the strategy
     */
    public static ICsrfDefenseStrategy getStrategy() {
        return strategy;
    }

    /**
     * @param strategy the strategy to set
     */
    public static void setStrategy(ICsrfDefenseStrategy strategy) {
        CsrfUtil.strategy = strategy;
    }
}
