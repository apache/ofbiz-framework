/**
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
**/

package org.ofbiz.googlecheckout;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilNumber;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.util.EntityQuery;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.entity.util.EntityUtilProperties;
import org.ofbiz.order.shoppingcart.ShoppingCart;
import org.ofbiz.order.shoppingcart.ShoppingCartItem;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.ServiceUtil;

import com.google.checkout.CheckoutException;
import com.google.checkout.CheckoutResponse;
import com.google.checkout.EnvironmentType;
import com.google.checkout.MerchantInfo;
import com.google.checkout.checkout.CarrierPickup;
import com.google.checkout.checkout.CheckoutShoppingCartRequest;
import com.google.checkout.checkout.Item;
import com.google.checkout.checkout.TaxArea;
import com.google.checkout.orderprocessing.AddMerchantOrderNumberRequest;
import com.google.checkout.orderprocessing.ArchiveOrderRequest;
import com.google.checkout.orderprocessing.AuthorizeOrderRequest;
import com.google.checkout.orderprocessing.CancelOrderRequest;
import com.google.checkout.orderprocessing.ChargeOrderRequest;
import com.google.checkout.orderprocessing.RefundOrderRequest;
import com.google.checkout.orderprocessing.UnarchiveOrderRequest;
import com.google.checkout.orderprocessing.lineitem.CancelItemsRequest;
import com.google.checkout.orderprocessing.lineitem.ReturnItemsRequest;
import com.google.checkout.orderprocessing.lineitem.ShipItemsRequest;

public class GoogleRequestServices {
    private static final String resource = "GoogleCheckoutUiLabels";
    private static final String module = GoogleRequestServices.class.getName();
    private static int decimals = UtilNumber.getBigDecimalScale("invoice.decimals");
    private static int rounding = UtilNumber.getBigDecimalRoundingMode("invoice.rounding");

    public static Map<String, Object> sendShoppingCartRequest(DispatchContext dctx, Map<String, ? extends Object> context) {
        ShoppingCart cart = (ShoppingCart) context.get("shoppingCart");
        String productStoreId = cart.getProductStoreId();
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get("locale");
        GenericValue googleCfg = getGoogleConfiguration(delegator, productStoreId);
        MerchantInfo mInfo = getMerchantInfo(delegator, productStoreId);
        if (mInfo == null) {
            Debug.logError("Invalid Google Chechout Merchant settings, check your configuration!", module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "GoogleCheckoutConfigurationError", locale));
        }

        // the checkout request object
        CheckoutShoppingCartRequest req = new CheckoutShoppingCartRequest(mInfo, 300);
        String requestAuthStr = googleCfg.getString("requestAuthDetails");
        if (requestAuthStr == null) {
            requestAuthStr = "Y";
        }
        boolean requestAuth = "Y".equalsIgnoreCase(requestAuthStr) ? true : false;
        req.setRequestInitialAuthDetails(requestAuth); // send the auth notification

        String sendPromoItemStr = googleCfg.getString("sendPromoItems");
        if (sendPromoItemStr == null) {
            sendPromoItemStr = "Y";
        }
        boolean sendPromoItems = "Y".equalsIgnoreCase(sendPromoItemStr) ? true : false;

        // add the items
        List<ShoppingCartItem> items = cart.items();
        for (ShoppingCartItem item : items) {
            if (!item.getIsPromo() || sendPromoItems) {
                Item i = new Item();
                i.setItemName(item.getName());
                i.setItemDescription(item.getDescription());
                i.setMerchantItemId(item.getProductId());
                i.setQuantity(item.getQuantity().intValue());
                i.setUnitPriceAmount(item.getBasePrice().floatValue());
                i.setUnitPriceCurrency(cart.getCurrency());
                //i.setItemWeight(item.getWeight().floatValue()); // must convert weight to Lb
                if (!item.taxApplies()) {
                    i.setTaxTableSelector("tax_exempt");
                }
                req.addItem(i);
            }
        }

        // flow support URLs
        String contShoppingUrl = EntityUtilProperties.getPropertyValue("googleCheckout", "continueShoppingUrl", delegator);
        String editCartUrl = EntityUtilProperties.getPropertyValue("googleCheckout", "editCartUrl", delegator);
        req.setContinueShoppingUrl(contShoppingUrl);
        req.setEditCartUrl(editCartUrl);

