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
package org.apache.ofbiz.shipment.test

import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.shipment.packing.PackingSession
import org.apache.ofbiz.testtools.JunitJupiterTest
import org.apache.ofbiz.testtools.JupiterTestHelper
import org.junit.jupiter.api.Test

/**
 * Item Issuance Tests
 */
@JunitJupiterTest
class IssuanceTest implements JupiterTestHelper {

    @Test
    void testMultipleInventoryItemIssuance() {
        String facilityId = 'WebStoreWarehouse'
        String productId = 'GZ-2644'
        String orderId = 'DEMO81015'
        String orderItemSeqId = '00001'
        String shipGroupSeqId = '00001'
        String shipmentItemSeqId = '00001'

        PackingSession packSession = new PackingSession(dispatcher, userLogin, facilityId, null, orderId, shipGroupSeqId)
        packSession.addOrIncreaseLine(orderId, orderItemSeqId, shipGroupSeqId, productId, BigDecimal.valueOf(6L), 1,
                BigDecimal.valueOf(1000L), false)
        String shipmentId = packSession.complete(false)

        GenericValue orderHeader = from('OrderHeader').where('orderId', orderId).queryOne()

        // Test the OrderShipment is correct
        List<GenericValue> orderShipments = orderHeader.getRelated('OrderShipment', null, null, false)

        assert orderShipments : 'No OrderShipment for order'
        assert orderShipments.size() == 1 : 'Incorrect number of OrderShipments for order'

        GenericValue orderShipment = orderShipments[0]
        assert orderShipment.orderItemSeqId == orderItemSeqId
        assert orderShipment.shipGroupSeqId == shipGroupSeqId
        assert orderShipment.shipmentId == shipmentId
        assert orderShipment.shipmentItemSeqId == shipmentItemSeqId
        BigDecimal shipmentQuantity = orderShipment.getBigDecimal('quantity')
        assert shipmentQuantity == 6 :
                "Incorrect quantity in OrderShipment. Expected 6.00000 actual ${shipmentQuantity}"

        // Test the ItemIssuances are correct
        List<GenericValue> itemIssuances = orderHeader.getRelated('ItemIssuance', null, ['inventoryItemId'], false)
        assert itemIssuances : 'No ItemIssuances for order'
        assert itemIssuances.size() == 2 : 'Incorrect number of ItemIssuances for order'

        GenericValue itemIssuance = itemIssuances[0]
        assert itemIssuance.orderItemSeqId == orderItemSeqId
        assert itemIssuance.shipGroupSeqId == shipGroupSeqId
        assert itemIssuance.shipmentId == shipmentId
        assert itemIssuance.shipmentItemSeqId == shipmentItemSeqId
        assert itemIssuance.inventoryItemId == '9001'
        BigDecimal firstQuantity = itemIssuance.getBigDecimal('quantity')
        assert firstQuantity == 5 :
                "Incorrect quantity in ItemIssuance. Expected 5.00000 actual ${firstQuantity}"

        itemIssuance = itemIssuances[1]
        assert itemIssuance.orderItemSeqId == orderItemSeqId
        assert itemIssuance.shipGroupSeqId == shipGroupSeqId
        assert itemIssuance.shipmentId == shipmentId
        assert itemIssuance.shipmentItemSeqId == shipmentItemSeqId
        assert itemIssuance.inventoryItemId == '9025'
        BigDecimal secondQuantity = itemIssuance.getBigDecimal('quantity')
        assert secondQuantity == 1 :
                "Incorrect quantity in ItemIssuance. Expected 1.00000 actual ${secondQuantity}"

        // Test reservations have been removed
        List<GenericValue> reservations = orderHeader.getRelated('OrderItemShipGrpInvRes', null, null, false)
        assert !reservations : 'Reservations exist for order - should have been deleted'

        // Test order header status is now ORDER_COMPLETED
        assert orderHeader.statusId == 'ORDER_COMPLETED'

        // Test order items status are now ITEM_COMPLETED
        List<GenericValue> orderItems = orderHeader.getRelated('OrderItem', null, null, false)
        orderItems.each { GenericValue orderItemGv ->
            assert orderItemGv.statusId == 'ITEM_COMPLETED'
        }
    }

}
