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
package org.apache.ofbiz.shipment

import org.apache.ofbiz.service.ServiceUtil
import org.apache.ofbiz.service.testtools.OFBizTestCase

class ShipmentCostTests extends OFBizTestCase {
    ShipmentCostTests(String name) {
        super(name)
    }

    /**
     * ShipmentCostEstimates from ShipmentCostTestData.xml
     *
     | Method | wghBr | qtyBr | prBr | Party    | Carrier   | flat | itemFlat | percent |
     | ------ | ----- | ----- | ---- | -------- | --------- | ---- | -------- | ------- |
     | ROAD   |       |       |      |          |           | 10   | 0        | 0       |
     | AIR    |       |       |      |          |           | 0    | 0        | 4       |
     | LOCAL  |       |       |      |          |           | 10   | 0        | 0       |
     | ROAD   | W1    |       |      |          | UPS_BREAK | 9    | 0        | 0       |
     | ROAD   | W2    |       |      |          | UPS_BREAK | 10   | 0        | 0       |
     | ROAD   | W3    |       |      |          | UPS_BREAK | 11   | 0        | 0       |
     | AIR    |       | Q1    |      |          | UPS_BREAK | 12   | 0        | 0       |
     | AIR    |       | Q2    |      |          | UPS_BREAK | 13   | 0        | 0       |
     | AIR    |       | Q3    |      |          | UPS_BREAK | 14   | 0        | 0       |
     | LOCAL  |       |       | P1   |          | UPS_BREAK | 15   | 0        | 0       |
     | LOCAL  |       |       | P2   |          | UPS_BREAK | 16   | 0        | 0       |
     | LOCAL  |       |       | P3   |          | UPS_BREAK | 17   | 0        | 0       |
     | LOCAL  | W2    |       | P2   |          | UPS_BREAK | 18   | 0        | 0       |
     | LOCAL  | W2    |       | P3   |          | UPS_BREAK | 19   | 0        | 0       |
     | LOCAL  | W2    |       | P3   |          | UPS_BREAK | 19   | 0        | 0       |
     | LOCAL  | W3    |       | P2   |          | UPS_BREAK | 20   | 0        | 0       |
     | ROAD   |       |       |      | RECEIVER | UPS_MULTI | 1    | 0        | 0       |
     | ROAD   |       |       | P2   | RECEIVER | UPS_MULTI | 2    | 0        | 0       |
     | ROAD   |       |       |      |          | UPS_MULTI | 3    | 0        | 0       |

     With break :

     | id  | BreakType | from | to    |
     | --- | --------- | ---- | ----- |
     | W1  | Weight    | 0    | 99    |
     | W2  | Weight    | 100  | 999   |
     | W3  | Weight    | 1000 | 0     |
     | P1  | Price     | 0    | 99    |
     | P2  | Price     | 100  | 999   |
     | P3  | Price     | 1000 | 99999 |
     | Q1  | Quantity  | 0    | 100   |
     | Q2  | Quantity  | 100  | 1000  |
     | Q3  | Quantity  | 1000 | 0     |

     */

    void testCalculateSimpleShipmentCostFlatValue() {
        Map serviceCtx = [
                shippableItemInfo   : [[:]],
                shippingCountryCode : 'USA',
                shipmentMethodTypeId: 'ROAD',
                carrierPartyId      : 'UPS_SIMPLE',
                carrierRoleTypeId   : 'CARRIER',
                productStoreId      : 'ShipCost',
                shippableQuantity   : 10 as BigDecimal,
                shippableWeight     : 10 as BigDecimal,
                shippableTotal      : 10 as BigDecimal,
                userLogin           : userLogin
        ]
        Map resultMap = dispatcher.runSync('calcShipmentCostEstimate', serviceCtx)
        assert ServiceUtil.isSuccess(resultMap)
        assertEquals 10d, resultMap.shippingEstimateAmount as Double
    }

    void testCalculateSimpleShipmentCostPercentValue() {
        Map serviceCtx = [
                shippableItemInfo   : [[:]],
                shippingCountryCode : 'USA',
                shipmentMethodTypeId: 'AIR',
                carrierPartyId      : 'UPS_SIMPLE',
                carrierRoleTypeId   : 'CARRIER',
                productStoreId      : 'ShipCost',
                shippableQuantity   : 10 as BigDecimal,
                shippableWeight     : 10 as BigDecimal,
                shippableTotal      : 10 as BigDecimal,
                userLogin           : userLogin
        ]
        Map resultMap = dispatcher.runSync('calcShipmentCostEstimate', serviceCtx)
        assert ServiceUtil.isSuccess(resultMap)
        assertEquals 0.4d, resultMap.shippingEstimateAmount as Double
    }

