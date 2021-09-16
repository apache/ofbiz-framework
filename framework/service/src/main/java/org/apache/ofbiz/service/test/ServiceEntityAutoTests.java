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
package org.apache.ofbiz.service.test;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.ServiceUtil;
import org.apache.ofbiz.service.testtools.OFBizTestCase;

public class ServiceEntityAutoTests extends OFBizTestCase {

    public ServiceEntityAutoTests(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
    }

    @Override
    protected void tearDown() throws Exception {
    }

    /**
     * Test entity auto create singl pk entity.
     * @throws Exception the exception
     */
    public void testEntityAutoCreateSinglPkEntity() throws Exception {
        Delegator delegator = getDelegator();
        //test create with given pk
        Map<String, Object> testingPkPresentMap = new HashMap<>();
        testingPkPresentMap.put("testingId", "TESTING_1");
        testingPkPresentMap.put("testingName", "entity auto testing");
        Map<String, Object> results = getDispatcher().runSync("testEntityAutoCreateTestingPkPresent", testingPkPresentMap);
        assertTrue(ServiceUtil.isSuccess(results));
        GenericValue testing = EntityQuery.use(delegator).from("Testing").where("testingId", "TESTING_1").queryOne();
        assertNotNull(testing);

        //test create with auto sequence
        Map<String, Object> testingPkMissingMap = new HashMap<>();
        testingPkPresentMap.put("testingName", "entity auto testing without pk part in");
        results = getDispatcher().runSync("testEntityAutoCreateTestingPkMissing", testingPkMissingMap);
        assertTrue(ServiceUtil.isSuccess(results));
        testing = EntityQuery.use(delegator).from("Testing").where("testingId", results.get("testingId")).queryOne();
        assertNotNull(testing);

        //test collision
        results = getDispatcher().runSync("testEntityAutoCreateTestingPkPresent", testingPkPresentMap, 10, true);
        assertTrue(ServiceUtil.isError(results));
    }

    /**
     * Test entity auto create double pk entity.
     * @throws Exception the exception
     */
    public void testEntityAutoCreateDoublePkEntity() throws Exception {
        Delegator delegator = getDelegator();
        delegator.create("Testing", "testingId", "TESTING_2");

        //test create with given pk
        Map<String, Object> testingItemPkPresentMap = UtilMisc.toMap("testingId", "TESTING_2", "testingSeqId", "00001");
        Map<String, Object> results = getDispatcher().runSync("testEntityAutoCreateTestingItemPkPresent", testingItemPkPresentMap);
        assertTrue(ServiceUtil.isSuccess(results));
        GenericValue testingItem = EntityQuery.use(delegator)
                                              .from("TestingItem")
                                              .where("testingId", "TESTING_2", "testingSeqId", "00001")
                                              .queryOne();
        assertNotNull(testingItem);

        //test create with auto sub-sequence
        Map<String, Object> testingItemPkMissingMap = UtilMisc.toMap("testingId", "TESTING_2");
        results = getDispatcher().runSync("testEntityAutoCreateTestingItemPkMissing", testingItemPkMissingMap);
        assertTrue(ServiceUtil.isSuccess(results));
        testingItem = EntityQuery.use(delegator)
                                 .from("TestingItem")
                                 .where("testingId", "TESTING_2", "testingSeqId", results.get("testingSeqId"))
                                 .queryOne();
        assertNotNull(testingItem);
        assertEquals("00002", testingItem.get("testingSeqId"));

        //test collision
        results = getDispatcher().runSync("testEntityAutoCreateTestingItemPkPresent", testingItemPkPresentMap, 10, true);
        assertTrue(ServiceUtil.isError(results));
    }

    /**
     * Test entity auto create multi pk entity.
     * @throws Exception the exception
     */
    public void testEntityAutoCreateMultiPkEntity() throws Exception {
        Delegator delegator = getDelegator();
        delegator.create("TestingNode", "testingNodeId", "NODE_1");
        delegator.create("Testing", "testingId", "TESTING_3");

        //test create given pk
        Map<String, Object> testingNodeMemberPkPresentMap = UtilMisc.toMap("testingId", "TESTING_3",
                "testingNodeId", "NODE_1", "fromDate", UtilDateTime.toTimestamp("01/01/2010 00:00:00"));
        Map<String, Object> results = getDispatcher().runSync("testEntityAutoCreateTestingNodeMemberPkPresent", testingNodeMemberPkPresentMap);
        assertTrue(ServiceUtil.isSuccess(results));
        GenericValue testingNodeMember = EntityQuery.use(delegator)
                                                    .from("TestingNodeMember")
                                                    .where(testingNodeMemberPkPresentMap)
                                                    .queryOne();
        assertNotNull(testingNodeMember);
        testingNodeMember.remove();

        //test create auto sub-sequence
        //test missing pk fromDate
        Map<String, Object> testingNodeMemberPkMissingMap = UtilMisc.toMap("testingId", "TESTING_3", "testingNodeId", "NODE_1");
        results = getDispatcher().runSync("testEntityAutoCreateTestingNodeMemberPkMissing", testingNodeMemberPkMissingMap, 10, true);
        assertTrue(ServiceUtil.isSuccess(results));
    }

