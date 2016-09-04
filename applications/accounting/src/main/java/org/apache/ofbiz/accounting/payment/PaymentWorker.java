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
package org.apache.ofbiz.accounting.payment;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletRequest;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilFormatOut;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilNumber;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityExpr;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.EntityUtil;


/**
 * Worker methods for Payments
 */
public final class PaymentWorker {

    public static final String module = PaymentWorker.class.getName();
    private static final int decimals = UtilNumber.getBigDecimalScale("invoice.decimals");
    private static final int rounding = UtilNumber.getBigDecimalRoundingMode("invoice.rounding");

    private PaymentWorker() {}

    // to be able to use in minilanguage where Boolean cannot be used
    public static List<Map<String, GenericValue>> getPartyPaymentMethodValueMaps(Delegator delegator, String partyId) {
        return(getPartyPaymentMethodValueMaps(delegator, partyId, false));
    }

    public static List<Map<String, GenericValue>> getPartyPaymentMethodValueMaps(Delegator delegator, String partyId, Boolean showOld) {
        List<Map<String, GenericValue>> paymentMethodValueMaps = new LinkedList<Map<String,GenericValue>>();
        try {
            List<GenericValue> paymentMethods = EntityQuery.use(delegator).from("PaymentMethod").where("partyId", partyId).queryList();

            if (!showOld) paymentMethods = EntityUtil.filterByDate(paymentMethods, true);

            for (GenericValue paymentMethod : paymentMethods) {
                Map<String, GenericValue> valueMap = new HashMap<String, GenericValue>();

                paymentMethodValueMaps.add(valueMap);
                valueMap.put("paymentMethod", paymentMethod);
                if ("CREDIT_CARD".equals(paymentMethod.getString("paymentMethodTypeId"))) {
                    GenericValue creditCard = paymentMethod.getRelatedOne("CreditCard", false);
                    if (creditCard != null) valueMap.put("creditCard", creditCard);
                } else if ("GIFT_CARD".equals(paymentMethod.getString("paymentMethodTypeId"))) {
                    GenericValue giftCard = paymentMethod.getRelatedOne("GiftCard", false);
                    if (giftCard != null) valueMap.put("giftCard", giftCard);
                } else if ("EFT_ACCOUNT".equals(paymentMethod.getString("paymentMethodTypeId"))) {
                    GenericValue eftAccount = paymentMethod.getRelatedOne("EftAccount", false);
                    if (eftAccount != null) valueMap.put("eftAccount", eftAccount);
                } else if ("COMPANY_CHECK".equals(paymentMethod.getString("paymentMethodTypeId"))) {
                    GenericValue companyCheckAccount = paymentMethod.getRelatedOne("CheckAccount", false);
                    if (companyCheckAccount != null) valueMap.put("companyCheckAccount", companyCheckAccount);
                } else if ("PERSONAL_CHECK".equals(paymentMethod.getString("paymentMethodTypeId"))) {
                    GenericValue personalCheckAccount = paymentMethod.getRelatedOne("CheckAccount", false);
                    if (personalCheckAccount != null) valueMap.put("personalCheckAccount", personalCheckAccount);
                } else if ("CERTIFIED_CHECK".equals(paymentMethod.getString("paymentMethodTypeId"))) {
                    GenericValue certifiedCheckAccount = paymentMethod.getRelatedOne("CheckAccount", false);
                    if (certifiedCheckAccount != null) valueMap.put("certifiedCheckAccount", certifiedCheckAccount);
                }
            }
        } catch (GenericEntityException e) {
            Debug.logWarning(e, module);
        }
        return paymentMethodValueMaps;
    }