    void testCalculateWeightBreakShipmentCostFlatValue() {
        Map serviceCtx = [
                shippableItemInfo   : [[:]],
                shippingCountryCode : 'USA',
                shipmentMethodTypeId: 'ROAD',
                carrierPartyId      : 'UPS_BREAK',
                carrierRoleTypeId   : 'CARRIER',
                productStoreId      : 'ShipCost',
                shippableQuantity   : 10 as BigDecimal,
                shippableWeight     : 10 as BigDecimal,
                shippableTotal      : 10 as BigDecimal,
                userLogin           : userLogin
        ]

        Map resultMap = dispatcher.runSync('calcShipmentCostEstimate', serviceCtx)
        assert ServiceUtil.isSuccess(resultMap)
        assertEquals 9d, resultMap.shippingEstimateAmount as Double

        serviceCtx = [
                shippableItemInfo   : [[:]],
                shippingCountryCode : 'USA',
                shipmentMethodTypeId: 'ROAD',
                carrierPartyId      : 'UPS_BREAK',
                carrierRoleTypeId   : 'CARRIER',
                productStoreId      : 'ShipCost',
                shippableQuantity   : 10 as BigDecimal,
                shippableWeight     : 100 as BigDecimal,
                shippableTotal      : 10 as BigDecimal,
                userLogin           : userLogin
        ]

        resultMap = dispatcher.runSync('calcShipmentCostEstimate', serviceCtx)
        assert ServiceUtil.isSuccess(resultMap)
        assertEquals 10d, resultMap.shippingEstimateAmount as Double

        serviceCtx = [
                shippableItemInfo   : [[:]],
                shippingCountryCode : 'USA',
                shipmentMethodTypeId: 'ROAD',
                carrierPartyId      : 'UPS_BREAK',
                carrierRoleTypeId   : 'CARRIER',
                productStoreId      : 'ShipCost',
                shippableQuantity   : 10 as BigDecimal,
                shippableWeight     : 1000 as BigDecimal,
                shippableTotal      : 10 as BigDecimal,
                userLogin           : userLogin
        ]
        resultMap = dispatcher.runSync('calcShipmentCostEstimate', serviceCtx)
        assert ServiceUtil.isSuccess(resultMap)
        assertEquals 11d, resultMap.shippingEstimateAmount as Double
    }

    void testCalculateQuantityBreakShipmentCostFlatValue() {
        Map serviceCtx = [
                shippableItemInfo   : [[:]],
                shippingCountryCode : 'USA',
                shipmentMethodTypeId: 'AIR',
                carrierPartyId      : 'UPS_BREAK',
                carrierRoleTypeId   : 'CARRIER',
                productStoreId      : 'ShipCost',
                shippableQuantity   : 10 as BigDecimal,
                shippableWeight     : 10 as BigDecimal,
                shippableTotal      : 10 as BigDecimal,
                userLogin           : userLogin
        ]
        Map resultMap = dispatcher.runSync('calcShipmentCostEstimate', serviceCtx)
        assert ServiceUtil.isSuccess(resultMap)
        assertEquals 12d, resultMap.shippingEstimateAmount as Double

        serviceCtx = [
                shippableItemInfo   : [[:]],
                shippingCountryCode : 'USA',
                shipmentMethodTypeId: 'AIR',
                carrierPartyId      : 'UPS_BREAK',
                carrierRoleTypeId   : 'CARRIER',
                productStoreId      : 'ShipCost',
                shippableQuantity   : 100 as BigDecimal,
                shippableWeight     : 10 as BigDecimal,
                shippableTotal      : 10 as BigDecimal,
                userLogin           : userLogin
        ]
        resultMap = dispatcher.runSync('calcShipmentCostEstimate', serviceCtx)
        assert ServiceUtil.isSuccess(resultMap)
        assertEquals 13d, resultMap.shippingEstimateAmount as Double

        serviceCtx = [
                shippableItemInfo   : [[:]],
                shippingCountryCode : 'USA',
                shipmentMethodTypeId: 'AIR',
                carrierPartyId      : 'UPS_BREAK',
                carrierRoleTypeId   : 'CARRIER',
                productStoreId      : 'ShipCost',
                shippableQuantity   : 1000 as BigDecimal,
                shippableWeight     : 10 as BigDecimal,
                shippableTotal      : 10 as BigDecimal,
                userLogin           : userLogin
        ]
        resultMap = dispatcher.runSync('calcShipmentCostEstimate', serviceCtx)
        assert ServiceUtil.isSuccess(resultMap)
        assertEquals 14d, resultMap.shippingEstimateAmount as Double
    }

