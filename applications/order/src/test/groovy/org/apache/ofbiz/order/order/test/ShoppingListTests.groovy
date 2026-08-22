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
package org.apache.ofbiz.order.order.test

import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.service.ServiceUtil
import org.apache.ofbiz.testtools.JunitJupiterTest
import org.apache.ofbiz.testtools.JupiterTestHelper
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test

@JunitJupiterTest
class ShoppingListTests implements JupiterTestHelper {

    @Test
    @Order(1)
    void testCreateShoppingList() {
        String userLoginId = testParams.userLoginId ?: 'DemoCustomer'
        String partyId = testParams.partyId ?: 'DemoCustomer'
        String shoppingListTypeId = testParams.shoppingListTypeId ?: 'SLT_WISH_LIST'
        String productStoreId = testParams.productStoreId ?: '9000'
        String listName = testParams.listName ?: 'Demo Wish List 1'
        String isActive = testParams.isActive ?: 'Y'
        String currencyUom = testParams.currencyUom ?: 'USD'
        GenericValue userLogin = delegator.findOne('UserLogin', [userLoginId: userLoginId], false)
        Map serviceCtx = [
                partyId: partyId,
                shoppingListTypeId: shoppingListTypeId,
                productStoreId: productStoreId,
                listName: listName,
                isActive: isActive,
                currencyUom: currencyUom,
                userLogin: userLogin
        ]
        Map resultMap = dispatcher.runSync('createShoppingList', serviceCtx, 600, true)
        String shoppingListId = resultMap.shoppingListId
        GenericValue shoppingList = delegator.findOne('ShoppingList', [shoppingListId: shoppingListId], false)
        assert ServiceUtil.isSuccess(resultMap)
        assert shoppingList
        assert shoppingList.partyId == partyId
        assert shoppingList.listName == listName
    }

    @Test
    @Order(2)
    void testCreateShoppingListItem() {
        String userLoginId = testParams.userLoginId ?: 'DemoCustomer'
        String productId = testParams.productId ?: 'GZ-8544'
        String productStoreId = testParams.productStoreId ?: '9000'
        GenericValue userLogin = delegator.findOne('UserLogin', [userLoginId: userLoginId], false)
        String shoppingListId = 'DemoWishList'
        Map serviceCtx = [
                shoppingListId: shoppingListId,
                productId: productId,
                quantity: new BigDecimal(3),
                productStoreId: productStoreId,
                userLogin: userLogin
        ]
        Map resultMap = dispatcher.runSync('createShoppingListItem', serviceCtx)
        String shoppingListItemSeqId = resultMap.shoppingListItemSeqId
        GenericValue shoppingListItem = from('ShoppingListItem').where('shoppingListItemSeqId', shoppingListItemSeqId).queryOne()
        assert ServiceUtil.isSuccess(resultMap)
        assert shoppingListItem
        assert shoppingListItem.productId == productId
        assert shoppingListItem.quantity == 3
    }

    @Test
    @Order(3)
    void testCreateShoppingListItemWithSameProduct() {
        String userLoginId = testParams.userLoginId ?: 'DemoCustomer'
        String productId = testParams.productId ?: 'GZ-2644'
        String productStoreId = testParams.productStoreId ?: '9000'
        GenericValue userLogin = delegator.findOne('UserLogin', [userLoginId: userLoginId], false)
        String shoppingListId = 'DemoWishList'
        Map serviceCtx = [
                shoppingListId: shoppingListId,
                productId: productId,
                quantity: new BigDecimal(2),
                productStoreId: productStoreId,
                userLogin: userLogin
        ]
        Map resultMap = dispatcher.runSync('createShoppingListItem', serviceCtx)
        String shoppingListItemSeqId = resultMap.shoppingListItemSeqId
        GenericValue shoppingListItem = from('ShoppingListItem').where('shoppingListItemSeqId', shoppingListItemSeqId).queryOne()
        assert ServiceUtil.isSuccess(resultMap)
        assert shoppingListItem
        assert shoppingListItem.quantity == 7
    }

