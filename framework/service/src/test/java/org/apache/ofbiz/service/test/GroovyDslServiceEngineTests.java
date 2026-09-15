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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Timestamp;
import java.util.Map;
import java.util.UUID;

import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.service.ModelService;
import org.apache.ofbiz.service.ServiceUtil;
import org.apache.ofbiz.testtools.JunitJupiterTest;
import org.apache.ofbiz.testtools.JupiterTestHelper;
import org.junit.jupiter.api.Test;

@JunitJupiterTest
public class GroovyDslServiceEngineTests implements JupiterTestHelper {

    @Test
    public final void testGroovyServices() throws Exception {
        String pingMsg = "Unit Test";
        Map<String, Object> pingMap = UtilMisc.toMap("ping", pingMsg);

        //test success
        Map<String, Object> result = getDispatcher().runSync("testGroovyPingSuccess", pingMap);
        assertTrue(ServiceUtil.isSuccess(result));
        assertEquals("Service result success", result.get(ModelService.SUCCESS_MESSAGE));
        assertEquals(pingMsg, result.get("pong"));

        //test error
        result = getDispatcher().runSync("testGroovyPingError", pingMap);
        assertTrue(ServiceUtil.isError(result));
        assertEquals("Service result error", ServiceUtil.getErrorMessage(result));
        assertEquals(pingMsg, result.get("pong"));

        //test success with DSL
        result = getDispatcher().runSync("testGroovyPingSuccessWithDSLCall", pingMap);
        assertTrue(ServiceUtil.isSuccess(result));
        assertEquals("Service result success", result.get(ModelService.SUCCESS_MESSAGE));
        assertEquals(pingMsg, result.get("pong"));

        //test error with DSL (no out param test since DSL do not support out param yet when error)
        result = getDispatcher().runSync("testGroovyPingErrorWithDSLCall", pingMap, 60, true);
        assertTrue(ServiceUtil.isError(result));
        assertEquals("Service result error", result.get(ModelService.ERROR_MESSAGE));
    }

    @Test
    public final void testGroovyServiceAsyncDSLCall() throws Exception {
        String pingMsg = "Unit Test Async";
        Map<String, Object> pingMap = UtilMisc.toMap("ping", pingMsg);

        // ofbiz --test does not wipe JobSandbox between runs, and JobPoller never picks up a
        // pending job during a test run (see below) - so a prior run's leftover SERVICE_PENDING
        // row for this same service would otherwise satisfy every assertion below even if
        // runServiceAsync() were broken. Scoping to runTime >= beforeCall makes the test hermetic.
        Timestamp beforeCall = UtilDateTime.nowTimestamp();
        Map<String, Object> result = getDispatcher().runSync("testGroovyPingSuccessWithAsyncDSLCall", pingMap);
        assertTrue(ServiceUtil.isSuccess(result));

        // Verifying actual pickup/completion by the Job Scheduler is not possible from inside an
        // "ofbiz --test" run: JobPoller's loop is gated on Start.getInstance().getCurrentState()
        // == RUNNING, but StartupControlPanel.loadContainers() only flips the server to RUNNING
        // after ContainerLoader.load() returns - and that method runs TestRunContainer (which runs
        // every test suite synchronously on that same thread) before returning. So the server never
        // reaches RUNNING until after this entire test suite has already finished, and JobPoller
        // never gets to poll during the run. Instead, this asserts that runServiceAsync() itself
        // persisted a well-formed job request - the only thing observable from within the test -
        // matching the existing precedent in ServicePurgeTest.groovy and
        // ServiceMultipleNodeRecoveryTest.groovy, neither of which waits for live completion either.
        GenericValue jobSandbox = from("JobSandbox")
                .where(EntityCondition.makeCondition("serviceName", "testGroovyPingSuccess"),
                        EntityCondition.makeCondition("runTime", EntityOperator.GREATER_THAN_EQUAL_TO, beforeCall))
                .orderBy("-runTime")
                .queryFirst();
        assertNotNull(jobSandbox, "Expected runServiceAsync() to persist a JobSandbox record for testGroovyPingSuccess");
        assertEquals("SERVICE_PENDING", jobSandbox.getString("statusId"));
        assertNotNull(jobSandbox.getString("runtimeDataId"),
                "Expected the async call's context to be persisted as RuntimeData");
    }

    // testingId is a VARCHAR(20) pk, so test ids need to stay short - an 8-char UUID fragment
    // per prefix is plenty of uniqueness for a single test run.
    private String shortTestingId(String prefix) {
        return prefix + UUID.randomUUID().toString().substring(0, 8);
    }

    @Test
    public final void testGroovyEntityDslCreate() throws Exception {
        String testingId = shortTestingId("gdCr-");
        Map<String, Object> input = UtilMisc.toMap("testingId", testingId, "testingName", "Created via DSL");

        Map<String, Object> result = getDispatcher().runSync("testGroovyEntityDslCreate", input);
        assertTrue(ServiceUtil.isSuccess(result));

        GenericValue created = from("Testing").where("testingId", testingId).queryOne();
        assertNotNull(created, "Expected create() to persist a new Testing record");
        assertEquals("Created via DSL", created.getString("testingName"));
    }

    @Test
    public final void testGroovyEntityDslUpdate() throws Exception {
        String testingId = shortTestingId("gdUp-");
        getDelegator().create("Testing", UtilMisc.toMap("testingId", testingId, "testingName", "Before"));

        Map<String, Object> result = getDispatcher().runSync("testGroovyEntityDslUpdate",
                UtilMisc.toMap("testingId", testingId, "testingName", "After"));
        assertTrue(ServiceUtil.isSuccess(result));

        GenericValue updated = from("Testing").where("testingId", testingId).queryOne();
        assertEquals("After", updated.getString("testingName"));
    }

    @Test
    public final void testGroovyEntityDslUpdateNotFound() throws Exception {
        // requireNewTransaction=true so the thrown-and-caught ExecutionServiceException's
        // rollback-only marking does not poison this test method's own transaction, matching
        // the existing testGroovyPingErrorWithDSLCall precedent above.
        Map<String, Object> result = getDispatcher().runSync("testGroovyEntityDslUpdate",
                UtilMisc.toMap("testingId", shortTestingId("gdMiss-"), "testingName", "Irrelevant"), 60, true);
        assertTrue(ServiceUtil.isError(result));
    }

    @Test
    public final void testGroovyEntityDslDelete() throws Exception {
        String testingId = shortTestingId("gdDel-");
        getDelegator().create("Testing", UtilMisc.toMap("testingId", testingId, "testingName", "To be removed"));

        Map<String, Object> result = getDispatcher().runSync("testGroovyEntityDslDelete",
                UtilMisc.toMap("testingId", testingId));
        assertTrue(ServiceUtil.isSuccess(result));
        assertEquals(1, result.get("rowsRemoved"));

        GenericValue afterDelete = from("Testing").where("testingId", testingId).queryOne();
        assertNull(afterDelete, "Expected delete() to remove the Testing record");
    }
}
