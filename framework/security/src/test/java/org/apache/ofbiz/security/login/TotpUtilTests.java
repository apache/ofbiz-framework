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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

/**
 * Unit tests verifying native TotpUtil against official RFC 6238 Appendix B test vectors.
 */
public class TotpUtilTests {

    // Official RFC 6238 Appendix B test secret: ASCII string "12345678901234567890" (20 bytes)
    private static final byte[] RFC_SECRET_BYTES = "12345678901234567890".getBytes(StandardCharsets.US_ASCII);

    @Test
    public void testRfc6238AppendixBTestVectors() {
        // Table 1 from RFC 6238 Appendix B for HMAC-SHA-1
        long[] steps = {1L, 37037036L, 37037037L, 41152263L, 66666666L, 666666666L};
        String[] expected8Digits = {"94287082", "07081804", "14050471", "89005924", "69279037", "65353130"};
        String[] expected6Digits = {"287082", "081804", "050471", "005924", "279037", "353130"};

        for (int i = 0; i < steps.length; i++) {
            String code8 = TotpUtil.generateTotp(RFC_SECRET_BYTES, steps[i], 8, TotpUtil.HMAC_SHA1);
            assertEquals(expected8Digits[i], code8, "RFC 6238 8-digit vector mismatch for step " + steps[i]);

            String code6 = TotpUtil.generateTotp(RFC_SECRET_BYTES, steps[i], 6, TotpUtil.HMAC_SHA1);
            assertEquals(expected6Digits[i], code6, "RFC 6238 6-digit vector mismatch for step " + steps[i]);
        }
    }

    @Test
    public void testBase32EncodingAndDecoding() {
        // "12345678901234567890" Base32 encoded should be "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ"
        String base32Secret = TotpUtil.encodeBase32(RFC_SECRET_BYTES);
        assertEquals("GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ", base32Secret);

        byte[] decoded = TotpUtil.decodeBase32(base32Secret);
        assertArrayEquals(RFC_SECRET_BYTES, decoded);

        // Test case-insensitivity, hyphens, and whitespace tolerance
        String formatted = "gez-dgnb-vgy3-tqoj-qgez-dgnb-vgy3-tqoj-q";
        byte[] decodedFormatted = TotpUtil.decodeBase32(formatted);
        assertArrayEquals(RFC_SECRET_BYTES, decodedFormatted);

        // Test with Base32 secret string overload
        String code = TotpUtil.generateTotp(base32Secret, 1L);
        assertEquals("287082", code);
    }

    @Test
    public void testSecretGeneration() {
        String secret1 = TotpUtil.generateSecret();
        String secret2 = TotpUtil.generateSecret();

        assertNotNull(secret1);
        assertNotNull(secret2);
        assertEquals(32, secret1.length(), "160-bit secret in Base32 must be exactly 32 chars");
        assertEquals(32, secret2.length());
        assertTrue(!secret1.equals(secret2), "Subsequent secrets must be random");

        byte[] decoded = TotpUtil.decodeBase32(secret1);
        assertEquals(20, decoded.length, "Decoded secret must be exactly 20 bytes (160 bits)");
    }

    @Test
    public void testWindowMatching() {
        String secret = TotpUtil.generateSecret();
        long currentStep = System.currentTimeMillis() / 1000L / TotpUtil.DEFAULT_TIME_STEP_SECONDS;

        String currentCode = TotpUtil.generateTotp(secret, currentStep);
        long accepted = TotpUtil.findMatchingTotpStep(secret, currentCode, 1);
        assertEquals(currentStep, accepted, "Current step code must match");

        String prevCode = TotpUtil.generateTotp(secret, currentStep - 1);
        long acceptedPrev = TotpUtil.findMatchingTotpStep(secret, prevCode, 1);
        assertEquals(currentStep - 1, acceptedPrev, "Previous step within window must match");

        String nextCode = TotpUtil.generateTotp(secret, currentStep + 1);
        long acceptedNext = TotpUtil.findMatchingTotpStep(secret, nextCode, 1);
        assertEquals(currentStep + 1, acceptedNext, "Next step within window must match");

        // Step outside window
        String oldCode = TotpUtil.generateTotp(secret, currentStep - 5);
        long rejectedOld = TotpUtil.findMatchingTotpStep(secret, oldCode, 1);
        assertEquals(-1L, rejectedOld, "Step outside window must be rejected");

        // Completely invalid code
        long invalid = TotpUtil.findMatchingTotpStep(secret, "999999", 1);
        assertEquals(-1L, invalid);
    }

}
