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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javolution.util.FastMap;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilGenerics;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.order.order.OrderChangeHelper;
import org.ofbiz.order.shoppingcart.CheckOutHelper;
import org.ofbiz.order.shoppingcart.ItemNotFoundException;
import org.ofbiz.order.shoppingcart.ShoppingCart;
import org.ofbiz.order.shoppingcart.ShoppingCartItem;
import org.ofbiz.party.party.PartyWorker;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceUtil;

import com.google.checkout.checkout.Item;
import com.google.checkout.notification.Address;
import com.google.checkout.notification.AuthorizationAmountNotification;
import com.google.checkout.notification.ChargeAmountNotification;
import com.google.checkout.notification.ChargebackAmountNotification;
import com.google.checkout.notification.FinancialOrderState;
import com.google.checkout.notification.MerchantCodes;
import com.google.checkout.notification.NewOrderNotification;
import com.google.checkout.notification.OrderAdjustment;
import com.google.checkout.notification.OrderStateChangeNotification;
import com.google.checkout.notification.RefundAmountNotification;
import com.google.checkout.notification.RiskInformationNotification;
import com.google.checkout.notification.Shipping;
import com.google.checkout.notification.StructuredName;

public class GoogleCheckoutHelper {

    private static final String module = GoogleCheckoutHelper.class.getName();

    public static final String SALES_CHANNEL = "GC_SALES_CHANNEL";
    public static final String ORDER_TYPE = "SALES_ORDER";
    public static final String PAYMENT_METHOD = "EXT_GOOGLE_CHECKOUT";

    public static final int SHIPPING_ADDRESS = 10;
    public static final int BILLING_ADDRESS = 50;

    protected LocalDispatcher dispatcher;
    protected Delegator delegator;
    protected GenericValue system;

    public GoogleCheckoutHelper(LocalDispatcher dispatcher, Delegator delegator) {
        this.dispatcher = dispatcher;
        this.delegator = delegator;

        try {
            system = delegator.findOne("UserLogin", true, "userLoginId", "system");
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            system = delegator.makeValue("UserLogin");
            system.set("userLoginId", "system");
            system.set("partyId", "admin");
            system.set("isSystem", "Y");
        }
    }

    public void processStateChange(OrderStateChangeNotification info) throws GeneralException {
        String externalId = info.getGoogleOrderNumber();
        GenericValue order = null;
        try {
            List<GenericValue> orders = delegator.findByAnd("OrderHeader", UtilMisc.toMap("externalId", externalId, "salesChannelEnumId" , SALES_CHANNEL), null, false);
            order = EntityUtil.getFirst(orders);
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }

        if (order != null) {
            String orderId = order.getString("orderId");

            // check for a financial state change
            FinancialOrderState oldFin = info.getPreviousFinancialOrderState();
            FinancialOrderState newFin = info.getNewFinancialOrderState();
            if (!oldFin.equals(newFin)) {
                // financial state change
                if (newFin.equals(FinancialOrderState.CANCELLED) || newFin.equals(FinancialOrderState.CANCELLED_BY_GOOGLE)) {
                    // cancel the order
                    if (!"ORDER_CANCELLED".equals(order.getString("statusId"))) {
                        OrderChangeHelper.cancelOrder(dispatcher, system, orderId);
                    }
                } else if (newFin.equals(FinancialOrderState.CHARGEABLE) && oldFin.equals(FinancialOrderState.REVIEWING)) {
                    // approve the order
                    if (!"ORDER_APPROVED".equals(order.getString("statusId"))) {
                        OrderChangeHelper.approveOrder(dispatcher, system, orderId, hasHoldOrderNotes(orderId));
                    }
                } else if (newFin.equals(FinancialOrderState.PAYMENT_DECLINED)) {
                    // reject the order
                    if (!"ORDER_REJECTED".equals(order.getString("statusId"))) {
                        OrderChangeHelper.rejectOrder(dispatcher, system, orderId);
                    }
                }
                // TODO: look at how to implement the other state changes
                // CHARGED, CHARGING
            }
        }
    }

