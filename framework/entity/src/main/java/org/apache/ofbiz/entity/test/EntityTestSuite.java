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

import java.math.BigDecimal;
import java.sql.Blob;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.sql.rowset.serial.SerialBlob;

import org.apache.ofbiz.base.concurrent.ExecutionPool;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.Observable;
import org.apache.ofbiz.base.util.Observer;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilXml;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.DelegatorFactory;
import org.apache.ofbiz.entity.GenericEntity;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericPK;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.entity.config.model.Datasource;
import org.apache.ofbiz.entity.config.model.EntityConfig;
import org.apache.ofbiz.entity.model.ModelEntity;
import org.apache.ofbiz.entity.model.ModelField;
import org.apache.ofbiz.entity.testtools.EntityTestCase;
import org.apache.ofbiz.entity.transaction.GenericTransactionException;
import org.apache.ofbiz.entity.transaction.TransactionUtil;
import org.apache.ofbiz.entity.util.EntityListIterator;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.EntitySaxReader;
import org.apache.ofbiz.entity.util.SequenceUtil;

public class EntityTestSuite extends EntityTestCase {

    public static final String module = EntityTestSuite.class.getName();
    /*
     * This sets how many values to insert when trying to create a large number of values.  10,000 causes HSQL to crash but is ok
     * with Derby.  Going up to 100,000 causes problems all around because Java List seems to be capped at about 65,000 values.
     *
     * NOTE: setting this lower so that the general tests don't take so long to run; to really push it can increase this number.
     * NOTE: Let's try to distinguish between functional testing and stress testing. Any value greater than 1 will be sufficient
     * for functional testing. Values like 10,000 or 100,000 are more appropriate for stress testing.
     */
    public static final long TEST_COUNT = 1000;

    public EntityTestSuite(String name) {
        super(name);
    }

    final static private int _level1max = 3;   // number of TestingNode entities to create

    public void testModels() throws Exception {
        ModelEntity modelEntity = delegator.getModelEntity("TestingType");
        assertNotNull("TestingType entity model not null", modelEntity);
        ModelField modelField = modelEntity.getField("description");
        assertNotNull("TestingType.description field model not null", modelField);
        modelField = ModelField.create(modelEntity, null, "newDesc", modelField.getType(), "NEW_DESC", null, null, false, false, false, false, false, null);
        modelEntity.addField(modelField);
        modelField = modelEntity.getField("newDesc");
        assertNotNull("TestingType.newDesc field model not null", modelField);
        modelEntity.removeField("newDesc");
        modelField = modelEntity.getField("newDesc");
        assertNull("TestingType.newDesc field model is null", modelField);
    }

    /*
     * Tests storing values with the delegator's .create, .makeValue, and .storeAll methods
     */
    public void testMakeValue() throws Exception {
        // This method call directly stores a new value into the entity engine
        GenericValue createdValue = delegator.create("TestingType", "testingTypeId", "TEST-MAKE-1", "description", "Testing Type #Make-1");
        assertTrue("Created value is mutable", createdValue.isMutable());
        assertFalse("Observable has not changed", createdValue.hasChanged());

        // This sequence creates the GenericValue entities first, puts them in a List, then calls the delegator to store them all
        List<GenericValue> newValues = new LinkedList<>();

        newValues.add(delegator.makeValue("TestingType", "testingTypeId", "TEST-MAKE-2", "description", "Testing Type #Make-2"));
        newValues.add(delegator.makeValue("TestingType", "testingTypeId", "TEST-MAKE-3", "description", "Testing Type #Make-3"));
        newValues.add(delegator.makeValue("TestingType", "testingTypeId", "TEST-MAKE-4", "description", "Testing Type #Make-4"));
        delegator.storeAll(newValues);

        // finds a List of newly created values.  the second parameter specifies the fields to order results by.
        List<GenericValue> newlyCreatedValues = EntityQuery.use(delegator)
                                                           .from("TestingType")
                                                           .where(EntityCondition.makeCondition("testingTypeId", EntityOperator.LIKE, "TEST-MAKE-%"))
                                                           .orderBy("testingTypeId")
                                                           .queryList();
        assertEquals("4 TestingTypes(for make) found", 4, newlyCreatedValues.size());
    }

    /*
     * Tests updating entities by doing a GenericValue .put(key, value) and .store()
     */
    public void testUpdateValue() throws Exception {
        // retrieve a sample GenericValue, make sure it's correct
        delegator.removeByCondition("TestingType", EntityCondition.makeCondition("testingTypeId", EntityOperator.LIKE, "TEST-UPDATE-%"));
        GenericValue testValue = EntityQuery.use(delegator).from("TestingType").where("testingTypeId", "TEST-UPDATE-1").queryOne();
        assertNull("No pre-existing type value", testValue);
        delegator.create("TestingType", "testingTypeId", "TEST-UPDATE-1", "description", "Testing Type #Update-1");
        testValue = EntityQuery.use(delegator).from("TestingType").where("testingTypeId", "TEST-UPDATE-1").queryOne();
        assertEquals("Retrieved value has the correct description", "Testing Type #Update-1", testValue.getString("description"));
        // Test Observable aspect
        assertFalse("Observable has not changed", testValue.hasChanged());
        TestObserver observer = new TestObserver();
        testValue.addObserver(observer);
        testValue.put("description", "New Testing Type #Update-1");
        assertEquals("Observer called with original GenericValue field name", "description", observer.arg);
        observer.observable = null;
        observer.arg = null;
        GenericValue clonedValue = (GenericValue) testValue.clone();
        clonedValue.put("description", "New Testing Type #Update-1");
        assertTrue("Cloned Observable has changed", clonedValue.hasChanged());
        assertEquals("Observer called with cloned GenericValue field name", "description", observer.arg);
        // now store it
        testValue.store();
        assertFalse("Observable has not changed", testValue.hasChanged());
        // now retrieve it again and make sure that the updated value is correct
        testValue = EntityQuery.use(delegator).from("TestingType").where("testingTypeId", "TEST-UPDATE-1").queryOne();
        assertEquals("Retrieved value has the correct description", "New Testing Type #Update-1", testValue.getString("description"));
    }

    public void testRemoveValue() throws Exception {
        // Retrieve a sample GenericValue, make sure it's correct
        delegator.removeByCondition("TestingType", EntityCondition.makeCondition("testingTypeId", EntityOperator.LIKE, "TEST-REMOVE-%"));
        GenericValue testValue = EntityQuery.use(delegator).from("TestingType").where("testingTypeId", "TEST-REMOVE-1").queryOne();
        assertNull("No pre-existing type value", testValue);
        delegator.create("TestingType", "testingTypeId", "TEST-REMOVE-1", "description", "Testing Type #Remove-1");
        testValue = EntityQuery.use(delegator).from("TestingType").where("testingTypeId", "TEST-REMOVE-1").queryOne();
        assertEquals("Retrieved value has the correct description", "Testing Type #Remove-1", testValue.getString("description"));
        testValue.remove();
        assertFalse("Observable has not changed", testValue.hasChanged());
        // Test immutable
        try {
            testValue.put("description", "New Testing Type #Remove-4");
            fail("Modified an immutable GenericValue");
        } catch (IllegalStateException e) {
        }
        try {
            testValue.remove("description");
            fail("Modified an immutable GenericValue");
        } catch (UnsupportedOperationException e) {
        }
        testValue = EntityQuery.use(delegator).from("TestingType").where("testingTypeId", "TEST-REMOVE-1").queryOne();
        assertEquals("Finding removed value returns null", null, testValue);
    }

