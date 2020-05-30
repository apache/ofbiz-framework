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

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilValidate;

public final class MultiBlockHtmlTemplateUtil {

    public static final String MULTI_BLOCK_WRITER = "multiBlockWriter";
    private static final String HTML_LINKS_FOR_HEAD = "htmlLinksForHead";
    private static final String SCRIPT_LINKS_FOR_FOOT = "ScriptLinksForFoot";
    private static int maxScriptCacheSizePerSession = 10;
    // store inline script from freemarker template by user session
    private static LinkedHashMap<String, Map<String, String>> scriptCache =
            new LinkedHashMap<String, Map<String, String>>() {
                private static final long serialVersionUID = 1L;
                protected boolean removeEldestEntry(Map.Entry<String, Map<String, String>> eldest) {
                    return size() > 100; // TODO probably set to max number of concurrent user
                }
            };
    // store the additional html import by screen location
    private static LinkedHashMap<String, Set<String>> htmlImportCache =
            new LinkedHashMap<String, Set<String>>() {
                private static final long serialVersionUID = 1L;
                protected boolean removeEldestEntry(Map.Entry<String, Set<String>> eldest) {
                    return size() > 300; // TODO probably set to max number of screens
                }
            };
    // store the child screen
    private static LinkedHashMap<String, Set<String>> dependentScreenCache =
            new LinkedHashMap<String, Set<String>>() {
                private static final long serialVersionUID = 1L;
                protected boolean removeEldestEntry(Map.Entry<String, Set<String>> eldest) {
                    return size() > 100;
                }
            };

    private MultiBlockHtmlTemplateUtil() { }

    public static void collectChildScreenInfo(ModelScreen parentModelScreen, String location, String name) {
        String key = parentModelScreen.getSourceLocation() + "#" + parentModelScreen.getName();
        Set<String> childList = dependentScreenCache.get(key);
        if (childList == null) {
            childList = new LinkedHashSet<>();
            dependentScreenCache.put(key, childList);
        }
        if (UtilValidate.isNotEmpty(location)) {
            childList.add(location + "#" + name);
        } else {
            childList.add(parentModelScreen.getSourceLocation() + "#" + name);
        }
    }

    public static void addLinksToHtmlImportCache(String location, String name, Set<String> urls) throws Exception {
        if (UtilValidate.isEmpty(urls)) {
            return;
        }
        String locHashName = location + "#" + name;
        Set<String> existingUrls = htmlImportCache.get(locHashName);
        if (existingUrls == null) {
            existingUrls = new LinkedHashSet<>();
            htmlImportCache.put(locHashName, existingUrls);
        }
        existingUrls.addAll(urls);
    }

    public static void addLinksToLayoutSettings(final Map<String, Object> context, String location, String name) throws Exception {
        HttpServletRequest request = (HttpServletRequest) context.get("request");
        if (request.getAttribute(HTML_LINKS_FOR_HEAD) == null) {
            String currentLocationHashName = location + "#" + name;
            request.setAttribute(HTML_LINKS_FOR_HEAD, currentLocationHashName);
            return;
        }
        // check "layoutSettings.javaScripts" is not empty
        Map<String, Object> layoutSettings = UtilGenerics.cast(context.get("layoutSettings"));
        if (UtilValidate.isEmpty(layoutSettings)) {
            return;
        }
        List<String> layoutSettingsJsList = UtilGenerics.cast(layoutSettings.get("javaScripts"));
        if (UtilValidate.isEmpty(layoutSettingsJsList)) {
            return;
        }
        Object objValue = request.getAttribute(HTML_LINKS_FOR_HEAD);
        if (objValue instanceof String) {
            String currentLocationHashName = (String) request.getAttribute(HTML_LINKS_FOR_HEAD);
            Set<String> htmlLinks = new LinkedHashSet<>();
            Set<String> locHashNameList = getRelatedScreenLocationHashName(currentLocationHashName);
            for (String locHashName:locHashNameList) {
                Set<String> urls = htmlImportCache.get(locHashName);
                if (UtilValidate.isNotEmpty(urls)) {
                    // check url is not already in layoutSettings.javaScripts
                    for (String url : urls) {
                        if (!htmlLinks.contains(url)) {
                            htmlLinks.add(url);
                        }
                    }
                }
            }
            if (UtilValidate.isNotEmpty(htmlLinks)) {
                // check url is not already in layoutSettings.javaScripts
                for (String url : htmlLinks) {
                    if (!layoutSettingsJsList.contains(url)) {
                        layoutSettingsJsList.add(url);
                    }
                }
            }
            request.setAttribute(HTML_LINKS_FOR_HEAD, true);
        }

    }

