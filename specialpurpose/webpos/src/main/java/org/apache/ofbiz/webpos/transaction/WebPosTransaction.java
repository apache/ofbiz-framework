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
package org.apache.ofbiz.webpos.transaction;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.util.EntityUtil;
import org.apache.ofbiz.order.shoppingcart.CheckOutHelper;
import org.apache.ofbiz.order.shoppingcart.ShoppingCart;
import org.apache.ofbiz.order.shoppingcart.ShoppingCartItem;
import org.apache.ofbiz.order.shoppingcart.ShoppingCart.CartPaymentInfo;
import org.apache.ofbiz.product.store.ProductStoreWorker;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;
import org.apache.ofbiz.webpos.session.WebPosSession;

public class WebPosTransaction {

    public static final String resource = "WebPosUiLabels";
    public static final String module = WebPosTransaction.class.getName();
    public static final int NO_PAYMENT = 0;
    public static final int INTERNAL_PAYMENT = 1;
    public static final int EXTERNAL_PAYMENT = 2;

    private CheckOutHelper ch = null;
    private GenericValue txLog = null;
    private String transactionId = null;
    private String orderId = null;
    private String partyId = null;
    private boolean isOpen = false;
    private int drawerIdx = 0;
    private GenericValue shipAddress = null;
    private WebPosSession webPosSession = null;

    public WebPosTransaction(WebPosSession session) {
        this.webPosSession = session;
        this.partyId = "_NA_";
        Delegator delegator = session.getDelegator();
        ShoppingCart cart = session.getCart();
        this.ch = new CheckOutHelper(session.getDispatcher(), delegator, cart);
        cart.setChannelType("POS_SALES_CHANNEL");
        cart.setFacilityId(session.getFacilityId());
        cart.setTerminalId(session.getId());

        if (session.getUserLogin() != null) {
            cart.addAdditionalPartyRole(session.getUserLogin().getString("partyId"), "SALES_REP");
        }

        // setup the TX log
        this.transactionId = delegator.getNextSeqId("PosTerminalLog");
        txLog = delegator.makeValue("PosTerminalLog");
        txLog.set("posTerminalLogId", this.transactionId);
        txLog.set("posTerminalId", session.getId());
        txLog.set("transactionId", transactionId);
        txLog.set("userLoginId", session.getUserLoginId());
        txLog.set("statusId", "POSTX_ACTIVE");
        txLog.set("logStartDateTime", UtilDateTime.nowTimestamp());
        try {
            txLog.create();
            cart.setTransactionId(transactionId);
        } catch (GenericEntityException e) {
            Debug.logError(e, "Unable to create TX log - not fatal", module);
        }

        Debug.logInfo("Created WebPosTransaction [" + this.transactionId + "]", module);
    }

    public String getUserLoginId() {
        return webPosSession.getUserLoginId();
    }

    public int getDrawerNumber() {
        return drawerIdx + 1;
    }

    public String getTransactionId() {
        return this.transactionId;
    }

    public String getTerminalId() {
        return webPosSession.getId();
    }

    public String getFacilityId() {
        return webPosSession.getFacilityId();
    }

    public String getTerminalLogId() {
        return txLog.getString("posTerminalLogId");
    }

    public boolean isOpen() {
        this.isOpen = false;
        GenericValue terminalState = this.getTerminalState();
        if (terminalState != null) {
            if ((terminalState.getDate("closedDate")) == null) {
                this.isOpen = true;
            }
        }
        return this.isOpen;
    }

    public GenericValue getTerminalState() {
        Delegator delegator = webPosSession.getDelegator();
        List<GenericValue> states = null;
        try {
            states = delegator.findList("PosTerminalState", EntityCondition.makeCondition(UtilMisc.toMap("posTerminalId", webPosSession.getId(), "startingTxId", getTransactionId())), null, null, null, false);
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }
        states = EntityUtil.filterByDate(states, UtilDateTime.nowTimestamp(), "openedDate", "closedDate", true);
        return EntityUtil.getFirst(states);
    }

