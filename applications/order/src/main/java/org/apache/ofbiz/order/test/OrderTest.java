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
package org.apache.ofbiz.order.test;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.service.ServiceUtil;
import org.apache.ofbiz.service.testtools.OFBizTestCase;

import java.util.HashMap;
import java.util.Map;

public class OrderTest extends OFBizTestCase {
    private static final String MODULE = OFBizTestCase.class.getName();

    public OrderTest(String name) {
        super(name);
    }

    /**
     * Test admin get next order seq id.
     * @throws Exception the exception
     */
    public void testAdminGetNextOrderSeqId() throws Exception {
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("partyId", "admin"); //party with no AcctgPref prefix
        Map<String, Object> resp = getDispatcher().runSync("getNextOrderId", ctx);
        if (ServiceUtil.isError(resp)) {
            Debug.logError(ServiceUtil.getErrorMessage(resp), MODULE);
            return;
        }
        String orderId = (String) resp.get("orderId");
        assertNotNull(orderId);
        assertTrue(orderId.matches("\\d{5,}"));
    }

    /**
     * Test company get next order seq id.
     * @throws Exception the exception
     */
    public void testCompanyGetNextOrderSeqId() throws Exception {
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("partyId", "Company"); //party with AcctgPref prefix : CO
        Map<String, Object> resp = getDispatcher().runSync("getNextOrderId", ctx);
        if (ServiceUtil.isError(resp)) {
            Debug.logError(ServiceUtil.getErrorMessage(resp), MODULE);
            return;
        }
        String orderId = (String) resp.get("orderId");
        assertNotNull(orderId);
        assertTrue(orderId.startsWith("CO"));
    }

    /**
     * Test complete get next order seq id.
     * @throws Exception the exception
     */
    public void testCompleteGetNextOrderSeqId() throws Exception {
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("partyId", "Company"); //party with AcctgPref prefix : CO
        ctx.put("productStoreId", "9000"); // prefix WS
        Map<String, Object> resp = getDispatcher().runSync("getNextOrderId", ctx);
        if (ServiceUtil.isError(resp)) {
            Debug.logError(ServiceUtil.getErrorMessage(resp), MODULE);
            return;
        }
        String orderId = (String) resp.get("orderId");
        assertNotNull(orderId);
        assertTrue(orderId.startsWith("WSCO"));
    }
}
