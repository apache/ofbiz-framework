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
 */
package org.apache.ofbiz.webapp.control;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.DelegatorFactory;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityUtilProperties;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.webapp.WebAppUtil;

/**
 * This class manages the single sign-on authentication through external login keys between OFBiz applications.
 */
public class ExternalLoginKeysManager {
    private static final String MODULE = ExternalLoginKeysManager.class.getName();
    private static final String EXTERNAL_LOGIN_KEY_ATTR = "externalLoginKey";
    // This Map is keyed by the randomly generated externalLoginKey and the value is a UserLogin GenericValue object
    private static final Map<String, GenericValue> EXTERNAL_LOGIN_KEYS = new ConcurrentHashMap<>();

    // This variable is set to empty so we know need to read from the properties file.
    private static String isExternalLoginKeyEnabled = "";

    /**
     * Gets (and creates if necessary) an authentication token to be used for an external login parameter.
     * When a new token is created, it is persisted in the web session and in the web request and map entry keyed by the
     * token and valued by a userLogin object is added to a map that is looked up for subsequent requests.
     * @param request - the http request in which the authentication token is searched and stored
     * @return the authentication token as persisted in the session and request objects
     */
    public static String getExternalLoginKey(HttpServletRequest request) {
        String externalKey = (String) request.getAttribute(EXTERNAL_LOGIN_KEY_ATTR);
        if (externalKey != null) return externalKey;

        HttpSession session = request.getSession();
        synchronized (session) {
            // if the session has a previous key in place, remove it from the master list
            String sesExtKey = (String) session.getAttribute(EXTERNAL_LOGIN_KEY_ATTR);

            if (sesExtKey != null) {
                if (isAjax(request)) return sesExtKey;

                EXTERNAL_LOGIN_KEYS.remove(sesExtKey);
            }

            GenericValue userLogin = (GenericValue) request.getAttribute("userLogin");
            //check the userLogin here, after the old session setting is set so that it will always be cleared
            if (userLogin == null) return "";

            //no key made yet for this request, create one
            while (externalKey == null || EXTERNAL_LOGIN_KEYS.containsKey(externalKey)) {
                UUID uuid = UUID.randomUUID();
                externalKey = "EL" + uuid.toString();
            }

            request.setAttribute(EXTERNAL_LOGIN_KEY_ATTR, externalKey);
            session.setAttribute(EXTERNAL_LOGIN_KEY_ATTR, externalKey);
            EXTERNAL_LOGIN_KEYS.put(externalKey, userLogin);
            return externalKey;
        }
    }

    /**
     * Removes the authentication token, if any, from the session.
     * @param session - the http session from which the authentication token is removed
     */
    static void cleanupExternalLoginKey(HttpSession session) {
        String sesExtKey = (String) session.getAttribute(EXTERNAL_LOGIN_KEY_ATTR);
        if (sesExtKey != null) {
            EXTERNAL_LOGIN_KEYS.remove(sesExtKey);
        }
    }

    /**
     * OFBiz controller event that performs the user authentication using the authentication token.
     * The method is designed to be used in a chain of controller preprocessor event: it always return "success"
     * even when the authentication token is missing or the authentication fails in order to move the processing to the
     * next event in the chain.
     * @param request - the http request object
     * @param response - the http response object
     * @return "success" in all the cases
     */
    public static String checkExternalLoginKey(HttpServletRequest request, HttpServletResponse response) {
        String externalKey = request.getParameter(EXTERNAL_LOGIN_KEY_ATTR);
        if (externalKey == null) return "success";

        GenericValue userLogin = EXTERNAL_LOGIN_KEYS.get(externalKey);
        if (userLogin != null) {
            //to check it's the right tenant
            //in case username and password are the same in different tenants
            Delegator delegator = (Delegator) request.getAttribute("delegator");
            String oldDelegatorName = delegator.getDelegatorName();
            if (!oldDelegatorName.equals(userLogin.getDelegator().getDelegatorName())) {
                delegator = DelegatorFactory.getDelegator(userLogin.getDelegator().getDelegatorName());
                LocalDispatcher dispatcher = WebAppUtil.makeWebappDispatcher(request.getServletContext(), delegator);
                LoginWorker.setWebContextObjects(request, response, delegator, dispatcher);
            }
            // found userLogin, do the external login...

            // if the user is already logged in and the login is different, logout the other user
            HttpSession session = request.getSession();
            GenericValue currentUserLogin = (GenericValue) session.getAttribute("userLogin");
            if (currentUserLogin != null) {
                if (currentUserLogin.getString("userLoginId").equals(userLogin.getString("userLoginId"))) {
                    // same user, just make sure the autoUserLogin is set to the same and that the client cookie has the correct userLoginId
                    LoginWorker.autoLoginSet(request, response);
                    // Same for the SecuredLoginId cookie
                    LoginWorker.createSecuredLoginIdCookie(request, response);
                    return "success";
                }

                // logout the current user and login the new user...
                LoginWorker.logout(request, response);
                // ignore the return value; even if the operation failed we want to set the new UserLogin
            }

            // check userLogin base permission and if it is enabled
            request.getSession().setAttribute("userLogin", userLogin);
            userLogin = LoginWorker.checkLogout(request, response);

            LoginWorker.doBasicLogin(userLogin, request);

            // Create a secured cookie with the correct userLoginId
            LoginWorker.createSecuredLoginIdCookie(request, response);

        } else {
            Debug.logWarning("Could not find userLogin for external login key: " + externalKey, MODULE);
        }

        // make sure the autoUserLogin is set to the same and that the client cookie has the correct userLoginId
        LoginWorker.autoLoginSet(request, response);
        return "success";
    }

    /**
     * Checks if the request is an Ajax request.
     * @param request the request to check
     * @return indicator
     */
    private static boolean isAjax(HttpServletRequest request) {
        return "XMLHttpRequest".equals(request.getHeader("X-Requested-With"));
    }

    /**
     * Check if using externalLoginKey
     * @return
     */
    public static boolean isExternalLoginKeyEnabled(HttpServletRequest request) {
        if (UtilValidate.isEmpty(isExternalLoginKeyEnabled)) {
            isExternalLoginKeyEnabled = EntityUtilProperties.getPropertyValue("security",
                    "security.login.externalLoginKey.enabled", "true",
                    (Delegator) request.getAttribute("delegator"));
        }
        return "true".equals(isExternalLoginKeyEnabled);
    }

}