    /*
     * Tests the entity cache
     */
    public void testEntityCache() throws Exception {
        // Test primary key cache
        delegator.removeByCondition("TestingType", EntityCondition.makeCondition("testingTypeId", EntityOperator.LIKE, "TEST-CACHE-%"));
        delegator.removeByCondition("TestingSubtype", EntityCondition.makeCondition("testingTypeId", EntityOperator.LIKE, "TEST-CACHE-%"));
        GenericValue testValue = EntityQuery.use(delegator).from("TestingType").where("testingTypeId", "TEST-CACHE-1").cache(true).queryOne();
        assertNull("No pre-existing type value", testValue);
        delegator.create("TestingType", "testingTypeId", "TEST-CACHE-1", "description", "Testing Type #Cache-1");
        testValue = EntityQuery.use(delegator).from("TestingType").where("testingTypeId", "TEST-CACHE-1").cache(true).queryOne();
        assertEquals("Retrieved from cache value has the correct description", "Testing Type #Cache-1", testValue.getString("description"));
        // Test immutable
        try {
            testValue.put("description", "New Testing Type #Cache-1");
            fail("Modified an immutable GenericValue");
        } catch (IllegalStateException e) {
        }
        try {
            testValue.remove("description");
            fail("Modified an immutable GenericValue");
        } catch (UnsupportedOperationException e) {
        }
        // Test entity value update operation updates the cache
        // Since the cache uses equals() and hashCode() methods, we test those as well
        int hashCode = testValue.hashCode();
        GenericValue originalValue = testValue;
        testValue = (GenericValue) testValue.clone();
        assertTrue("Cloned GenericValue equals original GenericValue", originalValue.equals(testValue));
        assertTrue("Cloned GenericValue has the same hash code", hashCode == testValue.hashCode());
        testValue.put("description", "New Testing Type #Cache-1");
        assertFalse("Modified GenericValue does not equal original GenericValue", originalValue.equals(testValue));
        assertTrue("Modified GenericValue has a different hash code", hashCode != testValue.hashCode());
        testValue.store();
        testValue = EntityQuery.use(delegator).from("TestingType").where("testingTypeId", "TEST-CACHE-1").cache(true).queryOne();
        assertEquals("Retrieved from cache value has the correct description", "New Testing Type #Cache-1", testValue.getString("description"));
        // Test storeByCondition updates the cache
        testValue = EntityQuery.use(delegator).from("TestingType").where("testingTypeId", "TEST-CACHE-1").cache(true).queryFirst();
        EntityCondition storeByCondition = EntityCondition.makeCondition(UtilMisc.toMap("testingTypeId", "TEST-CACHE-1",
                "lastUpdatedStamp", testValue.get("lastUpdatedStamp")));
        int qtyChanged = delegator.storeByCondition("TestingType", UtilMisc.toMap("description", "New Testing Type #Cache-0"), storeByCondition);
        assertEquals("Delegator.storeByCondition updated one value", 1, qtyChanged);
        testValue = EntityQuery.use(delegator).from("TestingType").where("testingTypeId", "TEST-CACHE-1").cache(true).queryFirst();

        assertEquals("Retrieved from cache value has the correct description", "New Testing Type #Cache-0", testValue.getString("description"));
        // Test removeByCondition updates the cache
        qtyChanged = delegator.removeByCondition("TestingType", storeByCondition);
        assertEquals("Delegator.removeByCondition removed one value", 1, qtyChanged);
        testValue = EntityQuery.use(delegator).from("TestingType").where("testingTypeId", "TEST-CACHE-1").cache(true).queryFirst();
        assertEquals("Retrieved from cache value is null", null, testValue);
        // Test entity value remove operation updates the cache
        testValue = delegator.create("TestingType", "testingTypeId", "TEST-CACHE-1", "description", "Testing Type #Cache-1");
        testValue.remove();
        testValue = EntityQuery.use(delegator).from("TestingType").where("testingTypeId", "TEST-CACHE-1").cache(true).queryOne();
        assertEquals("Retrieved from cache value is null", null, testValue);
        // Test entity condition cache
        List<GenericValue> testList = EntityQuery.use(delegator)
                                                 .from("TestingType")
                                                 .where(EntityCondition.makeCondition("description", EntityOperator.EQUALS, "Testing Type #Cache-2"))
                                                 .cache(true)
                                                 .queryList();
        assertEquals("Delegator findList returned no values", 0, testList.size());
        delegator.create("TestingType", "testingTypeId", "TEST-CACHE-2", "description", "Testing Type #Cache-2");
        testList = EntityQuery.use(delegator)
                              .from("TestingType")
                              .where(EntityCondition.makeCondition("description", EntityOperator.EQUALS, "Testing Type #Cache-2"))
                              .cache(true)
                              .queryList();
        assertEquals("Delegator findList returned one value", 1, testList.size());
        testValue = testList.get(0);
        assertEquals("Retrieved from cache value has the correct description", "Testing Type #Cache-2", testValue.getString("description"));
        // Test immutable
        try {
            testValue.put("description", "New Testing Type #2");
            fail("Modified an immutable GenericValue");
        } catch (IllegalStateException e) {
        }
        try {
            testValue.remove("description");
            fail("Modified an immutable GenericValue");
        } catch (UnsupportedOperationException e) {
        }
        // Test entity value create operation updates the cache
        testValue = (GenericValue) testValue.clone();
        testValue.put("testingTypeId", "TEST-CACHE-3");
        testValue.create();
        testList = EntityQuery.use(delegator)
                              .from("TestingType")
                              .where(EntityCondition.makeCondition("description", EntityOperator.EQUALS, "Testing Type #Cache-2"))
                              .cache(true)
                              .queryList();
        assertEquals("Delegator findList returned two values", 2, testList.size());
        // Test entity value update operation updates the cache
        testValue.put("description", "New Testing Type #Cache-3");
        testValue.store();
        testList = EntityQuery.use(delegator)
                              .from("TestingType")
                              .where(EntityCondition.makeCondition("description", EntityOperator.EQUALS, "Testing Type #Cache-2"))
                              .cache(true)
                              .queryList();
        assertEquals("Delegator findList returned one value", 1, testList.size());
        // Test entity value remove operation updates the cache
        testValue = testList.get(0);
        testValue = (GenericValue) testValue.clone();
        testValue.remove();
        testList = EntityQuery.use(delegator)
                              .from("TestingType")
                              .where(EntityCondition.makeCondition("description", EntityOperator.EQUALS, "Testing Type #Cache-2"))
                              .cache(true)
                              .queryList();
        assertEquals("Delegator findList returned empty list", 0, testList.size());
        // Test view entities in the pk cache - updating an entity should clear pk caches for all view entities containing that entity.
        testValue = EntityQuery.use(delegator).from("TestingSubtype").where("testingTypeId", "TEST-CACHE-3").cache(true).queryOne();
        assertNull("No pre-existing TestingSubtype", testValue);
        testValue = delegator.create("TestingSubtype", "testingTypeId", "TEST-CACHE-3", "subtypeDescription", "Testing Subtype #Cache-3");
        assertNotNull("TestingSubtype created", testValue);
        // Confirm member entity appears in the view
        testValue = EntityQuery.use(delegator).from("TestingViewPks").where("testingTypeId", "TEST-CACHE-3").cache(true).queryOne();
        assertEquals("View retrieved from cache has the correct member description", "Testing Subtype #Cache-3", testValue.getString("subtypeDescription"));
        testValue = EntityQuery.use(delegator).from("TestingSubtype").where("testingTypeId", "TEST-CACHE-3").cache(true).queryOne();
        // Modify member entity
        testValue = (GenericValue) testValue.clone();
        testValue.put("subtypeDescription", "New Testing Subtype #Cache-3");
        testValue.store();
        // Check if cached view contains the modification
        testValue = EntityQuery.use(delegator).from("TestingViewPks").where("testingTypeId", "TEST-CACHE-3").cache(true).queryOne();
        assertEquals("View retrieved from cache has the correct member description", "New Testing Subtype #Cache-3", testValue.getString("subtypeDescription"));
    }

    /*
     * Tests XML serialization by serializing/deserializing a GenericValue
     */
    public void testXmlSerialization() throws Exception {
        // Must use the default delegator because the deserialized GenericValue can't
        // find the randomized one.
        Delegator localDelegator = DelegatorFactory.getDelegator("default");
        boolean transBegin = TransactionUtil.begin();
        localDelegator.create("TestingType", "testingTypeId", "TEST-5", "description", "Testing Type #5");
        GenericValue testValue = EntityQuery.use(localDelegator).from("TestingType").where("testingTypeId", "TEST-5").queryOne();
        assertEquals("Retrieved value has the correct description", "Testing Type #5", testValue.getString("description"));
        String newValueStr = UtilXml.toXml(testValue);
        GenericValue newValue = (GenericValue) UtilXml.fromXml(newValueStr);
        assertEquals("Retrieved value has the correct description", "Testing Type #5", newValue.getString("description"));
        newValue.put("description", "XML Testing Type #5");
        newValue.store();
        newValue = EntityQuery.use(localDelegator).from("TestingType").where("testingTypeId", "TEST-5").queryOne();
        assertEquals("Retrieved value has the correct description", "XML Testing Type #5", newValue.getString("description"));
        TransactionUtil.rollback(transBegin, null, null);
    }

