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
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.codec.binary.Base64;
import org.apache.shiro.crypto.AesCipherService;
import org.apache.shiro.crypto.OperationMode;
import org.apache.shiro.crypto.hash.DefaultHashService;
import org.apache.shiro.crypto.hash.HashRequest;
import org.apache.shiro.crypto.hash.HashService;
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
import org.ofbiz.entity.model.ModelField.EncryptMethod;
import org.ofbiz.entity.transaction.TransactionUtil;

public final class EntityCrypto {

    public static final String module = EntityCrypto.class.getName();

    protected final Delegator delegator;
    protected final ConcurrentMap<String, byte[]> keyMap = new ConcurrentHashMap<String, byte[]>();
    protected final StorageHandler[] handlers;

    public EntityCrypto(Delegator delegator, String kekText) throws EntityCryptoException {
        this.delegator = delegator;
        byte[] kek;
        kek = UtilValidate.isNotEmpty(kekText) ? Base64.decodeBase64(kekText) : null;
        handlers = new StorageHandler[] {
            new ShiroStorageHandler(kek),
            new SaltedBase64StorageHandler(kek),
            NormalHashStorageHandler,
            OldFunnyHashStorageHandler,
        };
    }

    public void clearKeyCache() {
        keyMap.clear();
    }

    /** Encrypts an Object into an encrypted hex encoded String */
    @Deprecated
    public String encrypt(String keyName, Object obj) throws EntityCryptoException {
        return encrypt(keyName, EncryptMethod.TRUE, obj);
    }

