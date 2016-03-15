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
package org.ofbiz.base.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.FileNameMap;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Currency;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TimeZone;

import javax.net.ssl.SSLContext;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.apache.oro.text.regex.MalformedPatternException;
import org.apache.oro.text.regex.Pattern;
import org.apache.oro.text.regex.PatternMatcher;
import org.apache.oro.text.regex.Perl5Matcher;

import com.ibm.icu.util.Calendar;

/**
 * HttpUtil - Misc HTTP Utility Functions
 */
public class UtilHttp {

    public static final String module = UtilHttp.class.getName();

    public static final String MULTI_ROW_DELIMITER = "_o_";
    public static final String ROW_SUBMIT_PREFIX = "_rowSubmit_o_";
    public static final String COMPOSITE_DELIMITER = "_c_";
    public static final int MULTI_ROW_DELIMITER_LENGTH = MULTI_ROW_DELIMITER.length();
    public static final int ROW_SUBMIT_PREFIX_LENGTH = ROW_SUBMIT_PREFIX.length();
    public static final int COMPOSITE_DELIMITER_LENGTH = COMPOSITE_DELIMITER.length();

    public static final String SESSION_KEY_TIMEZONE = "timeZone";

    /**
     * Create a combined map from servlet context, session, attributes and parameters
     * @return The resulting Map
     */
    public static Map<String, Object> getCombinedMap(HttpServletRequest request) {
        return getCombinedMap(request, null);
    }

    /**
     * Create a combined map from servlet context, session, attributes and parameters
     * -- this method will only use the skip names for session and servlet context attributes
     * @return The resulting Map
     */
    public static Map<String, Object> getCombinedMap(HttpServletRequest request, Set<? extends String> namesToSkip) {
        Map<String, Object> combinedMap = new HashMap<String, Object>();
        combinedMap.putAll(getParameterMap(request));                   // parameters override nothing
        combinedMap.putAll(getServletContextMap(request, namesToSkip)); // bottom level application attributes
        combinedMap.putAll(getSessionMap(request, namesToSkip));        // session overrides application
        combinedMap.putAll(getAttributeMap(request));                   // attributes trump them all

        return combinedMap;
    }

    /**
     * Create a map from a HttpServletRequest (parameters) object
     * @return The resulting Map
     */
    public static Map<String, Object> getParameterMap(HttpServletRequest request) {
        return getParameterMap(request, null, null);
    }

    public static Map<String, Object> getParameterMap(HttpServletRequest request, Set<? extends String> nameSet) {
        return getParameterMap(request, nameSet, null);
    }

    /**
     * Create a map from a HttpServletRequest (parameters) object
     * @param onlyIncludeOrSkip If true only include, if false skip, the named parameters in the nameSet. If this is null and nameSet is not null, default to skip.
     * @return The resulting Map
     */
    public static Map<String, Object> getParameterMap(HttpServletRequest request, Set<? extends String> nameSet, Boolean onlyIncludeOrSkip) {
        boolean onlyIncludeOrSkipPrim = onlyIncludeOrSkip == null ? true : onlyIncludeOrSkip.booleanValue();
        Map<String, Object> paramMap = new HashMap<String, Object>();

        // add all the actual HTTP request parameters
        Enumeration<String> e = UtilGenerics.cast(request.getParameterNames());
        while (e.hasMoreElements()) {
            String name = e.nextElement();
            if (nameSet != null && (onlyIncludeOrSkipPrim ^ nameSet.contains(name))) {
                continue;
            }

            Object value = null;
            String[] paramArr = request.getParameterValues(name);
            if (paramArr != null) {
                if (paramArr.length > 1) {
                    value = Arrays.asList(paramArr);
                } else {
                    value = paramArr[0];
                    // does the same thing basically, nothing better about it as far as I can see: value = request.getParameter(name);
                }
            }
            paramMap.put(name, value);
        }

        paramMap.putAll(getPathInfoOnlyParameterMap(request, nameSet, onlyIncludeOrSkip));

        if (paramMap.size() == 0) {
            // nothing found in the parameters; maybe we read the stream instead
            Map<String, Object> multiPartMap = UtilGenerics.checkMap(request.getAttribute("multiPartMap"));
            if (UtilValidate.isNotEmpty(multiPartMap)) {
                paramMap.putAll(multiPartMap);
            }
        }

        if (Debug.verboseOn()) {
            Debug.logVerbose("Made Request Parameter Map with [" + paramMap.size() + "] Entries", module);
            Debug.logVerbose("Request Parameter Map Entries: " + System.getProperty("line.separator") + UtilMisc.printMap(paramMap), module);
        }

        return canonicalizeParameterMap(paramMap);
    }

    public static Map<String, Object> getQueryStringOnlyParameterMap(String queryString) {
        Map<String, Object> paramMap = new HashMap<String, Object>();
        if (UtilValidate.isNotEmpty(queryString)) {
            StringTokenizer queryTokens = new StringTokenizer(queryString, "&");
            while (queryTokens.hasMoreTokens()) {
                String token = queryTokens.nextToken();
                if (token.startsWith("amp;")) {
                    // this is most likely a split value that had an &amp; in it, so don't consider this a name; note that some old code just stripped the "amp;" and went with it
                    //token = token.substring(4);
                    continue;
                }
                int equalsIndex = token.indexOf("=");
                String name = token;
                if (equalsIndex > 0) {
                    name = token.substring(0, equalsIndex);
                    paramMap.put(name, token.substring(equalsIndex + 1));
                }
            }
        }
        return canonicalizeParameterMap(paramMap);
    }

    public static Map<String, Object> getPathInfoOnlyParameterMap(HttpServletRequest request, Set<? extends String> nameSet, Boolean onlyIncludeOrSkip) {
        return getPathInfoOnlyParameterMap(request.getPathInfo(), nameSet, onlyIncludeOrSkip);
    }

    public static Map<String, Object> getPathInfoOnlyParameterMap(String pathInfoStr, Set<? extends String> nameSet, Boolean onlyIncludeOrSkip) {
        boolean onlyIncludeOrSkipPrim = onlyIncludeOrSkip == null ? true : onlyIncludeOrSkip.booleanValue();
        Map<String, Object> paramMap = new HashMap<String, Object>();

        // now add in all path info parameters /~name1=value1/~name2=value2/
        // note that if a parameter with a given name already exists it will be put into a list with all values

        if (UtilValidate.isNotEmpty(pathInfoStr)) {
            // make sure string ends with a trailing '/' so we get all values
            if (!pathInfoStr.endsWith("/")) pathInfoStr += "/";

            int current = pathInfoStr.indexOf('/');
            int last = current;
            while ((current = pathInfoStr.indexOf('/', last + 1)) != -1) {
                String element = pathInfoStr.substring(last + 1, current);
                last = current;
                if (element.charAt(0) == '~' && element.indexOf('=') > -1) {
                    String name = element.substring(1, element.indexOf('='));
                    if (nameSet != null && (onlyIncludeOrSkipPrim ^ nameSet.contains(name))) {
                        continue;
                    }

                    String value = element.substring(element.indexOf('=') + 1);
                    Object curValue = paramMap.get(name);
                    if (curValue != null) {
                        List<String> paramList = null;
                        if (curValue instanceof List<?>) {
                            paramList = UtilGenerics.checkList(curValue);
                            paramList.add(value);
                        } else {
                            String paramString = (String) curValue;
                            paramList = new LinkedList<String>();
                            paramList.add(paramString);
                            paramList.add(value);
                        }
                        paramMap.put(name, paramList);
                    } else {
                        paramMap.put(name, value);
                    }
                }
            }
        }

        return canonicalizeParameterMap(paramMap);
    }

