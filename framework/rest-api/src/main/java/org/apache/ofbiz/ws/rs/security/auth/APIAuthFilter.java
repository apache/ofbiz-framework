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
package org.apache.ofbiz.ws.rs.security.auth;

import java.io.IOException;
import java.util.Map;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.service.ModelService;
import org.apache.ofbiz.webapp.control.JWTManager;
import org.apache.ofbiz.ws.rs.annotation.Secured;
import org.apache.ofbiz.ws.rs.common.AuthenticationScheme;
import org.apache.ofbiz.ws.rs.util.RestApiUtil;

import jakarta.annotation.Priority;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

/**
 * JAX-RS container request filter that enforces JWT Bearer token authentication
 * on all resources annotated with {@link Secured}.
 *
 * <p>On successful validation, the resolved {@link GenericValue} userLogin is
 * stored as a request attribute under {@code "userLogin"} for use by downstream
 * resource methods. On failure, the request is aborted with HTTP 401 Unauthorized.
 * If no {@code Authorization} header was present, a
 * {@code WWW-Authenticate: Bearer realm="OFBiz"} challenge header is included
 * in the response.</p>
 *
 * @see Secured
 * @see HttpBasicAuthFilter
 */
@Secured
@Provider
@Priority(Priorities.AUTHORIZATION)
public class APIAuthFilter implements ContainerRequestFilter {

    private static final String MODULE = APIAuthFilter.class.getName();

    @Context
    private HttpServletRequest httpRequest;

    @Context
    private ServletContext servletContext;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String authorizationHeader = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);
        Delegator delegator = (Delegator) servletContext.getAttribute("delegator");
        if (!isTokenBasedAuthentication(authorizationHeader)) {
            abortWithUnauthorized(requestContext, false, "Unauthorized: Access is denied due to invalid or absent Authorization header.");
            return;
        }
        String jwtToken = JWTManager.getHeaderAuthBearerToken(httpRequest);
        Map<String, Object> claims = JWTManager.validateToken(delegator, jwtToken);
        if (claims.containsKey(ModelService.ERROR_MESSAGE)) {
            abortWithUnauthorized(requestContext, true, "Unauthorized: " + (String) claims.get(ModelService.ERROR_MESSAGE));
        } else {
            GenericValue userLogin = extractUserLoginFromJwtClaim(delegator, claims);
            httpRequest.setAttribute("userLogin", userLogin);
        }
    }

    private boolean isTokenBasedAuthentication(String authorizationHeader) {
        return authorizationHeader != null
                && authorizationHeader.toLowerCase().startsWith(AuthenticationScheme.BEARER.getScheme().toLowerCase() + " ");
    }

    private void abortWithUnauthorized(ContainerRequestContext requestContext, boolean isAuthHeaderPresent, String message) {
        if (!isAuthHeaderPresent) {
            requestContext.abortWith(
                    RestApiUtil.errorBuilder(Response.Status.UNAUTHORIZED.getStatusCode(), Response.Status.UNAUTHORIZED.getReasonPhrase(), message)
                    .header(HttpHeaders.WWW_AUTHENTICATE,
                    AuthenticationScheme.BEARER.getScheme() + " realm=\"" + AuthenticationScheme.REALM + "\"").build());
        } else {
            requestContext
                .abortWith(RestApiUtil.error(Response.Status.UNAUTHORIZED.getStatusCode(), Response.Status.UNAUTHORIZED.getReasonPhrase(), message));
        }

    }

    private GenericValue extractUserLoginFromJwtClaim(Delegator delegator, Map<String, Object> claims) {
        String userLoginId = (String) claims.get("userLoginId");
        if (UtilValidate.isEmpty(userLoginId)) {
            Debug.logWarning("No userLoginId found in the JWT token.", MODULE);
            return null;
        }
        GenericValue userLogin = null;
        try {
            userLogin = EntityQuery.use(delegator).from("UserLogin").where("userLoginId", userLoginId).queryOne();
            if (UtilValidate.isEmpty(userLogin)) {
                Debug.logWarning("There was a problem with the JWT token. Could not find provided userLogin " + userLoginId, MODULE);
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, "Unable to get UserLogin information from JWT Token: " + e.getMessage(), MODULE);
        }
        return userLogin;
    }

}
