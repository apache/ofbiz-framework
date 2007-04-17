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

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.math.BigDecimal;

import javolution.util.FastList;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilNumber;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityConditionList;
import org.ofbiz.entity.condition.EntityExpr;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.order.order.OrderReadHelper;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceUtil;

/**
 * Worker methods for BillingAccounts
 */
public class BillingAccountWorker {
    
    public static final String module = BillingAccountWorker.class.getName();
    private static BigDecimal ZERO = new BigDecimal("0");
    private static int decimals = -1;
    private static int rounding = -1;
    static {
        decimals = UtilNumber.getBigDecimalScale("order.decimals");
        rounding = UtilNumber.getBigDecimalRoundingMode("order.rounding");

        // set zero to the proper scale
        if (decimals != -1) ZERO = ZERO.setScale(decimals);
    }

    public static List makePartyBillingAccountList(GenericValue userLogin, String currencyUomId, String partyId, GenericDelegator delegator, LocalDispatcher dispatcher) throws GeneralException {
        List billingAccountList = FastList.newInstance();

        Map agentResult = dispatcher.runSync("getRelatedParties", UtilMisc.toMap("userLogin", userLogin, "partyIdFrom", partyId, 
                "roleTypeIdFrom", "AGENT", "roleTypeIdTo", "CUSTOMER", "partyRelationshipTypeId", "AGENT", "includeFromToSwitched", "Y"));
        if (ServiceUtil.isError(agentResult)) {
            throw new GeneralException("Error while finding party BillingAccounts when getting Customers that this party is an agent of: " + ServiceUtil.getErrorMessage(agentResult));
        }
        List relatedPartyIdList = (List) agentResult.get("relatedPartyIdList");

        EntityCondition barFindCond = new EntityConditionList(UtilMisc.toList(
                new EntityExpr("partyId", EntityOperator.IN, relatedPartyIdList),
                new EntityExpr("roleTypeId", EntityOperator.EQUALS, "BILL_TO_CUSTOMER")), EntityOperator.AND);
        List billingAccountRoleList = delegator.findByCondition("BillingAccountRole", barFindCond, null, null);
        billingAccountRoleList = EntityUtil.filterByDate(billingAccountRoleList);

        if (billingAccountRoleList != null && billingAccountRoleList.size() > 0) {
            double totalAvailable = 0.0;
            TreeMap sortedAccounts = new TreeMap();
            Iterator billingAcctIter = billingAccountRoleList.iterator();
            while (billingAcctIter.hasNext()) {
                GenericValue billingAccountRole = (GenericValue) billingAcctIter.next();        
                GenericValue billingAccountVO = billingAccountRole.getRelatedOne("BillingAccount");

                // skip accounts that have thruDate < nowTimestamp
                java.sql.Timestamp thruDate = billingAccountVO.getTimestamp("thruDate");
                if ((thruDate != null) && UtilDateTime.nowTimestamp().after(thruDate)) continue;

                if (currencyUomId.equals(billingAccountVO.getString("accountCurrencyUomId"))) {
                    double accountBalance = (BillingAccountWorker.getBillingAccountBalance(billingAccountVO)).doubleValue();
                
                    Map billingAccount = new HashMap(billingAccountVO);
                    double accountLimit = getAccountLimit(billingAccountVO).doubleValue();
                
                    billingAccount.put("accountBalance", new Double(accountBalance)); 
                    double accountAvailable = accountLimit - accountBalance;
                    totalAvailable += accountAvailable;    
                    sortedAccounts.put(new Double(accountAvailable), billingAccount);
                }
            }
            
            billingAccountList.addAll(sortedAccounts.values());
        }
        return billingAccountList;
    }
    