    public void closeTx() {
        if (UtilValidate.isNotEmpty(txLog)) {
            txLog.set("statusId", "POSTX_CLOSED");
            txLog.set("itemCount", new Long(getCart().size()));
            txLog.set("logEndDateTime", UtilDateTime.nowTimestamp());
            try {
                txLog.store();
            } catch (GenericEntityException e) {
                Debug.logError(e, "Unable to store TX log - not fatal", module);
            }
            getCart().clear();
            Debug.logInfo("Transaction closed", module);
        }
    }

    public void paidInOut(String type) {
        if (UtilValidate.isNotEmpty(txLog)) {
            txLog.set("statusId", "POSTX_PAID_" + type);
            txLog.set("logEndDateTime", UtilDateTime.nowTimestamp());
            try {
                txLog.store();
            } catch (GenericEntityException e) {
                Debug.logError(e, "Unable to store TX log - not fatal", module);
            }
            webPosSession.setCurrentTransaction(null);
            Debug.logInfo("Paid "+ type, module);
        }
    }

    public void modifyPrice(int cartLineIdx, BigDecimal price) {
        ShoppingCartItem item = getCart().findCartItem(cartLineIdx);
        if (UtilValidate.isNotEmpty(item)) {
            Debug.logInfo("Modify item price " + item.getProductId() + "/" + price, module);
            item.setBasePrice(price);
        } else {
            Debug.logInfo("Item " + cartLineIdx + " not found", module);
        }
    }

    public void calcTax() {
        try {
            ch.calcAndAddTax(this.getStoreOrgAddress());
        } catch (GeneralException e) {
            Debug.logError(e, module);
        }
    }

    public BigDecimal processSale() throws GeneralException {
        Debug.logInfo("Process sale", module);
        BigDecimal grandTotal = this.getGrandTotal();
        BigDecimal paymentAmt = this.getPaymentTotal();
        if (grandTotal.compareTo(paymentAmt) > 0) {
            throw new GeneralException(UtilProperties.getMessage(resource, "WebPosNotEnoughFunds", webPosSession.getLocale()));
        }

        // attach the party ID to the cart
        getCart().setOrderPartyId(partyId);

        // validate payment methods
        Debug.logInfo("Validating payment methods", module);
        Map<String, ? extends Object> valRes = UtilGenerics.cast(ch.validatePaymentMethods());
        if (valRes != null && ServiceUtil.isError(valRes)) {
            throw new GeneralException(ServiceUtil.getErrorMessage(valRes));
        }

        // store the order
        Debug.logInfo("Store order", module);
        Map<String, ? extends Object> orderRes = UtilGenerics.cast(ch.createOrder(webPosSession.getUserLogin()));

        if (orderRes != null && ServiceUtil.isError(orderRes)) {
            throw new GeneralException(ServiceUtil.getErrorMessage(orderRes));
        } else if (orderRes != null) {
            this.orderId = (String) orderRes.get("orderId");
        }

        // process the payment(s)
        Debug.logInfo("Processing the payment(s)", module);
        Map<String, ? extends Object> payRes = null;
        try {
            payRes = UtilGenerics.cast(ch.processPayment(ProductStoreWorker.getProductStore(webPosSession.getProductStoreId(), webPosSession.getDelegator()), webPosSession.getUserLogin(), true));
        } catch (GeneralException e) {
            Debug.logError(e, module);
            throw e;
        }

        if (payRes != null && ServiceUtil.isError(payRes)) {
            throw new GeneralException(ServiceUtil.getErrorMessage(payRes));
        }

        // get the change due
        BigDecimal change = grandTotal.subtract(paymentAmt);

        // TODO notify the change due

        // threaded drawer/receipt printing
        // TODO open the drawer
        // TODO print the receipt

        // save the TX Log
        txLog.set("statusId", "POSTX_SOLD");
        txLog.set("orderId", orderId);
        txLog.set("itemCount", new Long(getCart().size()));
        txLog.set("logEndDateTime", UtilDateTime.nowTimestamp());
        try {
            txLog.store();
        } catch (GenericEntityException e) {
            Debug.logError(e, "Unable to store TX log - not fatal", module);
        }

        return change;
    }

