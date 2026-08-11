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

import java.util.Base64;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.HttpClient;
import org.apache.ofbiz.base.util.HttpClientException;
import org.apache.ofbiz.base.util.SSLUtil;
import org.apache.ofbiz.testtools.JunitJupiterTest;
import org.apache.ofbiz.testtools.JupiterTestHelper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
@JunitJupiterTest
class RestTestHttpRequest implements JupiterTestHelper {

    private static final String MODULE = RestTestHttpRequest.class.getName();
    private static final String BASE_URL = "https://localhost:8443/rest";
    private static String accessToken;
    private static ObjectMapper mapper = new ObjectMapper();

    private static HttpClient initHttpClient() {
        HttpClient http = new HttpClient();
        http.followRedirects(true);
        http.setAllowUntrusted(true);
        http.setHostVerificationLevel(SSLUtil.getHostCertNoCheck());
        return http;
    }

    @BeforeAll
    public static void init() {
        String creds = Base64.getEncoder().encodeToString(("admin:ofbiz").getBytes());

        HttpClient fetchClient = initHttpClient();
        fetchClient.setUrl(BASE_URL + "/auth/token");
        fetchClient.setHeader("Authorization", "Basic " + creds);
        fetchClient.setHeader("Accept", "application/json");
        String response = "";
        try {
            response = fetchClient.post();
        } catch (HttpClientException e) {
            Debug.logError(e, "Error returning rest access token", MODULE);
            return;
        }

        try {
            JsonNode root = mapper.readTree(response);
            accessToken = root.get("data").get("access_token").asText();
        } catch (JsonProcessingException | NullPointerException e) {
            Debug.logError(e, "Error parsing rest auth response", MODULE);
        }
    }

    @Test
    void returnSuccessreturnsExpectedStatusCode() throws Exception {
        HttpClient client = initHttpClient();
        client.setHeader("Content-Type", "application/json");
        client.setHeader("Authorization", "Bearer " + accessToken);
        client.setUrl(BASE_URL + "/exampleApi/returnSuccess");

        String response = "";
        try {
            response = client.post();
        } catch (HttpClientException e) {
            Debug.logError(e, "Error during rest POST to /exampleApi/returnSuccess", MODULE);
        }
        int statusCode = 0;
        try {
            JsonNode root = mapper.readTree(response);
            statusCode = root.get("statusCode").asInt(999999999);
        } catch (JsonProcessingException | NullPointerException e) {
            Debug.logError(e, "Error parsing rest auth response", MODULE);
        }

        assertEquals(200, statusCode);
    }

    //Corresponding service overwrites statusCode with 201
    @Test
    void returnSuccessOverwriteStatusCode() throws Exception {
        HttpClient client = initHttpClient();
        client.setHeader("Content-Type", "application/json");
        client.setHeader("Authorization", "Bearer " + accessToken);
        client.setUrl(BASE_URL + "/exampleApi/returnSuccessButOverwriteStatusCode");

        String response = "";
        try {
            response = client.post();
        } catch (HttpClientException e) {
            Debug.logError(e, "Error during rest POST to /exampleApi/returnSuccessButOverwriteStatusCode", MODULE);
        }
        int statusCode = 0;
        try {
            JsonNode root = mapper.readTree(response);
            statusCode = root.get("statusCode").asInt(999999999);
        } catch (JsonProcessingException | NullPointerException e) {
            Debug.logError(e, "Error parsing rest auth response", MODULE);
        }

        assertEquals(201, statusCode);
    }

    @Test
    void useCustomHeaderAsServiceParameter() throws Exception {
        HttpClient client = initHttpClient();
        client.setHeader("Content-Type", "application/json");
        client.setHeader("Authorization", "Bearer " + accessToken);
        client.setHeader("x-custom-header", "Foo");
        client.setUrl(BASE_URL + "/exampleApi/useCustomHeaderAsServiceParameter");

        String response = "";
        try {
            response = client.post();
        } catch (HttpClientException e) {
            Debug.logError(e, "Error during rest POST to /exampleApi/useCustomHeaderAsServiceParameter", MODULE);
        }
        String customHeaderValue = "";
        try {
            JsonNode root = mapper.readTree(response);
            customHeaderValue = root.get("data").get("x-custom-header").asText();
        } catch (JsonProcessingException | NullPointerException e) {
            Debug.logError(e, "Error parsing rest auth response", MODULE);
        }

        assertEquals("Foo", customHeaderValue);
    }
}
