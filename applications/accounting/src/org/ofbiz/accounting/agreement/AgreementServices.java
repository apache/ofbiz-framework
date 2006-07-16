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

package org.ofbiz.accounting.agreement;

import java.math.BigDecimal;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javolution.util.FastList;
import javolution.util.FastMap;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilNumber;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntity;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.security.Security;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.ModelService;
import org.ofbiz.service.ServiceUtil;

/**
 * Services for Agreement (Accounting)
 * @author     Vinay Agarwal
 * @version    1.0
 */

public class AgreementServices {
    
    public static final String module = AgreementServices.class.getName();
    // set some BigDecimal properties
    private static BigDecimal ZERO = new BigDecimal("0");
    private static int decimals = -1;
    private static int rounding = -1;
    static {
        decimals = UtilNumber.getBigDecimalScale("invoice.decimals");
        rounding = UtilNumber.getBigDecimalRoundingMode("invoice.rounding");

        // set zero to the proper scale
        if (decimals != -1) ZERO.setScale(decimals, rounding);
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
    public static Map getCommissionForProduct(DispatchContext ctx, Map context) {
        Map result = FastMap.newInstance();
        GenericDelegator delegator = ctx.getDelegator();
        Security security = ctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = (Locale) context.get("locale");
        String errMsg = null;
        List commissions = FastList.newInstance();
        
        // either ACCOUNTING_COMM_VIEW or ACCOUNTING_MANAGER should be allowed to see commission amounts
        String partyId = ServiceUtil.getPartyIdCheckSecurity(userLogin, security, context, result, "ACCOUNTING", "_COMM_VIEW");
        if (result.size() > 0)
            return result;
        try {
            BigDecimal amount = ((BigDecimal)context.get("amount"));
            BigDecimal quantity = (BigDecimal)context.get("quantity");
            quantity = quantity == null ? new BigDecimal("1") : quantity;
            boolean negative = amount.signum() < 0;
            // Ensure that price and quantity are positive since the terms may not be linear.
            amount = amount.abs();
            quantity = quantity.abs();
            String productId = (String) context.get("productId");
            String invoiceItemTypeId = (String) context.get("invoiceItemTypeId");
            // Collect agreementItems applicable to this orderItem/returnItem
            // Use the view entity to reduce database access and cache to improve performance
            List agreementItems = delegator.findByAndCache("AgreementItemAndProductAppl", UtilMisc.toMap(
                    "productId", productId,
                    "agreementItemTypeId", "AGREEMENT_COMMISSION"));
            // Try the first available virtual product if this is a variant product
            if (agreementItems.size() == 0) {
                List productAssocs = delegator.findByAndCache("ProductAssoc", UtilMisc.toMap(
                        "productIdTo", productId,
                        "productAssocTypeId", "PRODUCT_VARIANT"));
                if (productAssocs.size() > 0) {
                    GenericEntity productAssoc = EntityUtil.getFirst(productAssocs);
                    agreementItems = delegator.findByAndCache("AgreementItemAndProductAppl", UtilMisc.toMap(
                            "productId", productAssoc.getString("productId"),
                            "agreementItemTypeId", "AGREEMENT_COMMISSION"));
                }
            }
            
            Iterator it = agreementItems.iterator();
            while (it.hasNext()) {
                GenericValue agreementItem = (GenericValue) it.next();
                List terms = delegator.findByAndCache("AgreementTerm", UtilMisc.toMap(
                        "agreementId", agreementItem.getString("agreementId"),
                        "agreementItemSeqId", agreementItem.getString("agreementItemSeqId"),
                        "invoiceItemTypeId", invoiceItemTypeId));
                if (terms.size() > 0) {
                    BigDecimal commission = ZERO;
                    BigDecimal min = new BigDecimal("-1e12");   // Limit to 1 trillion commission
                    BigDecimal max = new BigDecimal("1e12");
                    long days = 0;
                    Iterator itt = terms.iterator();
                    while (itt.hasNext()) {
                        GenericValue elem = (GenericValue) itt.next();
                        String termTypeId = elem.getString("termTypeId");
                        BigDecimal termValue = elem.getBigDecimal("termValue");
                        if (termValue != null) {
                            if (termTypeId.equals("FIN_COMM_FIXED")) {
                                commission = commission.add(termValue.multiply(quantity));
                            } else if (termTypeId.equals("FIN_COMM_VARIABLE")) {
                                // if variable percentage commission, need to divide by 100, because 5% is stored as termValue of 5.0
                                commission = commission.add(termValue.multiply(amount).divide(new BigDecimal("100"), 12, rounding));
                            } else if (termTypeId.equals("FIN_COMM_MIN")) {
                                min = termValue.multiply(quantity);
                            } else if (termTypeId.equals("FIN_COMM_MAX")) {
                                max = termValue.multiply(quantity);
                            }
                            // TODO: Add other type of terms and handling here
                        }
                        Long termDays = elem.getLong("termDays");
                        if (termDays != null) {
                            days = Math.min(days, termDays.longValue());
                        }
                    }
                    if (commission.compareTo(min) < 0)
                        commission = min;
                    if (commission.compareTo(max) > 0)
                        commission = max;
                    commission = negative ? commission.negate() : commission;
                    commission = commission.setScale(decimals, rounding);
                    days = Math.max(0, days);
                    commissions.add(UtilMisc.toMap(
                            "partyIdFrom", agreementItem.getString("partyIdFrom"),
                            "partyIdTo", agreementItem.getString("partyIdTo"),
                            "commission", commission,
                            "days", new Long(days),
                            "currencyUomId", agreementItem.getString("currencyUomId"),
                            "productId", productId));
                }
            }
        } catch (GenericEntityException e) {
            Debug.logWarning(e, module);
            Map messageMap = UtilMisc.toMap("errMessage", e.getMessage());
            errMsg = UtilProperties.getMessage(resource, "AccountingDataSourceError", messageMap, locale);
            return ServiceUtil.returnError(errMsg);
        }
        return UtilMisc.toMap(
                "commissions", commissions,
                ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
    }
}