        // setup exempt tax support
        TaxArea exemptArea = new TaxArea();
        exemptArea.addWorldArea();
        req.addAlternateTaxRule("tax_exempt", true, 0, exemptArea);

        // setup default tax table
        // TODO: implement this; for now use the tax table in Google Checkout Settings

        // setup shipping options support
        List<GenericValue> shippingOptions = null;
        try {
            shippingOptions = EntityQuery.use(delegator).from("GoogleCoShippingMethod").where("productStoreId", productStoreId).queryList();
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }
        if (UtilValidate.isNotEmpty(shippingOptions)) {
            for (GenericValue option : shippingOptions) {
                String shippingName = option.getString("shipmentMethodName");
                Double amount = option.getDouble("amount");
                if (amount == null) {
                    amount = 0.0;
                }
                if ("GOOGLE_FLAT_RATE".equals(option.getString("methodTypeEnumId"))) {
                    req.addFlatRateShippingMethod(shippingName, amount.floatValue());
                } else if ("GOOGLE_MERCHANT_CALC".equals(option.getString("methodTypeEnumId"))) {
                    req.addMerchantCalculatedShippingMethod(shippingName, amount.floatValue());
                } else if ("GOOGLE_PICKUP".equals(option.getString("methodTypeEnumId"))) {
                    req.addPickupShippingMethod(shippingName, amount.floatValue());
                } else if ("GOOGLE_CARRIER_CALC".equals(option.getString("methodTypeEnumId"))) {
                    String carrierPartyId = option.getString("carrierPartyId");

                    Double additionalAmount = option.getDouble("additionalAmount");
                    Double additionalPercent = option.getDouble("additionalPercent");
                    if (additionalAmount == null) {
                        additionalAmount = 0.0;
                    }
                    if (additionalPercent == null) {
                        additionalPercent = 0.0;
                    }

                    String shippingCompany = null;
                    if ("ups".equalsIgnoreCase(carrierPartyId)) {
                        shippingCompany = "UPS";
                    } else if ("fedex".equalsIgnoreCase(carrierPartyId)) {
                        shippingCompany = "FedEx";
                    } else if ("usps".equalsIgnoreCase(carrierPartyId)) {
                        shippingCompany = "USPS";
                    }
                    if (shippingCompany == null) {
                        return ServiceUtil.returnError(UtilProperties.getMessage(resource, "GoogleCheckoutShippingConfigurationInvalid", locale));
                    }
                    req.addCarrierCalculatedShippingOption(amount.floatValue(), shippingCompany, CarrierPickup.REGULAR_PICKUP, shippingName, additionalAmount.floatValue(), additionalPercent.floatValue());
                }
            }
        }

        // merchant stuff
        String acceptCouponStr = googleCfg.getString("acceptCoupons");
        if (acceptCouponStr == null) {
            acceptCouponStr = "N";
        }
        boolean acceptCoupons = "Y".equalsIgnoreCase(acceptCouponStr) ? true : false;

        String acceptCertStr = googleCfg.getString("acceptGiftCerts");
        if (acceptCertStr == null) {
            acceptCertStr = "N";
        }
        boolean acceptGiftCerts = "Y".equalsIgnoreCase(acceptCertStr) ? true : false;

        if (acceptCoupons || acceptGiftCerts) {
            req.setAcceptMerchantCoupons(acceptCoupons);
            req.setAcceptMerchantGiftCertificates(acceptGiftCerts);

            // TODO: merchant calc support needs to be implemented if these are ever TRUE
        }

        String requestPhoneStr = googleCfg.getString("requestPhone");
        if (requestPhoneStr == null) {
            requestPhoneStr = "Y";
        }
        boolean requestPhone = "Y".equalsIgnoreCase(requestPhoneStr) ? true : false;
        req.setRequestBuyerPhoneNumber(requestPhone);

