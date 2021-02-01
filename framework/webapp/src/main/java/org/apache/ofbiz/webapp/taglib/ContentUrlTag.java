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
package org.apache.ofbiz.webapp.taglib;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.webapp.website.WebSiteWorker;

/**
 * ContentUrlTag - Creates a URL string prepending the content prefix from url.properties
 */
public class ContentUrlTag {

    private static final String MODULE = ContentUrlTag.class.getName();

    public static void appendContentPrefix(HttpServletRequest request, StringBuilder urlBuffer) {
        try {
            appendContentPrefix(request, (Appendable) urlBuffer);
        } catch (IOException e) {
            throw UtilMisc.initCause(new InternalError(e.getMessage()), e);
        }
    }

    public static void appendContentPrefix(HttpServletRequest request, Appendable urlBuffer) throws IOException {
        if (request == null) {
            Debug.logWarning("Request was null in appendContentPrefix; this probably means this was used where it shouldn't be, like using "
                    + "ofbizContentUrl in a screen rendered through a service; using best-bet behavior: standard prefix from url.properties (no "
                    + "WebSite or security setting known)", MODULE);
            String prefix = UtilProperties.getPropertyValue("url", "content.url.prefix.standard");
            if (prefix != null) {
                urlBuffer.append(prefix.trim());
            }
            return;
        }
        GenericValue webSite = WebSiteWorker.getWebSite(request);
        boolean isForwardedSecure = "HTTPS".equalsIgnoreCase(request.getHeader("X-Forwarded-Proto"));
        boolean isSecure = request.isSecure() || isForwardedSecure;
        appendContentPrefix(webSite, isSecure, urlBuffer);
    }

    public static void appendContentPrefix(GenericValue webSite, boolean secure, Appendable urlBuffer) throws IOException {
        if (secure) {
            if (webSite != null && UtilValidate.isNotEmpty(webSite.getString("secureContentPrefix"))) {
                urlBuffer.append(webSite.getString("secureContentPrefix").trim());
            } else {
                String prefix = UtilProperties.getPropertyValue("url", "content.url.prefix.secure");
                if (prefix != null) {
                    urlBuffer.append(prefix.trim());
                }
            }
        } else {
            if (webSite != null && UtilValidate.isNotEmpty(webSite.getString("standardContentPrefix"))) {
                urlBuffer.append(webSite.getString("standardContentPrefix").trim());
            } else {
                String prefix = UtilProperties.getPropertyValue("url", "content.url.prefix.standard");
                if (prefix != null) {
                    urlBuffer.append(prefix.trim());
                }
            }
        }
    }

    public static String getContentPrefix(HttpServletRequest request) {
        StringBuilder buf = new StringBuilder();
        ContentUrlTag.appendContentPrefix(request, buf);
        return buf.toString();
    }
}
