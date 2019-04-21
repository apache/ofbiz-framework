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

package org.apache.ofbiz.accounting.util;

import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.List;

import org.apache.ofbiz.accounting.AccountingException;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilNumber;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityExpr;
import org.apache.ofbiz.entity.condition.EntityJoinOperator;
import org.apache.ofbiz.entity.util.EntityQuery;


public final class UtilAccounting {
    
    public static final String module = UtilAccounting.class.getName();

    private UtilAccounting() {}

    /**
     * Get the GL Account for a product or the default account type based on input. This replaces the simple-method service
     * getProductOrgGlAccount. First it will look in ProductGlAccount using the primary keys productId and
     * productGlAccountTypeId. If none is found, it will look up GlAccountTypeDefault to find the default account for
     * organizationPartyId with type glAccountTypeId.
     *
     * @param   productId                  When searching for ProductGlAccounts, specify the productId
     * @param   glAccountTypeId            The default glAccountTypeId to look for if no ProductGlAccount is found
     * @param   organizationPartyId        The organization party of the default account
     * @return  The account ID (glAccountId) found
     * @throws  AccountingException        When the no accounts found or an entity exception occurs
     */
    public static String getProductOrgGlAccountId(String productId,
            String glAccountTypeId, String organizationPartyId, Delegator delegator)
        throws AccountingException {

        GenericValue account = null;
        try {
            // first try to find the account in ProductGlAccount
            account = EntityQuery.use(delegator).from("ProductGlAccount")
                    .where("productId", productId, "glAccountTypeId", glAccountTypeId, "organizationPartyId", organizationPartyId)
                    .cache().queryOne();
        } catch (GenericEntityException e) {
            throw new AccountingException("Failed to find a ProductGLAccount for productId [" + productId + "], organization [" + organizationPartyId + "], and productGlAccountTypeId [" + glAccountTypeId + "].", e);
        }

        // otherwise try the default accounts
        if (account == null) {
            try {
                account = EntityQuery.use(delegator).from("GlAccountTypeDefault").where("glAccountTypeId", glAccountTypeId, "organizationPartyId", organizationPartyId).cache().queryOne();
            } catch (GenericEntityException e) {
                throw new AccountingException("Failed to find a GlAccountTypeDefault for glAccountTypeId [" + glAccountTypeId + "] and organizationPartyId [" + organizationPartyId+ "].", e);
            }
        }

        // if no results yet, serious problem
        if (account == null) {
            throw new AccountingException("Failed to find any accounts for  productId [" + productId + "], organization [" + organizationPartyId + "], and productGlAccountTypeId [" + glAccountTypeId + "] or any accounts in GlAccountTypeDefault for glAccountTypeId [" + glAccountTypeId + "] and organizationPartyId [" + organizationPartyId+ "]. Please check your data to make sure that at least a GlAccountTypeDefault is defined for this account type and organization.");
        }

        // otherwise return the glAccountId
        return account.getString("glAccountId");
    }

    /**
     * As above, but explicitly looking for default account for given type and organization
     *
     * @param   glAccountTypeId         The type of account
     * @param   organizationPartyId     The organization of the account
     * @return  The default account ID (glAccountId) for this type
     * @throws  AccountingException     When the default is not configured
     */
    public static String getDefaultAccountId(String glAccountTypeId, String organizationPartyId, Delegator delegator) throws AccountingException {
        return getProductOrgGlAccountId(null, glAccountTypeId, organizationPartyId, delegator);
    }

    public static List<String> getDescendantGlAccountClassIds(GenericValue glAccountClass) throws GenericEntityException {
        List<String> glAccountClassIds = new LinkedList<>();
        getGlAccountClassChildren(glAccountClass, glAccountClassIds);
        return glAccountClassIds;
    }
    private static void getGlAccountClassChildren(GenericValue glAccountClass, List<String> glAccountClassIds) throws GenericEntityException {
        glAccountClassIds.add(glAccountClass.getString("glAccountClassId"));
        List<GenericValue> glAccountClassChildren = glAccountClass.getRelated("ChildGlAccountClass", null, null, true);
        for (GenericValue glAccountClassChild : glAccountClassChildren) {
            getGlAccountClassChildren(glAccountClassChild, glAccountClassIds);
        }
    }

    /**
     * Recurses up payment type tree via parentTypeId to see if input payment type ID is in tree.
     */
    private static boolean isPaymentTypeRecurse(GenericValue paymentType, String inputTypeId) throws GenericEntityException {

        // first check the parentTypeId against inputTypeId
        String parentTypeId = paymentType.getString("parentTypeId");

        // isPaymentTypeRecurse => otherwise, we have to go to the grandparent (recurse)
        return !(parentTypeId == null) &&
                (parentTypeId.equals(inputTypeId) || isPaymentTypeRecurse(paymentType.getRelatedOne("ParentPaymentType", false), inputTypeId));
    }


