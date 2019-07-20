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

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilNumber;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.order.order.OrderReadHelper;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;

/**
 * Worker methods for BillingAccounts
 */
public class BillingAccountWorker {

    public static final String module = BillingAccountWorker.class.getName();
    public static final String resourceError = "AccountingUiLabels";
    public static final int decimals = UtilNumber.getBigDecimalScale("order.decimals");
    public static final RoundingMode rounding = UtilNumber.getRoundingMode("order.rounding");
    public static final BigDecimal ZERO = BigDecimal.ZERO.setScale(decimals, rounding);

    public static List<Map<String, Object>> makePartyBillingAccountList(GenericValue userLogin, String currencyUomId, String partyId, Delegator delegator, LocalDispatcher dispatcher) throws GeneralException {
        List<Map<String, Object>> billingAccountList = new LinkedList<>();

        Map<String, Object> agentResult = dispatcher.runSync("getRelatedParties", UtilMisc.<String, Object>toMap("userLogin", userLogin, "partyIdFrom", partyId,
                "roleTypeIdFrom", "AGENT", "roleTypeIdTo", "CUSTOMER", "partyRelationshipTypeId", "AGENT", "includeFromToSwitched", "Y"));
        if (ServiceUtil.isError(agentResult)) {
            throw new GeneralException("Error while finding party BillingAccounts when getting Customers that this party is an agent of: " + ServiceUtil.getErrorMessage(agentResult));
        }
        List<String> relatedPartyIdList = UtilGenerics.cast(agentResult.get("relatedPartyIdList"));

        List<GenericValue> billingAccountRoleList = EntityQuery.use(delegator).from("BillingAccountRole")
                .where(EntityCondition.makeCondition("partyId", EntityOperator.IN, relatedPartyIdList),
                        EntityCondition.makeCondition("roleTypeId", EntityOperator.EQUALS, "BILL_TO_CUSTOMER")
                ).filterByDate().queryList();

        if (billingAccountRoleList.size() > 0) {
            BigDecimal totalAvailable = BigDecimal.ZERO;
            for (GenericValue billingAccountRole : billingAccountRoleList) {
                GenericValue billingAccountVO = billingAccountRole.getRelatedOne("BillingAccount", false);

                // skip accounts that have thruDate < nowTimestamp
                java.sql.Timestamp thruDate = billingAccountVO.getTimestamp("thruDate");
                if ((thruDate != null) && UtilDateTime.nowTimestamp().after(thruDate)) {
                    continue;
                }

                if (currencyUomId.equals(billingAccountVO.getString("accountCurrencyUomId"))) {
                    BigDecimal accountBalance = OrderReadHelper.getBillingAccountBalance(billingAccountVO);

                    Map<String, Object> billingAccount = new HashMap<>(billingAccountVO);
                    BigDecimal accountLimit = OrderReadHelper.getAccountLimit(billingAccountVO);

                    billingAccount.put("accountBalance", accountBalance);
                    BigDecimal accountAvailable = accountLimit.subtract(accountBalance);
                    totalAvailable = totalAvailable.add(accountAvailable);
                    billingAccountList.add(billingAccount);
                }
            }
            Collections.sort(billingAccountList, new BillingAccountComparator());
        }
        return billingAccountList;
    }

