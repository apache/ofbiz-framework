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

import java.util.Map;

import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.service.ModelService;
import org.apache.ofbiz.service.testtools.OFBizTestCase;

public class ServiceEngineTests extends OFBizTestCase {

    public ServiceEngineTests(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
    }

    @Override
    protected void tearDown() throws Exception {
    }

    /**
     * Test basic java invocation.
     * @throws Exception the exception
     */
    public void testBasicJavaInvocation() throws Exception {
        Map<String, Object> result = getDispatcher().runSync("testScv", UtilMisc.toMap("message", "Unit Test"));
        assertEquals("Service result success", ModelService.RESPOND_SUCCESS, result.get(ModelService.RESPONSE_MESSAGE));
    }

    /**
     * Test a basic clojure invocation
     * @throws Exception the exception
     */
    public void testBasicClojureInvocation() throws Exception {
        Map<String, Object> result = getDispatcher().runSync("testClojureSvc", UtilMisc.toMap("message", "Unit Test"));
        assertEquals("Service result success", ModelService.RESPOND_SUCCESS, result.get(ModelService.RESPONSE_MESSAGE));
    }

}
