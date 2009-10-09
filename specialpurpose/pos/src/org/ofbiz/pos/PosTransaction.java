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
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;

import javolution.util.FastList;
import javolution.util.FastMap;
import net.xoetrope.xui.data.XModel;
import net.xoetrope.xui.helper.SwingWorker;

import org.ofbiz.accounting.payment.PaymentGatewayServices;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.Log4jLoggerWriter;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilFormatOut;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilNumber;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.collections.LifoSet;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.model.DynamicViewEntity;
import org.ofbiz.entity.model.ModelKeyMap;
import org.ofbiz.entity.transaction.GenericTransactionException;
import org.ofbiz.entity.transaction.TransactionUtil;
import org.ofbiz.entity.util.EntityFindOptions;
import org.ofbiz.entity.util.EntityListIterator;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.guiapp.xui.XuiSession;
import org.ofbiz.order.shoppingcart.CartItemModifyException;
import org.ofbiz.order.shoppingcart.CheckOutHelper;
import org.ofbiz.order.shoppingcart.ItemNotFoundException;
import org.ofbiz.order.shoppingcart.ShoppingCart;
import org.ofbiz.order.shoppingcart.ShoppingCartItem;
import org.ofbiz.order.shoppinglist.ShoppingListEvents;
import org.ofbiz.party.contact.ContactMechWorker;
import org.ofbiz.pos.component.Journal;
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

    private static PrintWriter defaultPrintWriter = new Log4jLoggerWriter(Debug.getLogger(module));
    private static PosTransaction currentTx = null;
    private static LifoSet<PosTransaction> savedTx = new LifoSet<PosTransaction>();
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
    private Map<String, Integer> skuDiscounts = FastMap.newInstance();
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

    public Map getItemInfo(int index) {
        ShoppingCartItem item = cart.findCartItem(index);
        Map itemInfo = FastMap.newInstance();
        itemInfo.put("productId", item.getProductId());
        itemInfo.put("description", item.getDescription());
        itemInfo.put("quantity", UtilFormatOut.formatQuantity(item.getQuantity()));
        itemInfo.put("subtotal", UtilFormatOut.formatPrice(item.getItemSubTotal()));
        itemInfo.put("isTaxable", item.taxApplies() ? "T" : " ");

        itemInfo.put("discount", "");
        itemInfo.put("adjustments", "");
        if (item.getOtherAdjustments().compareTo(BigDecimal.ZERO) != 0) {
            itemInfo.put("itemDiscount", UtilFormatOut.padString(
                    UtilProperties.getMessage(PosTransaction.resource,"PosItemDiscount",defaultLocale), Receipt.pridLength[0] + 1, true, ' '));
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

    public List getItemConfigInfo(int index) {
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        // I think I need to initialize the list in a special way
        // to use foreach in receipt.java

        ShoppingCartItem item = cart.findCartItem(index);
        if (this.isAggregatedItem(item.getProductId())) {
            ProductConfigWrapper pcw = null;
            pcw = item.getConfigWrapper();
            List selected = pcw.getSelectedOptions();
            Iterator iter = selected.iterator();
            while (iter.hasNext()) {
                ConfigOption configoption = (ConfigOption)iter.next();
                Map itemInfo = FastMap.newInstance();
                if (configoption.isSelected() && !configoption.isDefault()) {
                    itemInfo.put("productId", "");
                    itemInfo.put("sku", "");
                    itemInfo.put("configDescription", configoption.getDescription());
                    itemInfo.put("configQuantity", UtilFormatOut.formatQuantity(item.getQuantity()));
                    itemInfo.put("configBasePrice", UtilFormatOut.formatPrice(configoption.getOffsetPrice()));
                    //itemInfo.put("isTaxable", item.taxApplies() ? "T" : " ");
                    list.add(itemInfo);
                }
            }
        }
        return list;
    }

    public Map getPaymentInfo(int index) {
        ShoppingCart.CartPaymentInfo inf = cart.getPaymentInfo(index);
        GenericValue infValue = inf.getValueObject(session.getDelegator());
        GenericValue paymentPref = null;
        try {
            Map fields = FastMap.newInstance();
            fields.put("paymentMethodTypeId", inf.paymentMethodTypeId);
            if (inf.paymentMethodId != null) {
                fields.put("paymentMethodId", inf.paymentMethodId);
            }
            fields.put("maxAmount", inf.amount);
            fields.put("orderId", this.getOrderId());

            List paymentPrefs = session.getDelegator().findByAnd("OrderPaymentPreference", fields);
            if (UtilValidate.isNotEmpty(paymentPrefs)) {
                //Debug.log("Found some prefs - " + paymentPrefs.size(), module);
                if (paymentPrefs.size() > 1) {
                    Debug.logError("Multiple OrderPaymentPreferences found for the same payment method!", module);
                } else {
                    paymentPref = EntityUtil.getFirst(paymentPrefs);
                    //Debug.log("Got the first pref - " + paymentPref, module);
                }
            } else {
                Debug.logError("No OrderPaymentPreference found - " + fields, module);
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }
        //Debug.log("PaymentPref - " + paymentPref, module);

        Map payInfo = FastMap.newInstance();

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
        //Debug.log("AuthTrans - " + authTrans, module);

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
                String cardStr = UtilFormatOut.formatPrintableCreditCard(cardNum);

                String expDate = cc.getString("expireDate");
                String infoString = cardStr + " " + expDate;
                payInfo.put("payInfo", infoString);
                payInfo.putAll(cc);
                payInfo.put("cardNumber", cardStr);  // masked cardNumber

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

    public BigDecimal getItemQuantity(String productId) {
        trace("request item quantity", productId);
        ShoppingCartItem item = cart.findCartItem(productId, null, null, null, BigDecimal.ZERO);
        if (item != null) {
            return item.getQuantity();
        } else {
            trace("item not found", productId);
            return BigDecimal.ZERO;
        }
    }

    public boolean isAggregatedItem(String productId) {
        trace("is Aggregated Item", productId);
        try {
            Delegator delegator = cart.getDelegator();
            GenericValue product = null;
            product = delegator.findByPrimaryKeyCache("Product", UtilMisc.toMap("productId", productId));
            if ("AGGREGATED".equals(product.getString("productTypeId"))) {
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
            Delegator delegator = cart.getDelegator();
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
            product = delegator.findByPrimaryKeyCache("Product", UtilMisc.toMap("productId", productId));
            if ("AGGREGATED".equals(product.getString("productTypeId"))) {
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

    public void addItem(String productId, ProductConfigWrapper pcw)
        throws ItemNotFoundException, CartItemModifyException {
        trace("add item with ProductConfigWrapper", productId);
        try {
            cart.addOrIncreaseItem(productId, null, BigDecimal.ONE, null, null, null, null, null, null, null, null, pcw, null, null, null, session.getDispatcher());
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

    public void modifyQty(String productId, BigDecimal quantity) throws CartItemModifyException {
        trace("modify item quantity", productId + "/" + quantity);
        ShoppingCartItem item = cart.findCartItem(productId, null, null, null, BigDecimal.ZERO);
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

    public void modifyPrice(String productId, BigDecimal price) {
        trace("modify item price", productId + "/" + price);
        ShoppingCartItem item = cart.findCartItem(productId, null, null, null, BigDecimal.ZERO);
        if (item != null) {
            item.setBasePrice(price);
        } else {
            trace("item not found", productId);
        }
    }

    public void addDiscount(String productId, BigDecimal discount, boolean percent) {
        GenericValue adjustment = session.getDelegator().makeValue("OrderAdjustment");
        adjustment.set("orderAdjustmentTypeId", "DISCOUNT_ADJUSTMENT");
        if (percent) {
            adjustment.set("sourcePercentage", discount.movePointRight(2));
        } else {
            adjustment.set("amount", discount);
        }

        if (productId != null) {
            trace("add item adjustment");
            ShoppingCartItem item = cart.findCartItem(productId, null, null, null, BigDecimal.ZERO);
            Integer itemAdj = skuDiscounts.get(productId);
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
        for(String productId : skuDiscounts.keySet()) {
            ShoppingCartItem item = cart.findCartItem(productId, null, null, null, BigDecimal.ZERO);
            Integer itemAdj = (Integer) skuDiscounts.remove(productId);
            if (itemAdj != null) {
                item.removeAdjustment(itemAdj.intValue());
            }
        }
    }

    public BigDecimal GetTotalDiscount() {
        return cart.getOrderOtherAdjustmentTotal();
    }

    public void voidItem(String productId) throws CartItemModifyException {
        trace("void item", productId);
        ShoppingCartItem item = cart.findCartItem(productId, null, null, null, BigDecimal.ZERO);
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
                //Debug.log("Testing [" + paymentMethodTypeId + "] - " + v, module);
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
        cart.setShipmentMethodTypeId("NO_SHIPPING");
       // cart.setCarrierPartyId();

        // validate payment methods
        output.print(UtilProperties.getMessage(PosTransaction.resource,"PosValidating",defaultLocale));
        Map valRes = ch.validatePaymentMethods();
        if (valRes != null && ServiceUtil.isError(valRes)) {
            throw new GeneralException(ServiceUtil.getErrorMessage(valRes));
        }

        // store the "order"
         if (UtilValidate.isEmpty(this.orderId)) {  // if order does not exist
             output.print(UtilProperties.getMessage(PosTransaction.resource,"PosSaving",defaultLocale));
             Map orderRes = ch.createOrder(session.getUserLogin());
             //Debug.log("Create Order Resp : " + orderRes, module);

             if (orderRes != null && ServiceUtil.isError(orderRes)) {
                 throw new GeneralException(ServiceUtil.getErrorMessage(orderRes));
             } else if (orderRes != null) {
                 this.orderId = (String) orderRes.get("orderId");
             }
         } else { // if the order has already been created
             Map changeMap = UtilMisc.toMap("itemReasonMap",
                     UtilMisc.toMap("reasonEnumId", "EnumIdHere"), // TODO: where does this come from?
                     "itemCommentMap",
                     UtilMisc.toMap("changeComments", "change Comments here")); //TODO

             Map svcCtx = FastMap.newInstance();
             svcCtx.put("userLogin", session.getUserLogin());
             svcCtx.put("orderId", orderId);
             svcCtx.put("shoppingCart", cart);
             svcCtx.put("locale", this.locale);
             svcCtx.put("changeMap", changeMap);

             Map svcRes = null;
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
        output.print(UtilProperties.getMessage(PosTransaction.resource, "PosProcessing", defaultLocale));
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
        BigDecimal change = grandTotal.subtract(paymentAmt);

        // notify the change due
        output.print(UtilProperties.getMessage(PosTransaction.resource,"PosChange",defaultLocale) + " " + UtilFormatOut.formatPrice(this.getTotalDue().negate()));

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
                    this.shipAddress = session.getDelegator().findByPrimaryKey("PostalAddress",
                            UtilMisc.toMap("contactMechId", facilityContactMech.getString("contactMechId")));
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
                BigDecimal quantity = item.getQuantity();
                BigDecimal unitPrice = item.getBasePrice();
                BigDecimal subTotal = unitPrice.multiply(quantity);
                BigDecimal adjustment = item.getOtherAdjustments();

                XModel line = Journal.appendNode(model, "tr", ""+cart.getItemIndex(item), "");
                Journal.appendNode(line, "td", "sku", item.getProductId());
                Journal.appendNode(line, "td", "desc", item.getName());
                Journal.appendNode(line, "td", "qty", UtilFormatOut.formatQuantity(quantity));
                Journal.appendNode(line, "td", "price", UtilFormatOut.formatPrice(subTotal));
                Journal.appendNode(line, "td", "index", Integer.toString(cart.getItemIndex(item)));

                if (this.isAggregatedItem(item.getProductId())) {
                    // put alterations here
                    ProductConfigWrapper pcw = null;
                    // product = delegator.findByPrimaryKeyCache("Product", UtilMisc.toMap("productId", productId));
                    // pcw = new ProductConfigWrapper(delegator, session.getDispatcher(), productId, null, null, null, null, null, null);
                    pcw = item.getConfigWrapper();
                    List selected = pcw.getSelectedOptions();
                    Iterator iter = selected.iterator();
                    while (iter.hasNext()) {
                        ConfigOption configoption = (ConfigOption)iter.next();
                        if (configoption.isSelected()) {
                            XModel option = Journal.appendNode(model, "tr", ""+cart.getItemIndex(item), "");
                            Journal.appendNode(option, "td", "sku", "");
                            Journal.appendNode(option, "td", "desc", configoption.getDescription());
                            Journal.appendNode(option, "td", "qty", "");
                            Journal.appendNode(option, "td", "price", UtilFormatOut.formatPrice(configoption.getPrice()));
                            Journal.appendNode(option, "td", "index", Integer.toString(cart.getItemIndex(item)));
                        }
                    }
                }

                if (adjustment.compareTo(BigDecimal.ZERO) != 0) {
                    // append the promo info
                    XModel promo = Journal.appendNode(model, "tr", "itemadjustment", "");
                    Journal.appendNode(promo, "td", "sku", "");
                    Journal.appendNode(promo, "td", "desc", UtilProperties.getMessage(PosTransaction.resource,"PosItemDiscount",defaultLocale));
                    Journal.appendNode(promo, "td", "qty", "");
                    Journal.appendNode(promo, "td", "price", UtilFormatOut.formatPrice(adjustment));
                }
            }
        }
    }

    public void appendTotalDataModel(XModel model) {
        if (cart != null) {
            BigDecimal taxAmount = cart.getTotalSalesTax();
            BigDecimal total = cart.getGrandTotal();
            List adjustments = cart.getAdjustments();
            BigDecimal itemsAdjustmentsAmount = BigDecimal.ZERO;

            Iterator i = cart.iterator();
            while (i.hasNext()) {
                ShoppingCartItem item = (ShoppingCartItem) i.next();
                BigDecimal adjustment = item.getOtherAdjustments();
                if (adjustment.compareTo(BigDecimal.ZERO) != 0) {
                    itemsAdjustmentsAmount = itemsAdjustmentsAmount.add(adjustment);
                }
            }

            if (UtilValidate.isNotEmpty(adjustments)) {
                Iterator iter = adjustments.iterator();
                while (iter.hasNext()) {
                    GenericValue orderAdjustment = (GenericValue) iter.next();
                    BigDecimal amount = orderAdjustment.getBigDecimal("amount");
                }

                iter = adjustments.iterator();
                while (iter.hasNext()) {
                    GenericValue orderAdjustment = (GenericValue) iter.next();
                    BigDecimal amount = orderAdjustment.getBigDecimal("amount");
                    BigDecimal sourcePercentage = orderAdjustment.getBigDecimal("sourcePercentage");
                    XModel adjustmentLine = Journal.appendNode(model, "tr", "adjustment", "");
                    Journal.appendNode(adjustmentLine, "td", "sku", "");
                    Journal.appendNode(adjustmentLine, "td", "desc",
                            UtilProperties.getMessage(PosTransaction.resource, "PosSalesDiscount",defaultLocale));
                    if (UtilValidate.isNotEmpty(amount)) {
                        Journal.appendNode(adjustmentLine, "td", "qty", "");
                        Journal.appendNode(adjustmentLine, "td", "price", UtilFormatOut.formatPrice(amount));
                    } else if (UtilValidate.isNotEmpty(sourcePercentage)) {
                        BigDecimal percentage = sourcePercentage.movePointLeft(2).negate(); // sourcePercentage is negative and must be show as a positive value (it's a discount not an amount)
                        Journal.appendNode(adjustmentLine, "td", "qty", UtilFormatOut.formatPercentage(percentage));
                        amount = cart.getItemTotal().add(itemsAdjustmentsAmount).multiply(percentage); // itemsAdjustmentsAmount is negative
                        Journal.appendNode(adjustmentLine, "td", "price", UtilFormatOut.formatPrice(amount.negate())); // amount must be shown as a negative value
                    }
                    Journal.appendNode(adjustmentLine, "td", "index", "-1");
                }
            }

            XModel taxLine = Journal.appendNode(model, "tr", "tax", "");
            Journal.appendNode(taxLine, "td", "sku", "");

            Journal.appendNode(taxLine, "td", "desc", UtilProperties.getMessage(PosTransaction.resource,"PosSalesTax",defaultLocale));
            Journal.appendNode(taxLine, "td", "qty", "");
            Journal.appendNode(taxLine, "td", "price", UtilFormatOut.formatPrice(taxAmount));
            Journal.appendNode(taxLine, "td", "index", "-1");

            XModel totalLine = Journal.appendNode(model, "tr", "total", "");
            Journal.appendNode(totalLine, "td", "sku", "");
            Journal.appendNode(totalLine, "td", "desc", UtilProperties.getMessage(PosTransaction.resource,"PosGrandTotal",defaultLocale));
            Journal.appendNode(totalLine, "td", "qty", "");
            Journal.appendNode(totalLine, "td", "price", UtilFormatOut.formatPrice(total));
            Journal.appendNode(totalLine, "td", "index", "-1");
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
                BigDecimal amount = BigDecimal.ZERO;
                if (inf.amount == null) {
                    amount = cart.getGrandTotal().subtract(cart.getPaymentTotal());
                } else {
                    amount = inf.amount;
                }

                XModel paymentLine = Journal.appendNode(model, "tr", Integer.toString(i), "");
                Journal.appendNode(paymentLine, "td", "sku", "");
                Journal.appendNode(paymentLine, "td", "desc", descString);
                Journal.appendNode(paymentLine, "td", "qty", "-");
                Journal.appendNode(paymentLine, "td", "price", UtilFormatOut.formatPrice(amount.negate()));
                Journal.appendNode(paymentLine, "td", "index", Integer.toString(i));
            }
        }
    }

    public void appendChangeDataModel(XModel model) {
        if (cart != null) {
            BigDecimal changeDue = this.getTotalDue().negate();
            if (changeDue.compareTo(BigDecimal.ZERO) >= 0) {
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

        Map svcCtx = FastMap.newInstance();
        svcCtx.put("userLogin", session.getUserLogin());
        svcCtx.put("partyId", partyId);
        svcCtx.put("cardNumber", cardNumber);
        svcCtx.put("firstNameOnCard", firstName == null ? "" : firstName);
        svcCtx.put("lastNameOnCard", lastName == null ? "" : lastName);
        svcCtx.put("expMonth", expMonth);
        svcCtx.put("expYear", expYear);
        svcCtx.put("cardType", UtilValidate.getCardType(cardNumber));

        //Debug.log("Create CC : " + svcCtx, module);
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
        Delegator delegator = session.getDelegator();
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
        trace("Load a sale");
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

    public void loadOrder(PosScreen pos) {
        List<GenericValue> orders = findOrders();
        if (!orders.isEmpty()) {
            LoadSale loadSale = new LoadSale(createOrderHash(orders), this, pos);
            loadSale.openDlg();
        } else {
            pos.showDialog("dialog/error/nosales");
        }
    }

    private List<GenericValue> findOrders() {
        LocalDispatcher dispatcher = session.getDispatcher();

        Map svcCtx = FastMap.newInstance();
        svcCtx.put("userLogin", session.getUserLogin());
        svcCtx.put("partyId", partyId);
        List orderStatusIds = new ArrayList();
        orderStatusIds.add("ORDER_CREATED");
        svcCtx.put("orderStatusId", orderStatusIds);
        svcCtx.put("viewIndex", 1);
        svcCtx.put("viewSize", 25);
        svcCtx.put("showAll", "Y");

        Map svcRes = null;
        try {
            svcRes = dispatcher.runSync("findOrders", svcCtx);
        } catch (GenericServiceException e) {
            Debug.logError(e, module);
        }

        if (svcRes == null) {
            Debug.log(UtilProperties.getMessage("EcommerceUiLabels","EcommerceNoShoppingListsCreate",locale), module);
        } else if (ServiceUtil.isError(svcRes)) {
            Debug.logError(ServiceUtil.getErrorMessage(svcRes) + " - " + svcRes, module);
        } else{
            Integer orderListSize = (Integer) svcRes.get("orderListSize");
            if (orderListSize > 0) {
               List orderList = (List) svcRes.get("orderList");
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

    public List createShoppingLists() {
        List shoppingLists = null;
        Delegator delegator = this.session.getDelegator();
        try {
            shoppingLists = delegator.findList("ShoppingList", null, null, null, null, false);
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
        while (i.hasNext()) {
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

    public Map<String, String> createOrderHash(List<GenericValue> orders) {
        Map<String, String> hash = FastMap.newInstance();
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
        Delegator delegator = session.getDelegator();
        LocalDispatcher dispatcher = session.getDispatcher();

        Map svcCtx = FastMap.newInstance();
        svcCtx.put("userLogin", session.getUserLogin());
        svcCtx.put("orderId", orderId);
        svcCtx.put("skipInventoryChecks", Boolean.TRUE);
        svcCtx.put("skipProductChecks", Boolean.TRUE);

        Map svcRes = null;
        try {
            svcRes = dispatcher.runSync("loadCartFromOrder", svcCtx);
        } catch (GenericServiceException e) {
            Debug.logError(e, module);
            pos.showDialog("dialog/error/exception", e.getMessage());
        }

        if (svcRes == null) {
            Debug.log(UtilProperties.getMessage("EcommerceUiLabels","EcommerceNoShoppingListsCreate",locale), module);
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
            pos.showDialog("dialog/error/exception", UtilProperties.getMessage("OrderErrorUiLabels", "OrderUnableToCreateNewShoppingList",locale));
            return;
        }
        Delegator delegator = this.session.getDelegator();
        LocalDispatcher dispatcher = session.getDispatcher();
        GenericValue userLogin = session.getUserLogin();
        Locale locale = defaultLocale;
        String shoppingListId = null;

        if (!UtilValidate.isEmpty(shoppingListName)) {
            // attach the party ID to the cart
            cart.setOrderPartyId(partyId);
            cart.setOrderName(shoppingListName);
            //cart.setExternalId(shoppingListName);
            //cart.setInternalCode("Internal Code");
            //ch.setCheckOutOptions(null, null, null, null, null, "shipping instructions", null, null, null, "InternalId", null, null, null);
            Map orderRes = ch.createOrder(session.getUserLogin());

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
            pos.showDialog("dialog/error/exception", UtilProperties.getMessage("OrderErrorUiLabels", "OrderUnableToCreateNewShoppingList",locale));
            return;
        }
        Delegator delegator = this.session.getDelegator();
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

    public String addProductPromoCode(String code) {
        trace("Add a promo code");
        LocalDispatcher dispatcher = session.getDispatcher();
        String result = cart.addProductPromoCode(code, dispatcher);
        calcTax();
        return result;
    }

    // TODO, I really wonder if there is not a better way to do this (DynamicView excluded because of the contactMechId collisions between phone and email)!
    private List<Map<String, String>> searchContactMechs(Delegator delegator, PosScreen pos, List<Map<String, String>> partyList, String valueToCompare, String contactMechType) {
        ListIterator<Map<String, String>>  partyListIt = partyList.listIterator();
        while(partyListIt.hasNext()) {
            Map<String, String> party = (Map<String, String>) partyListIt.next();
            String partyId = (String) party.get("partyId");
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
                Map<String, Object> keyTypeMap = (Map<String, Object>) partyContactMechValueMap.get(keyType);
                String keyTypeValue = ((String) keyTypeMap.get(key)).trim();
                if (valueToCompare.equals(keyTypeValue) || UtilValidate.isEmpty(valueToCompare)) {
                    if (nb == 1) {
                        party.put(key, keyTypeValue);
                        partyListIt.set(party);
                    } else {
                        Map partyClone = FastMap.newInstance();
                        partyClone.putAll(party);
                        partyClone.put(key, keyTypeValue);
                        partyListIt.add(partyClone);
                    }
                }
            }
        }
        return partyList;
    }


    public List<Map<String, String>> searchClientProfile(String name, String email, String  phone, String card, PosScreen pos) {
        Delegator delegator = this.session.getDelegator();
        LocalDispatcher dispatcher = session.getDispatcher();
        GenericValue userLogin = session.getUserLogin();
        Locale locale = defaultLocale;

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
        dynamicView.addAlias("PE", "memberId");
        dynamicView.addAlias("PE", "lastNameLocal");
        dynamicView.addViewLink("PT", "PE", Boolean.FALSE, ModelKeyMap.makeKeyMapList("partyId"));

        Boolean onlyPhone = UtilValidate.isEmpty(name) && UtilValidate.isEmpty(email) && UtilValidate.isNotEmpty(phone) && UtilValidate.isEmpty(card);
        if (!onlyPhone) {
            // ContactMech (email)
            dynamicView.addMemberEntity("PM", "PartyContactMechPurpose");            
            dynamicView.addMemberEntity("CM", "ContactMech");
            dynamicView.addAlias("PM", "contactMechId");
            dynamicView.addAlias("PM", "contactMechPurposeTypeId");            
            dynamicView.addAlias("PM", "thruDate");
            dynamicView.addAlias("CM", "infoString");            
            dynamicView.addViewLink("PT", "PM", Boolean.FALSE, ModelKeyMap.makeKeyMapList("partyId"));
            dynamicView.addViewLink("PM", "CM", Boolean.FALSE, ModelKeyMap.makeKeyMapList("contactMechId"));
        } else {
            dynamicView.addMemberEntity("PM", "PartyContactMechPurpose");            
            dynamicView.addMemberEntity("TN", "TelecomNumber");
            dynamicView.addAlias("PM", "contactMechId");
            dynamicView.addAlias("PM", "thruDate");
            dynamicView.addAlias("PM", "contactMechPurposeTypeId");            
            dynamicView.addAlias("TN", "contactNumber");
            dynamicView.addViewLink("PT", "PM", Boolean.FALSE, ModelKeyMap.makeKeyMapList("partyId"));
            dynamicView.addViewLink("PM", "TN", Boolean.FALSE, ModelKeyMap.makeKeyMapList("contactMechId"));
        }

            // define the main condition & expression list
            List<EntityCondition> andExprs = FastList.newInstance();
            EntityCondition mainCond = null;

            List<String> orderBy = FastList.newInstance();
            List<String> fieldsToSelect = FastList.newInstance();
            // fields we need to select; will be used to set distinct
            fieldsToSelect.add("partyId");
            fieldsToSelect.add("lastName");
            fieldsToSelect.add("memberId");
            if (!onlyPhone) {
                fieldsToSelect.add("infoString");
            } else {
                fieldsToSelect.add("contactNumber");
            }

            // NOTE: _must_ explicitly allow null as it is not included in a not equal in many databases... odd but true
            // This allows to get all clients when any informations has been entered
            andExprs.add(EntityCondition.makeCondition(EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, null), EntityOperator.OR, EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "PARTY_DISABLED")));
            andExprs.add(EntityCondition.makeCondition("partyTypeId", EntityOperator.EQUALS, "PERSON")); // Only persons for now...
            if (UtilValidate.isNotEmpty(name)) {
                andExprs.add(EntityCondition.makeCondition("lastName", EntityOperator.EQUALS, name));
            }
            if (UtilValidate.isNotEmpty(card)) {
                andExprs.add(EntityCondition.makeCondition("memberId", EntityOperator.EQUALS, card));
            }
            if (UtilValidate.isNotEmpty(email)) {
                andExprs.add(EntityCondition.makeCondition("infoString", EntityOperator.EQUALS, email));
                andExprs.add(EntityCondition.makeCondition("contactMechPurposeTypeId", EntityOperator.EQUALS, "PRIMARY_EMAIL"));
                andExprs.add(EntityCondition.makeCondition("thruDate", EntityOperator.EQUALS, null));
            }
            if (onlyPhone) {
                andExprs.add(EntityCondition.makeCondition("contactNumber", EntityOperator.EQUALS, phone));
                andExprs.add(EntityCondition.makeCondition("contactMechPurposeTypeId", EntityOperator.EQUALS, "PHONE_HOME"));
                andExprs.add(EntityCondition.makeCondition("thruDate", EntityOperator.EQUALS, null));
            } else if (UtilValidate.isEmpty(email)) {
                andExprs.add(EntityCondition.makeCondition("infoString", EntityOperator.NOT_EQUAL, null));                
                andExprs.add(EntityCondition.makeCondition("contactMechPurposeTypeId", EntityOperator.EQUALS, "PRIMARY_EMAIL"));
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
                    EntityFindOptions findOpts = new EntityFindOptions(true, EntityFindOptions.TYPE_SCROLL_INSENSITIVE, EntityFindOptions.CONCUR_READ_ONLY, -1, maxRows, true);
                    // using list iterator
                    EntityListIterator pli = delegator.findListIteratorByCondition(dynamicView, mainCond, null, fieldsToSelect, orderBy, findOpts);

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
                resultList = FastList.newInstance();
                for (GenericValue party : partyList) {
                    Map partyMap = FastMap.newInstance();
                    partyMap.put("partyId", party.getString("partyId"));
                    partyMap.put("lastName", party.getString("lastName"));
                    partyMap.put("memberId", party.getString("memberId"));
                    if (!onlyPhone) {
                        partyMap.put("infoString", party.getString("infoString"));
                        partyMap.put("contactNumber", "");
                    } else {
                        partyMap.put("contactNumber", party.getString("contactNumber"));
                        partyMap.put("infoString", "");
                    }
                    resultList.add(partyMap);
                }
                if (!onlyPhone) {
                    resultList = searchContactMechs(delegator, pos, resultList, phone, "TELECOM_NUMBER");
                } else {
                    resultList = searchContactMechs(delegator, pos, resultList, "", "EMAIL_ADDRESS"); //"" is more clear than email which is by definition here is empty
                }
            } else {
            resultList = FastList.newInstance();
        }
        return resultList;
    }

    public String editClientProfile(String name, String email, String  phone, String card, PosScreen pos, String editType, String partyId) {
        // We suppose here that a memberId (card number) can only belongs to one person (it's used as owned PromoCode)
        // We use the 1st party's login (it may change and be multiple since it depends on email and card)
        // We suppose only one email address (should be ok anyway because of the contactMechPurposeTypeId == "PRIMARY_EMAIL")
        // we suppose only one phone number (should be ok anyway because of the contactMechPurposeTypeId == "PHONE_HOME")
        Delegator delegator = session.getDelegator();
        LocalDispatcher dispatcher = session.getDispatcher();
        GenericValue userLogin = session.getUserLogin();
        String result = null;

        Map<String, Object> svcCtx = FastMap.newInstance();
        Map svcRes = null;

        if ("create".equals(editType)) {
            // Create
            trace("Create a client profile");
            svcCtx.put("memberId", card);
            svcCtx.put("lastName", name);
            svcCtx.put("firstName", ""); // Needed by service createPersonAndUserLogin
            svcCtx.put("userLogin", userLogin);
            svcCtx.put("userLoginId", email);
            svcCtx.put("currentPassword", card);
            svcCtx.put("currentPasswordVerify", card);
            svcCtx.put("passwordHint", "Your card number is your password");            

            // createPersonAndUserLogin
            try {
                svcRes = dispatcher.runSync("createPersonAndUserLogin", svcCtx);
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
            GenericValue newUserLogin = (GenericValue) svcRes.get("newUserLogin");

            // createPartyEmailAddress
            svcCtx.clear();
            svcCtx.put("userLogin", newUserLogin);
            svcCtx.put("emailAddress", email);
            svcCtx.put("partyId", partyId);
            svcCtx.put("contactMechPurposeTypeId", "PRIMARY_EMAIL");
            try {
                svcRes = dispatcher.runSync("createPartyEmailAddress", svcCtx);
            } catch (GenericServiceException e) {
                Debug.logError(e, module);
                pos.showDialog("dialog/error/exception", e.getMessage());
                return result;
            }
            if (ServiceUtil.isError(svcRes)) {
                pos.showDialog("dialog/error/exception", ServiceUtil.getErrorMessage(svcRes));
                return result;
            }

            // createPartyTelecomNumber
            svcCtx.clear();
            svcCtx.put("userLogin", newUserLogin);
            svcCtx.put("contactNumber", phone);
            svcCtx.put("partyId", partyId);
            svcCtx.put("contactMechPurposeTypeId", "PHONE_HOME");
            try {
                svcRes = dispatcher.runSync("createPartyTelecomNumber", svcCtx);
            } catch (GenericServiceException e) {
                Debug.logError(e, module);
                pos.showDialog("dialog/error/exception", e.getMessage());
                return result;
            }
            if (ServiceUtil.isError(svcRes)) {
                pos.showDialog("dialog/error/exception", ServiceUtil.getErrorMessage(svcRes));
                return result;
            }
            result = partyId;
        } else {
            trace("Update a client profile");
            List<GenericValue>  userLogins = null;
            try {
                userLogins = session.getDelegator().findByAnd("UserLogin", UtilMisc.toMap("partyId", partyId));
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
                pos.showDialog("dialog/error/exception", e.getMessage());
                return result;
            }
            GenericValue partyLogin = userLogins.get(0); // We need at least a party's login ...
            GenericValue  person = null;
            try {
                person = session.getDelegator().findByPrimaryKey("Person", UtilMisc.toMap("partyId", partyId));
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
                pos.showDialog("dialog/error/exception", e.getMessage());
                return result;
            }
            
            if (UtilValidate.isNotEmpty(name) && !person.getString("lastName").equals(name)
                    || UtilValidate.isNotEmpty(card) && !person.getString("memberId").equals(card)) {
                svcCtx.put("partyId", partyId);
                svcCtx.put("firstName", ""); // Needed by service updatePerson
                svcCtx.put("userLogin", partyLogin);
                if (UtilValidate.isNotEmpty(name)) {
                    svcCtx.put("lastName", name);
                }
                if (UtilValidate.isNotEmpty(card)) {
                    svcCtx.put("memberId", card);
                    if (!person.getString("memberId").equals(card)) {
                        // Update password
                        UtilProperties.setPropertyValue("security.properties", "password.accept.encrypted.and.plain", "true");
                        try {
                            svcRes = dispatcher.runSync("updatePassword", 
                                    UtilMisc.toMap("userLogin", partyLogin,
                                    "userLoginId", partyLogin.getString("userLoginId"), 
                                    "currentPassword", partyLogin.getString("currentPassword"), 
                                    "newPassword", card, 
                                    "newPasswordVerify", card,
                                    "passwordHint", "Your card number is your password"));            
                        } catch (GenericServiceException e) {
                            Debug.logError(e, "Error calling updatePassword service", module);
                            pos.showDialog("dialog/error/exception", e.getMessage());
                            return result;
                        }
                        if (ServiceUtil.isError(svcRes)) {
                            pos.showDialog("dialog/error/exception", ServiceUtil.getErrorMessage(svcRes));
                            return result;
                        }
                        // This remove comments from the security.properties file. I did not find a way to keep them, so I put a word about that
                        UtilProperties.setPropertyValue("security.properties", "password.accept.encrypted.and.plain", "false");
                        partyLogin = (GenericValue) svcRes.get("updatedUserLogin");
                        svcCtx.put("userLogin", partyLogin);
                    }                    
                }
    
                // Update name and card (memberId)
                try {
                    svcRes = dispatcher.runSync("updatePerson", svcCtx);
                } catch (GenericServiceException e) {
                    Debug.logError(e, module);
                    pos.showDialog("dialog/error/exception", e.getMessage());
                    return result;
                }
                if (ServiceUtil.isError(svcRes)) {
                    pos.showDialog("dialog/error/exception", ServiceUtil.getErrorMessage(svcRes));
                    return result;
                }                
            }

            // Update email            
            if (UtilValidate.isNotEmpty(email) && !partyLogin.getString("userLoginId").equals(email)) {                
                // create a new UserLogin (Update a UserLoginId by creating a new one and expiring the old one)
                try {
                    svcRes = dispatcher.runSync("updateUserLoginId", UtilMisc.toMap("userLoginId", email, "userLogin", partyLogin));
                } catch (GenericServiceException e) {
                    Debug.logError(e, module);
                    pos.showDialog("dialog/error/exception", e.getMessage());
                   return result;
                }
                if (ServiceUtil.isError(svcRes)) {
                    pos.showDialog("dialog/error/exception", ServiceUtil.getErrorMessage(svcRes));
                    return result;
                }
                partyLogin = (GenericValue) svcRes.get("newUserLogin");
                
                svcCtx.clear();
                svcCtx.put("partyId", partyId);
                svcCtx.put("contactMechTypeId", "EMAIL_ADDRESS");                
                svcCtx.put("thruDate", null); // last one                
                List<GenericValue>  PartyEmails = null;
                try {
                    PartyEmails = session.getDelegator().findByAnd("PartyAndContactMech", svcCtx);
                } catch (GenericEntityException e) {
                    Debug.logError(e, module);
                    pos.showDialog("dialog/error/exception", e.getMessage());
                    return result;
                }

                svcCtx.clear();
                svcCtx.put("userLogin", partyLogin);
                svcCtx.put("emailAddress", email);
                svcCtx.put("partyId", partyId);
                svcCtx.put("contactMechPurposeTypeId", "PRIMARY_EMAIL");

                if (UtilValidate.isNotEmpty(PartyEmails)) {
                    svcCtx.put("contactMechId", PartyEmails.get(0).getString("contactMechId")); // We suppose only one email address (should be ok anyway because of the contactMechPurposeTypeId == "PRIMARY_EMAIL")
                }
                try {
                    svcRes = dispatcher.runSync("createUpdatePartyEmailAddress", svcCtx);
                } catch (GenericServiceException e) {
                    Debug.logError(e, module);
                    pos.showDialog("dialog/error/exception", e.getMessage());
                    return result;
                }
                if (ServiceUtil.isError(svcRes)) {
                    pos.showDialog("dialog/error/exception", ServiceUtil.getErrorMessage(svcRes));
                    return result;
                }
            }

            // Update phone
            if (UtilValidate.isNotEmpty(phone)) {
                svcCtx.clear();
                svcCtx.put("partyId", partyId);
                svcCtx.put("thruDate", null); // last one
                List<GenericValue>  PartyTelecomNumbers = null;
                try {
                    PartyTelecomNumbers = session.getDelegator().findByAnd("PartyAndTelecomNumber", svcCtx);
                } catch (GenericEntityException e) {
                    Debug.logError(e, module);
                    pos.showDialog("dialog/error/exception", e.getMessage());
                    return result;
                }
                GenericValue PartyTelecomNumber = PartyTelecomNumbers.get(0); // we suppose only one phone number (should be ok anyway because of the contactMechPurposeTypeId == "PHONE_HOME")
                String contactNumber = PartyTelecomNumber.getString("contactNumber");
                if (!contactNumber.equals(phone)) {
                    String newContactMechId = PartyTelecomNumber.getString("contactMechId");
    
                    svcCtx.put("userLogin", partyLogin);
                    svcCtx.put("contactNumber", phone);
                    svcCtx.put("contactMechPurposeTypeId", "PHONE_HOME");
                    if (UtilValidate.isNotEmpty(PartyTelecomNumbers)) {
                        svcCtx.put("contactMechId", newContactMechId); 
                    }
                    
                    try {
                        svcRes = dispatcher.runSync("createUpdatePartyTelecomNumber", svcCtx);
                    } catch (GenericServiceException e) {
                        Debug.logError(e, module);
                        pos.showDialog("dialog/error/exception", e.getMessage());
                        return result;
                    }
                    if (ServiceUtil.isError(svcRes)) {
                        pos.showDialog("dialog/error/exception", ServiceUtil.getErrorMessage(svcRes));
                        return result;
                    }
                }
            }
        }
        return result;
    }
}
