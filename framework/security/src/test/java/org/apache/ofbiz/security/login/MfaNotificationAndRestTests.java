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
package org.apache.ofbiz.security.login;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

public class MfaNotificationAndRestTests {

    @Test
    public void testSendSecurityAlertNotificationHandlesNullSafely() {
        // Must never throw exceptions on null or missing delegator/user
        assertDoesNotThrow(() -> {
            MfaServices.sendSecurityAlertNotification(null, null, null, "MFA_ENROLL", "ULAF_TOTP", null);
        });

        assertDoesNotThrow(() -> {
            MfaServices.sendSecurityAlertNotification(null, "nonexistentUser", "admin", "MFA_FACTOR_REVOKED", "ULAF_TOTP", "Lost device");
        });
    }

    @Test
    public void testSendSecurityAlertIgnoresUnrelatedEvents() {
        assertDoesNotThrow(() -> {
            // Password-only login committed without MFA factor should be ignored
            MfaServices.sendSecurityAlertNotification(null, "testUser", "testUser", "LOGIN_COMMITTED", null, null);
            // Non-security lifecycle events should be ignored
            MfaServices.sendSecurityAlertNotification(null, "testUser", "testUser", "PRIMARY_AUTH_SUCCESS", null, null);
        });
    }

    @Test
    public void testRestTokensWithMfaAmrSatisfyAssurance() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userLoginId", "admin");
        claims.put("apiGroupPath", "api");
        claims.put("amr", List.of("pwd", "totp"));

        AuthenticationContext authContext = new AuthenticationContext(AuthenticationContext.METHOD_JWT, claims);
        assertTrue(authContext.isMfaSatisfied(null),
                "Tokens issued after successful Step 2 REST verification must satisfy MFA assurance");
    }

    @Test
    public void testRestTokensWithoutMfaAmrDoNotSatisfyAssurance() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userLoginId", "admin");
        claims.put("apiGroupPath", "api");
        claims.put("amr", List.of("pwd"));

        AuthenticationContext authContext = new AuthenticationContext(AuthenticationContext.METHOD_JWT, claims);
        assertFalse(authContext.isMfaSatisfied(null),
                "Tokens issued after password-only Step 1 must NOT satisfy MFA assurance");
    }
}
