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
package org.apache.ofbiz.product.product.test

import org.apache.ofbiz.base.util.UtilDateTime
import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.service.ServiceUtil
import org.apache.ofbiz.testtools.JunitJupiterTest
import org.apache.ofbiz.testtools.JupiterTestHelper
import org.apache.ofbiz.shipment.packing.PackingSession
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test

@JunitJupiterTest
class ShipmentTests implements JupiterTestHelper {

    @Test
    @Order(1)
    void testPackingServices() {
        String productId = testParams.productId ?: 'GZ-2644'
        String orderId = testParams.orderId ?: 'DEMO10090'
        String shipGroupSeqId = testParams.shipGroupSeqId ?: '00001'
        String pickerPartyId = testParams.pickerPartyId ?: 'DemoCustomer'
        String handlingInstructions = testParams.handlingInstructions ?: 'Handle with care'
        PackingSession packingSession = new PackingSession(dispatcher, userLogin)
        Map serviceCtx = [
                productId: productId,
                orderId: orderId,
                shipGroupSeqId: shipGroupSeqId,
                quantity: new BigDecimal('2'),
                packageSeq: 1,
                pickerPartyId: pickerPartyId,
                handlingInstructions: handlingInstructions,
                packingSession: packingSession,
                userLogin: userLogin
        ]

        Map serviceResult = dispatcher.runSync('packSingleItem', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        serviceCtx.clear()
        serviceResult.clear()
        serviceCtx = [
                updateQuantity: true,
                orderId: orderId,
                shipGroupSeqId: shipGroupSeqId,
                pickerPartyId: pickerPartyId,
                handlingInstructions: handlingInstructions,
                nextPackageSeq: 1,
                packingSession: packingSession,
                userLogin: userLogin
        ]
        serviceResult = dispatcher.runSync('packBulkItems', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        serviceCtx.clear()
        serviceResult.clear()
        serviceCtx = [
                orderId: orderId,
                pickerPartyId: pickerPartyId,
                handlingInstructions: handlingInstructions,
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

    @Test
    @Order(2)
    void testShipmentServices() {
        String shipmentTypeId = testParams.shipmentTypeId ?: 'SALES_SHIPMENT'
        String statusId = testParams.statusId ?: 'SHIPMENT_INPUT'
        String primaryOrderId = testParams.primaryOrderId ?: 'DEMO10090'
        String partyIdTo = testParams.partyIdTo ?: 'DemoCustomer'
        String originFacilityId = testParams.originFacilityId ?: 'WebStoreWarehouse'
        String statusId1 = testParams.statusId1 ?: 'SHIPMENT_PACKED'
        Map serviceCtx = [
                shipmentTypeId: shipmentTypeId,
                statusId: statusId,
                primaryOrderId: primaryOrderId,
                partyIdTo: partyIdTo,
                originFacilityId: originFacilityId,
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
                statusId: statusId1,
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
        assert shipment.statusId == 'SHIPMENT_SHIPPED'
    }

    @Test
    @Order(3)
    void testReceiveInventoryNonSerialized() {
        String facilityId = testParams.facilityId ?: 'WebStoreWarehouse'
        String productId = testParams.productId ?: 'GZ-2644'
        String inventoryItemTypeId = testParams.inventoryItemTypeId ?: 'NON_SERIAL_INV_ITEM'
        Map serviceCtx = [
                facilityId: facilityId,
                productId: productId,
                quantityAccepted: new BigDecimal('2'),
                quantityRejected: BigDecimal.ZERO,
                unitCost: new BigDecimal('24'),
                inventoryItemTypeId: inventoryItemTypeId,
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
        assert inventoryItem.productId == serviceCtx.productId
        assert inventoryItem.facilityId == serviceCtx.facilityId
        assert inventoryItem.quantityOnHandTotal == serviceCtx.quantityAccepted
        assert inventoryItem.availableToPromiseTotal == serviceCtx.quantityAccepted

        List inventoryItemDetails = from('InventoryItemDetail')
                .where('inventoryItemId', inventoryItemId)
                .queryList()
        assert inventoryItemDetails

        GenericValue shipmentReceipt = from('ShipmentReceipt')
                .where('inventoryItemId', inventoryItemId)
                .orderBy('datetimeReceived').queryFirst()
        assert shipmentReceipt
        assert shipmentReceipt.quantityAccepted == serviceCtx.quantityAccepted
        assert shipmentReceipt.productId == serviceCtx.productId
    }

    @Test
    @Order(4)
    void testIssueOrderItemShipGrpInvResToShipmentRejectsMixedDestinations() {
        String productId = testParams.productId ?: 'GZ-2644'
        String shipmentTypeId = testParams.shipmentTypeId ?: 'SALES_SHIPMENT'
        String statusId = testParams.statusId ?: 'SHIPMENT_INPUT'
        String primaryShipGroupSeqId = testParams.primaryShipGroupSeqId ?: '00001'
        String originFacilityId = testParams.originFacilityId ?: 'WebStoreWarehouse'
        String shipGroupSeqId = testParams.shipGroupSeqId ?: '00002'
        Map orderResult = dispatcher.runSync('createTestSalesOrderSingle',
                [userLogin: userLogin, productId: productId])
        assert ServiceUtil.isSuccess(orderResult)
        String orderId = orderResult.orderId
        assert orderId

        GenericValue orderItem = from('OrderItem').where('orderId', orderId).queryFirst()
        assert orderItem
        String orderItemSeqId = orderItem.orderItemSeqId

        GenericValue shipGroup1 = from('OrderItemShipGroup')
                .where('orderId', orderId, 'shipGroupSeqId', primaryShipGroupSeqId)
                .queryOne()
        assert shipGroup1
        assert shipGroup1.contactMechId

        Map createShipmentCtx = [
                shipmentTypeId: shipmentTypeId,
                statusId: statusId,
                primaryOrderId: orderId,
                primaryShipGroupSeqId: primaryShipGroupSeqId,
                originFacilityId: originFacilityId,
                userLogin: userLogin
        ]
        Map createShipmentResult = dispatcher.runSync('createShipment', createShipmentCtx)
        assert ServiceUtil.isSuccess(createShipmentResult)
        String shipmentId = createShipmentResult.shipmentId
        assert shipmentId

        GenericValue shipment = from('Shipment').where('shipmentId', shipmentId).queryOne()
        assert shipment.destinationContactMechId == shipGroup1.contactMechId

        String differentContactMechId = resolveDifferentDestinationAddress('789 Different St')
        assert differentContactMechId != shipGroup1.contactMechId
        addShipGroup(orderId, shipGroupSeqId, differentContactMechId)
        GenericValue inventoryItem = findGz2644InventoryItem()
        associateItemWithShipGroup(orderId, orderItemSeqId, shipGroupSeqId, new BigDecimal('1'))
        reserveInventoryToShipGroup(orderId, orderItemSeqId, shipGroupSeqId, inventoryItem.inventoryItemId, new BigDecimal('1'))

        Map issueCtx = [
                shipmentId: shipmentId,
                orderId: orderId,
                shipGroupSeqId: shipGroupSeqId,
                orderItemSeqId: orderItemSeqId,
                inventoryItemId: inventoryItem.inventoryItemId,
                quantity: new BigDecimal('1'),
                userLogin: userLogin
        ]
        Map issueResult = dispatcher.runSync('issueOrderItemShipGrpInvResToShipment', issueCtx)
        assert ServiceUtil.isError(issueResult)
        assert ServiceUtil.getErrorMessage(issueResult).contains('different destination')
    }

    @Test
    @Order(5)
    void testIssueOrderItemShipGrpInvResToShipmentRejectsMixedDestinationsAcrossOrders() {
        String productId = testParams.productId ?: 'GZ-2644'
        String shipmentTypeId = testParams.shipmentTypeId ?: 'SALES_SHIPMENT'
        String statusId = testParams.statusId ?: 'SHIPMENT_INPUT'
        String primaryShipGroupSeqId = testParams.primaryShipGroupSeqId ?: '00001'
        String originFacilityId = testParams.originFacilityId ?: 'WebStoreWarehouse'
        String contactMechPurposeTypeId = testParams.contactMechPurposeTypeId ?: 'SHIPPING_LOCATION'
        Map orderResult1 = dispatcher.runSync('createTestSalesOrderSingle',
                [userLogin: userLogin, productId: productId])
        assert ServiceUtil.isSuccess(orderResult1)
        String orderId1 = orderResult1.orderId
        assert orderId1

        GenericValue shipGroup1 = from('OrderItemShipGroup')
                .where('orderId', orderId1, 'shipGroupSeqId', primaryShipGroupSeqId)
                .queryOne()
        assert shipGroup1
        assert shipGroup1.contactMechId

        Map createShipmentCtx = [
                shipmentTypeId: shipmentTypeId,
                statusId: statusId,
                primaryOrderId: orderId1,
                primaryShipGroupSeqId: primaryShipGroupSeqId,
                originFacilityId: originFacilityId,
                userLogin: userLogin
        ]
        Map createShipmentResult = dispatcher.runSync('createShipment', createShipmentCtx)
        assert ServiceUtil.isSuccess(createShipmentResult)
        String shipmentId = createShipmentResult.shipmentId
        assert shipmentId

        // second, independent order; also uses shipGroupSeqId primaryShipGroupSeqId but a different destination
        Map orderResult2 = dispatcher.runSync('createTestSalesOrderSingle',
                [userLogin: userLogin, productId: productId])
        assert ServiceUtil.isSuccess(orderResult2)
        String orderId2 = orderResult2.orderId
        assert orderId2
        assert orderId2 != orderId1

        GenericValue orderItem2 = from('OrderItem').where('orderId', orderId2).queryFirst()
        assert orderItem2
        String orderItemSeqId2 = orderItem2.orderItemSeqId

        String differentContactMechId = resolveDifferentDestinationAddress('999 Cross Order Ave')
        assert differentContactMechId != shipGroup1.contactMechId

        Map updateShipGroupCtx = [
                orderId: orderId2,
                shipGroupSeqId: primaryShipGroupSeqId,
                contactMechId: differentContactMechId,
                contactMechPurposeTypeId: contactMechPurposeTypeId,
                userLogin: userLogin
        ]
        Map updateShipGroupResult = dispatcher.runSync('updateOrderItemShipGroup', updateShipGroupCtx)
        assert ServiceUtil.isSuccess(updateShipGroupResult)

        // order2's item is already associated with ship group 00001 from checkout; only reserve
        GenericValue inventoryItem = findGz2644InventoryItem()
        reserveInventoryToShipGroup(orderId2, orderItemSeqId2, primaryShipGroupSeqId, inventoryItem.inventoryItemId, new BigDecimal('1'))

        Map issueCtx = [
                shipmentId: shipmentId,
                orderId: orderId2,
                shipGroupSeqId: primaryShipGroupSeqId,
                orderItemSeqId: orderItemSeqId2,
                inventoryItemId: inventoryItem.inventoryItemId,
                quantity: new BigDecimal('1'),
                userLogin: userLogin
        ]
        Map issueResult = dispatcher.runSync('issueOrderItemShipGrpInvResToShipment', issueCtx)
        assert ServiceUtil.isError(issueResult)
        assert ServiceUtil.getErrorMessage(issueResult).contains('different destination')
    }

    @Test
    @Order(6)
    void testPackingSessionRejectsMixedShipGroupDestinations() {
        String productId = testParams.productId ?: 'GZ-2644'
        String shipGroupSeqId = testParams.shipGroupSeqId ?: '00001'
        String pickerPartyId = testParams.pickerPartyId ?: 'DemoCustomer'
        String shipGroupSeqId1 = testParams.shipGroupSeqId1 ?: '00002'
        Map orderResult = dispatcher.runSync('createTestSalesOrderSingle',
                [userLogin: userLogin, productId: productId])
        assert ServiceUtil.isSuccess(orderResult)
        String orderId = orderResult.orderId
        assert orderId

        GenericValue orderItem = from('OrderItem').where('orderId', orderId).queryFirst()
        assert orderItem
        String orderItemSeqId = orderItem.orderItemSeqId

        String differentContactMechId = resolveDifferentDestinationAddress('456 Other Ave')
        addShipGroup(orderId, shipGroupSeqId1, differentContactMechId)
        GenericValue inventoryItem = findGz2644InventoryItem()
        associateItemWithShipGroup(orderId, orderItemSeqId, shipGroupSeqId1, new BigDecimal('1'))
        reserveInventoryToShipGroup(orderId, orderItemSeqId, shipGroupSeqId1, inventoryItem.inventoryItemId, new BigDecimal('1'))

        PackingSession packingSession = new PackingSession(dispatcher, userLogin)

        Map packLine1Ctx = [
                productId: productId,
                orderId: orderId,
                shipGroupSeqId: shipGroupSeqId,
                quantity: new BigDecimal('1'),
                packageSeq: 1,
                pickerPartyId: pickerPartyId,
                packingSession: packingSession,
                userLogin: userLogin
        ]
        Map packLine1Result = dispatcher.runSync('packSingleItem', packLine1Ctx)
        assert ServiceUtil.isSuccess(packLine1Result)

        Map packLine2Ctx = [
                productId: productId,
                orderId: orderId,
                shipGroupSeqId: shipGroupSeqId1,
                quantity: new BigDecimal('1'),
                packageSeq: 1,
                pickerPartyId: pickerPartyId,
                packingSession: packingSession,
                userLogin: userLogin
        ]
        Map packLine2Result = dispatcher.runSync('packSingleItem', packLine2Ctx)
        assert ServiceUtil.isSuccess(packLine2Result)

        Map completeCtx = [
                orderId: orderId,
                pickerPartyId: pickerPartyId,
                packingSession: packingSession,
                forceComplete: true,
                userLogin: userLogin
        ]
        Map completeResult = dispatcher.runSync('completePack', completeCtx)
        assert ServiceUtil.isError(completeResult)
        assert ServiceUtil.getErrorMessage(completeResult).contains('[104]')
    }

    @Test
    @Order(7)
    void testCreateShipmentRouteSegment() {
        String shipmentRouteSegmentIdParam = testParams.shipmentRouteSegmentIdParam ?: '0001'
        GenericValue shipment = from('Shipment')
                .where('shipmentId', '9998')
                .queryOne()
        assert shipment

        Map serviceCtx = [
                shipmentId: shipment.shipmentId,
                shipmentRouteSegmentId: shipmentRouteSegmentIdParam,
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
        assert shipmentRouteSegment.shipmentId == '9998'
        assert shipmentRouteSegment.shipmentRouteSegmentId == shipmentRouteSegmentId
    }

    @Test
    @Order(8)
    void testQuickShipEntireOrderDoesNotMixShipGroupDestinations() {
        String productId = testParams.productId ?: 'GZ-2644'
        Map orderResult = dispatcher.runSync('createTestSalesOrderSingle',
                [userLogin: userLogin, productId: productId])
        assert ServiceUtil.isSuccess(orderResult)
        String orderId = orderResult.orderId
        assert orderId

        GenericValue orderItem = from('OrderItem').where('orderId', orderId).queryFirst()
        assert orderItem
        String orderItemSeqId = orderItem.orderItemSeqId

        String differentContactMechId = resolveDifferentDestinationAddress('321 Another Rd')
        addShipGroup(orderId, '00002', differentContactMechId)
        GenericValue inventoryItem = findGz2644InventoryItem()
        associateItemWithShipGroup(orderId, orderItemSeqId, '00002', new BigDecimal('1'))
        reserveInventoryToShipGroup(orderId, orderItemSeqId, '00002', inventoryItem.inventoryItemId, new BigDecimal('1'))

        Map quickShipResult = dispatcher.runSync('quickShipEntireOrder', [orderId: orderId, userLogin: userLogin])
        assert ServiceUtil.isSuccess(quickShipResult)

        List<GenericValue> shipments = from('Shipment').where('primaryOrderId', orderId).queryList()
        assert shipments.size() == 2

        for (GenericValue shipment : shipments) {
            List<GenericValue> itemIssuances = from('ItemIssuance').where('shipmentId', shipment.shipmentId).queryList()
            Set<String> shipGroupsOnThisShipment = itemIssuances*.shipGroupSeqId as Set
            assert shipGroupsOnThisShipment.size() == 1
            assert shipGroupsOnThisShipment.contains(shipment.primaryShipGroupSeqId)
        }
    }

    @Test
    @Order(9)
    void testQuickShipEntireOrderSkipsShipGroupWithNothingToShip() {
        String productId = testParams.productId ?: 'GZ-2644'
        Map orderResult = dispatcher.runSync('createTestSalesOrderSingle',
                [userLogin: userLogin, productId: productId])
        assert ServiceUtil.isSuccess(orderResult)
        String orderId = orderResult.orderId
        assert orderId

        GenericValue orderItem = from('OrderItem').where('orderId', orderId).queryFirst()
        assert orderItem

        // Ship group 00002: created but given NO order items and NO inventory reservation —
        // this is the "nothing to ship for this group" case.
        String secondContactMechId = resolveDifferentDestinationAddress('555 Empty Group Rd')
        addShipGroup(orderId, '00002', secondContactMechId)

        // orderItemShipGroupList (from getRelated('OrderItemShipGroup')) has no guaranteed order,
        // so this test only proves correctness if ship group 00001 (which HAS items) still gets
        // shipped regardless of iteration order relative to the empty 00002.
        Map quickShipResult = dispatcher.runSync('quickShipEntireOrder', [orderId: orderId, userLogin: userLogin])
        assert ServiceUtil.isSuccess(quickShipResult)

        List<GenericValue> shipments = from('Shipment').where('primaryOrderId', orderId).queryList()
        assert shipments.size() == 1
        assert shipments[0].primaryShipGroupSeqId == '00001'

        List<GenericValue> itemIssuances = from('ItemIssuance').where('shipmentId', shipments[0].shipmentId).queryList()
        assert itemIssuances
        assert itemIssuances*.shipGroupSeqId as Set == ['00001'] as Set
    }

    /**
     * Resolves a postal address in a different country/state/postal code than the demo customer's
     * default shipping address, returning its contactMechId.
     */
    private String resolveDifferentDestinationAddress(String addressStreet) {
        Map createAddressCtx = [
                address1: addressStreet,
                city: 'New York',
                stateProvinceGeoId: 'NY',
                postalCode: '10001',
                countryGeoId: 'USA',
                userLogin: userLogin
        ]
        Map createAddressResult = dispatcher.runSync('createPostalAddress', createAddressCtx)
        assert ServiceUtil.isSuccess(createAddressResult)
        String contactMechId = createAddressResult.contactMechId
        assert contactMechId
        return contactMechId
    }

    /** Adds a splittable OrderItemShipGroup with the given destination. */
    private void addShipGroup(String orderId, String shipGroupSeqId, String contactMechId) {
        Map createShipGroupCtx = [
                orderId: orderId,
                shipGroupSeqId: shipGroupSeqId,
                contactMechId: contactMechId,
                facilityId: 'WebStoreWarehouse',
                maySplit: 'Y',
                userLogin: userLogin
        ]
        Map createShipGroupResult = dispatcher.runSync('createOrderItemShipGroup', createShipGroupCtx)
        assert ServiceUtil.isSuccess(createShipGroupResult)
    }

    /** Associates an order item with a ship group for the given quantity. */
    private void associateItemWithShipGroup(String orderId, String orderItemSeqId, String shipGroupSeqId, BigDecimal quantity) {
        Map createAssocCtx = [
                orderId: orderId,
                orderItemSeqId: orderItemSeqId,
                shipGroupSeqId: shipGroupSeqId,
                quantity: quantity,
                userLogin: userLogin
        ]
        Map createAssocResult = dispatcher.runSync('addOrderItemShipGroupAssoc', createAssocCtx)
        assert ServiceUtil.isSuccess(createAssocResult)
    }

    /** Reserves inventory for an order item under a ship group. */
    private void reserveInventoryToShipGroup(String orderId, String orderItemSeqId, String shipGroupSeqId,
            String inventoryItemId, BigDecimal quantity) {
        Map createInvResCtx = [
                orderId: orderId,
                shipGroupSeqId: shipGroupSeqId,
                orderItemSeqId: orderItemSeqId,
                inventoryItemId: inventoryItemId,
                quantity: quantity,
                userLogin: userLogin
        ]
        Map createInvResResult = dispatcher.runSync('createOrderItemShipGrpInvRes', createInvResCtx)
        assert ServiceUtil.isSuccess(createInvResResult)
    }

    /** Finds an available GZ-2644 inventory item at WebStoreWarehouse. */
    private GenericValue findGz2644InventoryItem() {
        GenericValue inventoryItem = from('InventoryItem')
                .where('productId', 'GZ-2644', 'facilityId', 'WebStoreWarehouse')
                .queryFirst()
        assert inventoryItem
        return inventoryItem
    }

}
