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
package org.apache.ofbiz.ws.rs.resources;

import java.util.Map;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.EntityUtilProperties;
import org.apache.ofbiz.service.ModelService;
import org.apache.ofbiz.webapp.control.JWTManager;
import org.apache.ofbiz.webapp.control.LoginWorker;
import org.apache.ofbiz.ws.rs.annotation.AuthToken;
import org.apache.ofbiz.ws.rs.util.RestApiUtil;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;


@Path("/auth")
@Tag(name = "Authentication Token Generating Resource", description = "Intended to provide generation of authentication tokens.")
public class AuthenticationResource {

    private static final String MODULE = AuthenticationResource.class.getName();

    @Context
    private ServletContext servletContext;

    @Context
    private HttpServletRequest httpRequest;

    @Context
    private HttpServletResponse httpResponse;

    /**
     * Generates a JWT access token and refresh token for authenticated users.
     *
     * <p>This endpoint uses Basic Authentication credentials to authenticate the
     * user and issues a signed JWT token that can be used for subsequent API
     * requests. A refresh token is also generated to allow renewal of the access
     * token without re-authentication.</p>
     *
     * <p><b>Security:</b> This operation requires HTTP Basic Authentication and is
     * protected by {@code @AuthToken}.</p>
     *
     * <p>The method retrieves the current {@code Delegator} and dispatcher from the
     * servlet context and expects a resolved {@code userLogin} attribute on the
     * HTTP request.</p>
     *
     * @param creds the HTTP Authorization header containing Basic Authentication credentials
     * @return a JSON response containing:
     *         <ul>
     *             <li>{@code access_token} - the JWT access token</li>
     *             <li>{@code refresh_token} - token used to obtain new access tokens</li>
     *             <li>{@code expires_in} - token expiration time in seconds</li>
     *             <li>{@code token_type} - token type (Bearer)</li>
     *         </ul>
     */
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/token")
    @AuthToken
    @Operation(security = @SecurityRequirement(name = "basicAuth"), operationId = "getAuthToken",
            description = "Generates JWT token for subsequent API calls.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK, token generated.",
                            content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "{ \"statusCode\" : 200, \"statusDescription\" : \"OK\", "
                            + "\"successMessage\" : \"Token granted.\", \"data\" : { \"access_token\" : \"eyJ0eXAiOi...Ha3lpL19ag\", "
                            + "\"token_type\" : \"Bearer\", \"expires_in\" : \"86400000\" } }"))),
                    @ApiResponse(responseCode = "401", description = "Unauthorized.",
                            content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "{ \"statusCode\" : 401, \"statusDescription\" : \"Unauthorized\", "
                            + "\"errorMessage\" : \"Unauthorized: Access is denied due to invalid or absent Authorization header\" }"))),
                    @ApiResponse(responseCode = "403", description = "Forbidden.",
                            content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "{ \"statusCode\" : 403, \"statusDescription\" : \"Forbidden\", "
                            + "\"errorMessage\" : \"Access Denied: User not found.\" }")))
            })
    public Response getAuthToken(@Parameter(in = ParameterIn.HEADER, name = "Authorization",
            description = "Authorization header using Basic Authentication", example = HttpHeaders.AUTHORIZATION + ": Basic YWRtaW46b2ZiaXo=")
            @HeaderParam(HttpHeaders.AUTHORIZATION) String creds) {
        Delegator delegator = (Delegator) servletContext.getAttribute("delegator");
        httpRequest.setAttribute("delegator", delegator);
        httpRequest.setAttribute("dispatcher", servletContext.getAttribute("dispatcher"));
        GenericValue userLogin = (GenericValue) httpRequest.getAttribute("userLogin");
        //TODO : Move this into an OFBiz service. All such implementations should be inside an OFBiz service.
        String jwtToken = JWTManager.createJwt(delegator,
                UtilMisc.toMap("userLoginId", userLogin.getString("userLoginId")));
        String refreshToken = JWTManager.createRefreshToken(delegator, userLogin.getString("userLoginId"));

        Map<String, Object> tokenPayload = UtilMisc.toMap("access_token", jwtToken, "refresh_token", refreshToken,
                "expires_in", EntityUtilProperties.getPropertyValue("security", "security.jwt.token.expireTime", "1800", delegator),
                "token_type", "Bearer");
        return RestApiUtil.success("Token granted.", tokenPayload);
    }

    /**
     * Generates a new access token using a refresh token.
     * <p>
     * Subclasses overriding this method should ensure they call the parent implementation
     * or handle JWT validation and token generation securely.
     * </p>
     *
     * @param refreshToken The refresh token provided in the request header.
     * @return A response containing the new access and refresh tokens.
    ]*/
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/refresh-token")
    @Operation(description = "Generates a new access token using a refresh token.")
    public Response refreshToken(@HeaderParam("Refresh-Token") String refreshToken) {

        Delegator delegator = (Delegator) servletContext.getAttribute("delegator");
        httpRequest.setAttribute("delegator", delegator);
        httpRequest.setAttribute("dispatcher", delegator);
        Map<String, Object> claims = JWTManager.validateRefreshToken(delegator, refreshToken);

        if (claims.containsKey(ModelService.ERROR_MESSAGE)) {
            return RestApiUtil.error(Response.Status.UNAUTHORIZED.getStatusCode(), Response.Status.UNAUTHORIZED.getReasonPhrase(),
                    "Unauthorized: " + claims.get(ModelService.ERROR_MESSAGE));
        }

        String userLoginId = (String) claims.get("userLoginId");
        GenericValue userLogin = getActiveUserLogin(delegator, userLoginId);
        if (userLogin == null) {
            return RestApiUtil.error(Response.Status.UNAUTHORIZED.getStatusCode(), Response.Status.UNAUTHORIZED.getReasonPhrase(),
                    "Unauthorized: Invalid refresh token.");
        }

        String newAccessToken = JWTManager.createJwt(delegator, UtilMisc.toMap("userLoginId", userLoginId));
        String newRefreshToken = JWTManager.createRefreshToken(delegator, userLoginId);

        Map<String, Object> tokenPayload = UtilMisc.toMap("access_token", newAccessToken, "refresh_token", newRefreshToken, "expires_in",
                EntityUtilProperties.getPropertyValue("security", "security.jwt.token.expireTime", "1800", delegator), "token_type", "Bearer");

        return RestApiUtil.success("Token refreshed.", tokenPayload);
    }

    /**
     * Looks up the {@code UserLogin} named by a validated refresh token's claims and confirms
     * the account is still present and active, so a disabled or deleted account cannot keep
     * renewing tokens on the strength of a refresh token issued before the account changed.
     * @param delegator the delegator
     * @param userLoginId the userLoginId claim from a cryptographically validated refresh token
     * @return the active userLogin, or {@code null} if it is missing, deleted, or disabled
     */
    private GenericValue getActiveUserLogin(Delegator delegator, String userLoginId) {
        if (UtilValidate.isEmpty(userLoginId)) {
            return null;
        }
        GenericValue userLogin;
        try {
            userLogin = EntityQuery.use(delegator).from("UserLogin").where("userLoginId", userLoginId).queryOne();
        } catch (GenericEntityException e) {
            Debug.logError(e, "Unable to get UserLogin information from refresh token: " + e.getMessage(), MODULE);
            return null;
        }
        if (userLogin == null || !LoginWorker.isUserLoginActive(userLogin)) {
            return null;
        }
        return userLogin;
    }
}
