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
package org.ofbiz.order.shoppingcart;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.ObjectType;
import org.ofbiz.base.util.StringUtil;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilFormatOut;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilNumber;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityConditionList;
import org.ofbiz.entity.condition.EntityExpr;
import org.ofbiz.entity.condition.EntityFieldValue;
import org.ofbiz.entity.condition.EntityFunction;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.order.finaccount.FinAccountHelper;
import org.ofbiz.order.order.OrderChangeHelper;
import org.ofbiz.order.order.OrderReadHelper;
import org.ofbiz.order.shoppingcart.product.ProductPromoWorker;
import org.ofbiz.order.shoppingcart.shipping.ShippingEvents;
import org.ofbiz.order.thirdparty.paypal.ExpressCheckoutEvents;
import org.ofbiz.party.contact.ContactHelper;
import org.ofbiz.product.store.ProductStoreWorker;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ModelService;
import org.ofbiz.service.ServiceUtil;

/**
 * A facade over the ShoppingCart to simplify the relatively complex
 * processing required to create an order in the system.
 */
public class CheckOutHelper {

    public static final String module = CheckOutHelper.class.getName();
    public static final String resource = "OrderUiLabels";
    public static final String resource_error = "OrderErrorUiLabels";

    public static final int scale = UtilNumber.getBigDecimalScale("order.decimals");
    public static final int rounding = UtilNumber.getBigDecimalRoundingMode("order.rounding");

    protected LocalDispatcher dispatcher = null;
    protected Delegator delegator = null;
    protected ShoppingCart cart = null;

    public CheckOutHelper(LocalDispatcher dispatcher, Delegator delegator, ShoppingCart cart) {
        this.delegator = delegator;
        this.dispatcher = dispatcher;
        this.cart = cart;
    }

    public Map setCheckOutShippingAddress(String shippingContactMechId) {
        List errorMessages = new ArrayList();
        Map result;
        String errMsg = null;

        if (UtilValidate.isNotEmpty(this.cart)) {
            errorMessages.addAll(setCheckOutShippingAddressInternal(shippingContactMechId));
        } else {
            errMsg = UtilProperties.getMessage(resource_error,"checkhelper.no_items_in_cart", (cart != null ? cart.getLocale() : Locale.getDefault()));
            errorMessages.add(errMsg);
        }
        if (errorMessages.size() == 1) {
            result = ServiceUtil.returnError(errorMessages.get(0).toString());
        } else if (errorMessages.size() > 0) {
            result = ServiceUtil.returnError(errorMessages);
        } else {
            result = ServiceUtil.returnSuccess();
        }

        return result;
    }

    private List setCheckOutShippingAddressInternal(String shippingContactMechId) {
        List errorMessages = new ArrayList();
        String errMsg = null;

        // set the shipping address
        if (UtilValidate.isNotEmpty(shippingContactMechId)) {
            this.cart.setShippingContactMechId(shippingContactMechId);
        } else if (cart.shippingApplies()) {
            // only return an error if shipping is required for this purchase
            errMsg = UtilProperties.getMessage(resource_error,"checkhelper.select_shipping_destination", (cart != null ? cart.getLocale() : Locale.getDefault()));
            errorMessages.add(errMsg);
        }

        return errorMessages;
    }

    public Map setCheckOutShippingOptions(String shippingMethod, String shippingInstructions,
            String orderAdditionalEmails, String maySplit, String giftMessage, String isGift, String internalCode, String shipBeforeDate, String shipAfterDate) {
        List errorMessages = new ArrayList();
        Map result;
        String errMsg = null;

        if (UtilValidate.isNotEmpty(this.cart)) {
            errorMessages.addAll(setCheckOutShippingOptionsInternal(shippingMethod, shippingInstructions,
                    orderAdditionalEmails, maySplit, giftMessage, isGift, internalCode, shipBeforeDate, shipAfterDate));
        } else {
            errMsg = UtilProperties.getMessage(resource_error,"checkhelper.no_items_in_cart", (cart != null ? cart.getLocale() : Locale.getDefault()));
            errorMessages.add(errMsg);
        }

        if (errorMessages.size() == 1) {
            result = ServiceUtil.returnError(errorMessages.get(0).toString());
        } else if (errorMessages.size() > 0) {
            result = ServiceUtil.returnError(errorMessages);
        } else {
            result = ServiceUtil.returnSuccess();
        }

        return result;
    }

    private List setCheckOutShippingOptionsInternal(String shippingMethod, String shippingInstructions, String orderAdditionalEmails,
            String maySplit, String giftMessage, String isGift, String internalCode, String shipBeforeDate, String shipAfterDate) {
        List errorMessages = new ArrayList();
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

            this.cart.setShipmentMethodTypeId(shipmentMethodTypeId);
            this.cart.setCarrierPartyId(carrierPartyId);
        } else if (cart.shippingApplies()) {
            // only return an error if shipping is required for this purchase
            errMsg = UtilProperties.getMessage(resource_error,"checkhelper.select_shipping_method", (cart != null ? cart.getLocale() : Locale.getDefault()));
            errorMessages.add(errMsg);
        }

        // set the shipping instructions
        this.cart.setShippingInstructions(shippingInstructions);

        if (UtilValidate.isNotEmpty(maySplit)) {
            cart.setMaySplit(Boolean.valueOf(maySplit));
        } else {
            errMsg = UtilProperties.getMessage(resource_error,"checkhelper.select_splitting_preference", (cart != null ? cart.getLocale() : Locale.getDefault()));
            errorMessages.add(errMsg);
        }

        // set the gift message
        this.cart.setGiftMessage(giftMessage);

        if (UtilValidate.isNotEmpty(isGift)) {
            cart.setIsGift(Boolean.valueOf(isGift));
        } else {
            errMsg = UtilProperties.getMessage(resource_error, "checkhelper.specify_if_order_is_gift", (cart != null ? cart.getLocale() : Locale.getDefault()));
            errorMessages.add(errMsg);
        }

        // interal code
        this.cart.setInternalCode(internalCode);

        if (UtilValidate.isNotEmpty(shipBeforeDate)) {
            if (UtilValidate.isDate(shipBeforeDate)) {
                cart.setShipBeforeDate(UtilDateTime.toTimestamp(shipBeforeDate));
            } else {
                errMsg = UtilProperties.getMessage(resource_error, "checkhelper.specify_if_shipBeforeDate_is_date", (cart != null ? cart.getLocale() : Locale.getDefault()));
                errorMessages.add(errMsg);
            }
        }

        if (UtilValidate.isNotEmpty(shipAfterDate)) {
            if (UtilValidate.isDate(shipAfterDate)) {
                cart.setShipAfterDate(UtilDateTime.toTimestamp(shipAfterDate));
            } else {
                errMsg = UtilProperties.getMessage(resource_error, "checkhelper.specify_if_shipAfterDate_is_date", (cart != null ? cart.getLocale() : Locale.getDefault()));
                errorMessages.add(errMsg);
            }
        }

        // set any additional notification emails
        this.cart.setOrderAdditionalEmails(orderAdditionalEmails);

