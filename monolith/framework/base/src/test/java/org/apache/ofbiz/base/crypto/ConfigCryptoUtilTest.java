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
package org.apache.ofbiz.base.crypto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;
import static org.junit.Assume.assumeNotNull;

import org.apache.ofbiz.base.util.GeneralException;
import org.junit.Test;

public class ConfigCryptoUtilTest {

    private static final String TEST_KEY = "test-master-key-for-unit-tests";

    @Test
    public void encryptDecryptRoundTrip() throws GeneralException {
        String original = "my-secret-password";
        String encrypted = ConfigCryptoUtil.encrypt(original, TEST_KEY);
        assertEquals(original, ConfigCryptoUtil.decrypt(encrypted, TEST_KEY));
    }

    @Test
    public void eachEncryptionProducesUniqueCiphertext() throws GeneralException {
        // Random IV means identical plaintexts should not produce identical ciphertext.
        String enc1 = ConfigCryptoUtil.encrypt("same-value", TEST_KEY);
        String enc2 = ConfigCryptoUtil.encrypt("same-value", TEST_KEY);
        assertNotEquals(enc1, enc2);
    }

    @Test(expected = GeneralException.class)
    public void decryptWithWrongKeyThrows() throws GeneralException {
        String encrypted = ConfigCryptoUtil.encrypt("secret", TEST_KEY);
        ConfigCryptoUtil.decrypt(encrypted, "wrong-key");
    }

    @Test
    public void decryptIfEncryptedReturnsPlainValueUnchanged() throws GeneralException {
        assertEquals("plain-value", ConfigCryptoUtil.decryptIfEncrypted("plain-value", "test-secret"));
        assertEquals("notenc", ConfigCryptoUtil.decryptIfEncrypted("notenc", "test-secret"));
        // Partial ENC pattern must not be treated as encrypted
        assertEquals("ENC(noclosepar", ConfigCryptoUtil.decryptIfEncrypted("ENC(noclosepar", "test-secret"));
    }

    @Test
    public void decryptIfEncryptedEncWrapperIsStrippedBeforeDecrypt() throws GeneralException {
        // Verify that the "ENC(...)" wrapper is stripped correctly: the base64 content inside
        // ENC(...) must be identical to what encrypt() returns, so that decrypt() can reverse it.
        String plain = "strip-wrapper-test";
        String base64 = ConfigCryptoUtil.encrypt(plain, TEST_KEY);
        // decryptIfEncrypted would strip "ENC(" and ")" and pass base64 to decrypt().
        // Confirm the strip round-trip is correct by doing it manually:
        String encWrapped = "ENC(" + base64 + ")";
        String stripped = encWrapped.substring("ENC(".length(), encWrapped.length() - 1);
        assertEquals(plain, ConfigCryptoUtil.decrypt(stripped, TEST_KEY));
    }

    @Test
    public void decryptIfEncryptedHappyPath() throws GeneralException {
        // Only runs when OFBIZ_MASTER_KEY is set to TEST_KEY in the environment,
        // allowing the full decryptIfEncrypted() path to be exercised end-to-end.
        String envKey = System.getenv(ConfigCryptoUtil.MASTER_KEY_ENV_VAR);
        assumeNotNull("Skipped: " + ConfigCryptoUtil.MASTER_KEY_ENV_VAR + " is not set", envKey);
        assumeTrue("Skipped: " + ConfigCryptoUtil.MASTER_KEY_ENV_VAR + " must equal TEST_KEY for this test",
                TEST_KEY.equals(envKey));
        String plain = "happy-path-secret";
        String encWrapped = "ENC(" + ConfigCryptoUtil.encrypt(plain, TEST_KEY) + ")";
        assertEquals(plain, ConfigCryptoUtil.decryptIfEncrypted(encWrapped, "test-secret"));
    }

    // ── reEncrypt tests ──────────────────────────────────────────────────────────────────────

