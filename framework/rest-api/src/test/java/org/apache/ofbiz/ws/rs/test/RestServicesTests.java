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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.service.ModelService;
import org.apache.ofbiz.service.ServiceUtil;
import org.apache.ofbiz.testtools.JunitJupiterTest;
import org.apache.ofbiz.testtools.JupiterTestHelper;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@JunitJupiterTest
public class RestServicesTests implements JupiterTestHelper {

    /**
     * Verifies that the {@code generateAuthTokenService} returns a success response
     * when called with a valid {@code userLogin} in the service context.
     *
     * <p>This is the happy-path test — it confirms the service completes without
     * error and that {@link ServiceUtil#isSuccess(Map)} returns {@code true}.</p>
     *
     * @throws Exception if the service call or entity lookup fails unexpectedly
     */
    @Test
    @Order(1)
    public void testGenerateAuthTokenReturnsSuccess() throws Exception {
        GenericValue userLogin = getDelegator().findOne("UserLogin", UtilMisc.toMap("userLoginId", "admin"), false);
        assertNotNull(userLogin, "admin userLogin must exist in demo data");

        Map<String, Object> ctx = UtilMisc.toMap("userLogin", (Object) userLogin);
        Map<String, Object> result = getDispatcher().runSync("generateAuthTokenService", ctx);

        assertTrue(ServiceUtil.isSuccess(result), "Service should return success");
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
    @Test
    @Order(2)
    public void testGenerateAuthTokenAccessTokenPresent() throws Exception {
        GenericValue userLogin = getDelegator().findOne("UserLogin", UtilMisc.toMap("userLoginId", "admin"), false);

        Map<String, Object> result = getDispatcher().runSync(
                "generateAuthTokenService",
                UtilMisc.toMap("userLogin", (Object) userLogin));

        String accessToken = (String) result.get("access_token");
        assertNotNull(accessToken, "access_token should not be null");
        assertFalse(accessToken.isEmpty(), "access_token should not be empty");
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
    @Test
    @Order(3)
    public void testGenerateAuthTokenTokenTypeIsBearer() throws Exception {
        GenericValue userLogin = getDelegator().findOne("UserLogin", UtilMisc.toMap("userLoginId", "admin"), false);

        Map<String, Object> result = getDispatcher().runSync(
                "generateAuthTokenService",
                UtilMisc.toMap("userLogin", (Object) userLogin));

        assertEquals("Bearer", result.get("token_type"), "token_type should be Bearer");
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
    @Test
    @Order(4)
    public void testGenerateAuthTokenExpiresInIsValid() throws Exception {
        GenericValue userLogin = getDelegator().findOne("UserLogin", UtilMisc.toMap("userLoginId", "admin"), false);

        Map<String, Object> result = getDispatcher().runSync(
                "generateAuthTokenService",
                UtilMisc.toMap("userLogin", (Object) userLogin));

        String expiresIn = (String) result.get("expires_in");
        assertNotNull(expiresIn, "expires_in should not be null");

        int expiresCurrent = Integer.parseInt(expiresIn);
        assertTrue(expiresCurrent > 0, "expires_in should be a positive number");

        int expiresTarget = 1800;
        assertTrue(expiresCurrent == expiresTarget, "expires_in should match the configured amount");
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
    @Test
    @Order(5)
    public void testGenerateAuthTokenTokenIsValidJwtFormat() throws Exception {
        GenericValue userLogin = getDelegator().findOne("UserLogin", UtilMisc.toMap("userLoginId", "admin"), false);

        Map<String, Object> result = getDispatcher().runSync(
                "generateAuthTokenService",
                UtilMisc.toMap("userLogin", (Object) userLogin));

        String accessToken = (String) result.get("access_token");
        String[] parts = accessToken.split("\\.");
        assertEquals(3, parts.length, "JWT should have 3 parts (header.payload.signature)");
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
    @Test
    @Order(6)
    public void testGenerateAuthTokenDifferentUsersGetDifferentTokens() throws Exception {
        GenericValue adminLogin = getDelegator().findOne("UserLogin", UtilMisc.toMap("userLoginId", "admin"), false);
        GenericValue systemLogin = getDelegator().findOne("UserLogin", UtilMisc.toMap("userLoginId", "system"), false);

        assertNotNull(adminLogin, "admin userLogin must exist");
        assertNotNull(systemLogin, "system userLogin must exist");

        Map<String, Object> adminResult = getDispatcher().runSync(
                "generateAuthTokenService",
                UtilMisc.toMap("userLogin", (Object) adminLogin));

        Map<String, Object> systemResult = getDispatcher().runSync(
                "generateAuthTokenService",
                UtilMisc.toMap("userLogin", (Object) systemLogin));

        assertFalse(
                adminResult.get("access_token").equals(systemResult.get("access_token")),
                "Different users should receive different tokens");
    }

    /**
     * Verifies JWT payload hast correct issuer and userLoginId.
     *
     * @throws Exception if the service call or entity lookup fails unexpectedly
     */
    @Test
    @Order(7)
    public void testGenerateAuthTokenIssuerAndUserLoginIdInPayload() throws Exception {
        GenericValue adminLogin = getDelegator().findOne("UserLogin", UtilMisc.toMap("userLoginId", "admin"), false);
        Map<String, Object> adminResult = getDispatcher().runSync(
                "generateAuthTokenService",
                UtilMisc.toMap("userLogin", (Object) adminLogin));
        String jwt = (String) adminResult.get("access_token");

        String[] parts = jwt.split("\\.");
        String payloadSegment = parts[1];
        byte[] decodedBytes = Base64.getUrlDecoder().decode(padBase64(payloadSegment));
        String jwtDecoded = new String(decodedBytes, StandardCharsets.UTF_8);

        ObjectMapper mapper = new ObjectMapper();
        JsonNode claims = mapper.readTree(jwtDecoded);

        String iss = claims.path("iss").asText(null);
        String userLoginId = claims.path("userLoginId").asText(null);

        assertTrue(iss.equals("ApacheOFBiz"), "Issuer is ApacheOFBiz");
        assertTrue(userLoginId.equals("admin"), "UserLoginId is admin");
    }

    private static String padBase64(String input) {
        int padding = (4 - input.length() % 4) % 4;
        StringBuilder sb = new StringBuilder(input);
        for (int i = 0; i < padding; i++) sb.append('=');
        return sb.toString();
    }

    @Test
    public void testReturnCustomErrorCode() throws Exception {
        GenericValue adminLogin = getDelegator().findOne("UserLogin", UtilMisc.toMap("userLoginId", "admin"), false);
        Map<String, Object> result = getDispatcher().runSync(
                "returnCustomErrorTest",
                UtilMisc.toMap("userLogin", (Object) adminLogin));
        String errorCode = (String) result.get(ModelService.ERROR_CODE);
        assertTrue(errorCode.equals("999"));
    }
}
