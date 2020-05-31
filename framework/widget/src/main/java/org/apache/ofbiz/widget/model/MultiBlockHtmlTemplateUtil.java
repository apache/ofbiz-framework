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

import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.FileUtil;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.string.FlexibleStringExpander;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

public final class MultiBlockHtmlTemplateUtil {

    private static final String MODULE = MultiBlockHtmlTemplateUtil.class.getName();
    public static final String MULTI_BLOCK_WRITER = "multiBlockWriter";
    private static final String HTML_LINKS_FOR_HEAD = "htmlLinksForHead";
    private static final String SCRIPT_LINKS_FOR_FOOT = "ScriptLinksForFoot";
    private static int maxScriptCacheSizePerUserSession = 10;
    private static int estimatedConcurrentUserSessions = 250;
    private static int estimatedScreensWithMultiBlockHtmlTemplate = 200;
    private static int estimatedScreensWithChildScreen = 200;
    /**
     * Store inline script extracted from freemarker template for a user session.
     * Number of inline scripts for a user session will be constraint by {@link MultiBlockHtmlTemplateUtil#maxScriptCacheSizePerUserSession}
     * {@link MultiBlockHtmlTemplateUtil#cleanupScriptCache(HttpSession)} will be called to remove entry when session ends.
     */
    private static LinkedHashMap<String, Map<String, String>> scriptCache =
            new LinkedHashMap<String, Map<String, String>>() {
                private static final long serialVersionUID = 1L;
                protected boolean removeEldestEntry(Map.Entry<String, Map<String, String>> eldest) {
                    return size() > estimatedConcurrentUserSessions;
                }
            };
    /**
     * For each screen containing html-template, store a set of html imports headerized from html-template.
     * The set may contain entry of an expression of the the html-template's location.
     * In this case, we need to expand the location expression, read the html-template for any html imports.
     */
    private static LinkedHashMap<String, Set<String>> htmlImportCache =
            new LinkedHashMap<String, Set<String>>() {
                private static final long serialVersionUID = 1L;
                protected boolean removeEldestEntry(Map.Entry<String, Set<String>> eldest) {
                    return size() > estimatedScreensWithMultiBlockHtmlTemplate;
                }
            };
    /**
     * Store set of dependent screens info, in form of screen location + hash + name, of a given parent screen.
     * The set may contain entry of an expression of the dependent screen location and will need to be expanded before use.
     */
    private static LinkedHashMap<String, Set<String>> dependentScreenCache =
            new LinkedHashMap<String, Set<String>>() {
                private static final long serialVersionUID = 1L;
                protected boolean removeEldestEntry(Map.Entry<String, Set<String>> eldest) {
                    return size() > estimatedScreensWithChildScreen;
                }
            };

    private MultiBlockHtmlTemplateUtil() { }

    /**
     * Add child screen info to {@link MultiBlockHtmlTemplateUtil#dependentScreenCache}.
     * @param parentModelScreen parent screen.
     * @param location screen location. Expression is allowed.
     * @param name screen name. Expression is allowed.
     */
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

