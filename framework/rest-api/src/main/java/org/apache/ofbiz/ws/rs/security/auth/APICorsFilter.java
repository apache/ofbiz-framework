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
import java.util.Arrays;
import java.util.List;

import jakarta.annotation.Priority;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.Provider;

import org.apache.catalina.filters.CorsFilter;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;

/**
 * Read https://developer.mozilla.org/en-US/docs/Web/HTTP/CORS for more details
 */

@Provider
@Priority(Priorities.HEADER_DECORATOR)
public class APICorsFilter implements ContainerResponseFilter {

    // check security.properties file for 'cors.origins.allowed'
    private static final List<String> ALLOWED_CORS_ORIGINS = UtilMisc.getCorsOriginsAllowed();

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
            throws IOException {
        MultivaluedMap<String, Object> responseHeaders = responseContext.getHeaders();

        if (UtilValidate.isNotEmpty(ALLOWED_CORS_ORIGINS)) {
            String origin = requestContext.getHeaderString(CorsFilter.REQUEST_HEADER_ORIGIN);
            if (UtilValidate.isNotEmpty(origin) && ALLOWED_CORS_ORIGINS.contains(origin)) {
                responseHeaders.add(CorsFilter.RESPONSE_HEADER_ACCESS_CONTROL_ALLOW_ORIGIN, origin);
                // Vary: Origin is required when the response varies by origin, so that
                // caches do not serve a response for one origin to a different origin.
                responseHeaders.add("Vary", "Origin");
            }
            // Requests from unlisted origins receive no Access-Control-Allow-Origin header,
            // which causes the browser to block the response.
        }

        // credentials support is enabled per default
        responseHeaders.add(CorsFilter.RESPONSE_HEADER_ACCESS_CONTROL_ALLOW_CREDENTIALS, true);

        // publish supported request header field names
        responseHeaders.addAll(CorsFilter.RESPONSE_HEADER_ACCESS_CONTROL_ALLOW_HEADERS,
                Arrays.asList(HttpHeaders.CONTENT_TYPE, HttpHeaders.AUTHORIZATION));

        // inform about all the supported methods. Itemize these due to the lack of support for the wildcard (*)
        // in few browsers, e.g. in 'Safari' resp. 'FF for Android'
        responseHeaders.addAll(CorsFilter.RESPONSE_HEADER_ACCESS_CONTROL_ALLOW_METHODS,
                Arrays.asList(HttpMethod.GET, HttpMethod.PATCH,
                        HttpMethod.PUT, HttpMethod.POST,
                        HttpMethod.DELETE, HttpMethod.OPTIONS));
    }
}
