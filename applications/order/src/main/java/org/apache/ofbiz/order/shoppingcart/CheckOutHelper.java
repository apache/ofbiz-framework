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
package org.apache.ofbiz.order.shoppingcart;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.ObjectType;
import org.apache.ofbiz.base.util.StringUtil;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilFormatOut;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilNumber;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityExpr;
import org.apache.ofbiz.entity.condition.EntityFunction;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.EntityTypeUtil;
import org.apache.ofbiz.entity.util.EntityUtil;
import org.apache.ofbiz.entity.util.EntityUtilProperties;
import org.apache.ofbiz.order.finaccount.FinAccountHelper;
import org.apache.ofbiz.order.order.OrderChangeHelper;
import org.apache.ofbiz.order.order.OrderReadHelper;
import org.apache.ofbiz.order.shoppingcart.product.ProductPromoWorker;
import org.apache.ofbiz.order.shoppingcart.shipping.ShippingEvents;
import org.apache.ofbiz.order.thirdparty.paypal.ExpressCheckoutEvents;
import org.apache.ofbiz.party.contact.ContactHelper;
import org.apache.ofbiz.party.contact.ContactMechWorker;
import org.apache.ofbiz.product.store.ProductStoreWorker;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ModelService;
import org.apache.ofbiz.service.ServiceUtil;

/**
 * A facade over the ShoppingCart to simplify the relatively complex
 * processing required to create an order in the system.
 */
public class CheckOutHelper {

    private static final String MODULE = CheckOutHelper.class.getName();
    private static final String RES_ERROR = "OrderErrorUiLabels";

    private static final int DECIMALS = UtilNumber.getBigDecimalScale("order.decimals");
    private static final RoundingMode ROUNDING = UtilNumber.getRoundingMode("order.rounding");

    private LocalDispatcher dispatcher = null;
    private Delegator delegator = null;
    private ShoppingCart cart = null;

    public CheckOutHelper(LocalDispatcher dispatcher, Delegator delegator, ShoppingCart cart) {
        this.delegator = delegator;
        this.dispatcher = dispatcher;
        this.cart = cart;
    }

    /**
     * Sets check out shipping address.
     * @param shippingContactMechId the shipping contact mech id
     * @return the check out shipping address
     */
    public Map<String, Object> setCheckOutShippingAddress(String shippingContactMechId) {
        List<String> errorMessages = new ArrayList<>();
        Map<String, Object> result;
        String errMsg = null;

        if (UtilValidate.isNotEmpty(this.cart)) {
            errorMessages.addAll(setCheckOutShippingAddressInternal(shippingContactMechId));
        } else {
            errMsg = UtilProperties.getMessage(RES_ERROR, "checkhelper.no_items_in_cart", (cart != null ? cart.getLocale() : Locale.getDefault()));
            errorMessages.add(errMsg);
        }
        if (errorMessages.size() == 1) {
            result = ServiceUtil.returnError(errorMessages.get(0));
        } else if (!errorMessages.isEmpty()) {
            result = ServiceUtil.returnError(errorMessages);
        } else {
            result = ServiceUtil.returnSuccess();
        }

        return result;
    }

    private List<String> setCheckOutShippingAddressInternal(String shippingContactMechId) {
        List<String> errorMessages = new ArrayList<>();
        String errMsg = null;

        // set the shipping address
        if (UtilValidate.isNotEmpty(shippingContactMechId)) {
            this.cart.setAllShippingContactMechId(shippingContactMechId);
        } else if (cart.shippingApplies()) {
            // only return an error if shipping is required for this purchase
            errMsg = UtilProperties.getMessage(RES_ERROR, "checkhelper.select_shipping_destination", cart.getLocale());
            errorMessages.add(errMsg);
        }

        return errorMessages;
    }

    /**
     * Sets check out shipping options.
     * @param shippingMethod        the shipping method
     * @param shippingInstructions  the shipping instructions
     * @param orderAdditionalEmails the order additional emails
     * @param maySplit              the may split
     * @param giftMessage           the gift message
     * @param isGift                the is gift
     * @param internalCode          the internal code
     * @param shipBeforeDate        the ship before date
     * @param shipAfterDate         the ship after date
     * @return the check out shipping options
     */
    public Map<String, Object> setCheckOutShippingOptions(String shippingMethod, String shippingInstructions,
            String orderAdditionalEmails, String maySplit, String giftMessage, String isGift, String internalCode, String shipBeforeDate,
                                                          String shipAfterDate) {
        List<String> errorMessages = new ArrayList<>();
        Map<String, Object> result;
        String errMsg = null;

        if (UtilValidate.isNotEmpty(this.cart)) {
            errorMessages.addAll(setCheckOutShippingOptionsInternal(shippingMethod, shippingInstructions,
                    orderAdditionalEmails, maySplit, giftMessage, isGift, internalCode, shipBeforeDate, shipAfterDate));
        } else {
            errMsg = UtilProperties.getMessage(RES_ERROR, "checkhelper.no_items_in_cart", (cart != null ? cart.getLocale() : Locale.getDefault()));
            errorMessages.add(errMsg);
        }

        if (errorMessages.size() == 1) {
            result = ServiceUtil.returnError(errorMessages.get(0));
        } else if (!errorMessages.isEmpty()) {
            result = ServiceUtil.returnError(errorMessages);
        } else {
            result = ServiceUtil.returnSuccess();
        }

        return result;
    }

    private List<String> setCheckOutShippingOptionsInternal(String shippingMethod, String shippingInstructions, String orderAdditionalEmails,
            String maySplit, String giftMessage, String isGift, String internalCode, String shipBeforeDate, String shipAfterDate) {
        List<String> errorMessages = new ArrayList<>();
        String errMsg = null;

        // set the general shipping options
        if (UtilValidate.isNotEmpty(shippingMethod)) {
            int delimiterPos = shippingMethod.indexOf('@');
            String shipmentMethodTypeId = null;
            String carrierPartyId = null;

            if (delimiterPos > 0) {
                shipmentMethodTypeId = shippingMethod.substring(0, delimiterPos);
                carrierPartyId = shippingMethod.substring(delimiterPos + 1);
            }

            this.cart.setAllShipmentMethodTypeId(shipmentMethodTypeId);
            this.cart.setAllCarrierPartyId(carrierPartyId);
        } else if (cart.shippingApplies()) {
            // only return an error if shipping is required for this purchase
            errMsg = UtilProperties.getMessage(RES_ERROR, "checkhelper.select_shipping_method", cart.getLocale());
            errorMessages.add(errMsg);
        }

        // set the shipping instructions
        this.cart.setAllShippingInstructions(shippingInstructions);

        if (UtilValidate.isNotEmpty(maySplit)) {
            cart.setAllMaySplit(Boolean.valueOf(maySplit));
        } else {
            errMsg = UtilProperties.getMessage(RES_ERROR, "checkhelper.select_splitting_preference", cart.getLocale());
            errorMessages.add(errMsg);
        }

        // set the gift message
        this.cart.setAllGiftMessage(giftMessage);

        if (UtilValidate.isNotEmpty(isGift)) {
            cart.setAllIsGift(Boolean.valueOf(isGift));
        } else {
            errMsg = UtilProperties.getMessage(RES_ERROR, "checkhelper.specify_if_order_is_gift", cart.getLocale());
            errorMessages.add(errMsg);
        }

        // interal code
        this.cart.setInternalCode(internalCode);

        if (UtilValidate.isNotEmpty(shipBeforeDate)) {
            if (UtilValidate.isDate(shipBeforeDate)) {
                cart.setShipBeforeDate(UtilDateTime.toTimestamp(shipBeforeDate));
            } else {
                errMsg = UtilProperties.getMessage(RES_ERROR, "checkhelper.specify_if_shipBeforeDate_is_date", cart.getLocale());
                errorMessages.add(errMsg);
            }
        }

        if (UtilValidate.isNotEmpty(shipAfterDate)) {
            if (UtilValidate.isDate(shipAfterDate)) {
                cart.setShipAfterDate(UtilDateTime.toTimestamp(shipAfterDate));
            } else {
                errMsg = UtilProperties.getMessage(RES_ERROR, "checkhelper.specify_if_shipAfterDate_is_date", cart.getLocale());
                errorMessages.add(errMsg);
            }
        }

        // set any additional notification emails
        this.cart.setOrderAdditionalEmails(orderAdditionalEmails);

        return errorMessages;
    }

    /**
     * Sets check out payment.
     * @param selectedPaymentMethods the selected payment methods
     * @param singleUsePayments      the single use payments
     * @param billingAccountId       the billing account id
     * @return the check out payment
     */
    public Map<String, Object> setCheckOutPayment(Map<String, Map<String, Object>> selectedPaymentMethods, List<String> singleUsePayments,
                                                  String billingAccountId) {
        List<String> errorMessages = new ArrayList<>();
        Map<String, Object> result;
        String errMsg = null;

        if (UtilValidate.isNotEmpty(this.cart)) {
            errorMessages.addAll(setCheckOutPaymentInternal(selectedPaymentMethods, singleUsePayments, billingAccountId));
        } else {
            errMsg = UtilProperties.getMessage(RES_ERROR, "checkhelper.no_items_in_cart", (cart != null ? cart.getLocale() : Locale.getDefault()));
            errorMessages.add(errMsg);
        }

        if (errorMessages.size() == 1) {
            result = ServiceUtil.returnError(errorMessages.get(0));
        } else if (!errorMessages.isEmpty()) {
            result = ServiceUtil.returnError(errorMessages);
        } else {
            result = ServiceUtil.returnSuccess();
        }

        return result;
    }

