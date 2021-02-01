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
package org.apache.ofbiz.webtools;

import java.util.Iterator;
import java.util.Locale;
import java.util.List;
import java.util.Map;
import java.util.LinkedList;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilHttp;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.cache.UtilCache;
import org.apache.ofbiz.security.Security;

/**
 * Contains events for the UtilCache class; must be external to access security resources
 */
public final class UtilCacheEvents {

    private static final String ERR_RESOURCE = "WebtoolsErrorUiLabels";
    private static final String MODULE = UtilCacheEvents.class.getName();


    private UtilCacheEvents() { }

    /** An HTTP WebEvent handler the specified element from the specified cache
     * @param request The HTTP request object for the current JSP or Servlet request.
     * @param response The HTTP response object for the current JSP or Servlet request.
     * @return return an HTTP WebEvent handler the specified element from the specified cache
     */
    public static String removeElementEvent(HttpServletRequest request, HttpServletResponse response) {
        String errMsg = "";
        Locale locale = UtilHttp.getLocale(request);

        Security security = (Security) request.getAttribute("security");
        if (!security.hasPermission("UTIL_CACHE_EDIT", request.getSession())) {
            errMsg = UtilProperties.getMessage(ERR_RESOURCE, "utilCacheEvents.permissionEdit", locale) + ".";
            request.setAttribute("_ERROR_MESSAGE_", errMsg);
            return "error";
        }

        String name = request.getParameter("UTIL_CACHE_NAME");
        if (name == null) {
            errMsg = UtilProperties.getMessage(ERR_RESOURCE, "utilCacheEvents.noCacheNameSpecified", locale) + ".";
            request.setAttribute("_ERROR_MESSAGE_", errMsg);
            return "error";
        }
        String numString = request.getParameter("UTIL_CACHE_ELEMENT_NUMBER");

        if (numString == null) {
            errMsg = UtilProperties.getMessage(ERR_RESOURCE, "utilCacheEvents.noElementNumberSpecified", locale) + ".";
            request.setAttribute("_ERROR_MESSAGE_", "");
            return "error";
        }
        int number;

        try {
            number = Integer.parseInt(numString);
        } catch (Exception e) {
            return "error";
        }

        UtilCache<?, ?> utilCache = UtilCache.findCache(name);

        if (utilCache != null) {
            Object key = null;

            Iterator<?> ksIter = utilCache.getCacheLineKeys().iterator();
            int curNum = 0;

            while (ksIter.hasNext()) {
                if (number == curNum) {
                    key = ksIter.next();
                    break;
                } else {
                    ksIter.next();
                }
                curNum++;
            }

            if (key != null) {
                utilCache.remove(key);
                errMsg = UtilProperties.getMessage(ERR_RESOURCE, "utilCache.removeElementWithKey", UtilMisc.toMap("key",
                        key.toString()), locale) + ".";
                request.setAttribute("_EVENT_MESSAGE_", errMsg);
            } else {
                errMsg = UtilProperties.getMessage(ERR_RESOURCE, "utilCache.couldNotRemoveElementNumber", UtilMisc.toMap("name",
                        name, "numString", numString), locale) + ".";
                request.setAttribute("_ERROR_MESSAGE_", errMsg);
                return "error";
            }
        } else {
            errMsg = UtilProperties.getMessage(ERR_RESOURCE, "utilCache.couldNotRemoveElement", UtilMisc.toMap("name", name), locale) + ".";
            request.setAttribute("_ERROR_MESSAGE_", errMsg);
            return "error";
        }
        return "success";
    }

    /** An HTTP WebEvent handler that clears the named cache
     * @param request The HTTP request object for the current JSP or Servlet request.
     * @param response The HTTP response object for the current JSP or Servlet request.
     * @return return an HTTP WebEvent handler that clears the named cache
     */
    public static String clearEvent(HttpServletRequest request, HttpServletResponse response) {
        String errMsg = "";
        Locale locale = UtilHttp.getLocale(request);

        Security security = (Security) request.getAttribute("security");
        if (!security.hasPermission("UTIL_CACHE_EDIT", request.getSession())) {
            errMsg = UtilProperties.getMessage(ERR_RESOURCE, "utilCacheEvents.permissionEdit", locale) + ".";
            request.setAttribute("_ERROR_MESSAGE_", errMsg);
            return "error";
        }

        String name = request.getParameter("UTIL_CACHE_NAME");

        if (name == null) {
            errMsg = UtilProperties.getMessage(ERR_RESOURCE, "utilCache.couldNotClearCache", locale) + ".";
            request.setAttribute("_ERROR_MESSAGE_", errMsg);
            return "error";
        }
        UtilCache<?, ?> utilCache = UtilCache.findCache(name);

        if (utilCache != null) {
            utilCache.clear();
            errMsg = UtilProperties.getMessage(ERR_RESOURCE, "utilCache.clearCache", UtilMisc.toMap("name", name), locale) + ".";
            request.setAttribute("_EVENT_MESSAGE_", errMsg);
        } else {
            errMsg = UtilProperties.getMessage(ERR_RESOURCE, "utilCache.couldNotClearCacheNotFoundName", UtilMisc.toMap("name", name), locale) + ".";
            request.setAttribute("_ERROR_MESSAGE_", errMsg);
            return "error";
        }
        return "success";
    }

