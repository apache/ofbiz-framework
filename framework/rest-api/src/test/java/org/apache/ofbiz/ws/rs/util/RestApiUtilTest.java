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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.ofbiz.service.ModelService;
import org.apache.ofbiz.ws.rs.response.Error;
import org.apache.ofbiz.ws.rs.response.Success;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;

public final class RestApiUtilTest {

    private RestApiUtilTest() { }

    @Test
    void testSuccess() {
        String message = "Success";
        Map<String, Object> data = new HashMap<>();
        data.put("dataKey", "dataValue");

        Response response = RestApiUtil.success(message, data);

        Success expected = new Success(200, "OK", message, data);
        Success actual = (Success) response.getEntity();

        assertEquals(expected.getStatusCode(), actual.getStatusCode());
        assertEquals(expected.getStatusDescription(), actual.getStatusDescription());
        assertEquals(expected.getSuccessMessage(), actual.getSuccessMessage());
        assertEquals(expected.getData(), actual.getData());
    }

    @Test
    void testError() {
        String message = "Error";
        String reason = "reason for error";

        Response response = RestApiUtil.error(400, reason, message);

        Error expected = new Error(400, reason, message);
        Error actual = (Error) response.getEntity();

        assertEquals(expected.getAdditionalErrors(), actual.getAdditionalErrors());
        assertEquals(expected.getClass(), actual.getClass());
        assertEquals(expected.getErrorDescription(), actual.getErrorDescription());
        assertEquals(expected.getErrorMessage(), actual.getErrorMessage());
        assertEquals(expected.getStatusCode(), actual.getStatusCode());
        assertEquals(expected.getStatusDescription(), actual.getStatusDescription());
        assertEquals(expected.getType(), actual.getType());
    }

    @Test
    void testResponseBuilder() {
        String message = "Error";
        String reason = "reason for error";

        Response response = RestApiUtil.errorBuilder(400, reason, message).build();

        assertEquals(400, response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON, response.getMediaType().toString());

        Error expected = (Error) response.getEntity();
        assertEquals(expected, response.getEntity());
    }

    @Test
    void testExtractParams() {
        MultivaluedMap<String, String> input = new MultivaluedHashMap<>();
        input.add("name", "John");
        input.add("role", "admin");
        input.add("role", "user");
        input.put("empty", List.of());
        input.put("null", null);

        Map<String, Object> result = RestApiUtil.extractParams(input);

        // Assert single-value parameter
        assertEquals("John", result.get("name"));
        assertTrue(result.get("name") instanceof String);

        // Assert multi-value parameter
        assertTrue(result.get("role") instanceof List);

        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) result.get("role");
        assertEquals(List.of("admin", "user"), roles);

        // Assert empty parameter is ignored
        assertFalse(result.containsKey("empty"));

        // Assert null parameter is ignored
        assertFalse(result.containsKey("null"));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = { "my/path/without/parameters", ""})
    void testGetPathParametersReturnEmptyList(String pathInfo) {
        List<String> pathParameters = RestApiUtil.getPathParameters(pathInfo);
        assertTrue(pathParameters.isEmpty());
    }

    @Test
    void testGetPathParameters() {
        String pathInfo = "my/path/with/parameters/{parameterOne}/middle/and/end/{parameterTwo}";

        List<String> pathParameters = RestApiUtil.getPathParameters(pathInfo);

        assertTrue(pathParameters.contains("parameterOne"));
        assertTrue(pathParameters.contains("parameterTwo"));
    }

    @Test
    void testBuildErrorFromServiceResult() {
        Map<String, Object> result = new HashMap<>();

        List<String> errors = new LinkedList<>(List.of("errorOne", "errorTwo", "errorThree"));
        result.put(ModelService.ERROR_MESSAGE_LIST, errors);

        Response response =
                RestApiUtil.buildErrorFromServiceResult("TestService", result, Locale.ENGLISH);

        Error error = (Error) response.getEntity();

        assertEquals("errorOne", error.getErrorDescription());
        assertEquals(List.of("errorTwo", "errorThree"), error.getAdditionalErrors());
    }
}
