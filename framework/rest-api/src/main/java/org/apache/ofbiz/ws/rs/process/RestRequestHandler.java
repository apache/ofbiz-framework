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

import org.apache.ofbiz.base.util.UtilValidate;
import org.glassfish.jersey.message.internal.MediaTypes;
import org.glassfish.jersey.process.Inflector;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.ExtendedUriInfo;
import org.glassfish.jersey.spi.ExceptionMappers;

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

/**
 * Base request handler for REST endpoints.
 *
 * <p>This class provides access to commonly injected JAX-RS and servlet
 * resources and dispatches requests based on the HTTP method. Request
 * parameters from the path, query string, and request body are collected
 * into a single argument map before being passed to
 * {@link #execute(ContainerRequestContext, Map)}.</p>
 */
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
     * Returns the HTTP headers associated with the current request.
     *
     * @return the request headers
     */
    protected HttpHeaders getHttpHeaders() {
        return httpHeaders;
    }

    /**
     * Returns URI information for the current request.
     *
     * @return the request URI information
     */
    protected UriInfo getUriInfo() {
        return uriInfo;
    }

    /**
     * Returns extended URI metadata provided by Jersey.
     *
     * @return the extended URI information
     */
    protected ExtendedUriInfo getExtendedUriInfo() {
        return extendedUriInfo;
    }

    /**
     * Returns information about the matched resource class and method.
     *
     * @return the current resource information
     */
    protected ResourceInfo getResourceInfo() {
        return resourceInfo;
    }

    /**
     * Returns the servlet context associated with the current request.
     *
     * @return the servlet context
     */
    protected ServletContext getServletContext() {
        return servletContext;
    }

    /**
     * Returns the underlying servlet request.
     *
     * @return the HTTP servlet request
     */
    protected HttpServletRequest getHttpRequest() {
        return httpRequest;
    }

    /**
     * Dispatches the request to the appropriate HTTP method handler.
     *
     * @param ctx the request context
     * @return the response produced by the request handler
     */
    @Override
    public Response apply(ContainerRequestContext ctx) {
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
        default:
            return null;
        }
    }

    /**
     * Executes the request using the extracted request arguments.
     *
     * <p>The supplied argument map may contain values from path parameters,
     * query parameters, and the request body, depending on the HTTP method.</p>
     *
     * @param data the request context
     * @param arguments the extracted request arguments
     * @return the response to return to the client
     */
    protected abstract Response execute(ContainerRequestContext data, Map<String, Object> arguments);

    /**
     * Extracts a JSON request body into a map of argument values.
     *
     * <p>If the request does not contain a JSON entity, an empty map is
     * returned.</p>
     *
     * @param requestContext the request context
     * @return the extracted request body arguments
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
     * Extracts path parameters from the request.
     *
     * @param requestContext the request context
     * @return a map containing path parameter values
     */
    protected Map<String, Object> extractPathParameters(ContainerRequestContext requestContext) {
        return extract(requestContext.getUriInfo().getPathParameters());
    }

    /**
     * Extracts query parameters from the request.
     *
     * @param requestContext the request context
     * @return a map containing query parameter values
     */
    protected Map<String, Object> extractQueryParameters(ContainerRequestContext requestContext) {
        return extract(requestContext.getUriInfo().getQueryParameters());
    }

    /**
     * Extracts the header Parameters
     *
     * @param requestContext the request context
     * @return a map containing headers
     */
    protected Map<String, Object> extractHeaderParameters(ContainerRequestContext requestContext) {
        return extract(requestContext.getHeaders());
    }

    /**
     * Converts a multivalued parameter map into a standard argument map.
     *
     * <p>Single-value parameters are stored as strings, while parameters with
     * multiple values are stored as a list of strings.</p>
     *
     * @param multivaluedMap the source parameter map
     * @return the converted argument map
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
     * Determines whether the request content type is JSON.
     *
     * @param requestContext the request context
     * @return {@code true} if the request content type is JSON;
     *         {@code false} otherwise
     */
    private boolean isJson(ContainerRequestContext requestContext) {
        final MediaType mediaType = requestContext.getMediaType();
        return UtilValidate.isNotEmpty(mediaType)
                && MediaTypes.typeEqual(mediaType, MediaType.APPLICATION_JSON_TYPE);
    }

    private Response doGet(ContainerRequestContext requestContext) {
        Map<String, Object> arguments = new HashMap<>();
        arguments.putAll(extractPathParameters(requestContext));
        arguments.putAll(extractQueryParameters(requestContext));
        arguments.putAll(extractHeaderParameters(requestContext));
        return execute(requestContext, arguments);
    }

    private Response doPost(ContainerRequestContext requestContext) {
        Map<String, Object> arguments = new HashMap<>();
        arguments.putAll(extractRequestBody(requestContext));
        arguments.putAll(extractPathParameters(requestContext));
        arguments.putAll(extractQueryParameters(requestContext));
        arguments.putAll(extractHeaderParameters(requestContext));
        return execute(requestContext, arguments);
    }

    private Response doPut(ContainerRequestContext requestContext) {
        Map<String, Object> arguments = new HashMap<>();
        arguments.putAll(extractRequestBody(requestContext));
        arguments.putAll(extractPathParameters(requestContext));
        arguments.putAll(extractQueryParameters(requestContext));
        arguments.putAll(extractHeaderParameters(requestContext));
        return execute(requestContext, arguments);
    }

    private Response doPatch(ContainerRequestContext requestContext) {
        Map<String, Object> arguments = new HashMap<>();
        arguments.putAll(extractRequestBody(requestContext));
        arguments.putAll(extractPathParameters(requestContext));
        arguments.putAll(extractQueryParameters(requestContext));
        arguments.putAll(extractHeaderParameters(requestContext));
        return execute(requestContext, arguments);
    }

    private Response doDelete(ContainerRequestContext requestContext) {
        Map<String, Object> arguments = new HashMap<>();
        arguments.putAll(extractPathParameters(requestContext));
        arguments.putAll(extractQueryParameters(requestContext));
        arguments.putAll(extractHeaderParameters(requestContext));
        return execute(requestContext, arguments);
    }

    /**
     * Returns the exception mapper provider used to resolve JAX-RS exception
     * mappers.
     *
     * @return the exception mapper provider
     */
    public Provider<ExceptionMappers> getMappers() {
        return mappers;
    }
}
