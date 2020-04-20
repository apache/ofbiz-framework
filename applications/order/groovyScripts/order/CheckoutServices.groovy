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

import org.apache.ofbiz.base.util.StringUtil
import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.minilang.SimpleMapProcessor
import org.apache.ofbiz.order.shoppingcart.CheckOutHelper
import org.apache.ofbiz.order.shoppingcart.ShoppingCart
import org.apache.ofbiz.order.shoppingcart.ShoppingCart.CartPaymentInfo
import org.apache.ofbiz.service.ServiceUtil


/**
 * Create/Update Customer, Shipping Address and other contact details.
 * @return
 */
def createUpdateCustomerAndShippingAddress() {
    Map result = success()

    List<String> messages = []
    Map shipToPhoneCtx = [:]
    Map emailAddressCtx = [:]
    // TODO need to convert from MapProcessor
    SimpleMapProcessor.runSimpleMapProcessor('component://order/minilang/customer/CheckoutMapProcs.xml', 'shipToPhone', parameters, shipToPhoneCtx, messages, context.locale)
    SimpleMapProcessor.runSimpleMapProcessor('component://party/minilang/contact/PartyContactMechMapProcs.xml', 'emailAddress', parameters, emailAddressCtx, messages, context.locale)
    // Check errors
    if (messages) return error(StringUtil.join(messages, ','))

    ShoppingCart shoppingCart = parameters.shoppingCart
    String partyId = parameters.partyId
    GenericValue userLogin = shoppingCart.getUserLogin()
    // If registered user is coming then take partyId from userLogin
    if (userLogin && userLogin.partyId && !partyId) {
        partyId = userLogin.partyId
    }
    Map createUpdatePersonCtx = parameters
    createUpdatePersonCtx.userLogin = userLogin
    createUpdatePersonCtx.partyId = partyId
    Map serviceResultCUP = run service: "createUpdatePerson", with: createUpdatePersonCtx
    partyId = serviceResultCUP.partyId

    Map partyRoleCtx = [partyId: partyId, roleTypeId: "CUSTOMER"]
    if (userLogin) {
        if (userLogin.userLoginId == "anonymos") {
            userLogin.partyId = partyId
        }
        partyRoleCtx.userLogin = userLogin
    }
    run service: "ensurePartyRole", with: partyRoleCtx

    // Create Update Shipping address
    Map shipToAddressCtx = parameters
    shipToAddressCtx.userLogin = userLogin
    Map serviceResultCUSA = run service: "createUpdateShippingAddress", with: shipToAddressCtx
    parameters.shipToContactMechId = serviceResultCUSA.contactMechId
    result.contactMechId = serviceResultCUSA.contactMechId

    // Create Update Shipping Telecom Number
    Map createUpdatePartyTelecomNumberCtx = shipToPhoneCtx
    createUpdatePartyTelecomNumberCtx.userLogin = userLogin
    createUpdatePartyTelecomNumberCtx.partyId = partyId
    createUpdatePartyTelecomNumberCtx.roleTypeId = "CUSTOMER"
    createUpdatePartyTelecomNumberCtx.contactMechPurposeTypeId = "PHONE_SHIPPING"
    createUpdatePartyTelecomNumberCtx.contactMechId = parameters.shipToPhoneContactMechId
    Map serviceResultCUPTN = run service: "createUpdatePartyTelecomNumber", with: createUpdatePartyTelecomNumberCtx
    String shipToPhoneContactMechId = serviceResultCUPTN.contactMechId
    result.shipToPhoneContactMechId = serviceResultCUPTN.contactMechId

    if (shipToPhoneContactMechId) {
        shoppingCart.addContactMech("PHONE_SHIPPING", shipToPhoneContactMechId)
    }
    // Create Update email address
    Map createUpdatePartyEmailCtx = emailAddressCtx
    createUpdatePartyEmailCtx.contactMechPurposeTypeId = "PRIMARY_EMAIL"
    createUpdatePartyEmailCtx.userLogin = userLogin
    createUpdatePartyEmailCtx.partyId = partyId
    Map serviceResultCUPEM = run service: "createUpdatePartyEmailAddress", with: createUpdatePartyEmailCtx
    parameters.emailContactMechId = serviceResultCUPEM.contactMechId
    result.emailContactMechId = serviceResultCUPEM.contactMechId
    result.partyId = partyId
    if (parameters.emailContactMechId) {
        shoppingCart.addContactMech("ORDER_EMAIL", parameters.emailContactMechId)
    }
    shoppingCart.setUserLogin(userLogin, dispatcher)
    shoppingCart.addContactMech("SHIPPING_LOCATION", parameters.shipToContactMechId)
    shoppingCart.setAllShippingContactMechId(parameters.shipToContactMechId)
    shoppingCart.setOrderPartyId(partyId)
    return result
}

