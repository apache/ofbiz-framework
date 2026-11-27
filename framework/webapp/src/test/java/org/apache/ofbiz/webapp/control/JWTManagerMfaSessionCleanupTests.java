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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.EntityUtilProperties;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * Verifies that {@link JWTManager#checkJWTLogin} removes the session's {@code "userLogin"} attribute
 * before returning {@code "error"} when the MFA policy check fails closed (throws, or returns a
 * service-error result), matching the cleanup already done on the {@code required=true} step-up
 * branch before it returns {@code authFactorRequired}.
 */
public class JWTManagerMfaSessionCleanupTests {

    private static final String USER_LOGIN_ID = "testuser";

    private Delegator newSelfReferencingDelegatorMock() {
        Delegator delegator = mock(Delegator.class);
        when(delegator.getDelegator()).thenReturn(delegator);
        when(delegator.getDelegatorName()).thenReturn("default");
        return delegator;
    }

    private interface LocalDispatcherStub {
        void stub(LocalDispatcher dispatcher) throws GenericServiceException;
    }

    private void runFailClosedScenario(LocalDispatcherStub dispatcherStub) throws Exception {
        Delegator delegator = newSelfReferencingDelegatorMock();

        GenericValue userLogin = mock(GenericValue.class);
        when(userLogin.getString("userLoginId")).thenReturn(USER_LOGIN_ID);
        when(userLogin.getString("enabled")).thenReturn("Y");
        when(userLogin.getDelegator()).thenReturn(delegator);

        LocalDispatcher dispatcher = mock(LocalDispatcher.class);
        dispatcherStub.stub(dispatcher);

        Map<String, Object> claims = new HashMap<>();
        claims.put("userLoginId", USER_LOGIN_ID);
        String token = JWTManager.createJwt(delegator, claims);

        HttpSession session = mock(HttpSession.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute("delegator")).thenReturn(delegator);
        when(request.getAttribute("dispatcher")).thenReturn(dispatcher);
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(request.getSession()).thenReturn(session);
        when(request.getSession(false)).thenReturn(session);
        HttpServletResponse response = mock(HttpServletResponse.class);

        EntityQuery mockQuery = mock(EntityQuery.class);
        when(mockQuery.from("UserLogin")).thenReturn(mockQuery);
        when(mockQuery.where("userLoginId", USER_LOGIN_ID)).thenReturn(mockQuery);
        when(mockQuery.queryOne()).thenReturn(userLogin);

        try (MockedStatic<EntityQuery> entityQuery = mockStatic(EntityQuery.class);
                MockedStatic<EntityUtilProperties> props = mockStatic(EntityUtilProperties.class);
                MockedStatic<LoginWorker> loginWorker = mockStatic(LoginWorker.class)) {
            entityQuery.when(() -> EntityQuery.use(any())).thenReturn(mockQuery);
            // Any property not explicitly overridden below answers with its own default value,
            // exactly like a real, unconfigured install -- needed because createJwt/validateAccessToken
            // read several unrelated properties (e.g. the token expiry) through this same method.
            props.when(() -> EntityUtilProperties.getPropertyValue(any(), any(), any(), any()))
                    .thenAnswer(invocation -> invocation.getArgument(2));
            props.when(() -> EntityUtilProperties.getPropertyValue(eq("security"), eq("security.internal.sso.enabled"),
                    eq("false"), any())).thenReturn("true");
            props.when(() -> EntityUtilProperties.getPropertyValue(eq("security"), eq("security.login.authFactor.enable"),
                    eq("false"), any())).thenReturn("true");
            loginWorker.when(() -> LoginWorker.isMfaInProgress(any())).thenReturn(false);
            loginWorker.when(() -> LoginWorker.isUserLoginActive(any())).thenReturn(true);
            loginWorker.when(() -> LoginWorker.checkLogout(any(), any())).thenReturn(userLogin);

            String result = JWTManager.checkJWTLogin(request, response);

            assertEquals("error", result);
        }

        verify(session).setAttribute("userLogin", userLogin);
        verify(session).removeAttribute("userLogin");
    }

    @Test
    public void testCheckJWTLoginRemovesSessionUserLoginWhenMfaPolicyCheckThrows() throws Exception {
        runFailClosedScenario(dispatcher -> when(dispatcher.runSync(eq("isUserLoginMfaRequired"), anyMap()))
                .thenThrow(new GenericServiceException("Simulated MFA policy check failure")));
    }

    @Test
    public void testCheckJWTLoginRemovesSessionUserLoginWhenMfaPolicyCheckReturnsError() throws Exception {
        Map<String, Object> errorResult = ServiceUtil.returnError("Simulated MFA policy evaluation failure");
        runFailClosedScenario(dispatcher -> when(dispatcher.runSync(eq("isUserLoginMfaRequired"), anyMap())).thenReturn(errorResult));
    }
}
