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
        Map serviceCtx = [
                partyId: 'DemoCustomer',
                orderTypeId: 'SALES_ORDER',
                currencyUom: 'USD',
                productStoreId: '9000'
        ]

        List orderPaymentInfo = []
        GenericValue orderContactMech = delegator.makeValue('OrderContactMech', [
                contactMechId: '9015',
                contactMechPurposeTypeId: 'BILLING_LOCATION'
        ])
        orderPaymentInfo << orderContactMech

        GenericValue orderPaymentPreference = delegator.makeValue('OrderPaymentPreference', [
                paymentMethodId: '9015',
                paymentMethodTypeId: 'CREDIT_CARD',
                statusId: 'PAYMENT_NOT_AUTH',
                overflowFlag: 'N',
                maxAmount: 49.26
        ])
        orderPaymentInfo << orderPaymentPreference
        serviceCtx.orderPaymentInfo = orderPaymentInfo

        List orderItemShipGroupInfo = []
        orderContactMech.contactMechPurposeTypeId = 'SHIPPING_LOCATION'
        orderItemShipGroupInfo << orderContactMech

        GenericValue orderItemShipGroup = delegator.makeValue('OrderItemShipGroup', [
                carrierPartyId: 'UPS',
                contactMechId: '9015',
                isGift: 'N',
                shipGroupSeqId: '00001',
                shipmentMethodTypeId: 'NEXT_DAY'
        ])
        orderItemShipGroupInfo << orderItemShipGroup

        GenericValue orderItemShipGroupAssoc = delegator.makeValue('OrderItemShipGroupAssoc', [
                orderItemSeqId: '00001',
                quantity: BigDecimal.ONE,
                shipGroupSeqId: '00001'
        ])
        orderItemShipGroupInfo << orderItemShipGroupAssoc

        GenericValue shippingCharges = delegator.makeValue('OrderAdjustment', [
                orderAdjustmentTypeId: 'SHIPPING_CHARGES',
                shipGroupSeqId: '00001',
                amount: 12.45
        ])
        orderItemShipGroupInfo << shippingCharges

        GenericValue salesTaxUt = delegator.makeValue('OrderAdjustment', [
                orderAdjustmentTypeId: 'SALES_TAX',
                orderItemSeqId: '00001',
                overrideGlAccountId: '224153',
                primaryGeoId: 'UT',
                shipGroupSeqId: '00001',
                sourcePercentage: BigDecimal.valueOf(4.7)
        ])
        salesTaxUt.taxAuthGeoId = 'UT'
        salesTaxUt.taxAuthPartyId = 'UT_TAXMAN'
        salesTaxUt.taxAuthorityRateSeqId = '9004'
        salesTaxUt.amount = BigDecimal.valueOf(1.824)
        salesTaxUt.comments = 'Utah State Sales Tax'
        orderItemShipGroupInfo << salesTaxUt

        GenericValue salesTaxUtahCounty = delegator.makeValue('OrderAdjustment', [
                orderAdjustmentTypeId: 'SALES_TAX',
                orderItemSeqId: '00001',
                overrideGlAccountId: '224153',
                primaryGeoId: 'UT-UTAH',
                shipGroupSeqId: '00001',
                sourcePercentage: BigDecimal.valueOf(0.1)
        ])
        salesTaxUtahCounty.taxAuthGeoId = 'UT-UTAH'
        salesTaxUtahCounty.taxAuthPartyId = 'UT_UTAH_TAXMAN'
        salesTaxUtahCounty.taxAuthorityRateSeqId = '9005'
        salesTaxUtahCounty.amount = BigDecimal.valueOf(0.039)
        salesTaxUtahCounty.comments = 'Utah County, Utah Sales Tax'
        orderItemShipGroupInfo << salesTaxUtahCounty

        GenericValue ofbTax = delegator.makeValue('OrderAdjustment', [
                orderAdjustmentTypeId: 'SALES_TAX',
                orderItemSeqId: '00001',
                overrideGlAccountId: '224000',
                primaryGeoId: '_NA_',
                shipGroupSeqId: '00001',
                sourcePercentage: BigDecimal.valueOf(1)
        ])
        ofbTax.taxAuthGeoId = '_NA_'
        ofbTax.taxAuthPartyId = '_NA_'
        ofbTax.taxAuthorityRateSeqId = '9000'
        ofbTax.amount = BigDecimal.valueOf(0.384)
        ofbTax.comments = '1% OFB _NA_ Tax'
        orderItemShipGroupInfo << ofbTax

        serviceCtx.orderItemShipGroupInfo = orderItemShipGroupInfo

        GenericValue promoAdjustment = delegator.makeValue('OrderAdjustment', [
                orderAdjustmentTypeId: 'PROMOTION_ADJUSTMENT',
                productPromoActionSeqId: '01',
                productPromoId: '9011',
                productPromoRuleId: '01',
                amount: BigDecimal.valueOf(-3.84)
        ])
        serviceCtx.orderAdjustments = [promoAdjustment]

        GenericValue orderItem1 = delegator.makeValue('OrderItem', [
                orderItemSeqId: '00001',
                orderItemTypeId: 'PRODUCT_ORDER_ITEM',
                prodCatalogId: 'DemoCatalog',
                productId: 'GZ-2644',
                quantity: BigDecimal.ONE,
                selectedAmount: BigDecimal.ZERO
        ])
        orderItem1.isPromo = 'N'
        orderItem1.isModifiedPrice = 'N'
        orderItem1.unitPrice = 38.4
        orderItem1.unitListPrice = 48.0
        orderItem1.statusId = 'ITEM_CREATED'

        GenericValue orderItem2 = delegator.makeValue('OrderItem', [
                orderItemSeqId: '00002',
                orderItemTypeId: 'PRODUCT_ORDER_ITEM',
                prodCatalogId: 'DemoCatalog',
                productId: 'GZ-1006-1',
                quantity: BigDecimal.ONE,
                selectedAmount: BigDecimal.ZERO
        ])
        orderItem2.isPromo = 'N'
        orderItem2.isModifiedPrice = 'N'
        orderItem2.unitPrice = 1.99
        orderItem2.unitListPrice = 5.99
        orderItem2.statusId = 'ITEM_CREATED'

        serviceCtx.orderItems = [orderItem1, orderItem2]
        serviceCtx.orderTerms = []

        serviceCtx.placingCustomerPartyId = 'DemoCustomer'
        serviceCtx.endUserCustomerPartyId = 'DemoCustomer'
        serviceCtx.shipToCustomerPartyId = 'DemoCustomer'
        serviceCtx.billToCustomerPartyId = 'DemoCustomer'
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
