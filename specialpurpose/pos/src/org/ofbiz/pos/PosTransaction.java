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
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import net.xoetrope.xui.data.XModel;
import net.xoetrope.xui.helper.SwingWorker;

import org.ofbiz.accounting.payment.PaymentGatewayServices;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.Log4jLoggerWriter;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilFormatOut;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.collections.LifoSet;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.guiapp.xui.XuiSession;
import org.ofbiz.order.shoppingcart.CartItemModifyException;
import org.ofbiz.order.shoppingcart.CheckOutHelper;
import org.ofbiz.order.shoppingcart.ItemNotFoundException;
import org.ofbiz.order.shoppingcart.ShoppingCart;
import org.ofbiz.order.shoppingcart.ShoppingCartItem;
import org.ofbiz.order.shoppinglist.ShoppingListEvents;
import org.ofbiz.pos.component.Journal;
import org.ofbiz.pos.component.Output;
import org.ofbiz.pos.device.DeviceLoader;
import org.ofbiz.pos.screen.LoadSale;
import org.ofbiz.pos.screen.PosScreen;
import org.ofbiz.pos.screen.SaveSale;
import org.ofbiz.product.store.ProductStoreWorker;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceUtil;

public class PosTransaction implements Serializable {

    public static final String module = PosTransaction.class.getName();
    public static final int NO_PAYMENT = 0;
    public static final int INTERNAL_PAYMENT = 1;
    public static final int EXTERNAL_PAYMENT = 2;

    private static PrintWriter defaultPrintWriter = new Log4jLoggerWriter(Debug.getLogger(module));
    private static PosTransaction currentTx = null;
    private static LifoSet savedTx = new LifoSet();
    private Locale defaultLocale = Locale.getDefault();

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
    private Map skuDiscounts = new HashMap();
    private int cartDiscount = -1;


