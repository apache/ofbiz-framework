/*
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
 */

import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.base.util.UtilMisc;

if ("Y".equals(parameters.isSearch)) {
    fromDate = parameters.fromDate;
    thruDate = parameters.thruDate;
    partyId = parameters.partyId;
    productId = parameters.productId;
    invoiceItemAndAssocCond = [];
    if (productId) {
        invoiceItemAndAssocCond.add(EntityCondition.makeCondition("productId", EntityOperator.EQUALS, productId));
    }
    if (partyId) {
        invoiceItemAndAssocCond.add(EntityCondition.makeCondition("partyIdFrom", EntityOperator.EQUALS, partyId));
    }
    if (fromDate) {
        invoiceItemAndAssocCond.add(EntityCondition.makeCondition("fromDate", EntityOperator.GREATER_THAN_EQUAL_TO, Timestamp.valueOf(fromDate)));
    }
    if (thruDate) {
        invoiceItemAndAssocCond.add(EntityCondition.makeCondition("thruDate", EntityOperator.LESS_THAN_EQUAL_TO, Timestamp.valueOf(thruDate)));
    }
    invoiceItemAndAssocList = [];
    invoiceItemAndAssocList = delegator.findList("InvoiceItemAndAssoc", EntityCondition.makeCondition(invoiceItemAndAssocCond, EntityOperator.AND), null, null, null, false);

    //filtering invoiceItemAndAssocList for each productId with updating quantity, commission amount and number of order which generated sales invoices.
    totalQuantity = BigDecimal.ZERO;
    totalNumberOfOrders = BigDecimal.ZERO;
    totalCommissionAmount = BigDecimal.ZERO;
    totalNetSales = BigDecimal.ZERO;
    commissionReportList = [];
    if (invoiceItemAndAssocList) {
        productIds = EntityUtil.getFieldListFromEntityList(invoiceItemAndAssocList, "productId", true);
        productIds.each { productId ->
            quantity = BigDecimal.ZERO;
            commissionAmount = BigDecimal.ZERO;
            termAmount = BigDecimal.ZERO;
            invoiceItemProductAmount = BigDecimal.ZERO;
            assocProductId = null;
            commissionReportMap = [:];
            salesAgentAndTermAmtMap = [:];
            salesInvoiceIds = [];
            invoiceItemAndAssocList.each { invoiceItemAndAssoc ->
                if (productId.equals(invoiceItemAndAssoc.productId)) {
                    partyIdTermAmountMap = [:];
                    partyIdTermAmountKey = null;
                    assocProductId = invoiceItemAndAssoc.productId;
                    quantity = quantity.add(invoiceItemAndAssoc.quantity);
                    commissionAmount = commissionAmount.add(invoiceItemAndAssoc.termAmount.multiply(invoiceItemAndAssoc.quantity));
                    termAmount = termAmount.add(invoiceItemAndAssoc.termAmount);
                    partyIdTermAmountMap.partyId = invoiceItemAndAssoc.partyIdFrom;
                    partyIdTermAmountMap.termAmount = invoiceItemAndAssoc.termAmount;
                    partyIdTermAmountKey = invoiceItemAndAssoc.partyIdFrom + invoiceItemAndAssoc.termAmount;
                    if (!salesAgentAndTermAmtMap.containsKey(partyIdTermAmountKey)) {
                        salesAgentAndTermAmtMap.put(partyIdTermAmountKey, partyIdTermAmountMap);
                    }
                    salesInvoiceIds.add(invoiceItemAndAssoc.invoiceIdFrom);
                    invoiceItemProductAmount = invoiceItemAndAssoc.amount;
                }
            }
            commissionReportMap.productId = assocProductId;
            commissionReportMap.quantity = quantity;
            commissionReportMap.salesAgentAndTermAmtMap = salesAgentAndTermAmtMap;
            commissionReportMap.commissionAmount = commissionAmount;
            commissionReportMap.netSale = invoiceItemProductAmount.multiply(quantity);
            commissionReportMap.salesInvoiceIds = salesInvoiceIds;
            commissionReportMap.numberOfOrders = salesInvoiceIds.size();
            commissionReportList.add(commissionReportMap);
            totalQuantity = totalQuantity.add(quantity);
            totalNumberOfOrders = totalNumberOfOrders.add(salesInvoiceIds.size());
            totalCommissionAmount = totalCommissionAmount.add(commissionAmount);
            totalNetSales = totalNetSales.add(invoiceItemProductAmount.multiply(quantity));
        }
    }
    context.commissionReportList = commissionReportList;
    context.totalQuantity = totalQuantity;
    context.totalNumberOfOrders = totalNumberOfOrders;
    context.totalCommissionAmount = totalCommissionAmount;
    context.totalNetSales = totalNetSales;
}