    /**
     * Returns the accountLimit of the BillingAccount or BigDecimal ZERO if it is null
     * @param billingAccount
     * @throws GenericEntityException
     */
    public static BigDecimal getAccountLimit(GenericValue billingAccount) throws GenericEntityException {
        if (billingAccount.getBigDecimal("accountLimit") != null) {
            return billingAccount.getBigDecimal("accountLimit");
        } else {
            Debug.logWarning("Billing Account [" + billingAccount.getString("billingAccountId") + "] does not have an account limit defined, assuming zero.", module);
            return ZERO;
        }
    }
    
    /**
     * Calculates the "available" balance of a billing account, which is net balance minus amount of pending (not canceled, rejected, or completed) orders.  
     * Available balance will not exceed billing account's accountLimit.  
     * When looking at using a billing account for a new order, you should use this method.  
     * @param delegator
     * @param billingAccountId
     * @return
     * @throws GenericEntityException
     */
    public static BigDecimal getBillingAccountBalance(GenericValue billingAccount) throws GenericEntityException {
        GenericDelegator delegator = billingAccount.getDelegator();
        String billingAccountId = billingAccount.getString("billingAccountId");
        
        // first get the net balance of invoices - payments
        BigDecimal balance = getBillingAccountNetBalance(delegator, billingAccountId);
        
        // now the amounts of all the pending orders (not cancelled, rejected or completed)
        List orderHeaders = getBillingAccountOpenOrders(delegator, billingAccountId); 

        if (orderHeaders != null) {
            Iterator ohi = orderHeaders.iterator();
            while (ohi.hasNext()) {
                GenericValue orderHeader = (GenericValue) ohi.next();
                OrderReadHelper orh = new OrderReadHelper(orderHeader);
                balance = balance.add(orh.getOrderGrandTotalBd());
            }
        }

        // set the balance to BillingAccount.accountLimit if it is greater.  This is necessary because nowhere do we track the amount of BillingAccount
        // to be charged to an order, such as FinAccountAuth entity does for FinAccount.  As a result, we must assume that the system is doing things correctly
        // and use the accountLimit
        BigDecimal accountLimit = new BigDecimal(billingAccount.getDouble("accountLimit").doubleValue());
        if (balance.compareTo(accountLimit) == 1) {
            balance = accountLimit;
        } else {
            balance = balance.setScale(decimals, rounding);    
        }
        return balance;
    }
    
    /**
     * Returns list of orders which are currently open against a billing account
     */ 
    public static List getBillingAccountOpenOrders(GenericDelegator delegator, String billingAccountId) throws GenericEntityException {
        return delegator.findByAnd("OrderHeader", UtilMisc.toList( 
                    new EntityExpr("billingAccountId", EntityOperator.EQUALS, billingAccountId),
                    new EntityExpr("statusId", EntityOperator.NOT_EQUAL, "ORDER_REJECTED"),
                    new EntityExpr("statusId", EntityOperator.NOT_EQUAL, "ORDER_CANCELLED"),
                    new EntityExpr("statusId", EntityOperator.NOT_EQUAL, "ORDER_COMPLETED")));
    } 
    
    public static BigDecimal getBillingAccountBalance(GenericDelegator delegator, String billingAccountId) throws GenericEntityException {
        GenericValue billingAccount = delegator.findByPrimaryKey("BillingAccount", UtilMisc.toMap("billingAccountId", billingAccountId));
        return getBillingAccountBalance(billingAccount);
    }
    
    /**
     * Returns the amount which could be charged to a billing account, which is defined as the accountLimit minus account balance and minus the balance of outstanding orders
     * When trying to figure out how much of a billing account can be used to pay for an outstanding order, use this method
     * @param billingAccount
     * @return
     * @throws GenericEntityException
     */
    public static BigDecimal getBillingAccountAvailableBalance(GenericValue billingAccount) throws GenericEntityException {
        if ((billingAccount != null) && (billingAccount.get("accountLimit") != null)) {
            BigDecimal accountLimit = new BigDecimal(billingAccount.getDouble("accountLimit").doubleValue());
            BigDecimal availableBalance = accountLimit.subtract(getBillingAccountBalance(billingAccount)).setScale(decimals, rounding);
            return availableBalance;
        } else {
            Debug.logWarning("Available balance requested for null billing account, returning zero", module);
            return ZERO;
        }
    }
    
