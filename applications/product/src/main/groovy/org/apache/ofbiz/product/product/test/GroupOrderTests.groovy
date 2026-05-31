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
package org.apache.ofbiz.product.product.test

import org.apache.ofbiz.base.util.UtilDateTime
import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.service.ServiceUtil
import org.apache.ofbiz.service.testtools.OFBizTestCase

import java.sql.Timestamp

@SuppressWarnings(['LineLength', 'UnnecessaryObjectReferences', 'UnnecessaryGString', 'PublicMethodsBeforeNonPublicMethods', 'ClassSize', 'MethodCount', 'ConsecutiveBlankLines', 'BlockEndsWithBlankLine', 'ClassEndsWithBlankLine'])
class GroupOrderTests extends OFBizTestCase {

    GroupOrderTests(String name) {
        super(name)
    }

    void testGroupOrderLimitReached() {
        GenericValue systemUserLogin = from('UserLogin').where('userLoginId', 'system').queryOne()
        GenericValue adminUserLogin = from('UserLogin').where('userLoginId', 'admin').queryOne()
        Timestamp nowTimestamp = UtilDateTime.nowTimestamp()
        Timestamp thruDate = UtilDateTime.addDaysToTimestamp(nowTimestamp, 1)

        Map createGroupOrderCtx = [
                userLogin: systemUserLogin,
                productId: 'GZ-1000',
                fromDate: nowTimestamp,
                thruDate: thruDate,
                statusId: 'GO_CREATED',
                reqOrderQty: 1.0,
                soldOrderQty: 0.0
        ]
        Map serviceResult = dispatcher.runSync('createProductGroupOrder', createGroupOrderCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        String groupOrderId = serviceResult.groupOrderId
        assert groupOrderId

        // Create an order using the createTestSalesOrderSingle service
        Map orderResult = dispatcher.runSync('createTestSalesOrderSingle', [userLogin: adminUserLogin, productId: 'GZ-1000'])
        assert ServiceUtil.isSuccess(orderResult)

        GenericValue productGroupOrder = from('ProductGroupOrder').where('groupOrderId', groupOrderId).queryOne()
        assert productGroupOrder.soldOrderQty != 0.0

        Map checkExpiredCtx = [
                userLogin: systemUserLogin,
                groupOrderId: groupOrderId
        ]
        Map expiredResult = dispatcher.runSync('checkProductGroupOrderExpired', checkExpiredCtx)
        assert ServiceUtil.isSuccess(expiredResult)

        GenericValue orderItemGroupOrder = from('OrderItemGroupOrder').where('groupOrderId', groupOrderId).queryFirst()
        assert orderItemGroupOrder
        GenericValue orderItem = from('OrderItem').where('orderId', orderItemGroupOrder.orderId, 'orderItemSeqId', orderItemGroupOrder.orderItemSeqId).queryOne()
        assert orderItem.statusId == 'ITEM_APPROVED'
    }

    void testGroupOrderLimitNotReached() {
        GenericValue systemUserLogin = from('UserLogin').where('userLoginId', 'system').queryOne()
        GenericValue adminUserLogin = from('UserLogin').where('userLoginId', 'admin').queryOne()
        Timestamp nowTimestamp = UtilDateTime.nowTimestamp()
        Timestamp thruDate = UtilDateTime.addDaysToTimestamp(nowTimestamp, 1)

        Map createGroupOrderCtx = [
                userLogin: systemUserLogin,
                productId: 'GZ-1001',
                fromDate: nowTimestamp,
                thruDate: thruDate,
                statusId: 'GO_CREATED',
                reqOrderQty: 2.0,
                soldOrderQty: 0.0
        ]
        Map serviceResult = dispatcher.runSync('createProductGroupOrder', createGroupOrderCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        String groupOrderId = serviceResult.groupOrderId
        assert groupOrderId

        // Create an order using the createTestSalesOrderSingle service
        Map orderResult = dispatcher.runSync('createTestSalesOrderSingle', [userLogin: adminUserLogin, productId: 'GZ-1001'])
        assert ServiceUtil.isSuccess(orderResult)

        GenericValue productGroupOrder = from('ProductGroupOrder').where('groupOrderId', groupOrderId).queryOne()
        assert productGroupOrder.soldOrderQty != 0.0

        Map checkExpiredCtx = [
                userLogin: systemUserLogin,
                groupOrderId: groupOrderId
        ]
        Map expiredResult = dispatcher.runSync('checkProductGroupOrderExpired', checkExpiredCtx)
        assert ServiceUtil.isSuccess(expiredResult)

        GenericValue orderItemGroupOrder = from('OrderItemGroupOrder').where('groupOrderId', groupOrderId).queryFirst()
        assert orderItemGroupOrder
        GenericValue orderItem = from('OrderItem').where('orderId', orderItemGroupOrder.orderId, 'orderItemSeqId', orderItemGroupOrder.orderItemSeqId).queryOne()
        assert orderItem.statusId == 'ITEM_CANCELLED'
    }
}
