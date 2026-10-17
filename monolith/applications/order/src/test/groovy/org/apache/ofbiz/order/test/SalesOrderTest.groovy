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
package org.apache.ofbiz.order.test

import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.service.ServiceUtil
import org.apache.ofbiz.testtools.JunitJupiterTest
import org.apache.ofbiz.testtools.JupiterTestHelper
import org.junit.jupiter.api.Test

@JunitJupiterTest
class SalesOrderTest implements JupiterTestHelper {

    @Test
    void testCreateSalesOrder() {
        String partyId = testParams.partyId ?: 'DemoCustomer'
        String orderTypeId = testParams.orderTypeId ?: 'SALES_ORDER'
        String currencyUom = testParams.currencyUom ?: 'USD'
        String productStoreId = testParams.productStoreId ?: '9000'
        String contactMechId = testParams.contactMechId ?: '9015'
        String contactMechPurposeTypeId = testParams.contactMechPurposeTypeId ?: 'BILLING_LOCATION'
        String paymentMethodId = testParams.paymentMethodId ?: '9015'
        String paymentMethodTypeId = testParams.paymentMethodTypeId ?: 'CREDIT_CARD'
        String statusId = testParams.statusId ?: 'PAYMENT_NOT_AUTH'
        String overflowFlag = testParams.overflowFlag ?: 'N'
        String carrierPartyId = testParams.carrierPartyId ?: 'UPS'
        String isGift = testParams.isGift ?: 'N'
        String shipGroupSeqId = testParams.shipGroupSeqId ?: '00001'
        String shipmentMethodTypeId = testParams.shipmentMethodTypeId ?: 'NEXT_DAY'
        String orderItemSeqId = testParams.orderItemSeqId ?: '00001'
        String orderAdjustmentTypeId = testParams.orderAdjustmentTypeId ?: 'SHIPPING_CHARGES'
        String orderAdjustmentTypeId1 = testParams.orderAdjustmentTypeId1 ?: 'SALES_TAX'
        String overrideGlAccountId = testParams.overrideGlAccountId ?: '224153'
        String primaryGeoId = testParams.primaryGeoId ?: 'UT'
        String primaryGeoId1 = testParams.primaryGeoId1 ?: 'UT-UTAH'
        String overrideGlAccountId1 = testParams.overrideGlAccountId1 ?: '224000'
        String primaryGeoId2 = testParams.primaryGeoId2 ?: '_NA_'
        String orderAdjustmentTypeId4 = testParams.orderAdjustmentTypeId4 ?: 'PROMOTION_ADJUSTMENT'
        String productPromoActionSeqId = testParams.productPromoActionSeqId ?: '01'
        String productPromoId = testParams.productPromoId ?: '9011'
        String productPromoRuleId = testParams.productPromoRuleId ?: '01'
        String orderItemTypeId = testParams.orderItemTypeId ?: 'PRODUCT_ORDER_ITEM'
        String prodCatalogId = testParams.prodCatalogId ?: 'DemoCatalog'
        String productId = testParams.productId ?: 'GZ-2644'
        String orderItemSeqId1 = testParams.orderItemSeqId1 ?: '00002'
        String productId1 = testParams.productId1 ?: 'GZ-1006-1'
        Map serviceCtx = [
                partyId: partyId,
                orderTypeId: orderTypeId,
                currencyUom: currencyUom,
                productStoreId: productStoreId
        ]

        List orderPaymentInfo = []
        GenericValue orderContactMech = delegator.makeValue('OrderContactMech', [
                contactMechId: contactMechId,
                contactMechPurposeTypeId: contactMechPurposeTypeId
        ])
        orderPaymentInfo << orderContactMech

        GenericValue orderPaymentPreference = delegator.makeValue('OrderPaymentPreference', [
                paymentMethodId: paymentMethodId,
                paymentMethodTypeId: paymentMethodTypeId,
                statusId: statusId,
                overflowFlag: overflowFlag,
                maxAmount: 49.26
        ])
        orderPaymentInfo << orderPaymentPreference
        serviceCtx.orderPaymentInfo = orderPaymentInfo

        List orderItemShipGroupInfo = []
        orderContactMech.contactMechPurposeTypeId = 'SHIPPING_LOCATION'
        orderItemShipGroupInfo << orderContactMech

        GenericValue orderItemShipGroup = delegator.makeValue('OrderItemShipGroup', [
                carrierPartyId: carrierPartyId,
                contactMechId: contactMechId,
                isGift: isGift,
                shipGroupSeqId: shipGroupSeqId,
                shipmentMethodTypeId: shipmentMethodTypeId
        ])
        orderItemShipGroupInfo << orderItemShipGroup

        GenericValue orderItemShipGroupAssoc = delegator.makeValue('OrderItemShipGroupAssoc', [
                orderItemSeqId: orderItemSeqId,
                quantity: BigDecimal.ONE,
                shipGroupSeqId: shipGroupSeqId
        ])
        orderItemShipGroupInfo << orderItemShipGroupAssoc

        GenericValue shippingCharges = delegator.makeValue('OrderAdjustment', [
                orderAdjustmentTypeId: orderAdjustmentTypeId,
                shipGroupSeqId: shipGroupSeqId,
                amount: 12.45
        ])
        orderItemShipGroupInfo << shippingCharges

        GenericValue salesTaxUt = delegator.makeValue('OrderAdjustment', [
                orderAdjustmentTypeId: orderAdjustmentTypeId1,
                orderItemSeqId: orderItemSeqId,
                overrideGlAccountId: overrideGlAccountId,
                primaryGeoId: primaryGeoId,
                shipGroupSeqId: shipGroupSeqId,
                sourcePercentage: BigDecimal.valueOf(4.7)
        ])
        salesTaxUt.taxAuthGeoId = primaryGeoId
        salesTaxUt.taxAuthPartyId = 'UT_TAXMAN'
        salesTaxUt.taxAuthorityRateSeqId = '9004'
        salesTaxUt.amount = BigDecimal.valueOf(1.824)
        salesTaxUt.comments = 'Utah State Sales Tax'
        orderItemShipGroupInfo << salesTaxUt

        GenericValue salesTaxUtahCounty = delegator.makeValue('OrderAdjustment', [
                orderAdjustmentTypeId: orderAdjustmentTypeId1,
                orderItemSeqId: orderItemSeqId,
                overrideGlAccountId: overrideGlAccountId,
                primaryGeoId: primaryGeoId1,
                shipGroupSeqId: shipGroupSeqId,
                sourcePercentage: BigDecimal.valueOf(0.1)
        ])
        salesTaxUtahCounty.taxAuthGeoId = primaryGeoId1
        salesTaxUtahCounty.taxAuthPartyId = 'UT_UTAH_TAXMAN'
        salesTaxUtahCounty.taxAuthorityRateSeqId = '9005'
        salesTaxUtahCounty.amount = BigDecimal.valueOf(0.039)
        salesTaxUtahCounty.comments = 'Utah County, Utah Sales Tax'
        orderItemShipGroupInfo << salesTaxUtahCounty

        GenericValue ofbTax = delegator.makeValue('OrderAdjustment', [
                orderAdjustmentTypeId: orderAdjustmentTypeId1,
                orderItemSeqId: orderItemSeqId,
                overrideGlAccountId: overrideGlAccountId1,
                primaryGeoId: primaryGeoId2,
                shipGroupSeqId: shipGroupSeqId,
                sourcePercentage: BigDecimal.valueOf(1)
        ])
        ofbTax.taxAuthGeoId = primaryGeoId2
        ofbTax.taxAuthPartyId = primaryGeoId2
        ofbTax.taxAuthorityRateSeqId = productStoreId
        ofbTax.amount = BigDecimal.valueOf(0.384)
        ofbTax.comments = '1% OFB _NA_ Tax'
        orderItemShipGroupInfo << ofbTax

        serviceCtx.orderItemShipGroupInfo = orderItemShipGroupInfo

        GenericValue promoAdjustment = delegator.makeValue('OrderAdjustment', [
                orderAdjustmentTypeId: orderAdjustmentTypeId4,
                productPromoActionSeqId: productPromoActionSeqId,
                productPromoId: productPromoId,
                productPromoRuleId: productPromoRuleId,
                amount: BigDecimal.valueOf(-3.84)
        ])
        serviceCtx.orderAdjustments = [promoAdjustment]

        GenericValue orderItem1 = delegator.makeValue('OrderItem', [
                orderItemSeqId: orderItemSeqId,
                orderItemTypeId: orderItemTypeId,
                prodCatalogId: prodCatalogId,
                productId: productId,
                quantity: BigDecimal.ONE,
                selectedAmount: BigDecimal.ZERO
        ])
        orderItem1.isPromo = overflowFlag
        orderItem1.isModifiedPrice = overflowFlag
        orderItem1.unitPrice = 38.4
        orderItem1.unitListPrice = 48.0
        orderItem1.statusId = 'ITEM_CREATED'

        GenericValue orderItem2 = delegator.makeValue('OrderItem', [
                orderItemSeqId: orderItemSeqId1,
                orderItemTypeId: orderItemTypeId,
                prodCatalogId: prodCatalogId,
                productId: productId1,
                quantity: BigDecimal.ONE,
                selectedAmount: BigDecimal.ZERO
        ])
        orderItem2.isPromo = overflowFlag
        orderItem2.isModifiedPrice = overflowFlag
        orderItem2.unitPrice = 1.99
        orderItem2.unitListPrice = 5.99
        orderItem2.statusId = 'ITEM_CREATED'

        serviceCtx.orderItems = [orderItem1, orderItem2]
        serviceCtx.orderTerms = []

        serviceCtx.placingCustomerPartyId = partyId
        serviceCtx.endUserCustomerPartyId = partyId
        serviceCtx.shipToCustomerPartyId = partyId
        serviceCtx.billToCustomerPartyId = partyId
        serviceCtx.billFromVendorPartyId = 'Company'
        serviceCtx.userLogin = userLogin

        Map resp = dispatcher.runSync('storeOrder', serviceCtx)
        if (ServiceUtil.isError(resp)) {
            logError(ServiceUtil.getErrorMessage(resp))
            return
        }
        assert resp.orderId
        assert resp.statusId
    }

}
