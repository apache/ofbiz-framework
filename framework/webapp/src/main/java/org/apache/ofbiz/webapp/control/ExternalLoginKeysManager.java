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

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.DelegatorFactory;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ModelService;
import org.apache.ofbiz.webapp.WebAppUtil;

/**
 * This class manages the authentication tokens that provide single sign-on authentication to the OFBiz applications.
 */
public class ExternalLoginKeysManager {
    private static final String module = ExternalLoginKeysManager.class.getName();
    private static final String EXTERNAL_LOGIN_KEY_ATTR = "externalLoginKey";
    // This Map is keyed by the randomly generated externalLoginKey and the value is a UserLogin GenericValue object
    private static final Map<String, GenericValue> externalLoginKeys = new ConcurrentHashMap<>();

    /**
     * Gets (and creates if necessary) an authentication token to be used for an external login parameter.
     * When a new token is created, it is persisted in the web session and in the web request and map entry keyed by the
     * token and valued by a userLogin object is added to a map that is looked up for subsequent requests.
     *
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

                externalLoginKeys.remove(sesExtKey);
            }

            GenericValue userLogin = (GenericValue) request.getAttribute("userLogin");
            //check the userLogin here, after the old session setting is set so that it will always be cleared
            if (userLogin == null) return "";

            //no key made yet for this request, create one
            while (externalKey == null || externalLoginKeys.containsKey(externalKey)) {
                UUID uuid = UUID.randomUUID();
                externalKey = "EL" + uuid.toString();
            }

            request.setAttribute(EXTERNAL_LOGIN_KEY_ATTR, externalKey);
            session.setAttribute(EXTERNAL_LOGIN_KEY_ATTR, externalKey);
            externalLoginKeys.put(externalKey, userLogin);
            return externalKey;
        }
    }

    /**
     * Removes the authentication token, if any, from the session.
     *
     * @param session - the http session from which the authentication token is removed
     */
    static void cleanupExternalLoginKey(HttpSession session) {
        String sesExtKey = (String) session.getAttribute(EXTERNAL_LOGIN_KEY_ATTR);
        if (sesExtKey != null) {
            externalLoginKeys.remove(sesExtKey);
        }
    }

    /**
     * OFBiz controller event that performs the user authentication using the authentication token.
     * The method is designed to be used in a chain of controller preprocessor event: it always return "success"
     * even when the authentication token is missing or the authentication fails in order to move the processing to the
     * next event in the chain.
 
     *
     * @param request - the http request object
     * @param response - the http response object
     * @return - &amp;success&amp; in all the cases
     */
    public static String checkExternalLoginKey(HttpServletRequest request, HttpServletResponse response) {
        String externalKey = request.getParameter(EXTERNAL_LOGIN_KEY_ATTR);
        if (externalKey == null) return "success";

        GenericValue userLogin = externalLoginKeys.get(externalKey);
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
                    // Create a secured cookie the client cookie with the correct userLoginId
                    LoginWorker.createSecuredLoginIdCookie(request, response);
                    
                    // same user, just make sure the autoUserLogin is set to the same and that the client cookie has the correct userLoginId
                    LoginWorker.autoLoginSet(request, response);
                    return "success";
                }

