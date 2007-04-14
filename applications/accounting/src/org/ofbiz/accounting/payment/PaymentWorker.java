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
package org.ofbiz.accounting.payment;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletRequest;
import javax.servlet.jsp.PageContext;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilFormatOut;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilNumber;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.util.EntityUtil;

/**
 * Worker methods for Payments
 */
public class PaymentWorker {
    
    public static final String module = PaymentWorker.class.getName();
    private static int decimals = UtilNumber.getBigDecimalScale("invoice.decimals");
    private static int rounding = UtilNumber.getBigDecimalRoundingMode("invoice.rounding");
    
    public static void getPartyPaymentMethodValueMaps(PageContext pageContext, String partyId, boolean showOld, String paymentMethodValueMapsAttr) {
        GenericDelegator delegator = (GenericDelegator) pageContext.getRequest().getAttribute("delegator");
        List paymentMethodValueMaps = getPartyPaymentMethodValueMaps(delegator, partyId, showOld);
        pageContext.setAttribute(paymentMethodValueMapsAttr, paymentMethodValueMaps);
    }

    // to be able to use in minilanguage where boolean cannot be used
    public static List getPartyPaymentMethodValueMaps(GenericDelegator delegator, String partyId) {
        return(getPartyPaymentMethodValueMaps(delegator, partyId, false)); 
    }
    
    public static List getPartyPaymentMethodValueMaps(GenericDelegator delegator, String partyId, boolean showOld) {
        List paymentMethodValueMaps = new LinkedList();
        try {
            List paymentMethods = delegator.findByAnd("PaymentMethod", UtilMisc.toMap("partyId", partyId));

            if (!showOld) paymentMethods = EntityUtil.filterByDate(paymentMethods, true);
            if (paymentMethods != null) {
                Iterator pmIter = paymentMethods.iterator();

                while (pmIter.hasNext()) {
                    GenericValue paymentMethod = (GenericValue) pmIter.next();
                    Map valueMap = new HashMap();

                    paymentMethodValueMaps.add(valueMap);
                    valueMap.put("paymentMethod", paymentMethod);
                    if ("CREDIT_CARD".equals(paymentMethod.getString("paymentMethodTypeId"))) {
                        GenericValue creditCard = paymentMethod.getRelatedOne("CreditCard");
                        if (creditCard != null) valueMap.put("creditCard", creditCard);
                    } else if ("GIFT_CARD".equals(paymentMethod.getString("paymentMethodTypeId"))) {
                        GenericValue giftCard = paymentMethod.getRelatedOne("GiftCard");
                        if (giftCard != null) valueMap.put("giftCard", giftCard);
                    } else if ("EFT_ACCOUNT".equals(paymentMethod.getString("paymentMethodTypeId"))) {
                        GenericValue eftAccount = paymentMethod.getRelatedOne("EftAccount");
                        if (eftAccount != null) valueMap.put("eftAccount", eftAccount);
                    }
                }
            }
        } catch (GenericEntityException e) {
            Debug.logWarning(e, module);
        }
        return paymentMethodValueMaps;
    }

    /** TODO: REMOVE (DEJ 20030301): This is the OLD style and should be removed when the eCommerce and party mgr JSPs are */
    public static void getPaymentMethodAndRelated(PageContext pageContext, String partyId,
            String paymentMethodAttr, String creditCardAttr, String eftAccountAttr, String paymentMethodIdAttr, String curContactMechIdAttr,
            String donePageAttr, String tryEntityAttr) {

        ServletRequest request = pageContext.getRequest();
        Map results = getPaymentMethodAndRelated(request, partyId);
        
        if (results.get("paymentMethod") != null) pageContext.setAttribute(paymentMethodAttr, results.get("paymentMethod"));
        if (results.get("creditCard") != null) pageContext.setAttribute(creditCardAttr, results.get("creditCard"));
        if (results.get("eftAccount") != null) pageContext.setAttribute(eftAccountAttr, results.get("eftAccount"));
        if (results.get("paymentMethodId") != null) pageContext.setAttribute(paymentMethodIdAttr, results.get("paymentMethodId"));
        if (results.get("curContactMechId") != null) pageContext.setAttribute(curContactMechIdAttr, results.get("curContactMechId"));
        if (results.get("donePage") != null) pageContext.setAttribute(donePageAttr, results.get("donePage"));
        if (results.get("tryEntity") != null) pageContext.setAttribute(tryEntityAttr, results.get("tryEntity"));
    }
    
