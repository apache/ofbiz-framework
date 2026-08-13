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

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilHttp;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.collections.MapComparator;
import org.apache.ofbiz.service.ModelService;
import org.apache.ofbiz.ws.rs.core.ResponseStatus;
import org.apache.ofbiz.ws.rs.response.Error;
import org.apache.ofbiz.ws.rs.response.Success;

import groovy.lang.Binding;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.ResponseBuilder;
import jakarta.ws.rs.core.Response.StatusType;

public final class RestApiUtil {

    public static final String RESPONSE_STATUS_KEY = "httpResponseStatus";
    public static final List<String> SECURITY_PARAMS = UtilMisc.toList("primaryPermission", "mainAction");
    private static final String DEFAULT_MSG_UI_LABEL_RESOURCE = "ApiUiLabels";
    private static final String QUERY_STRING_SEPARATOR = "&";

    private RestApiUtil() {

    }

    /**
     * Builds a JSON success response with HTTP 200 / CUSTOM-STATUS-CODE and the given message and data.
     * Checks for a custom status code within data. Defaults to HTTP 200 if non present.
     *
     * @param message a human-readable success message
     * @param data    the response payload
     * @return a JAX-RS {@link Response} with status 200 and a JSON {@link Success} body
     */
    public static Response success(String message, Object data) {
        StatusType status = extractResponseCode(data);

        if (status == null) {
            status = Response.Status.OK;
        }

        Success success = new Success(status.getStatusCode(), status.getReasonPhrase(), message, data);
        ResponseBuilder builder = Response.status(status.getStatusCode()).type(MediaType.APPLICATION_JSON).entity(success);
        String linkHeaderValue = getPaginationLinkHeaderValue(data);
        if (UtilValidate.isNotEmpty(linkHeaderValue)) {
            builder.header("Link", linkHeaderValue);
        }
        return builder.build();
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
        String errorCode = null;
        if (!UtilValidate.isEmpty(result.get(ModelService.ERROR_CODE))) {
            errorCode = result.get(ModelService.ERROR_CODE).toString();
        }
        StatusType status = extractResponseCode(result);
        if (status == null) {
            status = ResponseStatus.Custom.UNPROCESSABLE_ENTITY;
        }

        Error error = new Error().type("ServiceError").statusCode(status.getStatusCode())
                .description(ResponseStatus.Custom.UNPROCESSABLE_ENTITY.getReasonPhrase())
                .message(getErrorMessage(service, "GenericServiceErrorMessage", locale))
                .errorDescription(errorMessage).errorCode(errorCode);
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

    /**
     * Returns the first non-empty parameter value found for the provided aliases,
     * matching keys case-insensitively.
     *
     * @param parameters the parameter map to search
     * @param aliases the accepted parameter names in lookup order
     * @return the first non-empty value, or {@code null} when none is present
     */
    static Object getParameterValueIgnoreCase(Map<String, ?> parameters, String... aliases) {
        if (UtilValidate.isEmpty(parameters) || aliases == null || aliases.length == 0) {
            return null;
        }
        for (String alias : aliases) {
            for (Map.Entry<String, ?> entry : parameters.entrySet()) {
                if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(alias)
                        && UtilValidate.isNotEmpty(entry.getValue())) {
                    return entry.getValue();
                }
            }
        }
        return null;
    }

