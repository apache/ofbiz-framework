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

import java.net.MalformedURLException;
import java.net.URL;
import java.security.interfaces.RSAPublicKey;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.HttpHeaders;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.StringUtil;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilHttp;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
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

import com.auth0.jwk.Jwk;
import com.auth0.jwk.JwkProvider;
import com.auth0.jwk.JwkProviderBuilder;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;


/**
 * This class manages the single sign-on authentication through JWT tokens between OFBiz applications.
 */
public class JWTManager {
    private static final String MODULE = JWTManager.class.getName();

    // Static map of thread-safe JwkProvider instances for each delegator.
    private static volatile Map<String, JwkProvider> jwkProviders = new ConcurrentHashMap<>();
    /**
     * Returns a shared, thread-safe JwkProvider instance for the delegator.
     */
    private static JwkProvider getJwkProvider(Delegator delegator) throws IllegalStateException, MalformedURLException {
        JwkProvider localRef = jwkProviders.get(delegator.getDelegatorName());
        if (localRef == null) {
            synchronized (JWTManager.class) {
                localRef = jwkProviders.get(delegator.getDelegatorName());
                if (localRef == null) {
                    String issuer = EntityUtilProperties.getPropertyValue("security", "security.token.issuer", "", delegator);
                    String jwksUrl = issuer + "/protocol/openid-connect/certs";
                    localRef = new JwkProviderBuilder(new URL(jwksUrl))
                            .cached(10, 24, TimeUnit.HOURS)   // cache up to 10 keys for 24h
                            .rateLimited(10, 1, TimeUnit.MINUTES) // prevent frequent fetches
                            .build();
                    jwkProviders.put(delegator.getDelegatorName(), localRef);
                }
            }
        }
        return localRef;
    }
    /**
     * OFBiz controller preprocessor event.
     * The method is designed to be used in a chain of controller preprocessor event: it always returns "success"
     * even when the Authorization token is missing or the Authorization fails.
     * This in order to move the processing to the next event in the chain.
     * This works in a similar same way than externalLoginKey but between 2 servers on 2 different domains,
     * not 2 webapps on the same server.
     * The OFBiz internal Single Sign On (SSO) is ensured by a JWT token,
     * then all is handled as normal by a session on the reached server.
     * The servers may or may not share a database but the 2 loginUserIds must be the same.
     * In case of a multitenancy usage, the tenant is verified.
     * @param request The HTTPRequest object for the current request
     * @param response The HTTPResponse object for the current request
     * @return String  always "success"
     */
    public static String checkJWTLogin(HttpServletRequest request, HttpServletResponse response) {
        Delegator delegator = (Delegator) request.getAttribute("delegator");

        if (!"true".equals(EntityUtilProperties.getPropertyValue("security", "security.internal.sso.enabled", "false", delegator))) {
            if (Debug.verboseOn()) {
                Debug.logVerbose("Internal single sign on is disabled.", MODULE);
            }
            return "success";
        }

        // we are only interested in the header entry "Authorization" containing "Bearer <token>"
        String jwtToken = getHeaderAuthBearerToken(request);
        if (jwtToken == null) {
            // No Authorization header, no need to continue.
            return "success";
        }

        Map<String, Object> claims = validateJwtToken(delegator, jwtToken);
        if (claims.containsKey(ModelService.ERROR_MESSAGE)) {
            // The JWT is wrong somehow, stop the process, details are in log
            return "success";
        }

        // get userLoginId from the token and retrieve the corresponding userLogin from the database
        GenericValue userLogin = getUserlogin(delegator, claims);

        if (UtilValidate.isNotEmpty(userLogin)) {
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

        LoginWorker.doBasicLogin(userLogin, request, response);
        return "success";
    }

    /**
     * Get the JWT secret key from database or security.properties.
     * @param delegator the delegator
     * @return the JWT secret key
     */
    private static String getJWTKey(Delegator delegator, String salt) {
        String key = UtilProperties.getPropertyValue("security", "security.token.key");
        if (key.length() < 64) { // The key must be 512 bits (ie 64 chars)  as we use HMAC512 to create the token, cf. OFBIZ-12724
            throw new SecurityException("The JWT secret key is too short. It must be at least 512 bites.");
        }
        if (salt != null) {
            return StringUtil.toHexString(salt.getBytes()) + key;
        }
        return key;
    }

    /**
     * Get the authentication token based for user
     * This takes OOTB username/password and if user is authenticated it will generate the JWT token using a secret key.
     * @param request the http request in which the authentication token is searched and stored
     * @return the authentication token
     */
    public static String getAuthenticationToken(HttpServletRequest request, HttpServletResponse response) {
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        Delegator delegator = (Delegator) request.getAttribute("delegator");

        String username;
        String password;

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
            Debug.logError("UserName / Password can not be empty", MODULE);
            return "error";
        }
        Map<String, Object> result;
        try {
            result = dispatcher.runSync("userLogin", UtilMisc.toMap("login.username", username, "login.password", password,
                    "locale", UtilHttp.getLocale(request)));
        } catch (GenericServiceException e) {
            Debug.logError(e, "Error calling userLogin service", MODULE);
            request.setAttribute("_ERROR_MESSAGE_", e.getMessage());
            return "error";
        }
        if (!ServiceUtil.isSuccess(result)) {
            Debug.logError(ServiceUtil.getErrorMessage(result), MODULE);
            request.setAttribute("_ERROR_MESSAGE_", ServiceUtil.getErrorMessage(result));
            return "error";
        }
        GenericValue userLogin = (GenericValue) result.get("userLogin");

        String token = createJwt(delegator, UtilMisc.toMap("userLoginId", userLogin.getString("userLoginId")));
        if (token == null) {
            Debug.logError("Unable to generate token", MODULE);
            request.setAttribute("_ERROR_MESSAGE_", "Unable to generate token");
            return "error";
        }
        request.setAttribute("token", token);
        return "success";
    }

