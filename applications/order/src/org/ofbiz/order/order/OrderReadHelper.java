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
package org.ofbiz.order.order;

import java.math.BigDecimal;
import org.ofbiz.base.util.UtilDateTime;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javolution.util.FastList;
import javolution.util.FastMap;

import org.apache.commons.collections.set.ListOrderedSet;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilFormatOut;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilNumber;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.common.DataModelConstants;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntity;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityConditionList;
import org.ofbiz.entity.condition.EntityExpr;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.product.product.ProductWorker;
import org.ofbiz.security.Security;

/**
 * Utility class for easily extracting important information from orders
 *
 * <p>NOTE: in the current scheme order adjustments are never included in tax or shipping,
 * but order item adjustments ARE included in tax and shipping calcs unless they are
 * tax or shipping adjustments or the includeInTax or includeInShipping are set to N.</p>
 */
public class OrderReadHelper {

    public static final String module = OrderReadHelper.class.getName();

    // scales and rounding modes for BigDecimal math
    public static final int scale = UtilNumber.getBigDecimalScale("order.decimals");
    public static final int rounding = UtilNumber.getBigDecimalRoundingMode("order.rounding");
    public static final int taxCalcScale = UtilNumber.getBigDecimalScale("salestax.calc.decimals");
    public static final int taxFinalScale = UtilNumber.getBigDecimalRoundingMode("salestax.final.decimals");
    public static final int taxRounding = UtilNumber.getBigDecimalRoundingMode("salestax.rounding");
    public static final BigDecimal ZERO = (new BigDecimal("0")).setScale(scale, rounding);    
    public static final BigDecimal percentage = (new BigDecimal("0.01")).setScale(scale, rounding);    

    protected GenericValue orderHeader = null;
    protected List orderItemAndShipGrp = null;
    protected List orderItems = null;
    protected List adjustments = null;
    protected List paymentPrefs = null;
    protected List orderStatuses = null;
    protected List orderItemPriceInfos = null;
    protected List orderItemShipGrpInvResList = null;
    protected List orderItemIssuances = null;
    protected List orderReturnItems = null;
    protected BigDecimal totalPrice = null;

    protected OrderReadHelper() {}

    public OrderReadHelper(GenericValue orderHeader, List adjustments, List orderItems) {
        this.orderHeader = orderHeader;
        this.adjustments = adjustments;
        this.orderItems = orderItems;
        if (this.orderHeader != null && !this.orderHeader.getEntityName().equals("OrderHeader")) {
            try {
                this.orderHeader = orderHeader.getDelegator().findByPrimaryKey("OrderHeader", UtilMisc.toMap("orderId",
                        orderHeader.getString("orderId")));
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
                this.orderHeader = null;
            }
        } else if (this.orderHeader == null && orderItems != null) {
            GenericValue firstItem = EntityUtil.getFirst(orderItems);
            try {
                this.orderHeader = firstItem.getRelatedOne("OrderHeader");
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
                this.orderHeader = null;
            }
        }
        if (this.orderHeader == null) {
            throw new IllegalArgumentException("Order header is not valid");
        }
    }

    public OrderReadHelper(GenericValue orderHeader) {
        this(orderHeader, null, null);
    }

    public OrderReadHelper(List adjustments, List orderItems) {
        this.adjustments = adjustments;
        this.orderItems = orderItems;
    }

    public OrderReadHelper(GenericDelegator delegator, String orderId) {
        try {
            this.orderHeader = delegator.findByPrimaryKey("OrderHeader", UtilMisc.toMap("orderId", orderId));
        } catch (GenericEntityException e) {
            throw new IllegalArgumentException("Invalid orderId");
        }
    }

    // ==========================================
    // ========== Order Header Methods ==========
    // ==========================================

    public String getOrderId() {
        return orderHeader.getString("orderId");
    }

    public String getWebSiteId() {
        return orderHeader.getString("webSiteId");
    }

    public String getProductStoreId() {
        return orderHeader.getString("productStoreId");
    }

    /**
     * Returns the ProductStore of this Order or null in case of Exception
     */ 
    public GenericValue getProductStore() {
        String productStoreId = orderHeader.getString("productStoreId");
        try {
            GenericDelegator delegator = orderHeader.getDelegator();
            GenericValue productStore = delegator.findByPrimaryKeyCache("ProductStore", UtilMisc.toMap("productStoreId", productStoreId));
            return productStore;
        } catch (GenericEntityException ex) {
            Debug.logError("Failed to get product store for order header [" + orderHeader + "] due to exception "+ ex.getMessage(), module);
            return null;
        }
    }
    
    public String getOrderTypeId() {
        return orderHeader.getString("orderTypeId");
    }

    public String getCurrency() {
        return orderHeader.getString("currencyUom");
    }

    public String getOrderName() {
        return orderHeader.getString("orderName");
    }
    
    public List getAdjustments() {
        if (adjustments == null) {
            try {
                adjustments = orderHeader.getRelated("OrderAdjustment");
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
            }
            if (adjustments == null)
                adjustments = new ArrayList();
        }
        return adjustments;
    }

    public List getPaymentPreferences() {
        if (paymentPrefs == null) {
            try {
                paymentPrefs = orderHeader.getRelated("OrderPaymentPreference", UtilMisc.toList("orderPaymentPreferenceId"));
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
            }
        }
        return paymentPrefs;
    }

    /**
     * Returns a Map of paymentMethodId -> amount charged (Double) based on PaymentGatewayResponse.  
     * @return
     */
    public Map getReceivedPaymentTotalsByPaymentMethod() {
        Map paymentMethodAmounts = FastMap.newInstance();
        List paymentPrefs = getPaymentPreferences();
        Iterator ppit = paymentPrefs.iterator();
        while (ppit.hasNext()) {
            GenericValue paymentPref = (GenericValue) ppit.next();
            List paymentGatewayResponses = new ArrayList();
            try {
                paymentGatewayResponses = paymentPref.getRelatedByAnd("PaymentGatewayResponse", UtilMisc.toMap("paymentServiceTypeEnumId","PRDS_PAY_CAPTURE"));
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
            }

            BigDecimal chargedToPaymentPref = ZERO;
            Iterator pgrit = paymentGatewayResponses.iterator();
            while(pgrit.hasNext()) {
                GenericValue paymentGatewayResponse = (GenericValue) pgrit.next();
                if (paymentGatewayResponse.get("amount") != null) {
                    chargedToPaymentPref = chargedToPaymentPref.add(paymentGatewayResponse.getBigDecimal("amount")).setScale(scale+1, rounding);
                }
            }

            // if chargedToPaymentPref > 0
            if (chargedToPaymentPref.compareTo(ZERO) == 1) {
                // key of the resulting map is paymentMethodId or paymentMethodTypeId if the paymentMethodId is not available
                String paymentMethodKey = paymentPref.getString("paymentMethodId") != null ? paymentPref.getString("paymentMethodId") : paymentPref.getString("paymentMethodTypeId");
                paymentMethodAmounts.put(paymentMethodKey, new Double(chargedToPaymentPref.setScale(scale, rounding).doubleValue()));
            }
        }
        return paymentMethodAmounts;
    }

    /**
     * Returns a Map of paymentMethodId -> amount refunded 
     * @return
     */
    public Map getReturnedTotalsByPaymentMethod() {
        Map paymentMethodAmounts = FastMap.newInstance();
        List paymentPrefs = getPaymentPreferences();
        Iterator ppit = paymentPrefs.iterator();
        while (ppit.hasNext()) {
            GenericValue paymentPref = (GenericValue) ppit.next();
            List returnItemResponses = new ArrayList();
            try {
                returnItemResponses = orderHeader.getDelegator().findByAnd("ReturnItemResponse", UtilMisc.toMap("orderPaymentPreferenceId", paymentPref.getString("orderPaymentPreferenceId")));
            } catch(GenericEntityException e) {
                Debug.logError(e, module);
            }
            BigDecimal refundedToPaymentPref = ZERO;
            Iterator ririt = returnItemResponses.iterator();
            while (ririt.hasNext()) {
                GenericValue returnItemResponse = (GenericValue) ririt.next();
                refundedToPaymentPref = refundedToPaymentPref.add(returnItemResponse.getBigDecimal("responseAmount")).setScale(scale+1, rounding);
            }

            // if refundedToPaymentPref > 0
            if (refundedToPaymentPref.compareTo(ZERO) == 1) {
                String paymentMethodId = paymentPref.getString("paymentMethodId") != null ? paymentPref.getString("paymentMethodId") : paymentPref.getString("paymentMethodTypeId");
                paymentMethodAmounts.put(paymentMethodId, new Double(refundedToPaymentPref.setScale(scale, rounding).doubleValue()));
            }
        }
        return paymentMethodAmounts;
    }

    public List getOrderPayments() {
        return getOrderPayments(null);
    }

    public List getOrderPayments(GenericValue orderPaymentPreference) {
        List orderPayments = new ArrayList();
        List prefs = null;

        if (orderPaymentPreference == null) {
            prefs = getPaymentPreferences();
        } else {
            prefs = UtilMisc.toList(orderPaymentPreference);
        }
        if (prefs != null) {
            Iterator i = prefs.iterator();
            while (i.hasNext()) {
                GenericValue payPref = (GenericValue) i.next();
                try {
                    orderPayments.addAll(payPref.getRelated("Payment"));
                } catch (GenericEntityException e) {
                    Debug.logError(e, module);
                    return null;
                }
            }
        }
        return orderPayments;
    }

    public List getOrderStatuses() {
        if (orderStatuses == null) {
            try {
                orderStatuses = orderHeader.getRelated("OrderStatus");
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
            }
        }
        return orderStatuses;
    }

    public List getOrderTerms() {
        try {
           return orderHeader.getRelated("OrderTerm");
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return null;
        }
    }

    /**
     * @return Long number of days from termDays of first FIN_PAYMENT_TERM
     */
    public Long getOrderTermNetDays() {
        List orderTerms = EntityUtil.filterByAnd(getOrderTerms(), UtilMisc.toMap("termTypeId", "FIN_PAYMENT_TERM"));
        if ((orderTerms == null) || (orderTerms.size() == 0)) {
            return null;
        } else if (orderTerms.size() > 1) {
            Debug.logWarning("Found " + orderTerms.size() + " FIN_PAYMENT_TERM order terms for orderId [" + getOrderId() + "], using the first one ", module);
        }
        return ((GenericValue) orderTerms.get(0)).getLong("termDays");
    }

    /** @deprecated */
    public String getShippingMethod() {
        throw new IllegalArgumentException("You must call the getShippingMethod method with the shipGroupdSeqId parameter, this is no londer supported since a single OrderShipmentPreference is no longer used.");
    }

    public String getShippingMethod(String shipGroupSeqId) {
        try {
            GenericValue shipGroup = orderHeader.getDelegator().findByPrimaryKey("OrderItemShipGroup",
                    UtilMisc.toMap("orderId", orderHeader.getString("orderId"), "shipGroupSeqId", shipGroupSeqId));

            if (shipGroup != null) {
                GenericValue carrierShipmentMethod = shipGroup.getRelatedOne("CarrierShipmentMethod");

                if (carrierShipmentMethod != null) {
                    GenericValue shipmentMethodType = carrierShipmentMethod.getRelatedOne("ShipmentMethodType");

                    if (shipmentMethodType != null) {
                        return UtilFormatOut.checkNull(shipGroup.getString("carrierPartyId")) + " " +
                                UtilFormatOut.checkNull(shipmentMethodType.getString("description"));
                    }
                }
                return UtilFormatOut.checkNull(shipGroup.getString("carrierPartyId"));
            }
        } catch (GenericEntityException e) {
            Debug.logWarning(e, module);
        }
        return "";
    }

    /** @deprecated */
    public String getShippingMethodCode() {
        throw new IllegalArgumentException("You must call the getShippingMethodCode method with the shipGroupdSeqId parameter, this is no londer supported since a single OrderShipmentPreference is no longer used.");
    }

    public String getShippingMethodCode(String shipGroupSeqId) {
        try {
            GenericValue shipGroup = orderHeader.getDelegator().findByPrimaryKey("OrderItemShipGroup",
                    UtilMisc.toMap("orderId", orderHeader.getString("orderId"), "shipGroupSeqId", shipGroupSeqId));

            if (shipGroup != null) {
                GenericValue carrierShipmentMethod = shipGroup.getRelatedOne("CarrierShipmentMethod");

                if (carrierShipmentMethod != null) {
                    GenericValue shipmentMethodType = carrierShipmentMethod.getRelatedOne("ShipmentMethodType");

                    if (shipmentMethodType != null) {
                        return UtilFormatOut.checkNull(shipmentMethodType.getString("shipmentMethodTypeId")) + "@" + UtilFormatOut.checkNull(shipGroup.getString("carrierPartyId"));
                    }
                }
                return UtilFormatOut.checkNull(shipGroup.getString("carrierPartyId"));
            }
        } catch (GenericEntityException e) {
            Debug.logWarning(e, module);
        }
        return "";
    }

    public boolean hasShippingAddress() {
        if (UtilValidate.isNotEmpty(this.getShippingLocations())) {
            return true;
        }
        return false;
    }

    public GenericValue getOrderItemShipGroup(String shipGroupSeqId) {
        try {
            return orderHeader.getDelegator().findByPrimaryKey("OrderItemShipGroup",
                    UtilMisc.toMap("orderId", orderHeader.getString("orderId"), "shipGroupSeqId", shipGroupSeqId));
        } catch (GenericEntityException e) {
            Debug.logWarning(e, module);
        }
        return null;
    }

    public List getOrderItemShipGroups() {
        try {
            return orderHeader.getRelated("OrderItemShipGroup", UtilMisc.toList("shipGroupSeqId"));
        } catch (GenericEntityException e) {
            Debug.logWarning(e, module);
        }
        return null;
    }

    public List getShippingLocations() {
        List shippingLocations = FastList.newInstance();
        List shippingCms = this.getOrderContactMechs("SHIPPING_LOCATION");
        if (shippingCms != null) {
            Iterator i = shippingCms.iterator();
            while (i.hasNext()) {
                GenericValue ocm = (GenericValue) i.next();
                if (ocm != null) {
                    try {
                        GenericValue addr = ocm.getDelegator().findByPrimaryKey("PostalAddress",
                                UtilMisc.toMap("contactMechId", ocm.getString("contactMechId")));
                        if (addr != null) {
                            shippingLocations.add(addr);
                        }
                    } catch (GenericEntityException e) {
                        Debug.logWarning(e, module);
                    }
                }
            }
        }
        return shippingLocations;
    }

    public GenericValue getShippingAddress(String shipGroupSeqId) {
        try {
            GenericValue shipGroup = orderHeader.getDelegator().findByPrimaryKey("OrderItemShipGroup",
                    UtilMisc.toMap("orderId", orderHeader.getString("orderId"), "shipGroupSeqId", shipGroupSeqId));

            if (shipGroup != null) {
                return shipGroup.getRelatedOne("PostalAddress");

            }
        } catch (GenericEntityException e) {
            Debug.logWarning(e, module);
        }
        return null;
    }

