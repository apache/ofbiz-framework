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

import org.apache.ofbiz.base.util.UtilMisc
import org.apache.ofbiz.entity.util.*
import org.apache.ofbiz.entity.condition.*
import org.apache.ofbiz.service.ServiceUtil

facilityId = request.getParameter("facilityId")
purchaseOrderId = request.getParameter("purchaseOrderId")
productId = request.getParameter("productId")
shipmentId = request.getParameter("shipmentId")

partialReceive = parameters.partialReceive
if (partialReceive) {
    context.partialReceive = partialReceive
}

facility = null
if (facilityId) {
    facility = from("Facility").where("facilityId", facilityId).queryOne()
}

ownerAcctgPref = null
if (facility) {
    owner = facility.getRelatedOne("OwnerParty", false)
    if (owner) {
        result = runService('getPartyAccountingPreferences', [organizationPartyId : owner.partyId, userLogin : request.getAttribute("userLogin")])
        if (ServiceUtil.isSuccess(result) && result.partyAccountingPreference) {
            ownerAcctgPref = result.partyAccountingPreference
        }
    }
}

purchaseOrder = null
if (purchaseOrderId) {
    purchaseOrder = from("OrderHeader").where("orderId", purchaseOrderId).queryOne()
    if (purchaseOrder && !"PURCHASE_ORDER".equals(purchaseOrder.orderTypeId)) {
        purchaseOrder = null
    }
}

product = null
if (productId) {
    product = from("Product").where("productId", productId).queryOne()
    context.supplierPartyIds = EntityUtil.getFieldListFromEntityList(from("SupplierProduct").where("productId", productId).orderBy("partyId").filterByDate(nowTimestamp, "availableFromDate", "availableThruDate").queryList(), "partyId", true)
}

shipments = null
if (purchaseOrder && !shipmentId) {
    orderShipments = from("OrderShipment").where("orderId", purchaseOrderId).queryList()
    if (orderShipments) {
        shipments = [] as TreeSet
        orderShipments.each { orderShipment ->
            shipment = orderShipment.getRelatedOne("Shipment", false)
            if (!"PURCH_SHIP_RECEIVED".equals(shipment.statusId) &&
                !"SHIPMENT_CANCELLED".equals(shipment.statusId) &&
                (!shipment.destinationFacilityId || facilityId.equals(shipment.destinationFacilityId))) {
                shipments.add(shipment)
            }
        }
    }
    // This is here for backward compatibility: ItemIssuances are no more created for purchase shipments.
    issuances = from("ItemIssuance").where("orderId", purchaseOrderId).queryList()
    if (issuances) {
        shipments = [] as TreeSet
        issuances.each { issuance ->
            shipment = issuance.getRelatedOne("Shipment", false)
            if (!"PURCH_SHIP_RECEIVED".equals(shipment.statusId) &&
                !"SHIPMENT_CANCELLED".equals(shipment.statusId) &&
                (!shipment.destinationFacilityId || facilityId.equals(shipment.destinationFacilityId))) {
                shipments.add(shipment)
            }
        }
    }
}

shipment = null
if (shipmentId && !"_NA_".equals(shipmentId)) {
    shipment = from("Shipment").where("shipmentId", shipmentId).queryOne()
}

shippedQuantities = [:]
purchaseOrderItems = null
if (purchaseOrder) {
    if (product) {
        purchaseOrderItems = purchaseOrder.getRelated("OrderItem", [productId : productId], null, false)
    } else if (shipment) {
        orderItems = purchaseOrder.getRelated("OrderItem", null, null, false)
        exprs = [] as ArrayList
        orderShipments = shipment.getRelated("OrderShipment", [orderId : purchaseOrderId], null, false)
        if (orderShipments) {
            orderShipments.each { orderShipment ->
                exprs.add(EntityCondition.makeCondition("orderItemSeqId", EntityOperator.EQUALS, orderShipment.orderItemSeqId))
                double orderShipmentQty = orderShipment.getDouble("quantity").doubleValue()
                if (shippedQuantities.containsKey(orderShipment.orderItemSeqId)) {
                    orderShipmentQty += ((Double)shippedQuantities.get(orderShipment.orderItemSeqId)).doubleValue()
                }
                shippedQuantities.put(orderShipment.orderItemSeqId, orderShipmentQty)
            }
        } else {
            // this is here for backward compatibility only: ItemIssuances are no more created for purchase shipments.
            issuances = shipment.getRelated("ItemIssuance", [orderId : purchaseOrderId], null, false)
            issuances.each { issuance ->
                exprs.add(EntityCondition.makeCondition("orderItemSeqId", EntityOperator.EQUALS, issuance.orderItemSeqId))
                double issuanceQty = issuance.getDouble("quantity").doubleValue()
                if (shippedQuantities.containsKey(issuance.orderItemSeqId)) {
                    issuanceQty += ((Double)shippedQuantities.get(issuance.orderItemSeqId)).doubleValue()
                }
                shippedQuantities.put(issuance.orderItemSeqId, issuanceQty)
            }
        }
        purchaseOrderItems = EntityUtil.filterByOr(orderItems, exprs)
    } else {
        purchaseOrderItems = purchaseOrder.getRelated("OrderItem", null, null, false)
    }
    purchaseOrderItems = EntityUtil.filterByAnd(purchaseOrderItems, [EntityCondition.makeCondition("statusId", EntityOperator.NOT_IN, ["ITEM_CANCELLED", "ITEM_COMPLETED"])])
}
// convert the unit prices to that of the facility owner's currency
orderCurrencyUnitPriceMap = [:]
if (purchaseOrder && facility) {
    if (ownerAcctgPref) {
        ownerCurrencyUomId = ownerAcctgPref.baseCurrencyUomId
        orderCurrencyUomId = purchaseOrder.currencyUom
        if (!orderCurrencyUomId.equals(ownerCurrencyUomId)) {
            purchaseOrderItems.each { item ->
            orderCurrencyUnitPriceMap.(item.orderItemSeqId) = item.unitPrice
                serviceResults = runService('convertUom',
                        [uomId : orderCurrencyUomId, uomIdTo : ownerCurrencyUomId, originalValue : item.unitPrice])
                if (ServiceUtil.isError(serviceResults)) {
                    request.setAttribute("_ERROR_MESSAGE_", ServiceUtil.getErrorMessage(serviceResults))
                    return
                } else {
                    convertedValue = serviceResults.convertedValue
                    if (convertedValue) {
                        item.unitPrice = convertedValue
                    }
                }
            }
        }

        // put the pref currency in the map for display and form use
        context.currencyUomId = ownerCurrencyUomId
        context.orderCurrencyUomId = orderCurrencyUomId
    } else {
        request.setAttribute("_ERROR_MESSAGE_", "Either no owner party was set for this facility, or no accounting preferences were set for this owner party.")
    }
}
context.orderCurrencyUnitPriceMap = orderCurrencyUnitPriceMap

