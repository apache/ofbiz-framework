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

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilHttp;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.DelegatorFactory;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.EntityUtilProperties;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ModelService;
import org.apache.ofbiz.service.ServiceUtil;
import org.apache.ofbiz.webapp.WebAppUtil;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.SignatureException;

/**
 * This class manages the single sign-on authentication through JWT tokens between OFBiz applications.
 */
public class JWTManager {
    private static final String module = JWTManager.class.getName();

    /**
     * OFBiz controller preprocessor event.
     *
     * The method is designed to be used in a chain of controller preprocessor event: it always returns "success"
     * even when the Authorization token is missing or the Authorization fails.
     * This in order to move the processing to the next event in the chain.
     *
     * This works in a similar same way than externalLoginKey but between 2 servers on 2 different domains,
     * not 2 webapps on the same server.
     *
     * The OFBiz internal Single Sign On (SSO) is ensured by a JWT token,
     * then all is handled as normal by a session on the reached server.
     *
     * The servers may or may not share a database but the 2 loginUserIds must be the same.
     *
     * In case of a multitenancy usage, the tenant is verified.
     * @param request The HTTPRequest object for the current request
     * @param response The HTTPResponse object for the current request
     * @return String  always "success"
     */
    public static String checkJWTLogin(HttpServletRequest request, HttpServletResponse response) {
        Delegator delegator = (Delegator) request.getAttribute("delegator");

        if(!"true".equals(EntityUtilProperties.getPropertyValue("security", "security.internal.sso.enabled", "false", delegator))) {
            if(Debug.verboseOn()) {
                Debug.logVerbose("Internal single sign on is disabled.", module);
            }
            return "success";
        }

        // we are only interested in the header entry "Authorization" containing "Bearer <token>"
        String jwtToken = getHeaderAuthBearerToken(request);
        if (jwtToken == null) {
            // No Authorization header, no need to continue.
            return "success";
        }

        Map<String, Object> claims = validateJwtToken(jwtToken, getJWTKey(delegator));
        if (claims.containsKey(ModelService.ERROR_MESSAGE)) {
            // The JWT is wrong somehow, stop the process, details are in log
            return "success";
        }

        // get userLoginId from the token and retrieve the corresponding userLogin from the database
        GenericValue userLogin = getUserlogin(delegator, claims);

        if(UtilValidate.isNotEmpty(userLogin)) {
            // check userLogin base permission and if it is enabled
            request.getSession().setAttribute("userLogin", userLogin);
            userLogin = LoginWorker.checkLogout(request, response);
        }

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
     * Get the JWT secret key from database or security.properties.
     * @param delegator the delegator
     * @return the JWT secret key
     */
    public static String getJWTKey(Delegator delegator) {
        return EntityUtilProperties.getPropertyValue("security", "security.token.key", delegator);
    }

     /**
     * Get the authentication token based for user
     * This takes OOTB username/password and if user is authenticated it will generate the JWT token using a secret key.
     *
     * @param request the http request in which the authentication token is searched and stored
     * @return the authentication token
     */
    public static String getAuthenticationToken(HttpServletRequest request, HttpServletResponse response) {
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        Delegator delegator = (Delegator) request.getAttribute("delegator");

        String username, password;

        if (UtilValidate.isNotEmpty(request.getAttribute("USERNAME"))) {
            username = (String) request.getAttribute("USERNAME");
        } else {
            username = request.getParameter("USERNAME");
        }
        if (UtilValidate.isNotEmpty(request.getAttribute("PASSWORD"))) {
            password = (String) request.getAttribute("PASSWORD");
        } else {
            password = request.getParameter("PASSWORD");
        }

        if (UtilValidate.isEmpty(username) || UtilValidate.isEmpty(password)) {
            request.setAttribute("_ERROR_MESSAGE_", "Username / Password can not be empty");
            Debug.logError("UserName / Password can not be empty", module);
            return "error";
        }
        Map<String, Object> result;
        try {
            result = dispatcher.runSync("userLogin", UtilMisc.toMap("login.username", username, "login.password", password, "locale", UtilHttp.getLocale(request)));
        } catch (GenericServiceException e) {
            Debug.logError(e, "Error calling userLogin service", module);
            request.setAttribute("_ERROR_MESSAGE_", e.getMessage());
            return "error";
        }
        if (!ServiceUtil.isSuccess(result)) {
            Debug.logError(ServiceUtil.getErrorMessage(result), module);
            request.setAttribute("_ERROR_MESSAGE_", ServiceUtil.getErrorMessage(result));
            return "error";
        }
        GenericValue userLogin = (GenericValue) result.get("userLogin");

        String token = createJwt(delegator, UtilMisc.toMap("userLoginId", userLogin.getString("userLoginId")));
        if (token == null) {
            Debug.logError("Unable to generate token", module);
            request.setAttribute("_ERROR_MESSAGE_", "Unable to generate token");
            return "error";
        }
        request.setAttribute("token", token);
        return "success";
    }

    /**
     * Gets the authentication token from the "Authorization" header if it is
     * in the form {@code Bearer <token>}.
     *
     * Public for API access from third party code.
     *
     * @param request the request to get the token from
     * @return the bare JWT token
     */
    public static String getHeaderAuthBearerToken(HttpServletRequest request) {

        String headerAuthValue = request.getHeader(HttpHeaders.AUTHORIZATION);
        String bearerPrefix = "Bearer ";

        if(UtilValidate.isEmpty(headerAuthValue) || !headerAuthValue.startsWith(bearerPrefix)) {
            return null;
        }

        // remove prefix and any leading/trailing spaces and return the bare token
        return headerAuthValue.replaceFirst(bearerPrefix, "").trim();
    }

    /* Validates the provided token using the secret key.
     * If the token is valid it will get the conteined claims and return them.
     * If token validation failed it will return an error.
     * Public for API access from third party code.
     *
     * @param token the JWT token
     * @param key the server side key to verify the signature
     * @return Map of the claims contained in the token
     */
    public static Map<String, Object> validateToken(String jwtToken, String key) {
        Map<String, Object> result = new HashMap<>();
        if (UtilValidate.isEmpty(jwtToken) || UtilValidate.isEmpty(key)) {
            String msg = "JWT token or key can not be empty.";
            Debug.logError(msg, module);
            result.put(ModelService.ERROR_MESSAGE, msg);
            return result;
        }
        try {
            Claims claims = Jwts.parser().setSigningKey(key.getBytes()).parseClaimsJws(jwtToken).getBody();
            //OK, we can trust this JWT
            result.putAll(claims);
            return result;
        } catch (SignatureException | ExpiredJwtException e) {
            // signature not valid or token expired
            Debug.logError(e.getMessage(), module);
            result.put(ModelService.ERROR_MESSAGE, e.getMessage());
            return result;
        }
    }

    /**
     * Create and return a JWT token using the claims of the provided map and the configured expiration time.
     * @param delegator the delegator
     * @param claims the map containing the JWT claims
     * @return a JWT token
     */
    public static String createJwt(Delegator delegator, Map<String, String> claims) {
        int expirationTime = Integer.parseInt(EntityUtilProperties.getPropertyValue("security", "security.jwt.token.expireTime", "1800",  delegator));
        return createJwt(delegator, claims, expirationTime);
    }

    /* Create and return a JWT token using the claims of the provided map and the provided expiration time.
     *
     * @param delegator
     * @param tokenMap the map containing the JWT claims
     * @param expireTime the expiration time in seconds
     * @return a JWT token
     */
    public static String createJwt(Delegator delegator, Map<String, String> claims, int expireTime) {
        String key = JWTManager.getJWTKey(delegator);

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(UtilDateTime.nowTimestamp().getTime());
        cal.add(Calendar.SECOND, expireTime);

        JwtBuilder builder = Jwts.builder()
                .setExpiration(cal.getTime())
                .setIssuedAt(UtilDateTime.nowTimestamp())
                .signWith(SignatureAlgorithm.HS512, key.getBytes());

        for (Map.Entry<String, String> entry : claims.entrySet()) {
            builder.claim(entry.getKey(), entry.getValue());
        }
        return builder.compact();
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

        String oldDelegatorName = delegator.getDelegatorName();
        ServletContext servletContext = request.getSession().getServletContext();
        if (!oldDelegatorName.equals(userLogin.getDelegator().getDelegatorName())) {
            delegator = DelegatorFactory.getDelegator(userLogin.getDelegator().getDelegatorName());
            LocalDispatcher dispatcher = WebAppUtil.makeWebappDispatcher(servletContext, delegator);
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

        if(UtilValidate.isEmpty(userLoginId)) {
            Debug.logWarning("No userLoginId found in the JWT token.", module);
            return null;
        }

        GenericValue userLogin = null;
        try {
            userLogin = EntityQuery.use(delegator).from("UserLogin").where("userLoginId", userLoginId).queryOne();
            if (userLogin == null) {
                Debug.logWarning("There was a problem with the JWT token. Could not find provided userLogin " + userLoginId, module);
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, "Cannot get UserLogin information: " + e.getMessage(), module);
        }
        return userLogin;
    }

    /**
     * Validate the token usingJWTManager::validateToken
     * If it fails, returns a ModelService.ERROR_MESSAGE in the result
     * @param jwtToken The JWT which normally contains the userLoginId
     * @param key the secret key to decrypt the token
     * @return Map of name, value pairs composing the result
     */
    private static Map<String, Object> validateJwtToken(String jwtToken, String key) {
        Map<String, Object> result = validateToken(jwtToken, key);
        if (result.containsKey(ModelService.ERROR_MESSAGE)) {
            // Something unexpected happened here
            Debug.logWarning("There was a problem with the JWT token, no single sign on user login possible.", module);
        }
        return result;
    }
}
