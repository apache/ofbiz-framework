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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ofbiz.security.login.AuthenticationContext;
import org.junit.jupiter.api.Test;

public class JWTManagerAssuranceTests {

    @Test
    public void testTokenWithoutAmrProvidesNoMfaAssurance() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userLoginId", "admin");

        String token = JWTManager.createJwt(null, claims);
        assertNotNull(token);

        Map<String, Object> extractedClaims = JWTManager.validateAccessToken(null, token);
        assertNotNull(extractedClaims);
        assertEquals("admin", extractedClaims.get("userLoginId"));

        AuthenticationContext authCtx = new AuthenticationContext(AuthenticationContext.METHOD_JWT, extractedClaims);
        assertFalse(authCtx.isMfaSatisfied(null),
                "Valid JWT without AMR must NOT satisfy MFA assurance (no implicit trust from validity alone)");
    }

    @Test
    public void testTokenWithPasswordOnlyAmrProvidesNoMfaAssurance() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userLoginId", "admin");
        claims.put("amr", Collections.singletonList("pwd"));

        String token = JWTManager.createJwt(null, claims);
        assertNotNull(token);

        Map<String, Object> extractedClaims = JWTManager.validateAccessToken(null, token);
        assertNotNull(extractedClaims);

        AuthenticationContext authCtx = new AuthenticationContext(AuthenticationContext.METHOD_JWT, extractedClaims);
        assertFalse(authCtx.isMfaSatisfied(null),
                "Valid JWT with AMR=['pwd'] must NOT satisfy MFA assurance");
    }

    @Test
    public void testTokenWithMfaAmrSatisfiesMfaAssurance() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userLoginId", "admin");
        claims.put("amr", Arrays.asList("pwd", "mfa"));

        String token = JWTManager.createJwt(null, claims);
        assertNotNull(token);

        Map<String, Object> extractedClaims = JWTManager.validateAccessToken(null, token);
        assertNotNull(extractedClaims);
        assertTrue(extractedClaims.get("amr") instanceof List, "AMR claim must be extracted as List");

        AuthenticationContext authCtx = new AuthenticationContext(AuthenticationContext.METHOD_JWT, extractedClaims);
        assertTrue(authCtx.isMfaSatisfied(null),
                "Valid JWT with AMR=['pwd', 'mfa'] must satisfy MFA assurance");
    }

    @Test
    public void testTokenWithTotpAmrSatisfiesMfaAssurance() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userLoginId", "admin");
        claims.put("amr", Arrays.asList("pwd", "totp"));

        String token = JWTManager.createJwt(null, claims);
        assertNotNull(token);

        Map<String, Object> extractedClaims = JWTManager.validateAccessToken(null, token);
        assertNotNull(extractedClaims);

        AuthenticationContext authCtx = new AuthenticationContext(AuthenticationContext.METHOD_JWT, extractedClaims);
        assertTrue(authCtx.isMfaSatisfied(null),
                "Valid JWT with AMR=['pwd', 'totp'] must satisfy MFA assurance");
    }
}