    public static Map<String, Object> getPaymentMethodAndRelated(ServletRequest request, String partyId) {
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        Map<String, Object> results = new HashMap<String, Object>();

        Boolean tryEntity = true;
        if (request.getAttribute("_ERROR_MESSAGE_") != null) tryEntity = false;

        String donePage = request.getParameter("DONE_PAGE");
        if (UtilValidate.isEmpty(donePage))
            donePage = "viewprofile";
        results.put("donePage", donePage);

        String paymentMethodId = request.getParameter("paymentMethodId");

        // check for a create
        if (request.getAttribute("paymentMethodId") != null) {
            paymentMethodId = (String) request.getAttribute("paymentMethodId");
        }

        results.put("paymentMethodId", paymentMethodId);

        GenericValue paymentMethod = null;
        GenericValue creditCard = null;
        GenericValue giftCard = null;
        GenericValue eftAccount = null;
        GenericValue checkAccount = null;

        if (UtilValidate.isNotEmpty(paymentMethodId)) {
            try {
                paymentMethod = EntityQuery.use(delegator).from("PaymentMethod").where("paymentMethodId", paymentMethodId).queryOne();
                creditCard = EntityQuery.use(delegator).from("CreditCard").where("paymentMethodId", paymentMethodId).queryOne();
                giftCard = EntityQuery.use(delegator).from("GiftCard").where("paymentMethodId", paymentMethodId).queryOne();
                eftAccount = EntityQuery.use(delegator).from("EftAccount").where("paymentMethodId", paymentMethodId).queryOne();
                checkAccount = EntityQuery.use(delegator).from("CheckAccount").where("paymentMethodId", paymentMethodId).queryOne();
            } catch (GenericEntityException e) {
                Debug.logWarning(e, module);
            }
        }
        if (paymentMethod != null) {
            results.put("paymentMethod", paymentMethod);
        } else {
            tryEntity = false;
        }

        if (creditCard != null) {
            results.put("creditCard", creditCard);
        }
        if (giftCard != null) {
            results.put("giftCard", giftCard);
        }
        if (eftAccount != null) {
            results.put("eftAccount", eftAccount);
        }
        if (checkAccount != null) {
            results.put("checkAccount", checkAccount);
        }

        String curContactMechId = null;

        if (creditCard != null) {
            curContactMechId = UtilFormatOut.checkNull(tryEntity ? creditCard.getString("contactMechId") : request.getParameter("contactMechId"));
        } else if (giftCard != null) {
            curContactMechId = UtilFormatOut.checkNull(tryEntity ? giftCard.getString("contactMechId") : request.getParameter("contactMechId"));
        } else if (eftAccount != null) {
            curContactMechId = UtilFormatOut.checkNull(tryEntity ? eftAccount.getString("contactMechId") : request.getParameter("contactMechId"));
        }  else if (checkAccount != null) {
            curContactMechId = UtilFormatOut.checkNull(tryEntity ? checkAccount.getString("contactMechId") : request.getParameter("contactMechId"));
        }
        if (curContactMechId != null) {
            results.put("curContactMechId", curContactMechId);
        }

        results.put("tryEntity", tryEntity);

        return results;
    }

    public static GenericValue getPaymentAddress(Delegator delegator, String partyId) {
        GenericValue purpose = null;
        try {
            purpose = EntityQuery.use(delegator).from("PartyContactWithPurpose")
                    .where("partyId", partyId, "contactMechPurposeTypeId", "PAYMENT_LOCATION")
                    .orderBy("-purposeFromDate").filterByDate("contactFromDate", "contactThruDate", "purposeFromDate", "purposeThruDate")
                    .queryFirst();
        } catch (GenericEntityException e) {
            Debug.logError(e, "Trouble getting PartyContactWithPurpose view entity list", module);
        }

        // get the address for the primary contact mech
        GenericValue postalAddress = null;
        if (purpose != null) {
            try {
                postalAddress = EntityQuery.use(delegator).from("PostalAddress").where("contactMechId", purpose.getString("contactMechId")).queryOne();
            } catch (GenericEntityException e) {
                Debug.logError(e, "Trouble getting PostalAddress record for contactMechId: " + purpose.getString("contactMechId"), module);
            }
        }

        return postalAddress;
    }

    /**
     * Returns the total from a list of Payment entities
     *
     * @param payments List of Payment GenericValue items
     * @return total payments as BigDecimal
     */

    public static BigDecimal getPaymentsTotal(List<GenericValue> payments) {
        if (payments == null) {
            throw new IllegalArgumentException("Payment list cannot be null");
        }

        BigDecimal paymentsTotal = BigDecimal.ZERO;
        for (GenericValue payment : payments) {
            paymentsTotal = paymentsTotal.add(payment.getBigDecimal("amount")).setScale(decimals, rounding);
        }
        return paymentsTotal;
    }

    /**
     * Method to return the total amount of an payment which is applied to a payment
     * @param delegator the delegator
     * @param paymentId paymentId of the Payment
     * @return the applied total as BigDecimal
     */
    public static BigDecimal getPaymentApplied(Delegator delegator, String paymentId) {
        return getPaymentApplied(delegator, paymentId, false);
    }

    public static BigDecimal getPaymentApplied(Delegator delegator, String paymentId, Boolean actual) {
        if (delegator == null) {
            throw new IllegalArgumentException("Null delegator is not allowed in this method");
        }

        GenericValue payment = null;
        try {
            payment = EntityQuery.use(delegator).from("Payment").where("paymentId", paymentId).queryOne();
        } catch (GenericEntityException e) {
            Debug.logError(e, "Problem getting Payment", module);
        }

        if (payment == null) {
            throw new IllegalArgumentException("The paymentId passed does not match an existing payment");
        }

        return getPaymentApplied(payment, actual);
    }
    /**
     * Method to return the amount applied converted to the currency of payment
     * @param paymentApplicationId the payment application id
     * @return appliedAmount the applied amount as BigDecimal
     */
    public static BigDecimal getPaymentAppliedAmount(Delegator delegator, String paymentApplicationId) {
        GenericValue paymentApplication = null;
        BigDecimal appliedAmount = BigDecimal.ZERO;
        try {
            paymentApplication = EntityQuery.use(delegator).from("PaymentApplication").where("paymentApplicationId", paymentApplicationId).queryOne();
            appliedAmount = paymentApplication.getBigDecimal("amountApplied");
            if (paymentApplication.get("paymentId") != null) {
                GenericValue payment = paymentApplication.getRelatedOne("Payment", false);
                if (paymentApplication.get("invoiceId") != null && payment.get("actualCurrencyAmount") != null && payment.get("actualCurrencyUomId") != null) {
                    GenericValue invoice = paymentApplication.getRelatedOne("Invoice", false);
                    if (payment.getString("actualCurrencyUomId").equals(invoice.getString("currencyUomId"))) {
                           appliedAmount = appliedAmount.multiply(payment.getBigDecimal("amount")).divide(payment.getBigDecimal("actualCurrencyAmount"),new MathContext(100));
                    }
                }
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, "Problem getting Payment", module);
        }
        return appliedAmount;
    }

