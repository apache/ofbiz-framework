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
package org.apache.ofbiz.ws.rs.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.Map;

import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityUtilProperties;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.Response;

/**
 * Verifies that {@link AuthenticationResource#getAuthToken} fails closed (HTTP 401) when the
 * {@code isUserLoginMfaRequired} policy check cannot be evaluated, instead of silently treating
 * the account as not requiring MFA, mirroring the behavior already enforced by APIAuthFilter.
 */
public class AuthenticationResourceMfaFailClosedTests {

    private AuthenticationResource newResourceWithContext(ServletContext servletContext, HttpServletRequest httpRequest)
            throws ReflectiveOperationException {
        AuthenticationResource resource = new AuthenticationResource();
        setPrivateField(resource, "servletContext", servletContext);
        setPrivateField(resource, "httpRequest", httpRequest);
        setPrivateField(resource, "httpResponse", mock(HttpServletResponse.class));
        return resource;
    }

    private void setPrivateField(Object target, String fieldName, Object value) throws ReflectiveOperationException {
        Field field = AuthenticationResource.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private HttpServletRequest requestWithUserLogin(GenericValue userLogin) {
        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        when(httpRequest.getAttribute("userLogin")).thenReturn(userLogin);
        return httpRequest;
    }

    private ServletContext servletContextWith(Delegator delegator, LocalDispatcher dispatcher) {
        ServletContext servletContext = mock(ServletContext.class);
        when(servletContext.getAttribute("delegator")).thenReturn(delegator);
        when(servletContext.getAttribute("dispatcher")).thenReturn(dispatcher);
        return servletContext;
    }

    /**
     * {@link org.apache.ofbiz.entity.util.EntityQuery#use} takes a {@code DelegatorProvider}, a
     * supertype of {@code Delegator} itself; it calls {@code getDelegator()} on whatever is passed,
     * so a bare Mockito mock must echo itself back or every entity lookup resolves a null delegator.
     */
    private Delegator newSelfReferencingDelegatorMock() {
        Delegator delegator = mock(Delegator.class);
        when(delegator.getDelegator()).thenReturn(delegator);
        return delegator;
    }

    @Test
    public void testGetAuthTokenFailsClosedWhenMfaPolicyCheckThrows() throws ReflectiveOperationException, GenericServiceException {
        GenericValue userLogin = mock(GenericValue.class);
        when(userLogin.getString("userLoginId")).thenReturn("testuser");

        Delegator delegator = newSelfReferencingDelegatorMock();
        LocalDispatcher dispatcher = mock(LocalDispatcher.class);
        when(dispatcher.runSync(eq("isUserLoginMfaRequired"), anyMap()))
                .thenThrow(new GenericServiceException("Simulated MFA policy check failure"));

        AuthenticationResource resource = newResourceWithContext(
                servletContextWith(delegator, dispatcher), requestWithUserLogin(userLogin));

        try (MockedStatic<EntityUtilProperties> props = mockStatic(EntityUtilProperties.class)) {
            props.when(() -> EntityUtilProperties.getPropertyValue(eq("security"),
                    eq("security.login.authFactor.enable"), eq("false"), any(Delegator.class))).thenReturn("true");

            Response response = resource.getAuthToken("api", "Basic dGVzdHVzZXI6cGFzc3dvcmQ=");

            assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus(),
                    "A thrown GenericServiceException while evaluating the MFA policy must deny the token "
                            + "(fail closed), not silently issue a password-only token.");
        }
    }

    @Test
    public void testGetAuthTokenFailsClosedWhenMfaPolicyCheckReturnsError() throws ReflectiveOperationException, GenericServiceException {
        GenericValue userLogin = mock(GenericValue.class);
        when(userLogin.getString("userLoginId")).thenReturn("testuser");

        Delegator delegator = newSelfReferencingDelegatorMock();
        LocalDispatcher dispatcher = mock(LocalDispatcher.class);
        Map<String, Object> errorResult = ServiceUtil.returnError("Simulated MFA policy evaluation failure");
        when(dispatcher.runSync(eq("isUserLoginMfaRequired"), anyMap())).thenReturn(errorResult);

        AuthenticationResource resource = newResourceWithContext(
                servletContextWith(delegator, dispatcher), requestWithUserLogin(userLogin));

        try (MockedStatic<EntityUtilProperties> props = mockStatic(EntityUtilProperties.class)) {
            props.when(() -> EntityUtilProperties.getPropertyValue(eq("security"),
                    eq("security.login.authFactor.enable"), eq("false"), any(Delegator.class))).thenReturn("true");

            Response response = resource.getAuthToken("api", "Basic dGVzdHVzZXI6cGFzc3dvcmQ=");

            assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus(),
                    "A service-error result from the MFA policy check must deny the token (fail closed), "
                            + "not be treated as \"MFA not required\".");
        }
    }
}