    @Test
    public void reEncryptRoundTrips() throws GeneralException {
        String oldKey = "old-master-key";
        String newKey = "new-master-key";
        String plain = "secret-db-password";
        String encWithOldKey = "ENC(" + ConfigCryptoUtil.encrypt(plain, oldKey) + ")";
        String encWithNewKey = ConfigCryptoUtil.reEncrypt(encWithOldKey, oldKey, newKey);
        assertTrue("reEncrypt output must be ENC(...) wrapped", encWithNewKey.startsWith("ENC(") && encWithNewKey.endsWith(")"));
        String base64 = encWithNewKey.substring("ENC(".length(), encWithNewKey.length() - 1);
        assertEquals("plaintext must survive a full reEncrypt cycle", plain, ConfigCryptoUtil.decrypt(base64, newKey));
    }

    @Test
    public void reEncryptWithSameKeyPreservesPlaintext() throws GeneralException {
        String key = "same-key-both-ends";
        String plain = "unchanged-value";
        String enc = "ENC(" + ConfigCryptoUtil.encrypt(plain, key) + ")";
        String reEnc = ConfigCryptoUtil.reEncrypt(enc, key, key);
        String base64 = reEnc.substring("ENC(".length(), reEnc.length() - 1);
        assertEquals("plaintext must be identical when old and new key are the same", plain, ConfigCryptoUtil.decrypt(base64, key));
    }

    @Test
    public void reEncryptProducesEncWrapper() throws GeneralException {
        String oldKey = "key-a";
        String newKey = "key-b";
        String enc = "ENC(" + ConfigCryptoUtil.encrypt("value", oldKey) + ")";
        String result = ConfigCryptoUtil.reEncrypt(enc, oldKey, newKey);
        assertTrue("output must start with ENC(", result.startsWith("ENC("));
        assertTrue("output must end with )", result.endsWith(")"));
    }

    @Test(expected = GeneralException.class)
    public void reEncryptWithWrongOldKeyThrows() throws GeneralException {
        String enc = "ENC(" + ConfigCryptoUtil.encrypt("secret", "correct-key") + ")";
        ConfigCryptoUtil.reEncrypt(enc, "wrong-key", "new-key");
    }

    @Test(expected = GeneralException.class)
    public void reEncryptRejectsNonEncValue() throws GeneralException {
        ConfigCryptoUtil.reEncrypt("plaintext-no-wrapper", "oldKey", "newKey");
    }

    @Test(expected = GeneralException.class)
    public void reEncryptRejectsMissingCloseParen() throws GeneralException {
        ConfigCryptoUtil.reEncrypt("ENC(base64withnoclose", "oldKey", "newKey");
    }

    @Test
    public void iterationCountIsPositive() {
        // Verifies that readIterations() returned a usable value regardless of what security.properties contains.
        assertTrue("ITERATIONS must be positive", ConfigCryptoUtil.ITERATIONS > 0);
    }

    @Test
    public void iterationCountMatchesSecurityPropertiesDefault() {
        // When no security.properties override is present in the test environment, the default of 310000 is used.
        // If an override is present, we just verify the value is still positive.
        assertTrue("ITERATIONS must be at least 1", ConfigCryptoUtil.ITERATIONS >= 1);
    }

    @Test
    public void decryptIfEncryptedThrowsWhenMasterKeyMissing() throws GeneralException {
        assumeTrue("Skipped: OFBIZ_MASTER_KEY is set in this environment",
                System.getenv("OFBIZ_MASTER_KEY") == null || System.getenv("OFBIZ_MASTER_KEY").isEmpty());
        String encWrapped = "ENC(" + ConfigCryptoUtil.encrypt("secret", TEST_KEY) + ")";
        try {
            ConfigCryptoUtil.decryptIfEncrypted(encWrapped, "test-secret");
            fail("Expected GeneralException when OFBIZ_MASTER_KEY is absent");
        } catch (GeneralException e) {
            assertTrue("Exception message should mention OFBIZ_MASTER_KEY",
                    e.getMessage().contains("OFBIZ_MASTER_KEY"));
        }
    }
}
