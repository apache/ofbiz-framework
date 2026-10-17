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
package org.apache.ofbiz.humanres

import static org.mockito.ArgumentMatchers.any
import static org.mockito.ArgumentMatchers.anyBoolean
import static org.mockito.ArgumentMatchers.anyString
import static org.mockito.ArgumentMatchers.eq
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.never
import static org.mockito.Mockito.verify
import static org.mockito.Mockito.when

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.servlet.http.HttpSession

import org.apache.ofbiz.entity.Delegator
import org.apache.ofbiz.security.Security
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Regression tests for the getHRChild fix: {@link HumanResEvents#getChildHRCategoryTree}
 * must deny access unless the caller holds HUMANRES_VIEW, and must not query any
 * HR/party entities before that check passes.
 */
// codenarc-disable JUnitLostTest
class HumanResEventsTest {

    private HttpServletRequest request
    private HttpServletResponse response
    private HttpSession session
    private Security security
    private Delegator delegator

    @BeforeEach
    void setupMocks() {
        request = mock(HttpServletRequest)
        response = mock(HttpServletResponse)
        session = mock(HttpSession)
        security = mock(Security)
        delegator = mock(Delegator)
        when(request.getSession()).thenReturn(session)
        when(request.getLocale()).thenReturn(Locale.US)
    }

    @Test
    void deniesAnonymousCallerWithNoSecurityAttribute() {
        when(request.getAttribute('security')).thenReturn(null)

        String result = HumanResEvents.getChildHRCategoryTree(request, response)

        assert result == 'error'
        verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN)
        verify(request, never()).setAttribute(eq('hrTree'), any())
        verify(request, never()).getAttribute('delegator')
    }

    @Test
    void deniesAuthenticatedCallerWithoutHumanresViewPermission() {
        when(request.getAttribute('security')).thenReturn(security)
        when(security.hasEntityPermission('HUMANRES', '_VIEW', session)).thenReturn(false)

        String result = HumanResEvents.getChildHRCategoryTree(request, response)

        assert result == 'error'
        verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN)
        verify(request, never()).setAttribute(eq('hrTree'), any())
        verify(request, never()).getAttribute('delegator')
    }

    @Test
    void allowsCallerWithHumanresViewPermissionPastTheGate() {
        when(request.getAttribute('security')).thenReturn(security)
        when(request.getAttribute('delegator')).thenReturn(delegator)
        when(request.getParameter('partyId')).thenReturn('Company')
        when(security.hasEntityPermission('HUMANRES', '_VIEW', session)).thenReturn(true)
        when(delegator.getDelegator()).thenReturn(delegator)
        when(delegator.findList(anyString(), any(), any(), any(), any(), any(), anyBoolean()))
                .thenReturn(Collections.emptyList())
        when(delegator.findCountByCondition(anyString(), any(), any(), any(), any())).thenReturn(0L)

        String result = HumanResEvents.getChildHRCategoryTree(request, response)

        assert result == 'success'
        verify(response, never()).setStatus(HttpServletResponse.SC_FORBIDDEN)
        verify(request).setAttribute(eq('hrTree'), any())
    }

}
