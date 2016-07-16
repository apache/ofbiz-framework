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
package org.apache.ofbiz.catalina.container;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.util.SessionConfig;
import org.apache.catalina.valves.ValveBase;
import org.apache.tomcat.util.buf.MessageBytes;
import org.apache.tomcat.util.http.MimeHeaders;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.util.EntityUtilProperties;

public class CrossSubdomainSessionValve extends ValveBase {

    public static final String module = CrossSubdomainSessionValve.class.getName();

    public CrossSubdomainSessionValve() {
        super();
    }

    public @Override void invoke(Request request, Response response) throws IOException, ServletException {

        // this will cause Request.doGetSession to create the session cookie if necessary
        request.getSession(true);

        // replace any Tomcat-generated session cookies with our own
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (SessionConfig.getSessionCookieName(null).equals(cookie.getName())) {
                    replaceCookie(request, response, cookie);
                }
            }
        }

        // process the next valve
        getNext().invoke(request, response);
    }

    protected void replaceCookie(Request request, Response response, Cookie cookie) {

    	Delegator delegator = (Delegator) request.getAttribute("delegator");
        // copy the existing session cookie, but use a different domain (only if domain is valid)
        String cookieDomain = null;
        cookieDomain = EntityUtilProperties.getPropertyValue("url", "cookie.domain", "", delegator);

        if (UtilValidate.isEmpty(cookieDomain)) {
            String serverName = request.getServerName();
            String[] domainArray = serverName.split("\\.");
            // check that the domain isn't an IP address
            if (domainArray.length == 4) {
                boolean isIpAddress = true;
                for (String domainSection : domainArray) {
                    if (!UtilValidate.isIntegerInRange(domainSection, 0, 255)) {
                        isIpAddress = false;
                        break;
                    }
                }
                if (isIpAddress) return;
            }
            if (domainArray.length > 2) {
                cookieDomain = "." + domainArray[domainArray.length - 2] + "." + domainArray[domainArray.length - 1];
            }
        }


        if (UtilValidate.isNotEmpty(cookieDomain)) {
            Cookie newCookie = new Cookie(cookie.getName(), cookie.getValue());
            if (cookie.getPath() != null) {
                newCookie.setPath(cookie.getPath());
            }
            newCookie.setDomain(cookieDomain);
            newCookie.setMaxAge(cookie.getMaxAge());
            newCookie.setVersion(cookie.getVersion());
            if (cookie.getComment() != null) {
                newCookie.setComment(cookie.getComment());
            }
            newCookie.setSecure(cookie.getSecure());

            // if the response has already been committed, our replacement strategy will have no effect
            if (response.isCommitted()) {
                Debug.logError("CrossSubdomainSessionValve: response was already committed!", module);
            }

            // find the Set-Cookie header for the existing cookie and replace its value with new cookie
            MimeHeaders mimeHeaders = request.getCoyoteRequest().getMimeHeaders();
            for (int i = 0, size = mimeHeaders.size(); i < size; i++) {
                if (mimeHeaders.getName(i).equals("Set-Cookie")) {
                    MessageBytes value = mimeHeaders.getValue(i);
                    if (value.indexOf(cookie.getName()) >= 0) {
                        String newCookieValue = request.getContext().getCookieProcessor().generateHeader(newCookie);
                        Debug.logVerbose("CrossSubdomainSessionValve: old Set-Cookie value: " + value.toString(), module);
                        Debug.logVerbose("CrossSubdomainSessionValve: new Set-Cookie value: " + newCookieValue, module);
                        value.setString(newCookieValue);
                    }
                }
            }
        }
    }
}