    public static Map<String, Object> getUrlOnlyParameterMap(HttpServletRequest request) {
        // NOTE: these have already been through canonicalizeParameterMap, so not doing it again here
        Map<String, Object> paramMap = getQueryStringOnlyParameterMap(request.getQueryString());
        paramMap.putAll(getPathInfoOnlyParameterMap(request.getPathInfo(), null, null));
        return paramMap;
    }

    public static Map<String, Object> canonicalizeParameterMap(Map<String, Object> paramMap) {
        for (Map.Entry<String, Object> paramEntry: paramMap.entrySet()) {
            if (paramEntry.getValue() instanceof String) {
                paramEntry.setValue(canonicalizeParameter((String) paramEntry.getValue()));
            } else if (paramEntry.getValue() instanceof Collection<?>) {
                List<String> newList = new LinkedList<String>();
                for (String listEntry: UtilGenerics.<String>checkCollection(paramEntry.getValue())) {
                    newList.add(canonicalizeParameter(listEntry));
                }
                paramEntry.setValue(newList);
            }
        }
        return paramMap;
    }

    public static String canonicalizeParameter(String paramValue) {
        try {
            /** calling canonicalize with strict flag set to false so we only get warnings about double encoding, etc; can be set to true for exceptions and more security */
            String cannedStr = UtilCodec.canonicalize(paramValue, false);
            if (Debug.verboseOn()) Debug.logVerbose("Canonicalized parameter with " + (cannedStr.equals(paramValue) ? "no " : "") + "change: original [" + paramValue + "] canned [" + cannedStr + "]", module);
            return cannedStr;
        } catch (Exception e) {
            Debug.logError(e, "Error in canonicalize parameter value [" + paramValue + "]: " + e.toString(), module);
            return paramValue;
        }
    }

    /**
     * Create a map from a HttpRequest (attributes) object used in JSON requests
     * @return The resulting Map
     */
    public static Map<String, Object> getJSONAttributeMap(HttpServletRequest request) {
        Map<String, Object> returnMap = new HashMap<String, Object>();
        Map<String, Object> attrMap = getAttributeMap(request);
        for (Map.Entry<String, Object> entry : attrMap.entrySet()) {
            String key = entry.getKey();
            Object val = entry.getValue();
            if (val instanceof java.sql.Timestamp) {
                val = val.toString();
            }
            if (val instanceof String || val instanceof Number || val instanceof Map<?, ?> || val instanceof List<?> || val instanceof Boolean) {
                if (Debug.verboseOn()) Debug.logVerbose("Adding attribute to JSON output: " + key, module);
                returnMap.put(key, val);
            }
        }

        return returnMap;
    }

    /**
     * Create a map from a HttpRequest (attributes) object
     * @return The resulting Map
     */
    public static Map<String, Object> getAttributeMap(HttpServletRequest request) {
        return getAttributeMap(request, null);
    }

    /**
     * Create a map from a HttpRequest (attributes) object
     * @return The resulting Map
     */
    public static Map<String, Object> getAttributeMap(HttpServletRequest request, Set<? extends String> namesToSkip) {
        Map<String, Object> attributeMap = new HashMap<String, Object>();

        // look at all request attributes
        Enumeration<String> requestAttrNames = UtilGenerics.cast(request.getAttributeNames());
        while (requestAttrNames.hasMoreElements()) {
            String attrName = requestAttrNames.nextElement();
            if (namesToSkip != null && namesToSkip.contains(attrName))
                continue;

            Object attrValue = request.getAttribute(attrName);
            attributeMap.put(attrName, attrValue);
        }

        if (Debug.verboseOn()) {
            Debug.logVerbose("Made Request Attribute Map with [" + attributeMap.size() + "] Entries", module);
            Debug.logVerbose("Request Attribute Map Entries: " + System.getProperty("line.separator") + UtilMisc.printMap(attributeMap), module);
        }

        return attributeMap;
    }

    /**
     * Create a map from a HttpSession object
     * @return The resulting Map
     */
    public static Map<String, Object> getSessionMap(HttpServletRequest request) {
        return getSessionMap(request, null);
    }

    /**
     * Create a map from a HttpSession object
     * @return The resulting Map
     */
    public static Map<String, Object> getSessionMap(HttpServletRequest request, Set<? extends String> namesToSkip) {
        Map<String, Object> sessionMap = new HashMap<String, Object>();
        HttpSession session = request.getSession();

        // look at all the session attributes
        Enumeration<String> sessionAttrNames = UtilGenerics.cast(session.getAttributeNames());
        while (sessionAttrNames.hasMoreElements()) {
            String attrName = sessionAttrNames.nextElement();
            if (namesToSkip != null && namesToSkip.contains(attrName))
                continue;

            Object attrValue = session.getAttribute(attrName);
            sessionMap.put(attrName, attrValue);
        }

        if (Debug.verboseOn()) {
            Debug.logVerbose("Made Session Attribute Map with [" + sessionMap.size() + "] Entries", module);
            Debug.logVerbose("Session Attribute Map Entries: " + System.getProperty("line.separator") + UtilMisc.printMap(sessionMap), module);
        }

        return sessionMap;
    }

    /**
     * Create a map from a ServletContext object
     * @return The resulting Map
     */
    public static Map<String, Object> getServletContextMap(HttpServletRequest request) {
        return getServletContextMap(request, null);
    }

    /**
     * Create a map from a ServletContext object
     * @return The resulting Map
     */
    public static Map<String, Object> getServletContextMap(HttpServletRequest request, Set<? extends String> namesToSkip) {
        Map<String, Object> servletCtxMap = new HashMap<String, Object>();

        // look at all servlet context attributes
        ServletContext servletContext = (ServletContext) request.getAttribute("servletContext");
        Enumeration<String> applicationAttrNames = UtilGenerics.cast(servletContext.getAttributeNames());
        while (applicationAttrNames.hasMoreElements()) {
            String attrName = applicationAttrNames.nextElement();
            if (namesToSkip != null && namesToSkip.contains(attrName))
                continue;

            Object attrValue = servletContext.getAttribute(attrName);
            servletCtxMap.put(attrName, attrValue);
        }

        if (Debug.verboseOn()) {
            Debug.logVerbose("Made ServletContext Attribute Map with [" + servletCtxMap.size() + "] Entries", module);
            Debug.logVerbose("ServletContext Attribute Map Entries: " + System.getProperty("line.separator") + UtilMisc.printMap(servletCtxMap), module);
        }

        return servletCtxMap;
    }

