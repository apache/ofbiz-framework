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

package org.ofbiz.ldap.cas;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.security.SecureRandom;

import javax.naming.NamingException;
import javax.naming.directory.SearchResult;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ofbiz.base.util.UtilXml;
import org.ofbiz.ldap.commons.AbstractOFBizAuthenticationHandler;
import org.ofbiz.ldap.commons.InterfaceOFBizAuthenticationHandler;
import org.w3c.dom.Element;

/**
 * The OFBiz CAS-LDAP Authentication Handler.<p>
 *
 * The ACL of a user is still controlled by OFBiz.
 *
 */
public final class OFBizCasAuthenticationHandler extends AbstractOFBizAuthenticationHandler {

    public static final String PARAM_TICKET = "ticket";

    public static final String PARAM_SERVICE = "service";

    public static final String PARAM_RENEW = "renew";

    /**
     * Public constructor, initializes some required member variables.<p>
     */
    public OFBizCasAuthenticationHandler() {

    }


    @Override
    public String login(HttpServletRequest request, HttpServletResponse response, Element rootElement) throws Exception {

        String ticket = request.getParameter(PARAM_TICKET);
        String username = request.getParameter("USERNAME");
        String password = request.getParameter("PASSWORD");

        String casUrl = UtilXml.childElementValue(rootElement, "CasUrl", "https://localhost:8443/cas");
        String loginUri = UtilXml.childElementValue(rootElement, "CasLoginUri", "/login");
        String validateUri = UtilXml.childElementValue(rootElement, "CasValidateUri", "/validate");
        String serviceUrl = request.getRequestURL().toString();
        String url = URLEncoder.encode(serviceUrl, "UTF-8");
        boolean casLoggedIn = false;
        if (ticket == null) {
            // forward the login page to CAS login page
            response.sendRedirect(casUrl + loginUri + "?" + PARAM_SERVICE + "=" + url);
        } else {
            // there's a ticket, we should validate the ticket
            URL validateURL = new URL(casUrl + validateUri + "?" + PARAM_TICKET + "=" + ticket + "&" + PARAM_SERVICE + "=" + url);
            URLConnection conn = validateURL.openConnection();
            InputStreamReader result = null;
            BufferedReader reader = null;
            try {
                result = new InputStreamReader(conn.getInputStream(), "UTF-8");
                reader = new BufferedReader(result);
                String oneline = reader.readLine();
                if (oneline != null && oneline.equals("yes")) {
                    // the ticket is true
                    username = reader.readLine().trim();
                    casLoggedIn = true;
                } else {
                    // the ticket is false, forward the request to cas login page
                    response.sendRedirect(casUrl + loginUri + "?service=" + url);
                }
            } catch (Exception e) {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (Exception e1) {
                    }
                }
                if (result != null) {
                    try {
                        result.close();
                    } catch (Exception e1) {
                    }
                }
            }
        }

        if (casLoggedIn && username != null) {
            // as we cannot get the password user input in CAS login page, we use a random one
            password = randomString();
            SearchResult result = getLdapSearchResult(username, password, rootElement, false);
            if (result != null) {
                return login(request, response, username, password, rootElement, result);
            }
        }
        return "error";
    }

    public static String randomString(int lo, int hi) {
        int n = rand(lo, hi);
        byte b[] = new byte[n];
        for (int i = 0; i < n; i++) {
            b[i] = (byte)rand('a', 'z');
        }
        return new String(b);
    }

    private static int rand(int lo, int hi) {
        java.util.Random rn = new SecureRandom();
        int n = hi - lo + 1;
        int i = rn.nextInt() % n;
        if (i < 0)
                i = -i;
        return lo + i;
    }

    public static String randomString() {
        return randomString(5, 15);
    }

    @Override
    public String logout(HttpServletRequest request, HttpServletResponse response, Element rootElement) {
        String casUrl = UtilXml.childElementValue(rootElement, "CasUrl", "https://localhost:8443/cas");
        String logoutUri = UtilXml.childElementValue(rootElement, "CasLogoutUri", "/logout");
        try {
            response.sendRedirect(casUrl + logoutUri);
        } catch (UnsupportedEncodingException e) {
        } catch (IOException e) {
        }
        return "success";
    }


    @Override
    public SearchResult getLdapSearchResult(String username, String password,
            Element rootElement, boolean bindRequired) throws NamingException {
        String className = UtilXml.childElementValue(rootElement, "CasLdapHandler", "org.ofbiz.ldap.openldap.OFBizLdapAuthenticationHandler");
        try {
            Class<?> handlerClass = Class.forName(className);
            InterfaceOFBizAuthenticationHandler casLdapHandler = (InterfaceOFBizAuthenticationHandler) handlerClass.newInstance();
            return casLdapHandler.getLdapSearchResult(username, password, rootElement, bindRequired);
        } catch (ClassNotFoundException e) {
            throw new NamingException(e.getLocalizedMessage());
        } catch (InstantiationException e) {
            throw new NamingException(e.getLocalizedMessage());
        } catch (IllegalAccessException e) {
            throw new NamingException(e.getLocalizedMessage());
        }
    }

    /**
     * An HTTP WebEvent handler that checks to see is a userLogin is logged out.
     * If yes, the user is forwarded to the login page.
     *
     * @param request The HTTP request object for the current JSP or Servlet request.
     * @param response The HTTP response object for the current JSP or Servlet request.
     * @param rootElement Element root element of ldap config file
     * @return true if the user has logged out from ldap; otherwise, false.
     */
    @Override
    public boolean hasLdapLoggedOut(HttpServletRequest request, HttpServletResponse response, Element rootElement) {
        String casTGC = UtilXml.childElementValue(rootElement, "CasTGTCookieName", "CASTGC");
        String casUrl = UtilXml.childElementValue(rootElement, "CasUrl", "https://localhost:8443/cas");
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return true;
        for (int i=0; i < cookies.length; i++) {
            Cookie cookie = cookies[i];
            if (cookie.getName().equals(casTGC) && casUrl.indexOf(cookie.getDomain()) > -1) {
                return false;
            }
        }
        return true;
    }
}
