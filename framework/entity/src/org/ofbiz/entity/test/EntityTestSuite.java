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
package org.ofbiz.entity.test;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.lang.Thread;

import junit.framework.TestCase;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntity;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericPK;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityConditionList;
import org.ofbiz.entity.condition.EntityExpr;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityFindOptions;
import org.ofbiz.entity.util.EntityListIterator;
import org.ofbiz.entity.transaction.GenericTransactionException;
import org.ofbiz.entity.transaction.TransactionUtil;

public class EntityTestSuite extends TestCase {

    public static final String module = EntityTestSuite.class.getName();
    public static final String DELEGATOR_NAME = "test";
    public GenericDelegator delegator = null;
    /*
     * This sets how many values to insert when trying to create a large number of values.  10,000 causes HSQL to crash but is ok
     * with Derby.  Going up to 100,000 causes problems all around because Java List seems to be capped at about 65,000 values.
     * 
     * NOTE: setting this lower so that the general tests don't take so long to run; to really push it can increase this number.
     */
    public static final long TEST_COUNT = 1000;

    public EntityTestSuite(String name) {
        super(name);
    }

    final static private int _level1max = 3;   // number of TestingNode entities to create

    protected void setUp() throws Exception {
        this.delegator = GenericDelegator.getGenericDelegator(DELEGATOR_NAME);
    }
    
    /*
     * Tests storing values with the delegator's .create, .makeValue, and .storeAll methods
     */
    public void testMakeValue() throws Exception {
        try {
            // This method call directly stores a new value into the entity engine
            delegator.create("TestingType", UtilMisc.toMap("testingTypeId", "TEST-1", "description", "Testing Type #1"));

            // This sequence creates the GenericValue entities first, puts them in a List, then calls the delegator to store them all
            List newValues = new LinkedList();

            newValues.add(delegator.makeValue("TestingType", UtilMisc.toMap("testingTypeId", "TEST-2", "description", "Testing Type #2")));
            newValues.add(delegator.makeValue("TestingType", UtilMisc.toMap("testingTypeId", "TEST-3", "description", "Testing Type #3")));
            newValues.add(delegator.makeValue("TestingType", UtilMisc.toMap("testingTypeId", "TEST-4", "description", "Testing Type #4")));
            delegator.storeAll(newValues);

            // finds a List of newly created values.  the second parameter specifies the fields to order results by.
            List newlyCreatedValues = delegator.findAll("TestingType", UtilMisc.toList("testingTypeId"));
            TestCase.assertEquals("4 TestingTypes found", 4, newlyCreatedValues.size());
        } catch (GenericEntityException ex) {
            TestCase.fail(ex.getMessage());
        }
    }

    /*
     * Tests updating entities by doing a GenericValue .put(key, value) and .store()
     */
    public void testUpdateValue() throws Exception {
        try {

            // retrieve a sample GenericValue, make sure it's correct
            GenericValue testValue = delegator.findByPrimaryKey("TestingType", UtilMisc.toMap("testingTypeId", "TEST-1"));
            TestCase.assertEquals("Retrieved value has the correct description", testValue.getString("description"), "Testing Type #1");

            // now update and store it
            testValue.put("description", "New Testing Type #1");
            testValue.store();

            // now retrieve it again and make sure that the updated value is correct
            testValue = delegator.findByPrimaryKey("TestingType", UtilMisc.toMap("testingTypeId", "TEST-1"));
            TestCase.assertEquals("Retrieved value has the correct description", testValue.getString("description"), "New Testing Type #1");

        } catch (GenericEntityException ex) {
            TestCase.fail(ex.getMessage());
        }
    }

