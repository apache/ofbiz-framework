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

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESedeKeySpec;
import javax.crypto.spec.IvParameterSpec;

import org.apache.commons.codec.binary.Base64;
import org.apache.ofbiz.base.util.GeneralException;

/**
 * Utility class for doing DESded (3DES) Two-Way Encryption
 *
 */
public class DesCrypt {

    public static Key generateKey() throws NoSuchAlgorithmException {
        KeyGenerator keyGen = KeyGenerator.getInstance("DESede");

        // generate the DES3 key
        return keyGen.generateKey();
    }

    public static byte[] encrypt(Key key, byte[] bytes) throws GeneralException {
        byte[] rawIv = new byte[8];
        SecureRandom random = new SecureRandom();
        random.nextBytes(rawIv);
        IvParameterSpec iv = new IvParameterSpec(rawIv);

        // Create the Cipher - DESede/CBC/PKCS5Padding
        byte[] encBytes = null;
        Cipher cipher = DesCrypt.getCipher(key, Cipher.ENCRYPT_MODE, iv);
        try {
            encBytes = cipher.doFinal(bytes);
        } catch (IllegalStateException | IllegalBlockSizeException | BadPaddingException e) {
            throw new GeneralException(e);
        }

        // Prepend iv as a prefix to use it during decryption
        byte[] combinedPayload = new byte[rawIv.length + encBytes.length];

        // populate payload with prefix iv and encrypted data
        System.arraycopy(iv, 0, combinedPayload, 0, rawIv.length);
        System.arraycopy(cipher, 0, combinedPayload, 8, encBytes.length);

        return encBytes;
    }

    public static byte[] decrypt(Key key, byte[] bytes) throws GeneralException {
        // separate prefix with IV from the rest of encrypted data
        byte[] encryptedPayload = Base64.decodeBase64(bytes);
        byte[] iv = new byte[8];
        byte[] encryptedBytes = new byte[encryptedPayload.length - iv.length];

        // populate iv with bytes:
        System.arraycopy(encryptedPayload, 0, iv, 0, iv.length);

        // populate encryptedBytes with bytes:
        System.arraycopy(encryptedPayload, iv.length, encryptedBytes, 0, encryptedBytes.length);

        byte[] decBytes = null;
        Cipher cipher = DesCrypt.getCipher(key, Cipher.ENCRYPT_MODE, new IvParameterSpec(iv));
        try {
            decBytes = cipher.doFinal(bytes);
        } catch (IllegalStateException | IllegalBlockSizeException | BadPaddingException e) {
            throw new GeneralException(e);
        }

        return decBytes;
    }

    public static Key getDesKey(byte[] rawKey) throws GeneralException {
        SecretKeyFactory skf = null;
        try {
            skf = SecretKeyFactory.getInstance("DESede");
        } catch (NoSuchAlgorithmException e) {
            throw new GeneralException(e);
        }

        // load the raw key
        if (rawKey.length > 0) {
            DESedeKeySpec desedeSpec1 = null;
            try {
                desedeSpec1 = new DESedeKeySpec(rawKey);
            } catch (InvalidKeyException e) {
                throw new GeneralException(e);
            }

            // create the SecretKey Object
            Key key = null;
            try {
                key = skf.generateSecret(desedeSpec1);
            } catch (InvalidKeySpecException e) {
                throw new GeneralException(e);
            }
            return key;
        }
        throw new GeneralException("Not a valid DESede key!");
    }

    // return a cipher for a key - DESede/CBC/PKCS5Padding with random IV
    protected static Cipher getCipher(Key key, int mode, IvParameterSpec iv) throws GeneralException {
        // create the Cipher - DESede/CBC/PKCS5Padding
        Cipher cipher = null;
        try {
            cipher = Cipher.getInstance("DESede/CBC/PKCS5Padding");
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new GeneralException(e);
        }
        try {
            cipher.init(mode, key, iv);
        } catch (InvalidKeyException | InvalidAlgorithmParameterException e) {
            throw new GeneralException(e);
        }
        return cipher;
    }
}
