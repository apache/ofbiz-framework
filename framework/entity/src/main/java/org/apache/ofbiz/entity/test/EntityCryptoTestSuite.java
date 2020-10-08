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
package org.apache.ofbiz.entity.test;

import java.util.List;

import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityConditionSubSelect;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.entity.testtools.EntityTestCase;
import org.apache.ofbiz.entity.util.EntityQuery;

/**
 * The type EntityCryptoTestSuite.
 */
public class EntityCryptoTestSuite extends EntityTestCase {
    public EntityCryptoTestSuite(String name) {
        super(name);
    }

    /**
     * Test crypto.
     * @throws Exception the exception
     */
    public void testCrypto() throws Exception {
        String nanoTime = "" + System.nanoTime();
        getDelegator().removeByAnd("TestingCrypto", UtilMisc.toMap("testingCryptoTypeId", "BASIC"));
        getDelegator().create("TestingCrypto", UtilMisc.toMap("testingCryptoId", "1", "testingCryptoTypeId", "BASIC"));
        GenericValue entity = EntityQuery.use(getDelegator()).from("TestingCrypto").where("testingCryptoId", "1").queryOne();
        assertNull(entity.getString("unencryptedValue"));
        assertNull(entity.getString("encryptedValue"));
        entity.setString("unencryptedValue", nanoTime);
        entity.setString("encryptedValue", nanoTime);
        entity.setString("saltedEncryptedValue", nanoTime);
        assertEquals(nanoTime, entity.getString("unencryptedValue"));
        assertEquals(nanoTime, entity.getString("encryptedValue"));
        assertEquals(nanoTime, entity.getString("saltedEncryptedValue"));
        entity.store();
        entity.refresh();
        assertEquals(nanoTime, entity.getString("unencryptedValue"));
        assertEquals(nanoTime, entity.getString("encryptedValue"));
        assertEquals(nanoTime, entity.getString("saltedEncryptedValue"));
    }

    /**
     * Test crypto encryption.
     * @throws Exception the exception
     */
    public void testCryptoEncryption() throws Exception {
        Delegator delegator = getDelegator();
        // clear out all values
        delegator.removeByAnd("TestingCrypto", UtilMisc.toMap("testingCryptoTypeId", "BASIC"));

        String nanoTime = "" + System.nanoTime();

        // Ensure that null values are passed thru unencrypted.
        delegator.create("TestingCrypto", UtilMisc.toMap("testingCryptoId", "1", "testingCryptoTypeId", "BASIC"));
        GenericValue entity = EntityQuery.use(delegator).from("TestingCrypto").where("testingCryptoId", "1").queryOne();
        assertNull(entity.getString("unencryptedValue"));
        assertNull(entity.getString("encryptedValue"));
        assertNull(entity.getString("saltedEncryptedValue"));
        GenericValue view = EntityQuery.use(delegator).from("TestingCryptoRawView").where("testingCryptoId", "1").queryOne();
        assertNull(view.getString("unencryptedValue"));
        assertNull(view.getString("encryptedValue"));
        assertNull(view.getString("saltedEncryptedValue"));
        assertNull(view.getString("rawEncryptedValue"));
        assertNull(view.getString("rawSaltedEncryptedValue"));

        // Verify that encryption is taking place
        entity.setString("unencryptedValue", nanoTime);
        entity.setString("encryptedValue", nanoTime);
        entity.setString("saltedEncryptedValue", nanoTime);
        entity.store();
        view.refresh();
        assertEquals(nanoTime, view.getString("unencryptedValue"));
        assertEquals(nanoTime, view.getString("encryptedValue"));
        assertEquals(nanoTime, view.getString("saltedEncryptedValue"));
        String initialValue = view.getString("rawEncryptedValue");
        String initialSaltedValue = view.getString("rawSaltedEncryptedValue");
        assertFalse(nanoTime.equals(initialValue));
        assertFalse(nanoTime.equals(initialSaltedValue));
        assertFalse(initialValue.equals(initialSaltedValue));

        // Verify that the same value stored repeatedly gives different raw encrypted values.
        entity.setString("encryptedValue", nanoTime);
        entity.setString("saltedEncryptedValue", nanoTime);
        entity.store();
        //entity.refresh(); // this is a bug; store() ends up setting the encrypted value *into* the entity
        assertEquals(nanoTime, entity.getString("unencryptedValue"));
        assertEquals(nanoTime, entity.getString("encryptedValue"));

        view.refresh();
        assertEquals(nanoTime, view.getString("unencryptedValue"));
        assertEquals(nanoTime, view.getString("encryptedValue"));
        assertEquals(nanoTime, view.getString("saltedEncryptedValue"));

        String updatedValue = view.getString("rawEncryptedValue");
        String updatedSaltedValue = view.getString("rawSaltedEncryptedValue");

        assertFalse(nanoTime.equals(updatedValue));
        assertFalse(nanoTime.equals(updatedSaltedValue));
        assertFalse(updatedValue.equals(updatedSaltedValue));
        assertEquals(initialValue, updatedValue);
        assertFalse(initialSaltedValue.equals(updatedSaltedValue));
    }