    /**
     * Checks if a payment is of a specified PaymentType.paymentTypeId.  Return false if payment is null.  It's better to use the
     * more specific calls like isTaxPayment().
     */
    public static boolean isPaymentType(GenericValue payment, String inputTypeId) throws GenericEntityException {
        if (payment == null) {
            return false;
        }

        GenericValue paymentType = payment.getRelatedOne("PaymentType", true);
        if (paymentType == null) {
            throw new GenericEntityException("Cannot find PaymentType for paymentId " + payment.getString("paymentId"));
        }

        String paymentTypeId = paymentType.getString("paymentTypeId");

        // recurse up tree
        return inputTypeId.equals(paymentTypeId) || isPaymentTypeRecurse(paymentType, inputTypeId);
    }


    public static boolean isTaxPayment(GenericValue payment) throws GenericEntityException {
        return isPaymentType(payment, "TAX_PAYMENT");
    }

    public static boolean isDisbursement(GenericValue payment) throws GenericEntityException {
        return isPaymentType(payment, "DISBURSEMENT");
    }

    public static boolean isReceipt(GenericValue payment) throws GenericEntityException {
        return isPaymentType(payment, "RECEIPT");
    }


    /**
     * Determines if a glAccountClass is of a child of a certain parent glAccountClass.
     */
    public static boolean isAccountClassClass(GenericValue glAccountClass, String parentGlAccountClassId) throws GenericEntityException {
        if (glAccountClass == null) return false;

        // check current class against input classId
        if (parentGlAccountClassId.equals(glAccountClass.get("glAccountClassId"))) {
            return true;
        }

        // check parentClassId against inputClassId
        String parentClassId = glAccountClass.getString("parentClassId");

        // otherwise, we have to go to the grandparent (recurse)
        return !(parentClassId == null) &&
                (parentClassId.equals(parentGlAccountClassId) || isAccountClassClass(glAccountClass.getRelatedOne("ParentGlAccountClass", true), parentGlAccountClassId));
    }

    /**
     * Checks if a GL account is of a specified GlAccountClass.glAccountClassId.  Returns false if account is null.  It's better to use the
     * more specific calls like isDebitAccount().
     */
    public static boolean isAccountClass(GenericValue glAccount, String glAccountClassId) throws GenericEntityException {
        if (glAccount == null) {
            return false;
        }

        GenericValue glAccountClass = glAccount.getRelatedOne("GlAccountClass", true);
        if (glAccountClass == null) {
            throw new GenericEntityException("Cannot find GlAccountClass for glAccountId " + glAccount.getString("glAccountId"));
        }

        return isAccountClassClass(glAccountClass, glAccountClassId);
    }


    public static boolean isDebitAccount(GenericValue account) throws GenericEntityException {
        return isAccountClass(account, "DEBIT");
    }

    public static boolean isCreditAccount(GenericValue account) throws GenericEntityException {
        return isAccountClass(account, "CREDIT");
    }

    public static boolean isAssetAccount(GenericValue account) throws GenericEntityException {
        return isAccountClass(account, "ASSET");
    }

    public static boolean isLiabilityAccount(GenericValue account) throws GenericEntityException {
        return isAccountClass(account, "LIABILITY");
    }

    public static boolean isEquityAccount(GenericValue account) throws GenericEntityException {
        return isAccountClass(account, "EQUITY");
    }

    public static boolean isIncomeAccount(GenericValue account) throws GenericEntityException {
        return isAccountClass(account, "INCOME");
    }

    public static boolean isRevenueAccount(GenericValue account) throws GenericEntityException {
        return isAccountClass(account, "REVENUE");
    }

    public static boolean isExpenseAccount(GenericValue account) throws GenericEntityException {
        return isAccountClass(account, "EXPENSE");
    }

    /**
     * Recurses up invoice type tree via parentTypeId to see if input invoice type ID is in tree.
     */
    private static boolean isInvoiceTypeRecurse(GenericValue invoiceType, String inputTypeId) throws GenericEntityException {

        // first check the invoiceTypeId and parentTypeId against inputTypeId
        String invoiceTypeId = invoiceType.getString("invoiceTypeId");
        String parentTypeId = invoiceType.getString("parentTypeId");

        // otherwise, we have to go to the grandparent (recurse)
        return !(parentTypeId == null || invoiceTypeId.equals(parentTypeId)) &&
                (parentTypeId.equals(inputTypeId) || isInvoiceTypeRecurse(invoiceType.getRelatedOne("ParentInvoiceType", false), inputTypeId));
    }

