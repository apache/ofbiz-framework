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
package org.apache.ofbiz.webapp.control;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.DelegatorFactory;
import org.apache.ofbiz.entity.util.EntityUtilProperties;
import org.apache.ofbiz.security.login.MfaServices;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;
import org.apache.ofbiz.webapp.WebAppUtil;
import org.apache.ofbiz.webapp.stats.VisitHandler;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * Verifies that with {@code username.lowercase=true}, a differently-cased web login (e.g. "ADMIN" for
 * a stored "admin") still routes through {@code userLoginMfaPending} rather than the plain, non-MFA
 * {@code userLogin} service.
 */
public class LoginWorkerMfaUsernameCaseTests {

    @Test
    public void testDifferentlyCasedUsernameStillRoutesThroughMfaPendingLogin() throws Exception {
        Delegator delegator = mock(Delegator.class);
        when(delegator.getDelegatorBaseName()).thenReturn("default");

        LocalDispatcher dispatcher = mock(LocalDispatcher.class);
        ServletContext servletContext = mock(ServletContext.class);

        HttpSession session = mock(HttpSession.class);
        when(session.isNew()).thenReturn(true);
        when(session.getServletContext()).thenReturn(servletContext);

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getSession()).thenReturn(session);
        when(request.getAttribute("delegator")).thenReturn(delegator);
        when(request.getAttribute("dispatcher")).thenReturn(dispatcher);
        when(request.getParameter("USERNAME")).thenReturn("ADMIN");
        when(request.getParameter("PASSWORD")).thenReturn("ofbiz");

        HttpServletResponse response = mock(HttpServletResponse.class);

        try (MockedStatic<DelegatorFactory> delegatorFactory = mockStatic(DelegatorFactory.class);
                MockedStatic<WebAppUtil> webAppUtil = mockStatic(WebAppUtil.class);
                MockedStatic<VisitHandler> visitHandler = mockStatic(VisitHandler.class);
                MockedStatic<EntityUtilProperties> props = mockStatic(EntityUtilProperties.class);
                MockedStatic<MfaServices> mfaServices = mockStatic(MfaServices.class)) {
            delegatorFactory.when(() -> DelegatorFactory.getDelegator("default")).thenReturn(delegator);
            webAppUtil.when(() -> WebAppUtil.makeWebappDispatcher(eq(servletContext), eq(delegator))).thenReturn(dispatcher);
            visitHandler.when(() -> VisitHandler.getVisitId(eq(session))).thenReturn("test-visit-id");
            props.when(() -> EntityUtilProperties.getPropertyValue(eq("security"), eq("username.lowercase"), any()))
                    .thenReturn("true");
            props.when(() -> EntityUtilProperties.getPropertyValue(eq("security"), eq("security.login.authFactor.enable"),
                    eq("false"), any())).thenReturn("false");
            // Only the normalized ("admin"), not the typed ("ADMIN"), userLoginId has an active factor --
            // exactly the mismatch that let this account's factor go unnoticed before the fix.
            mfaServices.when(() -> MfaServices.hasActiveFactor(eq(delegator), eq("admin"))).thenReturn(true);
            mfaServices.when(() -> MfaServices.hasActiveFactor(eq(delegator), eq("ADMIN"))).thenReturn(false);

            Map<String, Object> errorResult = ServiceUtil.returnError("Simulated login failure, not under test");
            when(dispatcher.runSync(eq("userLoginMfaPending"), anyMap())).thenReturn(errorResult);

            LoginWorker.login(request, response);
        }

        verify(dispatcher).runSync(eq("userLoginMfaPending"), anyMap());
        verify(dispatcher, never()).runSync(eq("userLogin"), anyMap());
    }
}
