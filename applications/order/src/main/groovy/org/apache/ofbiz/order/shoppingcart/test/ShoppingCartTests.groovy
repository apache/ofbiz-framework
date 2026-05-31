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
package org.apache.ofbiz.order.shoppingcart.test

import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.service.testtools.OFBizTestCase
import org.apache.ofbiz.order.shoppingcart.ShoppingCart
import org.apache.ofbiz.order.shoppingcart.CheckOutHelper
import org.apache.ofbiz.order.order.OrderChangeHelper
import org.apache.ofbiz.base.util.UtilDateTime
import org.apache.ofbiz.product.config.ProductConfigWrapper
import java.sql.Timestamp

class ShoppingCartTests extends OFBizTestCase {

    ShoppingCartTests(String name) {
        super(name)
    }

    void testCreateShoppingCart() {
        GenericValue userLogin = from('UserLogin').where('userLoginId', 'system').queryOne()
        Locale locale = Locale.getDefault()
        Map orderMap = createShoppinCartAndOrder(userLogin, locale)
        assert orderMap
        assert orderMap.orderId
    }

    void testCreateOrderRentalProduct() {
        Locale locale = Locale.getDefault()
        GenericValue demoCustomer = from('UserLogin').where('userLoginId', 'DemoCustomer').queryOne()
        ShoppingCart shoppingCart = new ShoppingCart(delegator, '9000', locale, 'USD')
        shoppingCart.with {
            setOrderType('SALES_ORDER')
            setChannelType('WEB_SALES_CHANNEL')
            setProductStoreId('9000')
            setBillToCustomerPartyId('DemoCustomer')
            setPlacingCustomerPartyId('DemoCustomer')
            setShipToCustomerPartyId('DemoCustomer')
            setEndUserCustomerPartyId('DemoCustomer')
            setUserLogin(demoCustomer, dispatcher)
        }

        Timestamp nextDate = UtilDateTime.addDaysToTimestamp(UtilDateTime.nowTimestamp(), 1)
        shoppingCart.with {
            addOrIncreaseItem('RentalShip', null, BigDecimal.ONE, nextDate, 3, 1,
                    null, null, null, null, null, null, 'DemoCatalog', null, 'RENTAL_ORDER_ITEM', null, null, dispatcher)
            setDefaultCheckoutOptions(dispatcher)
        }

        CheckOutHelper checkOutHelper = new CheckOutHelper(dispatcher, delegator, shoppingCart)
        Map orderCreateResult = checkOutHelper.createOrder(demoCustomer)
        String orderId = orderCreateResult.orderId
        assert orderId

        OrderChangeHelper.approveOrder(dispatcher, demoCustomer, orderId)
        GenericValue systemLogin = from('UserLogin').where('userLoginId', 'system').queryOne()
        dispatcher.runSync('quickShipEntireOrder', [orderId: orderId, userLogin: systemLogin])
    }

    void testCreateOrderServiceProduct() {
        Locale locale = Locale.getDefault()
        GenericValue demoCustomer = from('UserLogin').where('userLoginId', 'DemoCustomer').queryOne()
        ShoppingCart shoppingCart = new ShoppingCart(delegator, '9000', locale, 'USD')
        shoppingCart.with {
            setOrderType('SALES_ORDER')
            setChannelType('WEB_SALES_CHANNEL')
            setProductStoreId('9000')
            setBillToCustomerPartyId('DemoCustomer')
            setPlacingCustomerPartyId('DemoCustomer')
            setShipToCustomerPartyId('DemoCustomer')
            setEndUserCustomerPartyId('DemoCustomer')
            setUserLogin(demoCustomer, dispatcher)
            addOrIncreaseItem('SV-1001', null, BigDecimal.ONE, null, null, null,
                    null, null, null, null, null, null, 'DemoCatalog', null, null, null, null, dispatcher)
            setDefaultCheckoutOptions(dispatcher)
        }

        CheckOutHelper checkOutHelper = new CheckOutHelper(dispatcher, delegator, shoppingCart)
        Map orderCreateResult = checkOutHelper.createOrder(demoCustomer)
        String orderId = orderCreateResult.orderId
        assert orderId

        OrderChangeHelper.approveOrder(dispatcher, demoCustomer, orderId)
        GenericValue systemLogin = from('UserLogin').where('userLoginId', 'system').queryOne()
        dispatcher.runSync('quickShipEntireOrder', [orderId: orderId, userLogin: systemLogin])
    }

