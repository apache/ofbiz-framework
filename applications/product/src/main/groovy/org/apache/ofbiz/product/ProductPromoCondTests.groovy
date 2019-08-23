/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License") you may not use this file except in compliance
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
package org.apache.ofbiz.product

import java.sql.Timestamp
import org.apache.ofbiz.base.util.UtilDateTime
import org.apache.ofbiz.base.util.UtilMisc
import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.entity.util.EntityQuery
import org.apache.ofbiz.order.shoppingcart.ShoppingCart
import org.apache.ofbiz.service.testtools.OFBizTestCase
import org.apache.ofbiz.service.ServiceUtil

class ProductPromoCondTest extends OFBizTestCase {
    public ProductPromoCondTest(String name) {
        super(name)
    }

    Map prepareConditionMap(ShoppingCart cart, String condValue) {
        return prepareConditionMap(cart, condValue, false)
    }

    Map prepareConditionMap(ShoppingCart cart, String condValue, boolean persist) {
        GenericValue productPromoCond = delegator.makeValue("ProductPromoCond", [condValue: condValue])
        if (persist) {
            GenericValue productPromo = delegator.makeValue("ProductPromo", [productPromoId: 'TEST'])
            delegator.createOrStore(productPromo)
            GenericValue productPromoRule = delegator.makeValue("ProductPromoRule", [productPromoId: 'TEST', productPromoRuleId:'01'])
            delegator.createOrStore(productPromoRule)
            productPromoCond.productPromoId = 'TEST'
            productPromoCond.productPromoRuleId = '01'
            productPromoCond.productPromoCondSeqId = '01'
            delegator.createOrStore(productPromoCond)
        }
        return  [shoppingCart: cart, nowTimestamp: UtilDateTime.nowTimestamp(), productPromoCond: productPromoCond]
    }

    ShoppingCart loadOrder(String orderId) {
        GenericValue permUserLogin = EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "system").cache().queryOne()
        Map<String, Object> serviceCtx = [orderId: orderId,
                skipInventoryChecks: true, // the items are already reserved, no need to check again
                skipProductChecks: true, // the products are already in the order, no need to check their validity now
                includePromoItems: false,
                createAsNewOrder: 'Y',
                userLogin: permUserLogin]
        Map<String, Object> loadCartResp = dispatcher.runSync("loadCartFromOrder", serviceCtx)

