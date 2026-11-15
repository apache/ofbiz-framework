/*
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
package org.apache.ofbiz.catalina.container;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

import org.apache.catalina.Context;
import org.apache.catalina.util.SessionConfig;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.util.EntityUtilProperties;
import org.apache.ofbiz.webapp.WebAppUtil;
import org.apache.tomcat.util.http.Rfc6265CookieProcessor;

/**
 * Cookie processor that sets the domain of the session cookie so that the session is shared by all the subdomains.
 * The domain is the cookie.domain property of url.properties or, when empty, the last two parts of the server name.
 * Since Tomcat generates every session cookie header with the cookie processor of the context, this covers new
 * sessions as well as session id changes, such as the one done at login to prevent session fixation.
 */
public class CrossSubdomainSessionCookieProcessor extends Rfc6265CookieProcessor {

    private final Context context;

    CrossSubdomainSessionCookieProcessor(Context context) {
        this.context = context;
    }

    @Override
    public String generateHeader(Cookie cookie, HttpServletRequest request) {
        if (UtilValidate.isEmpty(cookie.getDomain()) && SessionConfig.getSessionCookieName(context).equals(cookie.getName())) {
            String cookieDomain = getCookieDomain(request);
            if (UtilValidate.isNotEmpty(cookieDomain)) {
                Cookie domainCookie = (Cookie) cookie.clone();
                domainCookie.setDomain(cookieDomain);
                return super.generateHeader(domainCookie, request);
            }
        }
        return super.generateHeader(cookie, request);
    }

    private static String getCookieDomain(HttpServletRequest request) {
        String cookieDomain = EntityUtilProperties.getPropertyValue("url", "cookie.domain", "",
                WebAppUtil.getDelegator(request.getServletContext()));
        if (UtilValidate.isNotEmpty(cookieDomain)) {
            return cookieDomain;
        }
        String[] domainArray = request.getServerName().split("\\.");
        // check that the domain isn't an IP address
        if (domainArray.length == 4) {
            boolean isIpAddress = true;
            for (String domainSection : domainArray) {
                if (!UtilValidate.isIntegerInRange(domainSection, 0, 255)) {
                    isIpAddress = false;
                    break;
                }
            }
            if (isIpAddress) {
                return null;
            }
        }
        if (domainArray.length > 2) {
            // RFC 6265 forbids a leading dot, a domain without it also matches all of its subdomains
            return domainArray[domainArray.length - 2] + "." + domainArray[domainArray.length - 1];
        }
        return null;
    }
}