    /*
     * Tests storing data with the delegator's .create method.  Also tests .findCountByCondition and .getNextSeqId
     */
    public void testCreateTree() throws Exception {
        try {
        // get how many child nodes did we have before creating the tree
        EntityCondition isChild = new EntityExpr("primaryParentNodeId", EntityOperator.NOT_EQUAL, GenericEntity.NULL_FIELD);
        long alreadyStored = delegator.findCountByCondition("TestingNode", isChild, null);

        //
        // The tree has a root, the root has level1max children.
        //

        // create the root
        GenericValue root = delegator.create("TestingNode",
                UtilMisc.toMap(
                        "testingNodeId", delegator.getNextSeqId("testingNodeId"),
                        "primaryParentNodeId", GenericEntity.NULL_FIELD,
                        "description", "root")
                );
        int level1;
        for(level1 = 0; level1 < _level1max; level1++) {
            String nextSeqId = delegator.getNextSeqId("testingNodeId");
            GenericValue v =
                delegator.create("TestingNode",
                    UtilMisc.toMap("testingNodeId", nextSeqId,
                                    "primaryParentNodeId", (String)root.get("testingNodeId"),
                                    "description", "node-level #1")
                                );
        }

        long created = level1;
        long newlyStored = delegator.findCountByCondition("TestingNode", isChild, null);

        // Normally, newlyStored = alreadyStored + created
        TestCase.assertEquals("Created/Stored Nodes", newlyStored, created + alreadyStored);
        } catch(GenericEntityException e) {
            Debug.logInfo(e.getMessage(), module);
        }
    }

    /*
     * More tests of storing data with .storeAll.  Also prepares data for testing view-entities (see below.)
     */
    public void testAddMembersToTree() throws Exception {
        // get the level1 nodes
        EntityCondition isLevel1 = new EntityExpr("primaryParentNodeId", EntityOperator.NOT_EQUAL, GenericEntity.NULL_FIELD);
        List nodeLevel1 = delegator.findByCondition("TestingNode", isLevel1, null, null);

        List newValues = new LinkedList();
        Timestamp now = UtilDateTime.nowTimestamp();

        Iterator nodeIterator = nodeLevel1.iterator();
        while(nodeIterator.hasNext()) {
            GenericValue node = (GenericValue)nodeIterator.next();
            GenericValue testing = delegator.makeValue("Testing",
                    UtilMisc.toMap(
                            "testingId", delegator.getNextSeqId("testing"),
                            "testingTypeId", "TEST-1"
                            )
                    );
            testing.put("testingName", "leaf-#" + node.getString("testingNodeId"));
            testing.put("description", "level1 leaf");
            testing.put("comments", "No-comments");
            testing.put("testingSize", new Long(10));
            testing.put("testingDate", now);

            newValues.add(testing);
            GenericValue member = delegator.makeValue("TestingNodeMember",
                    UtilMisc.toMap(
                            "testingNodeId", node.get("testingNodeId"),
                            "testingId", testing.get("testingId")
                            )
                    );

            member.put("fromDate", now);
            member.put("thruDate", UtilDateTime.getNextDayStart(now));

            newValues.add(member);
        }
        int n = delegator.storeAll(newValues);
        TestCase.assertEquals("Created/Stored Nodes", n, newValues.size());
    }

    /*
     * Tests findByCondition and tests searching on a view-entity
     */
    public void testCountViews() throws Exception {
        EntityCondition isNodeWithMember = new EntityExpr("testingId", EntityOperator.NOT_EQUAL, GenericEntity.NULL_FIELD);
        List nodeWithMembers = delegator.findByCondition("TestingNodeAndMember", isNodeWithMember, null, null);

        Iterator it;
        it = nodeWithMembers.iterator();

        while(it.hasNext()) {
            GenericValue v = (GenericValue)it.next();
            Map fields = v.getAllFields();
            Debug.logInfo("--------------------------", module);
            //      For values of a map
            for(Iterator it1 = fields.keySet().iterator(); it1.hasNext(); ) {
                Object field = it1.next();
                Object value = fields.get(field);
                Debug.logInfo(field.toString() + " = " + ((value == null) ? "[null]" : value.toString()), module);
            }
        }
        long testingcount = delegator.findCountByCondition("Testing", null, null);
        TestCase.assertEquals("Number of views should equal number of created entities in the test.", nodeWithMembers.size(), testingcount);
    }

    /*
     * Tests findByCondition and a find by distinct
     */
    public void testFindDistinct() throws Exception {
        List exprList = UtilMisc.toList(
                new EntityExpr("testingSize", EntityOperator.EQUALS, new Long(10)),
                new EntityExpr("comments", EntityOperator.EQUALS, "No-comments")
                );
        EntityConditionList condition = new EntityConditionList(exprList, EntityOperator.AND);

        EntityFindOptions findOptions = new EntityFindOptions();
        findOptions.setDistinct(true);

        List testingSize10 = delegator.findByCondition("Testing", condition, null, UtilMisc.toList("testingSize", "comments"), null, findOptions);
        Debug.logInfo("testingSize10 is " + testingSize10.size(), module);

        TestCase.assertEquals("There should only be 1 result found by findDistinct()", testingSize10.size(), 1);
    }

