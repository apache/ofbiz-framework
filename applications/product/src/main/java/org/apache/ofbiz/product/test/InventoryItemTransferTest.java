/*
 Licensed to the Apache Software Foundation (ASF) under one
 or more contributor license agreements.  See the NOTICE file
 distributed with this work for additional information
 regarding copyright ownership.  The ASF licenses this file
 to you under the Apache License, Version 2.0 (the
 "License"); you may not use this file except in compliance
 with the License.  You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing,
 software distributed under the License is distributed on an
 "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 KIND, either express or implied.  See the License for the
 specific language governing permissions and limitations
 under the License.
 */

package org.apache.ofbiz.product.test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.service.testtools.OFBizTestCase;

public class InventoryItemTransferTest extends OFBizTestCase {

    protected GenericValue userLogin = null;
    static String inventoryTransferId = null;
    protected BigDecimal transferQty = BigDecimal.ONE;

    public InventoryItemTransferTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        userLogin = EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "system").queryOne();
    }

    @Override
    protected void tearDown() throws Exception {
    }

    public void testCreateInventoryItemsTransfer() throws Exception {
        // create
        Map<String, Object> ctx = new HashMap<>();
        String inventoryItemId = "9005";
        ctx.put("inventoryItemId", inventoryItemId);
        ctx.put("statusId", "IXF_REQUESTED");
        ctx.put("facilityId", "WebStoreWarehouse");
        ctx.put("facilityIdTo", "WebStoreWarehouse");
        ctx.put("receiveDate", UtilDateTime.nowTimestamp());
        ctx.put("xferQty", transferQty);
        ctx.put("userLogin", userLogin);
        Map<String, Object> resp = dispatcher.runSync("createInventoryTransfer", ctx);
        setInventoryTransferId((String) resp.get("inventoryTransferId"));
        assertNotNull(inventoryTransferId);

        // transfer
        ctx = new HashMap<>();
        ctx.put("inventoryTransferId", getInventoryTransferId());
        ctx.put("inventoryItemId", inventoryItemId);
        ctx.put("statusId", "IXF_COMPLETE");
        ctx.put("userLogin", userLogin);
        resp = dispatcher.runSync("updateInventoryTransfer", ctx);
        String respMsg = (String) resp.get("responseMessage");
        assertNotSame("error", respMsg);
    }

    public static String getInventoryTransferId() {
        return inventoryTransferId;
    }

    public static void setInventoryTransferId(String inventoryTransferId) {
        InventoryItemTransferTest.inventoryTransferId = inventoryTransferId;
    }
}
