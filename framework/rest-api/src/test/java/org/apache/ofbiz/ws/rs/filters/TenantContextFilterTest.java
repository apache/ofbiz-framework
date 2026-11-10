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
package org.apache.ofbiz.ws.rs.filters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.security.Security;
import org.apache.ofbiz.service.LocalDispatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

class TenantContextFilterTest {

    private final Delegator base = mock(Delegator.class);
    private final ServletContext context = mock(ServletContext.class);
    private final FilterChain chain = mock(FilterChain.class);
    private final HttpServletResponse response = mock(HttpServletResponse.class);
    private final TestFilter filter = new TestFilter();

    @BeforeEach
    void setup() {
        FilterConfig config = mock(FilterConfig.class);
        when(config.getServletContext()).thenReturn(context);
        when(context.getAttribute("delegator")).thenReturn(base);
        filter.init(config);
    }

    @Test
    void registeredTenantDomainsKeepSeparateRequestContext() throws IOException, ServletException {
        Delegator tenantA = mock(Delegator.class);
        Delegator tenantB = mock(Delegator.class);
        LocalDispatcher dispatcherA = mock(LocalDispatcher.class);
        LocalDispatcher dispatcherB = mock(LocalDispatcher.class);
        Security securityA = mock(Security.class);
        Security securityB = mock(Security.class);
        filter.domains.put("a.example.test", "TENANT_A");
        filter.domains.put("b.example.test", "TENANT_B");
        filter.delegators.put("TENANT_A", tenantA);
        filter.delegators.put("TENANT_B", tenantB);
        filter.dispatchers.put(tenantA, dispatcherA);
        filter.dispatchers.put(tenantB, dispatcherB);
        filter.securities.put(tenantA, securityA);
        filter.securities.put(tenantB, securityB);

        HttpServletRequest requestA = request("a.example.test");
        HttpServletRequest requestB = request("b.example.test");
        filter.doFilter(requestA, response, chain);
        filter.doFilter(requestB, response, chain);

        verify(requestA).setAttribute("delegator", tenantA);
        verify(requestA).setAttribute("dispatcher", dispatcherA);
        verify(requestA).setAttribute("security", securityA);
        verify(requestA).setAttribute("userTenantId", "TENANT_A");
        verify(requestB).setAttribute("delegator", tenantB);
        verify(requestB).setAttribute("dispatcher", dispatcherB);
        verify(requestB).setAttribute("security", securityB);
        verify(requestB).setAttribute("userTenantId", "TENANT_B");
        verify(chain).doFilter(requestA, response);
        verify(chain).doFilter(requestB, response);
        verify(context, never()).setAttribute(anyString(), any());
    }

    @Test
    void concurrentTenantRequestsNeverShareDispatcherOrDelegator()
            throws InterruptedException, ExecutionException, TimeoutException {
        Delegator tenantA = mock(Delegator.class);
        Delegator tenantB = mock(Delegator.class);
        LocalDispatcher dispatcherA = mock(LocalDispatcher.class);
        LocalDispatcher dispatcherB = mock(LocalDispatcher.class);
        Security securityA = mock(Security.class);
        Security securityB = mock(Security.class);
        filter.domains.put("a.example.test", "TENANT_A");
        filter.domains.put("b.example.test", "TENANT_B");
        filter.delegators.put("TENANT_A", tenantA);
        filter.delegators.put("TENANT_B", tenantB);
        filter.dispatchers.put(tenantA, dispatcherA);
        filter.dispatchers.put(tenantB, dispatcherB);
        filter.securities.put(tenantA, securityA);
        filter.securities.put(tenantB, securityB);
        HttpServletRequest requestA = requestWithStoredAttributes("a.example.test");
        HttpServletRequest requestB = requestWithStoredAttributes("b.example.test");
        CyclicBarrier barrier = new CyclicBarrier(2);
        FilterChain chainA = (req, res) -> {
            awaitBothRequests(barrier);
            assertSame(tenantA, req.getAttribute("delegator"));
            assertSame(dispatcherA, req.getAttribute("dispatcher"));
            assertSame(securityA, req.getAttribute("security"));
        };
        FilterChain chainB = (req, res) -> {
            awaitBothRequests(barrier);
            assertSame(tenantB, req.getAttribute("delegator"));
            assertSame(dispatcherB, req.getAttribute("dispatcher"));
            assertSame(securityB, req.getAttribute("security"));
        };

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> first = executor.submit(() -> runFilter(requestA, chainA));
            Future<?> second = executor.submit(() -> runFilter(requestB, chainB));
            first.get(10, TimeUnit.SECONDS);
            second.get(10, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }
        verify(context, never()).setAttribute(anyString(), any());
    }