    /** @deprecated */
    public GenericValue getShippingAddress() {
        try {
            GenericValue orderContactMech = EntityUtil.getFirst(orderHeader.getRelatedByAnd("OrderContactMech", UtilMisc.toMap(
                            "contactMechPurposeTypeId", "SHIPPING_LOCATION")));

            if (orderContactMech != null) {
                GenericValue contactMech = orderContactMech.getRelatedOne("ContactMech");

                if (contactMech != null) {
                    return contactMech.getRelatedOne("PostalAddress");
                }
            }
        } catch (GenericEntityException e) {
            Debug.logWarning(e, module);
        }
        return null;
    }

    public List getBillingLocations() {
        List billingLocations = FastList.newInstance();
        List billingCms = this.getOrderContactMechs("BILLING_LOCATION");
        if (billingCms != null) {
            Iterator i = billingCms.iterator();
            while (i.hasNext()) {
                GenericValue ocm = (GenericValue) i.next();
                if (ocm != null) {
                    try {
                        GenericValue addr = ocm.getDelegator().findByPrimaryKey("PostalAddress",
                                UtilMisc.toMap("contactMechId", ocm.getString("contactMechId")));
                        if (addr != null) {
                            billingLocations.add(addr);
                        }
                    } catch (GenericEntityException e) {
                        Debug.logWarning(e, module);
                    }
                }
            }
        }
        return billingLocations;
    }

    /** @deprecated */
    public GenericValue getBillingAddress() {
        GenericValue billingAddress = null;
        try {
            GenericValue orderContactMech = EntityUtil.getFirst(orderHeader.getRelatedByAnd("OrderContactMech", UtilMisc.toMap("contactMechPurposeTypeId", "BILLING_LOCATION")));

            if (orderContactMech != null) {
                GenericValue contactMech = orderContactMech.getRelatedOne("ContactMech");

                if (contactMech != null) {
                    billingAddress = contactMech.getRelatedOne("PostalAddress");
                }
            }
        } catch (GenericEntityException e) {
            Debug.logWarning(e, module);
        }

        if (billingAddress == null) {
            // get the address from the billing account
            GenericValue billingAccount = getBillingAccount();
            if (billingAccount != null) {
                try {
                    billingAddress = billingAccount.getRelatedOne("PostalAddress");
                } catch (GenericEntityException e) {
                    Debug.logWarning(e, module);
                }
            } else {
                // get the address from the first payment method
                GenericValue paymentPreference = EntityUtil.getFirst(getPaymentPreferences());
                if (paymentPreference != null) {
                    try {
                        GenericValue paymentMethod = paymentPreference.getRelatedOne("PaymentMethod");
                        if (paymentMethod != null) {
                            GenericValue creditCard = paymentMethod.getRelatedOne("CreditCard");
                            if (creditCard != null) {
                                billingAddress = creditCard.getRelatedOne("PostalAddress");
                            } else {
                                GenericValue eftAccount = paymentMethod.getRelatedOne("EftAccount");
                                if (eftAccount != null) {
                                    billingAddress = eftAccount.getRelatedOne("PostalAddress");
                                }
                            }
                        }
                    } catch (GenericEntityException e) {
                        Debug.logWarning(e, module);
                    }
                }
            }
        }
        return billingAddress;
    }

    public List getOrderContactMechs(String purposeTypeId) {
        try {
            return orderHeader.getRelatedByAnd("OrderContactMech",
                    UtilMisc.toMap("contactMechPurposeTypeId", purposeTypeId));
        } catch (GenericEntityException e) {
            Debug.logWarning(e, module);
        }
        return null;
    }

    public Timestamp getEarliestShipByDate() {
        try {
            List groups = orderHeader.getRelated("OrderItemShipGroup", UtilMisc.toList("shipByDate"));
            if (groups.size() > 0) {
                GenericValue group = (GenericValue) groups.get(0);
                return group.getTimestamp("shipByDate");
            }
        } catch (GenericEntityException e) {
            Debug.logWarning(e, module);
        }
        return null;
    }

    public Timestamp getLatestShipAfterDate() {
        try {
            List groups = orderHeader.getRelated("OrderItemShipGroup", UtilMisc.toList("shipAfterDate DESC"));
            if (groups.size() > 0) {
                GenericValue group = (GenericValue) groups.get(0);
                return group.getTimestamp("shipAfterDate");
            }
        } catch (GenericEntityException e) {
            Debug.logWarning(e, module);
        }
        return null;
    }

    public String getCurrentStatusString() {
        GenericValue statusItem = null;
        try {
            statusItem = orderHeader.getRelatedOneCache("StatusItem");
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }
        if (statusItem != null) {
            return statusItem.getString("description");
        } else {
            return orderHeader.getString("statusId");
        }
    }

    public String getStatusString() {
        List orderStatusList = this.getOrderHeaderStatuses();

        if (orderStatusList == null || orderStatusList.size() == 0) return "";

        Iterator orderStatusIter = orderStatusList.iterator();
        StringBuffer orderStatusString = new StringBuffer(50);

        try {
            boolean isCurrent = true;
            while (orderStatusIter.hasNext()) {
                GenericValue orderStatus = (GenericValue) orderStatusIter.next();
                GenericValue statusItem = orderStatus.getRelatedOneCache("StatusItem");

                if (statusItem != null) {
                    orderStatusString.append(statusItem.getString("description"));
                } else {
                    orderStatusString.append(orderStatus.getString("statusId"));
                }

                if (isCurrent && orderStatusIter.hasNext()) {
                    orderStatusString.append(" (");
                    isCurrent = false;
                } else {
                    if (orderStatusIter.hasNext()) {
                        orderStatusString.append("/");
                    } else {
                        if (!isCurrent) {
                            orderStatusString.append(")");
                        }
                    }
                }
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, "Error getting Order Status information: " + e.toString(), module);
        }

        return orderStatusString.toString();
    }

    public GenericValue getBillingAccount() {
        GenericValue billingAccount = null;
        try {
            billingAccount = orderHeader.getRelatedOne("BillingAccount");
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }
        return billingAccount;
    }

    /**
     * Returns the OrderPaymentPreference.maxAmount for the billing account associated with the order, or 0 if there is no
     * billing account or no max amount set
     */
    public double getBillingAccountMaxAmount() {
        if (getBillingAccount() == null) {
            return 0.0;
        } else {
            List paymentPreferences = getPaymentPreferences();
            GenericValue billingAccountPaymentPreference = EntityUtil.getFirst(EntityUtil.filterByAnd(paymentPreferences, UtilMisc.toMap("paymentMethodTypeId", "EXT_BILLACT")));
            if ((billingAccountPaymentPreference != null) && (billingAccountPaymentPreference.getDouble("maxAmount") != null)) {
                return billingAccountPaymentPreference.getDouble("maxAmount").doubleValue();
            } else {
                return 0.0;
            }
        }
    }

    /**
     * Returns party from OrderRole of BILL_TO_CUSTOMER
     */
    public GenericValue getBillToParty() {
        return this.getPartyFromRole("BILL_TO_CUSTOMER");
    }

    /**
     * Returns party from OrderRole of BILL_FROM_VENDOR
     */
    public GenericValue getBillFromParty() {
        return this.getPartyFromRole("BILL_FROM_VENDOR");
    }

    /**
     * Returns party from OrderRole of SHIP_TO_CUSTOMER
     */
    public GenericValue getShipToParty() {
        return this.getPartyFromRole("SHIP_TO_CUSTOMER");
    }

    /**
     * Returns party from OrderRole of PLACING_CUSTOMER
     */
    public GenericValue getPlacingParty() {
        return this.getPartyFromRole("PLACING_CUSTOMER");
    }

    /**
     * Returns party from OrderRole of END_USER_CUSTOMER
     */
    public GenericValue getEndUserParty() {
        return this.getPartyFromRole("END_USER_CUSTOMER");
    }

    /**
     * Returns party from OrderRole of SUPPLIER_AGENT
     */
    public GenericValue getSupplierAgent() {
        return this.getPartyFromRole("SUPPLIER_AGENT");
    }

