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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 * Blowfish (Two-Way) Byte/String encryption
 *
 */
public class BlowFishCrypt {

    private SecretKeySpec secretKeySpec = null;

    /**
     * Creates a new BlowFishCrypt object.
     * @param secretKeySpec A SecretKeySpec object.
     */
    public BlowFishCrypt(SecretKeySpec secretKeySpec) {
        this.secretKeySpec = secretKeySpec;
    }

    /**
     * Creates a new BlowFishCrypt object.
     * @param key An encoded secret key
     */
    public BlowFishCrypt(byte[] key) {
        try {
            secretKeySpec = new SecretKeySpec(key, "Blowfish");
        } catch (Exception e) {}
    }

    /**
     * Creates a new BlowFishCrypt object.
     * @param keyFile A file object containing the secret key as a String object.
     */
    public BlowFishCrypt(File keyFile) {
        try {
            FileInputStream is = new FileInputStream(keyFile);
            ObjectInputStream os = new ObjectInputStream(is);
            String keyString = (String) os.readObject();

            is.close();

            byte[] keyBytes = keyString.getBytes();

            secretKeySpec = new SecretKeySpec(keyBytes, "Blowfish");
        } catch (Exception e) {}
    }

    /**
     * Encrypt the string with the secret key.
     * @param string The string to encrypt.
     */
    public byte[] encrypt(String string) {
        return encrypt(string.getBytes());
    }

    /**
     * Decrypt the string with the secret key.
     * @param string The string to decrypt.
     */
    public byte[] decrypt(String string) {
        return decrypt(string.getBytes());
    }

    /**
     * Encrypt the byte array with the secret key.
     * @param bytes The array of bytes to encrypt.
     */
    public byte[] encrypt(byte[] bytes) {
        byte[] resp = null;

        try {
            resp = crypt(bytes, Cipher.ENCRYPT_MODE);
        } catch (Exception e) {
            return null;
        }
        return resp;
    }

    /**
     * Decrypt the byte array with the secret key.
     * @param bytes The array of bytes to decrypt.
     */
    public byte[] decrypt(byte[] bytes) {
        byte[] resp = null;

        try {
            resp = crypt(bytes, Cipher.DECRYPT_MODE);
        } catch (Exception e) {
            return null;
        }
        return resp;
    }

    private byte[] crypt(byte[] bytes, int mode) throws Exception {
        if (secretKeySpec == null)
            throw new Exception("SecretKey cannot be null.");
        Cipher cipher = Cipher.getInstance("Blowfish");

        cipher.init(mode, secretKeySpec);
        return cipher.doFinal(bytes);
    }

    public static byte[] generateKey() throws NoSuchAlgorithmException {
        KeyGenerator keyGen = KeyGenerator.getInstance("Blowfish");
        keyGen.init(448);

        SecretKey secretKey = keyGen.generateKey();
        byte[] keyBytes = secretKey.getEncoded();

        return keyBytes;
    }

    public static boolean testKey(byte[] key) {
        String testString = "1234567890ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstufwxyz";
        BlowFishCrypt c = new BlowFishCrypt(key);
        byte[] encryptedBytes = c.encrypt(testString);
        String encryptedMessage = new String(encryptedBytes);

        byte[] decryptedBytes = c.decrypt(encryptedMessage);
        String decryptedMessage = new String(decryptedBytes);

        if (testString.equals(decryptedMessage)) {
            return true;
        }

        return false;
    }

    public static void main(String args[]) throws Exception {
        if (args[0] == null) {
            args[0] = "ofbkey";
        }

        byte[] key = generateKey();
        if (testKey(key)) {
            FileOutputStream fos = new FileOutputStream(args[0]);
            ObjectOutputStream os = new ObjectOutputStream(fos);
            String keyString = new String(key);
            os.writeObject(keyString);
            fos.close();
        }
    }
}