    private static void awaitBothRequests(CyclicBarrier barrier) throws ServletException {
        try {
            barrier.await(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new ServletException("Concurrent requests did not overlap", e);
        }
    }

    private void runFilter(HttpServletRequest request, FilterChain requestChain) {
        try {
            filter.doFilter(request, response, requestChain);
        } catch (IOException | ServletException e) {
            throw new IllegalStateException(e);
        }
    }

    private HttpServletRequest requestWithStoredAttributes(String serverName) {
        HttpServletRequest request = request(serverName);
        Map<String, Object> attributes = new ConcurrentHashMap<>();
        doAnswer(invocation -> {
            attributes.put(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(request).setAttribute(anyString(), any());
        when(request.getAttribute(anyString())).thenAnswer(invocation -> attributes.get(invocation.getArgument(0)));
        return request;
    }

    @Test
    void unmappedHostUsesExistingBaseBehavior() throws IOException, ServletException {
        HttpServletRequest request = request("unknown.example.test");
        filter.doFilter(request, response, chain);
        verify(chain).doFilter(request, response);
        verify(request, never()).setAttribute(anyString(), any());
    }

    @Test
    void invalidRegisteredTenantIsRejectedWithoutDispatch() throws IOException, ServletException {
        filter.domains.put("invalid.example.test", "NOT_ENABLED");
        filter.validTenant = false;
        HttpServletRequest request = request("invalid.example.test");

        filter.doFilter(request, response, chain);

        verify(response).sendError(HttpServletResponse.SC_NOT_FOUND);
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void unavailableTenantContextDoesNotFallBackToBase() throws IOException, ServletException {
        filter.domains.put("missing.example.test", "TENANT_MISSING");
        HttpServletRequest request = request("missing.example.test");

        filter.doFilter(request, response, chain);

        verify(response).sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void disabledMultitenancyKeepsExistingBehavior() throws IOException, ServletException {
        filter.multitenant = false;
        HttpServletRequest request = request("a.example.test");

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(request, never()).setAttribute(anyString(), any());
    }

    @Test
    void noRequestTenantIdParameterIsUsedForTenantSelection() throws IOException, ServletException {
        HttpServletRequest request = request("unmapped.example.test");
        when(request.getParameter("userTenantId")).thenReturn("ATTACKER_CHOSEN");

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(request, never()).setAttribute(anyString(), any());
        verify(request, never()).getParameter("userTenantId");
        assertEquals(0, filter.delegators.size());
    }

    private HttpServletRequest request(String serverName) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getServerName()).thenReturn(serverName);
        return request;
    }

    private static final class TestFilter extends TenantContextFilter {
        private boolean multitenant = true;
        private boolean validTenant = true;
        private final Map<String, String> domains = new HashMap<>();
        private final Map<String, Delegator> delegators = new HashMap<>();
        private final Map<Delegator, LocalDispatcher> dispatchers = new HashMap<>();
        private final Map<Delegator, Security> securities = new HashMap<>();

        @Override
        protected boolean isMultiTenantEnabled() {
            return multitenant;
        }

        @Override
        protected String findTenantId(Delegator baseDelegator, String serverName) {
            return domains.get(serverName);
        }

        @Override
        protected boolean isValidTenant(Delegator baseDelegator, String tenantId) {
            return validTenant;
        }

        @Override
        protected Delegator getTenantDelegator(Delegator baseDelegator, String tenantId) {
            return delegators.get(tenantId);
        }

        @Override
        protected LocalDispatcher getTenantDispatcher(Delegator tenantDelegator) {
            return dispatchers.get(tenantDelegator);
        }

        @Override
        protected Security getTenantSecurity(Delegator tenantDelegator) {
            return securities.get(tenantDelegator);
        }
    }
}
