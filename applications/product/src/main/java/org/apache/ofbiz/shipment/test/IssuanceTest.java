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

package org.apache.ofbiz.shipment.test;

import java.math.BigDecimal;
import java.util.List;

import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.service.testtools.OFBizTestCase;
import org.apache.ofbiz.shipment.packing.PackingSession;

/**
 * Item Issuance Tests
 */
public class IssuanceTest extends OFBizTestCase {

    protected GenericValue userLogin = null;

    public IssuanceTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        userLogin = EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "system").queryOne();
    }

    @Override
    protected void tearDown() throws Exception {
    }

    public void testMultipleInventoryItemIssuance() throws Exception {
        String facilityId = "WebStoreWarehouse";
        String productId="GZ-2644";
        String orderId="DEMO81015";
        String orderItemSeqId="00001";
        String shipGroupSeqId="00001";
        String shipmentItemSeqId = "00001";

        PackingSession packSession = new PackingSession(dispatcher, userLogin, facilityId, null, orderId, shipGroupSeqId);
        packSession.addOrIncreaseLine(orderId, orderItemSeqId, shipGroupSeqId, productId, BigDecimal.valueOf(6L), 1,
            BigDecimal.valueOf(1000L), false);
        String shipmentId = packSession.complete(false);

        GenericValue orderHeader = EntityQuery.use(delegator).from("OrderHeader").where("orderId", orderId).queryOne();

        // Test the OrderShipment is correct
        List<GenericValue> orderShipments = orderHeader.getRelated("OrderShipment", null, null, false);

        assertFalse("No OrderShipment for order", UtilValidate.isEmpty(orderShipments));
        assertEquals( "Incorrect number of OrderShipments for order", 1, orderShipments.size());

        GenericValue orderShipment = orderShipments.get(0);
        assertEquals(orderItemSeqId, orderShipment.getString("orderItemSeqId"));
        assertEquals(shipGroupSeqId, orderShipment.getString("shipGroupSeqId"));
        assertEquals(shipmentId, orderShipment.getString("shipmentId"));
        assertEquals(shipmentItemSeqId, orderShipment.getString("shipmentItemSeqId"));
        BigDecimal actual = orderShipment.getBigDecimal("quantity");
        assertTrue("Incorrect quantity in OrderShipment. Expected 6.00000 actual " + actual, actual.compareTo(BigDecimal.valueOf(6L))==0);

        // Test the ItemIssuances are correct
        List<GenericValue> itemIssuances = orderHeader.getRelated("ItemIssuance", null, UtilMisc.toList("inventoryItemId"), false);
        assertFalse("No ItemIssuances for order", UtilValidate.isEmpty(itemIssuances));
        assertEquals( "Incorrect number of ItemIssuances for order", 2, itemIssuances.size());

        GenericValue itemIssuance = itemIssuances.get(0);
        assertEquals(orderItemSeqId, itemIssuance.getString("orderItemSeqId"));
        assertEquals(shipGroupSeqId, itemIssuance.getString("shipGroupSeqId"));
        assertEquals(shipmentId, itemIssuance.getString("shipmentId"));
        assertEquals(shipmentItemSeqId, itemIssuance.getString("shipmentItemSeqId"));
        assertEquals("9001", itemIssuance.getString("inventoryItemId"));
        actual = itemIssuance.getBigDecimal("quantity");
        assertTrue("Incorrect quantity in ItemIssuance. Expected 5.00000 actual " + actual, actual.compareTo(BigDecimal.valueOf(5L))==0);

        itemIssuance = itemIssuances.get(1);
        assertEquals(orderItemSeqId, itemIssuance.getString("orderItemSeqId"));
        assertEquals(shipGroupSeqId, itemIssuance.getString("shipGroupSeqId"));
        assertEquals(shipmentId, itemIssuance.getString("shipmentId"));
        assertEquals(shipmentItemSeqId, itemIssuance.getString("shipmentItemSeqId"));
        assertEquals("9025", itemIssuance.getString("inventoryItemId"));
        actual = itemIssuance.getBigDecimal("quantity");
        assertTrue("Incorrect quantity in ItemIssuance. Expected 1.00000 actual " + actual, actual.compareTo(BigDecimal.valueOf(1L))==0);

        // Test reservations have been removed
        List<GenericValue> reservations = orderHeader.getRelated("OrderItemShipGrpInvRes", null, null, false);
        assertTrue("Reservations exist for order - should have been deleted", UtilValidate.isEmpty(reservations));

        // Test order header status is now ORDER_COMPLETED
        assertEquals(orderHeader.getString("statusId"), "ORDER_COMPLETED");

        // Test order items status are now ITEM_COMPLETED
        List<GenericValue> orderItems = orderHeader.getRelated("OrderItem", null, null, false);

        for (GenericValue orderItem : orderItems) {
            assertEquals("ITEM_COMPLETED", orderItem.getString("statusId"));
        }
    }
}
