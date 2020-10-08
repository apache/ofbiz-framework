/*
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
 */
package org.apache.ofbiz.service.test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.service.testtools.OFBizTestCase;

public class ServiceSOAPTests extends OFBizTestCase {

    private static final String MODULE = ServiceSOAPTests.class.getName();

    public ServiceSOAPTests(String name) {
        super(name);
    }

    /**
     * Test soap simple service.
     * @throws Exception the exception
     */
    public void testSOAPSimpleService() throws Exception {
        Map<String, Object> serviceContext = new HashMap<>();
        serviceContext.put("defaultValue", Double.valueOf("123.4567"));
        serviceContext.put("message", "Test Message !!!");
        getDispatcher().runSync("testSoapSimple", serviceContext);
    }

    /**
     * Test soap service.
     * @throws Exception the exception
     */
    public void testSOAPService() throws Exception {
        Map<String, Object> serviceContext = new HashMap<>();
        GenericValue testing = getDelegator().makeValue("Testing");
        testing.put("testingId", "COMPLEX_TYPE_TEST");
        testing.put("testingTypeId", "SOAP_TEST");
        testing.put("testingName", "Complex Type Test");
        testing.put("createdStamp", UtilDateTime.nowTimestamp());
        serviceContext.put("testing", testing);
        Map<String, Object> results = getDispatcher().runSync("testSoap", serviceContext);
        List<GenericValue> testingNodes = UtilGenerics.cast(results.get("testingNodes"));
        assertNotNull(testingNodes);
    }
}
