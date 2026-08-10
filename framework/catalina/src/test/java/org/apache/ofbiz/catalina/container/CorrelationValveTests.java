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
package org.apache.ofbiz.catalina.container;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpSession;

import org.apache.catalina.Valve;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.logging.log4j.ThreadContext;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.entity.GenericValue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public final class CorrelationValveTests {

    private static final String MODE_PROP = "log.correlation.userLoginId.mode";
    private static final String PEPPER_PROP = "log.correlation.userLoginId.hash.pepper";
    private static final String KEY_LENGTH_PROP = "log.correlation.userLoginId.hash.keyLengthBits";

    private Request request;
    private Response response;
    private HttpSession session;
    private GenericValue userLogin;
    private Valve next;

    @BeforeEach
    public void setUp() {
        request = mock(Request.class);
        response = mock(Response.class);
        session = mock(HttpSession.class);
        when(session.getAttribute(anyString())).thenReturn(null);
        // VisitHandler.getVisit() falls through to creating a new Visit entity (needs a real
        // Delegator) unless session already has a "visit" attribute - stub one so tests only
        // exercise the userLoginId path, which is what they're actually about.
        GenericValue visit = mock(GenericValue.class);
        when(visit.getString("visitId")).thenReturn("10000");
        when(session.getAttribute("visit")).thenReturn(visit);
        when(request.getSession(false)).thenReturn(session);
        userLogin = mock(GenericValue.class);
        next = mock(Valve.class);
        resetProperties();
    }

    @AfterEach
    public void tearDown() {
        resetProperties();
    }

    private void resetProperties() {
        UtilProperties.setPropertyValueInMemory("debug", MODE_PROP, "plain");
        UtilProperties.setPropertyValueInMemory("debug", PEPPER_PROP, "");
        UtilProperties.setPropertyValueInMemory("debug", KEY_LENGTH_PROP, "128");
    }

    private CorrelationValve newValve() {
        CorrelationValve valve = new CorrelationValve();
        valve.setNext(next);
        return valve;
    }

    private String captureUserLoginIdSeenByNext() throws Exception {
        String[] captured = new String[1];
        doAnswer(invocation -> {
            captured[0] = ThreadContext.get("userLoginId");
            return null;
        }).when(next).invoke(request, response);
        newValve().invoke(request, response);
        return captured[0];
    }

    @Test
    public void plainModeWritesRawUserLoginId() throws Exception {
        when(session.getAttribute("userLogin")).thenReturn(userLogin);
        when(userLogin.getString("userLoginId")).thenReturn("jdoe@example.com");

        assertEquals("jdoe@example.com", captureUserLoginIdSeenByNext());
        assertNull(ThreadContext.get("userLoginId"), "context must be cleared after the request");
    }

    @Test
    public void hashModeWritesDeterministicDigestNotRawValue() throws Exception {
        UtilProperties.setPropertyValueInMemory("debug", MODE_PROP, "hash");
        UtilProperties.setPropertyValueInMemory("debug", PEPPER_PROP, "test-pepper");
        when(session.getAttribute("userLogin")).thenReturn(userLogin);
        when(userLogin.getString("userLoginId")).thenReturn("jdoe@example.com");

        String firstDigest = captureUserLoginIdSeenByNext();
        String secondDigest = captureUserLoginIdSeenByNext();

        assertNotEquals("jdoe@example.com", firstDigest);
        assertEquals(firstDigest, secondDigest, "same user must hash the same way across requests");
        assertTrue(firstDigest.startsWith("{PBKDF2"), "digest should be self-describing, was: " + firstDigest);
    }

    @Test
    public void hashModeDefaultKeyLengthProducesThirtyNineCharDigest() throws Exception {
        UtilProperties.setPropertyValueInMemory("debug", MODE_PROP, "hash");
        UtilProperties.setPropertyValueInMemory("debug", PEPPER_PROP, "test-pepper");
        when(session.getAttribute("userLogin")).thenReturn(userLogin);
        when(userLogin.getString("userLoginId")).thenReturn("jdoe@example.com");

        String digest = captureUserLoginIdSeenByNext();

        // "{PBKDF2-SHA256}" (15 chars) + base64(16-byte/128-bit key, padded) (24 chars) = 39.
        assertEquals(39, digest.length(), "digest was: " + digest);
    }

    @Test
    public void hashModeProducesDifferentDigestsForDifferentUsers() throws Exception {
        UtilProperties.setPropertyValueInMemory("debug", MODE_PROP, "hash");
        UtilProperties.setPropertyValueInMemory("debug", PEPPER_PROP, "test-pepper");

        when(session.getAttribute("userLogin")).thenReturn(userLogin);
        when(userLogin.getString("userLoginId")).thenReturn("jdoe@example.com");
        String firstUserDigest = captureUserLoginIdSeenByNext();

        GenericValue otherUserLogin = mock(GenericValue.class);
        when(otherUserLogin.getString("userLoginId")).thenReturn("asmith@example.com");
        when(session.getAttribute("userLogin")).thenReturn(otherUserLogin);
        String secondUserDigest = captureUserLoginIdSeenByNext();

        assertNotEquals(firstUserDigest, secondUserDigest);
    }

    @Test
    public void hashModeWithoutPepperFailsFastAtConstruction() {
        UtilProperties.setPropertyValueInMemory("debug", MODE_PROP, "hash");

        assertThrows(IllegalStateException.class, CorrelationValve::new);
    }

    @Test
    public void hashModeWithZeroKeyLengthFailsFastAtConstruction() {
        UtilProperties.setPropertyValueInMemory("debug", MODE_PROP, "hash");
        UtilProperties.setPropertyValueInMemory("debug", PEPPER_PROP, "test-pepper");
        UtilProperties.setPropertyValueInMemory("debug", KEY_LENGTH_PROP, "0");

        assertThrows(IllegalStateException.class, CorrelationValve::new);
    }

    @Test
    public void hashModeWithNegativeKeyLengthFailsFastAtConstruction() {
        UtilProperties.setPropertyValueInMemory("debug", MODE_PROP, "hash");
        UtilProperties.setPropertyValueInMemory("debug", PEPPER_PROP, "test-pepper");
        UtilProperties.setPropertyValueInMemory("debug", KEY_LENGTH_PROP, "-128");

        assertThrows(IllegalStateException.class, CorrelationValve::new);
    }

    @Test
    public void offModeOmitsUserLoginIdEntirely() throws Exception {
        UtilProperties.setPropertyValueInMemory("debug", MODE_PROP, "off");
        when(session.getAttribute("userLogin")).thenReturn(userLogin);
        when(userLogin.getString("userLoginId")).thenReturn("jdoe@example.com");

        boolean[] hadKey = new boolean[1];
        doAnswer(invocation -> {
            hadKey[0] = ThreadContext.containsKey("userLoginId");
            return null;
        }).when(next).invoke(request, response);
        newValve().invoke(request, response);

        assertFalse(hadKey[0]);
    }

    @Test
    public void invalidModeFallsBackToPlain() throws Exception {
        UtilProperties.setPropertyValueInMemory("debug", MODE_PROP, "bogus");
        when(session.getAttribute("userLogin")).thenReturn(userLogin);
        when(userLogin.getString("userLoginId")).thenReturn("jdoe@example.com");

        assertEquals("jdoe@example.com", captureUserLoginIdSeenByNext());
    }
}
