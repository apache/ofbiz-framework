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
package org.ofbiz.pos;

import java.io.PrintWriter;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;

import net.xoetrope.xui.data.XModel;
import net.xoetrope.xui.helper.SwingWorker;

import org.ofbiz.accounting.payment.PaymentGatewayServices;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilFormatOut;
import org.ofbiz.base.util.UtilGenerics;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilNumber;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.collections.LifoSet;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityFunction;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.model.DynamicViewEntity;
import org.ofbiz.entity.model.ModelKeyMap;
import org.ofbiz.entity.transaction.GenericTransactionException;
import org.ofbiz.entity.transaction.TransactionUtil;
import org.ofbiz.entity.util.EntityListIterator;
import org.ofbiz.entity.util.EntityQuery;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.entity.util.EntityUtilProperties;
import org.ofbiz.guiapp.xui.XuiSession;
import org.ofbiz.order.shoppingcart.CartItemModifyException;
import org.ofbiz.order.shoppingcart.CheckOutHelper;
import org.ofbiz.order.shoppingcart.ItemNotFoundException;
import org.ofbiz.order.shoppingcart.ShoppingCart;
import org.ofbiz.order.shoppingcart.ShoppingCartItem;
import org.ofbiz.order.shoppinglist.ShoppingListEvents;
import org.ofbiz.party.contact.ContactMechWorker;
import org.ofbiz.pos.component.Journal;
import org.ofbiz.pos.component.JournalLineParams;
import org.ofbiz.pos.component.Output;
import org.ofbiz.pos.device.DeviceLoader;
import org.ofbiz.pos.device.impl.Receipt;
import org.ofbiz.pos.screen.ClientProfile;
import org.ofbiz.pos.screen.LoadSale;
import org.ofbiz.pos.screen.PosScreen;
import org.ofbiz.pos.screen.SaveSale;
import org.ofbiz.product.config.ProductConfigWrapper;
import org.ofbiz.product.config.ProductConfigWrapper.ConfigOption;
import org.ofbiz.product.product.ProductWorker;
import org.ofbiz.product.store.ProductStoreWorker;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceUtil;

@SuppressWarnings("serial")
public class PosTransaction implements Serializable {

    public static final int scale = UtilNumber.getBigDecimalScale("order.decimals");
    public static final int rounding = UtilNumber.getBigDecimalRoundingMode("order.rounding");
    public static final BigDecimal ZERO = (BigDecimal.ZERO).setScale(scale, rounding);

    public static final String resource = "PosUiLabels";
    public static final String module = PosTransaction.class.getName();
    public static final int NO_PAYMENT = 0;
    public static final int INTERNAL_PAYMENT = 1;
    public static final int EXTERNAL_PAYMENT = 2;

    private static PosTransaction currentTx = null;
    private static LifoSet<PosTransaction> savedTx = new LifoSet<PosTransaction>();

    protected XuiSession session = null;
    protected ShoppingCart cart = null;
    protected CheckOutHelper ch = null;
    protected PrintWriter trace = null;
    protected GenericValue txLog = null;

    protected String productStoreId = null;
    protected String transactionId = null;
    protected String facilityId = null;
    protected String terminalId = null;
    protected String currency = null;
    protected String orderId = null;
    protected String partyId = null;
    protected Locale locale = null;
    protected boolean isOpen = false;
    protected int drawerIdx = 0;

    private GenericValue shipAddress = null;
    private int cartDiscount = -1;


    public PosTransaction(XuiSession session) {
        this.session = session;
        this.terminalId = session.getId();
        this.partyId = "_NA_";
        //this.trace = defaultPrintWriter;

        this.productStoreId = (String) session.getAttribute("productStoreId");
        this.facilityId = (String) session.getAttribute("facilityId");
        this.currency = (String) session.getAttribute("currency");
//        this.locale = (Locale) session.getAttribute("locale"); This is legacy code and may come (demo) from ProductStore.defaultLocaleString defined in demoRetail and is incompatible with how localisation is handled in the POS
        this.locale = Locale.getDefault();

        this.cart = new ShoppingCart(session.getDelegator(), productStoreId, locale, currency);
        this.ch = new CheckOutHelper(session.getDispatcher(), session.getDelegator(), cart);
        cart.setChannelType("POS_SALES_CHANNEL");
        cart.setTransactionId(transactionId);
        cart.setFacilityId(facilityId);
        cart.setTerminalId(terminalId);
        if (session.getUserLogin() != null) {
            cart.addAdditionalPartyRole(session.getUserLogin().getString("partyId"), "SALES_REP");
        }

        // setup the TX log
        this.transactionId = session.getDelegator().getNextSeqId("PosTerminalLog");
        txLog = session.getDelegator().makeValue("PosTerminalLog");
        txLog.set("posTerminalLogId", this.transactionId);
        txLog.set("posTerminalId", terminalId);
        txLog.set("transactionId", transactionId);
        txLog.set("userLoginId", session.getUserId());
        txLog.set("statusId", "POSTX_ACTIVE");
        txLog.set("logStartDateTime", UtilDateTime.nowTimestamp());
        try {
            txLog.create();
        } catch (GenericEntityException e) {
            Debug.logError(e, "Unable to create TX log - not fatal", module);
        }

        currentTx = this;
        trace("transaction created");
    }

    public XuiSession getSession() {
        return session;
    }

    public String getUserId() {
        return session.getUserId();
    }

    public String getPartyId() {
        return partyId;
    }

    public void setPartyId(String partyId) {
        this.partyId = partyId;
        this.cart.setPlacingCustomerPartyId(partyId);
        try {
            this.cart.setUserLogin(session.getUserLogin(), session.getDispatcher());
        } catch (CartItemModifyException e) {
            Debug.logError(e, module);
        }
    }

    public int getDrawerNumber() {
        return drawerIdx + 1;
    }

    public void popDrawer() {
        DeviceLoader.drawer[drawerIdx].openDrawer();
    }

    public String getTransactionId() {
        return this.transactionId;
    }

    public String getTerminalId() {
        return this.terminalId;
    }

    public String getFacilityId() {
        return this.facilityId;
    }

    public String getTerminalLogId() {
        return txLog.getString("posTerminalLogId");
    }

    public boolean isOpen() {
        if (!this.isOpen) {
            GenericValue terminalState = this.getTerminalState();
            if (terminalState != null) {
                this.isOpen = true;
            } else {
                this.isOpen = false;
            }
        }
        return this.isOpen;
    }

    public boolean isEmpty() {
        return (UtilValidate.isEmpty(cart));
    }

    public List<GenericValue> lookupItem(String sku) throws GeneralException {
        return ProductWorker.findProductsById(session.getDelegator(), sku, null);
    }

    public String getOrderId() {
        return this.orderId;
    }

    public BigDecimal getTaxTotal() {
        return cart.getTotalSalesTax();
    }

    public BigDecimal getGrandTotal() {
        return cart.getGrandTotal();
    }

    public int getNumberOfPayments() {
        return cart.selectedPayments();
    }

    public BigDecimal getPaymentTotal() {
        return cart.getPaymentTotal();
    }

    public BigDecimal getTotalDue() {
        BigDecimal grandTotal = this.getGrandTotal();
        BigDecimal paymentAmt = this.getPaymentTotal();
        return grandTotal.subtract(paymentAmt);
    }

    public int size() {
        return cart.size();
    }

    public Map<String, Object> getItemInfo(int index) {
        ShoppingCartItem item = cart.findCartItem(index);
        Map<String, Object> itemInfo = new HashMap<String, Object>();
        itemInfo.put("productId", item.getProductId());
        String description = item.getDescription();
        if (UtilValidate.isEmpty(description)) {
            itemInfo.put("description", item.getName());
        } else {
            itemInfo.put("description", description);
        }
        itemInfo.put("quantity", UtilFormatOut.formatQuantity(item.getQuantity()));
        itemInfo.put("subtotal", UtilFormatOut.formatPrice(item.getItemSubTotal()));
        itemInfo.put("isTaxable", item.taxApplies() ? "T" : " ");

        itemInfo.put("discount", "");
        itemInfo.put("adjustments", "");
        if (item.getOtherAdjustments().compareTo(BigDecimal.ZERO) != 0) {
            itemInfo.put("itemDiscount", UtilFormatOut.padString(
                    UtilProperties.getMessage(resource, "PosItemDiscount", locale), Receipt.pridLength[0] + 1, true, ' '));
            itemInfo.put("adjustments", UtilFormatOut.formatPrice(item.getOtherAdjustments()));
        }

        if (isAggregatedItem(item.getProductId())) {
            ProductConfigWrapper pcw = null;
            pcw = item.getConfigWrapper();
            itemInfo.put("basePrice", UtilFormatOut.formatPrice(pcw.getDefaultPrice()));
        } else {
            itemInfo.put("basePrice", UtilFormatOut.formatPrice(item.getBasePrice()));
        }
        return itemInfo;
    }

    public List<Map<String, Object>> getItemConfigInfo(int index) {
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        // I think I need to initialize the list in a special way
        // to use foreach in receipt.java

        ShoppingCartItem item = cart.findCartItem(index);
        if (this.isAggregatedItem(item.getProductId())) {
            ProductConfigWrapper pcw = null;
            pcw = item.getConfigWrapper();
            List<ConfigOption> selected = pcw.getSelectedOptions();
            for (ConfigOption configoption : selected) {
                Map<String, Object> itemInfo = new HashMap<String, Object>();
                if (configoption.isSelected() && !configoption.isDefault()) {
                    itemInfo.put("productId", "");
                    itemInfo.put("sku", "");
                    itemInfo.put("configDescription", configoption.getDescription(locale));
                    itemInfo.put("configQuantity", UtilFormatOut.formatQuantity(item.getQuantity()));
                    itemInfo.put("configBasePrice", UtilFormatOut.formatPrice(configoption.getOffsetPrice()));
                    //itemInfo.put("isTaxable", item.taxApplies() ? "T" : " ");
                    list.add(itemInfo);
                }
            }
        }
        return list;
    }

