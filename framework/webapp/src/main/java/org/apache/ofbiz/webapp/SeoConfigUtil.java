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
package org.apache.ofbiz.webapp;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.StringUtil;
import org.apache.ofbiz.base.util.UtilURL;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.UtilXml;
import org.apache.oro.text.regex.MalformedPatternException;
import org.apache.oro.text.regex.Pattern;
import org.apache.oro.text.regex.Perl5Compiler;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * SeoConfigUtil - SEO Configuration file utility.
 */
public final class SeoConfigUtil {
    private static final String MODULE = SeoConfigUtil.class.getName();
    private static Perl5Compiler perlCompiler = new Perl5Compiler();
    private static boolean isInitialed = false;
    private static boolean categoryUrlEnabled = true;
    private static boolean categoryNameEnabled = false;
    private static String categoryUrlSuffix = null;
    private static final String DEFAULT_REGEXP = "^.*/.*$";
    private static Pattern regexpIfMatch = null;
    private static boolean useUrlRegexp = false;
    private static boolean jSessionIdAnonEnabled = false;
    private static boolean jSessionIdUserEnabled = false;
    private static Map<String, String> seoReplacements = null;
    private static Map<String, Pattern> seoPatterns = null;
    private static Map<String, String> forwardReplacements = null;
    private static Map<String, Integer> forwardResponseCodes = null;
    private static Map<String, String> charFilters = null;
    private static List<Pattern> userExceptionPatterns = null;
    private static Set<String> allowedContextPaths = null;
    private static Map<String, String> specialProductIds = null;
    private static final String ELEMENT_REGEXPIFMATCH = "regexpifmatch";
    private static final String ELEMENT_URL_CONFIG = "url-config";
    private static final String ELEMENT_FORWARD = "forward";
    private static final String ELEMENT_SEO = "seo";
    private static final String ELEMENT_URLPATTERN = "url-pattern";
    private static final String ELEMENT_REPLACEMENT = "replacement";
    private static final String ELEMENT_RESPONSECODE = "responsecode";
    private static final String ELEMENT_JSESSIONID = "jsessionid";
    private static final String ELEMENT_ANONYMOUS = "anonymous";
    private static final String ELEMENT_VALUE = "value";
    private static final String ELEMENT_USER = "user";
    private static final String ELEMENT_EXCEPTIONS = "exceptions";
    private static final String ELEMENT_CHAR_FILTER = "char-filter";
    private static final String ELEMENT_CHARACTER_PATTERN = "character-pattern";
    private static final String ELEMENT_CATEGORY_URL = "category-url";
    private static final String ELEMENT_ALLOWED_CONTEXT_PATHS = "allowed-context-paths";
    private static final String ELEMENT_CATEGORY_NAME = "category-name";
    private static final String ELEMENT_CATEGORY_URL_SUFFIX = "category-url-suffix";
    private static final String SEO_CONFIG_FILENAME = "SeoConfig.xml";
    private static final int DEFAULT_RESPONSECODE = HttpServletResponse.SC_MOVED_PERMANENTLY;
    private static final String DEFAULT_ANONYMOUS_VALUE = "disable";
    private static final String DEFAULT_USER_VALUE = "disable";
    private static final String DEFAULT_CATEGORY_URL_VALUE = "enable";
    private static final String DEFAULT_CATEGORY_NAME_VALUE = "disable";
    private static final String ALLOWED_CONTEXT_PATHS_SEPERATOR = ":";

    private SeoConfigUtil() { }

