/*
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
 */
package org.apache.ofbiz.ws.rs.security.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.EntityUtilProperties;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;
import org.apache.ofbiz.webapp.control.JWTManager;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.PathSegment;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

/**
 * Verifies that {@link APIAuthFilter#filter} fails closed (aborts the request) when the
 * {@code isUserLoginMfaRequired} policy check cannot be evaluated, instead of silently treating
 * the account as not requiring MFA, mirroring
 * {@link org.apache.ofbiz.ws.rs.resources.AuthenticationResourceMfaFailClosedTests}.
 */
public class APIAuthFilterMfaFailClosedTests {

    private static final String USER_LOGIN_ID = "testuser";
    private static final String API_GROUP_PATH = "api";

    private Delegator newSelfReferencingDelegatorMock() {
        Delegator delegator = mock(Delegator.class);
        when(delegator.getDelegator()).thenReturn(delegator);
        return delegator;
    }

    private APIAuthFilter newFilterWithContext(ServletContext servletContext, HttpServletRequest httpRequest)
            throws ReflectiveOperationException {
        APIAuthFilter filter = new APIAuthFilter();
        setPrivateField(filter, "httpRequest", httpRequest);
        setPrivateField(filter, "servletContext", servletContext);
        return filter;
    }

    private void setPrivateField(Object target, String fieldName, Object value) throws ReflectiveOperationException {
        Field field = APIAuthFilter.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private ContainerRequestContext requestContextFor(String apiGroupPath) {
        ContainerRequestContext requestContext = mock(ContainerRequestContext.class);
        when(requestContext.getHeaderString(jakarta.ws.rs.core.HttpHeaders.AUTHORIZATION)).thenReturn("Bearer a.b.c");
        UriInfo uriInfo = mock(UriInfo.class);
        PathSegment pathSegment = mock(PathSegment.class);
        when(pathSegment.getPath()).thenReturn(apiGroupPath);
        when(uriInfo.getPathSegments()).thenReturn(List.of(pathSegment));
        when(requestContext.getUriInfo()).thenReturn(uriInfo);
        return requestContext;
    }

    private void runFailClosedScenario(LocalDispatcherStub dispatcherStub, String expectedMessageFragment)
            throws ReflectiveOperationException, GenericServiceException, GenericEntityException, IOException {
        GenericValue userLogin = mock(GenericValue.class);
        when(userLogin.getString("userLoginId")).thenReturn(USER_LOGIN_ID);

        Delegator delegator = newSelfReferencingDelegatorMock();
        LocalDispatcher dispatcher = mock(LocalDispatcher.class);
        dispatcherStub.stub(dispatcher);

        ServletContext servletContext = mock(ServletContext.class);
        when(servletContext.getAttribute("delegator")).thenReturn(delegator);
        when(servletContext.getAttribute("dispatcher")).thenReturn(dispatcher);

        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        ContainerRequestContext requestContext = requestContextFor(API_GROUP_PATH);

        APIAuthFilter filter = newFilterWithContext(servletContext, httpRequest);

        EntityQuery mockQuery = mock(EntityQuery.class);
        when(mockQuery.from("UserLogin")).thenReturn(mockQuery);
        when(mockQuery.where("userLoginId", USER_LOGIN_ID)).thenReturn(mockQuery);
        when(mockQuery.queryOne()).thenReturn(userLogin);

        try (MockedStatic<EntityQuery> entityQuery = mockStatic(EntityQuery.class);
                MockedStatic<EntityUtilProperties> props = mockStatic(EntityUtilProperties.class);
                MockedStatic<JWTManager> jwtManager = mockStatic(JWTManager.class)) {
            entityQuery.when(() -> EntityQuery.use(any())).thenReturn(mockQuery);
            props.when(() -> EntityUtilProperties.getPropertyValue(eq("security"),
                    eq("security.login.authFactor.enable"), eq("false"), any(Delegator.class))).thenReturn("true");
            jwtManager.when(() -> JWTManager.getHeaderAuthBearerToken(httpRequest)).thenReturn("a.b.c");
            jwtManager.when(() -> JWTManager.validateAccessToken(eq(delegator), eq("a.b.c")))
                    .thenReturn(Map.of("userLoginId", USER_LOGIN_ID, "apiGroupPath", API_GROUP_PATH));

            filter.filter(requestContext);
        }

        ArgumentCaptor<Response> responseCaptor = ArgumentCaptor.forClass(Response.class);
        verify(requestContext).abortWith(responseCaptor.capture());
        Response aborted = responseCaptor.getValue();
        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), aborted.getStatus(),
                expectedMessageFragment);
    }

    private interface LocalDispatcherStub {
        void stub(LocalDispatcher dispatcher) throws GenericServiceException;
    }

    @Test
    public void testFilterFailsClosedWhenMfaPolicyCheckThrows()
            throws ReflectiveOperationException, GenericServiceException, GenericEntityException, IOException {
        runFailClosedScenario(dispatcher -> when(dispatcher.runSync(eq("isUserLoginMfaRequired"), anyMap()))
                        .thenThrow(new GenericServiceException("Simulated MFA policy check failure")),
                "A thrown GenericServiceException while evaluating the MFA policy must deny the request "
                        + "(fail closed), not silently admit it.");
    }

    @Test
    public void testFilterFailsClosedWhenMfaPolicyCheckReturnsError()
            throws ReflectiveOperationException, GenericServiceException, GenericEntityException, IOException {
        Map<String, Object> errorResult = ServiceUtil.returnError("Simulated MFA policy evaluation failure");
        runFailClosedScenario(dispatcher -> when(dispatcher.runSync(eq("isUserLoginMfaRequired"), anyMap())).thenReturn(errorResult),
                "A service-error result from the MFA policy check must deny the request (fail closed), "
                        + "not be treated as \"MFA not required\".");
    }
}
