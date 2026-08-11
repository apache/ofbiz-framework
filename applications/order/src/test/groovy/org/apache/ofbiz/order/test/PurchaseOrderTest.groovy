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
        GenericValue orderItem = delegator.makeValue('OrderItem', [
                orderItemSeqId: '00001',
                orderItemTypeId: 'PRODUCT_ORDER_ITEM',
                prodCatalogId: 'DemoCatalog',
                productId: 'GZ-1000',
                quantity: new BigDecimal('2'),
                isPromo: 'N'
        ])
        orderItem.unitPrice = 1399.5
        orderItem.unitListPrice = BigDecimal.ZERO
        orderItem.isModifiedPrice = 'N'
        orderItem.statusId = 'ITEM_CREATED'

        GenericValue orderContactMech = delegator.makeValue('OrderContactMech', [
                contactMechPurposeTypeId: 'SHIPPING_LOCATION',
                contactMechId: '9000'
        ])

        GenericValue orderItemContactMech = delegator.makeValue('OrderItemContactMech', [
                contactMechPurposeTypeId: 'SHIPPING_LOCATION',
                contactMechId: '9000',
                orderItemSeqId: '00001'
        ])

        GenericValue orderItemShipGroup = delegator.makeValue('OrderItemShipGroup', [
                carrierPartyId: 'UPS',
                contactMechId: '9000',
                isGift: 'N',
                maySplit: 'N',
                shipGroupSeqId: '00001',
                shipmentMethodTypeId: 'NEXT_DAY'
        ])
        orderItemShipGroup.carrierRoleTypeId = 'CARRIER'

        Map serviceCtx = [
                partyId: 'Company',
                orderTypeId: 'PURCHASE_ORDER',
                currencyUom: 'USD',
                productStoreId: '9000',
                orderItems: [orderItem],
                orderContactMechs: [orderContactMech],
                orderItemContactMechs: [orderItemContactMech],
                orderItemShipGroupInfo: [orderItemShipGroup],
                orderTerms: [],
                orderAdjustments: [],
                billToCustomerPartyId: 'Company',
                billFromVendorPartyId: 'DemoSupplier',
                shipFromVendorPartyId: 'Company',
                supplierAgentPartyId: 'DemoSupplier',
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