    private synchronized GenericValue getStoreOrgAddress() {
        if (UtilValidate.isEmpty(this.shipAddress)) {
            // locate the store's physical address - use this for tax
            GenericValue facility = null;
            try {
                facility = webPosSession.getDelegator().findOne("Facility", UtilMisc.toMap("facilityId", webPosSession.getFacilityId()), false);
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
            }
            if (UtilValidate.isEmpty(facility)) {
                return null;
            }

            List<GenericValue> fcp = null;
            try {
                fcp = facility.getRelated("FacilityContactMechPurpose", UtilMisc.toMap("contactMechPurposeTypeId", "SHIP_ORIG_LOCATION"), null, false);
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
            }
            fcp = EntityUtil.filterByDate(fcp);
            GenericValue purp = EntityUtil.getFirst(fcp);
            if (UtilValidate.isNotEmpty(purp)) {
                try {
                    this.shipAddress = webPosSession.getDelegator().findOne("PostalAddress",
                            UtilMisc.toMap("contactMechId", purp.getString("contactMechId")), false);
                } catch (GenericEntityException e) {
                    Debug.logError(e, module);
                }
            }
        }
        return this.shipAddress;
    }

    public void clearPayments() {
        Debug.logInfo("all payments cleared from sale", module);
        getCart().clearPayments();
    }

    public void clearPayment(int index) {
        Debug.logInfo("removing payment " + index, module);
        getCart().clearPayment(index);
    }

    public void clearPayment(String id) {
        Debug.logInfo("removing payment " + id, module);
        getCart().clearPayment(id);
    }

    public CartPaymentInfo getPaymentInfo(int index) {
        return getCart().getPaymentInfo(index);
    }

    public String getPaymentMethodTypeId(int index) {
        return getCart().getPaymentInfo(index).paymentMethodTypeId;
    }

