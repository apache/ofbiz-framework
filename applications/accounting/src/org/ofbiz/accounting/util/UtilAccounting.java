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

package org.ofbiz.accounting.util;

import org.ofbiz.accounting.AccountingException;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;

public class UtilAccounting {

    public static String module = UtilAccounting.class.getName();

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
            String glAccountTypeId, String organizationPartyId, GenericDelegator delegator) 
        throws AccountingException {

        GenericValue account = null;
        try {
            // first try to find the account in ProductGlAccount
            account = delegator.findByPrimaryKeyCache("ProductGlAccount", 
                    UtilMisc.toMap("productId", productId, "glAccountTypeId", glAccountTypeId, "organizationPartyId", organizationPartyId));
        } catch (GenericEntityException e) {
            throw new AccountingException("Failed to find a ProductGLAccount for productId [" + productId + "], organization [" + organizationPartyId + "], and productGlAccountTypeId [" + glAccountTypeId + "].", e);
        }

        // otherwise try the default accounts
        if (account == null) {
            try {
                account = delegator.findByPrimaryKeyCache("GlAccountTypeDefault", UtilMisc.toMap("glAccountTypeId", glAccountTypeId, "organizationPartyId", organizationPartyId));
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
    public static String getDefaultAccountId(String glAccountTypeId, String organizationPartyId, GenericDelegator delegator) throws AccountingException {
        return getProductOrgGlAccountId(null, glAccountTypeId, organizationPartyId, delegator);
    }

    /**
     * Little method to figure out the net or ending balance of a GlAccountHistory or GlAccountAndHistory value, based on what kind
     * of account (DEBIT or CREDIT) it is
     * @param account - GlAccountHistory or GlAccountAndHistory value
     * @return balance - a Double 
     */
    public static Double getNetBalance(GenericValue account, String debugModule) {
        try {
            GenericValue glAccount = account.getRelatedOne("GlAccount");
            double balance = 0.0;
            if (isDebitAccount(glAccount)) {
                balance = account.getDouble("postedDebits").doubleValue() - account.getDouble("postedCredits").doubleValue();
            } else if (isCreditAccount(glAccount)) {
                balance = account.getDouble("postedCredits").doubleValue() - account.getDouble("postedDebits").doubleValue();
            }
            return new Double(balance);    
        } catch (GenericEntityException ex) {
            Debug.logError(ex.getMessage(), debugModule);
            return null;
        }
    }

    /**
     * Recurses up payment type tree via parentTypeId to see if input payment type ID is in tree.
     */
    private static boolean isPaymentTypeRecurse(GenericValue paymentType, String inputTypeId) throws GenericEntityException {

        // first check the parentTypeId against inputTypeId
        String parentTypeId = paymentType.getString("parentTypeId");
        if (parentTypeId == null) {
            return false;
        }
        if (parentTypeId.equals(inputTypeId)) {
            return true;
        }

        // otherwise, we have to go to the grandparent (recurse)
        return isPaymentTypeRecurse(paymentType.getRelatedOne("ParentPaymentType"), inputTypeId);
    }


    /**
     * Checks if a payment is of a specified PaymentType.paymentTypeId.  Return false if payment is null.  It's better to use the
     * more specific calls like isTaxPayment(). 
     */
    public static boolean isPaymentType(GenericValue payment, String inputTypeId) throws GenericEntityException {
        if (payment == null) { 
            return false; 
        }

        GenericValue paymentType = payment.getRelatedOneCache("PaymentType");
        if (paymentType == null) {
            throw new GenericEntityException("Cannot find PaymentType for paymentId " + payment.getString("paymentId"));
        }

        String paymentTypeId = paymentType.getString("paymentTypeId");
        if (inputTypeId.equals(paymentTypeId)) {
            return true;
        }

        // recurse up tree
        return isPaymentTypeRecurse(paymentType, inputTypeId);
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
        if (parentClassId == null) {
            return false;
        }
        if (parentClassId.equals(parentGlAccountClassId)) {
            return true;
        }

        // otherwise, we have to go to the grandparent (recurse)
        return isAccountClassClass(glAccountClass.getRelatedOneCache("ParentGlAccountClass"), parentGlAccountClassId);
    }

    /**
     * Checks if a GL account is of a specified GlAccountClass.glAccountClassId.  Returns false if account is null.  It's better to use the
     * more specific calls like isDebitAccount().
     */
    public static boolean isAccountClass(GenericValue glAccount, String glAccountClassId) throws GenericEntityException {
        if (glAccount == null) {
            return false;
        }

        GenericValue glAccountClass = glAccount.getRelatedOneCache("GlAccountClass");
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
}
