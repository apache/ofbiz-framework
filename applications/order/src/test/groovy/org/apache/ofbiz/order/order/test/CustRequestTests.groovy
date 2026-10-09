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

import org.apache.ofbiz.order.shoppingcart.ShoppingCart
import org.apache.ofbiz.service.ServiceUtil
import org.apache.ofbiz.service.testtools.OFBizTestCase

class CustRequestTests extends OFBizTestCase {

    CustRequestTests(String name) {
        super(name)
    }

    void testCreateCustRequestFromEmptyCart() {
        ShoppingCart cart = new ShoppingCart(delegator, '9000', Locale.getDefault(), 'USD')
        cart.setOrderType('SALES_ORDER')
        cart.setChannelType('WEB_SALES_CHANNEL')
        cart.setBillToCustomerPartyId('DemoCustomer')
        cart.setUserLogin(userLogin, dispatcher)

        long custRequestCountBefore = from('CustRequest').queryCount()
        Map serviceResult = dispatcher.runSync('createCustRequestFromCart', [
                userLogin: userLogin,
                cart: cart
        ], 600, true)

        assert ServiceUtil.isError(serviceResult)
        assert from('CustRequest').queryCount() == custRequestCountBefore
    }

}