/**
 * Create/update billing address and payment information
 * @return
 */
def createUpdateBillingAddressAndPaymentMethod() {
    Map result = success()
    List<String> messages = []
    Map billToPhoneContext = [:]
    // TODO need to convert from MapProcessor
    SimpleMapProcessor.runSimpleMapProcessor('component://order/minilang/customer/CheckoutMapProcs.xml', 'billToPhone', parameters, billToPhoneContext, messages, context.locale)
    // Check Errors
    if (messages) return error(StringUtil.join(messages, ','))

    ShoppingCart shoppingCart = parameters.shoppingCart
    GenericValue userLogin = shoppingCart.getUserLogin()
    String partyId = parameters.partyId
    // If registered user is coming then take partyId from userLogin
    if (userLogin && !partyId) {
        partyId = userLogin.partyId
    }
    String shipToContactMechId = parameters.shipToContactMechId
    if (shoppingCart) {
        if (!partyId) {
            partyId = shoppingCart.getPartyId()
        }
        if (!shipToContactMechId) {
            shipToContactMechId = shoppingCart.getShippingContactMechId()
        }
    }
    if (partyId) {
        if (userLogin.userLoginId == "anonymous") {
            userLogin.partyId = partyId
        }
    }
    // Create Update Billing address
    Map billToAddressCtx = parameters
    billToAddressCtx.userLogin = userLogin
    Map serviceResultCUBA = run service: "createUpdateBillingAddress", with: billToAddressCtx
    if (ServiceUtil.isError(serviceResultCUBA)) return serviceResultCUBA
    parameters.billToContactMechId = serviceResultCUBA.contactMechId
    result.contactMechId = serviceResultCUBA.contactMechId
    if (parameters.billToContactMechId) {
        shoppingCart.addContactMech("BILLING_LOCATION", parameters.billToContactMechId)
    }
    // Create Update Billing Telecom Number
    Map createUpdatePartyTelecomNumberCtx = billToPhoneContext
    createUpdatePartyTelecomNumberCtx.userLogin = userLogin
    createUpdatePartyTelecomNumberCtx.partyId = partyId
    createUpdatePartyTelecomNumberCtx.roleTypeId = "CUSTOMER"
    createUpdatePartyTelecomNumberCtx.contactMechPurposeTypeId = "PHONE_BILLING"
    createUpdatePartyTelecomNumberCtx.contactMechId = parameters.billToPhoneContactMechId
    Map serviceResultCUPTN = run service: "createUpdatePartyTelecomNumber", with: createUpdatePartyTelecomNumberCtx
    if (ServiceUtil.isError(serviceResultCUPTN)) return serviceResultCUPTN
    String billToPhoneContactMechId = serviceResultCUPTN.contactMechId
    result.billToPhoneContactMechId = serviceResultCUPTN.contactMechId
    if (billToPhoneContactMechId) {
        shoppingCart.addContactMech("PHONE_BILLING", billToPhoneContactMechId)
    }
    // Create Update credit card
    Map creditCartCtx = parameters
    creditCartCtx.contactMechId = parameters.billToContactMechId
    creditCartCtx.userLogin = userLogin
    Map serviceResultCUCC = run service: "createUpdateCreditCard", with: creditCartCtx
    if (ServiceUtil.isError(serviceResultCUCC)) return serviceResultCUCC
    String paymentMethodId = serviceResultCUCC.paymentMethodId
    result.paymentMethodId = serviceResultCUCC.paymentMethodId
    // Set Payment Method
    String cardSecurityCode = parameters.billToCardSecurityCode
    CheckOutHelper checkOutHelper = new CheckOutHelper(dispatcher, delegator, shoppingCart)
    Map callResult = checkOutHelper.finalizeOrderEntryPayment(paymentMethodId, null, false, false)
    CartPaymentInfo cartPaymentInfo = shoppingCart.getPaymentInfo(paymentMethodId, null, null, null, true)
    cartPaymentInfo.securityCode = cardSecurityCode
    return result
}

/**
 * Set user login in the session
 * @return
 */
def setAnonUserLogin() {
    ShoppingCart shoppingCart = parameters.shoppingCart
    GenericValue userLogin = shoppingCart.getUserLogin()
    if (!userLogin) {
        userLogin = from("UserLogin").where(userLoginId: "anonymous").queryOne()
    } else {
        // If an anonymous user is coming back, update the party id in the userLogin object
        if (userLogin.userLoginId == "anonymous" && parameters.partyId) {
            userLogin.partyId = parameters.partyId
        }
    }
    shoppingCart.setUserLogin(userLogin, dispatcher)
    return success()
}
