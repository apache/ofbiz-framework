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
import java.util.List;
import java.util.Map;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.util.EntityUtil;
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
import com.google.checkout.orderprocessing.UnarchiveOrderRequest;
import com.google.checkout.orderprocessing.lineitem.CancelItemsRequest;
import com.google.checkout.orderprocessing.lineitem.ShipItemsRequest;

public class GoogleRequestServices {
    
    private static final String module = GoogleRequestServices.class.getName();
    
    @SuppressWarnings("unchecked")
    public static Map<String, Object> sendShoppingCartRequest(DispatchContext dctx, Map<String, ? extends Object> context) {        
        ShoppingCart cart = (ShoppingCart) context.get("shoppingCart");
        String productStoreId = cart.getProductStoreId();
        GenericDelegator delegator = dctx.getDelegator();
        MerchantInfo mInfo = getMerchantInfo();
        if (mInfo == null) {
            Debug.logError("Invalid Google Chechout Merchant settings, check your configuration!", module);
            return ServiceUtil.returnError("Google checkout configuration error");
        }
        
        // the checkout request object
        CheckoutShoppingCartRequest req = new CheckoutShoppingCartRequest(mInfo, 300);
        req.setRequestInitialAuthDetails(true); // send the auth notification
        
        // add the items
        List<ShoppingCartItem> items = cart.items();
        for (ShoppingCartItem item : items) {                        
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
                        
        // flow support URLs
        String contShoppingUrl = UtilProperties.getPropertyValue("googleCheckout.properties", "continueShoppingUrl");
        String editCartUrl = UtilProperties.getPropertyValue("googleCheckout.properties", "editCartUrl");
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
            shippingOptions = delegator.findByAnd("GoogleShippingMethods", UtilMisc.toMap("productStoreId", productStoreId));
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
                        return ServiceUtil.returnError("Invalid Google Checkout Shipping Configuration! Carriers can only be UPS, FedEx or USPS.");
                    }
                    req.addCarrierCalculatedShippingOption(amount.floatValue(), shippingCompany, CarrierPickup.REGULAR_PICKUP,
                             shippingName, additionalAmount.floatValue(), additionalPercent.floatValue());                              
                }
            }
        }
        
        // merchant stuff
        //req.setAcceptMerchantCoupons(false); // disable coupons through google
        //req.setAcceptMerchantGiftCertificates(false); // disable gift certs through google
        req.setRequestBuyerPhoneNumber(true);
        
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
            return ServiceUtil.returnError("Checkout response was null");
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
        GenericDelegator delegator = dctx.getDelegator();
        String orderId = (String) context.get("orderId");
        GenericValue order = findGoogleOrder(delegator, orderId);
        if (order != null) {
            MerchantInfo mInfo = getMerchantInfo();
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
        GenericDelegator delegator = dctx.getDelegator();
        String orderId = (String) context.get("orderId");
        GenericValue order = findGoogleOrder(delegator, orderId);
        if (order != null) {
            MerchantInfo mInfo = getMerchantInfo();
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
        GenericDelegator delegator = dctx.getDelegator();
        String orderId = (String) context.get("orderId");
        GenericValue order = findGoogleOrder(delegator, orderId);
        if (order != null) {
            MerchantInfo mInfo = getMerchantInfo();
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
    
    // NOT IMPLEMENTED
    public static Map<String, Object> sendReturnRequest(DispatchContext dctx, Map<String, ? extends Object> context) {
        // Implement to use ReturnItemsRequest - send each item being returned
        
        // Check to see if the return is a refund return -- if so also send a RefundOrderRequest
          
        return ServiceUtil.returnSuccess();
    }
    
    public static Map<String, Object> sendShipRequest(DispatchContext dctx, Map<String, ? extends Object> context) {
        GenericDelegator delegator = dctx.getDelegator();
        String shipmentId = (String) context.get("shipmentId");
        MerchantInfo mInfo = getMerchantInfo();
        if (mInfo != null) {
            try {
                sendItemsShipped(delegator, shipmentId, mInfo);
            } catch (GeneralException e) {
                // TODO: handle the error
            }
        }                
        return ServiceUtil.returnSuccess();
    }
    
    public static Map<String, Object> sendOrderCancelRequest(DispatchContext dctx, Map<String, ? extends Object> context) {
        GenericDelegator delegator = dctx.getDelegator();
        String orderId = (String) context.get("orderId");
        GenericValue order = findGoogleOrder(delegator, orderId);
        if (order != null) {
            MerchantInfo mInfo = getMerchantInfo();
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
        GenericDelegator delegator = dctx.getDelegator();
        String orderId = (String) context.get("orderId");
        String orderItemSeqId = (String) context.get("orderItemSeqId");
        GenericValue order = findGoogleOrder(delegator, orderId);
        if (order != null) {
            GenericValue orderItem = null;
            try {
                orderItem = delegator.findOne("OrderItem", UtilMisc.toMap("orderId", orderId, "orderItemSeqId", orderItemSeqId), false);
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
            }
            
            if (orderItem != null) {
                MerchantInfo mInfo = getMerchantInfo();
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
        GenericDelegator delegator = dctx.getDelegator();
        String orderId = (String) context.get("orderId");
        GenericValue order = findGoogleOrder(delegator, orderId);
        if (order != null) {
            MerchantInfo mInfo = getMerchantInfo();
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
        GenericDelegator delegator = dctx.getDelegator();
        String orderId = (String) context.get("orderId");
        GenericValue order = findGoogleOrder(delegator, orderId);
        if (order != null) {
            MerchantInfo mInfo = getMerchantInfo();
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
        GenericDelegator delegator = dctx.getDelegator();
        String orderId = (String) context.get("orderId");
        GenericValue order = findGoogleOrder(delegator, orderId);
        if (order != null) {
            Debug.log("Returning FAILURE; this IS an Google Checkout order and cannot be modified as requested!", module);
            return ServiceUtil.returnFailure("Google Checkout orders cannot be modified. You may cancel orders/items only!");
        }
        return ServiceUtil.returnSuccess();        
    }    
    
    private static void sendItemsShipped(GenericDelegator delegator, String shipmentId, MerchantInfo mInfo) throws GeneralException {
        List<GenericValue> issued = delegator.findByAnd("ItemIssuance", UtilMisc.toMap("shipmentId", shipmentId));
        if (issued != null && issued.size() > 0) {
            for (GenericValue issue : issued) {
                GenericValue orderItem = issue.getRelatedOne("OrderItem");
                String shipmentItemSeqId = issue.getString("shipmentItemSeqId"); 
                String productId = orderItem.getString("productId");
                String orderId = issue.getString("orderId");
                GenericValue order = findGoogleOrder(delegator, orderId);
                                 
                if (order != null) {
                    String externalId = order.getString("externalId");
                    
                    // locate the shipment package content record
                    Map<String, ? extends Object> spcLup = UtilMisc.toMap("shipmentId", shipmentId, "shipmentItemSeqId", shipmentItemSeqId);

                    List<GenericValue> spc = delegator.findByAnd("ShipmentPackageContent", spcLup);
                    GenericValue packageContent = EntityUtil.getFirst(spc);
                    String carrier = null;
                    
                    if (packageContent != null) {
                        GenericValue shipPackage = packageContent.getRelatedOne("ShipmentPackage");

                        if (shipPackage != null) {
                            List<GenericValue> prs = shipPackage.getRelated("ShipmentPackageRouteSeg");
                            GenericValue packageRoute = EntityUtil.getFirst(prs);

                            if (packageRoute != null) {
                                List<GenericValue> srs = packageRoute.getRelated("ShipmentRouteSegment");
                                GenericValue route = EntityUtil.getFirst(srs);
                                String track = packageRoute.getString("trackingCode");

                                if (route != null) { 
                                    carrier = route.getString("carrierPartyId");
                                    if (UtilValidate.isEmpty(track)) {
                                        track = route.getString("trackingIdNumber");
                                    }

                                    try {
                                        ShipItemsRequest isr = new ShipItemsRequest(mInfo, externalId);
                                        isr.addItemShippingInformation(productId, carrier, track);
                                        isr.send();
                                    } catch (CheckoutException e) {
                                        Debug.logError(e, module);
                                        throw new GeneralException(e);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    public static GenericValue findGoogleOrder(GenericDelegator delegator, String orderId) {
        GenericValue order = null;
        try {
            order = delegator.findOne("OrderHeader", false, "orderId", orderId);
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
                           
    public static MerchantInfo getMerchantInfo() {
        // merchant information
        String merchantId = UtilProperties.getPropertyValue("googleCheckout.properties", "merchantId");
        String merchantKey = UtilProperties.getPropertyValue("googleCheckout.properties", "merchantKey");
        String environment = UtilProperties.getPropertyValue("googleCheckout.properties", "environment.mode", "Sandbox");
        String currencyCode = "USD";
        
        if (UtilValidate.isEmpty(merchantId) || UtilValidate.isEmpty(merchantKey)) {
            return null;
        }
        
        // base URLs
        String productionRoot = UtilProperties.getPropertyValue("googleCheckout.properties", "production.root.url");
        String sandboxRoot = UtilProperties.getPropertyValue("googleCheckout.properties", "sandbox.root.url");
        
        // command strings
        String merchantCheckoutCommand = UtilProperties.getPropertyValue("googleCheckout.properties", "merchant.checkout.command", "merchantCheckout");
        String checkoutCommand = UtilProperties.getPropertyValue("googleCheckout.properties", "checkout.command", "checkout");        
        String requestCommand = UtilProperties.getPropertyValue("googleCheckout.properties", "request.command", "request");

        String checkoutUrl = "";
        String merchantCheckoutUrl = "";
        String requestUrl = "";

        // build the URLs based on the Environment type
        if (EnvironmentType.Sandbox.equals(environment)) {
            merchantCheckoutUrl = sandboxRoot + "/" + merchantCheckoutCommand + "/Merchant/" + merchantId;
            checkoutUrl = sandboxRoot + "/" + checkoutCommand + "/Merchant/" + merchantId;            
            requestUrl = sandboxRoot + "/" + requestCommand + "/Merchant/" + merchantId;
        } else if (EnvironmentType.Production.equals(environment)) {
            merchantCheckoutUrl = productionRoot + "/" + merchantCheckoutCommand + "/Merchant/" + merchantId;
            checkoutUrl = productionRoot + "/" + checkoutCommand + "/Merchant/" + merchantId;            
            requestUrl = productionRoot + "/" + requestCommand + "/Merchant/" + merchantId;
        } else {
            Debug.logError("Environment must be one of " + EnvironmentType.Sandbox + " or " + EnvironmentType.Production + ".", module);
            return null;
        }
        return new MerchantInfo(merchantId, merchantKey, environment, currencyCode, checkoutUrl, merchantCheckoutUrl, requestUrl);                
    }                   
}
