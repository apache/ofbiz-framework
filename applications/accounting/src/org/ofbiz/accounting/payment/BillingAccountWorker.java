/*
 * Copyright 2001-2006 The Apache Software Foundation
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package org.ofbiz.accounting.payment;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.math.BigDecimal;

import javolution.util.FastList;

import org.ofbiz.accounting.invoice.InvoiceWorker;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilNumber;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericEntity;
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
 *
 * @author     <a href="mailto:jaz@ofbiz.org">Andy Zeneski</a>
 * @author     <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 * @version    $Rev$
 * @since      2.1
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
        if (decimals != -1) ZERO.setScale(decimals);
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
                    double accountLimit = 0.0;
                    if (billingAccountVO.getDouble("accountLimit") != null) {
                        accountLimit = billingAccountVO.getDouble("accountLimit").doubleValue();
                    }
                
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
    
    public static BigDecimal getBillingAccountBalance(GenericValue billingAccount) throws GenericEntityException {
        return getBillingAccountBalance(billingAccount.getDelegator(), billingAccount.getString("billingAccountId"));
    }
    
    /**
     * Calculates the "available" balance of a billing account, which is net balance minus amount of pending (not canceled, rejected, or completed) orders.  When looking at
     * using a billing account for a new order, you should use this method
     * @param delegator
     * @param billingAccountId
     * @return
     * @throws GenericEntityException
     */
    
    public static BigDecimal getBillingAccountBalance(GenericDelegator delegator, String billingAccountId) throws GenericEntityException {
        // first get the net balance of invoices - payments
        BigDecimal balance = getBillingAccountNetBalance(delegator, billingAccountId);
        
        // now the amounts of all the pending orders (not cancelled, rejected or completed)
        List orderHeaders = null;
        List exprs1 = new LinkedList();
        exprs1.add(new EntityExpr("billingAccountId", EntityOperator.EQUALS, billingAccountId));
        exprs1.add(new EntityExpr("statusId", EntityOperator.NOT_EQUAL, "ORDER_REJECTED"));
        exprs1.add(new EntityExpr("statusId", EntityOperator.NOT_EQUAL, "ORDER_CANCELLED"));
        exprs1.add(new EntityExpr("statusId", EntityOperator.NOT_EQUAL, "ORDER_COMPLETED"));

        orderHeaders = delegator.findByAnd("OrderHeader", exprs1);

        if (orderHeaders != null) {
            Iterator ohi = orderHeaders.iterator();
            while (ohi.hasNext()) {
                GenericValue orderHeader = (GenericValue) ohi.next();
                OrderReadHelper orh = new OrderReadHelper(orderHeader);
                balance = balance.add(orh.getOrderGrandTotalBd());
            }
        }
        
        balance = balance.setScale(decimals, rounding);
        return balance;
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
            return ZERO;
        }
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
                if (paymentAppl.getString("invoiceId") != null) {
                    // make sure the invoice has not been canceled
                    if (!"INVOICE_CANCELED".equals(paymentAppl.getRelatedOne("Invoice").getString("statusId"))) {
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
    
    public static Map calcBillingAccountBalance(DispatchContext dctx, Map context) {
        GenericDelegator delegator = dctx.getDelegator();
        String billingAccountId = (String) context.get("billingAccountId");
        GenericValue billingAccount = null;
        Double accountBalance = null;
        Double netAccountBalance = null;
        Double availableBalance = null;
        try {
            billingAccount = delegator.findByPrimaryKey("BillingAccount", UtilMisc.toMap("billingAccountId", billingAccountId));
            accountBalance = new Double((getBillingAccountBalance(delegator, billingAccountId)).doubleValue());
            netAccountBalance = new Double((getBillingAccountNetBalance(delegator, billingAccountId)).doubleValue());
            availableBalance = new Double(getBillingAccountAvailableBalance(billingAccount).doubleValue());
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError("Error getting billing account or calculating balance for billing account #" + billingAccountId);
        }
        
        if (billingAccount == null) {
            return ServiceUtil.returnError("Unable to locate billing account #" + billingAccountId);
        }
        
        Map result = ServiceUtil.returnSuccess();
        result.put("accountBalance", accountBalance);
        result.put("netAccountBalance", netAccountBalance);
        result.put("availableBalance", availableBalance);
        result.put("billingAccount", billingAccount);
        return result;  
    }
}
