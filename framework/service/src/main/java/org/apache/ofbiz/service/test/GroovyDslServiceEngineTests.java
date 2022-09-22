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

import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.service.ModelService;
import org.apache.ofbiz.service.ServiceUtil;
import org.apache.ofbiz.service.testtools.OFBizTestCase;

import java.util.Map;

public class GroovyDslServiceEngineTests extends OFBizTestCase {

    public GroovyDslServiceEngineTests(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
    }

    @Override
    protected void tearDown() throws Exception {
    }

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
}
