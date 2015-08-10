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

package org.ofbiz.ebaystore;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilGenerics;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.ebay.EbayHelper;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.util.EntityQuery;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.order.order.OrderChangeHelper;
import org.ofbiz.order.shoppingcart.CheckOutHelper;
import org.ofbiz.order.shoppingcart.ShoppingCart;
import org.ofbiz.order.shoppingcart.ShoppingCartItem;
import org.ofbiz.party.party.PartyWorker;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceUtil;

public class EbayStoreOrder {

    private static final String resource = "EbayUiLabels";
    private static final String module = EbayStoreOrder.class.getName();

    public static Map<String, Object> EbayStoreImportTransaction(DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> result = new HashMap<String, Object>();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        try {
            if ("Complete".equals(context.get("checkoutStatus").toString()) && "NOT_IMPORT".equals(context.get("importStatus").toString())) {
                if (UtilValidate.isEmpty(context.get("shippingAddressStreet1"))) {
                    context.put("shippingAddressStreet1", context.get("shippingAddressStreet").toString());
                }
                result = dispatcher.runSync("EbayStoreCreateTransactionShoppingCart", context);
            }
        } catch (Exception e) {
            result = ServiceUtil.returnFailure(e.getMessage());
        }
        result.put("productStoreId", context.get("productStoreId").toString());
        result.put("formSelect", "transaction");
        return result;
    }
    public static Map<String, Object> EbayStoreImportOrder(DispatchContext dctx, Map<String, Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Map<String, Object> result = new HashMap<String, Object>();
        if (UtilValidate.isEmpty(context.get("orderId"))) {
            try {
                result = dispatcher.runSync("EbayStoreCreateOrderShoppingCart", context);
            } catch (Exception e) {
                result = ServiceUtil.returnFailure(e.getMessage());
            }
        }
        result.put("productStoreId", context.get("productStoreId").toString());
        result.put("formSelect", "order");
        return result;
    }
    
