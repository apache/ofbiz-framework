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
import java.util.Base64;
import java.util.Properties;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.shiro.crypto.CryptoException;
import org.apache.shiro.crypto.cipher.AesCipherService;
import org.apache.shiro.lang.util.ByteSource;

/**
 * AES-256-GCM encryption/decryption for configuration values such as the
 * database passwords stored in {@code passwords.properties} as {@code ENC(...)}.
 *
 * <p>The AES key is derived from a master key (e.g. the {@code OFBIZ_MASTER_KEY}
 * environment variable) via PBKDF2WithHmacSHA256, so the same master key always
 * yields the same AES key. The actual cipher operations are delegated to Shiro's
 * {@link AesCipherService} (the same library used by
 * {@link org.apache.ofbiz.entity.util.EntityCrypto}), which defaults to GCM mode
 * and prepends a random IV to the ciphertext.</p>
 */
public final class ConfigCryptoUtil {

    private static final String KEY_DERIVATION_ALGORITHM = "PBKDF2WithHmacSHA256";
    // Static salt: the master key is the actual secret; the salt only needs to defeat
    // precomputed rainbow tables, not provide per-installation uniqueness.
    private static final byte[] SALT = "OFBizConfigCryptoUtilSalt".getBytes(StandardCharsets.UTF_8);
    private static final int KEY_LENGTH_BITS = 256;

    // Note: AesCipherService derives the GCM tag length from getKeySize(), which must stay at
    // its default of 128 (a valid GCM tag length). The 256-bit AES key below is passed directly
    // to encrypt/decrypt and does not depend on this setting.
    private static final AesCipherService CIPHER_SERVICE = new AesCipherService();

    // Read config once at class-load time using raw Properties to avoid re-entrancy via
    // UtilProperties.getPropertyValue() → SecretValueResolver → ConfigCryptoUtil.
    private static final Properties SECURITY_PROPERTIES = UtilProperties.getProperties("security");

    /** Name of the environment variable holding the AES master key; configurable via secret.master.key.env.var. */
    public static final String MASTER_KEY_ENV_VAR = SECURITY_PROPERTIES != null
            ? SECURITY_PROPERTIES.getProperty("secret.master.key.env.var", "OFBIZ_MASTER_KEY").trim()
            : "OFBIZ_MASTER_KEY";

    // PBKDF2 iteration count. Raising this requires re-encrypting all existing ENC(...) values
    // because the derived AES key changes when the iteration count changes.
    static final int ITERATIONS = readIterations();

    private static int readIterations() {
        if (SECURITY_PROPERTIES == null) {
            return 310000;
        }
        try {
            int v = Integer.parseInt(
                    SECURITY_PROPERTIES.getProperty("secret.pbkdf2.iterations", "310000").trim());
            return v > 0 ? v : 310000;
        } catch (NumberFormatException e) {
            return 310000;
        }
    }

    private static byte[] deriveKey(String masterKey) throws GeneralSecurityException {
        SecretKeyFactory factory = SecretKeyFactory.getInstance(KEY_DERIVATION_ALGORITHM);
        PBEKeySpec spec = new PBEKeySpec(masterKey.toCharArray(), SALT, ITERATIONS, KEY_LENGTH_BITS);
        try {
            return factory.generateSecret(spec).getEncoded();
        } finally {
            spec.clearPassword();
        }
    }

    /**
     * Encrypts {@code plainText} with a key derived from {@code masterKey}.
     * @return Base64 string containing the random IV followed by the ciphertext (with GCM auth tag)
     */
    public static String encrypt(String plainText, String masterKey) throws GeneralException {
        if (masterKey == null || masterKey.isEmpty()) {
            throw new GeneralException("masterKey must not be null or empty");
        }
        try {
            byte[] key = deriveKey(masterKey);
            ByteSource encrypted = CIPHER_SERVICE.encrypt(plainText.getBytes(StandardCharsets.UTF_8), key);
            return encrypted.toBase64();
        } catch (GeneralSecurityException | CryptoException e) {
            throw new GeneralException("Unable to encrypt value", e);
        }
    }