    /**
     * Test crypto lookup.
     * @throws Exception the exception
     */
    public void testCryptoLookup() throws Exception {
        Delegator delegator = getDelegator();
        String nanoTime = "" + System.nanoTime();

        delegator.removeByAnd("TestingCrypto", UtilMisc.toMap("testingCryptoTypeId", "LOOKUP"));
        delegator.create("TestingCrypto", UtilMisc.toMap("testingCryptoId", "lookup-null", "testingCryptoTypeId", "LOOKUP"));
        delegator.create("TestingCrypto", UtilMisc.toMap("testingCryptoId", "lookup-value", "testingCryptoTypeId", "LOOKUP",
                "encryptedValue", nanoTime, "saltedEncryptedValue", nanoTime));

        // This ends up using EntityExpr contained in EntityConditionList
        assertEquals(1, (EntityQuery.use(delegator).from("TestingCrypto").where("testingCryptoTypeId", "LOOKUP",
                "encryptedValue", null).queryList()).size());
        assertEquals(1, (EntityQuery.use(delegator).from("TestingCrypto").where("testingCryptoTypeId", "LOOKUP",
                "saltedEncryptedValue", null).queryList()).size());
        assertEquals(1, (EntityQuery.use(delegator).from("TestingCrypto").where("testingCryptoTypeId", "LOOKUP",
                "encryptedValue", nanoTime).queryList()).size());
        assertEquals(0, (EntityQuery.use(delegator).from("TestingCrypto").where("testingCryptoTypeId", "LOOKUP",
                "saltedEncryptedValue", nanoTime).queryList()).size());

        assertEquals(1, EntityQuery.use(delegator).from("TestingCrypto").where("testingCryptoTypeId", "LOOKUP",
                "encryptedValue", nanoTime).queryList().size());
    }

    /**
     * Make sub select condition entity condition.
     * @param nanoTime the nano time
     * @return the entity condition
     */
    protected EntityCondition makeSubSelectCondition(String nanoTime) {
        return EntityCondition.makeCondition(
            EntityCondition.makeCondition("testingCryptoTypeId", EntityOperator.IN, UtilMisc.toList("SUB_SELECT_1", "SUB_SELECT_3")),
            EntityOperator.AND,
            EntityCondition.makeCondition("encryptedValue", EntityOperator.EQUALS, nanoTime));
    }

    /**
     * Make sub select entity condition sub select.
     * @param nanoTime the nano time
     * @return the entity condition sub select
     */
    protected EntityConditionSubSelect makeSubSelect(String nanoTime) {
        EntityCondition subCondition = makeSubSelectCondition(nanoTime);
        return new EntityConditionSubSelect("TestingCrypto", "testingCryptoId", subCondition, true, getDelegator());
    }

    /**
     * Test crypto sub select.
     * @throws Exception the exception
     */
    public void testCryptoSubSelect() throws Exception {
        Delegator delegator = getDelegator();
        String nanoTime = "" + System.nanoTime();
        EntityCondition condition;
        List<GenericValue> results;

        delegator.removeByAnd("TestingCrypto", UtilMisc.toMap("testingCryptoTypeId", "SUB_SELECT_1"));
        delegator.removeByAnd("TestingCrypto", UtilMisc.toMap("testingCryptoTypeId", "SUB_SELECT_2"));
        delegator.removeByAnd("TestingCrypto", UtilMisc.toMap("testingCryptoTypeId", "SUB_SELECT_3"));

        delegator.create("TestingCrypto", UtilMisc.toMap("testingCryptoId", "SUB_1", "testingCryptoTypeId", "SUB_SELECT_1",
                "encryptedValue", nanoTime));
        delegator.create("TestingCrypto", UtilMisc.toMap("testingCryptoId", "SUB_2", "testingCryptoTypeId", "SUB_SELECT_2",
                "encryptedValue", nanoTime));
        delegator.create("TestingCrypto", UtilMisc.toMap("testingCryptoId", "SUB_3", "testingCryptoTypeId", "SUB_SELECT_3",
                "encryptedValue", "constant"));

        results = EntityQuery.use(delegator).from("TestingCrypto").where("encryptedValue", nanoTime).orderBy("testingCryptoId").queryList();
        assertEquals(2, results.size());
        assertEquals("SUB_1", results.get(0).get("testingCryptoId"));
        assertEquals("SUB_2", results.get(1).get("testingCryptoId"));

        results = EntityQuery.use(delegator).from("TestingCrypto").where(EntityCondition.makeCondition("testingCryptoTypeId",
                EntityOperator.IN, UtilMisc.toList("SUB_SELECT_1", "SUB_SELECT_3"))).orderBy("testingCryptoId").queryList();
        assertEquals(2, results.size());
        assertEquals("SUB_1", results.get(0).get("testingCryptoId"));
        assertEquals("SUB_3", results.get(1).get("testingCryptoId"));

        condition = makeSubSelectCondition(nanoTime);
        results = EntityQuery.use(delegator).from("TestingCrypto").where(condition).orderBy("testingCryptoId").queryList();
        assertEquals(1, results.size());
        assertEquals("SUB_1", results.get(0).get("testingCryptoId"));

        condition = EntityCondition.makeCondition("testingCryptoId", EntityOperator.EQUALS, makeSubSelect(nanoTime));
        results = EntityQuery.use(delegator).from("TestingCrypto").where(condition).orderBy("testingCryptoId").queryList();
        assertEquals(1, results.size());
        assertEquals("SUB_1", results.get(0).get("testingCryptoId"));
    }
}