    /**
     * Initialize url regular express configuration.
     */
    public static void init() {
        String result = "success";
        seoPatterns = new HashMap<>();
        seoReplacements = new HashMap<>();
        forwardReplacements = new HashMap<>();
        forwardResponseCodes = new HashMap<>();
        userExceptionPatterns = new LinkedList<>();
        specialProductIds = new HashMap<>();
        charFilters = new LinkedHashMap<>();
        try {
            URL seoConfigFilename = UtilURL.fromResource(SEO_CONFIG_FILENAME);
            Document configDoc = UtilXml.readXmlDocument(seoConfigFilename, false);
            Element rootElement = configDoc.getDocumentElement();

            String regexIfMatch = UtilXml.childElementValue(rootElement, ELEMENT_REGEXPIFMATCH, DEFAULT_REGEXP);
            Debug.logInfo("Parsing " + regexIfMatch, MODULE);
            try {
                regexpIfMatch = perlCompiler.compile(regexIfMatch, Perl5Compiler.READ_ONLY_MASK);
            } catch (MalformedPatternException e1) {
                Debug.logWarning(e1, "Error while parsing " + regexIfMatch, MODULE);
            }

            // parse category-url element
            try {
                Element categoryUrlElement = UtilXml.firstChildElement(rootElement, ELEMENT_CATEGORY_URL);
                Debug.logInfo("Parsing " + ELEMENT_CATEGORY_URL + " [" + (categoryUrlElement != null) + "]:", MODULE);
                if (categoryUrlElement != null) {
                    String enableCategoryUrlValue = UtilXml.childElementValue(categoryUrlElement, ELEMENT_VALUE, DEFAULT_CATEGORY_URL_VALUE);
                    if (DEFAULT_CATEGORY_URL_VALUE.equalsIgnoreCase(enableCategoryUrlValue)) {
                        categoryUrlEnabled = true;
                    } else {
                        categoryUrlEnabled = false;
                    }

                    if (categoryUrlEnabled) {
                        String allowedContextValue = UtilXml.childElementValue(categoryUrlElement, ELEMENT_ALLOWED_CONTEXT_PATHS, null);
                        allowedContextPaths = new HashSet<>();
                        if (UtilValidate.isNotEmpty(allowedContextValue)) {
                            List<String> allowedContextPathList = StringUtil.split(allowedContextValue, ALLOWED_CONTEXT_PATHS_SEPERATOR);
                            for (String path : allowedContextPathList) {
                                if (UtilValidate.isNotEmpty(path)) {
                                    path = path.trim();
                                    if (!allowedContextPaths.contains(path)) {
                                        allowedContextPaths.add(path);
                                        Debug.logInfo("  " + ELEMENT_ALLOWED_CONTEXT_PATHS + ": " + path, MODULE);
                                    }
                                }
                            }
                        }

                        String categoryNameValue = UtilXml.childElementValue(categoryUrlElement, ELEMENT_CATEGORY_NAME, DEFAULT_CATEGORY_NAME_VALUE);
                        if (DEFAULT_CATEGORY_NAME_VALUE.equalsIgnoreCase(categoryNameValue)) {
                            categoryNameEnabled = false;
                        } else {
                            categoryNameEnabled = true;
                        }
                        Debug.logInfo("  " + ELEMENT_CATEGORY_NAME + ": " + categoryNameEnabled, MODULE);

                        categoryUrlSuffix = UtilXml.childElementValue(categoryUrlElement, ELEMENT_CATEGORY_URL_SUFFIX, null);
                        if (UtilValidate.isNotEmpty(categoryUrlSuffix)) {
                            categoryUrlSuffix = categoryUrlSuffix.trim();
                            if (categoryUrlSuffix.contains("/")) {
                                categoryUrlSuffix = null;
                            }
                        }
                        Debug.logInfo("  " + ELEMENT_CATEGORY_URL_SUFFIX + ": " + categoryUrlSuffix, MODULE);
                    }
                }
            } catch (NullPointerException e) {
                // no "category-url" element
                Debug.logWarning("No category-url element found in " + seoConfigFilename.toString(), MODULE);
            }

            // parse jsessionid element
            try {
                Element jSessionId = UtilXml.firstChildElement(rootElement, ELEMENT_JSESSIONID);
                Debug.logInfo("Parsing " + ELEMENT_JSESSIONID + " [" + (jSessionId != null) + "]:", MODULE);
                if (jSessionId != null) {
                    Element anonymous = UtilXml.firstChildElement(jSessionId, ELEMENT_ANONYMOUS);
                    if (anonymous != null) {
                        String anonymousValue = UtilXml.childElementValue(anonymous, ELEMENT_VALUE, DEFAULT_ANONYMOUS_VALUE);
                        if (DEFAULT_ANONYMOUS_VALUE.equalsIgnoreCase(anonymousValue)) {
                            jSessionIdAnonEnabled = false;
                        } else {
                            jSessionIdAnonEnabled = true;
                        }
                    } else {
                        jSessionIdAnonEnabled = Boolean.valueOf(DEFAULT_ANONYMOUS_VALUE);
                    }
                    Debug.logInfo("  " + ELEMENT_ANONYMOUS + ": " + jSessionIdAnonEnabled, MODULE);
                    Element user = UtilXml.firstChildElement(jSessionId, ELEMENT_USER);
                    if (user != null) {
                        String userValue = UtilXml.childElementValue(user, ELEMENT_VALUE, DEFAULT_USER_VALUE);
                        if (DEFAULT_USER_VALUE.equalsIgnoreCase(userValue)) {
                            jSessionIdUserEnabled = false;
                        } else {
                            jSessionIdUserEnabled = true;
                        }

                        Element exceptions = UtilXml.firstChildElement(user, ELEMENT_EXCEPTIONS);
                        if (exceptions != null) {
                            Debug.logInfo("  " + ELEMENT_EXCEPTIONS + ": ", MODULE);
                            List<? extends Element> exceptionUrlPatterns = UtilXml.childElementList(exceptions, ELEMENT_URLPATTERN);
                            for (int i = 0; i < exceptionUrlPatterns.size(); i++) {
                                Element element = exceptionUrlPatterns.get(i);
                                String urlpattern = element.getTextContent();
                                if (UtilValidate.isNotEmpty(urlpattern)) {
                                    try {
                                        Pattern pattern = perlCompiler.compile(urlpattern, Perl5Compiler.READ_ONLY_MASK);
                                        userExceptionPatterns.add(pattern);
                                        Debug.logInfo("    " + ELEMENT_URLPATTERN + ": " + urlpattern, MODULE);
                                    } catch (MalformedPatternException e) {
                                        Debug.logWarning("Can NOT parse " + urlpattern + " in element "
                                                + ELEMENT_URLPATTERN + " of " + ELEMENT_EXCEPTIONS + ". Error: "
                                                + e.getMessage(), MODULE);
                                    }
                                }
                            }
                        }
                    } else {
                        jSessionIdUserEnabled = Boolean.valueOf(DEFAULT_USER_VALUE);
                    }
                    Debug.logInfo("  " + ELEMENT_USER + ": " + jSessionIdUserEnabled, MODULE);
                }
            } catch (NullPointerException e) {
                Debug.logWarning("No jsessionid element found in " + seoConfigFilename.toString(), MODULE);
            }
            // parse url-config elements
            try {
                NodeList configs = rootElement.getElementsByTagName(ELEMENT_URL_CONFIG);
                Debug.logInfo("Parsing " + ELEMENT_URL_CONFIG, MODULE);
                int length = configs.getLength();
                for (int j = 0; j < length; j++) {
                    Element config = (Element) configs.item(j);
                    String urlpattern = UtilXml.childElementValue(config, ELEMENT_URLPATTERN, null);
                    if (UtilValidate.isEmpty(urlpattern)) {
                        continue;
                    }
                    Debug.logInfo("  " + ELEMENT_URLPATTERN + ": " + urlpattern, MODULE);
                    Pattern pattern;
                    try {
                        pattern = perlCompiler.compile(urlpattern, Perl5Compiler.READ_ONLY_MASK);
                        seoPatterns.put(urlpattern, pattern);
                    } catch (MalformedPatternException e) {
                        Debug.logWarning("Error while creating parttern for seo url-pattern: " + urlpattern, MODULE);
                        continue;
                    }
                    // construct seo patterns
                    Element seo = UtilXml.firstChildElement(config, ELEMENT_SEO);
                    if (UtilValidate.isNotEmpty(seo)) {
                        String replacement = UtilXml.childElementValue(seo, ELEMENT_REPLACEMENT, null);
                        if (UtilValidate.isNotEmpty(replacement)) {
                            seoReplacements.put(urlpattern, replacement);
                            Debug.logInfo("    " + ELEMENT_SEO + " " + ELEMENT_REPLACEMENT + ": " + replacement, MODULE);
                        }
                    }

                    // construct forward patterns
                    Element forward = UtilXml.firstChildElement(config, ELEMENT_FORWARD);
                    if (UtilValidate.isNotEmpty(forward)) {
                        String replacement = UtilXml.childElementValue(forward, ELEMENT_REPLACEMENT, null);
                        String responseCode = UtilXml.childElementValue(forward,
                                ELEMENT_RESPONSECODE, String.valueOf(DEFAULT_RESPONSECODE));
                        if (UtilValidate.isNotEmpty(replacement)) {
                            forwardReplacements.put(urlpattern, replacement);
                            Debug.logInfo("    " + ELEMENT_FORWARD + " " + ELEMENT_REPLACEMENT + ": " + replacement, MODULE);
                            if (UtilValidate.isNotEmpty(responseCode)) {
                                Integer responseCodeInt = DEFAULT_RESPONSECODE;
                                try {
                                    responseCodeInt = Integer.valueOf(responseCode);
                                } catch (NumberFormatException nfe) {
                                    Debug.logWarning(nfe, "Error while parsing response code number: " + responseCode, MODULE);
                                }
                                forwardResponseCodes.put(urlpattern, responseCodeInt);
                                Debug.logInfo("    " + ELEMENT_FORWARD + " " + ELEMENT_RESPONSECODE + ": " + responseCodeInt, MODULE);
                            }
                        }
                    }
                }
            } catch (NullPointerException e) {
                // no "url-config" element
                Debug.logWarning("No " + ELEMENT_URL_CONFIG + " element found in " + seoConfigFilename.toString(), MODULE);
            }

            // parse char-filters elements
            try {
                NodeList nameFilterNodes = rootElement
                        .getElementsByTagName(ELEMENT_CHAR_FILTER);
                Debug.logInfo("Parsing " + ELEMENT_CHAR_FILTER + ": ", MODULE);
                int length = nameFilterNodes.getLength();
                for (int i = 0; i < length; i++) {
                    Element element = (Element) nameFilterNodes.item(i);
                    String charaterPattern = UtilXml.childElementValue(element, ELEMENT_CHARACTER_PATTERN, null);
                    String replacement = UtilXml.childElementValue(element, ELEMENT_REPLACEMENT, null);
                    if (UtilValidate.isNotEmpty(charaterPattern)
                            && UtilValidate.isNotEmpty(replacement)) {
                        try {
                            perlCompiler.compile(charaterPattern, Perl5Compiler.READ_ONLY_MASK);
                            charFilters.put(charaterPattern, replacement);
                            Debug.logInfo("  " + ELEMENT_CHARACTER_PATTERN + ": " + charaterPattern, MODULE);
                            Debug.logInfo("  " + ELEMENT_REPLACEMENT + ": " + replacement, MODULE);
                        } catch (MalformedPatternException e) {
                            // skip this filter (character-pattern replacement) if any error happened
                            Debug.logWarning(e, "Error while parsing " + ELEMENT_CHARACTER_PATTERN + ": " + charaterPattern, MODULE);
                        }
                    }
                }
            } catch (NullPointerException e) {
                // no "char-filters" element
                Debug.logWarning("No " + ELEMENT_CHAR_FILTER + " element found in " + seoConfigFilename.toString(), MODULE);
            }
        } catch (SAXException | IOException | ParserConfigurationException e) {
            result = "error";
            Debug.logError(e, MODULE);
        }
        if (seoReplacements.keySet().isEmpty()) {
            useUrlRegexp = false;
        } else {
            useUrlRegexp = true;
        }
        if ("success".equals(result)) {
            isInitialed = true;
        }
    }
    /**
     * Check whether the configuration file has been read.
     * @return a boolean value to indicate whether the configuration file has been read.
     */
    public static boolean isInitialed() {
        return isInitialed;
    }
    /**
     * Check whether url regexp should be used.
     * @return a boolean value to indicate whether url regexp should be used.
     */
    public static boolean checkUseUrlRegexp() {
        return useUrlRegexp;
    }
    /**
     * Get the general regexp pattern.
     * @return the general regexp pattern.
     */
    public static Pattern getGeneralRegexpPattern() {
        return regexpIfMatch;
    }