    public static BigDecimal getBillingAccountAvailableBalance(GenericDelegator delegator, String billingAccountId) throws GenericEntityException {
        GenericValue billingAccount = delegator.findByPrimaryKey("BillingAccount", UtilMisc.toMap("billingAccountId", billingAccountId));
        return getBillingAccountAvailableBalance(billingAccount);
    }

    /**
     * Calculates the net balance of a billing account, which is sum of all amounts applied to invoices minus sum of all amounts applied from payments.
     * When charging or capturing an invoice to a billing account, use this method
     * @param delegator
     * @param billingAccountId
     * @return
     * @throws GenericEntityException
     */
    public static BigDecimal getBillingAccountNetBalance(GenericDelegator delegator, String billingAccountId) throws GenericEntityException {
        BigDecimal balance = ZERO;
     
        // search through all PaymentApplications and add the amount that was applied to invoice and subtract the amount applied from payments
        List paymentAppls = delegator.findByAnd("PaymentApplication", UtilMisc.toMap("billingAccountId", billingAccountId));
        if (paymentAppls != null) {
            for (Iterator pAi = paymentAppls.iterator(); pAi.hasNext(); ) {
                GenericValue paymentAppl = (GenericValue) pAi.next();
                BigDecimal amountApplied = paymentAppl.getBigDecimal("amountApplied");
                GenericValue invoice = paymentAppl.getRelatedOne("Invoice");
                if (invoice != null) {
                    // make sure the invoice has not been canceled and it is not a "Customer return invoice"
                    if (!"CUST_RTN_INVOICE".equals(invoice.getString("invoiceTypeId")) && !"INVOICE_CANCELLED".equals(invoice.getString("statusId"))) {
                        balance = balance.add(amountApplied);    
                    }
                } else {
                    balance = balance.subtract(amountApplied);
                }
            }
        }
    
        balance = balance.setScale(decimals, rounding);
        return balance;
    }
    
    /**
     * Returns the amount of the billing account which could be captured, which is BillingAccount.accountLimit - net balance
     * @param billingAccount
     * @return
     * @throws GenericEntityException
     */
    public static BigDecimal availableToCapture(GenericValue billingAccount) throws GenericEntityException {
        BigDecimal netBalance = getBillingAccountNetBalance(billingAccount.getDelegator(), billingAccount.getString("billingAccountId"));
        BigDecimal accountLimit = new BigDecimal(billingAccount.getDouble("accountLimit").doubleValue());
        
        return accountLimit.subtract(netBalance).setScale(decimals, rounding);
    }
    
    public static Map calcBillingAccountBalance(DispatchContext dctx, Map context) {
        GenericDelegator delegator = dctx.getDelegator();
        String billingAccountId = (String) context.get("billingAccountId");
        Map result = ServiceUtil.returnSuccess();
        
        try {
            GenericValue billingAccount = delegator.findByPrimaryKey("BillingAccount", UtilMisc.toMap("billingAccountId", billingAccountId));
            if (billingAccount == null) {
                return ServiceUtil.returnError("Unable to locate billing account #" + billingAccountId);
            }
            
            result.put("billingAccount", billingAccount);
            result.put("accountBalance",  new Double((getBillingAccountBalance(delegator, billingAccountId)).doubleValue()));
            result.put("netAccountBalance", new Double((getBillingAccountNetBalance(delegator, billingAccountId)).doubleValue()));
            result.put("availableBalance", new Double(getBillingAccountAvailableBalance(billingAccount).doubleValue()));
            result.put("availableToCapture", new Double(availableToCapture(billingAccount).doubleValue()));
        
            return result;  
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError("Error getting billing account or calculating balance for billing account #" + billingAccountId);
        }
        
        
    }
}
