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
package org.apache.ofbiz.order.entry

import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.entity.util.EntityUtil
import org.apache.ofbiz.base.util.UtilHttp
import org.apache.ofbiz.accounting.payment.BillingAccountWorker
import org.apache.ofbiz.order.shoppingcart.ShoppingCart

ShoppingCart cart = session.getAttribute('shoppingCart')
String orderPartyId = cart.getPartyId()
String currencyUomId = cart.getCurrency()
context.cart = cart
context.paymentMethodType = request.getParameter('paymentMethodType')

// nuke the event messages
request.removeAttribute('_EVENT_MESSAGE_')

// If there's a paymentMethodId request attribute, the user has just created a new payment method,
//  so put the new paymentMethodId in the context for the UI
String newPaymentMethodId = request.getAttribute('paymentMethodId')
if (newPaymentMethodId) {
    context.checkOutPaymentId = newPaymentMethodId
}

GenericValue orderParty = null
if (orderPartyId && '_NA_' != orderPartyId) {
    orderParty = from('Party').where('partyId', orderPartyId).cache().queryOne()
    context.orderParty = orderParty
    if (orderParty) {
        context.orderPerson = orderParty.getRelatedOne('Person', true)
        context.paymentMethodList = EntityUtil.filterByDate(orderParty.getRelated('PaymentMethod', null, null, false), true)

        List billingAccountList = BillingAccountWorker.makePartyBillingAccountList(userLogin, currencyUomId, orderPartyId, delegator, dispatcher)
        if (billingAccountList) {
            context.selectedBillingAccountId = cart.getBillingAccountId()
            context.billingAccountList = billingAccountList
        }
    }
}

context.postalFields = request.getParameter('useShipAddr') && cart.getShippingContactMechId()
        ? from('PostalAddress').where('contactMechId', cart.getShippingContactMechId()).queryOne()
        : UtilHttp.getParameterMap(request)

if (cart) {
    if (cart.getPaymentMethodIds()) {
        String checkOutPaymentId = cart.getPaymentMethodIds().first()
        context.checkOutPaymentId = checkOutPaymentId
        if (!orderParty) {
            GenericValue account = null
            GenericValue paymentMethod = from('PaymentMethod').where(paymentMethodId: checkOutPaymentId).queryOne()
            if (paymentMethod?.paymentMethodTypeId == 'CREDIT_CARD') {
                String paymentMethodType = 'CC'
                account = paymentMethod.getRelatedOne('CreditCard', false)
                context.creditCard = account
                context.paymentMethodType = paymentMethodType
            } else if (paymentMethod?.paymentMethodTypeId == 'EFT_ACCOUNT') {
                String paymentMethodType = 'EFT'
                account = paymentMethod.getRelatedOne('EftAccount', false)
                context.eftAccount = account
                context.paymentMethodType = paymentMethodType
            }
            if (account) {
                context.postalAddress = account.getRelatedOne('PostalAddress', false)
            }
        }
    } else if (cart.getPaymentMethodTypeIds()) {
        context.checkOutPaymentId = cart.getPaymentMethodTypeIds().first()
    }
}