receivedQuantities = [:]
salesOrderItems = [:]
if (purchaseOrderItems) {
    context.firstOrderItem = EntityUtil.getFirst(purchaseOrderItems)
    context.purchaseOrderItemsSize = purchaseOrderItems.size()
    purchaseOrderItems.each { thisItem ->
        totalReceived = 0.0
        receipts = thisItem.getRelated("ShipmentReceipt", null, null, false)
        if (receipts) {
            receipts.each { rec ->
                if (!shipment || (rec.shipmentId && rec.shipmentId.equals(shipment.shipmentId))) {
                    accepted = rec.getDouble("quantityAccepted")
                    rejected = rec.getDouble("quantityRejected")
                    if (accepted) {
                        totalReceived += accepted.doubleValue()
                    }
                    if (rejected) {
                        totalReceived += rejected.doubleValue()
                    }
                }
            }
        }
        receivedQuantities.put(thisItem.orderItemSeqId, new Double(totalReceived))
        //----------------------
        salesOrderItemAssocs = from("OrderItemAssoc").where(orderItemAssocTypeId : 'PURCHASE_ORDER', toOrderId : thisItem.orderId, toOrderItemSeqId : thisItem.orderItemSeqId).queryList()
        if (salesOrderItemAssocs) {
            salesOrderItem = EntityUtil.getFirst(salesOrderItemAssocs)
            salesOrderItems.put(thisItem.orderItemSeqId, salesOrderItem)
        }
    }
}

receivedItems = null
if (purchaseOrder) {
    receivedItems = from("ShipmentReceiptAndItem").where("orderId", purchaseOrderId, "facilityId", facilityId).queryList()
    context.receivedItems = receivedItems
}

invalidProductId = null
if (productId && !product) {
    invalidProductId = "No product found with product ID: [" + productId + "]"
    context.invalidProductId = invalidProductId
}

// reject reasons
rejectReasons = from("RejectionReason").queryList()

// inv item types
inventoryItemTypes = from("InventoryItemType").queryList()

// facilities
facilities = from("Facility").queryList()

// default per unit cost for both shipment or individual product
standardCosts = [:]
if (ownerAcctgPref) {

    // get the unit cost of the products in a shipment
    if (purchaseOrderItems) {
        purchaseOrderItems.each { orderItem ->
            productId = orderItem.productId
            if (productId) {
                result = runService('getProductCost', [productId : productId, currencyUomId : ownerAcctgPref.baseCurrencyUomId,
                                                               costComponentTypePrefix : 'EST_STD', userLogin : request.getAttribute("userLogin")])
                if (ServiceUtil.isSuccess(result)) {
                    standardCosts.put(productId, result.productCost)
                }
            }
        }
    }

    // get the unit cost of a single product
    if (productId) {
        result = runService('getProductCost', [productId : productId, currencyUomId : ownerAcctgPref.baseCurrencyUomId,
                                                       costComponentTypePrefix : 'EST_STD', userLogin : request.getAttribute("userLogin")])
        if (ServiceUtil.isSuccess(result)) {
            standardCosts.put(productId, result.productCost)
        }
    }
}

context.facilityId = facilityId
context.facility = facility
context.purchaseOrder = purchaseOrder
context.product = product
context.shipments = shipments
context.shipment = shipment
context.shippedQuantities = shippedQuantities
context.purchaseOrderItems = purchaseOrderItems
context.receivedQuantities = receivedQuantities
context.salesOrderItems = salesOrderItems
context.rejectReasons = rejectReasons
context.inventoryItemTypes = inventoryItemTypes
context.facilities = facilities
context.standardCosts = standardCosts
