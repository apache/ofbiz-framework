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
package com.simbaquartz.xaccounting.services;

import org.apache.ofbiz.accounting.invoice.InvoiceWorker;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilNumber;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.EntityUtil;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.ServiceUtil;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;


/**
 * InvoiceWorker - Worker methods of invoices
 */
public class AxInvoiceWorker {

    public static String module = AxInvoiceWorker.class.getName();
    private static BigDecimal ZERO = BigDecimal.ZERO;
    private static int decimals = UtilNumber.getBigDecimalScale("invoice.decimals");
    private static int rounding = UtilNumber.getBigDecimalRoundingMode("invoice.rounding");


    /**
     * Return the total amount of the invoice (including tax) using the the invoiceId as input.
     * @param delegator the delegator
     * @param invoiceId the invoice id
     * @return Return the total amount of the invoice 
     */
    public static BigDecimal getInvoiceTotal(Delegator delegator, String invoiceId) {
        return getInvoiceTotal(delegator, invoiceId, Boolean.TRUE);
    }

    /**
     * Return the total amount of the invoice (including tax) using the the invoiceId as input.
     * with the ability to specify if the actual currency is required.
     * @param delegator the delegator
     * @param invoiceId the invoice Id
     * @param actualCurrency true: provide the actual currency of the invoice (could be different from the system currency)
     *                       false: if required convert the actual currency into the system currency.
     * @return Return the total amount of the invoice
     */
    public static BigDecimal getInvoiceTotal(Delegator delegator, String invoiceId, Boolean actualCurrency) {
        if (delegator == null) {
            throw new IllegalArgumentException("Null delegator is not allowed in this method");
        }

        GenericValue invoice = null;
        try {
            invoice = EntityQuery.use(delegator).from("Invoice").where("invoiceId", invoiceId).queryOne();
        } catch (GenericEntityException e) {
            Debug.logError(e, "Problem getting Invoice", module);
        }

        if (invoice == null) {
            throw new IllegalArgumentException("The passed invoiceId [" +invoiceId + "] does not match an existing invoice");
        }

        return getInvoiceTotal(invoice, actualCurrency);
    }

    /**
     * Method to return the total amount of an invoice item i.e. quantity * amount
     * @param invoiceItem GenericValue object of the invoice item
     * @return the invoice total as BigDecimal
     */
    public static BigDecimal getInvoiceItemTotal(GenericValue invoiceItem) {
        BigDecimal quantity = invoiceItem.getBigDecimal("quantity");
        if (quantity == null) {
            quantity = BigDecimal.ONE;
        }
        BigDecimal amount = invoiceItem.getBigDecimal("amount");
        if (amount == null) {
            amount = ZERO;
        }
        BigDecimal quantityToConsider = getInvoiceItemAdjustmentQuantity(invoiceItem);
        return quantityToConsider.multiply(amount).setScale(decimals, rounding);
    }

    public static BigDecimal getInvoiceNoTaxTotal(GenericValue invoice) {
        return getInvoiceTotal(invoice, Boolean.TRUE).subtract(InvoiceWorker.getInvoiceTaxTotal(invoice));
    }

    /**
     * Method to return the total amount of an invoice
     * @param invoice GenericValue object of the Invoice
     * @return the invoice total as BigDecimal
     */
     public static BigDecimal getInvoiceTotal(GenericValue invoice) {
        return getInvoiceTotal(invoice, Boolean.TRUE);
    }

    //service implementation to get invoice total
    public static Map<String, Object> getFsdInvoiceTotal(DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
        GenericValue invoice = (GenericValue) context.get("invoice");
        BigDecimal invoiceTotal =  getInvoiceTotal(invoice, Boolean.TRUE);
        serviceResult.put("invoiceTotal", invoiceTotal);
        return serviceResult;
    }

