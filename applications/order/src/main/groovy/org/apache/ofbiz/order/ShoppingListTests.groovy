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
package org.apache.ofbiz.order

import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.service.ServiceUtil
import org.apache.ofbiz.service.testtools.OFBizTestCase

class ShoppingListTests extends OFBizTestCase {
    public ShoppingListTests(String name) {
        super(name)
    }

    // Test create shopping list.
    void testCreateShoppingList() {
        GenericValue userLogin = delegator.findOne("UserLogin", [userLoginId:'DemoCustomer'], false);
        Map serviceCtx = [
                partyId: 'DemoCustomer',
                shoppingListTypeId: 'SLT_WISH_LIST',
                productStoreId: '9000',
                listName: 'Demo Wish List 1',
                isActive: 'Y',
                currencyUom: 'USD',
                userLogin: userLogin
        ]
        Map resultMap = dispatcher.runSync('createShoppingList', serviceCtx, 600, true);
        def shoppingListId = resultMap.shoppingListId;
        GenericValue shoppingList = delegator.findOne("ShoppingList", [shoppingListId:shoppingListId], false);
        assert ServiceUtil.isSuccess(resultMap);
        assert shoppingList;
        assert shoppingList.partyId == 'DemoCustomer';
        assert shoppingList.listName == 'Demo Wish List 1';
    }

    // Test create shopping list item
    void testCreateShoppingListItem() {
        GenericValue userLogin = delegator.findOne("UserLogin", [userLoginId:'DemoCustomer'], false);
        def shoppingListId = 'DemoWishList';
        Map serviceCtx = [
                shoppingListId: shoppingListId,
                productId: 'GZ-8544',
                quantity: new BigDecimal(3),
                productStoreId: '9000',
                userLogin: userLogin
        ]
        Map resultMap = dispatcher.runSync('createShoppingListItem', serviceCtx);
        def shoppingListItemSeqId = resultMap.shoppingListItemSeqId;
        GenericValue shoppingListItem = from('ShoppingListItem').where('shoppingListItemSeqId', shoppingListItemSeqId).queryOne()
        assert ServiceUtil.isSuccess(resultMap);
        assert shoppingListItem;
        assert shoppingListItem.productId == 'GZ-8544';
        assert shoppingListItem.quantity == 3;
    }

    // Test create shopping list item by adding a product that already exist in shopping list.
    void testCreateShoppingListItemWithSameProduct() {
        GenericValue userLogin = delegator.findOne("UserLogin", [userLoginId:'DemoCustomer'], false);
        def shoppingListId = 'DemoWishList';
        Map serviceCtx = [
                shoppingListId: shoppingListId,
                productId: 'GZ-2644',
                quantity: new BigDecimal(2),
                productStoreId: '9000',
                userLogin: userLogin
        ]
        Map resultMap = dispatcher.runSync('createShoppingListItem', serviceCtx);
        def shoppingListItemSeqId = resultMap.shoppingListItemSeqId;
        GenericValue shoppingListItem = from('ShoppingListItem').where('shoppingListItemSeqId', shoppingListItemSeqId).queryOne()
        assert ServiceUtil.isSuccess(resultMap);
        assert shoppingListItem;
        assert shoppingListItem.quantity == 7;
    }

    // Test update shopping list by updating the listName
    void testUpdateShoppingList() {
        GenericValue userLogin = delegator.findOne("UserLogin", [userLoginId:'DemoCustomer'], false);
        Map serviceCtx = [
                shoppingListId: 'DemoWishList',
                listName: 'New Demo Wish List',
                userLogin: userLogin
        ]
        Map resultMap = dispatcher.runSync('updateShoppingList', serviceCtx);
        GenericValue shoppingList = delegator.findOne("ShoppingList", [shoppingListId:serviceCtx.shoppingListId], false);
        assert ServiceUtil.isSuccess(resultMap);
        assert shoppingList;
        assert shoppingList.listName == 'New Demo Wish List';
    }

    // Test update shopping list item by updating quantity of item
    void testUpdateShoppingListItem () {
        GenericValue userLogin = delegator.findOne("UserLogin", [userLoginId:'DemoCustomer'], false);
        Map serviceCtx = [
                shoppingListId: 'DemoWishList',
                shoppingListItemSeqId: '00002',
                quantity: new BigDecimal(4),
                userLogin: userLogin
        ]
        Map resultMap = dispatcher.runSync('updateShoppingListItem', serviceCtx);
        GenericValue shoppingListItem = delegator.findOne("ShoppingListItem", [shoppingListId:serviceCtx.shoppingListId, 'shoppingListItemSeqId': '00002'], false);
        assert ServiceUtil.isSuccess(resultMap)
        assert shoppingListItem
        assert shoppingListItem.quantity == 4
    }

    // Test update shopping list item for a product with zero quantity
    void testUpdateShoppingListItemWithZeroQty() {
        GenericValue userLogin = delegator.findOne("UserLogin", [userLoginId:'DemoCustomer'], false);
        Map serviceCtx = [
                shoppingListId: 'DemoWishList',
                shoppingListItemSeqId: '00003',
                quantity: new BigDecimal(0),
                userLogin: userLogin
        ]
        Map resultMap = dispatcher.runSync('updateShoppingListItem', serviceCtx);
        GenericValue shoppingListItem = delegator.findOne("ShoppingListItem", [shoppingListId:serviceCtx.shoppingListId, 'shoppingListItemSeqId': '00003'], false);
        assert ServiceUtil.isSuccess(resultMap)
        assert shoppingListItem
    }

    // Test remove shopping list item
    void testRemoveShoppingListItem() {
        GenericValue userLogin = delegator.findOne("UserLogin", [userLoginId:'DemoCustomer'], false);
        Map serviceCtx = [
                shoppingListId: 'DemoWishList',
                shoppingListItemSeqId: '00002',
                userLogin: userLogin
        ]
        Map resultMap = dispatcher.runSync('removeShoppingListItem', serviceCtx);
        GenericValue shoppingListItem = delegator.findOne("ShoppingListItem", [shoppingListId:serviceCtx.shoppingListId, 'shoppingListItemSeqId': '00002'], false);
        assert ServiceUtil.isSuccess(resultMap)
        assert !shoppingListItem
    }
}