    public Map<String, Object> getPaymentInfo(int index) {
        ShoppingCart.CartPaymentInfo inf = cart.getPaymentInfo(index);
        GenericValue infValue = inf.getValueObject(session.getDelegator());
        GenericValue paymentPref = null;
        try {
            Map<String, Object> fields = new HashMap<String, Object>();
            fields.put("paymentMethodTypeId", inf.paymentMethodTypeId);
            if (inf.paymentMethodId != null) {
                fields.put("paymentMethodId", inf.paymentMethodId);
            }
            fields.put("maxAmount", inf.amount);
            fields.put("orderId", this.getOrderId());

            List<GenericValue> paymentPrefs = session.getDelegator().findByAnd("OrderPaymentPreference", fields, null, false);
            if (UtilValidate.isNotEmpty(paymentPrefs)) {
                //Debug.logInfo("Found some prefs - " + paymentPrefs.size(), module);
                if (paymentPrefs.size() > 1) {
                    Debug.logError("Multiple OrderPaymentPreferences found for the same payment method!", module);
                } else {
                    paymentPref = EntityUtil.getFirst(paymentPrefs);
                    //Debug.logInfo("Got the first pref - " + paymentPref, module);
                }
            } else {
                Debug.logError("No OrderPaymentPreference found - " + fields, module);
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }
        //Debug.logInfo("PaymentPref - " + paymentPref, module);

        Map<String, Object> payInfo = new HashMap<String, Object>();

        // locate the auth info
        GenericValue authTrans = null;
        if (paymentPref != null) {
            authTrans = PaymentGatewayServices.getAuthTransaction(paymentPref);
            if (authTrans != null) {
                payInfo.putAll(authTrans);

                String authInfoString = "Ref: " + authTrans.getString("referenceNum") + " Auth: " + authTrans.getString("gatewayCode");
                payInfo.put("authInfoString", authInfoString);
            } else {
                Debug.logError("No Authorization transaction found for payment preference - " + paymentPref, module);
            }
        } else {
            Debug.logError("Payment preference is empty!", module);
            return payInfo;
        }
        //Debug.logInfo("AuthTrans - " + authTrans, module);

        if ("PaymentMethodType".equals(infValue.getEntityName())) {
            payInfo.put("description", infValue.get("description", locale));
            payInfo.put("payInfo", infValue.get("description", locale));
            payInfo.put("amount", UtilFormatOut.formatPrice(inf.amount));
        } else {
            String paymentMethodTypeId = infValue.getString("paymentMethodTypeId");
            GenericValue pmt = null;
            try {
                 pmt = infValue.getRelatedOne("PaymentMethodType", false);
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
            }
            if (pmt != null) {
                payInfo.put("description", pmt.get("description", locale));
                payInfo.put("amount", UtilFormatOut.formatPrice(inf.amount));
            }

            if ("CREDIT_CARD".equals(paymentMethodTypeId)) {
                GenericValue cc = null;
                try {
                    cc = infValue.getRelatedOne("CreditCard", false);
                } catch (GenericEntityException e) {
                    Debug.logError(e, module);
                }
                String nameOnCard = cc.getString("firstNameOnCard") + " " + cc.getString("lastNameOnCard");
                nameOnCard = nameOnCard.trim();
                payInfo.put("nameOnCard", nameOnCard);

                String cardNum = cc.getString("cardNumber");
                String cardStr = UtilFormatOut.formatPrintableCreditCard(cardNum);

                String expDate = cc.getString("expireDate");
                String infoString = cardStr + " " + expDate;
                payInfo.put("payInfo", infoString);
                payInfo.putAll(cc);
                payInfo.put("cardNumber", cardStr);  // masked cardNumber

            } else if ("GIFT_CARD".equals(paymentMethodTypeId)) {
                /*
                GenericValue gc = null;
                try {
                    gc = infValue.getRelatedOne("GiftCard", false); //FIXME is this really useful ? (Maybe later...)
                } catch (GenericEntityException e) {
                    Debug.logError(e, module);
                }
                */
            }
        }

        return payInfo;
    }

    public BigDecimal getItemQuantity(String cartIndex) {
        trace("request item quantity", cartIndex);
        int index = Integer.parseInt(cartIndex);
        ShoppingCartItem item = cart.findCartItem(index);
        if (item != null) {
            return item.getQuantity();
        } else {
            trace("item not found", cartIndex);
            return BigDecimal.ZERO;
        }
    }

    public boolean isAggregatedItem(String productId) {
        trace("is Aggregated Item", productId);
        try {
            Delegator delegator = cart.getDelegator();
            GenericValue product = null;
            product = EntityQuery.use(delegator).from("Product").where("productId", productId).cache().queryOne();
            if (UtilValidate.isNotEmpty(product) && ("AGGREGATED".equals(product.getString("productTypeId")) || "AGGREGATED_SERVICE".equals(product.getString("productTypeId")))) {
                return true;
            }
        } catch (GenericEntityException e) {
            trace("item lookup error", e);
            Debug.logError(e, module);
        } catch (Exception e) {
            trace("general exception", e);
            Debug.logError(e, module);
        }
        return false;
    }

    public ProductConfigWrapper getProductConfigWrapper(String productId) {
        //Get a PCW for a new product
        trace("get Product Config Wrapper", productId);
        ProductConfigWrapper pcw = null;
        try {
            Delegator delegator = cart.getDelegator();
            pcw = new ProductConfigWrapper(delegator, session.getDispatcher(),
                    productId, null, null, null, null, null, null);
        } catch (ItemNotFoundException e) {
            trace("item not found", e);
            //throw e;
        } catch (CartItemModifyException e) {
            trace("add item error", e);
            //throw e;
        } catch (GenericEntityException e) {
            trace("item lookup error", e);
            Debug.logError(e, module);
        } catch (Exception e) {
            trace("general exception", e);
            Debug.logError(e, module);
        }
        return pcw;
    }

    public ProductConfigWrapper getProductConfigWrapper(String productId, String cartIndex) {
        // Get a PCW for a pre-configured product
         trace("get Product Config Wrapper", productId + "/" + cartIndex);
         ProductConfigWrapper pcw = null;
         try {
            int index = Integer.parseInt(cartIndex);
            ShoppingCartItem product = cart.findCartItem(index);
            pcw = product.getConfigWrapper();
        } catch (Exception e) {
            trace("general exception", e);
            Debug.logError(e, module);
        }
        return pcw;
    }

    public void addItem(String productId, BigDecimal quantity) throws CartItemModifyException, ItemNotFoundException {
        trace("add item", productId + "/" + quantity);
        try {
            Delegator delegator = cart.getDelegator();
            GenericValue product = null;
            ProductConfigWrapper pcw = null;
            product = EntityQuery.use(delegator).from("Product").where("productId", productId).cache().queryOne();
            if (UtilValidate.isNotEmpty(product) && ("AGGREGATED".equals(product.getString("productTypeId"))||"AGGREGATED_SERVICE".equals(product.getString("productTypeId")))) {
                // if it's an aggregated item, load the configwrapper and set to defaults
                pcw = new ProductConfigWrapper(delegator, session.getDispatcher(), productId, null, null, null, null, null, null);
                pcw.setDefaultConfig();
            }
            //cart.addOrIncreaseItem(productId, null, quantity, null, null, null, null, null, null, null, null, null, null, null, null, session.getDispatcher());
            cart.addOrIncreaseItem(productId, null, quantity, null, null, null, null, null, null, null, null, pcw, null, null, null, session.getDispatcher());
        } catch (ItemNotFoundException e) {
            trace("item not found", e);
            throw e;
        } catch (CartItemModifyException e) {
            trace("add item error", e);
            throw e;
        } catch (GenericEntityException e) {
            trace("item lookup error", e);
            Debug.logError(e, module);
        } catch (Exception e) {
            trace("general exception", e);
            Debug.logError(e, module);
        }
    }

    public void addItem(String productId, BigDecimal quantity, ProductConfigWrapper pcw)
        throws ItemNotFoundException, CartItemModifyException {
        trace("add item with ProductConfigWrapper", productId + "/" + quantity);
        try {
            cart.addOrIncreaseItem(productId, null, quantity, null, null, null, null, null, null, null, null, pcw, null, null, null, session.getDispatcher());
        } catch (ItemNotFoundException e) {
            trace("item not found", e);
            throw e;
        } catch (CartItemModifyException e) {
            trace("add item error", e);
            throw e;
        } catch (Exception e) {
            trace("general exception", e);
            Debug.logError(e, module);
        }
    }

    public void modifyConfig(String productId, ProductConfigWrapper pcw, String cartIndex)
        throws CartItemModifyException, ItemNotFoundException {
        trace("modify item config", cartIndex);
        try {
            int cartIndexInt = Integer.parseInt(cartIndex);
            ShoppingCartItem cartItem = cart.findCartItem(cartIndexInt);
            BigDecimal quantity = cartItem.getQuantity();
            cart.removeCartItem(cartIndexInt, session.getDispatcher());
            cart.addOrIncreaseItem(productId, null, quantity, null, null, null, null, null, null, null, null, pcw, null, null, null, session.getDispatcher());
        } catch (CartItemModifyException e) {
            Debug.logError(e, module);
            trace("void or add item error", productId, e);
            throw e;
        } catch (ItemNotFoundException e) {
            trace("item not found", e);
            throw e;
        } catch (Exception e) {
            trace("general exception", e);
            Debug.logError(e, module);
        }
        return;
    }

    public void modifyQty(String cartIndex, BigDecimal quantity) throws CartItemModifyException {
        trace("modify item quantity", cartIndex + "/" + quantity);
        int index = Integer.parseInt(cartIndex);
        ShoppingCartItem item = cart.findCartItem(index);
        if (item != null) {
            try {
                item.setQuantity(quantity, session.getDispatcher(), cart, true);
            } catch (CartItemModifyException e) {
                Debug.logError(e, module);
                trace("modify item error", e);
                throw e;
            }
        } else {
            trace("item not found", cartIndex);
        }
    }

    public void modifyPrice(String cartIndex, BigDecimal price) {
        trace("modify item price", cartIndex + "/" + price);
        int index = Integer.parseInt(cartIndex);
        ShoppingCartItem item = cart.findCartItem(index);
        if (item != null) {
            item.setBasePrice(price);
        } else {
            trace("item not found", cartIndex);
        }
    }

    public void addDiscount(String cartIndex, BigDecimal discount, boolean percent) {
        GenericValue adjustment = session.getDelegator().makeValue("OrderAdjustment");
        adjustment.set("orderAdjustmentTypeId", "DISCOUNT_ADJUSTMENT");
        if (percent) {
            adjustment.set("sourcePercentage", discount.movePointRight(2));
        } else {
            adjustment.set("amount", discount);
        }

        if (cartIndex != null) {
            trace("add item adjustment");
            int iCartIndex = Integer.parseInt(cartIndex);
            ShoppingCartItem item = cart.findCartItem(iCartIndex);
            List<GenericValue> adjustments = item.getAdjustments();
            for (GenericValue gvAdjustment : adjustments){
                item.removeAdjustment(gvAdjustment);
            }
            item.addAdjustment(adjustment);
        } else {
            trace("add sale adjustment");
            if (cartDiscount > -1) {
                cart.removeAdjustment(cartDiscount);
            }
            cartDiscount = cart.addAdjustment(adjustment);
        }
    }

    public void clearDiscounts() {
        if (cartDiscount > -1) {
            cart.removeAdjustment(cartDiscount);
            cartDiscount = -1;
        }
        
        Iterator<ShoppingCartItem> cartIterator = cart.iterator();
        while(cartIterator.hasNext()){
            ShoppingCartItem item = (ShoppingCartItem) cartIterator.next();    
            List<GenericValue> adjustments = item.getAdjustments();
            for (GenericValue gvAdjustment : adjustments){
                item.removeAdjustment(gvAdjustment);
            }
        }
    }

    public BigDecimal GetTotalDiscount() {
        return cart.getOrderOtherAdjustmentTotal();
    }

    public void voidItem(String cartIndex) throws CartItemModifyException {
        trace("void item", cartIndex);
        int index;
            try {
            index = Integer.parseInt(cartIndex);
            cart.removeCartItem(index, session.getDispatcher());
            } catch (CartItemModifyException e) {
                Debug.logError(e, module);
            trace("void item error", cartIndex, e);
                throw e;
            }
        }

    public void voidSale(PosScreen pos) {
        trace("void sale");
        txLog.set("statusId", "POSTX_VOIDED");
        txLog.set("itemCount", new Long(cart.size()));
        txLog.set("logEndDateTime", UtilDateTime.nowTimestamp());
        try {
            txLog.store();
        } catch (GenericEntityException e) {
            Debug.logError(e, "Unable to store TX log - not fatal", module);
        }
        cart.clear();
        pos.getPromoStatusBar().clear();
        currentTx = null;
    }

    public void closeTx() {
        trace("transaction closed");
        txLog.set("statusId", "POSTX_CLOSED");
        txLog.set("itemCount", new Long(cart.size()));
        txLog.set("logEndDateTime", UtilDateTime.nowTimestamp());
        try {
            txLog.store();
        } catch (GenericEntityException e) {
            Debug.logError(e, "Unable to store TX log - not fatal", module);
        }
        cart.clear();
        currentTx = null;
    }

    public void paidInOut(String type) {
        trace("paid " + type);
        txLog.set("statusId", "POSTX_PAID_" + type);
        txLog.set("logEndDateTime", UtilDateTime.nowTimestamp());
        try {
            txLog.store();
        } catch (GenericEntityException e) {
            Debug.logError(e, "Unable to store TX log - not fatal", module);
        }
        currentTx = null;
    }

    public void calcTax() {
        try {
            ch.calcAndAddTax(this.getStoreOrgAddress());
        } catch (GeneralException e) {
            Debug.logError(e, module);
        }
    }

    public void clearTax() {
        cart.removeAdjustmentByType("SALES_TAX");
    }

    public int checkPaymentMethodType(String paymentMethodTypeId) {
        Map<String, String> fields = UtilMisc.toMap("paymentMethodTypeId", paymentMethodTypeId, "productStoreId", productStoreId);
        List<GenericValue> values = null;
        try {
            values = session.getDelegator().findByAnd("ProductStorePaymentSetting", fields, null, true);
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }

        final String externalCode = "PRDS_PAY_EXTERNAL";
        if (UtilValidate.isEmpty(values)) {
            return NO_PAYMENT;
        } else {
            boolean isExternal = true;
            Iterator<GenericValue> i = values.iterator();
            while (i.hasNext() && isExternal) {
                GenericValue v = i.next();
                //Debug.logInfo("Testing [" + paymentMethodTypeId + "] - " + v, module);
                if (!externalCode.equals(v.getString("paymentServiceTypeEnumId"))) {
                    isExternal = false;
                }
            }

            if (isExternal) {
                return EXTERNAL_PAYMENT;
            } else {
                return INTERNAL_PAYMENT;
            }
        }
    }

    public BigDecimal addPayment(String id, BigDecimal amount) {
        return this.addPayment(id, amount, null, null);
    }

    public BigDecimal addPayment(String id, BigDecimal amount, String refNum, String authCode) {
        trace("added payment", id + "/" + amount);
        if ("CASH".equals(id)) {
            // clear cash payments first; so there is only one
            cart.clearPayment(id);
        }
        cart.addPaymentAmount(id, amount, refNum, authCode, true, true, false);
        return this.getTotalDue();
    }

    public void setPaymentRefNum(int paymentIndex, String refNum, String authCode) {
        trace("setting payment index reference number", paymentIndex + " / " + refNum + " / " + authCode);
        ShoppingCart.CartPaymentInfo inf = cart.getPaymentInfo(paymentIndex);
        inf.refNum[0] = refNum;
        inf.refNum[1] = authCode;
    }

    /* CVV2 code should be entered when a card can't be swiped */
    public void setPaymentSecurityCode(String paymentId, String refNum, String securityCode) {
        trace("setting payment security code", paymentId);
        int paymentIndex = cart.getPaymentInfoIndex(paymentId, refNum);
        ShoppingCart.CartPaymentInfo inf = cart.getPaymentInfo(paymentIndex);
        inf.securityCode = securityCode;
        inf.isSwiped = false;
    }

    /* Track2 data should be sent to processor when a card is swiped. */
    public void setPaymentTrack2(String paymentId, String refNum, String securityCode) {
        trace("setting payment security code", paymentId);
        int paymentIndex = cart.getPaymentInfoIndex(paymentId, refNum);
        ShoppingCart.CartPaymentInfo inf = cart.getPaymentInfo(paymentIndex);
        inf.securityCode = securityCode;
        inf.isSwiped = true;
    }

    /* Postal code should be entered when a card can't be swiped */
    public void setPaymentPostalCode(String paymentId, String refNum, String postalCode) {
        trace("setting payment security code", paymentId);
        int paymentIndex = cart.getPaymentInfoIndex(paymentId, refNum);
        ShoppingCart.CartPaymentInfo inf = cart.getPaymentInfo(paymentIndex);
        inf.postalCode = postalCode;
    }

    public void clearPayments() {
        trace("all payments cleared from sale");
        cart.clearPayments();
    }

    public void clearPayment(int index) {
        trace("removing payment", "" + index);
        cart.clearPayment(index);
    }

    public void clearPayment(String id) {
        trace("removing payment", id);
        cart.clearPayment(id);
    }

    public int selectedPayments() {
        return cart.selectedPayments();
    }

    public void setTxAsReturn(String returnId) {
        trace("returned sale");
        txLog.set("statusId", "POSTX_RETURNED");
        txLog.set("returnId", returnId);
        txLog.set("logEndDateTime", UtilDateTime.nowTimestamp());
        try {
            txLog.store();
        } catch (GenericEntityException e) {
            Debug.logError(e, "Unable to store TX log - not fatal", module);
        }
        cart.clear();
        currentTx = null;
    }

    public BigDecimal processSale(Output output) throws GeneralException {
        trace("process sale");
        BigDecimal grandTotal = this.getGrandTotal();
        BigDecimal paymentAmt = this.getPaymentTotal();
        if (grandTotal.compareTo(paymentAmt) > 0) {
            throw new IllegalStateException();
        }

        // attach the party ID to the cart
        cart.setOrderPartyId(partyId);
        // Set the shipping type
        cart.setAllShipmentMethodTypeId("NO_SHIPPING");
       // cart.setAllCarrierPartyId();

        // validate payment methods
        output.print(UtilProperties.getMessage(resource, "PosValidating", locale));
        Map<String, Object> valRes = ch.validatePaymentMethods();
        if (valRes != null && ServiceUtil.isError(valRes)) {
            throw new GeneralException(ServiceUtil.getErrorMessage(valRes));
        }

        // store the "order"
         if (UtilValidate.isEmpty(this.orderId)) {  // if order does not exist
             output.print(UtilProperties.getMessage(resource, "PosSaving", locale));
             Map<String, Object> orderRes = ch.createOrder(session.getUserLogin());
             //Debug.logInfo("Create Order Resp : " + orderRes, module);

             if (orderRes != null && ServiceUtil.isError(orderRes)) {
                 throw new GeneralException(ServiceUtil.getErrorMessage(orderRes));
             } else if (orderRes != null) {
                 this.orderId = (String) orderRes.get("orderId");
             }
         } else { // if the order has already been created
             Map<?, ?> changeMap = UtilMisc.toMap("itemReasonMap",
                     UtilMisc.toMap("reasonEnumId", "EnumIdHere"), // TODO: where does this come from?
                     "itemCommentMap", UtilMisc.toMap("changeComments", "change Comments here")); //TODO

             Map<String, Object> svcCtx = new HashMap<String, Object>();
             svcCtx.put("userLogin", session.getUserLogin());
             svcCtx.put("orderId", orderId);
             svcCtx.put("shoppingCart", cart);
             svcCtx.put("locale", locale);
             svcCtx.put("changeMap", changeMap);

             Map<String, Object> svcRes = null;
             try {
                 LocalDispatcher dispatcher = session.getDispatcher();
                 svcRes = dispatcher.runSync("saveUpdatedCartToOrder", svcCtx);
             } catch (GenericServiceException e) {
                 Debug.logError(e, module);
                 //pos.showDialog("dialog/error/exception", e.getMessage());
                 throw new GeneralException(ServiceUtil.getErrorMessage(svcRes));
             }
          }

        // process the payment(s)
        output.print(UtilProperties.getMessage(resource, "PosProcessing", locale));
        Map<String, Object> payRes = null;
        try {
            payRes = ch.processPayment(ProductStoreWorker.getProductStore(productStoreId, session.getDelegator()), session.getUserLogin(), true);
        } catch (GeneralException e) {
            Debug.logError(e, module);
            throw e;
        }

        if (payRes != null && ServiceUtil.isError(payRes)) {
            throw new GeneralException(ServiceUtil.getErrorMessage(payRes));
        }

        // get the change due
        BigDecimal change = grandTotal.subtract(paymentAmt);

        // notify the change due
        output.print(UtilProperties.getMessage(resource, "PosChange",locale) + " " + UtilFormatOut.formatPrice(this.getTotalDue().negate()));

        // threaded drawer/receipt printing
        final PosTransaction currentTrans = this;
        final SwingWorker worker = new SwingWorker() {
            @Override
            public Object construct() {
                // open the drawer
                currentTrans.popDrawer();

                // print the receipt
                DeviceLoader.receipt.printReceipt(currentTrans, true);

                return null;
            }
        };
        worker.start();

        // save the TX Log
        txLog.set("statusId", "POSTX_SOLD");
        txLog.set("orderId", orderId);
        txLog.set("itemCount", new Long(cart.size()));
        txLog.set("logEndDateTime", UtilDateTime.nowTimestamp());
        try {
            txLog.store();
        } catch (GenericEntityException e) {
            Debug.logError(e, "Unable to store TX log - not fatal", module);
        }

        // clear the tx
        currentTx = null;
        partyId = "_NA_";

        return change;
    }

    private synchronized GenericValue getStoreOrgAddress() {
        if (this.shipAddress == null) {
            // locate the store's physical address - use this for tax
            GenericValue facility = (GenericValue) session.getAttribute("facility");
            if (facility == null) {
                return null;
            }

            Delegator delegator = session.getDelegator();
            GenericValue facilityContactMech = ContactMechWorker.getFacilityContactMechByPurpose(delegator, facilityId, UtilMisc.toList("SHIP_ORIG_LOCATION", "PRIMARY_LOCATION"));
            if (facilityContactMech != null) {
                try {
                    this.shipAddress = session.getDelegator().findOne("PostalAddress",
                            UtilMisc.toMap("contactMechId", facilityContactMech.getString("contactMechId")), false);
                } catch (GenericEntityException e) {
                    Debug.logError(e, module);
                }
            }
        }
        return this.shipAddress;
    }

    public void saveTx() {
        savedTx.push(this);
        currentTx = null;
        trace("transaction saved");
    }

    public void appendItemDataModel(XModel model) {
        if (cart != null) {
            Iterator<?> i = cart.iterator();
            while (i.hasNext()) {
                ShoppingCartItem item = (ShoppingCartItem) i.next();
                BigDecimal quantity = item.getQuantity();
                BigDecimal unitPrice = item.getBasePrice();
                BigDecimal subTotal = unitPrice.multiply(quantity);
                BigDecimal adjustment = item.getOtherAdjustments();

                XModel line = Journal.appendNode(new JournalLineParams(model, "tr", "" + cart.getItemIndex(item), ""));
                JournalLineParams sku = new JournalLineParams(line, "td", "sku", item.getProductId());
                JournalLineParams desc = new JournalLineParams(line, "td", "desc", item.getName());
                JournalLineParams qty = new JournalLineParams(line, "td", "qty", UtilFormatOut.formatQuantity(quantity));
                JournalLineParams price = new JournalLineParams(line, "td", "price", UtilFormatOut.formatPrice(subTotal));
                JournalLineParams index = new JournalLineParams(line, "td", "index", Integer.toString(cart.getItemIndex(item)));
                JournalLineParams[] journalLineParamses = new JournalLineParams[] {sku, desc, qty, price, index};
                appendJouralLine(journalLineParamses);

                if (this.isAggregatedItem(item.getProductId())) {
                    // put alterations here
                    ProductConfigWrapper pcw = null;
                    // product = EntityQuery.use(delegator).from("Product").where("productId", productId).cache().queryOne();
                    // pcw = new ProductConfigWrapper(delegator, session.getDispatcher(), productId, null, null, null, null, null, null);
                    pcw = item.getConfigWrapper();
                    List<ConfigOption> selected = pcw.getSelectedOptions();
                    for (ConfigOption configoption : selected) {
                        if (configoption.isSelected()) {
                            XModel option = Journal.appendNode(new JournalLineParams(model, "tr", "" + cart.getItemIndex(item), ""));
                            sku = new JournalLineParams(option, "td", "sku", "");
                            desc = new JournalLineParams(option, "td", "desc", configoption.getDescription());
                            qty = new JournalLineParams(option, "td", "qty", "");
                            price = new JournalLineParams(option, "td", "price", UtilFormatOut.formatPrice(configoption.getPrice()));
                            index = new JournalLineParams(option, "td", "index", Integer.toString(cart.getItemIndex(item)));
                            journalLineParamses = new JournalLineParams[] {sku, desc, qty, price, index};
                            appendJouralLine(journalLineParamses);
                        }
                    }
                }

                if (adjustment.compareTo(BigDecimal.ZERO) != 0) {
                    // append the promo info
                    XModel promo = Journal.appendNode(new JournalLineParams(model, "tr", "itemadjustment", ""));
                    sku = new JournalLineParams(promo, "td", "sku", "");
                    desc = new JournalLineParams(promo, "td", "desc", UtilProperties.getMessage(resource, "PosItemDiscount", locale));
                    qty = new JournalLineParams(promo, "td", "qty", "");
                    price = new JournalLineParams(promo, "td", "price", UtilFormatOut.formatPrice(adjustment));
                    journalLineParamses = new JournalLineParams[] {sku, desc, qty, price, index};
                    appendJouralLine(journalLineParamses);
                }
            }
        }
    }
    
    private void appendJouralLine(JournalLineParams[] journalLineParamses) {
        if (locale.getLanguage().equals("ar")) {
            int startParamIndex = 2;
            if (journalLineParamses.length == 4)
                startParamIndex = 1;
            for (int p = journalLineParamses.length - startParamIndex; p >= 0; p--) {
                Journal.appendNode(journalLineParamses[p]);
            }
            if (startParamIndex == 2)
                Journal.appendNode(journalLineParamses[journalLineParamses.length - 1]);
        }   else {
            for (int p = 0; p < journalLineParamses.length; p++) {
                Journal.appendNode(journalLineParamses[p]);
            }
        }
    }

    public void appendTotalDataModel(XModel model) {
        if (cart != null) {
            BigDecimal taxAmount = cart.getTotalSalesTax();
            BigDecimal total = cart.getGrandTotal();
            List<GenericValue> adjustments = cart.getAdjustments();
            BigDecimal itemsAdjustmentsAmount = BigDecimal.ZERO;

            Iterator<?> i = cart.iterator();
            while (i.hasNext()) {
                ShoppingCartItem item = (ShoppingCartItem) i.next();
                BigDecimal adjustment = item.getOtherAdjustments();
                if (adjustment.compareTo(BigDecimal.ZERO) != 0) {
                    itemsAdjustmentsAmount = itemsAdjustmentsAmount.add(adjustment);
                }
            }

            for (GenericValue orderAdjustment : adjustments) {
                BigDecimal amount = orderAdjustment.getBigDecimal("amount");
                BigDecimal sourcePercentage = orderAdjustment.getBigDecimal("sourcePercentage");
                XModel adjustmentLine = Journal.appendNode(new JournalLineParams(model, "tr", "adjustment", ""));
                JournalLineParams[] journalLineParamses = new JournalLineParams[5];
                journalLineParamses[0] = new JournalLineParams(adjustmentLine, "td", "sku", "");
                journalLineParamses[1] = new JournalLineParams(adjustmentLine, "td", "desc", 
                        UtilProperties.getMessage(resource, "PosSalesDiscount", locale));
                if (UtilValidate.isNotEmpty(amount)) {
                    journalLineParamses[2] = new JournalLineParams(adjustmentLine, "td", "qty", "");
                    journalLineParamses[3] = new JournalLineParams(adjustmentLine, "td", "price", UtilFormatOut.formatPrice(amount));
                } else if (UtilValidate.isNotEmpty(sourcePercentage)) {
                    BigDecimal percentage = sourcePercentage.movePointLeft(2).negate(); // sourcePercentage is negative and must be show as a positive value (it's a discount not an amount)
                    journalLineParamses[2] = new JournalLineParams(adjustmentLine, "td", "qty", UtilFormatOut.formatPercentage(percentage));
                    amount = cart.getItemTotal().add(itemsAdjustmentsAmount).multiply(percentage); // itemsAdjustmentsAmount is negative
                    journalLineParamses[3] = new JournalLineParams(adjustmentLine, "td", "price", UtilFormatOut.formatPrice(amount.negate())); // amount must be shown as a negative value
                }
                journalLineParamses[4] = new JournalLineParams(adjustmentLine, "td", "index", "-1");
                appendJouralLine(journalLineParamses);
            }

            XModel taxLine = Journal.appendNode(new JournalLineParams(model, "tr", "tax", ""));
            JournalLineParams[] journalTaxLineParamses = new JournalLineParams[5];
            journalTaxLineParamses[0] = new JournalLineParams(taxLine, "td", "sku", "");

            journalTaxLineParamses[1] = new JournalLineParams(taxLine, "td", "desc", UtilProperties.getMessage(resource, "PosSalesTax", locale));
            journalTaxLineParamses[2] = new JournalLineParams(taxLine, "td", "qty", "");
            journalTaxLineParamses[3] = new JournalLineParams(taxLine, "td", "price", UtilFormatOut.formatPrice(taxAmount));
            journalTaxLineParamses[4] = new JournalLineParams(taxLine, "td", "index", "-1");
            appendJouralLine(journalTaxLineParamses);

            XModel totalLine = Journal.appendNode(new JournalLineParams(model, "tr", "total", ""));
            JournalLineParams[] journalTotalLineParamses = new JournalLineParams[5];
            journalTotalLineParamses[0] = new JournalLineParams(totalLine, "td", "sku", "");
            journalTotalLineParamses[1] = new JournalLineParams(totalLine, "td", "desc", UtilProperties.getMessage(resource, "PosGrandTotal", locale));
            journalTotalLineParamses[2] = new JournalLineParams(totalLine, "td", "qty", "");
            journalTotalLineParamses[3] = new JournalLineParams(totalLine, "td", "price", UtilFormatOut.formatPrice(total));
            journalTotalLineParamses[4] = new JournalLineParams(totalLine, "td", "index", "-1");
            appendJouralLine(journalTotalLineParamses);
        }
    }

    public void appendPaymentDataModel(XModel model) {
        if (cart != null) {
            int paymentInfoSize = cart.selectedPayments();
            for (int i = 0; i < paymentInfoSize; i++) {
                ShoppingCart.CartPaymentInfo inf = cart.getPaymentInfo(i);
                GenericValue paymentInfoObj = inf.getValueObject(session.getDelegator());

                GenericValue paymentMethodType = null;
                GenericValue paymentMethod = null;
                if ("PaymentMethod".equals(paymentInfoObj.getEntityName())) {
                    paymentMethod = paymentInfoObj;
                    try {
                        paymentMethodType = paymentMethod.getRelatedOne("PaymentMethodType", false);
                    } catch (GenericEntityException e) {
                        Debug.logError(e, module);
                    }
                } else {
                    paymentMethodType = paymentInfoObj;
                }

                Object desc = paymentMethodType != null ? paymentMethodType.get("description",locale) : "??";
                String descString = desc.toString();
                BigDecimal amount = BigDecimal.ZERO;
                if (inf.amount == null) {
                    amount = cart.getGrandTotal().subtract(cart.getPaymentTotal());
                } else {
                    amount = inf.amount;
                }

                XModel paymentLine = Journal.appendNode(new JournalLineParams(model, "tr", Integer.toString(i), ""));
                JournalLineParams[] journalPaymentLineParamses = new JournalLineParams[5];
                journalPaymentLineParamses[0] = new JournalLineParams(paymentLine, "td", "sku", "");
                journalPaymentLineParamses[1] = new JournalLineParams(paymentLine, "td", "desc", descString);
                journalPaymentLineParamses[2] = new JournalLineParams(paymentLine, "td", "qty", "-");
                journalPaymentLineParamses[3] = new JournalLineParams(paymentLine, "td", "price", UtilFormatOut.formatPrice(amount.negate()));
                journalPaymentLineParamses[4] = new JournalLineParams(paymentLine, "td", "index", Integer.toString(i));
                appendJouralLine(journalPaymentLineParamses);
            }
        }
    }

    public void appendChangeDataModel(XModel model) {
        if (cart != null) {
            BigDecimal changeDue = this.getTotalDue().negate();
            if (changeDue.compareTo(BigDecimal.ZERO) >= 0) {
                XModel changeLine = Journal.appendNode(new JournalLineParams(model, "tr", "", ""));
                JournalLineParams[] journalPaymentLineParamses = new JournalLineParams[4];
                journalPaymentLineParamses[0] = new JournalLineParams(changeLine, "td", "sku", "");
                journalPaymentLineParamses[1] = new JournalLineParams(changeLine, "td", "desc", "Change");
                journalPaymentLineParamses[2] = new JournalLineParams(changeLine, "td", "qty", "-");
                journalPaymentLineParamses[3] = new JournalLineParams(changeLine, "td", "price", UtilFormatOut.formatPrice(changeDue));
                appendJouralLine(journalPaymentLineParamses);
            }
        }
    }

    public String makeCreditCardVo(String cardNumber, String expDate, String firstName, String lastName) {
        LocalDispatcher dispatcher = session.getDispatcher();
        String expMonth = expDate.substring(0, 2);
        String expYear = expDate.substring(2);
        // two digit year check -- may want to re-think this
        if (expYear.length() == 2) {
            expYear = "20" + expYear;
        }

        Map<String, Object> svcCtx = new HashMap<String, Object>();
        svcCtx.put("userLogin", session.getUserLogin());
        svcCtx.put("partyId", partyId);
        svcCtx.put("cardNumber", cardNumber);
        svcCtx.put("firstNameOnCard", firstName == null ? "" : firstName);
        svcCtx.put("lastNameOnCard", lastName == null ? "" : lastName);
        svcCtx.put("expMonth", expMonth);
        svcCtx.put("expYear", expYear);
        svcCtx.put("cardType", UtilValidate.getCardType(cardNumber));

        //Debug.logInfo("Create CC : " + svcCtx, module);
        Map<String, Object> svcRes = null;
        try {
            svcRes = dispatcher.runSync("createCreditCard", svcCtx);
        } catch (GenericServiceException e) {
            Debug.logError(e, module);
            return null;
        }
        if (ServiceUtil.isError(svcRes)) {
            Debug.logError(ServiceUtil.getErrorMessage(svcRes) + " - " + svcRes, module);
            return null;
        } else {
            return (String) svcRes.get("paymentMethodId");
        }
    }

    public GenericValue getTerminalState() {
        Delegator delegator = session.getDelegator();
        GenericValue state = null;
        try {
            state = EntityQuery.use(delegator).from("PosTerminalState").where("posTerminalId", this.getTerminalId()).filterByDate(UtilDateTime.nowTimestamp(), "openedDate", "closedDate").queryFirst();
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }
        return state;
    }

    public void setPrintWriter(PrintWriter writer) {
        this.trace = writer;
    }

    private void trace(String s) {
        trace(s, null, null);
    }

    private void trace(String s, Throwable t) {
        trace(s, null, t);
    }

    private void trace(String s1, String s2) {
        trace(s1, s2, null);
    }

    private void trace(String s1, String s2, Throwable t) {
        /*
        if (trace != null) {
            String msg = s1;
            if (UtilValidate.isNotEmpty(s2)) {
                msg = msg + "(" + s2 + ")";
            }
            if (t != null) {
                msg = msg + " : " + t.getMessage();
            }

            // print the trace line
            trace.println("[POS @ " + terminalId + " TX:" + transactionId + "] - " + msg);
            trace.flush();
        }
        */
    }

    public static synchronized PosTransaction getCurrentTx(XuiSession session) {
        if (currentTx == null) {
            if (session.getUserLogin() != null) {
                currentTx = new PosTransaction(session);
            }
        }
        return currentTx;
    }

    public void loadSale(PosScreen pos) {
        trace("Load a sale");
        List<GenericValue> shoppingLists = createShoppingLists();
        if (UtilValidate.isNotEmpty(shoppingLists)) {
            Map<String, String> salesMap = createSalesMap(shoppingLists);
            if (!salesMap.isEmpty()) {
                LoadSale loadSale = new LoadSale(salesMap, this, pos);
                loadSale.openDlg();
            }
            else {
                pos.showDialog("dialog/error/nosales");
            }
        } else {
            pos.showDialog("dialog/error/nosales");
        }
    }

    public void loadOrder(PosScreen pos) {
        List<GenericValue> orders = findOrders();
        if (UtilValidate.isNotEmpty(orders)) {
            LoadSale loadSale = new LoadSale(createOrderHash(orders), this, pos);
            loadSale.openDlg();
        } else {
            pos.showDialog("dialog/error/nosales");
        }
    }

    private List<GenericValue> findOrders() {
        LocalDispatcher dispatcher = session.getDispatcher();

        Map<String, Object> svcCtx = new HashMap<String, Object>();
        svcCtx.put("userLogin", session.getUserLogin());
        svcCtx.put("partyId", partyId);
        List<String> orderStatusIds = new LinkedList<String>();
        orderStatusIds.add("ORDER_CREATED");
        svcCtx.put("orderStatusId", orderStatusIds);
        svcCtx.put("viewIndex", 1);
        svcCtx.put("viewSize", 25);
        svcCtx.put("showAll", "Y");

        Map<String, Object> svcRes = null;
        try {
            svcRes = dispatcher.runSync("findOrders", svcCtx);
        } catch (GenericServiceException e) {
            Debug.logError(e, module);
        }

        if (svcRes == null) {
            Debug.logInfo(UtilProperties.getMessage("EcommerceUiLabels", "EcommerceNoShoppingListsCreate", locale), module);
        } else if (ServiceUtil.isError(svcRes)) {
            Debug.logError(ServiceUtil.getErrorMessage(svcRes) + " - " + svcRes, module);
        } else{
            Integer orderListSize = (Integer) svcRes.get("orderListSize");
            if (orderListSize > 0) {
               List<GenericValue> orderList = UtilGenerics.checkList(svcRes.get("orderList"), GenericValue.class);
               return orderList;
            }
        }
        return null;
    }

/*    public void configureItem(String cartIndex, PosScreen pos) {
         trace("configure item", cartIndex);
         try {
            int index = Integer.parseInt(cartIndex);
            ShoppingCartItem product = cart.findCartItem(index);
            Delegator delegator = cart.getDelegator();
            ProductConfigWrapper pcw = null;
            pcw = product.getConfigWrapper();
            if (pcw != null) {
                ConfigureItem configItem = new ConfigureItem(cartIndex, pcw, this, pos);
                configItem.openDlg();
            }
            else {
                pos.showDialog("dialog/error/itemnotconfigurable");
            }
        } catch (Exception e) {
            trace("general exception", e);
            Debug.logError(e, module);
        }
    }  */

    public List<GenericValue> createShoppingLists() {
        List<GenericValue> shoppingLists = null;
        Delegator delegator = this.session.getDelegator();
        try {
            shoppingLists = EntityQuery.use(delegator).from("ShoppingList").queryList();
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return null;
        }

        if (shoppingLists == null) {
            Debug.logInfo(UtilProperties.getMessage("EcommerceUiLabels", "EcommerceNoShoppingListsCreate", locale), module);
        }
        return shoppingLists;
    }

    public Map<String, String> createSalesMap(List<GenericValue> shoppingLists) {
        Map<String, String> salesMap = new HashMap<String, String>();
        for (GenericValue shoppingList : shoppingLists) {
            List<GenericValue> items = null;
            try {
                items = shoppingList.getRelated("ShoppingListItem", null, UtilMisc.toList("shoppingListItemSeqId"), false);
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
            }
            if (UtilValidate.isNotEmpty(items)) {
                String listName = shoppingList.getString("listName");
                String shoppingListId = shoppingList.getString("shoppingListId");
                salesMap.put(shoppingListId, listName);
            }
        }
        return salesMap;
    }

    public Map<String, String> createOrderHash(List<GenericValue> orders) {
        Map<String, String> hash = new HashMap<String, String>();
        for (GenericValue order : orders) {
            String orderName = order.getString("orderName");
            String orderId = order.getString("orderId");
            if (orderName != null) {
                hash.put(orderId, orderName);
            }
        }
        return hash;
    }

    public boolean addListToCart(String shoppingListId, PosScreen pos, boolean append) {
        trace("Add list to cart");
        Delegator delegator = session.getDelegator();
        LocalDispatcher dispatcher = session.getDispatcher();
        String includeChild = null; // Perhaps will be used later ...
        String prodCatalogId =  null;

        try {
            ShoppingListEvents.addListToCart(delegator, dispatcher, cart, prodCatalogId, shoppingListId, (includeChild != null), true, append);
        } catch (IllegalArgumentException e) {
            Debug.logError(e, module);
            pos.showDialog("dialog/error/exception", e.getMessage());
            return false;
        }
        return true;
    }

    public boolean restoreOrder(String orderId, PosScreen pos, boolean append) {
        trace("Restore an order");

        LocalDispatcher dispatcher = session.getDispatcher();

        Map<String, Object> svcCtx = new HashMap<String, Object>();
        svcCtx.put("userLogin", session.getUserLogin());
        svcCtx.put("orderId", orderId);
        svcCtx.put("skipInventoryChecks", Boolean.TRUE);
        svcCtx.put("skipProductChecks", Boolean.TRUE);

        Map<String, Object> svcRes = null;
        try {
            svcRes = dispatcher.runSync("loadCartFromOrder", svcCtx);
        } catch (GenericServiceException e) {
            Debug.logError(e, module);
            pos.showDialog("dialog/error/exception", e.getMessage());
        }

        if (svcRes == null) {
            Debug.logInfo(UtilProperties.getMessage("EcommerceUiLabels", "EcommerceNoShoppingListsCreate", locale), module);
        } else if (ServiceUtil.isError(svcRes)) {
            Debug.logError(ServiceUtil.getErrorMessage(svcRes) + " - " + svcRes, module);
        } else{
            ShoppingCart restoredCart = (ShoppingCart) svcRes.get("shoppingCart");
            if (append) {
                // TODO: add stuff to append items
                this.cart = restoredCart;
                this.orderId = orderId;
            } else {
                this.cart = restoredCart;
                this.orderId = orderId;
            }
            this.ch = new CheckOutHelper(session.getDispatcher(), session.getDelegator(), cart);
            if (session.getUserLogin() != null) {
                cart.addAdditionalPartyRole(session.getUserLogin().getString("partyId"), "SALES_REP");
            }
            cart.setFacilityId(facilityId);
            cart.setTerminalId(terminalId);
            cart.setOrderId(orderId);
            return true;
        }
        return false;
    }

    public boolean clearList(String shoppingListId, PosScreen pos) {
        Delegator delegator = session.getDelegator();
        try {
        ShoppingListEvents.clearListInfo(delegator, shoppingListId);
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            pos.showDialog("dialog/error/exception", e.getMessage());
            return false;
        }
        return true;
    }


    public void clientProfile(PosScreen pos) {
        ClientProfile clientProfile = new ClientProfile(this, pos);
        clientProfile.openDlg();
    }

    public void saveSale(PosScreen pos) {
        SaveSale saveSale = new SaveSale(this, pos);
        saveSale.openDlg();
    }

    public void saveOrder(String shoppingListName, PosScreen pos) {
        trace("Save an order");
        if (cart.size() == 0) {
            pos.showDialog("dialog/error/exception", UtilProperties.getMessage("OrderErrorUiLabels", "OrderUnableToCreateNewShoppingList", locale));
            return;
        }
        if (!UtilValidate.isEmpty(shoppingListName)) {
            // attach the party ID to the cart
            cart.setOrderPartyId(partyId);
            cart.setOrderName(shoppingListName);
            //cart.setExternalId(shoppingListName);
            //cart.setInternalCode("Internal Code");
            //ch.setCheckOutOptions(null, null, null, null, null, "shipping instructions", null, null, null, "InternalId", null, null, null);
            Map<String, Object> orderRes = ch.createOrder(session.getUserLogin());

            if (orderRes != null && ServiceUtil.isError(orderRes)) {
                Debug.logError(ServiceUtil.getErrorMessage(orderRes), module);
                //throw new GeneralException(ServiceUtil.getErrorMessage(orderRes));
            } else if (orderRes != null) {
                this.orderId = (String) orderRes.get("orderId");
            }
        }
    }

    public void saveSale(String  shoppingListName, PosScreen pos) {
        trace("Save a sale");
        if (cart.size() == 0) {
            pos.showDialog("dialog/error/exception", UtilProperties.getMessage("OrderErrorUiLabels", "OrderUnableToCreateNewShoppingList", locale));
            return;
        }
        Delegator delegator = this.session.getDelegator();
        LocalDispatcher dispatcher = session.getDispatcher();
        GenericValue userLogin = session.getUserLogin();
        String shoppingListId = null;

        if (!UtilValidate.isEmpty(shoppingListName)) {
            // create a new shopping list with partyId = user connected (POS clerk, etc.) and not buyer (_NA_ in POS)
            Map<String, Object> serviceCtx = UtilMisc.toMap("userLogin", session.getUserLogin(), "partyId", session.getUserPartyId(),
                    "productStoreId", productStoreId, "listName", shoppingListName);

            serviceCtx.put("shoppingListTypeId", "SLT_SPEC_PURP");
            Map<String, Object> newListResult = null;
            try {

                newListResult = dispatcher.runSync("createShoppingList", serviceCtx);
            } catch (GenericServiceException e) {
                Debug.logError(e, "Problem while creating new ShoppingList", module);
                pos.showDialog("dialog/error/exception", UtilProperties.getMessage("OrderErrorUiLabels", "OrderUnableToCreateNewShoppingList", locale));
                return;
            }

            // check for errors
            if (ServiceUtil.isError(newListResult)) {
                String error = ServiceUtil.getErrorMessage(newListResult);
                Debug.logError(error, module);
                pos.showDialog("dialog/error/exception", error);
                return;
            }

            // get the new list id
            if (newListResult != null) {
                shoppingListId = (String) newListResult.get("shoppingListId");
            } else {
                Debug.logError("Problem while creating new ShoppingList", module);
                pos.showDialog("dialog/error/exception", UtilProperties.getMessage("OrderErrorUiLabels", "OrderUnableToCreateNewShoppingList", locale));
                return;
            }
        }

        String selectedCartItems[] = new String[cart.size()];
        for (int i = 0; i < cart.size(); i++) {
            Integer integer = i;
            selectedCartItems[i] = integer.toString();
        }

        try {
            ShoppingListEvents.addBulkFromCart(delegator, dispatcher, cart, userLogin, shoppingListId, null, selectedCartItems, true, true);
        } catch (IllegalArgumentException e) {
            Debug.logError(e, "Problem while creating new ShoppingList", module);
            pos.showDialog("dialog/error/exception", UtilProperties.getMessage("OrderErrorUiLabels", "OrderUnableToCreateNewShoppingList", locale));
        }
    }

    public String addProductPromoCode(String code) {
        trace("Add a promo code");
        LocalDispatcher dispatcher = session.getDispatcher();
        String result = cart.addProductPromoCode(code, dispatcher);
        calcTax();
        return result;
    }

    private List<Map<String, String>> searchContactMechs(Delegator delegator, PosScreen pos, List<Map<String, String>> partyList, String valueToCompare, String contactMechType) {
        ListIterator<Map<String, String>>  partyListIt = partyList.listIterator();
        while(partyListIt.hasNext()) {
            Map<String, String> party = partyListIt.next();
            String partyId = party.get("partyId");
            List<Map<String, Object>> partyContactMechValueMaps = ContactMechWorker.getPartyContactMechValueMaps(delegator, partyId, false, contactMechType);
            Integer nb = 0;
            for (Map<String, Object> partyContactMechValueMap : partyContactMechValueMaps) {
                nb++;
                String keyType = null;
                String key = null;
                if ("TELECOM_NUMBER".equals(contactMechType)) {
                    keyType = "telecomNumber";
                    key = "contactNumber";
                } else if ("EMAIL_ADDRESS".equals(contactMechType)) {
                    keyType = "contactMech";
                    key = "infoString";
                }
                Map<String, Object> keyTypeMap = UtilGenerics.checkMap(partyContactMechValueMap.get(keyType));
                String keyTypeValue = ((String) keyTypeMap.get(key)).trim();
                if (valueToCompare.equals(keyTypeValue) || UtilValidate.isEmpty(valueToCompare)) {
                    if (nb == 1) {
                        party.put(key, keyTypeValue);
                        partyListIt.set(party);
                    } else {
                        Map<String, String> partyClone = new HashMap<String, String>();
                        partyClone.putAll(party);
                        partyClone.put(key, keyTypeValue);
                        partyListIt.add(partyClone);
                    }
                }
            }
        }
        return partyList;
    }


    public List<Map<String, String>> searchClientProfile(String name, String email, String  phone, String card, PosScreen pos, Boolean equalsName) {
        Delegator delegator = this.session.getDelegator();

        List<GenericValue> partyList = null;
        List<Map<String, String>> resultList = null;

        // create the dynamic view entity
        DynamicViewEntity dynamicView = new DynamicViewEntity();

        // Person (name + card)
        dynamicView.addMemberEntity("PT", "Party");
        dynamicView.addAlias("PT", "partyId");
        dynamicView.addAlias("PT", "statusId");
        dynamicView.addAlias("PT", "partyTypeId");
        dynamicView.addMemberEntity("PE", "Person");
        dynamicView.addAlias("PE", "partyId");
        dynamicView.addAlias("PE", "lastName");
        dynamicView.addAlias("PE", "cardId");
        dynamicView.addViewLink("PT", "PE", Boolean.FALSE, ModelKeyMap.makeKeyMapList("partyId"));

        if (UtilValidate.isNotEmpty(email)) {
            // ContactMech (email)
            dynamicView.addMemberEntity("PM", "PartyContactMechPurpose");
            dynamicView.addAlias("PM", "contactMechId");
            dynamicView.addAlias("PM", "contactMechPurposeTypeId");
            dynamicView.addAlias("PM", "thruDate");
            dynamicView.addMemberEntity("CM", "ContactMech");
            dynamicView.addAlias("CM", "infoString");
            dynamicView.addViewLink("PT", "PM", Boolean.FALSE, ModelKeyMap.makeKeyMapList("partyId"));
            dynamicView.addViewLink("PM", "CM", Boolean.FALSE, ModelKeyMap.makeKeyMapList("contactMechId"));
        } else if (UtilValidate.isNotEmpty(phone)) {
            dynamicView.addMemberEntity("PM", "PartyContactMechPurpose");
            dynamicView.addAlias("PM", "contactMechId");
            dynamicView.addAlias("PM", "thruDate");
            dynamicView.addAlias("PM", "contactMechPurposeTypeId");
            dynamicView.addMemberEntity("TN", "TelecomNumber");
            dynamicView.addAlias("TN", "contactNumber");
            dynamicView.addViewLink("PT", "PM", Boolean.FALSE, ModelKeyMap.makeKeyMapList("partyId"));
            dynamicView.addViewLink("PM", "TN", Boolean.FALSE, ModelKeyMap.makeKeyMapList("contactMechId"));
        }

            // define the main condition & expression list
            List<EntityCondition> andExprs = new LinkedList<EntityCondition>();
            EntityCondition mainCond = null;

            List<String> orderBy = new LinkedList<String>();
            List<String> fieldsToSelect = new LinkedList<String>();
            // fields we need to select; will be used to set distinct
            fieldsToSelect.add("partyId");
            fieldsToSelect.add("lastName");
            fieldsToSelect.add("cardId");
            if (UtilValidate.isNotEmpty(email)) {
                fieldsToSelect.add("infoString");
            } else if (UtilValidate.isNotEmpty(phone)) {
                fieldsToSelect.add("contactNumber");
            }

            // NOTE: _must_ explicitly allow null as it is not included in a not equal in many databases... odd but true
            // This allows to get all clients when any informations has been entered
            andExprs.add(EntityCondition.makeCondition(EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, null), EntityOperator.OR, EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "PARTY_DISABLED")));
            andExprs.add(EntityCondition.makeCondition("partyTypeId", EntityOperator.EQUALS, "PERSON")); // Only persons for now...
            if (UtilValidate.isNotEmpty(name)) {
                if (equalsName) {
                    andExprs.add(EntityCondition.makeCondition("lastName", EntityOperator.EQUALS, name));  // Plain name
                } else {
                    // andExprs.add(EntityCondition.makeCondition("lastName", EntityOperator.LIKE, "%"+name+"%")); // Less restrictive
                     andExprs.add(EntityCondition.makeCondition(EntityFunction.UPPER_FIELD("lastName"), EntityOperator.LIKE, EntityFunction.UPPER("%"+name+"%"))); // Even less restrictive
                }
            }
            if (UtilValidate.isNotEmpty(card)) {
                andExprs.add(EntityCondition.makeCondition("cardId", EntityOperator.EQUALS, card));
            }
            if (UtilValidate.isNotEmpty(email)) {
                andExprs.add(EntityCondition.makeCondition("infoString", EntityOperator.EQUALS, email));
                andExprs.add(EntityCondition.makeCondition("contactMechPurposeTypeId", EntityOperator.EQUALS, "PRIMARY_EMAIL"));
                andExprs.add(EntityCondition.makeCondition("thruDate", EntityOperator.EQUALS, null));
            } else if (UtilValidate.isNotEmpty(phone)) {
                andExprs.add(EntityCondition.makeCondition("contactNumber", EntityOperator.EQUALS, phone));
                andExprs.add(EntityCondition.makeCondition("contactMechPurposeTypeId", EntityOperator.EQUALS, "PHONE_HOME"));
                andExprs.add(EntityCondition.makeCondition("thruDate", EntityOperator.EQUALS, null));
            }

