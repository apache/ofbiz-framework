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
package org.ofbiz.entity.util;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Callable;

import javax.crypto.SecretKey;
import javax.transaction.Transaction;

import org.ofbiz.base.crypto.DesCrypt;
import org.ofbiz.base.crypto.HashCrypt;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.StringUtil;
import org.ofbiz.base.util.UtilObject;
import org.ofbiz.entity.EntityCryptoException;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.transaction.GenericTransactionException;
import org.ofbiz.entity.transaction.TransactionUtil;

public final class EntityCrypto {

    public static final String module = EntityCrypto.class.getName();

    protected final Delegator delegator;
    protected final Map<String, SecretKey> keyMap = new HashMap<String, SecretKey>();

    public EntityCrypto(Delegator delegator) {
        this.delegator = delegator;

        // check the key table and make sure there
        // make sure there are some dummy keys
        synchronized(EntityCrypto.class) {
            try {
                long size = delegator.findCountByCondition("EntityKeyStore", null, null, null);
                if (size == 0) {
                    for (int i = 0; i < 20; i++) {
                        String randomName = this.getRandomString();
                        this.createKey(randomName);
                    }
                }
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
            }
        }
    }

    /** Encrypts an Object into an encrypted hex encoded String */
    public String encrypt(String keyName, Object obj) throws EntityCryptoException {
        try {
            SecretKey key = this.findKey(keyName, false);
            if (key == null) {
                key = this.createKey(keyName);
            }
            byte[] encryptedBytes = DesCrypt.encrypt(key, UtilObject.getBytes(obj));
            String hexString = StringUtil.toHexString(encryptedBytes);
            return hexString;
        } catch (GeneralException e) {
            throw new EntityCryptoException(e);
        }
    }

    /** Decrypts a hex encoded String into an Object */
    public Object decrypt(String keyName, String encryptedString) throws EntityCryptoException {
        Object decryptedObj = null;
        byte[] encryptedBytes = StringUtil.fromHexString(encryptedString);
        try {
            decryptedObj = doDecrypt(keyName, encryptedBytes, false);
        } catch (GeneralException e) {
            try {
                // try using the old/bad hex encoding approach; this is another path the code may take, ie if there is an exception thrown in decrypt
                Debug.logInfo("Decrypt with DES key from standard key name hash failed, trying old/funny variety of key name hash", module);
                decryptedObj = doDecrypt(keyName, encryptedBytes, false);
                //Debug.logInfo("Old/funny variety succeeded: Decrypted value [" + encryptedString + "]", module);
            } catch (GeneralException e1) {
                // NOTE: this throws the original exception back, not the new one if it fails using the other approach
                throw new EntityCryptoException(e);
            }
        }

        // NOTE: this is definitely for debugging purposes only, do not uncomment in production server for security reasons: Debug.logInfo("Decrypted value [" + encryptedString + "] to result: " + decryptedObj, module);
        return decryptedObj;
    }

    protected Object doDecrypt(String keyName, byte[] encryptedBytes, boolean useOldFunnyKeyHash) throws GeneralException {
        SecretKey key = this.findKey(keyName, false);
        if (key == null) {
            throw new EntityCryptoException("key(" + keyName + ") not found in database");
        }
        byte[] decryptedBytes = DesCrypt.decrypt(key, encryptedBytes);
        try {
            return UtilObject.getObjectException(decryptedBytes);
        } catch (ClassNotFoundException e) {
            throw new GeneralException(e);
        } catch (IOException e) {
            throw new GeneralException(e);
        }
    }

    protected SecretKey findKey(String originalKeyName, boolean useOldFunnyKeyHash) throws EntityCryptoException {
        String keyMapName = originalKeyName + useOldFunnyKeyHash;
        synchronized (keyMap) {
            if (keyMap.containsKey(keyMapName)) {
                return keyMap.get(keyMapName);
            }
        }
        // it's ok to run the bulk of this method unlocked or
        // unprotected; since the same result will occur even if
        // multiple threads request the same key, there is no
        // need to protected this block of code.
        String hashedKeyName = useOldFunnyKeyHash? HashCrypt.digestHashOldFunnyHex(null, originalKeyName) : HashCrypt.digestHash("SHA", null, originalKeyName);

        GenericValue keyValue = null;
        try {
            keyValue = delegator.findOne("EntityKeyStore", false, "keyName", hashedKeyName);
        } catch (GenericEntityException e) {
            throw new EntityCryptoException(e);
        }
        if (keyValue == null || keyValue.get("keyText") == null) {
            return null;
        }
        try {
            byte[] keyBytes = StringUtil.fromHexString(keyValue.getString("keyText"));
            SecretKey key = DesCrypt.getDesKey(keyBytes);
            synchronized (keyMap) {
                keyMap.put(keyMapName, key);
            }
            return key;
        } catch (GeneralException e) {
            throw new EntityCryptoException(e);
        }
    }

    protected SecretKey createKey(String originalKeyName) throws EntityCryptoException {
        String hashedKeyName = HashCrypt.getDigestHash(originalKeyName);
        SecretKey key = null;
        try {
            key = DesCrypt.generateKey();
        } catch (NoSuchAlgorithmException e) {
            throw new EntityCryptoException(e);
        }
        final GenericValue newValue = delegator.makeValue("EntityKeyStore");
        newValue.set("keyText", StringUtil.toHexString(key.getEncoded()));
        newValue.set("keyName", hashedKeyName);

        try {
            TransactionUtil.doNewTransaction(new Callable<Void>() {
                public Void call() throws Exception {
                    delegator.create(newValue);
                    return null;
                }
            }, "storing encrypted key", 0, true);
        } catch (GenericEntityException e) {
            throw new EntityCryptoException(e);
        }
        String keyMapName = originalKeyName + false;
        synchronized (keyMap) {
            keyMap.put(keyMapName, key);
        }
        return key;
    }

    protected String getRandomString() {
        Random rand = new Random();
        byte[] randomBytes = new byte[24];
        rand.nextBytes(randomBytes);
        return StringUtil.toHexString(randomBytes);
    }
}