    /**
     * Check whether category url is enabled.
     * @return a boolean value to indicate whether category url is enabled.
     */
    public static boolean checkCategoryUrl() {
        return categoryUrlEnabled;
    }

    /**
     * Check whether the context path is enabled.
     * @return a boolean value to indicate whether the context path is enabled.
     */
    public static boolean isCategoryUrlEnabled(String contextPath) {
        if (contextPath == null) {
            return false;
        }
        if (UtilValidate.isEmpty(contextPath)) {
            contextPath = "/";
        }
        if (categoryUrlEnabled) {
            return allowedContextPaths.contains(contextPath.trim());
        }
        return false;
    }

    /**
     * Check whether category name is enabled.
     * @return a boolean value to indicate whether category name is enabled.
     */
    public static boolean isCategoryNameEnabled() {
        return categoryNameEnabled;
    }

    /**
     * Get category url suffix.
     * @return String category url suffix.
     */
    public static String getCategoryUrlSuffix() {
        return categoryUrlSuffix;
    }

    /**
     * Check whether jsessionid is enabled for anonymous.
     * @return a boolean value to indicate whether jsessionid is enabled for anonymous.
     */
    public static boolean isJSessionIdAnonEnabled() {
        return jSessionIdAnonEnabled;
    }

    /**
     * Check whether jsessionid is enabled for user.
     * @return a boolean value to indicate whether jsessionid is enabled for user.
     */
    public static boolean isJSessionIdUserEnabled() {
        return jSessionIdUserEnabled;
    }