    public void processRiskNotification(RiskInformationNotification info) throws GeneralException {
        // TODO implement me (if desired)
        return; // the notification will be accepted
    }

    public void processAuthNotification(AuthorizationAmountNotification info) throws GeneralException {
        String externalId = info.getGoogleOrderNumber();
        List<GenericValue> orders = null;
        GenericValue orderPaymentPreference = null;
        try {
            orders = delegator.findByAnd("OrderHeader", UtilMisc.toMap("externalId", externalId, "salesChannelEnumId", SALES_CHANNEL), null, false);
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }
        if (UtilValidate.isNotEmpty(orders)) {
            GenericValue order = EntityUtil.getFirst(orders);
            List<GenericValue> orderPaymentPreferences = order.getRelated("OrderPaymentPreference", null, null, false);
            if (UtilValidate.isNotEmpty(orderPaymentPreferences)) {
                orderPaymentPreference = EntityUtil.getFirst(orderPaymentPreferences);
                BigDecimal maxAmount = new BigDecimal(info.getAuthorizationAmount());
                //Update orderPaymentPreference
                Map<String, Object> paymentPrefMap = UtilMisc.toMap("orderPaymentPreferenceId", orderPaymentPreference.get("orderPaymentPreferenceId"), "maxAmount", maxAmount, "statusId", "PAYMENT_AUTHORIZED", "paymentMethodTypeId", "EXT_GOOGLE_CHECKOUT");
                updatePaymentPreference(paymentPrefMap);
                //Create PaymentGatewayResponse
                Map<String, Object> newGatewayMap = UtilMisc.toMap("amount", maxAmount, "transCodeEnumId", "PGT_AUTHORIZE", "referenceNum", externalId, "gatewayAvsResult", info.getAvsResponse(), "currencyUomId", info.getCurrencyCode());
                newGatewayMap.put("orderPaymentPreferenceId", orderPaymentPreference.get("orderPaymentPreferenceId"));
                newGatewayMap.put("paymentMethodTypeId", orderPaymentPreference.get("paymentMethodTypeId"));
                newGatewayMap.put("transactionDate", order.getTimestamp("orderDate"));
                createPaymentGatewayResponse(newGatewayMap);
            }
        }
        return;
    }

    public void processChargeNotification(ChargeAmountNotification info) throws GeneralException {
        String externalId = info.getGoogleOrderNumber();
        List<GenericValue> orders = null;
        GenericValue orderPaymentPreference = null;
        try {
            orders = delegator.findByAnd("OrderHeader", UtilMisc.toMap("externalId", externalId, "salesChannelEnumId", SALES_CHANNEL), null, false);
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }
        if (UtilValidate.isNotEmpty(orders)) {
            GenericValue order = EntityUtil.getFirst(orders);
            List<GenericValue> orderPaymentPreferences = order.getRelated("OrderPaymentPreference", null, null, false);
            if (UtilValidate.isNotEmpty(orderPaymentPreferences)) {
                orderPaymentPreference = EntityUtil.getFirst(orderPaymentPreferences);
                BigDecimal maxAmount = new BigDecimal(info.getTotalChargeAmount());
                //Update orderPaymentPreference
                Map<String, Object> paymentPrefMap = UtilMisc.toMap("orderPaymentPreferenceId", orderPaymentPreference.get("orderPaymentPreferenceId"), "maxAmount", maxAmount, "statusId", "PAYMENT_SETTLED", "paymentMethodTypeId", "EXT_GOOGLE_CHECKOUT");
                updatePaymentPreference(paymentPrefMap);
                //Create PaymentGatewayResponse
                maxAmount = new BigDecimal(info.getLatestChargeAmount());
                Map<String, Object> newGatewayMap = UtilMisc.toMap("amount", maxAmount, "transCodeEnumId", "PGT_CAPTURE", "referenceNum", externalId, "currencyUomId", info.getCurrencyCode());
                newGatewayMap.put("orderPaymentPreferenceId", orderPaymentPreference.get("orderPaymentPreferenceId"));
                newGatewayMap.put("paymentMethodTypeId", orderPaymentPreference.get("paymentMethodTypeId"));
                newGatewayMap.put("transactionDate", order.getTimestamp("orderDate"));
                createPaymentGatewayResponse(newGatewayMap);
            }
        }
        return;
    }

