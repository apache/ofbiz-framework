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
package org.ofbiz.entity.util;

import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import javax.crypto.SecretKey;
import javax.transaction.Transaction;

import org.ofbiz.base.crypto.DesCrypt;
import org.ofbiz.base.crypto.HashCrypt;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.StringUtil;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilObject;
import org.ofbiz.entity.EntityCryptoException;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.transaction.GenericTransactionException;
import org.ofbiz.entity.transaction.TransactionUtil;

public class EntityCrypto {

    public static final String module = EntityCrypto.class.getName();

    protected GenericDelegator delegator = null;
    protected Map keyMap = null;

    protected EntityCrypto() { }
    public EntityCrypto(GenericDelegator delegator) {
        this.delegator = delegator;
        this.keyMap = new HashMap();

        // check the key table and make sure there
        // make sure there are some dummy keys
        synchronized(EntityCrypto.class) {
            try {
                long size = delegator.findCountByAnd("EntityKeyStore", null);
                if (size == 0) {
                    for (int i = 0; i < 20; i++) {
                        String randomName = this.getRandomString();
                        this.getKeyFromStore(randomName);
                    }
                }
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
            }
        }
    }

    /** Encrypts a String into an encrypted hex encoded byte array */
    public String encrypt(String keyName, Object obj) throws EntityCryptoException {
        try {
            return StringUtil.toHexString(DesCrypt.encrypt(this.getKey(keyName), UtilObject.getBytes(obj)));
        } catch (GeneralException e) {
            throw new EntityCryptoException(e);
        }
    }

    /** Decrypts a hex encoded byte array into a String */
    public Object decrypt(String keyName, String str) throws EntityCryptoException {
        try {
            return UtilObject.getObject(DesCrypt.decrypt(this.getKey(keyName), StringUtil.fromHexString(str)));
        } catch (GeneralException e) {
            throw new EntityCryptoException(e);
        }
    }

    protected SecretKey getKey(String name) throws EntityCryptoException {
        SecretKey key = (SecretKey) keyMap.get(name);
        if (key == null) {
            synchronized(this) {
                String keyName = HashCrypt.getDigestHash(name);
                key = this.getKeyFromStore(keyName);
                keyMap.put(name, key);
            }
        }
        return key;
    }

    protected SecretKey getKeyFromStore(String keyName) throws EntityCryptoException {
        GenericValue keyValue = null;
        try {
            keyValue = delegator.findByPrimaryKey("EntityKeyStore", UtilMisc.toMap("keyName", keyName));
        } catch (GenericEntityException e) {
            throw new EntityCryptoException(e);
        }
        if (keyValue == null || keyValue.get("keyText") == null) {
            SecretKey key = null;
            try {
                key = DesCrypt.generateKey();
            } catch (NoSuchAlgorithmException e) {
                throw new EntityCryptoException(e);
            }
            GenericValue newValue = delegator.makeValue("EntityKeyStore", null);
            newValue.set("keyText", StringUtil.toHexString(key.getEncoded()));
            newValue.set("keyName", keyName);

            Transaction parentTransaction = null;
            boolean beganTrans = false;
            try {
                beganTrans = TransactionUtil.begin();
            } catch (GenericTransactionException e) {
                throw new EntityCryptoException(e);
            }

            if (!beganTrans) {
                try {
                    parentTransaction = TransactionUtil.suspend();
                } catch (GenericTransactionException e) {
                    throw new EntityCryptoException(e);
                }

                // now start a new transaction
                try {
                    beganTrans = TransactionUtil.begin();
                } catch (GenericTransactionException e) {
                    throw new EntityCryptoException(e);
                }
            }

            try {
                delegator.create(newValue);
            } catch (GenericEntityException e) {
                try {
                    TransactionUtil.rollback(beganTrans, "Error creating encrypted value", e);
                } catch (GenericTransactionException e1) {
                    Debug.logError(e1, "Could not rollback transaction", module);
                }
                throw new EntityCryptoException(e);
            } finally {
                try {
                    TransactionUtil.commit(beganTrans);
                } catch (GenericTransactionException e) {
                    throw new EntityCryptoException(e);
                }
                // resume the parent transaction
                if (parentTransaction != null) {
                    try {
                        TransactionUtil.resume(parentTransaction);
                    } catch (GenericTransactionException e) {
                        throw new EntityCryptoException(e);
                    }
                }
            }


            return key;
        } else {
            byte[] keyBytes = StringUtil.fromHexString(keyValue.getString("keyText"));
            try {
                return DesCrypt.getDesKey(keyBytes);
            } catch (GeneralException e) {
                throw new EntityCryptoException(e);
            }
        }
    }

    protected String getRandomString() {
        Random rand = new Random();
        byte[] randomBytes = new byte[24];
        rand.nextBytes(randomBytes);
        return StringUtil.toHexString(randomBytes);
    }
}