    public static Map<String, Object> makeParamMapWithPrefix(HttpServletRequest request, String prefix, String suffix) {
        return makeParamMapWithPrefix(request, null, prefix, suffix);
    }

    public static Map<String, Object> makeParamMapWithPrefix(HttpServletRequest request, Map<String, ? extends Object> additionalFields, String prefix, String suffix) {
        return makeParamMapWithPrefix(getCombinedMap(request), additionalFields, prefix, suffix);
    }

    public static Map<String, Object> makeParamMapWithPrefix(Map<String, ? extends Object> context, Map<String, ? extends Object> additionalFields, String prefix, String suffix) {
        Map<String, Object> paramMap = new HashMap<String, Object>();
        for (Map.Entry<String, ? extends Object> entry: context.entrySet()) {
            String parameterName = entry.getKey();
            if (parameterName.startsWith(prefix)) {
                if (UtilValidate.isNotEmpty(suffix)) {
                    if (parameterName.endsWith(suffix)) {
                        String key = parameterName.substring(prefix.length(), parameterName.length() - (suffix.length()));
                        if (entry.getValue() instanceof ByteBuffer) {
                            ByteBuffer value = (ByteBuffer) entry.getValue();
                            paramMap.put(key, value);
                        } else {
                            String value = (String) entry.getValue();
                            paramMap.put(key, value);
                        }
                    }
                } else {
                    String key = parameterName.substring(prefix.length());
                    if (context.get(parameterName) instanceof ByteBuffer) {
                        ByteBuffer value = (ByteBuffer) entry.getValue();
                        paramMap.put(key, value);
                    } else {
                        String value = (String) entry.getValue();
                        paramMap.put(key, value);
                    }
                }
            }
        }
        if (additionalFields != null) {
            for (Map.Entry<String, ? extends Object> entry: additionalFields.entrySet()) {
                String fieldName = entry.getKey();
                if (fieldName.startsWith(prefix)) {
                    if (UtilValidate.isNotEmpty(suffix)) {
                        if (fieldName.endsWith(suffix)) {
                            String key = fieldName.substring(prefix.length(), fieldName.length() - (suffix.length() - 1));
                            Object value = entry.getValue();
                            paramMap.put(key, value);

                            // check for image upload data
                            if (!(value instanceof String)) {
                                String nameKey = "_" + key + "_fileName";
                                Object nameVal = additionalFields.get("_" + fieldName + "_fileName");
                                if (nameVal != null) {
                                    paramMap.put(nameKey, nameVal);
                                }

                                String typeKey = "_" + key + "_contentType";
                                Object typeVal = additionalFields.get("_" + fieldName + "_contentType");
                                if (typeVal != null) {
                                    paramMap.put(typeKey, typeVal);
                                }

                                String sizeKey = "_" + key + "_size";
                                Object sizeVal = additionalFields.get("_" + fieldName + "_size");
                                if (sizeVal != null) {
                                    paramMap.put(sizeKey, sizeVal);
                                }
                            }
                        }
                    } else {
                        String key = fieldName.substring(prefix.length());
                        Object value = entry.getValue();
                        paramMap.put(key, value);

                        // check for image upload data
                        if (!(value instanceof String)) {
                            String nameKey = "_" + key + "_fileName";
                            Object nameVal = additionalFields.get("_" + fieldName + "_fileName");
                            if (nameVal != null) {
                                paramMap.put(nameKey, nameVal);
                            }

                            String typeKey = "_" + key + "_contentType";
                            Object typeVal = additionalFields.get("_" + fieldName + "_contentType");
                            if (typeVal != null) {
                                paramMap.put(typeKey, typeVal);
                            }

                            String sizeKey = "_" + key + "_size";
                            Object sizeVal = additionalFields.get("_" + fieldName + "_size");
                            if (sizeVal != null) {
                                paramMap.put(sizeKey, sizeVal);
                            }
                        }
                    }
                }
            }
        }
        return paramMap;
    }

    public static List<Object> makeParamListWithSuffix(HttpServletRequest request, String suffix, String prefix) {
        return makeParamListWithSuffix(request, null, suffix, prefix);
    }

    public static List<Object> makeParamListWithSuffix(HttpServletRequest request, Map<String, ? extends Object> additionalFields, String suffix, String prefix) {
        List<Object> paramList = new ArrayList<Object>();
        Enumeration<String> parameterNames = UtilGenerics.cast(request.getParameterNames());
        while (parameterNames.hasMoreElements()) {
            String parameterName = parameterNames.nextElement();
            if (parameterName.endsWith(suffix)) {
                if (UtilValidate.isNotEmpty(prefix)) {
                    if (parameterName.startsWith(prefix)) {
                        String value = request.getParameter(parameterName);
                        paramList.add(value);
                    }
                } else {
                    String value = request.getParameter(parameterName);
                    paramList.add(value);
                }
            }
        }
        if (additionalFields != null) {
            for (Map.Entry<String, ? extends Object> entry: additionalFields.entrySet()) {
                String fieldName = entry.getKey();
                if (fieldName.endsWith(suffix)) {
                    if (UtilValidate.isNotEmpty(prefix)) {
                        if (fieldName.startsWith(prefix)) {
                            paramList.add(entry.getValue());
                        }
                    } else {
                        paramList.add(entry.getValue());
                    }
                }
            }
        }
        return paramList;
    }

    /**
     * Given a request, returns the application name or "root" if deployed on root
     * @param request An HttpServletRequest to get the name info from
     * @return String
     */
    public static String getApplicationName(HttpServletRequest request) {
        String appName = "root";
        if (request.getContextPath().length() > 1) {
            appName = request.getContextPath().substring(1);
        }
        return appName;
    }

    public static void setInitialRequestInfo(HttpServletRequest request) {
        HttpSession session = request.getSession();
        if (UtilValidate.isNotEmpty(session.getAttribute("_WEBAPP_NAME_"))) {
            // oops, info already in place...
            return;
        }

        String fullRequestUrl = getFullRequestUrl(request);

        session.setAttribute("_WEBAPP_NAME_", getApplicationName(request));
        session.setAttribute("_CLIENT_LOCALE_", request.getLocale());
        session.setAttribute("_CLIENT_REQUEST_", fullRequestUrl);
        session.setAttribute("_CLIENT_USER_AGENT_", request.getHeader("User-Agent") != null ? request.getHeader("User-Agent") : "");
        session.setAttribute("_CLIENT_REFERER_", request.getHeader("Referer") != null ? request.getHeader("Referer") : "");

        session.setAttribute("_CLIENT_FORWARDED_FOR_", request.getHeader("X-Forwarded-For"));
        session.setAttribute("_CLIENT_REMOTE_ADDR_", request.getRemoteAddr());
        session.setAttribute("_CLIENT_REMOTE_HOST_", request.getRemoteHost());
        session.setAttribute("_CLIENT_REMOTE_USER_", request.getRemoteUser());
    }

