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
package org.apache.ofbiz.ws.rs.util;

import java.util.Map;

import javax.ws.rs.core.Response;

import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.service.ModelService;
import org.apache.ofbiz.service.ServiceUtil;
import org.apache.ofbiz.ws.rs.core.ResponseStatus;

/**
 * Convenience API for services used in a REST-API context.
 * <p>
 * This utility class provides shortcut methods for building service result
 * {@link Map}s (as used by {@link org.apache.ofbiz.service.LocalDispatcher})
 * that also carry an associated HTTP response status code under
 * {@link RestApiUtil#RESPONSE_STATUS_KEY}. It wraps the generic
 * {@link ServiceUtil#returnSuccess}, {@link ServiceUtil#returnFailure} and
 * {@link ServiceUtil#returnError} helpers, and additionally offers
 * dedicated shortcut methods for the most common HTTP status codes
 * (e.g. {@code 400 Bad Request}, {@code 401 Unauthorized},
 * {@code 404 Not Found}, {@code 200 OK}, {@code 201 Created}, etc.).
 */
public final class RestServiceUtil {

    public static final String MODULE = RestServiceUtil.class.getName();

    private RestServiceUtil() {
    }

    /**
     * Builds a success result map with the given message and HTTP status.
     *
     * @param successMessage
     * @param httpResponseStatus
     * @return a service result map representing success
     */
    public static Map<String, Object> returnSuccess(String successMessage, Integer httpResponseStatus) {
        Map<String, Object> resultMap = ServiceUtil.returnSuccess(successMessage);
        resultMap.put(RestApiUtil.RESPONSE_STATUS_KEY, httpResponseStatus);
        return resultMap;
    }

    /**
     * Builds a success result map with the given message and HTTP status.
     *
     * @param successMessage
     * @param httpResponseStatus
     * @return a service result map representing success
     */
    public static Map<String, Object> returnSuccess(String successMessage, int httpResponseStatus) {
        return returnSuccess(successMessage, Integer.valueOf(httpResponseStatus));
    }

    /**
     * Builds a success result map with the given message and HTTP status.
     *
     * @param successMessage
     * @param httpResponseStatus the HTTP status code (as a String)
     * @return a service result map representing success
     */
    public static Map<String, Object> returnSuccess(String successMessage, String httpResponseStatus) {
        return returnSuccess(successMessage, Integer.valueOf(httpResponseStatus));
    }

    /**
     * Builds a failure result map with the given message and HTTP status.
     *
     * @param errorMessage
     * @param httpResponseStatus
     * @return a service result map representing failure
     */
    public static Map<String, Object> returnFailure(String errorMessage, Integer httpResponseStatus) {
        Map<String, Object> resultMap = ServiceUtil.returnFailure(errorMessage);
        resultMap.put(RestApiUtil.RESPONSE_STATUS_KEY, httpResponseStatus);
        return resultMap;
    }

    /**
     * Builds a failure result map with the given message and HTTP status.
     *
     * @param errorMessage
     * @param httpResponseStatus the HTTP status code
     * @return a service result map representing failure
     */
    public static Map<String, Object> returnFailure(String errorMessage, int httpResponseStatus) {
        return returnFailure(errorMessage, Integer.valueOf(httpResponseStatus));
    }

    /**
     * Builds a failure result map with the given message and HTTP status.
     *
     * @param errorMessage
     * @param httpResponseStatus the HTTP status code (as a String)
     * @return a service result map representing failure
     */
    public static Map<String, Object> returnFailure(String errorMessage, String httpResponseStatus) {
        return returnFailure(errorMessage, Integer.valueOf(httpResponseStatus));
    }

    /**
     * Builds an error result map with the given error code, message and HTTP status.
     *
     * @param errorCode an optional application-specific error code
     * @param errorMessage
     * @param httpResponseStatus the HTTP status code to associate with the result
     * @return a service result map representing an error
     */
    public static Map<String, Object> returnError(String errorCode, String errorMessage, Integer httpResponseStatus) {
        Map<String, Object> resultMap = ServiceUtil.returnError(errorMessage);
        if (UtilValidate.isNotEmpty(errorCode)) {
            resultMap.put(ModelService.ERROR_CODE, errorCode);
        }
        if (httpResponseStatus != null) {
            resultMap.put(RestApiUtil.RESPONSE_STATUS_KEY, httpResponseStatus);
        }
        return resultMap;
    }

