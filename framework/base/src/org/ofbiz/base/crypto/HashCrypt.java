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
import java.util.Random;

import org.apache.commons.codec.EncoderException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang.RandomStringUtils;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralRuntimeException;
import org.ofbiz.base.util.StringUtil;
import org.ofbiz.base.util.UtilValidate;

/**
 * Utility class for doing SHA-1/MD5 One-Way Hash Encryption
 *
 */
public class HashCrypt {

    public static final String module = HashCrypt.class.getName();
    public static final String CRYPT_CHAR_SET = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789./";

    public static boolean comparePassword(String crypted, String defaultCrypt, String password) {
        try {
            if (crypted.startsWith("{")) {
                int typeEnd = crypted.indexOf("}");
                String hashType = crypted.substring(1, typeEnd);
                String hashed = crypted.substring(typeEnd + 1);
                MessageDigest messagedigest = MessageDigest.getInstance(hashType);
                // FIXME: should have been getBytes("UTF-8") originally
                messagedigest.update(password.getBytes());
                byte[] digestBytes = messagedigest.digest();
                char[] digestChars = Hex.encodeHex(digestBytes);
                if (hashed.equals(new String(digestChars))) {
                    return true;
                }
                // This next block should be removed when all {prefix}oldFunnyHex are fixed.
                int k = 0;
                digestChars = new char[digestBytes.length * 2];
                for (int l = 0; l < digestBytes.length; l++) {
                    int i1 = digestBytes[l];

                    if (i1 < 0) {
                        i1 = 127 + i1 * -1;
                    }
                    StringUtil.encodeInt(i1, k, digestChars);
                    k += 2;
                }
                if (hashed.equals(new String(digestChars))) {
                    Debug.logWarning("Warning: detected oldFunnyHex password prefixed with a hashType; this is not valid", module);
                    return true;
                }
                return false;
            } else if (crypted.startsWith("$")) {
                int typeEnd = crypted.indexOf("$", 1);
                int saltEnd = crypted.indexOf("$", typeEnd + 1);
                String hashType = crypted.substring(1, typeEnd);
                String salt = crypted.substring(typeEnd + 1, saltEnd);
                String hashed = crypted.substring(saltEnd + 1);
                return hashed.equals(getCrypted(hashType, salt, password));
            } else {
                String hashType = defaultCrypt;
                String hashed = crypted;
                MessageDigest messagedigest = MessageDigest.getInstance(hashType);
                // FIXME: should have been getBytes("UTF-8") originally
                messagedigest.update(password.getBytes());
                char[] digestChars = Hex.encodeHex(messagedigest.digest());
                return hashed.equals(new String(digestChars));
            }
        } catch (NoSuchAlgorithmException e) {
            throw new GeneralRuntimeException("Error while comparing password", e);
        }
    }

    public static String cryptPassword(String hashType, String password) {
        int saltLength = new Random().nextInt(15) + 1;
        return cryptPassword(hashType, RandomStringUtils.random(saltLength, CRYPT_CHAR_SET), password);
    }

    public static String cryptPassword(String hashType, String salt, String password) {
        StringBuilder sb = new StringBuilder();
        sb.append("$").append(hashType).append("$").append(salt).append("$");
        sb.append(getCrypted(hashType, salt, password));
        return sb.toString();
    }

    private static String getCrypted(String hashType, String salt, String password) {
        try {
            MessageDigest messagedigest = MessageDigest.getInstance(hashType);
            messagedigest.update(salt.getBytes("UTF-8"));
            messagedigest.update(password.getBytes("UTF-8"));
            return Base64.encodeBase64URLSafeString(messagedigest.digest()).replace('+', '.');
        } catch (NoSuchAlgorithmException e) {
            throw new GeneralRuntimeException("Error while comparing password", e);
        } catch (UnsupportedEncodingException e) {
            throw new GeneralRuntimeException("Error while comparing password", e);
        }
    }

    /**
     * @deprecated use cryptPassword
     */
    @Deprecated
    public static String getDigestHash(String str) {
        return getDigestHash(str, "SHA");
    }

    /**
     * @deprecated use cryptPassword
     */
    @Deprecated
    public static String getDigestHash(String str, String hashType) {
        if (str == null) return null;
        try {
            MessageDigest messagedigest = MessageDigest.getInstance(hashType);
            byte[] strBytes = str.getBytes();

            messagedigest.update(strBytes);
            byte[] digestBytes = messagedigest.digest();
            char[] digestChars = Hex.encodeHex(digestBytes);

            StringBuilder sb = new StringBuilder();
            sb.append("{").append(hashType).append("}");
            sb.append(digestChars, 0, digestChars.length);
            return sb.toString();
        } catch (Exception e) {
            throw new GeneralRuntimeException("Error while computing hash of type " + hashType, e);
        }
    }

    /**
     * @deprecated use cryptPassword
     */
    @Deprecated
    public static String getDigestHash(String str, String code, String hashType) {
        if (str == null) return null;
        try {
            byte codeBytes[] = null;

            if (code == null) codeBytes = str.getBytes();
            else codeBytes = str.getBytes(code);
            MessageDigest messagedigest = MessageDigest.getInstance(hashType);

            messagedigest.update(codeBytes);
            byte digestBytes[] = messagedigest.digest();
            char[] digestChars = Hex.encodeHex(digestBytes);
            StringBuilder sb = new StringBuilder();
            sb.append("{").append(hashType).append("}");
            sb.append(digestChars, 0, digestChars.length);
            return sb.toString();
        } catch (Exception e) {
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
     * @deprecated no replacement, logic moved into comparePassword
     */
    @Deprecated
    public static String getDigestHashOldFunnyHexEncode(String str, String hashType) {
        if (UtilValidate.isEmpty(hashType)) hashType = "SHA";
        if (str == null) return null;
        try {
            MessageDigest messagedigest = MessageDigest.getInstance(hashType);
            byte strBytes[] = str.getBytes();

            messagedigest.update(strBytes);
            byte digestBytes[] = messagedigest.digest();
            int k = 0;
            char digestChars[] = new char[digestBytes.length * 2];

            for (int l = 0; l < digestBytes.length; l++) {
                int i1 = digestBytes[l];

                if (i1 < 0) {
                    i1 = 127 + i1 * -1;
                }
                StringUtil.encodeInt(i1, k, digestChars);
                k += 2;
            }

            return new String(digestChars, 0, digestChars.length);
        } catch (Exception e) {
            Debug.logError(e, "Error while computing hash of type " + hashType, module);
        }
        return str;
    }
}
