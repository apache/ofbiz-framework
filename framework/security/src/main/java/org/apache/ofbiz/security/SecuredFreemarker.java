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

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.StringUtil;
import org.apache.ofbiz.base.util.UtilHttp;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;

public class SecuredFreemarker {
    private static final String MODULE = SecuredFreemarker.class.getName();
    private static final List<String> FTL_INTERPOLATION = List.of("%24%7B", "${", "%3C%23", "<#", "%23%7B", "#{", "%5B%3D", "[=", "%5B%23", "[#");

    /*
     * Prevents Freemarker exploits
     * @param req
     * @param resp
     * @param uri
     * @throws IOException
     */
    public static boolean containsFreemarkerInterpolation(HttpServletRequest req, HttpServletResponse resp, String uri)
            throws IOException {
        String urisOkForFreemarker = UtilProperties.getPropertyValue("security", "allowedURIsForFreemarkerInterpolation");
        List<String> urisOK = UtilValidate.isNotEmpty(urisOkForFreemarker) ? StringUtil.split(urisOkForFreemarker, ",")
                                                                           : new ArrayList<>();
        String uriEnd = uri.substring(uri.lastIndexOf("/") + 1, uri.length());

        if (!urisOK.contains(uriEnd)) {
            Map<String, String[]> parameterMap = req.getParameterMap();
            if (uri.contains("ecomseo")) { // SeoContextFilter call
                if (containsFreemarkerInterpolation(resp, uri)) {
                    return true;
                }
            } else if (!parameterMap.isEmpty()) { // ControlFilter call
                List<BasicNameValuePair> params = new ArrayList<>();
                parameterMap.forEach((name, values) -> {
                    for (String value : values) {
                        params.add(new BasicNameValuePair(name, value));
                    }
                });
                String queryString = URLEncodedUtils.format(params, Charset.forName("UTF-8"));
                uri = uri + "?" + queryString;
                if (containsFreemarkerInterpolation(resp, uri)) {
                    return true;
                }
            } else if (!UtilHttp.getAttributeMap(req).isEmpty()) { // Call with Content-Type modified by a MITM attack (rare case)
                String attributeMap = UtilHttp.getAttributeMap(req).toString();
                if (containsFreemarkerInterpolation(resp, attributeMap)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * @param resp
     * @param stringToCheck
     * @throws IOException
     */
    public static boolean containsFreemarkerInterpolation(HttpServletResponse resp, String stringToCheck) throws IOException {
        if (containsFreemarkerInterpolation(stringToCheck)) { // not used OOTB in OFBiz, but possible
            Debug.logError("===== Not saved for security reason, strings '${', '<#', '#{', '[=' or '[#' not accepted in fields! =====", MODULE);
            resp.sendError(HttpServletResponse.SC_FORBIDDEN,
                    "Not saved for security reason, strings '${', '<#', '#{', '[=' or '[#' not accepted in fields!");
            return true;
        }
        return false;
    }

    /**
     * Analyze if stringToCheck contains a freemarker template
     * @param stringToCheck
     * @return true if freemarker template is detected
     */
    public static boolean containsFreemarkerInterpolation(String stringToCheck) {
        return UtilValidate.isNotEmpty(stringToCheck)
                && FTL_INTERPOLATION.stream().anyMatch(stringToCheck::contains);
    }

    /**
     * Analyse each entry contains on params. If a freemarker template is detected, sanatize it to escape any exploit
     * @param params
     * @return Map with all values sanitized
     */
    public static Map<String, Object> sanitizeParameterMap(Map<String, Object> params) {
        List<Map.Entry<String, Object>> unsafeEntries = params.entrySet().stream()
                .filter(entry -> entry.getValue() instanceof String
                        && containsFreemarkerInterpolation((String) entry.getValue()))
                .toList();
        if (!unsafeEntries.isEmpty()) {
            Map<String, Object> paramsSanitize = new HashMap<>(params);
            unsafeEntries.forEach(entry -> {
                String sanitazedValue = (String) entry.getValue();
                for (String interpolation : FTL_INTERPOLATION) {
                    sanitazedValue = sanitazedValue.replace(interpolation, "##");
                }
                paramsSanitize.put(entry.getKey(), sanitazedValue);
            });
            return paramsSanitize;
        }
        return params;
    }
}
