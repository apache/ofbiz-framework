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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

public class AuthenticationContextTests {

    @Test
    public void testPasswordMethodDoesNotSatisfyMfa() {
        AuthenticationContext ctx = new AuthenticationContext(AuthenticationContext.METHOD_PASSWORD);
        assertEquals(AuthenticationContext.METHOD_PASSWORD, ctx.getAuthMethod());
        assertFalse(ctx.isMfaSatisfied(null), "Password authentication must not satisfy MFA assurance");
    }

    @Test
    public void testJwtWithoutAmrDoesNotSatisfyMfa() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userLoginId", "admin");
        claims.put("iss", "ApacheOFBiz");

        AuthenticationContext ctx = new AuthenticationContext(AuthenticationContext.METHOD_JWT, claims);
        assertFalse(ctx.isMfaSatisfied(null), "JWT without AMR claim must not satisfy MFA assurance (no implicit trust)");
    }

    @Test
    public void testJwtWithPasswordOnlyAmrDoesNotSatisfyMfa() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userLoginId", "admin");
        claims.put("amr", Collections.singletonList("pwd"));

        AuthenticationContext ctx = new AuthenticationContext(AuthenticationContext.METHOD_JWT, claims);
        assertFalse(ctx.isMfaSatisfied(null), "JWT with only 'pwd' AMR must not satisfy MFA assurance");
    }

    @Test
    public void testJwtWithMfaAmrSatisfiesMfa() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userLoginId", "admin");
        claims.put("amr", Arrays.asList("pwd", "mfa"));

        AuthenticationContext ctx = new AuthenticationContext(AuthenticationContext.METHOD_JWT, claims);
        assertTrue(ctx.isMfaSatisfied(null), "JWT with 'mfa' AMR must satisfy MFA assurance");
    }

    @Test
    public void testJwtWithTotpAmrSatisfiesMfa() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userLoginId", "admin");
        claims.put("amr", Collections.singletonList("totp"));

        AuthenticationContext ctx = new AuthenticationContext(AuthenticationContext.METHOD_JWT, claims);
        assertTrue(ctx.isMfaSatisfied(null), "JWT with 'totp' AMR must satisfy MFA assurance");
    }

    @Test
    public void testJwtWithCommaSeparatedAmrStringSatisfiesMfa() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userLoginId", "admin");
        claims.put("amr", "pwd,totp");

        AuthenticationContext ctx = new AuthenticationContext(AuthenticationContext.METHOD_JWT, claims);
        assertTrue(ctx.isMfaSatisfied(null), "JWT with comma-separated AMR string containing totp must satisfy MFA");
    }

    @Test
    public void testJwtWithFidoAmrSatisfiesMfa() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userLoginId", "admin");
        claims.put("amr", Arrays.asList("pwd", "fido"));

        AuthenticationContext ctx = new AuthenticationContext(AuthenticationContext.METHOD_JWT, claims);
        assertTrue(ctx.isMfaSatisfied(null), "JWT with 'fido' AMR must satisfy MFA assurance");
    }

    @Test
    public void testHttpHeaderDefaultDoesNotSatisfyMfa() {
        AuthenticationContext ctx = new AuthenticationContext(AuthenticationContext.METHOD_HTTP_HEADER);
        assertFalse(ctx.isMfaSatisfied(null), "HTTP header SSO by default must not satisfy MFA assurance");
    }

    @Test
    public void testExternalLoginKeyCarriesAssurance() {
        AuthenticationContext untrusted = new AuthenticationContext(AuthenticationContext.METHOD_EXTERNAL_LOGIN_KEY,
                null, true, false, Collections.emptyList(), null, null);
        assertFalse(untrusted.isMfaSatisfied(null), "Untrusted externalLoginKey must not satisfy MFA");

        AuthenticationContext trusted = new AuthenticationContext(AuthenticationContext.METHOD_EXTERNAL_LOGIN_KEY,
                null, true, true, Collections.emptyList(), null, null);
        assertTrue(trusted.isMfaSatisfied(null), "Originating MFA-verified externalLoginKey must satisfy MFA");
    }
}
