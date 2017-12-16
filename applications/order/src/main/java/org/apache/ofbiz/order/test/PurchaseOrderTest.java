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

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.service.testtools.OFBizTestCase;

public class PurchaseOrderTest extends OFBizTestCase {

    protected GenericValue userLogin = null;
    protected String orderId = null;
    protected String statusId = null;

    public PurchaseOrderTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        userLogin = EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "system").queryOne();
    }

    @Override
    protected void tearDown() throws Exception {
    }

    public void testCreatePurchaseOrder() throws Exception {
        Map <String, Object> ctx = new HashMap<>();
        ctx.put("partyId", "Company");
        ctx.put("orderTypeId", "PURCHASE_ORDER");
        ctx.put("currencyUom", "USD");
        ctx.put("productStoreId","9000");

        GenericValue orderItem = delegator.makeValue("OrderItem", UtilMisc.toMap("orderItemSeqId", "00001", "orderItemTypeId", "PRODUCT_ORDER_ITEM", "prodCatalogId", "DemoCatalog", "productId", "GZ-1000", "quantity", new BigDecimal("2"), "isPromo", "N"));
        orderItem.set("unitPrice", new BigDecimal("1399.5"));
        orderItem.set("unitListPrice", BigDecimal.ZERO);
        orderItem.set("isModifiedPrice", "N");
        orderItem.set("statusId", "ITEM_CREATED");
        List <GenericValue> orderItems = new LinkedList<>();
        orderItems.add(orderItem);
        ctx.put("orderItems", orderItems);

        GenericValue orderContactMech = delegator.makeValue("OrderContactMech", UtilMisc.toMap("contactMechPurposeTypeId", "SHIPPING_LOCATION", "contactMechId", "9000"));
        List <GenericValue> orderContactMechs = new LinkedList<>();
        orderContactMechs.add(orderContactMech);
        ctx.put("orderContactMechs", orderContactMechs);

        GenericValue orderItemContactMech = delegator.makeValue("OrderItemContactMech", UtilMisc.toMap("contactMechPurposeTypeId", "SHIPPING_LOCATION", "contactMechId", "9000", "orderItemSeqId", "00001"));
        List <GenericValue> orderItemContactMechs = new LinkedList<>();
        orderItemContactMechs.add(orderItemContactMech);
        ctx.put("orderItemContactMechs", orderItemContactMechs);

        GenericValue orderItemShipGroup = delegator.makeValue("OrderItemShipGroup", UtilMisc.toMap("carrierPartyId", "UPS", "contactMechId", "9000", "isGift", "N", "maySplit", "N", "shipGroupSeqId", "00001", "shipmentMethodTypeId", "NEXT_DAY"));
        orderItemShipGroup.set("carrierRoleTypeId","CARRIER");
        List <GenericValue> orderItemShipGroupInfo = new LinkedList<>();
        orderItemShipGroupInfo.add(orderItemShipGroup);
        ctx.put("orderItemShipGroupInfo", orderItemShipGroupInfo);

        List <GenericValue> orderTerms = new LinkedList<>();
        ctx.put("orderTerms", orderTerms);

        List <GenericValue> orderAdjustments = new LinkedList<>();
        ctx.put("orderAdjustments", orderAdjustments);

        ctx.put("billToCustomerPartyId", "Company");
        ctx.put("billFromVendorPartyId", "DemoSupplier");
        ctx.put("shipFromVendorPartyId", "Company");
        ctx.put("supplierAgentPartyId", "DemoSupplier");
        ctx.put("userLogin", userLogin);

        Map <String, Object> resp = dispatcher.runSync("storeOrder", ctx);
        orderId = (String) resp.get("orderId");
        statusId = (String) resp.get("statusId");
        assertNotNull(orderId);
        assertNotNull(statusId);
    }
}
