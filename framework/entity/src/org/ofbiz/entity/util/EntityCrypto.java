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
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.crypto.SecretKey;

import org.apache.commons.codec.binary.Base64;
import org.ofbiz.base.crypto.DesCrypt;
import org.ofbiz.base.crypto.HashCrypt;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.StringUtil;
import org.ofbiz.base.util.UtilObject;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.EntityCryptoException;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.transaction.TransactionUtil;

public final class EntityCrypto {

    public static final String module = EntityCrypto.class.getName();

    protected final Delegator delegator;
    protected final ConcurrentMap<String, SecretKey> keyMap = new ConcurrentHashMap<String, SecretKey>();
    protected final StorageHandler[] handlers;

    public EntityCrypto(Delegator delegator, String kekText) throws EntityCryptoException {
        this.delegator = delegator;
        SecretKey kek;
        try {
            kek = UtilValidate.isNotEmpty(kekText) ? DesCrypt.getDesKey(Base64.decodeBase64(kekText)) : null;
        } catch (GeneralException e) {
            throw new EntityCryptoException(e);
        }
        handlers = new StorageHandler[] {
            new SaltedBase64StorageHandler(kek),
            NormalHashStorageHandler,
            OldFunnyHashStorageHandler,
        };
    }

    /** Encrypts an Object into an encrypted hex encoded String */
    public String encrypt(String keyName, Object obj) throws EntityCryptoException {
        try {
            SecretKey key = this.findKey(keyName, handlers[0]);
            if (key == null) {
                EntityCryptoException caught = null;
                try {
                    this.createKey(keyName, handlers[0]);
                } catch (EntityCryptoException e) {
                    // either a database read error, or a duplicate key insert
                    // if the latter, try to fetch the value created by the
                    // other thread.
                    caught = e;
                } finally {
                    try {
                        key = this.findKey(keyName, handlers[0]);
                    } catch (EntityCryptoException e) {
                        // this is bad, couldn't lookup the value, some bad juju
                        // is occuring; rethrow the original exception if available
                        throw caught != null ? caught : e;
                    }
                    if (key == null) {
                        // this is also bad, couldn't find any key
                        throw caught != null ? caught : new EntityCryptoException("could not lookup key (" + keyName + ") after creation");
                    }
                }
            }
            return handlers[0].encryptValue(key, UtilObject.getBytes(obj));
        } catch (GeneralException e) {
            throw new EntityCryptoException(e);
        }
    }

    // NOTE: this is definitely for debugging purposes only, do not uncomment in production server for security reasons:
    // if you uncomment this, then change the real decrypt method to _decrypt.
    /*
    public Object decrypt(String keyName, String encryptedString) throws EntityCryptoException {
        Object result = _decrypt(keyName, encryptedString);
        Debug.logInfo("Decrypted value [%s] to result: %s", module, encryptedString, decryptedObj);
        return result;
    }
    */

    /** Decrypts a hex encoded String into an Object */
    public Object decrypt(String keyName, String encryptedString) throws EntityCryptoException {
        try {
            return doDecrypt(keyName, encryptedString, handlers[0]);
        } catch (GeneralException e) {
            Debug.logInfo("Decrypt with DES key from standard key name hash failed, trying old/funny variety of key name hash", module);
            for (int i = 1; i < handlers.length; i++) {
                try {
                    // try using the old/bad hex encoding approach; this is another path the code may take, ie if there is an exception thrown in decrypt
                    return doDecrypt(keyName, encryptedString, handlers[i]);
                } catch (GeneralException e1) {
                    // NOTE: this throws the original exception back, not the new one if it fails using the other approach
                    //throw new EntityCryptoException(e);
                }
            }
            throw new EntityCryptoException(e);
        }
    }

    protected Object doDecrypt(String keyName, String encryptedString, StorageHandler handler) throws GeneralException {
        SecretKey key = this.findKey(keyName, handler);
        if (key == null) {
            throw new EntityCryptoException("key(" + keyName + ") not found in database");
        }
        byte[] decryptedBytes = handler.decryptValue(key, encryptedString);
        try {
            return UtilObject.getObjectException(decryptedBytes);
        } catch (ClassNotFoundException e) {
            throw new GeneralException(e);
        } catch (IOException e) {
            throw new GeneralException(e);
        }
    }