    /**
     * Put request parameters in request object as attributes.
     * @param request
     */
    public static void parametersToAttributes(HttpServletRequest request) {
        java.util.Enumeration<String> e = UtilGenerics.cast(request.getParameterNames());
        while (e.hasMoreElements()) {
            String name = e.nextElement();
            request.setAttribute(name, request.getParameter(name));
        }
    }


    private static StringBuilder prepareServerRootUrl(HttpServletRequest request) {
        StringBuilder requestUrl = new StringBuilder();
        requestUrl.append(request.getScheme());
        requestUrl.append("://" + request.getServerName());
        if (request.getServerPort() != 80 && request.getServerPort() != 443)
            requestUrl.append(":" + request.getServerPort());
        return requestUrl;
    }

    public static String getServerRootUrl(HttpServletRequest request) {
        return prepareServerRootUrl(request).toString();
    }

    public static String getFullRequestUrl(HttpServletRequest request) {
        StringBuilder requestUrl = prepareServerRootUrl(request);
        requestUrl.append(request.getRequestURI());
        if (request.getQueryString() != null) {
            requestUrl.append("?" + request.getQueryString());
        }
        return requestUrl.toString();
    }

    public static Locale getLocale(HttpServletRequest request, HttpSession session, Object appDefaultLocale) {
        // check session first, should override all if anything set there
        Object localeObject = session != null ? session.getAttribute("locale") : null;

        // next see if the userLogin has a value
        if (localeObject == null) {
            Map<?, ?> userLogin = (Map<?, ?>) session.getAttribute("userLogin");
            if (userLogin == null) {
                userLogin = (Map<?,?>) session.getAttribute("autoUserLogin");
            }

            if (userLogin != null) {
                localeObject = userLogin.get("lastLocale");
            }
        }

        // no user locale? before global default try appDefaultLocale if specified
        if (localeObject == null && !UtilValidate.isEmpty(appDefaultLocale)) {
            localeObject = appDefaultLocale;
        }

        // finally request (w/ a fall back to default)
        if (localeObject == null) {
            localeObject = request != null ? request.getLocale() : null;
        }

        return UtilMisc.ensureLocale(localeObject);
    }

    /**
     * Get the Locale object from a session variable; if not found use the browser's default
     * @param request HttpServletRequest object to use for lookup
     * @return Locale The current Locale to use
     */
    public static Locale getLocale(HttpServletRequest request) {
        if (request == null) return Locale.getDefault();
        return getLocale(request, request.getSession(), null);
    }

    /**
     * Get the Locale object from a session variable; if not found use the system's default.
     * NOTE: This method is not recommended because it ignores the Locale from the browser not having the request object.
     * @param session HttpSession object to use for lookup
     * @return Locale The current Locale to use
     */
    public static Locale getLocale(HttpSession session) {
        if (session == null) return Locale.getDefault();
        return getLocale(null, session, null);
    }

    public static void setLocale(HttpServletRequest request, String localeString) {
        setLocale(request.getSession(), UtilMisc.parseLocale(localeString));
    }

    public static void setLocale(HttpSession session, Locale locale) {
        session.setAttribute("locale", locale);
    }

    public static void setLocaleIfNone(HttpSession session, String localeString) {
        if (UtilValidate.isNotEmpty(localeString) && session.getAttribute("locale") == null) {
            setLocale(session, UtilMisc.parseLocale(localeString));
        }
    }

    public static void setTimeZone(HttpServletRequest request, String tzId) {
        setTimeZone(request.getSession(), UtilDateTime.toTimeZone(tzId));
    }

    public static void setTimeZone(HttpSession session, TimeZone timeZone) {
        session.setAttribute(SESSION_KEY_TIMEZONE, timeZone);
    }

    public static void setTimeZoneIfNone(HttpSession session, String timeZoneString) {
        if (UtilValidate.isNotEmpty(timeZoneString) && session.getAttribute(SESSION_KEY_TIMEZONE) == null) {
            UtilHttp.setTimeZone(session, UtilDateTime.toTimeZone(timeZoneString));
        }
    }

    public static TimeZone getTimeZone(HttpServletRequest request) {
        HttpSession session = request.getSession();
        TimeZone timeZone = (TimeZone) session.getAttribute(SESSION_KEY_TIMEZONE);
        Map<String, String> userLogin = UtilGenerics.cast(session.getAttribute("userLogin"));
        if (userLogin != null) {
            String tzId = userLogin.get("lastTimeZone");
            if (tzId != null) {
                timeZone = TimeZone.getTimeZone(tzId);
            }
        }
        if (timeZone == null) {
            timeZone = TimeZone.getDefault();
        }
        session.setAttribute(SESSION_KEY_TIMEZONE, timeZone);
        return timeZone;
    }

    public static TimeZone getTimeZone(HttpServletRequest request, HttpSession session, String appDefaultTimeZoneString) {
        // check session first, should override all if anything set there
        TimeZone timeZone = session != null ? (TimeZone) session.getAttribute(SESSION_KEY_TIMEZONE) : null;
        
        // next see if the userLogin has a value
        if (timeZone == null) {
            Map<String, Object> userLogin = UtilGenerics.checkMap(session.getAttribute("userLogin"), String.class, Object.class);
            if (userLogin == null) {
                userLogin = UtilGenerics.checkMap(session.getAttribute("autoUserLogin"), String.class, Object.class);
            }

            if ((userLogin != null) && (UtilValidate.isNotEmpty(userLogin.get("lastTimeZone")))) {
                timeZone = UtilDateTime.toTimeZone((String) userLogin.get("lastTimeZone"));
            }
        }

        // if there is no user TimeZone, we will got the application default time zone (if provided)
        if ((timeZone == null) && (UtilValidate.isNotEmpty(appDefaultTimeZoneString))) {
            timeZone = UtilDateTime.toTimeZone(appDefaultTimeZoneString);
        }

        // finally request (w/ a fall back to default)
        if (timeZone == null) {
            timeZone = TimeZone.getDefault();
        }

        return timeZone;
    }