    /**
     * Get all the child screens including itself
     * @param locationHashName
     * @return
     */
    private static Set<String> getRelatedScreenLocationHashName(String locationHashName) {
        Set<String> resultList = new HashSet<>();
        resultList.add(locationHashName);
        Set<String> locHashNameList = dependentScreenCache.get(locationHashName);
        if (locHashNameList != null) {
            for (String locHashName : locHashNameList) {
                resultList.addAll(getRelatedScreenLocationHashName(locHashName));
            }
        }
        return resultList;
    }

    /**
     * add the script links that should be in the head tag
     * @param context
     * @param urls
     */
    private static void addJsLinkToLayoutSettings(final Map<String, Object> context, final Set<String> urls) {

    }

    /**
     * add script link for page footer.
     * @param context
     * @param filePath
     */
    public static void addScriptLinkForFoot(final Map<String, Object> context, final String filePath) {
        HttpServletRequest request = (HttpServletRequest) context.get("request");
        Set<String> scriptLinks = UtilGenerics.cast(request.getAttribute(SCRIPT_LINKS_FOR_FOOT));
        if (scriptLinks == null) {
            // use of LinkedHashSet to maintain insertion order
            scriptLinks = new LinkedHashSet<String>();
            request.setAttribute(SCRIPT_LINKS_FOR_FOOT, scriptLinks);
        }
        scriptLinks.add(filePath);
    }

    /**
     * get the script links for page footer. Also @see {@link org.apache.ofbiz.webapp.ftl.ScriptTagsFooterTransform}
     * @param request
     * @return
     */
    public static Set<String> getScriptLinksForFoot(HttpServletRequest request) {
        Set<String> scriptLinks = UtilGenerics.cast(request.getAttribute(SCRIPT_LINKS_FOR_FOOT));
        return scriptLinks;
    }

    /**
     * put script in cache for retrieval by the browser
     * @param context
     * @param fileName
     * @param fileContent
     */
    public static void putScriptInCache(Map<String, Object> context, String fileName, String fileContent) {
        HttpSession session = (HttpSession) context.get("session");
        String sessionId = session.getId();
        Map<String, String> scriptMap = UtilGenerics.cast(scriptCache.get(sessionId));
        if (scriptMap == null) {
            // use of LinkedHashMap to limit size of the map
            scriptMap = new LinkedHashMap<String, String>() {
                private static final long serialVersionUID = 1L;
                protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
                    return size() > maxScriptCacheSizePerSession;
                }
            };
            scriptCache.put(sessionId, scriptMap);
        }
        scriptMap.put(fileName, fileContent);
    }

    /**
     * Get the script stored in cache.
     * @param session
     * @param fileName
     * @return script to be sent back to browser
     */
    public static String getScriptFromCache(HttpSession session, final String fileName) {
        Map<String, String> scriptMap = UtilGenerics.cast(scriptCache.get(session.getId()));
        if (scriptMap != null) {
            return scriptMap.get(fileName);
        }
        return "";
    }

    public static void cleanupScriptCache(HttpSession session) {
        scriptCache.remove(session.getId());
    }
}
