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
import java.util.List;

import javax.annotation.Priority;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;

import org.apache.catalina.filters.CorsFilter;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;

/**
 * Read https://developer.mozilla.org/en-US/docs/Web/HTTP/CORS for more details
 */

@Provider
@Priority(Priorities.HEADER_DECORATOR)
public class APICorsFilter implements ContainerResponseFilter {

    // check security.properties file for 'host-headers-allowed'
    private static final List<String> allowedHostHeaders = UtilMisc.getHostHeadersAllowed();

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
            throws IOException {
        MultivaluedMap<String, Object> responseHeaders = responseContext.getHeaders();

        if (UtilValidate.isNotEmpty(allowedHostHeaders)) {
            // the list is quite short, hence return the single entry without further checks
            if (allowedHostHeaders.size() < 2) {
                responseHeaders.add(CorsFilter.RESPONSE_HEADER_ACCESS_CONTROL_ALLOW_ORIGIN, allowedHostHeaders.get(0));
            } else {
                // get the request origin from request context and localize it in the list
                String origin = requestContext.getHeaderString(CorsFilter.REQUEST_HEADER_ORIGIN);
                // return the origin in case it's part of the allowed hosts list
                if (UtilValidate.isNotEmpty(origin) && allowedHostHeaders.contains(origin)) {
                    responseHeaders.add(CorsFilter.RESPONSE_HEADER_ACCESS_CONTROL_ALLOW_ORIGIN, origin);
                } else {
                    // pick up the first one from the allowed hosts list in case the request origin is not listed there
                    responseHeaders.add(CorsFilter.RESPONSE_HEADER_ACCESS_CONTROL_ALLOW_ORIGIN, allowedHostHeaders.get(0));
                }
            }
        }

        // credentials support is enabled per default
        responseHeaders.add(CorsFilter.RESPONSE_HEADER_ACCESS_CONTROL_ALLOW_CREDENTIALS, true);

        // publish supported request header field names
        responseHeaders.addAll(CorsFilter.RESPONSE_HEADER_ACCESS_CONTROL_ALLOW_HEADERS,
                HttpHeaders.CONTENT_TYPE,
                HttpHeaders.AUTHORIZATION
        );

        // inform about all the supported methods. Itemize these due to the lack of support for the wildcard (*)
        // in few browsers, e.g. in 'Safari' resp. 'FF for Android'
        responseHeaders.addAll(CorsFilter.RESPONSE_HEADER_ACCESS_CONTROL_ALLOW_METHODS,
                HttpMethod.GET, HttpMethod.PATCH,
                HttpMethod.PUT, HttpMethod.POST,
                HttpMethod.DELETE, HttpMethod.OPTIONS);
    }
}
