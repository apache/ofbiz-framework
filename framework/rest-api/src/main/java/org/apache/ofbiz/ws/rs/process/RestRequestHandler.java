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
package org.apache.ofbiz.ws.rs.process;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.ofbiz.base.util.UtilValidate;
import org.glassfish.jersey.message.internal.MediaTypes;
import org.glassfish.jersey.process.Inflector;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.ExtendedUriInfo;
import org.glassfish.jersey.spi.ExceptionMappers;

public abstract class RestRequestHandler implements Inflector<ContainerRequestContext, Response> {

    @Inject
    private HttpHeaders httpHeaders;

    @Inject
    private UriInfo uriInfo;

    @Inject
    private ExtendedUriInfo extendedUriInfo;

    @Inject
    private ResourceInfo resourceInfo;

    @Inject
    private ServletContext servletContext;

    @Inject
    private HttpServletRequest httpRequest;

    @Inject
    private Provider<ExceptionMappers> mappers;

    /**
     * @return the httpHeaders
     */
    protected HttpHeaders getHttpHeaders() {
        return httpHeaders;
    }

    /**
     * @return the uriInfo
     */
    protected UriInfo getUriInfo() {
        return uriInfo;
    }

    /**
     * @return the extendedUriInfo
     */
    protected ExtendedUriInfo getExtendedUriInfo() {
        return extendedUriInfo;
    }

    /**
     * @return the resourceInfo
     */
    protected ResourceInfo getResourceInfo() {
        return resourceInfo;
    }

    /**
     * @return the servletContext
     */
    protected ServletContext getServletContext() {
        return servletContext;
    }

    /**
     * @return the httpRequest
     */
    protected HttpServletRequest getHttpRequest() {
        return httpRequest;
    }

    /**
     * @param ctx
     * @return
     */
    @Override
    public Response apply(ContainerRequestContext ctx) {
        // TODO Auto-generated method stub
        String method = ctx.getMethod();
        switch (method) {
        case HttpMethod.POST:
            return doPost(ctx);
        case HttpMethod.GET:
            return doGet(ctx);
        case HttpMethod.DELETE:
            return doDelete(ctx);
        case HttpMethod.PUT:
            return doPut(ctx);
        case HttpMethod.PATCH:
            return doPatch(ctx);
        }
        return null;
    }

    /**
     * @param data
     * @param arguments
     * @return
     */
    protected abstract Response execute(ContainerRequestContext data, Map<String, Object> arguments);

    /**
     * @param requestContext
     * @return
     */
    @SuppressWarnings("unchecked")
    protected Map<String, Object> extractRequestBody(ContainerRequestContext requestContext) {
        if (requestContext instanceof ContainerRequest) {
            ContainerRequest request = (ContainerRequest) requestContext;
            if (requestContext.hasEntity()) {
                request.bufferEntity();
                if (isJson(requestContext)) {
                    Map<String, Object> entity = request.readEntity(Map.class);
                    if (entity == null) {
                        return Collections.emptyMap();
                    }
                    return entity;
                }
            }
        }
        return Collections.emptyMap();
    }

    /**
     * @param requestContext
     * @return
     */
    protected Map<String, Object> extractPathParameters(ContainerRequestContext requestContext) {
        return extract(requestContext.getUriInfo().getPathParameters());
    }

    /**
     * @param requestContext
     * @return
     */
    protected Map<String, Object> extractQueryParameters(ContainerRequestContext requestContext) {
        return extract(requestContext.getUriInfo().getQueryParameters());
    }

    /**
     * @param multivaluedMap
     * @return
     */
    protected Map<String, Object> extract(MultivaluedMap<String, String> multivaluedMap) {
        Map<String, Object> result = new HashMap<>();
        multivaluedMap.forEach((name, values) -> {
            if (UtilValidate.isNotEmpty(values)) {
                result.put(name, (values.size() != 1) ? values : values.get(0));
            }
        });
        return result;
    }

    /**
     * @param requestContext
     * @return
     */
    private boolean isJson(ContainerRequestContext requestContext) {
        final MediaType mediaType = requestContext.getMediaType();
        if (UtilValidate.isNotEmpty(mediaType) && MediaTypes.typeEqual(mediaType, MediaType.APPLICATION_JSON_TYPE)) {
            return true;
        }
        return false;
    }

    /**
     * @param requestContext
     * @return
     */
    private Response doGet(ContainerRequestContext requestContext) {
        Map<String, Object> arguments = new HashMap<>();
        arguments.putAll(extractPathParameters(requestContext));
        arguments.putAll(extractQueryParameters(requestContext));
        return execute(requestContext, arguments);
    }

    /**
     * @param requestContext
     * @return
     */
    private Response doPost(ContainerRequestContext requestContext) {
        Map<String, Object> arguments = new HashMap<>();
        arguments.putAll(extractRequestBody(requestContext));
        arguments.putAll(extractPathParameters(requestContext));
        arguments.putAll(extractQueryParameters(requestContext));
        return execute(requestContext, arguments);
    }

    /**
     * @param requestContext
     * @return
     */
    private Response doPut(ContainerRequestContext requestContext) {
        Map<String, Object> arguments = new HashMap<>();
        arguments.putAll(extractRequestBody(requestContext));
        arguments.putAll(extractPathParameters(requestContext));
        arguments.putAll(extractQueryParameters(requestContext));
        return execute(requestContext, arguments);
    }

    /**
     * @param requestContext
     * @return
     */
    private Response doPatch(ContainerRequestContext requestContext) {
        Map<String, Object> arguments = new HashMap<>();
        arguments.putAll(extractRequestBody(requestContext));
        arguments.putAll(extractPathParameters(requestContext));
        arguments.putAll(extractQueryParameters(requestContext));
        return execute(requestContext, arguments);
    }

    /**
     * @param requestContext
     * @return
     */
    private Response doDelete(ContainerRequestContext requestContext) {
        Map<String, Object> arguments = new HashMap<>();
        arguments.putAll(extractPathParameters(requestContext));
        arguments.putAll(extractQueryParameters(requestContext));
        return execute(requestContext, arguments);
    }

    /**
     * @return the mappers
     */
    public Provider<ExceptionMappers> getMappers() {
        return mappers;
    }
}