    /**
     * Sets check out payment internal.
     * @param selectedPaymentMethods the selected payment methods
     * @param singleUsePayments      the single use payments
     * @param billingAccountId       the billing account id
     * @return the check out payment internal
     */
    public List<String> setCheckOutPaymentInternal(Map<String, Map<String, Object>> selectedPaymentMethods, List<String> singleUsePayments,
                                                   String billingAccountId) {
        List<String> errorMessages = new ArrayList<>();
        String errMsg = null;

        if (singleUsePayments == null) {
            singleUsePayments = new ArrayList<>();
        }

        // set the payment method option
        if (UtilValidate.isNotEmpty(selectedPaymentMethods)) {
            // clear out the old payments
            cart.clearPayments();

            if (UtilValidate.isNotEmpty(billingAccountId)) {
                Map<String, Object> billingAccountMap = selectedPaymentMethods.get("EXT_BILLACT");
                BigDecimal billingAccountAmt = (BigDecimal) billingAccountMap.get("amount");
                // set cart billing account data and generate a payment method containing the amount we will be charging
                cart.setBillingAccount(billingAccountId, (billingAccountAmt != null ? billingAccountAmt : BigDecimal.ZERO));
                // copy the billing account terms as order terms
                try {
                    List<GenericValue> billingAccountTerms = EntityQuery.use(delegator).from("BillingAccountTerm").where("billingAccountId",
                            billingAccountId).queryList();
                    if (UtilValidate.isNotEmpty(billingAccountTerms)) {
                        for (GenericValue billingAccountTerm : billingAccountTerms) {
                            // the term is not copied if in the cart a term of the same type is already set
                            if (!cart.hasOrderTerm(billingAccountTerm.getString("termTypeId"))) {
                                cart.addOrderTerm(billingAccountTerm.getString("termTypeId"), billingAccountTerm.getBigDecimal("termValue"),
                                        billingAccountTerm.getLong("termDays"));
                            }
                        }
                    }
                } catch (GenericEntityException gee) {
                    Debug.logWarning("Error copying billing account terms to order terms: " + gee.getMessage(), MODULE);
                }
            } else {
                // remove the billing account from the cart
                cart.setBillingAccount(null, BigDecimal.ZERO);
            }

            // if checkoutPaymentId == EXT_BILLACT, then we have billing account only, so make sure we have enough credit
            if (selectedPaymentMethods.containsKey("EXT_BILLACT") && selectedPaymentMethods.size() == 1) {
                BigDecimal accountCredit = this.availableAccountBalance(cart.getBillingAccountId());
                BigDecimal amountToUse = cart.getBillingAccountAmount();

                // if an amount was entered, check that it doesn't exceed available amount
                if (amountToUse.compareTo(BigDecimal.ZERO) > 0 && amountToUse.compareTo(accountCredit) > 0) {
                    errMsg = UtilProperties.getMessage(RES_ERROR, "checkhelper.insufficient_credit_available_on_account",
                            cart.getLocale());
                    errorMessages.add(errMsg);
                } else {
                    // otherwise use the available account credit (The user might enter 10.00 for an order worth 20.00 from an account with 30.00.
                    // This makes sure that the 30.00 is used)
                    amountToUse = accountCredit;
                }

                // check that the amount to use is enough to fulfill the order
                BigDecimal grandTotal = cart.getGrandTotal();
                if (grandTotal.compareTo(amountToUse) > 0) {
                    cart.setBillingAccount(null, BigDecimal.ZERO); // erase existing billing account data
                    errMsg = UtilProperties.getMessage(RES_ERROR, "checkhelper.insufficient_credit_available_on_account",
                            cart.getLocale());
                    errorMessages.add(errMsg);
                } else {
                    // since this is the only selected payment method, let's make this amount the grand total for convenience
                    amountToUse = grandTotal;
                }

                // associate the cart billing account amount and EXT_BILLACT selected payment method with whatever amount we have now
                // XXX: Note that this step is critical for the billing account to be charged correctly
                if (amountToUse.compareTo(BigDecimal.ZERO) > 0) {
                    cart.setBillingAccount(billingAccountId, amountToUse);
                    selectedPaymentMethods.put("EXT_BILLACT", UtilMisc.<String, Object>toMap("amount", amountToUse, "securityCode", null));
                }
            }

            for (String checkOutPaymentId : selectedPaymentMethods.keySet()) {
                String finAccountId = null;

                if (checkOutPaymentId.indexOf('|') > -1) {
                    // split type -- ID|Actual
                    String[] splitStr = checkOutPaymentId.split("\\|");
                    checkOutPaymentId = splitStr[0];
                    if ("FIN_ACCOUNT".equals(checkOutPaymentId)) {
                        finAccountId = splitStr[1];
                    }
                    if (Debug.verboseOn()) {
                        Debug.logVerbose("Split checkOutPaymentId: " + splitStr[0] + " / " + splitStr[1], MODULE);
                    }
                }

                // get the selected amount to use
                BigDecimal paymentAmount = null;
                String securityCode = null;
                String refNum = null;

                if (selectedPaymentMethods.get(checkOutPaymentId) != null) {
                    Map<String, Object> checkOutPaymentInfo = selectedPaymentMethods.get(checkOutPaymentId);
                    paymentAmount = (BigDecimal) checkOutPaymentInfo.get("amount");
                    securityCode = (String) checkOutPaymentInfo.get("securityCode");
                    refNum = (String) checkOutPaymentInfo.get("refNum");
                }

                boolean singleUse = singleUsePayments.contains(checkOutPaymentId);
                ShoppingCart.CartPaymentInfo inf = cart.addPaymentAmount(checkOutPaymentId, paymentAmount, singleUse);
                if (finAccountId != null) {
                    inf.setFinAccountId(finAccountId);
                }
                if (securityCode != null) {
                    inf.setSecurityCode(securityCode);
                }
                if (refNum != null) {
                    inf.setRefNum(refNum);
                }
            }
        } else if (cart.getGrandTotal().compareTo(BigDecimal.ZERO) != 0) {
            // only return an error if the order total is not 0.00
            errMsg = UtilProperties.getMessage(RES_ERROR, "checkhelper.select_method_of_payment",
                    cart.getLocale());
            errorMessages.add(errMsg);
        }

        return errorMessages;
    }

    /**
     * Sets check out dates.
     * @param shipBefore the ship before
     * @param shipAfter  the ship after
     * @return the check out dates
     */
    public Map<String, Object> setCheckOutDates(Timestamp shipBefore, Timestamp shipAfter) {
        List<String> errorMessages = new ArrayList<>();
        Map<String, Object> result = null;
        String errMsg = null;

        if (UtilValidate.isNotEmpty(this.cart)) {
            this.cart.setShipBeforeDate(shipBefore);
            this.cart.setShipAfterDate(shipAfter);
        } else {
            errMsg = UtilProperties.getMessage(RES_ERROR, "checkhelper.no_items_in_cart", (cart != null
                    ? cart.getLocale() : Locale.getDefault()));
            errorMessages.add(errMsg);
        }

        if (errorMessages.size() == 1) {
            result = ServiceUtil.returnError(errorMessages.get(0));
        } else if (!errorMessages.isEmpty()) {
            result = ServiceUtil.returnError(errorMessages);
        } else {
            result = ServiceUtil.returnSuccess();
        }
        return result;
    }


    /**
     * Sets check out options.
     * @param shippingMethod the shipping method
     * @param shippingContactMechId the shipping contact mech id
     * @param selectedPaymentMethods the selected payment methods
     * @param singleUsePayments the single use payments
     * @param billingAccountId the billing account id
     * @param shippingInstructions the shipping instructions
     * @param orderAdditionalEmails the order additional emails
     * @param maySplit the may split
     * @param giftMessage the gift message
     * @param isGift the is gift
     * @param internalCode the internal code
     * @param shipBeforeDate the ship before date
     * @param shipAfterDate the ship after date
     * @return the check out options
     */
    public Map<String, Object> setCheckOutOptions(String shippingMethod, String shippingContactMechId, Map<String,
            Map<String, Object>> selectedPaymentMethods, List<String> singleUsePayments, String billingAccountId, String shippingInstructions,
            String orderAdditionalEmails, String maySplit, String giftMessage, String isGift, String internalCode, String shipBeforeDate, String
            shipAfterDate) {
        List<String> errorMessages = new ArrayList<>();
        Map<String, Object> result = null;
        String errMsg = null;


        if (UtilValidate.isNotEmpty(this.cart)) {
            // set the general shipping options and method
            errorMessages.addAll(setCheckOutShippingOptionsInternal(shippingMethod, shippingInstructions,
                    orderAdditionalEmails, maySplit, giftMessage, isGift, internalCode, shipBeforeDate, shipAfterDate));

            // set the shipping address
            errorMessages.addAll(setCheckOutShippingAddressInternal(shippingContactMechId));

            // Recalc shipping costs before setting payment
            Map<String, Object> shipEstimateMap = ShippingEvents.getShipGroupEstimate(dispatcher, delegator, cart, 0);
            BigDecimal shippingTotal = (BigDecimal) shipEstimateMap.get("shippingTotal");
            if (shippingTotal == null) {
                shippingTotal = BigDecimal.ZERO;
            }
            cart.setItemShipGroupEstimate(shippingTotal, 0);
            ProductPromoWorker.doPromotions(cart, dispatcher);

            //Recalc tax before setting payment
            try {
                this.calcAndAddTax();
            } catch (GeneralException e) {
                Debug.logError(e, MODULE);
            }
            // set the payment method(s) option
            errorMessages.addAll(setCheckOutPaymentInternal(selectedPaymentMethods, singleUsePayments, billingAccountId));

        } else {
            errMsg = UtilProperties.getMessage(RES_ERROR, "checkhelper.no_items_in_cart", (cart != null ? cart.getLocale() : Locale.getDefault()));
            errorMessages.add(errMsg);
        }

        if (errorMessages.size() == 1) {
            result = ServiceUtil.returnError(errorMessages.get(0));
        } else if (!errorMessages.isEmpty()) {
            result = ServiceUtil.returnError(errorMessages);
        } else {
            result = ServiceUtil.returnSuccess();
        }

        return result;
    }

