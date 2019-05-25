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
package org.apache.ofbiz.base.util;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.ofbiz.base.html.SanitizerCustomPolicy;
import org.owasp.esapi.codecs.Codec;
import org.owasp.esapi.codecs.HTMLEntityCodec;
import org.owasp.esapi.codecs.PercentCodec;
import org.owasp.esapi.codecs.XMLEntityCodec;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.owasp.html.Sanitizers;

public class UtilCodec {
    private static final String module = UtilCodec.class.getName();
    private static final HtmlEncoder htmlEncoder = new HtmlEncoder();
    private static final XmlEncoder xmlEncoder = new XmlEncoder();
    private static final StringEncoder stringEncoder = new StringEncoder();
    private static final UrlCodec urlCodec = new UrlCodec();
    private static final List<Codec> codecs;
    // From https://www.owasp.org/index.php/XSS_Filter_Evasion_Cheat_Sheet#Event_Handlers 
    private static final List<String> jsEventList = Arrays.asList(new String[] { "onAbort", "onActivate",
            "onAfterPrint", "onAfterUpdate", "onBeforeActivate", "onBeforeCopy", "onBeforeCut", "onBeforeDeactivate",
            "onBeforeEditFocus", "onBeforePaste", "onBeforePrint", "onBeforeUnload", "onBeforeUpdate", "onBegin",
            "onBlur", "onBounce", "onCellChange", "onChange", "onClick", "onContextMenu", "onControlSelect", "onCopy",
            "onCut", "onDataAvailable", "onDataSetChanged", "onDataSetComplete", "onDblClick", "onDeactivate", "onDrag",
            "onDragEnd", "onDragLeave", "onDragEnter", "onDragOver", "onDragDrop", "onDragStart", "onDrop", "onEnd",
            "onError", "onErrorUpdate", "onFilterChange", "onFinish", "onFocus", "onFocusIn", "onFocusOut",
            "onHashChange", "onHelp", "onInput", "onKeyDown", "onKeyPress", "onKeyUp", "onLayoutComplete", "onLoad",
            "onLoseCapture", "onMediaComplete", "onMediaError", "onMessage", "onMouseDown", "onMouseEnter",
            "onMouseLeave", "onMouseMove", "onMouseOut", "onMouseOver", "onMouseUp", "onMouseWheel", "onMove",
            "onMoveEnd", "onMoveStart", "onOffline", "onOnline", "onOutOfSync", "onPaste", "onPause", "onPopState",
            "onProgress", "onPropertyChange", "onReadyStateChange", "onRedo", "onRepeat", "onReset", "onResize",
            "onResizeEnd", "onResizeStart", "onResume", "onReverse", "onRowsEnter", "onRowExit", "onRowDelete",
            "onRowInserted", "onScroll", "onSeek", "onSelect", "onSelectionChange", "onSelectStart", "onStart",
            "onStop", "onStorage", "onSyncRestored", "onSubmit", "onTimeError", "onTrackChange", "onUndo", "onUnload",
            "onURLFlip", "seekSegmentTime" });

    static {
        List<Codec> tmpCodecs = new ArrayList<>();
        tmpCodecs.add(new HTMLEntityCodec());
        tmpCodecs.add(new PercentCodec());
        codecs = Collections.unmodifiableList(tmpCodecs);
    }

    @SuppressWarnings("serial")
    public static class IntrusionException extends GeneralRuntimeException {
        public IntrusionException(String message) {
            super(message);
        }
    }

    public static interface SimpleEncoder {
        public String encode(String original);
        /**
         * @deprecated Use {@link #sanitize(String,String)} instead
         */
        @Deprecated
        public String sanitize(String outString); // Only really useful with HTML, else it simply calls encode() method
        public String sanitize(String outString, String contentTypeId); // Only really useful with HTML, else it simply calls encode() method
    }

    public static interface SimpleDecoder {
        public String decode(String original);
    }

    public static class HtmlEncoder implements SimpleEncoder {
        private static final char[] IMMUNE_HTML = {',', '.', '-', '_', ' '};
        private HTMLEntityCodec htmlCodec = new HTMLEntityCodec();
        @Override
        public String encode(String original) {
            if (original == null) {
                return null;
            }
            return htmlCodec.encode(IMMUNE_HTML, original);
        }
        /**
         * @deprecated Use {@link #sanitize(String,String)} instead
         */
        @Override
        @Deprecated
        public String sanitize(String original) {
            return sanitize(original, null);
        }