            mainCond = EntityCondition.makeCondition(andExprs, EntityOperator.AND);
            orderBy.add("lastName");

            Debug.logInfo("In searchClientProfile mainCond=" + mainCond, module);

            Integer maxRows = Integer.MAX_VALUE;
            // attempt to start a transaction
            boolean beganTransaction = false;
            try {
                beganTransaction = TransactionUtil.begin();

                try {
                    // set distinct on so we only get one row per person
                    // using list iterator
                     EntityListIterator pli = EntityQuery.use(delegator).select(UtilMisc.toSet(fieldsToSelect))
                            .from(dynamicView).where(mainCond)
                            .cursorScrollInsensitive()
                            .fetchSize(-1)
                            .maxRows(maxRows)
                            .cache(true)
                            .queryIterator();

                    // get the partial list for this page
                    partyList = pli.getPartialList(1, maxRows);

                    // close the list iterator
                    pli.close();
                    } catch (GenericEntityException e) {
                        Debug.logError(e, module);
                        pos.showDialog("dialog/error/exception", e.getMessage());
                    }
            } catch (GenericTransactionException e) {
                Debug.logError(e, module);
                try {
                    TransactionUtil.rollback(beganTransaction, e.getMessage(), e);
                } catch (GenericTransactionException e2) {
                    Debug.logError(e2, "Unable to rollback transaction", module);
                    pos.showDialog("dialog/error/exception", e2.getMessage());
                }
                pos.showDialog("dialog/error/exception", e.getMessage());
            } finally {
                try {
                    TransactionUtil.commit(beganTransaction);
                } catch (GenericTransactionException e) {
                    Debug.logError(e, "Unable to commit transaction", module);
                    pos.showDialog("dialog/error/exception", e.getMessage());
                }
            }

