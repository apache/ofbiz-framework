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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.service.testtools.OFBizTestCase;

public class SalesOrderTest extends OFBizTestCase {

    protected GenericValue userLogin = null;

    public SalesOrderTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        userLogin = EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "system").queryOne();
    }

    @Override
    protected void tearDown() throws Exception {
    }

    public void testCreateSalesOrder() throws Exception {
        Map<String, Object> ctx = UtilMisc.<String, Object>toMap("partyId", "DemoCustomer", "orderTypeId", "SALES_ORDER", "currencyUom", "USD", "productStoreId", "9000");

        List<GenericValue> orderPaymentInfo = new LinkedList<>();
        GenericValue orderContactMech = delegator.makeValue("OrderContactMech", UtilMisc.toMap("contactMechId", "9015", "contactMechPurposeTypeId", "BILLING_LOCATION"));
        orderPaymentInfo.add(orderContactMech);

        GenericValue orderPaymentPreference = delegator.makeValue("OrderPaymentPreference", UtilMisc.toMap("paymentMethodId", "9015", "paymentMethodTypeId", "CREDIT_CARD",
                "statusId", "PAYMENT_NOT_AUTH", "overflowFlag", "N", "maxAmount", new BigDecimal("49.26")));
        orderPaymentInfo.add(orderPaymentPreference);
        ctx.put("orderPaymentInfo", orderPaymentInfo);

        List<GenericValue> orderItemShipGroupInfo = new LinkedList<>();
        orderContactMech.set("contactMechPurposeTypeId", "SHIPPING_LOCATION");
        orderItemShipGroupInfo.add(orderContactMech);

        GenericValue orderItemShipGroup = delegator.makeValue("OrderItemShipGroup", UtilMisc.toMap("carrierPartyId", "UPS", "contactMechId", "9015", "isGift", "N",
                "shipGroupSeqId", "00001", "shipmentMethodTypeId", "NEXT_DAY"));
        orderItemShipGroupInfo.add(orderItemShipGroup);

        GenericValue orderItemShipGroupAssoc = delegator.makeValue("OrderItemShipGroupAssoc", UtilMisc.toMap("orderItemSeqId", "00001", "quantity", BigDecimal.ONE, "shipGroupSeqId", "00001"));
        orderItemShipGroupInfo.add(orderItemShipGroupAssoc);

        GenericValue orderAdjustment = null;
        orderAdjustment = delegator.makeValue("OrderAdjustment", UtilMisc.toMap("orderAdjustmentTypeId", "SHIPPING_CHARGES", "shipGroupSeqId", "00001", "amount", new BigDecimal("12.45")));
        orderItemShipGroupInfo.add(orderAdjustment);

        orderAdjustment = delegator.makeValue("OrderAdjustment", UtilMisc.toMap("orderAdjustmentTypeId", "SALES_TAX", "orderItemSeqId", "00001", "overrideGlAccountId", "224153",
                "primaryGeoId", "UT", "shipGroupSeqId", "00001", "sourcePercentage", BigDecimal.valueOf(4.7)));
        orderAdjustment.set("taxAuthGeoId", "UT");
        orderAdjustment.set("taxAuthPartyId", "UT_TAXMAN");
        orderAdjustment.set("taxAuthorityRateSeqId", "9004");
        orderAdjustment.set("amount", BigDecimal.valueOf(1.824));
        orderAdjustment.set("comments", "Utah State Sales Tax");
        orderItemShipGroupInfo.add(orderAdjustment);

        orderAdjustment = delegator.makeValue("OrderAdjustment", UtilMisc.toMap("orderAdjustmentTypeId", "SALES_TAX", "orderItemSeqId", "00001", "overrideGlAccountId", "224153",
                "primaryGeoId", "UT-UTAH", "shipGroupSeqId", "00001", "sourcePercentage", BigDecimal.valueOf(0.1)));
        orderAdjustment.set("taxAuthGeoId", "UT-UTAH");
        orderAdjustment.set("taxAuthPartyId", "UT_UTAH_TAXMAN");
        orderAdjustment.set("taxAuthorityRateSeqId", "9005");
        orderAdjustment.set("amount", BigDecimal.valueOf(0.039));
        orderAdjustment.set("comments", "Utah County, Utah Sales Tax");
        orderItemShipGroupInfo.add(orderAdjustment);

        orderAdjustment = delegator.makeValue("OrderAdjustment", UtilMisc.toMap("orderAdjustmentTypeId", "SALES_TAX", "orderItemSeqId", "00001", "overrideGlAccountId", "224000",
                "primaryGeoId", "_NA_", "shipGroupSeqId", "00001", "sourcePercentage", BigDecimal.valueOf(1)));
        orderAdjustment.set("taxAuthGeoId", "_NA_");
        orderAdjustment.set("taxAuthPartyId", "_NA_");
        orderAdjustment.set("taxAuthorityRateSeqId", "9000");
        orderAdjustment.set("amount", BigDecimal.valueOf(0.384));
        orderAdjustment.set("comments", "1% OFB _NA_ Tax");
        orderItemShipGroupInfo.add(orderAdjustment);

        ctx.put("orderItemShipGroupInfo", orderItemShipGroupInfo);

        List<GenericValue> orderAdjustments = new LinkedList<>();
        orderAdjustment = delegator.makeValue("OrderAdjustment", UtilMisc.toMap("orderAdjustmentTypeId", "PROMOTION_ADJUSTMENT", "productPromoActionSeqId", "01", "productPromoId", "9011", "productPromoRuleId", "01", "amount", BigDecimal.valueOf(-3.84)));
        orderAdjustments.add(orderAdjustment);
        ctx.put("orderAdjustments", orderAdjustments);

        List<GenericValue> orderItems = new LinkedList<>();
        GenericValue orderItem = delegator.makeValue("OrderItem", UtilMisc.toMap("orderItemSeqId", "00001", "orderItemTypeId", "PRODUCT_ORDER_ITEM", "prodCatalogId", "DemoCatalog", "productId", "GZ-2644", "quantity", BigDecimal.ONE, "selectedAmount", BigDecimal.ZERO));
        orderItem.set("isPromo", "N");
        orderItem.set("isModifiedPrice", "N");
        orderItem.set("unitPrice", new BigDecimal("38.4"));
        orderItem.set("unitListPrice", new BigDecimal("48.0"));
        orderItem.set("statusId", "ITEM_CREATED");
        orderItems.add(orderItem);

        orderItem = delegator.makeValue("OrderItem", UtilMisc.toMap("orderItemSeqId", "00002", "orderItemTypeId", "PRODUCT_ORDER_ITEM", "prodCatalogId", "DemoCatalog", "productId", "GZ-1006-1", "quantity", BigDecimal.ONE, "selectedAmount", BigDecimal.ZERO));
        orderItem.set("isPromo", "N");
        orderItem.set("isModifiedPrice", "N");
        orderItem.set("unitPrice", new BigDecimal("1.99"));
        orderItem.set("unitListPrice", new BigDecimal("5.99"));
        orderItem.set("statusId", "ITEM_CREATED");
        orderItems.add(orderItem);

        ctx.put("orderItems", orderItems);

        List<GenericValue> orderTerms = new LinkedList<>();
        ctx.put("orderTerms", orderTerms);

        GenericValue OrderContactMech = delegator.makeValue("OrderContactMech");
        OrderContactMech.set("contactMechPurposeTypeId", "SHIPPING_LOCATION");
        OrderContactMech.set("contactMechId", "10000");

        ctx.put("placingCustomerPartyId", "DemoCustomer");
        ctx.put("endUserCustomerPartyId", "DemoCustomer");
        ctx.put("shipToCustomerPartyId", "DemoCustomer");
        ctx.put("billToCustomerPartyId", "DemoCustomer");
        ctx.put("billFromVendorPartyId", "Company");

        ctx.put("userLogin", userLogin);
        Map<String, Object> resp = dispatcher.runSync("storeOrder", ctx);
        String orderId = (String) resp.get("orderId");
        String statusId = (String) resp.get("statusId");
        assertNotNull(orderId);
        assertNotNull(statusId);
    }
}
