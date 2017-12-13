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

    public void testEntityAutoCreateSinglPkEntity() throws Exception {
        //test create with given pk
        Map<String, Object> testingPkPresentMap = new HashMap<String, Object>();
        testingPkPresentMap.put("testingId", "TESTING_1");
        testingPkPresentMap.put("testingName", "entity auto testing");
        Map<String, Object> results = dispatcher.runSync("testEntityAutoCreateTestingPkPresent", testingPkPresentMap);
        assertTrue(ServiceUtil.isSuccess(results));
        GenericValue testing = EntityQuery.use(delegator).from("Testing").where("testingId", "TESTING_1").queryOne();
        assertNotNull(testing);

        //test create with auto sequence
        Map<String, Object> testingPkMissingMap = new HashMap<String, Object>();
        testingPkPresentMap.put("testingName", "entity auto testing without pk part in");
        results = dispatcher.runSync("testEntityAutoCreateTestingPkMissing", testingPkMissingMap);
        assertTrue(ServiceUtil.isSuccess(results));
        testing = EntityQuery.use(delegator).from("Testing").where("testingId", results.get("testingId")).queryOne();
        assertNotNull(testing);

        //test collision
        results = dispatcher.runSync("testEntityAutoCreateTestingPkPresent", testingPkPresentMap, 10, true);
        assertTrue(ServiceUtil.isError(results));
    }

    public void testEntityAutoCreateDoublePkEntity() throws Exception {
        delegator.create("Testing", "testingId", "TESTING_2");

        //test create with given pk
        Map<String, Object> testingItemPkPresentMap = UtilMisc.toMap("testingId", "TESTING_2", "testingSeqId", "00001");
        Map<String, Object> results = dispatcher.runSync("testEntityAutoCreateTestingItemPkPresent", testingItemPkPresentMap);
        assertTrue(ServiceUtil.isSuccess(results));
        GenericValue testingItem = EntityQuery.use(delegator)
                                              .from("TestingItem")
                                              .where("testingId", "TESTING_2", "testingSeqId", "00001")
                                              .queryOne();
        assertNotNull(testingItem);

        //test create with auto sub-sequence
        Map<String, Object> testingItemPkMissingMap = UtilMisc.toMap("testingId", "TESTING_2");
        results = dispatcher.runSync("testEntityAutoCreateTestingItemPkMissing", testingItemPkMissingMap);
        assertTrue(ServiceUtil.isSuccess(results));
        testingItem = EntityQuery.use(delegator)
                                 .from("TestingItem")
                                 .where("testingId", "TESTING_2", "testingSeqId", results.get("testingSeqId"))
                                 .queryOne();
        assertNotNull(testingItem);
        assertEquals("00002", testingItem.get("testingSeqId"));

        //test collision
        results = dispatcher.runSync("testEntityAutoCreateTestingItemPkPresent", testingItemPkPresentMap, 10, true);
        assertTrue(ServiceUtil.isError(results));
        //assertEquals("", ServiceUtil.getErrorMessage(results));
    }

    public void testEntityAutoCreateMultiPkEntity() throws Exception {
        delegator.create("TestingNode", "testingNodeId", "NODE_1");
        delegator.create("Testing", "testingId", "TESTING_3");

        //test create given pk
        Map<String, Object> testingNodeMemberPkPresentMap = UtilMisc.toMap("testingId", "TESTING_3",
                "testingNodeId", "NODE_1", "fromDate", UtilDateTime.toTimestamp("01/01/2010 00:00:00"));
        Map<String, Object> results = dispatcher.runSync("testEntityAutoCreateTestingNodeMemberPkPresent", testingNodeMemberPkPresentMap);
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
        results = dispatcher.runSync("testEntityAutoCreateTestingNodeMemberPkMissing", testingNodeMemberPkMissingMap, 10, true);
        assertTrue(ServiceUtil.isSuccess(results));
    }

    public void testEntityAutoUpdateEntity() throws Exception {
        delegator.create("Testing", "testingId", "TESTING_4", "testingName", "entity auto testing");

        //test update with exist pk
        Map<String, Object> testingUpdateMap = UtilMisc.toMap("testingId", "TESTING_4", "testingName", "entity auto testing updated");
        Map<String, Object> results = dispatcher.runSync("testEntityAutoUpdateTesting", testingUpdateMap);
        assertTrue(ServiceUtil.isSuccess(results));
        GenericValue testing = EntityQuery.use(delegator).from("Testing").where("testingId", "TESTING_4").queryOne();
        assertEquals("entity auto testing updated", testing.getString("testingName"));

        //test update with bad pk
        Map<String, Object> testingUpdateFailedMap = UtilMisc.toMap("testingId", "TESTING_4_FAILED", "testingName", "entity auto testing updated");
        results = dispatcher.runSync("testEntityAutoUpdateTesting", testingUpdateFailedMap, 10, true);
        assertTrue(ServiceUtil.isError(results));
        assertEquals(UtilProperties.getMessage("ServiceErrorUiLabels", "ServiceValueNotFound", Locale.ENGLISH), ServiceUtil.getErrorMessage(results));
    }

    public void testEntityAutoDeleteEntity() throws Exception {
        delegator.create("Testing", "testingId", "TESTING_5");

        //test delete with exist pk
        Map<String, Object> testingDeleteMap = UtilMisc.toMap("testingId", "TESTING_5");
        Map<String, Object> results = dispatcher.runSync("testEntityAutoRemoveTesting", testingDeleteMap);
        assertTrue(ServiceUtil.isSuccess(results));
        GenericValue testing = EntityQuery.use(delegator).from("Testing").where("testingId", "TESTING_5").queryOne();
        assertNull(testing);

        //test create with bad pk
        Map<String, Object> testingDeleteFailedMap = UtilMisc.toMap("testingId", "TESTING_5_FAILED");
        results = dispatcher.runSync("testEntityAutoRemoveTesting", testingDeleteFailedMap);
        assertTrue(ServiceUtil.isError(results));
        assertEquals(UtilProperties.getMessage("ServiceErrorUiLabels", "ServiceValueNotFoundForRemove", Locale.ENGLISH), ServiceUtil.getErrorMessage(results));
    }

    public void testEntityAutoExpireEntity() throws Exception {
        Timestamp now = UtilDateTime.nowTimestamp();
        delegator.create("Testing", "testingId", "TESTING_6");
        delegator.create("TestingNode", "testingNodeId", "TESTNODE_6");
        Map<String, Object> testingNodeMemberPkMap = UtilMisc.toMap("testingId", "TESTING_6", "testingNodeId", "TESTNODE_6", "fromDate", now);
        delegator.create("TestingNodeMember", testingNodeMemberPkMap);
 
        //test expire the thruDate
        Map<String, Object> results = dispatcher.runSync("testEntityAutoExpireTestingNodeMember", testingNodeMemberPkMap);
        assertTrue(ServiceUtil.isSuccess(results));
        GenericValue testingNodeMember = EntityQuery.use(delegator).from("TestingNodeMember").where(testingNodeMemberPkMap).queryOne();
        Timestamp expireDate = testingNodeMember.getTimestamp("thruDate");
        assertNotNull("Expire thruDate set ", expireDate);

        //test expire to ensure the thruDate isn't update but extendThruDate is
        results = dispatcher.runSync("testEntityAutoExpireTestingNodeMember", testingNodeMemberPkMap);
        assertTrue(ServiceUtil.isSuccess(results));
        testingNodeMember = EntityQuery.use(delegator).from("TestingNodeMember").where(testingNodeMemberPkMap).queryOne();
        assertTrue(expireDate.compareTo(testingNodeMember.getTimestamp("thruDate")) == 0);
        assertNotNull("Expire extendThruDate set ", testingNodeMember.getTimestamp("extendThruDate"));

        //test expire a specific field
        delegator.create("TestFieldType", "testFieldTypeId", "TESTING_6");
        Map<String, Object> testingExpireMap = UtilMisc.toMap("testFieldTypeId", "TESTING_6");
        results = dispatcher.runSync("testEntityAutoExpireTestFieldType", testingExpireMap);
        assertTrue(ServiceUtil.isSuccess(results));
        GenericValue testFieldType = EntityQuery.use(delegator).from("TestFieldType").where("testFieldTypeId", "TESTING_6").queryOne();
        assertNotNull("Expire dateTimeField set", testFieldType.getTimestamp("dateTimeField"));

        //test expire a specific field with in value
        delegator.create("TestFieldType", "testFieldTypeId", "TESTING_6bis");
        testingExpireMap = UtilMisc.toMap("testFieldTypeId", "TESTING_6bis", "dateTimeField", now);
        results = dispatcher.runSync("testEntityAutoExpireTestFieldType", testingExpireMap);
        assertTrue(ServiceUtil.isSuccess(results));
        testFieldType = EntityQuery.use(delegator).from("TestFieldType").where("testFieldTypeId", "TESTING_6bis").queryOne();
        assertTrue(now.compareTo(testFieldType.getTimestamp("dateTimeField")) == 0);
    }


    public void testEntityAutoEntityStatusConcept() throws Exception {
        delegator.create("Testing", "testingId", "TESTING_7");
        delegator.create("StatusType", "statusTypeId", "TESTINGSTATUS");
        delegator.create("StatusItem", "statusId", "TESTING_CREATE", "statusTypeId", "TESTINGSTATUS");
        delegator.create("StatusItem", "statusId", "TESTING_UPDATE", "statusTypeId", "TESTINGSTATUS");
        GenericValue userLogin = delegator.findOne("UserLogin", true, "userLoginId", "system");

        //test create testingStatus with userlogin
        Map<String, Object> testingStatusCreateMap = UtilMisc.toMap("testingId", "TESTING_7", "statusId", "TESTING_CREATE", "userLogin", userLogin);
        Map<String, Object> results = dispatcher.runSync("testEntityAutoCreateTestingStatus", testingStatusCreateMap);
        assertTrue(ServiceUtil.isSuccess(results));
        GenericValue testing = EntityQuery.use(delegator).from("TestingStatus").where("testingId", "TESTING_7").queryFirst();
        assertNotNull(testing.getTimestamp("statusDate"));
        assertEquals("system", testing.getString("changeByUserLoginId"));

        //test create testingStatus without userLogin
        try {
            testingStatusCreateMap = UtilMisc.toMap("testingId", "TESTING_7", "statusId", "TESTING_CREATE");
            results = dispatcher.runSync("testEntityAutoCreateTestingStatus", testingStatusCreateMap, 10, true);
            assertTrue(ServiceUtil.isError(results));
        } catch (GenericServiceException e) {
            assertEquals(e.toString(),"You call a creation on entity that require the userLogin to track the activity, please controle that your service definition has auth='true'");
        }

        //test update testingStatus
        try {
            Map<String, Object> testingStatusUpdateMap = UtilMisc.toMap("testingStatusId", testing.get("testingStatusId"), "statusId", "TESTING_UPDATE", "userLogin", userLogin);
            results = dispatcher.runSync("testEntityAutoUpdateTestingStatus", testingStatusUpdateMap, 10, true);
            assertTrue(ServiceUtil.isError(results));
        } catch (GenericServiceException e) {
            assertEquals(e.toString(), "You call a updating operation on entity that track the activity, sorry I can't do that, please amazing developer check your service definition ;)");
        }

        //test delete testingStatus
        try {
            Map<String, Object> testingStatusDeleteMap = UtilMisc.toMap("testingStatusId", testing.get("testingStatusId"), "userLogin", userLogin);
            results = dispatcher.runSync("testEntityAutoDeleteTestingStatus", testingStatusDeleteMap, 10, true);
            assertTrue(ServiceUtil.isError(results));
        } catch (GenericServiceException e) {
            assertEquals(e.toString(), "You call a deleting operation on entity that track the activity, sorry I can't do that, please amazing developer check your service definition ;)");
        }
    }
}