    /*
     * Tests a findByCondition using not like
     */
    public void testNotLike() throws Exception {
        EntityCondition cond  = new EntityExpr("description", EntityOperator.NOT_LIKE, "root%");
        List nodes = delegator.findByCondition("TestingNode", cond, null, null);
        TestCase.assertTrue("Found nodes", nodes != null);

        Iterator i = nodes.iterator();
        while (i.hasNext()) {
            GenericValue product = (GenericValue) i.next();
            String nodeId = product.getString("description");
            Debug.logInfo("Testing name - " + nodeId, module);
            TestCase.assertTrue("No nodes starting w/ root", !nodeId.startsWith("root"));
        }
    }

    /*
     * Tests foreign key integrity by trying to remove an entity which has foreign-key dependencies.  Should cause an exception.
     */
    public void testForeignKeyCreate() throws Exception {
        try {
            delegator.create("Testing", UtilMisc.toMap("testingId", delegator.getNextSeqId("Testing"), "testingTypeId", "NO-SUCH-KEY"));
        } catch(GenericEntityException e) {
            Debug.logInfo(e.toString(), module);
            return;
        }
        TestCase.fail("Foreign key referential integrity is not observed for create (INSERT)");
    }

    /*
     * Tests foreign key integrity by trying to remove an entity which has foreign-key dependencies.  Should cause an exception.
     */
    public void testForeignKeyRemove() throws Exception {
        try {
            EntityCondition isLevel1 = new EntityExpr("description", EntityOperator.EQUALS, "node-level #1");
            delegator.removeByCondition("TestingNode", isLevel1);
        } catch(GenericEntityException e) {
            Debug.logInfo(e.toString(), module);
            return;
        }
        TestCase.fail("Foreign key referential integrity is not observed for remove (DELETE)");
    }

    /*
     * Tests the .getRelatedOne method and removeAll for removing entities
     */
    public void testRemoveNodeMemberAndTesting() throws Exception {
            //
            // Find the testing entities tru the node member and build a list of them
            //
            List values = delegator.findAll("TestingNodeMember");
            Iterator i = values.iterator();

            ArrayList testings = new ArrayList();

            while(i.hasNext()) {
                GenericValue nodeMember = (GenericValue)i.next();
                testings.add(nodeMember.getRelatedOne("Testing"));
            }
            // and remove the nodeMember afterwards
            delegator.removeAll(values);
            values = delegator.findAll("TestingNodeMember");
            TestCase.assertTrue("No more Node Member entities", values.size() == 0);

            delegator.removeAll(testings);
            values = delegator.findAll("Testing");
            TestCase.assertTrue("No more Testing entities", values.size() == 0);
    }

    /*
     * Tests the storeByCondition operation
     */
    public void testStoreByCondition() throws Exception {
        // change the description of all the level1 nodes
        EntityCondition isLevel1 = new EntityExpr("description", EntityOperator.EQUALS, "node-level #1");
        Map fieldsToSet = UtilMisc.toMap("description", "node-level #1 (updated)");
        int n = 0;
        try {
            delegator.storeByCondition("TestingNode", fieldsToSet, isLevel1);
            List updatedNodes = delegator.findByAnd("TestingNode", fieldsToSet);
            n = updatedNodes.size();
        } catch (GenericEntityException e) {
            TestCase.fail("testStoreByCondition threw an exception");
        }

        TestCase.assertTrue("testStoreByCondition updated nodes > 0", n > 0);
    }

    /*
     * Tests the .removeByCondition method for removing entities directly
     */
    public void testRemoveByCondition() throws Exception {
        //
        // remove all the level1 nodes by using a condition on the description field
        //
        EntityCondition isLevel1 = new EntityExpr("description", EntityOperator.EQUALS, "node-level #1 (updated)");
        int n = 0;

        try {
            n = delegator.removeByCondition("TestingNode", isLevel1);
        } catch (GenericEntityException e) {
            TestCase.fail("testRemoveByCondition threw an exception");
        }

        TestCase.assertTrue("testRemoveByCondition nodes > 0", n > 0);
    }