    void testLoadCartFromQuote() {
        GenericValue systemLogin = from('UserLogin').where('userLoginId', 'system').queryOne()

        Map createQuoteMap = [
                userLogin: systemLogin,
                partyId: 'DemoCustomer',
                currencyUomId: 'USD',
                description: 'Test quote',
                issueDate: UtilDateTime.toTimestamp('11/01/2011 10:00:00'),
                productStoreId: '9000',
                quoteName: 'Test quote',
                quoteTypeId: 'PRODUCT_QUOTE',
                statusId: 'QUO_APPROVED',
                validFromDate: UtilDateTime.toTimestamp('11/01/2011 10:00:00')
        ]
        Map createQuoteResult = dispatcher.runSync('createQuote', createQuoteMap)
        String quoteId = createQuoteResult.quoteId

        Map createQuoteItemMap = [
                userLogin: systemLogin,
                quoteId: quoteId,
                productId: 'GZ-1000',
                quantity: 10,
                quoteUnitPrice: 15.00
        ]
        Map createQuoteItemResult = dispatcher.runSync('createQuoteItem', createQuoteItemMap)
        String quoteItemSeqId = createQuoteItemResult.quoteItemSeqId

        Map createQuoteAdjustmentMap = [
                userLogin: systemLogin,
                quoteId: quoteId,
                quoteItemSeqId: quoteItemSeqId,
                amount: 15.00,
                includeInShipping: 'N',
                includeInTax: 'Y',
                quoteAdjustmentTypeId: 'SALES_TAX',
                taxAuthGeoId: 'UT',
                taxAuthPartyId: 'UT_TAXMAN'
        ]
        dispatcher.runSync('createQuoteAdjustment', createQuoteAdjustmentMap)

        Map loadCartFromQuoteMap = [
                userLogin: systemLogin,
                quoteId: quoteId,
                applyQuoteAdjustments: 'true'
        ]
        Map loadCartResult = dispatcher.runSync('loadCartFromQuote', loadCartFromQuoteMap)
        ShoppingCart shoppingCart = (ShoppingCart) loadCartResult.shoppingCart

        BigDecimal expectedTax = 15.00
        assert shoppingCart.getTotalSalesTax() == expectedTax
        assert shoppingCart.getTotalSalesTax(0) == expectedTax

        BigDecimal expectedTotal = 165.00
        assert shoppingCart.getGrandTotal() == expectedTotal
    }

    void testCreateOrderConfigurableServiceProduct() {
        // ShoppingCartEvents requires request/response which are not readily mockable here without Spring
        // So we just use CheckOutHelper for test.
        Locale locale = Locale.getDefault()
        GenericValue demoCustomer = from('UserLogin').where('userLoginId', 'DemoCustomer').queryOne()

        ShoppingCart shoppingCart = new ShoppingCart(delegator, '9000', locale, 'USD')
        shoppingCart.with {
            setOrderType('SALES_ORDER')
            setChannelType('WEB_SALES_CHANNEL')
            setProductStoreId('9000')
            setBillToCustomerPartyId('DemoCustomer')
            setPlacingCustomerPartyId('DemoCustomer')
            setShipToCustomerPartyId('DemoCustomer')
            setEndUserCustomerPartyId('DemoCustomer')
            setUserLogin(demoCustomer, dispatcher)
        }

        ProductConfigWrapper configWrapper = new ProductConfigWrapper(
                delegator, dispatcher, 'CFSV1001', null, 'DemoCatalog', '9000', 'USD', locale, demoCustomer)
        configWrapper.setSelected('SCAN_TYPE', 0L, 'SCAN-EC', null)

        shoppingCart.with {
            addOrIncreaseItem('CFSV1001', null, BigDecimal.ONE, null, null, null,
                    null, null, null, null, null, null, 'DemoCatalog', configWrapper, null, null, null, dispatcher)
            setAllShippingContactMechId('9015')
            setAllShipmentMethodTypeId('GROUND')
            setAllCarrierPartyId('UPS')
            addPaymentAmount('EXT_COD', getGrandTotal())
            setDefaultCheckoutOptions(dispatcher)
        }

        CheckOutHelper checkOutHelper = new CheckOutHelper(dispatcher, delegator, shoppingCart)
        Map orderCreateResult = checkOutHelper.createOrder(demoCustomer)
        String orderId = orderCreateResult.orderId
        assert orderId

        OrderChangeHelper.approveOrder(dispatcher, demoCustomer, orderId)
        GenericValue systemLogin = from('UserLogin').where('userLoginId', 'system').queryOne()
        dispatcher.runSync('quickShipEntireOrder', [orderId: orderId, userLogin: systemLogin])
    }

