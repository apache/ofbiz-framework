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
import org.apache.ofbiz.entity.util.EntityQuery
import org.apache.ofbiz.service.ServiceUtil
import org.apache.ofbiz.service.testtools.OFBizTestCase

/**
 * OFBIZ-13449 regression: during PO receiving under a locale whose decimal
 * separator is a comma (e.g. pl_PL), the order-currency unit price submitted
 * from the receiving form must be parsed locale-aware (7,41 -> 7.41), not
 * corrupted (-> 741) by the locale-insensitive new BigDecimal(String).
 */
class ReceivePoLocaleTest extends OFBizTestCase {

    ReceivePoLocaleTest(String name) {
        super(name)
    }

    void testUpdateOrderItemUnitPriceUnderPolishLocale() {
        String orderId = 'TEST13449PO'
        // clean any leftovers from a previous run
        delegator.removeByAnd('OrderItem', [orderId: orderId])
        delegator.removeByAnd('OrderHeader', [orderId: orderId])

        delegator.makeValue('OrderHeader', [
                orderId: orderId,
                orderTypeId: 'PURCHASE_ORDER',
                statusId: 'ORDER_CREATED',
                currencyUom: 'PLN',
                orderDate: UtilDateTime.nowTimestamp(),
                entryDate: UtilDateTime.nowTimestamp()
        ]).create()
        delegator.makeValue('OrderItem', [
                orderId: orderId,
                orderItemSeqId: '00001',
                orderItemTypeId: 'PRODUCT_ORDER_ITEM',
                statusId: 'ITEM_CREATED',
                quantity: 1.0,
                unitPrice: 1.0,
                isPromo: 'N'
        ]).create()

        // Submit the price exactly as the pl_PL receiving form would: "7,41"
        Map<String, Object> ctx = [
                orderId: orderId,
                orderItemSeqId: '00001',
                quantityAccepted: 1.0,
                orderCurrencyUnitPrice: '7,41',
                userLogin: userLogin,
                locale: new Locale('pl', 'PL')
        ]
        Map<String, Object> result = dispatcher.runSync('updateIssuanceShipmentAndPoOnReceiveInventory', ctx)
        assert ServiceUtil.isSuccess(result)

        GenericValue updated = EntityQuery.use(delegator).from('OrderItem')
                .where(orderId: orderId, orderItemSeqId: '00001').queryOne()
        // 7,41 (pl_PL) must be stored as 7.41 -- not 741. Currently FAILS on trunk and on the
        // "type as BigDecimal" fix, because NumberConverters.fromString ignores the caller locale
        // (uses Locale.getDefault()). Should pass once the price is parsed with the request locale.
        assert updated.unitPrice == 7.41

        delegator.removeByAnd('OrderItem', [orderId: orderId])
        delegator.removeByAnd('OrderHeader', [orderId: orderId])
    }

}
