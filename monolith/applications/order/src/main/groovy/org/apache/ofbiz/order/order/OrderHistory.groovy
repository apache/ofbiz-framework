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
package org.apache.ofbiz.order.order

import org.apache.ofbiz.entity.condition.EntityCondition
import org.apache.ofbiz.entity.condition.EntityOperator

orderId = parameters.orderId
context.orderId = orderId

orderHeader = null
if (orderId) {
    orderHeader = from('OrderHeader').where(orderId: orderId).queryOne()
}

if (orderHeader) {
    shipmentMethodCond = [EntityCondition.makeCondition('changedEntityName', EntityOperator.EQUALS, 'OrderItemShipGroup'),
                          EntityCondition.makeCondition('changedFieldName', EntityOperator.EQUALS, 'shipmentMethodTypeId'),
                          EntityCondition.makeCondition('pkCombinedValueText', EntityOperator.LIKE, orderId + '%')]
    shipmentMethodHistories = from('EntityAuditLog').where(shipmentMethodCond).orderBy('-changedDate').queryList()

    carrierPartyCond = [EntityCondition.makeCondition('changedEntityName', EntityOperator.EQUALS, 'OrderItemShipGroup'),
                        EntityCondition.makeCondition('changedFieldName', EntityOperator.EQUALS, 'carrierPartyId'),
                        EntityCondition.makeCondition('pkCombinedValueText', EntityOperator.LIKE, orderId + '%')]
    carrierPartyHistories = from('EntityAuditLog').where(carrierPartyCond).queryList()

    orderShipmentHistories = []
    shipmentMethodHistories.each { shipmentMethodHistory ->
        orderShipmentHistory = [:]
        if (shipmentMethodHistory.changedFieldName == 'shipmentMethodTypeId') {
            shipmentMethodType = from('ShipmentMethodType').where('shipmentMethodTypeId', shipmentMethodHistory.newValueText).queryOne()
            if (shipmentMethodType != null) {
                carrierPartyHistories.each { carrierPartyHistory ->
                    if (carrierPartyHistory.lastUpdatedTxStamp == shipmentMethodHistory.lastUpdatedTxStamp) {
                        if (carrierPartyHistory.newValueText == '_NA_') {
                            orderShipmentHistory.shipmentMethod = shipmentMethodType.description
                        } else {
                            orderShipmentHistory.shipmentMethod = carrierPartyHistory.newValueText + ' ' + shipmentMethodType.description
                        }
                    }
                }
            }
            orderShipmentHistory.lastUpdated = shipmentMethodHistory.lastUpdatedTxStamp
            orderShipmentHistory.changedDate = shipmentMethodHistory.changedDate
            orderShipmentHistory.changedByUser = shipmentMethodHistory.changedByInfo
            orderShipmentHistories.add(orderShipmentHistory)
        }
    }
    context.orderShipmentHistories = orderShipmentHistories

    changedByInfoCond = [EntityCondition.makeCondition('changedEntityName', EntityOperator.EQUALS, 'OrderItem'),
                         EntityCondition.makeCondition('changedFieldName', EntityOperator.EQUALS, 'changeByUserLoginId'),
                         EntityCondition.makeCondition('pkCombinedValueText', EntityOperator.LIKE, orderId + '%')]
    changedByInfoHistories = from('EntityAuditLog').where(changedByInfoCond).orderBy('-changedDate').queryList()

    orderUnitPriceHistories = []
    unitPriceCond = [EntityCondition.makeCondition('changedEntityName', EntityOperator.EQUALS, 'OrderItem'),
                     EntityCondition.makeCondition('changedFieldName', EntityOperator.EQUALS, 'unitPrice'),
                     EntityCondition.makeCondition('pkCombinedValueText', EntityOperator.LIKE, orderId + '%')]
    unitPriceHistories = from('EntityAuditLog').where(unitPriceCond).orderBy('-changedDate').queryList()
    unitPriceHistories.each { unitPriceHistory ->
        orderUnitPriceHistory = [:]
        if ((unitPriceHistory.oldValueText) && (unitPriceHistory.newValueText)) {
            if (unitPriceHistory.oldValueText as BigDecimal != unitPriceHistory.newValueText as BigDecimal) {
                orderUnitPriceHistory.oldValue = unitPriceHistory.oldValueText
                orderUnitPriceHistory.newValue = unitPriceHistory.newValueText
                orderUnitPriceHistory.changedDate = unitPriceHistory.changedDate
                orderItemSeqId = (unitPriceHistory.pkCombinedValueText)
                        .substring((unitPriceHistory.pkCombinedValueText).indexOf('::') + 2, (unitPriceHistory.pkCombinedValueText).length())
                orderItem = from('OrderItem').where('orderId', orderId, 'orderItemSeqId', orderItemSeqId).queryOne()
                orderUnitPriceHistory.productId = orderItem.productId
                changedByInfoHistories.each { changedByInfoHistory ->
                    if (changedByInfoHistory.lastUpdatedTxStamp == unitPriceHistory.lastUpdatedTxStamp) {
                        if (changedByInfoHistory.newValueText) {
                            orderUnitPriceHistory.changedByUser = changedByInfoHistory.newValueText
                        } else {
                            orderUnitPriceHistory.changedByUser = changedByInfoHistory.oldValueText
                        }
                     }
                }
                orderUnitPriceHistories.add(orderUnitPriceHistory)
            }
        }
    }

    context.orderUnitPriceHistories = orderUnitPriceHistories
    orderQuantityHistories = []
    quantityCond = [EntityCondition.makeCondition('changedEntityName', EntityOperator.EQUALS, 'OrderItem'),
                    EntityCondition.makeCondition('changedFieldName', EntityOperator.EQUALS, 'quantity'),
                    EntityCondition.makeCondition('pkCombinedValueText', EntityOperator.LIKE, orderId + '%')]
    quantityHistories = from('EntityAuditLog').where(quantityCond).orderBy('-changedDate').queryList()
    quantityHistories.each { quantityHistory ->
        orderQuantityHistory = [:]
        if ((quantityHistory.oldValueText) && (quantityHistory.newValueText)) {
            if (quantityHistory.oldValueText as BigDecimal != quantityHistory.newValueText as BigDecimal) {
                orderQuantityHistory.oldValue =  new BigDecimal(quantityHistory.oldValueText)
                orderQuantityHistory.newValue = quantityHistory.newValueText
                orderQuantityHistory.changedDate = quantityHistory.changedDate
                orderItemSeqId = (quantityHistory.pkCombinedValueText)
                        .substring((quantityHistory.pkCombinedValueText).indexOf('::') + 2, (quantityHistory.pkCombinedValueText).length())
                orderItem = from('OrderItem').where('orderId', orderId, 'orderItemSeqId', orderItemSeqId).queryOne()
                orderQuantityHistory.productId = orderItem.productId
                changedByInfoHistories.each { changedByInfoHistory ->
                    if (changedByInfoHistory.lastUpdatedTxStamp == quantityHistory.lastUpdatedTxStamp) {
                        if (changedByInfoHistory.newValueText) {
                            orderQuantityHistory.changedByUser = changedByInfoHistory.newValueText
                        } else {
                            orderQuantityHistory.changedByUser = changedByInfoHistory.oldValueText
                        }
                    }
                }
                orderQuantityHistories.add(orderQuantityHistory)
            }
        }
    }
    context.orderQuantityHistories = orderQuantityHistories
}