    void testOrderMoveItemBetweenShipGoups() {
        GenericValue userLogin = from('UserLogin').where('userLoginId', 'system').queryOne()
        Locale locale = Locale.getDefault()
        Map orderMap = createShoppinCartAndOrder(userLogin, locale)
        String orderId = orderMap.orderId
        assert orderId

        GenericValue orderItem = from('OrderItem').where('orderId', orderId, 'productId', 'GZ-2644').queryFirst()

        Map createShipGroupMap = [
                userLogin: userLogin,
                orderId: orderId,
                contactMechId: '9015',
                carrierPartyId: 'UPS',
                shipmentMethodTypeId: 'NEXT_DAY'
        ]
        dispatcher.runSync('createOrderItemShipGroup', createShipGroupMap)

        Map moveItemMap = [
                userLogin: userLogin,
                orderId: orderId,
                orderItemSeqId: orderItem.orderItemSeqId,
                fromGroupIndex: '00001',
                toGroupIndex: '00002',
                quantity: 2
        ]
        dispatcher.runSync('moveItemBetweenShipGroups', moveItemMap)

        GenericValue orderItemShipGroupAssoc1 = from('OrderItemShipGroupAssoc').where('orderId',
                                                                                      orderId,
                                                                                      'orderItemSeqId',
                                                                                      orderItem.orderItemSeqId,
                                                                                      'shipGroupSeqId',
                                                                                      '00001').queryOne()
        assert orderItemShipGroupAssoc1.quantity == 3

        GenericValue orderItemShipGroupAssoc2 = from('OrderItemShipGroupAssoc').where('orderId',
                                                                                      orderId,
                                                                                      'orderItemSeqId',
                                                                                      orderItem.orderItemSeqId,
                                                                                      'shipGroupSeqId',
                                                                                      '00002').queryOne()
        assert orderItemShipGroupAssoc2.quantity == 2

        dispatcher.runSync('deleteOrderItemShipGroupAssoc',
                            [userLogin: userLogin,
                            orderId: orderId,
                            orderItemSeqId: orderItem.orderItemSeqId,
                            shipGroupSeqId: '00002'])
        dispatcher.runSync('deleteOrderItemShipGroup', [userLogin: userLogin, orderId: orderId, shipGroupSeqId: '00002'])

        long orderItemShipGroupCount = from('OrderItemShipGroup').where('orderId', orderId).queryCount()
        assert orderItemShipGroupCount == 1
    }
    protected Map createShoppinCartAndOrder(GenericValue userLogin, Locale locale) {
        ShoppingCart shoppingCart = new ShoppingCart(delegator, '9000', locale, 'USD')
        shoppingCart.with {
            setOrderType('SALES_ORDER')
            setUserLogin(userLogin, dispatcher)
            setProductStoreId('9000')
            addPaymentAmount('CREDIT_CARD', 49.26)
            setOrderPartyId('DemoCustomer')
        }

        int itemIndex = shoppingCart.addItemToEnd('GZ-2644', 0, 5, 38.4,
                (HashMap) null, (HashMap) null, 'DemoCatalog', 'PRODUCT_ORDER_ITEM', dispatcher, false, true, true, true)
        org.apache.ofbiz.order.shoppingcart.ShoppingCartItem cartItem = shoppingCart.findCartItem(itemIndex)
        shoppingCart.setItemShipGroupQty(cartItem, 5, 0)

        GenericValue orderAdjustmentPromotion = delegator.makeValue('OrderAdjustment')
        orderAdjustmentPromotion.with {
            orderAdjustmentTypeId = 'PROMOTION_ADJUSTMENT'
            shipGroupSeqId = '_NA_'
            amount = -3.84
            productPromoId = '9011'
            productPromoRuleId = '01'
            productPromoActionSeqId = '01'
        }

        GenericValue orderAdjustmentShipping = delegator.makeValue('OrderAdjustment')
        orderAdjustmentShipping.with {
            orderAdjustmentTypeId = 'SHIPPING_CHARGES'
            shipGroupSeqId = '00001'
            amount = 12.10
        }

        GenericValue orderAdjustmentSalesTax = delegator.makeValue('OrderAdjustment')
        orderAdjustmentSalesTax.with {
            orderAdjustmentTypeId = 'SALES_TAX'
            orderItemSeqId = '00001'
            shipGroupSeqId = '00001'
            amount = 1.824
            sourcePercentage = 0.100000
            taxAuthorityRateSeqId = '9004'
            primaryGeoId = 'UT'
            taxAuthGeoId = 'UT'
            taxAuthPartyId = 'UT_TAXMAN'
            overrideGlAccountId = '224153'
            comments = 'Utah State Sales Tax'
        }

        GenericValue orderAdjustmentSalesTax1 = delegator.makeValue('OrderAdjustment')
        orderAdjustmentSalesTax1.with {
            orderAdjustmentTypeId = 'SALES_TAX'
            orderItemSeqId = '00001'
            shipGroupSeqId = '00001'
            amount = 0.039
            sourcePercentage = 0.100000
            taxAuthorityRateSeqId = '9005'
            primaryGeoId = 'UT-UTAH'
            taxAuthGeoId = 'UT-UTAH'
            taxAuthPartyId = 'UT_UTAH_TAXMAN'
            overrideGlAccountId = '224153'
            comments = 'Utah County, Utah Sales Tax'
        }

        GenericValue orderAdjustmentSalesTax2 = delegator.makeValue('OrderAdjustment')
        orderAdjustmentSalesTax2.with {
            orderAdjustmentTypeId = 'SALES_TAX'
            orderItemSeqId = '00001'
            shipGroupSeqId = '00001'
            amount = 0.384
            sourcePercentage = 1
            taxAuthorityRateSeqId = '9000'
            primaryGeoId = '_NA_'
            taxAuthGeoId = '_NA_'
            taxAuthPartyId = '_NA_'
            overrideGlAccountId = '224000'
            comments = '1% OFB _NA_ Tax'
        }

        shoppingCart.with {
            addAdjustment(orderAdjustmentPromotion)
            addAdjustment(orderAdjustmentShipping)
            addAdjustment(orderAdjustmentSalesTax)
            addAdjustment(orderAdjustmentSalesTax1)
            addAdjustment(orderAdjustmentSalesTax2)
            setAllShippingContactMechId('9015')
            setAllShipmentMethodTypeId('NEXT_DAY')
            setAllCarrierPartyId('UPS')
            setAllIsGift(false)
            setAllMaySplit(false)
            setBillFromVendorPartyId('Company')
            setPlacingCustomerPartyId('DemoCustomer')
            setBillToCustomerPartyId('DemoCustomer')
            setShipToCustomerPartyId('DemoCustomer')
            setEndUserCustomerPartyId('DemoCustomer')
            makeAllShipGroupInfos(dispatcher)
        }

        CheckOutHelper checkOutHelper = new CheckOutHelper(dispatcher, delegator, shoppingCart)
        Map orderMap = checkOutHelper.createOrder(userLogin)
        shoppingCart.clear()

        return orderMap
    }

}