    /**
     * Get the currency string from the session.
     * @param session HttpSession object to use for lookup
     * @return String The ISO currency code
     */
    public static String getCurrencyUom(HttpSession session, String appDefaultCurrencyUom) {
        // session, should override all if set there
        String iso = (String) session.getAttribute("currencyUom");

        // check userLogin next, ie if nothing to override in the session
        if (iso == null) {
            Map<String, ?> userLogin = UtilGenerics.cast(session.getAttribute("userLogin"));
            if (userLogin == null) {
                userLogin = UtilGenerics.cast(session.getAttribute("autoUserLogin"));
            }

            if (userLogin != null) {
                iso = (String) userLogin.get("lastCurrencyUom");
            }
        }

        // no user currency? before global default try appDefaultCurrencyUom if specified
        if (iso == null && !UtilValidate.isEmpty(appDefaultCurrencyUom)) {
            iso = appDefaultCurrencyUom;
        }

        // if none is set we will use the configured default
        if (iso == null) {
            try {
                iso = UtilProperties.getPropertyValue("general", "currency.uom.id.default", "USD");
            } catch (Exception e) {
                Debug.logWarning("Error getting the general:currency.uom.id.default value: " + e.toString(), module);
            }
        }


        // if still none we will use the default for whatever currency we can get...
        if (iso == null) {
            Currency cur = Currency.getInstance(getLocale(session));
            iso = cur.getCurrencyCode();
        }

        return iso;
    }

    /**
     * Get the currency string from the session.
     * @param request HttpServletRequest object to use for lookup
     * @return String The ISO currency code
     */
    public static String getCurrencyUom(HttpServletRequest request) {
        return getCurrencyUom(request.getSession(), null);
    }

    /** Simple event to set the users per-session currency uom value */
    public static void setCurrencyUom(HttpSession session, String currencyUom) {
        session.setAttribute("currencyUom", currencyUom);
    }

    public static void setCurrencyUomIfNone(HttpSession session, String currencyUom) {
        if (UtilValidate.isNotEmpty(currencyUom) && session.getAttribute("currencyUom") == null) {
            session.setAttribute("currencyUom", currencyUom);
        }
    }

    /** URL Encodes a Map of arguements */
    public static String urlEncodeArgs(Map<String, ? extends Object> args) {
        return urlEncodeArgs(args, true);
    }

    /** URL Encodes a Map of arguements */
    public static String urlEncodeArgs(Map<String, ? extends Object> args, boolean useExpandedEntites) {
        StringBuilder buf = new StringBuilder();
        if (args != null) {
            for (Map.Entry<String, ? extends Object> entry: args.entrySet()) {
                String name = entry.getKey();
                Object value = entry.getValue();
                String valueStr = null;
                if (name == null || value == null) {
                    continue;
                }

                Collection<?> col;
                if (value instanceof String) {
                    col = Arrays.asList(value);
                } else if (value instanceof Collection) {
                    col = UtilGenerics.cast(value);
                } else if (value.getClass().isArray()) {
                    col = Arrays.asList((Object[]) value);
                } else {
                    col = Arrays.asList(value);
                }
                for (Object colValue: col) {
                    if (colValue instanceof String) {
                        valueStr = (String) colValue;
                    } else if (colValue == null) {
                        continue;
                    } else {
                        valueStr = colValue.toString();
                    }

                    if (UtilValidate.isNotEmpty(valueStr)) {
                        if (buf.length() > 0) {
                            if (useExpandedEntites) {
                                buf.append("&amp;");
                            } else {
                                buf.append("&");
                            }
                        }
                        buf.append(UtilCodec.getEncoder("url").encode(name));
                        /* the old way: try {
                            buf.append(URLEncoder.encode(name, "UTF-8"));
                        } catch (UnsupportedEncodingException e) {
                            Debug.logError(e, module);
                        } */
                        buf.append('=');
                        buf.append(UtilCodec.getEncoder("url").encode(valueStr));
                        /* the old way: try {
                            buf.append(URLEncoder.encode(valueStr, "UTF-8"));
                        } catch (UnsupportedEncodingException e) {
                            Debug.logError(e, module);
                        } */
                    }
                }
            }
        }
        return buf.toString();
    }

    public static String getRequestUriFromTarget(String target) {
        if (UtilValidate.isEmpty(target)) return null;
        int endOfRequestUri = target.length();
        if (target.indexOf('?') > 0) {
            endOfRequestUri = target.indexOf('?');
        }
        int slashBeforeRequestUri = target.lastIndexOf('/', endOfRequestUri);
        String requestUri = null;
        if (slashBeforeRequestUri < 0) {
            requestUri = target.substring(0, endOfRequestUri);
        } else {
            requestUri = target.substring(slashBeforeRequestUri, endOfRequestUri);
        }
        return requestUri;
    }

    /** Returns the query string contained in a request target - basically everything
     * after and including the ? character.
     * @param target The request target
     * @return The query string
     */
    public static String getQueryStringFromTarget(String target) {
        if (UtilValidate.isEmpty(target)) return "";
        int queryStart = target.indexOf('?');
        if (queryStart != -1) {
            return target.substring(queryStart);
        }
        return "";
    }

    /** Removes the query string from a request target - basically everything
     * after and including the ? character.
     * @param target The request target
     * @return The request target string
     */
    public static String removeQueryStringFromTarget(String target) {
        if (UtilValidate.isEmpty(target)) return null;
        int queryStart = target.indexOf('?');
        if (queryStart < 0) {
            return target;
        }
        return target.substring(0, queryStart);
    }

    public static String getWebappMountPointFromTarget(String target) {
        int firstChar = 0;
        if (UtilValidate.isEmpty(target)) return null;
        if (target.charAt(0) == '/') firstChar = 1;
        int pathSep = target.indexOf('/', 1);
        String webappMountPoint = null;
        if (pathSep > 0) {
            // if not then no good, supposed to be a inter-app, but there is no path sep! will do general search with null and treat like an intra-app
            webappMountPoint = target.substring(firstChar, pathSep);
        }
        return webappMountPoint;
    }

    public static String encodeAmpersands(String htmlString) {
        StringBuilder htmlBuffer = new StringBuilder(htmlString);
        int ampLoc = -1;
        while ((ampLoc = htmlBuffer.indexOf("&", ampLoc + 1)) != -1) {
            //NOTE: this should work fine, but if it doesn't could try making sure all characters between & and ; are letters, that would qualify as an entity

            // found ampersand, is it already and entity? if not change it to &amp;
            int semiLoc = htmlBuffer.indexOf(";", ampLoc);
            if (semiLoc != -1) {
                // found a semi colon, if it has another & or an = before it, don't count it as an entity, otherwise it may be an entity, so skip it
                int eqLoc = htmlBuffer.indexOf("=", ampLoc);
                int amp2Loc = htmlBuffer.indexOf("&", ampLoc + 1);
                if ((eqLoc == -1 || eqLoc > semiLoc) && (amp2Loc == -1 || amp2Loc > semiLoc)) {
                    continue;
                }
            }

            // at this point not an entity, no substitute with a &amp;
            htmlBuffer.insert(ampLoc + 1, "amp;");
        }
        return htmlBuffer.toString();
    }