    void testCalculatePriceBreakShipmentCostFlatValue() {
        Map serviceCtx = [
                shippableItemInfo   : [[:]],
                shippingCountryCode : 'USA',
                shipmentMethodTypeId: 'LOCAL_DELIVERY',
                carrierPartyId      : 'UPS_BREAK',
                carrierRoleTypeId   : 'CARRIER',
                productStoreId      : 'ShipCost',
                shippableQuantity   : 10 as BigDecimal,
                shippableWeight     : 10 as BigDecimal,
                shippableTotal      : 10 as BigDecimal,
                userLogin           : userLogin
        ]
        Map resultMap = dispatcher.runSync('calcShipmentCostEstimate', serviceCtx)
        assert ServiceUtil.isSuccess(resultMap)
        assertEquals 15d, resultMap.shippingEstimateAmount as Double

        serviceCtx = [
                shippableItemInfo   : [[:]],
                shippingCountryCode : 'USA',
                shipmentMethodTypeId: 'LOCAL_DELIVERY',
                carrierPartyId      : 'UPS_BREAK',
                carrierRoleTypeId   : 'CARRIER',
                productStoreId      : 'ShipCost',
                shippableQuantity   : 10 as BigDecimal,
                shippableWeight     : 10 as BigDecimal,
                shippableTotal      : 100 as BigDecimal,
                userLogin           : userLogin
        ]
        resultMap = dispatcher.runSync('calcShipmentCostEstimate', serviceCtx)
        assert ServiceUtil.isSuccess(resultMap)
        assertEquals 16d, resultMap.shippingEstimateAmount as Double

        serviceCtx = [
                shippableItemInfo   : [[:]],
                shippingCountryCode : 'USA',
                shipmentMethodTypeId: 'LOCAL_DELIVERY',
                carrierPartyId      : 'UPS_BREAK',
                carrierRoleTypeId   : 'CARRIER',
                productStoreId      : 'ShipCost',
                shippableQuantity   : 10 as BigDecimal,
                shippableWeight     : 10 as BigDecimal,
                shippableTotal      : 1000 as BigDecimal,
                userLogin           : userLogin
        ]
        resultMap = dispatcher.runSync('calcShipmentCostEstimate', serviceCtx)
        assert ServiceUtil.isSuccess(resultMap)
        assertEquals 17d, resultMap.shippingEstimateAmount as Double
    }

