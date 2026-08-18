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
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;
import java.util.zip.ZipException;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.HttpClient;
import org.apache.ofbiz.base.util.HttpClientException;
import org.apache.ofbiz.base.util.SSLUtil;
import org.apache.ofbiz.testtools.JunitJupiterTest;
import org.apache.ofbiz.testtools.JupiterTestHelper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.ws.rs.HttpMethod;

@JunitJupiterTest
class RestTestHttpRequest implements JupiterTestHelper {

    private static final String MODULE = RestTestHttpRequest.class.getName();
    private static final String BASE_URL = "https://localhost:8443/rest";
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static String accessToken;

    private static HttpClient initHttpClient() {
        HttpClient http = new HttpClient();
        http.followRedirects(true);
        http.setAllowUntrusted(true);
        http.setHostVerificationLevel(SSLUtil.getHostCertNoCheck());
        return http;
    }

    private static HttpClient createAuthorizedClient(String path) {
        HttpClient client = initHttpClient();
        client.setHeader("Content-Type", "application/json");
        client.setHeader("Authorization", "Bearer " + accessToken);
        client.setUrl(BASE_URL + path);
        return client;
    }

    private static JsonNode requestForJson(HttpClient client, String errorContext, String verb) {
        String response = "";
        try {
            response = switch (verb) {
            case "GET" -> client.get();
            case "POST" -> client.post();
            default -> "";
            };
        } catch (HttpClientException e) {
            Debug.logError(e, "Error during rest POST to " + errorContext, MODULE);
            fail("HTTP POST failed for " + errorContext + ": " + e.getMessage());
        }
        try {
            return MAPPER.readTree(response);
        } catch (JsonProcessingException e) {
            Debug.logError(e, "Error parsing rest response for " + errorContext, MODULE);
            fail("Error parsing rest response: " + e.getMessage());
            return null;
        }
    }

    private static byte[] postRawBytes(HttpClient client) throws Exception {
        try (InputStream responseStream = client.postStream()) {
            return responseStream.readAllBytes();
        }
    }

    private static String padBase64(String input) {
        int padding = (4 - input.length() % 4) % 4;
        StringBuilder sb = new StringBuilder(input);
        for (int i = 0; i < padding; i++) {
            sb.append('=');
        }
        return sb.toString();
    }

    private static JsonNode decodeJwtPayload(String jwt) {
        String[] parts = jwt.split("\\.");
        assertEquals(3, parts.length, "JWT should have 3 parts (header.payload.signature)");

        byte[] decodedBytes = Base64.getUrlDecoder().decode(padBase64(parts[1]));
        String payloadJson = new String(decodedBytes, StandardCharsets.UTF_8);
        try {
            return MAPPER.readTree(payloadJson);
        } catch (JsonProcessingException e) {
            fail("Error parsing JWT payload: " + e.getMessage());
            return null;
        }
    }

    private static JsonNode decompress(byte[] rawBytes, String encoding) throws Exception {
        InputStream decompressStream = "gzip".equals(encoding)
                ? new GZIPInputStream(new ByteArrayInputStream(rawBytes))
                : new InflaterInputStream(new ByteArrayInputStream(rawBytes));

        try (InputStream is = decompressStream) {
            return MAPPER.readTree(is);
        } catch (JsonProcessingException | ZipException e) {
            fail("Error parsing " + encoding + " response: " + e.getMessage());
            return null;
        }
    }

