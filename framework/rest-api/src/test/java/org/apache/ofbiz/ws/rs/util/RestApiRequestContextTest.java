/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ofbiz.ws.rs.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.security.Security;
import org.apache.ofbiz.service.LocalDispatcher;
import org.junit.jupiter.api.Test;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;

class RestApiRequestContextTest {

    @Test
    void tenantContextOverridesServletContextForAllThreeObjects() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        ServletContext context = mock(ServletContext.class);
        Delegator tenantDelegator = mock(Delegator.class);
        LocalDispatcher tenantDispatcher = mock(LocalDispatcher.class);
        Security tenantSecurity = mock(Security.class);
        when(request.getAttribute("userTenantId")).thenReturn("TENANT_A");
        when(request.getAttribute("delegator")).thenReturn(tenantDelegator);
        when(request.getAttribute("dispatcher")).thenReturn(tenantDispatcher);
        when(request.getAttribute("security")).thenReturn(tenantSecurity);

        assertSame(tenantDelegator, RestApiRequestContext.getDelegator(request, context));
        assertSame(tenantDispatcher, RestApiRequestContext.getDispatcher(request, context));
        assertSame(tenantSecurity, RestApiRequestContext.getSecurity(request, context));

        verify(context, never()).getAttribute("delegator");
        verify(context, never()).getAttribute("dispatcher");
        verify(context, never()).getAttribute("security");
    }

    @Test
    void unmappedRequestRetainsBaseContext() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        ServletContext context = mock(ServletContext.class);
        Delegator base = mock(Delegator.class);
        when(context.getAttribute("delegator")).thenReturn(base);

        assertSame(base, RestApiRequestContext.getDelegator(request, context));
    }

    @Test
    void partiallyInitializedTenantContextDoesNotFallBackToBaseDispatcher() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        ServletContext context = mock(ServletContext.class);
        when(request.getAttribute("userTenantId")).thenReturn("TENANT_B");
        when(context.getAttribute("dispatcher")).thenReturn(mock(LocalDispatcher.class));

        assertThrows(IllegalStateException.class, () -> RestApiRequestContext.getDispatcher(request, context));
        verify(context, never()).getAttribute("dispatcher");
    }

    @Test
    void rejectsTenantTokenReplayAndMissingTenantClaims() {
        HttpServletRequest tenantRequest = mock(HttpServletRequest.class);
        when(tenantRequest.getAttribute("userTenantId")).thenReturn("TENANT_A");

        assertTrue(RestApiRequestContext.matchesTenant(tenantRequest, Map.of("tenantId", "TENANT_A")));
        assertFalse(RestApiRequestContext.matchesTenant(tenantRequest, Map.of("tenantId", "TENANT_B")));
        assertFalse(RestApiRequestContext.matchesTenant(tenantRequest, Map.of("userLoginId", "admin")));
    }

    @Test
    void baseRequestRejectsTenantBoundTokens() {
        HttpServletRequest baseRequest = mock(HttpServletRequest.class);

        assertTrue(RestApiRequestContext.matchesTenant(baseRequest, Map.of("userLoginId", "admin")));
        assertFalse(RestApiRequestContext.matchesTenant(baseRequest, Map.of("tenantId", "TENANT_A")));
    }

    @Test
    void unexpectedRequestAttributeTypeFailsClosed() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        ServletContext context = mock(ServletContext.class);
        when(request.getAttribute("delegator")).thenReturn("not-a-delegator");

        assertThrows(IllegalStateException.class, () -> RestApiRequestContext.getDelegator(request, context));
    }
}
