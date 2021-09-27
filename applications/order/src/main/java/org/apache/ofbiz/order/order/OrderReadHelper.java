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
package org.apache.ofbiz.order.order;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilFormatOut;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilNumber;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.common.DataModelConstants;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntity;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityConditionList;
import org.apache.ofbiz.entity.condition.EntityExpr;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.EntityUtil;
import org.apache.ofbiz.product.product.ProductWorker;
import org.apache.ofbiz.security.Security;

/**
 * Utility class for easily extracting important information from orders
 *
 * <p>NOTE: in the current scheme order adjustments are never included in tax or shipping,
 * but order item adjustments ARE included in tax and shipping calcs unless they are
 * tax or shipping adjustments or the includeInTax or includeInShipping are set to N.</p>
 */
public class OrderReadHelper {

    private static final String MODULE = OrderReadHelper.class.getName();

    // scales and ROUNDING modes for BigDecimal math
    private static final int DECIMALS = UtilNumber.getBigDecimalScale("order.decimals");
    private static final RoundingMode ROUNDING = UtilNumber.getRoundingMode("order.rounding");
    private static final int TAX_SCALE = UtilNumber.getBigDecimalScale("salestax.calc.decimals");
    private static final int TAX_FINAL_SCALE = UtilNumber.getBigDecimalScale("salestax.final.decimals");
    private static final RoundingMode TAX_ROUNDING = UtilNumber.getRoundingMode("salestax.rounding");
    private static final BigDecimal ZERO = (BigDecimal.ZERO).setScale(DECIMALS, ROUNDING);
    private static final BigDecimal PERCENTAGE = (new BigDecimal("0.01")).setScale(DECIMALS, ROUNDING);

    private GenericValue orderHeader = null;
    private List<GenericValue> orderItemAndShipGrp = null;
    private List<GenericValue> orderItems = null;
    private List<GenericValue> adjustments = null;
    private List<GenericValue> paymentPrefs = null;
    private List<GenericValue> orderStatuses = null;
    private List<GenericValue> orderItemPriceInfos = null;
    private List<GenericValue> orderItemShipGrpInvResList = null;
    private List<GenericValue> orderItemIssuances = null;
    private List<GenericValue> orderReturnItems = null;
    private Map<String, GenericValue> orderAttributeMap = null;
    private List<GenericValue> orderItemAttributes = null;
    private BigDecimal totalPrice = null;
    protected OrderReadHelper() { }

    /**
     * Instantiates a new Order read helper.
     * @param orderHeader the order header
     * @param adjustments the adjustments
     * @param orderItems  the order items
     */
    public OrderReadHelper(GenericValue orderHeader, List<GenericValue> adjustments, List<GenericValue> orderItems) {
        this.orderHeader = orderHeader;
        this.adjustments = adjustments;
        this.orderItems = orderItems;
        if (this.orderHeader != null && !"OrderHeader".equals(this.orderHeader.getEntityName())) {
            try {
                this.orderHeader = orderHeader.getDelegator().findOne("OrderHeader", UtilMisc.toMap("orderId",
                        orderHeader.getString("orderId")), false);
            } catch (GenericEntityException e) {
                Debug.logError(e, MODULE);
                this.orderHeader = null;
            }
        } else if (this.orderHeader == null && orderItems != null) {
            GenericValue firstItem = EntityUtil.getFirst(orderItems);
            try {
                this.orderHeader = firstItem.getRelatedOne("OrderHeader", false);
            } catch (GenericEntityException e) {
                Debug.logError(e, MODULE);
                this.orderHeader = null;
            }
        }
        if (this.orderHeader == null) {
            if (orderHeader == null) {
                throw new IllegalArgumentException("Order header passed is null, or is otherwise invalid");
            }
            throw new IllegalArgumentException("Order header passed in is not valid for orderId [" + orderHeader.getString("orderId") + "]");
        }
    }

    /**
     * Instantiates a new Order read helper.
     * @param orderHeader the order header
     */
    public OrderReadHelper(GenericValue orderHeader) {
        this(orderHeader, null, null);
    }

    /**
     * Instantiates a new Order read helper.
     * @param adjustments the adjustments
     * @param orderItems  the order items
     */
    public OrderReadHelper(List<GenericValue> adjustments, List<GenericValue> orderItems) {
        this.adjustments = adjustments;
        this.orderItems = orderItems;
    }

    /**
     * Instantiates a new Order read helper.
     * @param delegator the delegator
     * @param orderId   the order id
     */
    public OrderReadHelper(Delegator delegator, String orderId) {
        try {
            this.orderHeader = EntityQuery.use(delegator).from("OrderHeader").where("orderId", orderId).queryOne();
        } catch (GenericEntityException e) {
            String errMsg = "Error finding order with ID [" + orderId + "]: " + e.toString();
            Debug.logError(e, errMsg, MODULE);
            throw new IllegalArgumentException(errMsg);
        }
        if (this.orderHeader == null) {
            throw new IllegalArgumentException("Order not found with orderId [" + orderId + "]");
        }
    }

    // ==========================================
    // ========== Order Header Methods ==========
    // ==========================================

    /**
     * Gets order id.
     * @return the order id
     */
    public String getOrderId() {
        return orderHeader.getString("orderId");
    }

    /**
     * Gets web site id.
     * @return the web site id
     */
    public String getWebSiteId() {
        return orderHeader.getString("webSiteId");
    }

    /**
     * Gets product store id.
     * @return the product store id
     */
    public String getProductStoreId() {
        return orderHeader.getString("productStoreId");
    }

    /**
     * Returns the ProductStore of this Order or null in case of Exception
     * @return the product store
     */
    public GenericValue getProductStore() {
        String productStoreId = orderHeader.getString("productStoreId");
        try {
            Delegator delegator = orderHeader.getDelegator();
            GenericValue productStore = EntityQuery.use(delegator).from("ProductStore").where("productStoreId", productStoreId).cache().queryOne();
            return productStore;
        } catch (GenericEntityException ex) {
            Debug.logError(ex, "Failed to get product store for order header [" + orderHeader + "] due to exception " + ex.getMessage(), MODULE);
            return null;
        }
    }

    /**
     * Gets order type id.
     * @return the order type id
     */
    public String getOrderTypeId() {
        return orderHeader.getString("orderTypeId");
    }

    /**
     * Gets currency.
     * @return the currency
     */
    public String getCurrency() {
        return orderHeader.getString("currencyUom");
    }

    /**
     * Gets order name.
     * @return the order name
     */
    public String getOrderName() {
        return orderHeader.getString("orderName");
    }

    /**
     * Gets adjustments.
     * @return the adjustments
     */
    public List<GenericValue> getAdjustments() {
        if (adjustments == null) {
            try {
                adjustments = orderHeader.getRelated("OrderAdjustment", null, null, false);
            } catch (GenericEntityException e) {
                Debug.logError(e, MODULE);
            }
            if (adjustments == null) {
                adjustments = new LinkedList<>();
            }
        }
        return adjustments;
    }
    /**
     * Gets payment preferences.
     * @return the payment preferences
     */
    public List<GenericValue> getPaymentPreferences() {
        if (paymentPrefs == null) {
            try {
                paymentPrefs = orderHeader.getRelated("OrderPaymentPreference", null, UtilMisc.toList("orderPaymentPreferenceId"), false);
            } catch (GenericEntityException e) {
                Debug.logError(e, MODULE);
            }
        }
        return paymentPrefs;
    }
    /**
     * Returns a Map of paymentMethodId -&gt; amount charged (BigDecimal) based on PaymentGatewayResponse.
     * @return returns a Map of paymentMethodId -&gt; amount charged (BigDecimal) based on PaymentGatewayResponse.
     */
    public Map<String, BigDecimal> getReceivedPaymentTotalsByPaymentMethod() {
        Map<String, BigDecimal> paymentMethodAmounts = new HashMap<>();
        List<GenericValue> paymentPrefs = getPaymentPreferences();
        for (GenericValue paymentPref : paymentPrefs) {
            List<GenericValue> payments = new LinkedList<>();
            try {
                List<EntityExpr> exprs = UtilMisc.toList(EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, "PMNT_RECEIVED"),
                        EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, "PMNT_CONFIRMED"));
                payments = paymentPref.getRelated("Payment", null, null, false);
                payments = EntityUtil.filterByOr(payments, exprs);
                List<EntityExpr> conds = UtilMisc.toList(EntityCondition.makeCondition("paymentTypeId", EntityOperator.EQUALS, "CUSTOMER_PAYMENT"),
                        EntityCondition.makeCondition("paymentTypeId", EntityOperator.EQUALS, "CUSTOMER_DEPOSIT"),
                        EntityCondition.makeCondition("paymentTypeId", EntityOperator.EQUALS, "INTEREST_RECEIPT"),
                        EntityCondition.makeCondition("paymentTypeId", EntityOperator.EQUALS, "GC_DEPOSIT"),
                        EntityCondition.makeCondition("paymentTypeId", EntityOperator.EQUALS, "POS_PAID_IN"));
                payments = EntityUtil.filterByOr(payments, conds);
            } catch (GenericEntityException e) {
                Debug.logError(e, MODULE);
            }

            BigDecimal chargedToPaymentPref = ZERO;
            for (GenericValue payment : payments) {
                if (payment.get("amount") != null) {
                    chargedToPaymentPref = chargedToPaymentPref.add(payment.getBigDecimal("amount")).setScale(DECIMALS + 1, ROUNDING);
                }
            }