    public PosTransaction(XuiSession session) {
        this.session = session;
        this.terminalId = session.getId();
        this.partyId = "_NA_";
        this.trace = defaultPrintWriter;

        this.productStoreId = (String) session.getAttribute("productStoreId");
        this.facilityId = (String) session.getAttribute("facilityId");
        this.currency = (String) session.getAttribute("currency");
        this.locale = (Locale) session.getAttribute("locale");

        this.cart = new ShoppingCart(session.getDelegator(), productStoreId, locale, currency);
        this.transactionId = session.getDelegator().getNextSeqId("PosTransaction");
        this.ch = new CheckOutHelper(session.getDispatcher(), session.getDelegator(), cart);
        cart.setChannelType("POS_SALES_CHANNEL");
        cart.setTransactionId(transactionId);
        cart.setFacilityId(facilityId);
        cart.setTerminalId(terminalId);
        if (session.getUserLogin() != null) {
            cart.addAdditionalPartyRole(session.getUserLogin().getString("partyId"), "SALES_REP");
        }

        // setup the TX log
        String txLogId = session.getDelegator().getNextSeqId("PosTerminalLog");
        txLog = session.getDelegator().makeValue("PosTerminalLog", null);
        txLog.set("posTerminalLogId", txLogId);
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

    public String getUserId() {
        return session.getUserId();
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
        return (cart == null || cart.size() == 0);
    }

    public List lookupItem(String sku) throws GeneralException {
        // first check for the product
        GenericValue product = session.getDelegator().findByPrimaryKey("Product", UtilMisc.toMap("productId", sku));
        if (product != null) {
            return UtilMisc.toList(product);
        } else {
            // not found; so we move on to GoodIdentification
           return session.getDelegator().findByAnd("GoodIdentificationAndProduct",
                   UtilMisc.toMap("idValue", sku), UtilMisc.toList("productId"));
        }
    }

    public String getOrderId() {
        return this.orderId;
    }

    public double getTaxTotal() {
        return cart.getTotalSalesTax();
    }

    public double getGrandTotal() {
        return UtilFormatOut.formatPriceNumber(cart.getGrandTotal()).doubleValue();
    }

    public int getNumberOfPayments() {
        return cart.selectedPayments();
    }

    public double getPaymentTotal() {
        return UtilFormatOut.formatPriceNumber(cart.getPaymentTotal()).doubleValue();
    }

    public double getTotalDue() {
        double grandTotal = this.getGrandTotal();
        double paymentAmt = this.getPaymentTotal();
        return (grandTotal - paymentAmt);
    }

    public int size() {
        return cart.size();
    }

    public Map getItemInfo(int index) {
        ShoppingCartItem item = cart.findCartItem(index);
        Map itemInfo = new HashMap();
        itemInfo.put("productId", item.getProductId());
        itemInfo.put("description", item.getDescription());
        itemInfo.put("quantity", UtilFormatOut.formatQuantity(item.getQuantity()));
        itemInfo.put("basePrice", UtilFormatOut.formatPrice(item.getBasePrice()));
        itemInfo.put("subtotal", UtilFormatOut.formatPrice(item.getItemSubTotal()));
        itemInfo.put("isTaxable", item.taxApplies() ? "T" : " ");
        itemInfo.put("adjustments", item.getOtherAdjustments() != 0 ?
                UtilFormatOut.formatPrice(item.getOtherAdjustments()) : "");

        return itemInfo;
    }

    public Map getPaymentInfo(int index) {
        ShoppingCart.CartPaymentInfo inf = cart.getPaymentInfo(index);
        GenericValue infValue = inf.getValueObject(session.getDelegator());
        GenericValue paymentPref = null;
        try {
            Map fields = new HashMap();
            fields.put("paymentMethodTypeId", inf.paymentMethodTypeId);
            if (inf.paymentMethodId != null) {
                fields.put("paymentMethodId", inf.paymentMethodId);
            }
            fields.put("maxAmount", inf.amount);
            fields.put("orderId", this.getOrderId());

            List paymentPrefs = session.getDelegator().findByAnd("OrderPaymentPreference", fields);
            if (paymentPrefs != null && paymentPrefs.size() > 0) {
                Debug.log("Found some prefs - " + paymentPrefs.size(), module);
                if (paymentPrefs.size() > 1) {
                    Debug.logError("Multiple OrderPaymentPreferences found for the same payment method!", module);
                } else {
                    paymentPref = EntityUtil.getFirst(paymentPrefs);
                    Debug.log("Got the first pref - " + paymentPref, module);
                }
            } else {
                Debug.logError("No OrderPaymentPreference found - " + fields, module);
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }
        Debug.log("PaymentPref - " + paymentPref, module);

        Map payInfo = new HashMap();

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
        Debug.log("AuthTrans - " + authTrans, module);

        if ("PaymentMethodType".equals(infValue.getEntityName())) {
            payInfo.put("description", infValue.getString("description"));
            payInfo.put("payInfo", infValue.getString("description"));
            payInfo.put("amount", UtilFormatOut.formatPrice(inf.amount));
        } else {
            String paymentMethodTypeId = infValue.getString("paymentMethodTypeId");
            GenericValue pmt = null;
            try {
                 pmt = infValue.getRelatedOne("PaymentMethodType");
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
            }
            if (pmt != null) {
                payInfo.put("description", pmt.getString("description"));
                payInfo.put("amount", UtilFormatOut.formatPrice(inf.amount));
            }

            if ("CREDIT_CARD".equals(paymentMethodTypeId)) {
                GenericValue cc = null;
                try {
                    cc = infValue.getRelatedOne("CreditCard");
                } catch (GenericEntityException e) {
                    Debug.logError(e, module);
                }
                String nameOnCard = cc.getString("firstNameOnCard") + " " + cc.getString("lastNameOnCard");
                nameOnCard = nameOnCard.trim();
                payInfo.put("nameOnCard", nameOnCard);

                String cardNum = cc.getString("cardNumber");
                String cardStr = cardNum.substring(0, 2);
                cardStr = cardStr + "****";
                cardStr = cardStr + cardNum.substring(cardNum.length() - 4);

                String expDate = cc.getString("expireDate");
                String infoString = cardStr + " " + expDate;
                payInfo.put("payInfo", infoString);
                payInfo.putAll(cc);


            } else if ("GIFT_CARD".equals(paymentMethodTypeId)) {
                GenericValue gc = null;
                try {
                    gc = infValue.getRelatedOne("GiftCard");
                } catch (GenericEntityException e) {
                    Debug.logError(e, module);
                }
            }
        }

        return payInfo;
    }

    public double getItemQuantity(String productId) {
        trace("request item quantity", productId);
        ShoppingCartItem item = cart.findCartItem(productId, null, null, null, 0.00);
        if (item != null) {
            return item.getQuantity();
        } else {
            trace("item not found", productId);
            return 0;
        }
    }

    public void addItem(String productId, double quantity) throws CartItemModifyException, ItemNotFoundException {
        trace("add item", productId + "/" + quantity);
        try {
            cart.addOrIncreaseItem(productId, null, quantity, null, null, null, null, null, null, null, null, null, null, null, null, session.getDispatcher());
        } catch (ItemNotFoundException e) {
            trace("item not found", e);
            throw e;
        } catch (CartItemModifyException e) {
            trace("add item error", e);
            throw e;
        }
    }

    public void modifyQty(String productId, double quantity) throws CartItemModifyException {
        trace("modify item quantity", productId + "/" + quantity);
        ShoppingCartItem item = cart.findCartItem(productId, null, null, null, 0.00);
        if (item != null) {
            try {
                item.setQuantity(quantity, session.getDispatcher(), cart, true);
            } catch (CartItemModifyException e) {
                Debug.logError(e, module);
                trace("modify item error", e);
                throw e;
            }
        } else {
            trace("item not found", productId);
        }
    }

    public void modifyPrice(String productId, double price) {
        trace("modify item price", productId + "/" + price);
        ShoppingCartItem item = cart.findCartItem(productId, null, null, null, 0.00);
        if (item != null) {
            item.setBasePrice(price);
        } else {
            trace("item not found", productId);
        }
    }

    public void addDiscount(String productId, double discount, boolean percent) {
        GenericValue adjustment = session.getDelegator().makeValue("OrderAdjustment", null);
        adjustment.set("orderAdjustmentTypeId", "DISCOUNT_ADJUSTMENT");
        if (percent) {
            adjustment.set("sourcePercentage", new Double(discount * 100));
        } else {
            adjustment.set("amount", new Double(discount));
        }

        if (productId != null) {
            trace("add item adjustment");
            ShoppingCartItem item = cart.findCartItem(productId, null, null, null, 0.00);
            Integer itemAdj = (Integer) skuDiscounts.get(productId);
            if (itemAdj != null) {
                item.removeAdjustment(itemAdj.intValue());
            }
               int idx = item.addAdjustment(adjustment);
            skuDiscounts.put(productId, new Integer(idx));
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
        if (skuDiscounts.size() > 0) {
            Iterator i = skuDiscounts.keySet().iterator();
            while (i.hasNext()) {
                String productId = (String) i.next();
                ShoppingCartItem item = cart.findCartItem(productId, null, null, null, 0.00);
                Integer itemAdj = (Integer) skuDiscounts.remove(productId);
                if (itemAdj != null) {
                    item.removeAdjustment(itemAdj.intValue());
                }
            }
        }
    }

    public void voidItem(String productId) throws CartItemModifyException {
        trace("void item", productId);
        ShoppingCartItem item = cart.findCartItem(productId, null, null, null, 0.00);
        if (item != null) {
            try {
                int itemIdx = cart.getItemIndex(item);
                cart.removeCartItem(itemIdx, session.getDispatcher());
            } catch (CartItemModifyException e) {
                Debug.logError(e, module);
                trace("void item error", productId, e);
                throw e;
            }
        } else {
            trace("item not found", productId);
        }
    }

    public void voidSale() {
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
        Map fields = UtilMisc.toMap("paymentMethodTypeId", paymentMethodTypeId, "productStoreId", productStoreId);
        List values = null;
        try {
            values = session.getDelegator().findByAndCache("ProductStorePaymentSetting", fields);
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }

        final String externalCode = "PRDS_PAY_EXTERNAL";
        if (UtilValidate.isEmpty(values)) {
            return NO_PAYMENT;
        } else {
            boolean isExternal = true;
            Iterator i = values.iterator();
            while (i.hasNext() && isExternal) {
                GenericValue v = (GenericValue) i.next();
                Debug.log("Testing [" + paymentMethodTypeId + "] - " + v, module);
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

    public double addPayment(String id, double amount) {
        return this.addPayment(id, amount, null, null);
    }

    public double addPayment(String id, double amount, String refNum, String authCode) {
        trace("added payment", id + "/" + amount);
        if ("CASH".equals(id)) {
            // clear cash payments first; so there is only one
            cart.clearPayment(id);
        }
        cart.addPaymentAmount(id, new Double(amount), refNum, authCode, true, true, false);
        return this.getTotalDue();
    }

    public void setPaymentRefNum(int paymentIndex, String refNum, String authCode) {
        trace("setting payment index reference number", paymentIndex + " / " + refNum + " / " + authCode);
        ShoppingCart.CartPaymentInfo inf = cart.getPaymentInfo(paymentIndex);
        inf.refNum[0] = refNum;
        inf.refNum[1] = authCode;
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

    public double processSale(Output output) throws GeneralException {
        trace("process sale");
        double grandTotal = this.getGrandTotal();
        double paymentAmt = this.getPaymentTotal();
        if (grandTotal > paymentAmt) {
            throw new IllegalStateException();
        }

        // attach the party ID to the cart
        cart.setOrderPartyId(partyId);

        // validate payment methods
        output.print(UtilProperties.getMessage("pos","Validating",defaultLocale));
        Map valRes = ch.validatePaymentMethods();
        if (valRes != null && ServiceUtil.isError(valRes)) {
            throw new GeneralException(ServiceUtil.getErrorMessage(valRes));
        }

        // store the "order"
        output.print(UtilProperties.getMessage("pos","Saving",defaultLocale));
        Map orderRes = ch.createOrder(session.getUserLogin());
        Debug.log("Create Order Resp : " + orderRes, module);

        if (orderRes != null && ServiceUtil.isError(orderRes)) {
            throw new GeneralException(ServiceUtil.getErrorMessage(orderRes));
        } else if (orderRes != null) {
            this.orderId = (String) orderRes.get("orderId");
        }

        // process the payment(s)
        output.print(UtilProperties.getMessage("pos","Processing",defaultLocale));
        Map payRes = null;
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
        double change = (grandTotal - paymentAmt);

        // notify the change due
        output.print(UtilProperties.getMessage("pos","CHANGE",defaultLocale) + " " + UtilFormatOut.formatPrice(this.getTotalDue() * -1));

        // threaded drawer/receipt printing
        final PosTransaction currentTrans = this;
        final SwingWorker worker = new SwingWorker() {
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

        return change;
    }

    private synchronized GenericValue getStoreOrgAddress() {
        if (this.shipAddress == null) {
            // locate the store's physical address - use this for tax
            GenericValue facility = (GenericValue) session.getAttribute("facility");
            if (facility == null) {
                return null;
            }

            List fcp = null;
            try {
                fcp = facility.getRelatedByAnd("FacilityContactMechPurpose", UtilMisc.toMap("contactMechPurposeTypeId", "SHIP_ORIG_LOCATION"));
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
            }
            fcp = EntityUtil.filterByDate(fcp);
            GenericValue purp = EntityUtil.getFirst(fcp);
            if (purp != null) {
                try {
                    this.shipAddress = session.getDelegator().findByPrimaryKey("PostalAddress",
                            UtilMisc.toMap("contactMechId", purp.getString("contactMechId")));
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
            Iterator i = cart.iterator();
            while (i.hasNext()) {
                ShoppingCartItem item = (ShoppingCartItem) i.next();
                double quantity = item.getQuantity();
                double unitPrice = item.getBasePrice();
                double subTotal = unitPrice * quantity;
                double adjustment = item.getOtherAdjustments();

                XModel line = Journal.appendNode(model, "tr", "", "");
                Journal.appendNode(line, "td", "sku", item.getProductId());
                Journal.appendNode(line, "td", "desc", item.getName());
                Journal.appendNode(line, "td", "qty", UtilFormatOut.formatQuantity(quantity));
                Journal.appendNode(line, "td", "price", UtilFormatOut.formatPrice(subTotal));
                Journal.appendNode(line, "td", "index", Integer.toString(cart.getItemIndex(item)));
                if (adjustment != 0) {
                    // append the promo info
                    XModel promo = Journal.appendNode(model, "tr", "", "");
                    Journal.appendNode(promo, "td", "sku", "");
                    Journal.appendNode(promo, "td", "desc", UtilProperties.getMessage("pos","(adjustment)",defaultLocale));
                    Journal.appendNode(promo, "td", "qty", "-");
                    Journal.appendNode(promo, "td", "price", UtilFormatOut.formatPrice(adjustment));
                }
            }
        }
    }

    public void appendTotalDataModel(XModel model) {
        if (cart != null) {
            double taxAmount = cart.getTotalSalesTax();
            double total = cart.getGrandTotal();

            XModel taxLine = Journal.appendNode(model, "tr", "", "");
            Journal.appendNode(taxLine, "td", "sku", "");

            Journal.appendNode(taxLine, "td", "desc", UtilProperties.getMessage("pos","Sales_Tax",defaultLocale));
            Journal.appendNode(taxLine, "td", "qty", "-");
            Journal.appendNode(taxLine, "td", "price", UtilFormatOut.formatPrice(taxAmount));

            XModel totalLine = Journal.appendNode(model, "tr", "", "");
            Journal.appendNode(totalLine, "td", "sku", "");
            Journal.appendNode(totalLine, "td", "desc", UtilProperties.getMessage("pos","Grand_Total",defaultLocale));
            Journal.appendNode(totalLine, "td", "qty", "-");
            Journal.appendNode(totalLine, "td", "price", UtilFormatOut.formatPrice(total));
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
                        paymentMethodType = paymentMethod.getRelatedOne("PaymentMethodType");
                    } catch (GenericEntityException e) {
                        Debug.logError(e, module);
                    }
                } else {
                    paymentMethodType = paymentInfoObj;
                }

                Object desc = paymentMethodType != null ? paymentMethodType.get("description",defaultLocale) : "??";
                String descString = desc.toString();
                double amount = 0;
                if (inf.amount == null) {
                    amount = cart.getGrandTotal() - cart.getPaymentTotal();
                } else {
                    amount = inf.amount.doubleValue();
                }

                XModel paymentLine = Journal.appendNode(model, "tr", "", "");
                Journal.appendNode(paymentLine, "td", "sku", "");
                Journal.appendNode(paymentLine, "td", "desc", descString);
                Journal.appendNode(paymentLine, "td", "qty", "-");
                Journal.appendNode(paymentLine, "td", "price", UtilFormatOut.formatPrice(-1 * amount));
                Journal.appendNode(paymentLine, "td", "index", Integer.toString(i));
            }
        }
    }

    public void appendChangeDataModel(XModel model) {
        if (cart != null) {
            double changeDue = (-1 * this.getTotalDue());
            if (changeDue >= 0) {
                XModel changeLine = Journal.appendNode(model, "tr", "", "");
                Journal.appendNode(changeLine, "td", "sku", "");
                Journal.appendNode(changeLine, "td", "desc", "Change");
                Journal.appendNode(changeLine, "td", "qty", "-");
                Journal.appendNode(changeLine, "td", "price", UtilFormatOut.formatPrice(changeDue));
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

        Map svcCtx = new HashMap();
        svcCtx.put("userLogin", session.getUserLogin());
        svcCtx.put("partyId", partyId);
        svcCtx.put("cardNumber", cardNumber);
        svcCtx.put("firstNameOnCard", firstName == null ? "" : firstName);
        svcCtx.put("lastNameOnCard", lastName == null ? "" : lastName);
        svcCtx.put("expMonth", expMonth);
        svcCtx.put("expYear", expYear);
        svcCtx.put("cardType", UtilValidate.getCardType(cardNumber));

        Debug.log("Create CC : " + svcCtx, module);
        Map svcRes = null;
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
        GenericDelegator delegator = session.getDelegator();
        List states = null;
        try {
            states = delegator.findByAnd("PosTerminalState", UtilMisc.toMap("posTerminalId", this.getTerminalId()));
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }
        states = EntityUtil.filterByDate(states, UtilDateTime.nowTimestamp(), "openedDate", "closedDate", true);
        return EntityUtil.getFirst(states);
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
        List shoppingLists = createShoppingLists();
        if (!shoppingLists.isEmpty()) {
            Hashtable salesMap = createSalesMap(shoppingLists);
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

    public List createShoppingLists() {
        List shoppingLists = null;
        GenericDelegator delegator = this.session.getDelegator();
        try {
            shoppingLists = delegator.findAll("ShoppingList");
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            ServiceUtil.returnError("Error running initLowLevelCode: " + e.getMessage());
        }

        if (shoppingLists == null) {
            Debug.log(UtilProperties.getMessage("EcommerceUiLabels","EcommerceNoShoppingListsCreate",locale), module);
        }
        return shoppingLists;
    }

    public Hashtable createSalesMap(List shoppingLists) {
        Hashtable salesMap = new Hashtable();
        Iterator i = shoppingLists.iterator();
        while (i.hasNext()){
            GenericValue shoppingList = (GenericValue) i.next();
            List items = null;
            try {
                items = shoppingList.getRelated("ShoppingListItem", UtilMisc.toList("shoppingListItemSeqId"));
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

    public boolean addListToCart(String  shoppingListId, PosScreen pos, boolean append) {
        GenericDelegator delegator = session.getDelegator();
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

    public boolean clearList(String shoppingListId, PosScreen pos) {
        GenericDelegator delegator = session.getDelegator();
        try {
        ShoppingListEvents.clearListInfo(delegator, shoppingListId);
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            pos.showDialog("dialog/error/exception", e.getMessage());
            return false;
        }
        return true;
    }


    public void saveSale(PosScreen pos) {
        SaveSale SaveSale = new SaveSale(this, pos);
        SaveSale.openDlg();
    }
    public void saveSale(String  shoppingListName, PosScreen pos) {
        if (cart.size() == 0 ) {
            pos.showDialog("dialog/error/exception", UtilProperties.getMessage("OrderErrorUiLabels", "OrderUnableToCreateNewShoppingList",locale));
            return;
        }
        GenericDelegator delegator = this.session.getDelegator();
        LocalDispatcher dispatcher = session.getDispatcher();
        GenericValue userLogin = session.getUserLogin();
        Locale locale = defaultLocale;
        String shoppingListId = null;

        if (!UtilValidate.isEmpty(shoppingListName)) {
            // create a new shopping list with partyId = user connected (POS clerk, etc.) and not buyer (_NA_ in POS)
            Map serviceCtx = UtilMisc.toMap("userLogin", session.getUserLogin(), "partyId", session.getUserPartyId(),
                    "productStoreId", productStoreId, "listName", shoppingListName);

            serviceCtx.put("shoppingListTypeId", "SLT_SPEC_PURP");
            Map newListResult = null;
            try {

                newListResult = dispatcher.runSync("createShoppingList", serviceCtx);
            } catch (GenericServiceException e) {
                Debug.logError(e, "Problem while creating new ShoppingList", module);
                pos.showDialog("dialog/error/exception", UtilProperties.getMessage("OrderErrorUiLabels", "OrderUnableToCreateNewShoppingList",locale));
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
                pos.showDialog("dialog/error/exception", UtilProperties.getMessage("OrderErrorUiLabels", "OrderUnableToCreateNewShoppingList",locale));
                return;
            }
        }

        String selectedCartItems[] = new String[cart.size()];
        for(int i = 0; i < cart.size(); i++) {
            Integer integer = new Integer(i);
            selectedCartItems[i] = integer.toString();
        }

        try {
            ShoppingListEvents.addBulkFromCart(delegator, dispatcher, cart, userLogin, shoppingListId, selectedCartItems, true, true);
        } catch (IllegalArgumentException e) {
            Debug.logError(e, "Problem while creating new ShoppingList", module);
            pos.showDialog("dialog/error/exception", UtilProperties.getMessage("OrderErrorUiLabels", "OrderUnableToCreateNewShoppingList",locale));
        }
    }
}