    /**
     * Add Html Imports to {@link MultiBlockHtmlTemplateUtil#htmlImportCache}.
     * @param location screen location. Expression is not allowed.
     * @param name screen name. Expression is not allowed.
     * @param urls Set of html links associated with the screen. May contain expression to html-template location.
     * @throws Exception
     */
    public static void addLinksToHtmlImportCache(String location, String name, Set<String> urls) {
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

    /**
     * Get html import scr location from html template
     * @param fileLocation Location to html template. Expression is not allowed.
     * @return
     */
    public static Set<String> getHtmlImportsFromHtmlTemplate(String fileLocation) throws IOException {
        Set<String> imports = new LinkedHashSet<>();
        String template = FileUtil.readString("UTF-8", FileUtil.getFile(fileLocation));
        Document doc = Jsoup.parseBodyFragment(template);
        Elements scriptElements = doc.select("script");
        if (scriptElements != null && scriptElements.size() > 0) {
            for (org.jsoup.nodes.Element script : scriptElements) {
                String src = script.attr("src");
                if (UtilValidate.isNotEmpty(src)) {
                    String dataImport = script.attr("data-import");
                    if ("head".equals(dataImport)) {
                        imports.add(src);
                    }
                }
            }
        }
        return imports;
    }

    /**
     * Store the 1st screen called by request
     * @param context
     * @param location screen location. Expression is not allowed.
     * @param name screen name. Expression is not allowed.
     */
    public static void storeScreenLocationName(final Map<String, Object> context, String location, String name) {
        HttpServletRequest request = (HttpServletRequest) context.get("request");
        if (request.getAttribute(HTML_LINKS_FOR_HEAD) == null) {
            String currentLocationHashName = location + "#" + name;
            request.setAttribute(HTML_LINKS_FOR_HEAD, currentLocationHashName);
        }
    }

    /**
     * Add html links to the header
     * @param context
     * @throws Exception
     */
    public static void addLinksToLayoutSettings(final Map<String, Object> context) throws IOException {
        HttpServletRequest request = (HttpServletRequest) context.get("request");

        // check "layoutSettings.javaScripts" is not empty
        Map<String, Object> layoutSettings = UtilGenerics.cast(context.get("layoutSettings"));
        if (UtilValidate.isEmpty(layoutSettings)) {
            return;
        }
        List<String> layoutSettingsJsList = UtilGenerics.cast(layoutSettings.get("javaScripts"));
        if (UtilValidate.isEmpty(layoutSettingsJsList)) {
            return;
        }
        // ensure initTheme.groovy has run.
        Map<String, String> commonScreenLocations = UtilGenerics.cast(context.get("commonScreenLocations"));
        if (UtilValidate.isEmpty(commonScreenLocations)) {
            return;
        }
        Object objValue = request.getAttribute(HTML_LINKS_FOR_HEAD);
        if (objValue instanceof String) {
            Set<String> retryHtmlLinks = new LinkedHashSet<>();
            String currentLocationHashName = (String) request.getAttribute(HTML_LINKS_FOR_HEAD);
            Set<String> htmlLinks = new LinkedHashSet<>();
            Set<String> locHashNameList = getRelatedScreenLocationHashName(currentLocationHashName, context);
            for (String locHashName:locHashNameList) {
                String expandLocHashName = "";
                if (locHashName.contains("${")) {
                    expandLocHashName = FlexibleStringExpander.expandString(locHashName, context);
                } else {
                    expandLocHashName = locHashName;
                }
                Set<String> urls = htmlImportCache.get(expandLocHashName);
                if (UtilValidate.isNotEmpty(urls)) {
                    for (String url : urls) {
                        if (url.contains("${")) {
                            String expandUrl = FlexibleStringExpander.expandString(url, context);
                            if (UtilValidate.isNotEmpty(expandUrl)) {
                                htmlLinks.addAll(getHtmlImportsFromHtmlTemplate(expandUrl));
                            } else {
                                retryHtmlLinks.add(url);
                                Debug.log("Unable to expand " + url, MODULE);
                            }
                        } else {
                            if (!htmlLinks.contains(url)) {
                                htmlLinks.add(url);
                            }
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
            if (UtilValidate.isEmpty(retryHtmlLinks)) {
                request.setAttribute(HTML_LINKS_FOR_HEAD, true);
            }
        }

    }

    /**
     * Get all the child screens including itself
     * @param locationHashName
     * @return
     */
    private static Set<String> getRelatedScreenLocationHashName(String locationHashName, final Map<String, Object> context) {
        Set<String> resultList = new HashSet<>();
        resultList.add(locationHashName);
        Set<String> locHashNameList = dependentScreenCache.get(locationHashName);
        if (locHashNameList != null) {
            for (String locHashName : locHashNameList) {
                String exLocHashName = "";
                if (locHashName.contains("${")) {
                    exLocHashName = FlexibleStringExpander.expandString(locHashName, context);
                } else {
                    exLocHashName = locHashName;
                }
                resultList.addAll(getRelatedScreenLocationHashName(exLocHashName, context));
            }
        }
        return resultList;
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
                    return size() > maxScriptCacheSizePerUserSession;
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