    /**
     * Method to return the total amount of an payment which is applied to a payment
     * @param payment GenericValue object of the Payment
     * @return the applied total as BigDecimal in the currency of the payment
     */
    public static BigDecimal getPaymentApplied(GenericValue payment) {
        return getPaymentApplied(payment, false);
    }

    /**
     * Method to return the total amount of a payment which is applied to a payment
     * @param payment GenericValue object of the Payment
     * @param actual false for currency of the payment, true for the actual currency
     * @return the applied total as BigDecimal in the currency of the payment
     */
    public static BigDecimal getPaymentApplied(GenericValue payment, Boolean actual) {
        BigDecimal paymentApplied = BigDecimal.ZERO;
        List<GenericValue> paymentApplications = null;
        try {
            List<EntityExpr> cond = UtilMisc.toList(
                    EntityCondition.makeCondition("paymentId", EntityOperator.EQUALS, payment.getString("paymentId")),
                    EntityCondition.makeCondition("toPaymentId", EntityOperator.EQUALS, payment.getString("paymentId"))
                   );
            EntityCondition partyCond = EntityCondition.makeCondition(cond, EntityOperator.OR);
            paymentApplications = payment.getDelegator().findList("PaymentApplication", partyCond, null, UtilMisc.toList("invoiceId", "billingAccountId"), null, false);
            if (UtilValidate.isNotEmpty(paymentApplications)) {
                for (GenericValue paymentApplication : paymentApplications) {
                    BigDecimal amountApplied = paymentApplication.getBigDecimal("amountApplied");
                    // check currency invoice and if different convert amount applied for display
                    if (actual.equals(Boolean.FALSE) && paymentApplication.get("invoiceId") != null && payment.get("actualCurrencyAmount") != null && payment.get("actualCurrencyUomId") != null) {
                        GenericValue invoice = paymentApplication.getRelatedOne("Invoice", false);
                        if (payment.getString("actualCurrencyUomId").equals(invoice.getString("currencyUomId"))) {
                               amountApplied = amountApplied.multiply(payment.getBigDecimal("amount")).divide(payment.getBigDecimal("actualCurrencyAmount"),new MathContext(100));
                        }
                    }
                    paymentApplied = paymentApplied.add(amountApplied).setScale(decimals,rounding);
                }
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, "Trouble getting entities", module);
        }
        return paymentApplied;
    }

    public static BigDecimal getPaymentNotApplied(GenericValue payment) {
        if (payment != null) { 
            return payment.getBigDecimal("amount").subtract(getPaymentApplied(payment)).setScale(decimals,rounding);
        } 
        return BigDecimal.ZERO;
    }

    public static BigDecimal getPaymentNotApplied(GenericValue payment, Boolean actual) {
        if (actual.equals(Boolean.TRUE) && UtilValidate.isNotEmpty(payment.getBigDecimal("actualCurrencyAmount"))) {
            return payment.getBigDecimal("actualCurrencyAmount").subtract(getPaymentApplied(payment, actual)).setScale(decimals,rounding);
        }
            return payment.getBigDecimal("amount").subtract(getPaymentApplied(payment)).setScale(decimals,rounding);
    }

    public static BigDecimal getPaymentNotApplied(Delegator delegator, String paymentId) {
        return getPaymentNotApplied(delegator,paymentId, false);
    }

    public static BigDecimal getPaymentNotApplied(Delegator delegator, String paymentId, Boolean actual) {
        if (delegator == null) {
            throw new IllegalArgumentException("Null delegator is not allowed in this method");
        }

        GenericValue payment = null;
        try {
            payment = EntityQuery.use(delegator).from("Payment").where("paymentId", paymentId).queryOne();
        } catch (GenericEntityException e) {
            Debug.logError(e, "Problem getting Payment", module);
        }

        if (payment == null) {
            throw new IllegalArgumentException("The paymentId passed does not match an existing payment");
        }
        return payment.getBigDecimal("amount").subtract(getPaymentApplied(delegator,paymentId, actual)).setScale(decimals,rounding);
    }
}
