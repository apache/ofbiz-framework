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

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Locale;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.ofbiz.base.util.GeneralRuntimeException;

/**
 * Native, zero-external-dependency RFC 6238 (TOTP) and RFC 4226 (HOTP) implementation.
 * Uses standard javax.crypto.Mac and RFC 4648 Base32 encoding.
 */
public final class TotpUtil {

    public static final String HMAC_SHA1 = "HmacSHA1";
    public static final String HMAC_SHA256 = "HmacSHA256";
    public static final String HMAC_SHA512 = "HmacSHA512";
    public static final int DEFAULT_TIME_STEP_SECONDS = 30;
    public static final int DEFAULT_DIGITS = 6;
    private static final String BASE32_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private TotpUtil() { }

    /**
     * Generate a cryptographically secure random 160-bit (20-byte) secret key,
     * encoded as a 32-character uppercase Base32 string (RFC 4648).
     * @return 32-character Base32 secret string.
     */
    public static String generateSecret() {
        byte[] buffer = new byte[20];
        SECURE_RANDOM.nextBytes(buffer);
        return encodeBase32(buffer);
    }

    /**
     * Compute a 6-digit TOTP code using HMAC-SHA1 and the default 30-second window.
     * @param base32Secret the Base32-encoded shared secret
     * @param timeStep the counter value (usually epochSeconds / 30)
     * @return 6-digit formatted TOTP string
     */
    public static String generateTotp(String base32Secret, long timeStep) {
        return generateTotp(decodeBase32(base32Secret), timeStep, DEFAULT_DIGITS, HMAC_SHA1);
    }

    /**
     * Compute a TOTP code conforming to RFC 6238 and RFC 4226.
     * @param keyBytes the raw secret key bytes
     * @param timeStep the counter step (T)
     * @param digits number of digits (typically 6 or 8)
     * @param hmacAlgorithm HMAC algorithm name (e.g. HmacSHA1, HmacSHA256, HmacSHA512)
     * @return zero-padded OTP string
     */
    public static String generateTotp(byte[] keyBytes, long timeStep, int digits, String hmacAlgorithm) {
        if (keyBytes == null || keyBytes.length == 0) {
            throw new IllegalArgumentException("Key bytes must not be null or empty");
        }
        try {
            byte[] data = ByteBuffer.allocate(8).putLong(timeStep).array();
            Mac mac = Mac.getInstance(hmacAlgorithm);
            mac.init(new SecretKeySpec(keyBytes, hmacAlgorithm));
            byte[] hash = mac.doFinal(data);

            // Dynamic Truncation (RFC 4226 Section 5.3)
            int offset = hash[hash.length - 1] & 0x0F;
            int binary = ((hash[offset] & 0x7F) << 24)
                    | ((hash[offset + 1] & 0xFF) << 16)
                    | ((hash[offset + 2] & 0xFF) << 8)
                    | (hash[offset + 3] & 0xFF);

            int modulus = (int) Math.pow(10, digits);
            int otp = binary % modulus;
            return String.format(Locale.ROOT, "%0" + digits + "d", otp);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new GeneralRuntimeException("Error calculating TOTP code", e);
        }
    }

    /**
     * Verify a submitted code against a Base32 secret within a +/- window tolerance.
     * @param base32Secret shared secret
     * @param submittedCode the code entered by the user
     * @param windowSteps number of steps before and after current step to accept (e.g. 1)
     * @return the accepted step if valid, or -1 if invalid
     */
    public static long findMatchingTotpStep(String base32Secret, String submittedCode, int windowSteps) {
        if (base32Secret == null || submittedCode == null || submittedCode.length() != DEFAULT_DIGITS) {
            return -1L;
        }
        long currentStep = System.currentTimeMillis() / 1000L / DEFAULT_TIME_STEP_SECONDS;
        byte[] keyBytes = decodeBase32(base32Secret);
        if (keyBytes.length == 0) {
            return -1L;
        }
        byte[] submittedBytes = submittedCode.getBytes(StandardCharsets.UTF_8);
        for (long step = currentStep - windowSteps; step <= currentStep + windowSteps; step++) {
            String candidate = generateTotp(keyBytes, step, DEFAULT_DIGITS, HMAC_SHA1);
            if (MessageDigest.isEqual(submittedBytes, candidate.getBytes(StandardCharsets.UTF_8))) {
                return step;
            }
        }
        return -1L;
    }

    /**
     * Encode byte array to RFC 4648 Base32 string (without padding).
     * @param data binary input data
     * @return Base32 encoded string
     */
    public static String encodeBase32(byte[] data) {
        if (data == null || data.length == 0) {
            return "";
        }
        char[] chars = BASE32_ALPHABET.toCharArray();
        StringBuilder sb = new StringBuilder((data.length * 8 + 4) / 5);
        int buffer = 0;
        int bitsLeft = 0;
        for (byte b : data) {
            buffer = (buffer << 8) | (b & 0xFF);
            bitsLeft += 8;
            while (bitsLeft >= 5) {
                bitsLeft -= 5;
                sb.append(chars[(buffer >> bitsLeft) & 0x1F]);
            }
        }
        if (bitsLeft > 0) {
            buffer <<= (5 - bitsLeft);
            sb.append(chars[buffer & 0x1F]);
        }
        return sb.toString();
    }

    /**
     * Decode RFC 4648 Base32 string to raw byte array.
     * @param base32 Base32 formatted string (case-insensitive, ignores hyphens/spaces/padding)
     * @return decoded byte array
     */
    public static byte[] decodeBase32(String base32) {
        if (base32 == null) {
            return new byte[0];
        }
        String clean = base32.trim().replace("-", "").replace(" ", "").replace("=", "").toUpperCase(Locale.ROOT);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int buffer = 0;
        int bitsLeft = 0;
        for (int i = 0; i < clean.length(); i++) {
            char c = clean.charAt(i);
            int val;
            if (c >= 'A' && c <= 'Z') {
                val = c - 'A';
            } else if (c >= '2' && c <= '7') {
                val = c - '2' + 26;
            } else {
                continue;
            }
            buffer = (buffer << 5) | val;
            bitsLeft += 5;
            if (bitsLeft >= 8) {
                bitsLeft -= 8;
                out.write((buffer >> bitsLeft) & 0xFF);
            }
        }
        return out.toByteArray();
    }
}