    /*
     * Test the .removeByPrimaryKey by using findByCondition and then retrieving the GenericPk from a GenericValue
     */
    public void testRemoveByPK() throws Exception {
        //
        // Find all the root nodes,
        // delete them their primary key
        //
        EntityCondition isRoot = new EntityExpr("primaryParentNodeId", EntityOperator.EQUALS, GenericEntity.NULL_FIELD);
        List rootValues = delegator.findByCondition("TestingNode", isRoot, UtilMisc.toList("testingNodeId"), null);

        Iterator it = rootValues.iterator();
        while(it.hasNext()) {
            GenericPK pk = ((GenericValue)it.next()).getPrimaryKey();
            int del = delegator.removeByPrimaryKey(pk);
            TestCase.assertEquals("Removing Root by primary key", del, 1);
        }

        // no more TestingNode should be in the data base anymore.

        List testingNodes = delegator.findAll("TestingNode");
        TestCase.assertEquals("No more TestingNode after removing the roots", testingNodes.size(), 0);
    }

    /*
     * Tests the .removeAll method only.
     */
    public void testRemoveType() throws Exception {
        List values = delegator.findAll("TestingType");
        delegator.removeAll(values);

        // now make sure there are no more of these
        values = delegator.findAll("TestingType");
        TestCase.assertEquals("No more TestingTypes after remove all", values.size(), 0);
    }

    /*
     * This test will create a large number of unique items and add them to the delegator at once
     */
    public void testCreateManyAndStoreAtOnce() throws Exception {
        try {
            List newValues = new LinkedList();
            for (int i = 0; i < TEST_COUNT; i++) {
                newValues.add(delegator.makeValue("Testing", UtilMisc.toMap("testingId", getTestId("T1-", i))));
            }
            delegator.storeAll(newValues);
            List newlyCreatedValues = delegator.findAll("Testing", UtilMisc.toList("testingId"));
            TestCase.assertEquals("Test to create " + TEST_COUNT + " and store all at once", TEST_COUNT, newlyCreatedValues.size());
        } catch (GenericEntityException e) {
            assertTrue("GenericEntityException:" + e.toString(), false);
            return;
        } finally {
            List newlyCreatedValues = delegator.findAll("Testing", UtilMisc.toList("testingId"));
            delegator.removeAll(newlyCreatedValues); 
        }
    }

    /*
     * This test will create a large number of unique items and add them to the delegator at once
     */
    public void testCreateManyAndStoreOneAtATime() throws Exception {
        try {
            for (int i = 0; i < TEST_COUNT; i++){
                delegator.create(delegator.makeValue("Testing", UtilMisc.toMap("testingId", getTestId("T2-", i))));
            }
            List newlyCreatedValues = delegator.findAll("Testing", UtilMisc.toList("testingId"));
            TestCase.assertEquals("Test to create " + TEST_COUNT + " and store one at a time: ", TEST_COUNT, newlyCreatedValues.size());
        } catch (GenericEntityException e){
            assertTrue("GenericEntityException:" + e.toString(), false);
            return;
        }
    }
 
    /*
     * This test will use the large number of unique items from above and test the EntityListIterator looping through the list
     */
    public void testEntityListIterator() throws Exception {
        try {
            EntityListIterator iterator = delegator.findListIteratorByCondition("Testing", new EntityExpr("testingId", EntityOperator.LIKE, "T2-%"), null, null);
            assertTrue("Test if EntityListIterator was created: ", iterator != null);

            int i = 0;
            GenericValue item = (GenericValue) iterator.next();
            while (item != null) {
                assertTrue("Testing if iterated data matches test data (row " + i + "): ", item.getString("testingId").equals(getTestId("T2-", i)));
                item = (GenericValue) iterator.next();
                i++;
            }
            assertTrue("Test if EntitlyListIterator iterates exactly " + TEST_COUNT + " times: " , i == TEST_COUNT);
            iterator.close();
        } catch (GenericEntityException e) {
            assertTrue("GenericEntityException:" + e.toString(), false);
            return;
        } finally {
            List entitiesToRemove = delegator.findByCondition("Testing", new EntityExpr("testingId", EntityOperator.LIKE, "T2-%"), null, null);
            delegator.removeAll(entitiesToRemove);
        }
    }

