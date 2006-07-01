/*
 * $Id: DesCrypt.java 5462 2005-08-05 18:35:48Z jonesde $
 *
 * Copyright (c) 2004 The Open For Business Project - www.ofbiz.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 * CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT
 * OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 * THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */
package org.ofbiz.base.crypto;

import java.security.NoSuchAlgorithmException;
import java.security.InvalidKeyException;
import java.security.InvalidAlgorithmParameterException;
import java.security.spec.InvalidKeySpecException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.BadPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.DESedeKeySpec;

import org.ofbiz.base.util.GeneralException;

/**
 * Utility class for doing DESded (3DES) Two-Way Encryption
 *
 * @author     <a href="mailto:jaz@ofbiz.org">Andy Zeneski</a>
 * @version    $Rev$
 * @since      3.2
 */
public class DesCrypt {

    public static final String module = DesCrypt.class.getName();

    public static SecretKey generateKey() throws NoSuchAlgorithmException {
        KeyGenerator keyGen = KeyGenerator.getInstance("DESede");

        // generate the DES3 key
        return keyGen.generateKey();
    }

    public static byte[] encrypt(SecretKey key, byte[] bytes) throws GeneralException {
        Cipher cipher = DesCrypt.getCipher(key, Cipher.ENCRYPT_MODE);
        byte[] encBytes = null;
        try {
            encBytes = cipher.doFinal(bytes);
        } catch (IllegalStateException e) {
            throw new GeneralException(e);
        } catch (IllegalBlockSizeException e) {
            throw new GeneralException(e);
        } catch (BadPaddingException e) {
            throw new GeneralException(e);
        }
        return encBytes;
    }

    public static byte[] decrypt(SecretKey key, byte[] bytes) throws GeneralException {
        Cipher cipher = DesCrypt.getCipher(key, Cipher.DECRYPT_MODE);
        byte[] decBytes = null;
        try {
            decBytes = cipher.doFinal(bytes);
        } catch (IllegalStateException e) {
            throw new GeneralException(e);
        } catch (IllegalBlockSizeException e) {
            throw new GeneralException(e);
        } catch (BadPaddingException e) {
            throw new GeneralException(e);
        }
        return decBytes;
    }

    public static SecretKey getDesKey(byte[] rawKey) throws GeneralException {
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
            SecretKey key = null;
            try {
                key = skf.generateSecret(desedeSpec1);
            } catch (InvalidKeySpecException e) {
                throw new GeneralException(e);
            }
            return key;
        } else {
            throw new GeneralException("Not a valid DESede key!");
        }
    }

    // return a cipher for a key - DESede/CBC/PKCS5Padding IV = 0
    protected static Cipher getCipher(SecretKey key, int mode) throws GeneralException {
        byte[] zeros = { 0, 0, 0, 0, 0, 0, 0, 0 };
        IvParameterSpec iv = new IvParameterSpec(zeros);

        // create the Cipher - DESede/CBC/NoPadding
        Cipher encCipher = null;
        try {
            encCipher = Cipher.getInstance("DESede/CBC/PKCS5Padding");
        } catch (NoSuchAlgorithmException e) {
            throw new GeneralException(e);
        } catch (NoSuchPaddingException e) {
            throw new GeneralException(e);
        }
        try {
            encCipher.init(mode, key, iv);
        } catch (InvalidKeyException e) {
            throw new GeneralException(e);
        } catch (InvalidAlgorithmParameterException e) {
            throw new GeneralException(e);
        }
        return encCipher;
    }
}