    void testCalculatePriceAndWeightBreakShipmentCostFlatValue() {
        Map serviceCtx = [
                shippableItemInfo   : [[:]],
                shippingCountryCode : 'USA',
                shipmentMethodTypeId: 'LOCAL_DELIVERY',
                carrierPartyId      : 'UPS_BREAK',
                carrierRoleTypeId   : 'CARRIER',
                productStoreId      : 'ShipCost',
                shippableQuantity   : 10 as BigDecimal,
                shippableWeight     : 100 as BigDecimal,
                shippableTotal      : 100 as BigDecimal,
                userLogin           : userLogin
        ]
        Map resultMap = dispatcher.runSync('calcShipmentCostEstimate', serviceCtx)
        assert ServiceUtil.isSuccess(resultMap)
        assertEquals 18d, resultMap.shippingEstimateAmount as Double

        serviceCtx = [
                shippableItemInfo   : [[:]],
                shippingCountryCode : 'USA',
                shipmentMethodTypeId: 'LOCAL_DELIVERY',
                carrierPartyId      : 'UPS_BREAK',
                carrierRoleTypeId   : 'CARRIER',
                productStoreId      : 'ShipCost',
                shippableQuantity   : 10 as BigDecimal,
                shippableWeight     : 100 as BigDecimal,
                shippableTotal      : 1000 as BigDecimal,
                userLogin           : userLogin
        ]
        resultMap = dispatcher.runSync('calcShipmentCostEstimate', serviceCtx)
        assert ServiceUtil.isSuccess(resultMap)
        assertEquals 19d, resultMap.shippingEstimateAmount as Double

        serviceCtx = [
                shippableItemInfo   : [[:]],
                shippingCountryCode : 'USA',
                shipmentMethodTypeId: 'LOCAL_DELIVERY',
                carrierPartyId      : 'UPS_BREAK',
                carrierRoleTypeId   : 'CARRIER',
                productStoreId      : 'ShipCost',
                shippableQuantity   : 10 as BigDecimal,
                shippableWeight     : 1000 as BigDecimal,
                shippableTotal      : 100 as BigDecimal,
                userLogin           : userLogin
        ]
        resultMap = dispatcher.runSync('calcShipmentCostEstimate', serviceCtx)
        assert ServiceUtil.isSuccess(resultMap)
        assertEquals 20d, resultMap.shippingEstimateAmount as Double

        serviceCtx = [
                shippableItemInfo   : [[:]],
                shippingCountryCode : 'USA',
                shipmentMethodTypeId: 'LOCAL_DELIVERY',
                carrierPartyId      : 'UPS_BREAK',
                carrierRoleTypeId   : 'CARRIER',
                productStoreId      : 'ShipCost',
                shippableQuantity   : 10 as BigDecimal,
                shippableWeight     : 1000 as BigDecimal,
                shippableTotal      : 1000 as BigDecimal,
                userLogin           : userLogin
        ]
        resultMap = dispatcher.runSync('calcShipmentCostEstimate', serviceCtx)
        assert ServiceUtil.isSuccess(resultMap)
        assertEquals 21d, resultMap.shippingEstimateAmount as Double
    }

    void testPriceBreakOverRangeAndFailedShipmentCost() {
        Map serviceCtx = [
                shippableItemInfo   : [[:]],
                shippingCountryCode : 'USA',
                shipmentMethodTypeId: 'LOCAL_DELIVERY',
                carrierPartyId      : 'UPS_BREAK',
                carrierRoleTypeId   : 'CARRIER',
                productStoreId      : 'ShipCost',
                shippableQuantity   : 10 as BigDecimal,
                shippableWeight     : 100 as BigDecimal,
                shippableTotal      : 100000 as BigDecimal,
                userLogin           : userLogin
        ]
        Map resultMap = dispatcher.runSync('calcShipmentCostEstimate', serviceCtx)
        assert ServiceUtil.isFailure(resultMap)
    }

    void testCalculateMultipleWithPartyShipmentCostFlatValue() {
        Map serviceCtx = [
                shippableItemInfo   : [[:]],
                shippingCountryCode : 'USA',
                shipmentMethodTypeId: 'ROAD',
                carrierPartyId      : 'UPS_MULTI',
                carrierRoleTypeId   : 'CARRIER',
                productStoreId      : 'ShipCost',
                partyId             : 'RECEIVER',
                shippableQuantity   : 10 as BigDecimal,
                shippableWeight     : 10 as BigDecimal,
                shippableTotal      : 10 as BigDecimal,
                userLogin           : userLogin
        ]
        Map resultMap = dispatcher.runSync('calcShipmentCostEstimate', serviceCtx)
        assert ServiceUtil.isSuccess(resultMap)
        assertEquals 1d, resultMap.shippingEstimateAmount as Double
    }

    void testCalculateMultipleWithBreakShipmentCostFlatValue() {
        Map serviceCtx = [
                shippableItemInfo   : [[:]],
                shippingCountryCode : 'USA',
                shipmentMethodTypeId: 'ROAD',
                carrierPartyId      : 'UPS_MULTI',
                carrierRoleTypeId   : 'CARRIER',
                productStoreId      : 'ShipCost',
                partyId             : 'RECEIVER',
                shippableQuantity   : 10 as BigDecimal,
                shippableWeight     : 10 as BigDecimal,
                shippableTotal      : 100 as BigDecimal,
                userLogin           : userLogin
        ]
        Map resultMap = dispatcher.runSync('calcShipmentCostEstimate', serviceCtx)
        assert ServiceUtil.isSuccess(resultMap)
        assertEquals 2d, resultMap.shippingEstimateAmount as Double
    }
}
