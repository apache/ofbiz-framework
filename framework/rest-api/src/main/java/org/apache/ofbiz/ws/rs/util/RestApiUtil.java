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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.service.ModelService;
import org.apache.ofbiz.ws.rs.core.ResponseStatus;
import org.apache.ofbiz.ws.rs.response.Error;
import org.apache.ofbiz.ws.rs.response.Success;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.ResponseBuilder;

public final class RestApiUtil {

    private static final String DEFAULT_MSG_UI_LABEL_RESOURCE = "ApiUiLabels";

    private RestApiUtil() {

    }

    /**
     * Builds a JSON success response with HTTP 200 and the given message and data.
     *
     * @param message a human-readable success message
     * @param data    the response payload
     * @return a JAX-RS {@link Response} with status 200 and a JSON {@link Success} body
     */
    public static Response success(String message, Object data) {
        Success success = new Success(Response.Status.OK.getStatusCode(), Response.Status.OK.getReasonPhrase(), message, data);
        return Response.status(Response.Status.OK).type(MediaType.APPLICATION_JSON).entity(success).build();
    }

    /**
     * Builds a JSON error response with the given status code, reason phrase,
     * and message.
     *
     * @param statusCode   the HTTP status code
     * @param reasonPhrase the HTTP reason phrase (e.g. {@code "Bad Request"})
     * @param message      a human-readable error message
     * @return a JAX-RS {@link Response} with a JSON {@link Error} body
     */
    public static Response error(int statusCode, String reasonPhrase, String message) {
        Error error = new Error(statusCode, reasonPhrase, message);
        return Response.status(statusCode).type(MediaType.APPLICATION_JSON).entity(error).build();
    }

    /**
     * Builds a JSON error {@link ResponseBuilder} that can be further modified
     * before building the final response.
     *
     * @param statusCode   the HTTP status code
     * @param reasonPhrase the HTTP reason phrase (e.g. {@code "Bad Request"})
     * @param message      a human-readable error message
     * @return a JAX-RS {@link ResponseBuilder} with a JSON {@link Error} entity
     */
    public static ResponseBuilder errorBuilder(int statusCode, String reasonPhrase, String message) {
        Error error = new Error(statusCode, reasonPhrase, message);
        return Response.status(statusCode).type(MediaType.APPLICATION_JSON).entity(error);
    }

    /**
     * Extracts query or form parameters from a {@link MultivaluedMap} into a
     * flat map. Single-value entries are unwrapped to a plain {@link String};
     * multi-value entries are kept as a {@link List}.
     *
     * @param multivaluedMap the JAX-RS parameter map to extract from
     * @return a map of parameter names to either a {@link String} or {@link List} of strings
     */
    public static Map<String, Object> extractParams(MultivaluedMap<String, String> multivaluedMap) {
        Map<String, Object> result = new HashMap<>();
        multivaluedMap.forEach((name, values) -> {
            if (UtilValidate.isNotEmpty(values)) {
                result.put(name, (values.size() != 1) ? values : values.get(0));
            }
        });
        return result;
    }

    /**
     * Extracts path parameter names from a JAX-RS path template string.
     * Template variables are identified by curly brace notation (e.g.
     * {@code {id}}).
     *
     * @param pathInfo the path template string (e.g. {@code "/order/{orderId}/item/{itemId}"})
     * @return a list of parameter names without braces; empty if {@code pathInfo}
     *         is {@code null} or contains no template variables
     */
    public static List<String> getPathParameters(String pathInfo) {
        List<String> pathParams = new ArrayList<>();
        if (pathInfo == null) {
            return pathParams;
        }
        String[] pathParts = pathInfo.split("/");
        for (String pathSegement : pathParts) {
            if (pathSegement.startsWith("{") && pathSegement.endsWith("}")) {
                pathParams.add(pathSegement.substring(1, pathSegement.length() - 1));
            }
        }
        return pathParams;
    }

    /**
     * Builds a JSON error response from an OFBiz service result map that
     * indicates failure. Extracts the primary error message and any additional
     * error messages from the result and returns an HTTP 422 Unprocessable Entity
     * response.
     *
     * @param service the name of the OFBiz service that produced the error
     * @param result  the service result map containing error message entries
     * @param locale  the locale used to resolve the error message label
     * @return a JAX-RS {@link Response} with status 422 and a JSON {@link Error} body
     */
    @SuppressWarnings("unchecked")
    public static Response buildErrorFromServiceResult(String service, Map<String, Object> result, Locale locale) {
        String errorMessage = null;
        List<String> additionalErrorMessages = new LinkedList<>();
        if (!UtilValidate.isEmpty(result.get(ModelService.ERROR_MESSAGE))) {
            errorMessage = result.get(ModelService.ERROR_MESSAGE).toString();
        }
        if (!UtilValidate.isEmpty(result.get(ModelService.ERROR_MESSAGE_LIST))) {
            List<String> errorMessageList = (List<String>) result.get(ModelService.ERROR_MESSAGE_LIST);
            if (UtilValidate.isEmpty(errorMessage)) {
                errorMessage = errorMessageList.get(0);
                errorMessageList.remove(0);
            }
            for (int i = 0; i < errorMessageList.size(); i++) {
                additionalErrorMessages.add(errorMessageList.get(i));
            }
        }
        Error error = new Error().type("ServiceError").code(ResponseStatus.Custom.UNPROCESSABLE_ENTITY.getStatusCode())
                .description(ResponseStatus.Custom.UNPROCESSABLE_ENTITY.getReasonPhrase())
                .message(getErrorMessage(service, "GenericServiceErrorMessage", locale)).errorDesc(errorMessage);
        if (!additionalErrorMessages.isEmpty()) {
            error.setAdditionalErrors(additionalErrorMessages);
        }
        return Response.status(ResponseStatus.Custom.UNPROCESSABLE_ENTITY).type(MediaType.APPLICATION_JSON)
                .entity(error).build();
    }

    /**
     * Resolves a localized error message from {@code ApiUiLabels} for the given
     * service name and error key, substituting the service name into the message
     * template.
     *
     * @param serviceName the OFBiz service name substituted into the message template
     * @param errorKey    the label key to look up in {@code ApiUiLabels}
     * @param locale      the locale used to resolve the label
     * @return the resolved and substituted error message string
     */
    public static String getErrorMessage(String serviceName, String errorKey, Locale locale) {
        String error = UtilProperties.getMessage(DEFAULT_MSG_UI_LABEL_RESOURCE, errorKey, locale);
        error = error.replace("${service}", serviceName);
        return error;
    }
}