        /**
         * This method will start a configurable sanitizing process. The sanitizer can
         * be turns off through "sanitizer.enable=false", the default value is true. It
         * is possible to configure a custom policy using the properties
         * "sanitizer.permissive.policy" and "sanitizer.custom.permissive.policy.class". 
         * The custom policy has to implement
         * {@link org.apache.ofbiz.base.html.SanitizerCustomPolicy}.
         *
         * @param original
         * @param contentTypeId
         * @return sanitized HTML-Code if enabled, original HTML-Code when disabled
         * @see org.apache.ofbiz.base.html.CustomPermissivePolicy
         */
        @Override
        public String sanitize(String original, String contentTypeId) {
            if (original == null) {
                return null;
            }
            if (UtilProperties.getPropertyAsBoolean("owasp", "sanitizer.enable", true)) {
                PolicyFactory sanitizer = Sanitizers.FORMATTING.and(Sanitizers.BLOCKS).and(Sanitizers.IMAGES).and(
                        Sanitizers.LINKS).and(Sanitizers.STYLES);
                // TODO to be improved to use a (or several) contentTypeId/s when necessary.
                // Below is an example with BIRT_FLEXIBLE_REPORT_POLICY
                if ("FLEXIBLE_REPORT".equals(contentTypeId)) {
                    sanitizer = sanitizer.and(BIRT_FLEXIBLE_REPORT_POLICY);
                }

                // Check if custom policy should be used and if so don't use PERMISSIVE_POLICY
                if ("CUSTOM".equals(UtilProperties.getPropertyValue("owasp", "sanitizer.permissive.policy"))) {
                    PolicyFactory policy = null;
                    try {
                        Class<?> customPolicyClass = Class.forName(UtilProperties.getPropertyValue("owasp",
                                "sanitizer.custom.permissive.policy.class"));
                        Object obj = customPolicyClass.newInstance();
                        if (SanitizerCustomPolicy.class.isAssignableFrom(customPolicyClass)) {
                            Method meth = customPolicyClass.getMethod("getSanitizerPolicy");
                            policy = (PolicyFactory) meth.invoke(obj);
                        }
                    } catch (ClassNotFoundException | IllegalAccessException | IllegalArgumentException
                            | InvocationTargetException | NoSuchMethodException | SecurityException
                            | InstantiationException e) {
                        // Just logging the error and falling back to default policy
                        Debug.logError(e, "Could not find custom permissive sanitizer policy. Using default instead", module);
                    }

                    if (policy != null) {
                        sanitizer = sanitizer.and(policy);
                        return sanitizer.sanitize(original);
                    }
                }

                // Fallback should be the default option PERMISSIVE_POLICY
                sanitizer = sanitizer.and(PERMISSIVE_POLICY);
                return sanitizer.sanitize(original);
            }
            return original;
        }

        // Given as an example based on rendering cmssite as it was before using the sanitizer.
        // To use the PERMISSIVE_POLICY set sanitizer.permissive.policy to true.
        // Note that I was unable to render </html> and </body>. I guess because <html> and <body> 
        // are not sanitized in 1st place (else the sanitizer makes some damages I found)
        // You might even want to adapt the PERMISSIVE_POLICY to your needs... 
        // Be sure to check https://www.owasp.org/index.php/XSS_Filter_Evasion_Cheat_Sheet before...
        // And https://github.com/OWASP/java-html-sanitizer/blob/master/docs/getting_started.md for examples.
        // If you want another example: https://android.googlesource.com/platform/packages/apps/UnifiedEmail/+/ec0fa48/src/com/android/mail/utils/HtmlSanitizer.java
        public static final PolicyFactory PERMISSIVE_POLICY = new HtmlPolicyBuilder()
                .allowWithoutAttributes("html", "body")
                .allowAttributes("id", "class").globally()
                .allowElements("div", "center", "span", "table", "td")
                .allowWithoutAttributes("html", "body", "div", "span", "table", "td")
                .allowAttributes("width").onElements("table")
                .toFactory();

