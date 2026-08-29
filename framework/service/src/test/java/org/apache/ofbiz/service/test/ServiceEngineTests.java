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

import java.util.Map;

import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.service.ModelService;
import org.apache.ofbiz.testtools.JunitJupiterTest;
import org.apache.ofbiz.testtools.JupiterTestHelper;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

@JunitJupiterTest
public class ServiceEngineTests implements JupiterTestHelper {

    /**
     * Test basic java invocation.
     * @throws Exception the exception
     */
    @Test
    @Order(1)
    public void testBasicJavaInvocation() throws Exception {
        Map<String, Object> result = getDispatcher().runSync("testScv", UtilMisc.toMap("message", "Unit Test"));
        assertEquals(ModelService.RESPOND_SUCCESS, result.get(ModelService.RESPONSE_MESSAGE), "Service result success");
    }

    /**
     * Test a seca with condition in
     * @throws Exception the exception
     */
    @Test
    @Order(2)
    public void testConditionSecaInInvocation() throws Exception {
        Map<String, Object> result = getDispatcher().runSync("ping", UtilMisc.toMap("message", "present"));
        assertEquals("set message to condition in message", result.get("message"));
        result = getDispatcher().runSync("ping", UtilMisc.toMap("message", "in"));
        assertEquals("set message to condition in message", result.get("message"));
        result = getDispatcher().runSync("ping", UtilMisc.toMap("message", "other"));
        assertEquals("other", result.get("message"));
    }

    /**
     * Test a basic clojure invocation
     * @throws Exception the exception
     */
    @Test
    @Order(3)
    public void testBasicClojureInvocation() throws Exception {
        Map<String, Object> result = getDispatcher().runSync("testClojureSvc", UtilMisc.toMap("message", "Unit Test"));
        assertEquals(ModelService.RESPOND_SUCCESS, result.get(ModelService.RESPONSE_MESSAGE), "Service result success");
    }

}
