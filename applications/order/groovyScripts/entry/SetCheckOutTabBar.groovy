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

import org.apache.ofbiz.order.shoppingcart.ShoppingCartEvents

checkoutSteps = []

shoppingCart = ShoppingCartEvents.getCartObject(request)

// ----------------------------------
// The ordered list of steps is prepared here
// ----------------------------------
checkoutSteps.add([label : "OrderOrderItems", uri : "orderentry", enabled : "Y"])

if ("PURCHASE_ORDER".equals(shoppingCart.getOrderType())) {
    checkoutSteps.add([label : "OrderOrderTerms", uri : "setOrderTerm", enabled : "Y"])
}
checkoutSteps.add([label : "FacilityShipping", uri : "setShipping", enabled : "Y"])
if (shoppingCart.getShipGroupSize() > 1) {
    checkoutSteps.add([label : "OrderShipGroups", uri : "SetItemShipGroups", enabled : "Y"])
}
checkoutSteps.add([label : "CommonOptions", uri : "setOptions", enabled : "Y"])
if ("SALES_ORDER".equals(shoppingCart.getOrderType())) {
    checkoutSteps.add([label : "OrderOrderTerms", uri : "setOrderTerm", enabled : "Y"])
    checkoutSteps.add([label : "AccountingPayment", uri : "setBilling", enabled : "Y"])
}
checkoutSteps.add([label : "PartyParties", uri : "setAdditionalParty", enabled : "Y"])
checkoutSteps.add([label : "OrderReviewOrder", uri : "confirmOrder", enabled : "Y"])

// ---------------------------------------

isLastStep = "N"
enabled = "Y"
checkoutSteps.eachWithIndex { checkoutStep, i ->
    checkoutStep.put("enabled", enabled)
    if ("Y".equals(enabled)) {
        if (i == (checkoutSteps.size() - 1)) {
            isLastStep = "Y"
        }
        if (stepLabelId.equals(checkoutStep.label)) {
            enabled = "N"
        }
    }
}

context.isLastStep = isLastStep
context.checkoutSteps = checkoutSteps
