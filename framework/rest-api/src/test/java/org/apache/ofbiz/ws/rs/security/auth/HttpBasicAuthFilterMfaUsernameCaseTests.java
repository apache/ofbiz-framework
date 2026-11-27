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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.Base64;
import java.util.Map;

import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityUtilProperties;
import org.apache.ofbiz.security.login.MfaServices;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;
import org.apache.ofbiz.ws.rs.util.RestApiUtil;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.UriInfo;

/**
 * Verifies that with {@code username.lowercase=true}, a differently-cased Basic-Auth login (e.g.
 * "ADMIN" for a stored "admin") still routes through {@code userLoginMfaPending} rather than the
 * plain, non-MFA {@code userLogin} service.
 */
public class HttpBasicAuthFilterMfaUsernameCaseTests {

    private HttpBasicAuthFilter newFilterWithContext(ServletContext servletContext, HttpServletRequest httpRequest)
            throws ReflectiveOperationException {
        HttpBasicAuthFilter filter = new HttpBasicAuthFilter();
        setPrivateField(filter, "httpRequest", httpRequest);
        setPrivateField(filter, "servletContext", servletContext);
        return filter;
    }

    private void setPrivateField(Object target, String fieldName, Object value) throws ReflectiveOperationException {
        Field field = HttpBasicAuthFilter.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    public void testDifferentlyCasedUsernameStillRoutesThroughMfaPendingLogin() throws Exception {
        Delegator delegator = mock(Delegator.class);
        LocalDispatcher dispatcher = mock(LocalDispatcher.class);
        ServletContext servletContext = mock(ServletContext.class);
        when(servletContext.getAttribute("delegator")).thenReturn(delegator);
        when(servletContext.getAttribute("dispatcher")).thenReturn(dispatcher);

        HttpServletRequest httpRequest = mock(HttpServletRequest.class);

        String basicAuthHeader = "Basic " + Base64.getEncoder().encodeToString("ADMIN:ofbiz".getBytes());
        ContainerRequestContext requestContext = mock(ContainerRequestContext.class);
        when(requestContext.getHeaderString(HttpHeaders.AUTHORIZATION)).thenReturn(basicAuthHeader);
        UriInfo uriInfo = mock(UriInfo.class);
        @SuppressWarnings("unchecked")
        MultivaluedMap<String, String> pathParams = mock(MultivaluedMap.class);
        when(pathParams.getFirst("apiGroupPath")).thenReturn("api");
        when(uriInfo.getPathParameters()).thenReturn(pathParams);
        when(requestContext.getUriInfo()).thenReturn(uriInfo);

        Map<String, Object> successResult = ServiceUtil.returnSuccess();
        successResult.put("userLogin", mock(GenericValue.class));

        HttpBasicAuthFilter filter = newFilterWithContext(servletContext, httpRequest);

        try (MockedStatic<EntityUtilProperties> props = mockStatic(EntityUtilProperties.class);
                MockedStatic<MfaServices> mfaServices = mockStatic(MfaServices.class);
                MockedStatic<RestApiUtil> restApiUtil = mockStatic(RestApiUtil.class)) {
            props.when(() -> EntityUtilProperties.getPropertyValue(eq("security"), eq("username.lowercase"), any()))
                    .thenReturn("true");
            props.when(() -> EntityUtilProperties.getPropertyValue(eq("security"), eq("security.login.authFactor.enable"),
                    eq("false"), any())).thenReturn("false");
            // Only the normalized ("admin"), not the typed ("ADMIN"), userLoginId has an active factor --
            // exactly the mismatch that let this account's factor go unnoticed before the fix.
            mfaServices.when(() -> MfaServices.hasActiveFactor(eq(delegator), eq("admin"))).thenReturn(true);
            mfaServices.when(() -> MfaServices.hasActiveFactor(eq(delegator), eq("ADMIN"))).thenReturn(false);
            restApiUtil.when(() -> RestApiUtil.validateApiGroupPath("api")).thenReturn("api");
            restApiUtil.when(() -> RestApiUtil.getApiGroupBasePermission(eq(delegator), eq("api"))).thenReturn("TEST_PERMISSION");

            when(dispatcher.runSync(eq("userLoginMfaPending"), anyMap())).thenReturn(successResult);

            filter.filter(requestContext);
        }

        verify(dispatcher).runSync(eq("userLoginMfaPending"), anyMap());
        verify(dispatcher, never()).runSync(eq("userLogin"), anyMap());
    }
}
