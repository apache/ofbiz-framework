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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.glassfish.jersey.server.ContainerRequest;
import org.junit.jupiter.api.Test;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;

/**
 * Tests for {@link RestRequestHandler#extract(MultivaluedMap)} and
 * {@link RestRequestHandler#extractRequestBody(ContainerRequestContext)}.
 *
 * <p>{@code RestRequestHandler} is abstract and its {@code @Inject} fields
 * are not relevant to these two methods, so a minimal concrete subclass is
 * used purely to obtain an instance. {@code extract} and
 * {@code extractRequestBody} are exercised directly rather than through
 * {@code apply(...)}.</p>
 */
class RestRequestHandlerTest {

    /**
     * Minimal concrete subclass solely to instantiate the abstract class
     * under test. {@code execute} is never invoked by the tests below.
     */
    private static final class TestHandler extends RestRequestHandler {
        @Override
        protected Response execute(ContainerRequestContext data, Map<String, Object> arguments) {
            return null;
        }
    }

    private final TestHandler handler = new TestHandler();


    @Test
    void testExtractSingleValueParameterIsStoredAsPlainString() {
        MultivaluedMap<String, String> params = new MultivaluedHashMap<>();
        params.putSingle("testParam", "testValue");

        Map<String, Object> result = handler.extract(params);

        assertEquals("testValue", result.get("testParam"));
    }

    @Test
    void testExtractMultiValueParameterIsStoredAsList() {
        MultivaluedMap<String, String> params = new MultivaluedHashMap<>();
        params.add("tag", "testValue");
        params.add("tag", "testValueTwo");

        Map<String, Object> result = handler.extract(params);

        assertEquals(List.of("testValue", "testValueTwo"), result.get("tag"));
    }

    @Test
    void testExtractSkipsKeysWithEmptyValueList() {
        MultivaluedMap<String, String> params = new MultivaluedHashMap<>();
        params.put("empty", List.of());
        params.putSingle("present", "value");

        Map<String, Object> result = handler.extract(params);

        assertEquals(1, result.size());
        assertEquals("value", result.get("present"));
        assertTrue(!result.containsKey("empty"));
    }

    @Test
    void testExtractOfEmptyMapReturnsEmptyMap() {
        MultivaluedMap<String, String> params = new MultivaluedHashMap<>();

        Map<String, Object> result = handler.extract(params);

        assertTrue(result.isEmpty());
    }


    @Test
    void testExtractRequestBodyReturnsEmptyMapWhenNotAContainerRequest() {
        ContainerRequestContext requestContext = mock(ContainerRequestContext.class);

        Map<String, Object> result = handler.extractRequestBody(requestContext);

        assertTrue(result.isEmpty());
        verifyNoInteractions(requestContext);
    }

    @Test
    void testExtractRequestBodyReturnsEmptyMapWhenNoEntity() {
        ContainerRequest requestContext = mock(ContainerRequest.class);
        when(requestContext.hasEntity()).thenReturn(false);

        Map<String, Object> result = handler.extractRequestBody(requestContext);

        assertTrue(result.isEmpty());
    }

    @Test
    void testExtractRequestBodyReturnsEmptyMapWhenContentTypeIsNotJson() {
        ContainerRequest requestContext = mock(ContainerRequest.class);
        when(requestContext.hasEntity()).thenReturn(true);
        when(requestContext.getMediaType()).thenReturn(MediaType.TEXT_PLAIN_TYPE);

        Map<String, Object> result = handler.extractRequestBody(requestContext);

        assertTrue(result.isEmpty());
        verify(requestContext, org.mockito.Mockito.never()).readEntity(eq(Map.class));
    }

    @Test
    void testExtractRequestBodyReturnsEmptyMapWhenJsonEntityIsNull() {
        ContainerRequest requestContext = mock(ContainerRequest.class);
        when(requestContext.hasEntity()).thenReturn(true);
        when(requestContext.getMediaType()).thenReturn(MediaType.APPLICATION_JSON_TYPE);
        when(requestContext.readEntity(eq(Map.class))).thenReturn(null);

        Map<String, Object> result = handler.extractRequestBody(requestContext);

        assertTrue(result.isEmpty());
    }

    @Test
    void testExtractRequestBodyReturnsParsedMapForJsonEntity() {
        ContainerRequest requestContext = mock(ContainerRequest.class);
        when(requestContext.hasEntity()).thenReturn(true);
        when(requestContext.getMediaType()).thenReturn(MediaType.APPLICATION_JSON_TYPE);
        when(requestContext.readEntity(eq(Map.class))).thenReturn(Map.of("id", "123", "active", true));

        Map<String, Object> result = handler.extractRequestBody(requestContext);

        assertEquals("123", result.get("id"));
        assertEquals(true, result.get("active"));
        verify(requestContext).bufferEntity();
    }
}