    protected SecretKey findKey(String originalKeyName, StorageHandler handler) throws EntityCryptoException {
        String hashedKeyName = handler.getHashedKeyName(originalKeyName);
        String keyMapName = handler.getKeyMapPrefix(hashedKeyName) + hashedKeyName;
        if (keyMap.containsKey(keyMapName)) {
            return keyMap.get(keyMapName);
        }
        // it's ok to run the bulk of this method unlocked or
        // unprotected; since the same result will occur even if
        // multiple threads request the same key, there is no
        // need to protected this block of code.

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
            byte[] keyBytes = handler.decodeKeyBytes(keyValue.getString("keyText"));
            SecretKey key = DesCrypt.getDesKey(keyBytes);
            keyMap.putIfAbsent(keyMapName, key);
            // Do not remove the next line, it's there to handle the
            // case of multiple threads trying to find the same key
            // both threads will do the findOne call, only one will
            // succeed at the putIfAbsent, but both will then fetch
            // the same value with the following get().
            return keyMap.get(keyMapName);
        } catch (GeneralException e) {
            throw new EntityCryptoException(e);
        }
    }

    protected void createKey(String originalKeyName, StorageHandler handler) throws EntityCryptoException {
        String hashedKeyName = handler.getHashedKeyName(originalKeyName);
        SecretKey key = null;
        try {
            key = DesCrypt.generateKey();
        } catch (NoSuchAlgorithmException e) {
            throw new EntityCryptoException(e);
        }
        final GenericValue newValue = delegator.makeValue("EntityKeyStore");
        try {
            newValue.set("keyText", handler.encodeKey(key));
        } catch (GeneralException e) {
            throw new EntityCryptoException(e);
        }
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
    }

    protected abstract static class StorageHandler {
        protected abstract String getHashedKeyName(String originalKeyName);
        protected abstract String getKeyMapPrefix(String hashedKeyName);

        protected abstract byte[] decodeKeyBytes(String keyText) throws GeneralException;
        protected abstract String encodeKey(SecretKey key) throws GeneralException;

        protected abstract byte[] decryptValue(SecretKey key, String encryptedString) throws GeneralException;
        protected abstract String encryptValue(SecretKey key, byte[] objBytes) throws GeneralException;
    }

    protected static abstract class LegacyStorageHandler extends StorageHandler {
        @Override
        protected byte[] decodeKeyBytes(String keyText) throws GeneralException {
            return StringUtil.fromHexString(keyText);
        }

        @Override
        protected String encodeKey(SecretKey key) {
            return StringUtil.toHexString(key.getEncoded());
        }

        @Override
        protected byte[] decryptValue(SecretKey key, String encryptedString) throws GeneralException {
            return DesCrypt.decrypt(key, StringUtil.fromHexString(encryptedString));
        }

        @Override
        protected String encryptValue(SecretKey key, byte[] objBytes) throws GeneralException {
            return StringUtil.toHexString(DesCrypt.encrypt(key, objBytes));
        }
    };

    protected static final StorageHandler OldFunnyHashStorageHandler = new LegacyStorageHandler() {
        @Override
        protected String getHashedKeyName(String originalKeyName) {
            return HashCrypt.digestHashOldFunnyHex(null, originalKeyName);
        }

        @Override
        protected String getKeyMapPrefix(String hashedKeyName) {
            return "{funny-hash}";
        }
    };

    protected static final StorageHandler NormalHashStorageHandler = new LegacyStorageHandler() {
        @Override
        protected String getHashedKeyName(String originalKeyName) {
            return HashCrypt.digestHash("SHA", originalKeyName.getBytes());
        }

        @Override
        protected String getKeyMapPrefix(String hashedKeyName) {
            return "{normal-hash}";
        }
    };

    protected static final class SaltedBase64StorageHandler extends StorageHandler {
        private final SecretKey kek;

        protected SaltedBase64StorageHandler(SecretKey kek) {
            this.kek = kek;
        }

        @Override
        protected String getHashedKeyName(String originalKeyName) {
            return HashCrypt.digestHash64("SHA", originalKeyName.getBytes());
        }

        @Override
        protected String getKeyMapPrefix(String hashedKeyName) {
            return "{salted-base64}";
        }

        @Override
        protected byte[] decodeKeyBytes(String keyText) throws GeneralException {
            byte[] keyBytes = Base64.decodeBase64(keyText);
            if (kek != null) {
                keyBytes = DesCrypt.decrypt(kek, keyBytes);
            }
            return keyBytes;
        }

        @Override
        protected String encodeKey(SecretKey key) throws GeneralException {
            byte[] keyBytes = key.getEncoded();
            if (kek != null) {
                keyBytes = DesCrypt.encrypt(kek, keyBytes);
            }
            return Base64.encodeBase64String(keyBytes);
        }

        @Override
        protected byte[] decryptValue(SecretKey key, String encryptedString) throws GeneralException {
            byte[] allBytes = DesCrypt.decrypt(key, Base64.decodeBase64(encryptedString));
            int length = allBytes[0];
            byte[] objBytes = new byte[allBytes.length - 1 - length];
            System.arraycopy(allBytes, 1 + length, objBytes, 0, objBytes.length);
            return objBytes;
        }

        @Override
        protected String encryptValue(SecretKey key, byte[] objBytes) throws GeneralException {
            Random random = new Random();
            // random length 5-16
            byte[] saltBytes = new byte[5 + random.nextInt(11)];
            random.nextBytes(saltBytes);
            byte[] allBytes = new byte[1 + saltBytes.length + objBytes.length];
            allBytes[0] = (byte) saltBytes.length;
            System.arraycopy(saltBytes, 0, allBytes, 1, saltBytes.length);
            System.arraycopy(objBytes, 0, allBytes, 1 + saltBytes.length, objBytes.length);
            String result = Base64.encodeBase64String(DesCrypt.encrypt(key, allBytes));
            return result;
        }
    };
}
