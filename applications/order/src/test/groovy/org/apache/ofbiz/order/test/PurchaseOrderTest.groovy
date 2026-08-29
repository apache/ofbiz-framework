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
class PurchaseOrderTest implements JupiterTestHelper {

    @Test
    void testCreatePurchaseOrder() {
        String orderItemSeqId = testParams.orderItemSeqId ?: '00001'
        String orderItemTypeId = testParams.orderItemTypeId ?: 'PRODUCT_ORDER_ITEM'
        String prodCatalogId = testParams.prodCatalogId ?: 'DemoCatalog'
        String productId = testParams.productId ?: 'GZ-1000'
        String isPromo = testParams.isPromo ?: 'N'
        String contactMechPurposeTypeId = testParams.contactMechPurposeTypeId ?: 'SHIPPING_LOCATION'
        String contactMechId = testParams.contactMechId ?: '9000'
        String carrierPartyId = testParams.carrierPartyId ?: 'UPS'
        String isGift = testParams.isGift ?: 'N'
        String maySplit = testParams.maySplit ?: 'N'
        String shipGroupSeqId = testParams.shipGroupSeqId ?: '00001'
        String shipmentMethodTypeId = testParams.shipmentMethodTypeId ?: 'NEXT_DAY'
        String partyId = testParams.partyId ?: 'Company'
        String orderTypeId = testParams.orderTypeId ?: 'PURCHASE_ORDER'
        String currencyUom = testParams.currencyUom ?: 'USD'
        String productStoreId = testParams.productStoreId ?: '9000'
        String billToCustomerPartyId = testParams.billToCustomerPartyId ?: 'Company'
        String billFromVendorPartyId = testParams.billFromVendorPartyId ?: 'DemoSupplier'
        String shipFromVendorPartyId = testParams.shipFromVendorPartyId ?: 'Company'
        String supplierAgentPartyId = testParams.supplierAgentPartyId ?: 'DemoSupplier'
        GenericValue orderItem = delegator.makeValue('OrderItem', [
                orderItemSeqId: orderItemSeqId,
                orderItemTypeId: orderItemTypeId,
                prodCatalogId: prodCatalogId,
                productId: productId,
                quantity: new BigDecimal('2'),
                isPromo: isPromo
        ])
        orderItem.unitPrice = 1399.5
        orderItem.unitListPrice = BigDecimal.ZERO
        orderItem.isModifiedPrice = isPromo
        orderItem.statusId = 'ITEM_CREATED'

        GenericValue orderContactMech = delegator.makeValue('OrderContactMech', [
                contactMechPurposeTypeId: contactMechPurposeTypeId,
                contactMechId: contactMechId
        ])

        GenericValue orderItemContactMech = delegator.makeValue('OrderItemContactMech', [
                contactMechPurposeTypeId: contactMechPurposeTypeId,
                contactMechId: contactMechId,
                orderItemSeqId: orderItemSeqId
        ])

        GenericValue orderItemShipGroup = delegator.makeValue('OrderItemShipGroup', [
                carrierPartyId: carrierPartyId,
                contactMechId: contactMechId,
                isGift: isGift,
                maySplit: maySplit,
                shipGroupSeqId: shipGroupSeqId,
                shipmentMethodTypeId: shipmentMethodTypeId
        ])
        orderItemShipGroup.carrierRoleTypeId = 'CARRIER'

        Map serviceCtx = [
                partyId: partyId,
                orderTypeId: orderTypeId,
                currencyUom: currencyUom,
                productStoreId: productStoreId,
                orderItems: [orderItem],
                orderContactMechs: [orderContactMech],
                orderItemContactMechs: [orderItemContactMech],
                orderItemShipGroupInfo: [orderItemShipGroup],
                orderTerms: [],
                orderAdjustments: [],
                billToCustomerPartyId: billToCustomerPartyId,
                billFromVendorPartyId: billFromVendorPartyId,
                shipFromVendorPartyId: shipFromVendorPartyId,
                supplierAgentPartyId: supplierAgentPartyId,
                userLogin: userLogin
        ]

        Map resp = dispatcher.runSync('storeOrder', serviceCtx)
        if (ServiceUtil.isError(resp)) {
            logError(ServiceUtil.getErrorMessage(resp))
            return
        }
        assert resp.orderId
        assert resp.statusId
    }

}
