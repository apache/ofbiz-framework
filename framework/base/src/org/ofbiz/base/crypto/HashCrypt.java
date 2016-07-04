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
package org.ofbiz.base.crypto;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang.RandomStringUtils;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralRuntimeException;
import org.ofbiz.base.util.StringUtil;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.UtilIO;

/**
 * Utility class for doing SHA-1/MD5 One-Way Hash Encryption
 *
 */
public class HashCrypt {

    public static final String module = HashCrypt.class.getName();
    public static final String CRYPT_CHAR_SET = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789./";

    public static MessageDigest getMessageDigest(String type) {
        try {
            return MessageDigest.getInstance(type);
        } catch (NoSuchAlgorithmException e) {
            throw new GeneralRuntimeException("Could not load digestor(" + type + ")", e);
        }
    }

    public static boolean comparePassword(String crypted, String defaultCrypt, String password) {
        if (crypted.startsWith("{")) {
            // FIXME: should have been getBytes("UTF-8") originally
            return doCompareTypePrefix(crypted, defaultCrypt, password.getBytes());
        } else if (crypted.startsWith("$")) {
            return doComparePosix(crypted, defaultCrypt, password.getBytes(UtilIO.getUtf8()));
        } else {
            // FIXME: should have been getBytes("UTF-8") originally
            return doCompareBare(crypted, defaultCrypt, password.getBytes());
        }
    }

    private static boolean doCompareTypePrefix(String crypted, String defaultCrypt, byte[] bytes) {
        int typeEnd = crypted.indexOf("}");
        String hashType = crypted.substring(1, typeEnd);
        String hashed = crypted.substring(typeEnd + 1);
        MessageDigest messagedigest = getMessageDigest(hashType);
        messagedigest.update(bytes);
        byte[] digestBytes = messagedigest.digest();
        char[] digestChars = Hex.encodeHex(digestBytes);
        String checkCrypted = new String(digestChars);
        if (hashed.equals(checkCrypted)) {
            return true;
        }
        // This next block should be removed when all {prefix}oldFunnyHex are fixed.
        if (hashed.equals(oldFunnyHex(digestBytes))) {
            Debug.logWarning("Warning: detected oldFunnyHex password prefixed with a hashType; this is not valid, please update the value in the database with ({%s}%s)", module, hashType, checkCrypted);
            return true;
        }
        return false;
    }

    private static boolean doComparePosix(String crypted, String defaultCrypt, byte[] bytes) {
        int typeEnd = crypted.indexOf("$", 1);
        int saltEnd = crypted.indexOf("$", typeEnd + 1);
        String hashType = crypted.substring(1, typeEnd);
        String salt = crypted.substring(typeEnd + 1, saltEnd);
        String hashed = crypted.substring(saltEnd + 1);
        return hashed.equals(getCryptedBytes(hashType, salt, bytes));
    }

    private static boolean doCompareBare(String crypted, String defaultCrypt, byte[] bytes) {
        String hashType = defaultCrypt;
        String hashed = crypted;
        MessageDigest messagedigest = getMessageDigest(hashType);
        messagedigest.update(bytes);
        return hashed.equals(oldFunnyHex(messagedigest.digest()));
    }

    /*
     * @deprecated use cryptBytes(hashType, salt, password); eventually, use
     * cryptUTF8(hashType, salt, password) after all existing installs are
     * salt-based.  If the call-site of cryptPassword is just used to create a *new*
     * value, then you can switch to cryptUTF8 directly.
     */
    @Deprecated
    public static String cryptPassword(String hashType, String salt, String password) {
        // FIXME: should have been getBytes("UTF-8") originally
        return password != null ? cryptBytes(hashType, salt, password.getBytes()) : null;
    }

    public static String cryptUTF8(String hashType, String salt, String value) {
        return value != null ? cryptBytes(hashType, salt, value.getBytes(UtilIO.getUtf8())) : null;
    }

    public static String cryptValue(String hashType, String salt, String value) {
        return value != null ? cryptBytes(hashType, salt, value.getBytes()) : null;
    }

    public static String cryptBytes(String hashType, String salt, byte[] bytes) {
        if (hashType == null) {
            hashType = "SHA";
        }
        if (salt == null) {
            salt = RandomStringUtils.random(new SecureRandom().nextInt(15) + 1, CRYPT_CHAR_SET);
        }
        StringBuilder sb = new StringBuilder();
        sb.append("$").append(hashType).append("$").append(salt).append("$");
        sb.append(getCryptedBytes(hashType, salt, bytes));
        return sb.toString();
    }