    /**
     * Test entity auto update entity.
     * @throws Exception the exception
     */
    public void testEntityAutoUpdateEntity() throws Exception {
        Delegator delegator = getDelegator();
        delegator.create("Testing", "testingId", "TESTING_4", "testingName", "entity auto testing");

        //test update with exist pk
        Map<String, Object> testingUpdateMap = UtilMisc.toMap("testingId", "TESTING_4", "testingName", "entity auto testing updated");
        Map<String, Object> results = getDispatcher().runSync("testEntityAutoUpdateTesting", testingUpdateMap);
        assertTrue(ServiceUtil.isSuccess(results));
        GenericValue testing = EntityQuery.use(delegator).from("Testing").where("testingId", "TESTING_4").queryOne();
        assertEquals("entity auto testing updated", testing.getString("testingName"));

        //test update with bad pk
        Map<String, Object> testingUpdateFailedMap = UtilMisc.toMap("testingId", "TESTING_4_FAILED", "testingName", "entity auto testing updated");
        results = getDispatcher().runSync("testEntityAutoUpdateTesting", testingUpdateFailedMap, 10, true);
        assertTrue(ServiceUtil.isError(results));
        assertEquals(UtilProperties.getMessage("ServiceErrorUiLabels", "ServiceValueNotFound", Locale.ENGLISH), ServiceUtil.getErrorMessage(results));
    }

    /**
     * Test entity auto delete entity.
     * @throws Exception the exception
     */
    public void testEntityAutoDeleteEntity() throws Exception {
        Delegator delegator = getDelegator();
        delegator.create("Testing", "testingId", "TESTING_5");

        //test delete with exist pk
        Map<String, Object> testingDeleteMap = UtilMisc.toMap("testingId", "TESTING_5");
        Map<String, Object> results = getDispatcher().runSync("testEntityAutoRemoveTesting", testingDeleteMap);
        assertTrue(ServiceUtil.isSuccess(results));
        GenericValue testing = EntityQuery.use(delegator).from("Testing").where("testingId", "TESTING_5").queryOne();
        assertNull(testing);

        //test create with bad pk
        Map<String, Object> testingDeleteFailedMap = UtilMisc.toMap("testingId", "TESTING_5_FAILED");
        results = getDispatcher().runSync("testEntityAutoRemoveTesting", testingDeleteFailedMap);
        assertTrue(ServiceUtil.isError(results));
        assertEquals(UtilProperties.getMessage("ServiceErrorUiLabels", "ServiceValueNotFoundForRemove", Locale.ENGLISH),
                ServiceUtil.getErrorMessage(results));
    }

