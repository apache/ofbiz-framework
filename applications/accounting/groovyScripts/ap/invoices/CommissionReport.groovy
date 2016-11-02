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


import org.apache.ofbiz.entity.condition.EntityCondition
import org.apache.ofbiz.entity.condition.EntityOperator
import org.apache.ofbiz.entity.util.EntityUtil

import java.sql.Timestamp

if ("Y".equals(parameters.isSearch)) {
    fromDate = parameters.fromDate
    thruDate = parameters.thruDate
    partyId = parameters.partyId
    productId = parameters.productId
    invoiceItemAndAssocProductCond = []
    if (productId) {
        invoiceItemAndAssocProductCond.add(EntityCondition.makeCondition("productId", EntityOperator.EQUALS, productId))
    }
    if (partyId) {
        invoiceItemAndAssocProductCond.add(EntityCondition.makeCondition("partyIdFrom", EntityOperator.EQUALS, partyId))
    }
    if (fromDate) {
        invoiceItemAndAssocProductCond.add(EntityCondition.makeCondition("fromDate", EntityOperator.GREATER_THAN_EQUAL_TO, Timestamp.valueOf(fromDate)))
    }
    if (thruDate) {
        invoiceItemAndAssocProductCond.add(EntityCondition.makeCondition("thruDate", EntityOperator.LESS_THAN_EQUAL_TO, Timestamp.valueOf(thruDate)))
    }
    invoiceItemAndAssocProductList = []
    invoiceItemAndAssocProductList = from("InvoiceItemAndAssocProduct").where(invoiceItemAndAssocProductCond).queryList()

    //filtering invoiceItemAndAssocProductList for each productId with updating quantity, commission amount and number of order which generated sales invoices.
    totalQuantity = BigDecimal.ZERO
    totalNumberOfOrders = BigDecimal.ZERO
    totalCommissionAmount = BigDecimal.ZERO
    totalNetSales = BigDecimal.ZERO
    commissionReportList = []
    if (invoiceItemAndAssocProductList) {
        productIds = EntityUtil.getFieldListFromEntityList(invoiceItemAndAssocProductList, "productId", true)
        productIds.each { productId ->
            quantity = BigDecimal.ZERO
            commissionAmount = BigDecimal.ZERO
            termAmount = BigDecimal.ZERO
            invoiceItemProductAmount = BigDecimal.ZERO
            assocProductId = null
            productName = null
            commissionReportMap = [:]
            salesAgentAndTermAmtMap = [:]
            salesInvoiceIds = []
            invoiceItemAndAssocProductList.each { invoiceItemAndAssocProduct ->
                if (productId.equals(invoiceItemAndAssocProduct.productId)) {
                    partyIdTermAmountMap = [:]
                    partyIdTermAmountKey = null
                    assocProductId = invoiceItemAndAssocProduct.productId
                    productName = invoiceItemAndAssocProduct.productName
                    quantity = quantity.add(invoiceItemAndAssocProduct.quantity)
                    commissionAmount = commissionAmount.add(invoiceItemAndAssocProduct.termAmount.multiply(invoiceItemAndAssocProduct.quantity))
                    termAmount = termAmount.add(invoiceItemAndAssocProduct.termAmount)
                    partyIdTermAmountMap.partyId = invoiceItemAndAssocProduct.partyIdFrom
                    partyIdTermAmountMap.termAmount = invoiceItemAndAssocProduct.termAmount
                    partyIdTermAmountKey = invoiceItemAndAssocProduct.partyIdFrom + invoiceItemAndAssocProduct.termAmount
                    if (!salesAgentAndTermAmtMap.containsKey(partyIdTermAmountKey)) {
                        salesAgentAndTermAmtMap.put(partyIdTermAmountKey, partyIdTermAmountMap)
                    }
                    salesInvoiceIds.add(invoiceItemAndAssocProduct.invoiceIdFrom)
                    invoiceItemProductAmount = invoiceItemAndAssocProduct.amount
                }
            }
            commissionReportMap.productId = assocProductId
            commissionReportMap.productName = productName
            commissionReportMap.quantity = quantity
            commissionReportMap.salesAgentAndTermAmtMap = salesAgentAndTermAmtMap
            commissionReportMap.commissionAmount = commissionAmount
            commissionReportMap.netSale = invoiceItemProductAmount.multiply(quantity)
            commissionReportMap.salesInvoiceIds = salesInvoiceIds
            commissionReportMap.numberOfOrders = salesInvoiceIds.size()
            commissionReportList.add(commissionReportMap)
            totalQuantity = totalQuantity.add(quantity)
            totalNumberOfOrders = totalNumberOfOrders.add(salesInvoiceIds.size())
            totalCommissionAmount = totalCommissionAmount.add(commissionAmount)
            totalNetSales = totalNetSales.add(invoiceItemProductAmount.multiply(quantity))
        }
    }
    context.commissionReportList = commissionReportList
    context.totalQuantity = totalQuantity
    context.totalNumberOfOrders = totalNumberOfOrders
    context.totalCommissionAmount = totalCommissionAmount
    context.totalNetSales = totalNetSales
}