    public void processRefundNotification(RefundAmountNotification info) throws GeneralException {
        String externalId = info.getGoogleOrderNumber();
        List<GenericValue> orders = null;
        GenericValue orderPaymentPreference = null;
        try {
            orders = delegator.findByAnd("OrderHeader", UtilMisc.toMap("externalId", externalId, "salesChannelEnumId", SALES_CHANNEL), null, false);
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }
        if (UtilValidate.isNotEmpty(orders)) {
            GenericValue order = EntityUtil.getFirst(orders);
            List<GenericValue> orderPaymentPreferences = order.getRelated("OrderPaymentPreference", null, null, false);
            if (UtilValidate.isNotEmpty(orderPaymentPreferences)) {
                orderPaymentPreference = EntityUtil.getFirst(orderPaymentPreferences);
                BigDecimal maxAmount = new BigDecimal(info.getTotalRefundAmount());
                //Update orderPaymentPreference
                Map<String, Object> paymentPrefMap = UtilMisc.toMap("orderPaymentPreferenceId", orderPaymentPreference.get("orderPaymentPreferenceId"), "maxAmount", maxAmount, "statusId", "PAYMENT_REFUNDED", "paymentMethodTypeId", "EXT_GOOGLE_CHECKOUT");
                updatePaymentPreference(paymentPrefMap);
                //Create PaymentGatewayResponse
                maxAmount = new BigDecimal(info.getLatestRefundAmount());
                Map<String, Object> newGatewayMap = UtilMisc.toMap("amount", maxAmount, "transCodeEnumId", "PGT_REFUND", "referenceNum", externalId, "currencyUomId", info.getCurrencyCode());
                newGatewayMap.put("orderPaymentPreferenceId", orderPaymentPreference.get("orderPaymentPreferenceId"));
                newGatewayMap.put("paymentMethodTypeId", orderPaymentPreference.get("paymentMethodTypeId"));
                newGatewayMap.put("transactionDate", order.getTimestamp("orderDate"));
                createPaymentGatewayResponse(newGatewayMap);
            }
        }
        return;
    }

    public void processChargeBackNotification(ChargebackAmountNotification info) throws GeneralException {
        // TODO: implement me (if desired)
        return; // the notification will be accepted
    }

