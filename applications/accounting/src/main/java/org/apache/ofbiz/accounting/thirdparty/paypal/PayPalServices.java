/*******************************************************************************
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
 *******************************************************************************/
package org.apache.ofbiz.accounting.thirdparty.paypal;

import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.lang.ref.WeakReference;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.Transaction;

import org.apache.commons.lang.StringUtils;
import org.apache.ofbiz.accounting.payment.PaymentGatewayServices;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.StringUtil;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilHttp;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityComparisonOperator;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityFunction;
import org.apache.ofbiz.entity.transaction.GenericTransactionException;
import org.apache.ofbiz.entity.transaction.TransactionUtil;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.EntityUtil;
import org.apache.ofbiz.order.order.OrderReadHelper;
import org.apache.ofbiz.order.shoppingcart.CartItemModifyException;
import org.apache.ofbiz.order.shoppingcart.CheckOutHelper;
import org.apache.ofbiz.order.shoppingcart.ShoppingCart;
import org.apache.ofbiz.order.shoppingcart.ShoppingCartItem;
import org.apache.ofbiz.order.shoppingcart.ShoppingCart.CartShipInfo;
import org.apache.ofbiz.order.shoppingcart.shipping.ShippingEstimateWrapper;
import org.apache.ofbiz.order.shoppingcart.shipping.ShippingEvents;
import org.apache.ofbiz.product.store.ProductStoreWorker;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ModelService;
import org.apache.ofbiz.service.ServiceUtil;

import com.paypal.sdk.core.nvp.NVPDecoder;
import com.paypal.sdk.core.nvp.NVPEncoder;
import com.paypal.sdk.exceptions.PayPalException;
import com.paypal.sdk.profiles.APIProfile;
import com.paypal.sdk.profiles.ProfileFactory;
import com.paypal.sdk.services.NVPCallerServices;

/**
 * PayPalServices for NVP API communication
 */
public class PayPalServices {

    public static final String module = PayPalServices.class.getName();
    public final static String resource = "AccountingErrorUiLabels";
    
    // Used to maintain a weak reference to the ShoppingCart for customers who have gone to PayPal to checkout
    // so that we can quickly grab the cart, perform shipment estimates and send the info back to PayPal.
    // The weak key is a simple wrapper for the checkout token String and is stored as a cart attribute. The value
    // is a weak reference to the ShoppingCart itself.  Entries will be removed as carts are removed from the
    // session (i.e. on cart clear or successful checkout) or when the session is destroyed
    private static Map<TokenWrapper, WeakReference<ShoppingCart>> tokenCartMap = new WeakHashMap<TokenWrapper, WeakReference<ShoppingCart>>();