    private static JsonNode requestAuthToken(String username, String password) {
        String creds = Base64.getEncoder().encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));

        HttpClient client = initHttpClient();
        client.setUrl(BASE_URL + "/auth/token");
        client.setHeader("Authorization", "Basic " + creds);
        client.setHeader("Accept", "application/json");

        return requestForJson(client, "/auth/token (" + username + ")", HttpMethod.POST);
    }

    private static JsonNode requestAuthTokenAsAdmin() {
        return requestAuthToken("admin", "ofbiz");
    }

    @BeforeAll
    public static void init() {
        JsonNode root = requestAuthTokenAsAdmin();
        accessToken = root.path("data").path("access_token").asText();
    }

    // ---- auth token tests ----

    @Test
    @Order(1)
    void testGenerateAuthTokenReturnsSuccess() {
        JsonNode root = requestAuthTokenAsAdmin();
        assertEquals(200, root.path("statusCode").asInt(), "Endpoint should return success status code");
    }

    @Test
    @Order(2)
    void testGenerateAuthTokenAccessTokenPresent() {
        JsonNode root = requestAuthTokenAsAdmin();
        String token = root.path("data").path("access_token").asText(null);

        assertNotNull(token, "access_token should not be null");
        assertFalse(token.isEmpty(), "access_token should not be empty");
    }

    @Test
    @Order(3)
    void testGenerateAuthTokenTokenTypeIsBearer() {
        JsonNode root = requestAuthTokenAsAdmin();
        assertEquals("Bearer", root.path("data").path("token_type").asText(null), "token_type should be Bearer");
    }

    @Test
    @Order(4)
    void testGenerateAuthTokenExpiresInIsValid() {
        JsonNode root = requestAuthTokenAsAdmin();
        JsonNode expiresInNode = root.path("data").path("expires_in");

        assertFalse(expiresInNode.isMissingNode(), "expires_in should be present");

        int expiresCurrent = expiresInNode.asInt();
        assertTrue(expiresCurrent > 0, "expires_in should be a positive number");
        assertEquals(1800, expiresCurrent, "expires_in should match the configured amount");
    }

    @Test
    @Order(5)
    void testGenerateAuthTokenTokenIsValidJwtFormat() {
        JsonNode root = requestAuthTokenAsAdmin();
        String token = root.path("data").path("access_token").asText(null);

        String[] parts = token.split("\\.");
        assertEquals(3, parts.length, "JWT should have 3 parts (header.payload.signature)");
    }

    @Test
    @Order(6)
    void testGenerateAuthTokenDifferentUsersGetDifferentTokens() {
        JsonNode adminRoot = requestAuthToken("admin", "ofbiz");
        JsonNode systemRoot = requestAuthToken("REST_API_TEST_USER", "ofbiz");

        String adminToken = adminRoot.path("data").path("access_token").asText(null);
        String systemToken = systemRoot.path("data").path("access_token").asText(null);

        assertNotNull(adminToken, "admin access_token should not be null");
        assertNotNull(systemToken, "REST_API_TEST_USER access_token should not be null");
        assertFalse(adminToken.equals(systemToken), "Different users should receive different tokens");
    }

    @Test
    @Order(7)
    void testGenerateAuthTokenIssuerAndUserLoginIdInPayload() {
        JsonNode root = requestAuthTokenAsAdmin();
        String token = root.path("data").path("access_token").asText(null);

        JsonNode claims = decodeJwtPayload(token);

        assertEquals("ApacheOFBiz", claims.path("iss").asText(null), "Issuer should be ApacheOFBiz");
        assertEquals("admin", claims.path("userLoginId").asText(null), "userLoginId should be admin");
    }

    @Test
    @Order(8)
    void testGenerateAuthTokenInvalidCredentialsFail() {
        JsonNode root = requestAuthToken("admin", "wrong-password");
        int statusCode = root.path("statusCode").asInt(200);
        assertFalse(statusCode == 200, "Invalid credentials should not return a success status code");
    }

    /* ========= generall tests - these rely on the Endpoints
    specified via exampleApiDefinition.rest.xml ============ */

    @Test
    void returnSuccessReturnsExpectedStatusCode() throws Exception {
        HttpClient client = createAuthorizedClient("/exampleApi/returnSuccess");
        JsonNode root = requestForJson(client, "/exampleApi/returnSuccess", HttpMethod.POST);
        assertEquals(200, root.path("statusCode").asInt());
    }

    // Corresponding service overwrites statusCode with 201
    @Test
    void returnSuccessOverwriteStatusCode() throws Exception {
        HttpClient client = createAuthorizedClient("/exampleApi/returnSuccessButOverwriteStatusCode");
        JsonNode root = requestForJson(client, "/exampleApi/returnSuccessButOverwriteStatusCode", HttpMethod.POST);
        assertEquals(201, root.path("statusCode").asInt());
    }

    @Test
    void useCustomHeaderAsServiceParameter() throws Exception {
        HttpClient client = createAuthorizedClient("/exampleApi/useCustomHeaderAsServiceParameter");
        client.setHeader("x-custom-header", "Foo");
        JsonNode root = requestForJson(client, "/exampleApi/useCustomHeaderAsServiceParameter", HttpMethod.POST);
        assertEquals("Foo", root.path("data").path("x-custom-header").asText());
    }

    @Test
    void testUseLocaleSetInRequestHeader() throws Exception {
        HttpClient client = createAuthorizedClient("/exampleApi/useLocaleSetInRequestHeader");
        client.setHeader("Accept-Language", "fr");
        JsonNode root = requestForJson(client, "/exampleApi/useLocaleSetInRequestHeader", HttpMethod.POST);
        assertEquals("fr", root.path("data").path("localeAsString").asText());
    }

    @Test
    void testGZipCompression() throws Exception {
        HttpClient client = createAuthorizedClient("/exampleApi/returnSuccess");
        client.setHeader("Accept-Encoding", "gzip");

        byte[] rawBytes = postRawBytes(client);

        // gzip magic bytes
        assertEquals((byte) 0x1f, rawBytes[0]);
        assertEquals((byte) 0x8b, rawBytes[1]);

        JsonNode root = decompress(rawBytes, "gzip");
        assertEquals(200, root.path("statusCode").asInt());
    }

    @Test
    void testDeflateCompression() throws Exception {
        HttpClient client = createAuthorizedClient("/exampleApi/returnSuccess");
        client.setHeader("Accept-Encoding", "deflate");

        byte[] rawBytes = postRawBytes(client);

        JsonNode root = decompress(rawBytes, "deflate");
        assertEquals(200, root.path("statusCode").asInt());
    }

    @Test
    void testServiceInputParametersAsPath() throws Exception {
        HttpClient client = createAuthorizedClient("/exampleApi/foo/testServiceInputParametersAsPath/");

        JsonNode root = requestForJson(client, "/exampleApi/foo/testServiceInputParametersAsPath/", HttpMethod.GET);
        assertEquals("foo", root.path("data").path("myInput").asText());
    }

    @Test
    void testServiceInputParametersAsQueryParam() throws Exception {
        HttpClient client = createAuthorizedClient("/exampleApi/testServiceInputParametersAsQueryParam/");
        client.setParameter("myInput", "foo");

        JsonNode root = requestForJson(client, "/exampleApi/testServiceInputParametersAsQueryParam/", HttpMethod.GET);
        assertEquals("foo", root.path("data").path("myInput").asText());
    }
}