    public void createOrder(NewOrderNotification info, String productStoreId, Locale locale) throws GeneralException {
        // get the google order number
        String externalId = info.getGoogleOrderNumber();

        // check and make sure this order doesn't already exist
        List<GenericValue> existingOrder = delegator.findByAnd("OrderHeader", UtilMisc.toMap("externalId", externalId), null, false);
        if (UtilValidate.isNotEmpty(existingOrder)) {
            //throw new GeneralException("Google order #" + externalId + " already exists.");
            Debug.logWarning("Google order #" + externalId + " already exists.", module);
            return;
        }

        // get the config object
        GenericValue googleCfg = GoogleRequestServices.getGoogleConfiguration(delegator, productStoreId);
        if (googleCfg == null) {
            throw new GeneralException("No google configuration found for product store : " + productStoreId);
        }

        String websiteId = googleCfg.getString("webSiteId");
        String currencyUom = googleCfg.getString("currencyUomId");
        String prodCatalogId = googleCfg.getString("prodCatalogId");
        boolean errorOnUnknownItem = googleCfg.get("errorOnBadItem") != null &&
            "Y".equalsIgnoreCase(googleCfg.getString("errorOnBadItem")) ? true : false;

        // Initialize the shopping cart
        ShoppingCart cart = new ShoppingCart(delegator, productStoreId, websiteId, locale, currencyUom);
        cart.setUserLogin(system, dispatcher);
        cart.setOrderType(ORDER_TYPE);
        cart.setChannelType(SALES_CHANNEL);
        //cart.setOrderDate(UtilDateTime.toTimestamp(info.getTimestamp().()));
        cart.setExternalId(externalId);

        Debug.logInfo("Created shopping cart for Google order: ", module);
        Debug.logInfo("-- WebSite : " + websiteId, module);
        Debug.logInfo("-- Product Store : " + productStoreId, module);
        Debug.logInfo("-- Locale : " + locale.toString(), module);
        Debug.logInfo("-- Google Order # : " + externalId, module);

        // set the customer information
        Address shippingAddress = info.getBuyerShippingAddress();
        Address billingAddress = info.getBuyerBillingAddress();
        String[] partyInfo = getPartyInfo(shippingAddress, billingAddress);
        if (partyInfo == null || partyInfo.length != 3) {
            throw new GeneralException("Unable to parse/create party information, invalid number of parameters returned");
        }

        cart.setOrderPartyId(partyInfo[0]);
        cart.setPlacingCustomerPartyId(partyInfo[0]);
        cart.setShippingContactMechId(partyInfo[1]);

        // contact info
        String shippingEmail = shippingAddress.getEmail();
        if (UtilValidate.isNotEmpty(shippingEmail)) {
            setContactInfo(cart, "PRIMARY_EMAIL", shippingEmail);
        }
        String billingEmail = billingAddress.getEmail();
        if (UtilValidate.isNotEmpty(billingEmail)) {
            setContactInfo(cart, "BILLING_EMAIL", billingEmail);
        }
        String shippingPhone = shippingAddress.getPhone();
        if (UtilValidate.isNotEmpty(shippingPhone)) {
            setContactInfo(cart, "PHONE_SHIPPING", shippingPhone);
        }
        String billingPhone = billingAddress.getPhone();
        if (UtilValidate.isNotEmpty(billingPhone)) {
            setContactInfo(cart, "PHONE_BILLING", billingPhone);
        }

        // set the order items
        Collection<Item> items = UtilGenerics.checkCollection(info.getShoppingCart().getItems());
        for (Item item : items) {
            try {
                addItem(cart, item, prodCatalogId, 0);
            } catch (ItemNotFoundException e) {
                Debug.logWarning(e, "Item was not found : " + item.getMerchantItemId(), module);
                // throwing this exception tell google the order failed; it will continue to retry
                if (errorOnUnknownItem) {
                    throw new GeneralException("Invalid item requested from Google Checkout - " + item.getMerchantItemId());
                }
            }
        }

        // handle the adjustments
        OrderAdjustment adjustment = info.getOrderAdjustment();
        if (adjustment != null) {
            addAdjustments(cart, adjustment);
            // ship group info
            Shipping shipping = info.getOrderAdjustment().getShipping();
            addShipInfo(cart, shipping, partyInfo[1]);
        }

        // set the cart payment method
        cart.addPaymentAmount(PAYMENT_METHOD, new BigDecimal(info.getOrderTotal()));

        // validate the payment methods
        CheckOutHelper coh = new CheckOutHelper(dispatcher, delegator, cart);
        Map<String, Object> validateResp = coh.validatePaymentMethods();
        if (ServiceUtil.isError(validateResp)) {
            throw new GeneralException(ServiceUtil.getErrorMessage(validateResp));
        }

        // create the order & process payments
        Map<String, Object> createResp = coh.createOrder(system);
        String orderId = cart.getOrderId();
        if (ServiceUtil.isError(createResp)) {
            throw new GeneralException(ServiceUtil.getErrorMessage(createResp));
        }

        // notify google of our order number
        try {
            dispatcher.runAsync("sendGoogleOrderNumberRequest", UtilMisc.toMap("orderId", orderId), true);
        } catch (GeneralException e) {
            Debug.logError(e, module);
        }
    }

