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

package org.apache.ofbiz.ws.rs.test;

import java.util.Map;

import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.service.ServiceUtil;
import org.apache.ofbiz.service.testtools.OFBizTestCase;

public class RestServicesTests extends OFBizTestCase {

    public RestServicesTests(String name) {
        super(name);
    }

    /**
     * Verifies that the {@code generateAuthTokenService} returns a success response
     * when called with a valid {@code userLogin} in the service context.
     *
     * <p>This is the happy-path test — it confirms the service completes without
     * error and that {@link ServiceUtil#isSuccess(Map)} returns {@code true}.</p>
     *
     * @throws Exception if the service call or entity lookup fails unexpectedly
     */
    public void testGenerateAuthTokenReturnsSuccess() throws Exception {
        GenericValue userLogin = getDelegator().findOne("UserLogin", UtilMisc.toMap("userLoginId", "admin"), false);
        assertNotNull("admin userLogin must exist in demo data", userLogin);

        Map<String, Object> ctx = UtilMisc.toMap("userLogin", (Object) userLogin);
        Map<String, Object> result = getDispatcher().runSync("generateAuthTokenService", ctx);

        assertTrue("Service should return success", ServiceUtil.isSuccess(result));
    }

    /**
     * Verifies that the {@code access_token} output attribute is present and non-empty
     * in the service response.
     *
     * <p>A null or empty token would indicate that {@code JWTManager.createJwt()}
     * failed silently or returned an unexpected value.</p>
     *
     * @throws Exception if the service call or entity lookup fails unexpectedly
     */
    public void testGenerateAuthTokenAccessTokenPresent() throws Exception {
        GenericValue userLogin = getDelegator().findOne("UserLogin", UtilMisc.toMap("userLoginId", "admin"), false);

        Map<String, Object> result = getDispatcher().runSync(
                "generateAuthTokenService",
                UtilMisc.toMap("userLogin", (Object) userLogin));

        String accessToken = (String) result.get("access_token");
        assertNotNull("access_token should not be null", accessToken);
        assertFalse("access_token should not be empty", accessToken.isEmpty());
    }

    /**
     * Verifies that the {@code token_type} output attribute is exactly {@code "Bearer"}.
     *
     * <p>REST clients rely on this value to correctly construct the
     * {@code Authorization: Bearer <token>} header. Any deviation would break
     * standard OAuth2 / JWT bearer token flows.</p>
     *
     * @throws Exception if the service call or entity lookup fails unexpectedly
     */
    public void testGenerateAuthTokenTokenTypeIsBearer() throws Exception {
        GenericValue userLogin = getDelegator().findOne("UserLogin", UtilMisc.toMap("userLoginId", "admin"), false);

        Map<String, Object> result = getDispatcher().runSync(
                "generateAuthTokenService",
                UtilMisc.toMap("userLogin", (Object) userLogin));

        assertEquals("token_type should be Bearer", "Bearer", result.get("token_type"));
    }

    /**
     * Verifies that the {@code expires_in} output attribute is present and represents
     * a valid positive integer (in seconds).
     *
     * <p>The value is read from the {@code security.jwt.token.expireTime} property
     * in {@code security.properties}. This test guards against a missing property
     * file entry or a non-numeric value being returned.</p>
     *
     * @throws Exception if the service call or entity lookup fails unexpectedly,
     *                   or if {@code expires_in} cannot be parsed as an integer
     */
    public void testGenerateAuthTokenExpiresInIsValid() throws Exception {
        GenericValue userLogin = getDelegator().findOne("UserLogin", UtilMisc.toMap("userLoginId", "admin"), false);

        Map<String, Object> result = getDispatcher().runSync(
                "generateAuthTokenService",
                UtilMisc.toMap("userLogin", (Object) userLogin));

        String expiresIn = (String) result.get("expires_in");
        assertNotNull("expires_in should not be null", expiresIn);

        int expiry = Integer.parseInt(expiresIn);
        assertTrue("expires_in should be a positive number", expiry > 0);
    }

    /**
     * Verifies that the {@code access_token} is a well-formed JWT by checking
     * it consists of exactly three Base64-encoded parts separated by dots
     * ({@code header.payload.signature}).
     *
     * <p>This does not validate the cryptographic signature — it confirms that
     * {@code JWTManager.createJwt()} produced a structurally valid token that
     * JWT libraries and API clients will be able to parse.</p>
     *
     * @throws Exception if the service call or entity lookup fails unexpectedly
     */
    public void testGenerateAuthTokenTokenIsValidJwtFormat() throws Exception {
        GenericValue userLogin = getDelegator().findOne("UserLogin", UtilMisc.toMap("userLoginId", "admin"), false);

        Map<String, Object> result = getDispatcher().runSync(
                "generateAuthTokenService",
                UtilMisc.toMap("userLogin", (Object) userLogin));

        String accessToken = (String) result.get("access_token");
        String[] parts = accessToken.split("\\.");
        assertEquals("JWT should have 3 parts (header.payload.signature)", 3, parts.length);
    }

    /**
     * Verifies that two different users receive different tokens.
     *
     * <p>Since the {@code userLoginId} is embedded in the JWT payload, tokens
     * generated for different users must not be identical. Identical tokens would
     * indicate that the {@code userLogin} is not being read from context correctly,
     * allowing one user to authenticate as another.</p>
     *
     * @throws Exception if the service call or entity lookup fails unexpectedly
     */
    public void testGenerateAuthTokenDifferentUsersGetDifferentTokens() throws Exception {
        GenericValue adminLogin = getDelegator().findOne("UserLogin", UtilMisc.toMap("userLoginId", "admin"), false);
        GenericValue systemLogin = getDelegator().findOne("UserLogin", UtilMisc.toMap("userLoginId", "system"), false);

        assertNotNull("admin userLogin must exist", adminLogin);
        assertNotNull("system userLogin must exist", systemLogin);

        Map<String, Object> adminResult = getDispatcher().runSync(
                "generateAuthTokenService",
                UtilMisc.toMap("userLogin", (Object) adminLogin));

        Map<String, Object> systemResult = getDispatcher().runSync(
                "generateAuthTokenService",
                UtilMisc.toMap("userLogin", (Object) systemLogin));

        assertFalse(
                "Different users should receive different tokens",
                adminResult.get("access_token").equals(systemResult.get("access_token")));
    }
}
