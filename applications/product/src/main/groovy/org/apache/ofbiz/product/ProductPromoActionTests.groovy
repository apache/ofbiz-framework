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

import org.apache.ofbiz.base.util.Debug
import org.apache.ofbiz.base.util.UtilDateTime
import org.apache.ofbiz.base.util.UtilMisc
import org.apache.ofbiz.base.util.UtilValidate
import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.entity.util.EntityQuery
import org.apache.ofbiz.order.shoppingcart.CheckOutHelper
import org.apache.ofbiz.order.shoppingcart.ShoppingCart
import org.apache.ofbiz.order.shoppingcart.ShoppingCartItem
import org.apache.ofbiz.testtools.GroovyScriptTestCase
import org.apache.ofbiz.order.shoppingcart.product.ProductPromoWorker
import org.apache.ofbiz.order.shoppingcart.product.ProductPromoWorker.ActionResultInfo
import org.apache.ofbiz.service.ServiceUtil

import java.sql.Timestamp
import java.util.Map

class ProductPromoActionTest extends GroovyScriptTestCase {

    ShoppingCart loadOrder(String orderId) {
        GenericValue permUserLogin = EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "system").cache().queryOne()
        Map<String, Object> serviceCtx = [orderId: orderId,
                skipInventoryChecks: true, // the items are already reserved, no need to check again
                skipProductChecks: true, // the products are already in the order, no need to check their validity now
                userLogin: permUserLogin]
        Map<String, Object> loadCartResp = dispatcher.runSync("loadCartFromOrder", serviceCtx)