     /**
      *
      * Return the total amount of the invoice (including tax) using the the invoice GenericValue as input.
      * with the ability to specify if the actual currency is required.
      * @param invoice GenericValue object of the Invoice
      * @param actualCurrency true: provide the actual currency of the invoice (could be different from the system currency)
      *                       false: if required convert the actual currency into the system currency.
      * @return Return the total amount of the invoice
      */
     public static BigDecimal getInvoiceTotal(GenericValue invoice, Boolean actualCurrency) {
        BigDecimal invoiceTotal = ZERO;
        BigDecimal invoiceTaxTotal = ZERO;
        invoiceTaxTotal = InvoiceWorker.getInvoiceTaxTotal(invoice);

        List<GenericValue> invoiceItems = null;
        try {
            invoiceItems = invoice.getRelated("InvoiceItem", null, null, false);
            invoiceItems = EntityUtil.filterByAnd(
                    invoiceItems, UtilMisc.toList(
                            EntityCondition.makeCondition("invoiceItemTypeId", EntityOperator.NOT_IN, InvoiceWorker.getTaxableInvoiceItemTypeIds(invoice.getDelegator()))
                    ));
        } catch (GenericEntityException e) {
            Debug.logError(e, "Trouble getting InvoiceItem list", module);
        }
        if (invoiceItems != null) {
            for (GenericValue invoiceItem : invoiceItems) {
                invoiceTotal = invoiceTotal.add(getInvoiceItemTotal(invoiceItem)).setScale(decimals,rounding);
            }
        }
        invoiceTotal = invoiceTotal.add(invoiceTaxTotal).setScale(decimals, rounding);
        if (UtilValidate.isNotEmpty(invoiceTotal) && !actualCurrency) {
            invoiceTotal = invoiceTotal.multiply(InvoiceWorker.getInvoiceCurrencyConversionRate(invoice)).setScale(decimals,rounding);
        }
        return invoiceTotal;
    }


    /**
     * For Adjustment Invoice item - calculates the quantity from corresponding OrderAdjustment
     * for others, returns its own quantity
     * @param invoiceItem
     * @return
     */
    public static BigDecimal getInvoiceItemAdjustmentQuantity(GenericValue invoiceItem) {
        BigDecimal quantity = invoiceItem.getBigDecimal("quantity");
        if (quantity == null) {
            quantity = BigDecimal.ONE;
        }
        BigDecimal quantityToConsider = quantity;

        if(invoiceItem.get("invoiceItemTypeId").equals("INV_TRADE_IN_ITEM") ||
                invoiceItem.get("invoiceItemTypeId").equals("PINV_TRADE_IN_ITEM")) {

            // Get adjustmentQuantity from OrderAdjustment using OrderAdjustmentBilling
            try {
                GenericValue invoicedAdjustment = EntityQuery.use(invoiceItem.getDelegator())
                        .from("OrderAdjustmentBilling")
                        .where("invoiceId", invoiceItem.get("invoiceId"),
                                "invoiceItemSeqId", invoiceItem.get("invoiceItemSeqId"))
                        .queryFirst();

                if (UtilValidate.isNotEmpty(invoicedAdjustment)) {
                    String orderAdjustmentId = (String) invoicedAdjustment.get("orderAdjustmentId");
                    GenericValue orderAdjustment = EntityQuery.use(invoiceItem.getDelegator())
                            .from("OrderAdjustment").where("orderAdjustmentId", orderAdjustmentId).queryOne();

                    BigDecimal adjustmentQuantity = (BigDecimal) orderAdjustment.get("adjustmentQuantity");
                    if (UtilValidate.isNotEmpty(adjustmentQuantity))
                        quantityToConsider = adjustmentQuantity;
                }
            } catch (GenericEntityException e) {
            }
        }
        return quantityToConsider;
    }


    /** Given an invoice Item, get it's parent invoice item's quantity
     *
     * @param invoiceItem
     * @return
     */
    public static BigDecimal getParentInvoiceItemQuantity(GenericValue invoiceItem) {
        BigDecimal quantity = invoiceItem.getBigDecimal("quantity");
        if (quantity == null) {
            quantity = BigDecimal.ONE;
        }
        BigDecimal parentItemQuantity = quantity;

        try {
            GenericValue parentInvoiceItem = EntityQuery.use(invoiceItem.getDelegator())
                    .from("InvoiceItem")
                    .where("invoiceId", invoiceItem.get("parentInvoiceId"),
                            "invoiceItemSeqId", invoiceItem.get("parentInvoiceItemSeqId"))
                    .queryOne();

            if (UtilValidate.isNotEmpty(parentInvoiceItem)) {
                parentItemQuantity = parentInvoiceItem.getBigDecimal("quantity");
            }
        } catch (GenericEntityException e) {
        }
        return parentItemQuantity;
    }

    public static BigDecimal getInvoiceNotApplied(Delegator delegator, String invoiceId) {
        return AxInvoiceWorker.getInvoiceTotal(delegator, invoiceId).subtract(InvoiceWorker.getInvoiceApplied(delegator, invoiceId));
    }
    public static BigDecimal getInvoiceNotApplied(Delegator delegator, GenericValue invoice) throws GenericEntityException {
        return getInvoicePending(delegator, invoice);
    }

    private static BigDecimal getInvoicePending (Delegator delegator, GenericValue invoice) throws GenericEntityException {
        return AxInvoiceWorker.getInvoiceTotal(invoice, Boolean.TRUE).subtract(InvoiceWorker.getInvoiceApplied(invoice));
    }
}