    /*
     * This test will verify transaction rollbacks using TransactionUtil.
     */
    public void testTransactionUtilRollback() throws Exception {
        try {
            GenericValue testValue = delegator.makeValue("Testing", UtilMisc.toMap("testingId", "rollback-test"));
            boolean transBegin = TransactionUtil.begin();
            delegator.create(testValue);
            TransactionUtil.rollback(transBegin, null, null);
            GenericValue testValueOut = delegator.findByPrimaryKey("Testing", UtilMisc.toMap("testingId", "rollback-test"));
            assertEquals("Test that transaction rollback removes value: ", testValueOut, null);
        } catch (GenericEntityException e) {
            assertTrue("GenericEntityException:" + e.toString(), false);
            return;
        }
    }

    /*
     * This test will verify that a transaction which takes longer than the pre-set timeout are rolled back. 
     */
    public void testTransactionUtilMoreThanTimeout() throws Exception {
        try {
            GenericValue testValue = delegator.makeValue("Testing", UtilMisc.toMap("testingId", "timeout-test"));
            boolean transBegin = TransactionUtil.begin(10); // timeout set to 10 seconds
            delegator.create(testValue);
            Thread.sleep(20*1000);
            TransactionUtil.commit(transBegin);
            assertTrue(false);
        } catch (GenericTransactionException e) {
            assertTrue(true);
        } catch (GenericEntityException e) {
            assertTrue("Other GenericEntityException encountered:" + e.toString(), false);
            return;
        } finally {
            delegator.removeByAnd("Testing", UtilMisc.toMap("testingId", "timeout-test"));
        }
    }
    
    /*
     * This test will verify that the same transaction transaction which takes less time than timeout will be committed.
     */
    public void testTransactionUtilLessThanTimeout() throws Exception {
        try {
            GenericValue testValue = delegator.makeValue("Testing", UtilMisc.toMap("testingId", "timeout-test"));
            boolean transBegin = TransactionUtil.begin();
            TransactionUtil.setTransactionTimeout(20); // now set timeout to 20 seconds
            delegator.create(testValue);
            Thread.sleep(10*1000);
            TransactionUtil.commit(transBegin);
            assertTrue(true);
        } catch (GenericTransactionException e) {
            assertTrue("Transaction error when testing transaction less than timeout " + e.toString(), false);
        } catch (GenericEntityException e) {
            assertTrue("Other GenericEntityException encountered:" + e.toString(), false);
            return;
        } finally {
            delegator.removeByAnd("Testing", UtilMisc.toMap("testingId", "timeout-test"));
        }
    }

  /*
   * This will test setting a blob field to null by creating a TestBlob entity whose blob field is not set
   */
  public void testSetNullBlob() throws Exception {
      try {
          delegator.create("TestBlob", UtilMisc.toMap("testBlobId", "null-blob"));
      } catch (GenericEntityException ex) {
          assertTrue("GenericEntityException:" + ex.toString(), false);
          return;
      } finally {
          List allTestBlobs = delegator.findAll("TestBlob");
          delegator.removeAll(allTestBlobs);
      }
  }

  /*
   * Tests setting a byte value into a blob data type using the GenericValue .setBytes method 
   */
  public void testBlobCreate() throws Exception {
      try {
          byte[] b = new byte[1];
          b[0] = (byte)0x01;
          GenericValue testingBlob = delegator.makeValue("TestBlob", UtilMisc.toMap("testBlobId", "byte-blob"));
          testingBlob.setBytes("testBlobField", b);
          testingBlob.create();
          
          TestCase.assertTrue("Blob with byte value successfully created...", true);
      } catch(Exception ex) {
        TestCase.fail(ex.getMessage());
      } finally {
          // Remove all our newly inserted values.
        List values = delegator.findAll("TestBlob");
        delegator.removeAll(values);
      }
  }

  /*
   * This creates an string id from a number 
   */
  private String getTestId(String strTestBase, int iNum) {
      StringBuffer strBufTemp = new StringBuffer(strTestBase);
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

}
