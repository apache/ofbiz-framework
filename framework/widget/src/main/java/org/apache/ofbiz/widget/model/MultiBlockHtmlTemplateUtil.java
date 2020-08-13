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
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.ofbiz.base.util.FileUtil;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.string.FlexibleStringExpander;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

/**
 * Utility to support different handling of code blocks in an html template:
 * 1. external script tags with data-import="head" are removed from the rendered template and merged with
 *    layoutSetting.javaScripts. This helps to keep page-specific external script tags to the html-template that needs it.
 *    In future when the javascript library allows, we can use import module functionality of the browser instead of
 *    special handling at the server side.
 * 2. link tags are removed from the rendered template and merged with layoutSetting.styleSheets.
 *    This helps to keep page-specific link tags to the html-template that needs it.
 * 3. Inline javascript tags are turned into external javascript tags for better compliance of Content Security Policy.
 *    These external javascript tags are placed at the bottom of the html page. The scripts are retrieved via the getJs
 *    request handler.
 */
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
     * In this case, we need to expand the location expression before reading the html-template for any html imports.
     */
    private static LinkedHashMap<String, Set<String>> htmlLinksForScreenCache =
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
    private static LinkedHashMap<String, Set<String>> childScreenCache =
            new LinkedHashMap<String, Set<String>>() {
                private static final long serialVersionUID = 1L;
                protected boolean removeEldestEntry(Map.Entry<String, Set<String>> eldest) {
                    return size() > estimatedScreensWithChildScreen;
                }
            };

    private MultiBlockHtmlTemplateUtil() { }

    /**
     * Allow unit testing to check results
     * @return
     */
    public static Map<String, Set<String>> getChildScreenCache() {
        return Collections.unmodifiableMap(childScreenCache);
    }

    /**
     * Add child screen info to {@link MultiBlockHtmlTemplateUtil#childScreenCache}.
     * @param parentModelScreen parent screen.
     * @param location screen location. Expression is allowed.
     * @param name screen name. Expression is allowed.
     */
    public static void addChildScreen(ModelScreen parentModelScreen, String location, String name) {
        String key = parentModelScreen.getSourceLocation() + "#" + parentModelScreen.getName();
        Set<String> childList = childScreenCache.get(key);
        if (childList == null) {
            childList = new LinkedHashSet<>();
            childScreenCache.put(key, childList);
        }
        if (UtilValidate.isNotEmpty(location)) {
            childList.add(location + "#" + name);
        } else {
            childList.add(parentModelScreen.getSourceLocation() + "#" + name);
        }
    }

    /**
     * Add Html Imports to {@link MultiBlockHtmlTemplateUtil#htmlLinksForScreenCache}.
     * @param location screen location. Expression is not allowed.
     * @param name screen name. Expression is not allowed.
     * @param urls Set of html links associated with the screen. May contain expression to html-template location.
     */
    public static void addHtmlLinksToHtmlLinksForScreenCache(String location, String name, Set<String> urls) {
        if (UtilValidate.isEmpty(urls)) {
            return;
        }
        String locHashName = location + "#" + name;
        Set<String> htmlLinks = htmlLinksForScreenCache.get(locHashName);
        if (htmlLinks == null) {
            htmlLinks = new LinkedHashSet<>();
            htmlLinksForScreenCache.put(locHashName, htmlLinks);
        }
        htmlLinks.addAll(urls);
    }

    /**
     * Get locations for external css link and external script from raw html template
     * @param fileLocation Location to html template. Expression is not allowed.
     * @return
     */
    public static Set<String> extractHtmlLinksFromRawHtmlTemplate(String fileLocation) throws IOException {
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
                        imports.add("script:" + src);
                    }
                }
            }
        }
        Elements csslinkElements = doc.select("link");
        if (csslinkElements != null && csslinkElements.size() > 0) {
            for (org.jsoup.nodes.Element link : csslinkElements) {
                String src = link.attr("href");
                if (UtilValidate.isNotEmpty(src)) {
                    imports.add("link:" + src);
                }
            }
        }
        return imports;
    }

    /**
     * Store the 1st screen called by request.
     * Request attribute HTML_LINKS_FOR_HEAD will be set to a string containing the initial screen location + hash + name
     * @param context
     * @param location screen location. Expression is not allowed.
     * @param name screen name. Expression is not allowed.
     */
    public static void storeScreenLocationName(final Map<String, Object> context, String location, String name) {
        HttpServletRequest request = (HttpServletRequest) context.get("request");
        if (request == null) {
            return;
        }
        if (request.getAttribute(HTML_LINKS_FOR_HEAD) == null) {
            String currentLocationHashName = location + "#" + name;
            request.setAttribute(HTML_LINKS_FOR_HEAD, currentLocationHashName);
        }
    }

    /**
     * Add html links to the header.
     * Request attribute HTML_LINKS_FOR_HEAD will be set to boolean when all html links are added to the 'layoutSettings'.
     * Request attribute HTML_LINKS_FOR_HEAD will be set to Object[] when there are expressions for screen location or
     * template location that need to be expanded, in order to read the content of screen or template.
     * @param context
     * @throws IOException
     */
    public static void addLinksToLayoutSettingsWhenConditionsAreRight(final Map<String, Object> context) throws IOException {
        HttpServletRequest request = (HttpServletRequest) context.get("request");
        if (request == null) {
            return;
        }
        // check "layoutSettings.javaScripts" is not empty
        Map<String, Object> layoutSettings = UtilGenerics.cast(context.get("layoutSettings"));
        if (UtilValidate.isEmpty(layoutSettings)) {
            return;
        }
        List<String> layoutSettingsJavaScripts = UtilGenerics.cast(layoutSettings.get("javaScripts"));
        if (UtilValidate.isEmpty(layoutSettingsJavaScripts)) {
            return;
        }
        List<String> layoutSettingsStyleSheets = UtilGenerics.cast(layoutSettings.get("styleSheets"));
        if (UtilValidate.isEmpty(layoutSettingsStyleSheets)) {
            layoutSettingsStyleSheets = UtilGenerics.cast(layoutSettings.get("VT_STYLESHEET"));
            if (UtilValidate.isEmpty(layoutSettingsStyleSheets)) {
                return;
            }
        }
        // ensure initTheme.groovy has run.
        Map<String, String> commonScreenLocations = UtilGenerics.cast(context.get("commonScreenLocations"));
        if (UtilValidate.isEmpty(commonScreenLocations)) {
            return;
        }
        Locale locale = (Locale) context.get("locale");
        Object objValue = request.getAttribute(HTML_LINKS_FOR_HEAD);
        if (objValue instanceof String) {
            // store expressions for Template Location that is not expanded correctly, for retry.
            Set<String> retryTemplateLocationExpressions = new LinkedHashSet<>();
            // store expressions for Screen Location + Hash + Name that is not expanded correctly, for retry.
            Set<String> retryScreenLocHashNameExpressions = new LinkedHashSet<>();
            String currentLocationHashName = (String) request.getAttribute(HTML_LINKS_FOR_HEAD);
            // store the html links that will be added to 'layoutSettings'
            Set<String> htmlLinks = new LinkedHashSet<>();
            Set<String> locHashNameList = getRelatedScreenLocationHashName(currentLocationHashName, context);
            for (String locHashName:locHashNameList) {
                if (locHashName.contains("${")) {
                    // as getRelatedScreenLocationHashName method has already tried to expand the expression;
                    // just add to the retry variable for the next loop.
                    retryScreenLocHashNameExpressions.add(locHashName);
                    continue;
                }
                // check for any html links associated with the screen
                Set<String> urls = htmlLinksForScreenCache.get(locHashName);
                if (UtilValidate.isNotEmpty(urls)) {
                    for (String url : urls) {
                        // if an expression is found, treat it like template location location, instead of html link
                        if (url.contains("${")) {
                            String expandUrl = FlexibleStringExpander.expandString(url, context);
                            if (UtilValidate.isNotEmpty(expandUrl)) {
                                htmlLinks.addAll(extractHtmlLinksFromRawHtmlTemplate(expandUrl));
                            } else {
                                retryTemplateLocationExpressions.add(url);
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
                addLinksToLayoutSettings(htmlLinks, layoutSettingsJavaScripts, layoutSettingsStyleSheets, locale);
            }
            if (UtilValidate.isEmpty(retryScreenLocHashNameExpressions) && UtilValidate.isEmpty(retryTemplateLocationExpressions)) {
                request.setAttribute(HTML_LINKS_FOR_HEAD, true);
            } else {
                Object[] retry = new Object[2];
                retry[0] = retryScreenLocHashNameExpressions;
                retry[1] = retryTemplateLocationExpressions;
                request.setAttribute(HTML_LINKS_FOR_HEAD, retry);
            }
        } else if (objValue instanceof Object[]) {
            Object[] retry = UtilGenerics.cast(request.getAttribute(HTML_LINKS_FOR_HEAD));
            Set<String> retryScreenLocHashNameExpressions = UtilGenerics.cast(retry[0]);
            Set<String> retryTemplateLocationExpressions = UtilGenerics.cast(retry[1]);
            Set<String> htmlLinks = new HashSet<>();
            if (UtilValidate.isNotEmpty(retryScreenLocHashNameExpressions)) {
                for (Iterator<String> it = retryScreenLocHashNameExpressions.iterator(); it.hasNext();) {
                    String locHashName = it.next();
                    String expandLocHashName = FlexibleStringExpander.expandString(locHashName, context);
                    if (!expandLocHashName.startsWith("#") && !expandLocHashName.endsWith("#")) {
                        Set<String> urls = htmlLinksForScreenCache.get(expandLocHashName);
                        if (UtilValidate.isNotEmpty(urls)) {
                            for (String url : urls) {
                                if (url.contains("${")) {
                                    retryTemplateLocationExpressions.add(url);
                                } else {
                                    if (!htmlLinks.contains(url)) {
                                        htmlLinks.add(url);
                                    }
                                }
                            }
                        }
                        it.remove();
                    }
                }
            }

            if (UtilValidate.isNotEmpty(retryTemplateLocationExpressions)) {
                for (Iterator<String> it = retryTemplateLocationExpressions.iterator(); it.hasNext();) {
                    String url = it.next();
                    // we know url contains "${", so we expand the url
                    String expandUrl = FlexibleStringExpander.expandString(url, context);
                    if (UtilValidate.isNotEmpty(expandUrl)) {
                        htmlLinks.addAll(extractHtmlLinksFromRawHtmlTemplate(expandUrl));
                        it.remove();
                    }

                }
            }
            if (UtilValidate.isNotEmpty(htmlLinks)) {
                addLinksToLayoutSettings(htmlLinks, layoutSettingsJavaScripts, layoutSettingsStyleSheets, locale);
            }
            if (UtilValidate.isEmpty(retryScreenLocHashNameExpressions) && UtilValidate.isEmpty(retryTemplateLocationExpressions)) {
                request.setAttribute(HTML_LINKS_FOR_HEAD, true);
            } else {
                Object[] retry2 = new Object[2];
                retry2[0] = retryScreenLocHashNameExpressions;
                retry2[1] = retryTemplateLocationExpressions;
                request.setAttribute(HTML_LINKS_FOR_HEAD, retry2);
            }
        }
    }

    private static void addLinksToLayoutSettings(Set<String> htmlLinks,
                                                 List<String> layoutSettingsJavaScripts,
                                                 List<String> layoutSettingsStyleSheets, Locale locale) {
        for (String link : htmlLinks) {
            if (link.startsWith("script:")) {
                String url = link.substring(7);
                // check url is not already in layoutSettings.javaScripts
                if (!layoutSettingsJavaScripts.contains(url)) {
                    layoutSettingsJavaScripts.add(url);
                    if (url.contains("select2")) {
                        // find and add select2 language js
                        String localeString = locale.toString();
                        String langJsUrl = org.apache.ofbiz.common.JsLanguageFilesMapping.select2.getFilePath(localeString);
                        if (!layoutSettingsJavaScripts.contains(langJsUrl)) {
                            layoutSettingsJavaScripts.add(langJsUrl);
                        }
                    }
                }
            } else if (link.startsWith("link:")) {
                String url = link.substring(5);
                // check url is not already in layoutSettings.styleSheets
                if (!layoutSettingsStyleSheets.contains(url)) {
                    layoutSettingsStyleSheets.add(url);
                }
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
        Set<String> locHashNameList = childScreenCache.get(locationHashName);
        if (locHashNameList != null) {
            for (String locHashName : locHashNameList) {
                String exLocHashName = "";
                if (locHashName.contains("${")) {
                    exLocHashName = FlexibleStringExpander.expandString(locHashName, context);
                    if (exLocHashName.startsWith("#") || exLocHashName.endsWith("#")) {
                        exLocHashName = locHashName;
                    }
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
     * @param request
     * @param filePath
     */
    public static void addScriptLinkForFoot(final HttpServletRequest request, final String filePath) {
        Set<String> scriptLinks = UtilGenerics.cast(request.getAttribute(SCRIPT_LINKS_FOR_FOOT));
        if (scriptLinks == null) {
            // use of LinkedHashSet to maintain insertion order
            scriptLinks = new LinkedHashSet<>();
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
     * @return key used to store the script
     */
    public static String putScriptInCache(Map<String, Object> context, String fileName, String fileContent) {
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

    /**
     * cleanup the script cache when user session is invalidated.
     * @param session
     */
    public static void cleanupScriptCache(HttpSession session) {
        scriptCache.remove(session.getId());
    }
}