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

@SuppressWarnings(['LineLength', 'UnnecessaryObjectReferences', 'UnnecessaryGString', 'PublicMethodsBeforeNonPublicMethods', 'ClassSize', 'MethodCount', 'ConsecutiveBlankLines', 'BlockEndsWithBlankLine', 'ClassEndsWithBlankLine'])
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
        shoppingCart.setOrderType('SALES_ORDER')
        shoppingCart.setChannelType('WEB_SALES_CHANNEL')
        shoppingCart.setProductStoreId('9000')
        shoppingCart.setBillToCustomerPartyId('DemoCustomer')
        shoppingCart.setPlacingCustomerPartyId('DemoCustomer')
        shoppingCart.setShipToCustomerPartyId('DemoCustomer')
        shoppingCart.setEndUserCustomerPartyId('DemoCustomer')
        shoppingCart.setUserLogin(demoCustomer, dispatcher)

        Timestamp nextDate = UtilDateTime.addDaysToTimestamp(UtilDateTime.nowTimestamp(), 1)
        shoppingCart.addOrIncreaseItem('RentalShip',
                                    null,
                                    BigDecimal.ONE,
                                    nextDate,
                                    3,
                                    1,
                                    null,
                                    null,
                                    null,
                                    null,
                                    null,
                                    null,
                                    'DemoCatalog',
                                    null,
                                    'RENTAL_ORDER_ITEM',
                                    null,
                                    null,
                                    dispatcher)
        shoppingCart.setDefaultCheckoutOptions(dispatcher)

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
        shoppingCart.setOrderType('SALES_ORDER')
        shoppingCart.setChannelType('WEB_SALES_CHANNEL')
        shoppingCart.setProductStoreId('9000')
        shoppingCart.setBillToCustomerPartyId('DemoCustomer')
        shoppingCart.setPlacingCustomerPartyId('DemoCustomer')
        shoppingCart.setShipToCustomerPartyId('DemoCustomer')
        shoppingCart.setEndUserCustomerPartyId('DemoCustomer')
        shoppingCart.setUserLogin(demoCustomer, dispatcher)

        shoppingCart.addOrIncreaseItem('SV-1001',
                                    null,
                                    BigDecimal.ONE,
                                    null,
                                    null,
                                    null,
                                    null,
                                    null,
                                    null,
                                    null,
                                    null,
                                    null,
                                    'DemoCatalog',
                                    null,
                                    null,
                                    null,
                                    null,
                                    dispatcher)
        shoppingCart.setDefaultCheckoutOptions(dispatcher)

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
        shoppingCart.setOrderType('SALES_ORDER')
        shoppingCart.setChannelType('WEB_SALES_CHANNEL')
        shoppingCart.setProductStoreId('9000')
        shoppingCart.setBillToCustomerPartyId('DemoCustomer')
        shoppingCart.setPlacingCustomerPartyId('DemoCustomer')
        shoppingCart.setShipToCustomerPartyId('DemoCustomer')
        shoppingCart.setEndUserCustomerPartyId('DemoCustomer')
        shoppingCart.setUserLogin(demoCustomer, dispatcher)

        ProductConfigWrapper configWrapper = new ProductConfigWrapper(delegator, dispatcher, 'CFSV1001', null, 'DemoCatalog', '9000', 'USD', locale, demoCustomer)
        configWrapper.setSelected('SCAN_TYPE', 0L, 'SCAN-EC', null)

        shoppingCart.addOrIncreaseItem('CFSV1001',
                                    null,
                                    BigDecimal.ONE,
                                    null,
                                    null,
                                    null,
                                    null,
                                    null,
                                    null,
                                    null,
                                    null,
                                    null,
                                    'DemoCatalog',
                                    configWrapper,
                                    null,
                                    null,
                                    null,
                                    dispatcher)

        shoppingCart.setAllShippingContactMechId('9015')
        shoppingCart.setAllShipmentMethodTypeId('GROUND')
        shoppingCart.setAllCarrierPartyId('UPS')
        shoppingCart.addPaymentAmount('EXT_COD', shoppingCart.getGrandTotal())

        shoppingCart.setDefaultCheckoutOptions(dispatcher)

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
        shoppingCart.setOrderType('SALES_ORDER')
        shoppingCart.setUserLogin(userLogin, dispatcher)
        shoppingCart.setProductStoreId('9000')
        shoppingCart.addPaymentAmount('CREDIT_CARD', 49.26)
        shoppingCart.setOrderPartyId('DemoCustomer')

        int itemIndex = shoppingCart.addItemToEnd('GZ-2644',
                                                    0,
                                                    5,
                                                    38.4,
                                                    (HashMap) null, (HashMap) null, 'DemoCatalog', 'PRODUCT_ORDER_ITEM', dispatcher, false, true, true, true)
        org.apache.ofbiz.order.shoppingcart.ShoppingCartItem cartItem = shoppingCart.findCartItem(itemIndex)
        shoppingCart.setItemShipGroupQty(cartItem, 5, 0)

        GenericValue orderAdjustmentPromotion = delegator.makeValue('OrderAdjustment')
        orderAdjustmentPromotion.orderAdjustmentTypeId = 'PROMOTION_ADJUSTMENT'
        orderAdjustmentPromotion.shipGroupSeqId = '_NA_'
        orderAdjustmentPromotion.amount = -3.84
        orderAdjustmentPromotion.productPromoId = '9011'
        orderAdjustmentPromotion.productPromoRuleId = '01'
        orderAdjustmentPromotion.productPromoActionSeqId = '01'
        shoppingCart.addAdjustment(orderAdjustmentPromotion)

        GenericValue orderAdjustmentShipping = delegator.makeValue('OrderAdjustment')
        orderAdjustmentShipping.orderAdjustmentTypeId = 'SHIPPING_CHARGES'
        orderAdjustmentShipping.shipGroupSeqId = '00001'
        orderAdjustmentShipping.amount = 12.10
        shoppingCart.addAdjustment(orderAdjustmentShipping)

        GenericValue orderAdjustmentSalesTax = delegator.makeValue('OrderAdjustment')
        orderAdjustmentSalesTax.orderAdjustmentTypeId = 'SALES_TAX'
        orderAdjustmentSalesTax.orderItemSeqId = '00001'
        orderAdjustmentSalesTax.shipGroupSeqId = '00001'
        orderAdjustmentSalesTax.amount = 1.824
        orderAdjustmentSalesTax.sourcePercentage = 0.100000
        orderAdjustmentSalesTax.taxAuthorityRateSeqId = '9004'
        orderAdjustmentSalesTax.primaryGeoId = 'UT'
        orderAdjustmentSalesTax.taxAuthGeoId = 'UT'
        orderAdjustmentSalesTax.with {
            taxAuthPartyId = 'UT_TAXMAN'
            overrideGlAccountId = '224153'
            comments = 'Utah State Sales Tax'
        }
        shoppingCart.addAdjustment(orderAdjustmentSalesTax)

        GenericValue orderAdjustmentSalesTax1 = delegator.makeValue('OrderAdjustment')
        orderAdjustmentSalesTax1.orderAdjustmentTypeId = 'SALES_TAX'
        orderAdjustmentSalesTax1.orderItemSeqId = '00001'
        orderAdjustmentSalesTax1.shipGroupSeqId = '00001'
        orderAdjustmentSalesTax1.amount = 0.039
        orderAdjustmentSalesTax1.sourcePercentage = 0.100000
        orderAdjustmentSalesTax1.taxAuthorityRateSeqId = '9005'
        orderAdjustmentSalesTax1.primaryGeoId = 'UT-UTAH'
        orderAdjustmentSalesTax1.taxAuthGeoId = 'UT-UTAH'
        orderAdjustmentSalesTax1.with {
            taxAuthPartyId = 'UT_UTAH_TAXMAN'
            overrideGlAccountId = '224153'
            comments = 'Utah County, Utah Sales Tax'
        }
        shoppingCart.addAdjustment(orderAdjustmentSalesTax1)

        GenericValue orderAdjustmentSalesTax2 = delegator.makeValue('OrderAdjustment')
        orderAdjustmentSalesTax2.orderAdjustmentTypeId = 'SALES_TAX'
        orderAdjustmentSalesTax2.orderItemSeqId = '00001'
        orderAdjustmentSalesTax2.shipGroupSeqId = '00001'
        orderAdjustmentSalesTax2.amount = 0.384
        orderAdjustmentSalesTax2.sourcePercentage = 1
        orderAdjustmentSalesTax2.taxAuthorityRateSeqId = '9000'
        orderAdjustmentSalesTax2.primaryGeoId = '_NA_'
        orderAdjustmentSalesTax2.taxAuthGeoId = '_NA_'
        orderAdjustmentSalesTax2.with {
            taxAuthPartyId = '_NA_'
            overrideGlAccountId = '224000'
            comments = '1% OFB _NA_ Tax'
        }
        shoppingCart.addAdjustment(orderAdjustmentSalesTax2)

        shoppingCart.setAllShippingContactMechId('9015')
        shoppingCart.setAllShipmentMethodTypeId('NEXT_DAY')
        shoppingCart.setAllCarrierPartyId('UPS')
        shoppingCart.setAllIsGift(false)
        shoppingCart.setAllMaySplit(false)

        shoppingCart.setBillFromVendorPartyId('Company')
        shoppingCart.setPlacingCustomerPartyId('DemoCustomer')
        shoppingCart.with {
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