    public static String encodeBlanks(String htmlString) {
        return htmlString.replaceAll(" ", "%20");
    }

    public static String setResponseBrowserProxyNoCache(HttpServletRequest request, HttpServletResponse response) {
        setResponseBrowserProxyNoCache(response);
        return "success";
    }

    public static void setResponseBrowserProxyNoCache(HttpServletResponse response) {
        long nowMillis = System.currentTimeMillis();
        response.setDateHeader("Expires", nowMillis);
        response.setDateHeader("Last-Modified", nowMillis); // always modified
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate"); // HTTP/1.1
        response.addHeader("Cache-Control", "post-check=0, pre-check=0, false");
        response.setHeader("Pragma", "no-cache"); // HTTP/1.0
    }

    public static String getContentTypeByFileName(String fileName) {
        FileNameMap mime = URLConnection.getFileNameMap();
        return mime.getContentTypeFor(fileName);
    }

    /**
     * Stream an array of bytes to the browser
     * This method will close the ServletOutputStream when finished
     *
     * @param response HttpServletResponse object to get OutputStream from
     * @param bytes Byte array of content to stream
     * @param contentType The content type to pass to the browser
     * @param fileName the fileName to tell the browser we are downloading
     * @throws IOException
     */
    public static void streamContentToBrowser(HttpServletResponse response, byte[] bytes, String contentType, String fileName) throws IOException {
        // tell the browser not the cache
        setResponseBrowserProxyNoCache(response);

        // set the response info
        response.setContentLength(bytes.length);
        if (contentType != null) {
            response.setContentType(contentType);
        }
        if (fileName != null) {
            setContentDisposition(response, fileName);
        }

        // create the streams
        OutputStream out = response.getOutputStream();
        InputStream in = new ByteArrayInputStream(bytes);

        // stream the content
        try {
            streamContent(out, in, bytes.length);
        } catch (IOException e) {
            in.close();
            out.close(); // should we close the ServletOutputStream on error??
            throw e;
        }

        // close the input stream
        in.close();

        // close the servlet output stream
        out.flush();
        out.close();
    }

    public static void streamContentToBrowser(HttpServletResponse response, byte[] bytes, String contentType) throws IOException {
        streamContentToBrowser(response, bytes, contentType, null);
    }

    /**
     * Streams content from InputStream to the ServletOutputStream
     * This method will close the ServletOutputStream when finished
     * This method does not close the InputSteam passed
     *
     * @param response HttpServletResponse object to get OutputStream from
     * @param in InputStream of the actual content
     * @param length Size (in bytes) of the content
     * @param contentType The content type to pass to the browser
     * @throws IOException
     */
    public static void streamContentToBrowser(HttpServletResponse response, InputStream in, int length, String contentType, String fileName) throws IOException {
        // tell the browser not the cache
        setResponseBrowserProxyNoCache(response);

        // set the response info
        response.setContentLength(length);
        if (contentType != null) {
            response.setContentType(contentType);
        }
        if (fileName != null) {
            setContentDisposition(response, fileName);
        }

        // stream the content
        OutputStream out = response.getOutputStream();
        try {
            streamContent(out, in, length);
        } catch (IOException e) {
            out.close();
            throw e;
        }

        // close the servlet output stream
        out.flush();
        out.close();
    }

    public static void streamContentToBrowser(HttpServletResponse response, InputStream in, int length, String contentType) throws IOException {
        streamContentToBrowser(response, in, length, contentType, null);
    }

    /**
     * Stream binary content from InputStream to OutputStream
     * This method does not close the streams passed
     *
     * @param out OutputStream content should go to
     * @param in InputStream of the actual content
     * @param length Size (in bytes) of the content
     * @throws IOException
     */
    public static void streamContent(OutputStream out, InputStream in, int length) throws IOException {
        int bufferSize = 512; // same as the default buffer size; change as needed

        // make sure we have something to write to
        if (out == null) {
            throw new IOException("Attempt to write to null output stream");
        }

        // make sure we have something to read from
        if (in == null) {
            throw new IOException("Attempt to read from null input stream");
        }

        // make sure we have some content
        if (length == 0) {
            throw new IOException("Attempt to write 0 bytes of content to output stream");
        }

        // initialize the buffered streams
        BufferedOutputStream bos = new BufferedOutputStream(out, bufferSize);
        BufferedInputStream bis = new BufferedInputStream(in, bufferSize);

        byte[] buffer = new byte[length];
        int read = 0;
        try {
            while ((read = bis.read(buffer, 0, buffer.length)) != -1) {
                bos.write(buffer, 0, read);
            }
        } catch (IOException e) {
            Debug.logError(e, "Problem reading/writing buffers", module);
            bis.close();
            bos.close();
            throw e;
        } finally {
            if (bis != null) {
                bis.close();
            }
            if (bos != null) {
                bos.flush();
                bos.close();
            }
        }
    }

    public static String stripViewParamsFromQueryString(String queryString) {
        return stripViewParamsFromQueryString(queryString, null);
    }

    public static String stripViewParamsFromQueryString(String queryString, String paginatorNumber) {
        Set<String> paramNames = new HashSet<String>();
        if (UtilValidate.isNotEmpty(paginatorNumber)) {
            paginatorNumber = "_" + paginatorNumber;
        }
        paramNames.add("VIEW_INDEX" + paginatorNumber);
        paramNames.add("VIEW_SIZE" + paginatorNumber);
        paramNames.add("viewIndex" + paginatorNumber);
        paramNames.add("viewSize" + paginatorNumber);
        return stripNamedParamsFromQueryString(queryString, paramNames);
    }

    public static String stripNamedParamsFromQueryString(String queryString, Collection<String> paramNames) {
        String retStr = null;
        if (UtilValidate.isNotEmpty(queryString)) {
            StringTokenizer queryTokens = new StringTokenizer(queryString, "&");
            StringBuilder cleanQuery = new StringBuilder();
            while (queryTokens.hasMoreTokens()) {
                String token = queryTokens.nextToken();
                if (token.startsWith("amp;")) {
                    token = token.substring(4);
                }
                int equalsIndex = token.indexOf("=");
                String name = token;
                if (equalsIndex > 0) {
                    name = token.substring(0, equalsIndex);
                }
                if (!paramNames.contains(name)) {
                    if (cleanQuery.length() > 0) {
                        cleanQuery.append("&");
                    }
                    cleanQuery.append(token);
                }
            }
            retStr = cleanQuery.toString();
        }
        return retStr;
    }

