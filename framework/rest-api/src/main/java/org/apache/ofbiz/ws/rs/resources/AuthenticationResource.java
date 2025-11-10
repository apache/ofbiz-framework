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

import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityUtilProperties;
import org.apache.ofbiz.webapp.control.JWTManager;
import org.apache.ofbiz.ws.rs.annotation.AuthToken;
import org.apache.ofbiz.ws.rs.util.RestApiUtil;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;


@Path("/auth")
@Tag(name = "Authentication Token Generating Resource", description = "Intended to provide generation of authentication tokens.")
public class AuthenticationResource {

    @Context
    private ServletContext servletContext;

    @Context
    private HttpServletRequest httpRequest;

    @Context
    private HttpServletResponse httpResponse;

    /**
     */
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/token")
    @AuthToken
    @Operation(security = @SecurityRequirement(name = "basicAuth"),
            operationId = "getAuthToken", description = "Generates JWT token for subsequent API calles.")
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

        // Fetch delegator, dispatcher, and userLogin
        if (claims.containsKey("errorMessage")) {
            System.out.println("Error with JWT token: ");
        }

        String userLoginId = (String) claims.get("userLoginId");

        String newAccessToken = JWTManager.createJwt(delegator, UtilMisc.toMap("userLoginId", userLoginId));
        String newRefreshToken = JWTManager.createRefreshToken(delegator, userLoginId);

        Map<String, Object> tokenPayload = UtilMisc.toMap("access_token", newAccessToken, "refresh_token", newRefreshToken, "expires_in",
                EntityUtilProperties.getPropertyValue("security", "security.jwt.token.expireTime", "1800", delegator), "token_type", "Bearer");

        return RestApiUtil.success("Token refreshed.", tokenPayload);
    }
}