                // logout the current user and login the new user...
                LoginWorker.logout(request, response);
                // ignore the return value; even if the operation failed we want to set the new UserLogin
            }

            LoginWorker.doBasicLogin(userLogin, request);
        } else {
            Debug.logWarning("Could not find userLogin for external login key: " + externalKey, module);
        }

        // make sure the autoUserLogin is set to the same and that the client cookie has the correct userLoginId
        LoginWorker.autoLoginSet(request, response);
        
        return "success";
    }

    private static boolean isAjax(HttpServletRequest request) {
       return "XMLHttpRequest".equals(request.getHeader("X-Requested-With"));
    }

    /**
    * OFBiz controller preprocessor event
    * The method is designed to be used in a chain of controller preprocessor event: it always return "success"
    * even when the Authorization token is missing or the Authorization fails.
    * This in order to move the processing to the next event in the chain.
    * 
    * This works in a similar same way than externalLoginKey but between 2 servers on 2 different domains, 
    * not 2 webapps on the same server.
    *  
    * The Single Sign On (SSO) is ensured by a JWT token, 
    * then all is handled as normal by a session on the reached server.
    *  
    * The servers may or may not share a database but the 2 loginUserIds must be the same.
    * 
    * In case of a multitenancy usage, the tenant is verified.
    * @param request The HTTPRequest object for the current request
    * @param response The HTTPResponse object for the current request
    * @return String "success" 
    */
    public static String checkJWTLogin(HttpServletRequest request, HttpServletResponse response) {
        Delegator delegator = (Delegator) request.getAttribute("delegator");

        Map<String, Object> result = null;
        String authorizationHeader = request.getHeader("Authorization");
        if (authorizationHeader == null) {
            // No Authorization header, no need to continue, most likely case.
            return "success";
        }

        result = jwtValidation(delegator, authorizationHeader);
        if (result.containsKey(ModelService.ERROR_MESSAGE)) {
            // The JWT is wrong somehow, stop the process, details are in log
            return "success";
        }

        GenericValue userLogin = getUserlogin(delegator, result);
        if (userLogin == null) {
            // No UserLogin GenericValue could be retrieved, stop the process, details are in log 
            return "success";
        }

        checkTenant(request, response, delegator, userLogin);

        if (!storeUserlogin(userLogin)) {
            // We could not store the UserLogin GenericValue (very unlikely), stop the process, details are in log
            return "success";
        }

        LoginWorker.doBasicLogin(userLogin, request);
        return "success";
    }

    /**
     * Checks it's the right tenant in case username and password are the same in different tenants
     * If not, sets the necessary session attributes
     * @param request The HTTPRequest object for the current request
     * @param response The HTTPResponse object for the current request
     * @param delegator The current delegator
     * @param userLogin The GenericValue object of userLogin to check
     */
    private static void checkTenant(HttpServletRequest request, HttpServletResponse response, Delegator delegator,
            GenericValue userLogin) {
        // 
        // 

        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        String oldDelegatorName = delegator.getDelegatorName();
        ServletContext servletContext = request.getSession().getServletContext();
        if (!oldDelegatorName.equals(userLogin.getDelegator().getDelegatorName())) {
            delegator = DelegatorFactory.getDelegator(userLogin.getDelegator().getDelegatorName());
            dispatcher = WebAppUtil.makeWebappDispatcher(servletContext, delegator);
            LoginWorker.setWebContextObjects(request, response, delegator, dispatcher);
        }
    }

    /**
     * Stores the userLogin in DB. If it fails log an error message
     * @param userLogin The userLogin GenericValue to store
     * @return boolean True if it works, log an error message if it fails 
     */
    private static boolean storeUserlogin(GenericValue userLogin) {
        String enabled = userLogin.getString("enabled");
        if (enabled == null || "Y".equals(enabled)) {
            userLogin.set("hasLoggedOut", "N");
            try {
                userLogin.store();
            } catch (GenericEntityException e) {
                Debug.logError(e, "Cannot store UserLogin information: " + e.getMessage(), module);
                return false;
            }
        }
        return true;
    }

    /**
     * Gets the userLogin from the userLoginId in the result of the JWT validation
     * If it fails, log a warning or error message  
     * @param delegator The current delegator
     * @param jwtMap Map of name, value pairs composing the result of the JWT validation
     * @return userLogin The userLogin GenericValue extracted from DB 
     */
    private static GenericValue getUserlogin(Delegator delegator, Map<String, Object> jwtMap) {
        String userLoginId = (String) jwtMap.get("userLoginId");
        GenericValue userLogin = null;
        try {
            userLogin = EntityQuery.use(delegator).from("UserLogin").where("userLoginId", userLoginId).queryOne();
            if (userLogin == null) {
                Debug.logWarning("*** There was a problem with the JWT token. Could not find userLogin " + userLoginId, module);
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, "Cannot get UserLogin information: " + e.getMessage(), module);
        }
        return userLogin;
    }

    /**
     * Validate the token usingJWTManager::validateToken
     * If it fails, returns a ModelService.ERROR_MESSAGE in the result
     * @param delegator The current delegator
     * @param authorizationHeader The JWT which normally contains the userLoginId
     * @param result  Map of name, value pairs composing the result 
     */
    private static Map<String, Object> jwtValidation(Delegator delegator, String authorizationHeader) {
        Map<String, Object> result;
        List<String> types = Arrays.asList("userLoginId");
        result = JWTManager.validateToken(delegator, authorizationHeader, types);
        if (result.containsKey(ModelService.ERROR_MESSAGE)) {
            // Something unexpected happened here  
            Debug.logWarning("*** There was a problem with the JWT, not signin in the user login ", module);
        }        
        return result;
    }
    
}