    /**
     * Builds an error result map with the given message and HTTP status.
     *
     * @param errorMessage
     * @param httpResponseStatus the HTTP status code to associate with the result
     * @return a service result map representing an error
     */
    public static Map<String, Object> returnError(String errorMessage, int httpResponseStatus) {
        return returnError(null, errorMessage, Integer.valueOf(httpResponseStatus));
    }

    /**
     * Builds an error result map with the given message and HTTP status.
     *
     * @param errorMessage
     * @param httpResponseStatus the HTTP status code (as a String)
     * @return a service result map representing an error
     */
    public static Map<String, Object> returnError(String errorMessage, String httpResponseStatus) {
        return returnError(null, errorMessage, Integer.valueOf(httpResponseStatus));
    }

    /**
     * Builds an error result map with the given error code, message and HTTP status.
     *
     * @param errorCode
     * @param errorMessage
     * @param httpResponseStatus the HTTP status code
     * @return a service result map representing an error
     */
    public static Map<String, Object> returnError(String errorCode, String errorMessage, int httpResponseStatus) {
        return returnError(errorCode, errorMessage, Integer.valueOf(httpResponseStatus));
    }

    /**
     * Builds an error result map with the given error code, message and HTTP status.
     *
     * @param errorCode
     * @param errorMessage
     * @param httpResponseStatus the HTTP status code (as a String)
     * @return a service result map representing an error
     */
    public static Map<String, Object> returnError(String errorCode, String errorMessage, String httpResponseStatus) {
        return returnError(errorCode, errorMessage, Integer.valueOf(httpResponseStatus));
    }

    /**
     * Builds a {@code 400 Bad Request} error result map with an error code and message.
     *
     * @param errorCode
     * @param errorMessage
     * @return a service result map representing a {@code 400 Bad Request} error
     */
    public static Map<String, Object> returnBadRequest(String errorCode, String errorMessage) {
        return returnError(errorCode, errorMessage, Response.Status.BAD_REQUEST.getStatusCode());
    }

    /**
     * Builds a {@code 400 Bad Request} error result map using the default reason phrase as the message.
     *
     * @return a service result map representing a {@code 400 Bad Request} error
     */
    public static Map<String, Object> returnBadRequest() {
        return returnError(Response.Status.BAD_REQUEST.getReasonPhrase(), Response.Status.BAD_REQUEST.getStatusCode());
    }

    /**
     * Builds a {@code 401 Unauthorized} error result map with the given message.
     *
     * @param errorMessage
     * @return a service result map representing a {@code 401 Unauthorized} error
     */
    public static Map<String, Object> returnUnauthorized(String errorMessage) {
        return returnError(errorMessage, Response.Status.UNAUTHORIZED.getStatusCode());
    }

    /**
     * Builds a {@code 401 Unauthorized} error result map using the default reason phrase as the message.
     *
     * @return a service result map representing a {@code 401 Unauthorized} error
     */
    public static Map<String, Object> returnUnauthorized() {
        return returnError(Response.Status.UNAUTHORIZED.getReasonPhrase(), Response.Status.UNAUTHORIZED.getStatusCode());
    }

    /**
     * Builds a {@code 403 Forbidden} error result map with the given message.
     *
     * @param errorMessage
     * @return a service result map representing a {@code 403 Forbidden} error
     */
    public static Map<String, Object> returnForbidden(String errorMessage) {
        return returnError(errorMessage, Response.Status.FORBIDDEN.getStatusCode());
    }

    /**
     * Builds a {@code 403 Forbidden} error result map using the default reason phrase as the message.
     *
     * @return a service result map representing a {@code 403 Forbidden} error
     */
    public static Map<String, Object> returnForbidden() {
        return returnError(Response.Status.FORBIDDEN.getReasonPhrase(), Response.Status.FORBIDDEN.getStatusCode());
    }

