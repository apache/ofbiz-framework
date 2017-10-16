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

package org.apache.ofbiz.accounting.agreement;

import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilNumber;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.ModelService;
import org.apache.ofbiz.service.ServiceUtil;

/**
 * Services for Agreement (Accounting)
 */

public class AgreementServices {

    public static final String module = AgreementServices.class.getName();
    // set some BigDecimal properties
    private static BigDecimal ZERO = BigDecimal.ZERO;
    private static int decimals = -1;
    private static int rounding = -1;
    static {
        decimals = UtilNumber.getBigDecimalScale("invoice.decimals");
        rounding = UtilNumber.getBigDecimalRoundingMode("invoice.rounding");

        // set zero to the proper scale
        if (decimals != -1) ZERO = ZERO.setScale(decimals, rounding);
    }
    public static final String resource = "AccountingUiLabels";

    /**
     * Determines commission receiving parties and amounts for the provided product, price, and quantity
     * @param ctx The DispatchContext that this service is operating in.
     * @param context Map containing the input parameters.
     *      productId   String      Product Id
     *      invoiceItemTypeId   String      Invoice Type
     *      amount      BigDecimal  Entire amount
     *      quantity    BigDecimal  Quantity
     * @return Map with the result of the service, the output parameters.
     *      commissions List        List of Maps each containing
     *              partyIdFrom     String  commission paying party
     *              partyIdTo       String  commission receiving party
     *              commission      BigDecimal  Commission
     *              days            Long    term days
     *              currencyUomId   String  Currency
     *              productId       String  Product Id
     */
    public static Map<String, Object> getCommissionForProduct(DispatchContext ctx, Map<String, Object> context) {
        Delegator delegator = ctx.getDelegator();
        Locale locale = (Locale) context.get("locale");
        String errMsg = null;
        List<Map<String, Object>> commissions = new LinkedList<Map<String,Object>>();

        try {
            BigDecimal amount = ((BigDecimal)context.get("amount"));
            BigDecimal quantity = (BigDecimal)context.get("quantity");
            quantity = quantity == null ? BigDecimal.ONE : quantity;
            boolean negative = amount.signum() < 0;
            // Ensure that price and quantity are positive since the terms may not be linear.
            amount = amount.abs();
            quantity = quantity.abs();
            String productId = (String) context.get("productId");
            String invoiceItemTypeId = (String) context.get("invoiceItemTypeId");
            String invoiceItemSeqId = (String) context.get("invoiceItemSeqId");
            String invoiceId = (String) context.get("invoiceId");

            // Collect agreementItems applicable to this orderItem/returnItem
            // TODO: partyIds should be part of this query!
            List<GenericValue> agreementItems = EntityQuery.use(delegator).from("AgreementItemAndProductAppl")
                    .where("productId", productId, "agreementItemTypeId", "AGREEMENT_COMMISSION")
                    .cache().filterByDate().queryList();
            // Try the first available virtual product if this is a variant product
            if (agreementItems.size() == 0) {
                GenericValue productAssoc = EntityQuery.use(delegator).from("ProductAssoc")
                        .where("productIdTo", productId, "productAssocTypeId", "PRODUCT_VARIANT")
                        .cache().filterByDate().queryFirst();
                if (productAssoc != null) {
                    agreementItems = EntityQuery.use(delegator).from("AgreementItemAndProductAppl")
                            .where("productId", productAssoc.getString("productId"), "agreementItemTypeId", "AGREEMENT_COMMISSION")
                            .cache().filterByDate().queryList();
                }
            }

            for (GenericValue agreementItem : agreementItems) {
                List<GenericValue> terms = EntityQuery.use(delegator).from("AgreementTerm")
                        .where("agreementId", agreementItem.getString("agreementId"),
                                "agreementItemSeqId", agreementItem.getString("agreementItemSeqId"),
                                "invoiceItemTypeId", invoiceItemTypeId)
                                .cache().queryList();
                if (terms.size() > 0) {
                    BigDecimal commission = ZERO;
                    BigDecimal min = new BigDecimal("-1e12");   // Limit to 1 trillion commission
                    BigDecimal max = new BigDecimal("1e12");

                    // number of days due for commission, which will be the lowest termDays of all the AgreementTerms
                    long days = -1;
                    for (GenericValue term : terms) {
                        String termTypeId = term.getString("termTypeId");
                        BigDecimal termValue = term.getBigDecimal("termValue");
                        if (termValue != null) {
                            if ("FIN_COMM_FIXED".equals(termTypeId)) {
                                commission = commission.add(termValue);
                            } else if ("FIN_COMM_VARIABLE".equals(termTypeId)) {
                                // if variable percentage commission, need to divide by 100, because 5% is stored as termValue of 5.0
                                commission = commission.add(termValue.multiply(amount).divide(new BigDecimal("100"), 12, rounding));
                            } else if ("FIN_COMM_MIN".equals(termTypeId)) {
                                min = termValue;
                            } else if ("FIN_COMM_MAX".equals(termTypeId)) {
                                max = termValue;
                            }
                            // TODO: Add other type of terms and handling here
                        }

                        // see if we need to update the number of days for paying commission
                        Long termDays = term.getLong("termDays");
                        if (termDays != null) {
                            // if days is greater than zero, then it has been set with another value, so we use the lowest term days
                            // if days is less than zero, then it has not been set yet.
                            if (days > 0) {
                                days = Math.min(days, termDays.longValue());
                            } else {
                                days = termDays.longValue();
                            }
                        }
                    }
                    if (commission.compareTo(min) < 0)
                        commission = min;
                    if (commission.compareTo(max) > 0)
                        commission = max;
                    commission = negative ? commission.negate() : commission;
                    commission = commission.setScale(decimals, rounding);

                    Map<String, Object> partyCommissionResult = UtilMisc.toMap(
                            "partyIdFrom", agreementItem.getString("partyIdFrom"),
                            "partyIdTo", agreementItem.getString("partyIdTo"),
                            "invoiceItemSeqId", invoiceItemSeqId,
                            "invoiceId", invoiceId,
                            "commission", commission,
                            "quantity", quantity,
                            "currencyUomId", agreementItem.getString("currencyUomId"),
                            "productId", productId);
                    if (days >= 0) {
                        partyCommissionResult.put("days", Long.valueOf(days));
                    }
                    if (!commissions.contains(partyCommissionResult)) {
                        commissions.add(partyCommissionResult);
                    }
                }
            }
        } catch (GenericEntityException e) {
            Debug.logWarning(e, module);
            Map<String, String> messageMap = UtilMisc.toMap("errMessage", e.getMessage());
            errMsg = UtilProperties.getMessage("CommonUiLabels", "CommonDatabaseProblem", messageMap, locale);
            return ServiceUtil.returnError(errMsg);
        }
        return UtilMisc.toMap(
                "commissions", commissions,
                ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
    }
}