        return loadCartResp.shoppingCart
    }

    /**
     * This test check if the function productPartyID work correctly
     *  1. test success with a valid partyId
     *  2. test failed with passing non valid value
     */
    void testPartyIdPromo() {
        String condValue = "FrenchCustomer"
        ShoppingCart cart = new ShoppingCart(delegator, "9000", Locale.getDefault(), "EUR")
        cart.setOrderPartyId(condValue)

        // call service promo
        Map<String, Object> serviceContext = prepareConditionMap(cart, condValue)
        Map<String, Object> serviceResult = dispatcher.runSync("productPromoCondPartyID", serviceContext)

        //Check result service
        assert ServiceUtil.isSuccess(serviceResult)
        assert serviceResult.compareBase == 0

        //2.test promo nonvalid
        cart.setOrderPartyId("OtherPartyId")
        serviceResult = dispatcher.runSync("productPromoCondPartyID", serviceContext)
        assert ServiceUtil.isSuccess(serviceResult)
        assert serviceResult.compareBase != 0
    }

    /**
     * This test check if the function productNewACCT work correctly
     *  1. test success if the customer is subscribed more than the "condValue"
     *  2. test failed with passing non valid value
     */
    void testNewACCTPromo() {
        String condValue = "1095"
        GenericValue frenchCustomer = delegator.makeValue("Party", [partyId: "FrenchCustomer", createdDate: Timestamp.valueOf("2010-01-01 00:00:00")])
        frenchCustomer.store()
        ShoppingCart cart = new ShoppingCart(delegator, "9000", Locale.getDefault(), "EUR")
        Timestamp nowTimestamp = UtilDateTime.nowTimestamp()
        cart.setOrderPartyId("FrenchCustomer")
        cart.getPartyDaysSinceCreated(nowTimestamp)
        Map<String, Object> serviceContext = prepareConditionMap(cart, condValue)
        Map<String, Object> serviceResult = dispatcher.runSync("productPromoCondNewACCT", serviceContext)

        //Check result service
        assert ServiceUtil.isSuccess(serviceResult)
        assert serviceResult.compareBase >= 0

        //2.test promo nonvalid
        frenchCustomer.createdDate = nowTimestamp
        frenchCustomer.store()
        cart.getPartyDaysSinceCreated(nowTimestamp)
        serviceResult = dispatcher.runSync("productPromoCondNewACCT", serviceContext)
        assert ServiceUtil.isSuccess(serviceResult)
        assert serviceResult.compareBase < 0
    }

    /**
     * This test check if the function productPartyClass work correctly
     *  1. test success if the login user is from part of classification of the promoCondition
     *  2. test failed with passing non valid value
     */
    void testPartyClassPromo() {
        String condValue = "PROMO_TEST"
        GenericValue partyClassGroup = delegator.makeValue("PartyClassificationGroup", [partyClassificationGroupId: condValue])
        delegator.createOrStore(partyClassGroup)
        GenericValue partyClassification = delegator.makeValue("PartyClassification",
                [partyId: "FrenchCustomer", partyClassificationGroupId: condValue,
                 fromDate: Timestamp.valueOf("2010-01-01 00:00:00"),
                 thruDate: null])
        delegator.createOrStore(partyClassification)
        ShoppingCart cart = new ShoppingCart(delegator, "9000", Locale.getDefault(), "EUR")
        cart.setOrderPartyId("FrenchCustomer")

        // call service promo
        Map<String, Object> serviceContext = prepareConditionMap(cart, condValue)
        Map<String, Object> serviceResult = dispatcher.runSync("productPromoCondPartyClass", serviceContext)

        //Check result service
        assert ServiceUtil.isSuccess(serviceResult)
        assert serviceResult.compareBase == 0

        //2.test promo nonvalid
        partyClassification.refresh()
        partyClassification.thruDate = Timestamp.valueOf("2010-01-01 00:00:00")
        partyClassification.store()
        serviceResult = dispatcher.runSync("productPromoCondPartyClass", serviceContext)
        assert ServiceUtil.isSuccess(serviceResult)
        assert serviceResult.compareBase == 1
    }

    /**
     * This test check if the function productPartyGM work correctly
     *  1. test success if the login user is from part of the Group member of the promoCondition
     *  2. test failed with passing non valid value
     */
    void testPartyGMPromo() {
        String condValue = "HUMAN_RES"
        ShoppingCart cart = new ShoppingCart(delegator, "9000", Locale.getDefault(), "EUR")
        cart.setOrderPartyId(condValue)
        GenericValue partyRole = delegator.makeValue("PartyRole", [partyId: "FrenchCustomer", roleTypeId: "_NA_"])
        delegator.createOrStore(partyRole)
        partyRole.partyId = condValue
        delegator.createOrStore(partyRole)
        GenericValue relation = delegator.makeValue("PartyRelationship", [partyIdFrom: "FrenchCustomer", roleTypeIdFrom: "_NA_",
                                               partyIdTo: condValue, roleTypeIdTo: "_NA_",
                                               fromDate: Timestamp.valueOf("2010-01-01 00:00:00"),
                                               partyRelationshipTypeId: "GROUP_ROLLUP"])
        delegator.createOrStore(relation)

        // call service promo
        Map<String, Object> serviceContext = prepareConditionMap(cart, condValue)
        Map<String, Object> serviceResult = dispatcher.runSync("productPromoCondPartyGM", serviceContext)

        //Check result service
        assert ServiceUtil.isSuccess(serviceResult)
        assert serviceResult.compareBase == 0

        //2.test promo nonvalid
        cart.setOrderPartyId("OtherPartyId")
        serviceResult = dispatcher.runSync("productPromoCondPartyGM", serviceContext)
        assert ServiceUtil.isSuccess(serviceResult)
        assert serviceResult.compareBase == 1
    }

    /**
     * This test check if the function productRoleType work correctly
     *  1. test success if the login user role type is equal to the condValue
     *  2. test failed with passing non valid value
     */
    void testRoleTypePromo() {
        String condValue = "APPROVER"
        ShoppingCart cart = new ShoppingCart(delegator, "9000", Locale.getDefault(), "EUR")
        cart.setOrderPartyId("FrenchCustomer")
        GenericValue partyRole = delegator.makeValue("PartyRole", [partyId: "FrenchCustomer", roleTypeId: condValue])
        delegator.createOrStore(partyRole)

        // call service promo
        Map<String, Object> serviceContext = prepareConditionMap(cart, condValue)
        Map<String, Object> serviceResult = dispatcher.runSync("productPromoCondRoleType", serviceContext)

        //Check result service
        assert ServiceUtil.isSuccess(serviceResult)
        assert serviceResult.compareBase == 0

        //2.test promo nonvalid
        cart.setOrderPartyId("OtherPartyId")
        serviceResult = dispatcher.runSync("productPromoCondRoleType", serviceContext)
        assert ServiceUtil.isSuccess(serviceResult)
        assert serviceResult.compareBase != 0
    }

    /**
     * This test check if the function productGeoID work correctly
     *  1. test success if the shipping address is equal to the condValue
     *  2. test failed with passing non valid value
     */
    void testCondGeoIdPromo() {
        ShoppingCart cart = loadOrder("DEMO10090")
        cart.setShippingContactMechId(0, "9200")
        GenericValue productPromoCond = EntityQuery.use(delegator).from("ProductPromoCond").where("productPromoId", "9022", "productPromoRuleId", "01", "productPromoCondSeqId", "01").queryOne()

        // call service promo
        Map<String, Object> serviceContext = [shoppingCart: cart, nowTimestamp: UtilDateTime.nowTimestamp(), productPromoCond: productPromoCond]
        Map<String, Object> serviceResult = dispatcher.runSync("productPromoCondGeoID", serviceContext)

        //Check result service
        assert ServiceUtil.isSuccess(serviceResult)
        assert serviceResult.compareBase == 0

        //2.test promo nonvalid
        cart = loadOrder("DEMO10091")
        cart.setShippingContactMechId(0, "10000")
        serviceContext.shoppingCart = cart
        serviceResult = dispatcher.runSync("productPromoCondGeoID", serviceContext)
        assert ServiceUtil.isSuccess(serviceResult)
        assert serviceResult.compareBase != 0
    }

    /**
     * This test check if the function productOrderTotal work correctly
     *  1. test success if the order total is equal or greater than the condValue
     *  2. test failed with passing non valid value
     */
    void testCondOrderTotalPromo() {
        String condValue = "34.56"
        // call service promo
        Map<String, Object> serviceContext = prepareConditionMap(loadOrder("DEMO10090"), condValue)
        Map<String, Object> serviceResult = dispatcher.runSync("productPromoCondOrderTotal", serviceContext)

        //Check result service
        assert ServiceUtil.isSuccess(serviceResult)
        assert serviceResult.compareBase == 0

        //2.test promo nonvalid
        serviceContext.shoppingCart = loadOrder("Demo1002")
        serviceResult = dispatcher.runSync("productPromoCondOrderTotal", serviceContext)
        assert ServiceUtil.isSuccess(serviceResult)
        assert serviceResult.compareBase > 0
    }

    /**
     * This test check if the function productPromoRecurrence work correctly
     *  1. test success if the recurrence is equal to the condValue
     *  2. test failed with passing non valid value
     */
    void testRecurrencePromo() {
        String condValue = "TEST_PROMO"
        ShoppingCart cart = new ShoppingCart(delegator, "9000", Locale.getDefault(), "EUR")
        GenericValue reccurenceRule = delegator.makeValue("RecurrenceRule", [recurrenceRuleId: condValue, frequency: "DAILY", intervalNumber: 1l,
                                                                       countNumber: -1l, byDayList: "MO,TU,WE,TH,FR,SA,SU"])
        delegator.createOrStore(reccurenceRule)
        GenericValue reccurenceInfo = delegator.makeValue("RecurrenceInfo", [recurrenceInfoId: condValue, startDateTime: Timestamp.valueOf("2008-01-01 00:00:00.000"),
                                                                       recurrenceRuleId: condValue, recurrenceCount: 0l])
        delegator.createOrStore(reccurenceInfo)

        // call service promo
        Map<String, Object> serviceContext = prepareConditionMap(cart, condValue)
        Map<String, Object> serviceResult = dispatcher.runSync("productPromoCondPromoRecurrence", serviceContext)

        //Check result service
        assert ServiceUtil.isSuccess(serviceResult)
        assert serviceResult.compareBase == 0

        //2.test promo nonvalid
        condValue = "3"
        serviceContext = prepareConditionMap(cart, condValue)
        serviceResult = dispatcher.runSync("productPromoCondPromoRecurrence", serviceContext)
        assert ServiceUtil.isSuccess(serviceResult)
        assert serviceResult.compareBase != 0
    }

    /**
     * This test check if the function productShipTotal work correctly
     *  1. test success if ship total is greater than the condValue
     *  2. test failed with passing non valid value
     */
    void testShipTotalPromo() {
        String condValue = "20"
        BigDecimal amount = BigDecimal.valueOf(25)
        ShoppingCart cart = loadOrder("DEMO10090")
        cart.setItemShipGroupEstimate(amount, 0)

        // call service promo
        Map<String, Object> serviceContext = prepareConditionMap(cart, condValue)
        Map<String, Object> serviceResult = dispatcher.runSync("productPromoCondOrderShipTotal", serviceContext)

        //Check result service
        assert ServiceUtil.isSuccess(serviceResult)
        assert serviceResult.compareBase > 0

        //2.test promo nonvalid
        amount = BigDecimal.valueOf(19)
        cart.setItemShipGroupEstimate(amount, 0)
        serviceResult = dispatcher.runSync("productPromoCondOrderShipTotal", serviceContext)
        assert ServiceUtil.isSuccess(serviceResult)
        assert serviceResult.compareBase <= 0
    }

    /**
     * This test check if the function productAmount work correctly
     *  1. test success if the amount of specific product is equal to the condValue
     *  2. test failed with passing non valid value
     */
    void testProductAmountPromo() {
        String condValue = "30"
        String orderId = "DEMO10090"
        ShoppingCart cart = loadOrder(orderId)

        // call service promo
        Map<String, Object> serviceContext = prepareConditionMap(cart, condValue, true)
        GenericValue productPromoProduct = delegator.makeValue("ProductPromoProduct",
                [productPromoId: 'TEST', productPromoRuleId: '01', productPromoCondSeqId: '01',
                 productId: 'GZ-2644', productPromoApplEnumId: 'PPPA_INCLUDE', productPromoActionSeqId: '_NA_'])
        delegator.createOrStore(productPromoProduct)
        Map<String, Object> serviceResult = dispatcher.runSync("productPromoCondProductAmount", serviceContext)

        //Check result service
        assert ServiceUtil.isSuccess(serviceResult)
        assert serviceResult.compareBase == 0

        //2.test promo nonvalid
        condValue = "50"
        serviceContext = prepareConditionMap(cart, condValue, true)
        serviceResult = dispatcher.runSync("productPromoCondProductAmount", serviceContext)
        assert ServiceUtil.isSuccess(serviceResult)
        assert serviceResult.compareBase != 0
    }

    /**
     * This test check if the function productTotal work correctly
     *  1. test success if the total amount of product is equal or greater than the condValue
     *  2. test failed with passing non valid value
     */
    void testProductTotalPromo() {
        String orderId = "Demo1002"
        ShoppingCart cart = loadOrder(orderId)

        // call service promo
        Map<String, Object> serviceContext = prepareConditionMap(cart, "50", true)
        GenericValue productPromoProduct = delegator.makeValue("ProductPromoProduct",
            [productPromoId: 'TEST', productPromoRuleId: '01', productPromoCondSeqId: '01',
             productId: 'WG-1111', productPromoApplEnumId: 'PPPA_INCLUDE', productPromoActionSeqId: '_NA_'])
        delegator.createOrStore(productPromoProduct)
        Map<String, Object> serviceResult = dispatcher.runSync("productPromoCondProductTotal", serviceContext)

        //Check result service
        assert ServiceUtil.isSuccess(serviceResult)
        assert serviceResult.compareBase >= 0


        //2.test promo nonvalid
        cart = loadOrder(orderId)
        serviceContext = prepareConditionMap(cart, "150", true)
        serviceResult = dispatcher.runSync("productPromoCondProductTotal", serviceContext)
        assert ServiceUtil.isSuccess(serviceResult)
        assert serviceResult.compareBase < 0
    }
}
