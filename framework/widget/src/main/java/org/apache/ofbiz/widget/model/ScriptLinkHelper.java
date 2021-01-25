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
package org.apache.ofbiz.widget.model;

import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.security.CsrfUtil;
import org.apache.ofbiz.webapp.SeoConfigUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Utility to support different handling of code blocks in an html template:
 * 1. Inline javascript tags are turned into external javascript tags for better compliance of Content Security Policy.
 *    These external javascript tags are placed at the bottom of the html page. The scripts are retrieved via the getJs
 *    request handler.
 */
public final class ScriptLinkHelper {

    private static final String MODULE = ScriptLinkHelper.class.getName();
    public static final String FTL_WRITER = "WriterForFTL";
    public static final String SCRIPT_LINKS_FOR_BODY_END = "ScriptLinksForBodyEnd";
    private static int maxScriptCacheSizePerUserSession = 15;
    private static int estimatedConcurrentUserSessions = 250;
    /**
     * Store inline script extracted from freemarker template for a user session.
     * Number of inline scripts for a user session will be constraint by {@link ScriptLinkHelper#maxScriptCacheSizePerUserSession}
     * {@link ScriptLinkHelper#cleanupScriptCache(HttpSession)} will be called to remove entry when session ends.
     */
    private static LinkedHashMap<String, Map<String, String>> scriptCache =
            new LinkedHashMap<String, Map<String, String>>() {
                private static final long serialVersionUID = 1L;
                protected boolean removeEldestEntry(Map.Entry<String, Map<String, String>> eldest) {
                    return size() > estimatedConcurrentUserSessions;
                }
            };

    private ScriptLinkHelper() { }

    /**
     * add script link for page footer.
     * @param request
     * @param filePath
     */
    private static void addScriptLinkForBodyEnd(final HttpServletRequest request, final String filePath) {
        Set<String> scriptLinks = UtilGenerics.cast(request.getAttribute(SCRIPT_LINKS_FOR_BODY_END));
        if (scriptLinks == null) {
            // use of LinkedHashSet to maintain insertion order
            scriptLinks = new LinkedHashSet<>();
            request.setAttribute(SCRIPT_LINKS_FOR_BODY_END, scriptLinks);
        }
        scriptLinks.add(filePath);
    }

    /**
     * get the script links for page footer. Also @see {@link org.apache.ofbiz.webapp.ftl.ScriptTagsFooterTransform}
     * @param request
     * @return
     */
    public static Set<String> getScriptLinksForBodyEnd(HttpServletRequest request) {
        Set<String> scriptLinks = UtilGenerics.cast(request.getAttribute(SCRIPT_LINKS_FOR_BODY_END));
        return scriptLinks;
    }

    /**
     * put script in cache for retrieval by the browser
     * @param context
     * @param fileName
     * @param fileContent
     * @return key used to store the script
     */
    private static String putScriptInCache(Map<String, Object> context, String fileName, String fileContent) {
        HttpSession session = (HttpSession) context.get("session");
        String sessionId = session.getId();
        Map<String, String> scriptMap = UtilGenerics.cast(scriptCache.get(sessionId));
        if (scriptMap == null) {
            // use of LinkedHashMap to limit size of the map
            scriptMap = new LinkedHashMap<String, String>() {
                private static final long serialVersionUID = 1L;
                protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
                    return size() > maxScriptCacheSizePerUserSession;
                }
            };
            scriptCache.put(sessionId, scriptMap);
        }
        String key = fileName;
        if (scriptMap.containsKey(fileName)) {
            int counter = 1;
            key = fileName + "-" + counter;
            while (scriptMap.containsKey(key)) {
                counter++;
                key = fileName + "-" + counter;
            }
        }
        scriptMap.put(key, fileContent);
        return key;
    }

    /**
     * Remove script from cache after reading.
     * @param session
     * @param fileName
     * @return script to be sent back to browser
     */
    public static String getScriptFromCache(HttpSession session, final String fileName) {
        Map<String, String> scriptMap = UtilGenerics.cast(scriptCache.get(session.getId()));
        if (scriptMap != null && scriptMap.containsKey(fileName)) {
            return scriptMap.remove(fileName);
        }
        return "";
    }

    /**
     * cleanup the script cache when user session is invalidated.
     * @param session
     */
    public static void cleanupScriptCache(HttpSession session) {
        scriptCache.remove(session.getId());
    }

    public static String prepareScriptLinkForBodyEnd(HttpServletRequest request, String fileName, String script) {

        Map<String, Object> context = new HashMap<>();
        context.put("session", request.getSession());
        String key = putScriptInCache(context, fileName, script);

        // construct script link
        String contextPath = request.getContextPath();
        String url = null;
        if (SeoConfigUtil.isCategoryUrlEnabled(contextPath)) {
            url = contextPath + "/getJs?name=" + key;
        } else {
            url = contextPath + "/control/getJs?name=" + key;
        }

        // add csrf token to script link
        String tokenValue = CsrfUtil.generateTokenForNonAjax(request, "getJs");
        url = CsrfUtil.addOrUpdateTokenInUrl(url, tokenValue);

        // store script link to be output by scriptTagsFooter freemarker macro
        addScriptLinkForBodyEnd(request, url);

        return "success";
    }
}