    /**
     * Returns {@code true} when the given parameter name matches any reserved
     * name case-insensitively.
     *
     * @param parameterName the parameter name to inspect
     * @param reservedNames the reserved names to compare against
     * @return {@code true} when the parameter name is reserved
     */
    static boolean isReservedParameter(String parameterName, Set<String> reservedNames) {
        if (parameterName == null || UtilValidate.isEmpty(reservedNames)) {
            return false;
        }
        for (String reservedName : reservedNames) {
            if (parameterName.equalsIgnoreCase(reservedName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Parses a request path or query string into an insertion-ordered parameter list.
     * When the input does not contain a query string, an empty map is returned.
     *
     * @param requestPath the full request path or query string
     * @return a list containing decoded query parameters in encounter order
     */
    static List<QueryParameter> extractQueryParameters(String requestPath) {
        List<QueryParameter> parameters = new ArrayList<>();
        if (UtilValidate.isEmpty(requestPath)) {
            return parameters;
        }

        int querySeparatorIndex = requestPath.indexOf('?');
        if (querySeparatorIndex < 0) {
            return parameters;
        }
        String queryString = requestPath.substring(querySeparatorIndex + 1);
        if (UtilValidate.isEmpty(queryString)) {
            return parameters;
        }

        for (String entry : queryString.split(QUERY_STRING_SEPARATOR)) {
            if (UtilValidate.isEmpty(entry)) {
                continue;
            }

            int assignmentIndex = entry.indexOf('=');
            String key = assignmentIndex >= 0 ? entry.substring(0, assignmentIndex) : entry;
            String value = assignmentIndex >= 0 ? entry.substring(assignmentIndex + 1) : "";
            String decodedKey = decodeQueryValue(key);
            String decodedValue = decodeQueryValue(value);
            if (UtilValidate.isNotEmpty(decodedKey)) {
                parameters.add(new QueryParameter(decodedKey, decodedValue));
            }
        }
        return parameters;
    }

    /**
     * Encodes query parameters for safe inclusion in a URL query string while
     * preserving encounter order and repeated keys.
     *
     * @param parameters decoded query parameters
     * @return an encoded query string, or an empty string when no parameters exist
     */
    static String encodeQueryParameters(List<QueryParameter> parameters) {
        if (UtilValidate.isEmpty(parameters)) {
            return "";
        }
        StringBuilder queryBuilder = new StringBuilder();
        boolean first = true;
        for (QueryParameter parameter : parameters) {
            if (!first) {
                queryBuilder.append(QUERY_STRING_SEPARATOR);
            }
            queryBuilder.append(encodeQueryValue(parameter.getName())).append('=')
                    .append(encodeQueryValue(parameter.getValue()));
            first = false;
        }
        return queryBuilder.toString();
    }

    /**
     * Builds a map representation of a REST link that matches the existing
     * serializer contract of {@code href} plus optional link parameters.
     *
     * @param href the link target
     * @param params optional link parameters such as {@code rel}
     * @return an insertion-ordered map suitable for JSON serialization
     */
    static Map<String, Object> makeLinkMap(String href, Map<String, String> params) {
        Map<String, Object> link = new LinkedHashMap<>();
        if (UtilValidate.isEmpty(href)) {
            return link;
        }
        link.put("href", href);
        if (UtilValidate.isNotEmpty(params)) {
            params.forEach((key, value) -> {
                if (UtilValidate.isNotEmpty(key) && UtilValidate.isNotEmpty(value)) {
                    link.put(key, value);
                }
            });
        }
        return link;
    }

    /**
     * Validates a comma-separated sort expression against the endpoint fields
     * allowed for sorting and returns the accepted normalized tokens in order.
     *
     * @param sortExpression the requested sort expression
     * @param allowedFields the endpoint-supported sortable fields, or
     *        {@code null}/empty to apply syntax-only validation
     * @return the validated sort tokens, or an empty list when no sort was requested
     * @throws IllegalArgumentException when a token is malformed or a field is unsupported
     */
    static List<String> validateSortFields(String sortExpression, Set<String> allowedFields) {
        if (UtilValidate.isEmpty(sortExpression)) {
            return Collections.emptyList();
        }

        List<String> validatedFields = new ArrayList<>();
        Set<String> seenFields = new HashSet<>();
        for (String token : sortExpression.split(",", -1)) {
            String candidate = token.trim();
            if (UtilValidate.isEmpty(candidate)) {
                throw new IllegalArgumentException("Sort expression contains an empty field");
            }
            if ("-".equals(candidate)) {
                throw new IllegalArgumentException("Sort expression contains a malformed field");
            }

            String normalizedField = candidate.startsWith("-") ? candidate.substring(1) : candidate;
            if (!seenFields.add(normalizedField)) {
                throw new IllegalArgumentException("Duplicate sort field: " + normalizedField);
            }
            if (UtilValidate.isNotEmpty(allowedFields) && !allowedFields.contains(normalizedField)) {
                throw new IllegalArgumentException("Unsupported sort field: " + normalizedField);
            }
            validatedFields.add(candidate);
        }
        return validatedFields;
    }

    /**
     * Resolves a REST sort expression into EntityQuery order-by fields.
     * Endpoint contracts can expose semantic sort names while entities use
     * different field names, so public names are validated before mapping.
     *
     * @param sortExpression the requested sort expression
     * @param sortFieldMap endpoint-supported sort fields mapped to entity fields
     * @param defaultOrderBy fallback order fields and stable tie-breakers
     * @return entity order-by fields suitable for {@code EntityQuery.orderBy}
     * @throws IllegalArgumentException when a token is malformed, unsupported,
     *         or maps to a duplicate entity field
     */
    public static List<String> resolveOrderBy(String sortExpression, Map<String, String> sortFieldMap, List<String> defaultOrderBy) {
        List<String> orderBy = new ArrayList<>();
        if (UtilValidate.isEmpty(sortExpression)) {
            if (UtilValidate.isNotEmpty(defaultOrderBy)) {
                orderBy.addAll(defaultOrderBy);
            }
            return orderBy;
        }

        Set<String> allowedFields = UtilValidate.isNotEmpty(sortFieldMap) ? sortFieldMap.keySet() : null;
        List<String> validatedFields = validateSortFields(sortExpression, allowedFields);
        Set<String> seenEntityFields = new HashSet<>();
        for (String requestedField : validatedFields) {
            boolean descending = requestedField.startsWith("-");
            String sortKey = descending ? requestedField.substring(1) : requestedField;
            String entityField = UtilValidate.isNotEmpty(sortFieldMap) ? sortFieldMap.get(sortKey) : sortKey;
            if (UtilValidate.isEmpty(entityField)) {
                throw new IllegalArgumentException("Unsupported sort field: " + sortKey);
            }
            if (!seenEntityFields.add(entityField)) {
                throw new IllegalArgumentException("Duplicate sort field: " + entityField);
            }
            orderBy.add((descending ? "-" : "") + entityField);
        }

        if (UtilValidate.isNotEmpty(defaultOrderBy)) {
            for (String defaultField : defaultOrderBy) {
                String normalizedDefaultField = defaultField.startsWith("-") ? defaultField.substring(1) : defaultField;
                if (!seenEntityFields.contains(normalizedDefaultField)) {
                    orderBy.add(defaultField);
                    seenEntityFields.add(normalizedDefaultField);
                }
            }
        }
        return orderBy;
    }

    /**
     * Returns a page slice from an already computed list.
     * <p>
     * Purpose: lets REST services apply framework-style pagination after they
     * build derived rows that cannot be paged directly through EntityQuery.
     *
     * @param rows the complete in-memory result list
     * @param pageIndex the zero-based page index
     * @param pageSize the requested page size
     * @param <T> the row type
     * @return a new list containing the requested page, or an empty list when the page is beyond the result size
     */
    public static <T> List<T> pageList(List<T> rows, int pageIndex, int pageSize) {
        if (UtilValidate.isEmpty(rows)) {
            return Collections.emptyList();
        }
        long fromIndex = (long) pageIndex * pageSize;
        if (fromIndex >= rows.size()) {
            return Collections.emptyList();
        }
        int safeFromIndex = (int) fromIndex;
        int toIndex = Math.min(safeFromIndex + pageSize, rows.size());
        return new ArrayList<>(rows.subList(safeFromIndex, toIndex));
    }

    /**
     * Sorts already computed map rows using REST sort-expression syntax.
     * <p>
     * Purpose: lets REST services expose the same {@code sort} contract for
     * derived map rows that EntityQuery-backed endpoints expose for entity rows.
     * The framework MapComparator performs the row comparison while this method
     * handles REST sort validation and stable fallback ordering.
     *
     * @param rows the complete in-memory result rows
     * @param sortExpression comma-separated REST sort tokens, optionally prefixed with {@code -}
     * @param allowedFields endpoint-supported map fields, or {@code null}/empty to apply syntax-only validation
     * @param fallbackComparator stable comparator used when no sort was requested or requested values tie
     * @return a sorted list, or an empty list when no rows exist
     * @throws IllegalArgumentException when a sort token is malformed or unsupported
     */
    public static List<Map<String, Object>> sortMapRows(List<Map<String, Object>> rows, String sortExpression,
            Set<String> allowedFields, Comparator<Map<String, Object>> fallbackComparator) {
        List<String> sortTokens = validateSortFields(sortExpression, allowedFields);
        if (UtilValidate.isEmpty(rows)) {
            return Collections.emptyList();
        }
        Comparator<Map<String, Object>> rowComparator = fallbackComparator;
        if (UtilValidate.isNotEmpty(sortTokens)) {
            MapComparator sortComparator = new MapComparator(sortTokens);
            rowComparator = (left, right) -> {
                int comparison = sortComparator.compare(UtilGenerics.cast(left), UtilGenerics.cast(right));
                return comparison != 0 ? comparison : fallbackComparator.compare(left, right);
            };
        }
        return new ArrayList<>(rows.stream()
                .sorted(rowComparator)
                .toList());
    }

    /**
     * Extracts the relative request URI plus query string for pagination link generation.
     * <p>
     * Purpose: REST pagination links operate on the request path stored in
     * {@link RestListResponseBuilder}, not on an absolute URL. This keeps API
     * responses consistent with existing relative link output while avoiding
     * repeated URI/query-string assembly in individual services. Use
     * {@code UtilHttp.getFullRequestUrl(request)} when an absolute URL is needed.
     *
     * @param request the originating servlet request
     * @return the relative request URI with query string, or {@code null} when the request is unavailable
     */
    public static String getRelativeRequestPath(HttpServletRequest request) {
        return UtilHttp.getRelativeRequestPath(request);
    }

    /**
     * Extracts the relative request path from a Groovy service binding when an
     * HTTP request is available.
     * <p>
     * Purpose: REST-exposed Groovy services can be invoked through HTTP or
     * directly through the service engine. Direct service calls do not bind a
     * servlet request, so this helper centralizes the safe missing-request check
     * while still letting HTTP calls generate pagination links.
     *
     * @param binding the Groovy service script binding
     * @return the relative request URI with query string, or {@code null} when no servlet request is bound
     */
    public static String getRelativeRequestPath(Binding binding) {
        if (binding == null || !binding.hasVariable("request")) {
            return null;
        }
        Object request = binding.getVariable("request");
        return request instanceof HttpServletRequest ? getRelativeRequestPath((HttpServletRequest) request) : null;
    }

    /**
     * Validates candidate filter parameters against an optional endpoint-defined
     * allowlist while preserving insertion order.
     * <p>
     * Purpose: lets REST services share framework filter validation instead of
     * implementing endpoint-local allowlist checks.
     *
     * @param filters the candidate filter parameters collected from the request
     * @param allowedFields the endpoint-supported filter fields, or
     *        {@code null}/empty to skip field-level validation
     * @return the validated filter parameters in insertion order
     * @throws IllegalArgumentException when a filter field is unsupported
     */
    public static Map<String, Object> validateFilterParameters(Map<String, ?> filters, Set<String> allowedFields) {
        return validateFilterParameters(filters, allowedFields, null, null);
    }

    /**
     * Validates candidate direct filter parameters against optional endpoint-defined
     * policies while preserving insertion order.
     * <p>
     * Purpose: lets REST services share filter allowlist, repeatable-parameter,
     * and per-field value validation rules.
     *
     * @param filters the candidate filter parameters collected from the request
     * @param allowedFields the endpoint-supported filter fields, or
     *        {@code null}/empty to skip field-level validation
     * @param repeatableFields the fields allowed to appear more than once, or
     *        {@code null}/empty to reject repeated values for all fields
     * @param valueValidators optional per-field validators for scalar or repeated values
     * @return the validated filter parameters in insertion order
     * @throws IllegalArgumentException when a filter field or value is invalid
     */
    public static Map<String, Object> validateFilterParameters(Map<String, ?> filters, Set<String> allowedFields,
            Set<String> repeatableFields, Map<String, FilterValueValidator> valueValidators) {
        if (UtilValidate.isEmpty(filters)) {
            return Collections.emptyMap();
        }

        Map<String, Object> validatedFilters = new LinkedHashMap<>();
        for (Map.Entry<String, ?> entry : filters.entrySet()) {
            if (UtilValidate.isEmpty(entry.getKey()) || UtilValidate.isEmpty(entry.getValue())) {
                continue;
            }
            if (UtilValidate.isNotEmpty(allowedFields) && !allowedFields.contains(entry.getKey())) {
                throw new IllegalArgumentException("Unsupported filter field: " + entry.getKey());
            }
            validateFilterValues(entry.getKey(), entry.getValue(), repeatableFields, valueValidators);
            validatedFilters.put(entry.getKey(), entry.getValue());
        }
        return validatedFilters;
    }

    /**
     * Serializes body-style pagination links into an RFC-style HTTP Link header
     * value while preserving the existing body-link order.
     *
     * @param links the response-body pagination links keyed by relation
     * @return the HTTP Link header value, or {@code null} when no complete relations exist
     */
    static String toLinkHeaderValue(Map<String, Object> links) {
        if (UtilValidate.isEmpty(links)) {
            return null;
        }

        List<String> headerParts = new ArrayList<>();
        for (Map.Entry<String, Object> entry : links.entrySet()) {
            Map<String, Object> link = castLinkMap(entry.getValue());
            Object href = link.get("href");
            Object rel = link.get("rel");
            if (UtilValidate.isNotEmpty(href) && UtilValidate.isNotEmpty(rel)) {
                headerParts.add("<" + href + ">; rel=\"" + rel + "\"");
            }
        }
        return headerParts.isEmpty() ? null : String.join(", ", headerParts);
    }

    @SuppressWarnings("unchecked")
    private static String getPaginationLinkHeaderValue(Object data) {
        if (!(data instanceof Map<?, ?> dataMap)) {
            return null;
        }
        Object links = dataMap.get("links");
        return links instanceof Map<?, ?> ? toLinkHeaderValue((Map<String, Object>) links) : null;
    }

    /**
     * Builds a list result from already normalized REST query options.
     *
     * @param collectionName the response property name for the collection
     * @param collectionData the collection payload
     * @param queryOptions the normalized REST query options
     * @param totalCount the total matching record count
     * @param requestPath the originating request path
     * @return a map containing collection data, pagination metadata, and links
     */
    public static Map<String, Object> getPagedResult(String collectionName, Object collectionData, RestQueryOptions queryOptions,
            long totalCount, String requestPath) {
        return RestListResponseBuilder.forList(collectionName, collectionData)
                .pageIndex(queryOptions.getPageIndex())
                .pageSize(queryOptions.getPageSize())
                .totalCount(totalCount)
                .requestPath(requestPath)
                .build();
    }

    /**
     * Builds a list result from explicit paging values.
     *
     * @param collectionName the response property name for the collection
     * @param collectionData the collection payload
     * @param pageIndex the zero-based page index
     * @param pageSize the page size
     * @param totalCount the total matching record count
     * @param requestPath the originating request path
     * @return a map containing collection data, pagination metadata, and links
     */
    public static Map<String, Object> getCollectionResult(String collectionName, Object collectionData, int pageIndex, int pageSize,
            long totalCount, String requestPath) {
        return RestListResponseBuilder.forList(collectionName, collectionData)
                .pageIndex(pageIndex)
                .pageSize(pageSize)
                .totalCount(totalCount)
                .requestPath(requestPath)
                .build();
    }

    private static String decodeQueryValue(String value) {
        try {
            return URLDecoder.decode(value, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("UTF-8 must always be available", e);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castLinkMap(Object value) {
        return value instanceof Map<?, ?> ? (Map<String, Object>) value : Collections.emptyMap();
    }

    private static void validateFilterValues(String fieldName, Object value, Set<String> repeatableFields,
            Map<String, FilterValueValidator> valueValidators) {
        if (value instanceof List<?> values) {
            if (values.size() > 1 && (UtilValidate.isEmpty(repeatableFields) || !repeatableFields.contains(fieldName))) {
                throw new IllegalArgumentException("Filter parameter does not support repeated values: " + fieldName);
            }
            for (Object item : values) {
                validateFilterValue(fieldName, item, valueValidators);
            }
            return;
        }
        validateFilterValue(fieldName, value, valueValidators);
    }

    private static void validateFilterValue(String fieldName, Object value, Map<String, FilterValueValidator> valueValidators) {
        if (UtilValidate.isEmpty(value) || UtilValidate.isEmpty(valueValidators)) {
            return;
        }
        FilterValueValidator validator = valueValidators.get(fieldName);
        if (validator != null) {
            validator.validate(fieldName, value);
        }
    }

    private static String encodeQueryValue(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.name()).replace("+", "%20");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("UTF-8 must always be available", e);
        }
    }

    /**
     * Validates the direct query-parameter values accepted for a filter field.
     */
    @FunctionalInterface
    public interface FilterValueValidator {

        /**
         * Validates a single filter value for the given field.
         *
         * @param fieldName the filter field name
         * @param value the direct request value to validate
         * @throws IllegalArgumentException when the value is not supported
         */
        void validate(String fieldName, Object value);
    }

    /**
     * Represents a decoded query parameter while preserving encounter order and
     * repeated keys for downstream link generation.
     */
    static final class QueryParameter {
        private final String name;
        private final String value;

        QueryParameter(String name, String value) {
            this.name = name;
            this.value = value;
        }

        /**
         * Returns the decoded parameter name.
         *
         * @return the decoded parameter name
         */
        public String getName() {
            return name;
        }

        /**
         * Returns the decoded parameter value.
         *
         * @return the decoded parameter value
         */
        public String getValue() {
            return value;
        }
    }

    /**
     * Extracts the http status code from the service result to override the default.
     * The parameter is removed from the resultMap to not show in the resulting data.
     * @param data
     * @return
     */
    public static StatusType extractResponseCode(Object data) {
        Map<String, ? extends Object> resultMap = UtilGenerics.<String, Object>checkMap(data, String.class, Object.class);
        Integer statusCode = null;

        try {
            statusCode = (Integer) resultMap.get(RESPONSE_STATUS_KEY);
        } catch (ClassCastException e) {
            // do nothing
        }

        if (statusCode == null) {
            return null;
        }
        resultMap.remove(RESPONSE_STATUS_KEY);

        StatusType statusType = Response.Status.fromStatusCode(statusCode);
        if (statusType == null) {
            statusType = ResponseStatus.Custom.fromStatusCode(statusCode);
        }
        return statusType;
    }
}