        return errorMessages;
    }

    public Map setCheckOutPayment(Map selectedPaymentMethods, List singleUsePayments, String billingAccountId) {
        List errorMessages = new ArrayList();
        Map result;
        String errMsg = null;

        if (UtilValidate.isNotEmpty(this.cart)) {
            errorMessages.addAll(setCheckOutPaymentInternal(selectedPaymentMethods, singleUsePayments, billingAccountId));
        } else {
            errMsg = UtilProperties.getMessage(resource_error,"checkhelper.no_items_in_cart", (cart != null ? cart.getLocale() : Locale.getDefault()));
            errorMessages.add(errMsg);
        }

        if (errorMessages.size() == 1) {
            result = ServiceUtil.returnError(errorMessages.get(0).toString());
        } else if (errorMessages.size() > 0) {
            result = ServiceUtil.returnError(errorMessages);
        } else {
            result = ServiceUtil.returnSuccess();
        }

        return result;
    }

    public List setCheckOutPaymentInternal(Map selectedPaymentMethods, List singleUsePayments, String billingAccountId) {
        List errorMessages = new ArrayList();
        String errMsg = null;

        if (singleUsePayments == null) {
            singleUsePayments = new ArrayList();
        }

        // set the payment method option
        if (UtilValidate.isNotEmpty(selectedPaymentMethods)) {
            // clear out the old payments
            cart.clearPayments();

            if (UtilValidate.isNotEmpty(billingAccountId)) {
                Map billingAccountMap = (Map)selectedPaymentMethods.get("EXT_BILLACT");
                BigDecimal billingAccountAmt = (BigDecimal)billingAccountMap.get("amount");
                // set cart billing account data and generate a payment method containing the amount we will be charging
                cart.setBillingAccount(billingAccountId, (billingAccountAmt != null ? billingAccountAmt: BigDecimal.ZERO));
                // copy the billing account terms as order terms
                try {
                    List billingAccountTerms = delegator.findByAnd("BillingAccountTerm", UtilMisc.toMap("billingAccountId", billingAccountId));
                    if (UtilValidate.isNotEmpty(billingAccountTerms)) {
                        Iterator billingAccountTermsIt = billingAccountTerms.iterator();
                        while (billingAccountTermsIt.hasNext()) {
                            GenericValue billingAccountTerm = (GenericValue)billingAccountTermsIt.next();
                            // the term is not copied if in the cart a term of the same type is already set
                            if (!cart.hasOrderTerm(billingAccountTerm.getString("termTypeId"))) {
                                cart.addOrderTerm(billingAccountTerm.getString("termTypeId"), billingAccountTerm.getBigDecimal("termValue"), billingAccountTerm.getLong("termDays"));
                            }
                        }
                    }
                } catch (GenericEntityException gee) {
                    Debug.logWarning("Error copying billing account terms to order terms: " + gee.getMessage(), module);
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
                    errMsg = UtilProperties.getMessage(resource_error,"checkhelper.insufficient_credit_available_on_account",
                            (cart != null ? cart.getLocale() : Locale.getDefault()));
                    errorMessages.add(errMsg);
                } else {
                    // otherwise use the available account credit (The user might enter 10.00 for an order worth 20.00 from an account with 30.00. This makes sure that the 30.00 is used)
                    amountToUse = accountCredit;
                }

                // check that the amount to use is enough to fulfill the order
                BigDecimal grandTotal = cart.getGrandTotal();
                if (grandTotal.compareTo(amountToUse) > 0) {
                    cart.setBillingAccount(null, BigDecimal.ZERO); // erase existing billing account data
                    errMsg = UtilProperties.getMessage(resource_error,"checkhelper.insufficient_credit_available_on_account",
                            (cart != null ? cart.getLocale() : Locale.getDefault()));
                    errorMessages.add(errMsg);
                } else {
                    // since this is the only selected payment method, let's make this amount the grand total for convenience
                    amountToUse = grandTotal;
                }

                // associate the cart billing account amount and EXT_BILLACT selected payment method with whatever amount we have now
                // XXX: Note that this step is critical for the billing account to be charged correctly
                if (amountToUse.compareTo(BigDecimal.ZERO) > 0) {
                    cart.setBillingAccount(billingAccountId, amountToUse);
                    selectedPaymentMethods.put("EXT_BILLACT", UtilMisc.toMap("amount", amountToUse, "securityCode", null));
                }
            }

            Set paymentMethods = selectedPaymentMethods.keySet();
            Iterator i = paymentMethods.iterator();
            while (i.hasNext()) {
                String checkOutPaymentId = (String) i.next();
                String finAccountId = null;

                if (checkOutPaymentId.indexOf("|") > -1) {
                    // split type -- ID|Actual
                    String[] splitStr = checkOutPaymentId.split("\\|");
                    checkOutPaymentId = splitStr[0];
                    if ("FIN_ACCOUNT".equals(checkOutPaymentId)) {
                        finAccountId = splitStr[1];
                    }
                    if (Debug.verboseOn()) Debug.logVerbose("Split checkOutPaymentId: " + splitStr[0] + " / " + splitStr[1], module);
                }

                // get the selected amount to use
                BigDecimal paymentAmount = null;
                String securityCode = null;
                if (selectedPaymentMethods.get(checkOutPaymentId) != null) {
                    Map checkOutPaymentInfo = (Map) selectedPaymentMethods.get(checkOutPaymentId);
                    paymentAmount = (BigDecimal) checkOutPaymentInfo.get("amount");
                    securityCode = (String) checkOutPaymentInfo.get("securityCode");
                }

                boolean singleUse = singleUsePayments.contains(checkOutPaymentId);
                ShoppingCart.CartPaymentInfo inf = cart.addPaymentAmount(checkOutPaymentId, paymentAmount, singleUse);
                if (finAccountId != null) {
                    inf.finAccountId = finAccountId;
                }
                if (securityCode != null) {
                    inf.securityCode = securityCode;
                }
            }
        } else if (cart.getGrandTotal().compareTo(BigDecimal.ZERO) != 0) {
            // only return an error if the order total is not 0.00
            errMsg = UtilProperties.getMessage(resource_error,"checkhelper.select_method_of_payment",
                    (cart != null ? cart.getLocale() : Locale.getDefault()));
            errorMessages.add(errMsg);
        }

        return errorMessages;
    }

    public Map setCheckOutDates(Timestamp shipBefore, Timestamp shipAfter) {
          List errorMessages = new ArrayList();
          Map result = null;
          String errMsg = null;

          if (UtilValidate.isNotEmpty(this.cart)) {
              this.cart.setShipBeforeDate(shipBefore);
              this.cart.setShipAfterDate(shipAfter);
          } else {
              errMsg = UtilProperties.getMessage(resource_error,"checkhelper.no_items_in_cart",
                                                     (cart != null ? cart.getLocale() : Locale.getDefault()));
              errorMessages.add(errMsg);
          }

          if (errorMessages.size() == 1) {
              result = ServiceUtil.returnError(errorMessages.get(0).toString());
          } else if (errorMessages.size() > 0) {
              result = ServiceUtil.returnError(errorMessages);
          } else {
              result = ServiceUtil.returnSuccess();
          }
          return result;
    }


    public Map setCheckOutOptions(String shippingMethod, String shippingContactMechId, Map selectedPaymentMethods,
            List singleUsePayments, String billingAccountId, String shippingInstructions,
            String orderAdditionalEmails, String maySplit, String giftMessage, String isGift, String internalCode, String shipBeforeDate, String shipAfterDate) {
        List errorMessages = new ArrayList();
        Map result = null;
        String errMsg = null;


        if (UtilValidate.isNotEmpty(this.cart)) {
            // set the general shipping options and method
            errorMessages.addAll(setCheckOutShippingOptionsInternal(shippingMethod, shippingInstructions,
                    orderAdditionalEmails, maySplit, giftMessage, isGift, internalCode, shipBeforeDate, shipAfterDate));

            // set the shipping address
            errorMessages.addAll(setCheckOutShippingAddressInternal(shippingContactMechId));

            // Recalc shipping costs before setting payment
            Map shipEstimateMap = ShippingEvents.getShipGroupEstimate(dispatcher, delegator, cart, 0);
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
                Debug.logError(e, module);
            }
            // set the payment method(s) option
            errorMessages.addAll(setCheckOutPaymentInternal(selectedPaymentMethods, singleUsePayments, billingAccountId));

        } else {
            errMsg = UtilProperties.getMessage(resource_error,"checkhelper.no_items_in_cart", (cart != null ? cart.getLocale() : Locale.getDefault()));
            errorMessages.add(errMsg);
        }

        if (errorMessages.size() == 1) {
            result = ServiceUtil.returnError(errorMessages.get(0).toString());
        } else if (errorMessages.size() > 0) {
            result = ServiceUtil.returnError(errorMessages);
        } else {
            result = ServiceUtil.returnSuccess();
        }

        return result;
    }

    public Map checkGiftCard(Map params, Map selectedPaymentMethods) {
        List errorMessages = new ArrayList();
        Map errorMaps = new HashMap();
        Map result = new HashMap();
        String errMsg = null;
        // handle gift card payment
        if (params.get("addGiftCard") != null) {
            String gcNum = (String) params.get("giftCardNumber");
            String gcPin = (String) params.get("giftCardPin");
            String gcAmt = (String) params.get("giftCardAmount");
            BigDecimal gcAmount = BigDecimal.ONE.negate();

            boolean gcFieldsOkay = true;
            if (UtilValidate.isEmpty(gcNum)) {
                errMsg = UtilProperties.getMessage(resource_error,"checkhelper.enter_gift_card_number", (cart != null ? cart.getLocale() : Locale.getDefault()));
                errorMessages.add(errMsg);
                gcFieldsOkay = false;
            }
            if (cart.isPinRequiredForGC(delegator)) {
                //  if a PIN is required, make sure the PIN is valid
                if (UtilValidate.isEmpty(gcPin)) {
                    errMsg = UtilProperties.getMessage(resource_error,"checkhelper.enter_gift_card_pin_number", (cart != null ? cart.getLocale() : Locale.getDefault()));
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
                            errMsg = UtilProperties.getMessage(resource_error,"checkhelper.gift_card_does_not_exist", (cart != null ? cart.getLocale() : Locale.getDefault()));
                            errorMessages.add(errMsg);
                            gcFieldsOkay = false;
                        } else if ((finAccount.getBigDecimal("availableBalance") == null) ||
                                !((finAccount.getBigDecimal("availableBalance")).compareTo(FinAccountHelper.ZERO) > 0)) {
                            // if account's available balance (including authorizations) is not greater than zero, then return an error
                            errMsg = UtilProperties.getMessage(resource_error,"checkhelper.gift_card_has_no_value", (cart != null ? cart.getLocale() : Locale.getDefault()));
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
                    errMsg = UtilProperties.getMessage(resource_error,"checkhelper.enter_amount_to_place_on_gift_card", (cart != null ? cart.getLocale() : Locale.getDefault()));
                    errorMessages.add(errMsg);
                    gcFieldsOkay = false;
                }
            }
            if (UtilValidate.isNotEmpty(gcAmt)) {
                try {
                    gcAmount = new BigDecimal(gcAmt);
                } catch (NumberFormatException e) {
                    Debug.logError(e, module);
                    errMsg = UtilProperties.getMessage(resource_error,"checkhelper.invalid_amount_for_gift_card", (cart != null ? cart.getLocale() : Locale.getDefault()));
                    errorMessages.add(errMsg);
                    gcFieldsOkay = false;
                }
            }

            if (gcFieldsOkay) {
                // store the gift card
                Map gcCtx = new HashMap();
                gcCtx.put("partyId", params.get("partyId"));
                gcCtx.put("cardNumber", gcNum);
                if (cart.isPinRequiredForGC(delegator)) {
                    gcCtx.put("pinNumber", gcPin);
                }
                gcCtx.put("userLogin", cart.getUserLogin());
                Map gcResult = null;
                try {
                    gcResult = dispatcher.runSync("createGiftCard", gcCtx);
                } catch (GenericServiceException e) {
                    Debug.logError(e, module);
                    errorMessages.add(e.getMessage());
                }
                if (gcResult != null) {
                    ServiceUtil.addErrors(errorMessages, errorMaps, gcResult);

                    if (errorMessages.size() == 0 && errorMaps.size() == 0) {
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
                    errMsg = UtilProperties.getMessage(resource_error,"checkhelper.problem_with_gift_card_information", (cart != null ? cart.getLocale() : Locale.getDefault()));
                    errorMessages.add(errMsg);
                }
            }
        } else {
            result = ServiceUtil.returnSuccess();
        }

        // see whether we need to return an error or not
        if (errorMessages.size() > 0) {
            result.put(ModelService.ERROR_MESSAGE_LIST, errorMessages);
            result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_ERROR);
        }
        if (errorMaps.size() > 0) {
            result.put(ModelService.ERROR_MESSAGE_MAP, errorMaps);
            result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_ERROR);
        }

        return result;
    }

    public Map<String, Object> createOrder(GenericValue userLogin) {
        return createOrder(userLogin, null, null, null, false, null, null);
    }

    // Create order event - uses createOrder service for processing
    public Map<String, Object> createOrder(GenericValue userLogin, String distributorId, String affiliateId,
            List trackingCodeOrders, boolean areOrderItemsExploded, String visitId, String webSiteId) {
        if (this.cart == null) {
            return null;
        }
        String orderId = this.cart.getOrderId();
        String supplierPartyId = (String) this.cart.getAttribute("supplierPartyId");
        String originOrderId = (String) this.cart.getAttribute("originOrderId");

        this.cart.clearAllItemStatus();

        BigDecimal grandTotal = this.cart.getGrandTotal();

        // store the order - build the context
        Map context = this.cart.makeCartMap(this.dispatcher, areOrderItemsExploded);

        //get the TrackingCodeOrder List
        context.put("trackingCodeOrders", trackingCodeOrders);

        if (distributorId != null) context.put("distributorId", distributorId);
        if (affiliateId != null) context.put("affiliateId", affiliateId);

        context.put("orderId", orderId);
        context.put("supplierPartyId", supplierPartyId);
        context.put("grandTotal", grandTotal);
        context.put("userLogin", userLogin);
        context.put("visitId", visitId);
        context.put("webSiteId", webSiteId);
        context.put("originOrderId", originOrderId);

        // need the partyId; don't use userLogin in case of an order via order mgr
        String partyId = this.cart.getPartyId();
        String productStoreId = cart.getProductStoreId();

        // store the order - invoke the service
        Map storeResult = null;

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
            Map messageMap = UtilMisc.toMap("service", service);
            String errMsg = UtilProperties.getMessage(resource_error, "checkhelper.could_not_create_order_invoking_service", messageMap, (cart != null ? cart.getLocale() : Locale.getDefault()));
            Debug.logError(e, errMsg, module);
            return ServiceUtil.returnError(errMsg);
        }

        // check for error message(s)
        if (ServiceUtil.isError(storeResult)) {
            String errMsg = UtilProperties.getMessage(resource_error, "checkhelper.did_not_complete_order_following_occurred", (cart != null ? cart.getLocale() : Locale.getDefault()));
            List resErrorMessages = new LinkedList();
            resErrorMessages.add(errMsg);
            resErrorMessages.add(ServiceUtil.getErrorMessage(storeResult));
            return ServiceUtil.returnError(resErrorMessages);
        }

        // ----------
        // If needed, the production runs are created and linked to the order lines.
        //
        Iterator orderItems = ((List)context.get("orderItems")).iterator();
        int counter = 0;
        while (orderItems.hasNext()) {
            GenericValue orderItem = (GenericValue) orderItems.next();
            String productId = orderItem.getString("productId");
            if (productId != null) {
                try {
                    // do something tricky here: run as the "system" user
                    // that can actually create and run a production run
                    GenericValue permUserLogin = delegator.findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", "system"));
                    GenericValue productStore = ProductStoreWorker.getProductStore(productStoreId, delegator);
                    GenericValue product = delegator.findByPrimaryKey("Product", UtilMisc.toMap("productId", productId));
                    if ("AGGREGATED_CONF".equals(product.getString("productTypeId"))) {
                        org.ofbiz.product.config.ProductConfigWrapper config = this.cart.findCartItem(counter).getConfigWrapper();
                        Map inputMap = new HashMap();
                        inputMap.put("config", config);
                        inputMap.put("facilityId", productStore.getString("inventoryFacilityId"));
                        inputMap.put("orderId", orderId);
                        inputMap.put("orderItemSeqId", orderItem.getString("orderItemSeqId"));
                        inputMap.put("quantity", orderItem.getBigDecimal("quantity"));
                        inputMap.put("userLogin", permUserLogin);

                        Map prunResult = dispatcher.runSync("createProductionRunFromConfiguration", inputMap);
                        if (ServiceUtil.isError(prunResult)) {
                            Debug.logError(ServiceUtil.getErrorMessage(prunResult) + " for input:" + inputMap, module);
                        }
                    }
                } catch (Exception e) {
                    String service = e.getMessage();
                    Map messageMap = UtilMisc.toMap("service", service);
                    String errMsg = UtilProperties.getMessage(resource_error, "checkhelper.could_not_create_order_invoking_service", messageMap, (cart != null ? cart.getLocale() : Locale.getDefault()));
                    Debug.logError(e, errMsg, module);
                    return ServiceUtil.returnError(errMsg);
                }
            }
            counter++;
        }
        // ----------

        // ----------
        // The status of the requirement associated to the shopping cart lines is set to "ordered".
        //
        Iterator shoppingCartItems = this.cart.items().iterator();
        while (shoppingCartItems.hasNext()) {
            ShoppingCartItem shoppingCartItem = (ShoppingCartItem)shoppingCartItems.next();
            String requirementId = shoppingCartItem.getRequirementId();
            if (requirementId != null) {
                try {
                    Map inputMap = UtilMisc.toMap("requirementId", requirementId, "statusId", "REQ_ORDERED");
                    inputMap.put("userLogin", userLogin);
                    // TODO: check service result for an error return
                    Map outMap = dispatcher.runSync("updateRequirement", inputMap);
                } catch (Exception e) {
                    String service = e.getMessage();
                    Map messageMap = UtilMisc.toMap("service", service);
                    String errMsg = UtilProperties.getMessage(resource_error, "checkhelper.could_not_create_order_invoking_service", messageMap, (cart != null ? cart.getLocale() : Locale.getDefault()));
                    Debug.logError(e, errMsg, module);
                    return ServiceUtil.returnError(errMsg);
                }
            }
        }
        // ----------

        // set the orderId for use by chained events
        Map result = ServiceUtil.returnSuccess();
        result.put("orderId", orderId);
        result.put("orderAdditionalEmails", this.cart.getOrderAdditionalEmails());

        // save the emails to the order
        List toBeStored = new LinkedList();

        GenericValue party = null;
        try {
            party = this.delegator.findByPrimaryKey("Party", UtilMisc.toMap("partyId", partyId));
        } catch (GenericEntityException e) {
            Debug.logWarning(e, UtilProperties.getMessage(resource_error,"OrderProblemsGettingPartyRecord", cart.getLocale()), module);
            party = null;
        }

        // create order contact mechs for the email address(s)
        if (party != null) {
            Iterator emailIter = UtilMisc.toIterator(ContactHelper.getContactMechByType(party, "EMAIL_ADDRESS", false));
            while (emailIter != null && emailIter.hasNext()) {
                GenericValue email = (GenericValue) emailIter.next();
                GenericValue orderContactMech = this.delegator.makeValue("OrderContactMech",
                        UtilMisc.toMap("orderId", orderId, "contactMechId", email.getString("contactMechId"), "contactMechPurposeTypeId", "ORDER_EMAIL"));
                toBeStored.add(orderContactMech);
            }
        }

        // create dummy contact mechs and order contact mechs for the additional emails
        String additionalEmails = this.cart.getOrderAdditionalEmails();
        List emailList = StringUtil.split(additionalEmails, ",");
        if (emailList == null) emailList = new ArrayList();
        Iterator eli = emailList.iterator();
        while (eli.hasNext()) {
            String email = (String) eli.next();
            String contactMechId = this.delegator.getNextSeqId("ContactMech");
            GenericValue contactMech = this.delegator.makeValue("ContactMech",
                    UtilMisc.toMap("contactMechId", contactMechId, "contactMechTypeId", "EMAIL_ADDRESS", "infoString", email));

            GenericValue orderContactMech = this.delegator.makeValue("OrderContactMech",
                    UtilMisc.toMap("orderId", orderId, "contactMechId", contactMechId, "contactMechPurposeTypeId", "ORDER_EMAIL"));
            toBeStored.add(contactMech);
            toBeStored.add(orderContactMech);
        }

        if (toBeStored.size() > 0) {
            try {
                if (Debug.verboseOn()) Debug.logVerbose("To Be Stored: " + toBeStored, module);
                this.delegator.storeAll(toBeStored);
            } catch (GenericEntityException e) {
                // not a fatal error; so just print a message
                Debug.logWarning(e, UtilProperties.getMessage(resource_error,"OrderProblemsStoringOrderEmailContactInformation", cart.getLocale()), module);
            }
        }

        return result;
    }

    public void calcAndAddTax() throws GeneralException {
        calcAndAddTax(null);
    }

    public void calcAndAddTax(GenericValue shipAddress) throws GeneralException {
        if (UtilValidate.isEmpty(cart.getShippingContactMechId()) && cart.getBillingAddress() == null && shipAddress == null) {
            return;
        }

        int shipGroups = this.cart.getShipGroupSize();
        for (int i = 0; i < shipGroups; i++) {
            Map shoppingCartItemIndexMap = new HashMap();
            Map serviceContext = this.makeTaxContext(i, shipAddress, shoppingCartItemIndexMap);
            List taxReturn = this.getTaxAdjustments(dispatcher, "calcTax", serviceContext);

            if (Debug.verboseOn()) Debug.logVerbose("ReturnList: " + taxReturn, module);
            ShoppingCart.CartShipInfo csi = cart.getShipInfo(i);
            List orderAdj = (List) taxReturn.get(0);
            List itemAdj = (List) taxReturn.get(1);

            // set the item adjustments
            if (itemAdj != null) {
                for (int x = 0; x < itemAdj.size(); x++) {
                    List adjs = (List) itemAdj.get(x);
                    ShoppingCartItem item = (ShoppingCartItem) shoppingCartItemIndexMap.get(Integer.valueOf(x));
                    if (adjs == null) {
                        adjs = new LinkedList();
                    }
                    csi.setItemInfo(item, adjs);
                    if (Debug.verboseOn()) Debug.logVerbose("Added item adjustments to ship group [" + i + " / " + x + "] - " + adjs, module);
                }
            }

            // need to manually clear the order adjustments
            csi.shipTaxAdj.clear();
            csi.shipTaxAdj.addAll(orderAdj);
        }
    }

    private Map makeTaxContext(int shipGroup, GenericValue shipAddress, Map shoppingCartItemIndexMap) throws GeneralException {
        ShoppingCart.CartShipInfo csi = cart.getShipInfo(shipGroup);
        int totalItems = csi.shipItemInfo.size();

        List product = new ArrayList(totalItems);
        List amount = new ArrayList(totalItems);
        List price = new ArrayList(totalItems);
        List shipAmt = new ArrayList(totalItems);

        // Debug.logInfo("====== makeTaxContext passed in shipAddress=" + shipAddress, module);

        Iterator<ShoppingCartItem> it = csi.shipItemInfo.keySet().iterator();
        for (int i = 0; i < totalItems; i++) {
            ShoppingCartItem cartItem = it.next();
            ShoppingCart.CartShipInfo.CartShipItemInfo itemInfo = csi.getShipItemInfo(cartItem);

            //Debug.logInfo("In makeTaxContext for item [" + i + "] in ship group [" + shipGroup + "] got cartItem: " + cartItem, module);
            //Debug.logInfo("In makeTaxContext for item [" + i + "] in ship group [" + shipGroup + "] got itemInfo: " + itemInfo, module);

            product.add(i, cartItem.getProduct());
            amount.add(i, cartItem.getItemSubTotal(itemInfo.quantity));
            price.add(i, cartItem.getBasePrice());
            shipAmt.add(i, BigDecimal.ZERO); // no per item shipping yet
            shoppingCartItemIndexMap.put(Integer.valueOf(i), cartItem);
        }

        //add promotion adjustments
        List allAdjustments = cart.getAdjustments();
        BigDecimal orderPromoAmt = OrderReadHelper.calcOrderPromoAdjustmentsBd(allAdjustments);

        BigDecimal shipAmount = csi.shipEstimate;
        if (shipAddress == null) {
            shipAddress = cart.getShippingAddress(shipGroup);
            // Debug.logInfo("====== makeTaxContext set shipAddress to cart.getShippingAddress(shipGroup): " + shipAddress, module);
        }

        // no shipping address; try the billing address
        if (shipAddress == null) {
            for (int i = 0; i < cart.selectedPayments(); i++) {
                ShoppingCart.CartPaymentInfo cpi = cart.getPaymentInfo(i);
                GenericValue billAddr = cpi.getBillingAddress(delegator);
                if (billAddr != null) {
                    shipAddress = billAddr;
                    Debug.logInfo("In makeTaxContext no shipping address, but found address with ID [" + shipAddress.get("contactMechId") + "] from payment method.", module);
                    break;
                }
            }
        }

        Map serviceContext = UtilMisc.toMap("productStoreId", cart.getProductStoreId());
        serviceContext.put("payToPartyId", cart.getBillFromVendorPartyId());
        serviceContext.put("billToPartyId", cart.getBillToCustomerPartyId());
        serviceContext.put("itemProductList", product);
        serviceContext.put("itemAmountList", amount);
        serviceContext.put("itemPriceList", price);
        serviceContext.put("itemShippingList", shipAmt);
        serviceContext.put("orderShippingAmount", shipAmount);
        serviceContext.put("shippingAddress", shipAddress);
        serviceContext.put("orderPromotionsAmount", orderPromoAmt);

        return serviceContext;
    }

    // Calc the tax adjustments.
    private List getTaxAdjustments(LocalDispatcher dispatcher, String taxService, Map serviceContext) throws GeneralException {
        Map serviceResult = null;

        try {
            serviceResult = dispatcher.runSync(taxService, serviceContext);
        } catch (GenericServiceException e) {
            Debug.logError(e, module);
            throw new GeneralException("Problem occurred in tax service (" + e.getMessage() + ")", e);
        }

        if (ServiceUtil.isError(serviceResult)) {
            throw new GeneralException(ServiceUtil.getErrorMessage(serviceResult));
        }

        // the adjustments (returned in order) from taxware.
        List orderAdj = (List) serviceResult.get("orderAdjustments");
        List itemAdj = (List) serviceResult.get("itemAdjustments");

        return UtilMisc.toList(orderAdj, itemAdj);
    }

    public Map<String, Object> processPayment(GenericValue productStore, GenericValue userLogin) throws GeneralException {
        return CheckOutHelper.processPayment(this.cart.getOrderId(), this.cart.getGrandTotal(), this.cart.getCurrency(), productStore, userLogin, false, false, dispatcher, delegator);
    }

    public Map<String, Object> processPayment(GenericValue productStore, GenericValue userLogin, boolean faceToFace) throws GeneralException {
        return CheckOutHelper.processPayment(this.cart.getOrderId(), this.cart.getGrandTotal(), this.cart.getCurrency(), productStore, userLogin, faceToFace, false, dispatcher, delegator);
    }

    public Map<String, Object> processPayment(GenericValue productStore, GenericValue userLogin, boolean faceToFace, boolean manualHold) throws GeneralException {
        return CheckOutHelper.processPayment(this.cart.getOrderId(), this.cart.getGrandTotal(), this.cart.getCurrency(), productStore, userLogin, faceToFace, manualHold, dispatcher, delegator);
    }

    public static Map<String, Object> processPayment(String orderId, BigDecimal orderTotal, String currencyUomId, GenericValue productStore, GenericValue userLogin, boolean faceToFace, boolean manualHold, LocalDispatcher dispatcher, Delegator delegator) throws GeneralException {
        // Get some payment related strings
        String DECLINE_MESSAGE = productStore.getString("authDeclinedMessage");
        String ERROR_MESSAGE = productStore.getString("authErrorMessage");
        String RETRY_ON_ERROR = productStore.getString("retryFailedAuths");
        if (RETRY_ON_ERROR == null) {
            RETRY_ON_ERROR = "Y";
        }

        List<GenericValue> allPaymentPreferences = null;
        try {
            allPaymentPreferences = delegator.findByAnd("OrderPaymentPreference", UtilMisc.toMap("orderId", orderId));
        } catch (GenericEntityException e) {
            throw new GeneralException("Problems getting payment preferences", e);
        }

        // filter out cancelled preferences
        List canExpr = UtilMisc.toList(EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "PAYMENT_CANCELLED"));
        allPaymentPreferences = EntityUtil.filterByAnd(allPaymentPreferences, canExpr);

        // check for online payment methods or in-hand payment types with verbal or external refs
        List exprs = UtilMisc.toList(EntityCondition.makeCondition("manualRefNum", EntityOperator.NOT_EQUAL, null));
        List manualRefPaymentPrefs = EntityUtil.filterByAnd(allPaymentPreferences, exprs);
        if (UtilValidate.isNotEmpty(manualRefPaymentPrefs)) {
            Iterator i = manualRefPaymentPrefs.iterator();
            while (i.hasNext()) {
                GenericValue opp = (GenericValue) i.next();
                Map authCtx = new HashMap();
                authCtx.put("orderPaymentPreference", opp);
                if (opp.get("paymentMethodId") == null) {
                    authCtx.put("serviceTypeEnum", "PRDS_PAY_EXTERNAL");
                }
                authCtx.put("processAmount", opp.getBigDecimal("maxAmount"));
                authCtx.put("authRefNum", opp.getString("manualRefNum"));
                authCtx.put("authResult", Boolean.TRUE);
                authCtx.put("userLogin", userLogin);
                authCtx.put("currencyUomId", currencyUomId);

                Map authResp = dispatcher.runSync("processAuthResult", authCtx);
                if (authResp != null && ServiceUtil.isError(authResp)) {
                    throw new GeneralException(ServiceUtil.getErrorMessage(authResp));
                }

                // approve the order
                OrderChangeHelper.approveOrder(dispatcher, userLogin, orderId, manualHold);

                if ("Y".equalsIgnoreCase(productStore.getString("manualAuthIsCapture"))) {
                    Map captCtx = new HashMap();
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

                    Map capResp = dispatcher.runSync("processCaptureResult", captCtx);
                    if (capResp != null && ServiceUtil.isError(capResp)) {
                        throw new GeneralException(ServiceUtil.getErrorMessage(capResp));
                    }
                }
            }
        }

        // check for a paypal express checkout needing completion
        List<EntityExpr> payPalExprs = UtilMisc.toList(
                EntityCondition.makeCondition("paymentMethodId", EntityOperator.NOT_EQUAL, null),
                EntityCondition.makeCondition("paymentMethodTypeId", "EXT_PAYPAL")
           );
        List<GenericValue> payPalPaymentPrefs = EntityUtil.filterByAnd(allPaymentPreferences, payPalExprs);
        if (UtilValidate.isNotEmpty(payPalPaymentPrefs)) {
            GenericValue payPalPaymentPref = EntityUtil.getFirst(payPalPaymentPrefs);
            ExpressCheckoutEvents.doExpressCheckout(productStore.getString("productStoreId"), orderId, payPalPaymentPref, userLogin, delegator, dispatcher);
        }

        // check for online payment methods needing authorization
        Map paymentFields = UtilMisc.toMap("statusId", "PAYMENT_NOT_AUTH");
        List onlinePaymentPrefs = EntityUtil.filterByAnd(allPaymentPreferences, paymentFields);

        // Check the payment preferences; if we have ANY w/ status PAYMENT_NOT_AUTH invoke payment service.
        // Invoke payment processing.
        if (UtilValidate.isNotEmpty(onlinePaymentPrefs)) {
            boolean autoApproveOrder = UtilValidate.isEmpty(productStore.get("autoApproveOrder")) || "Y".equalsIgnoreCase(productStore.getString("autoApproveOrder"));
            if (orderTotal.compareTo(BigDecimal.ZERO) == 0 && autoApproveOrder) {
                // if there is nothing to authorize; don't bother
                boolean ok = OrderChangeHelper.approveOrder(dispatcher, userLogin, orderId, manualHold);
                if (!ok) {
                    throw new GeneralException("Problem with order change; see above error");
                }
            }

            // now there should be something to authorize; go ahead
            Map paymentResult = null;
            try {
                // invoke the payment gateway service.
                paymentResult = dispatcher.runSync("authOrderPayments",
                        UtilMisc.<String, Object>toMap("orderId", orderId, "userLogin", userLogin), 180, false);
            } catch (GenericServiceException e) {
                Debug.logWarning(e, module);
                throw new GeneralException("Error in authOrderPayments service: " + e.toString(), e.getNested());
            }
            if (Debug.verboseOn()) Debug.logVerbose("Finsished w/ Payment Service", module);

            if (paymentResult != null && ServiceUtil.isError(paymentResult)) {
                throw new GeneralException(ServiceUtil.getErrorMessage(paymentResult));
            }

            // grab the customer messages -- only passed back in the case of an error or failure
            List messages = (List) paymentResult.get("authResultMsgs");

            if (paymentResult != null && paymentResult.containsKey("processResult")) {
                String authResp = (String) paymentResult.get("processResult");

                if (authResp.equals("FAILED")) {
                    // order was NOT approved
                    if (Debug.verboseOn()) Debug.logVerbose("Payment auth was NOT a success!", module);

                    boolean ok = OrderChangeHelper.rejectOrder(dispatcher, userLogin, orderId);
                    if (!ok) {
                        throw new GeneralException("Problem with order change; see above error");
                    }
                    if (UtilValidate.isEmpty(messages)) {
                        return ServiceUtil.returnError(DECLINE_MESSAGE);
                    } else {
                        return ServiceUtil.returnError(messages);
                    }
                } else if (authResp.equals("APPROVED")) {
                    // order WAS approved
                    if (Debug.verboseOn()) Debug.logVerbose("Payment auth was a success!", module);

                    // set the order and item status to approved
                    if (autoApproveOrder) {
                        boolean ok = OrderChangeHelper.approveOrder(dispatcher, userLogin, orderId, manualHold);
                        if (!ok) {
                            throw new GeneralException("Problem with order change; see above error");
                        }
                    }
                } else if (authResp.equals("ERROR")) {
                    // service failed
                    if (Debug.verboseOn()) Debug.logVerbose("Payment auth failed due to processor trouble.", module);
                    if (!faceToFace && "Y".equalsIgnoreCase(RETRY_ON_ERROR)) {
                        // never do this for a face to face purchase regardless of store setting
                        return ServiceUtil.returnSuccess(ERROR_MESSAGE);
                    } else {
                        boolean ok = OrderChangeHelper.cancelOrder(dispatcher, userLogin, orderId);
                        if (!ok) {
                            throw new GeneralException("Problem with order change; see above error");
                        }
                        if (UtilValidate.isEmpty(messages)) {
                            return ServiceUtil.returnError(ERROR_MESSAGE);
                        } else {
                            return ServiceUtil.returnError(messages);
                        }
                    }
                } else {
                    // should never happen
                    return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderPleaseContactCustomerServicePaymentReturnCodeUnknown", Locale.getDefault()));
                }
            } else {
                // result returned null == service failed
                if (Debug.verboseOn()) Debug.logVerbose("Payment auth failed due to processor trouble.", module);
                if (!faceToFace && "Y".equalsIgnoreCase(RETRY_ON_ERROR)) {
                    // never do this for a face to face purchase regardless of store setting
                    return ServiceUtil.returnSuccess(ERROR_MESSAGE);
                } else {
                    boolean ok = OrderChangeHelper.cancelOrder(dispatcher, userLogin, orderId);
                    if (!ok) {
                        throw new GeneralException("Problem with order change; see above error");
                    }
                    return ServiceUtil.returnError(ERROR_MESSAGE);
                }
            }
        } else {
            // Get the paymentMethodTypeIds - this will need to change when ecom supports multiple payments
            List cashCodPcBaExpr = UtilMisc.toList(EntityCondition.makeCondition("paymentMethodTypeId", EntityOperator.EQUALS, "CASH"),
                                           EntityCondition.makeCondition("paymentMethodTypeId", EntityOperator.EQUALS, "EXT_COD"),
                                           EntityCondition.makeCondition("paymentMethodTypeId", EntityOperator.EQUALS, "PERSONAL_CHECK"),
                                           EntityCondition.makeCondition("paymentMethodTypeId", EntityOperator.EQUALS, "EXT_BILLACT"));
            List cashCodPcBaPaymentPreferences = EntityUtil.filterByOr(allPaymentPreferences, cashCodPcBaExpr);

            if (UtilValidate.isNotEmpty(cashCodPcBaPaymentPreferences) &&
                    UtilValidate.isNotEmpty(allPaymentPreferences) &&
                    cashCodPcBaPaymentPreferences.size() == allPaymentPreferences.size()) {

                //if there are Check type, approve the order only if it is face to face
                List checkPareferences = EntityUtil.filterByAnd(cashCodPcBaPaymentPreferences, UtilMisc.toMap("paymentMethodTypeId", "PERSONAL_CHECK"));
                if (UtilValidate.isNotEmpty(checkPareferences)) {
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

            } else {
                // There is nothing to do, we just treat this as a success
            }
        }

        // check to see if we should auto-invoice/bill
        if (faceToFace) {
            if (Debug.verboseOn()) Debug.logVerbose("Face-To-Face Sale - " + orderId, module);
            CheckOutHelper.adjustFaceToFacePayment(orderId, orderTotal, allPaymentPreferences, userLogin, delegator);
            boolean ok = OrderChangeHelper.completeOrder(dispatcher, userLogin, orderId);
            if (Debug.verboseOn()) Debug.logVerbose("Complete Order Result - " + ok, module);
            if (!ok) {
                throw new GeneralException("Problem with order change; see error logs");
            }
        }
        return ServiceUtil.returnSuccess();
    }

    public static void adjustFaceToFacePayment(String orderId, BigDecimal cartTotal, List allPaymentPrefs, GenericValue userLogin, Delegator delegator) throws GeneralException {
        BigDecimal prefTotal = BigDecimal.ZERO;
        if (allPaymentPrefs != null) {
            Iterator i = allPaymentPrefs.iterator();
            while (i.hasNext()) {
                GenericValue pref = (GenericValue) i.next();
                BigDecimal maxAmount = pref.getBigDecimal("maxAmount");
                if (maxAmount == null) maxAmount = BigDecimal.ZERO;
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

    public Map checkOrderBlacklist(GenericValue userLogin) {
        if (cart == null) {
            return ServiceUtil.returnSuccess("success");
        }
        GenericValue shippingAddressObj = this.cart.getShippingAddress();
        if (shippingAddressObj == null) {
            return ServiceUtil.returnSuccess("success");
        }
        String shippingAddress = UtilFormatOut.checkNull(shippingAddressObj.getString("address1")).toUpperCase();
        shippingAddress = UtilFormatOut.makeSqlSafe(shippingAddress);
        List exprs = UtilMisc.toList(EntityCondition.makeCondition(
                EntityCondition.makeCondition(EntityFunction.UPPER_FIELD("blacklistString"), EntityOperator.EQUALS, EntityFunction.UPPER(shippingAddress)),
                EntityOperator.AND,
                EntityCondition.makeCondition("orderBlacklistTypeId", EntityOperator.EQUALS, "BLACKLIST_ADDRESS")));
        String errMsg=null;

        List paymentMethods = this.cart.getPaymentMethods();
        Iterator i = paymentMethods.iterator();
        while (i.hasNext()) {
            GenericValue paymentMethod = (GenericValue) i.next();
            if ((paymentMethod != null) && ("CREDIT_CARD".equals(paymentMethod.getString("paymentMethodTypeId")))) {
                GenericValue creditCard = null;
                GenericValue billingAddress = null;
                try {
                    creditCard = paymentMethod.getRelatedOne("CreditCard");
                    if (creditCard != null)
                        billingAddress = creditCard.getRelatedOne("PostalAddress");
                } catch (GenericEntityException e) {
                    Debug.logError(e, "Problems getting credit card from payment method", module);
                    errMsg = UtilProperties.getMessage(resource_error,"checkhelper.problems_reading_database", (cart != null ? cart.getLocale() : Locale.getDefault()));
                    return ServiceUtil.returnError(errMsg);
                }
                if (creditCard != null) {
                    String creditCardNumber = UtilFormatOut.checkNull(creditCard.getString("cardNumber"));
                    exprs.add(EntityCondition.makeCondition(
                            EntityCondition.makeCondition("blacklistString", EntityOperator.EQUALS, creditCardNumber), EntityOperator.AND,
                            EntityCondition.makeCondition("orderBlacklistTypeId", EntityOperator.EQUALS, "BLACKLIST_CREDITCARD")));
                }
                if (billingAddress != null) {
                    String address = UtilFormatOut.checkNull(billingAddress.getString("address1").toUpperCase());
                    address = UtilFormatOut.makeSqlSafe(address);
                    exprs.add(EntityCondition.makeCondition(
                            EntityCondition.makeCondition(EntityFunction.UPPER_FIELD("blacklistString"), EntityOperator.EQUALS, EntityFunction.UPPER(address)),
                            EntityOperator.AND,
                            EntityCondition.makeCondition("orderBlacklistTypeId", EntityOperator.EQUALS, "BLACKLIST_ADDRESS")));
                }
            }
        }

        List blacklistFound = null;
        if (exprs.size() > 0) {
            try {
                EntityConditionList ecl = EntityCondition.makeCondition(exprs, EntityOperator.AND);
                blacklistFound = this.delegator.findList("OrderBlacklist", ecl, null, null, null, false);
            } catch (GenericEntityException e) {
                Debug.logError(e, "Problems with OrderBlacklist lookup.", module);
                errMsg = UtilProperties.getMessage(resource_error,"checkhelper.problems_reading_database", (cart != null ? cart.getLocale() : Locale.getDefault()));
                return ServiceUtil.returnError(errMsg);
            }
        }

        if (UtilValidate.isNotEmpty(blacklistFound)) {
            return ServiceUtil.returnFailure(UtilProperties.getMessage(resource_error,"OrderFailed", (cart != null ? cart.getLocale() : Locale.getDefault())));
        } else {
            return ServiceUtil.returnSuccess("success");
        }
    }

    public Map failedBlacklistCheck(GenericValue userLogin, GenericValue productStore) {
        Map result;
        String errMsg=null;
        String REJECT_MESSAGE = productStore.getString("authFraudMessage");
        String orderId = this.cart.getOrderId();

        try {
            if (userLogin != null) {
                // nuke the userlogin
                userLogin.set("enabled", "N");
                userLogin.store();
            } else {
                userLogin = delegator.findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", "system"));
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            errMsg = UtilProperties.getMessage(resource_error,"checkhelper.database_error", (cart != null ? cart.getLocale() : Locale.getDefault()));
            result = ServiceUtil.returnError(errMsg);
            return result;
        }

        // set the order/item status - reverse inv
        OrderChangeHelper.rejectOrder(dispatcher, userLogin, orderId);
        result = ServiceUtil.returnSuccess();
        result.put(ModelService.ERROR_MESSAGE, REJECT_MESSAGE);

        // wipe the cart and session
        this.cart.clear();
        return result;
    }

    public Map checkExternalPayment(String orderId) {
        Map result;
        String errMsg=null;
        // warning there can only be ONE payment preference for this to work
        // you cannot accept multiple payment type when using an external gateway
        GenericValue orderHeader = null;
        try {
            orderHeader = this.delegator.findByPrimaryKey("OrderHeader", UtilMisc.toMap("orderId", orderId));
        } catch (GenericEntityException e) {
            Debug.logError(e, "Problems getting order header", module);
            errMsg = UtilProperties.getMessage(resource_error,"checkhelper.problems_getting_order_header", (cart != null ? cart.getLocale() : Locale.getDefault()));
            result = ServiceUtil.returnError(errMsg);
            return result;
        }
        if (orderHeader != null) {
            List paymentPrefs = null;
            try {
                paymentPrefs = orderHeader.getRelated("OrderPaymentPreference");
            } catch (GenericEntityException e) {
                Debug.logError(e, "Problems getting order payments", module);
                errMsg = UtilProperties.getMessage(resource_error,"checkhelper.problems_getting_payment_preference", (cart != null ? cart.getLocale() : Locale.getDefault()));
                result = ServiceUtil.returnError(errMsg);
                return result;
            }
            if (UtilValidate.isNotEmpty(paymentPrefs)) {
                if (paymentPrefs.size() > 1) {
                    Debug.logError("Too many payment preferences, you cannot have more then one when using external gateways", module);
                }
                GenericValue paymentPreference = EntityUtil.getFirst(paymentPrefs);
                String paymentMethodTypeId = paymentPreference.getString("paymentMethodTypeId");
                if (paymentMethodTypeId.startsWith("EXT_")) {
                    // PayPal with a PaymentMethod is not an external payment method
                    if (!(paymentMethodTypeId.equals("EXT_PAYPAL") && UtilValidate.isNotEmpty(paymentPreference.getString("paymentMethodId")))) {
                        String type = paymentMethodTypeId.substring(4);
                        result = ServiceUtil.returnSuccess();
                        result.put("type", type.toLowerCase());
                        return result;
                    }
                }
            }
            result = ServiceUtil.returnSuccess();
            result.put("type", "none");
            return result;
        } else {
            errMsg = UtilProperties.getMessage(resource_error,"checkhelper.problems_getting_order_header", (cart != null ? cart.getLocale() : Locale.getDefault()));
            result = ServiceUtil.returnError(errMsg);
            result.put("type", "error");
            return result;
        }
    }

    /**
     * Sets the shipping contact mechanism for a given ship group on the cart
     *
     * @param shipGroupIndex The index of the ship group in the cart
     * @param shippingContactMechId The identifier of the contact
     * @return A Map conforming to the OFBiz Service conventions containing
     * any error messages
     */
    public Map finalizeOrderEntryShip(int shipGroupIndex, String shippingContactMechId, String supplierPartyId) {
        Map result;
        String errMsg=null;
        //Verify the field is valid
        if (UtilValidate.isNotEmpty(shippingContactMechId)) {
            this.cart.setShippingContactMechId(shipGroupIndex, shippingContactMechId);
            if (UtilValidate.isNotEmpty(supplierPartyId)) {
                this.cart.setSupplierPartyId(shipGroupIndex, supplierPartyId);
            }
            result = ServiceUtil.returnSuccess();
        } else {
            errMsg = UtilProperties.getMessage(resource_error,"checkhelper.enter_shipping_address", (cart != null ? cart.getLocale() : Locale.getDefault()));
            result = ServiceUtil.returnError(errMsg);
        }

        return result;
    }

    /**
     * Sets the options associated with the order for a given ship group
     *
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
    public Map finalizeOrderEntryOptions(int shipGroupIndex, String shippingMethod, String shippingInstructions, String maySplit,
            String giftMessage, String isGift, String internalCode, String shipBeforeDate, String shipAfterDate, String orderAdditionalEmails) {
        this.cart.setOrderAdditionalEmails(orderAdditionalEmails);
        return finalizeOrderEntryOptions(shipGroupIndex, shippingMethod, shippingInstructions, maySplit, giftMessage, isGift, internalCode, shipBeforeDate, shipAfterDate, null, null);
    }
    public Map finalizeOrderEntryOptions(int shipGroupIndex, String shippingMethod, String shippingInstructions, String maySplit,
            String giftMessage, String isGift, String internalCode, String shipBeforeDate, String shipAfterDate, String internalOrderNotes, String shippingNotes) {

        Map result = ServiceUtil.returnSuccess();

        String errMsg=null;
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
            errMsg = UtilProperties.getMessage(resource_error,"checkhelper.select_shipping_method", (cart != null ? cart.getLocale() : Locale.getDefault()));
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
               this.cart.setShipBeforeDate(shipGroupIndex, (Timestamp) ObjectType.simpleTypeConvert(shipBeforeDate, "Timestamp", null, null));
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
               this.cart.setShipAfterDate(shipGroupIndex, (Timestamp) ObjectType.simpleTypeConvert(shipAfterDate,"Timestamp", null, null));
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
     *
     * @param checkOutPaymentId The payment ID to be associated with the cart
     * @return A Map conforming to the OFBiz Service conventions containing
     * any error messages.
     */
    public Map finalizeOrderEntryPayment(String checkOutPaymentId, BigDecimal amount, boolean singleUse, boolean append) {
        Map result = ServiceUtil.returnSuccess();

        if (UtilValidate.isNotEmpty(checkOutPaymentId)) {
            if (!append) {
                cart.clearPayments();
            }
            cart.addPaymentAmount(checkOutPaymentId, amount, singleUse);
        }

        return result;
    }

    public static BigDecimal availableAccountBalance(String billingAccountId, LocalDispatcher dispatcher) {
        if (billingAccountId == null) return BigDecimal.ZERO;
        try {
            Map res = dispatcher.runSync("calcBillingAccountBalance", UtilMisc.toMap("billingAccountId", billingAccountId));
            BigDecimal availableBalance = (BigDecimal) res.get("accountBalance");
            if (availableBalance != null) {
                return availableBalance;
            }
        } catch (GenericServiceException e) {
            Debug.logError(e, module);
        }
        return BigDecimal.ZERO;
    }

    public BigDecimal availableAccountBalance(String billingAccountId) {
        return availableAccountBalance(billingAccountId, dispatcher);
    }

    public Map makeBillingAccountMap(List paymentPrefs) {
        Map accountMap = new HashMap();
        if (paymentPrefs != null) {
            Iterator i = accountMap.keySet().iterator();
            while (i.hasNext()) {
                GenericValue pp = (GenericValue) i.next();
                if (pp.get("billingAccountId") != null) {
                    accountMap.put(pp.getString("billingAccountId"), pp.getBigDecimal("maxAmount"));
                }
            }
        }
        return accountMap;
    }

    public Map<String, Object> validatePaymentMethods() {
        String errMsg = null;
        String billingAccountId = cart.getBillingAccountId();
        BigDecimal billingAccountAmt = cart.getBillingAccountAmount();
        BigDecimal availableAmount = this.availableAccountBalance(billingAccountId);
        if (billingAccountAmt.compareTo(availableAmount) > 0) {
            Map messageMap = UtilMisc.toMap("billingAccountId", billingAccountId);
            errMsg = UtilProperties.getMessage(resource_error, "checkevents.not_enough_available_on_account", messageMap, (cart != null ? cart.getLocale() : Locale.getDefault()));
            return ServiceUtil.returnError(errMsg);
        }

        // payment by billing account only requires more checking
        List paymentMethods = cart.getPaymentMethodIds();
        List paymentTypes = cart.getPaymentMethodTypeIds();
        if (paymentTypes.contains("EXT_BILLACT") && paymentTypes.size() == 1 && paymentMethods.size() == 0) {
            if (cart.getGrandTotal().compareTo(availableAmount) > 0) {
                errMsg = UtilProperties.getMessage(resource_error, "checkevents.insufficient_credit_available_on_account", (cart != null ? cart.getLocale() : Locale.getDefault()));
                return ServiceUtil.returnError(errMsg);
            }
        }

        // validate any gift card balances
        this.validateGiftCardAmounts();

        // update the selected payment methods amount with valid numbers
        if (paymentMethods != null) {
            List nullPaymentIds = new ArrayList();
            Iterator i = paymentMethods.iterator();
            while (i.hasNext()) {
                String paymentMethodId = (String) i.next();
                BigDecimal paymentAmount = cart.getPaymentAmount(paymentMethodId);
                if (paymentAmount == null || paymentAmount.compareTo(BigDecimal.ZERO) == 0) {
                    if (Debug.verboseOn()) Debug.logVerbose("Found null paymentMethodId - " + paymentMethodId, module);
                    nullPaymentIds.add(paymentMethodId);
                }
            }
            Iterator npi = nullPaymentIds.iterator();
            while (npi.hasNext()) {
                String paymentMethodId = (String) npi.next();
                BigDecimal selectedPaymentTotal = cart.getPaymentTotal();
                BigDecimal requiredAmount = cart.getGrandTotal();
                BigDecimal newAmount = requiredAmount.subtract(selectedPaymentTotal);
                boolean setOverflow = false;

                ShoppingCart.CartPaymentInfo info = cart.getPaymentInfo(paymentMethodId);

                if (Debug.verboseOn()) Debug.logVerbose("Remaining total is - " + newAmount, module);
                if (newAmount.compareTo(BigDecimal.ZERO) > 0) {
                    info.amount = newAmount;
                    if (Debug.verboseOn()) Debug.logVerbose("Set null paymentMethodId - " + info.paymentMethodId + " / " + info.amount, module);
                } else {
                    info.amount = BigDecimal.ZERO;
                    if (Debug.verboseOn()) Debug.logVerbose("Set null paymentMethodId - " + info.paymentMethodId + " / " + info.amount, module);
                }
                if (!setOverflow) {
                    info.overflow = setOverflow = true;
                    if (Debug.verboseOn()) Debug.logVerbose("Set overflow flag on payment - " + info.paymentMethodId, module);
                }
            }
        }

        // verify the selected payment method amounts will cover the total
        BigDecimal reqAmtPreParse = cart.getGrandTotal().subtract(cart.getBillingAccountAmount());
        BigDecimal selectedPaymentTotal = cart.getPaymentTotal();

        BigDecimal requiredAmount = reqAmtPreParse.setScale(scale, rounding);
        if (UtilValidate.isNotEmpty(paymentMethods) && requiredAmount.compareTo(selectedPaymentTotal) > 0) {
            Debug.logError("Required Amount : " + requiredAmount + " / Selected Amount : " + selectedPaymentTotal, module);
            errMsg = UtilProperties.getMessage(resource_error, "checkevents.payment_not_cover_this_order", (cart != null ? cart.getLocale() : Locale.getDefault()));
            return ServiceUtil.returnError(errMsg);
        }
        if (UtilValidate.isNotEmpty(paymentMethods) && requiredAmount.compareTo(selectedPaymentTotal) < 0) {
            BigDecimal changeAmount = selectedPaymentTotal.subtract(requiredAmount);
            if (!paymentTypes.contains("CASH")) {
                Debug.logError("Change Amount : " + changeAmount + " / No cash.", module);
                errMsg = UtilProperties.getMessage(resource_error, "checkhelper.change_returned_cannot_be_greater_than_cash", (cart != null ? cart.getLocale() : Locale.getDefault()));
                return ServiceUtil.returnError(errMsg);
            } else {
                int cashIndex = paymentTypes.indexOf("CASH");
                String cashId = (String) paymentTypes.get(cashIndex);
                BigDecimal cashAmount = cart.getPaymentAmount(cashId);
                if (cashAmount.compareTo(changeAmount) < 0) {
                    Debug.logError("Change Amount : " + changeAmount + " / Cash Amount : " + cashAmount, module);
                    errMsg = UtilProperties.getMessage(resource_error, "checkhelper.change_returned_cannot_be_greater_than_cash", (cart != null ? cart.getLocale() : Locale.getDefault()));
                    return ServiceUtil.returnError(errMsg);
                }
            }
        }
        return ServiceUtil.returnSuccess();
    }

    public void validateGiftCardAmounts() {
        // get the product store
        GenericValue productStore = ProductStoreWorker.getProductStore(cart.getProductStoreId(), delegator);
        if (productStore != null && !"Y".equalsIgnoreCase(productStore.getString("checkGcBalance"))) {
            return;
        }

        // get the payment config
        String paymentConfig = ProductStoreWorker.getProductStorePaymentProperties(delegator, cart.getProductStoreId(), "GIFT_CARD", null, true);
        String giftCardType = UtilProperties.getPropertyValue(paymentConfig, "", "ofbiz");
        String balanceField = null;

        // get the gift card objects to check
        Iterator i = cart.getGiftCards().iterator();
        while (i.hasNext()) {
            GenericValue gc = (GenericValue) i.next();
            Map gcBalanceMap = null;
            BigDecimal gcBalance = BigDecimal.ZERO;
            try {
                Map ctx = UtilMisc.toMap("userLogin", cart.getUserLogin());
                ctx.put("currency", cart.getCurrency());
                if ("ofbiz".equalsIgnoreCase(giftCardType)) {
                    balanceField = "balance";
                    ctx.put("cardNumber", gc.getString("cardNumber"));
                    ctx.put("pinNumber", gc.getString("pinNumber"));
                    gcBalanceMap = dispatcher.runSync("checkGiftCertificateBalance", ctx);
                }
                if ("valuelink".equalsIgnoreCase(giftCardType)) {
                    balanceField = "balance";
                    ctx.put("paymentConfig", paymentConfig);
                    ctx.put("cardNumber", gc.getString("cardNumber"));
                    ctx.put("pin", gc.getString("pinNumber"));
                    gcBalanceMap = dispatcher.runSync("balanceInquireGiftCard", ctx);
                }
            } catch (GenericServiceException e) {
                Debug.logError(e, module);
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
