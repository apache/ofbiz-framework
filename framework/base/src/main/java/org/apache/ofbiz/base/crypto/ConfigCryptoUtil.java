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

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.ofbiz.base.util.GeneralException;

/**
 * AES-256-GCM encryption/decryption for configuration values such as the
 * database passwords stored in {@code passwords.properties} as {@code ENC(...)}.
 *
 * <p>The AES key is derived from a master key (e.g. the {@code OFBIZ_DB_KEY}
 * environment variable) via PBKDF2WithHmacSHA256, so the same master key always
 * yields the same AES key. The IV is randomly generated per encryption and
 * stored alongside the ciphertext, both Base64-encoded together.</p>
 */
public final class ConfigCryptoUtil {

    private static final String CIPHER_ALGORITHM = "AES/GCM/NoPadding";
    private static final String KEY_DERIVATION_ALGORITHM = "PBKDF2WithHmacSHA256";
    // Static salt: the master key is the actual secret; the salt only needs to defeat
    // precomputed rainbow tables, not provide per-installation uniqueness.
    private static final byte[] SALT = "OFBizConfigCryptoUtilSalt".getBytes(StandardCharsets.UTF_8);
    private static final int ITERATIONS = 10000;
    private static final int KEY_LENGTH_BITS = 256;
    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final int IV_LENGTH_BYTES = 12;

    private static SecretKeySpec deriveKey(String masterKey) throws GeneralSecurityException {
        SecretKeyFactory factory = SecretKeyFactory.getInstance(KEY_DERIVATION_ALGORITHM);
        PBEKeySpec spec = new PBEKeySpec(masterKey.toCharArray(), SALT, ITERATIONS, KEY_LENGTH_BITS);
        try {
            return new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");
        } finally {
            spec.clearPassword();
        }
    }

    /**
     * Encrypts {@code plainText} with a key derived from {@code masterKey}.
     * @return Base64 string containing the random IV followed by the ciphertext (with GCM auth tag)
     */
    public static String encrypt(String plainText, String masterKey) throws GeneralException {
        try {
            SecretKeySpec secretKey = deriveKey(masterKey);
            byte[] iv = new byte[IV_LENGTH_BYTES];
            new SecureRandom().nextBytes(iv);
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            byte[] payload = new byte[IV_LENGTH_BYTES + cipherText.length];
            System.arraycopy(iv, 0, payload, 0, IV_LENGTH_BYTES);
            System.arraycopy(cipherText, 0, payload, IV_LENGTH_BYTES, cipherText.length);
            return Base64.getEncoder().encodeToString(payload);
        } catch (GeneralSecurityException e) {
            throw new GeneralException("Unable to encrypt value", e);
        }
    }

    /**
     * Reverses {@link #encrypt(String, String)}: derives the AES key from {@code masterKey},
     * splits the IV from the ciphertext and decrypts.
     */
    public static String decrypt(String encryptedBase64, String masterKey) throws GeneralException {
        try {
            byte[] payload = Base64.getDecoder().decode(encryptedBase64);
            if (payload.length <= IV_LENGTH_BYTES) {
                throw new GeneralException("Encrypted value is too short to contain an IV and ciphertext");
            }
            byte[] iv = Arrays.copyOfRange(payload, 0, IV_LENGTH_BYTES);
            byte[] cipherText = Arrays.copyOfRange(payload, IV_LENGTH_BYTES, payload.length);
            SecretKeySpec secretKey = deriveKey(masterKey);
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            return new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new GeneralException("Unable to decrypt value: verify the master key is correct", e);
        }
    }

    private ConfigCryptoUtil() { }

    /**
     * Command-line helper that prints {@code ENC(<base64>)} for a given master key and plaintext
     * value, ready to be pasted into {@code passwords.properties} (e.g. as
     * {@code jdbc-password.<lookup-key>=ENC(...)}). Invoked via the {@code encryptDbPassword}
     * Gradle task.
     */
    public static void main(String[] args) throws GeneralException {
        if (args.length != 2) {
            System.err.println("Usage: ConfigCryptoUtil <masterKey> <plainTextValue>");
            System.exit(1);
            return;
        }
        System.out.println("ENC(" + encrypt(args[1], args[0]) + ")");
    }
}