    private static String getCryptedBytes(String hashType, String salt, byte[] bytes) {
        try {
            MessageDigest messagedigest = MessageDigest.getInstance(hashType);
            messagedigest.update(salt.getBytes(UtilIO.getUtf8()));
            messagedigest.update(bytes);
            return Base64.encodeBase64URLSafeString(messagedigest.digest()).replace('+', '.');
        } catch (NoSuchAlgorithmException e) {
            throw new GeneralRuntimeException("Error while comparing password", e);
        }
    }

    /**
     * @deprecated use digestHash("SHA", null, str)
     */
    @Deprecated
    public static String getDigestHash(String str) {
        return digestHash("SHA", null, str);
    }

    /**
     * @deprecated use digestHash(hashType, null, str))
     */
    @Deprecated
    public static String getDigestHash(String str, String hashType) {
        return digestHash(hashType, null, str);
    }

    /**
     * @deprecated use digestHash(hashType, code, str);
     */
    @Deprecated
    public static String getDigestHash(String str, String code, String hashType) {
        return digestHash(hashType, code, str);
    }

    public static String digestHash(String hashType, String code, String str) {
        if (str == null) return null;
        byte[] codeBytes;
        try {
            if (code == null) codeBytes = str.getBytes();
            else codeBytes = str.getBytes(code);
        } catch (UnsupportedEncodingException e) {
            throw new GeneralRuntimeException("Error while computing hash of type " + hashType, e);
        }
        return digestHash(hashType, codeBytes);
    }

    public static String digestHash(String hashType, byte[] bytes) {
        try {
            MessageDigest messagedigest = MessageDigest.getInstance(hashType);
            messagedigest.update(bytes);
            byte[] digestBytes = messagedigest.digest();
            char[] digestChars = Hex.encodeHex(digestBytes);

            StringBuilder sb = new StringBuilder();
            sb.append("{").append(hashType).append("}");
            sb.append(digestChars, 0, digestChars.length);
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new GeneralRuntimeException("Error while computing hash of type " + hashType, e);
        }
    }

    public static String digestHash64(String hashType, byte[] bytes) {
        if (hashType == null) {
            hashType = "SHA";
        }
        try {
            MessageDigest messagedigest = MessageDigest.getInstance(hashType);
            messagedigest.update(bytes);
            byte[] digestBytes = messagedigest.digest();

            StringBuilder sb = new StringBuilder();
            sb.append("{").append(hashType).append("}");
            sb.append(Base64.encodeBase64URLSafeString(digestBytes).replace('+', '.'));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new GeneralRuntimeException("Error while computing hash of type " + hashType, e);
        }
    }

    /**
     * @deprecated use cryptPassword
     */
    @Deprecated
    public static String getHashTypeFromPrefix(String hashString) {
        if (UtilValidate.isEmpty(hashString) || hashString.charAt(0) != '{') {
            return null;
        }

        return hashString.substring(1, hashString.indexOf('}'));
    }

    /**
     * @deprecated use cryptPassword
     */
    @Deprecated
    public static String removeHashTypePrefix(String hashString) {
        if (UtilValidate.isEmpty(hashString) || hashString.charAt(0) != '{') {
            return hashString;
        }

        return hashString.substring(hashString.indexOf('}') + 1);
    }

    /**
     * @deprecated use digestHashOldFunnyHex(hashType, str)
     */
    @Deprecated
    public static String getDigestHashOldFunnyHexEncode(String str, String hashType) {
        return digestHashOldFunnyHex(hashType, str);
    }

    public static String digestHashOldFunnyHex(String hashType, String str) {
        if (UtilValidate.isEmpty(hashType)) hashType = "SHA";
        if (str == null) return null;
        try {
            MessageDigest messagedigest = MessageDigest.getInstance(hashType);
            byte[] strBytes = str.getBytes();

            messagedigest.update(strBytes);
            return oldFunnyHex(messagedigest.digest());
        } catch (Exception e) {
            Debug.logError(e, "Error while computing hash of type " + hashType, module);
        }
        return str;
    }

    // This next block should be removed when all {prefix}oldFunnyHex are fixed.
    private static String oldFunnyHex(byte[] bytes) {
        int k = 0;
        char[] digestChars = new char[bytes.length * 2];
        for (int l = 0; l < bytes.length; l++) {
            int i1 = bytes[l];

            if (i1 < 0) {
                i1 = 127 + i1 * -1;
            }
            StringUtil.encodeInt(i1, k, digestChars);
            k += 2;
        }
        return new String(digestChars);
    }
}