    public static Map<String, Object> setExpressCheckout(DispatchContext dctx, Map<String, ? extends Object> context) {
        ShoppingCart cart = (ShoppingCart) context.get("cart");
        Locale locale = cart.getLocale();
        if (cart == null || cart.items().size() <= 0) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                    "AccountingPayPalShoppingCartIsEmpty", locale));
        }

        GenericValue payPalConfig = getPaymentMethodGatewayPayPal(dctx, context, null);
        if (payPalConfig == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                    "AccountingPayPalPaymentGatewayConfigCannotFind", locale));
        }


        NVPEncoder encoder = new NVPEncoder();

        // Set Express Checkout Request Parameters
        encoder.add("METHOD", "SetExpressCheckout");
        String token = (String) cart.getAttribute("payPalCheckoutToken");
        if (UtilValidate.isNotEmpty(token)) {
            encoder.add("TOKEN", token);
        }
        encoder.add("RETURNURL", payPalConfig.getString("returnUrl"));
        encoder.add("CANCELURL", payPalConfig.getString("cancelReturnUrl"));
        if (!cart.shippingApplies()) {
            encoder.add("NOSHIPPING", "1");
        } else {
            encoder.add("CALLBACK", payPalConfig.getString("shippingCallbackUrl"));
            encoder.add("CALLBACKTIMEOUT", "6");
            // Default to no
            String reqConfirmShipping = "Y".equals(payPalConfig.getString("requireConfirmedShipping")) ? "1" : "0";
            encoder.add("REQCONFIRMSHIPPING", reqConfirmShipping);
            // Default shipment method
            encoder.add("L_SHIPPINGOPTIONISDEFAULT0", "true");
            encoder.add("L_SHIPPINGOPTIONNAME0", "Calculated Offline");
            encoder.add("L_SHIPPINGOPTIONAMOUNT0", "0.00");
        }
        encoder.add("ALLOWNOTE", "1");
        encoder.add("INSURANCEOPTIONOFFERED", "false");
        if (UtilValidate.isNotEmpty(payPalConfig.getString("imageUrl")));
        encoder.add("PAYMENTACTION", "Order");

        // Cart information
        try {
            addCartDetails(encoder, cart);
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                    "AccountingPayPalErrorDuringRetrievingCartDetails", locale));
        }

        NVPDecoder decoder;
        try {
            decoder = sendNVPRequest(payPalConfig, encoder);
        } catch (PayPalException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        Map<String, String> errorMessages = getErrorMessageMap(decoder);
        if (UtilValidate.isNotEmpty(errorMessages)) {
            if (errorMessages.containsKey("10411")) {
                // Token has expired, get a new one
                cart.setAttribute("payPalCheckoutToken", null);
                return PayPalServices.setExpressCheckout(dctx, context);
            }
            return ServiceUtil.returnError(UtilMisc.toList(errorMessages.values()));
        }

        token = decoder.get("TOKEN");
        cart.setAttribute("payPalCheckoutToken", token);
        TokenWrapper tokenWrapper = new TokenWrapper(token);
        cart.setAttribute("payPalCheckoutTokenObj", tokenWrapper);
        PayPalServices.tokenCartMap.put(tokenWrapper, new WeakReference<ShoppingCart>(cart));
        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> payPalCheckoutUpdate(DispatchContext dctx, Map<String, Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        HttpServletRequest request = (HttpServletRequest) context.get("request");
        HttpServletResponse response = (HttpServletResponse) context.get("response");

        Map<String, Object> paramMap = UtilHttp.getParameterMap(request);

        String token = (String)paramMap.get("TOKEN");
        WeakReference<ShoppingCart> weakCart = tokenCartMap.get(new TokenWrapper(token));
        ShoppingCart cart = null;
        if (weakCart != null) {
            cart = weakCart.get();
        }
        if (cart == null) {
            Debug.logError("Could locate the ShoppingCart for token " + token, module);
            return ServiceUtil.returnSuccess();
        }
        // Since most if not all of the shipping estimate codes requires a persisted contactMechId we'll create one and
        // then delete once we're done, now is not the time to worry about updating everything
        String contactMechId = null;
        Map<String, Object> inMap = new HashMap<String, Object>();
        inMap.put("address1", paramMap.get("SHIPTOSTREET"));
        inMap.put("address2", paramMap.get("SHIPTOSTREET2"));
        inMap.put("city", paramMap.get("SHIPTOCITY"));
        String countryGeoCode = (String) paramMap.get("SHIPTOCOUNTRY");
        String countryGeoId = PayPalServices.getCountryGeoIdFromGeoCode(countryGeoCode, delegator);
        if (countryGeoId == null) {
            return ServiceUtil.returnSuccess();
        }
        inMap.put("countryGeoId", countryGeoId);
        inMap.put("stateProvinceGeoId", parseStateProvinceGeoId((String)paramMap.get("SHIPTOSTATE"), countryGeoId, delegator));
        inMap.put("postalCode", paramMap.get("SHIPTOZIP"));

        try {
            GenericValue userLogin = EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "system").cache().queryOne();
            inMap.put("userLogin", userLogin);
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }
        boolean beganTransaction = false;
        Transaction parentTransaction = null;
        try {
            parentTransaction = TransactionUtil.suspend();
            beganTransaction = TransactionUtil.begin();
        } catch (GenericTransactionException e1) {
            Debug.logError(e1, module);
        }
        try {
            Map<String, Object> outMap = dispatcher.runSync("createPostalAddress", inMap);
            contactMechId = (String) outMap.get("contactMechId");
        } catch (GenericServiceException e) {
            Debug.logError(e.getMessage(), module);
            return ServiceUtil.returnSuccess();
        }
        try {
            TransactionUtil.commit(beganTransaction);
            if (parentTransaction != null) TransactionUtil.resume(parentTransaction);
        } catch (GenericTransactionException e) {
            Debug.logError(e, module);
        }
        // clone the cart so we can modify it temporarily
        CheckOutHelper coh = new CheckOutHelper(dispatcher, delegator, cart);
        String oldShipAddress = cart.getShippingContactMechId();
        coh.setCheckOutShippingAddress(contactMechId);
        ShippingEstimateWrapper estWrapper = new ShippingEstimateWrapper(dispatcher, cart, 0);
        int line = 0;
        NVPEncoder encoder = new NVPEncoder();
        encoder.add("METHOD", "CallbackResponse");

        for (GenericValue shipMethod : estWrapper.getShippingMethods()) {
            BigDecimal estimate = estWrapper.getShippingEstimate(shipMethod);
            //Check that we have a valid estimate (allowing zero value estimates for now)
            if (estimate == null || estimate.compareTo(BigDecimal.ZERO) < 0) {
                continue;
            }
            cart.setAllShipmentMethodTypeId(shipMethod.getString("shipmentMethodTypeId"));
            cart.setAllCarrierPartyId(shipMethod.getString("partyId"));
            try {
                coh.calcAndAddTax();
            } catch (GeneralException e) {
                Debug.logError(e, module);
                continue;
            }
            String estimateLabel = shipMethod.getString("partyId") + " - " + shipMethod.getString("description");
            encoder.add("L_SHIPINGPOPTIONLABEL" + line, estimateLabel);
            encoder.add("L_SHIPPINGOPTIONAMOUNT" + line, estimate.setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString());
            // Just make this first one default for now
            encoder.add("L_SHIPPINGOPTIONISDEFAULT" + line, line == 0 ? "true" : "false");
            encoder.add("L_TAXAMT" + line, cart.getTotalSalesTax().setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString());
            line++;
        }
        String responseMsg = null;
        try {
            responseMsg = encoder.encode();
        } catch (PayPalException e) {
            Debug.logError(e, module);
        }
        if (responseMsg != null) {
            try {
                response.setContentLength(responseMsg.getBytes("UTF-8").length);
            } catch (UnsupportedEncodingException e) {
                Debug.logError(e, module);
            }

            try {
                Writer writer = response.getWriter();
                writer.write(responseMsg);
                writer.close();
            } catch (IOException e) {
                Debug.logError(e, module);
            }
        }

        // Remove the temporary ship address
        try {
            GenericValue postalAddress = EntityQuery.use(delegator).from("PostalAddress").where("contactMechId", contactMechId).queryOne();
            postalAddress.remove();
            GenericValue contactMech = EntityQuery.use(delegator).from("ContactMech").where("contactMechId", contactMechId).queryOne();
            contactMech.remove();
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }
        coh.setCheckOutShippingAddress(oldShipAddress);
        return ServiceUtil.returnSuccess();
    }

    private static Map<String, String> getErrorMessageMap(NVPDecoder decoder) {
        String ack = decoder.get("ACK");
        Map<String, String> result = null;
        if (!"Success".equals(ack)) {
            result = new HashMap<String, Object>();
            int i = 0;
            while (UtilValidate.isNotEmpty(decoder.get("L_ERRORCODE" + i))) {
                String errorCode = decoder.get("L_ERRORCODE" + i);
                String longMsg = decoder.get("L_LONGMESSAGE" + i);
                result.put(errorCode, "PayPal Response Error: [" + errorCode + "]" + longMsg);
                i++;
            }
        }
        return result;
    }

    private static void addCartDetails(NVPEncoder encoder, ShoppingCart cart) throws GenericEntityException {
        encoder.add("CURRENCYCODE", cart.getCurrency());
        int line = 0;
        for (ShoppingCartItem item : cart.items()) {
            encoder.add("L_NUMBER" + line, item.getProductId());
            encoder.add("L_NAME" + line, item.getName());
            encoder.add("L_AMT" + line, item.getBasePrice().setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString());
            encoder.add("L_QTY" + line, item.getQuantity().toBigInteger().toString());
            line++;
            BigDecimal otherAdjustments = item.getOtherAdjustments();
            if (otherAdjustments.compareTo(BigDecimal.ZERO) != 0) {
                encoder.add("L_NUMBER" + line, item.getProductId());
                encoder.add("L_NAME" + line, item.getName() + " Adjustments");
                encoder.add("L_AMT" + line, otherAdjustments.setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString());
                encoder.add("L_QTY" + line, "1");
                line++;
            }
        }
        BigDecimal otherAdjustments = cart.getOrderOtherAdjustmentTotal();
        if (otherAdjustments.compareTo(BigDecimal.ZERO) != 0) {
            encoder.add("L_NUMBER" + line, "N/A");
            encoder.add("L_NAME" + line, "Order Adjustments");
            encoder.add("L_AMT" + line, otherAdjustments.setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString());
            encoder.add("L_QTY" + line, "1");
            line++;
        }
        encoder.add("ITEMAMT", cart.getSubTotal().add(otherAdjustments).setScale(2).toPlainString());
        encoder.add("SHIPPINGAMT", "0.00");
        encoder.add("TAXAMT", "0.00");
        encoder.add("AMT", cart.getSubTotal().add(otherAdjustments).setScale(2).toPlainString());
        //NOTE: The docs say this is optional but then won't work without it
        encoder.add("MAXAMT", cart.getSubTotal().add(otherAdjustments).setScale(2).toPlainString());
    }

    public static Map<String, Object> getExpressCheckout(DispatchContext dctx, Map<String, Object> context) {
        Locale locale = (Locale) context.get("locale");
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();

        ShoppingCart cart = (ShoppingCart) context.get("cart");
        GenericValue payPalConfig = getPaymentMethodGatewayPayPal(dctx, context, null);
        if (payPalConfig == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                    "AccountingPayPalPaymentGatewayConfigCannotFind", locale));
        }

        NVPEncoder encoder = new NVPEncoder();
        encoder.add("METHOD", "GetExpressCheckoutDetails");
        String token = (String) cart.getAttribute("payPalCheckoutToken");
        if (UtilValidate.isNotEmpty(token)) {
            encoder.add("TOKEN", token);
        } else {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                    "AccountingPayPalTokenNotFound", locale));
        }

        NVPDecoder decoder;
        try {
            decoder = sendNVPRequest(payPalConfig, encoder);
        } catch (PayPalException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        if (UtilValidate.isNotEmpty(decoder.get("NOTE"))) {
            cart.addOrderNote(decoder.get("NOTE"));
        }

        if (cart.getUserLogin() == null) {
            try {
                GenericValue userLogin = EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "anonymous").queryOne();
                try {
                    cart.setUserLogin(userLogin, dispatcher);
                } catch (CartItemModifyException e) {
                    Debug.logError(e, module);
                    return ServiceUtil.returnError(e.getMessage());
                }
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
                return ServiceUtil.returnError(e.getMessage());
            }
        }
        boolean anon = "anonymous".equals(cart.getUserLogin().getString("userLoginId"));
        // Even if anon, a party could already have been created
        String partyId = cart.getOrderPartyId();
        if (partyId == null && anon) {
            // Check nothing has been set on the anon userLogin either
            partyId = cart.getUserLogin() != null ? cart.getUserLogin().getString("partyId") : null;
            cart.setOrderPartyId(partyId);
        }
        if (partyId != null) {
            GenericValue party = null;
            try {
                party = EntityQuery.use(delegator).from("Party").where("partyId", partyId).queryOne();
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
            }
            if (party == null) {
                partyId = null;
            }
        }

        Map<String, Object> inMap = new HashMap<String, Object>();
        Map<String, Object> outMap = null;
        // Create the person if necessary
        boolean newParty = false;
        if (partyId == null) {
            newParty = true;
            inMap.put("userLogin", cart.getUserLogin());
            inMap.put("personalTitle", decoder.get("SALUTATION"));
            inMap.put("firstName", decoder.get("FIRSTNAME"));
            inMap.put("middleName", decoder.get("MIDDLENAME"));
            inMap.put("lastName", decoder.get("LASTNAME"));
            inMap.put("suffix", decoder.get("SUFFIX"));
            try {
                outMap = dispatcher.runSync("createPerson", inMap);
                partyId = (String) outMap.get("partyId");
                cart.setOrderPartyId(partyId);
                cart.getUserLogin().setString("partyId", partyId);
                inMap.clear();
                inMap.put("userLogin", cart.getUserLogin());
                inMap.put("partyId", partyId);
                inMap.put("roleTypeId", "CUSTOMER");
                dispatcher.runSync("createPartyRole", inMap);
            } catch (GenericServiceException e) {
                Debug.logError(e, module);
                return ServiceUtil.returnError(e.getMessage());
            }
        }
        // Create a new email address if necessary
        String emailContactMechId = null;
        String emailContactPurposeTypeId = "PRIMARY_EMAIL";
        String emailAddress = decoder.get("EMAIL");
        if (!newParty) {
            EntityCondition cond = EntityCondition.makeCondition(UtilMisc.toList(
                    EntityCondition.makeCondition(UtilMisc.toMap("partyId", partyId, "contactMechTypeId", "EMAIL_ADDRESS")),
                    EntityCondition.makeCondition(EntityFunction.UPPER_FIELD("infoString"), EntityComparisonOperator.EQUALS, EntityFunction.UPPER(emailAddress))

           ));
            try {
                GenericValue matchingEmail = EntityQuery.use(delegator).from("PartyAndContactMech").where(cond).orderBy("fromDate").filterByDate().queryFirst();
                if (matchingEmail != null) {
                    emailContactMechId = matchingEmail.getString("contactMechId");
                } else {
                    // No email found so we'll need to create one but first check if it should be PRIMARY or just BILLING
                    long primaryEmails = EntityQuery.use(delegator)
                            .from("PartyContactWithPurpose")
                            .where("partyId", partyId, 
                                    "contactMechTypeId", "EMAIL_ADDRESS", 
                                    "contactMechPurposeTypeId", "PRIMARY_EMAIL")
                            .filterByDate("contactFromDate", "contactThruDate", "purposeFromDate", "purposeThruDate")
                            .queryCount();
                    if (primaryEmails > 0) emailContactPurposeTypeId = "BILLING_EMAIL";
                }
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
            }
        }
        if (emailContactMechId == null) {
            inMap.clear();
            inMap.put("userLogin", cart.getUserLogin());
            inMap.put("contactMechPurposeTypeId", emailContactPurposeTypeId);
            inMap.put("emailAddress", emailAddress);
            inMap.put("partyId", partyId);
            inMap.put("roleTypeId", "CUSTOMER");
            inMap.put("verified", "Y");  // Going to assume PayPal has taken care of this for us
            inMap.put("fromDate", UtilDateTime.nowTimestamp());
            try {
                outMap = dispatcher.runSync("createPartyEmailAddress", inMap);
                emailContactMechId = (String) outMap.get("contactMechId");
            } catch (GenericServiceException e) {
                Debug.logError(e, module);
                return ServiceUtil.returnError(e.getMessage());
            }
        }
        cart.addContactMech("ORDER_EMAIL", emailContactMechId);

        // Phone number
        String phoneNumber = decoder.get("PHONENUM");
        String phoneContactId = null;
        if (phoneNumber != null) {
            inMap.clear();
            if (phoneNumber.startsWith("+")) {
                // International, format is +XXX XXXXXXXX which we'll split into countryCode + contactNumber
                String[] phoneNumbers = phoneNumber.split(" ");
                inMap.put("countryCode", StringUtil.removeNonNumeric(phoneNumbers[0]));
                inMap.put("contactNumber", phoneNumbers[1]);
            } else {
                // U.S., format is XXX-XXX-XXXX which we'll split into areaCode + contactNumber
                inMap.put("countryCode", "1");
                String[] phoneNumbers = phoneNumber.split("-");
                inMap.put("areaCode", phoneNumbers[0]);
                inMap.put("contactNumber", phoneNumbers[1] + phoneNumbers[2]);
            }
            inMap.put("userLogin", cart.getUserLogin());
            inMap.put("partyId", partyId);
            try {
                outMap = dispatcher.runSync("createUpdatePartyTelecomNumber", inMap);
                phoneContactId = (String) outMap.get("contactMechId");
                cart.addContactMech("PHONE_BILLING", phoneContactId);
            } catch (GenericServiceException e) {
                Debug.logError(e, module);
            }
        }
        // Create a new Postal Address if necessary
        String postalContactId = null;
        boolean needsShippingPurpose = true;
        // if the cart for some reason already has a billing address, we'll leave it be
        boolean needsBillingPurpose = (cart.getContactMech("BILLING_LOCATION") == null);
        Map<String, Object> postalMap = new HashMap<String, Object>();
        postalMap.put("toName", decoder.get("SHIPTONAME"));
        postalMap.put("address1", decoder.get("SHIPTOSTREET"));
        postalMap.put("address2", decoder.get("SHIPTOSTREET2"));
        postalMap.put("city", decoder.get("SHIPTOCITY"));
        String countryGeoId = PayPalServices.getCountryGeoIdFromGeoCode(decoder.get("SHIPTOCOUNTRYCODE"), delegator);
        postalMap.put("countryGeoId", countryGeoId);
        postalMap.put("stateProvinceGeoId", parseStateProvinceGeoId(decoder.get("SHIPTOSTATE"), countryGeoId, delegator));
        postalMap.put("postalCode", decoder.get("SHIPTOZIP"));
        if (!newParty) {
            // We want an exact match only
            EntityCondition cond = EntityCondition.makeCondition(UtilMisc.toList(
                    EntityCondition.makeCondition(postalMap),
                    EntityCondition.makeCondition(UtilMisc.toMap("attnName", null, "directions", null, "postalCodeExt", null,"postalCodeGeoId", null)),
                    EntityCondition.makeCondition("partyId", partyId)
           ));
            try {
                GenericValue postalMatch = EntityQuery.use(delegator).from("PartyAndPostalAddress")
                        .where(cond).orderBy("fromDate").filterByDate().queryFirst();
                if (postalMatch != null) {
                    postalContactId = postalMatch.getString("contactMechId");
                    List<GenericValue> postalPurposes = EntityQuery.use(delegator).from("PartyContactMechPurpose")
                            .where("partyId", partyId, "contactMechId", postalContactId)
                            .filterByDate().queryList();
                    List<Object> purposeStrings = EntityUtil.getFieldListFromEntityList(postalPurposes, "contactMechPurposeTypeId", false);
                    if (UtilValidate.isNotEmpty(purposeStrings) && purposeStrings.contains("SHIPPING_LOCATION")) {
                        needsShippingPurpose = false;
                    }
                    if (needsBillingPurpose && UtilValidate.isNotEmpty(purposeStrings) && purposeStrings.contains("BILLING_LOCATION")) {
                        needsBillingPurpose = false;
                    }
                }
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
            }
        }
        if (postalContactId == null) {
            postalMap.put("userLogin", cart.getUserLogin());
            postalMap.put("fromDate", UtilDateTime.nowTimestamp());
            try {
                outMap = dispatcher.runSync("createPartyPostalAddress", postalMap);
                postalContactId = (String) outMap.get("contactMechId");
            } catch (GenericServiceException e) {
                Debug.logError(e, module);
                return ServiceUtil.returnError(e.getMessage());
            }
        }
        if (needsShippingPurpose || needsBillingPurpose) {
            inMap.clear();
            inMap.put("userLogin", cart.getUserLogin());
            inMap.put("contactMechId", postalContactId);
            inMap.put("partyId", partyId);
            try {
                if (needsShippingPurpose) {
                    inMap.put("contactMechPurposeTypeId", "SHIPPING_LOCATION");
                    dispatcher.runSync("createPartyContactMechPurpose", inMap);
                }
                if (needsBillingPurpose) {
                    inMap.put("contactMechPurposeTypeId", "BILLING_LOCATION");
                    dispatcher.runSync("createPartyContactMechPurpose", inMap);
                }
            } catch (GenericServiceException e) {
                // Not the end of the world, we'll carry on
                Debug.logInfo(e.getMessage(), module);
            }
        }

        // Load the selected shipping method - thanks to PayPal's less than sane API all we've to work with is the shipping option label
        // that was shown to the customer
        String shipMethod = decoder.get("SHIPPINGOPTIONNAME");
        if ("Calculated Offline".equals(shipMethod)) {
            cart.setAllCarrierPartyId("_NA_");
            cart.setAllShipmentMethodTypeId("NO_SHIPPING");
        } else {
            String[] shipMethodSplit = shipMethod.split(" - ");
            cart.setAllCarrierPartyId(shipMethodSplit[0]);
            String shippingMethodTypeDesc = StringUtils.join(shipMethodSplit, " - ", 1, shipMethodSplit.length);
            try {
                GenericValue shipmentMethod = EntityQuery.use(delegator)
                        .from("ProductStoreShipmentMethView")
                        .where("productStoreId", cart.getProductStoreId(), 
                                "partyId", shipMethodSplit[0], 
                                "roleTypeId", "CARRIER", 
                                "description", shippingMethodTypeDesc)
                        .queryFirst();
                cart.setAllShipmentMethodTypeId(shipmentMethod.getString("shipmentMethodTypeId"));
            } catch (GenericEntityException e1) {
                Debug.logError(e1, module);
            }
        }
        //Get rid of any excess ship groups
        List<CartShipInfo> shipGroups = cart.getShipGroups();
        for (int i = 1; i < shipGroups.size(); i++) {
            Map<ShoppingCartItem, BigDecimal> items = cart.getShipGroupItems(i);
            for (Map.Entry<ShoppingCartItem, BigDecimal> entry : items.entrySet()) {
                cart.positionItemToGroup(entry.getKey(), entry.getValue(), i, 0, false);
            }
        }
        cart.cleanUpShipGroups();
        cart.setAllShippingContactMechId(postalContactId);
        Map<String, Object> result = ShippingEvents.getShipGroupEstimate(dispatcher, delegator, cart, 0);
        if (result.get(ModelService.RESPONSE_MESSAGE).equals(ModelService.RESPOND_ERROR)) {
            return ServiceUtil.returnError((String) result.get(ModelService.ERROR_MESSAGE));
        }

        BigDecimal shippingTotal = (BigDecimal) result.get("shippingTotal");
        if (shippingTotal == null) {
            shippingTotal = BigDecimal.ZERO;
        }
        cart.setItemShipGroupEstimate(shippingTotal, 0);
        CheckOutHelper cho = new CheckOutHelper(dispatcher, delegator, cart);
        try {
            cho.calcAndAddTax();
        } catch (GeneralException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        // Create the PayPal payment method
        inMap.clear();
        inMap.put("userLogin", cart.getUserLogin());
        inMap.put("partyId", partyId);
        inMap.put("contactMechId", postalContactId);
        inMap.put("fromDate", UtilDateTime.nowTimestamp());
        inMap.put("payerId", decoder.get("PAYERID"));
        inMap.put("expressCheckoutToken", token);
        inMap.put("payerStatus", decoder.get("PAYERSTATUS"));

        try {
            outMap = dispatcher.runSync("createPayPalPaymentMethod", inMap);
        } catch (GenericServiceException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }
        String paymentMethodId = (String) outMap.get("paymentMethodId");

        cart.clearPayments();
        BigDecimal maxAmount = cart.getGrandTotal().setScale(2, BigDecimal.ROUND_HALF_UP);
        cart.addPaymentAmount(paymentMethodId, maxAmount, true);

        return ServiceUtil.returnSuccess();

    }

    // Note we're not doing a lot of error checking here as this method is really only used
    // to confirm the order with PayPal, the subsequent authorizations will handle any errors
    // that may occur.
    public static Map<String, Object> doExpressCheckout(DispatchContext dctx, Map<String, Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        GenericValue paymentPref = (GenericValue) context.get("orderPaymentPreference");
        OrderReadHelper orh = new OrderReadHelper(delegator, paymentPref.getString("orderId"));
        Locale locale = (Locale) context.get("locale");

        GenericValue payPalPaymentSetting = getPaymentMethodGatewayPayPal(dctx, context, null);
        GenericValue payPalPaymentMethod = null;
        try {
            payPalPaymentMethod = paymentPref.getRelatedOne("PaymentMethod", false);
            payPalPaymentMethod = payPalPaymentMethod.getRelatedOne("PayPalPaymentMethod", false);
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }
        BigDecimal processAmount = paymentPref.getBigDecimal("maxAmount");

        NVPEncoder encoder = new NVPEncoder();
        encoder.add("METHOD", "DoExpressCheckoutPayment");
        encoder.add("TOKEN", payPalPaymentMethod.getString("expressCheckoutToken"));
        encoder.add("PAYMENTACTION", "Order");
        encoder.add("PAYERID", payPalPaymentMethod.getString("payerId"));
        // set the amount
        encoder.add("AMT", processAmount.setScale(2).toPlainString());
        encoder.add("CURRENCYCODE", orh.getCurrency());
        BigDecimal grandTotal = orh.getOrderGrandTotal();
        BigDecimal shippingTotal = orh.getShippingTotal().setScale(2, BigDecimal.ROUND_HALF_UP);
        BigDecimal taxTotal = orh.getTaxTotal().setScale(2, BigDecimal.ROUND_HALF_UP);
        BigDecimal subTotal = grandTotal.subtract(shippingTotal).subtract(taxTotal).setScale(2, BigDecimal.ROUND_HALF_UP);
        encoder.add("ITEMAMT", subTotal.toPlainString());
        encoder.add("SHIPPINGAMT", shippingTotal.toPlainString());
        encoder.add("TAXAMT", taxTotal.toPlainString());

        NVPDecoder decoder = null;
        try {
            decoder = sendNVPRequest(payPalPaymentSetting, encoder);
        } catch (PayPalException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }
        if (decoder == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                    "AccountingPayPalUnknownError", locale));
        }

        Map<String, String> errorMessages = getErrorMessageMap(decoder);
        if (UtilValidate.isNotEmpty(errorMessages)) {
            if (errorMessages.containsKey("10417")) {
                // "The transaction cannot complete successfully,  Instruct the customer to use an alternative payment method"
                // I've only encountered this once and there's no indication of the cause so the temporary solution is to try again
                boolean retry = context.get("_RETRY_") == null || (Boolean) context.get("_RETRY_");
                if (retry) {
                    context.put("_RETRY_", false);
                    return PayPalServices.doExpressCheckout(dctx, context);
                }
            }
            return ServiceUtil.returnError(UtilMisc.toList(errorMessages.values()));
        }

        Map<String, Object> inMap = new HashMap<String, Object>();
        inMap.put("userLogin", userLogin);
        inMap.put("paymentMethodId", payPalPaymentMethod.get("paymentMethodId"));
        inMap.put("transactionId", decoder.get("TRANSACTIONID"));

        Map<String, Object> outMap = null;
        try {
            outMap = dispatcher.runSync("updatePayPalPaymentMethod", inMap);
        } catch (GenericServiceException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }
        if (ServiceUtil.isError(outMap)) {
            Debug.logError(ServiceUtil.getErrorMessage(outMap), module);
            return outMap;
        }
        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> doAuthorization(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        String orderId = (String) context.get("orderId");
        BigDecimal processAmount = (BigDecimal) context.get("processAmount");
        GenericValue payPalPaymentMethod = (GenericValue) context.get("payPalPaymentMethod");
        OrderReadHelper orh = new OrderReadHelper(delegator, orderId);
        GenericValue payPalConfig = getPaymentMethodGatewayPayPal(dctx, context, PaymentGatewayServices.AUTH_SERVICE_TYPE);
        Locale locale = (Locale) context.get("locale");

        NVPEncoder encoder = new NVPEncoder();
        encoder.add("METHOD", "DoAuthorization");
        encoder.add("TRANSACTIONID", payPalPaymentMethod.getString("transactionId"));
        encoder.add("AMT", processAmount.setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString());
        encoder.add("TRANSACTIONENTITY", "Order");
        String currency = (String) context.get("currency");
        if (currency == null) {
            currency = orh.getCurrency();
        }
        encoder.add("CURRENCYCODE", currency);

        NVPDecoder decoder = null;
        try {
            decoder = sendNVPRequest(payPalConfig, encoder);
        } catch (PayPalException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        if (decoder == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                    "AccountingPayPalUnknownError", locale));
        }

        Map<String, Object> result = ServiceUtil.returnSuccess();
        Map<String, String> errors = getErrorMessageMap(decoder);
        if (UtilValidate.isNotEmpty(errors)) {
            result.put("authResult", false);
            result.put("authRefNum", "N/A");
            result.put("processAmount", BigDecimal.ZERO);
            if (errors.size() == 1) {
                Map.Entry<String, String> error = errors.entrySet().iterator().next();
                result.put("authCode", error.getKey());
                result.put("authMessage", error.getValue());
            } else {
                result.put("authMessage", "Multiple errors occurred, please refer to the gateway response messages");
                result.put("internalRespMsgs", errors);
            }
        } else {
            result.put("authResult", true);
            result.put("processAmount", new BigDecimal(decoder.get("AMT")));
            result.put("authRefNum", decoder.get("TRANSACTIONID"));
        }
        //TODO: Look into possible PAYMENTSTATUS and PENDINGREASON return codes, it is unclear what should be checked for this type of transaction
        return result;
    }

    public static Map<String, Object> doCapture(DispatchContext dctx, Map<String, Object> context) {
        GenericValue paymentPref = (GenericValue) context.get("orderPaymentPreference");
        BigDecimal captureAmount = (BigDecimal) context.get("captureAmount");
        GenericValue payPalConfig = getPaymentMethodGatewayPayPal(dctx, context, PaymentGatewayServices.AUTH_SERVICE_TYPE);
        GenericValue authTrans = (GenericValue) context.get("authTrans");
        Locale locale = (Locale) context.get("locale");
        if (authTrans == null) {
            authTrans = PaymentGatewayServices.getAuthTransaction(paymentPref);
        }

        NVPEncoder encoder = new NVPEncoder();
        encoder.add("METHOD", "DoCapture");
        encoder.add("AUTHORIZATIONID", authTrans.getString("referenceNum"));
        encoder.add("AMT", captureAmount.setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString());
        encoder.add("CURRENCYCODE", authTrans.getString("currencyUomId"));
        encoder.add("COMPLETETYPE", "NotComplete");

        NVPDecoder decoder = null;
        try {
            decoder = sendNVPRequest(payPalConfig, encoder);
        } catch (PayPalException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        if (decoder == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                    "AccountingPayPalUnknownError", locale));
        }

        Map<String, Object> result = ServiceUtil.returnSuccess();
        Map<String, String> errors = getErrorMessageMap(decoder);
        if (UtilValidate.isNotEmpty(errors)) {
            result.put("captureResult", false);
            result.put("captureRefNum", "N/A");
            result.put("captureAmount", BigDecimal.ZERO);
            if (errors.size() == 1) {
                Map.Entry<String, String> error = errors.entrySet().iterator().next();
                result.put("captureCode", error.getKey());
                result.put("captureMessage", error.getValue());
            } else {
                result.put("captureMessage", "Multiple errors occurred, please refer to the gateway response messages");
                result.put("internalRespMsgs", errors);
            }
        } else {
            result.put("captureResult", true);
            result.put("captureAmount", new BigDecimal(decoder.get("AMT")));
            result.put("captureRefNum", decoder.get("TRANSACTIONID"));
        }
        //TODO: Look into possible PAYMENTSTATUS and PENDINGREASON return codes, it is unclear what should be checked for this type of transaction
        return result;
    }

    public static Map<String, Object> doVoid(DispatchContext dctx, Map<String, Object> context) {
        GenericValue payPalConfig = getPaymentMethodGatewayPayPal(dctx, context, null);
        Locale locale = (Locale) context.get("locale");
        if (payPalConfig == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                    "AccountingPayPalPaymentGatewayConfigCannotFind", locale));
        }
        GenericValue orderPaymentPreference = (GenericValue) context.get("orderPaymentPreference");
        GenericValue authTrans = PaymentGatewayServices.getAuthTransaction(orderPaymentPreference);
        NVPEncoder encoder = new NVPEncoder();
        encoder.add("METHOD", "DoVoid");
        encoder.add("AUTHORIZATIONID", authTrans.getString("referenceNum"));
        NVPDecoder decoder = null;
        try {
            decoder = sendNVPRequest(payPalConfig, encoder);
        } catch (PayPalException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        if (decoder == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                    "AccountingPayPalUnknownError", locale));
        }

        Map<String, Object> result = ServiceUtil.returnSuccess();
        Map<String, String> errors = getErrorMessageMap(decoder);
        if (UtilValidate.isNotEmpty(errors)) {
            result.put("releaseResult", false);
            result.put("releaseRefNum", authTrans.getString("referenceNum"));
            result.put("releaseAmount", BigDecimal.ZERO);
            if (errors.size() == 1) {
                Map.Entry<String, String> error = errors.entrySet().iterator().next();
                result.put("releaseCode", error.getKey());
                result.put("releaseMessage", error.getValue());
            } else {
                result.put("releaseMessage", "Multiple errors occurred, please refer to the gateway response messages");
                result.put("internalRespMsgs", errors);
            }
        } else {
            result.put("releaseResult", true);
            // PayPal voids the entire order amount minus any captures, that's a little difficult to figure out here
            // so until further testing proves we should do otherwise I'm just going to return requested void amount
            result.put("releaseAmount", context.get("releaseAmount"));
            result.put("releaseRefNum", decoder.get("AUTHORIZATIONID"));
        }
        return result;
    }

    public static Map<String, Object> doRefund (DispatchContext dctx, Map<String, Object> context) {
        Locale locale = (Locale) context.get("locale");
        GenericValue payPalConfig = getPaymentMethodGatewayPayPal(dctx, context, null);
        if (payPalConfig == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                    "AccountingPayPalPaymentGatewayConfigCannotFind", locale));
        }
        GenericValue orderPaymentPreference = (GenericValue) context.get("orderPaymentPreference");
        GenericValue captureTrans = PaymentGatewayServices.getCaptureTransaction(orderPaymentPreference);
        BigDecimal refundAmount = (BigDecimal) context.get("refundAmount");
        NVPEncoder encoder = new NVPEncoder();
        encoder.add("METHOD", "RefundTransaction");
        encoder.add("TRANSACTIONID", captureTrans.getString("referenceNum"));
        encoder.add("REFUNDTYPE", "Partial");
        encoder.add("CURRENCYCODE", captureTrans.getString("currencyUomId"));
        encoder.add("AMT", refundAmount.setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString());
        encoder.add("NOTE", "Order #" + orderPaymentPreference.getString("orderId"));
        NVPDecoder decoder = null;
        try {
            decoder = sendNVPRequest(payPalConfig, encoder);
        } catch (PayPalException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        if (decoder == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                    "AccountingPayPalUnknownError", locale));
        }

        Map<String, Object> result = ServiceUtil.returnSuccess();
        Map<String, String> errors = getErrorMessageMap(decoder);
        if (UtilValidate.isNotEmpty(errors)) {
            result.put("refundResult", false);
            result.put("refundRefNum", captureTrans.getString("referenceNum"));
            result.put("refundAmount", BigDecimal.ZERO);
            if (errors.size() == 1) {
                Map.Entry<String, String> error = errors.entrySet().iterator().next();
                result.put("refundCode", error.getKey());
                result.put("refundMessage", error.getValue());
            } else {
                result.put("refundMessage", "Multiple errors occurred, please refer to the gateway response messages");
                result.put("internalRespMsgs", errors);
            }
        } else {
            result.put("refundResult", true);
            result.put("refundAmount", new BigDecimal(decoder.get("GROSSREFUNDAMT")));
            result.put("refundRefNum", decoder.get("REFUNDTRANSACTIONID"));
        }
        return result;
    }

    private static GenericValue getPaymentMethodGatewayPayPal(DispatchContext dctx, Map<String, ? extends Object> context, String paymentServiceTypeEnumId) {
        Delegator delegator = dctx.getDelegator();
        String paymentGatewayConfigId = (String) context.get("paymentGatewayConfigId");
        GenericValue payPalGatewayConfig = null;

        if (paymentGatewayConfigId == null) {
            String productStoreId = null;
            GenericValue orderPaymentPreference = (GenericValue) context.get("orderPaymentPreference");
            if (orderPaymentPreference != null) {
                OrderReadHelper orh = new OrderReadHelper(delegator, orderPaymentPreference.getString("orderId"));
                productStoreId = orh.getProductStoreId();
            } else {
                ShoppingCart cart = (ShoppingCart) context.get("cart");
                if (cart != null) {
                    productStoreId = cart.getProductStoreId();
                }
            }
            if (productStoreId != null) {
                GenericValue payPalPaymentSetting = ProductStoreWorker.getProductStorePaymentSetting(delegator, productStoreId, "EXT_PAYPAL", paymentServiceTypeEnumId, true);
                if (payPalPaymentSetting != null) {
                    paymentGatewayConfigId = payPalPaymentSetting.getString("paymentGatewayConfigId");
                }
            }
        }
        if (paymentGatewayConfigId != null) {
            try {
                payPalGatewayConfig = EntityQuery.use(delegator).from("PaymentGatewayPayPal").where("paymentGatewayConfigId", paymentGatewayConfigId).cache().queryOne();
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
            }
        }
        return payPalGatewayConfig;
    }

    private static NVPDecoder sendNVPRequest(GenericValue payPalConfig, NVPEncoder encoder) throws PayPalException {
        NVPCallerServices caller = new NVPCallerServices();
        try {
            APIProfile profile = ProfileFactory.createSignatureAPIProfile();
            profile.setAPIUsername(payPalConfig.getString("apiUserName"));
            profile.setAPIPassword(payPalConfig.getString("apiPassword"));
            profile.setSignature(payPalConfig.getString("apiSignature"));
            profile.setEnvironment(payPalConfig.getString("apiEnvironment"));
            caller.setAPIProfile(profile);
        } catch (PayPalException e) {
            Debug.logError(e.getMessage(), module);
        }

        String requestMessage = encoder.encode();
        String responseMessage = caller.call(requestMessage);

        NVPDecoder decoder = new NVPDecoder();
        decoder.decode(responseMessage);
        if (!"Success".equals(decoder.get("ACK"))) {
            Debug.logError("A response other than success was received from PayPal: " + responseMessage, module);
        }

        return decoder;
    }

    private static String getCountryGeoIdFromGeoCode(String geoCode, Delegator delegator) {
        String geoId = null;
        try {
            GenericValue countryGeo = EntityQuery.use(delegator).from("Geo")
                    .where("geoTypeId", "COUNTRY", "geoCode", geoCode).cache().queryFirst();
            if (countryGeo != null) {
                geoId = countryGeo.getString("geoId");
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }
        return geoId;
    }

    private static String parseStateProvinceGeoId(String payPalShipToState, String countryGeoId, Delegator delegator) {
        String lookupField = "geoName";
        List<EntityCondition> conditionList = new LinkedList<EntityCondition>();
        conditionList.add(EntityCondition.makeCondition("geoAssocTypeId", "REGIONS"));
        if ("USA".equals(countryGeoId) || "CAN".equals(countryGeoId)) {
            // PayPal returns two letter code for US and Canadian States/Provinces
            String geoTypeId = "USA".equals(countryGeoId) ? "STATE" : "PROVINCE";
            conditionList.add(EntityCondition.makeCondition("geoTypeId", geoTypeId));
            lookupField = "geoCode";
        }
        conditionList.add(EntityCondition.makeCondition("geoIdFrom", countryGeoId));
        conditionList.add(EntityCondition.makeCondition(lookupField, payPalShipToState));
        EntityCondition cond = EntityCondition.makeCondition(conditionList);
        GenericValue geoAssocAndGeoTo = null;
        try {
            geoAssocAndGeoTo = EntityQuery.use(delegator).from("GeoAssocAndGeoTo").where(cond).cache().queryFirst();
            
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }
        if (geoAssocAndGeoTo != null) {
            return geoAssocAndGeoTo.getString("geoId");
        }
        return null;
    }

    @SuppressWarnings("serial")
    public static class TokenWrapper implements Serializable {
        String theString;
        public TokenWrapper(String theString) {
            this.theString = theString;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null) return false;
            if (!(o instanceof TokenWrapper)) return false;
            TokenWrapper other = (TokenWrapper) o;
            return theString.equals(other.theString);
        }
        @Override
        public int hashCode() {
            return theString.hashCode();
        }
    }
}