    public static Map getPaymentMethodAndRelated(ServletRequest request, String partyId) {
        GenericDelegator delegator = (GenericDelegator) request.getAttribute("delegator");
        Map results = new HashMap();
        
        boolean tryEntity = true;
        if (request.getAttribute("_ERROR_MESSAGE_") != null) tryEntity = false;

        String donePage = request.getParameter("DONE_PAGE");
        if (donePage == null || donePage.length() <= 0)
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

        if (UtilValidate.isNotEmpty(paymentMethodId)) {
            try {
                paymentMethod = delegator.findByPrimaryKey("PaymentMethod", UtilMisc.toMap("paymentMethodId", paymentMethodId));
                creditCard = delegator.findByPrimaryKey("CreditCard", UtilMisc.toMap("paymentMethodId", paymentMethodId));
                giftCard = delegator.findByPrimaryKey("GiftCard", UtilMisc.toMap("paymentMethodId", paymentMethodId));
                eftAccount = delegator.findByPrimaryKey("EftAccount", UtilMisc.toMap("paymentMethodId", paymentMethodId));
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

        String curContactMechId = null;

        if (creditCard != null) {
            curContactMechId = UtilFormatOut.checkNull(tryEntity ? creditCard.getString("contactMechId") : request.getParameter("contactMechId"));
        } else if (giftCard != null) {
            curContactMechId = UtilFormatOut.checkNull(tryEntity ? giftCard.getString("contactMechId") : request.getParameter("contactMechId"));
        } else if (eftAccount != null) {
            curContactMechId = UtilFormatOut.checkNull(tryEntity ? eftAccount.getString("contactMechId") : request.getParameter("contactMechId"));
        }
        if (curContactMechId != null) {
            results.put("curContactMechId", curContactMechId);
        }

        results.put("tryEntity", new Boolean(tryEntity));
        
        return results;
    }
            
    public static GenericValue getPaymentAddress(GenericDelegator delegator, String partyId) {                
        List paymentAddresses = null;        
        try {
            paymentAddresses = delegator.findByAnd("PartyContactMechPurpose", 
                UtilMisc.toMap("partyId", partyId, "contactMechPurposeTypeId", "PAYMENT_LOCATION"), 
                UtilMisc.toList("-fromDate"));
            paymentAddresses = EntityUtil.filterByDate(paymentAddresses);                
        } catch (GenericEntityException e) {
            Debug.logError(e, "Trouble getting PartyContactMechPurpose entity list", module);            
        }
        
        // get the address for the primary contact mech
        GenericValue purpose = EntityUtil.getFirst(paymentAddresses);
        GenericValue postalAddress = null;
        if (purpose != null) {
            try {
                postalAddress = delegator.findByPrimaryKey("PostalAddress", UtilMisc.toMap("contactMechId", purpose.getString("contactMechId")));
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

    public static BigDecimal getPaymentsTotal(List payments) {
        if (payments == null) {
            throw new IllegalArgumentException("Payment list cannot be null");            
        }

        BigDecimal paymentsTotal = new BigDecimal("0");
        Iterator i = payments.iterator();
        while (i.hasNext()) {
            GenericValue payment = (GenericValue) i.next();
            paymentsTotal = paymentsTotal.add(payment.getBigDecimal("amount")).setScale(decimals, rounding);
        }
        return paymentsTotal;
    }

    /**
     * Method to return the total amount of an payment which is applied to a payment
     * @param payment GenericValue object of the Payment
     * @return the applied total as double
     */
    public static double getPaymentApplied(GenericDelegator delegator, String paymentId) {
        return getPaymentAppliedBd(delegator, paymentId).doubleValue(); 
    }
    
    public static BigDecimal getPaymentAppliedBd(GenericDelegator delegator, String paymentId) {
        if (delegator == null) {
            throw new IllegalArgumentException("Null delegator is not allowed in this method");
        }
        
        GenericValue payment = null;
        try {
            payment = delegator.findByPrimaryKey("Payment", UtilMisc.toMap("paymentId", paymentId));    
        } catch (GenericEntityException e) {
            Debug.logError(e, "Problem getting Payment", module);
        }
        
        if (payment == null) {
            throw new IllegalArgumentException("The paymentId passed does not match an existing payment");
        }
        
        return getPaymentAppliedBd(payment);
    }
    /**
     * Method to return the total amount of an payment which is applied to a payment
     * @param payment GenericValue object of the Payment
     * @return the applied total as double
     */
    public static double getPaymentApplied(GenericValue payment) {
        return getPaymentAppliedBd(payment).doubleValue();
    }

    public static BigDecimal getPaymentAppliedBd(GenericValue payment) {
        BigDecimal paymentApplied = new BigDecimal("0");
        List paymentApplications = null;
        try {
            paymentApplications = payment.getRelated("PaymentApplication");
        } catch (GenericEntityException e) {
            Debug.logError(e, "Trouble getting paymentApplicationlist", module);            
        }
        if (paymentApplications != null && paymentApplications.size() > 0) {
            Iterator p = paymentApplications.iterator();
            while (p.hasNext()) {
                GenericValue paymentApplication = (GenericValue) p.next();
                paymentApplied = paymentApplied.add(paymentApplication.getBigDecimal("amountApplied")).setScale(decimals,rounding);
            }
        }
        // check for payment to payment applications
        paymentApplications = null;
        try {
            paymentApplications = payment.getRelated("ToPaymentApplication");
        } catch (GenericEntityException e) {
            Debug.logError(e, "Trouble getting the 'to' paymentApplicationlist", module);            
        }
        if (paymentApplications != null && paymentApplications.size() > 0) {
            Iterator p = paymentApplications.iterator();
            while (p.hasNext()) {
                GenericValue paymentApplication = (GenericValue) p.next();
                paymentApplied = paymentApplied.add(paymentApplication.getBigDecimal("amountApplied")).setScale(decimals,rounding);
            }
        }
        return paymentApplied;        
    }
    public static double getPaymentNotApplied(GenericValue payment) {
        return getPaymentNotAppliedBd(payment).doubleValue();
    }

    public static BigDecimal getPaymentNotAppliedBd(GenericValue payment) {
        return payment.getBigDecimal("amount").subtract(getPaymentAppliedBd(payment)).setScale(decimals,rounding);
    }
    public static double getPaymentNotApplied(GenericDelegator delegator, String paymentId) {
        return getPaymentNotAppliedBd(delegator,paymentId).doubleValue();
    }

    public static BigDecimal getPaymentNotAppliedBd(GenericDelegator delegator, String paymentId) {
        if (delegator == null) {
            throw new IllegalArgumentException("Null delegator is not allowed in this method");
        }
        
        GenericValue payment = null;
        try {
            payment = delegator.findByPrimaryKey("Payment", UtilMisc.toMap("paymentId", paymentId));    
        } catch (GenericEntityException e) {
            Debug.logError(e, "Problem getting Payment", module);
        }
        
        if (payment == null) {
            throw new IllegalArgumentException("The paymentId passed does not match an existing payment");
        }
        return payment.getBigDecimal("amount").subtract(getPaymentAppliedBd(delegator,paymentId)).setScale(decimals,rounding);
    }
}