    protected long flushAndRecreateTree(String descriptionPrefix) throws Exception {
        //
        // The tree has a root, the root has level1max children.
        //

        // create the root
        GenericValue root = delegator.create("TestingNode",
                        "testingNodeId", delegator.getNextSeqId("TestingNode"),
                        "primaryParentNodeId", GenericEntity.NULL_FIELD,
                        "description", descriptionPrefix + ":0:root");
        int level1;
        for (level1 = 0; level1 < _level1max; level1++) {
            String nextSeqId = delegator.getNextSeqId("TestingNode");
            GenericValue v = delegator.create("TestingNode", "testingNodeId", nextSeqId,
                                    "primaryParentNodeId", root.get("testingNodeId"),
                                    "description", descriptionPrefix + ":1:node-level #1");
            assertNotNull(v);
        }
        return level1 + 1;
    }

    /*
     * Tests storing data with the delegator's .create method.  Also tests .findCountByCondition and .getNextSeqId
     */
    public void testCreateTree() throws Exception {
        // get how many child nodes did we have before creating the tree
        delegator.removeByCondition("TestingNode", EntityCondition.makeCondition("description", EntityOperator.LIKE, "create:"));
        long created = flushAndRecreateTree("create");
        long newlyStored = EntityQuery.use(delegator)
                                      .from("TestingNode")
                                      .where(EntityCondition.makeCondition("description", EntityOperator.LIKE, "create:%"))
                                      .queryCount();
        assertEquals("Created/Stored Nodes", created, newlyStored);
    }

    /*
     * More tests of storing data with .storeAll.  Also prepares data for testing view-entities (see below.)
     */
    public void testAddMembersToTree() throws Exception {
        delegator.removeByCondition("TestingType", EntityCondition.makeCondition("testingTypeId", EntityOperator.LIKE, "TEST-TREE-%"));
        GenericValue testValue = EntityQuery.use(delegator).from("TestingType").where("testingTypeId", "TEST-TREE-1").cache(true).queryOne();
        assertNull("No pre-existing type value", testValue);
        delegator.create("TestingType", "testingTypeId", "TEST-TREE-1", "description", "Testing Type #Tree-1");
        // get the level1 nodes
        List<GenericValue> nodeLevel1 = EntityQuery.use(delegator)
                                                   .from("TestingNode")
                                                   .where(EntityCondition.makeCondition("primaryParentNodeId", EntityOperator.NOT_EQUAL, GenericEntity.NULL_FIELD))
                                                   .queryList();

        List<GenericValue> newValues = new LinkedList<>();
        Timestamp now = UtilDateTime.nowTimestamp();

        for (GenericValue node: nodeLevel1) {
            GenericValue testing = delegator.makeValue("Testing",
                            "testingId", delegator.getNextSeqId("Testing"),
                            "testingTypeId", "TEST-TREE-1"
                   );
            testing.put("testingName", "leaf-#" + node.getString("testingNodeId"));
            testing.put("description", "level1 leaf");
            testing.put("comments", "No-comments");
            testing.put("testingSize", Long.valueOf(10));
            testing.put("testingDate", now);

            newValues.add(testing);
            GenericValue member = delegator.makeValue("TestingNodeMember",
                            "testingNodeId", node.get("testingNodeId"),
                            "testingId", testing.get("testingId")
                   );

            member.put("fromDate", now);
            member.put("thruDate", UtilDateTime.getNextDayStart(now));

            newValues.add(member);
        }
        int n = delegator.storeAll(newValues);
        assertEquals("Created/Stored Nodes", newValues.size(), n);
    }

    protected void purgeTestingByTypeId(String likeTypeId) throws GenericEntityException {
        delegator.removeByCondition("Testing", EntityCondition.makeCondition("testingTypeId", EntityOperator.LIKE, likeTypeId));
        delegator.removeByCondition("TestingTest", EntityCondition.makeCondition("testingTypeId", EntityOperator.LIKE, likeTypeId));
    }

    protected void createNodeMembers(String typeId, String typeDescription, String descriptionPrefix) throws GenericEntityException {
        delegator.removeByCondition("TestingType", EntityCondition.makeCondition("testingTypeId", EntityOperator.EQUALS, typeId));
        delegator.create("TestingType", "testingTypeId", typeId, "description", typeDescription);
        int i = 0;
        Timestamp now = UtilDateTime.nowTimestamp();

        for (GenericValue node: EntityQuery.use(delegator)
                                           .from("TestingNode")
                                           .where(EntityCondition.makeCondition("description", EntityOperator.LIKE, descriptionPrefix + "%"))
                                           .queryList()
        ) {
            if (i % 2 == 0) {
                GenericValue testing = delegator.create("Testing", "testingId", descriptionPrefix + ":" + node.get("testingNodeId"), "testingTypeId", typeId, "description", node.get("description"));
                GenericValue member = delegator.makeValue("TestingNodeMember",
                    "testingNodeId", node.get("testingNodeId"),
                    "testingId", testing.get("testingId")
                );

                member.put("fromDate", now);
                member.put("thruDate", UtilDateTime.getNextDayStart(now));
                member.create();
            }
            i++;
        }
    }

    /*
     * Tests findByCondition and tests searching on a view-entity
     */
    public void testCountViews() throws Exception {
        delegator.removeByCondition("Testing", EntityCondition.makeCondition("testingTypeId", EntityOperator.EQUALS, "TEST-COUNT-VIEW"));
        flushAndRecreateTree("count-views");
        createNodeMembers("TEST-COUNT-VIEW", "Testing Type #Count", "count-views");

        EntityCondition isNodeWithMember = EntityCondition.makeCondition(
            EntityCondition.makeCondition("testingId", EntityOperator.NOT_EQUAL, GenericEntity.NULL_FIELD),
            EntityOperator.AND,
            EntityCondition.makeCondition("description", EntityOperator.LIKE, "count-views:%")
        );
        List<GenericValue> nodeWithMembers = EntityQuery.use(delegator).from("TestingNodeAndMember").where(isNodeWithMember).queryList();

        for (GenericValue v: nodeWithMembers) {
            Map<String, Object> fields = v.getAllFields();
            Debug.logInfo("--------------------------", module);
            //      For values of a map
            for (Map.Entry<String, Object> entry: fields.entrySet()) {
                String field = entry.getKey();
                Object value = entry.getValue();
                Debug.logInfo(field + " = " + ((value == null) ? "[null]" : value), module);
            }
        }
        long testingcount = EntityQuery.use(delegator).from("Testing").where("testingTypeId", "TEST-COUNT-VIEW").queryCount();
        assertEquals("Number of views should equal number of created entities in the test.", testingcount, nodeWithMembers.size());
    }

    /*
     * Tests findByCondition and a find by distinct
     */
    public void testFindDistinct() throws Exception {
        delegator.removeByCondition("Testing", EntityCondition.makeCondition("testingTypeId", EntityOperator.LIKE, "TEST-DISTINCT-%"));
        List<GenericValue> testingDistinctList = EntityQuery.use(delegator)
                                                            .from("Testing")
                                                            .where(EntityCondition.makeCondition("testingTypeId", EntityOperator.LIKE, "TEST-DISTINCT-%"))
                                                            .queryList();

        assertEquals("No existing Testing entities for distinct", 0, testingDistinctList.size());
        delegator.removeByCondition("TestingType", EntityCondition.makeCondition("testingTypeId", EntityOperator.LIKE, "TEST-DISTINCT-%"));
        GenericValue testValue = EntityQuery.use(delegator).from("TestingType").where("testingTypeId", "TEST-DISTINCT-1").cache(true).queryOne();
        assertNull("No pre-existing type value", testValue);
        delegator.create("TestingType", "testingTypeId", "TEST-DISTINCT-1", "description", "Testing Type #Distinct-1");
        testValue = EntityQuery.use(delegator).from("TestingType").where("testingTypeId", "TEST-DISTINCT-1").cache(true).queryOne();
        assertNotNull("Found newly created type value", testValue);

        delegator.create("Testing", "testingId", "TEST-DISTINCT-1", "testingTypeId", "TEST-DISTINCT-1", "testingSize", Long.valueOf(10), "comments", "No-comments");
        delegator.create("Testing", "testingId", "TEST-DISTINCT-2", "testingTypeId", "TEST-DISTINCT-1", "testingSize", Long.valueOf(10), "comments", "Some-comments");
        delegator.create("Testing", "testingId", "TEST-DISTINCT-3", "testingTypeId", "TEST-DISTINCT-1", "testingSize", Long.valueOf(9), "comments", "No-comments");
        delegator.create("Testing", "testingId", "TEST-DISTINCT-4", "testingTypeId", "TEST-DISTINCT-1", "testingSize", Long.valueOf(11), "comments", "Some-comments");

        List<GenericValue> testingSize10 = EntityQuery.use(delegator)
                                                      .select("testingSize", "comments")
                                                      .from("Testing")
                                                      .where("testingSize", Long.valueOf(10), "comments", "No-comments")
                                                      .distinct()
                                                      .cache()
                                                      .queryList();
        Debug.logInfo("testingSize10 is " + testingSize10.size(), module);

        assertEquals("There should only be 1 result found by findDistinct()", 1, testingSize10.size());
    }

