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
import java.util.Base64;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilHttp;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;
import org.apache.ofbiz.ws.rs.common.AuthenticationScheme;
import org.apache.ofbiz.ws.rs.security.AuthToken;
import org.apache.ofbiz.ws.rs.util.RestApiUtil;


@AuthToken
@Provider
public class HttpBasicAuthFilter implements ContainerRequestFilter {

    private static final String MODULE = HttpBasicAuthFilter.class.getName();

    @Context
    private ResourceInfo resourceInfo;

    @Context
    private HttpServletRequest httpRequest;

    @Context
    private ServletContext servletContext;

    private static final String REALM = "OFBiz";

    /**
     * @param requestContext
     * @throws IOException
     */
    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String authorizationHeader = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);
        if (!isBasicAuth(authorizationHeader)) {
            abortWithUnauthorized(requestContext, false, "Unauthorized: Access is denied due to invalid or absent Authorization header");
            return;
        }
        String[] tokens = (new String(Base64.getDecoder().decode(authorizationHeader.split(" ")[1]), "UTF-8")).split(":");
        final String username = tokens[0];
        final String password = tokens[1];
        try {
            authenticate(username, password);
        } catch (ForbiddenException fe) {
            abortWithUnauthorized(requestContext, true, "Access Denied: " + fe.getMessage());
        }

    }

    /**
     * @param authorizationHeader
     * @return
     */
    private boolean isBasicAuth(String authorizationHeader) {
        return authorizationHeader != null
                && authorizationHeader.toLowerCase().startsWith(AuthenticationScheme.BASIC.getScheme().toLowerCase() + " ");
    }

    /**
     * @param requestContext
     */
    private void abortWithUnauthorized(ContainerRequestContext requestContext, boolean isAuthHeaderPresent, String message) {
        if (!isAuthHeaderPresent) {
            requestContext.abortWith(
                    RestApiUtil.errorBuilder(Response.Status.UNAUTHORIZED.getStatusCode(), Response.Status.UNAUTHORIZED.getReasonPhrase(), message)
                            .header(HttpHeaders.WWW_AUTHENTICATE, AuthenticationScheme.BASIC.getScheme() + " realm=\"" + REALM + "\"").build());
        } else {
            requestContext
                    .abortWith(RestApiUtil.error(Response.Status.FORBIDDEN.getStatusCode(), Response.Status.FORBIDDEN.getReasonPhrase(), message));
        }
    }

    private void authenticate(String userName, String password) throws ForbiddenException {
        Map<String, Object> result = null;
        LocalDispatcher dispatcher = (LocalDispatcher) servletContext.getAttribute("dispatcher");
        try {
            result = dispatcher.runSync("userLogin",
                    UtilMisc.toMap("login.username", userName, "login.password", password, "locale", UtilHttp.getLocale(httpRequest)));
        } catch (GenericServiceException e) {
            Debug.logError(e, "Error calling userLogin service", MODULE);
            throw new ForbiddenException(e.getMessage());
        }
        if (!ServiceUtil.isSuccess(result)) {
            Debug.logError(ServiceUtil.getErrorMessage(result), MODULE);
            throw new ForbiddenException(ServiceUtil.getErrorMessage(result));
        }

        GenericValue userLogin = (GenericValue) result.get("userLogin");
        httpRequest.setAttribute("userLogin", userLogin);
    }

}