            if (partyList != null) {
                resultList = new LinkedList<Map<String,String>>();
                for (GenericValue party : partyList) {
                    Map<String, String> partyMap = new HashMap<String, String>();
                    partyMap.put("partyId", party.getString("partyId"));
                    partyMap.put("lastName", party.getString("lastName"));
                    partyMap.put("cardId", party.getString("cardId"));
                    if (UtilValidate.isNotEmpty(email)) {
                        partyMap.put("infoString", party.getString("infoString"));
                        partyMap.put("contactNumber", "");
                    } else if (UtilValidate.isNotEmpty(phone)) {
                        partyMap.put("infoString", "");
                        partyMap.put("contactNumber", party.getString("contactNumber"));
                    } else { // both empty
                        partyMap.put("infoString", "");
                        partyMap.put("contactNumber", "");

                    }
                    resultList.add(partyMap);
                }

                if (UtilValidate.isNotEmpty(email)) {
                    resultList = searchContactMechs(delegator, pos, resultList, phone, "TELECOM_NUMBER");
                } else if (UtilValidate.isNotEmpty(phone)) {
                    resultList = searchContactMechs(delegator, pos, resultList, "", "EMAIL_ADDRESS"); // "" is more clear than email which is by definition here is empty
                } else { // both empty
                    resultList = searchContactMechs(delegator, pos, resultList, "", "TELECOM_NUMBER"); // "" is more clear than phone which is by definition here is empty
                    resultList = searchContactMechs(delegator, pos, resultList, "", "EMAIL_ADDRESS");
                }
            } else {
            resultList = new LinkedList<Map<String,String>>();
        }
        return resultList;
    }

    public String editClientProfile(String name, String email, String  phone, String card, PosScreen pos, String editType, String partyId) {
        // We suppose here that a cardId (card number) can only belongs to one person (it's used as owned PromoCode)
        // We use the 1st party's login (it may change and be multiple since it depends on email and card)
        // We suppose only one email address (should be ok anyway because of the contactMechPurposeTypeId == "PRIMARY_EMAIL")
        // we suppose only one phone number (should be ok anyway because of the contactMechPurposeTypeId == "PHONE_HOME")
        LocalDispatcher dispatcher = session.getDispatcher();
        Delegator delegator = dispatcher.getDelegator();
        GenericValue userLogin = session.getUserLogin();
        GenericValue partyUserLogin = null;
        String result = null;

        Map<String, Object> svcCtx = new HashMap<String, Object>();
        Map<String, Object> svcRes = null;

        // Create
        if ("create".equals(editType)) {
            trace("Create a client profile");
            // createPersonAndUserLogin
            trace("createPersonAndUserLogin");
            if (UtilValidate.isNotEmpty(card)) {
                svcCtx.put("cardId", card);
            }
            svcCtx.put("userLogin", userLogin);
            svcCtx.put("lastName", name);
            svcCtx.put("firstName", ""); // Needed by service createPersonAndUserLogin
            if (UtilValidate.isNotEmpty(email) && UtilValidate.isNotEmpty(phone)) {
                svcCtx.put("userLoginId", email);
                svcCtx.put("currentPassword", phone);
                svcCtx.put("currentPasswordVerify", phone);
                try {
                    svcRes = dispatcher.runSync("createPersonAndUserLogin", svcCtx);
                } catch (GenericServiceException e) {
                    Debug.logError(e, module);
                    pos.showDialog("dialog/error/exception", e.getMessage());
                   return null;
                }
                if (ServiceUtil.isError(svcRes)) {
                    pos.showDialog("dialog/error/exceptionLargeSmallFont", ServiceUtil.getErrorMessage(svcRes)); // exceptionLargeSmallFont used to show duplicate key error message for card
                    return null;
                }
                partyId = (String) svcRes.get("partyId");
                partyUserLogin = (GenericValue) svcRes.get("newUserLogin");
            } else {
                // createPerson
                trace("createPerson");
                try {
                    svcRes = dispatcher.runSync("createPerson", svcCtx);
                } catch (GenericServiceException e) {
                    Debug.logError(e, module);
                    pos.showDialog("dialog/error/exception", e.getMessage());
                   return result;
                }
                if (ServiceUtil.isError(svcRes)) {
                    pos.showDialog("dialog/error/exception", ServiceUtil.getErrorMessage(svcRes));
                    return result;
                }
                partyId = (String) svcRes.get("partyId");
                partyUserLogin = userLogin;
            }

            if (UtilValidate.isNotEmpty(email)) {
                // createPartyEmailAddress
                trace("createPartyEmailAddress");
                svcCtx.clear();
                svcCtx.put("userLogin", partyUserLogin);
                svcCtx.put("emailAddress", email);
                svcCtx.put("partyId", partyId);
                svcCtx.put("contactMechPurposeTypeId", "PRIMARY_EMAIL");
                try {
                    svcRes = dispatcher.runSync("createPartyEmailAddress", svcCtx);
                } catch (GenericServiceException e) {
                    Debug.logError(e, module);
                    pos.showDialog("dialog/error/exception", e.getMessage());
                    return null;
                }
                if (ServiceUtil.isError(svcRes)) {
                    pos.showDialog("dialog/error/exception", ServiceUtil.getErrorMessage(svcRes));
                    return null;
                }
            }

            if (UtilValidate.isNotEmpty(phone)) {
                if (phone.length() < 5 ) {
                    pos.showDialog("dialog/error/exception", UtilProperties.getMessage(PosTransaction.resource, "PosPhoneField5Required", locale));
                } else {
                    // createPartyTelecomNumber
                    trace("createPartyTelecomNumber");
                    svcCtx.clear();
                    svcCtx.put("userLogin", partyUserLogin);
                    svcCtx.put("contactNumber", phone);
                    svcCtx.put("partyId", partyId);
                    svcCtx.put("contactMechPurposeTypeId", "PHONE_HOME");
                    try {
                        svcRes = dispatcher.runSync("createPartyTelecomNumber", svcCtx);
                    } catch (GenericServiceException e) {
                        Debug.logError(e, module);
                        pos.showDialog("dialog/error/exception", e.getMessage());
                        return null;
                    }
                    if (ServiceUtil.isError(svcRes)) {
                        pos.showDialog("dialog/error/exception", ServiceUtil.getErrorMessage(svcRes));
                        return null;
                    }
                }
            }

            result = partyId;

        // Update
        } else if (UtilValidate.isNotEmpty(partyId)){
            trace("Update a client profile");
            GenericValue  person = null;

            try {
                person = session.getDelegator().findOne("Person", UtilMisc.toMap("partyId", partyId), false);
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
                pos.showDialog("dialog/error/exception", e.getMessage());
                return null;
            }

            Boolean newLogin = true;
            try {
                List<GenericValue>  userLogins = session.getDelegator().findByAnd("UserLogin", UtilMisc.toMap("partyId", partyId), null, false);
                if (UtilValidate.isNotEmpty(userLogins)) {
                    userLogin = userLogins.get(0);
                    newLogin = false;
                }
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
                pos.showDialog("dialog/error/exception", e.getMessage());
                return null;
            }

            if (!person.getString("lastName").equals(name)
                    || UtilValidate.isNotEmpty(card) && !card.equals(person.getString("cardId"))) {
                // Update name and possibly card (cardId)
                svcCtx.put("userLogin", userLogin);
                svcCtx.put("partyId", partyId);
                svcCtx.put("firstName", ""); // Needed by service updatePerson
                svcCtx.put("lastName", name);
                if (UtilValidate.isNotEmpty(card)) {
                    svcCtx.put("cardId", card);
                }
                try {
                    // updatePerson
                    trace("updatePerson");
                    svcRes = dispatcher.runSync("updatePerson", svcCtx);
                } catch (GenericServiceException e) {
                    Debug.logError(e, module);
                    pos.showDialog("dialog/error/exception", e.getMessage());
                    return null;
                }
                if (ServiceUtil.isError(svcRes)) {
                    pos.showDialog("dialog/error/exceptionLargeSmallFont", ServiceUtil.getErrorMessage(svcRes));
                    return null;
                }
            }


            if (UtilValidate.isNotEmpty(phone)) {
                // Create or update phone
                if (phone.length() < 5 ) {
                    pos.showDialog("dialog/error/exception", UtilProperties.getMessage(PosTransaction.resource, "PosPhoneField5Required", locale));
                } else {
                    String contactNumber = null;
                    String contactMechId = null;
                    svcCtx.clear();
                    svcCtx.put("partyId", partyId);
                    svcCtx.put("thruDate", null); // last one
                    try {
                        List<GenericValue>  PartyTelecomNumbers = session.getDelegator().findByAnd("PartyAndTelecomNumber", svcCtx, null, false);
                        if (UtilValidate.isNotEmpty(PartyTelecomNumbers)) {
                            GenericValue PartyTelecomNumber = PartyTelecomNumbers.get(0); // There is  only one phone number (contactMechPurposeTypeId == "PHONE_HOME")
                            contactNumber = PartyTelecomNumber.getString("contactNumber");
                            contactMechId = PartyTelecomNumber.getString("contactMechId");
                        }
                    } catch (GenericEntityException e) {
                        Debug.logError(e, module);
                        pos.showDialog("dialog/error/exception", e.getMessage());
                        return null;
                    }

                    // Create or update phone
                    trace("createUpdatePartyTelecomNumber");
                    svcCtx.remove("thruDate");
                    svcCtx.put("userLogin", userLogin);
                    svcCtx.put("contactNumber", phone);
                    svcCtx.put("contactMechPurposeTypeId", "PHONE_HOME");
                    if (UtilValidate.isNotEmpty(contactMechId)) {
                        svcCtx.put("contactMechId", contactMechId);
                    }
                    try {
                        svcRes = dispatcher.runSync("createUpdatePartyTelecomNumber", svcCtx);
                    } catch (GenericServiceException e) {
                        Debug.logError(e, module);
                        pos.showDialog("dialog/error/exception", e.getMessage());
                        return null;
                    }
                    if (ServiceUtil.isError(svcRes)) {
                        pos.showDialog("dialog/error/exception", ServiceUtil.getErrorMessage(svcRes));
                        return null;
                    }

                    // Handle login aspect where phone is taken as pwd
                    if (UtilValidate.isNotEmpty(contactNumber) && !phone.equals(contactNumber)) {
                        if (!newLogin) { // to create a new login we need also an email address
                            // Update password, we need to temporary set password.accept.encrypted.and.plain to "true"
                            // This is done only for the properties loaded for the session in memory (we don't persist the value)
                            trace("updatePassword");
                            String passwordAcceptEncryptedAndPlain = null;
                            try {
                                
                                passwordAcceptEncryptedAndPlain = EntityUtilProperties.getPropertyValue("security", "password.accept.encrypted.and.plain", delegator);
                                UtilProperties.setPropertyValueInMemory("security", "password.accept.encrypted.and.plain", "true");
                                svcRes = dispatcher.runSync("updatePassword",
                                        UtilMisc.toMap("userLogin", userLogin,
                                        "userLoginId", userLogin.getString("userLoginId"),
                                        "currentPassword", userLogin.getString("currentPassword"),
                                        "newPassword", phone,
                                        "newPasswordVerify", phone));
                            } catch (GenericServiceException e) {
                                Debug.logError(e, "Error calling updatePassword service", module);
                                pos.showDialog("dialog/error/exception", e.getMessage());
                                UtilProperties.setPropertyValueInMemory("security", "password.accept.encrypted.and.plain", passwordAcceptEncryptedAndPlain);
                                return null;
                            } finally {
                                // Put back passwordAcceptEncryptedAndPlain value in memory
                                UtilProperties.setPropertyValueInMemory("security", "password.accept.encrypted.and.plain", passwordAcceptEncryptedAndPlain);
                            }
                            if (ServiceUtil.isError(svcRes)) {
                                pos.showDialog("dialog/error/exception", ServiceUtil.getErrorMessage(svcRes));
                                return null;
                            }
                        }
                    }
                }
            }

            if (UtilValidate.isNotEmpty(email)) {
            // Update email
                svcCtx.clear();
                svcCtx.put("partyId", partyId);
                svcCtx.put("thruDate", null); // last one
                svcCtx.put("contactMechTypeId", "EMAIL_ADDRESS");
                String contactMechId = null;
                String infoString = null;
                try {
                    List<GenericValue>  PartyEmails = session.getDelegator().findByAnd("PartyAndContactMech", svcCtx, null, false);
                    if (UtilValidate.isNotEmpty(PartyEmails)) {
                        GenericValue PartyEmail = PartyEmails.get(0); // There is  only one email address (contactMechPurposeTypeId == "PRIMARY_EMAIL")
                        contactMechId = PartyEmail.getString("contactMechId");
                        infoString = PartyEmail.getString("infoString");
                    }
                } catch (GenericEntityException e) {
                    Debug.logError(e, module);
                    pos.showDialog("dialog/error/exception", e.getMessage());
                    return null;
                }

                svcCtx.remove("thruDate");
                svcCtx.remove("contactMechTypeId");
                svcCtx.put("userLogin", userLogin);
                svcCtx.put("emailAddress", email);
                svcCtx.put("contactMechPurposeTypeId", "PRIMARY_EMAIL");
                if (UtilValidate.isNotEmpty(contactMechId)) {
                    svcCtx.put("contactMechId", contactMechId);
                }
                if (UtilValidate.isNotEmpty(infoString) && !email.equals(infoString)
                        || UtilValidate.isEmpty(infoString)) {
                    // Create or update email
                    trace("createUpdatePartyEmailAddress");
                    try {
                        svcRes = dispatcher.runSync("createUpdatePartyEmailAddress", svcCtx);
                    } catch (GenericServiceException e) {
                        Debug.logError(e, module);
                        pos.showDialog("dialog/error/exception", e.getMessage());
                        return null;
                    }
                    if (ServiceUtil.isError(svcRes)) {
                        pos.showDialog("dialog/error/exception", ServiceUtil.getErrorMessage(svcRes));
                        return null;
                    }
                }


                if (!newLogin && UtilValidate.isNotEmpty(infoString) && !email.equals(infoString)) {
                    // create a new UserLogin (Update a UserLoginId by creating a new one and expiring the old one). Keep the same password possibly changed just above if phone has also changed.
                    trace("updateUserLoginId");
                    try {
                        svcRes = dispatcher.runSync("updateUserLoginId", UtilMisc.toMap("userLoginId", email, "userLogin", userLogin));
                    } catch (GenericServiceException e) {
                        Debug.logError(e, module);
                        pos.showDialog("dialog/error/exception", e.getMessage());
                       return null;
                    }
                    if (ServiceUtil.isError(svcRes)) {
                        pos.showDialog("dialog/error/exception", ServiceUtil.getErrorMessage(svcRes));
                        return null;
                    }
                } else if (newLogin && UtilValidate.isNotEmpty(phone)) {
                    // createUserLogin
                    trace("createUserLogin");
                    try {
                        svcRes = dispatcher.runSync("createUserLogin",
                                UtilMisc.toMap("userLogin", userLogin,
                                        "userLoginId", email,
                                        "currentPassword", phone,
                                        "currentPasswordVerify", phone,
                                        "partyId", partyId));
                    } catch (GenericServiceException e) {
                        Debug.logError(e, "Error calling updatePassword service", module);
                        pos.showDialog("dialog/error/exception", e.getMessage());
                        return null;
                    }
                    if (ServiceUtil.isError(svcRes)) {
                        pos.showDialog("dialog/error/exception", ServiceUtil.getErrorMessage(svcRes));
                        return null;
                    }
                }
            }
        } else {
            pos.showDialog("dialog/error/exception", UtilProperties.getMessage(resource, "PosNoClientProfile", locale));
            return null;
        }
        return null;
    }
}
