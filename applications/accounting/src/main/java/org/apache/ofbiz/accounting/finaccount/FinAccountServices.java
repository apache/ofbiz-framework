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

package org.apache.ofbiz.accounting.finaccount;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityExpr;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.entity.util.EntityListIterator;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.order.finaccount.FinAccountHelper;
import org.apache.ofbiz.product.store.ProductStoreWorker;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ModelService;
import org.apache.ofbiz.service.ServiceUtil;

public class FinAccountServices {

    public static final String module = FinAccountServices.class.getName();
    public static final String resourceError = "AccountingErrorUiLabels";

    public static Map<String, Object> createAccountAndCredit(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        String finAccountTypeId = (String) context.get("finAccountTypeId");
        String accountName = (String) context.get("accountName");
        String finAccountId = (String) context.get("finAccountId");
        Locale locale = (Locale) context.get("locale");

        // check the type
        if (finAccountTypeId == null) {
            finAccountTypeId = "SVCCRED_ACCOUNT";
        }
        if (accountName == null) {
            if ("SVCCRED_ACCOUNT".equals(finAccountTypeId)) {
                accountName = "Customer Service Credit Account";
            } else {
                accountName = "Financial Account";
            }
        }

        GenericValue userLogin = (GenericValue) context.get("userLogin");
        try {
            // find the most recent (active) service credit account for the specified party
            String partyId = (String) context.get("partyId");
            Map<String, String> lookupMap = UtilMisc.toMap("finAccountTypeId", finAccountTypeId, "ownerPartyId",
                    partyId);

            // if a productStoreId is present, restrict the accounts returned using the
            // store's payToPartyId
            String productStoreId = (String) context.get("productStoreId");
            if (UtilValidate.isNotEmpty(productStoreId)) {
                String payToPartyId = ProductStoreWorker.getProductStorePayToPartyId(productStoreId, delegator);
                if (UtilValidate.isNotEmpty(payToPartyId)) {
                    lookupMap.put("organizationPartyId", payToPartyId);
                }
            }

            // if a currencyUomId is present, use it to restrict the accounts returned
            String currencyUomId = (String) context.get("currencyUomId");
            if (UtilValidate.isNotEmpty(currencyUomId)) {
                lookupMap.put("currencyUomId", currencyUomId);
            }

            // check for an existing account
            GenericValue creditAccount;
            if (finAccountId != null) {
                creditAccount = EntityQuery.use(delegator).from("FinAccount").where("finAccountId", finAccountId)
                        .queryOne();
            } else {
                creditAccount = EntityQuery.use(delegator).from("FinAccount").where(lookupMap).orderBy("-fromDate")
                        .filterByDate().queryFirst();
            }

            if (creditAccount == null) {
                // create a new service credit account
                String createAccountServiceName = "createFinAccount";
                if (UtilValidate.isNotEmpty(productStoreId)) {
                    createAccountServiceName = "createFinAccountForStore";
                }
                // automatically set the parameters
                ModelService createAccountService = dctx.getModelService(createAccountServiceName);
                Map<String, Object> createAccountContext = createAccountService.makeValid(context, ModelService.IN_PARAM);
                createAccountContext.put("finAccountTypeId", finAccountTypeId);
                createAccountContext.put("finAccountName", accountName);
                createAccountContext.put("ownerPartyId", partyId);
                createAccountContext.put("userLogin", userLogin);

                Map<String, Object> createAccountResult = dispatcher.runSync(createAccountServiceName, createAccountContext);
                if (ServiceUtil.isError(createAccountResult) || ServiceUtil.isFailure(createAccountResult)) {
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(createAccountResult));
                }

                if (createAccountResult != null) {
                    String creditAccountId = (String) createAccountResult.get("finAccountId");
                    if (UtilValidate.isNotEmpty(creditAccountId)) {
                        creditAccount = EntityQuery.use(delegator).from("FinAccount").where("finAccountId",
                                creditAccountId).queryOne();

                        // create the owner role
                        Map<String, Object> roleCtx = new HashMap<>();
                        roleCtx.put("partyId", partyId);
                        roleCtx.put("roleTypeId", "OWNER");
                        roleCtx.put("finAccountId", creditAccountId);
                        roleCtx.put("userLogin", userLogin);
                        roleCtx.put("fromDate", UtilDateTime.nowTimestamp());
                        Map<String, Object> roleResp;
                        try {
                            roleResp = dispatcher.runSync("createFinAccountRole", roleCtx);
                        } catch (GenericServiceException e) {
                            return ServiceUtil.returnError(e.getMessage());
                        }
                        if (ServiceUtil.isError(roleResp)) {
                            return ServiceUtil.returnError(ServiceUtil.getErrorMessage(roleResp));
                        }
                        finAccountId = creditAccountId; // update the finAccountId for return parameter
                    }
                }
                if (creditAccount == null) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(resourceError,
                            "AccountingFinAccountCannotCreditAccount", locale));
                }
            }

            // create the credit transaction
            Map<String, Object> transactionMap = new HashMap<>();
            transactionMap.put("finAccountTransTypeId", "ADJUSTMENT");
            transactionMap.put("finAccountId", creditAccount.getString("finAccountId"));
            transactionMap.put("partyId", partyId);
            transactionMap.put("amount", context.get("amount"));
            transactionMap.put("reasonEnumId", context.get("reasonEnumId"));
            transactionMap.put("comments", context.get("comments"));
            transactionMap.put("userLogin", userLogin);

            Map<String, Object> creditTransResult = dispatcher.runSync("createFinAccountTrans", transactionMap);
            if (ServiceUtil.isError(creditTransResult) || ServiceUtil.isFailure(creditTransResult)) {
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(creditTransResult));
            }
        } catch (GenericEntityException | GenericServiceException ge) {
            return ServiceUtil.returnError(ge.getMessage());
        }

        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put("finAccountId", finAccountId);
        return result;
    }

    public static Map<String, Object> createFinAccountForStore(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String productStoreId = (String) context.get("productStoreId");
        String finAccountTypeId = (String) context.get("finAccountTypeId");
        Locale locale = (Locale) context.get("locale");
        GenericValue productStore = ProductStoreWorker.getProductStore(productStoreId, delegator);

        try {
            // get the product store id and use it to generate a unique fin account code
            GenericValue productStoreFinAccountSetting = EntityQuery.use(delegator).from("ProductStoreFinActSetting")
                    .where("productStoreId", productStoreId, "finAccountTypeId", finAccountTypeId).cache().queryOne();
            if (productStoreFinAccountSetting == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resourceError,
                        "AccountingFinAccountSetting",
                        UtilMisc.toMap("productStoreId", productStoreId, "finAccountTypeId", finAccountTypeId),
                        locale));
            }

            Long accountCodeLength = productStoreFinAccountSetting.getLong("accountCodeLength");
            Long accountValidDays = productStoreFinAccountSetting.getLong("accountValidDays");
            Long pinCodeLength = productStoreFinAccountSetting.getLong("pinCodeLength");
            String requirePinCode = productStoreFinAccountSetting.getString("requirePinCode");

            // automatically set the parameters for the create fin account service
            ModelService createService = dctx.getModelService("createFinAccount");
            Map<String, Object> inContext = createService.makeValid(context, ModelService.IN_PARAM);
            Timestamp now = UtilDateTime.nowTimestamp();

            // now use our values
            String finAccountCode = null;
            if (UtilValidate.isNotEmpty(accountCodeLength)) {
                finAccountCode = FinAccountHelper.getNewFinAccountCode(accountCodeLength.intValue(), delegator);
                inContext.put("finAccountCode", finAccountCode);
            }

            // with pin codes, the account code becomes the ID and the pin becomes the code
            if ("Y".equalsIgnoreCase(requirePinCode)) {
                String pinCode = FinAccountHelper.getNewFinAccountCode(pinCodeLength.intValue(), delegator);
                inContext.put("finAccountPin", pinCode);
            }

            // set the dates/userlogin
            if (UtilValidate.isNotEmpty(accountValidDays)) {
                inContext.put("thruDate", UtilDateTime.getDayEnd(now, accountValidDays));
            }
            inContext.put("fromDate", now);
            inContext.put("userLogin", userLogin);

            // product store payToPartyId
            String payToPartyId = ProductStoreWorker.getProductStorePayToPartyId(productStoreId, delegator);
            inContext.put("organizationPartyId", payToPartyId);
            inContext.put("currencyUomId", productStore.get("defaultCurrencyUomId"));

            Map<String, Object> createResult = dispatcher.runSync("createFinAccount", inContext);
            if (ServiceUtil.isError(createResult)) {
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(createResult));
            }
            Map<String, Object> result = ServiceUtil.returnSuccess();
            result.put("finAccountId", createResult.get("finAccountId"));
            result.put("finAccountCode", finAccountCode);
            return result;
        } catch (GenericEntityException | GenericServiceException ex) {
            return ServiceUtil.returnError(ex.getMessage());
        }
    }

    public static Map<String, Object> checkFinAccountBalance(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        String finAccountId = (String) context.get("finAccountId");
        String finAccountCode = (String) context.get("finAccountCode");
        Locale locale = (Locale) context.get("locale");

        GenericValue finAccount;
        if (finAccountId == null) {
            try {
                finAccount = FinAccountHelper.getFinAccountFromCode(finAccountCode, delegator);
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
                return ServiceUtil.returnError(e.getMessage());
            }
        } else {
            try {
                finAccount = EntityQuery.use(delegator).from("FinAccount").where("finAccountId", finAccountId)
                        .queryOne();
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
                return ServiceUtil.returnError(e.getMessage());
            }
        }
        if (finAccount == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceError,
                    "AccountingFinAccountNotFound", UtilMisc.toMap("finAccountId", finAccountId), locale));
        }

        // get the balance
        BigDecimal availableBalance = finAccount.getBigDecimal("availableBalance");
        BigDecimal balance = finAccount.getBigDecimal("actualBalance");
        if (availableBalance == null) {
            availableBalance = FinAccountHelper.ZERO;
        }
        if (balance == null) {
            balance = FinAccountHelper.ZERO;
        }

        String statusId = finAccount.getString("statusId");
        Debug.logInfo("FinAccount Balance [" + balance + "] Available [" + availableBalance + "] - Status: " + statusId,
                module);

        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put("availableBalance", availableBalance);
        result.put("balance", balance);
        result.put("statusId", statusId);
        return result;
    }

    public static Map<String, Object> checkFinAccountStatus(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        String finAccountId = (String) context.get("finAccountId");
        Locale locale = (Locale) context.get("locale");

        if (finAccountId == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resourceError,
                    "AccountingFinAccountNotFound", UtilMisc.toMap("finAccountId", ""), locale));
        }

        GenericValue finAccount;
        try {
            finAccount = EntityQuery.use(delegator).from("FinAccount").where("finAccountId", finAccountId).queryOne();
        } catch (GenericEntityException ex) {
            return ServiceUtil.returnError(ex.getMessage());
        }

        if (finAccount != null) {
            String statusId = finAccount.getString("statusId");
            if (statusId == null) {
                statusId = "FNACT_ACTIVE";
            }

            BigDecimal balance = finAccount.getBigDecimal("actualBalance");
            if (balance == null) {
                balance = FinAccountHelper.ZERO;
            }

            Debug.logInfo("Account #" + finAccountId + " Balance: " + balance + " Status: " + statusId, module);

            if ("FNACT_ACTIVE".equals(statusId) && balance.compareTo(FinAccountHelper.ZERO) < 1) {
                finAccount.set("statusId", "FNACT_MANFROZEN");
                Debug.logInfo("Financial account [" + finAccountId + "] has passed its threshold [" + balance
                        + "] (Frozen)", module);
            } else if ("FNACT_MANFROZEN".equals(statusId) && balance.compareTo(FinAccountHelper.ZERO) > 0) {
                finAccount.set("statusId", "FNACT_ACTIVE");
                Debug.logInfo("Financial account [" + finAccountId + "] has been made current [" + balance
                        + "] (Un-Frozen)", module);
            }
            try {
                finAccount.store();
            } catch (GenericEntityException e) {
                return ServiceUtil.returnError(e.getMessage());
            }
        }

        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> refundFinAccount(DispatchContext dctx, Map<String, Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get("locale");
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String finAccountId = (String) context.get("finAccountId");
        Map<String, Object> result = null;

        GenericValue finAccount;
        try {
            finAccount = EntityQuery.use(delegator).from("FinAccount").where("finAccountId", finAccountId).queryOne();
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(e.getMessage());
        }

        if (finAccount != null) {
            // check to make sure the account is refundable
            if (!"Y".equals(finAccount.getString("isRefundable"))) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resourceError,
                        "AccountingFinAccountIsNotRefundable", locale));
            }

            // get the actual and available balance
            BigDecimal availableBalance = finAccount.getBigDecimal("availableBalance");
            BigDecimal actualBalance = finAccount.getBigDecimal("actualBalance");

            // if they do not match, then there are outstanding authorizations which need to
            // be settled first
            if (actualBalance.compareTo(availableBalance) != 0) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resourceError,
                        "AccountingFinAccountCannotBeRefunded", locale));
            }

            // now we make sure there is something to refund
            if (actualBalance.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal remainingBalance = new BigDecimal(actualBalance.toString());
                BigDecimal refundAmount = BigDecimal.ZERO;

                List<EntityExpr> exprs = UtilMisc.toList(EntityCondition.makeCondition("finAccountTransTypeId",
                        EntityOperator.EQUALS, "DEPOSIT"),
                        EntityCondition.makeCondition("finAccountId", EntityOperator.EQUALS, finAccountId));
                EntityCondition condition = EntityCondition.makeCondition(exprs, EntityOperator.AND);

                try (EntityListIterator eli = EntityQuery.use(delegator).from("FinAccountTrans").where(condition)
                        .orderBy("-transactionDate").queryIterator()) {
                    GenericValue trans;
                    while (remainingBalance.compareTo(FinAccountHelper.ZERO) < 0 && (trans = eli.next()) != null) {
                        String orderId = trans.getString("orderId");
                        String orderItemSeqId = trans.getString("orderItemSeqId");

                        // make sure there is an order available to refund
                        if (orderId != null && orderItemSeqId != null) {
                            GenericValue orderHeader = EntityQuery.use(delegator).from("OrderHeader").where("orderId",
                                    orderId).queryOne();
                            GenericValue productStore = orderHeader.getRelatedOne("ProductStore", false);
                            GenericValue orderItem = EntityQuery.use(delegator).from("OrderItem").where("orderId",
                                    orderId, "orderItemSeqId", orderItemSeqId).queryOne();
                            if (!"ITEM_CANCELLED".equals(orderItem.getString("statusId"))) {

                                // make sure the item hasn't already been returned
                                List<GenericValue> returnItems = orderItem.getRelated("ReturnItem", null, null, false);
                                if (UtilValidate.isEmpty(returnItems)) {
                                    BigDecimal txAmt = trans.getBigDecimal("amount");
                                    BigDecimal refAmt = txAmt;
                                    if (remainingBalance.compareTo(txAmt) == -1) {
                                        refAmt = remainingBalance;
                                    }
                                    remainingBalance = remainingBalance.subtract(refAmt);
                                    refundAmount = refundAmount.add(refAmt);

                                    // create the return header
                                    Map<String, Object> rhCtx = UtilMisc.toMap("returnHeaderTypeId", "CUSTOMER_RETURN",
                                            "fromPartyId", finAccount.getString("ownerPartyId"), "toPartyId",
                                            productStore.getString("payToPartyId"), "userLogin", userLogin);
                                    Map<String, Object> rhResp = dispatcher.runSync("createReturnHeader", rhCtx);
                                    if (ServiceUtil.isError(rhResp)) {
                                        throw new GeneralException(ServiceUtil.getErrorMessage(rhResp));
                                    }
                                    String returnId = (String) rhResp.get("returnId");

                                    // create the return item
                                    Map<String, Object> returnItemCtx = new HashMap<>();
                                    returnItemCtx.put("returnId", returnId);
                                    returnItemCtx.put("orderId", orderId);
                                    returnItemCtx.put("description", orderItem.getString("itemDescription"));
                                    returnItemCtx.put("orderItemSeqId", orderItemSeqId);
                                    returnItemCtx.put("returnQuantity", BigDecimal.ONE);
                                    returnItemCtx.put("receivedQuantity", BigDecimal.ONE);
                                    returnItemCtx.put("returnPrice", refAmt);
                                    returnItemCtx.put("returnReasonId", "RTN_NOT_WANT");
                                    returnItemCtx.put("returnTypeId", "RTN_REFUND"); // refund return
                                    returnItemCtx.put("returnItemTypeId", "RET_NPROD_ITEM");
                                    returnItemCtx.put("userLogin", userLogin);

                                    Map<String, Object> retItResp = dispatcher.runSync("createReturnItem",
                                            returnItemCtx);
                                    if (ServiceUtil.isError(retItResp)) {
                                        throw new GeneralException(ServiceUtil.getErrorMessage(retItResp));
                                    }
                                    String returnItemSeqId = (String) retItResp.get("returnItemSeqId");

                                    // approve the return
                                    Map<String, Object> appRet = UtilMisc.toMap("statusId", "RETURN_ACCEPTED",
                                            "returnId", returnId, "userLogin", userLogin);
                                    Map<String, Object> appResp = dispatcher.runSync("updateReturnHeader", appRet);
                                    if (ServiceUtil.isError(appResp)) {
                                        throw new GeneralException(ServiceUtil.getErrorMessage(appResp));
                                    }

                                    // "receive" the return - should trigger the refund
                                    Map<String, Object> recRet = UtilMisc.toMap("statusId", "RETURN_RECEIVED",
                                            "returnId", returnId, "userLogin", userLogin);
                                    Map<String, Object> recResp = dispatcher.runSync("updateReturnHeader", recRet);
                                    if (ServiceUtil.isError(recResp)) {
                                        throw new GeneralException(ServiceUtil.getErrorMessage(recResp));
                                    }

                                    // get the return item
                                    GenericValue returnItem = EntityQuery.use(delegator).from("ReturnItem").where(
                                            "returnId", returnId, "returnItemSeqId", returnItemSeqId).queryOne();
                                    GenericValue response = returnItem.getRelatedOne("ReturnItemResponse", false);
                                    if (response == null) {
                                        throw new GeneralException("No return response found for: " + returnItem
                                                .getPrimaryKey());
                                    }
                                    String paymentId = response.getString("paymentId");

                                    // create the adjustment transaction
                                    Map<String, Object> txCtx = new HashMap<>();
                                    txCtx.put("finAccountTransTypeId", "ADJUSTMENT");
                                    txCtx.put("finAccountId", finAccountId);
                                    txCtx.put("orderId", orderId);
                                    txCtx.put("orderItemSeqId", orderItemSeqId);
                                    txCtx.put("paymentId", paymentId);
                                    txCtx.put("amount", refAmt.negate());
                                    txCtx.put("partyId", finAccount.getString("ownerPartyId"));
                                    txCtx.put("userLogin", userLogin);

                                    Map<String, Object> txResp = dispatcher.runSync("createFinAccountTrans", txCtx);
                                    if (ServiceUtil.isError(txResp)) {
                                        throw new GeneralException(ServiceUtil.getErrorMessage(txResp));
                                    }
                                }
                            }
                        }
                    }
                } catch (GeneralException e) {
                    Debug.logError(e, module);
                    return ServiceUtil.returnError(e.getMessage());
                }

                // check to make sure we balanced out
                if (remainingBalance.compareTo(FinAccountHelper.ZERO) == 1) {
                    result = ServiceUtil.returnSuccess(UtilProperties.getMessage(resourceError,
                            "AccountingFinAccountPartiallyRefunded", locale));
                }
            }
        }

        if (result == null) {
            result = ServiceUtil.returnSuccess();
        }

        return result;
    }
}
