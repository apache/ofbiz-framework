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

package org.apache.ofbiz.product

import org.apache.ofbiz.base.util.UtilDateTime
import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.service.ServiceUtil
import org.apache.ofbiz.service.testtools.OFBizTestCase
import org.apache.ofbiz.shipment.packing.PackingSession

class ShipmentTests extends OFBizTestCase {
    public ShipmentTests(String name) {
        super(name)
    }

    void testPackingServices() {
        PackingSession packingSession = new PackingSession(dispatcher, userLogin)
        Map serviceCtx = [
                productId: 'GZ-2644',
                orderId: 'DEMO10090',
                shipGroupSeqId: '00001',
                quantity: new BigDecimal('2'),
                packageSeq: 1,
                pickerPartyId: 'DemoCustomer',
                handlingInstructions: 'Handle with care',
                packingSession: packingSession,
                userLogin: userLogin
        ]

        Map serviceResult = dispatcher.runSync('packSingleItem', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        serviceCtx.clear()
        serviceResult.clear()
        serviceCtx = [
                updateQuantity: true,
                orderId: 'DEMO10090',
                shipGroupSeqId: '00001',
                pickerPartyId: 'DemoCustomer',
                handlingInstructions: 'Handle with care',
                nextPackageSeq: 1,
                packingSession: packingSession,
                userLogin: userLogin
        ]
        serviceResult = dispatcher.runSync('packBulkItems', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        serviceCtx.clear()
        serviceResult.clear()
        serviceCtx = [
                orderId: 'DEMO10090',
                pickerPartyId: 'DemoCustomer',
                handlingInstructions: 'Handle with care',
                packingSession: packingSession,
                additionalShippingCharge: new BigDecimal('10'),
                forceComplete: true,
                userLogin: userLogin
        ]
        serviceResult = dispatcher.runSync('completePack', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        String shipmentId = serviceResult.shipmentId
        assert  shipmentId

        GenericValue shipment = from('Shipment')
                .where('shipmentId', shipmentId)
                .queryOne()
        assert shipment

        serviceCtx.clear()
        serviceResult.clear()
        serviceCtx = [
                shipmentId: shipmentId,
                userLogin: userLogin
        ]
        serviceResult = dispatcher.runSync('createInvoicesFromShipment', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        List invoicesCreated = serviceResult.invoicesCreated
        assert  shipmentId

        for (String invoiceId : invoicesCreated) {
            GenericValue invoice = from('Invoice')
                    .where('invoiceId', invoiceId)
                    .queryOne()
            assert invoice
        }
    }

    void testShipmentServices() {
        Map serviceCtx = [
                shipmentTypeId: 'SALES_SHIPMENT',
                statusId: 'SHIPMENT_INPUT',
                primaryOrderId: 'DEMO10090',
                partyIdTo: 'DemoCustomer',
                originFacilityId: 'WebStoreWarehouse',
                userLogin: userLogin
        ]

        Map serviceResult = dispatcher.runSync('createShipment', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        String shipmentId = serviceResult.shipmentId
        assert  shipmentId

        serviceCtx.clear()
        serviceResult.clear()
        serviceCtx = [
                shipmentId: shipmentId,
                statusId: 'SHIPMENT_PACKED',
                userLogin: userLogin
        ]
        serviceResult = dispatcher.runSync('updateShipment', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        serviceCtx.statusId = 'SHIPMENT_SHIPPED'
        serviceResult = dispatcher.runSync('updateShipment', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        GenericValue shipment = from('Shipment')
                .where('shipmentId', shipmentId)
                .queryOne()
        assert shipment
        assert 'SHIPMENT_SHIPPED'.equals(shipment.statusId)
    }

    void testReceiveInventoryNonSerialized() {
        Map serviceCtx = [
                facilityId: 'WebStoreWarehouse',
                productId: 'GZ-2644',
                quantityAccepted: new BigDecimal('2'),
                quantityRejected: BigDecimal.ZERO,
                unitCost: new BigDecimal('24'),
                inventoryItemTypeId: 'NON_SERIAL_INV_ITEM',
                datetimeReceived: UtilDateTime.nowTimestamp(),
                userLogin: userLogin
        ]

        Map serviceResult = dispatcher.runSync('receiveInventoryProduct', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        String inventoryItemId = serviceResult.inventoryItemId
        assert inventoryItemId

        GenericValue inventoryItem = from('InventoryItem')
                .where('inventoryItemId', inventoryItemId)
                .queryOne()
        assert inventoryItem
        assert inventoryItem.productId.equals(serviceCtx.productId)
        assert inventoryItem.facilityId.equals(serviceCtx.facilityId)
        assert inventoryItem.quantityOnHandTotal.compareTo(serviceCtx.quantityAccepted) == 0
        assert inventoryItem.availableToPromiseTotal.compareTo(serviceCtx.quantityAccepted) == 0

        List inventoryItemDetails = from('InventoryItemDetail')
                .where('inventoryItemId', inventoryItemId)
                .queryList()
        assert inventoryItemDetails

        GenericValue shipmentReceipt = from('ShipmentReceipt')
                .where('inventoryItemId', inventoryItemId)
                .orderBy('datetimeReceived').queryFirst()
        assert shipmentReceipt
        assert shipmentReceipt.quantityAccepted.compareTo(serviceCtx.quantityAccepted) == 0
        assert shipmentReceipt.productId.equals(serviceCtx.productId)
    }

    void testCreateShipmentRouteSegment() {
        GenericValue shipment = from('Shipment')
                .where('shipmentId', '9998')
                .queryOne()
        assert shipment

        Map serviceCtx = [
                shipmentId: shipment.shipmentId,
                shipmentRouteSegmentId: '0001',
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync('createShipmentRouteSegment', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        String shipmentRouteSegmentId = serviceResult.shipmentRouteSegmentId
        assert shipmentRouteSegmentId

        GenericValue shipmentRouteSegment = from('ShipmentRouteSegment')
                .where('shipmentId', shipment.shipmentId, 'shipmentRouteSegmentId', shipmentRouteSegmentId)
                .queryOne()
        assert shipmentRouteSegment
        assert '9998'.equals(shipmentRouteSegment.shipmentId)
        assert shipmentRouteSegment.shipmentRouteSegmentId.equals(shipmentRouteSegmentId)
    }
}