        // send the request
        CheckoutResponse resp = null;
        try {
            Debug.logInfo("Sending XML to Google:\n\n" + req.getXmlPretty() + "\n\n", module);
            resp = req.send();
        } catch (CheckoutException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }
        if (resp == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "GoogleCheckoutResponseIsNull", locale));
        }
        if (!resp.isValidRequest()) {
            Debug.logError("Error returned from Google: " + resp.getErrorMessage(), module);
            return ServiceUtil.returnError(resp.getErrorMessage());
        }

        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put("redirect", resp.getRedirectUrl());
        return result;
    }

    public static Map<String, Object> sendOrderNumberRequest(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        String orderId = (String) context.get("orderId");
        GenericValue order = findGoogleOrder(delegator, orderId);
        if (order != null) {
            MerchantInfo mInfo = getMerchantInfo(delegator, getProductStoreFromOrder(order));
            if (mInfo != null) {
                String externalId = order.getString("externalId");
                AddMerchantOrderNumberRequest aor = new AddMerchantOrderNumberRequest(mInfo, externalId, orderId);
                try {
                    aor.send();
                } catch (CheckoutException e) {
                    Debug.logError(e, module);
                }
            }
        }

        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> sendAuthorizeRequest(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        String orderId = (String) context.get("orderId");
        GenericValue order = findGoogleOrder(delegator, orderId);
        if (order != null) {
            MerchantInfo mInfo = getMerchantInfo(delegator, getProductStoreFromOrder(order));
            if (mInfo != null) {
                String externalId = order.getString("externalId");
                AuthorizeOrderRequest aor = new AuthorizeOrderRequest(mInfo, externalId);
                try {
                    aor.send();
                } catch (CheckoutException e) {
                    Debug.logError(e, module);
                }
            }
        }

        return ServiceUtil.returnSuccess();
    }

    // trigger on captureOrderPayments
    public static Map<String, Object> sendChargeRequest(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        String orderId = (String) context.get("orderId");
        GenericValue order = findGoogleOrder(delegator, orderId);
        if (order != null) {
            MerchantInfo mInfo = getMerchantInfo(delegator, getProductStoreFromOrder(order));
            if (mInfo != null) {
                String externalId = order.getString("externalId");
                Double amountToCharge = (Double) context.get("captureAmount");
                if (amountToCharge == null || amountToCharge == 0) {
                    amountToCharge = order.getDouble("grandTotal"); // captureAmount 0 means capture all??
                }
                if (amountToCharge > 0) {
                    ChargeOrderRequest cor = new ChargeOrderRequest(mInfo, externalId, amountToCharge.floatValue());
                    try {
                        cor.send();
                    } catch (CheckoutException e) {
                        Debug.logError(e, module);
                    }
                }
            }
        }

        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> sendReturnRequest(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        String returnId = (String) context.get("returnId");

        // sort by order
        Map<String, BigDecimal> toRefund = new HashMap<String, BigDecimal>();
        Map<String, List<String>> toReturn = new HashMap<String, List<String>>();
        BigDecimal refundTotal = new BigDecimal(0.0);

        List<GenericValue> returnItems = null;
        try {
            returnItems = EntityQuery.use(delegator).from("ReturnItem").where("returnId", returnId).queryList();
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }

        // go through the items and sort them by order
        if (UtilValidate.isNotEmpty(returnItems)) {
            for (GenericValue returnItem : returnItems) {
                String orderId = returnItem.getString("orderId");
                GenericValue order = findGoogleOrder(delegator, orderId);

                if (order != null) {
                    refundTotal = toRefund.get(orderId);
                    if (refundTotal == null) {
                        refundTotal = new BigDecimal(0.0);
                    }
                    List<String> items = toReturn.get(orderId);
                    if (items == null) {
                        items = new LinkedList<String>();
                    }

                    // get the values from the return item
                    BigDecimal returnQty = returnItem.getBigDecimal("returnQuantity");
                    BigDecimal returnPrice = returnItem.getBigDecimal("returnPrice").multiply(returnQty);
                    String productId = returnItem.getString("productId");

                    // only look at refund returns to calculate the refund amount
                    if ("RTN_REFUND".equals(returnItem.getString("returnTypeId"))) {
                        if (returnPrice.doubleValue() > 0) {
                            refundTotal = refundTotal.add(returnPrice).setScale(decimals, rounding);
                            Debug.logInfo("Added [" + returnPrice + "] to refund total for order #" + orderId + " : " + refundTotal, module);
                        }
                    }
                    if (productId != null) {
                        items.add(productId);
                    }

                    // update the map values
                    toRefund.put(orderId, refundTotal);
                    toReturn.put(orderId, items);
                }
            }
        }

        // create the return items request
        for (String returnOrderId : toReturn.keySet()) {
            GenericValue gOrder = findGoogleOrder(delegator, returnOrderId);
            if (gOrder != null) {
                MerchantInfo mInfo = getMerchantInfo(delegator, getProductStoreFromOrder(gOrder));
                if (mInfo != null) {
                    ReturnItemsRequest rir = new ReturnItemsRequest(mInfo, gOrder.getString("externalId"));
                    List<String> items = toReturn.get(returnOrderId);
                    for (String item : items) {
                        rir.addItem(item);
                    }
                    try {
                        rir.send();
                    } catch (CheckoutException e) {
                        Debug.logError(e, module);
                        return ServiceUtil.returnError(e.getMessage());
                    }
                }
            }
        }

        // create the refund request
        for (String refundOrderId : toRefund.keySet()) {
            GenericValue gOrder = findGoogleOrder(delegator, refundOrderId);
            if (gOrder != null) {
                MerchantInfo mInfo = getMerchantInfo(delegator, getProductStoreFromOrder(gOrder));
                if (mInfo != null) {
                    BigDecimal amount = toRefund.get(refundOrderId).setScale(decimals, rounding);
                    String externalId = gOrder.getString("externalId");
                    String reason = "Item(s) Returned";
                    if (amount.floatValue() > 0) {
                        try {
                            RefundOrderRequest ror = new RefundOrderRequest(mInfo, externalId, reason, amount.floatValue(), "");
                            ror.send();
                        } catch (CheckoutException e) {
                            Debug.logError(e, module);
                            return ServiceUtil.returnError(e.getMessage());
                        }
                    } else {
                        Debug.logWarning("Refund for order #" + refundOrderId + " was 0, nothing to refund?", module);
                    }
                }
            }
        }

        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> sendShipRequest(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        String shipmentId = (String) context.get("shipmentId");
        try {
            sendItemsShipped(delegator, shipmentId);
        } catch (GeneralException e) {
            Debug.logError(e, module);
        }

        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> sendOrderCancelRequest(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        String orderId = (String) context.get("orderId");
        GenericValue order = findGoogleOrder(delegator, orderId);
        if (order != null) {
            MerchantInfo mInfo = getMerchantInfo(delegator, getProductStoreFromOrder(order));
            if (mInfo != null) {
                String externalId = order.getString("externalId");
                CancelOrderRequest cor = new CancelOrderRequest(mInfo, externalId, "Order Cancelled", ""); // TODO: configure the reason and comment
                try {
                    cor.send();
                } catch (CheckoutException e) {
                    Debug.logError(e, module);
                }
            }
        }

        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> sendOrderItemCancelRequest(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        String orderId = (String) context.get("orderId");
        String orderItemSeqId = (String) context.get("orderItemSeqId");
        GenericValue order = findGoogleOrder(delegator, orderId);
        if (order != null) {
            GenericValue orderItem = null;
            try {
                orderItem = EntityQuery.use(delegator).from("OrderItem").where("orderId", orderId, "orderItemSeqId", orderItemSeqId).queryOne();
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
            }

            if (orderItem != null) {
                MerchantInfo mInfo = getMerchantInfo(delegator, getProductStoreFromOrder(order));
                if (mInfo != null) {
                    String externalId = order.getString("externalId");
                    CancelItemsRequest cir = new CancelItemsRequest(mInfo, externalId, "Item Cancelled", ""); // TODO: configure the reason and comment
                    cir.addItem(orderItem.getString("productId"));
                    try {
                        cir.send();
                    } catch (CheckoutException e) {
                        Debug.logError(e, module);
                    }
                }
            }
        }

        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> sendArchiveOrderRequest(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        String orderId = (String) context.get("orderId");
        GenericValue order = findGoogleOrder(delegator, orderId);
        if (order != null) {
            MerchantInfo mInfo = getMerchantInfo(delegator, getProductStoreFromOrder(order));
            if (mInfo != null) {
                String externalId = order.getString("externalId");
                ArchiveOrderRequest aor = new ArchiveOrderRequest(mInfo, externalId);
                try {
                    aor.send();
                } catch (CheckoutException e) {
                    Debug.logError(e, module);
                }
            }
        }

        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> sendUnarchiveOrderRequest(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        String orderId = (String) context.get("orderId");
        GenericValue order = findGoogleOrder(delegator, orderId);
        if (order != null) {
            MerchantInfo mInfo = getMerchantInfo(delegator, getProductStoreFromOrder(order));
            if (mInfo != null) {
                String externalId = order.getString("externalId");
                UnarchiveOrderRequest uor = new UnarchiveOrderRequest(mInfo, externalId);
                try {
                    uor.send();
                } catch (CheckoutException e) {
                    Debug.logError(e, module);
                }
            }
        }

        return ServiceUtil.returnSuccess();
    }

    // special service to tigger off of events which prevent editing orders
    public static Map<String, Object> catchEditGoogleOrder(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get("locale");
        String orderId = (String) context.get("orderId");
        GenericValue order = findGoogleOrder(delegator, orderId);
        if (order != null) {
            Debug.logWarning("Returning FAILURE; this IS an Google Checkout order and cannot be modified as requested!", module);
            return ServiceUtil.returnFailure(UtilProperties.getMessage(resource, "GoogleCheckoutOrderCannotBeModified", locale));
        }
        return ServiceUtil.returnSuccess();
    }

    private static void sendItemsShipped(Delegator delegator, String shipmentId) throws GeneralException {
        List<GenericValue> issued = EntityQuery.use(delegator).from("ItemIssuance").where("shipmentId", shipmentId).queryList();
        if (UtilValidate.isNotEmpty(issued)) {
            try {
                GenericValue googleOrder = null;
                ShipItemsRequest isr = null;
                for (GenericValue issue : issued) {
                    GenericValue orderItem = issue.getRelatedOne("OrderItem", false);
                    String shipmentItemSeqId = issue.getString("shipmentItemSeqId");
                    String productId = orderItem.getString("productId");
                    String orderId = issue.getString("orderId");
                    googleOrder = findGoogleOrder(delegator, orderId);
                    if (UtilValidate.isNotEmpty(googleOrder)) {
                        MerchantInfo mInfo = getMerchantInfo(delegator, getProductStoreFromOrder(googleOrder));
                        if (UtilValidate.isEmpty(mInfo)) {
                            Debug.logInfo("Cannot find Google MerchantInfo for Order #" + orderId, module);
                            continue;
                        }
                        String externalId = googleOrder.getString("externalId");
                        if (UtilValidate.isEmpty(isr)) {
                            isr = new ShipItemsRequest(mInfo, externalId);
                        }
                        // locate the shipment package content record
                        GenericValue packageContent = EntityQuery.use(delegator).from("ShipmentPackageContent").where("shipmentId", shipmentId, "shipmentItemSeqId", shipmentItemSeqId).queryFirst();
                        String carrier = null;
                        if (UtilValidate.isNotEmpty(packageContent)) {
                            GenericValue shipPackage = packageContent.getRelatedOne("ShipmentPackage", false);
                            if (UtilValidate.isNotEmpty(shipPackage)) {
                                List<GenericValue> prs = shipPackage.getRelated("ShipmentPackageRouteSeg", null, null, false);
                                GenericValue packageRoute = EntityUtil.getFirst(prs);
                                if (UtilValidate.isNotEmpty(packageRoute)) {
                                    List<GenericValue> srs = packageRoute.getRelated("ShipmentRouteSegment", null, null, false);
                                    GenericValue route = EntityUtil.getFirst(srs);
                                    String track = packageRoute.getString("trackingCode");
                                    if (UtilValidate.isNotEmpty(route)) {
                                        carrier = route.getString("carrierPartyId");
                                    if (UtilValidate.isEmpty(track)) {
                                        track = route.getString("trackingIdNumber");
                                    }
                                    if (track == null) {
                                        track = "";
                                    }
                                    isr.addItemShippingInformation(productId, carrier, track);
                                    Debug.logInfo("Sending item shipped notification: " + productId + " / " + carrier + " / " + track, module);
                                    Debug.logInfo("Using merchantInfo : " + mInfo.getMerchantId() + " #" + externalId, module);
                                }
                            }
                        }
                    }
                }
            }
            if (UtilValidate.isNotEmpty(googleOrder)) {
                isr.send();
            }
            } catch (CheckoutException e) {
                Debug.logError(e, module);
                throw new GeneralException(e);
            }
        }
    }

    public static GenericValue findGoogleOrder(Delegator delegator, String orderId) {
        GenericValue order = null;
        try {
            order = EntityQuery.use(delegator).from("OrderHeader").where("orderId", orderId).queryOne();
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }
        if (order != null) {
            String salesChannel = order.getString("salesChannelEnumId");
            String externalId = order.getString("externalId");
            if (GoogleCheckoutHelper.SALES_CHANNEL.equals(salesChannel) && UtilValidate.isNotEmpty(externalId)) {
                return order;
            }
        }

        return null;
    }

    public static String getProductStoreFromShipment(Delegator delegator, String shipmentId) {
        GenericValue shipment = null;
        try {
            shipment = EntityQuery.use(delegator).from("Shipment").where("shipmentId", shipmentId).queryOne();
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }
        if (shipment != null) {
            String orderId = shipment.getString("primaryOrderId");
            return getProductStoreFromOrder(findGoogleOrder(delegator, orderId));
        }
        return null;
    }

    public static String getProductStoreFromOrder(GenericValue order) {
        if  (order != null) {
            return order.getString("productStoreId");
        }
        return null;
    }

    public static GenericValue getGoogleConfiguration(Delegator delegator, String productStoreId) {
        if (productStoreId == null) return null;
        GenericValue config = null;
        try {
            config = EntityQuery.use(delegator).from("GoogleCoConfiguration").where("productStoreId", productStoreId).cache().queryOne();
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }
        return config;
    }

    public static MerchantInfo getMerchantInfo(Delegator delegator, String productStoreId) {
        // google configuration
        GenericValue config = getGoogleConfiguration(delegator, productStoreId);
        if (config == null) {
            Debug.logError("No google configuration found for product store ID : " + productStoreId, module);
            return null;
        }

        // merchant information
        String merchantId = config.getString("merchantId");
        String merchantKey = config.getString("merchantKey");
        String envEnumId = config.getString("envEnumId");
        String currencyCode = config.getString("currencyUomId");

        if (UtilValidate.isEmpty(merchantId) || UtilValidate.isEmpty(merchantKey)) {
            return null;
        }

        // base URLs
        String productionRoot = EntityUtilProperties.getPropertyValue("google-checkout", "production.root.url", delegator);
        String sandboxRoot = EntityUtilProperties.getPropertyValue("google-checkout", "sandbox.root.url", delegator);

        // command strings
        String merchantCheckoutCommand = EntityUtilProperties.getPropertyValue("google-checkout", "merchant.checkout.command", "merchantCheckout", delegator);
        String checkoutCommand = EntityUtilProperties.getPropertyValue("google-checkout", "checkout.command", "checkout", delegator);
        String requestCommand = EntityUtilProperties.getPropertyValue("google-checkout", "request.command", "request", delegator);

        String environment = null;
        String checkoutUrl = "";
        String merchantCheckoutUrl = "";
        String requestUrl = "";

        // build the URLs based on the Environment type
        if ("GOOGLE_SANDBOX".equals(envEnumId)) {
            merchantCheckoutUrl = sandboxRoot + "/" + merchantCheckoutCommand + "/Merchant/" + merchantId;
            checkoutUrl = sandboxRoot + "/" + checkoutCommand + "/Merchant/" + merchantId;
            requestUrl = sandboxRoot + "/" + requestCommand + "/Merchant/" + merchantId;
            environment = EnvironmentType.Sandbox;
        } else if ("GOOGLE_PRODUCTION".equals(envEnumId)) {
            merchantCheckoutUrl = productionRoot + "/" + merchantCheckoutCommand + "/Merchant/" + merchantId;
            checkoutUrl = productionRoot + "/" + checkoutCommand + "/Merchant/" + merchantId;
            requestUrl = productionRoot + "/" + requestCommand + "/Merchant/" + merchantId;
            environment = EnvironmentType.Production;
        } else {
            Debug.logError("Environment must be one of " + EnvironmentType.Sandbox + " or " + EnvironmentType.Production + ".", module);
            return null;
        }
        return new MerchantInfo(merchantId, merchantKey, environment, currencyCode, checkoutUrl, merchantCheckoutUrl, requestUrl);
    }
}