        // This is the PolicyFactory used for the Birt Report Builder usage feature. ("FLEXIBLE_REPORT" contentTypeId)
        // It allows to use the OOTB Birt Report Builder example.
        // You might need to enhance it for your needs (when using a new REPORT_MASTER) but normally you should not. 
        // See PERMISSIVE_POLICY above for documentation and examples
        public static final PolicyFactory BIRT_FLEXIBLE_REPORT_POLICY = new HtmlPolicyBuilder()
                .allowWithoutAttributes("html", "body")
                .allowElements("form", "div", "span", "table", "tr", "td", "input", "textarea", "label", "select", "option")
                .allowAttributes("id", "class", "name", "value", "onclick").globally()
                .allowAttributes("width", "cellspacing").onElements("table")
                .allowAttributes("type", "size", "maxlength").onElements("input")
                .allowAttributes("cols", "rows").onElements("textarea")
                .allowAttributes("class").onElements("td")
                .allowAttributes("method").onElements("form")
                .allowAttributes("accept", "action", "accept-charset", "autocomplete", "enctype", "method", 
                        "name", "novalidate", "target").onElements("form")
                .toFactory();
    }

    public static class XmlEncoder implements SimpleEncoder {
        private static final char[] IMMUNE_XML = {',', '.', '-', '_', ' '};
        private XMLEntityCodec xmlCodec = new XMLEntityCodec();
        @Override
        public String encode(String original) {
            if (original == null) {
                return null;
            }
            return xmlCodec.encode(IMMUNE_XML, original);
        }
        /**
         * @deprecated Use {@link #sanitize(String,String)} instead
         */
        @Override
        @Deprecated
        public String sanitize(String original) {
            return sanitize(original, null);
        }
        @Override
        public String sanitize(String original, String contentTypeId) {
            return encode(original);
        }
    }

    public static class UrlCodec implements SimpleEncoder, SimpleDecoder {
        @Override
        public String encode(String original) {
            try {
                return URLEncoder.encode(original, "UTF-8");
            } catch (UnsupportedEncodingException ee) {
                Debug.logError(ee, module);
                return null;
            }
        }
        /**
         * @deprecated Use {@link #sanitize(String,String)} instead
         */
        @Override
        @Deprecated
        public String sanitize(String original) {
            return sanitize(original, null);
        }
        @Override
        public String sanitize(String original, String contentTypeId) {
            return encode(original);
        }

        @Override
        public String decode(String original) {
            try {
                canonicalize(original);
                return URLDecoder.decode(original, "UTF-8");
            } catch (UnsupportedEncodingException ee) {
                Debug.logError(ee, module);
                return null;
            }
        }
    }

    public static class StringEncoder implements SimpleEncoder {
        @Override
        public String encode(String original) {
            if (original != null) {
                original = original.replace("\"", "\\\"");
            }
            return original;
        }
        /**
         * @deprecated Use {@link #sanitize(String,String)} instead
         */
        @Override
        @Deprecated
        public String sanitize(String original) {
            return sanitize(original, null);
        }
        @Override
        public String sanitize(String original, String contentTypeId) {
            return encode(original);
        }
    }

    // ================== Begin General Functions ==================

    public static SimpleEncoder getEncoder(String type) {
        if ("url".equals(type)) {
            return urlCodec;
        } else if ("xml".equals(type)) {
            return xmlEncoder;
        } else if ("html".equals(type)) {
            return htmlEncoder;
        } else if ("string".equals(type)) {
            return stringEncoder;
        } else {
            return null;
        }
    }

    public static SimpleDecoder getDecoder(String type) {
        if ("url".equals(type)) {
            return urlCodec;
        }
        return null;
    }

    public static String canonicalize(String value) throws IntrusionException {
        return canonicalize(value, false, false);
    }

    public static String canonicalize(String value, boolean strict) throws IntrusionException {
        return canonicalize(value, strict, strict);
    }