    /**
     * Get user exception url pattern configures.
     * @return user exception url pattern configures (java.util.List&lt;Pattern&gt;)
     */
    public static List<Pattern> getUserExceptionPatterns() {
        return userExceptionPatterns;
    }

    /**
     * Get char filters.
     * @return char filters (java.util.Map&lt;String, String&gt;)
     */
    public static Map<String, String> getCharFilters() {
        return charFilters;
    }

    /**
     * Get seo url pattern configures.
     * @return seo url pattern configures (java.util.Map&lt;String, Pattern&gt;)
     */
    public static Map<String, Pattern> getSeoPatterns() {
        return seoPatterns;
    }

    /**
     * Get seo replacement configures.
     * @return seo replacement configures (java.util.Map&lt;String, String&gt;)
     */
    public static Map<String, String> getSeoReplacements() {
        return seoReplacements;
    }

    /**
     * Get forward replacement configures.
     * @return forward replacement configures (java.util.Map&lt;String, String&gt;)
     */
    public static Map<String, String> getForwardReplacements() {
        return forwardReplacements;
    }

    /**
     * Get forward response codes.
     * @return forward response code configures (java.util.Map&lt;String, Integer&gt;)
     */
    public static Map<String, Integer> getForwardResponseCodes() {
        return forwardResponseCodes;
    }