            if (chargedToPaymentPref.compareTo(ZERO) > 0) {
                // key of the resulting map is paymentMethodId or paymentMethodTypeId if the paymentMethodId is not available
                String paymentMethodKey = paymentPref.getString("paymentMethodId") != null ? paymentPref.getString("paymentMethodId")
                        : paymentPref.getString("paymentMethodTypeId");
                if (paymentMethodAmounts.containsKey(paymentMethodKey)) {
                    BigDecimal value = paymentMethodAmounts.get(paymentMethodKey);
                    if (value != null) {
                        chargedToPaymentPref = chargedToPaymentPref.add(value);
                    }
                }
                paymentMethodAmounts.put(paymentMethodKey, chargedToPaymentPref.setScale(DECIMALS, ROUNDING));
            }
        }
        return paymentMethodAmounts;
    }

    /**
     * Returns a Map of paymentMethodId -&gt; amount refunded
     * @return returns a Map of paymentMethodId -&gt; amount refunded
     */
    public Map<String, BigDecimal> getReturnedTotalsByPaymentMethod() {
        Map<String, BigDecimal> paymentMethodAmounts = new HashMap<>();
        List<GenericValue> paymentPrefs = getPaymentPreferences();
        for (GenericValue paymentPref : paymentPrefs) {
            List<GenericValue> returnItemResponses = new LinkedList<>();
            try {
                returnItemResponses = orderHeader.getDelegator().findByAnd("ReturnItemResponse",
                        UtilMisc.toMap("orderPaymentPreferenceId", paymentPref.getString("orderPaymentPreferenceId")), null, false);
            } catch (GenericEntityException e) {
                Debug.logError(e, MODULE);
            }
            BigDecimal refundedToPaymentPref = ZERO;
            for (GenericValue returnItemResponse : returnItemResponses) {
                refundedToPaymentPref = refundedToPaymentPref.add(returnItemResponse.getBigDecimal("responseAmount"))
                        .setScale(DECIMALS + 1, ROUNDING);
            }

            if (refundedToPaymentPref.compareTo(ZERO) == 1) {
                String paymentMethodId = paymentPref.getString("paymentMethodId") != null ? paymentPref.getString("paymentMethodId")
                        : paymentPref.getString("paymentMethodTypeId");
                paymentMethodAmounts.put(paymentMethodId, refundedToPaymentPref.setScale(DECIMALS, ROUNDING));
            }
        }
        return paymentMethodAmounts;
    }

    /**
     * Gets order payments.
     * @return the order payments
     */
    public List<GenericValue> getOrderPayments() {
        return getOrderPayments(null);
    }

    /**
     * Gets order payments.
     * @param orderPaymentPreference the order payment preference
     * @return the order payments
     */
    public List<GenericValue> getOrderPayments(GenericValue orderPaymentPreference) {
        List<GenericValue> orderPayments = new LinkedList<>();
        List<GenericValue> prefs = null;

        if (orderPaymentPreference == null) {
            prefs = getPaymentPreferences();
        } else {
            prefs = UtilMisc.toList(orderPaymentPreference);
        }
        if (prefs != null) {
            for (GenericValue payPref : prefs) {
                try {
                    orderPayments.addAll(payPref.getRelated("Payment", null, null, false));
                } catch (GenericEntityException e) {
                    Debug.logError(e, MODULE);
                    return null;
                }
            }
        }
        return orderPayments;
    }

    /**
     * Gets order statuses.
     * @return the order statuses
     */
    public List<GenericValue> getOrderStatuses() {
        if (orderStatuses == null) {
            try {
                orderStatuses = orderHeader.getRelated("OrderStatus", null, null, false);
            } catch (GenericEntityException e) {
                Debug.logError(e, MODULE);
            }
        }
        return orderStatuses;
    }

    /**
     * Gets order terms.
     * @return the order terms
     */
    public List<GenericValue> getOrderTerms() {
        try {
            return orderHeader.getRelated("OrderTerm", null, null, false);
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return null;
        }
    }

    /**
     * Return the number of days from termDays of first FIN_PAYMENT_TERM
     * @return number of days from termDays of first FIN_PAYMENT_TERM
     */
    public Long getOrderTermNetDays() {
        List<GenericValue> orderTerms = EntityUtil.filterByAnd(getOrderTerms(), UtilMisc.toMap("termTypeId", "FIN_PAYMENT_TERM"));
        if (UtilValidate.isEmpty(orderTerms)) {
            return null;
        } else if (orderTerms.size() > 1) {
            Debug.logWarning("Found " + orderTerms.size() + " FIN_PAYMENT_TERM order terms for orderId [" + getOrderId()
                    + "], using the first one ", MODULE);
        }
        return orderTerms.get(0).getLong("termDays");
    }

    /**
     * Gets shipping method.
     * @param shipGroupSeqId the ship group seq id
     * @return the shipping method
     */
    public String getShippingMethod(String shipGroupSeqId) {
        try {
            GenericValue shipGroup = orderHeader.getDelegator().findOne("OrderItemShipGroup",
                    UtilMisc.toMap("orderId", orderHeader.getString("orderId"), "shipGroupSeqId", shipGroupSeqId), false);

            if (shipGroup != null) {
                GenericValue carrierShipmentMethod = shipGroup.getRelatedOne("CarrierShipmentMethod", false);

                if (carrierShipmentMethod != null) {
                    GenericValue shipmentMethodType = carrierShipmentMethod.getRelatedOne("ShipmentMethodType", false);

                    if (shipmentMethodType != null) {
                        return UtilFormatOut.checkNull(shipGroup.getString("carrierPartyId")) + " "
                                + UtilFormatOut.checkNull(shipmentMethodType.getString("description"));
                    }
                }
                return UtilFormatOut.checkNull(shipGroup.getString("carrierPartyId"));
            }
        } catch (GenericEntityException e) {
            Debug.logWarning(e, MODULE);
        }
        return "";
    }

    /**
     * Gets shipping method code.
     * @param shipGroupSeqId the ship group seq id
     * @return the shipping method code
     */
    public String getShippingMethodCode(String shipGroupSeqId) {
        try {
            GenericValue shipGroup = orderHeader.getDelegator().findOne("OrderItemShipGroup",
                    UtilMisc.toMap("orderId", orderHeader.getString("orderId"), "shipGroupSeqId", shipGroupSeqId), false);

            if (shipGroup != null) {
                GenericValue carrierShipmentMethod = shipGroup.getRelatedOne("CarrierShipmentMethod", false);

                if (carrierShipmentMethod != null) {
                    GenericValue shipmentMethodType = carrierShipmentMethod.getRelatedOne("ShipmentMethodType", false);

                    if (shipmentMethodType != null) {
                        return UtilFormatOut.checkNull(shipmentMethodType.getString("shipmentMethodTypeId")) + "@"
                                + UtilFormatOut.checkNull(shipGroup.getString("carrierPartyId"));
                    }
                }
                return UtilFormatOut.checkNull(shipGroup.getString("carrierPartyId"));
            }
        } catch (GenericEntityException e) {
            Debug.logWarning(e, MODULE);
        }
        return "";
    }

    /**
     * Has shipping address boolean.
     * @return the boolean
     */
    public boolean hasShippingAddress() {
        return UtilValidate.isNotEmpty(this.getShippingLocations());
    }

    /**
     * Has physical product items boolean.
     * @return the boolean
     * @throws GenericEntityException the generic entity exception
     */
    public boolean hasPhysicalProductItems() throws GenericEntityException {
        for (GenericValue orderItem : this.getOrderItems()) {
            GenericValue product = orderItem.getRelatedOne("Product", true);
            if (product != null) {
                GenericValue productType = product.getRelatedOne("ProductType", true);
                if ("Y".equals(productType.getString("isPhysical"))) {
                    return true;
                }
            }
        }
        return false;
    }
    /**
     * Gets order item ship group.
     * @param shipGroupSeqId the ship group seq id
     * @return the order item ship group
     */
    public GenericValue getOrderItemShipGroup(String shipGroupSeqId) {
        try {
            return orderHeader.getDelegator().findOne("OrderItemShipGroup",
                    UtilMisc.toMap("orderId", orderHeader.getString("orderId"), "shipGroupSeqId", shipGroupSeqId), false);
        } catch (GenericEntityException e) {
            Debug.logWarning(e, MODULE);
        }
        return null;
    }

    /**
     * Gets order item ship groups.
     * @return the order item ship groups
     */
    public List<GenericValue> getOrderItemShipGroups() {
        try {
            return orderHeader.getRelated("OrderItemShipGroup", null, UtilMisc.toList("shipGroupSeqId"), false);
        } catch (GenericEntityException e) {
            Debug.logWarning(e, MODULE);
        }
        return null;
    }

    /**
     * Gets shipping locations.
     * @return the shipping locations
     */
    public List<GenericValue> getShippingLocations() {
        List<GenericValue> shippingLocations = new LinkedList<>();
        List<GenericValue> shippingCms = this.getOrderContactMechs("SHIPPING_LOCATION");
        if (shippingCms != null) {
            for (GenericValue ocm : shippingCms) {
                if (ocm != null) {
                    try {
                        GenericValue addr = ocm.getDelegator().findOne("PostalAddress",
                                UtilMisc.toMap("contactMechId", ocm.getString("contactMechId")), false);
                        if (addr != null) {
                            shippingLocations.add(addr);
                        }
                    } catch (GenericEntityException e) {
                        Debug.logWarning(e, MODULE);
                    }
                }
            }
        }
        return shippingLocations;
    }

    /**
     * Gets shipping address.
     * @param shipGroupSeqId the ship group seq id
     * @return the shipping address
     */
    public GenericValue getShippingAddress(String shipGroupSeqId) {
        try {
            GenericValue shipGroup = orderHeader.getDelegator().findOne("OrderItemShipGroup",
                    UtilMisc.toMap("orderId", orderHeader.getString("orderId"), "shipGroupSeqId", shipGroupSeqId), false);

            if (shipGroup != null) {
                return shipGroup.getRelatedOne("PostalAddress", false);

            }
        } catch (GenericEntityException e) {
            Debug.logWarning(e, MODULE);
        }
        return null;
    }

    /**
     * Gets shipping address.
     * @return the shipping address
     * @deprecated
     */
    @Deprecated
    public GenericValue getShippingAddress() {
        try {
            GenericValue orderContactMech = EntityUtil.getFirst(orderHeader.getRelated("OrderContactMech",
                    UtilMisc.toMap("contactMechPurposeTypeId", "SHIPPING_LOCATION"), null, false));

            if (orderContactMech != null) {
                GenericValue contactMech = orderContactMech.getRelatedOne("ContactMech", false);

                if (contactMech != null) {
                    return contactMech.getRelatedOne("PostalAddress", false);
                }
            }
        } catch (GenericEntityException e) {
            Debug.logWarning(e, MODULE);
        }
        return null;
    }

    /**
     * Gets billing locations.
     * @return the billing locations
     */
    public List<GenericValue> getBillingLocations() {
        List<GenericValue> billingLocations = new LinkedList<>();
        List<GenericValue> billingCms = this.getOrderContactMechs("BILLING_LOCATION");
        if (billingCms != null) {
            for (GenericValue ocm : billingCms) {
                if (ocm != null) {
                    try {
                        GenericValue addr = ocm.getDelegator().findOne("PostalAddress",
                                UtilMisc.toMap("contactMechId", ocm.getString("contactMechId")), false);
                        if (addr != null) {
                            billingLocations.add(addr);
                        }
                    } catch (GenericEntityException e) {
                        Debug.logWarning(e, MODULE);
                    }
                }
            }
        }
        return billingLocations;
    }

    /**
     * Gets billing address.
     * @return the billing address
     * @deprecated
     */
    @Deprecated
    public GenericValue getBillingAddress() {
        GenericValue billingAddress = null;
        try {
            GenericValue orderContactMech = EntityUtil.getFirst(orderHeader.getRelated("OrderContactMech",
                    UtilMisc.toMap("contactMechPurposeTypeId", "BILLING_LOCATION"), null, false));

            if (orderContactMech != null) {
                GenericValue contactMech = orderContactMech.getRelatedOne("ContactMech", false);

                if (contactMech != null) {
                    billingAddress = contactMech.getRelatedOne("PostalAddress", false);
                }
            }
        } catch (GenericEntityException e) {
            Debug.logWarning(e, MODULE);
        }

        if (billingAddress == null) {
            // get the address from the billing account
            GenericValue billingAccount = getBillingAccount();
            if (billingAccount != null) {
                try {
                    billingAddress = billingAccount.getRelatedOne("PostalAddress", false);
                } catch (GenericEntityException e) {
                    Debug.logWarning(e, MODULE);
                }
            } else {
                // get the address from the first payment method
                GenericValue paymentPreference = EntityUtil.getFirst(getPaymentPreferences());
                if (paymentPreference != null) {
                    try {
                        GenericValue paymentMethod = paymentPreference.getRelatedOne("PaymentMethod", false);
                        if (paymentMethod != null) {
                            GenericValue creditCard = paymentMethod.getRelatedOne("CreditCard", false);
                            if (creditCard != null) {
                                billingAddress = creditCard.getRelatedOne("PostalAddress", false);
                            } else {
                                GenericValue eftAccount = paymentMethod.getRelatedOne("EftAccount", false);
                                if (eftAccount != null) {
                                    billingAddress = eftAccount.getRelatedOne("PostalAddress", false);
                                }
                            }
                        }
                    } catch (GenericEntityException e) {
                        Debug.logWarning(e, MODULE);
                    }
                }
            }
        }
        return billingAddress;
    }

    /**
     * Gets order contact mechs.
     * @param purposeTypeId the purpose type id
     * @return the order contact mechs
     */
    public List<GenericValue> getOrderContactMechs(String purposeTypeId) {
        try {
            return orderHeader.getRelated("OrderContactMech", UtilMisc.toMap("contactMechPurposeTypeId", purposeTypeId), null, false);
        } catch (GenericEntityException e) {
            Debug.logWarning(e, MODULE);
        }
        return null;
    }

    /**
     * Gets earliest ship by date.
     * @return the earliest ship by date
     */
    public Timestamp getEarliestShipByDate() {
        try {
            List<GenericValue> groups = orderHeader.getRelated("OrderItemShipGroup", null, UtilMisc.toList("shipByDate"), false);
            if (!groups.isEmpty()) {
                GenericValue group = groups.get(0);
                return group.getTimestamp("shipByDate");
            }
        } catch (GenericEntityException e) {
            Debug.logWarning(e, MODULE);
        }
        return null;
    }

    /**
     * Gets latest ship after date.
     * @return the latest ship after date
     */
    public Timestamp getLatestShipAfterDate() {
        try {
            List<GenericValue> groups = orderHeader.getRelated("OrderItemShipGroup", null, UtilMisc.toList("shipAfterDate DESC"), false);
            if (!groups.isEmpty()) {
                GenericValue group = groups.get(0);
                return group.getTimestamp("shipAfterDate");
            }
        } catch (GenericEntityException e) {
            Debug.logWarning(e, MODULE);
        }
        return null;
    }

    /**
     * Gets current status string.
     * @return the current status string
     */
    public String getCurrentStatusString() {
        GenericValue statusItem = null;
        try {
            statusItem = orderHeader.getRelatedOne("StatusItem", true);
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
        }
        if (statusItem != null) {
            return statusItem.getString("description");
        }
        return orderHeader.getString("statusId");
    }

    /**
     * Gets status string.
     * @param locale the locale
     * @return the status string
     */
    public String getStatusString(Locale locale) {
        List<GenericValue> orderStatusList = this.getOrderHeaderStatuses();

        if (UtilValidate.isEmpty(orderStatusList)) {
            return "";
        }

        Iterator<GenericValue> orderStatusIter = orderStatusList.iterator();
        StringBuilder orderStatusString = new StringBuilder(50);

        try {
            boolean isCurrent = true;
            while (orderStatusIter.hasNext()) {
                GenericValue orderStatus = orderStatusIter.next();
                GenericValue statusItem = orderStatus.getRelatedOne("StatusItem", true);

                if (statusItem != null) {
                    orderStatusString.append(statusItem.get("description", locale));
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
            Debug.logError(e, "Error getting Order Status information: " + e.toString(), MODULE);
        }

        return orderStatusString.toString();
    }

    /**
     * Gets billing account.
     * @return the billing account
     */
    public GenericValue getBillingAccount() {
        GenericValue billingAccount = null;
        try {
            billingAccount = orderHeader.getRelatedOne("BillingAccount", false);
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
        }
        return billingAccount;
    }

    /**
     * Returns the OrderPaymentPreference.maxAmount for the billing account associated with the order, or 0 if there is no
     * billing account or no max amount set
     * @return the billing account max amount
     */
    public BigDecimal getBillingAccountMaxAmount() {
        if (getBillingAccount() == null) {
            return BigDecimal.ZERO;
        }
        List<GenericValue> paymentPreferences = null;
        try {
            Delegator delegator = orderHeader.getDelegator();
            paymentPreferences = EntityQuery.use(delegator).from("OrderPurchasePaymentSummary")
                    .where("orderId", orderHeader.get("orderId")).queryList();
        } catch (GenericEntityException e) {
            Debug.logWarning(e, MODULE);
        }
        List<EntityExpr> exprs = UtilMisc.toList(
                EntityCondition.makeCondition("paymentMethodTypeId", "EXT_BILLACT"),
                EntityCondition.makeCondition("preferenceStatusId", EntityOperator.NOT_EQUAL, "PAYMENT_CANCELLED"));
        GenericValue billingAccountPaymentPreference = EntityUtil.getFirst(EntityUtil.filterByAnd(paymentPreferences, exprs));
        if ((billingAccountPaymentPreference != null) && (billingAccountPaymentPreference.getBigDecimal("maxAmount") != null)) {
            return billingAccountPaymentPreference.getBigDecimal("maxAmount");
        }
        return BigDecimal.ZERO;
    }

    /**
     * Returns party from OrderRole of BILL_TO_CUSTOMER
     * @return the bill to party
     */
    public GenericValue getBillToParty() {
        return this.getPartyFromRole("BILL_TO_CUSTOMER");
    }

    /**
     * Returns party from OrderRole of BILL_FROM_VENDOR
     * @return the bill from party
     */
    public GenericValue getBillFromParty() {
        return this.getPartyFromRole("BILL_FROM_VENDOR");
    }

    /**
     * Returns party from OrderRole of SHIP_TO_CUSTOMER
     * @return the ship to party
     */
    public GenericValue getShipToParty() {
        return this.getPartyFromRole("SHIP_TO_CUSTOMER");
    }

    /**
     * Returns party from OrderRole of PLACING_CUSTOMER
     * @return the placing party
     */
    public GenericValue getPlacingParty() {
        return this.getPartyFromRole("PLACING_CUSTOMER");
    }

    /**
     * Returns party from OrderRole of END_USER_CUSTOMER
     * @return the end user party
     */
    public GenericValue getEndUserParty() {
        return this.getPartyFromRole("END_USER_CUSTOMER");
    }

    /**
     * Returns party from OrderRole of SUPPLIER_AGENT
     * @return the supplier agent
     */
    public GenericValue getSupplierAgent() {
        return this.getPartyFromRole("SUPPLIER_AGENT");
    }

    /**
     * Gets party from role.
     * @param roleTypeId the role type id
     * @return the party from role
     */
    public GenericValue getPartyFromRole(String roleTypeId) {
        Delegator delegator = orderHeader.getDelegator();
        GenericValue partyObject = null;
        try {
            GenericValue orderRole = EntityUtil.getFirst(orderHeader.getRelated("OrderRole", UtilMisc.toMap("roleTypeId", roleTypeId), null, false));

            if (orderRole != null) {
                partyObject = EntityQuery.use(delegator).from("Person").where("partyId", orderRole.getString("partyId")).queryOne();

                if (partyObject == null) {
                    partyObject = EntityQuery.use(delegator).from("PartyGroup").where("partyId", orderRole.getString("partyId")).queryOne();
                }
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
        }
        return partyObject;
    }

    /**
     * Gets distributor id.
     * @return the distributor id
     */
    public String getDistributorId() {
        try {
            GenericEntity distributorRole = EntityUtil.getFirst(orderHeader.getRelated("OrderRole",
                    UtilMisc.toMap("roleTypeId", "DISTRIBUTOR"), null, false));

            return distributorRole == null ? null : distributorRole.getString("partyId");
        } catch (GenericEntityException e) {
            Debug.logWarning(e, MODULE);
        }
        return null;
    }

    /**
     * Gets affiliate id.
     * @return the affiliate id
     */
    public String getAffiliateId() {
        try {
            GenericEntity distributorRole = EntityUtil.getFirst(orderHeader.getRelated("OrderRole",
                    UtilMisc.toMap("roleTypeId", "AFFILIATE"), null, false));

            return distributorRole == null ? null : distributorRole.getString("partyId");
        } catch (GenericEntityException e) {
            Debug.logWarning(e, MODULE);
        }
        return null;
    }

    /**
     * Gets shipping total.
     * @return the shipping total
     */
    public BigDecimal getShippingTotal() {
        return OrderReadHelper.calcOrderAdjustments(getOrderHeaderAdjustments(), getOrderItemsSubTotal(), false, false, true);
    }

    /**
     * Gets header tax total.
     * @return the header tax total
     */
    public BigDecimal getHeaderTaxTotal() {
        return OrderReadHelper.calcOrderAdjustments(getOrderHeaderAdjustments(), getOrderItemsSubTotal(), false, true, false);
    }

    /**
     * Gets tax total.
     * @return the tax total
     */
    public BigDecimal getTaxTotal() {
        return OrderReadHelper.calcOrderAdjustments(getAdjustments(), getOrderItemsSubTotal(), false, true, false);
    }

    /**
     * Gets item feature set.
     * @param item the item
     * @return the item feature set
     */
    public Set<String> getItemFeatureSet(GenericValue item) {
        Set<String> featureSet = new LinkedHashSet<>();
        List<GenericValue> featureAppls = null;
        if (item.get("productId") != null) {
            try {
                featureAppls = item.getDelegator().findByAnd("ProductFeatureAppl",
                        UtilMisc.toMap("productId", item.getString("productId")), null, true);
                List<EntityExpr> filterExprs = UtilMisc.toList(EntityCondition.makeCondition("productFeatureApplTypeId",
                        EntityOperator.EQUALS, "STANDARD_FEATURE"));
                filterExprs.add(EntityCondition.makeCondition("productFeatureApplTypeId", EntityOperator.EQUALS, "REQUIRED_FEATURE"));
                featureAppls = EntityUtil.filterByOr(featureAppls, filterExprs);
            } catch (GenericEntityException e) {
                Debug.logError(e, "Unable to get ProductFeatureAppl for item : " + item, MODULE);
            }
            if (featureAppls != null) {
                for (GenericValue appl : featureAppls) {
                    featureSet.add(appl.getString("productFeatureId"));
                }
            }
        }

        // get the ADDITIONAL_FEATURE adjustments
        List<GenericValue> additionalFeatures = null;
        try {
            additionalFeatures = item.getRelated("OrderAdjustment", UtilMisc.toMap("orderAdjustmentTypeId", "ADDITIONAL_FEATURE"), null, false);
        } catch (GenericEntityException e) {
            Debug.logError(e, "Unable to get OrderAdjustment from item : " + item, MODULE);
        }
        if (additionalFeatures != null) {
            for (GenericValue adj : additionalFeatures) {
                String featureId = adj.getString("productFeatureId");
                if (featureId != null) {
                    featureSet.add(featureId);
                }
            }
        }

        return featureSet;
    }

    /**
     * Gets feature id qty map.
     * @param shipGroupSeqId the ship group seq id
     * @return the feature id qty map
     */
    public Map<String, BigDecimal> getFeatureIdQtyMap(String shipGroupSeqId) {
        Map<String, BigDecimal> featureMap = new HashMap<>();
        List<GenericValue> validItems = getValidOrderItems(shipGroupSeqId);
        if (validItems != null) {
            for (GenericValue item : validItems) {
                List<GenericValue> featureAppls = null;
                if (item.get("productId") != null) {
                    try {
                        featureAppls = ProductWorker.getProductFeaturesApplIncludeMarketingPackage(
                                item.getRelatedOne("Product", true));
                    } catch (GenericEntityException e) {
                        Debug.logError(e, "Unable to get ProductFeatureAppl for item : " + item, MODULE);
                    }
                    if (featureAppls != null) {
                        for (GenericValue appl : featureAppls) {
                            BigDecimal lastQuantity = featureMap.get(appl.getString("productFeatureId"));
                            if (lastQuantity == null) {
                                lastQuantity = BigDecimal.ZERO;
                            }
                            BigDecimal newQuantity = lastQuantity.add(getOrderItemQuantity(item));
                            featureMap.put(appl.getString("productFeatureId"), newQuantity);
                        }
                    }
                }

                // get the ADDITIONAL_FEATURE adjustments
                List<GenericValue> additionalFeatures = null;
                try {
                    additionalFeatures = item.getRelated("OrderAdjustment", UtilMisc.toMap("orderAdjustmentTypeId", "ADDITIONAL_FEATURE"),
                            null, false);
                } catch (GenericEntityException e) {
                    Debug.logError(e, "Unable to get OrderAdjustment from item : " + item, MODULE);
                }
                if (additionalFeatures != null) {
                    for (GenericValue adj : additionalFeatures) {
                        String featureId = adj.getString("productFeatureId");
                        if (featureId != null) {
                            BigDecimal lastQuantity = featureMap.get(featureId);
                            if (lastQuantity == null) {
                                lastQuantity = BigDecimal.ZERO;
                            }
                            BigDecimal newQuantity = lastQuantity.add(getOrderItemQuantity(item));
                            featureMap.put(featureId, newQuantity);
                        }
                    }
                }
            }
        }

        return featureMap;
    }

    /**
     * Shipping applies boolean.
     * @return the boolean
     */
    public boolean shippingApplies() {
        boolean shippingApplies = false;
        List<GenericValue> validItems = this.getValidOrderItems();
        if (validItems != null) {
            for (GenericValue item : validItems) {
                GenericValue product = null;
                try {
                    product = item.getRelatedOne("Product", false);
                } catch (GenericEntityException e) {
                    Debug.logError(e, "Problem getting Product from OrderItem; returning 0", MODULE);
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

    /**
     * Tax applies boolean.
     * @return the boolean
     */
    public boolean taxApplies() {
        boolean taxApplies = false;
        List<GenericValue> validItems = this.getValidOrderItems();
        if (validItems != null) {
            for (GenericValue item : validItems) {
                GenericValue product = null;
                try {
                    product = item.getRelatedOne("Product", false);
                } catch (GenericEntityException e) {
                    Debug.logError(e, "Problem getting Product from OrderItem; returning 0", MODULE);
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

    /**
     * Gets shippable total.
     * @param shipGroupSeqId the ship group seq id
     * @return the shippable total
     */
    public BigDecimal getShippableTotal(String shipGroupSeqId) {
        BigDecimal shippableTotal = ZERO;
        List<GenericValue> validItems = getValidOrderItems(shipGroupSeqId);
        if (validItems != null) {
            for (GenericValue item : validItems) {
                GenericValue product = null;
                try {
                    product = item.getRelatedOne("Product", false);
                } catch (GenericEntityException e) {
                    Debug.logError(e, "Problem getting Product from OrderItem; returning 0", MODULE);
                    return ZERO;
                }
                if (product != null) {
                    if (ProductWorker.shippingApplies(product)) {
                        shippableTotal = shippableTotal.add(OrderReadHelper.getOrderItemSubTotal(item, getAdjustments(), false, true))
                                .setScale(DECIMALS, ROUNDING);
                    }
                }
            }
        }
        return shippableTotal.setScale(DECIMALS, ROUNDING);
    }

    /**
     * Gets shippable quantity.
     * @return the shippable quantity
     */
    public BigDecimal getShippableQuantity() {
        BigDecimal shippableQuantity = ZERO;
        List<GenericValue> shipGroups = getOrderItemShipGroups();
        if (UtilValidate.isNotEmpty(shipGroups)) {
            for (GenericValue shipGroup : shipGroups) {
                shippableQuantity = shippableQuantity.add(getShippableQuantity(shipGroup.getString("shipGroupSeqId")));
            }
        }
        return shippableQuantity.setScale(DECIMALS, ROUNDING);
    }

    /**
     * Gets shippable quantity.
     * @param shipGroupSeqId the ship group seq id
     * @return the shippable quantity
     */
    public BigDecimal getShippableQuantity(String shipGroupSeqId) {
        BigDecimal shippableQuantity = ZERO;
        List<GenericValue> validItems = getValidOrderItems(shipGroupSeqId);
        if (validItems != null) {
            for (GenericValue item : validItems) {
                GenericValue product = null;
                try {
                    product = item.getRelatedOne("Product", false);
                } catch (GenericEntityException e) {
                    Debug.logError(e, "Problem getting Product from OrderItem; returning 0", MODULE);
                    return ZERO;
                }
                if (product != null) {
                    if (ProductWorker.shippingApplies(product)) {
                        shippableQuantity = shippableQuantity.add(getOrderItemQuantity(item)).setScale(DECIMALS, ROUNDING);
                    }
                }
            }
        }
        return shippableQuantity.setScale(DECIMALS, ROUNDING);
    }

    /**
     * Gets shippable weight.
     * @param shipGroupSeqId the ship group seq id
     * @return the shippable weight
     */
    public BigDecimal getShippableWeight(String shipGroupSeqId) {
        BigDecimal shippableWeight = ZERO;
        List<GenericValue> validItems = getValidOrderItems(shipGroupSeqId);
        if (validItems != null) {
            for (GenericValue item : validItems) {
                shippableWeight = shippableWeight.add(this.getItemWeight(item).multiply(getOrderItemQuantity(item))).setScale(DECIMALS, ROUNDING);
            }
        }

        return shippableWeight.setScale(DECIMALS, ROUNDING);
    }

    /**
     * Gets item weight.
     * @param item the item
     * @return the item weight
     */
    public BigDecimal getItemWeight(GenericValue item) {
        Delegator delegator = orderHeader.getDelegator();
        BigDecimal itemWeight = ZERO;

        GenericValue product = null;
        try {
            product = item.getRelatedOne("Product", false);
        } catch (GenericEntityException e) {
            Debug.logError(e, "Problem getting Product from OrderItem; returning 0", MODULE);
            return BigDecimal.ZERO;
        }
        if (product != null) {
            if (ProductWorker.shippingApplies(product)) {
                BigDecimal weight = product.getBigDecimal("shippingWeight");
                String isVariant = product.getString("isVariant");
                if (weight == null && "Y".equals(isVariant)) {
                    // get the virtual product and check its weight
                    try {
                        String virtualId = ProductWorker.getVariantVirtualId(product);
                        if (UtilValidate.isNotEmpty(virtualId)) {
                            GenericValue virtual = EntityQuery.use(delegator).from("Product").where("productId", virtualId).cache().queryOne();
                            if (virtual != null) {
                                weight = virtual.getBigDecimal("shippingWeight");
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

    /**
     * Gets shippable sizes.
     * @return the shippable sizes
     */
    public List<BigDecimal> getShippableSizes() {
        List<BigDecimal> shippableSizes = new LinkedList<>();

        List<GenericValue> validItems = getValidOrderItems();
        if (validItems != null) {
            for (GenericValue item : validItems) {
                shippableSizes.add(this.getItemSize(item));
            }
        }
        return shippableSizes;
    }

    /**
     * Get the total payment preference amount by payment type.  Specify null to get amount
     * for all preference types.  TODO: filter by status as well?
     * @param paymentMethodTypeId the payment method type id
     * @return the order payment preference total by type
     */
    public BigDecimal getOrderPaymentPreferenceTotalByType(String paymentMethodTypeId) {
        BigDecimal total = ZERO;
        for (GenericValue preference : getPaymentPreferences()) {
            if (preference.get("maxAmount") == null) {
                continue;
            }
            if (paymentMethodTypeId == null || paymentMethodTypeId.equals(preference.get("paymentMethodTypeId"))) {
                total = total.add(preference.getBigDecimal("maxAmount")).setScale(DECIMALS, ROUNDING);
            }
        }
        return total;
    }

    /**
     * Gets credit card payment preference total.
     * @return the credit card payment preference total
     */
    public BigDecimal getCreditCardPaymentPreferenceTotal() {
        return getOrderPaymentPreferenceTotalByType("CREDIT_CARD");
    }

    /**
     * Gets billing account payment preference total.
     * @return the billing account payment preference total
     */
    public BigDecimal getBillingAccountPaymentPreferenceTotal() {
        return getOrderPaymentPreferenceTotalByType("EXT_BILLACT");
    }

    /**
     * Gets gift card payment preference total.
     * @return the gift card payment preference total
     */
    public BigDecimal getGiftCardPaymentPreferenceTotal() {
        return getOrderPaymentPreferenceTotalByType("GIFT_CARD");
    }

    /**
     * Get the total payment received amount by payment type.  Specify null to get amount
     * over all types. This method works by going through all the PaymentAndApplications
     * for all order Invoices that have status PMNT_RECEIVED.
     * @param paymentMethodTypeId the payment method type id
     * @return the order payment received total by type
     */
    public BigDecimal getOrderPaymentReceivedTotalByType(String paymentMethodTypeId) {
        BigDecimal total = ZERO;

        try {
            // get a set of invoice IDs that belong to the order
            List<GenericValue> orderItemBillings = orderHeader.getRelated("OrderItemBilling", null, null, false);
            Set<String> invoiceIds = new HashSet<>();
            for (GenericValue orderItemBilling : orderItemBillings) {
                invoiceIds.add(orderItemBilling.getString("invoiceId"));
            }

            // get the payments of the desired type for these invoices TODO: in models where invoices can have many orders, this needs to be refined
            List<EntityExpr> conditions = UtilMisc.toList(
                    EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, "PMNT_RECEIVED"),
                    EntityCondition.makeCondition("invoiceId", EntityOperator.IN, invoiceIds));
            if (paymentMethodTypeId != null) {
                conditions.add(EntityCondition.makeCondition("paymentMethodTypeId", EntityOperator.EQUALS, paymentMethodTypeId));
            }
            EntityConditionList<EntityExpr> ecl = EntityCondition.makeCondition(conditions, EntityOperator.AND);
            List<GenericValue> payments = orderHeader.getDelegator().findList("PaymentAndApplication", ecl, null, null, null, true);

            for (GenericValue payment : payments) {
                if (payment.get("amountApplied") == null) {
                    continue;
                }
                total = total.add(payment.getBigDecimal("amountApplied")).setScale(DECIMALS, ROUNDING);
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, e.getMessage(), MODULE);
        }
        return total;
    }

    /**
     * Gets item size.
     * @param item the item
     * @return the item size
     */
    public BigDecimal getItemSize(GenericValue item) {
        Delegator delegator = orderHeader.getDelegator();
        BigDecimal size = BigDecimal.ZERO;

        GenericValue product = null;
        try {
            product = item.getRelatedOne("Product", false);
        } catch (GenericEntityException e) {
            Debug.logError(e, "Problem getting Product from OrderItem", MODULE);
            return BigDecimal.ZERO;
        }
        if (product != null) {
            if (ProductWorker.shippingApplies(product)) {
                BigDecimal height = product.getBigDecimal("shippingHeight");
                BigDecimal width = product.getBigDecimal("shippingWidth");
                BigDecimal depth = product.getBigDecimal("shippingDepth");
                String isVariant = product.getString("isVariant");
                if ((height == null || width == null || depth == null) && "Y".equals(isVariant)) {
                    // get the virtual product and check its values
                    try {
                        String virtualId = ProductWorker.getVariantVirtualId(product);
                        if (UtilValidate.isNotEmpty(virtualId)) {
                            GenericValue virtual = EntityQuery.use(delegator).from("Product").where("productId", virtualId).cache().queryOne();
                            if (virtual != null) {
                                if (height == null) {
                                    height = virtual.getBigDecimal("shippingHeight");
                                }
                                if (width == null) {
                                    width = virtual.getBigDecimal("shippingWidth");
                                }
                                if (depth == null) {
                                    depth = virtual.getBigDecimal("shippingDepth");
                                }
                            }
                        }
                    } catch (GenericEntityException e) {
                        Debug.logError(e, "Problem getting virtual product");
                    }
                }

                if (height == null) {
                    height = BigDecimal.ZERO;
                }
                if (width == null) {
                    width = BigDecimal.ZERO;
                }
                if (depth == null) {
                    depth = BigDecimal.ZERO;
                }

                // determine girth (longest field is length)
                BigDecimal[] sizeInfo = {height, width, depth };
                Arrays.sort(sizeInfo);

                size = sizeInfo[0].multiply(new BigDecimal("2")).add(sizeInfo[1].multiply(new BigDecimal("2"))).add(sizeInfo[2]);
            }
        }

        return size;
    }

    /**
     * Gets item pieces included.
     * @param item the item
     * @return the item pieces included
     */
    public long getItemPiecesIncluded(GenericValue item) {
        Delegator delegator = orderHeader.getDelegator();
        long piecesIncluded = 1;

        GenericValue product = null;
        try {
            product = item.getRelatedOne("Product", false);
        } catch (GenericEntityException e) {
            Debug.logError(e, "Problem getting Product from OrderItem; returning 1", MODULE);
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
                        virtual = EntityQuery.use(delegator).from("ProductAssoc")
                                .where("productIdTo", product.get("productId"),
                                        "productAssocTypeId", "PRODUCT_VARIANT")
                                .orderBy("-fromDate")
                                .filterByDate().queryFirst();
                    } catch (GenericEntityException e) {
                        Debug.logError(e, "Problem getting virtual product");
                    }
                    if (virtual != null) {
                        try {
                            GenericValue virtualProduct = virtual.getRelatedOne("MainProduct", false);
                            pieces = virtualProduct.getLong("piecesIncluded");
                        } catch (GenericEntityException e) {
                            Debug.logError(e, "Problem getting virtual product");
                        }
                    }
                }

                if (pieces != null) {
                    piecesIncluded = pieces;
                }
            }
        }

        return piecesIncluded;
    }

    /**
     * Gets shippable item info.
     * @param shipGroupSeqId the ship group seq id
     * @return the shippable item info
     */
    public List<Map<String, Object>> getShippableItemInfo(String shipGroupSeqId) {
        List<Map<String, Object>> shippableInfo = new LinkedList<>();

        List<GenericValue> validItems = getValidOrderItems(shipGroupSeqId);
        if (validItems != null) {
            for (GenericValue item : validItems) {
                shippableInfo.add(this.getItemInfoMap(item));
            }
        }

        return shippableInfo;
    }

    /**
     * Gets item info map.
     * @param item the item
     * @return the item info map
     */
    public Map<String, Object> getItemInfoMap(GenericValue item) {
        Map<String, Object> itemInfo = new HashMap<>();
        itemInfo.put("productId", item.getString("productId"));
        itemInfo.put("quantity", getOrderItemQuantity(item));
        itemInfo.put("weight", this.getItemWeight(item));
        itemInfo.put("size", this.getItemSize(item));
        itemInfo.put("piecesIncluded", this.getItemPiecesIncluded(item));
        itemInfo.put("featureSet", this.getItemFeatureSet(item));
        return itemInfo;
    }

    /**
     * Gets order email string.
     * @return the order email string
     */
    public String getOrderEmailString() {
        Delegator delegator = orderHeader.getDelegator();
        // get the email addresses from the order contact mech(s)
        List<GenericValue> orderContactMechs = null;
        try {
            orderContactMechs = EntityQuery.use(delegator).from("OrderContactMech")
                    .where("orderId", orderHeader.get("orderId"),
                            "contactMechPurposeTypeId", "ORDER_EMAIL")
                    .queryList();
        } catch (GenericEntityException e) {
            Debug.logWarning(e, "Problems getting order contact mechs", MODULE);
        }

        StringBuilder emails = new StringBuilder();
        if (orderContactMechs != null) {
            for (GenericValue orderContactMech : orderContactMechs) {
                try {
                    GenericValue contactMech = orderContactMech.getRelatedOne("ContactMech", false);
                    emails.append(emails.length() > 0 ? "," : "").append(contactMech.getString("infoString"));
                } catch (GenericEntityException e) {
                    Debug.logWarning(e, "Problems getting contact mech from order contact mech", MODULE);
                }
            }
        }
        return emails.toString();
    }

    /**
     * Gets order grand total.
     * @return the order grand total
     */
    public BigDecimal getOrderGrandTotal() {
        if (totalPrice == null) {
            totalPrice = getOrderGrandTotal(getValidOrderItems(), getAdjustments());
        } // else already set
        return totalPrice;
    }

    /**
     * Gets the amount open on the order that is not covered by the relevant OrderPaymentPreferences.
     * This works by adding up the amount allocated to each unprocessed OrderPaymentPreference and the
     * amounts received and refunded as payments for the settled ones.
     * @return the order open amount
     * @throws GenericEntityException the generic entity exception
     */
    public BigDecimal getOrderOpenAmount() throws GenericEntityException {
        BigDecimal total = getOrderGrandTotal();
        BigDecimal openAmount = BigDecimal.ZERO;
        List<GenericValue> prefs = getPaymentPreferences();

        // add up the covered amount, but skip preferences which are declined or cancelled
        for (GenericValue pref : prefs) {
            if ("PAYMENT_CANCELLED".equals(pref.get("statusId")) || "PAYMENT_DECLINED".equals(pref.get("statusId"))) {
                continue;
            } else if ("PAYMENT_SETTLED".equals(pref.get("statusId"))) {
                List<GenericValue> responses = pref.getRelated("PaymentGatewayResponse", UtilMisc.toMap("transCodeEnumId", "PGT_CAPTURE"), null,
                        false);
                for (GenericValue response : responses) {
                    BigDecimal amount = response.getBigDecimal("amount");
                    if (amount != null) {
                        openAmount = openAmount.add(amount);
                    }
                }
                responses = pref.getRelated("PaymentGatewayResponse", UtilMisc.toMap("transCodeEnumId", "PGT_REFUND"), null, false);
                for (GenericValue response : responses) {
                    BigDecimal amount = response.getBigDecimal("amount");
                    if (amount != null) {
                        openAmount = openAmount.subtract(amount);
                    }
                }
            } else {
                // all others are currently "unprocessed" payment preferences
                BigDecimal maxAmount = pref.getBigDecimal("maxAmount");
                if (maxAmount != null) {
                    openAmount = openAmount.add(maxAmount);
                }
            }
        }
        openAmount = total.subtract(openAmount).setScale(DECIMALS, ROUNDING);
        // return either a positive amount or positive zero
        return openAmount.compareTo(BigDecimal.ZERO) > 0 ? openAmount : BigDecimal.ZERO;
    }

    /**
     * Gets order header adjustments.
     * @return the order header adjustments
     */
    public List<GenericValue> getOrderHeaderAdjustments() {
        return getOrderHeaderAdjustments(getAdjustments(), null);
    }

    /**
     * Gets order header adjustments.
     * @param shipGroupSeqId the ship group seq id
     * @return the order header adjustments
     */
    public List<GenericValue> getOrderHeaderAdjustments(String shipGroupSeqId) {
        return getOrderHeaderAdjustments(getAdjustments(), shipGroupSeqId);
    }

    /**
     * Gets order header adjustments tax.
     * @param shipGroupSeqId the ship group seq id
     * @return the order header adjustments tax
     */
    public List<GenericValue> getOrderHeaderAdjustmentsTax(String shipGroupSeqId) {
        return filterOrderAdjustments(getOrderHeaderAdjustments(getAdjustments(), shipGroupSeqId), false, true, false, false, false);
    }

    /**
     * Gets order header adjustments to show.
     * @return the order header adjustments to show
     */
    public List<GenericValue> getOrderHeaderAdjustmentsToShow() {
        return filterOrderAdjustments(getOrderHeaderAdjustments(), true, false, false, false, false);
    }

    /**
     * Gets order header statuses.
     * @return the order header statuses
     */
    public List<GenericValue> getOrderHeaderStatuses() {
        return getOrderHeaderStatuses(getOrderStatuses());
    }

    /**
     * Gets order adjustments total.
     * @return the order adjustments total
     */
    public BigDecimal getOrderAdjustmentsTotal() {
        return getOrderAdjustmentsTotal(getValidOrderItems(), getAdjustments());
    }

    /**
     * Gets order adjustment total.
     * @param adjustment the adjustment
     * @return the order adjustment total
     */
    public BigDecimal getOrderAdjustmentTotal(GenericValue adjustment) {
        return calcOrderAdjustment(adjustment, getOrderItemsSubTotal());
    }

    /**
     * Has survey int.
     * @return the int
     */
    public int hasSurvey() {
        Delegator delegator = orderHeader.getDelegator();
        List<GenericValue> surveys = null;
        try {
            surveys = EntityQuery.use(delegator).from("SurveyResponse")
                    .where("orderId", orderHeader.get("orderId")).queryList();
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
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

    /**
     * Gets order items.
     * @return the order items
     */
    public List<GenericValue> getOrderItems() {
        if (orderItems == null) {
            try {
                orderItems = orderHeader.getRelated("OrderItem", null, UtilMisc.toList("orderItemSeqId"), false);
            } catch (GenericEntityException e) {
                Debug.logWarning(e, MODULE);
            }
        }
        return orderItems;
    }

    /**
     * Gets order item and ship group assoc.
     * @return the order item and ship group assoc
     */
    public List<GenericValue> getOrderItemAndShipGroupAssoc() {
        if (orderItemAndShipGrp == null) {
            try {
                orderItemAndShipGrp = orderHeader.getDelegator().findByAnd("OrderItemAndShipGroupAssoc",
                        UtilMisc.toMap("orderId", orderHeader.getString("orderId")), null, false);
            } catch (GenericEntityException e) {
                Debug.logWarning(e, MODULE);
            }
        }
        return orderItemAndShipGrp;
    }

    /**
     * Gets order item and ship group assoc.
     * @param shipGroupSeqId the ship group seq id
     * @return the order item and ship group assoc
     */
    public List<GenericValue> getOrderItemAndShipGroupAssoc(String shipGroupSeqId) {
        List<EntityExpr> exprs = UtilMisc.toList(EntityCondition.makeCondition("shipGroupSeqId", EntityOperator.EQUALS, shipGroupSeqId));
        return EntityUtil.filterByAnd(getOrderItemAndShipGroupAssoc(), exprs);
    }

    /**
     * Gets valid order items.
     * @return the valid order items
     */
    public List<GenericValue> getValidOrderItems() {
        List<EntityExpr> exprs = UtilMisc.toList(
                EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "ITEM_CANCELLED"),
                EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "ITEM_REJECTED"));
        return EntityUtil.filterByAnd(getOrderItems(), exprs);
    }

    /**
     * Gets past eta order items.
     * @param orderId the order id
     * @return the past eta order items
     */
    public boolean getPastEtaOrderItems(String orderId) {
        Delegator delegator = orderHeader.getDelegator();
        GenericValue orderDeliverySchedule = null;
        try {
            orderDeliverySchedule = EntityQuery.use(delegator).from("OrderDeliverySchedule").where("orderId", orderId, "orderItemSeqId", "_NA_")
                    .queryOne();
        } catch (GenericEntityException e) {
            if (Debug.infoOn()) {
                Debug.logInfo(" OrderDeliverySchedule not found for order " + orderId, MODULE);
            }
            return false;
        }
        if (orderDeliverySchedule == null) {
            return false;
        }
        Timestamp estimatedShipDate = orderDeliverySchedule.getTimestamp("estimatedReadyDate");
        return estimatedShipDate != null && UtilDateTime.nowTimestamp().after(estimatedShipDate);
    }

    /**
     * Gets rejected order items.
     * @return the rejected order items
     */
    public boolean getRejectedOrderItems() {
        List<GenericValue> items = getOrderItems();
        for (GenericValue item : items) {
            List<GenericValue> receipts = null;
            try {
                receipts = item.getRelated("ShipmentReceipt", null, null, false);
            } catch (GenericEntityException e) {
                Debug.logWarning(e, MODULE);
            }
            if (UtilValidate.isNotEmpty(receipts)) {
                for (GenericValue rec : receipts) {
                    BigDecimal rejected = rec.getBigDecimal("quantityRejected");
                    if (rejected != null && rejected.compareTo(BigDecimal.ZERO) > 0) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Gets partially received items.
     * @return the partially received items
     */
    public boolean getPartiallyReceivedItems() {
        List<GenericValue> items = getOrderItems();
        for (GenericValue item : items) {
            List<GenericValue> receipts = null;
            try {
                receipts = item.getRelated("ShipmentReceipt", null, null, false);
            } catch (GenericEntityException e) {
                Debug.logWarning(e, MODULE);
            }
            if (UtilValidate.isNotEmpty(receipts)) {
                for (GenericValue rec : receipts) {
                    BigDecimal acceptedQuantity = rec.getBigDecimal("quantityAccepted");
                    BigDecimal orderedQuantity = (BigDecimal) item.get("quantity");
                    if (acceptedQuantity.intValue() != orderedQuantity.intValue() && acceptedQuantity.intValue() > 0) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Gets valid order items.
     * @param shipGroupSeqId the ship group seq id
     * @return the valid order items
     */
    public List<GenericValue> getValidOrderItems(String shipGroupSeqId) {
        if (shipGroupSeqId == null) {
            return getValidOrderItems();
        }
        List<EntityExpr> exprs = UtilMisc.toList(
                EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "ITEM_CANCELLED"),
                EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "ITEM_REJECTED"),
                EntityCondition.makeCondition("shipGroupSeqId", EntityOperator.EQUALS, shipGroupSeqId));
        return EntityUtil.filterByAnd(getOrderItemAndShipGroupAssoc(), exprs);
    }

    /**
     * Gets order item.
     * @param orderItemSeqId the order item seq id
     * @return the order item
     */
    public GenericValue getOrderItem(String orderItemSeqId) {
        List<EntityExpr> exprs = UtilMisc.toList(EntityCondition.makeCondition("orderItemSeqId", EntityOperator.EQUALS, orderItemSeqId));
        return EntityUtil.getFirst(EntityUtil.filterByAnd(getOrderItems(), exprs));
    }

    /**
     * Gets valid digital items.
     * @return the valid digital items
     */
    public List<GenericValue> getValidDigitalItems() {
        List<GenericValue> digitalItems = new LinkedList<>();
        // only approved or complete items apply
        List<EntityExpr> exprs = UtilMisc.toList(
                EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, "ITEM_APPROVED"),
                EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, "ITEM_COMPLETED"));
        List<GenericValue> items = EntityUtil.filterByOr(getOrderItems(), exprs);
        for (GenericValue item : items) {
            if (item.get("productId") != null) {
                GenericValue product = null;
                try {
                    product = item.getRelatedOne("Product", false);
                } catch (GenericEntityException e) {
                    Debug.logError(e, "Unable to get Product from OrderItem", MODULE);
                }
                if (product != null) {
                    GenericValue productType = null;
                    try {
                        productType = product.getRelatedOne("ProductType", false);
                    } catch (GenericEntityException e) {
                        Debug.logError(e, "ERROR: Unable to get ProductType from Product", MODULE);
                    }

                    if (productType != null) {
                        String isDigital = productType.getString("isDigital");

                        if (isDigital != null && "Y".equalsIgnoreCase(isDigital)) {
                            // make sure we have an OrderItemBilling record
                            List<GenericValue> orderItemBillings = null;
                            try {
                                orderItemBillings = item.getRelated("OrderItemBilling", null, null, false);
                            } catch (GenericEntityException e) {
                                Debug.logError(e, "Unable to get OrderItemBilling from OrderItem");
                            }

                            if (UtilValidate.isNotEmpty(orderItemBillings)) {
                                // get the ProductContent records
                                List<GenericValue> productContents = null;
                                try {
                                    productContents = product.getRelated("ProductContent", null, null, false);
                                } catch (GenericEntityException e) {
                                    Debug.logError("Unable to get ProductContent from Product", MODULE);
                                }
                                List<EntityExpr> cExprs = UtilMisc.toList(
                                        EntityCondition.makeCondition("productContentTypeId", EntityOperator.EQUALS, "DIGITAL_DOWNLOAD"),
                                        EntityCondition.makeCondition("productContentTypeId", EntityOperator.EQUALS, "FULFILLMENT_EMAIL"),
                                        EntityCondition.makeCondition("productContentTypeId", EntityOperator.EQUALS, "FULFILLMENT_EXTERNAL"));
                                // add more as needed
                                productContents = EntityUtil.filterByDate(productContents);
                                productContents = EntityUtil.filterByOr(productContents, cExprs);

                                if (UtilValidate.isNotEmpty(productContents)) {
                                    // make sure we are still within the allowed timeframe and use limits
                                    for (GenericValue productContent : productContents) {
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

    /**
     * Gets order item adjustments.
     * @param orderItem the order item
     * @return the order item adjustments
     */
    public List<GenericValue> getOrderItemAdjustments(GenericValue orderItem) {
        return getOrderItemAdjustmentList(orderItem, getAdjustments());
    }

    /**
     * Gets current order item work effort.
     * @param orderItem the order item
     * @return the current order item work effort
     */
    public String getCurrentOrderItemWorkEffort(GenericValue orderItem) {
        String orderItemSeqId = orderItem.getString("orderItemSeqId");
        String orderId = orderItem.getString("orderId");
        Delegator delegator = orderItem.getDelegator();
        GenericValue workOrderItemFulFillment = null;
        GenericValue workEffort = null;
        try {
            workOrderItemFulFillment = EntityQuery.use(delegator).from("WorkOrderItemFulfillment")
                    .where("orderId", orderId, "orderItemSeqId", orderItemSeqId)
                    .cache().queryFirst();
            if (workOrderItemFulFillment != null) {
                workEffort = workOrderItemFulFillment.getRelatedOne("WorkEffort", false);
            }
        } catch (GenericEntityException e) {
            return null;
        }
        if (workEffort != null) {
            return workEffort.getString("workEffortId");
        }
        return null;
    }

    /**
     * Gets current item status.
     * @param orderItem the order item
     * @return the current item status
     */
    public String getCurrentItemStatus(GenericValue orderItem) {
        GenericValue statusItem = null;
        try {
            statusItem = orderItem.getRelatedOne("StatusItem", false);
        } catch (GenericEntityException e) {
            Debug.logError(e, "Trouble getting StatusItem : " + orderItem, MODULE);
        }
        if (statusItem == null || statusItem.get("description") == null) {
            return "Not Available";
        }
        return statusItem.getString("description");
    }

    /**
     * Gets order item price infos.
     * @param orderItem the order item
     * @return the order item price infos
     */
    public List<GenericValue> getOrderItemPriceInfos(GenericValue orderItem) {
        if (orderItem == null) {
            return null;
        }
        if (this.orderItemPriceInfos == null) {
            Delegator delegator = orderHeader.getDelegator();

            try {
                orderItemPriceInfos = EntityQuery.use(delegator).from("OrderItemPriceInfo")
                        .where("orderId", orderHeader.get("orderId")).queryList();
            } catch (GenericEntityException e) {
                Debug.logWarning(e, MODULE);
            }
        }
        String orderItemSeqId = (String) orderItem.get("orderItemSeqId");

        return EntityUtil.filterByAnd(this.orderItemPriceInfos, UtilMisc.toMap("orderItemSeqId", orderItemSeqId));
    }

    /**
     * Gets order item ship group assocs.
     * @param orderItem the order item
     * @return the order item ship group assocs
     */
    public List<GenericValue> getOrderItemShipGroupAssocs(GenericValue orderItem) {
        if (orderItem == null) {
            return null;
        }
        try {
            return orderHeader.getDelegator().findByAnd("OrderItemShipGroupAssoc",
                    UtilMisc.toMap("orderId", orderItem.getString("orderId"), "orderItemSeqId", orderItem.getString("orderItemSeqId")),
                    UtilMisc.toList("shipGroupSeqId"), false);
        } catch (GenericEntityException e) {
            Debug.logWarning(e, MODULE);
        }
        return null;
    }

    /**
     * Gets order item ship grp inv res list.
     * @param orderItem the order item
     * @return the order item ship grp inv res list
     */
    public List<GenericValue> getOrderItemShipGrpInvResList(GenericValue orderItem) {
        if (orderItem == null) {
            return null;
        }
        if (this.orderItemShipGrpInvResList == null) {
            Delegator delegator = orderItem.getDelegator();
            try {
                orderItemShipGrpInvResList = EntityQuery.use(delegator).from("OrderItemShipGrpInvRes")
                        .where("orderId", orderItem.get("orderId")).queryList();
            } catch (GenericEntityException e) {
                Debug.logWarning(e, "Trouble getting OrderItemShipGrpInvRes List", MODULE);
            }
        }
        return EntityUtil.filterByAnd(orderItemShipGrpInvResList, UtilMisc.toMap("orderItemSeqId", orderItem.getString("orderItemSeqId")));
    }

    /**
     * Gets order item issuances.
     * @param orderItem the order item
     * @return the order item issuances
     */
    public List<GenericValue> getOrderItemIssuances(GenericValue orderItem) {
        return this.getOrderItemIssuances(orderItem, null);
    }

    /**
     * Gets order item issuances.
     * @param orderItem  the order item
     * @param shipmentId the shipment id
     * @return the order item issuances
     */
    public List<GenericValue> getOrderItemIssuances(GenericValue orderItem, String shipmentId) {
        if (orderItem == null) {
            return null;
        }
        if (this.orderItemIssuances == null) {
            Delegator delegator = orderItem.getDelegator();

            try {
                orderItemIssuances = EntityQuery.use(delegator).from("ItemIssuance")
                        .where("orderId", orderItem.get("orderId")).queryList();
            } catch (GenericEntityException e) {
                Debug.logWarning(e, "Trouble getting ItemIssuance(s)", MODULE);
            }
        }

        // filter the issuances
        Map<String, Object> filter = UtilMisc.toMap("orderItemSeqId", orderItem.get("orderItemSeqId"));
        if (shipmentId != null) {
            filter.put("shipmentId", shipmentId);
        }
        return EntityUtil.filterByAnd(orderItemIssuances, filter);
    }

    /**
     * Get a set of productIds in the order.  @return the order product ids
     */
    public Collection<String> getOrderProductIds() {
        Set<String> productIds = new HashSet<>();
        for (GenericValue orderItem : getOrderItems()) {
            if (orderItem.get("productId") != null) {
                productIds.add(orderItem.getString("productId"));
            }
        }
        return productIds;
    }

    /**
     * Gets order return items.
     * @return the order return items
     */
    public List<GenericValue> getOrderReturnItems() {
        Delegator delegator = orderHeader.getDelegator();
        if (this.orderReturnItems == null) {
            try {
                this.orderReturnItems = EntityQuery.use(delegator).from("ReturnItem").where("orderId", orderHeader.get("orderId")).queryList();
            } catch (GenericEntityException e) {
                Debug.logError(e, "Problem getting ReturnItem from order", MODULE);
                return null;
            }
        }
        return this.orderReturnItems;
    }

    /**
     * Get the quantity returned per order item.
     * In other words, this method will count the ReturnItems
     * related to each OrderItem.
     * @return Map of returned quantities as BigDecimals keyed to the orderItemSeqId
     */
    public Map<String, BigDecimal> getOrderItemReturnedQuantities() {
        List<GenericValue> returnItems = getOrderReturnItems();

        // since we don't have a handy grouped view entity, we'll have to group the return items by hand
        Map<String, BigDecimal> returnMap = new HashMap<>();
        for (GenericValue orderItem : this.getValidOrderItems()) {
            List<GenericValue> group = EntityUtil.filterByAnd(returnItems, UtilMisc.toList(
                    EntityCondition.makeCondition("orderId", orderItem.get("orderId")),
                    EntityCondition.makeCondition("orderItemSeqId", orderItem.get("orderItemSeqId")),
                    EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "RETURN_CANCELLED")));

            // add up the returned quantities for this group TODO: received quantity should be used eventually
            BigDecimal returned = BigDecimal.ZERO;
            for (GenericValue returnItem : group) {
                if (returnItem.getBigDecimal("returnQuantity") != null) {
                    returned = returned.add(returnItem.getBigDecimal("returnQuantity"));
                }
            }

            // the quantity returned per order item
            returnMap.put(orderItem.getString("orderItemSeqId"), returned);
        }
        return returnMap;
    }

    /**
     * Get the total quantity of returned items for an order. This will count
     * only the ReturnItems that are directly correlated to an OrderItem.
     * @return the order returned quantity
     */
    public BigDecimal getOrderReturnedQuantity() {
        List<GenericValue> returnedItemsBase = getOrderReturnItems();
        List<GenericValue> returnedItems = new ArrayList<>(returnedItemsBase.size());

        // filter just order items
        List<EntityExpr> orderItemExprs = UtilMisc.toList(EntityCondition.makeCondition("returnItemTypeId", EntityOperator.EQUALS, "RET_PROD_ITEM"));
        orderItemExprs.add(EntityCondition.makeCondition("returnItemTypeId", EntityOperator.EQUALS, "RET_FPROD_ITEM"));
        orderItemExprs.add(EntityCondition.makeCondition("returnItemTypeId", EntityOperator.EQUALS, "RET_DPROD_ITEM"));
        orderItemExprs.add(EntityCondition.makeCondition("returnItemTypeId", EntityOperator.EQUALS, "RET_FDPROD_ITEM"));
        orderItemExprs.add(EntityCondition.makeCondition("returnItemTypeId", EntityOperator.EQUALS, "RET_PROD_FEATR_ITEM"));
        orderItemExprs.add(EntityCondition.makeCondition("returnItemTypeId", EntityOperator.EQUALS, "RET_SPROD_ITEM"));
        orderItemExprs.add(EntityCondition.makeCondition("returnItemTypeId", EntityOperator.EQUALS, "RET_WE_ITEM"));
        orderItemExprs.add(EntityCondition.makeCondition("returnItemTypeId", EntityOperator.EQUALS, "RET_TE_ITEM"));
        returnedItemsBase = EntityUtil.filterByOr(returnedItemsBase, orderItemExprs);

        // get only the RETURN_RECEIVED and RETURN_COMPLETED statusIds
        returnedItems.addAll(EntityUtil.filterByAnd(returnedItemsBase, UtilMisc.toMap("statusId", "RETURN_RECEIVED")));
        returnedItems.addAll(EntityUtil.filterByAnd(returnedItemsBase, UtilMisc.toMap("statusId", "RETURN_COMPLETED")));

        BigDecimal returnedQuantity = ZERO;
        for (GenericValue returnedItem : returnedItems) {
            if (returnedItem.get("returnQuantity") != null) {
                returnedQuantity = returnedQuantity.add(returnedItem.getBigDecimal("returnQuantity")).setScale(DECIMALS, ROUNDING);
            }
        }
        return returnedQuantity.setScale(DECIMALS, ROUNDING);
    }

    /**
     * Get the returned total by return type (credit, refund, etc.).  Specify returnTypeId = null to get sum over all
     * return types.  Specify includeAll = true to sum up over all return statuses except cancelled.  Specify includeAll
     * = false to sum up over ACCEPTED, RECEIVED And COMPLETED returns.
     * @param returnTypeId the return type id
     * @param includeAll   the include all
     * @return the order returned total by type bd
     */
    public BigDecimal getOrderReturnedTotalByTypeBd(String returnTypeId, boolean includeAll) {
        List<GenericValue> returnedItemsBase = getOrderReturnItems();
        if (returnTypeId != null) {
            returnedItemsBase = EntityUtil.filterByAnd(returnedItemsBase, UtilMisc.toMap("returnTypeId", returnTypeId));
        }
        List<GenericValue> returnedItems = new ArrayList<>(returnedItemsBase.size());

        // get only the RETURN_RECEIVED and RETURN_COMPLETED statusIds
        if (!includeAll) {
            returnedItems.addAll(EntityUtil.filterByAnd(returnedItemsBase, UtilMisc.toMap("statusId", "RETURN_ACCEPTED")));
            returnedItems.addAll(EntityUtil.filterByAnd(returnedItemsBase, UtilMisc.toMap("statusId", "RETURN_RECEIVED")));
            returnedItems.addAll(EntityUtil.filterByAnd(returnedItemsBase, UtilMisc.toMap("statusId", "RETURN_COMPLETED")));
        } else {
            // otherwise get all of them except cancelled ones
            returnedItems.addAll(EntityUtil.filterByAnd(returnedItemsBase,
                    UtilMisc.toList(EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "RETURN_CANCELLED"))));
        }
        BigDecimal returnedAmount = ZERO;
        String orderId = orderHeader.getString("orderId");
        List<String> returnHeaderList = new LinkedList<>();
        for (GenericValue returnedItem : returnedItems) {
            if ((returnedItem.get("returnPrice") != null) && (returnedItem.get("returnQuantity") != null)) {
                returnedAmount = returnedAmount.add(returnedItem.getBigDecimal("returnPrice").multiply(returnedItem.getBigDecimal("returnQuantity"))
                        .setScale(DECIMALS, ROUNDING));
            }
            Map<String, Object> itemAdjustmentCondition = UtilMisc.toMap("returnId", returnedItem.get("returnId"), "returnItemSeqId",
                    returnedItem.get("returnItemSeqId"));
            if (UtilValidate.isNotEmpty(returnTypeId)) {
                itemAdjustmentCondition.put("returnTypeId", returnTypeId);
            }
            returnedAmount = returnedAmount.add(getReturnAdjustmentTotal(orderHeader.getDelegator(), itemAdjustmentCondition));
            if (orderId.equals(returnedItem.getString("orderId")) && (!returnHeaderList.contains(returnedItem.getString("returnId")))) {
                returnHeaderList.add(returnedItem.getString("returnId"));
            }
        }
        //get  returnedAmount from returnHeader adjustments whose orderId must equals to current orderHeader.orderId
        for (String returnId : returnHeaderList) {
            Map<String, Object> returnHeaderAdjFilter = UtilMisc.<String, Object>toMap("returnId", returnId, "returnItemSeqId", "_NA_",
                    "returnTypeId", returnTypeId);
            returnedAmount = returnedAmount.add(getReturnAdjustmentTotal(orderHeader.getDelegator(), returnHeaderAdjFilter))
                    .setScale(DECIMALS, ROUNDING);
        }
        return returnedAmount.setScale(DECIMALS, ROUNDING);
    }

    /**
     * Gets the total return credit for COMPLETED and RECEIVED returns.  @return the order returned credit total bd
     */
    public BigDecimal getOrderReturnedCreditTotalBd() {
        return getOrderReturnedTotalByTypeBd("RTN_CREDIT", false);
    }

    /**
     * Gets the total return refunded for COMPLETED and RECEIVED returns.  @return the order returned refund total bd
     */
    public BigDecimal getOrderReturnedRefundTotalBd() {
        return getOrderReturnedTotalByTypeBd("RTN_REFUND", false);
    }

    /**
     * Gets the total return amount (all return types) for COMPLETED and RECEIVED returns.  @return the order returned total
     */
    public BigDecimal getOrderReturnedTotal() {
        return getOrderReturnedTotalByTypeBd(null, false);
    }

    /**
     * Gets the total returned over all return types.  Specify true to include all return statuses
     * except cancelled.  Specify false to include only COMPLETED and RECEIVED returns.
     * @param includeAll the include all
     * @return the order returned total
     */
    public BigDecimal getOrderReturnedTotal(boolean includeAll) {
        return getOrderReturnedTotalByTypeBd(null, includeAll);
    }

    /**
     * Gets order non returned tax and shipping.
     * @return the order non returned tax and shipping
     */
    public BigDecimal getOrderNonReturnedTaxAndShipping() {
        // first make a Map of orderItemSeqId key, returnQuantity value
        List<GenericValue> returnedItemsBase = getOrderReturnItems();
        List<GenericValue> returnedItems = new ArrayList<>(returnedItemsBase.size());

        // get only the RETURN_RECEIVED and RETURN_COMPLETED statusIds
        returnedItems.addAll(EntityUtil.filterByAnd(returnedItemsBase, UtilMisc.toMap("statusId", "RETURN_RECEIVED")));
        returnedItems.addAll(EntityUtil.filterByAnd(returnedItemsBase, UtilMisc.toMap("statusId", "RETURN_COMPLETED")));

        Map<String, BigDecimal> itemReturnedQuantities = new HashMap<>();
        for (GenericValue returnedItem : returnedItems) {
            String orderItemSeqId = returnedItem.getString("orderItemSeqId");
            BigDecimal returnedQuantity = returnedItem.getBigDecimal("returnQuantity");
            if (orderItemSeqId != null && returnedQuantity != null) {
                BigDecimal existingQuantity = itemReturnedQuantities.get(orderItemSeqId);
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

        for (GenericValue orderItem : this.getValidOrderItems()) {
            BigDecimal itemQuantityDbl = orderItem.getBigDecimal("quantity");
            if (itemQuantityDbl == null || itemQuantityDbl.compareTo(ZERO) == 0) {
                continue;
            }
            BigDecimal itemQuantity = itemQuantityDbl;
            BigDecimal itemSubTotal = this.getOrderItemSubTotal(orderItem);
            BigDecimal itemTaxes = this.getOrderItemTax(orderItem);
            BigDecimal itemShipping = this.getOrderItemShipping(orderItem);

            BigDecimal quantityReturned = itemReturnedQuantities.get(orderItem.get("orderItemSeqId"));
            if (quantityReturned == null) {
                quantityReturned = BigDecimal.ZERO;
            }

            BigDecimal quantityNotReturned = itemQuantity.subtract(quantityReturned);

            // pro-rated factor (quantity not returned / total items ordered), which shouldn't be rounded to 2 decimals
            BigDecimal factorNotReturned = quantityNotReturned.divide(itemQuantity, 100, ROUNDING);

            BigDecimal subTotalNotReturned = itemSubTotal.multiply(factorNotReturned).setScale(DECIMALS, ROUNDING);

            // calculate tax and shipping adjustments for each item, add to accumulators
            BigDecimal itemTaxNotReturned = itemTaxes.multiply(factorNotReturned).setScale(DECIMALS, ROUNDING);
            BigDecimal itemShippingNotReturned = itemShipping.multiply(factorNotReturned).setScale(DECIMALS, ROUNDING);

            totalSubTotalNotReturned = totalSubTotalNotReturned.add(subTotalNotReturned);
            totalTaxNotReturned = totalTaxNotReturned.add(itemTaxNotReturned);
            totalShippingNotReturned = totalShippingNotReturned.add(itemShippingNotReturned);
        }

        // calculate tax and shipping adjustments for entire order, add to result
        BigDecimal orderItemsSubTotal = this.getOrderItemsSubTotal();
        BigDecimal orderFactorNotReturned = ZERO;
        if (orderItemsSubTotal.signum() != 0) {
            // pro-rated factor (subtotal not returned / item subtotal), which shouldn't be rounded to 2 decimals
            orderFactorNotReturned = totalSubTotalNotReturned.divide(orderItemsSubTotal, 100, ROUNDING);
        }
        BigDecimal orderTaxNotReturned = this.getHeaderTaxTotal().multiply(orderFactorNotReturned).setScale(DECIMALS, ROUNDING);
        BigDecimal orderShippingNotReturned = this.getShippingTotal().multiply(orderFactorNotReturned).setScale(DECIMALS, ROUNDING);

        return totalTaxNotReturned.add(totalShippingNotReturned).add(orderTaxNotReturned).add(orderShippingNotReturned).setScale(DECIMALS, ROUNDING);
    }
    /**
     * Gets the total refunded to the order billing account by type. Specify null to get total over all types.
     * @param returnTypeId the return type id
     * @return the billing account returned total by type bd
     */
    public BigDecimal getBillingAccountReturnedTotalByTypeBd(String returnTypeId) {
        BigDecimal returnedAmount = ZERO;
        List<GenericValue> returnedItemsBase = getOrderReturnItems();
        if (returnTypeId != null) {
            returnedItemsBase = EntityUtil.filterByAnd(returnedItemsBase, UtilMisc.toMap("returnTypeId", returnTypeId));
        }
        List<GenericValue> returnedItems = new ArrayList<>(returnedItemsBase.size());

        // get only the RETURN_RECEIVED and RETURN_COMPLETED statusIds
        returnedItems.addAll(EntityUtil.filterByAnd(returnedItemsBase, UtilMisc.toMap("statusId", "RETURN_RECEIVED")));
        returnedItems.addAll(EntityUtil.filterByAnd(returnedItemsBase, UtilMisc.toMap("statusId", "RETURN_COMPLETED")));

        // sum up the return items that have a return item response with a billing account defined
        try {
            for (GenericValue returnItem : returnedItems) {
                GenericValue returnItemResponse = returnItem.getRelatedOne("ReturnItemResponse", false);
                if (returnItemResponse == null) {
                    continue;
                }
                if (returnItemResponse.get("billingAccountId") == null) {
                    continue;
                }

                // we can just add the response amounts
                returnedAmount = returnedAmount.add(returnItemResponse.getBigDecimal("responseAmount")).setScale(DECIMALS, ROUNDING);
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, e.getMessage(), MODULE);
        }
        return returnedAmount;
    }

    /**
     * Get the total return credited to the order billing accounts  @return the billing account returned credit total bd
     */
    public BigDecimal getBillingAccountReturnedCreditTotalBd() {
        return getBillingAccountReturnedTotalByTypeBd("RTN_CREDIT");
    }

    /**
     * Get the total return refunded to the order billing accounts  @return the billing account returned refund total bd
     */
    public BigDecimal getBillingAccountReturnedRefundTotalBd() {
        return getBillingAccountReturnedTotalByTypeBd("RTN_REFUND");
    }

    /**
     * Gets the total return credited amount with refunds and credits to the billing account figured in  @return the returned credit total with
     * billing account bd
     */
    public BigDecimal getReturnedCreditTotalWithBillingAccountBd() {
        return getOrderReturnedCreditTotalBd().add(getBillingAccountReturnedRefundTotalBd()).subtract(getBillingAccountReturnedCreditTotalBd());
    }

    /**
     * Gets the total return refund amount with refunds and credits to the billing account figured in  @return the returned refund total with billing
     * account bd
     */
    public BigDecimal getReturnedRefundTotalWithBillingAccountBd() {
        return getOrderReturnedRefundTotalBd().add(getBillingAccountReturnedCreditTotalBd()).subtract(getBillingAccountReturnedRefundTotalBd());
    }

    /**
     * Gets order backorder quantity.
     * @return the order backorder quantity
     */
    public BigDecimal getOrderBackorderQuantity() {
        BigDecimal backorder = ZERO;
        List<GenericValue> items = this.getValidOrderItems();
        if (items != null) {
            for (GenericValue item : items) {
                List<GenericValue> reses = this.getOrderItemShipGrpInvResList(item);
                if (reses != null) {
                    for (GenericValue res : reses) {
                        BigDecimal nav = res.getBigDecimal("quantityNotAvailable");
                        if (nav != null) {
                            backorder = backorder.add(nav).setScale(DECIMALS, ROUNDING);
                        }
                    }
                }
            }
        }
        return backorder.setScale(DECIMALS, ROUNDING);
    }

    /**
     * Gets item picked quantity bd.
     * @param orderItem the order item
     * @return the item picked quantity bd
     */
    public BigDecimal getItemPickedQuantityBd(GenericValue orderItem) {
        BigDecimal quantityPicked = ZERO;
        EntityConditionList<EntityExpr> pickedConditions = EntityCondition.makeCondition(UtilMisc.toList(
                EntityCondition.makeCondition("orderId", EntityOperator.EQUALS, orderItem.get("orderId")),
                EntityCondition.makeCondition("orderItemSeqId", EntityOperator.EQUALS, orderItem.getString("orderItemSeqId")),
                EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "PICKLIST_CANCELLED")),
                EntityOperator.AND);

        List<GenericValue> picked = null;
        try {
            picked = orderHeader.getDelegator().findList("PicklistAndBinAndItem", pickedConditions, null, null, null, false);
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            this.orderHeader = null;
        }

        if (picked != null) {
            for (GenericValue pickedItem : picked) {
                BigDecimal issueQty = pickedItem.getBigDecimal("quantity");
                if (issueQty != null) {
                    quantityPicked = quantityPicked.add(issueQty).setScale(DECIMALS, ROUNDING);
                }
            }
        }
        return quantityPicked.setScale(DECIMALS, ROUNDING);
    }

    /**
     * Gets item shipped quantity.
     * @param orderItem the order item
     * @return the item shipped quantity
     */
    public BigDecimal getItemShippedQuantity(GenericValue orderItem) {
        BigDecimal quantityShipped = ZERO;
        List<GenericValue> issuance = getOrderItemIssuances(orderItem);
        if (issuance != null) {
            for (GenericValue issue : issuance) {
                BigDecimal issueQty = issue.getBigDecimal("quantity");
                BigDecimal cancelQty = issue.getBigDecimal("cancelQuantity");
                if (cancelQty == null) {
                    cancelQty = ZERO;
                }
                if (issueQty == null) {
                    issueQty = ZERO;
                }
                quantityShipped = quantityShipped.add(issueQty.subtract(cancelQty)).setScale(DECIMALS, ROUNDING);
            }
        }
        return quantityShipped.setScale(DECIMALS, ROUNDING);
    }

    /**
     * Gets item ship group assoc shipped quantity.
     * @param orderItem      the order item
     * @param shipGroupSeqId the ship group seq id
     * @return the item ship group assoc shipped quantity
     */
    public BigDecimal getItemShipGroupAssocShippedQuantity(GenericValue orderItem, String shipGroupSeqId) {
        BigDecimal quantityShipped = ZERO;

        if (orderItem == null) {
            return null;
        }
        if (this.orderItemIssuances == null) {
            Delegator delegator = orderItem.getDelegator();
            try {
                orderItemIssuances = EntityQuery.use(delegator).from("ItemIssuance").where("orderId", orderItem.get("orderId"),
                        "shipGroupSeqId", shipGroupSeqId).queryList();
            } catch (GenericEntityException e) {
                Debug.logWarning(e, "Trouble getting ItemIssuance(s)", MODULE);
            }
        }

        // filter the issuance
        Map<String, Object> filter = UtilMisc.toMap("orderItemSeqId", orderItem.get("orderItemSeqId"), "shipGroupSeqId", shipGroupSeqId);
        List<GenericValue> issuances = EntityUtil.filterByAnd(orderItemIssuances, filter);
        if (UtilValidate.isNotEmpty(issuances)) {
            for (GenericValue issue : issuances) {
                BigDecimal issueQty = issue.getBigDecimal("quantity");
                BigDecimal cancelQty = issue.getBigDecimal("cancelQuantity");
                if (cancelQty == null) {
                    cancelQty = ZERO;
                }
                if (issueQty == null) {
                    issueQty = ZERO;
                }
                quantityShipped = quantityShipped.add(issueQty.subtract(cancelQty)).setScale(DECIMALS, ROUNDING);
            }
        }
        return quantityShipped.setScale(DECIMALS, ROUNDING);
    }

    /**
     * Gets item reserved quantity.
     * @param orderItem the order item
     * @return the item reserved quantity
     */
    public BigDecimal getItemReservedQuantity(GenericValue orderItem) {
        BigDecimal reserved = ZERO;

        List<GenericValue> reses = getOrderItemShipGrpInvResList(orderItem);
        if (reses != null) {
            for (GenericValue res : reses) {
                BigDecimal quantity = res.getBigDecimal("quantity");
                if (quantity != null) {
                    reserved = reserved.add(quantity).setScale(DECIMALS, ROUNDING);
                }
            }
        }
        return reserved.setScale(DECIMALS, ROUNDING);
    }

    /**
     * Gets item backordered quantity.
     * @param orderItem the order item
     * @return the item backordered quantity
     */
    public BigDecimal getItemBackorderedQuantity(GenericValue orderItem) {
        BigDecimal backOrdered = ZERO;

        Timestamp shipDate = orderItem.getTimestamp("estimatedShipDate");
        Timestamp autoCancel = orderItem.getTimestamp("autoCancelDate");

        List<GenericValue> reses = getOrderItemShipGrpInvResList(orderItem);
        if (reses != null) {
            for (GenericValue res : reses) {
                Timestamp promised = res.getTimestamp("currentPromisedDate");
                if (promised == null) {
                    promised = res.getTimestamp("promisedDatetime");
                }
                if (autoCancel != null || (shipDate != null && shipDate.after(promised))) {
                    BigDecimal resQty = res.getBigDecimal("quantity");
                    if (resQty != null) {
                        backOrdered = backOrdered.add(resQty).setScale(DECIMALS, ROUNDING);
                    }
                }
            }
        }
        return backOrdered;
    }

    /**
     * Gets item pending shipment quantity.
     * @param orderItem the order item
     * @return the item pending shipment quantity
     */
    public BigDecimal getItemPendingShipmentQuantity(GenericValue orderItem) {
        BigDecimal reservedQty = getItemReservedQuantity(orderItem);
        BigDecimal backordered = getItemBackorderedQuantity(orderItem);
        return reservedQty.subtract(backordered).setScale(DECIMALS, ROUNDING);
    }

    /**
     * Gets item canceled quantity.
     * @param orderItem the order item
     * @return the item canceled quantity
     */
    public BigDecimal getItemCanceledQuantity(GenericValue orderItem) {
        BigDecimal cancelQty = orderItem.getBigDecimal("cancelQuantity");
        if (cancelQty == null) {
            cancelQty = BigDecimal.ZERO;
        }
        return cancelQty;
    }

    /**
     * Gets total order items quantity.
     * @return the total order items quantity
     */
    public BigDecimal getTotalOrderItemsQuantity() {
        List<GenericValue> orderItems = getValidOrderItems();
        BigDecimal totalItems = ZERO;

        for (int i = 0; i < orderItems.size(); i++) {
            GenericValue oi = orderItems.get(i);

            totalItems = totalItems.add(getOrderItemQuantity(oi)).setScale(DECIMALS, ROUNDING);
        }
        return totalItems.setScale(DECIMALS, ROUNDING);
    }

    /**
     * Gets total order items ordered quantity.
     * @return the total order items ordered quantity
     */
    public BigDecimal getTotalOrderItemsOrderedQuantity() {
        List<GenericValue> orderItems = getValidOrderItems();
        BigDecimal totalItems = ZERO;

        for (int i = 0; i < orderItems.size(); i++) {
            GenericValue oi = orderItems.get(i);

            totalItems = totalItems.add(oi.getBigDecimal("quantity")).setScale(DECIMALS, ROUNDING);
        }
        return totalItems;
    }

    /**
     * Gets order items sub total.
     * @return the order items sub total
     */
    public BigDecimal getOrderItemsSubTotal() {
        return getOrderItemsSubTotal(getValidOrderItems(), getAdjustments());
    }

    /**
     * Gets order item sub total.
     * @param orderItem the order item
     * @return the order item sub total
     */
    public BigDecimal getOrderItemSubTotal(GenericValue orderItem) {
        return getOrderItemSubTotal(orderItem, getAdjustments());
    }

    /**
     * Gets order items total.
     * @return the order items total
     */
    public BigDecimal getOrderItemsTotal() {
        return getOrderItemsTotal(getValidOrderItems(), getAdjustments());
    }

    /**
     * Gets order item total.
     * @param orderItem the order item
     * @return the order item total
     */
    public BigDecimal getOrderItemTotal(GenericValue orderItem) {
        return getOrderItemTotal(orderItem, getAdjustments());
    }

    /**
     * Gets order item tax.
     * @param orderItem the order item
     * @return the order item tax
     */
    public BigDecimal getOrderItemTax(GenericValue orderItem) {
        return getOrderItemAdjustmentsTotal(orderItem, false, true, false);
    }

    /**
     * Gets order item shipping.
     * @param orderItem the order item
     * @return the order item shipping
     */
    public BigDecimal getOrderItemShipping(GenericValue orderItem) {
        return getOrderItemAdjustmentsTotal(orderItem, false, false, true);
    }

    /**
     * Gets order item adjustments total.
     * @param orderItem       the order item
     * @param includeOther    the include other
     * @param includeTax      the include tax
     * @param includeShipping the include shipping
     * @return the order item adjustments total
     */
    public BigDecimal getOrderItemAdjustmentsTotal(GenericValue orderItem, boolean includeOther, boolean includeTax, boolean includeShipping) {
        return getOrderItemAdjustmentsTotal(orderItem, getAdjustments(), includeOther, includeTax, includeShipping);
    }

    /**
     * Gets order item adjustments total.
     * @param orderItem the order item
     * @return the order item adjustments total
     */
    public BigDecimal getOrderItemAdjustmentsTotal(GenericValue orderItem) {
        return getOrderItemAdjustmentsTotal(orderItem, true, false, false);
    }

    /**
     * Gets order item adjustment total.
     * @param orderItem  the order item
     * @param adjustment the adjustment
     * @return the order item adjustment total
     */
    public BigDecimal getOrderItemAdjustmentTotal(GenericValue orderItem, GenericValue adjustment) {
        return calcItemAdjustment(adjustment, orderItem);
    }

    /**
     * Gets adjustment type.
     * @param adjustment the adjustment
     * @return the adjustment type
     */
    public String getAdjustmentType(GenericValue adjustment) {
        GenericValue adjustmentType = null;
        try {
            adjustmentType = adjustment.getRelatedOne("OrderAdjustmentType", false);
        } catch (GenericEntityException e) {
            Debug.logError(e, "Problems with order adjustment", MODULE);
        }
        if (adjustmentType == null || adjustmentType.get("description") == null) {
            return "";
        }
        return adjustmentType.getString("description");
    }

    /**
     * Gets order item statuses.
     * @param orderItem the order item
     * @return the order item statuses
     */
    public List<GenericValue> getOrderItemStatuses(GenericValue orderItem) {
        return getOrderItemStatuses(orderItem, getOrderStatuses());
    }

    /**
     * Gets current item status string.
     * @param orderItem the order item
     * @return the current item status string
     */
    public String getCurrentItemStatusString(GenericValue orderItem) {
        GenericValue statusItem = null;
        try {
            statusItem = orderItem.getRelatedOne("StatusItem", true);
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
        }
        if (statusItem != null) {
            return statusItem.getString("description");
        }
        return orderHeader.getString("statusId");
    }

    /**
     * Fetches the set of order items with the given EntityCondition.  @param entityCondition the entity condition
     * @return the order items by condition
     */
    public List<GenericValue> getOrderItemsByCondition(EntityCondition entityCondition) {
        return EntityUtil.filterByCondition(getOrderItems(), entityCondition);
    }

    /**
     * Gets product promo codes entered.
     * @return the product promo codes entered
     */
    public Set<String> getProductPromoCodesEntered() {
        Delegator delegator = orderHeader.getDelegator();
        Set<String> productPromoCodesEntered = new HashSet<>();
        try {
            for (GenericValue orderProductPromoCode: EntityQuery.use(delegator).from("OrderProductPromoCode")
                    .where("orderId", orderHeader.get("orderId")).cache().queryList()) {
                productPromoCodesEntered.add(orderProductPromoCode.getString("productPromoCodeId"));
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
        }
        return productPromoCodesEntered;
    }

    /**
     * Gets product promo use.
     * @return the product promo use
     */
    public List<GenericValue> getProductPromoUse() {
        Delegator delegator = orderHeader.getDelegator();
        try {
            return EntityQuery.use(delegator).from("ProductPromoUse").where("orderId", orderHeader.get("orderId")).cache().queryList();
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
        }
        return new LinkedList<>();
    }

    /**
     * Checks to see if this user has read permission on this order
     * @param security  the security
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

    /**
     * Gets order header.
     * @param delegator the delegator
     * @param orderId   the order id
     * @return the order header
     */
    public static GenericValue getOrderHeader(Delegator delegator, String orderId) {
        GenericValue orderHeader = null;
        if (orderId != null && delegator != null) {
            try {
                orderHeader = EntityQuery.use(delegator).from("OrderHeader").where("orderId", orderId).queryOne();
            } catch (GenericEntityException e) {
                Debug.logError(e, "Cannot get order header", MODULE);
            }
        }
        return orderHeader;
    }

    /**
     * Gets order item quantity.
     * @param orderItem the order item
     * @return the order item quantity
     */
    public static BigDecimal getOrderItemQuantity(GenericValue orderItem) {

        BigDecimal cancelQty = orderItem.getBigDecimal("cancelQuantity");
        BigDecimal orderQty = orderItem.getBigDecimal("quantity");

        if (cancelQty == null) {
            cancelQty = ZERO;
        }
        if (orderQty == null) {
            orderQty = ZERO;
        }

        return orderQty.subtract(cancelQty);
    }

    /**
     * Gets order item ship group quantity.
     * @param shipGroupAssoc the ship group assoc
     * @return the order item ship group quantity
     */
    public static BigDecimal getOrderItemShipGroupQuantity(GenericValue shipGroupAssoc) {
        BigDecimal cancelQty = shipGroupAssoc.getBigDecimal("cancelQuantity");
        BigDecimal orderQty = shipGroupAssoc.getBigDecimal("quantity");

        if (cancelQty == null) {
            cancelQty = BigDecimal.ZERO;
        }
        if (orderQty == null) {
            orderQty = BigDecimal.ZERO;
        }

        return orderQty.subtract(cancelQty);
    }

    /**
     * Gets product store from order.
     * @param delegator the delegator
     * @param orderId   the order id
     * @return the product store from order
     */
    public static GenericValue getProductStoreFromOrder(Delegator delegator, String orderId) {
        GenericValue orderHeader = getOrderHeader(delegator, orderId);
        if (orderHeader == null) {
            Debug.logWarning("Could not find OrderHeader for orderId [" + orderId + "] in getProductStoreFromOrder, returning null", MODULE);
        }
        return getProductStoreFromOrder(orderHeader);
    }

    /**
     * Gets product store from order.
     * @param orderHeader the order header
     * @return the product store from order
     */
    public static GenericValue getProductStoreFromOrder(GenericValue orderHeader) {
        if (orderHeader == null) {
            return null;
        }
        Delegator delegator = orderHeader.getDelegator();
        GenericValue productStore = null;
        if (orderHeader.get("productStoreId") != null) {
            try {
                productStore = EntityQuery.use(delegator).from("ProductStore").where("productStoreId",
                        orderHeader.getString("productStoreId")).cache().queryOne();
            } catch (GenericEntityException e) {
                Debug.logError(e, "Cannot locate ProductStore from OrderHeader", MODULE);
            }
        } else {
            Debug.logError("Null header or productStoreId", MODULE);
        }
        return productStore;
    }

    /**
     * Gets order grand total.
     * @param orderItems  the order items
     * @param adjustments the adjustments
     * @return the order grand total
     */
    public static BigDecimal getOrderGrandTotal(List<GenericValue> orderItems, List<GenericValue> adjustments) {
        Map<String, Object> orderTaxByTaxAuthGeoAndParty = getOrderTaxByTaxAuthGeoAndParty(adjustments);
        BigDecimal taxGrandTotal = (BigDecimal) orderTaxByTaxAuthGeoAndParty.get("taxGrandTotal");
        adjustments = EntityUtil.filterByAnd(adjustments, UtilMisc.toList(EntityCondition.makeCondition("orderAdjustmentTypeId",
                EntityOperator.NOT_EQUAL, "SALES_TAX")));
        BigDecimal total = getOrderItemsTotal(orderItems, adjustments);
        BigDecimal adj = getOrderAdjustmentsTotal(orderItems, adjustments);
        total = ((total.add(taxGrandTotal)).add(adj)).setScale(DECIMALS, ROUNDING);
        return total;
    }

    /**
     * Gets order header adjustments.
     * @param adjustments    the adjustments
     * @param shipGroupSeqId the ship group seq id
     * @return the order header adjustments
     */
    public static List<GenericValue> getOrderHeaderAdjustments(List<GenericValue> adjustments, String shipGroupSeqId) {
        List<EntityExpr> contraints1 = UtilMisc.toList(EntityCondition.makeCondition("orderItemSeqId", EntityOperator.EQUALS, null));
        List<EntityExpr> contraints2 = UtilMisc.toList(EntityCondition.makeCondition("orderItemSeqId", EntityOperator.EQUALS,
                DataModelConstants.SEQ_ID_NA));
        List<EntityExpr> contraints3 = UtilMisc.toList(EntityCondition.makeCondition("orderItemSeqId", EntityOperator.EQUALS, ""));
        List<EntityExpr> contraints4 = new LinkedList<>();
        if (shipGroupSeqId != null) {
            contraints4.add(EntityCondition.makeCondition("shipGroupSeqId", EntityOperator.EQUALS, shipGroupSeqId));
        }
        List<GenericValue> toFilter = null;
        List<GenericValue> adj = new LinkedList<>();

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

    /**
     * Gets order header statuses.
     * @param orderStatuses the order statuses
     * @return the order header statuses
     */
    public static List<GenericValue> getOrderHeaderStatuses(List<GenericValue> orderStatuses) {
        List<EntityExpr> contraints1 = UtilMisc.toList(EntityCondition.makeCondition("orderItemSeqId", EntityOperator.EQUALS, null));
        contraints1.add(EntityCondition.makeCondition("orderItemSeqId", EntityOperator.EQUALS, DataModelConstants.SEQ_ID_NA));
        contraints1.add(EntityCondition.makeCondition("orderItemSeqId", EntityOperator.EQUALS, ""));

        List<EntityExpr> contraints2 = UtilMisc.toList(EntityCondition.makeCondition("orderPaymentPreferenceId", EntityOperator.EQUALS, null));
        contraints2.add(EntityCondition.makeCondition("orderPaymentPreferenceId", EntityOperator.EQUALS, DataModelConstants.SEQ_ID_NA));
        contraints2.add(EntityCondition.makeCondition("orderPaymentPreferenceId", EntityOperator.EQUALS, ""));

        List<GenericValue> newOrderStatuses = new LinkedList<>();
        newOrderStatuses.addAll(EntityUtil.filterByOr(orderStatuses, contraints1));
        return EntityUtil.orderBy(EntityUtil.filterByOr(newOrderStatuses, contraints2), UtilMisc.toList("-statusDatetime"));
    }

    /**
     * Gets order adjustments total.
     * @param orderItems  the order items
     * @param adjustments the adjustments
     * @return the order adjustments total
     */
    public static BigDecimal getOrderAdjustmentsTotal(List<GenericValue> orderItems, List<GenericValue> adjustments) {
        return calcOrderAdjustments(getOrderHeaderAdjustments(adjustments, null), getOrderItemsSubTotal(orderItems, adjustments), true, true, true);
    }

    /**
     * Gets order survey responses.
     * @param orderHeader the order header
     * @return the order survey responses
     */
    public static List<GenericValue> getOrderSurveyResponses(GenericValue orderHeader) {
        Delegator delegator = orderHeader.getDelegator();
        String orderId = orderHeader.getString("orderId");
        List<GenericValue> responses = null;
        try {
            responses = EntityQuery.use(delegator).from("SurveyResponse").where("orderId", orderId, "orderItemSeqId", "_NA_").queryList();
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
        }

        if (responses == null) {
            responses = new LinkedList<>();
        }
        return responses;
    }

    /**
     * Gets order item survey response.
     * @param orderItem the order item
     * @return the order item survey response
     */
    public static List<GenericValue> getOrderItemSurveyResponse(GenericValue orderItem) {
        Delegator delegator = orderItem.getDelegator();
        String orderItemSeqId = orderItem.getString("orderItemSeqId");
        String orderId = orderItem.getString("orderId");
        List<GenericValue> responses = null;
        try {
            responses = EntityQuery.use(delegator).from("SurveyResponse").where("orderId", orderId, "orderItemSeqId", orderItemSeqId).queryList();
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
        }

        if (responses == null) {
            responses = new LinkedList<>();
        }
        return responses;
    }

    // ================= Order Adjustments =================

    /**
     * Calc order adjustments big decimal.
     * @param orderHeaderAdjustments the order header adjustments
     * @param subTotal               the sub total
     * @param includeOther           the include other
     * @param includeTax             the include tax
     * @param includeShipping        the include shipping
     * @return the big decimal
     */
    public static BigDecimal calcOrderAdjustments(List<GenericValue> orderHeaderAdjustments, BigDecimal subTotal, boolean includeOther,
                                                  boolean includeTax, boolean includeShipping) {
        BigDecimal adjTotal = ZERO;

        if (UtilValidate.isNotEmpty(orderHeaderAdjustments)) {
            List<GenericValue> filteredAdjs = filterOrderAdjustments(orderHeaderAdjustments, includeOther, includeTax, includeShipping, false, false);
            for (GenericValue orderAdjustment : filteredAdjs) {
                adjTotal = adjTotal.add(OrderReadHelper.calcOrderAdjustment(orderAdjustment, subTotal)).setScale(DECIMALS, ROUNDING);
            }
        }
        return adjTotal.setScale(DECIMALS, ROUNDING);
    }

    /**
     * Calc order adjustment big decimal.
     * @param orderAdjustment the order adjustment
     * @param orderSubTotal   the order sub total
     * @return the big decimal
     */
    public static BigDecimal calcOrderAdjustment(GenericValue orderAdjustment, BigDecimal orderSubTotal) {
        BigDecimal adjustment = ZERO;

        if (orderAdjustment.get("amount") != null) {
            BigDecimal amount = orderAdjustment.getBigDecimal("amount");
            adjustment = adjustment.add(amount);
        } else if (orderAdjustment.get("sourcePercentage") != null) {
            BigDecimal percent = orderAdjustment.getBigDecimal("sourcePercentage");
            BigDecimal amount = orderSubTotal.multiply(percent).multiply(PERCENTAGE);
            adjustment = adjustment.add(amount);
        }
        if ("SALES_TAX".equals(orderAdjustment.get("orderAdjustmentTypeId"))) {
            return adjustment.setScale(TAX_SCALE, TAX_ROUNDING);
        }
        return adjustment.setScale(DECIMALS, ROUNDING);
    }

    /**
     * Gets order items sub total.
     * @param orderItems  the order items
     * @param adjustments the adjustments
     * @return the order items sub total
     */
// ================= Order Item Adjustments =================
    public static BigDecimal getOrderItemsSubTotal(List<GenericValue> orderItems, List<GenericValue> adjustments) {
        return getOrderItemsSubTotal(orderItems, adjustments, null);
    }

    /**
     * Gets order items sub total.
     * @param orderItems  the order items
     * @param adjustments the adjustments
     * @param workEfforts the work efforts
     * @return the order items sub total
     */
    public static BigDecimal getOrderItemsSubTotal(List<GenericValue> orderItems, List<GenericValue> adjustments, List<GenericValue> workEfforts) {
        BigDecimal result = ZERO;
        Iterator<GenericValue> itemIter = UtilMisc.toIterator(orderItems);

        while (itemIter != null && itemIter.hasNext()) {
            GenericValue orderItem = itemIter.next();
            BigDecimal itemTotal = getOrderItemSubTotal(orderItem, adjustments);

            if (workEfforts != null && orderItem.getString("orderItemTypeId").compareTo("RENTAL_ORDER_ITEM") == 0) {
                Iterator<GenericValue> weIter = UtilMisc.toIterator(workEfforts);
                while (weIter != null && weIter.hasNext()) {
                    GenericValue workEffort = weIter.next();
                    if (workEffort.getString("workEffortId").compareTo(orderItem.getString("orderItemSeqId")) == 0) {
                        itemTotal = itemTotal.multiply(getWorkEffortRentalQuantity(workEffort)).setScale(DECIMALS, ROUNDING);
                        break;
                    }
                }
            }
            result = result.add(itemTotal).setScale(DECIMALS, ROUNDING);

        }
        return result.setScale(DECIMALS, ROUNDING);
    }

    /**
     * The passed adjustments can be all adjustments for the order, ie for all line items  @param orderItem the order item
     * @param adjustments the adjustments
     * @return the order item sub total
     */
    public static BigDecimal getOrderItemSubTotal(GenericValue orderItem, List<GenericValue> adjustments) {
        return getOrderItemSubTotal(orderItem, adjustments, false, false);
    }

    /**
     * The passed adjustments can be all adjustments for the order, ie for all line items  @param orderItem the order item
     * @param adjustments the adjustments
     * @param forTax      the for tax
     * @param forShipping the for shipping
     * @return the order item sub total
     */
    public static BigDecimal getOrderItemSubTotal(GenericValue orderItem, List<GenericValue> adjustments, boolean forTax, boolean forShipping) {
        BigDecimal unitPrice = orderItem.getBigDecimal("unitPrice");
        BigDecimal quantity = getOrderItemQuantity(orderItem);
        BigDecimal result = ZERO;

        if (unitPrice == null || quantity == null) {
            Debug.logWarning("[getOrderItemTotal] unitPrice or quantity are null, using 0 for the item base price", MODULE);
        } else {
            if (Debug.verboseOn()) {
                Debug.logVerbose("Unit Price : " + unitPrice + " / " + "Quantity : " + quantity, MODULE);
            }
            result = unitPrice.multiply(quantity);

            if ("RENTAL_ORDER_ITEM".equals(orderItem.getString("orderItemTypeId"))) {
                // retrieve related work effort when required.
                List<GenericValue> workOrderItemFulfillments = null;
                try {
                    workOrderItemFulfillments = orderItem.getDelegator().findByAnd("WorkOrderItemFulfillment", UtilMisc.toMap("orderId",
                            orderItem.getString("orderId"), "orderItemSeqId", orderItem.getString("orderItemSeqId")), null, true);
                } catch (GenericEntityException e) {
                    Debug.logError(e, MODULE);
                }
                if (workOrderItemFulfillments != null) {
                    Iterator<GenericValue> iter = workOrderItemFulfillments.iterator();
                    if (iter.hasNext()) {
                        GenericValue workOrderItemFulfillment = iter.next();
                        GenericValue workEffort = null;
                        try {
                            workEffort = workOrderItemFulfillment.getRelatedOne("WorkEffort", true);
                        } catch (GenericEntityException e) {
                            Debug.logError(e, MODULE);
                        }
                        result = result.multiply(getWorkEffortRentalQuantity(workEffort));
                    }
                }
            }
        }

        // subtotal also includes non tax and shipping adjustments; tax and shipping will be calculated using this adjusted value
        result = result.add(getOrderItemAdjustmentsTotal(orderItem, adjustments, true, false, false, forTax, forShipping));

        return result.setScale(DECIMALS, ROUNDING);
    }

    /**
     * Gets order items total.
     * @param orderItems  the order items
     * @param adjustments the adjustments
     * @return the order items total
     */
    public static BigDecimal getOrderItemsTotal(List<GenericValue> orderItems, List<GenericValue> adjustments) {
        BigDecimal result = ZERO;
        Iterator<GenericValue> itemIter = UtilMisc.toIterator(orderItems);

        while (itemIter != null && itemIter.hasNext()) {
            result = result.add(getOrderItemTotal(itemIter.next(), adjustments));
        }
        return result.setScale(DECIMALS, ROUNDING);
    }

    /**
     * Gets order item total.
     * @param orderItem   the order item
     * @param adjustments the adjustments
     * @return the order item total
     */
    public static BigDecimal getOrderItemTotal(GenericValue orderItem, List<GenericValue> adjustments) {
        // add tax and shipping to subtotal
        return getOrderItemSubTotal(orderItem, adjustments).add(getOrderItemAdjustmentsTotal(orderItem, adjustments, false, true, true));
    }

    /**
     * Calc order promo adjustments bd big decimal.
     * @param allOrderAdjustments the all order adjustments
     * @return the big decimal
     */
    public static BigDecimal calcOrderPromoAdjustmentsBd(List<GenericValue> allOrderAdjustments) {
        BigDecimal promoAdjTotal = ZERO;

        List<GenericValue> promoAdjustments = EntityUtil.filterByAnd(allOrderAdjustments, UtilMisc.toMap("orderAdjustmentTypeId",
                "PROMOTION_ADJUSTMENT"));

        if (UtilValidate.isNotEmpty(promoAdjustments)) {
            Iterator<GenericValue> promoAdjIter = promoAdjustments.iterator();
            while (promoAdjIter.hasNext()) {
                GenericValue promoAdjustment = promoAdjIter.next();
                if (promoAdjustment != null) {
                    BigDecimal amount = promoAdjustment.getBigDecimal("amount").setScale(TAX_SCALE, TAX_ROUNDING);
                    promoAdjTotal = promoAdjTotal.add(amount);
                }
            }
        }
        return promoAdjTotal.setScale(DECIMALS, ROUNDING);
    }

    /**
     * Gets work effort rental length.
     * @param workEffort the work effort
     * @return the work effort rental length
     */
    public static BigDecimal getWorkEffortRentalLength(GenericValue workEffort) {
        BigDecimal length = null;
        if (workEffort.get("estimatedStartDate") != null && workEffort.get("estimatedCompletionDate") != null) {
            length = new BigDecimal(UtilDateTime.getInterval(workEffort.getTimestamp("estimatedStartDate"),
                    workEffort.getTimestamp("estimatedCompletionDate")) / 86400000);
        }
        return length;
    }

    /**
     * Gets work effort rental quantity.
     * @param workEffort the work effort
     * @return the work effort rental quantity
     */
    public static BigDecimal getWorkEffortRentalQuantity(GenericValue workEffort) {
        BigDecimal persons = BigDecimal.ONE;
        if (workEffort.get("reservPersons") != null) {
            persons = workEffort.getBigDecimal("reservPersons");
        }
        BigDecimal secondPersonPerc = ZERO;
        if (workEffort.get("reserv2ndPPPerc") != null) {
            secondPersonPerc = workEffort.getBigDecimal("reserv2ndPPPerc");
        }
        BigDecimal nthPersonPerc = ZERO;
        if (workEffort.get("reservNthPPPerc") != null) {
            nthPersonPerc = workEffort.getBigDecimal("reservNthPPPerc");
        }
        long length = 1;
        if (workEffort.get("estimatedStartDate") != null && workEffort.get("estimatedCompletionDate") != null) {
            length = (workEffort.getTimestamp("estimatedCompletionDate").getTime() - workEffort.getTimestamp("estimatedStartDate").getTime())
                    / 86400000;
        }

        BigDecimal rentalAdjustment = ZERO;
        if (persons.compareTo(BigDecimal.ONE) == 1) {
            if (persons.compareTo(new BigDecimal(2)) == 1) {
                persons = persons.subtract(new BigDecimal(2));
                if (nthPersonPerc.signum() == 1) {
                    rentalAdjustment = persons.multiply(nthPersonPerc);
                } else {
                    rentalAdjustment = persons.multiply(secondPersonPerc);
                }
                persons = new BigDecimal("2");
            }
            if (persons.compareTo(new BigDecimal("2")) == 0) {
                rentalAdjustment = rentalAdjustment.add(secondPersonPerc);
            }
        }
        rentalAdjustment = rentalAdjustment.add(new BigDecimal(100));  // add final 100 percent for first person
        rentalAdjustment = rentalAdjustment.divide(new BigDecimal(100), DECIMALS, ROUNDING).multiply(new BigDecimal(String.valueOf(length)));
        return rentalAdjustment; // return total rental adjustment
    }

    /**
     * Gets all order items adjustments total.
     * @param orderItems      the order items
     * @param adjustments     the adjustments
     * @param includeOther    the include other
     * @param includeTax      the include tax
     * @param includeShipping the include shipping
     * @return the all order items adjustments total
     */
    public static BigDecimal getAllOrderItemsAdjustmentsTotal(List<GenericValue> orderItems, List<GenericValue> adjustments, boolean includeOther,
                                                              boolean includeTax, boolean includeShipping) {
        BigDecimal result = ZERO;
        Iterator<GenericValue> itemIter = UtilMisc.toIterator(orderItems);

        while (itemIter != null && itemIter.hasNext()) {
            result = result.add(getOrderItemAdjustmentsTotal(itemIter.next(), adjustments, includeOther, includeTax, includeShipping));
        }
        return result.setScale(DECIMALS, ROUNDING);
    }

    /**
     * The passed adjustments can be all adjustments for the order, ie for all line items  @param orderItem the order item
     * @param adjustments     the adjustments
     * @param includeOther    the include other
     * @param includeTax      the include tax
     * @param includeShipping the include shipping
     * @return the order item adjustments total
     */
    public static BigDecimal getOrderItemAdjustmentsTotal(GenericValue orderItem, List<GenericValue> adjustments, boolean includeOther,
                                                          boolean includeTax, boolean includeShipping) {
        return getOrderItemAdjustmentsTotal(orderItem, adjustments, includeOther, includeTax, includeShipping, false, false);
    }

    /**
     * The passed adjustments can be all adjustments for the order, ie for all line items  @param orderItem the order item
     * @param adjustments     the adjustments
     * @param includeOther    the include other
     * @param includeTax      the include tax
     * @param includeShipping the include shipping
     * @param forTax          the for tax
     * @param forShipping     the for shipping
     * @return the order item adjustments total
     */
    public static BigDecimal getOrderItemAdjustmentsTotal(GenericValue orderItem, List<GenericValue> adjustments, boolean includeOther,
                                                          boolean includeTax, boolean includeShipping, boolean forTax, boolean forShipping) {
        return calcItemAdjustments(getOrderItemQuantity(orderItem), orderItem.getBigDecimal("unitPrice"),
                getOrderItemAdjustmentList(orderItem, adjustments),
                includeOther, includeTax, includeShipping, forTax, forShipping);
    }

    /**
     * Gets order item adjustment list.
     * @param orderItem   the order item
     * @param adjustments the adjustments
     * @return the order item adjustment list
     */
    public static List<GenericValue> getOrderItemAdjustmentList(GenericValue orderItem, List<GenericValue> adjustments) {
        return EntityUtil.filterByAnd(adjustments, UtilMisc.toMap("orderItemSeqId", orderItem.get("orderItemSeqId")));
    }

    /**
     * Gets order item statuses.
     * @param orderItem     the order item
     * @param orderStatuses the order statuses
     * @return the order item statuses
     */
    public static List<GenericValue> getOrderItemStatuses(GenericValue orderItem, List<GenericValue> orderStatuses) {
        List<EntityExpr> contraints1 = UtilMisc.toList(EntityCondition.makeCondition("orderItemSeqId", EntityOperator.EQUALS,
                orderItem.get("orderItemSeqId")));
        List<EntityExpr> contraints2 = UtilMisc.toList(EntityCondition.makeCondition("orderPaymentPreferenceId", EntityOperator.EQUALS, null));
        contraints2.add(EntityCondition.makeCondition("orderPaymentPreferenceId", EntityOperator.EQUALS, DataModelConstants.SEQ_ID_NA));
        contraints2.add(EntityCondition.makeCondition("orderPaymentPreferenceId", EntityOperator.EQUALS, ""));

        List<GenericValue> newOrderStatuses = new LinkedList<>();
        newOrderStatuses.addAll(EntityUtil.filterByAnd(orderStatuses, contraints1));
        return EntityUtil.orderBy(EntityUtil.filterByOr(newOrderStatuses, contraints2), UtilMisc.toList("-statusDatetime"));
    }


    // Order Item Adjs Utility Methods

    /**
     * Calc item adjustments big decimal.
     * @param quantity        the quantity
     * @param unitPrice       the unit price
     * @param adjustments     the adjustments
     * @param includeOther    the include other
     * @param includeTax      the include tax
     * @param includeShipping the include shipping
     * @param forTax          the for tax
     * @param forShipping     the for shipping
     * @return the big decimal
     */
    public static BigDecimal calcItemAdjustments(BigDecimal quantity, BigDecimal unitPrice, List<GenericValue> adjustments, boolean includeOther,
                                                 boolean includeTax, boolean includeShipping, boolean forTax, boolean forShipping) {
        BigDecimal adjTotal = ZERO;

        if (UtilValidate.isNotEmpty(adjustments)) {
            List<GenericValue> filteredAdjs = filterOrderAdjustments(adjustments, includeOther, includeTax, includeShipping, forTax, forShipping);
            for (GenericValue orderAdjustment : filteredAdjs) {
                adjTotal = adjTotal.add(OrderReadHelper.calcItemAdjustment(orderAdjustment, quantity, unitPrice));
            }
        }
        return adjTotal;
    }

    /**
     * Calc item adjustments recurring bd big decimal.
     * @param quantity        the quantity
     * @param unitPrice       the unit price
     * @param adjustments     the adjustments
     * @param includeOther    the include other
     * @param includeTax      the include tax
     * @param includeShipping the include shipping
     * @param forTax          the for tax
     * @param forShipping     the for shipping
     * @return the big decimal
     */
    public static BigDecimal calcItemAdjustmentsRecurringBd(BigDecimal quantity, BigDecimal unitPrice, List<GenericValue> adjustments,
                boolean includeOther, boolean includeTax, boolean includeShipping, boolean forTax, boolean forShipping) {
        BigDecimal adjTotal = ZERO;

        if (UtilValidate.isNotEmpty(adjustments)) {
            List<GenericValue> filteredAdjs = filterOrderAdjustments(adjustments, includeOther, includeTax, includeShipping, forTax, forShipping);
            for (GenericValue orderAdjustment : filteredAdjs) {
                adjTotal = adjTotal.add(OrderReadHelper.calcItemAdjustmentRecurringBd(orderAdjustment, quantity, unitPrice))
                        .setScale(DECIMALS, ROUNDING);
            }
        }
        return adjTotal;
    }

    /**
     * Calc item adjustment big decimal.
     * @param itemAdjustment the item adjustment
     * @param item           the item
     * @return the big decimal
     */
    public static BigDecimal calcItemAdjustment(GenericValue itemAdjustment, GenericValue item) {
        return calcItemAdjustment(itemAdjustment, getOrderItemQuantity(item), item.getBigDecimal("unitPrice"));
    }

    /**
     * Calc item adjustment big decimal.
     * @param itemAdjustment the item adjustment
     * @param quantity       the quantity
     * @param unitPrice      the unit price
     * @return the big decimal
     */
    public static BigDecimal calcItemAdjustment(GenericValue itemAdjustment, BigDecimal quantity, BigDecimal unitPrice) {
        BigDecimal adjustment = ZERO;
        if (itemAdjustment.get("amount") != null) {
            // shouldn't round amounts here, wait until item total is added up otherwise incremental errors are introduced, and there is code that
            // calls this method that does that already: adjustment =
            // adjustment.add(setScaleByType("SALES_TAX".equals(itemAdjustment.get("orderAdjustmentTypeId")),
            // itemAdjustment.getBigDecimal("amount")));
            adjustment = adjustment.add(itemAdjustment.getBigDecimal("amount"));
        } else if (itemAdjustment.get("sourcePercentage") != null) {
            // see comment above about ROUNDING: adjustment =
            // adjustment.add(setScaleByType("SALES_TAX".equals(itemAdjustment.get("orderAdjustmentTypeId")),
            // itemAdjustment.getBigDecimal("sourcePercentage").multiply(quantity).multiply(unitPrice).multiply(PERCENTAGE)));
            adjustment = adjustment.add(itemAdjustment.getBigDecimal("sourcePercentage").multiply(quantity).multiply(unitPrice).multiply(PERCENTAGE));
        }
        if (Debug.verboseOn()) {
            Debug.logVerbose("calcItemAdjustment: " + itemAdjustment + ", quantity=" + quantity + ", unitPrice=" + unitPrice + ", adjustment="
                    + adjustment, MODULE);
        }
        return adjustment;
    }

    /**
     * Calc item adjustment recurring bd big decimal.
     * @param itemAdjustment the item adjustment
     * @param quantity       the quantity
     * @param unitPrice      the unit price
     * @return the big decimal
     */
    public static BigDecimal calcItemAdjustmentRecurringBd(GenericValue itemAdjustment, BigDecimal quantity, BigDecimal unitPrice) {
        BigDecimal adjustmentRecurring = ZERO;
        if (itemAdjustment.get("recurringAmount") != null) {
            adjustmentRecurring = adjustmentRecurring.add(setScaleByType("SALES_TAX".equals(itemAdjustment.get("orderAdjustmentTypeId")),
                    itemAdjustment.getBigDecimal("recurringAmount")));
        }
        if (Debug.verboseOn()) {
            Debug.logVerbose("calcItemAdjustmentRecurring: " + itemAdjustment + ", quantity=" + quantity + ", unitPrice=" + unitPrice
                    + ", adjustmentRecurring=" + adjustmentRecurring, MODULE);
        }
        return adjustmentRecurring.setScale(DECIMALS, ROUNDING);
    }

    /**
     * Filter order adjustments list.
     * @param adjustments     the adjustments
     * @param includeOther    the include other
     * @param includeTax      the include tax
     * @param includeShipping the include shipping
     * @param forTax          the for tax
     * @param forShipping     the for shipping
     * @return the list
     */
    public static List<GenericValue> filterOrderAdjustments(List<GenericValue> adjustments, boolean includeOther, boolean includeTax,
                                                            boolean includeShipping, boolean forTax, boolean forShipping) {
        List<GenericValue> newOrderAdjustmentsList = new LinkedList<>();

        if (UtilValidate.isNotEmpty(adjustments)) {
            for (GenericValue orderAdjustment : adjustments) {
                boolean includeAdjustment = false;

                if ("SALES_TAX".equals(orderAdjustment.getString("orderAdjustmentTypeId"))
                        || "VAT_TAX".equals(orderAdjustment.getString("orderAdjustmentTypeId"))
                        || "VAT_PRICE_CORRECT".equals(orderAdjustment.getString("orderAdjustmentTypeId"))) {
                    if (includeTax) {
                        includeAdjustment = true;
                    }
                } else if ("SHIPPING_CHARGES".equals(orderAdjustment.getString("orderAdjustmentTypeId"))) {
                    if (includeShipping) {
                        includeAdjustment = true;
                    }
                } else {
                    if (includeOther) {
                        includeAdjustment = true;
                    }
                }

                // default to yes, include for shipping; so only exclude if includeInShipping is N, or false;
                // if Y or null or anything else it will be included
                if (forTax && "N".equals(orderAdjustment.getString("includeInTax"))) {
                    includeAdjustment = false;
                }

                // default to yes, include for shipping; so only exclude if includeInShipping is N, or false;
                // if Y or null or anything else it will be included
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

    /**
     * Gets quantity on order.
     * @param delegator the delegator
     * @param productId the product id
     * @return the quantity on order
     */
    public static BigDecimal getQuantityOnOrder(Delegator delegator, String productId) {
        BigDecimal quantity = BigDecimal.ZERO;

        // first find all open purchase orders
        List<GenericValue> openOrders = null;
        try {
            openOrders = EntityQuery.use(delegator).from("OrderHeaderAndItems")
                    .where(EntityCondition.makeCondition("orderTypeId", EntityOperator.EQUALS, "PURCHASE_ORDER"),
                            EntityCondition.makeCondition("itemStatusId", EntityOperator.NOT_EQUAL, "ITEM_CANCELLED"),
                            EntityCondition.makeCondition("itemStatusId", EntityOperator.NOT_EQUAL, "ITEM_REJECTED"),
                            EntityCondition.makeCondition("itemStatusId", EntityOperator.NOT_EQUAL, "ITEM_COMPLETED"),
                            EntityCondition.makeCondition("productId", EntityOperator.EQUALS, productId))
                    .queryList();
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
        }

        if (UtilValidate.isNotEmpty(openOrders)) {
            for (GenericValue order : openOrders) {
                BigDecimal thisQty = order.getBigDecimal("quantity");
                if (thisQty == null) {
                    thisQty = BigDecimal.ZERO;
                }
                quantity = quantity.add(thisQty);
            }
        }

        return quantity;
    }

    /**
     * Checks to see if this user has read permission on the specified order
     * @param security    the security
     * @param userLogin   The UserLogin value object to check
     * @param orderHeader The OrderHeader for the specified order
     * @return boolean True if we have read permission
     */
    public static boolean hasPermission(Security security, GenericValue userLogin, GenericValue orderHeader) {
        if (userLogin == null || orderHeader == null) {
            return false;
        }

        if (security.hasEntityPermission("ORDERMGR", "_VIEW", userLogin)) {
            return true;
        } else if (security.hasEntityPermission("ORDERMGR", "_ROLEVIEW", userLogin)) {
            List<GenericValue> orderRoles = null;
            try {
                orderRoles = orderHeader.getRelated("OrderRole", UtilMisc.toMap("partyId", userLogin.getString("partyId")), null, false);
            } catch (GenericEntityException e) {
                Debug.logError(e, "Cannot get OrderRole from OrderHeader", MODULE);
            }

            if (UtilValidate.isNotEmpty(orderRoles)) {
                // we are in at least one role
                return true;
            }
        }

        return false;
    }

    /**
     * Gets helper.
     * @param orderHeader the order header
     * @return the helper
     */
    public static OrderReadHelper getHelper(GenericValue orderHeader) {
        return new OrderReadHelper(orderHeader);
    }

    /**
     * Get orderAdjustments that have no corresponding returnAdjustment
     * It also handles the case of partial adjustment amount. Check OFBIZ-11185 for details
     * @return return the order adjustments that have no corresponding with return adjustment
     */
    public List<GenericValue> getAvailableOrderHeaderAdjustments() {
        List<GenericValue> orderHeaderAdjustments = this.getOrderHeaderAdjustments();
        List<GenericValue> filteredAdjustments = new LinkedList<>();
        for (GenericValue orderAdjustment : orderHeaderAdjustments) {
            BigDecimal returnedAmount = BigDecimal.ZERO;
            try {
                List<GenericValue> returnAdjustments = EntityQuery.use(orderHeader.getDelegator()).from("ReturnAdjustment")
                        .where("orderAdjustmentId", orderAdjustment.getString("orderAdjustmentId")).queryList();
                if (UtilValidate.isNotEmpty(returnAdjustments)) {
                    for (GenericValue returnAdjustment : returnAdjustments) {
                        returnedAmount = returnedAmount.add(returnAdjustment.getBigDecimal("amount"));
                    }
                }
            } catch (GenericEntityException e) {
                Debug.logError(e, MODULE);
            }
            if (orderAdjustment.getBigDecimal("amount").compareTo(returnedAmount) > 0) {
                orderAdjustment.set("amount", orderAdjustment.getBigDecimal("amount").subtract(returnedAmount));
                filteredAdjustments.add(orderAdjustment);
            }
        }
        return filteredAdjustments;
    }

    /**
     * Get the total return adjustments for a set of key -&gt; value condition pairs.  Done for code efficiency.
     * @param delegator the delegator
     * @param condition a map of the conditions to use
     * @return Get the total return adjustments
     */
    public static BigDecimal getReturnAdjustmentTotal(Delegator delegator, Map<String, Object> condition) {
        BigDecimal total = ZERO;
        List<GenericValue> adjustments;
        try {
            // TODO: find on a view-entity with a sum is probably more efficient
            adjustments = EntityQuery.use(delegator).from("ReturnAdjustment").where(condition).queryList();
            if (adjustments != null) {
                for (GenericValue returnAdjustment : adjustments) {
                    total = total.add(setScaleByType("RET_SALES_TAX_ADJ".equals(returnAdjustment.get("returnAdjustmentTypeId")),
                            returnAdjustment.getBigDecimal("amount")));
                }
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
        }
        return total;
    }

    /**
     * Sets scale by type.
     * @param isTax the is tax
     * @param value the value
     * @return the scale by type
     */
// little helper method to set the DECIMALS according to tax type
    public static BigDecimal setScaleByType(boolean isTax, BigDecimal value) {
        return isTax ? value.setScale(TAX_SCALE, TAX_ROUNDING) : value.setScale(DECIMALS, ROUNDING);
    }

    /**
     * Get the quantity of order items that have been invoiced  @param orderItem the order item
     * @return the order item invoiced quantity
     */
    public static BigDecimal getOrderItemInvoicedQuantity(GenericValue orderItem) {
        BigDecimal invoiced = BigDecimal.ZERO;
        try {
            // this is simply the sum of quantity billed in all related OrderItemBillings
            List<GenericValue> billings = orderItem.getRelated("OrderItemBilling", null, null, false);
            for (GenericValue billing : billings) {
                BigDecimal quantity = billing.getBigDecimal("quantity");
                if (quantity != null) {
                    invoiced = invoiced.add(quantity);
                }
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, e.getMessage(), MODULE);
        }
        return invoiced;
    }

    /**
     * Gets order payment statuses.
     * @return the order payment statuses
     */
    public List<GenericValue> getOrderPaymentStatuses() {
        return getOrderPaymentStatuses(getOrderStatuses());
    }

    /**
     * Gets order payment statuses.
     * @param orderStatuses the order statuses
     * @return the order payment statuses
     */
    public static List<GenericValue> getOrderPaymentStatuses(List<GenericValue> orderStatuses) {
        List<EntityExpr> contraints1 = UtilMisc.toList(EntityCondition.makeCondition("orderItemSeqId", EntityOperator.EQUALS, null));
        contraints1.add(EntityCondition.makeCondition("orderItemSeqId", EntityOperator.EQUALS, DataModelConstants.SEQ_ID_NA));
        contraints1.add(EntityCondition.makeCondition("orderItemSeqId", EntityOperator.EQUALS, ""));

        List<EntityExpr> contraints2 = UtilMisc.toList(EntityCondition.makeCondition("orderPaymentPreferenceId", EntityOperator.NOT_EQUAL, null));
        List<GenericValue> newOrderStatuses = new LinkedList<>();
        newOrderStatuses.addAll(EntityUtil.filterByOr(orderStatuses, contraints1));

        return EntityUtil.orderBy(EntityUtil.filterByAnd(newOrderStatuses, contraints2), UtilMisc.toList("-statusDatetime"));
    }

    /**
     * When you call this function after a OrderReadHelper instantiation
     * all OrderItemAttributes related to the orderHeader are load on local cache
     * to optimize database call, after we just filter the cache with attributeName and
     * orderItemSeqId wanted.
     * @param orderItemSeqId the order item seq id
     * @param attributeName  the attribute name
     * @return order item attribute
     */
    public String getOrderItemAttribute(String orderItemSeqId, String attributeName) {
        GenericValue orderItemAttribute = null;
        if (orderHeader != null) {
            if (orderItemAttributes == null) {
                try {
                    orderItemAttributes = EntityQuery.use(orderHeader.getDelegator())
                            .from("OrderItemAttribute")
                            .where("orderId", getOrderId())
                            .queryList();
                } catch (GenericEntityException e) {
                    Debug.logError(e, MODULE);
                }
            }
            orderItemAttribute = EntityUtil.getFirst(
                    EntityUtil.filterByAnd(orderItemAttributes,
                            UtilMisc.toMap("orderItemSeqId", orderItemSeqId, "attrName", attributeName)));
        }
        return orderItemAttribute != null ? orderItemAttribute.getString("attrValue") : null;
    }

    /**
     * Gets order item attribute.
     * @param orderItem     the order item
     * @param attributeName the attribute name
     * @return the order item attribute
     */
    public static String getOrderItemAttribute(GenericValue orderItem, String attributeName) {
        String attributeValue = null;
        if (orderItem != null) {
            try {
                GenericValue orderItemAttribute = EntityUtil.getFirst(orderItem.getRelated("OrderItemAttribute",
                        UtilMisc.toMap("attrName", attributeName), null, false));
                if (orderItemAttribute != null) {
                    attributeValue = orderItemAttribute.getString("attrValue");
                }
            } catch (GenericEntityException e) {
                Debug.logError(e, MODULE);
            }
        }
        return attributeValue;
    }

    /**
     * When you call this function after a OrderReadHelper instantiation
     * all OrderAttributes related to the orderHeader are load on local cache
     * to optimize database call, after we just filter the cache with attributeName wanted.
     * @param attributeName the attribute name
     * @return order attribute
     */
    public String getOrderAttribute(String attributeName) {
        GenericValue orderAttribute = null;
        if (orderHeader != null) {
            if (orderAttributeMap == null) {
                orderAttributeMap = new HashMap<>();
            }
            if (!orderAttributeMap.containsKey(attributeName)) {
                try {
                    orderAttribute = EntityQuery.use(orderHeader.getDelegator())
                            .from("OrderAttribute")
                            .where("orderId", getOrderId(), "attrName", attributeName)
                            .queryFirst();
                    if (orderAttribute != null) {
                        orderAttributeMap.put(attributeName, orderAttribute);
                    }
                } catch (GenericEntityException e) {
                    Debug.logError(e, MODULE);
                }
            } else {
                orderAttribute = orderAttributeMap.get(attributeName);
            }
        }
        return orderAttribute != null ? orderAttribute.getString("attrValue") : null;
    }

    /**
     * Gets order tax by tax auth geo and party.
     * @param orderAdjustments the order adjustments
     * @return the order tax by tax auth geo and party
     */
    public static Map<String, Object> getOrderTaxByTaxAuthGeoAndParty(List<GenericValue> orderAdjustments) {
        BigDecimal taxGrandTotal = BigDecimal.ZERO;
        List<Map<String, Object>> taxByTaxAuthGeoAndPartyList = new LinkedList<>();
        if (UtilValidate.isNotEmpty(orderAdjustments)) {
            // get orderAdjustment where orderAdjustmentTypeId is SALES_TAX.
            orderAdjustments = EntityUtil.filterByAnd(orderAdjustments, UtilMisc.toMap("orderAdjustmentTypeId", "SALES_TAX"));
            orderAdjustments = EntityUtil.orderBy(orderAdjustments, UtilMisc.toList("taxAuthGeoId", "taxAuthPartyId"));

            // get the list of all distinct taxAuthGeoId and taxAuthPartyId. It is for getting the number of taxAuthGeo and
            // taxAuthPartyId in adjustments.
            List<String> distinctTaxAuthGeoIdList = EntityUtil.getFieldListFromEntityList(orderAdjustments, "taxAuthGeoId", true);
            List<String> distinctTaxAuthPartyIdList = EntityUtil.getFieldListFromEntityList(orderAdjustments, "taxAuthPartyId", true);

            // Keep a list of amount that have been added to make sure none are missed (if taxAuth* information is missing)
            List<GenericValue> processedAdjustments = new LinkedList<>();
            // For each taxAuthGeoId get and add amount from orderAdjustment
            for (String taxAuthGeoId : distinctTaxAuthGeoIdList) {
                for (String taxAuthPartyId : distinctTaxAuthPartyIdList) {
                    //get all records for orderAdjustments filtered by taxAuthGeoId and taxAurhPartyId
                    List<GenericValue> orderAdjByTaxAuthGeoAndPartyIds = EntityUtil.filterByAnd(orderAdjustments, UtilMisc.toMap("taxAuthGeoId",
                            taxAuthGeoId, "taxAuthPartyId", taxAuthPartyId));
                    if (UtilValidate.isNotEmpty(orderAdjByTaxAuthGeoAndPartyIds)) {
                        BigDecimal totalAmount = BigDecimal.ZERO;
                        //Now for each orderAdjustment record get and add amount.
                        for (GenericValue orderAdjustment : orderAdjByTaxAuthGeoAndPartyIds) {
                            BigDecimal amount = orderAdjustment.getBigDecimal("amount");
                            if (amount == null) {
                                amount = ZERO;
                            }
                            totalAmount = totalAmount.add(amount).setScale(TAX_SCALE, TAX_ROUNDING);
                            processedAdjustments.add(orderAdjustment);
                        }
                        totalAmount = totalAmount.setScale(TAX_FINAL_SCALE, TAX_ROUNDING);
                        taxByTaxAuthGeoAndPartyList.add(UtilMisc.<String, Object>toMap("taxAuthPartyId", taxAuthPartyId, "taxAuthGeoId",
                                taxAuthGeoId, "totalAmount", totalAmount));
                        taxGrandTotal = taxGrandTotal.add(totalAmount);
                    }
                }
            }
            // Process any adjustments that got missed
            List<GenericValue> missedAdjustments = new LinkedList<>();
            missedAdjustments.addAll(orderAdjustments);
            missedAdjustments.removeAll(processedAdjustments);
            for (GenericValue orderAdjustment : missedAdjustments) {
                taxGrandTotal = taxGrandTotal.add(orderAdjustment.getBigDecimal("amount").setScale(TAX_SCALE, TAX_ROUNDING));
            }
            taxGrandTotal = taxGrandTotal.setScale(TAX_FINAL_SCALE, TAX_ROUNDING);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("taxByTaxAuthGeoAndPartyList", taxByTaxAuthGeoAndPartyList);
        result.put("taxGrandTotal", taxGrandTotal);
        return result;
    }

    /**
     * Gets order item tax by tax auth geo and party for display.
     * @param orderItem                the order item
     * @param orderAdjustmentsOriginal the order adjustments original
     * @return the order item tax by tax auth geo and party for display
     */
    public static Map<String, Object> getOrderItemTaxByTaxAuthGeoAndPartyForDisplay(GenericValue orderItem,
                                                                                    List<GenericValue> orderAdjustmentsOriginal) {
        return getOrderTaxByTaxAuthGeoAndPartyForDisplay(getOrderItemAdjustmentList(orderItem, orderAdjustmentsOriginal));
    }

    /**
     * Gets order tax by tax auth geo and party for display.
     * @param orderAdjustmentsOriginal the order adjustments original
     * @return the order tax by tax auth geo and party for display
     */
    public static Map<String, Object> getOrderTaxByTaxAuthGeoAndPartyForDisplay(List<GenericValue> orderAdjustmentsOriginal) {
        BigDecimal taxGrandTotal = BigDecimal.ZERO;
        List<Map<String, Object>> taxByTaxAuthGeoAndPartyList = new LinkedList<>();
        List<GenericValue> orderAdjustmentsToUse = new LinkedList<>();
        if (UtilValidate.isNotEmpty(orderAdjustmentsOriginal)) {
            // get orderAdjustment where orderAdjustmentTypeId is SALES_TAX.
            orderAdjustmentsToUse.addAll(EntityUtil.filterByAnd(orderAdjustmentsOriginal, UtilMisc.toMap("orderAdjustmentTypeId", "SALES_TAX")));
            orderAdjustmentsToUse.addAll(EntityUtil.filterByAnd(orderAdjustmentsOriginal, UtilMisc.toMap("orderAdjustmentTypeId", "VAT_TAX")));
            orderAdjustmentsToUse = EntityUtil.orderBy(orderAdjustmentsToUse, UtilMisc.toList("taxAuthGeoId", "taxAuthPartyId"));

            // get the list of all distinct taxAuthGeoId and taxAuthPartyId. It is for getting the number of taxAuthGeo and taxAuthPartyId in
            // adjustments.
            List<String> distinctTaxAuthGeoIdList = EntityUtil.getFieldListFromEntityList(orderAdjustmentsToUse, "taxAuthGeoId", true);
            List<String> distinctTaxAuthPartyIdList = EntityUtil.getFieldListFromEntityList(orderAdjustmentsToUse, "taxAuthPartyId", true);

            // Keep a list of amount that have been added to make sure none are missed (if taxAuth* information is missing)
            List<GenericValue> processedAdjustments = new LinkedList<>();
            // For each taxAuthGeoId get and add amount from orderAdjustment
            for (String taxAuthGeoId : distinctTaxAuthGeoIdList) {
                for (String taxAuthPartyId : distinctTaxAuthPartyIdList) {
                    //get all records for orderAdjustments filtered by taxAuthGeoId and taxAurhPartyId
                    List<GenericValue> orderAdjByTaxAuthGeoAndPartyIds = EntityUtil.filterByAnd(orderAdjustmentsToUse,
                            UtilMisc.toMap("taxAuthGeoId", taxAuthGeoId, "taxAuthPartyId", taxAuthPartyId));
                    if (UtilValidate.isNotEmpty(orderAdjByTaxAuthGeoAndPartyIds)) {
                        BigDecimal totalAmount = BigDecimal.ZERO;
                        //Now for each orderAdjustment record get and add amount.
                        for (GenericValue orderAdjustment : orderAdjByTaxAuthGeoAndPartyIds) {
                            BigDecimal amount = orderAdjustment.getBigDecimal("amount");
                            if (amount != null) {
                                totalAmount = totalAmount.add(amount);
                            }
                            if ("VAT_TAX".equals(orderAdjustment.getString("orderAdjustmentTypeId"))
                                    && orderAdjustment.get("amountAlreadyIncluded") != null) {
                                // this is the only case where the VAT_TAX amountAlreadyIncluded should be added in, and should just be for display
                                // and not to calculate the order grandTotal
                                totalAmount = totalAmount.add(orderAdjustment.getBigDecimal("amountAlreadyIncluded"));
                            }
                            totalAmount = totalAmount.setScale(TAX_SCALE, TAX_ROUNDING);
                            processedAdjustments.add(orderAdjustment);
                        }
                        totalAmount = totalAmount.setScale(TAX_FINAL_SCALE, TAX_ROUNDING);
                        taxByTaxAuthGeoAndPartyList.add(UtilMisc.<String, Object>toMap("taxAuthPartyId", taxAuthPartyId, "taxAuthGeoId",
                                taxAuthGeoId, "totalAmount", totalAmount));
                        taxGrandTotal = taxGrandTotal.add(totalAmount);
                    }
                }
            }
            // Process any adjustments that got missed
            List<GenericValue> missedAdjustments = new LinkedList<>();
            missedAdjustments.addAll(orderAdjustmentsToUse);
            missedAdjustments.removeAll(processedAdjustments);
            for (GenericValue orderAdjustment : missedAdjustments) {
                taxGrandTotal = taxGrandTotal.add(orderAdjustment.getBigDecimal("amount").setScale(TAX_SCALE, TAX_ROUNDING));
            }
            taxGrandTotal = taxGrandTotal.setScale(TAX_FINAL_SCALE, TAX_ROUNDING);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("taxByTaxAuthGeoAndPartyList", taxByTaxAuthGeoAndPartyList);
        result.put("taxGrandTotal", taxGrandTotal);
        return result;
    }

    /**
     * Calculates the "available" balance of a billing account, which is the
     * net balance minus amount of pending (not cancelled, rejected, or received) order payments.
     * When looking at using a billing account for a new order, you should use this method.
     * @param billingAccount the billing account record
     * @return return the "available" balance of a billing account
     * @throws GenericEntityException the generic entity exception
     */
    public static BigDecimal getBillingAccountBalance(GenericValue billingAccount) throws GenericEntityException {

        Delegator delegator = billingAccount.getDelegator();
        String billingAccountId = billingAccount.getString("billingAccountId");

        BigDecimal balance = ZERO;
        BigDecimal accountLimit = getAccountLimit(billingAccount);
        balance = balance.add(accountLimit);
        // pending (not cancelled, rejected, or received) order payments
        List<GenericValue> orderPaymentPreferenceSums = EntityQuery.use(delegator)
                .select("maxAmount")
                .from("OrderPurchasePaymentSummary")
                .where(EntityCondition.makeCondition("billingAccountId", EntityOperator.EQUALS, billingAccountId),
                        EntityCondition.makeCondition("paymentMethodTypeId", EntityOperator.EQUALS, "EXT_BILLACT"),
                        EntityCondition.makeCondition("statusId", EntityOperator.NOT_IN, UtilMisc.toList("ORDER_CANCELLED", "ORDER_REJECTED")),
                        EntityCondition.makeCondition("preferenceStatusId", EntityOperator.NOT_IN, UtilMisc.toList("PAYMENT_SETTLED",
                                "PAYMENT_RECEIVED", "PAYMENT_DECLINED", "PAYMENT_CANCELLED"))) // PAYMENT_NOT_AUTH
                .queryList();

        for (GenericValue orderPaymentPreferenceSum : orderPaymentPreferenceSums) {
            BigDecimal maxAmount = orderPaymentPreferenceSum.getBigDecimal("maxAmount");
            balance = maxAmount != null ? balance.subtract(maxAmount) : balance;
        }

        List<GenericValue> paymentAppls = EntityQuery.use(delegator).from("PaymentApplication").where("billingAccountId", billingAccountId)
                .queryList();
        // TODO: cancelled payments?
        for (GenericValue paymentAppl : paymentAppls) {
            if (paymentAppl.getString("invoiceId") == null) {
                BigDecimal amountApplied = paymentAppl.getBigDecimal("amountApplied");
                balance = balance.add(amountApplied);
            }
        }

        balance = balance.setScale(DECIMALS, ROUNDING);
        return balance;
    }

    /**
     * Returns the accountLimit of the BillingAccount or BigDecimal ZERO if it is null
     * @param billingAccount the billing account
     * @return the account limit
     * @throws GenericEntityException the generic entity exception
     */
    public static BigDecimal getAccountLimit(GenericValue billingAccount) throws GenericEntityException {
        if (billingAccount.getBigDecimal("accountLimit") != null) {
            return billingAccount.getBigDecimal("accountLimit");
        }
        Debug.logWarning("Billing Account [" + billingAccount.getString("billingAccountId")
                + "] does not have an account limit defined, assuming zero.", MODULE);
        return ZERO;
    }

    /**
     * Gets shippable sizes.
     * @param shipGrouSeqId the ship grou seq id
     * @return the shippable sizes
     */
    public List<BigDecimal> getShippableSizes(String shipGrouSeqId) {
        List<BigDecimal> shippableSizes = new ArrayList<>();

        List<GenericValue> validItems = getValidOrderItems(shipGrouSeqId);
        if (validItems != null) {
            Iterator<GenericValue> i = validItems.iterator();
            while (i.hasNext()) {
                GenericValue item = i.next();
                shippableSizes.add(this.getItemSize(item));
            }
        }
        return shippableSizes;
    }

    /**
     * Gets item received quantity.
     * @param orderItem the order item
     * @return the item received quantity
     */
    public BigDecimal getItemReceivedQuantity(GenericValue orderItem) {
        BigDecimal totalReceived = BigDecimal.ZERO;
        try {
            if (orderItem != null) {
                EntityCondition cond = EntityCondition.makeCondition(UtilMisc.toList(
                        EntityCondition.makeCondition("orderId", orderItem.getString("orderId")),
                        EntityCondition.makeCondition("quantityAccepted", EntityOperator.GREATER_THAN, BigDecimal.ZERO),
                        EntityCondition.makeCondition("orderItemSeqId", orderItem.getString("orderItemSeqId"))));
                Delegator delegator = orderItem.getDelegator();
                List<GenericValue> shipmentReceipts = EntityQuery.use(delegator).select("quantityAccepted", "quantityRejected")
                        .from("ShipmentReceiptAndItem").where(cond).queryList();
                for (GenericValue shipmentReceipt : shipmentReceipts) {
                    if (shipmentReceipt.getBigDecimal("quantityAccepted") != null) {
                        totalReceived = totalReceived.add(shipmentReceipt.getBigDecimal("quantityAccepted"));
                    }
                    if (shipmentReceipt.getBigDecimal("quantityRejected") != null) {
                        totalReceived = totalReceived.add(shipmentReceipt.getBigDecimal("quantityRejected"));
                    }
                }
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
        }
        return totalReceived;
    }

}