    public static String canonicalize(String input, boolean restrictMultiple, boolean restrictMixed) {
        if (input == null) {
            return null;
        }

        String working = input;
        Codec codecFound = null;
        int mixedCount = 1;
        int foundCount = 0;
        boolean clean = false;
        while (!clean) {
            clean = true;

            // try each codec and keep track of which ones work
            Iterator<Codec> i = codecs.iterator();
            while (i.hasNext()) {
                Codec codec = i.next();
                String old = working;
                working = codec.decode(working);
                if (!old.equals(working)) {
                    if (codecFound != null && codecFound != codec) {
                        mixedCount++;
                    }
                    codecFound = codec;
                    if (clean) {
                        foundCount++;
                    }
                    clean = false;
                }
            }
        }

        // do strict tests and handle if any mixed, multiple, nested encoding were found
        if (foundCount >= 2 && mixedCount > 1) {
            if (restrictMultiple || restrictMixed) {
                throw new IntrusionException("Input validation failure");
            }
            Debug.logWarning("Multiple (" + foundCount + "x) and mixed encoding (" + mixedCount + "x) detected in " + input, module);
        } else if (foundCount >= 2) {
            if (restrictMultiple) {
                throw new IntrusionException("Input validation failure");
            }
            Debug.logWarning("Multiple (" + foundCount + "x) encoding detected in " + input, module);
        } else if (mixedCount > 1) {
            if (restrictMixed) {
                throw new IntrusionException("Input validation failure");
            }
            Debug.logWarning("Mixed encoding (" + mixedCount + "x) detected in " + input, module);
        }
        return working;
    }

    /**
     * Uses a black-list approach for necessary characters for HTML.
     * Does not allow various characters (after canonicalization), including
     * "&lt;", "&gt;", "&amp;" and "%" (if not followed by a space).
     * 
     * Also does not allow js events as in OFBIZ-10054
     *
     * @param valueName field name checked
     * @param value value checked
     * @param errorMessageList an empty list passed by and modified in case of issues
     * @param locale
     */
    public static String checkStringForHtmlStrictNone(String valueName, String value, List<String> errorMessageList, 
            Locale locale) {
        if (UtilValidate.isEmpty(value)) {
            return value;
        }
        

        // canonicalize, strict (error on double-encoding)
        try {
            value = canonicalize(value, true);
        } catch (IntrusionException e) {
            // NOTE: using different log and user targeted error messages to allow the end-user message to be less technical
            Debug.logError("Canonicalization (format consistency, character escaping that is mixed or double, etc) "
                    + "error for attribute named [" + valueName + "], String [" + value + "]: " + e.toString(), module);
            String issueMsg = null;
            if (locale.equals(new Locale("test"))) { // labels are not available in testClasses Gradle task
                issueMsg = "In field [" + valueName + "] found character escaping (mixed or double) "
                        + "that is not allowed or other format consistency error: ";
            } else {
                issueMsg = UtilProperties.getMessage("SecurityUiLabels","PolicyNoneMixedOrDouble", 
                        UtilMisc.toMap("valueName", valueName), locale);
            }
            errorMessageList.add(issueMsg + e.toString());
        }

        // check for "<", ">"
        if (value.indexOf("<") >= 0 || value.indexOf(">") >= 0) {
            String issueMsg = null;
            if (locale.equals(new Locale("test"))) {
                issueMsg = "In field [" + valueName + "] less-than (<) and greater-than (>) symbols are not allowed.";
            } else {
                issueMsg = UtilProperties.getMessage("SecurityUiLabels","PolicyNoneLess-thanGreater-than", 
                        UtilMisc.toMap("valueName", valueName), locale);
            }
            errorMessageList.add(issueMsg);
        }
        
        // check for js events
        String onEvent = "on" + StringUtils.substringBetween(value, " on", "=");
        if (jsEventList.stream().anyMatch(str -> StringUtils.containsIgnoreCase(str, onEvent)) 
                || value.contains("seekSegmentTime")) {
            String issueMsg = null;
            if (locale.equals(new Locale("test"))) {
                issueMsg = "In field [" + valueName + "] Javascript events are not allowed.";
            } else {
                issueMsg = UtilProperties.getMessage("SecurityUiLabels","PolicyNoneJsEvents", 
                        UtilMisc.toMap("valueName", valueName), locale);
            }
            errorMessageList.add(issueMsg);
        }

        // TODO: anything else to check for that can be used to get HTML or JavaScript going without these characters?
        //
        // Another would be https://www.owasp.org/index.php/XSS_Filter_Evasion_Cheat_Sheet#US-ASCII_encoding
        // But all our Tomcat connectors use UTF-8
        // We don't care about Flash now rather deprecated
        // AFAIK all others need less-than (<) and greater-than (>) symbols
        
        return value;
    }

