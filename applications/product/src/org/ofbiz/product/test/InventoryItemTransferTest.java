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

package org.ofbiz.product.test;

import java.math.BigDecimal;
import java.util.Map;

import javolution.util.FastMap;

import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.service.testtools.OFBizTestCase;

public class InventoryItemTransferTest extends OFBizTestCase {

    protected GenericValue userLogin = null;
    protected static String inventoryTransferId = null;
    protected BigDecimal transferQty = BigDecimal.ONE;

    public InventoryItemTransferTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        userLogin = delegator.findByPrimaryKey("UserLogin", UtilMisc.toMap("userLoginId", "system"));
    }

    @Override
    protected void tearDown() throws Exception {
    }

    public void testCreateInventoryItemsTransfer() throws Exception {
        Map<String, Object> ctx = FastMap.newInstance();
        String statusId = "IXF_REQUESTED";
        String inventoryItemId = "9005";
        ctx.put("inventoryItemId", inventoryItemId);
        ctx.put("statusId", statusId);
        ctx.put("facilityId", "WebStoreWarehouse");
        ctx.put("facilityIdTo", "WebStoreWarehouse");
        ctx.put("receiveDate", UtilDateTime.nowTimestamp());
        ctx.put("xferQty", transferQty);
        ctx.put("userLogin", userLogin);
        Map<String, Object> resp = dispatcher.runSync("createInventoryTransfer", ctx);
        inventoryTransferId = (String) resp.get("inventoryTransferId");
        assertNotNull(inventoryTransferId);
    }

    public void testUpdateInventoryItemTransfer() throws Exception {
        Map<String, Object> ctx = FastMap.newInstance();
        String statusId = "IXF_COMPLETE";
        ctx.put("inventoryTransferId", inventoryTransferId);
        String inventoryItemId = delegator.findByPrimaryKey("InventoryTransfer", UtilMisc.toMap("inventoryTransferId", inventoryTransferId)).getString("inventoryItemId");
        ctx.put("inventoryItemId", inventoryItemId);
        ctx.put("statusId", statusId);
        ctx.put("userLogin", userLogin);
        Map<String, Object> resp = dispatcher.runSync("updateInventoryTransfer", ctx);
        String respMsg = (String) resp.get("responseMessage");
        assertNotSame("error", respMsg);
    }
}