    /** An HTTP WebEvent handler that clears all caches
     * @param request The HTTP request object for the current JSP or Servlet request.
     * @param response The HTTP response object for the current JSP or Servlet request.
     * @return return an HTTP WebEvent handler that clears all caches
     */
    public static String clearAllEvent(HttpServletRequest request, HttpServletResponse response) {
        String errMsg = "";
        Locale locale = UtilHttp.getLocale(request);

        Security security = (Security) request.getAttribute("security");
        if (!security.hasPermission("UTIL_CACHE_EDIT", request.getSession())) {
            errMsg = UtilProperties.getMessage(ERR_RESOURCE, "utilCacheEvents.permissionEdit", locale) + ".";
            request.setAttribute("_ERROR_MESSAGE_", errMsg);
            return "error";
        }

        UtilCache.clearAllCaches();
        errMsg = UtilProperties.getMessage(ERR_RESOURCE, "utilCache.clearAllCaches", locale);
        request.setAttribute("_EVENT_MESSAGE_", errMsg + " (" + UtilDateTime.nowDateString("yyyy-MM-dd HH:mm:ss") + ").");
        return "success";
    }
    /** An HTTP WebEvent handler that clears the selected caches
     * @param request The HTTP request object for the current JSP or Servlet request.
     * @param response The HTTP response object for the current JSP or Servlet request.
     * @return return an HTTP WebEvent handler that clears all caches
     */
    public static String clearSelectedCachesEvent(HttpServletRequest request, HttpServletResponse response) {

        String errMsg = "";
        Locale locale = UtilHttp.getLocale(request);

        Security security = (Security) request.getAttribute("security");
        if (!security.hasPermission("UTIL_CACHE_EDIT", request.getSession())) {
            errMsg = UtilProperties.getMessage(ERR_RESOURCE, "utilCacheEvents.permissionEdit", locale) + ".";
            request.setAttribute("_ERROR_MESSAGE_", errMsg);
            return "error";
        }

        Map<String, Object> ctx = UtilHttp.getParameterMap(request);
        boolean isSelected;
        List<String> eventList = new LinkedList<>();
        int rowCount = UtilHttp.getMultiFormRowCount(ctx);
        for (int i = 0; i < rowCount; i++) {
            String suffix = UtilHttp.getMultiRowDelimiter() + i;
            isSelected = (ctx.containsKey("_rowSubmit" + suffix) && "Y".equalsIgnoreCase((String) ctx.get("_rowSubmit" + suffix)));
            if (!isSelected) {
                continue;
            }

            String name = request.getParameter("cacheName" + suffix);

            if (name == null) {
                errMsg = UtilProperties.getMessage(ERR_RESOURCE, "utilCache.couldNotClearCache", locale) + ".";
                eventList.add(errMsg);
            }

            UtilCache<?, ?> utilCache = UtilCache.findCache(name);

            if (utilCache != null) {
                utilCache.clear();
                errMsg = UtilProperties.getMessage(ERR_RESOURCE, "utilCache.clearCache", UtilMisc.toMap("name", name), locale) + ".";
                eventList.add(errMsg);
            } else {
                errMsg = UtilProperties.getMessage(ERR_RESOURCE, "utilCache.couldNotClearCacheNotFoundName",
                        UtilMisc.toMap("name", name), locale) + ".";
                eventList.add(errMsg);
            }
        }
        request.setAttribute("_EVENT_MESSAGE_LIST_", eventList);
        return "success";
    }

    /** An HTTP WebEvent handler that updates the named cache
     * @param request The HTTP request object for the current JSP or Servlet request.
     * @param response The HTTP response object for the current JSP or Servlet request.
     * @return return an HTTP WebEvent handler that updates the named cache
     */
    public static String updateEvent(HttpServletRequest request, HttpServletResponse response) {
        String errMsg = "";
        Locale locale = UtilHttp.getLocale(request);

        Security security = (Security) request.getAttribute("security");
        if (!security.hasPermission("UTIL_CACHE_EDIT", request.getSession())) {
            errMsg = UtilProperties.getMessage(ERR_RESOURCE, "utilCacheEvents.permissionEdit", locale) + ".";
            request.setAttribute("_EVENT_MESSAGE_", errMsg);
            return "error";
        }

        String name = request.getParameter("UTIL_CACHE_NAME");

        if (name == null) {
            errMsg = UtilProperties.getMessage(ERR_RESOURCE, "utilCache.couldNotUpdateCacheSetting", locale) + ".";
            request.setAttribute("_ERROR_MESSAGE_", errMsg);
            return "error";
        }
        String maxInMemoryStr = request.getParameter("UTIL_CACHE_MAX_IN_MEMORY");
        String expireTimeStr = request.getParameter("UTIL_CACHE_EXPIRE_TIME");
        String useSoftReferenceStr = request.getParameter("UTIL_CACHE_USE_SOFT_REFERENCE");

        Integer maxInMemory = null;
        Long expireTime = null;

        try {
            maxInMemory = Integer.valueOf(maxInMemoryStr);
        } catch (Exception e) {
            Debug.logError(e, MODULE);
        }

        try {
            expireTime = Long.valueOf(expireTimeStr);
        } catch (Exception e) {
            Debug.logError(e, MODULE);
        }


        UtilCache<?, ?> utilCache = UtilCache.findCache(name);

        if (utilCache != null) {
            if (maxInMemory != null) {
                utilCache.setMaxInMemory(maxInMemory);
            }
            if (expireTime != null) {
                utilCache.setExpireTime(expireTime);
            }
            if (useSoftReferenceStr != null) {
                utilCache.setUseSoftReference("true".equals(useSoftReferenceStr));
            }
        }
        return "success";
    }
}
