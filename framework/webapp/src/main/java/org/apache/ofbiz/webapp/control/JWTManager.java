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
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilHttp;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityUtilProperties;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ModelService;
import org.apache.ofbiz.service.ServiceUtil;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.SignatureException;

/**
 * This class manages the authentication tokens
 */
public class JWTManager {
    private static final String module = JWTManager.class.getName();

     /**
     * Get the authentication token based for user
     * This takes OOTB username/password and if user is authenticated it will generate JJWT token using secreate key
     *
     * @param request - the http request in which the authentication token is searched and stored
     * @return the authentication token
     */

    public static String getAuthenticationToken(HttpServletRequest request, HttpServletResponse response){
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        String username = request.getParameter("USERNAME");
        String password = request.getParameter("PASSWORD");

        if (UtilValidate.isNotEmpty(request.getAttribute("USERNAME"))) {
            username = (String) request.getAttribute("USERNAME");
        }
        if (UtilValidate.isNotEmpty(request.getAttribute("PASSWORD"))) {
            password = (String) request.getAttribute("PASSWORD");
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

        String token = createJwt (delegator, UtilMisc.toMap("userLoginId", userLogin.getString("userLoginId")));
        if (token == null) {
            Debug.logError("Unable to generate token", module);
            request.setAttribute("_ERROR_MESSAGE_", "Unable to generate token");
            return "error";
        }
        request.setAttribute("token",token);
        return "success";
    }

    /* This method will be used to validate token,
     * If token is valid it will get the claims and return
     * If token validation failed it will return error
     *
     * @param delegator
     * @param token
     * @param types  List of string that will be extracted from token claims if found
     * @param result  Map of name, value pairs composing the result
     */
    public static Map<String, Object> validateToken(Delegator delegator, String token, List<String> types) {
        Map<String, Object> result = new HashMap<String, Object>();
        if (UtilValidate.isEmpty(token)) {
            Debug.logError("Token can not be empty", module);
            result.put(ModelService.ERROR_MESSAGE, "Token can not be empty.");
            return result;
        }
        try {
            String key = EntityUtilProperties.getPropertyValue("security", "security.token.key", "ofbiz", delegator);
            Claims claims = Jwts.parser().setSigningKey(key).parseClaimsJws(token).getBody();
            //OK, we can trust this JWT
            for (int i = 0; i < types.size(); i++) {
                result.put(types.get(i), (String) claims.get(types.get(i)));
            }
            return result;
        } catch (SignatureException e) {
            //don't trust the JWT!
            Debug.logError(e.getMessage(), module);
            result.put(ModelService.ERROR_MESSAGE, e.getMessage());
            return result;
        } catch (ExpiredJwtException e) {
            //Token Expired: Ask for login again.
            Debug.logError(e.getMessage(), module);
            result.put(ModelService.ERROR_MESSAGE, e.getMessage());
            return result;
        }
    }

    public static String createJwt(Delegator delegator, Map<String, String> tokenMap) {
        int expirationTime = Integer.parseInt(EntityUtilProperties.getPropertyValue("security", "security.jwt.token.expireTime", "1800",  delegator));
        return createJwt(delegator, tokenMap, expirationTime);
    }

    /* Generate and return a JWT key
     *
     * @param delegator
     * @param tokenMap Map name, value pairs to set as claims
     * @param expirationtime the expiration time in seconds
     * @return a JWT token
     */
    public static String createJwt (Delegator delegator, Map<String, String> claims, int expireTime) {
        String key = EntityUtilProperties.getPropertyValue("security", "security.token.key", "ofbiz", delegator);

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(UtilDateTime.nowTimestamp().getTime());
        cal.add(Calendar.SECOND, expireTime);

        JwtBuilder builder = Jwts.builder()
                .setExpiration(cal.getTime())
                .setIssuedAt(UtilDateTime.nowTimestamp())
                .signWith(SignatureAlgorithm.HS512, key);

        for (Map.Entry<String, String> entry : claims.entrySet()) {
            builder.claim(entry.getKey(), entry.getValue());
        }
        return builder.compact();
    }
}
