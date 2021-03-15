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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityUtilProperties;
import org.apache.ofbiz.webapp.control.JWTManager;
import org.apache.ofbiz.ws.rs.security.AuthToken;
import org.apache.ofbiz.ws.rs.util.RestApiUtil;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;


@Path("/auth")
@Provider
@Tag(name = "Authentication Token Generating Resource", description = "Intended to provide generation of authentication tokens.")
public class AuthenticationResource extends OFBizResource {

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
        httpRequest.setAttribute("delegator", getDelegator());
        httpRequest.setAttribute("dispatcher", getDispatcher());
        GenericValue userLogin = (GenericValue) httpRequest.getAttribute("userLogin");
        //TODO : Move this into an OFBiz service. All such implementations should be inside an OFBiz service.
        String jwtToken = JWTManager.createJwt(getDelegator(), UtilMisc.toMap("userLoginId", userLogin.getString("userLoginId")));
        Map<String, Object> tokenPayload = UtilMisc.toMap("access_token", jwtToken, "expires_in",
                EntityUtilProperties.getPropertyValue("security", "security.jwt.token.expireTime", "1800", getDelegator()), "token_type", "Bearer");
        return RestApiUtil.success("Token granted.", tokenPayload);
    }

}