    /**
     * Gets the authentication token from the "Authorization" header if it is
     * in the form {@code Bearer <token>}.
     * Public for API access from third party code.
     * @param request the request to get the token from
     * @return the bare JWT token
     */
    public static String getHeaderAuthBearerToken(HttpServletRequest request) {

        String headerAuthValue = request.getHeader(HttpHeaders.AUTHORIZATION);
        String bearerPrefix = "Bearer ";

        if (UtilValidate.isEmpty(headerAuthValue) || !headerAuthValue.startsWith(bearerPrefix)) {
            return null;
        }

        // remove prefix and any leading/trailing spaces and return the bare token
        return headerAuthValue.replaceFirst(bearerPrefix, "").trim();
    }

    /**
     * Validates a JSON Web Token (JWT) and extracts its claims.
     *
     * This method supports two validation modes:
     *   External authentication server (JWK-based): if an issuer is configured
     *     in the "security.token.issuer" property, the token is verified using a JWK provider and
     *     the issuer's public key used to sign the token.
     *   Local HMAC verification: If no issuer is configured, the token is verified
     *     locally using an HMAC key derived from the secret key configured
     *     in the "security.token.key" (and optionally a salt).
     *
     * If the token is successfully verified, the contained claims are returned as a map.
     * Otherwise, an error map is returned containing the failure message.
     *
     * @param delegator the delegator used to retrieve security properties and keys from a database
     * @param jwtToken  the JWT string to validate
     * @param keySalt   an optional salt used when building the local HMAC key (can be null or empty)
     * @return a map containing:
     *   the token claims if validation succeeds
     *   an error entry if validation fails
     */
    public static Map<String, Object> validateToken(Delegator delegator, String jwtToken, String keySalt) {
        JWTVerifier verifier = null;
        // Retrieve configured issuer (if present, assume external JWK-based validation)
        String issuer = EntityUtilProperties.getPropertyValue("security", "security.token.issuer", "", delegator);
        if (UtilValidate.isNotEmpty(issuer)) {
            String audience = EntityUtilProperties.getPropertyValue("security", "security.token.audience", "", delegator);
            try {
                // Decode the token to extract the Key ID (kid)
                DecodedJWT decodedJWT = JWT.decode(jwtToken);
                String kid = decodedJWT.getKeyId();

                // Fetch the corresponding JWK (JSON Web Key) for this Key ID
                JwkProvider provider = getJwkProvider(delegator);
                Jwk jwk = provider.get(kid);

                // Build the RSA256 Algorithm using the JWK’s public key
                Algorithm algorithm = Algorithm.RSA256((RSAPublicKey) jwk.getPublicKey(), null);

                // Create a JWT verifier: include expected issuer and audience for safety
                verifier = JWT.require(algorithm)
                        .withIssuer(issuer)
                        .withAudience(audience)
                        .build();
            } catch (Exception e) {
                String msg = "JWT token: unable to build a token verifier for tokens issued by " + issuer;
                Debug.logError(msg, MODULE);
                return ServiceUtil.returnError(msg);
            }
        } else {
            // Fallback: validate using local secret key
            String key = getJWTKey(delegator, keySalt);
            if (UtilValidate.isEmpty(jwtToken) || UtilValidate.isEmpty(key)) {
                String msg = "JWT token or key can not be empty.";
                Debug.logError(msg, MODULE);
                return ServiceUtil.returnError(msg);
            }
            verifier = JWT.require(Algorithm.HMAC512(key))
                    .withIssuer("ApacheOFBiz")
                    .build();
        }
        if (UtilValidate.isEmpty(verifier)) {
            String msg = "JWT token or key can not be empty.";
            Debug.logError(msg, MODULE);
            return ServiceUtil.returnError(msg);
        }
        try {
            Map<String, Object> result = new HashMap<>();
            DecodedJWT jwt = verifier.verify(jwtToken);
            Map<String, Claim> claims = jwt.getClaims();
            //OK, we can trust this JWT
            for (Map.Entry<String, Claim> entry : claims.entrySet()) {
                result.put(entry.getKey(), entry.getValue().asString());
            }
            return result;
        } catch (JWTVerificationException e) {
            // signature not valid or token expired
            Debug.logError(e.getMessage(), MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }
    }

    /**
     * Validates a JSON Web Token (JWT) and extracts its claims using the default validation process.
     *
     * This method is a convenience overload that calls validateToken(Delegator, String, String)
     * without providing a key salt. The validation will use either an external authentication
     * server (if configured) or the locally stored secret key.
     *
     * If the token is successfully verified, the contained claims are returned as a map.
     * If validation fails, an error map is returned containing details about the failure.
     *
     * @param delegator the delegator used to retrieve security properties and keys from the database
     * @param jwtToken  the JWT string to validate
     * @return a map containing the token claims if validation succeeds,
     *         or an error entry if validation fails
     * @see #validateToken(Delegator, String, String)
     */
    public static Map<String, Object> validateToken(Delegator delegator, String jwtToken) {
        return validateToken(delegator, jwtToken, null);
    }

    /**
     * Create and return a JWT token using the claims of the provided map and the configured expiration time.
     * @param delegator the delegator
     * @param claims the map containing the JWT claims
     * @return a JWT token
     */
    public static String createJwt(Delegator delegator, Map<String, String> claims) {
        int expirationTime = Integer.parseInt(EntityUtilProperties.getPropertyValue("security", "security.jwt.token.expireTime", "1800", delegator));
        return createJwt(delegator, claims, expirationTime);
    }

    /** Create and return a JWT token using the claims of the provided map and the provided expiration time.
     * @param delegator
     * @param claims the map containing the JWT claims
     * @param expireTime the expiration time in seconds
     * @return a JWT token
     */
    public static String createJwt(Delegator delegator, Map<String, String> claims, int expireTime) {
        return createJwt(delegator, claims, null, expireTime);
    }

    /** Create and return a JWT token using the claims of the provided map and the provided expiration time.
     * @param delegator
     * @param claims the map containing the JWT claims
     * @param keySalt salt to use as prefix on the encrypt key
     * @param expireTime the expiration time in seconds
     * @return a JWT token
     */
    public static String createJwt(Delegator delegator, Map<String, String> claims, String keySalt, int expireTime) {
        if (expireTime <= 0) {
            expireTime = Integer.parseInt(EntityUtilProperties.getPropertyValue("security", "security.jwt.token.expireTime", "1800", delegator));
        }

        String key = JWTManager.getJWTKey(delegator, keySalt);

        Calendar cal = Calendar.getInstance();
        Timestamp now = UtilDateTime.nowTimestamp();
        cal.setTimeInMillis(now.getTime());
        cal.add(Calendar.SECOND, expireTime);

        JWTCreator.Builder builder = JWT.create()
                .withIssuedAt(now)
                .withExpiresAt(cal.getTime())
                .withIssuer("ApacheOFBiz");
        for (Map.Entry<String, String> entry : claims.entrySet()) {
            builder.withClaim(entry.getKey(), entry.getValue());
        }

        return builder.sign(Algorithm.HMAC512(key));
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
                Debug.logError(e, "Cannot store UserLogin information: " + e.getMessage(), MODULE);
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

        if (UtilValidate.isEmpty(userLoginId)) {
            Debug.logWarning("No userLoginId found in the JWT token.", MODULE);
            return null;
        }

        GenericValue userLogin = null;
        try {
            userLogin = EntityQuery.use(delegator).from("UserLogin").where("userLoginId", userLoginId).queryOne();
            if (userLogin == null) {
                Debug.logWarning("There was a problem with the JWT token. Could not find provided userLogin " + userLoginId, MODULE);
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, "Cannot get UserLogin information: " + e.getMessage(), MODULE);
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
    private static Map<String, Object> validateJwtToken(Delegator delegator, String jwtToken) {
        Map<String, Object> result = validateToken(delegator, jwtToken);
        if (result.containsKey(ModelService.ERROR_MESSAGE)) {
            // Something unexpected happened here
            Debug.logWarning("There was a problem with the JWT token, no single sign on user login possible.", MODULE);
        }
        return result;
    }

    public static String createRefreshToken(Delegator delegator, String userLoginId) {
        int refreshTokenExpireTime = Integer.parseInt(EntityUtilProperties.getPropertyValue("security",
                "security.jwt.refresh.token.expireTime", "86400", delegator));
        return createJwt(delegator, UtilMisc.toMap("userLoginId", userLoginId, "type", "refresh"), refreshTokenExpireTime);
    }

    public static Map<String, Object> validateRefreshToken(Delegator delegator, String refreshToken) {
        Map<String, Object> claims = validateToken(delegator, refreshToken);
        if (!claims.containsKey("type") || !"refresh".equals(claims.get("type"))) {
            return ServiceUtil.returnError("Invalid refresh token.");
        }
        return claims;
    }
}