    protected void addItem(ShoppingCart cart, Item item, String prodCatalogId, int groupIdx) throws GeneralException {
        String productId = item.getMerchantItemId();
        BigDecimal qty = new BigDecimal(item.getQuantity());
        BigDecimal price = new BigDecimal(item.getUnitPriceAmount());
        price = price.setScale(ShoppingCart.scale, ShoppingCart.rounding);

        HashMap<String, Object> attrs = new HashMap<String, Object>();
        attrs.put("shipGroup", groupIdx);

        int idx = cart.addItemToEnd(productId, null, qty, null, null, attrs, prodCatalogId, null, dispatcher, Boolean.FALSE, Boolean.TRUE, Boolean.TRUE, Boolean.TRUE);
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
            cart.addInternalOrderNote("Price received [" + price + "] (for item # " + productId + ") from Google Checkout does not match the price in the database [" + cartPrice + "]. Order is held for manual review.");
        }

        // assign the item to its ship group
        cart.setItemShipGroupQty(cartItem, qty, groupIdx);
    }

    protected void addAdjustments(ShoppingCart cart, OrderAdjustment adjustment) {
        // handle shipping
        Shipping shipping = adjustment.getShipping();
        BigDecimal shipAmount = new BigDecimal(shipping.getShippingCost());
        GenericValue shipAdj = delegator.makeValue("OrderAdjustment", FastMap.newInstance());
        shipAdj.set("orderAdjustmentTypeId", "SHIPPING_CHARGES");
        shipAdj.set("amount", shipAmount);
        cart.addAdjustment(shipAdj);

        // handle tax
        BigDecimal taxAmount = new BigDecimal(adjustment.getTotalTax());
        GenericValue taxAdj = delegator.makeValue("OrderAdjustment", FastMap.newInstance());
        taxAdj.set("orderAdjustmentTypeId", "SALES_TAX");
        taxAdj.set("amount", taxAmount);
        cart.addAdjustment(taxAdj);

        // handle promotions
        Collection<MerchantCodes> merchantCodes = UtilGenerics.checkCollection(adjustment.getMerchantCodes());
        for (MerchantCodes codes : merchantCodes) {
            GenericValue promoAdj = delegator.makeValue("OrderAdjustment", FastMap.newInstance());
            promoAdj.set("orderAdjustmentTypeId", "PROMOTION_ADJUSTMENT");
            promoAdj.set("description", "Promotion Code: " + codes.getCode());
            promoAdj.set("comments", "Google Promotion: " + codes.getMessage());
            promoAdj.set("amount", new BigDecimal(-1 * (codes.getAppliedAmount()))); // multiply by -1
            cart.addAdjustment(promoAdj);
        }
    }

    protected void addShipInfo(ShoppingCart cart, Shipping shipping, String shipContactMechId) {
        String shippingName = shipping.getShippingName();
        GenericValue googleShipping = null;
        try {
            googleShipping = delegator.findOne("GoogleCoShippingMethod", UtilMisc.toMap("shipmentMethodName", shippingName,
                    "productStoreId", cart.getProductStoreId()), false);
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }
        if (googleShipping != null) {
            String shipmentMethodTypeId = googleShipping.getString("shipmentMethodTypeId");
            String carrierPartyId = googleShipping.getString("carrierPartyId");
            Boolean maySplit = Boolean.FALSE;

            cart.setShipmentMethodTypeId(shipmentMethodTypeId);
            cart.setCarrierPartyId(carrierPartyId);
            cart.setMaySplit(maySplit);
            cart.setShippingContactMechId(shipContactMechId);
        } else {
            Debug.logWarning("No valid fulfillment method found! No shipping info set!", module);
        }
    }

    protected String[] getPartyInfo(Address shipAddr, Address billAddr) throws GeneralException {
        String shipCmId = null;
        String billCmId = null;
        String partyId = null;

        // look for an existing shipping address
        List<GenericValue> shipInfo = PartyWorker.findMatchingPersonPostalAddresses(delegator, shipAddr.getAddress1(),
                (UtilValidate.isEmpty(shipAddr.getAddress2()) ? null : shipAddr.getAddress2()), shipAddr.getCity(), shipAddr.getRegion(),
                shipAddr.getPostalCode(), null, getCountryGeoId(shipAddr.getCountryCode()), shipAddr.getStructuredName().getFirstName(),
                null, shipAddr.getStructuredName().getLastName());

        if (UtilValidate.isNotEmpty(shipInfo)) {
            GenericValue first = EntityUtil.getFirst(shipInfo);
            shipCmId = first.getString("contactMechId");
            partyId = first.getString("partyId");
            Debug.logInfo("Existing shipping address found : " + shipCmId + " (party: " + partyId + ")", module);
        }

        // look for an existing billing address
        List<GenericValue> billInfo = PartyWorker.findMatchingPersonPostalAddresses(delegator, billAddr.getAddress1(),
                (UtilValidate.isEmpty(billAddr.getAddress2()) ? null : billAddr.getAddress2()), billAddr.getCity(), billAddr.getRegion(),
                billAddr.getPostalCode(), null, getCountryGeoId(billAddr.getCountryCode()), billAddr.getStructuredName().getFirstName(),
                null, billAddr.getStructuredName().getLastName());

        if (UtilValidate.isNotEmpty(billInfo)) {
            GenericValue first = EntityUtil.getFirst(billInfo);
            billCmId = first.getString("contactMechId");
            if (partyId == null) {
                partyId = first.getString("partyId");
            } else {
               String billPartyId = first.getString("partyId");
                if (!billPartyId.equals(partyId)) {
                    // address found for a different partyID -- probably a duplicate
                    Debug.logWarning("Duplicate partyId found : " + billPartyId + " -> " + partyId, module);
                }
            }
            Debug.logInfo("Existing billing address found : " + billCmId + " (party: " + partyId + ")", module);
        }

        // create the party if necessary
        if (partyId == null) {
            partyId = createPerson(shipAddr.getStructuredName());
        }

        // create the shipping address if necessary
        if (shipCmId == null) {
            shipCmId = createPartyAddress(partyId, shipAddr);
            addPurposeToAddress(partyId, shipCmId, SHIPPING_ADDRESS);
        }

        // create the billing address if necessary
        if (billCmId == null) {
            // check the billing address again (in case it was just created)
            billInfo = PartyWorker.findMatchingPersonPostalAddresses(delegator, billAddr.getAddress1(),
                    billAddr.getAddress2(), billAddr.getCity(), billAddr.getRegion(),
                    billAddr.getPostalCode(), null, getCountryGeoId(billAddr.getCountryCode()), billAddr.getStructuredName().getFirstName(),
                    null, billAddr.getStructuredName().getLastName());

            if (UtilValidate.isNotEmpty(billInfo)) {
                GenericValue first = EntityUtil.getFirst(billInfo);
                billCmId = first.getString("contactMechId");
           } else {
                billCmId = createPartyAddress(partyId, shipAddr);
                addPurposeToAddress(partyId, billCmId, BILLING_ADDRESS);
            }
        }

        return new String[] { partyId, shipCmId, billCmId };
    }

    protected String createPerson(StructuredName name) throws GeneralException {
        Map<String, Object> personMap = FastMap.newInstance();
        personMap.put("firstName", name.getFirstName());
        personMap.put("lastName", name.getLastName());
        personMap.put("userLogin", system);

        Map<String, Object> personResp = dispatcher.runSync("createPerson", personMap);
        if (ServiceUtil.isError(personResp)) {
           throw new GeneralException("Unable to create new customer account: " + ServiceUtil.getErrorMessage(personResp));
        }
        String partyId = (String) personResp.get("partyId");

        Debug.logInfo("New party created : " + partyId, module);
        return partyId;
    }

    protected String createPartyAddress(String partyId, Address addr) throws GeneralException {
        // check for zip+4
        String postalCode = addr.getPostalCode();
        String postalCodeExt = null;
        if (postalCode.length() == 10 && postalCode.indexOf("-") != -1) {
            String[] strSplit = postalCode.split("-", 2);
            postalCode = strSplit[0];
            postalCodeExt = strSplit[1];
        }

        // prepare the create address map
        Map<String, Object> addrMap = FastMap.newInstance();
        addrMap.put("partyId", partyId);
        addrMap.put("toName", addr.getContactName());
        addrMap.put("address1", addr.getAddress1());
        addrMap.put("address2", addr.getAddress2());
        addrMap.put("city", addr.getCity());
        addrMap.put("stateProvinceGeoId",addr.getRegion());
        addrMap.put("countryGeoId", getCountryGeoId(addr.getCountryCode()));
        addrMap.put("postalCode", postalCode);
        addrMap.put("postalCodeExt", postalCodeExt);
        addrMap.put("allowSolicitation", "Y");
        addrMap.put("contactMechPurposeTypeId", "GENERAL_LOCATION");
        addrMap.put("userLogin", system); // run as the system user

        // invoke the create address service
        Map<String, Object> addrResp = dispatcher.runSync("createPartyPostalAddress", addrMap);
        if (ServiceUtil.isError(addrResp)) {
            throw new GeneralException("Unable to create new customer address record: " +
                    ServiceUtil.getErrorMessage(addrResp));
        }
        String contactMechId = (String) addrResp.get("contactMechId");

        Debug.logInfo("Created new address for partyId [" + partyId + "] :" + contactMechId, module);
        return contactMechId;
    }

    protected void addPurposeToAddress(String partyId, String contactMechId, int addrType) throws GeneralException {
        // convert the int to a purpose type ID
        String contactMechPurposeTypeId = getAddressType(addrType);

        // check to make sure the purpose doesn't already exist
        List<GenericValue> values = delegator.findByAnd("PartyContactMechPurpose", UtilMisc.toMap("partyId", partyId,
                "contactMechId", contactMechId, "contactMechPurposeTypeId", contactMechPurposeTypeId), null, false);

        if (UtilValidate.isEmpty(values)) {
            Map<String, Object> addPurposeMap = FastMap.newInstance();
            addPurposeMap.put("contactMechId", contactMechId);
            addPurposeMap.put("partyId", partyId);
            addPurposeMap.put("contactMechPurposeTypeId", contactMechPurposeTypeId);
            addPurposeMap.put("userLogin", system);

            Map<String, Object> addPurposeResp = dispatcher.runSync("createPartyContactMechPurpose", addPurposeMap);
            if (addPurposeResp != null && ServiceUtil.isError(addPurposeResp)) {
                throw new GeneralException(ServiceUtil.getErrorMessage(addPurposeResp));
            }
        }
    }

    protected String getAddressType(int addrType) {
        String contactMechPurposeTypeId = "GENERAL_LOCATION";
        switch (addrType) {
            case SHIPPING_ADDRESS:
                contactMechPurposeTypeId = "SHIPPING_LOCATION";
                break;
            case BILLING_ADDRESS:
                contactMechPurposeTypeId = "BILLING_LOCATION";
                break;
        }
        return contactMechPurposeTypeId;
    }

    protected void setContactInfo(ShoppingCart cart, String contactMechPurposeTypeId, String infoString) throws GeneralException {
        Map<String, Object> lookupMap = FastMap.newInstance();
        String cmId = null;

        String entityName = "PartyAndContactMech";
        if (contactMechPurposeTypeId.startsWith("PHONE_")) {
            lookupMap.put("partyId", cart.getOrderPartyId());
            lookupMap.put("contactNumber", infoString);
            entityName = "PartyAndTelecomNumber";
        } else if (contactMechPurposeTypeId.endsWith("_EMAIL")) {
            lookupMap.put("partyId", cart.getOrderPartyId());
            lookupMap.put("infoString", infoString);
        } else {
            throw new GeneralException("Invalid contact mech type");
        }

        List<GenericValue> cmLookup;
        try {
            cmLookup = delegator.findByAnd(entityName, lookupMap, UtilMisc.toList("-fromDate"), false);
            cmLookup = EntityUtil.filterByDate(cmLookup);
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            throw e;
        }

        if (UtilValidate.isNotEmpty(cmLookup)) {
            GenericValue v = EntityUtil.getFirst(cmLookup);
            if (v != null) {
                cmId = v.getString("contactMechId");
            }
        } else {
            // create it
            lookupMap.put("contactMechPurposeTypeId", contactMechPurposeTypeId);
            lookupMap.put("userLogin", system);
            Map<String, Object> createResp = null;
            if (contactMechPurposeTypeId.startsWith("PHONE_")) {
                try {
                    createResp = dispatcher.runSync("createPartyTelecomNumber", lookupMap);
                } catch (GeneralException e) {
                    Debug.logError(e, module);
                    throw e;
                }
            } else if (contactMechPurposeTypeId.endsWith("_EMAIL")) {
                lookupMap.put("emailAddress", lookupMap.get("infoString"));
                lookupMap.put("allowSolicitation", "Y");
                try {
                    createResp = dispatcher.runSync("createPartyEmailAddress", lookupMap);
                } catch (GeneralException e) {
                    Debug.logError(e, module);
                    throw e;
                }
            }
            if (createResp == null || ServiceUtil.isError(createResp)) {
                throw new GeneralException("Unable to create the request contact mech");
            }

            // get the created ID
            cmId = (String) createResp.get("contactMechId");
        }

        if (cmId != null) {
            cart.addContactMech(contactMechPurposeTypeId, cmId);
        }
    }

    protected String getCountryGeoId(String geoCode) {
        if (geoCode != null && geoCode.length() == 3) {
            return geoCode;
        }
        List<GenericValue> geos = null;
        try {
            geos = delegator.findByAnd("Geo", UtilMisc.toMap("geoCode", geoCode, "geoTypeId", "COUNTRY"), null, false);
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }
        if (UtilValidate.isNotEmpty(geos)) {
            return EntityUtil.getFirst(geos).getString("geoId");
        } else {
            return "_NA_";
        }
    }

    protected boolean hasHoldOrderNotes(String orderId) {
        EntityCondition idCond = EntityCondition.makeCondition("orderId", EntityOperator.EQUALS, orderId);
        EntityCondition content = EntityCondition.makeCondition("noteInfo", EntityOperator.LIKE, "%Order is held%");
        EntityCondition mainCond = EntityCondition.makeCondition(UtilMisc.toList(idCond, content), EntityOperator.AND);
        List<GenericValue> holdOrderNotes = null;
        try {
            holdOrderNotes = delegator.findList("OrderHeaderNoteView", mainCond, null, null, null, false);
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }
        return UtilValidate.isNotEmpty(holdOrderNotes);
    }

    protected void updatePaymentPreference(Map<String, Object> paymentPrefMap) {
        GenericValue newPref = delegator.makeValue("OrderPaymentPreference");
        newPref.set("orderPaymentPreferenceId", paymentPrefMap.get("orderPaymentPreferenceId"));
        newPref.set("createdByUserLogin", system.getString("userLoginId"));
        newPref.setNonPKFields(paymentPrefMap);
        try {
            delegator.store(newPref);
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }
    }

    protected void createPaymentGatewayResponse(Map<String, Object> newGatewayMap) {
        try {
            String responseId = delegator.getNextSeqId("PaymentGatewayResponse");
            GenericValue response = delegator.makeValue("PaymentGatewayResponse");
            response.set("paymentGatewayResponseId", responseId);
            response.setNonPKFields(newGatewayMap);
            delegator.create(response);
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }
    }

}