    /*
     * Tests a findByCondition using not like
     */
    public void testNotLike() throws Exception {
        List<GenericValue> nodes = EntityQuery.use(delegator)
                                              .from("TestingNode")
                                              .where(EntityCondition.makeCondition("description", EntityOperator.NOT_LIKE, "root%"))
                                              .queryList();
        assertNotNull("Found nodes", nodes);

        for (GenericValue product: nodes) {
            String nodeId = product.getString("description");
            Debug.logInfo("Testing name - " + nodeId, module);
            assertFalse("No nodes starting w/ root", nodeId.startsWith("root"));
        }
    }

    /*
     * Tests foreign key integrity by trying to remove an entity which has foreign-key dependencies.  Should cause an exception.
     */
    public void testForeignKeyCreate() {
        try {
            String helperName = delegator.getEntityHelper("Testing").getHelperName();
            Datasource datasourceInfo = EntityConfig.getDatasource(helperName);
            if (!datasourceInfo.getUseForeignKeys()) {
                Debug.logInfo("Datasource " + datasourceInfo.getName() + " use-foreign-keys set to false, skipping testForeignKeyCreate", module);
                return;
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }
        GenericEntityException caught = null;
        try {
            delegator.create("Testing", "testingId", delegator.getNextSeqId("Testing"), "testingTypeId", "NO-SUCH-KEY");
        } catch (GenericEntityException e) {
            caught = e;
        }
        assertNotNull("Foreign key referential integrity is not observed for create (INSERT)", caught);
        Debug.logInfo(caught.toString(), module);
    }

    /*
     * Tests foreign key integrity by trying to remove an entity which has foreign-key dependencies.  Should cause an exception.
     */
    public void testForeignKeyRemove() throws Exception {
        try {
            String helperName = delegator.getEntityHelper("TestingNode").getHelperName();
            Datasource datasourceInfo = EntityConfig.getDatasource(helperName);
            if (!datasourceInfo.getUseForeignKeys()) {
                Debug.logInfo("Datasource " + datasourceInfo.getName() + " use-foreign-keys set to false, skipping testForeignKeyRemove", module);
                return;
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }
        delegator.removeByCondition("TestingNode", EntityCondition.makeCondition("description", EntityOperator.LIKE, "foreign-key-remove #%"));
        delegator.create("TestingNode", "testingNodeId", "TEST-FK-REMOVE-0", "description", "foreign-key-remove #0");
        delegator.create("TestingNode", "testingNodeId", "TEST-FK-REMOVE-1", "primaryParentNodeId", "TEST-FK-REMOVE-0", "description", "foreign-key-remove #1");
        delegator.create("TestingNode", "testingNodeId", "TEST-FK-REMOVE-2", "primaryParentNodeId", "TEST-FK-REMOVE-1", "description", "foreign-key-remove #2");
        delegator.create("TestingNode", "testingNodeId", "TEST-FK-REMOVE-3", "primaryParentNodeId", "TEST-FK-REMOVE-2", "description", "foreign-key-remove #3");
        GenericEntityException caught = null;
        try {
            EntityCondition isLevel1 = EntityCondition.makeCondition("description", EntityOperator.EQUALS, "foreign-key-remove #1");
            delegator.removeByCondition("TestingNode", isLevel1);
        } catch (GenericEntityException e) {
            caught = e;
        }
        assertNotNull("Foreign key referential integrity is not observed for remove (DELETE)", caught);
        Debug.logInfo(caught.toString(), module);
    }

    /*
     * Tests the .getRelatedOne method and removeAll for removing entities
     */
    public void testRemoveNodeMemberAndTesting() throws Exception {
        flushAndRecreateTree("rnmat");
        createNodeMembers("TEST-RNMAT", "remove-node-member-and-testing", "rnmat");
        //
        // Find the testing entities tru the node member and build a list of them
        //
        List<GenericValue> values = EntityQuery.use(delegator)
                                               .from("TestingNodeMember")
                                               .where(EntityCondition.makeCondition("testingId", EntityOperator.LIKE, "rnmat:%"))
                                               .queryList();

        List<GenericValue> testings = new ArrayList<>();

        for (GenericValue nodeMember: values) {
            testings.add(nodeMember.getRelatedOne("Testing", false));
        }
        // and remove the nodeMember afterwards
        delegator.removeAll(values);
        values = EntityQuery.use(delegator)
                            .from("TestingNodeMember")
                            .where(EntityCondition.makeCondition("testingId", EntityOperator.LIKE, "rnmat:%"))
                            .queryList();
        assertEquals("No more Node Member entities", 0, values.size());

        delegator.removeAll(testings);
        values = EntityQuery.use(delegator)
                            .from("Testing")
                            .where(EntityCondition.makeCondition("description", EntityOperator.LIKE, "rnmat:%"))
                            .queryList();
        assertEquals("No more Testing entities", 0, values.size());
    }

    /*
     * Tests the storeByCondition operation
     */
    public void testStoreByCondition() throws Exception {
        flushAndRecreateTree("store-by-condition-a");
        flushAndRecreateTree("store-by-condition-b");
        // change the description of all the level1 nodes
        EntityCondition isLevel1 = EntityCondition.makeCondition("description", EntityOperator.LIKE, "store-by-condition-a:%");
        Map<String, String> fieldsToSet = UtilMisc.toMap("description", "store-by-condition-a:updated");
        delegator.storeByCondition("TestingNode", fieldsToSet, isLevel1);
        List<GenericValue> updatedNodes = EntityQuery.use(delegator).from("TestingNode").where(fieldsToSet).queryList();
        int n = updatedNodes.size();
        assertTrue("testStoreByCondition updated nodes > 0", n > 0);
    }

    /*
     * Tests the .removeByCondition method for removing entities directly
     */
    public void testRemoveByCondition() throws Exception {
        flushAndRecreateTree("remove-by-condition-a");
        //
        // remove all the level1 nodes by using a condition on the description field
        //
        EntityCondition isLevel1 = EntityCondition.makeCondition("description", EntityOperator.LIKE, "remove-by-condition-a:1:%");
        int n = delegator.removeByCondition("TestingNode", isLevel1);
        assertTrue("testRemoveByCondition nodes > 0", n > 0);
    }

    /*
     * Test the .removeByPrimaryKey by using findByCondition and then retrieving the GenericPk from a GenericValue
     */
    public void testRemoveByPK() throws Exception {
        flushAndRecreateTree("remove-by-pk");
        //
        // Find all the root nodes,
        // delete them their primary key
        //
        EntityCondition isRoot = EntityCondition.makeCondition(
            EntityCondition.makeCondition("description", EntityOperator.LIKE, "remove-by-pk:%"),
            EntityOperator.AND,
            EntityCondition.makeCondition("primaryParentNodeId", EntityOperator.NOT_EQUAL, GenericEntity.NULL_FIELD)
        );
        List<GenericValue> rootValues = EntityQuery.use(delegator).select("testingNodeId").from("TestingNode").where(isRoot).queryList();

        for (GenericValue value: rootValues) {
            GenericPK pk = value.getPrimaryKey();
            int del = delegator.removeByPrimaryKey(pk);
            assertEquals("Removing Root by primary key", 1, del);
        }

        // no more TestingNode should be in the data base anymore.

        List<GenericValue> testingNodes = EntityQuery.use(delegator).from("TestingNode").where(isRoot).queryList();
        assertEquals("No more TestingNode after removing the roots", 0, testingNodes.size());
    }

    /*
     * Tests the .removeAll method only.
     */
    public void testRemoveType() throws Exception {
        List<GenericValue> values = EntityQuery.use(delegator).from("TestingRemoveAll").queryList();
        delegator.removeAll(values);
        values = EntityQuery.use(delegator).from("TestingRemoveAll").queryList();
        assertEquals("No more TestingRemoveAll: setup", 0, values.size());
        for (int i = 0; i < 10; i++) {
            delegator.create("TestingRemoveAll", "testingRemoveAllId", "prefix:" + i);
        }
        values = EntityQuery.use(delegator).from("TestingRemoveAll").queryList();
        assertEquals("No more TestingRemoveAll: create", 10, values.size());

        delegator.removeAll(values);

        // now make sure there are no more of these
        values = EntityQuery.use(delegator).from("TestingRemoveAll").queryList();
        assertEquals("No more TestingRemoveAll: finish", 0, values.size());
    }

    /*
     * This test will create a large number of unique items and add them to the delegator at once
     */
    public void testCreateManyAndStoreAtOnce() throws Exception {
        try {
            List<GenericValue> newValues = new LinkedList<>();
            for (int i = 0; i < TEST_COUNT; i++) {
                newValues.add(delegator.makeValue("Testing", "testingId", getTestId("T1-", i)));
            }
            delegator.storeAll(newValues);
            List<GenericValue> newlyCreatedValues = EntityQuery.use(delegator)
                                                               .from("Testing")
                                                               .where(EntityCondition.makeCondition("testingId", EntityOperator.LIKE, "T1-%"))
                                                               .orderBy("testingId")
                                                               .queryList();
            assertEquals("Test to create " + TEST_COUNT + " and store all at once", TEST_COUNT, newlyCreatedValues.size());
        } finally {
            List<GenericValue> newlyCreatedValues = EntityQuery.use(delegator)
                                                               .from("Testing")
                                                               .where(EntityCondition.makeCondition("testingId", EntityOperator.LIKE, "T1-%"))
                                                               .orderBy("testingId")
                                                               .queryList();
            delegator.removeAll(newlyCreatedValues);
        }
    }

    /*
     * This test will create a large number of unique items and add them to the delegator at once
     */
    public void testCreateManyAndStoreOneAtATime() throws Exception {
        try {
            for (int i = 0; i < TEST_COUNT; i++) {
                delegator.create(delegator.makeValue("Testing", "testingId", getTestId("T2-", i)));
            }
            List<GenericValue> newlyCreatedValues = EntityQuery.use(delegator)
                                                               .from("Testing")
                                                               .where(EntityCondition.makeCondition("testingId", EntityOperator.LIKE, "T2-%"))
                                                               .orderBy("testingId")
                                                               .queryList();
            assertEquals("Test to create " + TEST_COUNT + " and store one at a time: ", TEST_COUNT, newlyCreatedValues.size());
        } finally {
            List<GenericValue> newlyCreatedValues = EntityQuery.use(delegator)
                                                               .from("Testing")
                                                               .where(EntityCondition.makeCondition("testingId", EntityOperator.LIKE, "T2-%"))
                                                               .orderBy("testingId")
                                                               .queryList();
            delegator.removeAll(newlyCreatedValues);
        }
    }

    /*
     * This test will use the large number of unique items from above and test the EntityListIterator looping through the list
     */
    public void testEntityListIterator() throws Exception {
        try {
            List<GenericValue> newValues = new LinkedList<>();
            for (int i = 0; i < TEST_COUNT; i++) {
                newValues.add(delegator.makeValue("Testing", "testingId", getTestId("T3-", i)));
            }
            delegator.storeAll(newValues);
            List<GenericValue> newlyCreatedValues = EntityQuery.use(delegator)
                                                               .from("Testing")
                                                               .where(EntityCondition.makeCondition("testingId", EntityOperator.LIKE, "T3-%"))
                                                               .orderBy("testingId")
                                                               .queryList();
            assertEquals("Test to create " + TEST_COUNT + " and store all at once", TEST_COUNT, newlyCreatedValues.size());
            boolean beganTransaction = false;
            try {
                beganTransaction = TransactionUtil.begin();
                EntityListIterator iterator = EntityQuery.use(delegator)
                                                         .from("Testing")
                                                         .where(EntityCondition.makeCondition("testingId", EntityOperator.LIKE, "T3-%"))
                                                         .orderBy("testingId")
                                                         .queryIterator();
                assertNotNull("Test if EntityListIterator was created: ", iterator);

                int i = 0;
                GenericValue item = iterator.next();
                while (item != null) {
                    assertEquals("Testing if iterated data matches test data (row " + i + "): ", getTestId("T3-", i), item.getString("testingId"));
                    item = iterator.next();
                    i++;
                }
                assertEquals("Test if EntitlyListIterator iterates exactly " + TEST_COUNT + " times: " , TEST_COUNT, i);
                iterator.close();
            } catch (GenericEntityException e) {
                TransactionUtil.rollback(beganTransaction, "GenericEntityException occurred while iterating with EntityListIterator", e);
                assertTrue("GenericEntityException:" + e.toString(), false);
                return;
            } finally {
                TransactionUtil.commit(beganTransaction);
            }
        } finally {
            List<GenericValue> entitiesToRemove = EntityQuery.use(delegator)
                                                             .from("Testing")
                                                             .where(EntityCondition.makeCondition("testingId", EntityOperator.LIKE, "T3-%"))
                                                             .queryList();
            delegator.removeAll(entitiesToRemove);
        }
    }

    /*
     * This test will verify transaction rollbacks using TransactionUtil.
     */
    public void testTransactionUtilRollback() throws Exception {
        GenericValue testValue = delegator.makeValue("Testing", "testingId", "rollback-test");
        boolean transBegin = TransactionUtil.begin();
        delegator.create(testValue);
        TransactionUtil.rollback(transBegin, null, null);
        GenericValue testValueOut = EntityQuery.use(delegator).from("Testing").where("testingId", "rollback-test").queryOne();
        assertEquals("Test that transaction rollback removes value: ", null, testValueOut);
    }

    /*
     * This test will verify that a transaction which takes longer than the pre-set timeout are rolled back.
     */
    public void testTransactionUtilMoreThanTimeout() throws Exception {
        GenericTransactionException caught = null;
        try {
            GenericValue testValue = delegator.makeValue("Testing", "testingId", "timeout-test");
            boolean transBegin = TransactionUtil.begin(10); // timeout set to 10 seconds
            delegator.create(testValue);
            Thread.sleep(20*1000);
            TransactionUtil.commit(transBegin);
        } catch (GenericTransactionException e) {
            caught = e;
        } finally {
            assertNotNull("timeout thrown", caught);
            delegator.removeByAnd("Testing", "testingId", "timeout-test");
        }
    }

    /*
     * This test will verify that the same transaction transaction which takes less time than timeout will be committed.
     */
    public void testTransactionUtilLessThanTimeout() throws Exception {
        try {
            GenericValue testValue = delegator.makeValue("Testing", "testingId", "timeout-test");
            boolean transBegin = TransactionUtil.begin();
            TransactionUtil.setTransactionTimeout(20); // now set timeout to 20 seconds
            delegator.create(testValue);
            Thread.sleep(10*1000);
            TransactionUtil.commit(transBegin);
        } finally {
            delegator.removeByAnd("Testing", "testingId", "timeout-test");
        }
    }

    /*
     * Tests field types.
     */
    public void testFieldTypes() throws Exception {
        String id = "testFieldTypes";
        byte[] b = new byte[100000];
        for (int i = 0; i < b.length; i++) {
            b[i] = (byte) i;
        }
        Blob testBlob = new SerialBlob(b);
        String alpha = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        StringBuilder sb = new StringBuilder(alpha.length() * 1000);
        for (int i = 0; i < 1000; i++) {
            sb.append(alpha);
        }
        String clobStr = sb.toString();
        long currentMillis = System.currentTimeMillis();
        Date currentDate = Date.valueOf(new Date(currentMillis).toString());
        Time currentTime = Time.valueOf(new Time(currentMillis).toString());
        // Different databases have different precision for Timestamps, so
        // we will ignore fractional seconds.
        Timestamp currentTimestamp = new Timestamp(currentDate.getTime());
        BigDecimal fixedPoint = new BigDecimal("999999999999.999999");
        // Different databases have different precision for floating
        // point types, so we will use a simple decimal number.
        Double floatingPoint = 1.0123456789;
        Long numeric = Long.MAX_VALUE;
        try {
            GenericValue testValue = delegator.makeValue("TestFieldType", "testFieldTypeId", id);
            testValue.create();
            testValue.set("blobField", testBlob);
            testValue.set("byteArrayField", b);
            testValue.set("objectField", currentTimestamp);
            testValue.set("dateField", currentDate);
            testValue.set("timeField", currentTime);
            testValue.set("dateTimeField", currentTimestamp);
            testValue.set("fixedPointField", fixedPoint);
            testValue.set("floatingPointField", floatingPoint);
            testValue.set("numericField", numeric);
            testValue.set("clobField", clobStr);
            testValue.store();
            testValue = EntityQuery.use(delegator).from("TestFieldType").where("testFieldTypeId", id).queryOne();
            assertEquals("testFieldTypeId", id, testValue.get("testFieldTypeId"));
            Blob blob = (Blob) testValue.get("blobField");
            byte[] c = blob.getBytes(1, (int) blob.length());
            assertEquals("Byte array read from entity is the same length", b.length, c.length);
            for (int i = 0; i < b.length; i++) {
                assertEquals("Byte array data[" + i + "]", b[i], c[i]);
            }
            c = (byte[]) testValue.get("byteArrayField");
            assertEquals("Byte array read from entity is the same length", b.length, c.length);
            for (int i = 0; i < b.length; i++) {
                assertEquals("Byte array data[" + i + "]", b[i], c[i]);
            }
            assertEquals("objectField", currentTimestamp, testValue.get("objectField"));
            assertEquals("dateField", currentDate, testValue.get("dateField"));
            assertEquals("timeField", currentTime, testValue.get("timeField"));
            assertEquals("dateTimeField", currentTimestamp, testValue.get("dateTimeField"));
            assertEquals("fixedPointField", fixedPoint, testValue.get("fixedPointField"));
            assertEquals("floatingPointField", floatingPoint, testValue.get("floatingPointField"));
            assertEquals("numericField", numeric, testValue.get("numericField"));
            assertEquals("clobField", clobStr, testValue.get("clobField"));
            testValue.set("blobField", null);
            testValue.set("byteArrayField", null);
            testValue.set("objectField", null);
            testValue.set("dateField", null);
            testValue.set("timeField", null);
            testValue.set("dateTimeField", null);
            testValue.set("fixedPointField", null);
            testValue.set("floatingPointField", null);
            testValue.set("numericField", null);
            testValue.set("clobField", null);
            testValue.store();
            testValue = EntityQuery.use(delegator).from("TestFieldType").where("testFieldTypeId", id).queryOne();
            assertEquals("testFieldTypeId", id, testValue.get("testFieldTypeId"));
            assertNull("blobField null", testValue.get("blobField"));
            assertNull("byteArrayField null", testValue.get("byteArrayField"));
            assertNull("objectField null", testValue.get("objectField"));
            assertNull("dateField null", testValue.get("dateField"));
            assertNull("timeField null", testValue.get("timeField"));
            assertNull("dateTimeField null", testValue.get("dateTimeField"));
            assertNull("fixedPointField null", testValue.get("fixedPointField"));
            assertNull("floatingPointField null", testValue.get("floatingPointField"));
            assertNull("numericField null", testValue.get("numericField"));
            assertNull("clobField null", testValue.get("clobField"));
        } finally {
            // Remove all our newly inserted values.
            List<GenericValue> values = EntityQuery.use(delegator).from("TestFieldType").queryList();
            delegator.removeAll(values);
        }
    }

    /*
     * This creates an string id from a number
     */
    private String getTestId(String strTestBase, int iNum) {
        StringBuilder strBufTemp = new StringBuilder(strTestBase);
        if (iNum < 10000) {
           strBufTemp.append("0");
        }
        if (iNum < 1000) {
           strBufTemp.append("0");
        }
        if (iNum < 100) {
           strBufTemp.append("0");
        }
        if (iNum < 10) {
           strBufTemp.append("0");
        }
        strBufTemp.append(iNum);
        return strBufTemp.toString();
    }


    /*
     * This test will verify that the LIMIT and OFFSET options can work properly.
     * Commented out because it makes the framework dependent on the content component
     */
    /*public void testLimitOffsetOptions() throws Exception {
        String entityName = "Content";
        Datasource datasourceInfo = EntityConfig.getDatasource(delegator.getEntityHelper(entityName).getHelperName());
        if (UtilValidate.isEmpty(datasourceInfo.offsetStyle) || "none".equals(datasourceInfo.offsetStyle)) {
            Debug.logInfo("The offset-stype configured in datasource is " + datasourceInfo.offsetStyle +  ", this test is skipped.", module);
            return;
        } else {
            Debug.logInfo("The offset-stype configured in datasource is " + datasourceInfo.offsetStyle +  ".", module);
        }
        try {
            EntityFindOptions findOptions = new EntityFindOptions();
            long count = delegator.findCountByCondition("Content", null, null, null);
            Debug.logInfo("Content entity has " + count + " rows", module);
            int rowsPerPage = 10;
            // use rows/page as limit option
            findOptions.setLimit(rowsPerPage);
            int pages = (int) count/rowsPerPage;
            if (count > pages * rowsPerPage) {
                pages += 1;
            }
            Debug.logInfo("These rows will be displayed in " + pages + " pages, each page has " + rowsPerPage + " rows.", module);
            ModelEntity modelEntity = delegator.getModelEntity(entityName);

            long start = UtilDateTime.nowTimestamp().getTime();
            for (int page = 1; page <= pages; page++) {
                Debug.logInfo("Page " + page + ":", module);
                // set offset option
                findOptions.setOffset((page - 1) * rowsPerPage);
                EntityListIterator iterator = null;
                try {
                    iterator = delegator.getEntityHelper(entityName).findListIteratorByCondition(modelEntity, null, null, null, UtilMisc.toList("lastUpdatedStamp DESC"), findOptions);
                    while (iterator != null) {
                        GenericValue gv = iterator.next();
                        if (gv == null) {
                            break;
                        }
                        Debug.logInfo(gv.getString("contentId") + ": " + gv.getString("contentName") + "       (updated: " + gv.getTimestamp("lastUpdatedStamp") + ")", module);
                    }
                } catch (GenericEntityException e) {
                    Debug.logError(e, module);
                } finally {
                    if (iterator != null) {
                        iterator.close();
                    }
                }
            }
            long end = UtilDateTime.nowTimestamp().getTime();
            long time1 = end - start;
            Debug.logInfo("Time consumed WITH limit and offset option (ms): " + time1, module);

            start = UtilDateTime.nowTimestamp().getTime();
            for (int page = 1; page <= pages; page++) {
                Debug.logInfo("Page " + page + ":", module);
                EntityListIterator iterator = null;
                try {
                    iterator = ((GenericHelperDAO) delegator.getEntityHelper(entityName)).findListIteratorByCondition(modelEntity, null, null, null, UtilMisc.toList("lastUpdatedStamp DESC"), null);
                    if (iterator == null) {
                        continue;
                    }
                    iterator.setDelegator(delegator);
                    List<GenericValue> gvs = iterator.getCompleteList();
                    int fromIndex = (page - 1) * rowsPerPage;
                    int toIndex = fromIndex + rowsPerPage;
                    if (toIndex > count) {
                        toIndex = (int) count;
                    }
                    gvs = gvs.subList(fromIndex, toIndex);
                    for (GenericValue gv : gvs) {
                        Debug.logInfo(gv.getString("contentId") + ": " + gv.getString("contentName") + "       (updated: " + gv.getTimestamp("lastUpdatedStamp") + ")", module);
                    }
                } catch (GenericEntityException e) {
                    Debug.logError(e, module);
                } finally {
                    if (iterator != null) {
                        iterator.close();
                    }
                }
            }
            end = UtilDateTime.nowTimestamp().getTime();
            long time2 = end - start;
            Debug.logInfo("Time consumed WITHOUT limit and offset option (ms): " + time2, module);
            Debug.logInfo("Time saved (ms): " + (time2 - time1), module);
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }
    }*/

    /*
     * Tests EntitySaxReader, verification loading data with tag create, create-update, create-replace, delete
     */
    public void testEntitySaxReaderCreation() throws Exception {
        String xmlContentLoad =
                "<entity-engine-xml>" +
                "<TestingType testingTypeId=\"JUNIT-TEST\" description=\"junit test\"/>" +
                "<create>" +
                "    <TestingType testingTypeId=\"JUNIT-TEST2\" description=\"junit test\"/>" +
                "    <Testing testingId=\"T1\" testingTypeId=\"JUNIT-TEST\" testingName=\"First test\" testingSize=\"10\" testingDate=\"2010-01-01 00:00:00\"/>" +
                "</create>" +
                "<Testing testingId=\"T2\" testingTypeId=\"JUNIT-TEST2\" testingName=\"Second test\" testingSize=\"20\" testingDate=\"2010-02-01 00:00:00\"/>" +
                "</entity-engine-xml>";
        EntitySaxReader reader = new EntitySaxReader(delegator);
        long numberLoaded = reader.parse(xmlContentLoad);
        assertEquals("Create Entity loaded ", 4, numberLoaded);
        GenericValue t1 = EntityQuery.use(delegator).from("Testing").where("testingId", "T1").queryOne();
        GenericValue t2 = EntityQuery.use(delegator).from("Testing").where("testingId", "T2").cache(true).queryOne();
        assertNotNull("Create Testing(T1)", t1);
        assertEquals("Create Testing(T1).testingTypeId", "JUNIT-TEST", t1.getString("testingTypeId"));
        assertEquals("Create Testing(T1).testingName", "First test", t1.getString("testingName"));
        assertEquals("Create Testing(T1).testingSize", Long.valueOf(10), t1.getLong("testingSize"));
        assertEquals("Create Testing(T1).testingDate", UtilDateTime.toTimestamp("01/01/2010 00:00:00"), t1.getTimestamp("testingDate"));

        assertNotNull("Create Testing(T2)", t2);
        assertEquals("Create Testing(T2).testingTypeId", "JUNIT-TEST2", t2.getString("testingTypeId"));
        assertEquals("Create Testing(T2).testingName", "Second test", t2.getString("testingName"));
        assertEquals("Create Testing(T2).testingSize", Long.valueOf(20), t2.getLong("testingSize"));
        assertEquals("Create Testing(T2).testingDate", UtilDateTime.toTimestamp("02/01/2010 00:00:00"), t2.getTimestamp("testingDate"));
    }

    public void testEntitySaxReaderCreateSkip() throws Exception {
        String xmlContentLoad =
                "<entity-engine-xml>" +
                "<TestingType testingTypeId=\"reader-create-skip\" description=\"reader create skip\"/>" +
                "<Testing testingId=\"reader-create-skip\" testingTypeId=\"reader-create-skip\" testingName=\"reader create skip\" testingSize=\"10\" testingDate=\"2010-01-01 00:00:00\"/>" +
                "</entity-engine-xml>";
        EntitySaxReader reader = new EntitySaxReader(delegator);
        long numberLoaded = reader.parse(xmlContentLoad);
        xmlContentLoad =
                "<create>" +
                "    <Testing testingId=\"reader-create-skip\" testingName=\"reader create skip updated\" testingSize=\"20\" testingDate=\"2012-02-02 02:02:02\"/>" +
                "</create>";
        reader = new EntitySaxReader(delegator);
        numberLoaded += reader.parse(xmlContentLoad);
        assertEquals("Create Skip Entity loaded ", 3, numberLoaded);
        GenericValue t1 = EntityQuery.use(delegator).from("Testing").where("testingId", "reader-create-skip").queryOne();
        assertNotNull("Create Skip Testing(T1)", t1);
        assertEquals("Create Skip Testing(T1).testingTypeId", "reader-create-skip", t1.getString("testingTypeId"));
        assertEquals("Create Skip Testing(T1).testingName", "reader create skip", t1.getString("testingName"));
        assertEquals("Create Skip Testing(T1).testingSize", Long.valueOf(10), t1.getLong("testingSize"));
        assertEquals("Create Skip Testing(T1).testingDate", UtilDateTime.toTimestamp("01/01/2010 00:00:00"), t1.getTimestamp("testingDate"));
    }

    public void testEntitySaxReaderUpdate() throws Exception {
        String xmlContentLoad =
                "<entity-engine-xml>" +
                "<TestingType testingTypeId=\"create-update\" description=\"create update\"/>" +
                "<TestingType testingTypeId=\"create-updated\" description=\"create update updated\"/>" +
                "<Testing testingId=\"create-update-T3\" testingTypeId=\"create-update\" testingName=\"Test 3\" testingSize=\"10\" testingDate=\"2010-01-01 00:00:00\"/>" +
                "<create-update>" +
                "    <Testing testingId=\"create-update-T1\" testingTypeId=\"create-update\" testingName=\"First test update\" testingSize=\"20\" testingDate=\"2010-01-01 00:00:00\"/>" +
                "    <Testing testingId=\"create-update-T3\" testingTypeId=\"create-updated\" testingName=\"Third test\" testingSize=\"30\" testingDate=\"2010-03-01 00:00:00\"/>" +
                "</create-update>" +
                "</entity-engine-xml>";
        EntitySaxReader reader = new EntitySaxReader(delegator);
        long numberLoaded = reader.parse(xmlContentLoad);
        assertEquals("Update Entity loaded ", 5, numberLoaded);
        GenericValue t1 = EntityQuery.use(delegator).from("Testing").where("testingId", "create-update-T1").queryOne();
        GenericValue t3 = EntityQuery.use(delegator).from("Testing").where("testingId", "create-update-T3").queryOne();
        assertNotNull("Update Testing(T1)", t1);
        assertEquals("Update Testing(T1).testingTypeId", "create-update", t1.getString("testingTypeId"));
        assertEquals("Update Testing(T1).testingName", "First test update", t1.getString("testingName"));
        assertEquals("Update Testing(T1).testingSize", Long.valueOf(20), t1.getLong("testingSize"));
        assertEquals("Update Testing(T1).testingDate", UtilDateTime.toTimestamp("01/01/2010 00:00:00"), t1.getTimestamp("testingDate"));

        assertNotNull("Update Testing(T3)", t3);
        assertEquals("Update Testing(T3).testingTypeId", "create-updated", t3.getString("testingTypeId"));
        assertEquals("Update Testing(T3).testingName", "Third test", t3.getString("testingName"));
        assertEquals("Update Testing(T3).testingSize", Long.valueOf(30), t3.getLong("testingSize"));
        assertEquals("Update Testing(T3).testingDate", UtilDateTime.toTimestamp("03/01/2010 00:00:00"), t3.getTimestamp("testingDate"));
    }

    public void testEntitySaxReaderReplace() throws Exception {
        String xmlContentLoad =
                "<entity-engine-xml>" +
                "<TestingType testingTypeId=\"create-replace\" description=\"reader create skip\"/>" +
                "<Testing testingTypeId=\"create-replace\" testingId=\"create-replace-T1\" testingName=\"First test\" testingSize=\"10\" testingDate=\"2010-01-01 00:00:00\"/>" +
                "<create-replace>" +
                "    <Testing testingTypeId=\"create-replace\" testingId=\"create-replace-T1\" testingName=\"First test replace\" />" +
                "</create-replace>" +
                "<Testing testingTypeId=\"create-replace\" testingId=\"create-replace-T2\" testingName=\"Second test update\" testingSize=\"20\" testingDate=\"2010-02-01 00:00:00\"/>" +
                "</entity-engine-xml>";
        EntitySaxReader reader = new EntitySaxReader(delegator);
        long numberLoaded = reader.parse(xmlContentLoad);
        assertEquals("Replace Entity loaded ", 4, numberLoaded);
        GenericValue t1 = EntityQuery.use(delegator).from("Testing").where("testingId", "create-replace-T1").queryOne();
        GenericValue t2 = EntityQuery.use(delegator).from("Testing").where("testingId", "create-replace-T2").queryOne();
        assertNotNull("Replace Testing(T1)", t1);
        assertEquals("Replace Testing(T1).testingTypeId", "create-replace", t1.getString("testingTypeId"));
        assertEquals("Replace Testing(T1).testingName", "First test replace", t1.getString("testingName"));
        assertNull("Replace Testing(T1).testingSize", t1.getLong("testingSize"));
        assertNull("Replace Testing(T1).testingDate", t1.getTimestamp("testingDate"));

        assertNotNull("Replace Testing(T2)", t2);
        assertEquals("Replace Testing(T2).testingTypeId", "create-replace", t2.getString("testingTypeId"));
        assertEquals("Replace Testing(T2).testingName", "Second test update", t2.getString("testingName"));
        assertEquals("Replace Testing(T2).testingSize", Long.valueOf(20), t2.getLong("testingSize"));
        assertEquals("Replace Testing(T2).testingDate", UtilDateTime.toTimestamp("02/01/2010 00:00:00"), t2.getTimestamp("testingDate"));
    }

    public void testEntitySaxReaderDelete() throws Exception {
        String xmlContentLoad =
                        "<delete>" +
                        "    <Testing testingId=\"T1\"/>" +
                        "    <Testing testingId=\"T2\"/>" +
                        "    <Testing testingId=\"T3\"/>" +
                        "    <TestingType testingTypeId=\"JUNIT-TEST\"/>" +
                        "    <TestingType testingTypeId=\"JUNIT-TEST2\"/>" +
                        "</delete>";
        EntitySaxReader reader = new EntitySaxReader(delegator);
        long numberLoaded = reader.parse(xmlContentLoad);
        assertEquals("Delete Entity loaded ", 5, numberLoaded);
        GenericValue t1 = EntityQuery.use(delegator).from("Testing").where("testingId", "T1").queryOne();
        GenericValue t2 = EntityQuery.use(delegator).from("Testing").where("testingId", "T2").queryOne();
        GenericValue t3 = EntityQuery.use(delegator).from("Testing").where("testingId", "T3").queryOne();
        assertNull("Delete Testing(T1)", t1);
        assertNull("Delete Testing(T2)", t2);
        assertNull("Delete Testing(T3)", t3);
        GenericValue testType = EntityQuery.use(delegator).from("TestingType").where("testingTypeId", "JUNIT-TEST").queryOne();
        assertNull("Delete TestingType 1", testType);
        testType = EntityQuery.use(delegator).from("TestingType").where("testingTypeId", "JUNIT-TEST2").queryOne();
        assertNull("Delete TestingType 2", testType);
    }

    public void testSequenceValueItem() {
        SequenceUtil sequencer = new SequenceUtil(delegator.getGroupHelperInfo(delegator.getEntityGroupName("SequenceValueItem")),
                                                  delegator.getModelEntity("SequenceValueItem"),
                                                  "seqName", "seqId");
        UUID id = UUID.randomUUID();
        String sequenceName = "BogusSequence" + id.toString();
        for (int i = 10000; i <= 10015; i++) {
            Long seqId = sequencer.getNextSeqId(sequenceName, 1, null);
            assertEquals(i, seqId.longValue());
        }
        sequencer.forceBankRefresh(sequenceName, 1);
        Long seqId = sequencer.getNextSeqId(sequenceName, 1, null);
        assertEquals(10020, seqId.longValue());
    }

    public void testSequenceValueItemWithConcurrentThreads() {
        final SequenceUtil sequencer = new SequenceUtil(delegator.getGroupHelperInfo(delegator.getEntityGroupName("SequenceValueItem")),
                                                  delegator.getModelEntity("SequenceValueItem"),
                                                  "seqName", "seqId");
        UUID id = UUID.randomUUID();
        final String sequenceName = "BogusSequence" + id.toString();
        final ConcurrentMap<Long, Long> seqIds = new ConcurrentHashMap<>();
        final AtomicBoolean duplicateFound = new AtomicBoolean(false);
        final AtomicBoolean nullSeqIdReturned = new AtomicBoolean(false);

        List<Future<Void>> futures = new ArrayList<>();
        Callable<Void> getSeqIdTask = new Callable<Void>() {
                    public Void call() throws Exception {
                        Long seqId = sequencer.getNextSeqId(sequenceName, 1, null);
                        if (seqId == null) {
                            nullSeqIdReturned.set(true);
                            return null;
                        }
                        Long existingValue = seqIds.putIfAbsent(seqId, seqId);
                        if (existingValue != null) {
                            duplicateFound.set(true);
                        }
                        return null;
                    }
                };
        Callable<Void> refreshTask = new Callable<Void>() {
                            public Void call() throws Exception {
                                sequencer.forceBankRefresh(sequenceName, 1);
                                return null;
                            }
                        };
        double probabilityOfRefresh = 0.1;
        for (int i = 1; i <= 1000; i++) {
            Callable<Void> randomTask = Math.random() < probabilityOfRefresh ? refreshTask : getSeqIdTask;
            futures.add(ExecutionPool.GLOBAL_FORK_JOIN.submit(randomTask));
        }
        long startTime = System.currentTimeMillis();
        ExecutionPool.getAllFutures(futures);
        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        Debug.logInfo("testSequenceValueItemWithConcurrentThreads total time (ms): " + totalTime, module);
        assertFalse("Null sequence id returned", nullSeqIdReturned.get());
        assertFalse("Duplicate sequence id returned", duplicateFound.get());
    }

    /*
        This test is useful to confirm that the default setting of use-transaction="true" for screen definitions is
        the best one for performance.
        With this setting one database transaction is started by the framework before rendering the screen.
        Most screens usually have multiple Delegator calls to find records.
        In most of the GenericDelegator's find methods, a transaction is created if one does not already exist,
        the statement is performed, and the transaction is committed.
        So, by making that attribute "false" - a screen will have many individual transactions instead of just one.

        This test assess that running the same number of sql statements withing one transaction is faster than running
        them with individual transactions.
     */
    public void testOneBigTransactionIsFasterThanSeveralSmallOnes() {
        boolean transactionStarted = false;
        long startTime = System.currentTimeMillis();
        int totalNumberOfRows = 0;
        int numberOfQueries = 500;
        boolean noErrors = true;
        try {
            transactionStarted = TransactionUtil.begin();
            for (int i = 1; i <= numberOfQueries; i++) {
                List rows = EntityQuery.use(delegator).from("SequenceValueItem").queryList();
                totalNumberOfRows = totalNumberOfRows + rows.size();
            }
            TransactionUtil.commit(transactionStarted);
        } catch (GenericEntityException e) {
            try {
                TransactionUtil.rollback(transactionStarted, "", e);
            } catch (GenericTransactionException e2) {
            }
            noErrors = false;
        }
        long endTime = System.currentTimeMillis();
        long totalTimeOneTransaction = endTime - startTime;
        Debug.logInfo("Selected " + totalNumberOfRows + " rows with " + numberOfQueries + " queries (all contained in one big transaction) in " + totalTimeOneTransaction + " ms", module);
        assertTrue("Errors detected executing the big transaction", noErrors);

        totalNumberOfRows = 0;
        noErrors = true;
        try {
            for (int i = 1; i <= numberOfQueries; i++) {
                transactionStarted = TransactionUtil.begin();
                List rows = EntityQuery.use(delegator).from("SequenceValueItem").queryList();
                totalNumberOfRows = totalNumberOfRows + rows.size();
                TransactionUtil.commit(transactionStarted);
            }
        } catch (GenericEntityException e) {
            try {
                TransactionUtil.rollback(transactionStarted, "", e);
            } catch (Exception e2) {}
            noErrors = false;
        }
        endTime = System.currentTimeMillis();
        long totalTimeSeveralSmallTransactions = endTime - startTime;
        Debug.logInfo("Selected " + totalNumberOfRows + " rows with " + numberOfQueries + " queries (each in its own transaction) in " + totalTimeSeveralSmallTransactions + " ms", module);
        assertTrue("Errors detected executing the small transactions", noErrors);
        assertTrue("One big transaction was not faster than several small ones", totalTimeOneTransaction < totalTimeSeveralSmallTransactions);
    }

    private final class TestObserver implements Observer {
        private Observable observable;
        private Object arg;

        @Override
        public void update(Observable observable, Object arg) {
            this.observable = observable;
            this.arg = arg;
        }
    }
}