    /**
     * Check gift card map.
     * @param params                 the params
     * @param selectedPaymentMethods the selected payment methods
     * @return the map
     */
    public Map<String, Object> checkGiftCard(Map<String, Object> params, Map<String, Map<String, Object>> selectedPaymentMethods) {
        List<String> errorMessages = new ArrayList<>();
        Map<String, Object> errorMaps = new HashMap<>();
        Map<String, Object> result = new HashMap<>();
        String errMsg = null;
        // handle gift card payment
        if (params.get("addGiftCard") != null) {
            String gcNum = (String) params.get("giftCardNumber");
            String gcPin = (String) params.get("giftCardPin");
            String gcAmt = (String) params.get("giftCardAmount");
            BigDecimal gcAmount = BigDecimal.ONE.negate();

            boolean gcFieldsOkay = true;
            if (UtilValidate.isEmpty(gcNum)) {
                errMsg = UtilProperties.getMessage(RES_ERROR, "checkhelper.enter_gift_card_number", (cart != null ? cart.getLocale()
                        : Locale.getDefault()));
                errorMessages.add(errMsg);
                gcFieldsOkay = false;
            }
            if (cart.isPinRequiredForGC(delegator)) {
                //  if a PIN is required, make sure the PIN is valid
                if (UtilValidate.isEmpty(gcPin)) {
                    errMsg = UtilProperties.getMessage(RES_ERROR, "checkhelper.enter_gift_card_pin_number", cart.getLocale());
                    errorMessages.add(errMsg);
                    gcFieldsOkay = false;
                }
            }
            // See if we should validate gift card code against FinAccount's accountCode
            if (cart.isValidateGCFinAccount(delegator)) {
                try {
                    // No PIN required - validate gift card number against account code
                    if (!cart.isPinRequiredForGC(delegator)) {
                        GenericValue finAccount = FinAccountHelper.getFinAccountFromCode(gcNum, delegator);
                        if (finAccount == null) {
                            errMsg = UtilProperties.getMessage(RES_ERROR, "checkhelper.gift_card_does_not_exist", cart.getLocale());
                            errorMessages.add(errMsg);
                            gcFieldsOkay = false;
                        } else if ((finAccount.getBigDecimal("availableBalance") == null)
                                || !((finAccount.getBigDecimal("availableBalance")).compareTo(FinAccountHelper.getZero()) > 0)) {
                            // if account's available balance (including authorizations) is not greater than zero, then return an error
                            errMsg = UtilProperties.getMessage(RES_ERROR, "checkhelper.gift_card_has_no_value", cart.getLocale());
                            errorMessages.add(errMsg);
                            gcFieldsOkay = false;
                        }
                    }
                    // TODO: else case when pin is required - we should validate gcNum and gcPin
                } catch (GenericEntityException ex) {
                    errorMessages.add(ex.getMessage());
                    gcFieldsOkay = false;
                }
            }

            if (UtilValidate.isNotEmpty(selectedPaymentMethods)) {
                if (UtilValidate.isEmpty(gcAmt)) {
                    errMsg = UtilProperties.getMessage(RES_ERROR, "checkhelper.enter_amount_to_place_on_gift_card", cart.getLocale());
                    errorMessages.add(errMsg);
                    gcFieldsOkay = false;
                }
            }
            if (UtilValidate.isNotEmpty(gcAmt)) {
                try {
                    gcAmount = new BigDecimal(gcAmt);
                } catch (NumberFormatException e) {
                    Debug.logError(e, MODULE);
                    errMsg = UtilProperties.getMessage(RES_ERROR, "checkhelper.invalid_amount_for_gift_card", cart.getLocale());
                    errorMessages.add(errMsg);
                    gcFieldsOkay = false;
                }
            }

            if (gcFieldsOkay) {
                // store the gift card
                Map<String, Object> gcCtx = new HashMap<>();
                gcCtx.put("partyId", params.get("partyId"));
                gcCtx.put("cardNumber", gcNum);
                if (cart.isPinRequiredForGC(delegator)) {
                    gcCtx.put("pinNumber", gcPin);
                }
                gcCtx.put("userLogin", cart.getUserLogin());
                Map<String, Object> gcResult = null;
                try {
                    gcResult = dispatcher.runSync("createGiftCard", gcCtx);
                    if (ServiceUtil.isError(gcResult)) {
                        return ServiceUtil.returnError(ServiceUtil.getErrorMessage(gcResult));
                    }
                } catch (GenericServiceException e) {
                    Debug.logError(e, MODULE);
                    errorMessages.add(e.getMessage());
                }
                if (gcResult != null) {
                    ServiceUtil.addErrors(errorMessages, errorMaps, gcResult);

                    if (errorMessages.isEmpty() && errorMaps.isEmpty()) {
                        // set the GC payment method
                        BigDecimal giftCardAmount = null;
                        if (gcAmount.compareTo(BigDecimal.ZERO) > 0) {
                            giftCardAmount = gcAmount;
                        }
                        String gcPaymentMethodId = (String) gcResult.get("paymentMethodId");
                        result = ServiceUtil.returnSuccess();
                        result.put("paymentMethodId", gcPaymentMethodId);
                        result.put("amount", giftCardAmount);
                    }
                } else {
                    errMsg = UtilProperties.getMessage(RES_ERROR, "checkhelper.problem_with_gift_card_information", cart.getLocale());
                    errorMessages.add(errMsg);
                }
            }
        } else {
            result = ServiceUtil.returnSuccess();
        }

        // see whether we need to return an error or not
        if (!errorMessages.isEmpty()) {
            result.put(ModelService.ERROR_MESSAGE_LIST, errorMessages);
            result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_ERROR);
        }
        if (!errorMaps.isEmpty()) {
            result.put(ModelService.ERROR_MESSAGE_MAP, errorMaps);
            result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_ERROR);
        }

        return result;
    }

    /**
     * Create order map.
     * @param userLogin the user login
     * @return the map
     */
    public Map<String, Object> createOrder(GenericValue userLogin) {
        return createOrder(userLogin, null, null, null, false, null, cart.getWebSiteId());
    }

    /**
     * Create order event - uses createOrder service for processing
     * @param userLogin             the user login
     * @param distributorId         the distributor id
     * @param affiliateId           the affiliate id
     * @param trackingCodeOrders    the tracking code orders
     * @param areOrderItemsExploded the are order items exploded
     * @param visitId               the visit id
     * @param webSiteId             the web site id
     * @return the map
     */
    public Map<String, Object> createOrder(GenericValue userLogin, String distributorId, String affiliateId,
            List<GenericValue> trackingCodeOrders, boolean areOrderItemsExploded, String visitId, String webSiteId) {
        if (this.cart == null) {
            return null;
        }
        String orderId = this.cart.getOrderId();
        String supplierPartyId = (String) this.cart.getAttribute("supplierPartyId");
        String originOrderId = (String) this.cart.getAttribute("originOrderId");

        this.cart.clearAllItemStatus();
        this.cart.removeEmptyCartItems();

        BigDecimal grandTotal = this.cart.getGrandTotal();

        // store the order - build the context
        Map<String, Object> context = this.cart.makeCartMap(this.dispatcher, areOrderItemsExploded);

        //get the TrackingCodeOrder List
        context.put("trackingCodeOrders", trackingCodeOrders);

        if (distributorId != null) {
            context.put("distributorId", distributorId);
        }
        if (affiliateId != null) {
            context.put("affiliateId", affiliateId);
        }

        context.put("orderId", orderId);
        context.put("supplierPartyId", supplierPartyId);
        context.put("grandTotal", grandTotal);
        context.put("userLogin", userLogin);
        context.put("visitId", visitId);
        if (UtilValidate.isEmpty(webSiteId)) {
            webSiteId = cart.getWebSiteId();
        }
        context.put("webSiteId", webSiteId);
        context.put("originOrderId", originOrderId);
        context.put("agreementId", cart.getAgreementId());

        // need the partyId; don't use userLogin in case of an order via order mgr
        String partyId = this.cart.getPartyId();
        String productStoreId = cart.getProductStoreId();

        // store the order - invoke the service
        Map<String, Object> storeResult = null;

        try {
            storeResult = dispatcher.runSync("storeOrder", context);
            orderId = (String) storeResult.get("orderId");
            if (UtilValidate.isNotEmpty(orderId)) {
                this.cart.setOrderId(orderId);
                if (this.cart.getFirstAttemptOrderId() == null) {
                    this.cart.setFirstAttemptOrderId(orderId);
                }
            }
        } catch (GenericServiceException e) {
            String service = e.getMessage();
            Map<String, Object> messageMap = UtilMisc.<String, Object>toMap("service", service);
            String errMsg = UtilProperties.getMessage(RES_ERROR, "checkhelper.could_not_create_order_invoking_service", messageMap, cart.getLocale());
            Debug.logError(e, errMsg, MODULE);
            return ServiceUtil.returnError(errMsg);
        }

        // check for error message(s)
        if (ServiceUtil.isError(storeResult)) {
            String errMsg = UtilProperties.getMessage(RES_ERROR, "checkhelper.did_not_complete_order_following_occurred", cart.getLocale());
            List<String> resErrorMessages = new LinkedList<>();
            resErrorMessages.add(errMsg);
            resErrorMessages.add(ServiceUtil.getErrorMessage(storeResult));
            return ServiceUtil.returnError(resErrorMessages);
        }

        // ----------
        // If needed, the production runs are created and linked to the order lines.
        //
        List<GenericValue> orderItems = UtilGenerics.cast(context.get("orderItems"));
        int counter = 0;
        for (GenericValue orderItem : orderItems) {
            String productId = orderItem.getString("productId");
            if (productId != null) {
                try {
                    // do something tricky here: run as the "system" user
                    // that can actually create and run a production run
                    GenericValue permUserLogin = EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "system").cache().queryOne();
                    GenericValue productStore = ProductStoreWorker.getProductStore(productStoreId, delegator);
                    GenericValue product = EntityQuery.use(delegator).from("Product").where("productId", productId).queryOne();
                    if (EntityTypeUtil.hasParentType(delegator, "ProductType", "productTypeId", product.getString("productTypeId"), "parentTypeId",
                            "AGGREGATED")) {
                        org.apache.ofbiz.product.config.ProductConfigWrapper config = this.cart.findCartItem(counter).getConfigWrapper();
                        Map<String, Object> inputMap = new HashMap<>();
                        inputMap.put("config", config);
                        inputMap.put("facilityId", productStore.getString("inventoryFacilityId"));
                        inputMap.put("orderId", orderId);
                        inputMap.put("orderItemSeqId", orderItem.getString("orderItemSeqId"));
                        inputMap.put("quantity", orderItem.getBigDecimal("quantity"));
                        inputMap.put("userLogin", permUserLogin);

                        Map<String, Object> prunResult = dispatcher.runSync("createProductionRunFromConfiguration", inputMap);
                        if (ServiceUtil.isError(prunResult)) {
                            Debug.logError(ServiceUtil.getErrorMessage(prunResult) + " for input:" + inputMap, MODULE);
                        }
                    }
                } catch (GenericEntityException e) {
                    String service = e.getMessage();
                    Map<String, String> messageMap = UtilMisc.toMap("service", service);
                    String errMsg = UtilProperties.getMessage(RES_ERROR, "checkhelper.problems_reading_database", cart.getLocale());
                    errMsg += UtilProperties.getMessage(RES_ERROR, "checkhelper.could_not_create_order_invoking_service", messageMap,
                            cart.getLocale());
                    Debug.logError(e, errMsg, MODULE);
                    return ServiceUtil.returnError(errMsg);
                } catch (GenericServiceException e) {
                    String service = e.getMessage();
                    Map<String, String> messageMap = UtilMisc.toMap("service", service);
                    String errMsg = UtilProperties.getMessage(RES_ERROR, "checkhelper.could_not_create_order_invoking_service", messageMap,
                            cart.getLocale());
                    Debug.logError(e, errMsg, MODULE);
                    return ServiceUtil.returnError(errMsg);
                }
            }
            counter++;
        }
        // The status of the requirement associated to the shopping cart lines is set to "ordered".
        for (ShoppingCartItem shoppingCartItem : this.cart.items()) {
            String requirementId = shoppingCartItem.getRequirementId();
            if (requirementId != null) {
                try {
                    /* OrderRequirementCommitment records will map which POs which are created from which requirements.
                    With the help of this mapping requirements will be updated to Ordered when POs will be approved.  */
                    Map<String, Object> inputMap = UtilMisc.toMap("userLogin", userLogin, "orderId", orderId, "orderItemSeqId",
                            shoppingCartItem.getOrderItemSeqId(), "requirementId", requirementId, "quantity", shoppingCartItem.getQuantity());
                    Map<String, Object> serviceResult = dispatcher.runSync("createOrderRequirementCommitment", inputMap);
                    if (ServiceUtil.isError(serviceResult)) {
                        return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                    }
                } catch (GenericServiceException e) {
                    String service = e.getMessage();
                    Map<String, String> messageMap = UtilMisc.toMap("service", service);
                    String errMsg = UtilProperties.getMessage(RES_ERROR, "checkhelper.could_not_create_order_invoking_service", messageMap,
                            cart.getLocale());
                    Debug.logError(e, errMsg, MODULE);
                    return ServiceUtil.returnError(errMsg);
                }
            }
        }
        // ----------

        // set the orderId for use by chained events
        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put("orderId", orderId);
        result.put("orderAdditionalEmails", this.cart.getOrderAdditionalEmails());

        // save the emails to the order
        List<GenericValue> toBeStored = new LinkedList<>();

        GenericValue party = null;
        try {
            party = EntityQuery.use(delegator).from("Party").where("partyId", partyId).queryOne();
        } catch (GenericEntityException e) {
            Debug.logWarning(e, UtilProperties.getMessage(RES_ERROR, "OrderProblemsGettingPartyRecord", cart.getLocale()), MODULE);
        }

        // create order contact mechs for the email address(s)
        if (party != null) {
            Iterator<GenericValue> emailIter = UtilMisc.toIterator(ContactHelper.getContactMechByType(party, "EMAIL_ADDRESS", false));
            while (emailIter != null && emailIter.hasNext()) {
                GenericValue email = emailIter.next();
                GenericValue orderContactMech = this.delegator.makeValue("OrderContactMech",
                        UtilMisc.toMap("orderId", orderId, "contactMechId", email.getString("contactMechId"), "contactMechPurposeTypeId",
                                "ORDER_EMAIL"));
                toBeStored.add(orderContactMech);
                if (UtilValidate.isEmpty(ContactHelper.getContactMechByPurpose(party, "ORDER_EMAIL", false))) {
                    GenericValue partyContactMechPurpose = this.delegator.makeValue("PartyContactMechPurpose",
                            UtilMisc.toMap("partyId", party.getString("partyId"), "contactMechId", email.getString("contactMechId"),
                                    "contactMechPurposeTypeId", "ORDER_EMAIL", "fromDate", UtilDateTime.nowTimestamp()));
                    toBeStored.add(partyContactMechPurpose);
                }
            }
        }

        // create dummy contact mechs and order contact mechs for the additional emails
        String additionalEmails = this.cart.getOrderAdditionalEmails();
        List<String> emailList = StringUtil.split(additionalEmails, ",");
        if (emailList == null) {
            emailList = new ArrayList<>();
        }
        for (String email : emailList) {
            String contactMechId = this.delegator.getNextSeqId("ContactMech");
            GenericValue contactMech = this.delegator.makeValue("ContactMech",
                    UtilMisc.toMap("contactMechId", contactMechId, "contactMechTypeId", "EMAIL_ADDRESS", "infoString", email));

            GenericValue orderContactMech = this.delegator.makeValue("OrderContactMech",
                    UtilMisc.toMap("orderId", orderId, "contactMechId", contactMechId, "contactMechPurposeTypeId", "ORDER_EMAIL"));
            toBeStored.add(contactMech);
            toBeStored.add(orderContactMech);
        }

        if (!toBeStored.isEmpty()) {
            try {
                if (Debug.verboseOn()) {
                    Debug.logVerbose("To Be Stored: " + toBeStored, MODULE);
                }
                this.delegator.storeAll(toBeStored);
            } catch (GenericEntityException e) {
                // not a fatal error; so just print a message
                Debug.logWarning(e, UtilProperties.getMessage(RES_ERROR, "OrderProblemsStoringOrderEmailContactInformation", cart.getLocale()),
                        MODULE);
            }
        }

        return result;
    }

    /**
     * Calc and add tax.
     * @throws GeneralException the general exception
     */
    public void calcAndAddTax() throws GeneralException {
        calcAndAddTax(null, false);
    }

    /**
     * Calc and add tax.
     * @param skipEmptyAddresses the skip empty addresses
     * @throws GeneralException the general exception
     */
    public void calcAndAddTax(boolean skipEmptyAddresses) throws GeneralException {
        calcAndAddTax(null, skipEmptyAddresses);
    }

    /**
     * Calc and add tax.
     * @param shipAddress the ship address
     * @throws GeneralException the general exception
     */
    public void calcAndAddTax(GenericValue shipAddress) throws GeneralException {
        calcAndAddTax(shipAddress, false);
    }

    /**
     * Calc and add tax.
     * @param shipAddress        the ship address
     * @param skipEmptyAddresses the skip empty addresses
     * @throws GeneralException the general exception
     */
    public void calcAndAddTax(GenericValue shipAddress, boolean skipEmptyAddresses) throws GeneralException {
        if (UtilValidate.isEmpty(cart.getShippingContactMechId()) && cart.getBillingAddress() == null && shipAddress == null) {
            return;
        }

        int shipGroups = this.cart.getShipGroupSize();
        for (int i = 0; i < shipGroups; i++) {
            ShoppingCart.CartShipInfo csi = cart.getShipInfo(i);
            Map<Integer, ShoppingCartItem> shoppingCartItemIndexMap = new HashMap<>();
            Map<String, Object> serviceContext = this.makeTaxContext(i, shipAddress, shoppingCartItemIndexMap, cart.getFacilityId(),
                    skipEmptyAddresses);
            if (skipEmptyAddresses && serviceContext == null) {
                csi.clearAllTaxInfo();
                continue;
            }
            List<List<? extends Object>> taxReturn = getTaxAdjustments(dispatcher, "calcTax", serviceContext);

            if (Debug.verboseOn()) {
                Debug.logVerbose("ReturnList: " + taxReturn, MODULE);
            }
            List<GenericValue> orderAdj = UtilGenerics.cast(taxReturn.get(0));
            List<List<GenericValue>> itemAdj = UtilGenerics.cast(taxReturn.get(1));

            // set the item adjustments
            if (itemAdj != null) {
                for (int x = 0; x < itemAdj.size(); x++) {
                    List<GenericValue> adjs = itemAdj.get(x);
                    ShoppingCartItem item = shoppingCartItemIndexMap.get(x);
                    if (adjs == null) {
                        adjs = new LinkedList<>();
                    }
                    csi.setItemInfo(item, adjs);
                    if (Debug.verboseOn()) {
                        Debug.logVerbose("Added item adjustments to ship group [" + i + " / " + x + "] - " + adjs, MODULE);
                    }
                }
            }

            // need to manually clear the order adjustments
            csi.clearShipTaxAdj();
            csi.addShipTaxAdj(orderAdj);
        }
    }

    private Map<String, Object> makeTaxContext(int shipGroup, GenericValue shipAddress, Map<Integer, ShoppingCartItem> shoppingCartItemIndexMap,
                                               String originFacilityId, boolean skipEmptyAddresses) {
        ShoppingCart.CartShipInfo csi = cart.getShipInfo(shipGroup);
        int totalItems = csi.getShipItemInfo().size();

        List<GenericValue> product = new ArrayList<>(totalItems);
        List<BigDecimal> amount = new ArrayList<>(totalItems);
        List<BigDecimal> price = new ArrayList<>(totalItems);
        List<BigDecimal> quantity = new ArrayList<>(totalItems);
        List<BigDecimal> shipAmt = new ArrayList<>(totalItems);

        Iterator<ShoppingCartItem> it = csi.getShipItemInfo().keySet().iterator();
        for (int i = 0; i < totalItems; i++) {
            ShoppingCartItem cartItem = it.next();
            ShoppingCart.CartShipInfo.CartShipItemInfo itemInfo = csi.getShipItemInfo(cartItem);
            product.add(i, cartItem.getProduct());
            amount.add(i, cartItem.getItemSubTotal(itemInfo.getItemQuantity()));
            price.add(i, cartItem.getBasePrice());
            quantity.add(i, cartItem.getQuantity());
            shipAmt.add(i, BigDecimal.ZERO); // no per item shipping yet
            shoppingCartItemIndexMap.put(i, cartItem);
        }

        //add promotion adjustments
        List<GenericValue> allAdjustments = cart.getAdjustments();
        BigDecimal orderPromoAmt = OrderReadHelper.calcOrderPromoAdjustmentsBd(allAdjustments);

        BigDecimal shipAmount = csi.getShipEstimate();
        if (shipAddress == null) {
            shipAddress = cart.getShippingAddress(shipGroup);
        }

        if (shipAddress == null && skipEmptyAddresses) {
            return null;
        }

        // no shipping address; try the billing address
        if (shipAddress == null) {
            for (int i = 0; i < cart.selectedPayments(); i++) {
                ShoppingCart.CartPaymentInfo cpi = cart.getPaymentInfo(i);
                GenericValue billAddr = cpi.getBillingAddress(delegator);
                if (billAddr != null) {
                    shipAddress = billAddr;
                    Debug.logInfo("In makeTaxContext no shipping address, but found address with ID [" + shipAddress.get("contactMechId")
                            + "] from payment method.", MODULE);
                    break;
                }
            }
        }

        if (shipAddress == null) {
            // face-to-face order; use the facility address
            if (originFacilityId != null) {
                GenericValue facilityContactMech = ContactMechWorker.getFacilityContactMechByPurpose(delegator, originFacilityId,
                        UtilMisc.toList("SHIP_ORIG_LOCATION", "PRIMARY_LOCATION"));
                if (facilityContactMech != null) {
                    try {
                        shipAddress = EntityQuery.use(delegator).from("PostalAddress").where("contactMechId",
                                facilityContactMech.getString("contactMechId")).queryOne();
                    } catch (GenericEntityException e) {
                        Debug.logError(e, MODULE);
                    }
                }
            }
        }

        // if shippingAddress is still null then don't calculate tax; it may be an situation where no tax is applicable, or the data is bad and we
        // don't have a way to find an address to check tax for
        if (shipAddress == null) {
            Debug.logWarning("Not calculating tax for new order because there is no shipping address, no billing address, and no address on the"
                    + "origin facility [" + originFacilityId + "]", MODULE);
        }

        Map<String, Object> serviceContext = UtilMisc.<String, Object>toMap("productStoreId", cart.getProductStoreId());
        serviceContext.put("payToPartyId", cart.getBillFromVendorPartyId());
        serviceContext.put("billToPartyId", cart.getBillToCustomerPartyId());
        serviceContext.put("itemProductList", product);
        serviceContext.put("itemAmountList", amount);
        serviceContext.put("itemPriceList", price);
        serviceContext.put("itemQuantityList", quantity);
        serviceContext.put("itemShippingList", shipAmt);
        serviceContext.put("orderShippingAmount", shipAmount);
        serviceContext.put("shippingAddress", shipAddress);
        serviceContext.put("orderPromotionsAmount", orderPromoAmt);

        return serviceContext;
    }

    // Calc the tax adjustments.
    private static List<List<? extends Object>> getTaxAdjustments(LocalDispatcher dispatcher, String taxService,
            Map<String, Object> serviceContext) throws GeneralException {
        Map<String, Object> serviceResult = null;

        try {
            serviceResult = dispatcher.runSync(taxService, serviceContext);
            if (ServiceUtil.isError(serviceResult)) {
                String errorMessage = ServiceUtil.getErrorMessage(serviceResult);
                Debug.logError(errorMessage, MODULE);
                throw new GeneralException(errorMessage);
            }
        } catch (GenericServiceException e) {
            Debug.logError(e, MODULE);
            throw new GeneralException("Problem occurred in tax service (" + e.getMessage() + ")", e);
        }
        // the adjustments (returned in order) from taxware.
        List<GenericValue> orderAdj = UtilGenerics.cast(serviceResult.get("orderAdjustments"));
        List<List<GenericValue>> itemAdj = UtilGenerics.cast(serviceResult.get("itemAdjustments"));

        return UtilMisc.toList(orderAdj, itemAdj);
    }

    /**
     * Process payment map.
     * @param productStore the product store
     * @param userLogin    the user login
     * @return the map
     * @throws GeneralException the general exception
     */
    public Map<String, Object> processPayment(GenericValue productStore, GenericValue userLogin) throws GeneralException {
        return CheckOutHelper.processPayment(this.cart.getOrderId(), this.cart.getGrandTotal(), this.cart.getCurrency(), productStore, userLogin,
                false, false, dispatcher, delegator);
    }

    /**
     * Process payment map.
     * @param productStore the product store
     * @param userLogin    the user login
     * @param faceToFace   the face to face
     * @return the map
     * @throws GeneralException the general exception
     */
    public Map<String, Object> processPayment(GenericValue productStore, GenericValue userLogin, boolean faceToFace) throws GeneralException {
        return CheckOutHelper.processPayment(this.cart.getOrderId(), this.cart.getGrandTotal(), this.cart.getCurrency(), productStore, userLogin,
                faceToFace, false, dispatcher, delegator);
    }

    /**
     * Process payment map.
     * @param productStore the product store
     * @param userLogin    the user login
     * @param faceToFace   the face to face
     * @param manualHold   the manual hold
     * @return the map
     * @throws GeneralException the general exception
     */
    public Map<String, Object> processPayment(GenericValue productStore, GenericValue userLogin, boolean faceToFace, boolean manualHold)
            throws GeneralException {
        return CheckOutHelper.processPayment(this.cart.getOrderId(), this.cart.getGrandTotal(), this.cart.getCurrency(), productStore, userLogin,
                faceToFace, manualHold, dispatcher, delegator);
    }

    public static Map<String, Object> processPayment(String orderId, BigDecimal orderTotal, String currencyUomId, GenericValue productStore,
                GenericValue userLogin, boolean faceToFace, boolean manualHold, LocalDispatcher dispatcher, Delegator delegator)
                throws GeneralException {
        // Get some payment related strings
        String declineMessage = productStore.getString("authDeclinedMessage");
        String errMessage = productStore.getString("authErrorMessage");
        String retryOnError = productStore.getString("retryFailedAuths");
        if (retryOnError == null) {
            retryOnError = "Y";
        }

        List<GenericValue> allPaymentPreferences = null;
        try {
            allPaymentPreferences = EntityQuery.use(delegator).from("OrderPaymentPreference").where("orderId", orderId).queryList();
        } catch (GenericEntityException e) {
            throw new GeneralException("Problems getting payment preferences", e);
        }

        // filter out cancelled preferences
        List<EntityExpr> canExpr = UtilMisc.toList(EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "PAYMENT_CANCELLED"));
        allPaymentPreferences = EntityUtil.filterByAnd(allPaymentPreferences, canExpr);

        // check for online payment methods or in-hand payment types with verbal or external refs
        List<EntityExpr> exprs = UtilMisc.toList(EntityCondition.makeCondition("manualRefNum", EntityOperator.NOT_EQUAL, null));
        List<GenericValue> manualRefPaymentPrefs = EntityUtil.filterByAnd(allPaymentPreferences, exprs);
        if (UtilValidate.isNotEmpty(manualRefPaymentPrefs)) {
            for (GenericValue opp : manualRefPaymentPrefs) {
                Map<String, Object> authCtx = new HashMap<>();
                authCtx.put("orderPaymentPreference", opp);
                if (opp.get("paymentMethodId") == null) {
                    authCtx.put("serviceTypeEnum", "PRDS_PAY_EXTERNAL");
                }
                authCtx.put("processAmount", opp.getBigDecimal("maxAmount"));
                authCtx.put("authRefNum", opp.getString("manualRefNum"));
                authCtx.put("authResult", Boolean.TRUE);
                authCtx.put("userLogin", userLogin);
                authCtx.put("currencyUomId", currencyUomId);

                Map<String, Object> authResp = dispatcher.runSync("processAuthResult", authCtx);
                if (ServiceUtil.isError(authResp)) {
                    String errorMessage = ServiceUtil.getErrorMessage(authResp);
                    Debug.logError(errorMessage, MODULE);
                    throw new GeneralException(errorMessage);
                }

                // approve the order
                OrderChangeHelper.approveOrder(dispatcher, userLogin, orderId, manualHold);

                if ("Y".equalsIgnoreCase(productStore.getString("manualAuthIsCapture"))) {
                    Map<String, Object> captCtx = new HashMap<>();
                    captCtx.put("orderPaymentPreference", opp);
                    if (opp.get("paymentMethodId") == null) {
                        captCtx.put("serviceTypeEnum", "PRDS_PAY_EXTERNAL");
                    }
                    captCtx.put("payToPartyId", productStore.get("payToPartyId"));
                    captCtx.put("captureResult", Boolean.TRUE);
                    captCtx.put("captureAmount", opp.getBigDecimal("maxAmount"));
                    captCtx.put("captureRefNum", opp.getString("manualRefNum"));
                    captCtx.put("userLogin", userLogin);
                    captCtx.put("currencyUomId", currencyUomId);

                    Map<String, Object> capResp = dispatcher.runSync("processCaptureResult", captCtx);
                    if (ServiceUtil.isError(capResp)) {
                        String errorMessage = ServiceUtil.getErrorMessage(capResp);
                        Debug.logError(errorMessage, MODULE);
                        throw new GeneralException(errorMessage);
                    }
                }
            }
        }

        // check for a paypal express checkout needing completion
        List<EntityExpr> payPalExprs = UtilMisc.toList(
                EntityCondition.makeCondition("paymentMethodId", EntityOperator.NOT_EQUAL, null),
                EntityCondition.makeCondition("paymentMethodTypeId", "EXT_PAYPAL"));
        List<GenericValue> payPalPaymentPrefs = EntityUtil.filterByAnd(allPaymentPreferences, payPalExprs);
        if (UtilValidate.isNotEmpty(payPalPaymentPrefs)) {
            GenericValue payPalPaymentPref = EntityUtil.getFirst(payPalPaymentPrefs);
            ExpressCheckoutEvents.doExpressCheckout(productStore.getString("productStoreId"), orderId, payPalPaymentPref, userLogin,
                    delegator, dispatcher);
        }

        // check for online payment methods needing authorization
        Map<String, Object> paymentFields = UtilMisc.<String, Object>toMap("statusId", "PAYMENT_NOT_AUTH");
        List<GenericValue> onlinePaymentPrefs = EntityUtil.filterByAnd(allPaymentPreferences, paymentFields);

        // Check the payment preferences; if we have ANY w/ status PAYMENT_NOT_AUTH invoke payment service.
        // Invoke payment processing.
        if (UtilValidate.isNotEmpty(onlinePaymentPrefs)) {
            boolean autoApproveOrder = UtilValidate.isEmpty(productStore.get("autoApproveOrder")) || "Y".equalsIgnoreCase(productStore
                    .getString("autoApproveOrder"));
            if (orderTotal.compareTo(BigDecimal.ZERO) == 0 && autoApproveOrder) {
                // if there is nothing to authorize; don't bother
                boolean ok = OrderChangeHelper.approveOrder(dispatcher, userLogin, orderId, manualHold);
                if (!ok) {
                    throw new GeneralException("Problem with order change; see above error");
                }
            }

            // now there should be something to authorize; go ahead
            Map<String, Object> paymentResult = null;
            try {
                // invoke the payment gateway service.
                paymentResult = dispatcher.runSync("authOrderPayments",
                        UtilMisc.<String, Object>toMap("orderId", orderId, "userLogin", userLogin), 180, false);
                if (ServiceUtil.isError(paymentResult)) {
                    String errorMessage = ServiceUtil.getErrorMessage(paymentResult);
                    Debug.logError(errorMessage, MODULE);
                    throw new GeneralException(errorMessage);
                }
            } catch (GenericServiceException e) {
                Debug.logWarning(e, MODULE);
                throw new GeneralException("Error in authOrderPayments service: " + e.toString(), e.getNested());
            }
            if (Debug.verboseOn()) {
                Debug.logVerbose("Finished w/ Payment Service", MODULE);
            }
            if (paymentResult != null && paymentResult.containsKey("processResult")) {
                // grab the customer messages -- only passed back in the case of an error or failure
                List<String> messages = UtilGenerics.cast(paymentResult.get("authResultMsgs"));

                String authResp = (String) paymentResult.get("processResult");

                if ("FAILED".equals(authResp)) {
                    // order was NOT approved
                    if (Debug.verboseOn()) {
                        Debug.logVerbose("Payment auth was NOT a success!", MODULE);
                    }

                    boolean ok = OrderChangeHelper.rejectOrder(dispatcher, userLogin, orderId);
                    if (!ok) {
                        throw new GeneralException("Problem with order change; see above error");
                    }
                    if (UtilValidate.isEmpty(messages)) {
                        return ServiceUtil.returnError(declineMessage);
                    }
                    return ServiceUtil.returnError(messages);
                } else if ("APPROVED".equals(authResp)) {
                    // order WAS approved
                    if (Debug.verboseOn()) {
                        Debug.logVerbose("Payment auth was a success!", MODULE);
                    }

                    // set the order and item status to approved
                    if (autoApproveOrder) {
                        List<GenericValue> productStorePaymentSettingList = EntityQuery.use(delegator).from("ProductStorePaymentSetting")
                                .where("productStoreId", productStore.getString("productStoreId"), "paymentMethodTypeId", "CREDIT_CARD",
                                        "paymentService", "cyberSourceCCAuth").queryList();
                        if (!productStorePaymentSettingList.isEmpty()) {
                            String decision = (String) paymentResult.get("authCode");
                            if (UtilValidate.isNotEmpty(decision)) {
                                if ("ACCEPT".equalsIgnoreCase(decision)) {
                                    boolean ok = OrderChangeHelper.approveOrder(dispatcher, userLogin, orderId, manualHold);
                                    if (!ok) {
                                        throw new GeneralException("Problem with order change; see above error");
                                    }
                                }
                            } else {
                                boolean ok = OrderChangeHelper.approveOrder(dispatcher, userLogin, orderId, manualHold);
                                if (!ok) {
                                    throw new GeneralException("Problem with order change; see above error");
                                }
                            }
                        } else {
                            boolean ok = OrderChangeHelper.approveOrder(dispatcher, userLogin, orderId, manualHold);
                            if (!ok) {
                                throw new GeneralException("Problem with order change; see above error");
                            }
                        }
                    }
                } else if ("ERROR".equals(authResp)) {
                    // service failed
                    if (Debug.verboseOn()) {
                        Debug.logVerbose("Payment auth failed due to processor trouble.", MODULE);
                    }
                    if (!faceToFace && "Y".equalsIgnoreCase(retryOnError)) {
                        // never do this for a face to face purchase regardless of store setting
                        return ServiceUtil.returnSuccess(errMessage);
                    }
                    boolean ok = OrderChangeHelper.cancelOrder(dispatcher, userLogin, orderId);
                    if (!ok) {
                        throw new GeneralException("Problem with order change; see above error");
                    }
                    if (UtilValidate.isEmpty(messages)) {
                        return ServiceUtil.returnError(errMessage);
                    }
                    return ServiceUtil.returnError(messages);
                } else {
                    // should never happen
                    return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, "OrderPleaseContactCustomerServicePaymentReturnCodeUnknown",
                            Locale.getDefault()));
                }
            } else {
                // result returned null == service failed
                if (Debug.verboseOn()) {
                    Debug.logVerbose("Payment auth failed due to processor trouble.", MODULE);
                }
                if (!faceToFace && "Y".equalsIgnoreCase(retryOnError)) {
                    // never do this for a face to face purchase regardless of store setting
                    return ServiceUtil.returnSuccess(errMessage);
                }
                boolean ok = OrderChangeHelper.cancelOrder(dispatcher, userLogin, orderId);
                if (!ok) {
                    throw new GeneralException("Problem with order change; see above error");
                }
                return ServiceUtil.returnError(errMessage);
            }
        } else {
            // Get the paymentMethodTypeIds - this will need to change when ecom supports multiple payments
            List<EntityExpr> cashCodPcBaExpr = UtilMisc.toList(EntityCondition.makeCondition("paymentMethodTypeId", EntityOperator.EQUALS, "CASH"),
                                           EntityCondition.makeCondition("paymentMethodTypeId", EntityOperator.EQUALS, "EXT_COD"),
                                           EntityCondition.makeCondition("paymentMethodTypeId", EntityOperator.EQUALS, "PERSONAL_CHECK"),
                                           EntityCondition.makeCondition("paymentMethodTypeId", EntityOperator.EQUALS, "EXT_BILLACT"));
            List<GenericValue> cashCodPcBaPaymentPreferences = EntityUtil.filterByOr(allPaymentPreferences, cashCodPcBaExpr);

            if (UtilValidate.isNotEmpty(cashCodPcBaPaymentPreferences)
                    && UtilValidate.isNotEmpty(allPaymentPreferences)
                    && cashCodPcBaPaymentPreferences.size() == allPaymentPreferences.size()) {

                //if there are Check type, approve the order only if it is face to face
                List<GenericValue> checkPreferences = EntityUtil.filterByAnd(cashCodPcBaPaymentPreferences,
                        UtilMisc.toMap("paymentMethodTypeId", "PERSONAL_CHECK"));
                if (UtilValidate.isNotEmpty(checkPreferences)) {
                    if (faceToFace) {
                        boolean ok = OrderChangeHelper.approveOrder(dispatcher, userLogin, orderId, manualHold);
                        if (!ok) {
                            throw new GeneralException("Problem with order change; see above error");
                        }
                    }
                // approve this as long as there are only CASH, COD and Billing Account types
                } else {
                    boolean ok = OrderChangeHelper.approveOrder(dispatcher, userLogin, orderId, manualHold);
                    if (!ok) {
                        throw new GeneralException("Problem with order change; see above error");
                    }
                }

            }
        }

        // check to see if we should auto-invoice/bill
        if (faceToFace) {
            if (Debug.verboseOn()) {
                Debug.logVerbose("Face-To-Face Sale - " + orderId, MODULE);
            }
            CheckOutHelper.adjustFaceToFacePayment(orderId, orderTotal, allPaymentPreferences, userLogin, delegator);
            boolean ok = OrderChangeHelper.completeOrder(dispatcher, userLogin, orderId);
            if (Debug.verboseOn()) {
                Debug.logVerbose("Complete Order Result - " + ok, MODULE);
            }
            if (!ok) {
                throw new GeneralException("Problem with order change; see error logs");
            }
        }
        return ServiceUtil.returnSuccess();
    }

    public static void adjustFaceToFacePayment(String orderId, BigDecimal cartTotal, List<GenericValue> allPaymentPrefs, GenericValue userLogin,
                                               Delegator delegator) throws GeneralException {
        BigDecimal prefTotal = BigDecimal.ZERO;
        if (allPaymentPrefs != null) {
            for (GenericValue pref : allPaymentPrefs) {
                BigDecimal maxAmount = pref.getBigDecimal("maxAmount");
                if (maxAmount == null) {
                    maxAmount = BigDecimal.ZERO;
                }
                prefTotal = prefTotal.add(maxAmount);
            }
        }

        if (prefTotal.compareTo(cartTotal) > 0) {
            BigDecimal change = prefTotal.subtract(cartTotal).negate();
            GenericValue newPref = delegator.makeValue("OrderPaymentPreference");
            newPref.set("orderId", orderId);
            newPref.set("paymentMethodTypeId", "CASH");
            newPref.set("statusId", "PAYMENT_RECEIVED");
            newPref.set("maxAmount", change);
            newPref.set("createdDate", UtilDateTime.nowTimestamp());
            if (userLogin != null) {
                newPref.set("createdByUserLogin", userLogin.getString("userLoginId"));
            }
            delegator.createSetNextSeqId(newPref);
        }
    }

    /**
     * Check order black list map.
     * @return the map
     */
    public Map<String, Object> checkOrderDenyList() {
        if (cart == null) {
            return ServiceUtil.returnSuccess("success");
        }
        GenericValue shippingAddressObj = this.cart.getShippingAddress();
        if (shippingAddressObj == null) {
            return ServiceUtil.returnSuccess("success");
        }
        String shippingAddress = UtilFormatOut.checkNull(shippingAddressObj.getString("address1")).toUpperCase(Locale.getDefault());
        shippingAddress = UtilFormatOut.makeSqlSafe(shippingAddress);
        List<EntityExpr> exprs = UtilMisc.toList(EntityCondition.makeCondition(
                EntityCondition.makeCondition(EntityFunction.upperField("denylistString"), EntityOperator.EQUALS,
                EntityFunction.upper(shippingAddress)),
                EntityOperator.AND,
                EntityCondition.makeCondition("orderDenylistTypeId", EntityOperator.EQUALS, "DENYLIST_ADDRESS")));
        String errMsg = null;

        List<GenericValue> paymentMethods = this.cart.getPaymentMethods();
        for (GenericValue paymentMethod : paymentMethods) {
            if ((paymentMethod != null) && ("CREDIT_CARD".equals(paymentMethod.getString("paymentMethodTypeId")))) {
                GenericValue creditCard = null;
                GenericValue billingAddress = null;
                try {
                    creditCard = paymentMethod.getRelatedOne("CreditCard", false);
                    if (creditCard != null) {
                        billingAddress = creditCard.getRelatedOne("PostalAddress", false);
                    }
                } catch (GenericEntityException e) {
                    Debug.logError(e, "Problems getting credit card from payment method", MODULE);
                    errMsg = UtilProperties.getMessage(RES_ERROR, "checkhelper.problems_reading_database", cart.getLocale());
                    return ServiceUtil.returnError(errMsg);
                }
                if (creditCard != null) {
                    String creditCardNumber = UtilFormatOut.checkNull(creditCard.getString("cardNumber"));
                    exprs.add(EntityCondition.makeCondition(
                            EntityCondition.makeCondition("denylistString", EntityOperator.EQUALS, creditCardNumber), EntityOperator.AND,
                            EntityCondition.makeCondition("orderDenylistTypeId", EntityOperator.EQUALS, "DENYLIST_CREDITCARD")));
                }
                if (billingAddress != null) {
                    String address = UtilFormatOut.checkNull(billingAddress.getString("address1").toUpperCase(Locale.getDefault()));
                    address = UtilFormatOut.makeSqlSafe(address);
                    exprs.add(EntityCondition.makeCondition(
                            EntityCondition.makeCondition(EntityFunction.upperField("denylistString"), EntityOperator.EQUALS,
                            EntityFunction.upper(address)),
                            EntityOperator.AND,
                            EntityCondition.makeCondition("orderDenylistTypeId", EntityOperator.EQUALS, "DENYLIST_ADDRESS")));
                }
            }
        }

        List<GenericValue> denylistFound = null;
        if (!exprs.isEmpty()) {
            try {
                denylistFound = EntityQuery.use(this.delegator).from("OrderDenylist").where(exprs).queryList();
            } catch (GenericEntityException e) {
                Debug.logError(e, "Problems with OrderDenylist lookup.", MODULE);
                errMsg = UtilProperties.getMessage(RES_ERROR, "checkhelper.problems_reading_database", cart.getLocale());
                return ServiceUtil.returnError(errMsg);
            }
        }

        if (UtilValidate.isNotEmpty(denylistFound)) {
            return ServiceUtil.returnFailure(UtilProperties.getMessage(RES_ERROR, "OrderFailed", cart.getLocale()));
        }
        return ServiceUtil.returnSuccess("success");
    }

    /**
     * Check order denylist map.
     * @param userLogin the user login
     * @return the map
     */
    @Deprecated
    public Map<String, Object> checkOrderDenylist(GenericValue userLogin) {
        return checkOrderDenyList();
    }

    /**
     * Failed denylist check map.
     * @param userLogin    the user login
     * @param productStore the product store
     * @return the map
     */
    public Map<String, Object> failedDenylistCheck(GenericValue userLogin, GenericValue productStore) {
        Map<String, Object> result;
        String errMsg = null;
        String rejectMessage = productStore.getString("authFraudMessage");
        String orderId = this.cart.getOrderId();

        try {
            if (userLogin != null) {
                // nuke the userlogin
                userLogin.set("enabled", "N");
                userLogin.store();
            } else {
                userLogin = EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "system").cache().queryOne();
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            errMsg = UtilProperties.getMessage(RES_ERROR, "checkhelper.database_error", cart.getLocale());
            result = ServiceUtil.returnError(errMsg);
            return result;
        }

        // set the order/item status - reverse inv
        OrderChangeHelper.rejectOrder(dispatcher, userLogin, orderId);
        result = ServiceUtil.returnSuccess();
        result.put(ModelService.ERROR_MESSAGE, rejectMessage);

        // wipe the cart and session
        this.cart.clear();
        return result;
    }

    /**
     * Check external payment map.
     * @param orderId the order id
     * @return the map
     */
    public Map<String, Object> checkExternalPayment(String orderId) {
        Map<String, Object> result;
        String errMsg = null;
        // warning there can only be ONE payment preference for this to work
        // you cannot accept multiple payment type when using an external gateway
        GenericValue orderHeader = null;
        try {
            orderHeader = EntityQuery.use(delegator).from("OrderHeader").where("orderId", orderId).queryOne();
        } catch (GenericEntityException e) {
            Debug.logError(e, "Problems getting order header", MODULE);
            errMsg = UtilProperties.getMessage(RES_ERROR, "checkhelper.problems_getting_order_header", (cart != null ? cart.getLocale()
                    : Locale.getDefault()));
            result = ServiceUtil.returnError(errMsg);
            return result;
        }
        if (orderHeader != null) {
            List<GenericValue> paymentPrefs = null;
            try {
                paymentPrefs = orderHeader.getRelated("OrderPaymentPreference", null, null, false);
            } catch (GenericEntityException e) {
                Debug.logError(e, "Problems getting order payments", MODULE);
                errMsg = UtilProperties.getMessage(RES_ERROR, "checkhelper.problems_getting_payment_preference", (cart != null ? cart.getLocale()
                        : Locale.getDefault()));
                result = ServiceUtil.returnError(errMsg);
                return result;
            }
            if (UtilValidate.isNotEmpty(paymentPrefs)) {
                if (paymentPrefs.size() > 1) {
                    Debug.logError("Too many payment preferences, you cannot have more then one when using external gateways", MODULE);
                }
                GenericValue paymentPreference = EntityUtil.getFirst(paymentPrefs);
                String paymentMethodTypeId = paymentPreference.getString("paymentMethodTypeId");
                if (paymentMethodTypeId.startsWith("EXT_")) {
                    // PayPal with a PaymentMethod is not an external payment method
                    if (!("EXT_PAYPAL".equals(paymentMethodTypeId) && UtilValidate.isNotEmpty(paymentPreference.getString("paymentMethodId")))) {
                        String type = paymentMethodTypeId.substring(4);
                        result = ServiceUtil.returnSuccess();
                        result.put("type", type.toLowerCase(Locale.getDefault()));
                        return result;
                    }
                }
            }
            result = ServiceUtil.returnSuccess();
            result.put("type", "none");
            return result;
        }
        errMsg = UtilProperties.getMessage(RES_ERROR, "checkhelper.problems_getting_order_header", (cart != null
                ? cart.getLocale()
                : Locale.getDefault()));
        result = ServiceUtil.returnError(errMsg);
        result.put("type", "error");
        return result;
    }

    /**
     * Sets the shipping contact mechanism for a given ship group on the cart
     * @param shipGroupIndex The index of the ship group in the cart
     * @param shippingContactMechId The identifier of the contact
     * @param supplierPartyId The identifier of the supplier to use for the drop shipment
     * @param supplierAgreementId The identifier of the agreement with the supplier
     * @return A Map conforming to the OFBiz Service conventions containing
     * any error messages
     */
    public Map<String, Object> finalizeOrderEntryShip(int shipGroupIndex, String shippingContactMechId, String supplierPartyId,
                                                      String supplierAgreementId) {
        Map<String, Object> result;
        String errMsg = null;
        //Verify the field is valid
        if (UtilValidate.isNotEmpty(shippingContactMechId)) {
            this.cart.setShippingContactMechId(shipGroupIndex, shippingContactMechId);
            if (UtilValidate.isNotEmpty(supplierPartyId)) {
                this.cart.setSupplierPartyId(shipGroupIndex, supplierPartyId);
                if (UtilValidate.isNotEmpty(supplierAgreementId)) {
                    this.cart.setSupplierAgreementId(shipGroupIndex, supplierAgreementId);
                }
            }
            result = ServiceUtil.returnSuccess();
        } else {
            errMsg = UtilProperties.getMessage(RES_ERROR, "checkhelper.enter_shipping_address", (cart != null ? cart.getLocale()
                    : Locale.getDefault()));
            result = ServiceUtil.returnError(errMsg);
        }

        return result;
    }

    /**
     * Sets the options associated with the order for a given ship group
     * @param shipGroupIndex The index of the ship group in the cart
     * @param shippingMethod The shipping method indicating the carrier and
     * shipment type to use
     * @param shippingInstructions Any additional handling instructions
     * @param maySplit "true" or anything else for <code>false</code>
     * @param giftMessage A message to have included for the recipient
     * @param isGift "true" or anything else for <code>false</code>
     * @param internalCode an internal code associated with the order
     * @return A Map conforming to the OFBiz Service conventions containing
     * any error messages
     */
    public Map<String, Object> finalizeOrderEntryOptions(int shipGroupIndex, String shippingMethod, String shippingInstructions, String maySplit,
            String giftMessage, String isGift, String internalCode, String shipBeforeDate, String shipAfterDate, String orderAdditionalEmails) {
        this.cart.setOrderAdditionalEmails(orderAdditionalEmails);
        return finalizeOrderEntryOptions(shipGroupIndex, shippingMethod, shippingInstructions, maySplit, giftMessage, isGift, internalCode,
                shipBeforeDate, shipAfterDate, null, null);
    }

    /**
     * Finalize order entry options map.
     * @param shipGroupIndex       the ship group index
     * @param shippingMethod       the shipping method
     * @param shippingInstructions the shipping instructions
     * @param maySplit             the may split
     * @param giftMessage          the gift message
     * @param isGift               the is gift
     * @param internalCode         the internal code
     * @param shipBeforeDate       the ship before date
     * @param shipAfterDate        the ship after date
     * @param internalOrderNotes   the internal order notes
     * @param shippingNotes        the shipping notes
     * @param shipEstimate         the ship estimate
     * @return the map
     */
    public Map<String, Object> finalizeOrderEntryOptions(int shipGroupIndex, String shippingMethod, String shippingInstructions, String maySplit,
            String giftMessage, String isGift, String internalCode, String shipBeforeDate, String shipAfterDate, String internalOrderNotes,
                                                         String shippingNotes, BigDecimal shipEstimate) {
        this.cart.setItemShipGroupEstimate(shipEstimate, shipGroupIndex);
        return finalizeOrderEntryOptions(shipGroupIndex, shippingMethod, shippingInstructions, maySplit, giftMessage, isGift, internalCode,
                shipBeforeDate, shipAfterDate, internalOrderNotes, shippingNotes);
    }

    /**
     * Finalize order entry options map.
     * @param shipGroupIndex       the ship group index
     * @param shippingMethod       the shipping method
     * @param shippingInstructions the shipping instructions
     * @param maySplit             the may split
     * @param giftMessage          the gift message
     * @param isGift               the is gift
     * @param internalCode         the internal code
     * @param shipBeforeDate       the ship before date
     * @param shipAfterDate        the ship after date
     * @param internalOrderNotes   the internal order notes
     * @param shippingNotes        the shipping notes
     * @return the map
     */
    public Map<String, Object> finalizeOrderEntryOptions(int shipGroupIndex, String shippingMethod, String shippingInstructions, String maySplit,
            String giftMessage, String isGift, String internalCode, String shipBeforeDate, String shipAfterDate, String internalOrderNotes,
            String shippingNotes) {

        Map<String, Object> result = ServiceUtil.returnSuccess();

        String errMsg = null;
        //Verify the shipping method is valid
        if (UtilValidate.isNotEmpty(shippingMethod)) {
            int delimiterPos = shippingMethod.indexOf('@');
            String shipmentMethodTypeId = null;
            String carrierPartyId = null;

            if (delimiterPos > 0) {
                shipmentMethodTypeId = shippingMethod.substring(0, delimiterPos);
                carrierPartyId = shippingMethod.substring(delimiterPos + 1);
            }

            this.cart.setShipmentMethodTypeId(shipGroupIndex, shipmentMethodTypeId);
            this.cart.setCarrierPartyId(shipGroupIndex, carrierPartyId);
        } else {
            errMsg = UtilProperties.getMessage(RES_ERROR, "checkhelper.select_shipping_method", cart.getLocale());
            result = ServiceUtil.returnError(errMsg);
        }

        //Set the remaining order options
        this.cart.setShippingInstructions(shipGroupIndex, shippingInstructions);
        this.cart.setGiftMessage(shipGroupIndex, giftMessage);
        this.cart.setMaySplit(shipGroupIndex, Boolean.valueOf(maySplit));
        this.cart.setIsGift(shipGroupIndex, Boolean.valueOf(isGift));
        this.cart.setInternalCode(internalCode); // FIXME: the internalCode is not a ship group field and should be moved outside of this method
        if (UtilValidate.isNotEmpty(internalOrderNotes)) {
            this.cart.addInternalOrderNote(internalOrderNotes);
        }

        // set ship before date
        if ((shipBeforeDate != null) && (shipBeforeDate.length() > 8)) {
            shipBeforeDate = shipBeforeDate.trim();
            if (shipBeforeDate.length() < 14) {
                shipBeforeDate = shipBeforeDate + " " + "00:00:00.000";
            }

            try {
                this.cart.setShipBeforeDate(shipGroupIndex, (Timestamp) ObjectType.simpleTypeOrObjectConvert(shipBeforeDate, "Timestamp", null,
                        null));
            } catch (Exception e) {
                errMsg = "Ship Before Date must be a valid date formed ";
                result = ServiceUtil.returnError(errMsg);
            }
        }

        // set ship after date
        if ((shipAfterDate != null) && (shipAfterDate.length() > 8)) {
            shipAfterDate = shipAfterDate.trim();
            if (shipAfterDate.length() < 14) {
                shipAfterDate = shipAfterDate + " " + "00:00:00.000";
            }

            try {
                this.cart.setShipAfterDate(shipGroupIndex, (Timestamp) ObjectType.simpleTypeOrObjectConvert(shipAfterDate, "Timestamp", null, null));
            } catch (Exception e) {
                errMsg = "Ship After Date must be a valid date formed ";
                result = ServiceUtil.returnError(errMsg);
            }
        }

        // Shipping Notes for order will be public
        if (UtilValidate.isNotEmpty(shippingNotes)) {
            this.cart.addOrderNote(shippingNotes);
        }

        return result;
    }

    /**
     * Sets the payment ID to use during the checkout process
     * @param checkOutPaymentId The payment ID to be associated with the cart
     * @return A Map conforming to the OFBiz Service conventions containing
     * any error messages.
     */
    public Map<String, Object> finalizeOrderEntryPayment(String checkOutPaymentId, BigDecimal amount, boolean singleUse, boolean append) {
        Map<String, Object> result = ServiceUtil.returnSuccess();

        if (UtilValidate.isNotEmpty(checkOutPaymentId)) {
            if (!append) {
                cart.clearPayments();
            }
            cart.addPaymentAmount(checkOutPaymentId, amount, singleUse);
        }

        return result;
    }

    public static BigDecimal availableAccountBalance(String billingAccountId, LocalDispatcher dispatcher) {
        if (billingAccountId == null) {
            return BigDecimal.ZERO;
        }
        try {
            Map<String, Object> res = dispatcher.runSync("calcBillingAccountBalance", UtilMisc.toMap("billingAccountId", billingAccountId));
            if (ServiceUtil.isError(res)) {
                Debug.logError(ServiceUtil.getErrorMessage(res), MODULE);
                return BigDecimal.ZERO;
            }
            BigDecimal availableBalance = (BigDecimal) res.get("accountBalance");
            if (availableBalance != null) {
                return availableBalance;
            }
        } catch (GenericServiceException e) {
            Debug.logError(e, MODULE);
        }
        return BigDecimal.ZERO;
    }

    /**
     * Available account balance big decimal.
     * @param billingAccountId the billing account id
     * @return the big decimal
     */
    public BigDecimal availableAccountBalance(String billingAccountId) {
        return availableAccountBalance(billingAccountId, dispatcher);
    }

    /**
     * Make billing account map map.
     * @param paymentPrefs the payment prefs
     * @return the map
     */
    public Map<String, BigDecimal> makeBillingAccountMap(List<GenericValue> paymentPrefs) {
        Map<String, BigDecimal> accountMap = new HashMap<>();
        if (paymentPrefs != null) {
            for (GenericValue pp : paymentPrefs) {
                if (pp.get("billingAccountId") != null) {
                    accountMap.put(pp.getString("billingAccountId"), pp.getBigDecimal("maxAmount"));
                }
            }
        }
        return accountMap;
    }

    /**
     * Validate payment methods map.
     * @return the map
     */
    public Map<String, Object> validatePaymentMethods() {
        String errMsg = null;
        String billingAccountId = cart.getBillingAccountId();
        BigDecimal billingAccountAmt = cart.getBillingAccountAmount();
        BigDecimal availableAmount = this.availableAccountBalance(billingAccountId);
        if (billingAccountAmt.compareTo(availableAmount) > 0) {
            Debug.logError("Billing account " + billingAccountId + " has [" + availableAmount + "] available but needs [" + billingAccountAmt
                    + "] for this order", MODULE);
            Map<String, String> messageMap = UtilMisc.toMap("billingAccountId", billingAccountId);
            errMsg = UtilProperties.getMessage(RES_ERROR, "checkevents.not_enough_available_on_account", messageMap, cart.getLocale());
            return ServiceUtil.returnError(errMsg);
        }

        // payment by billing account only requires more checking
        List<String> paymentMethods = cart.getPaymentMethodIds();
        List<String> paymentTypes = cart.getPaymentMethodTypeIds();
        if (paymentTypes.contains("EXT_BILLACT") && paymentTypes.size() == 1 && paymentMethods.isEmpty()) {
            if (cart.getGrandTotal().compareTo(availableAmount) > 0) {
                errMsg = UtilProperties.getMessage(RES_ERROR, "checkevents.insufficient_credit_available_on_account", cart.getLocale());
                return ServiceUtil.returnError(errMsg);
            }
        }

        // validate any gift card balances
        this.validateGiftCardAmounts();

        // update the selected payment methods amount with valid numbers
        if (paymentMethods != null) {
            List<String> nullPaymentIds = new ArrayList<>();
            for (String paymentMethodId : paymentMethods) {
                BigDecimal paymentAmount = cart.getPaymentAmount(paymentMethodId);
                if (paymentAmount == null || paymentAmount.compareTo(BigDecimal.ZERO) == 0) {
                    if (Debug.verboseOn()) {
                        Debug.logVerbose("Found null paymentMethodId - " + paymentMethodId, MODULE);
                    }
                    nullPaymentIds.add(paymentMethodId);
                }
            }
            for (String paymentMethodId : nullPaymentIds) {
                BigDecimal selectedPaymentTotal = cart.getPaymentTotal();
                BigDecimal requiredAmount = cart.getGrandTotal();
                BigDecimal newAmount = requiredAmount.subtract(selectedPaymentTotal);
                boolean setOverflow = false;

                ShoppingCart.CartPaymentInfo info = cart.getPaymentInfo(paymentMethodId);

                if (Debug.verboseOn()) {
                    Debug.logVerbose("Remaining total is - " + newAmount, MODULE);
                }
                if (newAmount.compareTo(BigDecimal.ZERO) > 0) {
                    info.setAmount(newAmount);
                    if (Debug.verboseOn()) {
                        Debug.logVerbose("Set null paymentMethodId - " + info.getPaymentMethodId() + " / " + newAmount, MODULE);
                    }
                } else {
                    info.setAmount(BigDecimal.ZERO);
                    if (Debug.verboseOn()) {
                        Debug.logVerbose("Set null paymentMethodId - " + info.getPaymentMethodId() + " / " + BigDecimal.ZERO, MODULE);
                    }
                }
                if (!setOverflow) {
                    info.setOverflow(true);
                    if (Debug.verboseOn()) {
                        Debug.logVerbose("Set overflow flag on payment - " + info.getPaymentMethodId(), MODULE);
                    }
                }
            }
        }

        // verify the selected payment method amounts will cover the total
        BigDecimal reqAmtPreParse = cart.getGrandTotal().subtract(cart.getBillingAccountAmount());
        BigDecimal selectedPmnt = cart.getPaymentTotal();

        BigDecimal selectedPaymentTotal = selectedPmnt.setScale(DECIMALS, ROUNDING);
        BigDecimal requiredAmount = reqAmtPreParse.setScale(DECIMALS, ROUNDING);

        if (UtilValidate.isNotEmpty(paymentMethods) && requiredAmount.compareTo(selectedPaymentTotal) > 0) {
            Debug.logError("Required Amount : " + requiredAmount + " / Selected Amount : " + selectedPaymentTotal, MODULE);
            errMsg = UtilProperties.getMessage(RES_ERROR, "checkevents.payment_not_cover_this_order", cart.getLocale());
            return ServiceUtil.returnError(errMsg);
        }
        if (UtilValidate.isNotEmpty(paymentMethods) && requiredAmount.compareTo(selectedPaymentTotal) < 0) {
            BigDecimal changeAmount = selectedPaymentTotal.subtract(requiredAmount);
            if (!paymentTypes.contains("CASH")) {
                Debug.logError("Change Amount : " + changeAmount + " / No cash.", MODULE);
                errMsg = UtilProperties.getMessage(RES_ERROR, "checkhelper.change_returned_cannot_be_greater_than_cash", cart.getLocale());
                return ServiceUtil.returnError(errMsg);
            }
            int cashIndex = paymentTypes.indexOf("CASH");
            String cashId = paymentTypes.get(cashIndex);
            BigDecimal cashAmount = cart.getPaymentAmount(cashId);
            if (cashAmount.compareTo(changeAmount) < 0) {
                Debug.logError("Change Amount : " + changeAmount + " / Cash Amount : " + cashAmount, MODULE);
                errMsg = UtilProperties.getMessage(RES_ERROR,
                        "checkhelper.change_returned_cannot_be_greater_than_cash", cart.getLocale());
                return ServiceUtil.returnError(errMsg);
            }
        }
        return ServiceUtil.returnSuccess();
    }

    /**
     * Validate gift card amounts.
     */
    public void validateGiftCardAmounts() {
        // get the product store
        GenericValue productStore = ProductStoreWorker.getProductStore(cart.getProductStoreId(), delegator);
        if (productStore != null && !"Y".equalsIgnoreCase(productStore.getString("checkGcBalance"))) {
            return;
        }

        // get the payment config
        String paymentConfig = ProductStoreWorker.getProductStorePaymentProperties(delegator, cart.getProductStoreId(), "GIFT_CARD", null, true);
        String giftCardType = EntityUtilProperties.getPropertyValue(paymentConfig, "", "ofbiz", delegator);
        String balanceField = null;

        // get the gift card objects to check
        for (GenericValue gc : cart.getGiftCards()) {
            Map<String, Object> gcBalanceMap = null;
            BigDecimal gcBalance = BigDecimal.ZERO;
            try {
                Map<String, Object> ctx = UtilMisc.<String, Object>toMap("userLogin", cart.getUserLogin());
                ctx.put("currency", cart.getCurrency());
                if ("ofbiz".equalsIgnoreCase(giftCardType)) {
                    balanceField = "balance";
                    ctx.put("cardNumber", gc.getString("cardNumber"));
                    ctx.put("pinNumber", gc.getString("pinNumber"));
                    gcBalanceMap = dispatcher.runSync("checkGiftCertificateBalance", ctx);
                    if (ServiceUtil.isError(gcBalanceMap)) {
                        Debug.logError(ServiceUtil.getErrorMessage(gcBalanceMap), MODULE);
                    }
                }
                if ("valuelink".equalsIgnoreCase(giftCardType)) {
                    balanceField = "balance";
                    ctx.put("paymentConfig", paymentConfig);
                    ctx.put("cardNumber", gc.getString("cardNumber"));
                    ctx.put("pin", gc.getString("pinNumber"));
                    gcBalanceMap = dispatcher.runSync("balanceInquireGiftCard", ctx);
                    if (ServiceUtil.isError(gcBalanceMap)) {
                        Debug.logError(ServiceUtil.getErrorMessage(gcBalanceMap), MODULE);
                    }
                }
            } catch (GenericServiceException e) {
                Debug.logError(e, MODULE);
            }
            if (gcBalanceMap != null) {
                BigDecimal bal = (BigDecimal) gcBalanceMap.get(balanceField);
                if (bal != null) {
                    gcBalance = bal;
                }
            }

            // get the bill-up to amount
            BigDecimal billUpTo = cart.getPaymentAmount(gc.getString("paymentMethodId"));

            // null bill-up to means use the full balance || update the bill-up to with the balance
            if (billUpTo == null || billUpTo.compareTo(BigDecimal.ZERO) == 0 || gcBalance.compareTo(billUpTo) < 0) {
                cart.addPaymentAmount(gc.getString("paymentMethodId"), gcBalance);
            }
        }
    }
}
