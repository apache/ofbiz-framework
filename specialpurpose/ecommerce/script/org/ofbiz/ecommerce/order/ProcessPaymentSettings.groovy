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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ofbiz.base.util.UtilHttp;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.order.shoppingcart.CheckOutHelper;
import org.ofbiz.order.shoppingcart.ShoppingCart;
import org.ofbiz.order.shoppingcart.ShoppingCartEvents;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ModelService;
import org.ofbiz.service.ServiceUtil;

cart = ShoppingCartEvents.getCartObject(request);
dispatcher = request.getAttribute("dispatcher");
delegator = request.getAttribute("delegator");
checkOutHelper = new CheckOutHelper(dispatcher, delegator, cart);
paramMap = UtilHttp.getParameterMap(request);

paymentMethodTypeId = paramMap.paymentMethodTypeId;
errorMessages = [];
errorMaps = [:];

if (paymentMethodTypeId) {
    paymentMethodId = request.getAttribute("paymentMethodId");
    if ("EXT_OFFLINE".equals(paymentMethodTypeId)) {
        paymentMethodId = "EXT_OFFLINE";
    }
    singleUsePayment = paramMap.singleUsePayment;
    appendPayment = paramMap.appendPayment;
    isSingleUsePayment = "Y".equalsIgnoreCase(singleUsePayment) ?: false;
    doAppendPayment = "Y".equalsIgnoreCase(appendPayment) ?: false;
    callResult = checkOutHelper.finalizeOrderEntryPayment(paymentMethodId, null, isSingleUsePayment, doAppendPayment);
    cpi = cart.getPaymentInfo(paymentMethodId, null, null, null, true);
    cpi.securityCode = paramMap.cardSecurityCode;
    ServiceUtil.addErrors(errorMessages, errorMaps, callResult);
}

if (!errorMessages && !errorMaps) {
    selPaymentMethods = null;
    addGiftCard = paramMap.addGiftCard;
    if ("Y".equalsIgnoreCase(addGiftCard)) {
        selPaymentMethods = [paymentMethodTypeId : null];
        callResult = checkOutHelper.checkGiftCard(paramMap, selPaymentMethods);
        ServiceUtil.addErrors(errorMessages, errorMaps, callResult);
        if (!errorMessages && !errorMaps) {
            gcPaymentMethodId = callResult.paymentMethodId;
            giftCardAmount = callResult.amount;
            gcCallRes = checkOutHelper.finalizeOrderEntryPayment(gcPaymentMethodId, giftCardAmount, true, true);
            ServiceUtil.addErrors(errorMessages, errorMaps, gcCallRes);
        }
    }
}

//See whether we need to return an error or not
callResult = ServiceUtil.returnSuccess();
if (errorMessages || errorMaps) {
    request.setAttribute(ModelService.ERROR_MESSAGE_LIST, errorMessages);
    request.setAttribute(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_ERROR);
    return "error";
}
return "success";