        return loadCartResp.shoppingCart
    }

    Map prepareConditionMap(ShoppingCart cart, BigDecimal amount, boolean persist) {
        GenericValue productPromoAction = delegator.makeValue("ProductPromoAction", [amount: amount, orderAdjustmentTypeId:'PROMOTION_ADJUSTMENT'])
        if (persist) {
            GenericValue productPromo = delegator.makeValue("ProductPromo", [productPromoId: 'TEST'])
            delegator.createOrStore(productPromo)
            GenericValue productPromoRule = delegator.makeValue("ProductPromoRule", [productPromoId: 'TEST', productPromoRuleId:'01'])
            delegator.createOrStore(productPromoRule)
            productPromoAction.productPromoId = 'TEST'
            productPromoAction.productPromoRuleId = '01'
            productPromoAction.productPromoActionSeqId = '01'
            delegator.createOrStore(productPromoAction)
        }
        return  [shoppingCart: cart, nowTimestamp: UtilDateTime.nowTimestamp(), actionResultInfo: new ActionResultInfo(), productPromoAction: productPromoAction]
    }

    /**
     * This test check if the function productTaxPercent work correctly
     *  1. test failed with passing non valid value
     *  2. test success if the tax percent promo is set for tax percent
     */
    void testActionProductTaxPercent() {
        ShoppingCart cart = loadOrder("DEMO10090")

        Map<String, Object> serviceContext = prepareConditionMap(cart, 10, true)
        Map<String, Object> serviceResult = dispatcher.runSync("productPromoActTaxPercent", serviceContext)

        //Check result service false test
        assert ServiceUtil.isSuccess(serviceResult)
        assert !serviceResult.actionResultInfo.ranAction
        assert serviceResult.actionResultInfo.totalDiscountAmount == 0

        //Increase item quantity to generate higher tax amount
        for (ShoppingCartItem item : cart.items()) {
            if (!item.getIsPromo()) {
                item.setQuantity(300, dispatcher, cart)
            }
        }

        //Add tax to cart
        CheckOutHelper coh = new CheckOutHelper(dispatcher, delegator, cart);
        coh.calcAndAddTax(false);

        serviceResult = dispatcher.runSync("productPromoActTaxPercent", serviceContext)

        //Check result service
        assert ServiceUtil.isSuccess(serviceResult)
        assert serviceResult.actionResultInfo.ranAction
        assert serviceResult.actionResultInfo.totalDiscountAmount != null
    }

    /**
     * This test check if the function productShipCharge work correctly
     *  1. test failed with passing non valid value
     *  2. test success if the ship charge promo is set for the shipping amount
     */
    void testProductShipCharge() {
        ShoppingCart cart = loadOrder("DEMO10090")

        Map<String, Object> serviceContext = prepareConditionMap(cart, 10, true)
        Map<String, Object> serviceResult = dispatcher.runSync("productPromoActShipCharge", serviceContext)

        //Check result service false test
        assert ServiceUtil.isSuccess(serviceResult)
        assert !serviceResult.actionResultInfo.ranAction
        assert serviceResult.actionResultInfo.totalDiscountAmount == 0

        //Add shipgroup estimate to cart
        cart.setItemShipGroupEstimate(22 , 0)

        serviceResult = dispatcher.runSync("productPromoActShipCharge", serviceContext)

        //Check result service
        assert ServiceUtil.isSuccess(serviceResult)
        assert serviceResult.actionResultInfo.ranAction
        assert serviceResult.actionResultInfo.totalDiscountAmount != null
    }

    /**
     * This test check if the function PoductSpecialPrice work correctly
     *  1. test failed with passing non valid value
     *  2. test success if the special price is set
     */
    void testPoductSpecialPrice() {
        ShoppingCart cart = loadOrder("DEMO10091")

        Map<String, Object> serviceContext = prepareConditionMap(cart, 10, false)
        GenericValue productPromoAction = EntityQuery.use(delegator).from("ProductPromoAction").where("productPromoId", "9013", "productPromoRuleId", "01", "productPromoActionSeqId", "01").queryOne()
        serviceContext.productPromoAction = productPromoAction
        Map<String, Object> serviceResult = dispatcher.runSync("productPromoActProdSpecialPrice", serviceContext)

        //Check result service false test
        assert ServiceUtil.isSuccess(serviceResult)
        assert !serviceResult.actionResultInfo.ranAction
        assert serviceResult.actionResultInfo.totalDiscountAmount == 0

        //Add item to cart to trigger action
        int itemIndex = cart.addItemToEnd("WG-1111", 100, 10, null, null, null, null, null, dispatcher, null, null)
        ShoppingCartItem item = cart.findCartItem(itemIndex)
        if (item) {
            item.setSpecialPromoPrice(BigDecimal.valueOf(22))
        }

        serviceResult = dispatcher.runSync("productPromoActProdSpecialPrice", serviceContext)

        //Check result service
        assert ServiceUtil.isSuccess(serviceResult)
        assert serviceResult.actionResultInfo.ranAction
        assert serviceResult.actionResultInfo.totalDiscountAmount != null
    }

    /**
     * This test check if the function ProductOrderAmount work correctly
     *  1. test success if the order amount off is set
     */
    void testProductOrderAmount() {
        ShoppingCart cart = loadOrder("DEMO10090")

        Map<String, Object> serviceContext = prepareConditionMap(cart, 10, false)
        GenericValue productPromoAction = EntityQuery.use(delegator).from("ProductPromoAction").where("productPromoId", "9012", "productPromoRuleId", "01", "productPromoActionSeqId", "01").queryOne()
        serviceContext.productPromoAction = productPromoAction
        Map<String, Object> serviceResult = dispatcher.runSync("productPromoActOrderAmount", serviceContext)

        //Check result service
        assert ServiceUtil.isSuccess(serviceResult)
        assert serviceResult.actionResultInfo.ranAction
        assert serviceResult.actionResultInfo.totalDiscountAmount != null

        // no condition so no false test
    }

    /**
     * This test check if the function productOrderPercent work correctly
     *  1. test success if the order percent off promo is set
     *  2. test failed with passing non valid value
     */
    void testProductOrderPercent() {
        ShoppingCart cart = loadOrder("DEMO10090")

        Map<String, Object> serviceContext = prepareConditionMap(cart, 10, false)
        GenericValue productPromoAction = EntityQuery.use(delegator).from("ProductPromoAction").where("productPromoId", "9019", "productPromoRuleId", "01", "productPromoActionSeqId", "01").queryOne()
        serviceContext.productPromoAction = productPromoAction
        Map<String, Object> serviceResult = dispatcher.runSync("productPromoActOrderPercent", serviceContext)

        //Check result service
        assert ServiceUtil.isSuccess(serviceResult)
        assert serviceResult.actionResultInfo.ranAction
        assert serviceResult.actionResultInfo.totalDiscountAmount != null

        //Update cart to cancel trigger action
        cart.clearAllAdjustments()
        for (ShoppingCartItem item : cart.items()) {
            if (!item.getIsPromo()) {
                GenericValue product = EntityQuery.use(delegator).from("Product").where("productId", item.getProductId()).queryOne()
                if (product != null) {
                    product.put("includeInPromotions", "N")
                    item._product = product
                }
            }
        }

        serviceContext.shoppingCart = cart
        serviceContext.actionResultInfo = new ActionResultInfo()
        serviceResult = dispatcher.runSync("productPromoActOrderPercent", serviceContext)

        //Check result service false test
        assert ServiceUtil.isSuccess(serviceResult)
        assert !serviceResult.actionResultInfo.ranAction
        assert serviceResult.actionResultInfo.totalDiscountAmount == 0
    }

    /**
     * This test check if the function productPromoActProdPrice work correctly
     *  1. test failed with passing non valid value
     *  2. test success if promo is applied
     */
    void testProductPrice() {
        ShoppingCart cart = loadOrder("DEMO10090")

        Map<String, Object> serviceContext = prepareConditionMap(cart, 10, false)
        GenericValue productPromoAction = EntityQuery.use(delegator).from("ProductPromoAction").where("productPromoId", "9015", "productPromoRuleId", "01", "productPromoActionSeqId", "01").queryOne()
        serviceContext.productPromoAction = productPromoAction
        Map<String, Object> serviceResult = dispatcher.runSync("productPromoActProdPrice", serviceContext)

        //Check result service false test
        assert ServiceUtil.isSuccess(serviceResult)
        assert !serviceResult.actionResultInfo.ranAction
        assert serviceResult.actionResultInfo.totalDiscountAmount == 0

        //Update cart to trigger action
        for (ShoppingCartItem item : cart.items()) {
            if (!item.getIsPromo()) {
                item.setQuantity(20, dispatcher, cart)
            }
        }

        serviceContext.shoppingCart = cart
        serviceContext.actionResultInfo = new ActionResultInfo()
        serviceResult = dispatcher.runSync("productPromoActProdPrice", serviceContext)

        //Check result service
        assert ServiceUtil.isSuccess(serviceResult)
        assert serviceResult.actionResultInfo.ranAction
        assert serviceResult.actionResultInfo.totalDiscountAmount != null
    }

    /**
     * This test check if the function productPromoActProdAMDISC work correctly
     *  1. test success if promo is applied
     *  2. test failed with passing already applied promo
     */
    void testProductAMDISC() {
        ShoppingCart cart = loadOrder("DEMO10090")

        Map<String, Object> serviceContext = prepareConditionMap(cart, 10, false)
        GenericValue productPromoAction = EntityQuery.use(delegator).from("ProductPromoAction").where("productPromoId", "9015", "productPromoRuleId", "01", "productPromoActionSeqId", "01").queryOne()
        serviceContext.productPromoAction = productPromoAction
        Map<String, Object> serviceResult = dispatcher.runSync("productPromoActProdAMDISC", serviceContext)

        //Check result service
        assert ServiceUtil.isSuccess(serviceResult)
        assert serviceResult.actionResultInfo.ranAction
        assert serviceResult.actionResultInfo.totalDiscountAmount != null

        serviceContext.shoppingCart = cart
        serviceContext.actionResultInfo = new ActionResultInfo()
        serviceResult = dispatcher.runSync("productPromoActProdAMDISC", serviceContext)

        //Check result service false test
        assert ServiceUtil.isSuccess(serviceResult)
        assert !serviceResult.actionResultInfo.ranAction
        assert serviceResult.actionResultInfo.totalDiscountAmount == 0
    }

    /**
     * This test check if the function productPromoActProdDISC work correctly
     *  1. test success if promo is applied
     *  2. test failed with passing already applied promo
     */
    void testProductDISC() {
        ShoppingCart cart = loadOrder("DEMO10090")

        Map<String, Object> serviceContext = prepareConditionMap(cart, 10, false)
        GenericValue productPromoAction = EntityQuery.use(delegator).from("ProductPromoAction").where("productPromoId", "9015", "productPromoRuleId", "01", "productPromoActionSeqId", "01").queryOne()
        serviceContext.productPromoAction = productPromoAction
        Map<String, Object> serviceResult = dispatcher.runSync("productPromoActProdDISC", serviceContext)

        //Check result service
        assert ServiceUtil.isSuccess(serviceResult)
        assert serviceResult.actionResultInfo.ranAction
        assert serviceResult.actionResultInfo.totalDiscountAmount != null

        //Check result service false test
        serviceContext.shoppingCart = cart
        serviceContext.actionResultInfo = new ActionResultInfo()
        serviceResult = dispatcher.runSync("productPromoActProdDISC", serviceContext)

        assert ServiceUtil.isSuccess(serviceResult)
        assert !serviceResult.actionResultInfo.ranAction
        assert serviceResult.actionResultInfo.totalDiscountAmount == 0
    }

    /**
     * This test check if the function ProductGWP work correctly
     *  1. test failed with passing non valid value
     *  2. test success if gift with purchase promo is set for order
     */
    void testProductGWP() {
        ShoppingCart cart = loadOrder("DEMO10090")

        Map<String, Object> serviceContext = prepareConditionMap(cart, 10, true)
        Map<String, Object> serviceResult = dispatcher.runSync("productPromoActGiftGWP", serviceContext)

        //Check result service false test
        assert ServiceUtil.isSuccess(serviceResult)
        assert !serviceResult.actionResultInfo.ranAction
        assert serviceResult.actionResultInfo.totalDiscountAmount == 0

        serviceContext.shoppingCart = cart
        serviceContext.actionResultInfo = new ActionResultInfo()
        GenericValue productPromoAction = EntityQuery.use(delegator).from("ProductPromoAction").where("productPromoId", "9017",  "productPromoRuleId", "01", "productPromoActionSeqId", "01").queryOne()
        serviceContext.productPromoAction = productPromoAction
        serviceResult = dispatcher.runSync("productPromoActGiftGWP", serviceContext)

        //Check result service
        assert ServiceUtil.isSuccess(serviceResult)
        assert serviceResult.actionResultInfo.ranAction
        assert serviceResult.actionResultInfo.totalDiscountAmount != null
    }

    /**
     * This test check if the function productActFreeShip work correctly
     *  1. test success if the free shipping promo is set for tax percent
     *  2. don't need to make false test because this fonction doesn't need condition
     */
    void testFreeShippingAct() {
        ShoppingCart cart = loadOrder("DEMO10090")

        Map<String, Object> serviceContext = prepareConditionMap(cart, 10, true)
        Map<String, Object> serviceResult = dispatcher.runSync("productPromoActFreeShip", serviceContext)

        //Check result service
        assert ServiceUtil.isSuccess(serviceResult)
        assert serviceResult.actionResultInfo.ranAction
        assert serviceResult.actionResultInfo.totalDiscountAmount != null
    }
}
