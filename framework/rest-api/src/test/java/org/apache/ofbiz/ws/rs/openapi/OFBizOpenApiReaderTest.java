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
package org.apache.ofbiz.ws.rs.openapi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.ws.rs.core.OFBizApiConfig;
import org.apache.ofbiz.ws.rs.model.ModelApi;
import org.apache.ofbiz.ws.rs.model.ModelOperation;
import org.apache.ofbiz.ws.rs.model.ModelResource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import jakarta.servlet.ServletContext;

class OFBizOpenApiReaderTest {

    private OFBizOpenApiReader reader;

    @BeforeEach
    void setUp() {
        reader = new OFBizOpenApiReader();
    }

    /**
     * Verifies that a single API resource produces a correct OpenAPI path + GET operation.
     */
    @SuppressWarnings("deprecation")
    @Test
    void testReaderGenerateOpenApiPathAndGetOperation() throws Exception {

        // Mocked Dispatcher Chain
        LocalDispatcher dispatcher = mock(LocalDispatcher.class);
        DispatchContext dctx = mock(DispatchContext.class);

        when(dispatcher.getDispatchContext()).thenReturn(dctx);

        // Mock servlet context + static access
        try (MockedStatic<org.apache.ofbiz.ws.rs.listener.ApiContextListener> ctxMock =
                     Mockito.mockStatic(org.apache.ofbiz.ws.rs.listener.ApiContextListener.class);
             MockedStatic<org.apache.ofbiz.webapp.WebAppUtil> webMock =
                     Mockito.mockStatic(org.apache.ofbiz.webapp.WebAppUtil.class);
             MockedStatic<OFBizApiConfig> apiMock =
                     Mockito.mockStatic(OFBizApiConfig.class)) {

            ServletContext servletContext = mock(ServletContext.class);

            ctxMock.when(org.apache.ofbiz.ws.rs.listener.ApiContextListener::getApplicationCntx)
                    .thenReturn(servletContext);

            webMock.when(() -> org.apache.ofbiz.webapp.WebAppUtil.getDispatcher(servletContext))
                    .thenReturn(dispatcher);

            // Mocked API structure
            ModelOperation op = mock(ModelOperation.class);
            when(op.getPath()).thenReturn("hello");
            when(op.getVerb()).thenReturn("GET");
            when(op.getService()).thenReturn("testService");
            when(op.getDescription()).thenReturn("test operation");

            ModelResource resource = mock(ModelResource.class);
            when(resource.getPath()).thenReturn("resource");
            when(resource.getDisplayName()).thenReturn("TestResource");
            when(resource.getDescription()).thenReturn("desc");
            when(resource.getOperations()).thenReturn(List.of(op));
            when(resource.getSubResources()).thenReturn(List.of());

            ModelApi api = mock(ModelApi.class);
            when(api.isPublish()).thenReturn(true);
            when(api.getPath()).thenReturn("api");
            when(api.getResources()).thenReturn(List.of(resource));

            apiMock.when(OFBizApiConfig::getModelApis)
                    .thenReturn(Map.of("test", api));

            // Mocked service resolution
            org.apache.ofbiz.service.ModelService service =
                    mock(org.apache.ofbiz.service.ModelService.class);

            when(service.getName()).thenReturn("testService");
            when(service.getInModelParamList()).thenReturn(List.of());
            when(service.isExport()).thenReturn(true);

            when(dctx.getModelService("testService")).thenReturn(service);

            // actual test
            OpenAPI openAPI = reader.read(Set.of(), Map.of());

            assertNotNull(openAPI);

            PathItem pathItem = openAPI.getPaths().get("/api/resource/hello");
            assertNotNull(pathItem);

            Operation getOp = pathItem.getGet();
            assertNotNull(getOp);

            assertEquals("testService", getOp.getOperationId());
            assertEquals("test operation", getOp.getSummary());
        }
    }

    /**
     * Verifies that buildNestedUrl correctly joins segments.
     */
    @Test
    void testBuildNestedUrlShouldNormalizeSegments() {
        List<String> segments = List.of("/api/", "/resource/", "hello");

        String url = OFBizOpenApiReader.buildNestedUrl(segments);

        assertEquals("/api/resource/hello", url);
    }

    /**
     * Verifies that empty segments are ignored.
     */
    @Test
    void buildNestedUrlShouldIgnoreEmptySegments() {
        List<String> segments = new ArrayList<>();
        segments.add("");
        segments.add("/");
        segments.add("api");
        segments.add(null);
        segments.add("test");

        String url = OFBizOpenApiReader.buildNestedUrl(segments);

        assertEquals("/api/test", url);
    }
}