    /** Encrypts an Object into an encrypted hex encoded String */
    public String encrypt(String keyName, EncryptMethod encryptMethod, Object obj) throws EntityCryptoException {
        try {
            byte[] key = this.findKey(keyName, handlers[0]);
            if (key == null) {
                EntityCryptoException caught = null;
                try {
                    this.createKey(keyName, handlers[0], encryptMethod);
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
                        // is occurring; rethrow the original exception if available
                        throw caught != null ? caught : e;
                    }
                    if (key == null) {
                        // this is also bad, couldn't find any key
                        throw caught != null ? caught : new EntityCryptoException("could not lookup key (" + keyName + ") after creation");
                    }
                }
            }
            return handlers[0].encryptValue(encryptMethod, key, UtilObject.getBytes(obj));
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
    public Object decrypt(String keyName, EncryptMethod encryptMethod, String encryptedString) throws EntityCryptoException {
        try {
            return doDecrypt(keyName, encryptMethod, encryptedString, handlers[0]);
        } catch (GeneralException e) {
            Debug.logInfo("Decrypt with DES key from standard key name hash failed, trying old/funny variety of key name hash", module);
            for (int i = 1; i < handlers.length; i++) {
                try {
                    // try using the old/bad hex encoding approach; this is another path the code may take, ie if there is an exception thrown in decrypt
                    return doDecrypt(keyName, encryptMethod, encryptedString, handlers[i]);
                } catch (GeneralException e1) {
                    // NOTE: this throws the original exception back, not the new one if it fails using the other approach
                    //throw new EntityCryptoException(e);
                }
            }
            throw new EntityCryptoException(e);
        }
    }

    protected Object doDecrypt(String keyName, EncryptMethod encryptMethod, String encryptedString, StorageHandler handler) throws GeneralException {
        byte[] key = this.findKey(keyName, handler);
        if (key == null) {
            throw new EntityCryptoException("key(" + keyName + ") not found in database");
        }
        byte[] decryptedBytes = handler.decryptValue(key, encryptMethod, encryptedString);
        try {
            return UtilObject.getObjectException(decryptedBytes);
        } catch (ClassNotFoundException e) {
            throw new GeneralException(e);
        } catch (IOException e) {
            throw new GeneralException(e);
        }
    }

    protected byte[] findKey(String originalKeyName, StorageHandler handler) throws EntityCryptoException {
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
            keyValue = EntityQuery.use(delegator).from("EntityKeyStore").where("keyName", hashedKeyName).queryOne();
        } catch (GenericEntityException e) {
            throw new EntityCryptoException(e);
        }
        if (keyValue == null || keyValue.get("keyText") == null) {
            return null;
        }
        try {
            byte[] keyBytes = handler.decodeKeyBytes(keyValue.getString("keyText"));
            keyMap.putIfAbsent(keyMapName, keyBytes);
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

    protected void createKey(String originalKeyName, StorageHandler handler, EncryptMethod encryptMethod) throws EntityCryptoException {
        String hashedKeyName = handler.getHashedKeyName(originalKeyName);
        Key key = handler.generateNewKey();
        final GenericValue newValue = delegator.makeValue("EntityKeyStore");
        try {
            newValue.set("keyText", handler.encodeKey(key.getEncoded()));
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
        protected abstract Key generateNewKey() throws EntityCryptoException;

        protected abstract String getHashedKeyName(String originalKeyName);
        protected abstract String getKeyMapPrefix(String hashedKeyName);

        protected abstract byte[] decodeKeyBytes(String keyText) throws GeneralException;
        protected abstract String encodeKey(byte[] key) throws GeneralException;

        protected abstract byte[] decryptValue(byte[] key, EncryptMethod encryptMethod, String encryptedString) throws GeneralException;
        protected abstract String encryptValue(EncryptMethod encryptMethod, byte[] key, byte[] objBytes) throws GeneralException;
    }

    protected static final class ShiroStorageHandler extends StorageHandler {
        private final HashService hashService;
        private final AesCipherService cipherService;
        private final AesCipherService saltedCipherService;
        private final byte[] kek;

        protected ShiroStorageHandler(byte[] kek) {
            hashService = new DefaultHashService();
            cipherService = new AesCipherService();
            cipherService.setMode(OperationMode.ECB);
            saltedCipherService = new AesCipherService();
            this.kek = kek;
        }

        @Override
        protected Key generateNewKey() {
            return saltedCipherService.generateNewKey();
        }

        @Override
        protected String getHashedKeyName(String originalKeyName) {
            HashRequest hashRequest = new HashRequest.Builder().setSource(originalKeyName).build();
            return hashService.computeHash(hashRequest).toBase64();
        }

        @Override
        protected String getKeyMapPrefix(String hashedKeyName) {
            return "{shiro}";
        }

        @Override
        protected byte[] decodeKeyBytes(String keyText) throws GeneralException {
            byte[] keyBytes = Base64.decodeBase64(keyText);
            if (kek != null) {
                keyBytes = saltedCipherService.decrypt(keyBytes, kek).getBytes();
            }
            return keyBytes;
        }

        @Override
        protected String encodeKey(byte[] key) throws GeneralException {
            if (kek != null) {
                return saltedCipherService.encrypt(key, kek).toBase64();
            } else {
                return Base64.encodeBase64String(key);
            }
        }

        @Override
        protected byte[] decryptValue(byte[] key, EncryptMethod encryptMethod, String encryptedString) throws GeneralException {
            switch (encryptMethod) {
                case SALT:
                    return saltedCipherService.decrypt(Base64.decodeBase64(encryptedString), key).getBytes();
                default:
                    return cipherService.decrypt(Base64.decodeBase64(encryptedString), key).getBytes();
            }
        }

        @Override
        protected String encryptValue(EncryptMethod encryptMethod, byte[] key, byte[] objBytes) throws GeneralException {
            switch (encryptMethod) {
                case SALT:
                    return saltedCipherService.encrypt(objBytes, key).toBase64();
                default:
                    return cipherService.encrypt(objBytes, key).toBase64();
            }
        }
    }

    protected static abstract class LegacyStorageHandler extends StorageHandler {
        @Override
        protected Key generateNewKey() throws EntityCryptoException {
            try {
                return DesCrypt.generateKey();
            } catch (NoSuchAlgorithmException e) {
                throw new EntityCryptoException(e);
            }
        }

        @Override
        protected byte[] decodeKeyBytes(String keyText) throws GeneralException {
            return StringUtil.fromHexString(keyText);
        }

        @Override
        protected String encodeKey(byte[] key) {
            return StringUtil.toHexString(key);
        }

        @Override
        protected byte[] decryptValue(byte[] key, EncryptMethod encryptMethod, String encryptedString) throws GeneralException {
            return DesCrypt.decrypt(DesCrypt.getDesKey(key), StringUtil.fromHexString(encryptedString));
        }

        @Override
        protected String encryptValue(EncryptMethod encryptMethod, byte[] key, byte[] objBytes) throws GeneralException {
            return StringUtil.toHexString(DesCrypt.encrypt(DesCrypt.getDesKey(key), objBytes));
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
        private final Key kek;

        protected SaltedBase64StorageHandler(byte[] kek) throws EntityCryptoException {
            Key key = null;
            if (kek != null) {
                try {
                    key = DesCrypt.getDesKey(kek);
                } catch (GeneralException e) {
                    Debug.logInfo("Invalid key-encryption-key specified for SaltedBase64StorageHandler; the key is probably valid for the newer ShiroStorageHandler", module);
                }
            }
            this.kek = key;
        }

        @Override
        protected Key generateNewKey() throws EntityCryptoException {
            try {
                return DesCrypt.generateKey();
            } catch (NoSuchAlgorithmException e) {
                throw new EntityCryptoException(e);
            }
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
        protected String encodeKey(byte[] key) throws GeneralException {
            if (kek != null) {
                key = DesCrypt.encrypt(kek, key);
            }
            return Base64.encodeBase64String(key);
        }

        @Override
        protected byte[] decryptValue(byte[] key, EncryptMethod encryptMethod, String encryptedString) throws GeneralException {
            byte[] allBytes = DesCrypt.decrypt(DesCrypt.getDesKey(key), Base64.decodeBase64(encryptedString));
            int length = allBytes[0];
            byte[] objBytes = new byte[allBytes.length - 1 - length];
            System.arraycopy(allBytes, 1 + length, objBytes, 0, objBytes.length);
            return objBytes;
        }

        @Override
        protected String encryptValue(EncryptMethod encryptMethod, byte[] key, byte[] objBytes) throws GeneralException {
            byte[] saltBytes;
            switch (encryptMethod) {
                case SALT:
                    Random random = new SecureRandom();
                    // random length 5-16
                    saltBytes = new byte[5 + random.nextInt(11)];
                    random.nextBytes(saltBytes);
                    break;
                default:
                    saltBytes = new byte[0];
                    break;
            }
            byte[] allBytes = new byte[1 + saltBytes.length + objBytes.length];
            allBytes[0] = (byte) saltBytes.length;
            System.arraycopy(saltBytes, 0, allBytes, 1, saltBytes.length);
            System.arraycopy(objBytes, 0, allBytes, 1 + saltBytes.length, objBytes.length);
            String result = Base64.encodeBase64String(DesCrypt.encrypt(DesCrypt.getDesKey(key), allBytes));
            return result;
        }
    };
}