    /**
     * Returns list of orders which are currently open against a billing account
     */
    public static List<GenericValue> getBillingAccountOpenOrders(Delegator delegator, String billingAccountId) throws GenericEntityException {
        return EntityQuery.use(delegator).from("OrderHeader")
                .where(EntityCondition.makeCondition("billingAccountId", EntityOperator.EQUALS, billingAccountId),
                        EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "ORDER_REJECTED"),
                        EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "ORDER_CANCELLED"),
                        EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "ORDER_COMPLETED")
                ).queryList();
    }

    /**
     * Returns the amount which could be charged to a billing account, which is defined as the accountLimit minus account balance and minus the balance of outstanding orders
     * When trying to figure out how much of a billing account can be used to pay for an outstanding order, use this method
     * @param billingAccount GenericValue object of the billing account
     * @return returns the amount which could be charged to a billing account
     * @throws GenericEntityException
     */
    public static BigDecimal getBillingAccountAvailableBalance(GenericValue billingAccount) throws GenericEntityException {
        if ((billingAccount != null) && (billingAccount.get("accountLimit") != null)) {
            BigDecimal accountLimit = billingAccount.getBigDecimal("accountLimit");
            BigDecimal availableBalance = accountLimit.subtract(OrderReadHelper.getBillingAccountBalance(billingAccount)).setScale(decimals, rounding);
            return availableBalance;
        }
        Debug.logWarning("Available balance requested for null billing account, returning zero", module);
        return ZERO;
    }

    public static BigDecimal getBillingAccountAvailableBalance(Delegator delegator, String billingAccountId) throws GenericEntityException {
        GenericValue billingAccount = EntityQuery.use(delegator).from("BillingAccount").where("billingAccountId", billingAccountId).queryOne();
        return getBillingAccountAvailableBalance(billingAccount);
    }

    /**
     * Calculates the net balance of a billing account, which is sum of all amounts applied to invoices minus sum of all amounts applied from payments.
     * When charging or capturing an invoice to a billing account, use this method
     * @param delegator the delegator
     * @param billingAccountId the billing account id
     * @return the amount of the billing account which could be captured
     * @throws GenericEntityException
     */
    public static BigDecimal getBillingAccountNetBalance(Delegator delegator, String billingAccountId) throws GenericEntityException {
        BigDecimal balance = ZERO;

        // search through all PaymentApplications and add the amount that was applied to invoice and subtract the amount applied from payments
        List<GenericValue> paymentAppls = EntityQuery.use(delegator).from("PaymentApplication").where("billingAccountId", billingAccountId).queryList();
        for (GenericValue paymentAppl : paymentAppls) {
            BigDecimal amountApplied = paymentAppl.getBigDecimal("amountApplied");
            GenericValue invoice = paymentAppl.getRelatedOne("Invoice", false);
            if (invoice != null) {
                // make sure the invoice has not been canceled and it is not a "Customer return invoice"
                if (!"CUST_RTN_INVOICE".equals(invoice.getString("invoiceTypeId")) && !"INVOICE_CANCELLED".equals(invoice.getString("statusId"))) {
                    balance = balance.add(amountApplied);
                }
            } else {
                balance = balance.subtract(amountApplied);
            }
        }

        balance = balance.setScale(decimals, rounding);
        return balance;
    }

    /**
     * Returns the amount of the billing account which could be captured, which is BillingAccount.accountLimit - net balance
     * @param billingAccount GenericValue object of the billing account
     * @return the amount of the billing account which could be captured
     * @throws GenericEntityException 
     */
    public static BigDecimal availableToCapture(GenericValue billingAccount) throws GenericEntityException {
        BigDecimal netBalance = getBillingAccountNetBalance(billingAccount.getDelegator(), billingAccount.getString("billingAccountId"));
        BigDecimal accountLimit = billingAccount.getBigDecimal("accountLimit");

        return accountLimit.subtract(netBalance).setScale(decimals, rounding);
    }

    public static Map<String, Object> calcBillingAccountBalance(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        String billingAccountId = (String) context.get("billingAccountId");
        Locale locale = (Locale) context.get("locale");
        Map<String, Object> result = ServiceUtil.returnSuccess();

        try {
            GenericValue billingAccount = EntityQuery.use(delegator).from("BillingAccount").where("billingAccountId", billingAccountId).queryOne();
            if (billingAccount == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                        "AccountingBillingAccountNotFound",
                        UtilMisc.toMap("billingAccountId", billingAccountId), locale));
            }

            result.put("billingAccount", billingAccount);
            result.put("accountBalance", OrderReadHelper.getBillingAccountBalance(billingAccount));
            result.put("netAccountBalance", getBillingAccountNetBalance(delegator, billingAccountId));
            result.put("availableBalance", getBillingAccountAvailableBalance(billingAccount));
            result.put("availableToCapture", availableToCapture(billingAccount));

            return result;
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceError, 
                    "AccountingBillingAccountNotFound",
                    UtilMisc.toMap("billingAccountId", billingAccountId), locale));
        }
    }
    
    @SuppressWarnings("serial")
    protected static class BillingAccountComparator implements Comparator<Map<String, Object>>, Serializable{
        @Override
        public int compare(Map<String, Object> billingAccount1, Map<String, Object> billingAccount2) {
            return ((BigDecimal)billingAccount1.get("accountBalance")).compareTo((BigDecimal)billingAccount2.get("accountBalance"));
        }
    }
}
