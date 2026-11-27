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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.math.BigInteger;
import java.security.cert.X509Certificate;

import javax.security.auth.x500.X500Principal;

import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.EntityUtilProperties;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * Verifies that {@link LoginWorker#check509CertLogin} falls through to {@code "success"} for an
 * unknown or disabled certificate CN, instead of failing the request with {@code "error"}, matching
 * the fall-through behavior of {@link LoginWorker#checkRequestHeaderLogin} and
 * {@link LoginWorker#checkServletRequestRemoteUserLogin}.
 */
public class Check509CertLoginMfaRegressionTests {

    @Test
    public void testUnknownCertUserLoginIdFallsThroughToSuccess() throws Exception {
        Delegator delegator = mock(Delegator.class);
        when(delegator.getDelegator()).thenReturn(delegator);

        X509Certificate cert = mock(X509Certificate.class);
        when(cert.getSubjectX500Principal()).thenReturn(new X500Principal("CN=unknown-cert-user"));
        when(cert.getSerialNumber()).thenReturn(BigInteger.ONE);

        HttpSession session = mock(HttpSession.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute("delegator")).thenReturn(delegator);
        when(request.getAttribute("jakarta.servlet.request.X509Certificate")).thenReturn(new X509Certificate[] {cert});
        when(request.getSession()).thenReturn(session);
        HttpServletResponse response = mock(HttpServletResponse.class);

        EntityQuery mockQuery = mock(EntityQuery.class);
        when(mockQuery.from(anyString())).thenReturn(mockQuery);
        when(mockQuery.where(anyString(), any())).thenReturn(mockQuery);
        when(mockQuery.where(any(EntityCondition.class))).thenReturn(mockQuery);
        // X509IssuerProvision lookup (checkValidIssuer): a valid issuer is on file.
        when(mockQuery.queryCount()).thenReturn(1L);
        // UserLogin lookup (loginUserWithUserLoginId): no account matches this certificate's CN.
        when(mockQuery.queryOne()).thenReturn(null);

        try (MockedStatic<EntityQuery> entityQuery = mockStatic(EntityQuery.class);
                MockedStatic<EntityUtilProperties> props = mockStatic(EntityUtilProperties.class)) {
            entityQuery.when(() -> EntityQuery.use(any())).thenReturn(mockQuery);
            // Any property not explicitly overridden answers with its own default value, matching an
            // unconfigured install: security.login.cert.allow defaults to "true" and
            // security.login.cert.pattern defaults to "(.*)" (matches the whole CN).
            props.when(() -> EntityUtilProperties.getPropertyValue(any(), any(), any(), any()))
                    .thenAnswer(invocation -> invocation.getArgument(2));

            String result = LoginWorker.check509CertLogin(request, response);

            assertEquals("success", result, "An unknown certificate CN must not fail the request for a "
                    + "non-MFA user -- it must fall through the same way trunk did before this lookup "
                    + "was folded into loginUserWithUserLoginId.");
        }
    }
}