    /**
     * Test entity auto expire entity.
     * @throws Exception the exception
     */
    public void testEntityAutoExpireEntity() throws Exception {
        Delegator delegator = getDelegator();
        Timestamp now = UtilDateTime.nowTimestamp();
        delegator.create("Testing", "testingId", "TESTING_6");
        delegator.create("TestingNode", "testingNodeId", "TESTNODE_6");
        Map<String, Object> testingNodeMemberPkMap = UtilMisc.toMap("testingId", "TESTING_6", "testingNodeId", "TESTNODE_6", "fromDate", now);
        delegator.create("TestingNodeMember", testingNodeMemberPkMap);

        //test expire the thruDate
        Map<String, Object> results = getDispatcher().runSync("testEntityAutoExpireTestingNodeMember", testingNodeMemberPkMap);
        assertTrue(ServiceUtil.isSuccess(results));
        GenericValue testingNodeMember = EntityQuery.use(delegator).from("TestingNodeMember").where(testingNodeMemberPkMap).queryOne();
        Timestamp expireDate = testingNodeMember.getTimestamp("thruDate");
        assertNotNull("Expire thruDate set ", expireDate);

        //test expire to ensure the thruDate isn't update but extendThruDate is
        results = getDispatcher().runSync("testEntityAutoExpireTestingNodeMember", testingNodeMemberPkMap);
        assertTrue(ServiceUtil.isSuccess(results));
        testingNodeMember = EntityQuery.use(delegator).from("TestingNodeMember").where(testingNodeMemberPkMap).queryOne();
        assertTrue(expireDate.compareTo(testingNodeMember.getTimestamp("thruDate")) == 0);
        assertNotNull("Expire extendThruDate set ", testingNodeMember.getTimestamp("extendThruDate"));

        //test expire a specific field
        delegator.create("TestFieldType", "testFieldTypeId", "TESTING_6");
        Map<String, Object> testingExpireMap = UtilMisc.toMap("testFieldTypeId", "TESTING_6");
        results = getDispatcher().runSync("testEntityAutoExpireTestFieldType", testingExpireMap);
        assertTrue(ServiceUtil.isSuccess(results));
        GenericValue testFieldType = EntityQuery.use(delegator).from("TestFieldType").where("testFieldTypeId", "TESTING_6").queryOne();
        assertNotNull("Expire dateTimeField set", testFieldType.getTimestamp("dateTimeField"));

        //test expire a specific field with in value
        delegator.create("TestFieldType", "testFieldTypeId", "TESTING_6bis");
        testingExpireMap = UtilMisc.toMap("testFieldTypeId", "TESTING_6bis", "dateTimeField", now);
        results = getDispatcher().runSync("testEntityAutoExpireTestFieldType", testingExpireMap);
        assertTrue(ServiceUtil.isSuccess(results));
        testFieldType = EntityQuery.use(delegator).from("TestFieldType").where("testFieldTypeId", "TESTING_6bis").queryOne();
        assertTrue(now.compareTo(testFieldType.getTimestamp("dateTimeField")) == 0);
    }


    /**
     * Test entity auto entity status concept.
     * @throws Exception the exception
     */
    public void testEntityAutoEntityStatusConcept() throws Exception {
        Delegator delegator = getDelegator();
        delegator.create("Testing", "testingId", "TESTING_7");
        delegator.create("StatusType", "statusTypeId", "TESTINGSTATUS");
        delegator.create("StatusItem", "statusId", "TESTING_CREATE", "statusTypeId", "TESTINGSTATUS");
        delegator.create("StatusItem", "statusId", "TESTING_UPDATE", "statusTypeId", "TESTINGSTATUS");
        GenericValue userLogin = getUserLogin("system");

        //test create testingStatus with userlogin
        Map<String, Object> testingStatusCreateMap = UtilMisc.toMap("testingId", "TESTING_7", "statusId", "TESTING_CREATE", "userLogin", userLogin);
        Map<String, Object> results = getDispatcher().runSync("testEntityAutoCreateTestingStatus", testingStatusCreateMap);
        assertTrue(ServiceUtil.isSuccess(results));
        GenericValue testing = EntityQuery.use(delegator).from("TestingStatus").where("testingId", "TESTING_7").queryFirst();
        assertNotNull(testing.getTimestamp("statusDate"));
        assertEquals("system", testing.getString("changeByUserLoginId"));

        //test create testingStatus without userLogin
        try {
            testingStatusCreateMap = UtilMisc.toMap("testingId", "TESTING_7", "statusId", "TESTING_CREATE");
            results = getDispatcher().runSync("testEntityAutoCreateTestingStatus", testingStatusCreateMap, 10, true);
            assertTrue(ServiceUtil.isError(results));
        } catch (GenericServiceException e) {
            assertEquals(e.toString(), "You call a creation on entity that require the userLogin to track the activity, "
                    + "please control that your service definition has auth='true'");
        }

        //test update testingStatus
        try {
            Map<String, Object> testingStatusUpdateMap = UtilMisc.toMap("testingStatusId", testing.get("testingStatusId"),
                    "statusId", "TESTING_UPDATE", "userLogin", userLogin);
            results = getDispatcher().runSync("testEntityAutoUpdateTestingStatus", testingStatusUpdateMap, 10, true);
            assertTrue(ServiceUtil.isError(results));
        } catch (GenericServiceException e) {
            assertEquals(e.toString(), "You call a updating operation on entity that track the activity, sorry I can't do that,"
                    + "please amazing developer check your service definition;)");
        }

        //test delete testingStatus
        try {
            Map<String, Object> testingStatusDeleteMap = UtilMisc.toMap("testingStatusId", testing.get("testingStatusId"), "userLogin", userLogin);
            results = getDispatcher().runSync("testEntityAutoDeleteTestingStatus", testingStatusDeleteMap, 10, true);
            assertTrue(ServiceUtil.isError(results));
        } catch (GenericServiceException e) {
            assertEquals(e.toString(), "You call a deleting operation on entity that track the activity, sorry I can't do that, "
                    + "please amazing developer check your service definition;)");
        }
    }
}