    /**
     * Builds a {@code 404 Not Found} error result map with the given message.
     *
     * @param errorMessage
     * @return a service result map representing a {@code 404 Not Found} error
     */
    public static Map<String, Object> returnNotFound(String errorMessage) {
        return returnNotFound(null, errorMessage);
    }

    /**
     * Builds a {@code 404 Not Found} error result map with an error code and message.
     *
     * @param errorCode
     * @param errorMessage
     * @return a service result map representing a {@code 404 Not Found} error
     */
    public static Map<String, Object> returnNotFound(String errorCode, String errorMessage) {
        return returnError(errorCode, errorMessage, Response.Status.NOT_FOUND.getStatusCode());
    }

    /**
     * Builds a {@code 404 Not Found} error result map using the default reason phrase as the message.
     *
     * @return a service result map representing a {@code 404 Not Found} error
     */
    public static Map<String, Object> returnNotFound() {
        return returnError(Response.Status.NOT_FOUND.getReasonPhrase(), Response.Status.NOT_FOUND.getStatusCode());
    }

    /**
     * Builds a {@code 409 Conflict} error result map with the given message.
     *
     * @param errorMessage
     * @return a service result map representing a {@code 409 Conflict} error
     */
    public static Map<String, Object> returnConflict(String errorMessage) {
        return returnError(errorMessage, Response.Status.CONFLICT.getStatusCode());
    }

    /**
     * Builds a {@code 409 Conflict} error result map using the default reason phrase as the message.
     *
     * @return a service result map representing a {@code 409 Conflict} error
     */
    public static Map<String, Object> returnConflict() {
        return returnError(Response.Status.CONFLICT.getReasonPhrase(), Response.Status.CONFLICT.getStatusCode());
    }

    /**
     * Builds a {@code 422 Unprocessable Entity} error result map with the given message.
     *
     * @param errorMessage
     * @return a service result map representing a {@code 422 Unprocessable Entity} error
     */
    public static Map<String, Object> returnUnprocessableEntity(String errorMessage) {
        return returnUnprocessableEntity(null, errorMessage);
    }

    /**
     * Builds a {@code 422 Unprocessable Entity} error result map with an error code and message.
     *
     * @param errorCode
     * @param errorMessage
     * @return a service result map representing a {@code 422 Unprocessable Entity} error
     */
    public static Map<String, Object> returnUnprocessableEntity(String errorCode, String errorMessage) {
        return returnError(errorCode, errorMessage, ResponseStatus.Custom.UNPROCESSABLE_ENTITY.getStatusCode());
    }

    /**
     * Builds a {@code 422 Unprocessable Entity} error result map using the default reason phrase as
     * the message.
     *
     * @return a service result map representing a {@code 422 Unprocessable Entity} error
     */
    public static Map<String, Object> returnUnprocessableEntity() {
        return returnError(ResponseStatus.Custom.UNPROCESSABLE_ENTITY.getReasonPhrase(), ResponseStatus.Custom.UNPROCESSABLE_ENTITY.getStatusCode());
    }

    /**
     * Builds a {@code 200 OK} success result map with the given message.
     *
     * @param successMessage
     * @return a service result map representing a {@code 200 OK} success
     */
    public static Map<String, Object> returnOK(String successMessage) {
        return returnSuccess(successMessage, Response.Status.OK.getStatusCode());
    }

    /**
     * Builds a {@code 200 OK} success result map using the default reason phrase as the message.
     *
     * @return a service result map representing a {@code 200 OK} success
     */
    public static Map<String, Object> returnOK() {
        return returnSuccess(Response.Status.OK.getReasonPhrase(), Response.Status.OK.getStatusCode());
    }

    /**
     * Builds a {@code 201 Created} success result map with the given message.
     *
     * @param successMessage the human-readable success message
     * @return a service result map representing a {@code 201 Created} success
     */
    public static Map<String, Object> returnCreated(String successMessage) {
        return returnSuccess(successMessage, Response.Status.CREATED.getStatusCode());
    }

    /**
     * Builds a {@code 201 Created} success result map using the default reason phrase as the message.
     *
     * @return a service result map representing a {@code 201 Created} success
     */
    public static Map<String, Object> returnCreated() {
        return returnSuccess(Response.Status.CREATED.getReasonPhrase(), Response.Status.CREATED.getStatusCode());
    }
}