    public static Map<String, Object> EbayStoreCreateTransactionShoppingCart(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Locale locale = (Locale) context.get("locale");
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Map<String, Object> result = new HashMap<String, Object>();
        
        String productStoreId = context.get("productStoreId").toString();
        String defaultCurrencyUomId = "";
        String payToPartyId = "";
        String facilityId = "";
        
        try {
            if (UtilValidate.isEmpty(productStoreId)) {
                return ServiceUtil.returnFailure(UtilProperties.getMessage(resource, "ordersImportFromEbay.productStoreIdIsMandatory", locale));
            } else {
                GenericValue productStore = EntityQuery.use(delegator).from("ProductStore").where("productStoreId", productStoreId).queryOne();
                if (UtilValidate.isNotEmpty(productStore)) {
                    defaultCurrencyUomId = productStore.getString("defaultCurrencyUomId");
                    payToPartyId = productStore.getString("payToPartyId");
                    facilityId = productStore.getString("inventoryFacilityId");
                } else {
                    return ServiceUtil.returnFailure(UtilProperties.getMessage(resource, "ordersImportFromEbay.productStoreIdIsMandatory", locale));
                }
            }
            
            ShoppingCart cart = new ShoppingCart(delegator, productStoreId, locale, defaultCurrencyUomId);
            String externalId = context.get("externalId").toString();
            if (UtilValidate.isNotEmpty(externalId)) {
                cart.setExternalId(externalId);
            } else {
                return ServiceUtil.returnFailure(UtilProperties.getMessage(resource, "EbayStoreOrder.externalIdNotAvailable", locale));
            }

            cart.setOrderType("SALES_ORDER");
            cart.setChannelType("EBAY_SALES_CHANNEL");
            cart.setUserLogin(userLogin, dispatcher);
            cart.setProductStoreId(productStoreId);

            if (UtilValidate.isNotEmpty(facilityId)) {
                cart.setFacilityId(facilityId);
            }

            String amountStr = context.get("amountPaid").toString();
            BigDecimal amountPaid = new BigDecimal(amountStr);
            if (UtilValidate.isNotEmpty(amountPaid)) {
                amountPaid = new BigDecimal(amountStr);
            }
            cart.addPaymentAmount("EXT_EBAY", amountPaid, externalId, null, true, false, false);

            Timestamp orderDate = UtilDateTime.nowTimestamp();
            if (UtilValidate.isNotEmpty(context.get("createdDate"))) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
                Date createdDate = dateFormat.parse((String) context.get("createdDate"));
                orderDate = new Timestamp(createdDate.getTime());
            }
            cart.setOrderDate(orderDate);

            String productId = context.get("productId").toString();
            if (UtilValidate.isEmpty(productId)) {
                return ServiceUtil.returnFailure(UtilProperties.getMessage(resource, "ordersImportFromEbay.productIdNotAvailable", locale));
            } else {
                GenericValue product = EntityQuery.use(delegator).from("Product").where("productId", productId).queryOne();
                if (UtilValidate.isEmpty(product)) {
                    return ServiceUtil.returnFailure(UtilProperties.getMessage(resource, "ordersImportFromEbay.productIdDoesNotExist", locale));
                }
            }

            if (UtilValidate.isEmpty(context.get("paidTime"))) {
                return ServiceUtil.returnFailure(UtilProperties.getMessage(resource, "ordersImportFromEbay.paymentIsStillNotReceived", locale));
            }

            BigDecimal unitPrice = new BigDecimal(context.get("transactionPrice").toString());
            BigDecimal quantity = new BigDecimal(context.get("quantityPurchased").toString());
            cart.addItemToEnd(productId, null, quantity, unitPrice, null, null, null, "PRODUCT_ORDER_ITEM", dispatcher, Boolean.FALSE, Boolean.FALSE);

            if (UtilValidate.isNotEmpty(payToPartyId)) {
                cart.setBillFromVendorPartyId(payToPartyId);
            }

            String shippingCost = context.get("shippingServiceCost").toString();
            if (UtilValidate.isNotEmpty(shippingCost)) {
                BigDecimal shippingAmount = new BigDecimal(shippingCost);
                if (shippingAmount.doubleValue() > 0) {
                    GenericValue shippingAdjustment = EbayHelper.makeOrderAdjustment(delegator, "SHIPPING_CHARGES", cart.getOrderId(), null, null, shippingAmount.doubleValue(), 0.0);
                    if (UtilValidate.isNotEmpty(shippingAdjustment)) {
                        cart.addAdjustment(shippingAdjustment);
                    }
                }
            }

            String shippingTotalAdditionalCost = context.get("shippingTotalAdditionalCost").toString();
            if (UtilValidate.isNotEmpty(shippingTotalAdditionalCost)) {
                double shippingAdditionalCost = new Double(shippingTotalAdditionalCost).doubleValue();
                if (shippingAdditionalCost > 0) {
                    GenericValue shippingAdjustment = EbayHelper.makeOrderAdjustment(delegator, "MISCELLANEOUS_CHARGE", cart.getOrderId(), null, null, shippingAdditionalCost, 0.0);
                    if (shippingAdjustment != null) {
                        cart.addAdjustment(shippingAdjustment);
                    }
                }
            }

            String salesTaxAmount = context.get("salesTaxAmount").toString();
            String salesTaxPercent = context.get("salesTaxPercent").toString();
            if (UtilValidate.isNotEmpty(salesTaxAmount)) {
                double salesTaxAmountTotal = new Double(salesTaxAmount).doubleValue();
                if (salesTaxAmountTotal > 0) {
                    double salesPercent = 0.0;
                    if (UtilValidate.isNotEmpty(salesTaxPercent)) {
                        salesPercent = new Double(salesTaxPercent).doubleValue();
                    }
                    GenericValue salesTaxAdjustment = EbayHelper.makeOrderAdjustment(delegator, "SALES_TAX", cart.getOrderId(), null, null, salesTaxAmountTotal, salesPercent);
                    if (UtilValidate.isNotEmpty(salesTaxAdjustment)) {
                        cart.addAdjustment(salesTaxAdjustment);
                    }
                }
            }
            
                Debug.logInfo("Importing new order from eBay", module);
                // set partyId to
                String partyId = null;
                String contactMechId = "";
                GenericValue partyAttribute = null;
                if (UtilValidate.isNotEmpty(context.get("eiasTokenBuyer").toString())) {
                    partyAttribute = EntityQuery.use(delegator).from("PartyAttribute").where("attrValue", context.get("eiasTokenBuyer").toString()).queryFirst();
                }

                // if we get a party, check its contact information.
                if (UtilValidate.isNotEmpty(partyAttribute)) {
                    partyId = (String) partyAttribute.get("partyId");
                    Debug.logInfo("Found existing party associated to the eBay buyer: " + partyId, module);
                    GenericValue party = EntityQuery.use(delegator).from("Party").where("partyId", partyId).queryOne();

                    contactMechId = EbayHelper.setShippingAddressContactMech(dispatcher, delegator, party, userLogin, context);
                    String emailBuyer = context.get("emailBuyer").toString();
                    if (!(emailBuyer.equals("") || emailBuyer.equalsIgnoreCase("Invalid Request"))) {
                        EbayHelper.setEmailContactMech(dispatcher, delegator, party, userLogin, context);
                    }
                    EbayHelper.setPhoneContactMech(dispatcher, delegator, party, userLogin, context);
                }

                // create party if none exists already
                if (UtilValidate.isEmpty(partyId)) {
                    Debug.logInfo("Creating new party for the eBay buyer.", module);
                    partyId = EbayHelper.createCustomerParty(dispatcher, context.get("buyerName").toString(), userLogin);
                    if (UtilValidate.isEmpty(partyId)) {
                        Debug.logWarning("Using admin party for the eBay buyer.", module);
                        partyId = "admin";
                    }
                }

                // create new party's contact information
                if (UtilValidate.isEmpty(contactMechId)) {
                    Debug.logInfo("Creating new postal address for party: " + partyId, module);
                    contactMechId = EbayHelper.createAddress(dispatcher, partyId, userLogin, "SHIPPING_LOCATION", context);
                    if (UtilValidate.isEmpty(contactMechId)) {
                        return ServiceUtil.returnFailure(UtilProperties.getMessage(resource, "EbayStoreUnableToCreatePostalAddress", locale) + context);
                    }
                    Debug.logInfo("Created postal address: " + contactMechId, module);
                    Debug.logInfo("Creating new phone number for party: " + partyId, module);
                    EbayHelper.createPartyPhone(dispatcher, partyId, context.get("shippingAddressPhone").toString(), userLogin);
                    Debug.logInfo("Creating association to eBay buyer for party: " + partyId, module);
                    EbayHelper.createEbayCustomer(dispatcher, partyId, context.get("ebayUserIdBuyer").toString(), context.get("eiasTokenBuyer").toString(), userLogin);
                    String emailBuyer = context.get("emailBuyer").toString();
                    if (UtilValidate.isNotEmpty(emailBuyer) && !emailBuyer.equalsIgnoreCase("Invalid Request")) {
                        Debug.logInfo("Creating new email for party: " + partyId, module);
                        EbayHelper.createPartyEmail(dispatcher, partyId, emailBuyer, userLogin);
                    }
                }

                Debug.logInfo("Setting cart roles for party: " + partyId, module);
                cart.setBillToCustomerPartyId(partyId);
                cart.setPlacingCustomerPartyId(partyId);
                cart.setShipToCustomerPartyId(partyId);
                cart.setEndUserCustomerPartyId(partyId);

                Debug.logInfo("Setting contact mech in cart: " + contactMechId, module);
                cart.setAllShippingContactMechId(contactMechId);
                cart.setAllMaySplit(Boolean.FALSE);

                Debug.logInfo("Setting shipment method: " + context.get("shippingService").toString(), module);
                EbayHelper.setShipmentMethodType(cart, context.get("shippingService").toString(), productStoreId, delegator);

                cart.makeAllShipGroupInfos();

                // create the order
                Debug.logInfo("Creating CheckOutHelper.", module);
                CheckOutHelper checkout = new CheckOutHelper(dispatcher, delegator, cart);
                Debug.logInfo("Creating order.", module);
                Map<String, Object> orderCreate = checkout.createOrder(userLogin);

                String orderId = orderCreate.get("orderId").toString();
                Debug.logInfo("Created order with id: " + orderId, module);

                // approve the order
                if (UtilValidate.isNotEmpty(orderId)) {
                    Debug.logInfo("Approving order with id: " + orderId, module);
                    boolean approved = OrderChangeHelper.approveOrder(dispatcher, userLogin, orderId);
                    Debug.logInfo("Order approved with result: " + approved, module);

                    // create the payment from the preference
                    if (approved) {
                        Debug.logInfo("Creating payment for approved order.", module);
                        EbayHelper.createPaymentFromPaymentPreferences(delegator, dispatcher, userLogin, orderId, externalId, cart.getOrderDate(), amountPaid, partyId);
                        Debug.logInfo("Payment created.", module);
                    }
                }
        } catch (Exception e) {
            result = ServiceUtil.returnFailure(e.getMessage());
        }
        return result;
    }
    public static Map<String, Object> EbayStoreCreateOrderShoppingCart(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Locale locale = (Locale) context.get("locale");
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Map <String, Object> result = new HashMap<String, Object>();

        String productStoreId = context.get("productStoreId").toString();
        String defaultCurrencyUomId = null;
        String payToPartyId = null;
        String facilityId = null;
        try {
            if (productStoreId == null) {
                return ServiceUtil.returnFailure(UtilProperties.getMessage(resource, "ordersImportFromEbay.productStoreIdIsMandatory", locale));
            } else {
                GenericValue productStore = EntityQuery.use(delegator).from("ProductStore").where("productStoreId", productStoreId).queryOne();
                if (productStore != null) {
                    defaultCurrencyUomId = productStore.getString("defaultCurrencyUomId");
                    payToPartyId = productStore.getString("payToPartyId");
                    facilityId = productStore.getString("inventoryFacilityId");
                } else {
                    return ServiceUtil.returnFailure(UtilProperties.getMessage(resource, "ordersImportFromEbay.productStoreIdIsMandatory", locale));
                }
            }
            ShoppingCart cart = new ShoppingCart(delegator, productStoreId, locale, defaultCurrencyUomId);

            // set the external id with the eBay Item Id
            String externalId = (String) context.get("externalId");
            cart.setOrderType("SALES_ORDER");
            cart.setChannelType("EBAY_SALES_CHANNEL");
            cart.setUserLogin(userLogin, dispatcher);
            cart.setProductStoreId(productStoreId);

            if (UtilValidate.isNotEmpty(facilityId)) {
                cart.setFacilityId(facilityId);
            }

            String amountStr = (String) context.get("amountPaid");
            BigDecimal amountPaid = BigDecimal.ZERO;
            if (UtilValidate.isNotEmpty(amountStr)) {
                amountPaid = new BigDecimal(amountStr);
            }
            cart.addPaymentAmount("EXT_EBAY", amountPaid, externalId, null, true, false, false);
            Timestamp orderDate = UtilDateTime.nowTimestamp();
            if (UtilValidate.isNotEmpty(context.get("createdDate"))) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
                Date createdDate = dateFormat.parse((String) context.get("createdDate"));
                orderDate = new Timestamp(createdDate.getTime());
            }

            cart.setOrderDate(orderDate);
            // Before import the order from eBay to OFBiz is mandatory that the payment has be received
            String paidTime = (String) context.get("paidTime");
            if (UtilValidate.isEmpty(paidTime)) {
                return ServiceUtil.returnFailure(UtilProperties.getMessage(resource, "ordersImportFromEbay.paymentIsStillNotReceived", locale));
            }

            List<Map<String, Object>> orderItemList = UtilGenerics.checkList(context.get("orderItemList"));
            Iterator<Map<String, Object>> orderItemIter = orderItemList.iterator();
            while (orderItemIter.hasNext()) {
                Map<String, Object> orderItem = orderItemIter.next();
                addItem(cart, orderItem, dispatcher, delegator, 0);
            }

            // set partyId from
            if (UtilValidate.isNotEmpty(payToPartyId)) {
                cart.setBillFromVendorPartyId(payToPartyId);
            }

            Map<String, Object> shippingServiceSelectedCtx =  UtilGenerics.checkMap(context.get("shippingServiceSelectedCtx"));
            if (UtilValidate.isNotEmpty(shippingServiceSelectedCtx.get("shippingServiceCost"))) {
                BigDecimal shippingAmount = new BigDecimal(shippingServiceSelectedCtx.get("shippingServiceCost").toString());
                if (shippingAmount.doubleValue() > 0) {
                    GenericValue shippingAdjustment = EbayHelper.makeOrderAdjustment(delegator, "SHIPPING_CHARGES", cart.getOrderId(), null, null, shippingAmount.doubleValue(), 0.0);
                    if (shippingAdjustment != null) {
                        cart.addAdjustment(shippingAdjustment);
                    }
                }
            }

            // Apply additional shipping costs as order adjustment
            if (UtilValidate.isNotEmpty(shippingServiceSelectedCtx.get("shippingTotalAdditionalCost"))) {
                BigDecimal shippingAdditionalCost = new BigDecimal(shippingServiceSelectedCtx.get("shippingTotalAdditionalCost").toString());
                if (shippingAdditionalCost.doubleValue() > 0) {
                    GenericValue shippingAdjustment = EbayHelper.makeOrderAdjustment(delegator, "MISCELLANEOUS_CHARGE", cart.getOrderId(), null, null, shippingAdditionalCost.doubleValue(), 0.0);
                    if (shippingAdjustment != null) {
                        cart.addAdjustment(shippingAdjustment);
                    }
                }
            }

            // Apply sales tax as order adjustment
            Map<String, Object> shippingDetailsCtx = UtilGenerics.checkMap(context.get("shippingDetailsCtx"));
            if (UtilValidate.isNotEmpty(shippingDetailsCtx.get("salesTaxAmount"))) {
                BigDecimal salesTaxAmount = new BigDecimal(shippingDetailsCtx.get("salesTaxAmount").toString());
                if (salesTaxAmount.doubleValue() > 0) {
                    double salesPercent = 0.0;
                    if (UtilValidate.isNotEmpty(shippingDetailsCtx.get("salesTaxPercent"))) {
                        salesPercent = new Double(shippingDetailsCtx.get("salesTaxPercent").toString()).doubleValue();
                    }
                    GenericValue salesTaxAdjustment = EbayHelper.makeOrderAdjustment(delegator, "SALES_TAX", cart.getOrderId(), null, null, salesTaxAmount.doubleValue(), salesPercent);
                    if (salesTaxAdjustment != null) {
                        cart.addAdjustment(salesTaxAdjustment);
                    }
                }
            }

            Debug.logInfo("Importing new order from eBay", module);
            // set partyId to
            String partyId = null;
            String contactMechId = null;

            Map<String, Object> shippingAddressCtx = UtilGenerics.checkMap(context.get("shippingAddressCtx"));
            if (UtilValidate.isNotEmpty(shippingAddressCtx)) {
                String buyerName = (String) shippingAddressCtx.get("buyerName");
                String firstName = buyerName.substring(0, buyerName.indexOf(" "));
                String lastName = buyerName.substring(buyerName.indexOf(" ")+1);

                String country = (String) shippingAddressCtx.get("shippingAddressCountry");
                String state = (String) shippingAddressCtx.get("shippingAddressStateOrProvince");
                String city = (String) shippingAddressCtx.get("shippingAddressCityName");
                EbayHelper.correctCityStateCountry(dispatcher, shippingAddressCtx, city, state, country);
                String shippingAddressStreet = null;
                if (UtilValidate.isEmpty(shippingAddressCtx.get("shippingAddressStreet1"))) {
                    shippingAddressStreet = shippingAddressCtx.get("shippingAddressStreet").toString();
                    shippingAddressCtx.put("shippingAddressStreet1", shippingAddressStreet);
                } else {
                    shippingAddressStreet = shippingAddressCtx.get("shippingAddressStreet1").toString();
                }

                List<GenericValue> shipInfo = PartyWorker.findMatchingPersonPostalAddresses(delegator, shippingAddressStreet,
                        (UtilValidate.isEmpty(shippingAddressCtx.get("shippingAddressStreet2")) ? null : shippingAddressCtx.get("shippingAddressStreet2").toString()), shippingAddressCtx.get("city").toString(), shippingAddressCtx.get("stateProvinceGeoId").toString(),
                        shippingAddressCtx.get("shippingAddressPostalCode").toString(), null, shippingAddressCtx.get("countryGeoId").toString(), firstName, null, lastName);
                if (UtilValidate.isNotEmpty(shipInfo)) {
                    GenericValue first = EntityUtil.getFirst(shipInfo);
                    partyId = first.getString("partyId");
                    Debug.logInfo("Existing shipping address found for : (party: " + partyId + ")", module);
                }
            }

            // If matching party not found then try to find partyId from PartyAttribute entity.
            GenericValue partyAttribute = null;
            if (UtilValidate.isNotEmpty(context.get("eiasTokenBuyer"))) {
                partyAttribute = EntityQuery.use(delegator).from("PartyAttribute").where("attrValue", (String) context.get("eiasTokenBuyer")).queryFirst();
                if (UtilValidate.isNotEmpty(partyAttribute)) {
                    partyId = (String) partyAttribute.get("partyId");
                }
            }

            // if we get a party, check its contact information.
            if (UtilValidate.isNotEmpty(partyId)) {
                Debug.logInfo("Found existing party associated to the eBay buyer: " + partyId, module);
                GenericValue party = EntityQuery.use(delegator).from("Party").where("partyId", partyId).queryOne();

                contactMechId = EbayHelper.setShippingAddressContactMech(dispatcher, delegator, party, userLogin, shippingAddressCtx);
                String emailBuyer = (String) context.get("emailBuyer");
                if (!(emailBuyer.equals("") || emailBuyer.equalsIgnoreCase("Invalid Request"))) {
                    EbayHelper.setEmailContactMech(dispatcher, delegator, party, userLogin, context);
                }
                EbayHelper.setPhoneContactMech(dispatcher, delegator, party, userLogin, shippingAddressCtx);
            }

            // create party if none exists already
            if (UtilValidate.isEmpty(partyId)) {
                Debug.logInfo("Creating new party for the eBay buyer.", module);
                partyId = EbayHelper.createCustomerParty(dispatcher, (String) shippingAddressCtx.get("buyerName"), userLogin);
                if (UtilValidate.isEmpty(partyId)) {
                    Debug.logWarning("Using admin party for the eBay buyer.", module);
                    partyId = "admin";
                }
            }

            // create new party's contact information
            if (UtilValidate.isEmpty(contactMechId)) {

                Debug.logInfo("Creating new postal address for party: " + partyId, module);
                contactMechId = EbayHelper.createAddress(dispatcher, partyId, userLogin, "SHIPPING_LOCATION", shippingAddressCtx);
                if (UtilValidate.isEmpty(contactMechId)) {
                    return ServiceUtil.returnFailure(UtilProperties.getMessage(resource, "EbayStoreUnableToCreatePostalAddress", locale) + shippingAddressCtx);
                }
                Debug.logInfo("Created postal address: " + contactMechId, module);
                Debug.logInfo("Creating new phone number for party: " + partyId, module);
                EbayHelper.createPartyPhone(dispatcher, partyId, (String) shippingAddressCtx.get("shippingAddressPhone"), userLogin);
                Debug.logInfo("Creating association to eBay buyer for party: " + partyId, module);
                EbayHelper.createEbayCustomer(dispatcher, partyId, (String) context.get("ebayUserIdBuyer"), null, userLogin);
                String emailBuyer = (String) context.get("emailBuyer");
                if (UtilValidate.isNotEmpty(emailBuyer) && !emailBuyer.equalsIgnoreCase("Invalid Request")) {
                    Debug.logInfo("Creating new email for party: " + partyId, module);
                    EbayHelper.createPartyEmail(dispatcher, partyId, emailBuyer, userLogin);
                }
            }

            Debug.logInfo("Setting cart roles for party: " + partyId, module);
            cart.setBillToCustomerPartyId(partyId);
            cart.setPlacingCustomerPartyId(partyId);
            cart.setShipToCustomerPartyId(partyId);
            cart.setEndUserCustomerPartyId(partyId);

            Debug.logInfo("Setting contact mech in cart: " + contactMechId, module);
            cart.setAllShippingContactMechId(contactMechId);
            cart.setAllMaySplit(Boolean.FALSE);

            Debug.logInfo("Setting shipment method: " + (String) shippingServiceSelectedCtx.get("shippingService"), module);
            EbayHelper.setShipmentMethodType(cart, (String) shippingServiceSelectedCtx.get("shippingService"), productStoreId, delegator);
            cart.makeAllShipGroupInfos();

            // create the order
            Debug.logInfo("Creating CheckOutHelper.", module);
            CheckOutHelper checkout = new CheckOutHelper(dispatcher, delegator, cart);
            Debug.logInfo("Creating order.", module);
            Map<?, ?> orderCreate = checkout.createOrder(userLogin);

            if ("error".equals(orderCreate.get("responseMessage"))) {
                List<String> errorMessageList = UtilGenerics.checkList(orderCreate.get("errorMessageList"), String.class);
                return ServiceUtil.returnError(errorMessageList);
            }
            String orderId = (String) orderCreate.get("orderId");
            Debug.logInfo("Created order with id: " + orderId, module);

            // approve the order
            if (UtilValidate.isNotEmpty(orderId)) {
                Debug.logInfo("Approving order with id: " + orderId, module);
                boolean approved = OrderChangeHelper.approveOrder(dispatcher, userLogin, orderId);
                Debug.logInfo("Order approved with result: " + approved, module);

                // create the payment from the preference
                if (approved) {
                    Debug.logInfo("Creating payment for approved order.", module);
                    EbayHelper.createPaymentFromPaymentPreferences(delegator, dispatcher, userLogin, orderId, externalId, cart.getOrderDate(), amountPaid, partyId);
                    Debug.logInfo("Payment created.", module);
                }
                result = ServiceUtil.returnFailure("Order created successfully with ID (" + orderId + ") & eBay Order ID associated with this order is (" + externalId + ").");
            }
        } catch (Exception e) {
            result = ServiceUtil.returnFailure(e.getMessage());
        }
        return result;
    }

    private static void addItem(ShoppingCart cart, Map<String, Object> orderItem, LocalDispatcher dispatcher, Delegator delegator, int groupIdx) throws GeneralException {
        String productId = orderItem.get("productId").toString();
        GenericValue product = EntityQuery.use(delegator).from("Product").where("productId", productId).queryOne();
        if (UtilValidate.isEmpty(product)) {
            Debug.logError("The product having ID (" + productId + ") is misssing in the system.", module);
        }
        BigDecimal qty = new BigDecimal(orderItem.get("quantity").toString());
        String itemPrice = orderItem.get("transactionPrice").toString();
        if (UtilValidate.isEmpty(itemPrice)) {
            itemPrice = orderItem.get("amount").toString();
        }
        BigDecimal price = new BigDecimal(itemPrice);
        price = price.setScale(ShoppingCart.scale, ShoppingCart.rounding);

        HashMap<String, Object> attrs = new HashMap<String, Object>();
        attrs.put("shipGroup", groupIdx);

        int idx = cart.addItemToEnd(productId, null, qty, null, null, attrs, null, null, dispatcher, Boolean.FALSE, Boolean.TRUE, Boolean.TRUE, Boolean.TRUE);
        ShoppingCartItem cartItem = cart.findCartItem(idx);
        cartItem.setQuantity(qty, dispatcher, cart, true, false);
        // locate the price verify it matches the expected price
        BigDecimal cartPrice = cartItem.getBasePrice();
        cartPrice = cartPrice.setScale(ShoppingCart.scale, ShoppingCart.rounding);
        if (price.doubleValue() != cartPrice.doubleValue()) {
            // does not match; honor the price but hold the order for manual review
            cartItem.setIsModifiedPrice(true);
            cartItem.setBasePrice(price);
            cart.setHoldOrder(true);
            cart.addInternalOrderNote("Price received [" + price + "] (for item # " + productId + ") from eBay Checkout does not match the price in the database [" + cartPrice + "]. Order is held for manual review.");
        }
        // assign the item to its ship group
        cart.setItemShipGroupQty(cartItem, qty, groupIdx);
    }
}
