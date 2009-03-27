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

import java.security.MessageDigest;

import org.apache.commons.codec.binary.Hex;
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

    public static String getDigestHash(String str) {
        return getDigestHash(str, "SHA");
    }

    public static String getDigestHash(String str, String hashType) {
        if (str == null) return null;
        try {
            MessageDigest messagedigest = MessageDigest.getInstance(hashType);
            byte[] strBytes = str.getBytes();

            messagedigest.update(strBytes);
            byte[] digestBytes = messagedigest.digest();
            char[] digestChars = Hex.encodeHex(digestBytes);

            return "{" + hashType + "}" + new String(digestChars, 0, digestChars.length);
        } catch (Exception e) {
            throw new GeneralRuntimeException("Error while computing hash of type " + hashType, e);
        }
    }

    public static String getDigestHash(String str, String code, String hashType) {
        if (str == null) return null;
        try {
            byte codeBytes[] = null;

            if (code == null) codeBytes = str.getBytes();
            else codeBytes = str.getBytes(code);
            MessageDigest messagedigest = MessageDigest.getInstance(hashType);

            messagedigest.update(codeBytes);
            byte digestBytes[] = messagedigest.digest();
            char[] digestChars = Hex.encodeHex(digestBytes);;
            return "{" + hashType + "}" + new String(digestChars, 0, digestChars.length);
        } catch (Exception e) {
            throw new GeneralRuntimeException("Error while computing hash of type " + hashType, e);
        }
    }

    public static String getHashTypeFromPrefix(String hashString) {
        if (UtilValidate.isEmpty(hashString) || hashString.charAt(0) != '{') {
            return null;
        }

        return hashString.substring(1, hashString.indexOf('}'));
    }

    public static String removeHashTypePrefix(String hashString) {
        if (UtilValidate.isEmpty(hashString) || hashString.charAt(0) != '{') {
            return hashString;
        }

        return hashString.substring(hashString.indexOf('}') + 1);
    }

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