    /**
     * This method check if the input is safe HTML.
     * It is possible to configure a safe policy using the properties
     * "sanitizer.safe.policy" and "sanitizer.custom.safe.policy.class". 
     * The safe policy has to implement
     * {@link org.apache.ofbiz.base.html.SanitizerCustomPolicy}.
     *
     * @param valueName field name checked
     * @param value value checked
     * @param errorMessageList an empty list passed by and modified in case of issues
     * @param locale
     */
    public static String checkStringForHtmlSafe(String valueName, String value, List<String> errorMessageList, 
            Locale locale) {
        PolicyFactory policy = null;
        try {
            Class<?> customPolicyClass = null;
            if (locale.equals(new Locale("test"))) {
                customPolicyClass = Class.forName("org.apache.ofbiz.base.html.CustomSafePolicy");
            } else {
            customPolicyClass = Class.forName(UtilProperties.getPropertyValue("owasp",
                    "sanitizer.custom.safe.policy.class"));
            }
            Object obj = customPolicyClass.newInstance();
            if (SanitizerCustomPolicy.class.isAssignableFrom(customPolicyClass)) {
                Method meth = customPolicyClass.getMethod("getSanitizerPolicy");
                policy = (PolicyFactory) meth.invoke(obj);
            }
        } catch (ClassNotFoundException | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException | NoSuchMethodException | SecurityException
                | InstantiationException e) {
            Debug.logError(e, "Could not find custom safe sanitizer policy. Using default instead."
                    + "Beware: the result is not rightly checked!", module);
        }

        String filtered = policy.sanitize(value);
        if (!value.equals(StringEscapeUtils.unescapeHtml4(filtered))) {
            String issueMsg = null;
            if (locale.equals(new Locale("test"))) {
                issueMsg = "In field [" + valueName + "] by our input policy, your input has not been accepted "
                        + "for security reason. Please check and modify accordingly, thanks.";
            } else {
                issueMsg = UtilProperties.getMessage("SecurityUiLabels","PolicySafe", 
                        UtilMisc.toMap("valueName", valueName), locale);
            }
            errorMessageList.add(issueMsg);
        }
        
        return value;
    }
    
    /**
     * A simple Map wrapper class that will do HTML encoding. 
     * To be used for passing a Map to something that will expand Strings with it as a context, etc.
     */
    public static class HtmlEncodingMapWrapper<K> implements Map<K, Object> {
        public static <K> HtmlEncodingMapWrapper<K> getHtmlEncodingMapWrapper(Map<K, Object> mapToWrap, SimpleEncoder encoder) {
            if (mapToWrap == null) {
                return null;
            }

            HtmlEncodingMapWrapper<K> mapWrapper = new HtmlEncodingMapWrapper<>();
            mapWrapper.setup(mapToWrap, encoder);
            return mapWrapper;
        }

        protected Map<K, Object> internalMap = null;
        protected SimpleEncoder encoder = null;
        protected HtmlEncodingMapWrapper() { }

        public void setup(Map<K, Object> mapToWrap, SimpleEncoder encoder) {
            this.internalMap = mapToWrap;
            this.encoder = encoder;
        }
        public void reset() {
            this.internalMap = null;
            this.encoder = null;
        }

        @Override
        public int size() { return this.internalMap.size(); }
        @Override
        public boolean isEmpty() { return this.internalMap.isEmpty(); }
        @Override
        public boolean containsKey(Object key) { return this.internalMap.containsKey(key); }
        @Override
        public boolean containsValue(Object value) { return this.internalMap.containsValue(value); }
        @Override
        public Object get(Object key) {
            Object theObject = this.internalMap.get(key);
            if (theObject instanceof String) {
                if (this.encoder != null) {
                    return encoder.encode((String) theObject);
                }
                return UtilCodec.getEncoder("html").encode((String) theObject);
            } else if (theObject instanceof Map<?, ?>) {
                return HtmlEncodingMapWrapper.getHtmlEncodingMapWrapper(UtilGenerics.<K, Object>checkMap(theObject), this.encoder);
            }
            return theObject;
        }
        @Override
        public Object put(K key, Object value) { return this.internalMap.put(key, value); }
        @Override
        public Object remove(Object key) { return this.internalMap.remove(key); }
        @Override
        public void putAll(Map<? extends K, ? extends Object> arg0) { this.internalMap.putAll(arg0); }
        @Override
        public void clear() { this.internalMap.clear(); }
        @Override
        public Set<K> keySet() { return this.internalMap.keySet(); }
        @Override
        public Collection<Object> values() { return this.internalMap.values(); }
        @Override
        public Set<Map.Entry<K, Object>> entrySet() { return this.internalMap.entrySet(); }
        @Override
        public String toString() { return this.internalMap.toString(); }
    }

}