    public int checkPaymentMethodType(String paymentMethodTypeId) {
        Map<String, ? extends Object> fields = UtilMisc.toMap("paymentMethodTypeId", paymentMethodTypeId, "productStoreId", webPosSession.getProductStoreId());
        List<GenericValue> values = null;
        try {
            values = webPosSession.getDelegator().findList("ProductStorePaymentSetting", EntityCondition.makeCondition(fields), null, null, null, true);
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

    public static int getNoPaymentCode() {
        return NO_PAYMENT;
    }

    public static int getExternalPaymentCode() {
        return EXTERNAL_PAYMENT;
    }

    public static int getInternalPaymentCode() {
        return INTERNAL_PAYMENT;
    }

    public BigDecimal addPayment(String id, BigDecimal amount) {
        return this.addPayment(id, amount, null, null);
    }

    public BigDecimal addPayment(String id, BigDecimal amount, String refNum, String authCode) {
        Debug.logInfo("Added payment " + id + "/" + amount, module);
        if ("CASH".equals(id)) {
            // clear cash payments first; so there is only one
            getCart().clearPayment(id);
        }
        getCart().addPaymentAmount(id, amount, refNum, authCode, true, true, false);
        return this.getTotalDue();
    }

    public BigDecimal processAmount(BigDecimal amount) throws GeneralException {
        if (UtilValidate.isEmpty(amount)) {
            Debug.logInfo("Amount is empty; assumption is full amount : " + this.getTotalDue(), module);
            amount = this.getTotalDue();
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new GeneralException();
            }
        }
        return amount;
    }

    public synchronized void processNoPayment(String paymentMethodTypeId) {
        try {
            BigDecimal amount = processAmount(null);
            Debug.logInfo("Processing [" + paymentMethodTypeId + "] Amount : " + amount, module);

            // add the payment
            addPayment(paymentMethodTypeId, amount, null, null);
        } catch (GeneralException e) {
            // errors handled
        }
    }

    public synchronized void processExternalPayment(String paymentMethodTypeId, BigDecimal amount, String refNum) {
        if (refNum == null) {
            //TODO handle error message
            return;
        }

        try {
            amount = processAmount(amount);
            Debug.logInfo("Processing [" + paymentMethodTypeId + "] Amount : " + amount, module);

            // add the payment
            addPayment(paymentMethodTypeId, amount, refNum, null);
        } catch (GeneralException e) {
            // errors handled
        }
    }

    public String makeCreditCardVo(String cardNumber, String expDate, String firstName, String lastName) {
        LocalDispatcher dispatcher = webPosSession.getDispatcher();
        String expMonth = expDate.substring(0, 2);
        String expYear = expDate.substring(2);
        // two digit year check -- may want to re-think this
        if (expYear.length() == 2) {
            expYear = "20" + expYear;
        }

        Map<String, Object> svcCtx = new HashMap<String, Object>();
        svcCtx.put("userLogin", webPosSession.getUserLogin());
        svcCtx.put("partyId", partyId);
        svcCtx.put("cardNumber", cardNumber);
        svcCtx.put("firstNameOnCard", firstName == null ? "" : firstName);
        svcCtx.put("lastNameOnCard", lastName == null ? "" : lastName);
        svcCtx.put("expMonth", expMonth);
        svcCtx.put("expYear", expYear);
        svcCtx.put("cardType", UtilValidate.getCardType(cardNumber));

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

    public void setPaymentRefNum(int paymentIndex, String refNum, String authCode) {
        Debug.logInfo("setting payment index reference number " + paymentIndex + " / " + refNum + " / " + authCode, module);
        ShoppingCart.CartPaymentInfo inf = getCart().getPaymentInfo(paymentIndex);
        inf.refNum[0] = refNum;
        inf.refNum[1] = authCode;
    }

    /* CVV2 code should be entered when a card can't be swiped */
    public void setPaymentSecurityCode(String paymentId, String refNum, String securityCode) {
        Debug.logInfo("setting payment security code " + paymentId, module);
        int paymentIndex = getCart().getPaymentInfoIndex(paymentId, refNum);
        ShoppingCart.CartPaymentInfo inf = getCart().getPaymentInfo(paymentIndex);
        inf.securityCode = securityCode;
        inf.isSwiped = false;
    }

    /* Track2 data should be sent to processor when a card is swiped. */
    public void setPaymentTrack2(String paymentId, String refNum, String securityCode) {
        Debug.logInfo("setting payment security code " + paymentId, module);
        int paymentIndex = getCart().getPaymentInfoIndex(paymentId, refNum);
        ShoppingCart.CartPaymentInfo inf = getCart().getPaymentInfo(paymentIndex);
        inf.securityCode = securityCode;
        inf.isSwiped = true;
    }

    /* Postal code should be entered when a card can't be swiped */
    public void setPaymentPostalCode(String paymentId, String refNum, String postalCode) {
        Debug.logInfo("setting payment security code " + paymentId, module);
        int paymentIndex = getCart().getPaymentInfoIndex(paymentId, refNum);
        ShoppingCart.CartPaymentInfo inf = getCart().getPaymentInfo(paymentIndex);
        inf.postalCode = postalCode;
    }

    public BigDecimal getTaxTotal() {
        return getCart().getTotalSalesTax();
    }

    public BigDecimal getGrandTotal() {
        return getCart().getGrandTotal();
    }

    public int getNumberOfPayments() {
        return getCart().selectedPayments();
    }

    public BigDecimal getPaymentTotal() {
        return getCart().getPaymentTotal();
    }
    
    public BigDecimal getTotalQuantity() {
        return getCart().getTotalQuantity();
    }

    public BigDecimal getTotalDue() {
        BigDecimal grandTotal = this.getGrandTotal();
        BigDecimal paymentAmt = this.getPaymentTotal();
        return grandTotal.subtract(paymentAmt);
    }

    public String addProductPromoCode(String code) {
        String result = getCart().addProductPromoCode(code, webPosSession.getDispatcher());
        calcTax();
        return result;
    }

    public ShoppingCart getCart() {
        return webPosSession.getCart();
    }
}