    public GenericValue getPartyFromRole(String roleTypeId) {
        GenericDelegator delegator = orderHeader.getDelegator();
        GenericValue partyObject = null;
        try {
            GenericValue orderRole = EntityUtil.getFirst(orderHeader.getRelatedByAnd("OrderRole", UtilMisc.toMap("roleTypeId", roleTypeId)));

            if (orderRole != null) {
                partyObject = delegator.findByPrimaryKey("Person", UtilMisc.toMap("partyId", orderRole.getString("partyId")));

                if (partyObject == null) {
                    partyObject = delegator.findByPrimaryKey("PartyGroup", UtilMisc.toMap("partyId", orderRole.getString("partyId")));
                }
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }
        return partyObject;
    }

    public String getDistributorId() {
        try {
            GenericEntity distributorRole = EntityUtil.getFirst(orderHeader.getRelatedByAnd("OrderRole", UtilMisc.toMap("roleTypeId", "DISTRIBUTOR")));

            return distributorRole == null ? null : distributorRole.getString("partyId");
        } catch (GenericEntityException e) {
            Debug.logWarning(e, module);
        }
        return null;
    }

    public String getAffiliateId() {
        try {
            GenericEntity distributorRole = EntityUtil.getFirst(orderHeader.getRelatedByAnd("OrderRole", UtilMisc.toMap("roleTypeId", "AFFILIATE")));

            return distributorRole == null ? null : distributorRole.getString("partyId");
        } catch (GenericEntityException e) {
            Debug.logWarning(e, module);
        }
        return null;
    }

    public BigDecimal getShippingTotalBd() {
        return OrderReadHelper.calcOrderAdjustmentsBd(getOrderHeaderAdjustments(), getOrderItemsSubTotalBd(), false, false, true);
    }

    /** @deprecated Use getShippingTotalBd() instead */
    public double getShippingTotal() {
        return getShippingTotalBd().doubleValue();
    }

    public BigDecimal getHeaderTaxTotalBd() {
        return OrderReadHelper.calcOrderAdjustmentsBd(getOrderHeaderAdjustments(), getOrderItemsSubTotalBd(), false, true, false);
    }

    /** @deprecated Use getHeaderTaxTotalBd() instead */
    public double getHeaderTaxTotal() {
        return getHeaderTaxTotalBd().doubleValue();
    }

    public BigDecimal getTaxTotalBd() {
        return OrderReadHelper.calcOrderAdjustmentsBd(getAdjustments(), getOrderItemsSubTotalBd(), false, true, false);
    }

    /** @deprecated Use getTaxTotalBd() instead */
    public double getTaxTotal() {
        return getTaxTotalBd().doubleValue();
    }

    public Set getItemFeatureSet(GenericValue item) {
        Set featureSet = new ListOrderedSet();
        List featureAppls = null;
        if (item.get("productId") != null) {
            try {
                featureAppls = item.getDelegator().findByAndCache("ProductFeatureAppl", UtilMisc.toMap("productId", item.getString("productId")));
                List filterExprs = UtilMisc.toList(new EntityExpr("productFeatureApplTypeId", EntityOperator.EQUALS, "STANDARD_FEATURE"));
                filterExprs.add(new EntityExpr("productFeatureApplTypeId", EntityOperator.EQUALS, "REQUIRED_FEATURE"));
                featureAppls = EntityUtil.filterByOr(featureAppls, filterExprs);
            } catch (GenericEntityException e) {
                Debug.logError(e, "Unable to get ProductFeatureAppl for item : " + item, module);
            }
            if (featureAppls != null) {
                Iterator fai = featureAppls.iterator();
                while (fai.hasNext()) {
                    GenericValue appl = (GenericValue) fai.next();
                    featureSet.add(appl.getString("productFeatureId"));
                }
            }
        }

        // get the ADDITIONAL_FEATURE adjustments
        List additionalFeatures = null;
        try {
            additionalFeatures = item.getRelatedByAnd("OrderAdjustment", UtilMisc.toMap("orderAdjustmentTypeId", "ADDITIONAL_FEATURE"));
        } catch (GenericEntityException e) {
            Debug.logError(e, "Unable to get OrderAdjustment from item : " + item, module);
        }
        if (additionalFeatures != null) {
            Iterator afi = additionalFeatures.iterator();
            while (afi.hasNext()) {
                GenericValue adj = (GenericValue) afi.next();
                String featureId = adj.getString("productFeatureId");
                if (featureId != null) {
                    featureSet.add(featureId);
                }
            }
        }

        return featureSet;
    }

    public Map getFeatureIdQtyMap(String shipGroupSeqId) {
        Map featureMap = FastMap.newInstance();
        List validItems = getValidOrderItems(shipGroupSeqId);
        if (validItems != null) {
            Iterator i = validItems.iterator();
            while (i.hasNext()) {
                GenericValue item = (GenericValue) i.next();
                List featureAppls = null;
                if (item.get("productId") != null) {
                    try {
                        featureAppls = item.getDelegator().findByAndCache("ProductFeatureAppl", UtilMisc.toMap("productId", item.getString("productId")));
                        List filterExprs = UtilMisc.toList(new EntityExpr("productFeatureApplTypeId", EntityOperator.EQUALS, "STANDARD_FEATURE"));
                        filterExprs.add(new EntityExpr("productFeatureApplTypeId", EntityOperator.EQUALS, "REQUIRED_FEATURE"));
                        featureAppls = EntityUtil.filterByOr(featureAppls, filterExprs);
                    } catch (GenericEntityException e) {
                        Debug.logError(e, "Unable to get ProductFeatureAppl for item : " + item, module);
                    }
                    if (featureAppls != null) {
                        Iterator fai = featureAppls.iterator();
                        while (fai.hasNext()) {
                            GenericValue appl = (GenericValue) fai.next();
                            Double lastQuantity = (Double) featureMap.get(appl.getString("productFeatureId"));
                            if (lastQuantity == null) {
                                lastQuantity = new Double(0);
                            }
                            Double newQuantity = new Double(lastQuantity.doubleValue() + getOrderItemQuantity(item).doubleValue());
                            featureMap.put(appl.getString("productFeatureId"), newQuantity);
                        }
                    }
                }

                // get the ADDITIONAL_FEATURE adjustments
                List additionalFeatures = null;
                try {
                    additionalFeatures = item.getRelatedByAnd("OrderAdjustment", UtilMisc.toMap("orderAdjustmentTypeId", "ADDITIONAL_FEATURE"));
                } catch (GenericEntityException e) {
                    Debug.logError(e, "Unable to get OrderAdjustment from item : " + item, module);
                }
                if (additionalFeatures != null) {
                    Iterator afi = additionalFeatures.iterator();
                    while (afi.hasNext()) {
                        GenericValue adj = (GenericValue) afi.next();
                        String featureId = adj.getString("productFeatureId");
                        if (featureId != null) {
                            Double lastQuantity = (Double) featureMap.get(featureId);
                            if (lastQuantity == null) {
                                lastQuantity = new Double(0);
                            }
                            Double newQuantity = new Double(lastQuantity.doubleValue() + getOrderItemQuantity(item).doubleValue());
                            featureMap.put(featureId, newQuantity);
                        }
                    }
                }
            }
        }

        return featureMap;
    }

    public boolean shippingApplies() {
        boolean shippingApplies = false;
        List validItems = this.getValidOrderItems();
        if (validItems != null) {
            Iterator i = validItems.iterator();
            while (i.hasNext()) {
                GenericValue item = (GenericValue) i.next();
                GenericValue product = null;
                try {
                    product = item.getRelatedOne("Product");
                } catch (GenericEntityException e) {
                    Debug.logError(e, "Problem getting Product from OrderItem; returning 0", module);
                }
                if (product != null) {
                    if (ProductWorker.shippingApplies(product)) {
                        shippingApplies = true;
                        break;
                    }
                }
            }
        }
        return shippingApplies;
    }

    public boolean taxApplies() {
        boolean taxApplies = false;
        List validItems = this.getValidOrderItems();
        if (validItems != null) {
            Iterator i = validItems.iterator();
            while (i.hasNext()) {
                GenericValue item = (GenericValue) i.next();
                GenericValue product = null;
                try {
                    product = item.getRelatedOne("Product");
                } catch (GenericEntityException e) {
                    Debug.logError(e, "Problem getting Product from OrderItem; returning 0", module);
                }
                if (product != null) {
                    if (ProductWorker.taxApplies(product)) {
                        taxApplies = true;
                        break;
                    }
                }
            }
        }
        return taxApplies;
    }

    public BigDecimal getShippableTotalBd(String shipGroupSeqId) {
        BigDecimal shippableTotal = ZERO;
        List validItems = getValidOrderItems(shipGroupSeqId);
        if (validItems != null) {
            Iterator i = validItems.iterator();
            while (i.hasNext()) {
                GenericValue item = (GenericValue) i.next();
                GenericValue product = null;
                try {
                    product = item.getRelatedOne("Product");
                } catch (GenericEntityException e) {
                    Debug.logError(e, "Problem getting Product from OrderItem; returning 0", module);
                    return ZERO;
                }
                if (product != null) {
                    if (ProductWorker.shippingApplies(product)) {
                        shippableTotal = shippableTotal.add(OrderReadHelper.getOrderItemSubTotalBd(item, getAdjustments(), false, true)).setScale(scale, rounding);
                    }
                }
            }
        }
        return shippableTotal.setScale(scale, rounding);
    }

    /** @deprecated Use getShippableTotalBd() instead */
    public double getShippableTotal(String shipGroupSeqId) {
        return getShippableTotalBd(shipGroupSeqId).doubleValue();
    }

    public BigDecimal getShippableQuantityBd(String shipGroupSeqId) {
        BigDecimal shippableQuantity = ZERO;
        List validItems = getValidOrderItems(shipGroupSeqId);
        if (validItems != null) {
            Iterator i = validItems.iterator();
            while (i.hasNext()) {
                GenericValue item = (GenericValue) i.next();
                GenericValue product = null;
                try {
                    product = item.getRelatedOne("Product");
                } catch (GenericEntityException e) {
                    Debug.logError(e, "Problem getting Product from OrderItem; returning 0", module);
                    return ZERO;
                }
                if (product != null) {
                    if (ProductWorker.shippingApplies(product)) {
                        shippableQuantity = shippableQuantity.add(getOrderItemQuantityBd(item)).setScale(scale, rounding);
                    }
                }
            }
        }
        return shippableQuantity.setScale(scale, rounding);
    }

    /** @deprecated Use getShippableQuantityBd() instead */
    public double getShippableQuantity(String shipGroupSeqId) {
        return getShippableQuantityBd(shipGroupSeqId).doubleValue();
    }

    public BigDecimal getShippableWeightBd(String shipGroupSeqId) {
        BigDecimal shippableWeight = ZERO;
        List validItems = getValidOrderItems(shipGroupSeqId);
        if (validItems != null) {
            Iterator i = validItems.iterator();
            while (i.hasNext()) {
                GenericValue item = (GenericValue) i.next();
                shippableWeight = shippableWeight.add(this.getItemWeightBd(item).multiply( getOrderItemQuantityBd(item))).setScale(scale, rounding);
            }
        }

        return shippableWeight.setScale(scale, rounding);
    }

    /** @deprecated Use getShippableWeightBd() instead */
    public double getShippableWeight(String shipGroupSeqId) {
        return getShippableWeightBd(shipGroupSeqId).doubleValue();
    }

    public BigDecimal getItemWeightBd(GenericValue item) {
        GenericDelegator delegator = orderHeader.getDelegator();
        BigDecimal itemWeight = ZERO;

        GenericValue product = null;
        try {
            product = item.getRelatedOne("Product");
        } catch (GenericEntityException e) {
            Debug.logError(e, "Problem getting Product from OrderItem; returning 0", module);
            return new BigDecimal ("0.00");
        }
        if (product != null) {
            if (ProductWorker.shippingApplies(product)) {
                BigDecimal weight = product.getBigDecimal("weight");
                String isVariant = product.getString("isVariant");
                if (weight == null && "Y".equals(isVariant)) {
                    // get the virtual product and check its weight
                    try {
                        String virtualId = ProductWorker.getVariantVirtualId(product);
                        if (UtilValidate.isNotEmpty(virtualId)) {
                            GenericValue virtual = delegator.findByPrimaryKeyCache("Product", UtilMisc.toMap("productId", virtualId));
                            if (virtual != null) {
                                weight = virtual.getBigDecimal("weight");
                            }
                        }
                    } catch (GenericEntityException e) {
                        Debug.logError(e, "Problem getting virtual product");
                    }
                }

                if (weight != null) {
                    itemWeight = weight;
                }
            }
        }

        return itemWeight;
    }

    /** @deprecated Use getItemWeightBd() instead */
    public double getItemWeight(GenericValue item) {
        return getItemWeightBd(item).doubleValue();
    }

    public List getShippableSizes() {
        List shippableSizes = FastList.newInstance();

        List validItems = getValidOrderItems();
        if (validItems != null) {
            Iterator i = validItems.iterator();
            while (i.hasNext()) {
                GenericValue item = (GenericValue) i.next();
                shippableSizes.add(new Double(this.getItemSize(item)));
            }
        }
        return shippableSizes;
    }
    
    /**
     * Get the total payment preference amount by payment type.  Specify null to get amount
     * for all preference types.  TODO: filter by status as well?
     */
    public BigDecimal getOrderPaymentPreferenceTotalByType(String paymentMethodTypeId) {
        BigDecimal total = ZERO;
        for (Iterator iter = getPaymentPreferences().iterator(); iter.hasNext(); ) {
            GenericValue preference = (GenericValue) iter.next();
            if (preference.get("maxAmount") == null) continue;
            if (paymentMethodTypeId == null || paymentMethodTypeId.equals(preference.get("paymentMethodTypeId"))) {
                total = total.add(preference.getBigDecimal("maxAmount")).setScale(scale, rounding);
            }
        }
        return total;
    }

    public BigDecimal getCreditCardPaymentPreferenceTotal() {
        return getOrderPaymentPreferenceTotalByType("CREDIT_CARD");
    }

    public BigDecimal getBillingAccountPaymentPreferenceTotal() {
        return getOrderPaymentPreferenceTotalByType("EXT_BILLACT");
    }

    public BigDecimal getGiftCardPaymentPreferenceTotal() {
        return getOrderPaymentPreferenceTotalByType("GIFT_CARD");
    }

    /**
     * Get the total payment received amount by payment type.  Specify null to get amount
     * over all types. This method works by going through all the PaymentAndApplications
     * for all order Invoices that have status PMNT_RECEIVED.  
     */
    public BigDecimal getOrderPaymentReceivedTotalByType(String paymentMethodTypeId) {
        BigDecimal total = ZERO;

        try {
            // get a set of invoice IDs that belong to the order
            List orderItemBillings = orderHeader.getRelatedCache("OrderItemBilling");
            Set invoiceIds = new HashSet();
            for (Iterator iter = orderItemBillings.iterator(); iter.hasNext(); ) {
                GenericValue orderItemBilling = (GenericValue) iter.next();
                invoiceIds.add(orderItemBilling.get("invoiceId"));
            }

            // get the payments of the desired type for these invoices TODO: in models where invoices can have many orders, this needs to be refined
            List conditions = UtilMisc.toList(
                    new EntityExpr("statusId", EntityOperator.EQUALS, "PMNT_RECEIVED"),
                    new EntityExpr("invoiceId", EntityOperator.IN, invoiceIds)
                    );
            if (paymentMethodTypeId != null) {
                conditions.add(new EntityExpr("paymentMethodTypeId", EntityOperator.EQUALS, paymentMethodTypeId));
            }
            EntityConditionList ecl = new EntityConditionList(conditions, EntityOperator.AND);
            List payments = orderHeader.getDelegator().findByConditionCache("PaymentAndApplication", ecl, null, null);

            for (Iterator iter = payments.iterator(); iter.hasNext(); ) {
                GenericValue payment = (GenericValue) iter.next();
                if (payment.get("amountApplied") == null) continue;
                total = total.add(payment.getBigDecimal("amountApplied")).setScale(scale, rounding);
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, e.getMessage(), module);
        }
        return total;
    }

    // TODO: Might want to use BigDecimal here if precision matters
    public double getItemSize(GenericValue item) {
        GenericDelegator delegator = orderHeader.getDelegator();
        double size = 0;

        GenericValue product = null;
        try {
            product = item.getRelatedOne("Product");
        } catch (GenericEntityException e) {
            Debug.logError(e, "Problem getting Product from OrderItem", module);
            return 0;
        }
        if (product != null) {
            if (ProductWorker.shippingApplies(product)) {
                Double height = product.getDouble("shippingHeight");
                Double width = product.getDouble("shippingWidth");
                Double depth = product.getDouble("shippingDepth");
                String isVariant = product.getString("isVariant");
                if ((height == null || width == null || depth == null) && "Y".equals(isVariant)) {
                    // get the virtual product and check its values
                    try {
                        String virtualId = ProductWorker.getVariantVirtualId(product);
                        if (UtilValidate.isNotEmpty(virtualId)) {
                            GenericValue virtual = delegator.findByPrimaryKeyCache("Product", UtilMisc.toMap("productId", virtualId));
                            if (virtual != null) {
                                if (height == null) height = virtual.getDouble("shippingHeight");
                                if (width == null) width = virtual.getDouble("shippingWidth");
                                if (depth == null) depth = virtual.getDouble("shippingDepth");
                            }
                        }
                    } catch (GenericEntityException e) {
                        Debug.logError(e, "Problem getting virtual product");
                    }
                }

                if (height == null) height = new Double(0);
                if (width == null) width = new Double(0);
                if (depth == null) depth = new Double(0);

                // determine girth (longest field is length)
                double[] sizeInfo = { height.doubleValue(), width.doubleValue(), depth.doubleValue() };
                Arrays.sort(sizeInfo);

                size = (sizeInfo[0] * 2) + (sizeInfo[1] * 2) + sizeInfo[2];
            }
        }

        return size;
    }

    public long getItemPiecesIncluded(GenericValue item) {
        GenericDelegator delegator = orderHeader.getDelegator();
        long piecesIncluded = 1;

        GenericValue product = null;
        try {
            product = item.getRelatedOne("Product");
        } catch (GenericEntityException e) {
            Debug.logError(e, "Problem getting Product from OrderItem; returning 1", module);
            return 1;
        }
        if (product != null) {
            if (ProductWorker.shippingApplies(product)) {
                Long pieces = product.getLong("piecesIncluded");
                String isVariant = product.getString("isVariant");
                if (pieces == null && isVariant != null && "Y".equals(isVariant)) {
                    // get the virtual product and check its weight
                    GenericValue virtual = null;
                    try {
                        List virtuals = delegator.findByAnd("ProductAssoc", UtilMisc.toMap("productIdTo", product.getString("productId"), "productAssocTypeId", "PRODUCT_VARIANT"), UtilMisc.toList("-fromDate"));
                        if (virtuals != null) {
                            virtuals = EntityUtil.filterByDate(virtuals);
                        }
                        virtual = EntityUtil.getFirst(virtuals);
                    } catch (GenericEntityException e) {
                        Debug.logError(e, "Problem getting virtual product");
                    }
                    if (virtual != null) {
                        try {
                            GenericValue virtualProduct = virtual.getRelatedOne("MainProduct");
                            pieces = virtualProduct.getLong("piecesIncluded");
                        } catch (GenericEntityException e) {
                            Debug.logError(e, "Problem getting virtual product");
                        }
                    }
                }

                if (pieces != null) {
                    piecesIncluded = pieces.longValue();
                }
            }
        }

        return piecesIncluded;
    }

   public List getShippableItemInfo(String shipGroupSeqId) {
        List shippableInfo = FastList.newInstance();

        List validItems = getValidOrderItems(shipGroupSeqId);
        if (validItems != null) {
            Iterator i = validItems.iterator();
            while (i.hasNext()) {
                GenericValue item = (GenericValue) i.next();
                shippableInfo.add(this.getItemInfoMap(item));
            }
        }

        return shippableInfo;
    }

    public Map getItemInfoMap(GenericValue item) {
        Map itemInfo = FastMap.newInstance();
        itemInfo.put("productId", item.getString("productId"));
        itemInfo.put("quantity", getOrderItemQuantity(item));
        itemInfo.put("weight", new Double(this.getItemWeight(item)));
        itemInfo.put("size",  new Double(this.getItemSize(item)));
        itemInfo.put("piecesIncluded", new Long(this.getItemPiecesIncluded(item)));
        itemInfo.put("featureSet", this.getItemFeatureSet(item));
        return itemInfo;
    }

    public String getOrderEmailString() {
        GenericDelegator delegator = orderHeader.getDelegator();
        // get the email addresses from the order contact mech(s)
        List orderContactMechs = null;
        try {
            Map ocFields = UtilMisc.toMap("orderId", orderHeader.get("orderId"), "contactMechPurposeTypeId", "ORDER_EMAIL");
            orderContactMechs = delegator.findByAnd("OrderContactMech", ocFields);
        } catch (GenericEntityException e) {
            Debug.logWarning(e, "Problems getting order contact mechs", module);
        }

        StringBuffer emails = new StringBuffer();
        if (orderContactMechs != null) {
            Iterator oci = orderContactMechs.iterator();
            while (oci.hasNext()) {
                try {
                    GenericValue orderContactMech = (GenericValue) oci.next();
                    GenericValue contactMech = orderContactMech.getRelatedOne("ContactMech");
                    emails.append(emails.length() > 0 ? "," : "").append(contactMech.getString("infoString"));
                } catch (GenericEntityException e) {
                    Debug.logWarning(e, "Problems getting contact mech from order contact mech", module);
                }
            }
        }
        return emails.toString();
    }

    public BigDecimal getOrderGrandTotalBd() {
        if (totalPrice == null) {
            totalPrice = getOrderGrandTotalBd(getValidOrderItems(), getAdjustments());
        }// else already set
        return totalPrice;
    }

    /** @deprecated Use getOrderGrandTotalBd() instead */
    public double getOrderGrandTotal() {
        return getOrderGrandTotalBd().doubleValue();
    }

    /**
     * Gets the amount open on the order that is not covered by the relevant OrderPaymentPreferences.
     * This works by adding up the amount allocated to each unprocessed OrderPaymentPreference and the
     * amounts received as payments for the settled ones.
     */
    public double getOrderOpenAmount() throws GenericEntityException {
        GenericDelegator delegator = orderHeader.getDelegator();
        double total = getOrderGrandTotal();
        double openAmount = 0;
        List prefs = getPaymentPreferences();

        // add up the covered amount, but skip preferences which are declined or cancelled
        for (Iterator iter = prefs.iterator(); iter.hasNext(); ) {
            GenericValue pref = (GenericValue) iter.next();
            if ("PAYMENT_CANCELLED".equals(pref.get("statusId")) || "PAYMENT_DECLINED".equals(pref.get("statusId"))) {
                continue;
            } else if ("PAYMENT_SETTLED".equals(pref.get("statusId"))) {
                List responses = pref.getRelatedByAnd("PaymentGatewayResponse", UtilMisc.toMap("transCodeEnumId", "PGT_CAPTURE"));
                for (Iterator respIter = responses.iterator(); respIter.hasNext(); ) {
                    GenericValue response = (GenericValue) respIter.next();
                    Double amount = response.getDouble("amount");
                    if (amount != null) {
                        openAmount += amount.doubleValue();
                    }
                }
            } else {
                // all others are currently "unprocessed" payment preferences
                Double maxAmount = pref.getDouble("maxAmount");
                if (maxAmount != null) {
                    openAmount += maxAmount.doubleValue();
                }
            }
        }
        return total - openAmount;
    }

    public List getOrderHeaderAdjustments() {
        return getOrderHeaderAdjustments(getAdjustments(), null);
    }

    public List getOrderHeaderAdjustments(String shipGroupSeqId) {
        return getOrderHeaderAdjustments(getAdjustments(), shipGroupSeqId);
    }

    public List getOrderHeaderAdjustmentsToShow() {
        return filterOrderAdjustments(getOrderHeaderAdjustments(), true, false, false, false, false);
    }

    public List getOrderHeaderStatuses() {
        return getOrderHeaderStatuses(getOrderStatuses());
    }

    public BigDecimal getOrderAdjustmentsTotalBd() {
        return getOrderAdjustmentsTotalBd(getValidOrderItems(), getAdjustments());
    }

    /** @deprecated Use getOrderAdjustmentsTotalBd() instead */
    public double getOrderAdjustmentsTotal() {
        return getOrderAdjustmentsTotalBd().doubleValue();
    }

    public BigDecimal getOrderAdjustmentTotalBd(GenericValue adjustment) {
        return calcOrderAdjustmentBd(adjustment, getOrderItemsSubTotalBd());
    }

    /** @deprecated Use getOrderAdjustmentsTotalBd() instead */
    public double getOrderAdjustmentTotal(GenericValue adjustment) {
        return getOrderAdjustmentTotalBd(adjustment).doubleValue();
    }

    public int hasSurvey() {
        GenericDelegator delegator = orderHeader.getDelegator();
        List surveys = null;
        try {
            surveys = delegator.findByAnd("SurveyResponse", UtilMisc.toMap("orderId", orderHeader.getString("orderId")));
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }
        int size = 0;
        if (surveys != null) {
            size = surveys.size();
        }

        return size;
    }

    // ========================================
    // ========== Order Item Methods ==========
    // ========================================

    public List getOrderItems() {
        if (orderItems == null) {
            try {
                orderItems = orderHeader.getRelated("OrderItem", UtilMisc.toList("orderItemSeqId"));
            } catch (GenericEntityException e) {
                Debug.logWarning(e, module);
            }
        }
        return orderItems;
    }

    public List getOrderItemAndShipGroupAssoc() {
        if (orderItemAndShipGrp == null) {
            try {
                orderItemAndShipGrp = orderHeader.getDelegator().findByAnd("OrderItemAndShipGroupAssoc",
                        UtilMisc.toMap("orderId", orderHeader.getString("orderId")));
            } catch (GenericEntityException e) {
                Debug.logWarning(e, module);
            }
        }
        return orderItemAndShipGrp;
    }

    public List getOrderItemAndShipGroupAssoc(String shipGroupSeqId) {
        List exprs = UtilMisc.toList(new EntityExpr("shipGroupSeqId", EntityOperator.EQUALS, shipGroupSeqId));
        return EntityUtil.filterByAnd(getOrderItemAndShipGroupAssoc(), exprs);
    }

    public List getValidOrderItems() {
        List exprs = UtilMisc.toList(
                new EntityExpr("statusId", EntityOperator.NOT_EQUAL, "ITEM_CANCELLED"),
                new EntityExpr("statusId", EntityOperator.NOT_EQUAL, "ITEM_REJECTED"));
        return EntityUtil.filterByAnd(getOrderItems(), exprs);
    }

    public boolean getPastEtaOrderItems(String orderId) {
        /*List exprs = UtilMisc.toList(new EntityExpr("statusId", EntityOperator.EQUALS, "ITEM_APPROVED"));
        List itemsApproved = EntityUtil.filterByAnd(getOrderItems(), exprs);
        Iterator i = itemsApproved.iterator();
        while (i.hasNext()) {
            GenericValue item = (GenericValue) i.next();
            Timestamp estimatedDeliveryDate = (Timestamp) item.get("estimatedDeliveryDate");
            if (estimatedDeliveryDate != null && UtilDateTime.nowTimestamp().after(estimatedDeliveryDate)) {
        	return true;
            }            
        }
        return false;
    }*/
	GenericDelegator delegator = orderHeader.getDelegator();
        GenericValue orderDeliverySchedule = null;
        try {
            orderDeliverySchedule = delegator.findByPrimaryKey("OrderDeliverySchedule", UtilMisc.toMap("orderId", orderId, "orderItemSeqId", "_NA_"));
        } catch (GenericEntityException e) {
        }
        Timestamp estimatedShipDate = null;
        if (orderDeliverySchedule != null && orderDeliverySchedule.get("estimatedReadyDate") != null) {
            estimatedShipDate = orderDeliverySchedule.getTimestamp("estimatedReadyDate");
        }
        if (estimatedShipDate != null && UtilDateTime.nowTimestamp().after(estimatedShipDate)) {
    		return true;
        }
        return false;
    }

    public boolean getRejectedOrderItems() {
    	List items = getOrderItems();	
        Iterator i = items.iterator();
        while (i.hasNext()) {
            GenericValue item = (GenericValue) i.next();
            List receipts = null;                  
            try {
        	receipts = item.getRelated("ShipmentReceipt");
            } catch (GenericEntityException e) {
                Debug.logWarning(e, module);
            }                         
            if (receipts != null && receipts.size() > 0) {
                Iterator recIter = receipts.iterator();
                while (recIter.hasNext()) {
                    GenericValue rec = (GenericValue) recIter.next();
                    Double rejected = rec.getDouble("quantityRejected");
                    if (rejected != null && rejected.doubleValue() > 0) {
                	return true;
                    }
                }            
            }
        }
        return false;
    }

    public boolean getPartiallyReceivedItems() {	
        /*List exprs = UtilMisc.toList(new EntityExpr("statusId", EntityOperator.EQUALS, "ITEM_APPROVED"));
        List itemsApproved = EntityUtil.filterByAnd(getOrderItems(), exprs);
        Iterator i = itemsApproved.iterator();
        while (i.hasNext()) {
            GenericValue item = (GenericValue) i.next();            
            int shippedQuantity = (int) getItemShippedQuantity(item);            
            Double orderedQuantity = (Double) item.get("quantity");            
            if (shippedQuantity != orderedQuantity.intValue() && shippedQuantity > 0) {
        	return true;
            }            
        }
        return false;
    }*/
    	List items = getOrderItems();	
        Iterator i = items.iterator();
        while (i.hasNext()) {
            GenericValue item = (GenericValue) i.next();
            List receipts = null;                  
            try {
        	receipts = item.getRelated("ShipmentReceipt");
            } catch (GenericEntityException e) {
                Debug.logWarning(e, module);
            }                         
            if (receipts != null && receipts.size() > 0) {
                Iterator recIter = receipts.iterator();
                while (recIter.hasNext()) {
                    GenericValue rec = (GenericValue) recIter.next();
                    Double acceptedQuantity = rec.getDouble("quantityAccepted");
                    Double orderedQuantity = (Double) item.get("quantity");            
                    if (acceptedQuantity.intValue() != orderedQuantity.intValue() && acceptedQuantity.intValue()  > 0) {
                	return true;                    
                    }
                }            
            }
        }
        return false;
    }

    public List getValidOrderItems(String shipGroupSeqId) {
        if (shipGroupSeqId == null) return getValidOrderItems();
        List exprs = UtilMisc.toList(
                new EntityExpr("statusId", EntityOperator.NOT_EQUAL, "ITEM_CANCELLED"),
                new EntityExpr("statusId", EntityOperator.NOT_EQUAL, "ITEM_REJECTED"),
                new EntityExpr("shipGroupSeqId", EntityOperator.EQUALS, shipGroupSeqId));
        return EntityUtil.filterByAnd(getOrderItemAndShipGroupAssoc(), exprs);
    }

    public GenericValue getOrderItem(String orderItemSeqId) {
        List exprs = UtilMisc.toList(new EntityExpr("orderItemSeqId", EntityOperator.EQUALS, orderItemSeqId));
        return EntityUtil.getFirst(EntityUtil.filterByAnd(getOrderItems(), exprs));
    }

    public List getValidDigitalItems() {
        List digitalItems = new ArrayList();
        // only approved or complete items apply
        List exprs = UtilMisc.toList(
                new EntityExpr("statusId", EntityOperator.EQUALS, "ITEM_APPROVED"),
                new EntityExpr("statusId", EntityOperator.EQUALS, "ITEM_COMPLETED"));
        List items = EntityUtil.filterByOr(getOrderItems(), exprs);
        Iterator i = items.iterator();
        while (i.hasNext()) {
            GenericValue item = (GenericValue) i.next();
            if (item.get("productId") != null) {
                GenericValue product = null;
                try {
                    product = item.getRelatedOne("Product");
                } catch (GenericEntityException e) {
                    Debug.logError(e, "Unable to get Product from OrderItem", module);
                }
                if (product != null) {
                    GenericValue productType = null;
                    try {
                        productType = product.getRelatedOne("ProductType");
                    } catch (GenericEntityException e) {
                        Debug.logError(e, "ERROR: Unable to get ProductType from Product", module);
                    }

                    if (productType != null) {
                        String isDigital = productType.getString("isDigital");

                        if (isDigital != null && "Y".equalsIgnoreCase(isDigital)) {
                            // make sure we have an OrderItemBilling record
                            List orderItemBillings = null;
                            try {
                                orderItemBillings = item.getRelated("OrderItemBilling");
                            } catch (GenericEntityException e) {
                                Debug.logError(e, "Unable to get OrderItemBilling from OrderItem");
                            }

                            if (orderItemBillings != null && orderItemBillings.size() > 0) {
                                // get the ProductContent records
                                List productContents = null;
                                try {
                                    productContents = product.getRelated("ProductContent");
                                } catch (GenericEntityException e) {
                                    Debug.logError("Unable to get ProductContent from Product", module);
                                }
                                List cExprs = UtilMisc.toList(
                                        new EntityExpr("productContentTypeId", EntityOperator.EQUALS, "DIGITAL_DOWNLOAD"),
                                        new EntityExpr("productContentTypeId", EntityOperator.EQUALS, "FULFILLMENT_EMAIL"),
                                        new EntityExpr("productContentTypeId", EntityOperator.EQUALS, "FULFILLMENT_EXTERNAL"));
                                // add more as needed
                                productContents = EntityUtil.filterByDate(productContents);
                                productContents = EntityUtil.filterByOr(productContents, cExprs);

                                if (productContents != null && productContents.size() > 0) {
                                    // make sure we are still within the allowed timeframe and use limits
                                    Iterator pci = productContents.iterator();
                                    while (pci.hasNext()) {
                                        GenericValue productContent = (GenericValue) pci.next();
                                        Timestamp fromDate = productContent.getTimestamp("purchaseFromDate");
                                        Timestamp thruDate = productContent.getTimestamp("purchaseThruDate");
                                        if (fromDate == null || item.getTimestamp("orderDate").after(fromDate)) {
                                            if (thruDate == null || item.getTimestamp("orderDate").before(thruDate)) {
                                                // TODO: Implement use count and days
                                                digitalItems.add(item);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return digitalItems;
    }

    public List getOrderItemAdjustments(GenericValue orderItem) {
        return getOrderItemAdjustmentList(orderItem, getAdjustments());
    }

    public String getCurrentOrderItemWorkEffort(GenericValue orderItem)    {
        GenericValue workOrderItemFulFillment;
        try {
            workOrderItemFulFillment = orderItem.getRelatedOne("WorkOrderItemFulFillment");
        }
        catch (GenericEntityException e) {
            return null;
        }
        GenericValue workEffort = null;
        try {
        workEffort = workOrderItemFulFillment.getRelatedOne("WorkEffort");
        }
        catch (GenericEntityException e) {
            return null;
        }
        return workEffort.getString("workEffortId");
    }

    public String getCurrentItemStatus(GenericValue orderItem) {
        GenericValue statusItem = null;
        try {
            statusItem = orderItem.getRelatedOne("StatusItem");
        } catch (GenericEntityException e) {
            Debug.logError(e, "Trouble getting StatusItem : " + orderItem, module);
        }
        if (statusItem == null || statusItem.get("description") == null) {
            return "Not Available";
        } else {
            return statusItem.getString("description");
        }
    }

    public List getOrderItemPriceInfos(GenericValue orderItem) {
        if (orderItem == null) return null;
        if (this.orderItemPriceInfos == null) {
            GenericDelegator delegator = orderHeader.getDelegator();

            try {
                orderItemPriceInfos = delegator.findByAnd("OrderItemPriceInfo", UtilMisc.toMap("orderId", orderHeader.get("orderId")));
            } catch (GenericEntityException e) {
                Debug.logWarning(e, module);
            }
        }
        String orderItemSeqId = (String) orderItem.get("orderItemSeqId");

        return EntityUtil.filterByAnd(this.orderItemPriceInfos, UtilMisc.toMap("orderItemSeqId", orderItemSeqId));
    }

    public List getOrderItemShipGroupAssocs(GenericValue orderItem) {
        if (orderItem == null) return null;
        try {
            return orderHeader.getDelegator().findByAnd("OrderItemShipGroupAssoc",
                    UtilMisc.toMap("orderId", orderItem.getString("orderId"), "orderItemSeqId", orderItem.getString("orderItemSeqId")), UtilMisc.toList("shipGroupSeqId"));
        } catch (GenericEntityException e) {
            Debug.logWarning(e, module);
        }
        return null;
    }

    public List getOrderItemShipGrpInvResList(GenericValue orderItem) {
        if (orderItem == null) return null;
        if (this.orderItemShipGrpInvResList == null) {
            GenericDelegator delegator = orderItem.getDelegator();
            try {
                orderItemShipGrpInvResList = delegator.findByAnd("OrderItemShipGrpInvRes", UtilMisc.toMap("orderId", orderItem.get("orderId")));
            } catch (GenericEntityException e) {
                Debug.logWarning(e, "Trouble getting OrderItemShipGrpInvRes List", module);
            }
        }
        return EntityUtil.filterByAnd(orderItemShipGrpInvResList, UtilMisc.toMap("orderItemSeqId", orderItem.getString("orderItemSeqId")));
    }

    public List getOrderItemIssuances(GenericValue orderItem) {
        return this.getOrderItemIssuances(orderItem, null);
    }

    public List getOrderItemIssuances(GenericValue orderItem, String shipmentId) {
        if (orderItem == null) return null;
        if (this.orderItemIssuances == null) {
            GenericDelegator delegator = orderItem.getDelegator();

            try {
                orderItemIssuances = delegator.findByAnd("ItemIssuance", UtilMisc.toMap("orderId", orderItem.get("orderId")));
            } catch (GenericEntityException e) {
                Debug.logWarning(e, "Trouble getting ItemIssuance(s)", module);
            }
        }

        // filter the issuances
        Map filter = UtilMisc.toMap("orderItemSeqId", orderItem.get("orderItemSeqId"));
        if (shipmentId != null) {
            filter.put("shipmentId", shipmentId);
        }
        return EntityUtil.filterByAnd(orderItemIssuances, filter);
    }

    /** Get a set of productIds in the order. */
    public Collection getOrderProductIds() {
        Set productIds = new HashSet();
        for (Iterator iter = getOrderItems().iterator(); iter.hasNext(); ) {
            productIds.add(((GenericValue) iter.next()).getString("productId"));
        }
        return productIds;
    }

    public List getOrderReturnItems() {
        GenericDelegator delegator = orderHeader.getDelegator();
        if (this.orderReturnItems == null) {
            try {
                this.orderReturnItems = delegator.findByAnd("ReturnItem", UtilMisc.toMap("orderId", orderHeader.getString("orderId")));
            } catch (GenericEntityException e) {
                Debug.logError(e, "Problem getting ReturnItem from order", module);
                return null;
            }
        }
        return this.orderReturnItems;
    }

   /**
    * Get the quantity returned per order item.
    * In other words, this method will count the ReturnItems
    * related to each OrderItem.
    *
    * @return  Map of returned quantities as Doubles keyed to the orderItemSeqId
    */
   public Map getOrderItemReturnedQuantities() {
       List returnItems = getOrderReturnItems();

       // since we don't have a handy grouped view entity, we'll have to group the return items by hand
       Map returnMap = FastMap.newInstance();
       for (Iterator iter = this.getValidOrderItems().iterator(); iter.hasNext(); ) {
           GenericValue orderItem = (GenericValue) iter.next();
           List group = EntityUtil.filterByAnd(returnItems,
                   UtilMisc.toMap("orderId", orderItem.get("orderId"), "orderItemSeqId", orderItem.get("orderItemSeqId")));

           // add up the returned quantities for this group TODO: received quantity should be used eventually
           double returned = 0;
           for (Iterator groupiter = group.iterator(); groupiter.hasNext(); ) {
               GenericValue returnItem = (GenericValue) groupiter.next();
               if (returnItem.getDouble("returnQuantity") != null) {
                   returned += (returnItem.getDouble("returnQuantity")).doubleValue();
               }
           }

           // the quantity returned per order item
           returnMap.put(orderItem.get("orderItemSeqId"), new Double(returned));
       }
       return returnMap;
   }

   /**
    * Get the total quantity of returned items for an order. This will count
    * only the ReturnItems that are directly correlated to an OrderItem.
    */
    public BigDecimal getOrderReturnedQuantityBd() {
        List returnedItemsBase = getOrderReturnItems();
        List returnedItems = new ArrayList(returnedItemsBase.size());

        // filter just order items
        List orderItemExprs = UtilMisc.toList(new EntityExpr("returnItemTypeId", EntityOperator.EQUALS, "RET_PROD_ITEM"));
        orderItemExprs.add(new EntityExpr("returnItemTypeId", EntityOperator.EQUALS, "RET_FPROD_ITEM"));
        orderItemExprs.add(new EntityExpr("returnItemTypeId", EntityOperator.EQUALS, "RET_DPROD_ITEM"));
        orderItemExprs.add(new EntityExpr("returnItemTypeId", EntityOperator.EQUALS, "RET_FDPROD_ITEM"));
        orderItemExprs.add(new EntityExpr("returnItemTypeId", EntityOperator.EQUALS, "RET_PROD_FEATR_ITEM"));
        orderItemExprs.add(new EntityExpr("returnItemTypeId", EntityOperator.EQUALS, "RET_SPROD_ITEM"));
        orderItemExprs.add(new EntityExpr("returnItemTypeId", EntityOperator.EQUALS, "RET_WE_ITEM"));
        orderItemExprs.add(new EntityExpr("returnItemTypeId", EntityOperator.EQUALS, "RET_TE_ITEM"));
        returnedItemsBase = EntityUtil.filterByOr(returnedItemsBase, orderItemExprs);

        // get only the RETURN_RECEIVED and RETURN_COMPLETED statusIds
        returnedItems.addAll(EntityUtil.filterByAnd(returnedItemsBase, UtilMisc.toMap("statusId", "RETURN_RECEIVED")));
        returnedItems.addAll(EntityUtil.filterByAnd(returnedItemsBase, UtilMisc.toMap("statusId", "RETURN_COMPLETED")));

        BigDecimal returnedQuantity = ZERO;
        if (returnedItems != null) {
            Iterator i = returnedItems.iterator();
            while (i.hasNext()) {
                GenericValue returnedItem = (GenericValue) i.next();
                if (returnedItem.get("returnQuantity") != null) {
                    returnedQuantity = returnedQuantity.add(returnedItem.getBigDecimal("returnQuantity")).setScale(scale, rounding);
                }
            }
        }
        return returnedQuantity.setScale(scale, rounding);
    }

    /** @deprecated */
    public double getOrderReturnedQuantity() {
        return getOrderReturnedQuantityBd().doubleValue();
    }

    /** @deprecated */
    public double getOrderReturnedTotal() {
        return getOrderReturnedTotalBd().doubleValue();
    }

    /** @deprecated */
    public double getOrderReturnedTotal(boolean includeAll) {
        return getOrderReturnedTotalBd(includeAll).doubleValue();
    }

    /** 
     * Get the returned total by return type (credit, refund, etc.).  Specify returnTypeId = null to get sum over all
     * return types.  Specify includeAll = true to sum up over all return statuses except cancelled.  Specify includeAll
     * = false to sum up over ACCEPTED,RECEIVED And COMPLETED returns.
     */
    public BigDecimal getOrderReturnedTotalByTypeBd(String returnTypeId, boolean includeAll) {
        List returnedItemsBase = getOrderReturnItems();
        if (returnTypeId != null) {
            returnedItemsBase = EntityUtil.filterByAnd(returnedItemsBase, UtilMisc.toMap("returnTypeId", returnTypeId));
        }
        List returnedItems = new ArrayList(returnedItemsBase.size());

        // get only the RETURN_RECEIVED and RETURN_COMPLETED statusIds
        if (!includeAll) {
            returnedItems.addAll(EntityUtil.filterByAnd(returnedItemsBase, UtilMisc.toMap("statusId", "RETURN_ACCEPTED")));            
            returnedItems.addAll(EntityUtil.filterByAnd(returnedItemsBase, UtilMisc.toMap("statusId", "RETURN_RECEIVED")));
            returnedItems.addAll(EntityUtil.filterByAnd(returnedItemsBase, UtilMisc.toMap("statusId", "RETURN_COMPLETED")));
        } else {
            // otherwise get all of them except cancelled ones
            returnedItems.addAll(EntityUtil.filterByAnd(returnedItemsBase,
                    UtilMisc.toList(new EntityExpr("statusId", EntityOperator.NOT_EQUAL, "RETURN_CANCELLED"))));
        }
        BigDecimal returnedAmount = ZERO;
        Iterator i = returnedItems.iterator();
        String orderId = orderHeader.getString("orderId");
        List returnHeaderList = new ArrayList();
        while (i.hasNext()) {
            GenericValue returnedItem = (GenericValue) i.next();
            if ((returnedItem.get("returnPrice") != null) && (returnedItem.get("returnQuantity") != null)) {
                returnedAmount = returnedAmount.add(returnedItem.getBigDecimal("returnPrice").multiply(returnedItem.getBigDecimal("returnQuantity"))).setScale(scale, rounding);
            }
            Map itemAdjustmentCondition = UtilMisc.toMap("returnId", returnedItem.get("returnId"), "returnItemSeqId", returnedItem.get("returnItemSeqId"));
            returnedAmount = returnedAmount.add(getReturnAdjustmentTotalBd(orderHeader.getDelegator(), itemAdjustmentCondition));
            if(orderId.equals(returnedItem.getString("orderId")) && (!returnHeaderList.contains(returnedItem.getString("returnId")))) {
                returnHeaderList.add(returnedItem.getString("returnId"));
            }
        }
        //get  returnedAmount from returnHeader adjustments whose orderId must equals to current orderHeader.orderId
        Iterator returnHeaderIterator = returnHeaderList.iterator();
        while(returnHeaderIterator.hasNext()) {
            String returnId = (String) returnHeaderIterator.next();
            Map returnHeaderAdjFilter = UtilMisc.toMap("returnId", returnId, "returnItemSeqId", "_NA_");
            returnedAmount =returnedAmount.add(getReturnAdjustmentTotalBd(orderHeader.getDelegator(), returnHeaderAdjFilter)).setScale(scale, rounding);
        }
        return returnedAmount.setScale(scale, rounding);
    }

    /** Gets the total return credit for COMPLETED and RECEIVED returns. */
    public BigDecimal getOrderReturnedCreditTotalBd() {
        return getOrderReturnedTotalByTypeBd("RTN_CREDIT", false);
    }

    /** Gets the total return refunded for COMPLETED and RECEIVED returns. */
    public BigDecimal getOrderReturnedRefundTotalBd() {
        return getOrderReturnedTotalByTypeBd("RTN_REFUND", false);
    }

    /** Gets the total return amount (all return types) for COMPLETED and RECEIVED returns. */
    public BigDecimal getOrderReturnedTotalBd() {
        return getOrderReturnedTotalByTypeBd(null, false);
    }

    /** 
     * Gets the total returned over all return types.  Specify true to include all return statuses
     * except cancelled.  Specify false to include only COMPLETED and RECEIVED returns.
     */ 
    public BigDecimal getOrderReturnedTotalBd(boolean includeAll) {
        return getOrderReturnedTotalByTypeBd(null, includeAll);
    }

    public BigDecimal getOrderNonReturnedTaxAndShippingBd() {
        // first make a Map of orderItemSeqId key, returnQuantity value
        List returnedItemsBase = getOrderReturnItems();
        List returnedItems = new ArrayList(returnedItemsBase.size());

        // get only the RETURN_RECEIVED and RETURN_COMPLETED statusIds
        returnedItems.addAll(EntityUtil.filterByAnd(returnedItemsBase, UtilMisc.toMap("statusId", "RETURN_RECEIVED")));
        returnedItems.addAll(EntityUtil.filterByAnd(returnedItemsBase, UtilMisc.toMap("statusId", "RETURN_COMPLETED")));

        Map itemReturnedQuantities = FastMap.newInstance();
        Iterator i = returnedItems.iterator();
        while (i.hasNext()) {
            GenericValue returnedItem = (GenericValue) i.next();
            String orderItemSeqId = returnedItem.getString("orderItemSeqId");
            BigDecimal returnedQuantity = returnedItem.getBigDecimal("returnQuantity");
            if (orderItemSeqId != null && returnedQuantity != null) {
                BigDecimal existingQuantity =  (BigDecimal) itemReturnedQuantities.get(orderItemSeqId);
                if (existingQuantity == null) {
                    itemReturnedQuantities.put(orderItemSeqId, returnedQuantity);
                } else {
                    itemReturnedQuantities.put(orderItemSeqId, returnedQuantity.add(existingQuantity));
                }
            }
        }

        // then go through all order items and for the quantity not returned calculate it's portion of the item, and of the entire order
        BigDecimal totalSubTotalNotReturned = ZERO;
        BigDecimal totalTaxNotReturned = ZERO;
        BigDecimal totalShippingNotReturned = ZERO;

        Iterator orderItems = this.getValidOrderItems().iterator();
        while (orderItems.hasNext()) {
            GenericValue orderItem = (GenericValue) orderItems.next();

            BigDecimal itemQuantityDbl = orderItem.getBigDecimal("quantity");
            if (itemQuantityDbl == null) {
                continue;
            }
            BigDecimal itemQuantity = itemQuantityDbl;
            BigDecimal itemSubTotal = this.getOrderItemSubTotalBd(orderItem);
            BigDecimal itemTaxes = this.getOrderItemTaxBd(orderItem);
            BigDecimal itemShipping = this.getOrderItemShippingBd(orderItem);

            BigDecimal quantityReturnedDouble = (BigDecimal) itemReturnedQuantities.get(orderItem.get("orderItemSeqId"));
            BigDecimal quantityReturned = ZERO;
            if (quantityReturnedDouble != null) {
                quantityReturned = quantityReturnedDouble;
            }

            BigDecimal quantityNotReturned = itemQuantity.subtract(quantityReturned);

            // pro-rated factor (quantity not returned / total items ordered), which shouldn't be rounded to 2 decimals
            BigDecimal factorNotReturned = quantityNotReturned.divide(itemQuantity, 100, rounding);

            BigDecimal subTotalNotReturned = itemSubTotal.multiply(factorNotReturned).setScale(scale, rounding);

            // calculate tax and shipping adjustments for each item, add to accumulators
            BigDecimal itemTaxNotReturned = itemTaxes.multiply(factorNotReturned).setScale(scale, rounding);
            BigDecimal itemShippingNotReturned = itemShipping.multiply(factorNotReturned).setScale(scale, rounding);

            totalSubTotalNotReturned = totalSubTotalNotReturned.add(subTotalNotReturned);
            totalTaxNotReturned = totalTaxNotReturned.add(itemTaxNotReturned);
            totalShippingNotReturned = totalShippingNotReturned.add(itemShippingNotReturned);
        }

        // calculate tax and shipping adjustments for entire order, add to result
        BigDecimal orderItemsSubTotal = this.getOrderItemsSubTotalBd();
        BigDecimal orderFactorNotReturned = ZERO;
        if (orderItemsSubTotal.signum() != 0) {
            // pro-rated factor (subtotal not returned / item subtotal), which shouldn't be rounded to 2 decimals
            orderFactorNotReturned = totalSubTotalNotReturned.divide(orderItemsSubTotal, 100, rounding);
        }
        BigDecimal orderTaxNotReturned = this.getHeaderTaxTotalBd().multiply(orderFactorNotReturned).setScale(scale, rounding);
        BigDecimal orderShippingNotReturned = this.getShippingTotalBd().multiply(orderFactorNotReturned).setScale(scale, rounding);

        return totalTaxNotReturned.add(totalShippingNotReturned).add(orderTaxNotReturned).add(orderShippingNotReturned).setScale(scale, rounding);
    }

    /** Gets the total refunded to the order billing account by type.  Specify null to get total over all types. */
    public BigDecimal getBillingAccountReturnedTotalByTypeBd(String returnTypeId) {
        BigDecimal returnedAmount = ZERO;
        List returnedItemsBase = getOrderReturnItems();
        if (returnTypeId != null) {
            returnedItemsBase = EntityUtil.filterByAnd(returnedItemsBase, UtilMisc.toMap("returnTypeId", returnTypeId));
        }
        List returnedItems = new ArrayList(returnedItemsBase.size());

        // get only the RETURN_RECEIVED and RETURN_COMPLETED statusIds
        returnedItems.addAll(EntityUtil.filterByAnd(returnedItemsBase, UtilMisc.toMap("statusId", "RETURN_RECEIVED")));
        returnedItems.addAll(EntityUtil.filterByAnd(returnedItemsBase, UtilMisc.toMap("statusId", "RETURN_COMPLETED")));

        // sum up the return items that have a return item response with a billing account defined
        try {
            for (Iterator iter = returnedItems.iterator(); iter.hasNext(); ) {
                GenericValue returnItem = (GenericValue) iter.next();
                GenericValue returnItemResponse = returnItem.getRelatedOne("ReturnItemResponse");
                if (returnItemResponse == null) continue;
                if (returnItemResponse.get("billingAccountId") == null) continue;

                // we can just add the response amounts
                returnedAmount = returnedAmount.add(returnItemResponse.getBigDecimal("responseAmount")).setScale(scale, rounding);
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, e.getMessage(), module);
        }
        return returnedAmount;
    }

    /** Get the total return credited to the order billing accounts */
    public BigDecimal getBillingAccountReturnedCreditTotalBd() {
        return getBillingAccountReturnedTotalByTypeBd("RTN_CREDIT");
    }

    /** Get the total return refunded to the order billing accounts */
    public BigDecimal getBillingAccountReturnedRefundTotalBd() {
        return getBillingAccountReturnedTotalByTypeBd("RTN_REFUND");
    }

    /** Gets the total return credited amount with refunds and credits to the billing account figured in */
    public BigDecimal getReturnedCreditTotalWithBillingAccountBd() {
        return getOrderReturnedCreditTotalBd().add(getBillingAccountReturnedRefundTotalBd()).subtract(getBillingAccountReturnedCreditTotalBd());
    }

    /** Gets the total return refund amount with refunds and credits to the billing account figured in */
    public BigDecimal getReturnedRefundTotalWithBillingAccountBd() {
        return getOrderReturnedRefundTotalBd().add(getBillingAccountReturnedCreditTotalBd()).subtract(getBillingAccountReturnedRefundTotalBd());
    }

    /** @deprecated */
    public double getOrderNonReturnedTaxAndShipping() {
        return getOrderNonReturnedTaxAndShippingBd().doubleValue();
    }

    public BigDecimal getOrderBackorderQuantityBd() {
        BigDecimal backorder = ZERO;
        List items = this.getValidOrderItems();
        if (items != null) {
            Iterator ii = items.iterator();
            while (ii.hasNext()) {
                GenericValue item = (GenericValue) ii.next();
                List reses = this.getOrderItemShipGrpInvResList(item);
                if (reses != null) {
                    Iterator ri = reses.iterator();
                    while (ri.hasNext()) {
                        GenericValue res = (GenericValue) ri.next();
                        BigDecimal nav = res.getBigDecimal("quantityNotAvailable");
                        if (nav != null) {
                            backorder = backorder.add(nav).setScale(scale, rounding);
                        }
                    }
                }
            }
        }
        return backorder.setScale(scale, rounding);
    }

    /** @deprecated */
    public double getOrderBackorderQuantity() {
        return getOrderBackorderQuantityBd().doubleValue();
    }

    public BigDecimal getItemPickedQuantityBd(GenericValue orderItem) {
        BigDecimal quantityPicked = ZERO;
        EntityConditionList pickedConditions = new EntityConditionList(UtilMisc.toList(
                new EntityExpr("orderId", EntityOperator.EQUALS, orderItem.get("orderId")),
                new EntityExpr("orderItemSeqId", EntityOperator.EQUALS, orderItem.getString("orderItemSeqId")),
                new EntityExpr("statusId", EntityOperator.NOT_EQUAL, "PICKLIST_CANCELLED")),
                EntityOperator.AND);
        
        List picked = null;
        try {
        	picked = orderHeader.getDelegator().findByCondition("PicklistAndBinAndItem", pickedConditions, null, null);
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            this.orderHeader = null;
        }

        if (picked != null) {
            Iterator i = picked.iterator();
            while (i.hasNext()) {
                GenericValue pickedItem = (GenericValue) i.next();
                BigDecimal issueQty = pickedItem.getBigDecimal("quantity");
                if (issueQty != null) {
                    quantityPicked = quantityPicked.add(issueQty).setScale(scale, rounding);
                }
            }
        }
        return quantityPicked.setScale(scale, rounding);
    }

    public BigDecimal getItemShippedQuantityBd(GenericValue orderItem) {
        BigDecimal quantityShipped = ZERO;
        List issuance = getOrderItemIssuances(orderItem);
        if (issuance != null) {
            Iterator i = issuance.iterator();
            while (i.hasNext()) {
                GenericValue issue = (GenericValue) i.next();
                BigDecimal issueQty = issue.getBigDecimal("quantity");
                if (issueQty != null) {
                    quantityShipped = quantityShipped.add(issueQty).setScale(scale, rounding);
                }
            }
        }
        return quantityShipped.setScale(scale, rounding);
    }

    /** @deprecated */
    public double getItemShippedQuantity(GenericValue orderItem) {
        return getItemShippedQuantityBd(orderItem).doubleValue();
    }

    public BigDecimal getItemReservedQuantityBd(GenericValue orderItem) {
        BigDecimal reserved = ZERO;

        List reses = getOrderItemShipGrpInvResList(orderItem);
        if (reses != null) {
            Iterator i = reses.iterator();
            while (i.hasNext()) {
                GenericValue res = (GenericValue) i.next();
                BigDecimal quantity = res.getBigDecimal("quantity");
                if (quantity != null) {
                    reserved = reserved.add(quantity).setScale(scale, rounding);
                }
            }
        }
        return reserved.setScale(scale, rounding);
    }

    /** @deprecated */
    public double getItemReservedQuantity(GenericValue orderItem) {
        return getItemReservedQuantityBd(orderItem).doubleValue();
    }

    public BigDecimal getItemBackorderedQuantityBd(GenericValue orderItem) {
        BigDecimal backOrdered = ZERO;

        Timestamp shipDate = orderItem.getTimestamp("estimatedShipDate");
        Timestamp autoCancel = orderItem.getTimestamp("autoCancelDate");

        List reses = getOrderItemShipGrpInvResList(orderItem);
        if (reses != null) {
            Iterator i = reses.iterator();
            while (i.hasNext()) {
                GenericValue res = (GenericValue) i.next();
                Timestamp promised = res.getTimestamp("currentPromisedDate");
                if (promised == null) {
                    promised = res.getTimestamp("promisedDatetime");
                }
                if (autoCancel != null || (shipDate != null && shipDate.after(promised))) {
                    BigDecimal resQty = res.getBigDecimal("quantity");
                    if (resQty != null) {
                        backOrdered = backOrdered.add(resQty).setScale(scale, rounding);
                    }
                }
            }
        }
        return backOrdered;
    }

    /** @deprecated */
    public double getItemBackorderedQuantity(GenericValue orderItem) {
        return getItemBackorderedQuantityBd(orderItem).doubleValue();
    }

    public BigDecimal getItemPendingShipmentQuantityBd(GenericValue orderItem) {
        BigDecimal reservedQty = getItemReservedQuantityBd(orderItem);
        BigDecimal backordered = getItemBackorderedQuantityBd(orderItem);
        return reservedQty.subtract(backordered).setScale(scale, rounding);
    }

    /** @deprecated */
    public double getItemPendingShipmentQuantity(GenericValue orderItem) {
        return getItemPendingShipmentQuantityBd(orderItem).doubleValue();
    }

    public double getItemCanceledQuantity(GenericValue orderItem) {
        Double cancelQty = orderItem.getDouble("cancelQuantity");
        if (cancelQty == null) cancelQty = new Double(0);
        return cancelQty.doubleValue();
    }

    public BigDecimal getTotalOrderItemsQuantityBd() {
        List orderItems = getValidOrderItems();
        BigDecimal totalItems = ZERO;

        for (int i = 0; i < orderItems.size(); i++) {
            GenericValue oi = (GenericValue) orderItems.get(i);

            totalItems = totalItems.add(getOrderItemQuantityBd(oi)).setScale(scale, rounding);
        }
        return totalItems.setScale(scale, rounding);
    }

    /** @deprecated */
    public double getTotalOrderItemsQuantity() {
        return getTotalOrderItemsQuantityBd().doubleValue();
    }

    public BigDecimal getTotalOrderItemsOrderedQuantityBd() {
        List orderItems = getValidOrderItems();
        BigDecimal totalItems = ZERO;

        for (int i = 0; i < orderItems.size(); i++) {
            GenericValue oi = (GenericValue) orderItems.get(i);

            totalItems = totalItems.add(oi.getBigDecimal("quantity")).setScale(scale, rounding);
        }
        return totalItems;
    }

    /** @deprecated */
    public double getTotalOrderItemsOrderedQuantity() {
        return getTotalOrderItemsOrderedQuantityBd().doubleValue();
    }

    public BigDecimal getOrderItemsSubTotalBd() {
        return getOrderItemsSubTotalBd(getValidOrderItems(), getAdjustments());
    }

    /** @deprecated */
    public double getOrderItemsSubTotal() {
        return getOrderItemsSubTotalBd().doubleValue();
    }

    public BigDecimal getOrderItemSubTotalBd(GenericValue orderItem) {
        return getOrderItemSubTotalBd(orderItem, getAdjustments());
    }

    /** @deprecated */
    public double getOrderItemSubTotal(GenericValue orderItem) {
        return getOrderItemSubTotalBd(orderItem).doubleValue();
    }

    public BigDecimal getOrderItemsTotalBd() {
        return getOrderItemsTotalBd(getValidOrderItems(), getAdjustments());
    }

    /** @deprecated */
    public double getOrderItemsTotal() {
        return getOrderItemsTotalBd().doubleValue();
    }

    public BigDecimal getOrderItemTotalBd(GenericValue orderItem) {
        return getOrderItemTotalBd(orderItem, getAdjustments());
    }

    /** @deprecated */
    public double getOrderItemTotal(GenericValue orderItem) {
        return getOrderItemTotalBd(orderItem).doubleValue();
    }

    public BigDecimal getOrderItemTaxBd(GenericValue orderItem) {
        return getOrderItemAdjustmentsTotalBd(orderItem, false, true, false);
    }

    /** @deprecated */
    public double getOrderItemTax(GenericValue orderItem) {
        return getOrderItemTaxBd(orderItem).doubleValue();
    }

    public BigDecimal getOrderItemShippingBd(GenericValue orderItem) {
        return getOrderItemAdjustmentsTotalBd(orderItem, false, false, true);
    }

    /** @deprecated */
    public double getOrderItemShipping(GenericValue orderItem) {
        return getOrderItemShippingBd(orderItem).doubleValue();
    }

    public BigDecimal getOrderItemAdjustmentsTotalBd(GenericValue orderItem, boolean includeOther, boolean includeTax, boolean includeShipping) {
        return getOrderItemAdjustmentsTotalBd(orderItem, getAdjustments(), includeOther, includeTax, includeShipping);
    }

    /** @deprecated */
    public double getOrderItemAdjustmentsTotal(GenericValue orderItem, boolean includeOther, boolean includeTax, boolean includeShipping) {
        return getOrderItemAdjustmentsTotalBd(orderItem, getAdjustments(), includeOther, includeTax, includeShipping).doubleValue();
    }

    public BigDecimal getOrderItemAdjustmentsTotalBd(GenericValue orderItem) {
        return getOrderItemAdjustmentsTotalBd(orderItem, true, false, false);
    }

    /** @deprecated */
    public double getOrderItemAdjustmentsTotal(GenericValue orderItem) {
        return getOrderItemAdjustmentsTotalBd(orderItem, true, false, false).doubleValue();
    }

    public BigDecimal getOrderItemAdjustmentTotalBd(GenericValue orderItem, GenericValue adjustment) {
        return calcItemAdjustmentBd(adjustment, orderItem);
    }

    /** @deprecated */
    public double getOrderItemAdjustmentTotal(GenericValue orderItem, GenericValue adjustment) {
        return getOrderItemAdjustmentTotalBd(orderItem, adjustment).doubleValue();
    }

    public String getAdjustmentType(GenericValue adjustment) {
        GenericValue adjustmentType = null;
        try {
            adjustmentType = adjustment.getRelatedOne("OrderAdjustmentType");
        } catch (GenericEntityException e) {
            Debug.logError(e, "Problems with order adjustment", module);
        }
        if (adjustmentType == null || adjustmentType.get("description") == null) {
            return "";
        } else {
            return adjustmentType.getString("description");
        }
    }

    public List getOrderItemStatuses(GenericValue orderItem) {
        return getOrderItemStatuses(orderItem, getOrderStatuses());
    }

    public String getCurrentItemStatusString(GenericValue orderItem) {
        GenericValue statusItem = null;
        try {
            statusItem = orderItem.getRelatedOneCache("StatusItem");
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }
        if (statusItem != null) {
            return statusItem.getString("description");
        } else {
            return orderHeader.getString("statusId");
        }
    }

    /** Fetches the set of order items with the given EntityCondition. */
    public List getOrderItemsByCondition(EntityCondition entityCondition) {
        return EntityUtil.filterByCondition(getOrderItems(), entityCondition);
    }

    /**
     * Checks to see if this user has read permission on this order
     * @param userLogin The UserLogin value object to check
     * @return boolean True if we have read permission
     */
    public boolean hasPermission(Security security, GenericValue userLogin) {
        return OrderReadHelper.hasPermission(security, userLogin, orderHeader);
    }

    /**
     * Getter for property orderHeader.
     * @return Value of property orderHeader.
     */
    public GenericValue getOrderHeader() {
        return orderHeader;
    }

    // ======================================================
    // =================== Static Methods ===================
    // ======================================================

    public static GenericValue getOrderHeader(GenericDelegator delegator, String orderId) {
        GenericValue orderHeader = null;
        if (orderId != null && delegator != null) {
            try {
                orderHeader = delegator.findByPrimaryKey("OrderHeader", UtilMisc.toMap("orderId", orderId));
            } catch (GenericEntityException e) {
                Debug.logError(e, "Cannot get order header", module);
            }
        }
        return orderHeader;
    }

    public static BigDecimal getOrderItemQuantityBd(GenericValue orderItem) {

        BigDecimal cancelQty = orderItem.getBigDecimal("cancelQuantity");
        BigDecimal orderQty = orderItem.getBigDecimal("quantity");

        if (cancelQty == null) cancelQty = ZERO;
        if (orderQty == null) orderQty = ZERO;

        return orderQty.subtract(cancelQty).setScale(scale, rounding);
    }

    /** @deprecated */
    public static Double getOrderItemQuantity(GenericValue orderItem) {
        return new Double(getOrderItemQuantityBd(orderItem).doubleValue());
    }

    public static Double getOrderItemShipGroupQuantity(GenericValue shipGroupAssoc) {
        Double cancelQty = shipGroupAssoc.getDouble("cancelQuantity");
        Double orderQty = shipGroupAssoc.getDouble("quantity");

        if (cancelQty == null) cancelQty = new Double(0.0);
        if (orderQty == null) orderQty = new Double(0.0);

        return new Double(orderQty.doubleValue() - cancelQty.doubleValue());
    }

    public static GenericValue getProductStoreFromOrder(GenericDelegator delegator, String orderId) {
        GenericValue orderHeader = getOrderHeader(delegator, orderId);
        if (orderHeader == null) {
            Debug.logWarning("Could not find OrderHeader for orderId [" + orderId + "] in getProductStoreFromOrder, returning null", module);
        }
        return getProductStoreFromOrder(orderHeader);
    }

    public static GenericValue getProductStoreFromOrder(GenericValue orderHeader) {
        if (orderHeader == null) {
            return null;
        }
        GenericDelegator delegator = orderHeader.getDelegator();
        GenericValue productStore = null;
        if (orderHeader != null && orderHeader.get("productStoreId") != null) {
            try {
                productStore = delegator.findByPrimaryKeyCache("ProductStore", UtilMisc.toMap("productStoreId", orderHeader.getString("productStoreId")));
            } catch (GenericEntityException e) {
                Debug.logError(e, "Cannot locate ProductStore from OrderHeader", module);
            }
        } else {
            Debug.logError("Null header or productStoreId", module);
        }
        return productStore;
    }

    public static BigDecimal getOrderGrandTotalBd(List orderItems, List adjustments) {
        BigDecimal total = getOrderItemsTotalBd(orderItems, adjustments);
        BigDecimal adj = getOrderAdjustmentsTotalBd(orderItems, adjustments);
        return total.add(adj).setScale(scale,rounding);
    }

    /** @deprecated */
    public static double getOrderGrandTotal(List orderItems, List adjustments) {
        return getOrderGrandTotalBd(orderItems, adjustments).doubleValue();
    }

    public static List getOrderHeaderAdjustments(List adjustments, String shipGroupSeqId) {
        List contraints1 = UtilMisc.toList(new EntityExpr("orderItemSeqId", EntityOperator.EQUALS, null));
        List contraints2 = UtilMisc.toList(new EntityExpr("orderItemSeqId", EntityOperator.EQUALS, DataModelConstants.SEQ_ID_NA));
        List contraints3 = UtilMisc.toList(new EntityExpr("orderItemSeqId", EntityOperator.EQUALS, ""));
        List contraints4 = FastList.newInstance();
        if (shipGroupSeqId != null) {
            contraints4.add(new EntityExpr("shipGroupSeqId", EntityOperator.EQUALS, shipGroupSeqId));
        }
        List toFilter = null;
        List adj = FastList.newInstance();

        if (shipGroupSeqId != null) {
            toFilter = EntityUtil.filterByAnd(adjustments, contraints4);
        } else {
            toFilter = adjustments;
        }

        adj.addAll(EntityUtil.filterByAnd(toFilter, contraints1));
        adj.addAll(EntityUtil.filterByAnd(toFilter, contraints2));
        adj.addAll(EntityUtil.filterByAnd(toFilter, contraints3));
        return adj;
    }

    public static List getOrderHeaderStatuses(List orderStatuses) {
        List contraints1 = UtilMisc.toList(new EntityExpr("orderItemSeqId", EntityOperator.EQUALS, null));
        List contraints2 = UtilMisc.toList(new EntityExpr("orderItemSeqId", EntityOperator.EQUALS, DataModelConstants.SEQ_ID_NA));
        List contraints3 = UtilMisc.toList(new EntityExpr("orderItemSeqId", EntityOperator.EQUALS, ""));
        List newOrderStatuses = FastList.newInstance();

        newOrderStatuses.addAll(EntityUtil.filterByAnd(orderStatuses, contraints1));
        newOrderStatuses.addAll(EntityUtil.filterByAnd(orderStatuses, contraints2));
        newOrderStatuses.addAll(EntityUtil.filterByAnd(orderStatuses, contraints3));
        newOrderStatuses = EntityUtil.orderBy(newOrderStatuses, UtilMisc.toList("-statusDatetime"));
        return newOrderStatuses;
    }

    public static BigDecimal getOrderAdjustmentsTotalBd(List orderItems, List adjustments) {
        return calcOrderAdjustmentsBd(getOrderHeaderAdjustments(adjustments, null), getOrderItemsSubTotalBd(orderItems, adjustments), true, true, true);
    }

    /** @deprecated */
    public static double getOrderAdjustmentsTotal(List orderItems, List adjustments) {
        return getOrderAdjustmentsTotalBd(orderItems, adjustments).doubleValue();
    }

    public static List getOrderSurveyResponses(GenericValue orderHeader) {
        GenericDelegator delegator = orderHeader.getDelegator();
        String orderId = orderHeader.getString("orderId");
         List responses = null;
        try {
            responses = delegator.findByAnd("SurveyResponse", UtilMisc.toMap("orderId", orderId, "orderItemSeqId", "_NA_"));
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }

        if (responses == null) {
            responses = FastList.newInstance();
        }
        return responses;
    }

    public static List getOrderItemSurveyResponse(GenericValue orderItem) {
        GenericDelegator delegator = orderItem.getDelegator();
        String orderItemSeqId = orderItem.getString("orderItemSeqId");
        String orderId = orderItem.getString("orderId");
        List responses = null;
        try {
            responses = delegator.findByAnd("SurveyResponse", UtilMisc.toMap("orderId", orderId, "orderItemSeqId", orderItemSeqId));
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }

        if (responses == null) {
            responses = FastList.newInstance();
        }
        return responses;
    }

    // ================= Order Adjustments =================

    public static BigDecimal calcOrderAdjustmentsBd(List orderHeaderAdjustments, BigDecimal subTotal, boolean includeOther, boolean includeTax, boolean includeShipping) {
        BigDecimal adjTotal = ZERO;

        if (orderHeaderAdjustments != null && orderHeaderAdjustments.size() > 0) {
            List filteredAdjs = filterOrderAdjustments(orderHeaderAdjustments, includeOther, includeTax, includeShipping, false, false);
            Iterator adjIt = filteredAdjs.iterator();

            while (adjIt.hasNext()) {
                GenericValue orderAdjustment = (GenericValue) adjIt.next();

                adjTotal = adjTotal.add(OrderReadHelper.calcOrderAdjustmentBd(orderAdjustment, subTotal)).setScale(scale, rounding);
            }
        }
        return adjTotal.setScale(scale, rounding);
    }

    /** @deprecated */
    public static double calcOrderAdjustments(List orderHeaderAdjustments, double subTotal, boolean includeOther, boolean includeTax, boolean includeShipping) {
        return calcOrderAdjustmentsBd(orderHeaderAdjustments, new BigDecimal(subTotal), includeOther, includeTax, includeShipping).doubleValue();
    }

    public static BigDecimal calcOrderAdjustmentBd(GenericValue orderAdjustment, BigDecimal orderSubTotal) {
        BigDecimal adjustment = ZERO;

        if (orderAdjustment.get("amount") != null) {
            // round amount to best precision (taxCalcScale) because db value of 0.825 is pulled as 0.8249999...
            BigDecimal amount = orderAdjustment.getBigDecimal("amount").setScale(taxCalcScale, taxRounding); 
            adjustment = adjustment.add(amount);
        }
        else if (orderAdjustment.get("sourcePercentage") != null) {
            // round amount to best precision (taxCalcScale) because db value of 0.825 is pulled as 0.8249999...
            BigDecimal percent = orderAdjustment.getBigDecimal("sourcePercentage").setScale(taxCalcScale,taxRounding);
            BigDecimal amount = orderSubTotal.multiply(percent).multiply(percentage).setScale(taxCalcScale, taxRounding);
            adjustment = adjustment.add(amount);
        }        
        return adjustment.setScale(scale, rounding);
    }

    /** @deprecated */
    public static double calcOrderAdjustment(GenericValue orderAdjustment, double orderSubTotal) {
        return calcOrderAdjustmentBd(orderAdjustment, new BigDecimal(orderSubTotal)).doubleValue();
    }

    // ================= Order Item Adjustments =================
    public static BigDecimal getOrderItemsSubTotalBd(List orderItems, List adjustments) {
        return getOrderItemsSubTotalBd(orderItems, adjustments, null);
    }

    /** @deprecated */
    public static double getOrderItemsSubTotal(List orderItems, List adjustments) {
        return getOrderItemsSubTotalBd(orderItems, adjustments).doubleValue();
    }

    public static BigDecimal getOrderItemsSubTotalBd(List orderItems, List adjustments, List workEfforts) {
        BigDecimal result = ZERO;
        Iterator itemIter = UtilMisc.toIterator(orderItems);

        while (itemIter != null && itemIter.hasNext()) {
            GenericValue orderItem = (GenericValue) itemIter.next();
            BigDecimal itemTotal = getOrderItemSubTotalBd(orderItem, adjustments);
            // Debug.log("Item : " + orderItem.getString("orderId") + " / " + orderItem.getString("orderItemSeqId") + " = " + itemTotal, module);

            if (workEfforts != null && orderItem.getString("orderItemTypeId").compareTo("RENTAL_ORDER_ITEM") == 0) {
                Iterator weIter = UtilMisc.toIterator(workEfforts);
                while (weIter != null && weIter.hasNext()) {
                    GenericValue workEffort = (GenericValue) weIter.next();
                    if (workEffort.getString("workEffortId").compareTo(orderItem.getString("orderItemSeqId")) == 0)    {
                        itemTotal = itemTotal.multiply(getWorkEffortRentalQuantityBd(workEffort)).setScale(scale, rounding);
                        break;
                    }
//                    Debug.log("Item : " + orderItem.getString("orderId") + " / " + orderItem.getString("orderItemSeqId") + " = " + itemTotal, module);
                }
            }
            result = result.add(itemTotal).setScale(scale, rounding);

        }
        return result.setScale(scale, rounding);
    }

    /** @deprecated */
    public static double getOrderItemsSubTotal(List orderItems, List adjustments, List workEfforts) {
        return getOrderItemsSubTotalBd(orderItems, adjustments, workEfforts).doubleValue();
    }

    /** The passed adjustments can be all adjustments for the order, ie for all line items */
    public static BigDecimal getOrderItemSubTotalBd(GenericValue orderItem, List adjustments) {
        return getOrderItemSubTotalBd(orderItem, adjustments, false, false);
    }

    /** @deprecated */
    public static double getOrderItemSubTotal(GenericValue orderItem, List adjustments) {
        return getOrderItemSubTotalBd(orderItem, adjustments).doubleValue();
    }

    /** The passed adjustments can be all adjustments for the order, ie for all line items */
    public static BigDecimal getOrderItemSubTotalBd(GenericValue orderItem, List adjustments, boolean forTax, boolean forShipping) {
        BigDecimal unitPrice = orderItem.getBigDecimal("unitPrice");
        BigDecimal quantity = getOrderItemQuantityBd(orderItem);
        BigDecimal result = ZERO;

        if (unitPrice == null || quantity == null) {
            Debug.logWarning("[getOrderItemTotal] unitPrice or quantity are null, using 0 for the item base price", module);
        } else {
            if (Debug.verboseOn()) Debug.logVerbose("Unit Price : " + unitPrice + " / " + "Quantity : " + quantity, module);
            result = unitPrice.multiply(quantity);

            if (orderItem.getString("orderItemTypeId").compareTo("RENTAL_ORDER_ITEM") == 0)    { // retrieve related work effort when required.
                List WorkOrderItemFulfillments = null;
                try {
                    WorkOrderItemFulfillments = orderItem.getRelatedCache("WorkOrderItemFulfillment");
                } catch (GenericEntityException e) {}
                Iterator iter = WorkOrderItemFulfillments.iterator();
                if (iter.hasNext())    {
                    GenericValue WorkOrderItemFulfillment = (GenericValue) iter.next();
                    GenericValue workEffort = null;
                    try {
                        workEffort = WorkOrderItemFulfillment.getRelatedOneCache("WorkEffort");
                    } catch (GenericEntityException e) {}
                    result = result.multiply(getWorkEffortRentalQuantityBd(workEffort));
                }
            }
        }

        // subtotal also includes non tax and shipping adjustments; tax and shipping will be calculated using this adjusted value
        result =result.add(getOrderItemAdjustmentsTotalBd(orderItem, adjustments, true, false, false, forTax, forShipping));

        return result.setScale(scale, rounding);
    }

    /** @deprecated */
    public static double getOrderItemSubTotal(GenericValue orderItem, List adjustments, boolean forTax, boolean forShipping) {
        return getOrderItemSubTotalBd(orderItem, adjustments, forTax, forShipping).doubleValue();
    }

    public static BigDecimal getOrderItemsTotalBd(List orderItems, List adjustments) {
        BigDecimal result = ZERO;
        Iterator itemIter = UtilMisc.toIterator(orderItems);

        while (itemIter != null && itemIter.hasNext()) {
            result = result.add(getOrderItemTotalBd((GenericValue) itemIter.next(), adjustments)).setScale(scale, rounding);
        }
        return result.setScale(scale,  rounding);
    }

    /** @deprecated */
    public static double getOrderItemsTotal(List orderItems, List adjustments) {
        return getOrderItemsTotalBd(orderItems, adjustments).doubleValue();
    }

    public static BigDecimal getOrderItemTotalBd(GenericValue orderItem, List adjustments) {
        // add tax and shipping to subtotal
        return getOrderItemSubTotalBd(orderItem, adjustments).add(getOrderItemAdjustmentsTotalBd(orderItem, adjustments, false, true, true)).setScale(scale, rounding);
    }

    /** @deprecated */
    public static double getOrderItemTotal(GenericValue orderItem, List adjustments) {
        return getOrderItemTotalBd(orderItem, adjustments).doubleValue();
    }

    public static BigDecimal getWorkEffortRentalQuantityBd(GenericValue workEffort){
        BigDecimal persons = new BigDecimal(1);
        if (workEffort.get("reservPersons") != null)
            persons = workEffort.getBigDecimal("reservPersons");
        BigDecimal secondPersonPerc = ZERO;
        if (workEffort.get("reserv2ndPPPerc") != null)
            secondPersonPerc = workEffort.getBigDecimal("reserv2ndPPPerc");
        BigDecimal nthPersonPerc = ZERO;
        if (workEffort.get("reservNthPPPerc") != null)
            nthPersonPerc = workEffort.getBigDecimal("reservNthPPPerc");
        long length = 1;
        if (workEffort.get("estimatedStartDate") != null && workEffort.get("estimatedCompletionDate") != null)
            length = (workEffort.getTimestamp("estimatedCompletionDate").getTime() - workEffort.getTimestamp("estimatedStartDate").getTime()) / 86400000;

        BigDecimal rentalAdjustment = ZERO;
        if (persons.compareTo(new BigDecimal(1)) == 1)    {
            if (persons.compareTo(new BigDecimal(2)) == 1) {
                persons = persons.subtract(new BigDecimal(2));
                if(nthPersonPerc.signum() == 1)
                    rentalAdjustment = persons.multiply(nthPersonPerc);
                else
                    rentalAdjustment = persons.multiply(secondPersonPerc);
                persons = new BigDecimal("2");
            }
            if (persons.compareTo(new BigDecimal("2")) == 0)
                rentalAdjustment = rentalAdjustment.add(secondPersonPerc);
        }
        rentalAdjustment = rentalAdjustment.add(new BigDecimal(100));  // add final 100 percent for first person
        rentalAdjustment = rentalAdjustment.divide(new BigDecimal(100), scale, rounding).multiply(new BigDecimal(String.valueOf(length)));
//        Debug.logInfo("rental parameters....Nbr of persons:" + persons + " extra% 2nd person:" + secondPersonPerc + " extra% Nth person:" + nthPersonPerc + " Length: " + length + "  total rental adjustment:" + rentalAdjustment ,module);
        return rentalAdjustment; // return total rental adjustment
        }

    /** @deprecated */
    public static double getWorkEffortRentalQuantity(GenericValue workEffort) {
        return getWorkEffortRentalQuantityBd(workEffort).doubleValue();
    }

    public static BigDecimal getAllOrderItemsAdjustmentsTotalBd(List orderItems, List adjustments, boolean includeOther, boolean includeTax, boolean includeShipping) {
        BigDecimal result = ZERO;
        Iterator itemIter = UtilMisc.toIterator(orderItems);

        while (itemIter != null && itemIter.hasNext()) {
            result = result.add(getOrderItemAdjustmentsTotalBd((GenericValue) itemIter.next(), adjustments, includeOther, includeTax, includeShipping)).setScale(scale, rounding);
        }
        return result;
    }

    /** @deprecated */
    public static double getAllOrderItemsAdjustmentsTotal(List orderItems, List adjustments, boolean includeOther, boolean includeTax, boolean includeShipping) {
        return getAllOrderItemsAdjustmentsTotalBd(orderItems, adjustments, includeOther, includeTax, includeShipping).doubleValue();
    }

    /** The passed adjustments can be all adjustments for the order, ie for all line items */
    public static BigDecimal getOrderItemAdjustmentsTotalBd(GenericValue orderItem, List adjustments, boolean includeOther, boolean includeTax, boolean includeShipping) {
        return getOrderItemAdjustmentsTotalBd(orderItem, adjustments, includeOther, includeTax, includeShipping, false, false);
    }

    /** @deprecated */
    public static double getOrderItemAdjustmentsTotal(GenericValue orderItem, List adjustments, boolean includeOther, boolean includeTax, boolean includeShipping) {
        return getOrderItemAdjustmentsTotalBd(orderItem, adjustments, includeOther, includeTax, includeShipping, false, false).doubleValue();
    }

    /** The passed adjustments can be all adjustments for the order, ie for all line items */
    public static BigDecimal getOrderItemAdjustmentsTotalBd(GenericValue orderItem, List adjustments, boolean includeOther, boolean includeTax, boolean includeShipping, boolean forTax, boolean forShipping) {
        return calcItemAdjustmentsBd(getOrderItemQuantityBd(orderItem), orderItem.getBigDecimal("unitPrice"),
                getOrderItemAdjustmentList(orderItem, adjustments),
                includeOther, includeTax, includeShipping, forTax, forShipping);
    }

    /** @deprecated */
    public double getOrderItemAdjustmentsTotal(GenericValue orderItem, List adjustments, boolean includeOther, boolean includeTax, boolean includeShipping, boolean forTax, boolean forShipping) {
        return getOrderItemAdjustmentsTotalBd(orderItem, adjustments, includeOther, includeTax, includeShipping, forTax, forShipping).doubleValue();
    }

    public static List getOrderItemAdjustmentList(GenericValue orderItem, List adjustments) {
        return EntityUtil.filterByAnd(adjustments, UtilMisc.toMap("orderItemSeqId", orderItem.get("orderItemSeqId")));
    }

    public static List getOrderItemStatuses(GenericValue orderItem, List orderStatuses) {
        return EntityUtil.orderBy(EntityUtil.filterByAnd(orderStatuses, UtilMisc.toMap("orderItemSeqId", orderItem.get("orderItemSeqId"))), UtilMisc.toList("-statusDatetime"));
    }


    // Order Item Adjs Utility Methods

    public static BigDecimal calcItemAdjustmentsBd(BigDecimal quantity, BigDecimal unitPrice, List adjustments, boolean includeOther, boolean includeTax, boolean includeShipping, boolean forTax, boolean forShipping) {
        BigDecimal adjTotal = ZERO;

        if (adjustments != null && adjustments.size() > 0) {
            List filteredAdjs = filterOrderAdjustments(adjustments, includeOther, includeTax, includeShipping, forTax, forShipping);
            Iterator adjIt = filteredAdjs.iterator();

            while (adjIt.hasNext()) {
                GenericValue orderAdjustment = (GenericValue) adjIt.next();

                adjTotal = adjTotal.add(OrderReadHelper.calcItemAdjustmentBd(orderAdjustment, quantity, unitPrice)).setScale(scale, rounding);
            }
        }
        return adjTotal;
    }

    public static BigDecimal calcItemAdjustmentsRecurringBd(BigDecimal quantity, BigDecimal unitPrice, List adjustments, boolean includeOther, boolean includeTax, boolean includeShipping, boolean forTax, boolean forShipping) {
        BigDecimal adjTotal = ZERO;

        if (adjustments != null && adjustments.size() > 0) {
            List filteredAdjs = filterOrderAdjustments(adjustments, includeOther, includeTax, includeShipping, forTax, forShipping);
            Iterator adjIt = filteredAdjs.iterator();

            while (adjIt.hasNext()) {
                GenericValue orderAdjustment = (GenericValue) adjIt.next();

                adjTotal = adjTotal.add(OrderReadHelper.calcItemAdjustmentRecurringBd(orderAdjustment, quantity, unitPrice)).setScale(scale, rounding);
            }
        }
        return adjTotal;
    }

    /** @deprecated */
    public static double calcItemAdjustments(Double quantity, Double unitPrice, List adjustments, boolean includeOther, boolean includeTax, boolean includeShipping, boolean forTax, boolean forShipping) {
        return calcItemAdjustmentsBd(new BigDecimal(quantity.doubleValue()), new BigDecimal(unitPrice.doubleValue()), adjustments, includeOther, includeTax, includeShipping, forTax, forShipping).doubleValue();
    }

    public static BigDecimal calcItemAdjustmentBd(GenericValue itemAdjustment, GenericValue item) {
        return calcItemAdjustmentBd(itemAdjustment, getOrderItemQuantityBd(item), item.getBigDecimal("unitPrice"));
    }

    /** @deprecated */
    public static double calcItemAdjustment(GenericValue itemAdjustment, GenericValue item) {
        return calcItemAdjustmentBd(itemAdjustment, item).doubleValue();
    }

    public static BigDecimal calcItemAdjustmentBd(GenericValue itemAdjustment, BigDecimal quantity, BigDecimal unitPrice) {
        BigDecimal adjustment = ZERO;
        if (itemAdjustment.get("amount") != null) {
            adjustment = adjustment.add(setScaleByType("SALES_TAX".equals(itemAdjustment.get("orderAdjustmentTypeId")), itemAdjustment.getBigDecimal("amount")));
        }
        else if (itemAdjustment.get("sourcePercentage") != null) {
            adjustment = adjustment.add(setScaleByType("SALES_TAX".equals(itemAdjustment.get("orderAdjustmentTypeId")), itemAdjustment.getBigDecimal("sourcePercentage").multiply(quantity).multiply(unitPrice).multiply(percentage)));
        }
        if (Debug.verboseOn()) Debug.logVerbose("calcItemAdjustment: " + itemAdjustment + ", quantity=" + quantity + ", unitPrice=" + unitPrice + ", adjustment=" + adjustment, module);
        return adjustment.setScale(scale, rounding);
    }

    public static BigDecimal calcItemAdjustmentRecurringBd(GenericValue itemAdjustment, BigDecimal quantity, BigDecimal unitPrice) {
        BigDecimal adjustmentRecurring = ZERO;
        if (itemAdjustment.get("recurringAmount") != null) {
            adjustmentRecurring = adjustmentRecurring.add(setScaleByType("SALES_TAX".equals(itemAdjustment.get("orderAdjustmentTypeId")), itemAdjustment.getBigDecimal("recurringAmount")));
        }
        if (Debug.verboseOn()) Debug.logVerbose("calcItemAdjustmentRecurring: " + itemAdjustment + ", quantity=" + quantity + ", unitPrice=" + unitPrice + ", adjustmentRecurring=" + adjustmentRecurring, module);
        return adjustmentRecurring.setScale(scale, rounding);
    }

    /** @deprecated */
    public static double calcItemAdjustment(GenericValue itemAdjustment, Double quantity, Double unitPrice) {
        return calcItemAdjustmentBd(itemAdjustment, new BigDecimal(quantity.doubleValue()), new BigDecimal(unitPrice.doubleValue())).doubleValue();
    }

    public static List filterOrderAdjustments(List adjustments, boolean includeOther, boolean includeTax, boolean includeShipping, boolean forTax, boolean forShipping) {
        List newOrderAdjustmentsList = FastList.newInstance();

        if (adjustments != null && adjustments.size() > 0) {
            Iterator adjIt = adjustments.iterator();

            while (adjIt.hasNext()) {
                GenericValue orderAdjustment = (GenericValue) adjIt.next();

                boolean includeAdjustment = false;

                if ("SALES_TAX".equals(orderAdjustment.getString("orderAdjustmentTypeId"))) {
                    if (includeTax) includeAdjustment = true;
                } else if ("SHIPPING_CHARGES".equals(orderAdjustment.getString("orderAdjustmentTypeId"))) {
                    if (includeShipping) includeAdjustment = true;
                } else {
                    if (includeOther) includeAdjustment = true;
                }

                // default to yes, include for shipping; so only exclude if includeInShipping is N, or false; if Y or null or anything else it will be included
                if (forTax && "N".equals(orderAdjustment.getString("includeInTax"))) {
                    includeAdjustment = false;
                }

                // default to yes, include for shipping; so only exclude if includeInShipping is N, or false; if Y or null or anything else it will be included
                if (forShipping && "N".equals(orderAdjustment.getString("includeInShipping"))) {
                    includeAdjustment = false;
                }

                if (includeAdjustment) {
                    newOrderAdjustmentsList.add(orderAdjustment);
                }
            }
        }
        return newOrderAdjustmentsList;
    }

    public static double getQuantityOnOrder(GenericDelegator delegator, String productId) {
        double quantity = 0.0;

        // first find all open purchase orders
        List openOrdersExprs = UtilMisc.toList(new EntityExpr("orderTypeId", EntityOperator.EQUALS, "PURCHASE_ORDER"));
        openOrdersExprs.add(new EntityExpr("itemStatusId", EntityOperator.NOT_EQUAL, "ITEM_CANCELLED"));
        openOrdersExprs.add(new EntityExpr("itemStatusId", EntityOperator.NOT_EQUAL, "ITEM_REJECTED"));
        openOrdersExprs.add(new EntityExpr("itemStatusId", EntityOperator.NOT_EQUAL, "ITEM_COMPLETED"));
        openOrdersExprs.add(new EntityExpr("productId", EntityOperator.EQUALS, productId));
        EntityCondition openOrdersCond = new EntityConditionList(openOrdersExprs, EntityOperator.AND);
        List openOrders = null;
        try {
            openOrders = delegator.findByCondition("OrderHeaderAndItems", openOrdersCond, null, null);
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }

        if (openOrders != null && openOrders.size() > 0) {
            Iterator i = openOrders.iterator();
            while (i.hasNext()) {
                GenericValue order = (GenericValue) i.next();
                Double thisQty = order.getDouble("quantity");
                if (thisQty == null) {
                    thisQty = new Double(0);
                }
                quantity += thisQty.doubleValue();
            }
        }

        return quantity;
    }

    /**
     * Checks to see if this user has read permission on the specified order
     * @param userLogin The UserLogin value object to check
     * @param orderHeader The OrderHeader for the specified order
     * @return boolean True if we have read permission
     */
    public static boolean hasPermission(Security security, GenericValue userLogin, GenericValue orderHeader) {
        if (userLogin == null || orderHeader == null)
            return false;

        if (security.hasEntityPermission("ORDERMGR", "_VIEW", userLogin)) {
            return true;
        } else if (security.hasEntityPermission("ORDERMGR", "_ROLEVIEW", userLogin)) {
            List orderRoles = null;
            try {
                orderRoles = orderHeader.getRelatedByAnd("OrderRole",
                        UtilMisc.toMap("partyId", userLogin.getString("partyId")));
            } catch (GenericEntityException e) {
                Debug.logError(e, "Cannot get OrderRole from OrderHeader", module);
            }

            if (orderRoles.size() > 0) {
                // we are in at least one role
                return true;
            }
        }

        return false;
    }

    public static OrderReadHelper getHelper(GenericValue orderHeader) {
        return new OrderReadHelper(orderHeader);
    }

    /**
     * Get orderAdjustments that have no corresponding returnAdjustment
     * @return orderAdjustmentList
     */
    public List getAvailableOrderHeaderAdjustments() {
        List orderHeaderAdjustments = this.getOrderHeaderAdjustments();
        List filteredAdjustments = new ArrayList();
        if (orderHeaderAdjustments != null) {
            Iterator orderAdjIterator = orderHeaderAdjustments.iterator();
            while (orderAdjIterator.hasNext()) {
                GenericValue orderAdjustment = (GenericValue) orderAdjIterator.next();
                long count = 0;
                try {
                    count = orderHeader.getDelegator().findCountByAnd("ReturnAdjustment", UtilMisc.toMap("orderAdjustmentId", orderAdjustment.get("orderAdjustmentId")));
                } catch (GenericEntityException e) {
                    Debug.logError(e, module);
                }
                if (count == 0) {
                    filteredAdjustments.add(orderAdjustment);
                }
            }
        }
        return filteredAdjustments;
    }

    /**
     * Get the total return adjustments for a set of key -> value condition pairs.  Done for code efficiency.
     * @param delegator
     * @param condition
     * @return
     */
    public static BigDecimal getReturnAdjustmentTotalBd(GenericDelegator delegator, Map condition) {
        BigDecimal total = ZERO;
        List adjustments;
        try {
            // TODO: find on a view-entity with a sum is probably more efficient
            adjustments = delegator.findByAnd("ReturnAdjustment", condition);
            if (adjustments != null) {
                Iterator adjustmentIterator = adjustments.iterator();
                while (adjustmentIterator.hasNext()) {
                    GenericValue returnAdjustment = (GenericValue) adjustmentIterator.next();
                    total = total.add(setScaleByType("RET_SALES_TAX_ADJ".equals(returnAdjustment.get("returnAdjustmentTypeId")),returnAdjustment.getBigDecimal("amount"))).setScale(scale, rounding);
                }
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, OrderReturnServices.module);
        }
        return total.setScale(scale, rounding);
    }

    /** @deprecated */
    public static double getReturnAdjustmentTotal(GenericDelegator delegator, Map condition) {
        return getReturnAdjustmentTotalBd(delegator, condition).doubleValue();
    }

    // little helper method to set the scale according to tax type
    public static BigDecimal setScaleByType(boolean isTax, BigDecimal value){
        return isTax ? value.setScale(taxCalcScale, taxRounding) : value.setScale(scale, rounding);
    }

   /** Get the quantity of order items that have been invoiced */
   public static double getOrderItemInvoicedQuantity(GenericValue orderItem) {
       double invoiced = 0;
       try {
           // this is simply the sum of quantity billed in all related OrderItemBillings
           List billings = orderItem.getRelated("OrderItemBilling");
           for (Iterator iter = billings.iterator(); iter.hasNext(); ) {
               GenericValue billing = (GenericValue) iter.next();
               Double quantity = billing.getDouble("quantity");
               if (quantity != null) {
                   invoiced += quantity.doubleValue();
               }
           }
       } catch (GenericEntityException e) {
           Debug.logError(e, e.getMessage(), module);
       }
       return invoiced;
   }
}