    /**
     * Checks if a invoice is of a specified InvoiceType.invoiceTypeId. Return false if invoice is null. It's better to use
     * more specific calls like isPurchaseInvoice().
     */
    public static boolean isInvoiceType(GenericValue invoice, String inputTypeId) throws GenericEntityException {
        if (invoice == null) {
            return false;
        }

        GenericValue invoiceType = invoice.getRelatedOne("InvoiceType", true);
        if (invoiceType == null) {
            throw new GenericEntityException("Cannot find InvoiceType for invoiceId " + invoice.getString("invoiceId"));
        }

        String invoiceTypeId = invoiceType.getString("invoiceTypeId");

        // recurse up tree
        return inputTypeId.equals(invoiceTypeId)
                || isInvoiceTypeRecurse(invoiceType, inputTypeId);
    }


    public static boolean isPurchaseInvoice(GenericValue invoice) throws GenericEntityException {
        return isInvoiceType(invoice, "PURCHASE_INVOICE");
    }

    public static boolean isSalesInvoice(GenericValue invoice) throws GenericEntityException {
        return isInvoiceType(invoice, "SALES_INVOICE");
    }

    public static boolean isTemplate(GenericValue invoice) throws GenericEntityException {
        return isInvoiceType(invoice, "TEMPLATE");
    }

    public static BigDecimal getGlExchangeRateOfPurchaseInvoice(GenericValue paymentApplication) throws GenericEntityException {
        BigDecimal exchangeRate = BigDecimal.ONE;
        Delegator delegator = paymentApplication.getDelegator();
        List<EntityExpr> andConditions = UtilMisc.toList(
                EntityCondition.makeCondition("glAccountTypeId", "ACCOUNTS_PAYABLE"),
                EntityCondition.makeCondition("debitCreditFlag", "C"),
                EntityCondition.makeCondition("acctgTransTypeId", "PURCHASE_INVOICE"),
                EntityCondition.makeCondition("invoiceId", paymentApplication.getString("invoiceId")));
        EntityCondition whereCondition = EntityCondition.makeCondition(andConditions, EntityJoinOperator.AND);
        GenericValue amounts = EntityQuery.use(delegator).select("origAmount", "amount").from("AcctgTransAndEntries").where(whereCondition).queryFirst();
        if (amounts == null) {
            return exchangeRate;
        }
        BigDecimal origAmount = amounts.getBigDecimal("origAmount");
        BigDecimal amount = amounts.getBigDecimal("amount");
        if (origAmount != null && amount != null && BigDecimal.ZERO.compareTo(origAmount) != 0 && BigDecimal.ZERO.compareTo(amount) != 0 && amount.compareTo(origAmount) != 0) {
            exchangeRate = amount.divide(origAmount, UtilNumber.getBigDecimalScale("ledger.decimals"), UtilNumber.getRoundingMode("invoice.rounding"));
        }
        return exchangeRate;
    }

    public static BigDecimal getGlExchangeRateOfOutgoingPayment(GenericValue paymentApplication) throws GenericEntityException {
        BigDecimal exchangeRate = BigDecimal.ONE;
        Delegator delegator = paymentApplication.getDelegator();
        List<EntityExpr> andConditions = UtilMisc.toList(
                EntityCondition.makeCondition("glAccountTypeId", "CURRENT_ASSET"),
                EntityCondition.makeCondition("debitCreditFlag", "C"),
                EntityCondition.makeCondition("acctgTransTypeId", "OUTGOING_PAYMENT"),
                EntityCondition.makeCondition("paymentId", paymentApplication.getString("paymentId")));
        EntityCondition whereCondition = EntityCondition.makeCondition(andConditions, EntityJoinOperator.AND);
        GenericValue amounts = EntityQuery.use(delegator).select("origAmount", "amount").from("AcctgTransAndEntries").where(whereCondition).queryFirst();
        if (amounts == null) {
            return exchangeRate;
        }
        BigDecimal origAmount = amounts.getBigDecimal("origAmount");
        BigDecimal amount = amounts.getBigDecimal("amount");
        if (origAmount != null && amount != null && BigDecimal.ZERO.compareTo(origAmount) != 0 && BigDecimal.ZERO.compareTo(amount) != 0 && amount.compareTo(origAmount) != 0) {
            exchangeRate = amount.divide(origAmount, UtilNumber.getBigDecimalScale("ledger.decimals"), UtilNumber.getRoundingMode("invoice.rounding"));
        }
        return exchangeRate;
    }

}