    /**
     * Check whether a product id is in the special list. If we cannot get a product from a lower cased
     * or upper cased product id, then it's special.
     * @return boolean to indicate whether the product id is special.
     */
    @Deprecated
    public static boolean isSpecialProductId(String productId) {
        return specialProductIds.containsKey(productId);
    }

    /**
     * Add a special product id to the special list.
     * @param productId a product id get from database.
     * @return true to indicate it has been added to special product id; false to indicate it's not special.
     * @throws Exception to indicate there's already same lower cased product id in the list but value is a different product id.
     */
    @Deprecated
    public static boolean addSpecialProductId(String productId) throws Exception {
        if (productId.toLowerCase(Locale.getDefault()).equals(productId) || productId.toUpperCase(Locale.getDefault()).equals(productId)) {
            return false;
        }
        if (isSpecialProductId(productId.toLowerCase(Locale.getDefault()))) {
            if (specialProductIds.containsValue(productId)) {
                return true;
            } else {
                throw new Exception("This product Id cannot be lower cased for SEO URL purpose: " + productId);
            }
        }
        specialProductIds.put(productId.toLowerCase(Locale.getDefault()), productId);
        return true;
    }

    /**
     * Get a product id is in the special list.
     * @return String of the original product id
     */
    @Deprecated
    public static String getSpecialProductId(String productId) {
        return specialProductIds.get(productId);
    }

    public static int getDefaultResponseCode() {
        return DEFAULT_RESPONSECODE;
    }
}
