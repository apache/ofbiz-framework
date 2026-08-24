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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericValue;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

public class ExternalLoginKeysManagerTests {
    @Test
    public void getExternalLoginKeyReturnsKeyFromRequest() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute("externalLoginKey")).thenReturn("abcd");

        String externalLoginKey = ExternalLoginKeysManager.getExternalLoginKey(request);

        assertEquals("abcd", externalLoginKey);
    }

    @Test
    public void getExternalLoginKeyReturnsEmptyKeyIfUserLoginIsNull() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpSession session = mock(HttpSession.class);
        when(request.getSession()).thenReturn(session);

        String externalLoginKey = ExternalLoginKeysManager.getExternalLoginKey(request);

        assertEquals("", externalLoginKey);
    }

    @Test
    public void getExternalLoginKeyReturnsKeyFromSessionForAjaxRequests() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Requested-With")).thenReturn("XMLHttpRequest");
        HttpSession session = mock(HttpSession.class);
        when(session.getAttribute("externalLoginKey")).thenReturn("abcd");
        when(request.getSession()).thenReturn(session);

        String externalLoginKey = ExternalLoginKeysManager.getExternalLoginKey(request);

        assertEquals("abcd", externalLoginKey);
    }

    @Test
    public void getExternalLoginKeyGeneratesNewKey() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        GenericValue userLogin = new GenericValue();
        when(request.getAttribute("userLogin")).thenReturn(userLogin);
        HttpSession session = mock(HttpSession.class);
        when(request.getSession()).thenReturn(session);

        String externalLoginKey = ExternalLoginKeysManager.getExternalLoginKey(request);

        assertTrue(externalLoginKey.startsWith("EL"));
        verify(request).setAttribute("externalLoginKey", externalLoginKey);
        verify(session).setAttribute("externalLoginKey", externalLoginKey);
    }

    @Test
    public void checkExternalLoginKeyIgnoresUnknownKeyWithoutLoggingAnyoneIn() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(request.getParameter("externalLoginKey")).thenReturn("ELunknown-key-not-in-map");

        try (MockedStatic<LoginWorker> loginWorker = mockStatic(LoginWorker.class)) {
            String result = ExternalLoginKeysManager.checkExternalLoginKey(request, response);

            assertEquals("success", result);
            loginWorker.verify(() -> LoginWorker.checkLogout(any(), any()), never());
            loginWorker.verify(() -> LoginWorker.doBasicLogin(any(), any(), any()), never());
            loginWorker.verify(() -> LoginWorker.autoLoginSet(request, response));
        }
    }

    @Test
    public void checkExternalLoginKeyConsumesTheKeySoItCannotBeReplayed() {
        // Mint a key the way ScreenRenderer/RequestHandler do.
        Delegator delegator = mock(Delegator.class);
        when(delegator.getDelegatorName()).thenReturn("default");
        GenericValue userLogin = mock(GenericValue.class);
        when(userLogin.getDelegator()).thenReturn(delegator);
        when(userLogin.getString("userLoginId")).thenReturn("demoadmin");

        HttpServletRequest mintRequest = mock(HttpServletRequest.class);
        HttpSession mintSession = mock(HttpSession.class);
        when(mintRequest.getSession()).thenReturn(mintSession);
        when(mintRequest.getAttribute("userLogin")).thenReturn(userLogin);
        String key = ExternalLoginKeysManager.getExternalLoginKey(mintRequest);

        // Both redemptions target the same webapp, so the second one is the actual replay case.
        ServletContext servletContext = mock(ServletContext.class);
        when(servletContext.getContextPath()).thenReturn("/partymgr");

        try (MockedStatic<LoginWorker> loginWorker = mockStatic(LoginWorker.class)) {
            loginWorker.when(() -> LoginWorker.checkLogout(any(), any())).thenReturn(userLogin);

            // First redemption: a cookie-less client presents the freshly minted key.
            HttpServletRequest firstUse = mock(HttpServletRequest.class);
            HttpServletResponse firstResponse = mock(HttpServletResponse.class);
            HttpSession firstSession = mock(HttpSession.class);
            when(firstUse.getParameter("externalLoginKey")).thenReturn(key);
            when(firstUse.getAttribute("delegator")).thenReturn(delegator);
            when(firstUse.getSession()).thenReturn(firstSession);
            when(firstUse.getServletContext()).thenReturn(servletContext);

            String firstResult = ExternalLoginKeysManager.checkExternalLoginKey(firstUse, firstResponse);

            assertEquals("success", firstResult);
            loginWorker.verify(() -> LoginWorker.doBasicLogin(userLogin, firstUse, firstResponse), times(1));

            // Replay: a second client presents the very same key value, against the same webapp.
            HttpServletRequest replay = mock(HttpServletRequest.class);
            HttpServletResponse replayResponse = mock(HttpServletResponse.class);
            HttpSession replaySession = mock(HttpSession.class);
            when(replay.getParameter("externalLoginKey")).thenReturn(key);
            when(replay.getAttribute("delegator")).thenReturn(delegator);
            when(replay.getSession()).thenReturn(replaySession);
            when(replay.getServletContext()).thenReturn(servletContext);

            String replayResult = ExternalLoginKeysManager.checkExternalLoginKey(replay, replayResponse);

            assertEquals("success", replayResult);
            // doBasicLogin was called exactly once overall: never for the replay.
            loginWorker.verify(() -> LoginWorker.doBasicLogin(any(), any(), any()), times(1));
            loginWorker.verify(() -> LoginWorker.autoLoginSet(replay, replayResponse));
        }
    }
}
