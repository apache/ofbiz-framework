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

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    static {
        List<Codec> tmpCodecs = new ArrayList<Codec>();
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
        public String sanitize(String outString); // Only really useful with HTML, else simply calls encode() method 
    }

    public static interface SimpleDecoder {
        public String decode(String original);
    }

    public static class HtmlEncoder implements SimpleEncoder {
        private static final char[] IMMUNE_HTML = {',', '.', '-', '_', ' '};
        private HTMLEntityCodec htmlCodec = new HTMLEntityCodec();
        public String encode(String original) {
            if (original == null) {
                return null;
            }
            return htmlCodec.encode(IMMUNE_HTML, original);
        }
        public String sanitize(String original) {
            if (original == null) {
                return null;
            }
            PolicyFactory sanitizer = Sanitizers.FORMATTING.and(Sanitizers.BLOCKS).and(Sanitizers.IMAGES).and(Sanitizers.LINKS).and(Sanitizers.STYLES);
            if (UtilProperties.getPropertyAsBoolean("owasp", "sanitizer.permissive.policy", false)) {
                sanitizer = sanitizer.and(PERMISSIVE_POLICY);
            }
            return sanitizer.sanitize(original);
        }
        // Given as an example based on rendering cmssite as it was before using the sanitizer.
        // To use the PERMISSIVE_POLICY set sanitizer.permissive.policy to true. 
        // Note that I was unable to render </html> and </body>. I guess because are <html> and <body> are not sanitized in 1st place (else the sanitizer makes some damages I found)
        // You might even want to adapt the PERMISSIVE_POLICY to your needs... Be sure to check https://www.owasp.org/index.php/XSS_Filter_Evasion_Cheat_Sheet before...
        public static final PolicyFactory PERMISSIVE_POLICY = new HtmlPolicyBuilder()
                .allowAttributes("id", "class").globally()
                .allowElements("html", "body", "div", "center", "span", "table", "td")
                .allowWithoutAttributes("html", "body", "div", "span", "table", "td")
                .allowAttributes("width").onElements("table")
                .toFactory();
    }

    public static class XmlEncoder implements SimpleEncoder {
        private static final char[] IMMUNE_XML = {',', '.', '-', '_', ' '};
        private XMLEntityCodec xmlCodec = new XMLEntityCodec();
        public String encode(String original) {
            if (original == null) {
                   return null;
               }
               return xmlCodec.encode(IMMUNE_XML, original);
        }
        public String sanitize(String original) {
            return encode(original);
        }
    }

    public static class UrlCodec implements SimpleEncoder, SimpleDecoder {
        public String encode(String original) {
            try {
                return URLEncoder.encode(original, "UTF-8");
            } catch (UnsupportedEncodingException ee) {
                Debug.logError(ee, module);
                return null;
            }
        }
        public String sanitize(String original) {
            return encode(original);
        }

        public String decode(String original) {
            try {
                String canonical = canonicalize(original);
                return URLDecoder.decode(canonical, "UTF-8");
            } catch (UnsupportedEncodingException ee) {
                Debug.logError(ee, module);
                return null;
            }
        }
    }

    public static class StringEncoder implements SimpleEncoder {
        public String encode(String original) {
            if (original != null) {
                original = original.replace("\"", "\\\"");
            }
            return original;
        }
        public String sanitize(String original) {
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
        } else {
            return null;
        }
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
            } else {
                Debug.logWarning("Multiple (" + foundCount + "x) and mixed encoding (" + mixedCount + "x) detected in " + input, module);
            }
        } else if (foundCount >= 2) {
            if (restrictMultiple) {
                throw new IntrusionException("Input validation failure");
            } else {
                Debug.logWarning("Multiple (" + foundCount + "x) encoding detected in " + input, module);
            }
        } else if (mixedCount > 1) {
            if (restrictMixed) {
                throw new IntrusionException("Input validation failure");
            } else {
                Debug.logWarning("Mixed encoding (" + mixedCount + "x) detected in " + input, module);
            }
        }
        return working;
    }

    /**
     * Uses a black-list approach for necessary characters for HTML.
     * Does not allow various characters (after canonicalization), including "<", ">", "&" (if not followed by a space), and "%" (if not followed by a space).
     *
     * @param value
     * @param errorMessageList
     */
    public static String checkStringForHtmlStrictNone(String valueName, String value, List<String> errorMessageList) {
        if (UtilValidate.isEmpty(value)) return value;

        // canonicalize, strict (error on double-encoding)
        try {
            value = canonicalize(value, true);
        } catch (IntrusionException e) {
            // NOTE: using different log and user targeted error messages to allow the end-user message to be less technical
            Debug.logError("Canonicalization (format consistency, character escaping that is mixed or double, etc) error for attribute named [" + valueName + "], String [" + value + "]: " + e.toString(), module);
            errorMessageList.add("In field [" + valueName + "] found character escaping (mixed or double) that is not allowed or other format consistency error: " + e.toString());
        }

        // check for "<", ">"
        if (value.indexOf("<") >= 0 || value.indexOf(">") >= 0) {
            errorMessageList.add("In field [" + valueName + "] less-than (<) and greater-than (>) symbols are not allowed.");
        }

        /* NOTE DEJ 20090311: After playing with this more this doesn't seem to be necessary; the canonicalize will convert all such characters into actual text before this check is done, including other illegal chars like &lt; which will canonicalize to < and then get caught
        // check for & followed a semicolon within 7 characters, no spaces in-between (and perhaps other things sometime?)
        int curAmpIndex = value.indexOf("&");
        while (curAmpIndex > -1) {
            int semicolonIndex = value.indexOf(";", curAmpIndex + 1);
            int spaceIndex = value.indexOf(" ", curAmpIndex + 1);
            if (semicolonIndex > -1 && (semicolonIndex - curAmpIndex <= 7) && (spaceIndex < 0 || (spaceIndex > curAmpIndex && spaceIndex < semicolonIndex))) {
                errorMessageList.add("In field [" + valueName + "] the ampersand (&) symbol is only allowed if not used as an encoded character: no semicolon (;) within 7 spaces or there is a space between.");
                // once we find one like this we have the message so no need to check for more
                break;
            }
            curAmpIndex = value.indexOf("&", curAmpIndex + 1);
        }
         */

        /* NOTE DEJ 20090311: After playing with this more this doesn't seem to be necessary; the canonicalize will convert all such characters into actual text before this check is done, including other illegal chars like %3C which will canonicalize to < and then get caught
        // check for % followed by 2 hex characters
        int curPercIndex = value.indexOf("%");
        while (curPercIndex >= 0) {
            if (value.length() > (curPercIndex + 3) && UtilValidate.isHexDigit(value.charAt(curPercIndex + 1)) && UtilValidate.isHexDigit(value.charAt(curPercIndex + 2))) {
                errorMessageList.add("In field [" + valueName + "] the percent (%) symbol is only allowed if followed by a space.");
                // once we find one like this we have the message so no need to check for more
                break;
            }
            curPercIndex = value.indexOf("%", curPercIndex + 1);
        }
         */

        // TODO: anything else to check for that can be used to get HTML or JavaScript going without these characters?

        return value;
    }

    /**
     * A simple Map wrapper class that will do HTML encoding. To be used for passing a Map to something that will expand Strings with it as a context, etc.
     */
    public static class HtmlEncodingMapWrapper<K> implements Map<K, Object> {
        public static <K> HtmlEncodingMapWrapper<K> getHtmlEncodingMapWrapper(Map<K, Object> mapToWrap, SimpleEncoder encoder) {
            if (mapToWrap == null) return null;

            HtmlEncodingMapWrapper<K> mapWrapper = new HtmlEncodingMapWrapper<K>();
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

        public int size() { return this.internalMap.size(); }
        public boolean isEmpty() { return this.internalMap.isEmpty(); }
        public boolean containsKey(Object key) { return this.internalMap.containsKey(key); }
        public boolean containsValue(Object value) { return this.internalMap.containsValue(value); }
        public Object get(Object key) {
            Object theObject = this.internalMap.get(key);
            if (theObject instanceof String) {
                if (this.encoder != null) {
                    return encoder.encode((String) theObject);
                } else {
                    return UtilCodec.getEncoder("html").encode((String) theObject);
                }
            } else if (theObject instanceof Map<?, ?>) {
                return HtmlEncodingMapWrapper.getHtmlEncodingMapWrapper(UtilGenerics.<K, Object>checkMap(theObject), this.encoder);
            }
            return theObject;
        }
        public Object put(K key, Object value) { return this.internalMap.put(key, value); }
        public Object remove(Object key) { return this.internalMap.remove(key); }
        public void putAll(Map<? extends K, ? extends Object> arg0) { this.internalMap.putAll(arg0); }
        public void clear() { this.internalMap.clear(); }
        public Set<K> keySet() { return this.internalMap.keySet(); }
        public Collection<Object> values() { return this.internalMap.values(); }
        public Set<Map.Entry<K, Object>> entrySet() { return this.internalMap.entrySet(); }
        @Override
        public String toString() { return this.internalMap.toString(); }
    }

}