    @Test
    @Order(4)
    void testUpdateShoppingList() {
        String userLoginId = testParams.userLoginId ?: 'DemoCustomer'
        String shoppingListId = testParams.shoppingListId ?: 'DemoWishList'
        String listName = testParams.listName ?: 'New Demo Wish List'
        GenericValue userLogin = delegator.findOne('UserLogin', [userLoginId: userLoginId], false)
        Map serviceCtx = [
                shoppingListId: shoppingListId,
                listName: listName,
                userLogin: userLogin
        ]
        Map resultMap = dispatcher.runSync('updateShoppingList', serviceCtx)
        GenericValue shoppingList = delegator.findOne('ShoppingList', [shoppingListId: serviceCtx.shoppingListId], false)
        assert ServiceUtil.isSuccess(resultMap)
        assert shoppingList
        assert shoppingList.listName == listName
    }

    @Test
    @Order(5)
    void testUpdateShoppingListItem() {
        String userLoginId = testParams.userLoginId ?: 'DemoCustomer'
        String shoppingListId = testParams.shoppingListId ?: 'DemoWishList'
        String shoppingListItemSeqId = testParams.shoppingListItemSeqId ?: '00002'
        GenericValue userLogin = delegator.findOne('UserLogin', [userLoginId: userLoginId], false)
        Map serviceCtx = [
                shoppingListId: shoppingListId,
                shoppingListItemSeqId: shoppingListItemSeqId,
                quantity: new BigDecimal(4),
                userLogin: userLogin
        ]
        Map resultMap = dispatcher.runSync('updateShoppingListItem', serviceCtx)
        GenericValue shoppingListItem = delegator.findOne('ShoppingListItem',
                [shoppingListId: serviceCtx.shoppingListId, 'shoppingListItemSeqId': shoppingListItemSeqId], false)
        assert ServiceUtil.isSuccess(resultMap)
        assert shoppingListItem
        assert shoppingListItem.quantity == 4
    }

    @Test
    @Order(6)
    void testUpdateShoppingListItemWithZeroQty() {
        String userLoginId = testParams.userLoginId ?: 'DemoCustomer'
        String shoppingListId = testParams.shoppingListId ?: 'DemoWishList'
        String shoppingListItemSeqId = testParams.shoppingListItemSeqId ?: '00003'
        GenericValue userLogin = delegator.findOne('UserLogin', [userLoginId: userLoginId], false)
        Map serviceCtx = [
                shoppingListId: shoppingListId,
                shoppingListItemSeqId: shoppingListItemSeqId,
                quantity: new BigDecimal(0),
                userLogin: userLogin
        ]
        Map resultMap = dispatcher.runSync('updateShoppingListItem', serviceCtx)
        GenericValue shoppingListItem = delegator.findOne('ShoppingListItem',
                [shoppingListId: serviceCtx.shoppingListId, 'shoppingListItemSeqId': shoppingListItemSeqId], false)
        assert ServiceUtil.isSuccess(resultMap)
        assert shoppingListItem
    }

    @Test
    @Order(7)
    void testRemoveShoppingListItem() {
        String userLoginId = testParams.userLoginId ?: 'DemoCustomer'
        String shoppingListId = testParams.shoppingListId ?: 'DemoWishList'
        String shoppingListItemSeqId = testParams.shoppingListItemSeqId ?: '00002'
        GenericValue userLogin = delegator.findOne('UserLogin', [userLoginId: userLoginId], false)
        Map serviceCtx = [
                shoppingListId: shoppingListId,
                shoppingListItemSeqId: shoppingListItemSeqId,
                userLogin: userLogin
        ]
        Map resultMap = dispatcher.runSync('removeShoppingListItem', serviceCtx)
        GenericValue shoppingListItem = delegator.findOne('ShoppingListItem',
                [shoppingListId: serviceCtx.shoppingListId, 'shoppingListItemSeqId': shoppingListItemSeqId], false)
        assert ServiceUtil.isSuccess(resultMap)
        assert !shoppingListItem
    }

}
