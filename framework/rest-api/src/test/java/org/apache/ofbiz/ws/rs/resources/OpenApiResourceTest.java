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
package org.apache.ofbiz.ws.rs.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.ofbiz.ws.rs.openapi.OFBizOpenApiReader;
import org.apache.ofbiz.ws.rs.openapi.OFBizResourceScanner;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

class OpenApiResourceTest {


    @SuppressWarnings("try")
    @ParameterizedTest
    @CsvSource({
        "json, application/json",
        "yaml, application/yaml"
    })
    void testGetOpenApi(String type, String expectedType) throws Exception {

        // Mock HTTP request context
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getScheme()).thenReturn("http");
        when(request.getServerName()).thenReturn("localhost");
        when(request.getServerPort()).thenReturn(8080);
        when(request.getContextPath()).thenReturn("/ofbiz");

        OpenApiResource resource = new OpenApiResource();

        // inject request via reflection (since field is @Context)
        java.lang.reflect.Field f = OpenApiResource.class.getDeclaredField("request");
        f.setAccessible(true);
        f.set(resource, request);

        // Mock heavy Swagger builder
        try (MockedConstruction<org.apache.ofbiz.ws.rs.openapi.OFBizOpenApiReader> readerMock =
                Mockito.mockConstruction(OFBizOpenApiReader.class, (mock, context) -> { });
                MockedConstruction<OFBizResourceScanner> scannerMock = Mockito.mockConstruction(OFBizResourceScanner.class)) {

            Response response = resource.getOpenApi(mock(HttpHeaders.class), mock(UriInfo.class), type);

            assertEquals(200, response.getStatus());
            assertEquals(expectedType, response.getMediaType().toString());
            assertNotNull(response.getEntity());
        }
    }
}