    /**
     * Given multi form data with the ${param}_o_N notation, creates a Collection
     * of Maps for the submitted rows. Each Map contains the key/value pairs
     * of a particular row. The keys will be stripped of the _o_N suffix.
     * There is an additionaly key "row" for each Map that holds the
     * index of the row.
     */
    public static Collection<Map<String, Object>> parseMultiFormData(Map<String, Object> parameters) {
        Map<Integer, Map<String, Object>> rows = new HashMap<Integer, Map<String, Object>>(); // stores the rows keyed by row number

        // first loop through all the keys and create a hashmap for each ${ROW_SUBMIT_PREFIX}${N} = Y
        for (String key: parameters.keySet()) {
            // skip everything that is not ${ROW_SUBMIT_PREFIX}N
            if (key == null || key.length() <= ROW_SUBMIT_PREFIX_LENGTH) continue;
            if (key.indexOf(MULTI_ROW_DELIMITER) <= 0) continue;
            if (!key.substring(0, ROW_SUBMIT_PREFIX_LENGTH).equals(ROW_SUBMIT_PREFIX)) continue;
            if (!parameters.get(key).equals("Y")) continue;

            // decode the value of N and create a new map for it
            Integer n = Integer.decode(key.substring(ROW_SUBMIT_PREFIX_LENGTH, key.length()));
            Map<String, Object> m = new HashMap<String, Object>();
            m.put("row", n); // special "row" = N tuple
            rows.put(n, m); // key it to N
        }

        // next put all parameters with matching N in the right map
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            String key = entry.getKey();
            // skip keys without DELIMITER and skip ROW_SUBMIT_PREFIX
            if (key == null) continue;
            int index = key.indexOf(MULTI_ROW_DELIMITER);
            if (index <= 0) continue;
            if (key.length() > ROW_SUBMIT_PREFIX_LENGTH && key.substring(0, ROW_SUBMIT_PREFIX_LENGTH).equals(ROW_SUBMIT_PREFIX)) continue;

            // get the map with index N
            Integer n = Integer.decode(key.substring(index + MULTI_ROW_DELIMITER_LENGTH, key.length())); // N from ${param}${DELIMITER}${N}
            Map<String, Object> map = rows.get(n);
            if (map == null) continue;

            // get the key without the <DELIMITER>N suffix and store it and its value
            String newKey = key.substring(0, index);
            map.put(newKey, entry.getValue());
        }
        // return only the values, which is the list of maps
        return rows.values();
    }

    /**
     * Returns a new map containing all the parameters from the input map except for the
     * multi form parameters (usually named according to the ${param}_o_N notation).
     */
    public static <V> Map<String, V> removeMultiFormParameters(Map<String, V> parameters) {
        Map<String, V> filteredParameters = new HashMap<String, V>();
        for (Map.Entry<String, V> entry : parameters.entrySet()) {
            String key = entry.getKey();
            if (key != null && (key.indexOf(MULTI_ROW_DELIMITER) != -1 || key.indexOf("_useRowSubmit") != -1 || key.indexOf("_rowCount") != -1)) {
                continue;
            }

            filteredParameters.put(key, entry.getValue());
        }
        return filteredParameters;
    }

    /**
     * Utility to make a composite parameter from the given prefix and suffix.
     * The prefix should be a regular paramter name such as meetingDate. The
     * suffix is the composite field, such as the hour of the meeting. The
     * result would be meetingDate_${COMPOSITE_DELIMITER}_hour.
     *
     * @param prefix
     * @param suffix
     * @return the composite parameter
     */
    public static String makeCompositeParam(String prefix, String suffix) {
        return prefix + COMPOSITE_DELIMITER + suffix;
    }

    /**
     * Given the prefix of a composite parameter, recomposes a single Object from
     * the composite according to compositeType. For example, consider the following
     * form widget field,
     *
     *   <field name="meetingDate">
     *     <date-time type="timestamp" input-method="time-dropdown">
     *   </field>
     *
     * The result in HTML is three input boxes to input the date, hour and minutes separately.
     * The parameter names are named meetingDate_c_date, meetingDate_c_hour, meetingDate_c_minutes.
     * Additionally, there will be a field named meetingDate_c_compositeType with a value of "Timestamp".
     * where _c_ is the COMPOSITE_DELIMITER. These parameters will then be recomposed into a Timestamp
     * object from the composite fields.
     *
     * @param request
     * @param prefix
     * @return Composite object from data or null if not supported or a parsing error occurred.
     */
    public static Object makeParamValueFromComposite(HttpServletRequest request, String prefix, Locale locale) {
        String compositeType = request.getParameter(makeCompositeParam(prefix, "compositeType"));
        if (UtilValidate.isEmpty(compositeType)) return null;

        // collect the composite fields into a map
        Map<String, String> data = new HashMap<String, String>();
        for (Enumeration<String> names = UtilGenerics.cast(request.getParameterNames()); names.hasMoreElements();) {
            String name = names.nextElement();
            if (!name.startsWith(prefix + COMPOSITE_DELIMITER)) continue;

            // extract the suffix of the composite name
            String suffix = name.substring(name.indexOf(COMPOSITE_DELIMITER) + COMPOSITE_DELIMITER_LENGTH);

            // and the value of this parameter
            String value = request.getParameter(name);

            // key = suffix, value = parameter data
            data.put(suffix, value);
        }
        if (Debug.verboseOn()) { Debug.logVerbose("Creating composite type with parameter data: " + data.toString(), module); }

        // handle recomposition of data into the compositeType
        if ("Timestamp".equals(compositeType)) {
            String date = data.get("date");
            String hour = data.get("hour");
            String minutes = data.get("minutes");
            String ampm = data.get("ampm");
            if (date == null || date.length() < 10) return null;
            if (UtilValidate.isEmpty(hour)) return null;
            if (UtilValidate.isEmpty(minutes)) return null;
            boolean isTwelveHour = UtilValidate.isNotEmpty(ampm);

            // create the timestamp from the data
            try {
                int h = Integer.parseInt(hour);
                Timestamp timestamp = Timestamp.valueOf(date.substring(0, 10) + " 00:00:00.000");
                Calendar cal = Calendar.getInstance(locale);
                cal.setTime(timestamp);
                if (isTwelveHour) {
                    boolean isAM = ("AM".equals(ampm) ? true : false);
                    if (isAM && h == 12) h = 0;
                    if (!isAM && h < 12) h += 12;
                }
                cal.set(Calendar.HOUR_OF_DAY, h);
                cal.set(Calendar.MINUTE, Integer.parseInt(minutes));
                return new Timestamp(cal.getTimeInMillis());
            } catch (IllegalArgumentException e) {
                Debug.logWarning("User input for composite timestamp was invalid: " + e.getMessage(), module);
                return null;
            }
        }

        // we don't support any other compositeTypes (yet)
        return null;
    }

    /** Obtains the session ID from the request, or "unknown" if no session pressent. */
    public static String getSessionId(HttpServletRequest request) {
        HttpSession session = request.getSession();
        return (session == null ? "unknown" : session.getId());
    }
    /**
     * checks, if the current request comes from a searchbot
     *
     * @param request
     * @return whether the request is from a web searchbot
     */
    public static boolean checkURLforSpiders(HttpServletRequest request) {
        boolean result = false;

        String spiderRequest = (String) request.getAttribute("_REQUEST_FROM_SPIDER_");
        if (UtilValidate.isNotEmpty(spiderRequest)) {
            if ("Y".equals(spiderRequest)) {
                return true;
            } else {
                return false;
            }
        } else {
            String initialUserAgent = request.getHeader("User-Agent") != null ? request.getHeader("User-Agent") : "";
            List<String> spiderList = StringUtil.split(UtilProperties.getPropertyValue("url", "link.remove_lsessionid.user_agent_list"), ",");

            if (UtilValidate.isNotEmpty(spiderList)) {
                for (String spiderNameElement : spiderList) {
                    Pattern pattern = null;
                    try {
                        pattern = PatternFactory.createOrGetPerl5CompiledPattern(spiderNameElement, false);
                    } catch (MalformedPatternException e) {
                        Debug.logError(e, module);
                    }
                    PatternMatcher matcher = new Perl5Matcher();
                    if (matcher.contains(initialUserAgent, pattern)) {
                        request.setAttribute("_REQUEST_FROM_SPIDER_", "Y");
                        result = true;
                        break;
                    }
                }
            }
        }

        if (!result) {
            request.setAttribute("_REQUEST_FROM_SPIDER_", "N");
        }

        return result;
    }

    /** Returns true if the user has JavaScript enabled.
     * @param request
     * @return whether javascript is enabled
     */
    public static boolean isJavaScriptEnabled(HttpServletRequest request) {
        HttpSession session = request.getSession();
        Boolean javaScriptEnabled = (Boolean) session.getAttribute("javaScriptEnabled");
        if (javaScriptEnabled != null) {
            return javaScriptEnabled.booleanValue();
        }
        return false;
    }

    /** Returns the number or rows submitted by a multi form.
     */
    public static int getMultiFormRowCount(HttpServletRequest request) {
        return getMultiFormRowCount(getParameterMap(request));
    }
    /** Returns the number or rows submitted by a multi form.
     */
    public static int getMultiFormRowCount(Map<String, ?> requestMap) {
        // The number of multi form rows is computed selecting the maximum index
        int rowCount = 0;
        String maxRowIndex = "";
        int rowDelimiterLength = MULTI_ROW_DELIMITER.length();
        for (String parameterName: requestMap.keySet()) {
            int rowDelimiterIndex = (parameterName != null? parameterName.indexOf(MULTI_ROW_DELIMITER): -1);
            if (rowDelimiterIndex > 0) {
                String thisRowIndex = parameterName.substring(rowDelimiterIndex + rowDelimiterLength);
                if (thisRowIndex.indexOf("_") > -1) {
                    thisRowIndex = thisRowIndex.substring(0, thisRowIndex.indexOf("_"));
                }
                if (maxRowIndex.length() < thisRowIndex.length()) {
                    maxRowIndex = thisRowIndex;
                } else if (maxRowIndex.length() == thisRowIndex.length() && maxRowIndex.compareTo(thisRowIndex) < 0) {
                    maxRowIndex = thisRowIndex;
                }
            }
        }
        if (UtilValidate.isNotEmpty(maxRowIndex)) {
            try {
                rowCount = Integer.parseInt(maxRowIndex);
                rowCount++; // row indexes are zero based
            } catch (NumberFormatException e) {
                Debug.logWarning("Invalid value for row index found: " + maxRowIndex, module);
            }
        }
        return rowCount;
    }

    public static String stashParameterMap(HttpServletRequest request) {
        HttpSession session = request.getSession();
        Map<String, Map<String, Object>> paramMapStore = UtilGenerics.checkMap(session.getAttribute("_PARAM_MAP_STORE_"));
        if (paramMapStore == null) {
            paramMapStore = new HashMap<String, Map<String, Object>>();
            session.setAttribute("_PARAM_MAP_STORE_", paramMapStore);
        }
        Map<String, Object> parameters = getParameterMap(request);
        String paramMapId = RandomStringUtils.randomAlphanumeric(10);
        paramMapStore.put(paramMapId, parameters);
        return paramMapId;
    }

    public static void restoreStashedParameterMap(HttpServletRequest request, String paramMapId) {
        HttpSession session = request.getSession();
        Map<String, Map<String, Object>> paramMapStore = UtilGenerics.checkMap(session.getAttribute("_PARAM_MAP_STORE_"));
        if (paramMapStore != null) {
            Map<String, Object> paramMap = paramMapStore.get(paramMapId);
            if (paramMap != null) {
                paramMapStore.remove(paramMapId);
                for (Map.Entry<String, Object> paramEntry : paramMap.entrySet()) {
                    if (request.getAttribute(paramEntry.getKey()) != null) {
                        Debug.logWarning("Skipped loading parameter [" + paramEntry.getKey() + "] because it would have overwritten a request attribute" , module);
                        continue;
                    }
                    request.setAttribute(paramEntry.getKey(), paramEntry.getValue());
                }
            }
        }
    }

    /**
     * Returns a unique Id for the current request
     * @param request An HttpServletRequest to get the name info from
     * @return String
     */
    public static String getNextUniqueId(HttpServletRequest request) {
        Integer uniqueIdNumber= (Integer)request.getAttribute("UNIQUE_ID");
        if (uniqueIdNumber == null) {
            uniqueIdNumber = Integer.valueOf(1);
        }

        request.setAttribute("UNIQUE_ID", Integer.valueOf(uniqueIdNumber.intValue() + 1));
        return "autoId_" + uniqueIdNumber;
    }

    public static void setContentDisposition(final HttpServletResponse response, final String filename) {
        String dispositionType = UtilProperties.getPropertyValue("requestHandler", "content-disposition-type", "attachment");
        response.setHeader("Content-Disposition", String.format("%s; filename=\"%s\"", dispositionType, filename));
    }

    public static CloseableHttpClient getAllowAllHttpClient() {
        return getAllowAllHttpClient("component://base/config/ofbizssl.jks", "changeit");
    }

    public static CloseableHttpClient getAllowAllHttpClient(String jksStoreFileName, String jksStorePassword) {
        try {
            // Trust own CA and all self-signed certs
            SSLContext sslContext = SSLContexts.custom()
                    .loadTrustMaterial(FileUtil.getFile(jksStoreFileName), jksStorePassword.toCharArray(),
                            new TrustSelfSignedStrategy())
                    .build();
            // No host name verifier
            SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(
                    sslContext,
                    NoopHostnameVerifier.INSTANCE);
            CloseableHttpClient httpClient = HttpClients.custom()
                    .setSSLSocketFactory(sslsf)
                    .build();
            return httpClient;
        } catch (Exception e) {
            return HttpClients.createDefault();
        }
    }

}