    /**
     * Reverses {@link #encrypt(String, String)}: derives the AES key from {@code masterKey}
     * and lets {@link AesCipherService} split the IV from the ciphertext and decrypt.
     */
    public static String decrypt(String encryptedBase64, String masterKey) throws GeneralException {
        if (masterKey == null || masterKey.isEmpty()) {
            throw new GeneralException("masterKey must not be null or empty");
        }
        try {
            byte[] key = deriveKey(masterKey);
            byte[] payload = Base64.getDecoder().decode(encryptedBase64);
            byte[] decrypted = CIPHER_SERVICE.decrypt(payload, key).getClonedBytes();
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | CryptoException | IllegalArgumentException e) {
            throw new GeneralException("Unable to decrypt value: verify the master key is correct", e);
        }
    }

    /**
     * Returns the plaintext of {@code rawValue} if it is wrapped in {@code ENC(...)},
     * otherwise returns it unchanged.
     *
     * <p>All {@link org.apache.ofbiz.base.secret.SecretProvider} implementations should
     * call this after fetching a value from their remote backend so that operators can
     * optionally add a client-side AES-256-GCM layer on top of whatever the vault already
     * provides. The master key is read from the {@code OFBIZ_MASTER_KEY} environment variable.</p>
     *
     * @param rawValue   the value as returned by the secret backend (plaintext or {@code ENC(...)})
     * @param secretName used only in error messages to identify which secret failed
     * @return the plaintext value — either the original string or the decrypted one
     * @throws GeneralException if the value is encrypted but {@code OFBIZ_MASTER_KEY} is missing,
     *                          or if decryption fails (e.g. wrong key)
     */
    public static String decryptIfEncrypted(String rawValue, String secretName) throws GeneralException {
        if (!rawValue.startsWith("ENC(") || !rawValue.endsWith(")")) {
            return rawValue;
        }
        String masterKey = System.getenv(MASTER_KEY_ENV_VAR);
        if (masterKey == null || masterKey.isEmpty()) {
            throw new GeneralException("Secret '" + secretName + "' is encrypted (ENC(...)) but the "
                    + MASTER_KEY_ENV_VAR + " environment variable is not set");
        }
        String base64 = rawValue.substring("ENC(".length(), rawValue.length() - 1);
        try {
            return decrypt(base64, masterKey);
        } catch (GeneralException e) {
            throw new GeneralException("Failed to decrypt secret '" + secretName
                    + "': verify OFBIZ_MASTER_KEY matches the key used to encrypt", e);
        }
    }

    /**
     * Decrypts {@code encValue} (which must be an {@code ENC(...)} wrapper) with
     * {@code oldMasterKey} and immediately re-encrypts the plaintext with {@code newMasterKey}.
     *
     * <p>Used by the {@code reEncryptAllSecrets} Gradle task to rotate the master AES key
     * without ever exposing the raw plaintext beyond the JVM heap.</p>
     *
     * @param encValue     the current {@code ENC(...)} value from {@code passwords.properties}
     *                     or a {@code SystemProperty} row
     * @param oldMasterKey the master key currently in use
     * @param newMasterKey the new master key to encrypt with
     * @return a fresh {@code ENC(...)} value encrypted under {@code newMasterKey}
     * @throws GeneralException if {@code encValue} is not a valid {@code ENC(...)} wrapper,
     *                          if decryption fails (wrong key or corrupted data),
     *                          or if re-encryption fails
     */
    public static String reEncrypt(String encValue, String oldMasterKey, String newMasterKey)
            throws GeneralException {
        if (encValue == null || !encValue.startsWith("ENC(") || !encValue.endsWith(")")) {
            throw new GeneralException("Value is not a valid ENC(...) wrapper: " + encValue);
        }
        String base64 = encValue.substring("ENC(".length(), encValue.length() - 1);
        String plainText = decrypt(base64, oldMasterKey);
        return "ENC(" + encrypt(plainText, newMasterKey) + ")";
    }

    private ConfigCryptoUtil() { }

    /**
     * Command-line helper that prints {@code ENC(<base64>)} for a given master key and plaintext
     * value. Invoked via the {@code generateDBPassword} and {@code generateEncryptedSecret}
     * Gradle tasks to produce values for {@code passwords.properties}
     * ({@code jdbc-password.<lookup-key>=ENC(...)}) and {@code SystemProperty.systemPropertyValue}
     * respectively.
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
