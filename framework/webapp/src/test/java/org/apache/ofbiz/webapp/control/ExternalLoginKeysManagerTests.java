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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.ofbiz.entity.GenericValue;
import org.junit.Test;

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
}
