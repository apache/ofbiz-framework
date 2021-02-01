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
package org.apache.ofbiz.accounting.invoice;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.ofbiz.accounting.payment.PaymentGatewayServices;
import org.apache.ofbiz.accounting.payment.PaymentWorker;
import org.apache.ofbiz.accounting.util.UtilAccounting;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilFormatOut;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilNumber;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityExpr;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.EntityTypeUtil;
import org.apache.ofbiz.entity.util.EntityUtil;
import org.apache.ofbiz.entity.util.EntityUtilProperties;
import org.apache.ofbiz.order.order.OrderReadHelper;
import org.apache.ofbiz.product.product.ProductWorker;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;

/**
 * InvoiceServices - Services for creating invoices
 * <p>
 * Note that throughout this file we use BigDecimal to do arithmetic. It is
 * critical to understand the way BigDecimal works if you wish to modify the
 * computations in this file. The most important things to keep in mind:
 * <p>
 * Critically important: BigDecimal arithmetic methods like add(),
 * multiply(), divide() do not modify the BigDecimal itself. Instead, they
 * return a new BigDecimal. For example, to keep a running total of an
 * amount, make sure you do this:
 * <p>
 * amount = amount.add(subAmount);
 * <p>
 * and not this,
 * <p>
 * amount.add(subAmount);
 * <p>
 * Use .setScale(scale, roundingMode) after every computation to scale and
 * round off the decimals. Check the code to see how the scale and
 * roundingMode are obtained and how the function is used.
 * <p>
 * use .compareTo() to compare big decimals
 * <p>
 * ex.  (amountOne.compareTo(amountTwo) == 1)
 * checks if amountOne is greater than amountTwo
 * <p>
 * Use .signum() to test if value is negative, zero, or positive
 * <p>
 * ex.  (amountOne.signum() == 1)
 * checks if the amount is a positive non-zero number
 * <p>
 * Never use the .equals() function becaues it considers 2.0 not equal to 2.00 (the scale is different)
 * Instead, use .compareTo() or .signum(), which handles scale correctly.
 * <p>
 * For reference, check the official Sun Javadoc on java.math.BigDecimal.
 */
public class InvoiceServices {

    private static final String MODULE = InvoiceServices.class.getName();

    // set some BigDecimal properties
    private static final int DECIMALS = UtilNumber.getBigDecimalScale("invoice.decimals");
    private static final RoundingMode ROUNDING = UtilNumber.getRoundingMode("invoice.rounding");
    private static final int TAX_DECIMALS = UtilNumber.getBigDecimalScale("salestax.calc.decimals");
    private static final RoundingMode TAX_ROUNDING = UtilNumber.getRoundingMode("salestax.rounding");
    private static final int INVOICE_ITEM_SEQUENCE_ID_DIGITS = 5; // this is the number of digits used for invoiceItemSeqId: 00001, 00002...

    private static final String RESOURCE = "AccountingUiLabels";

    // service to create an invoice for a complete order by the system userid
    public static Map<String, Object> createInvoiceForOrderAllItems(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Locale locale = (Locale) context.get("locale");
        try {
            List<GenericValue> orderItems = EntityQuery.use(delegator).from("OrderItem")
                    .where("orderId", context.get("orderId")).orderBy("orderItemSeqId").queryList();
            if (!orderItems.isEmpty()) {
                context.put("billItems", orderItems);
            }
            // get the system userid and store in context otherwise the invoice add service does not work
            GenericValue userLogin = EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "system").queryOne();
            if (userLogin != null) {
                context.put("userLogin", userLogin);
            }

            Map<String, Object> result = dispatcher.runSync("createInvoiceForOrder", context);
            if (ServiceUtil.isError(result)) {
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(result));
            }
            result.remove("invoiceTypeId");  //remove extra parameter
            return result;
        } catch (GenericServiceException | GenericEntityException e) {
            Debug.logError(e, "Entity/data problem creating invoice from order items: " + e.toString(), MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    "AccountingEntityDataProblemCreatingInvoiceFromOrderItems",
                    UtilMisc.toMap("reason", e.toString()), locale));
        }
    }

    /** Service to create an invoice for an order */
    public static Map<String, Object> createInvoiceForOrder(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = (Locale) context.get("locale");
        if (DECIMALS == -1 || ROUNDING == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    "AccountingAritmeticPropertiesNotConfigured", locale));
        }

        String orderId = (String) context.get("orderId");
        List<GenericValue> billItems = UtilGenerics.cast(context.get("billItems"));
        String invoiceId = (String) context.get("invoiceId");

        if (UtilValidate.isEmpty(billItems)) {
            if (Debug.verboseOn()) {
                Debug.logVerbose("No order items to invoice; not creating invoice; returning success", MODULE);
            }
            return ServiceUtil.returnSuccess(UtilProperties.getMessage(RESOURCE,
                    "AccountingNoOrderItemsToInvoice", locale));
        }

        try {
            GenericValue orderHeader = EntityQuery.use(delegator).from("OrderHeader").where("orderId", orderId).queryOne();
            if (orderHeader == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                        "AccountingNoOrderHeader", locale));
            }

            // figure out the invoice type
            String invoiceType = null;

            String orderType = orderHeader.getString("orderTypeId");
            if ("SALES_ORDER".equals(orderType)) {
                invoiceType = "SALES_INVOICE";
            } else if ("PURCHASE_ORDER".equals(orderType)) {
                invoiceType = "PURCHASE_INVOICE";
            }

            // Set the precision depending on the type of invoice
            int invoiceTypeDecimals = UtilNumber.getBigDecimalScale("invoice." + invoiceType + ".decimals");
            if (invoiceTypeDecimals == -1) {
                invoiceTypeDecimals = DECIMALS;
            }

            // Make an order read helper from the order
            OrderReadHelper orh = new OrderReadHelper(orderHeader);

            // get the product store
            GenericValue productStore = orh.getProductStore();

            // get the shipping adjustment mode (Y = Pro-Rate; N = First-Invoice)
            String prorateShipping = productStore != null ? productStore.getString("prorateShipping") : "Y";
            if (prorateShipping == null) {
                prorateShipping = "Y";
            }

            // get the billing parties
            String billToCustomerPartyId = orh.getBillToParty().getString("partyId");
            String billFromVendorPartyId = orh.getBillFromParty().getString("partyId");

            // get some price totals
            BigDecimal shippableAmount = orh.getShippableTotal(null);
            BigDecimal shippableQuantity = orh.getShippableQuantity(null);
            BigDecimal orderSubTotal = orh.getOrderItemsSubTotal();
            BigDecimal orderQuantity = orh.getTotalOrderItemsQuantity();

            // these variables are for pro-rating order amounts across invoices, so they should not be rounded off for maximum accuracy
            BigDecimal invoiceShipProRateAmount = BigDecimal.ZERO;
            BigDecimal invoiceShippableQuantity = BigDecimal.ZERO;
            BigDecimal invoiceSubTotal = BigDecimal.ZERO;
            BigDecimal invoiceQuantity = BigDecimal.ZERO;

            GenericValue billingAccount = orderHeader.getRelatedOne("BillingAccount", false);
            String billingAccountId = billingAccount != null ? billingAccount.getString("billingAccountId") : null;

            Timestamp invoiceDate = (Timestamp) context.get("eventDate");
            if (UtilValidate.isEmpty(invoiceDate)) {
                // TODO: ideally this should be the same time as when a shipment is sent and be passed in as a parameter
                invoiceDate = UtilDateTime.nowTimestamp();
            }
            // TODO: perhaps consider billing account net days term as well?
            Long orderTermNetDays = orh.getOrderTermNetDays();
            Timestamp dueDate = null;
            if (orderTermNetDays != null) {
                dueDate = UtilDateTime.getDayEnd(invoiceDate, orderTermNetDays);
            }

            // create the invoice record
            if (UtilValidate.isEmpty(invoiceId)) {
                Map<String, Object> createInvoiceContext = new HashMap<>();
                createInvoiceContext.put("partyId", billToCustomerPartyId);
                createInvoiceContext.put("partyIdFrom", billFromVendorPartyId);
                createInvoiceContext.put("billingAccountId", billingAccountId);
                createInvoiceContext.put("invoiceDate", invoiceDate);
                createInvoiceContext.put("dueDate", dueDate);
                createInvoiceContext.put("invoiceTypeId", invoiceType);
                // start with INVOICE_IN_PROCESS, in the INVOICE_READY we can't change the invoice (or shouldn't be able to...)
                createInvoiceContext.put("statusId", "INVOICE_IN_PROCESS");
                createInvoiceContext.put("currencyUomId", orderHeader.getString("currencyUom"));
                createInvoiceContext.put("userLogin", userLogin);

                // store the invoice first
                Map<String, Object> createInvoiceResult = dispatcher.runSync("createInvoice", createInvoiceContext);
                if (ServiceUtil.isError(createInvoiceResult)) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                            "AccountingErrorCreatingInvoiceFromOrder", locale), null, null, createInvoiceResult);
                }

                // call service, not direct entity op: delegator.create(invoice);
                invoiceId = (String) createInvoiceResult.get("invoiceId");
            }

            // order roles to invoice roles
            List<GenericValue> orderRoles = orderHeader.getRelated("OrderRole", null, null, false);
            Map<String, Object> createInvoiceRoleContext = new HashMap<>();
            createInvoiceRoleContext.put("invoiceId", invoiceId);
            createInvoiceRoleContext.put("userLogin", userLogin);
            for (GenericValue orderRole : orderRoles) {
                createInvoiceRoleContext.put("partyId", orderRole.getString("partyId"));
                createInvoiceRoleContext.put("roleTypeId", orderRole.getString("roleTypeId"));
                Map<String, Object> createInvoiceRoleResult = dispatcher.runSync("createInvoiceRole", createInvoiceRoleContext);
                if (ServiceUtil.isError(createInvoiceRoleResult)) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                            "AccountingErrorCreatingInvoiceFromOrder", locale), null, null, createInvoiceRoleResult);
                }
            }

            // order terms to invoice terms.
            // TODO: it might be nice to filter OrderTerms to only copy over financial terms.
            List<GenericValue> orderTerms = orh.getOrderTerms();
            createInvoiceTerms(delegator, dispatcher, invoiceId, orderTerms, userLogin, locale);

            // billing accounts
            // List billingAccountTerms = null;
            // for billing accounts we will use related information
            if (billingAccount != null) {
                /*
                 * jacopoc: billing account terms were already copied as order terms
                 *          when the order was created.
                // get the billing account terms
                billingAccountTerms = billingAccount.getRelated("BillingAccountTerm", null, null, false);

                // set the invoice terms as defined for the billing account
                createInvoiceTerms(delegator, dispatcher, invoiceId, billingAccountTerms, userLogin, locale);
                */
                // set the invoice bill_to_customer from the billing account
                List<GenericValue> billToRoles = billingAccount.getRelated("BillingAccountRole", UtilMisc.toMap("roleTypeId", "BILL_TO_CUSTOMER"),
                        null, false);
                for (GenericValue billToRole : billToRoles) {
                    if (!(billToRole.getString("partyId").equals(billToCustomerPartyId))) {
                        createInvoiceRoleContext = UtilMisc.toMap("invoiceId", invoiceId, "partyId", billToRole.get("partyId"),
                                "roleTypeId", "BILL_TO_CUSTOMER", "userLogin", userLogin);
                        Map<String, Object> createInvoiceRoleResult = dispatcher.runSync("createInvoiceRole", createInvoiceRoleContext);
                        if (ServiceUtil.isError(createInvoiceRoleResult)) {
                            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                                    "AccountingErrorCreatingInvoiceRoleFromOrder", locale), null, null, createInvoiceRoleResult);
                        }
                    }
                }

                // set the bill-to contact mech as the contact mech of the billing account
                if (UtilValidate.isNotEmpty(billingAccount.getString("contactMechId"))) {
                    Map<String, Object> createBillToContactMechContext = UtilMisc.toMap("invoiceId", invoiceId, "contactMechId",
                            billingAccount.getString("contactMechId"),
                            "contactMechPurposeTypeId", "BILLING_LOCATION", "userLogin", userLogin);
                    Map<String, Object> createBillToContactMechResult = dispatcher.runSync("createInvoiceContactMech",
                            createBillToContactMechContext);
                    if (ServiceUtil.isError(createBillToContactMechResult)) {
                        return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                                "AccountingErrorCreatingInvoiceContactMechFromOrder", locale), null, null, createBillToContactMechResult);
                    }
                }
            } else {
                List<GenericValue> billingLocations = orh.getBillingLocations();
                if (UtilValidate.isNotEmpty(billingLocations)) {
                    for (GenericValue ocm : billingLocations) {
                        Map<String, Object> createBillToContactMechContext = UtilMisc.toMap("invoiceId", invoiceId, "contactMechId", ocm.getString(
                                "contactMechId"),
                                "contactMechPurposeTypeId", "BILLING_LOCATION", "userLogin", userLogin);
                        Map<String, Object> createBillToContactMechResult = dispatcher.runSync("createInvoiceContactMech",
                                createBillToContactMechContext);
                        if (ServiceUtil.isError(createBillToContactMechResult)) {
                            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                                    "AccountingErrorCreatingInvoiceContactMechFromOrder", locale), null, null, createBillToContactMechResult);
                        }
                    }
                } else {
                    Debug.logWarning("No billing locations found for order [" + orderId + "] and none were created for Invoice [" + invoiceId
                            + "]", MODULE);
                }
            }

            // get a list of the payment method types
            //DEJ20050705 doesn't appear to be used: List paymentPreferences = orderHeader.getRelated("OrderPaymentPreference", null, null, false);

            // create the bill-from (or pay-to) contact mech as the primary PAYMENT_LOCATION of the party from the store
            GenericValue payToAddress = null;
            if ("PURCHASE_INVOICE".equals(invoiceType)) {
                // for purchase orders, the pay to address is the BILLING_LOCATION of the vendor
                GenericValue billFromVendor = orh.getPartyFromRole("BILL_FROM_VENDOR");
                if (billFromVendor != null) {
                    List<GenericValue> billingContactMechs = billFromVendor.getRelatedOne("Party", false).getRelated("PartyContactMechPurpose",
                            UtilMisc.toMap("contactMechPurposeTypeId", "BILLING_LOCATION"), null, false);
                    if (UtilValidate.isNotEmpty(billingContactMechs)) {
                        payToAddress = EntityUtil.getFirst(EntityUtil.filterByDate(billingContactMechs));
                    }
                }
            } else {
                // for sales orders, it is the payment address on file for the store
                payToAddress = PaymentWorker.getPaymentAddress(delegator, productStore.getString("payToPartyId"));
            }
            if (payToAddress != null) {
                Map<String, Object> createPayToContactMechContext = UtilMisc.toMap("invoiceId", invoiceId, "contactMechId", payToAddress.getString(
                        "contactMechId"),
                        "contactMechPurposeTypeId", "PAYMENT_LOCATION", "userLogin", userLogin);
                Map<String, Object> createPayToContactMechResult = dispatcher.runSync("createInvoiceContactMech", createPayToContactMechContext);
                if (ServiceUtil.isError(createPayToContactMechResult)) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                            "AccountingErrorCreatingInvoiceContactMechFromOrder", locale), null, null, createPayToContactMechResult);
                }
            }

            // sequence for items - all OrderItems or InventoryReservations + all Adjustments
            int invoiceItemSeqNum = 1;
            String invoiceItemSeqId = UtilFormatOut.formatPaddedNumber(invoiceItemSeqNum, INVOICE_ITEM_SEQUENCE_ID_DIGITS);

            // create the item records
            for (GenericValue currentValue : billItems) {
                GenericValue itemIssuance = null;
                GenericValue orderItem = null;
                GenericValue shipmentReceipt = null;
                if ("ItemIssuance".equals(currentValue.getEntityName())) {
                    itemIssuance = currentValue;
                } else if ("OrderItem".equals(currentValue.getEntityName())) {
                    orderItem = currentValue;
                } else if ("ShipmentReceipt".equals(currentValue.getEntityName())) {
                    shipmentReceipt = currentValue;
                } else {
                    Debug.logError("Unexpected entity " + currentValue + " of type " + currentValue.getEntityName(), MODULE);
                }

                if (orderItem == null && itemIssuance != null) {
                    orderItem = itemIssuance.getRelatedOne("OrderItem", false);
                } else if ((orderItem == null) && (shipmentReceipt != null)) {
                    orderItem = shipmentReceipt.getRelatedOne("OrderItem", false);
                }

                if (orderItem == null) {
                    Debug.logError("Cannot create invoice when orderItem, itemIssuance, and shipmentReceipt are all null", MODULE);
                    return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                            "AccountingIllegalValuesPassedToCreateInvoiceService", locale));
                }

                GenericValue product = null;
                if (orderItem.get("productId") != null) {
                    product = orderItem.getRelatedOne("Product", false);
                }

                // get some quantities
                BigDecimal billingQuantity = null;
                if (itemIssuance != null) {
                    billingQuantity = itemIssuance.getBigDecimal("quantity");
                    BigDecimal cancelQty = itemIssuance.getBigDecimal("cancelQuantity");
                    if (cancelQty == null) {
                        cancelQty = BigDecimal.ZERO;
                    }
                    billingQuantity = billingQuantity.subtract(cancelQty).setScale(DECIMALS, ROUNDING);
                } else if (shipmentReceipt != null) {
                    billingQuantity = shipmentReceipt.getBigDecimal("quantityAccepted");
                } else {
                    BigDecimal orderedQuantity = OrderReadHelper.getOrderItemQuantity(orderItem);
                    BigDecimal invoicedQuantity = OrderReadHelper.getOrderItemInvoicedQuantity(orderItem);
                    billingQuantity = orderedQuantity.subtract(invoicedQuantity);
                    if (billingQuantity.compareTo(BigDecimal.ZERO) < 0) {
                        billingQuantity = BigDecimal.ZERO;
                    }
                }
                if (billingQuantity == null) {
                    billingQuantity = BigDecimal.ZERO;
                }

                // check if shipping applies to this item.  Shipping is calculated for sales invoices, not purchase invoices.
                boolean shippingApplies = false;
                if ((product != null) && (ProductWorker.shippingApplies(product)) && ("SALES_INVOICE".equals(invoiceType))) {
                    shippingApplies = true;
                }

                BigDecimal billingAmount = BigDecimal.ZERO;
                GenericValue orderAdj = EntityUtil.getFirst(orderItem.getRelated("OrderAdjustment", UtilMisc.toMap("orderAdjustmentTypeId",
                        "VAT_TAX"), null, false));
                /* Apply formula to get actual product price to set amount in invoice item
                    Formula is: productPrice = (productPriceWithTax.multiply(100)) / (orderAdj sourcePercentage + 100))
                    product price = (43*100) / (20+100) = 35.83 (Here product price is 43 with VAT)
                 */
                if (UtilValidate.isNotEmpty(orderAdj) && (orderAdj.getBigDecimal("amount").signum() == 0)
                        && UtilValidate.isNotEmpty(orderAdj.getBigDecimal("amountAlreadyIncluded"))
                        && orderAdj.getBigDecimal("amountAlreadyIncluded").signum() != 0) {
                    BigDecimal sourcePercentageTotal = orderAdj.getBigDecimal("sourcePercentage").add(new BigDecimal(100));
                    billingAmount =
                            orderItem.getBigDecimal("unitPrice").divide(sourcePercentageTotal, 100, ROUNDING)
                                    .multiply(new BigDecimal(100)).setScale(invoiceTypeDecimals, ROUNDING);
                } else {
                    billingAmount = orderItem.getBigDecimal("unitPrice").setScale(invoiceTypeDecimals, ROUNDING);
                }

                Map<String, Object> createInvoiceItemContext = new HashMap<>();
                createInvoiceItemContext.put("invoiceId", invoiceId);
                createInvoiceItemContext.put("invoiceItemSeqId", invoiceItemSeqId);
                createInvoiceItemContext.put("invoiceItemTypeId", getInvoiceItemType(delegator, (orderItem.getString("orderItemTypeId")),
                        (product == null ? null : product.getString("productTypeId")), invoiceType, "INV_FPROD_ITEM"));
                createInvoiceItemContext.put("description", orderItem.get("itemDescription"));
                createInvoiceItemContext.put("quantity", billingQuantity);
                createInvoiceItemContext.put("amount", billingAmount);
                createInvoiceItemContext.put("productId", orderItem.get("productId"));
                createInvoiceItemContext.put("productFeatureId", orderItem.get("productFeatureId"));
                createInvoiceItemContext.put("overrideGlAccountId", orderItem.get("overrideGlAccountId"));
                createInvoiceItemContext.put("userLogin", userLogin);

                String itemIssuanceId = null;
                if (itemIssuance != null && itemIssuance.get("inventoryItemId") != null) {
                    itemIssuanceId = itemIssuance.getString("itemIssuanceId");
                    createInvoiceItemContext.put("inventoryItemId", itemIssuance.get("inventoryItemId"));
                }
                // similarly, tax only for purchase invoices
                if ((product != null) && ("SALES_INVOICE".equals(invoiceType))) {
                    createInvoiceItemContext.put("taxableFlag", product.get("taxable"));
                }

                Map<String, Object> createInvoiceItemResult = dispatcher.runSync("createInvoiceItem", createInvoiceItemContext);
                if (ServiceUtil.isError(createInvoiceItemResult)) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                            "AccountingErrorCreatingInvoiceItemFromOrder", locale), null, null, createInvoiceItemResult);
                }

                // this item total
                BigDecimal thisAmount = billingAmount.multiply(billingQuantity).setScale(invoiceTypeDecimals, ROUNDING);

                // add to the ship amount only if it applies to this item
                if (shippingApplies) {
                    invoiceShipProRateAmount = invoiceShipProRateAmount.add(thisAmount).setScale(invoiceTypeDecimals, ROUNDING);
                    invoiceShippableQuantity = invoiceQuantity.add(billingQuantity).setScale(invoiceTypeDecimals, ROUNDING);
                }

                // increment the invoice subtotal
                invoiceSubTotal = invoiceSubTotal.add(thisAmount).setScale(100, ROUNDING);

                // increment the invoice quantity
                invoiceQuantity = invoiceQuantity.add(billingQuantity).setScale(invoiceTypeDecimals, ROUNDING);

                // create the OrderItemBilling record
                Map<String, Object> createOrderItemBillingContext = new HashMap<>();
                createOrderItemBillingContext.put("invoiceId", invoiceId);
                createOrderItemBillingContext.put("invoiceItemSeqId", invoiceItemSeqId);
                createOrderItemBillingContext.put("orderId", orderItem.get("orderId"));
                createOrderItemBillingContext.put("orderItemSeqId", orderItem.get("orderItemSeqId"));
                createOrderItemBillingContext.put("itemIssuanceId", itemIssuanceId);
                createOrderItemBillingContext.put("quantity", billingQuantity);
                createOrderItemBillingContext.put("amount", billingAmount);
                createOrderItemBillingContext.put("userLogin", userLogin);
                if ((shipmentReceipt != null) && (shipmentReceipt.getString("receiptId") != null)) {
                    createOrderItemBillingContext.put("shipmentReceiptId", shipmentReceipt.getString("receiptId"));
                }

                Map<String, Object> createOrderItemBillingResult = dispatcher.runSync("createOrderItemBilling", createOrderItemBillingContext);
                if (ServiceUtil.isError(createOrderItemBillingResult)) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                            "AccountingErrorCreatingOrderItemBillingFromOrder", locale), null, null, createOrderItemBillingResult);
                }

                if ("ItemIssuance".equals(currentValue.getEntityName())) {
                    /* Find ShipmentItemBilling based on shipmentId, shipmentItemSeqId, invoiceId, invoiceItemSeqId as
                       because if any order item has multiple quantity and reserved by multiple inventories then there will be multiple invoice items.
                       In that case ShipmentItemBilling was creating only for one invoice item. Fixed under OFBIZ-6806.
                    */
                    List<GenericValue> shipmentItemBillings = EntityQuery.use(delegator).from("ShipmentItemBilling")
                            .where("shipmentId", currentValue.get("shipmentId"), "shipmentItemSeqId", currentValue.get("shipmentItemSeqId"),
                                    "invoiceId", invoiceId, "invoiceItemSeqId", invoiceItemSeqId)
                            .queryList();
                    if (UtilValidate.isEmpty(shipmentItemBillings)) {

                        // create the ShipmentItemBilling record
                        Map<String, Object> shipmentItemBillingCtx = new HashMap<>();
                        shipmentItemBillingCtx.put("invoiceId", invoiceId);
                        shipmentItemBillingCtx.put("invoiceItemSeqId", invoiceItemSeqId);
                        shipmentItemBillingCtx.put("shipmentId", currentValue.get("shipmentId"));
                        shipmentItemBillingCtx.put("shipmentItemSeqId", currentValue.get("shipmentItemSeqId"));
                        shipmentItemBillingCtx.put("userLogin", userLogin);
                        Map<String, Object> result = dispatcher.runSync("createShipmentItemBilling", shipmentItemBillingCtx);
                        if (ServiceUtil.isError(result)) {
                            return ServiceUtil.returnError(ServiceUtil.getErrorMessage(result));
                        }
                    }
                }

                String parentInvoiceItemSeqId = invoiceItemSeqId;
                // increment the counter
                invoiceItemSeqNum++;
                invoiceItemSeqId = UtilFormatOut.formatPaddedNumber(invoiceItemSeqNum, INVOICE_ITEM_SEQUENCE_ID_DIGITS);

                // Get the original order item from the DB, in case the quantity has been overridden
                GenericValue originalOrderItem = EntityQuery.use(delegator).from("OrderItem").where("orderId", orderId, "orderItemSeqId",
                        orderItem.get("orderItemSeqId")).queryOne();

                // create the item adjustment as line items
                List<GenericValue> itemAdjustments = OrderReadHelper.getOrderItemAdjustmentList(orderItem, orh.getAdjustments());
                for (GenericValue adj : itemAdjustments) {

                    // Check against OrderAdjustmentBilling to see how much of this adjustment has already been invoiced
                    BigDecimal adjAlreadyInvoicedAmount = null;
                    try {
                        Map<String, Object> checkResult = dispatcher.runSync("calculateInvoicedAdjustmentTotal", UtilMisc.toMap("orderAdjustment",
                                adj));
                        if (ServiceUtil.isError(checkResult)) {
                            Debug.logError("Accounting trouble calling calculateInvoicedAdjustmentTotal service", MODULE);
                            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                                    "AccountingTroubleCallingCalculateInvoicedAdjustmentTotalService", locale));
                        }
                        adjAlreadyInvoicedAmount = (BigDecimal) checkResult.get("invoicedTotal");
                    } catch (GenericServiceException e) {
                        Debug.logError(e, "Accounting trouble calling calculateInvoicedAdjustmentTotal service", MODULE);
                        return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                                "AccountingTroubleCallingCalculateInvoicedAdjustmentTotalService", locale));
                    }

                    // Set adjustment amount as amountAlreadyIncluded to continue invoice item creation process
                    boolean isTaxIncludedInPrice = "VAT_TAX".equals(adj.getString("orderAdjustmentTypeId"))
                                    && UtilValidate.isNotEmpty(adj.getBigDecimal("amountAlreadyIncluded"))
                                    && adj.getBigDecimal("amountAlreadyIncluded").signum() != 0;
                    if (isTaxIncludedInPrice && (adj.getBigDecimal("amount").signum() == 0)) {
                        adj.set("amount", adj.getBigDecimal("amountAlreadyIncluded"));
                    }
                    // If the absolute invoiced amount >= the abs of the adjustment amount, the full amount has already been invoiced, so skip this
                    // adjustment
                    if (isTaxIncludedInPrice && adjAlreadyInvoicedAmount.abs().compareTo(adj.getBigDecimal("amount").setScale(invoiceTypeDecimals,
                            ROUNDING).abs()) > 0) {
                        continue;
                    }

                    BigDecimal originalOrderItemQuantity = OrderReadHelper.getOrderItemQuantity(originalOrderItem);
                    BigDecimal amount = BigDecimal.ZERO;
                    if (originalOrderItemQuantity.signum() != 0) {
                        if (adj.get("amount") != null) {
                            if ("PROMOTION_ADJUSTMENT".equals(adj.getString("orderAdjustmentTypeId")) && adj.get("productPromoId") != null) {
                                    /* Find negative amountAlreadyIncluded in OrderAdjustment to subtract it from discounted amount.
                                                                          As we stored negative sales tax amount in order adjustment for discounted
                                                                           item.
                                     */
                                List<EntityExpr> exprs = UtilMisc.toList(EntityCondition.makeCondition("orderId", EntityOperator.EQUALS,
                                        orderItem.getString("orderId")),
                                        EntityCondition.makeCondition("orderItemSeqId", EntityOperator.EQUALS, orderItem.getString("orderItemSeqId")),
                                        EntityCondition.makeCondition("orderAdjustmentTypeId", EntityOperator.EQUALS, "VAT_TAX"),
                                        EntityCondition.makeCondition("amountAlreadyIncluded", EntityOperator.LESS_THAN, BigDecimal.ZERO));
                                EntityCondition andCondition = EntityCondition.makeCondition(exprs, EntityOperator.AND);
                                GenericValue orderAdjustment = EntityUtil.getFirst(delegator.findList("OrderAdjustment", andCondition, null, null,
                                        null, false));
                                if (UtilValidate.isNotEmpty(orderAdjustment)) {
                                    amount =
                                            adj.getBigDecimal("amount").subtract(orderAdjustment.getBigDecimal("amountAlreadyIncluded")).setScale(100,
                                                    ROUNDING);
                                } else {
                                    amount = adj.getBigDecimal("amount");
                                }
                            } else {
                                // pro-rate the amount
                                // set decimals = 100 means we don't round this intermediate value, which is very important
                                if (isTaxIncludedInPrice) {
                                    BigDecimal priceWithTax = originalOrderItem.getBigDecimal("unitPrice");
                                    // Get tax included in item price
                                    amount = priceWithTax.subtract(billingAmount);
                                    amount = amount.multiply(billingQuantity);
                                    // get adjustment amount
                                        /* Get tax amount of other invoice and calculate remaining amount need to store in invoice item(Handle case
                                         of of partial shipment and promotional item)
                                                                              to adjust tax amount in invoice item.
                                         */
                                    BigDecimal otherInvoiceTaxAmount = BigDecimal.ZERO;
                                    GenericValue orderAdjBilling = EntityQuery.use(delegator).from("OrderAdjustmentBilling").where(
                                            "orderAdjustmentId", adj.getString("orderAdjustmentId")).queryFirst();
                                    if (UtilValidate.isNotEmpty(orderAdjBilling)) {
                                        //FIXME: Need to check here isTaxIncludedInPrice pass to use cache
                                        List<GenericValue> invoiceItems = EntityQuery.use(delegator).from("InvoiceItem").where("invoiceId",
                                                orderAdjBilling.getString("invoiceId"), "invoiceItemTypeId", "ITM_SALES_TAX", "productId",
                                                originalOrderItem.getString("productId")).cache(isTaxIncludedInPrice).queryList();
                                        for (GenericValue invoiceItem : invoiceItems) {
                                            otherInvoiceTaxAmount = otherInvoiceTaxAmount.add(invoiceItem.getBigDecimal("amount"));
                                        }
                                        if (otherInvoiceTaxAmount.compareTo(BigDecimal.ZERO) > 0) {
                                            BigDecimal remainingAmount = adj.getBigDecimal("amountAlreadyIncluded").subtract(otherInvoiceTaxAmount);
                                            amount = amount.min(remainingAmount);
                                        }
                                    }
                                    amount = amount.min(adj.getBigDecimal("amountAlreadyIncluded")).setScale(100, ROUNDING);
                                } else {
                                    amount = adj.getBigDecimal("amount").divide(originalOrderItemQuantity, 100, ROUNDING);
                                    amount = amount.multiply(billingQuantity);
                                }
                            }
                            // Tax needs to be rounded differently from other order adjustments
                            if ("SALES_TAX".equals(adj.getString("orderAdjustmentTypeId"))) {
                                amount = amount.setScale(TAX_DECIMALS, TAX_ROUNDING);
                            } else {
                                amount = amount.setScale(invoiceTypeDecimals, ROUNDING);
                            }
                        } else if (adj.get("sourcePercentage") != null) {
                            // pro-rate the amount
                            // set decimals = 100 means we don't round this intermediate value, which is very important
                            BigDecimal percent = adj.getBigDecimal("sourcePercentage");
                            percent = percent.divide(new BigDecimal(100), 100, ROUNDING);
                            amount = billingAmount.multiply(percent);
                            amount = amount.divide(originalOrderItemQuantity, 100, ROUNDING);
                            amount = amount.multiply(billingQuantity);
                            amount = amount.setScale(invoiceTypeDecimals, ROUNDING);
                        }
                    }
                    if (amount.signum() != 0) {
                        Map<String, Object> createInvoiceItemAdjContext = new HashMap<>();
                        createInvoiceItemAdjContext.put("invoiceId", invoiceId);
                        createInvoiceItemAdjContext.put("invoiceItemSeqId", invoiceItemSeqId);
                        createInvoiceItemAdjContext.put("invoiceItemTypeId", getInvoiceItemType(delegator, adj.getString("orderAdjustmentTypeId"),
                                null, invoiceType, "INVOICE_ITM_ADJ"));
                        createInvoiceItemAdjContext.put("quantity", BigDecimal.ONE);
                        createInvoiceItemAdjContext.put("amount", amount);
                        createInvoiceItemAdjContext.put("productId", orderItem.get("productId"));
                        createInvoiceItemAdjContext.put("productFeatureId", orderItem.get("productFeatureId"));
                        createInvoiceItemAdjContext.put("overrideGlAccountId", adj.get("overrideGlAccountId"));
                        createInvoiceItemAdjContext.put("parentInvoiceId", invoiceId);
                        createInvoiceItemAdjContext.put("parentInvoiceItemSeqId", parentInvoiceItemSeqId);
                        createInvoiceItemAdjContext.put("userLogin", userLogin);
                        createInvoiceItemAdjContext.put("taxAuthPartyId", adj.get("taxAuthPartyId"));
                        createInvoiceItemAdjContext.put("taxAuthGeoId", adj.get("taxAuthGeoId"));
                        createInvoiceItemAdjContext.put("taxAuthorityRateSeqId", adj.get("taxAuthorityRateSeqId"));

                        // some adjustments fill out the comments field instead
                        String description = (UtilValidate.isEmpty(adj.getString("description")) ? adj.getString("comments") : adj.getString(
                                "description"));
                        createInvoiceItemAdjContext.put("description", description);

                        // invoice items for sales tax are not taxable themselves
                        // TODO: This is not an ideal solution. Instead, we need to use OrderAdjustment.includeInTax when it is implemented
                        if (!("SALES_TAX".equals(adj.getString("orderAdjustmentTypeId")))) {
                            createInvoiceItemAdjContext.put("taxableFlag", product.get("taxable"));
                        }

                        // If the OrderAdjustment is associated to a ProductPromo,
                        // and the field ProductPromo.overrideOrgPartyId is set,
                        // copy the value to InvoiceItem.overrideOrgPartyId: this
                        // represent an organization override for the payToPartyId
                        if (UtilValidate.isNotEmpty(adj.getString("productPromoId"))) {
                            try {
                                GenericValue productPromo = adj.getRelatedOne("ProductPromo", false);
                                if (UtilValidate.isNotEmpty(productPromo.getString("overrideOrgPartyId"))) {
                                    createInvoiceItemAdjContext.put("overrideOrgPartyId", productPromo.getString("overrideOrgPartyId"));
                                }
                            } catch (GenericEntityException e) {
                                Debug.logError(e, "Error looking up ProductPromo with id [" + adj.getString("productPromoId") + "]", MODULE);
                            }
                        }

                        Map<String, Object> createInvoiceItemAdjResult = dispatcher.runSync("createInvoiceItem", createInvoiceItemAdjContext);
                        if (ServiceUtil.isError(createInvoiceItemAdjResult)) {
                            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                                    "AccountingErrorCreatingInvoiceItemFromOrder", locale), null, null, createInvoiceItemAdjResult);
                        }

                        // Create the OrderAdjustmentBilling record
                        Map<String, Object> createOrderAdjustmentBillingContext = new HashMap<>();
                        createOrderAdjustmentBillingContext.put("orderAdjustmentId", adj.getString("orderAdjustmentId"));
                        createOrderAdjustmentBillingContext.put("invoiceId", invoiceId);
                        createOrderAdjustmentBillingContext.put("invoiceItemSeqId", invoiceItemSeqId);
                        createOrderAdjustmentBillingContext.put("amount", amount);
                        createOrderAdjustmentBillingContext.put("userLogin", userLogin);

                        Map<String, Object> createOrderAdjustmentBillingResult = dispatcher.runSync("createOrderAdjustmentBilling",
                                createOrderAdjustmentBillingContext);
                        if (ServiceUtil.isError(createOrderAdjustmentBillingResult)) {
                            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                                    "AccountingErrorCreatingOrderAdjustmentBillingFromOrder", locale), null, null,
                                    createOrderAdjustmentBillingContext);
                        }

                        // this adjustment amount
                        BigDecimal thisAdjAmount = amount;

                        // adjustments only apply to totals when they are not tax or shipping adjustments
                        if (!"SALES_TAX".equals(adj.getString("orderAdjustmentTypeId"))
                                && !"SHIPPING_ADJUSTMENT".equals(adj.getString("orderAdjustmentTypeId"))) {
                            // increment the invoice subtotal
                            invoiceSubTotal = invoiceSubTotal.add(thisAdjAmount).setScale(100, ROUNDING);

                            // add to the ship amount only if it applies to this item
                            if (shippingApplies) {
                                invoiceShipProRateAmount = invoiceShipProRateAmount.add(thisAdjAmount).setScale(invoiceTypeDecimals, ROUNDING);
                            }
                        }

                        // increment the counter
                        invoiceItemSeqNum++;
                        invoiceItemSeqId = UtilFormatOut.formatPaddedNumber(invoiceItemSeqNum, INVOICE_ITEM_SEQUENCE_ID_DIGITS);
                    }
                }
            }

            // create header adjustments as line items -- always to tax/shipping last
            Map<GenericValue, BigDecimal> shipAdjustments = new HashMap<>();
            Map<GenericValue, BigDecimal> taxAdjustments = new HashMap<>();

            List<GenericValue> headerAdjustments = orh.getOrderHeaderAdjustments();
            for (GenericValue adj : headerAdjustments) {

                // Check against OrderAdjustmentBilling to see how much of this adjustment has already been invoiced
                BigDecimal adjAlreadyInvoicedAmount = null;
                try {
                    Map<String, Object> checkResult = dispatcher.runSync("calculateInvoicedAdjustmentTotal", UtilMisc.toMap("orderAdjustment", adj));
                    if (ServiceUtil.isError(checkResult)) {
                        return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                                "AccountingTroubleCallingCalculateInvoicedAdjustmentTotalService", locale));
                    }
                    adjAlreadyInvoicedAmount = ((BigDecimal) checkResult.get("invoicedTotal")).setScale(invoiceTypeDecimals, ROUNDING);
                } catch (GenericServiceException e) {
                    Debug.logError(e, "Accounting trouble calling calculateInvoicedAdjustmentTotal service", MODULE);
                    return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                            "AccountingTroubleCallingCalculateInvoicedAdjustmentTotalService", locale));
                }

                // If the absolute invoiced amount >= the abs of the adjustment amount, the full amount has already been invoiced, so skip this
                // adjustment
                if (adjAlreadyInvoicedAmount.abs().compareTo(adj.getBigDecimal("amount").setScale(invoiceTypeDecimals, ROUNDING).abs()) >= 0) {
                    continue;
                }

                if ("SHIPPING_CHARGES".equals(adj.getString("orderAdjustmentTypeId"))) {
                    shipAdjustments.put(adj, adjAlreadyInvoicedAmount);
                } else if ("SALES_TAX".equals(adj.getString("orderAdjustmentTypeId"))) {
                    taxAdjustments.put(adj, adjAlreadyInvoicedAmount);
                } else {
                    // these will effect the shipping pro-rate (unless commented)
                    // other adjustment type
                    BigDecimal divisor = orderSubTotal;
                    BigDecimal multiplier = invoiceSubTotal;
                    if (BigDecimal.ZERO.compareTo(multiplier) == 0 && BigDecimal.ZERO.compareTo(divisor) == 0) {
                        // if multiplier and divisor are equal to zero then use the quantities instead of the amounts
                        // this is useful when the order has free items and misc charges
                        divisor = orderQuantity;
                        multiplier = invoiceQuantity;
                    }

                    calcHeaderAdj(delegator, adj, invoiceType, invoiceId, invoiceItemSeqId, divisor, multiplier,
                            adj.getBigDecimal("amount").setScale(invoiceTypeDecimals, ROUNDING), invoiceTypeDecimals, ROUNDING, userLogin,
                            dispatcher, locale);
                    // invoiceShipProRateAmount += adjAmount;
                    // do adjustments compound or are they based off subtotal? Here we will (unless commented)
                    // invoiceSubTotal += adjAmount;

                    // increment the counter
                    invoiceItemSeqNum++;
                    invoiceItemSeqId = UtilFormatOut.formatPaddedNumber(invoiceItemSeqNum, INVOICE_ITEM_SEQUENCE_ID_DIGITS);
                }
            }

            // next do the shipping adjustments.  Note that we do not want to add these to the invoiceSubTotal or orderSubTotal for pro-rating tax
            // later, as that would cause
            // numerator/denominator problems when the shipping is not pro-rated but rather charged all on the first invoice
            for (Map.Entry<GenericValue, BigDecimal> set : shipAdjustments.entrySet()) {
                BigDecimal adjAlreadyInvoicedAmount = set.getValue();
                GenericValue adj = set.getKey();

                if ("N".equalsIgnoreCase(prorateShipping)) {

                    // Set the divisor and multiplier to 1 to avoid prorating
                    BigDecimal divisor = BigDecimal.ONE;
                    BigDecimal multiplier = BigDecimal.ONE;

                    // The base amount in this case is the adjustment amount minus the total already invoiced for that adjustment, since
                    //  it won't be prorated
                    BigDecimal baseAmount = adj.getBigDecimal("amount").setScale(invoiceTypeDecimals, ROUNDING).subtract(adjAlreadyInvoicedAmount);
                    calcHeaderAdj(delegator, adj, invoiceType, invoiceId, invoiceItemSeqId, divisor, multiplier, baseAmount,
                            invoiceTypeDecimals, ROUNDING, userLogin, dispatcher, locale);
                } else {

                    // Pro-rate the shipping amount based on shippable information
                    BigDecimal divisor = shippableAmount;
                    BigDecimal multiplier = invoiceShipProRateAmount;
                    if (BigDecimal.ZERO.compareTo(multiplier) == 0 && BigDecimal.ZERO.compareTo(divisor) == 0) {
                        // if multiplier and divisor are equal to zero then use the quantities instead of the amounts
                        // this is useful when the order has free items and shipping charges
                        divisor = shippableQuantity;
                        multiplier = invoiceShippableQuantity;
                    }

                    // The base amount in this case is the adjustment amount, since we want to prorate based on the full amount
                    BigDecimal baseAmount = adj.getBigDecimal("amount").setScale(invoiceTypeDecimals, ROUNDING);
                    calcHeaderAdj(delegator, adj, invoiceType, invoiceId, invoiceItemSeqId, divisor, multiplier,
                            baseAmount, invoiceTypeDecimals, ROUNDING, userLogin, dispatcher, locale);
                }

                // Increment the counter
                invoiceItemSeqNum++;
                invoiceItemSeqId = UtilFormatOut.formatPaddedNumber(invoiceItemSeqNum, INVOICE_ITEM_SEQUENCE_ID_DIGITS);
            }

            // last do the tax adjustments
            String prorateTaxes = productStore != null ? productStore.getString("prorateTaxes") : "Y";
            if (prorateTaxes == null) {
                prorateTaxes = "Y";
            }
            for (Map.Entry<GenericValue, BigDecimal> entry : taxAdjustments.entrySet()) {
                GenericValue adj = entry.getKey();
                BigDecimal adjAlreadyInvoicedAmount = entry.getValue();
                BigDecimal adjAmount = null;

                if ("N".equalsIgnoreCase(prorateTaxes)) {

                    // Set the divisor and multiplier to 1 to avoid prorating
                    BigDecimal divisor = BigDecimal.ONE;
                    BigDecimal multiplier = BigDecimal.ONE;

                    // The base amount in this case is the adjustment amount minus the total already invoiced for that adjustment, since
                    //  it won't be prorated
                    BigDecimal baseAmount = adj.getBigDecimal("amount").setScale(TAX_DECIMALS, TAX_ROUNDING).subtract(adjAlreadyInvoicedAmount);
                    adjAmount = calcHeaderAdj(delegator, adj, invoiceType, invoiceId, invoiceItemSeqId,
                            divisor, multiplier, baseAmount, TAX_DECIMALS, TAX_ROUNDING, userLogin, dispatcher, locale);
                } else {

                    // Pro-rate the tax amount based on shippable information
                    BigDecimal divisor = orderSubTotal;
                    BigDecimal multiplier = invoiceSubTotal;

                    // The base amount in this case is the adjustment amount, since we want to prorate based on the full amount
                    BigDecimal baseAmount = adj.getBigDecimal("amount");
                    adjAmount = calcHeaderAdj(delegator, adj, invoiceType, invoiceId, invoiceItemSeqId,
                            divisor, multiplier, baseAmount, TAX_DECIMALS, TAX_ROUNDING, userLogin, dispatcher, locale);
                }
                invoiceSubTotal = invoiceSubTotal.add(adjAmount).setScale(invoiceTypeDecimals, ROUNDING);

                // Increment the counter
                invoiceItemSeqNum++;
                invoiceItemSeqId = UtilFormatOut.formatPaddedNumber(invoiceItemSeqNum, INVOICE_ITEM_SEQUENCE_ID_DIGITS);
            }

            // check for previous order payments
            List<GenericValue> orderPaymentPrefs = EntityQuery.use(delegator).from("OrderPaymentPreference")
                    .where(EntityCondition.makeCondition("orderId", EntityOperator.EQUALS, orderId),
                            EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "PAYMENT_CANCELLED")).queryList();
            List<GenericValue> currentPayments = new LinkedList<>();
            for (GenericValue paymentPref : orderPaymentPrefs) {
                List<GenericValue> payments = paymentPref.getRelated("Payment", null, null, false);
                currentPayments.addAll(payments);
            }
            // apply these payments to the invoice if they have any remaining amount to apply
            for (GenericValue payment : currentPayments) {
                if ("PMNT_VOID".equals(payment.getString("statusId")) || "PMNT_CANCELLED".equals(payment.getString("statusId"))) {
                    continue;
                }
                BigDecimal notApplied = PaymentWorker.getPaymentNotApplied(payment);
                if (notApplied.signum() > 0) {
                    Map<String, Object> appl = new HashMap<>();
                    appl.put("paymentId", payment.get("paymentId"));
                    appl.put("invoiceId", invoiceId);
                    appl.put("billingAccountId", billingAccountId);
                    appl.put("amountApplied", notApplied);
                    appl.put("userLogin", userLogin);
                    Map<String, Object> createPayApplResult = dispatcher.runSync("createPaymentApplication", appl);
                    if (ServiceUtil.isError(createPayApplResult)) {
                        return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                                "AccountingErrorCreatingInvoiceFromOrder", locale), null, null, createPayApplResult);
                    }
                }
            }

            // Should all be in place now. Depending on the ProductStore.autoApproveInvoice setting, set status to INVOICE_READY (unless it's a
            // purchase invoice, which we set to INVOICE_IN_PROCESS)
            String autoApproveInvoice = productStore != null ? productStore.getString("autoApproveInvoice") : "Y";
            if (!"N".equals(autoApproveInvoice)) {
                String nextStatusId = "PURCHASE_INVOICE".equals(invoiceType) ? "INVOICE_IN_PROCESS" : "INVOICE_READY";
                Map<String, Object> setInvoiceStatusResult = dispatcher.runSync("setInvoiceStatus", UtilMisc.<String, Object>toMap("invoiceId",
                        invoiceId, "statusId", nextStatusId, "userLogin", userLogin));
                if (ServiceUtil.isError(setInvoiceStatusResult)) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                            "AccountingErrorCreatingInvoiceFromOrder", locale), null, null, setInvoiceStatusResult);
                }
            }

            Map<String, Object> resp = ServiceUtil.returnSuccess();
            resp.put("invoiceId", invoiceId);
            resp.put("invoiceTypeId", invoiceType);
            return resp;
        } catch (GenericEntityException e) {
            Debug.logError(e, "Entity/data problem creating invoice from order items: " + e.toString(), MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    "AccountingEntityDataProblemCreatingInvoiceFromOrderItems",
                    UtilMisc.toMap("reason", e.toString()), locale));
        } catch (GenericServiceException e) {
            Debug.logError(e, "Service/other problem creating invoice from order items: " + e.toString(), MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    "AccountingServiceOtherProblemCreatingInvoiceFromOrderItems",
                    UtilMisc.toMap("reason", e.toString()), locale));
        }
    }

    // Service for creating commission invoices
    public static Map<String, Object> createCommissionInvoices(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = (Locale) context.get("locale");
        List<String> salesInvoiceIds = UtilGenerics.cast(context.get("invoiceIds"));
        List<Map<String, String>> invoicesCreated = new LinkedList<>();
        Map<String, List<Map<String, Object>>> commissionParties = new HashMap<>();
        for (String salesInvoiceId : salesInvoiceIds) {
            List<String> salesRepPartyIds = UtilGenerics.cast(context.get("partyIds"));
            BigDecimal amountTotal = InvoiceWorker.getInvoiceTotal(delegator, salesInvoiceId);
            if (amountTotal.signum() == 0) {
                Debug.logWarning("Invoice [" + salesInvoiceId + "] has an amount total of [" + amountTotal + "], so no commission invoice will be "
                        + "created", MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                        "AccountingInvoiceCommissionZeroInvoiceAmount", locale));
            }
            BigDecimal appliedFraction = amountTotal.divide(amountTotal, 12, ROUNDING);
            GenericValue invoice = null;
            boolean isReturn = false;
            List<String> billFromVendorInvoiceRoles;
            List<GenericValue> invoiceItems;
            try {
                List<EntityExpr> invoiceRoleConds = UtilMisc.toList(
                        EntityCondition.makeCondition("invoiceId", EntityOperator.EQUALS, salesInvoiceId),
                        EntityCondition.makeCondition("roleTypeId", EntityOperator.EQUALS, "BILL_FROM_VENDOR"));
                EntityQuery roleQuery = EntityQuery.use(delegator).select("partyId").from("InvoiceRole").where(invoiceRoleConds);
                billFromVendorInvoiceRoles = EntityUtil.getFieldListFromEntityList(roleQuery.queryList(), "partyId", true);

                invoiceRoleConds = UtilMisc.toList(
                        EntityCondition.makeCondition("invoiceId", EntityOperator.EQUALS, salesInvoiceId),
                        EntityCondition.makeCondition("roleTypeId", EntityOperator.EQUALS, "SALES_REP"));
                // if the receiving parties is empty then we will create commission invoices for all sales agent associated to sales invoice.
                if (UtilValidate.isEmpty(salesRepPartyIds)) {
                    salesRepPartyIds = EntityUtil.getFieldListFromEntityList(roleQuery.where(invoiceRoleConds).queryList(), "partyId", true);
                    if (UtilValidate.isEmpty(salesRepPartyIds)) {
                        return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                                "No party found with role sales representative for sales invoice " + salesInvoiceId, locale));
                    }
                } else {
                    List<String> salesInvoiceRolePartyIds = EntityUtil.getFieldListFromEntityList(roleQuery.where(invoiceRoleConds).queryList(),
                            "partyId", true);
                    if (UtilValidate.isNotEmpty(salesInvoiceRolePartyIds)) {
                        salesRepPartyIds = UtilGenerics.cast(CollectionUtils.intersection(salesRepPartyIds, salesInvoiceRolePartyIds));
                    }
                }
                invoice = EntityQuery.use(delegator).from("Invoice").where("invoiceId", salesInvoiceId).queryOne();
                String invoiceTypeId = invoice.getString("invoiceTypeId");
                if ("CUST_RTN_INVOICE".equals(invoiceTypeId)) {
                    isReturn = true;
                } else if (!"SALES_INVOICE".equals(invoiceTypeId)) {
                    Debug.logWarning("This type of invoice has no commission; returning success", MODULE);
                    return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                            "AccountingInvoiceCommissionInvalid", locale));
                }
                invoiceItems = EntityQuery.use(delegator).from("InvoiceItem").where("invoiceId", salesInvoiceId).queryList();
            } catch (GenericEntityException e) {
                return ServiceUtil.returnError(e.getMessage());
            }
            // Map of commission Lists (of Maps) for each party.
            // Determine commissions for various parties.
            for (GenericValue invoiceItem : invoiceItems) {
                BigDecimal amount = BigDecimal.ZERO;
                BigDecimal quantity = invoiceItem.getBigDecimal("quantity");
                amount = invoiceItem.getBigDecimal("amount");
                amount = isReturn ? amount.negate() : amount;
                String productId = invoiceItem.getString("productId");
                String invoiceItemSeqId = invoiceItem.getString("invoiceItemSeqId");
                String invoiceId = invoiceItem.getString("invoiceId");
                // Determine commission parties for this invoiceItem
                if (UtilValidate.isNotEmpty(productId)) {
                    Map<String, Object> resultMap = null;
                    try {
                        resultMap = dispatcher.runSync("getCommissionForProduct", UtilMisc.<String, Object>toMap(
                                "productId", productId,
                                "invoiceId", invoiceId,
                                "invoiceItemSeqId", invoiceItemSeqId,
                                "invoiceItemTypeId", invoiceItem.getString("invoiceItemTypeId"),
                                "amount", amount,
                                "quantity", quantity,
                                "userLogin", userLogin));
                        if (ServiceUtil.isError(resultMap)) {
                            return ServiceUtil.returnError(ServiceUtil.getErrorMessage(resultMap));
                        }
                    } catch (GenericServiceException e) {
                        return ServiceUtil.returnError(e.getMessage());
                    }
                    // build a Map of partyIds (both to and from) in a commission and the amounts
                    // Note that getCommissionForProduct returns a List of Maps with a lot values.  See services.xml definition for reference.
                    List<Map<String, Object>> itemCommissions = UtilGenerics.cast(resultMap.get("commissions"));
                    if (UtilValidate.isNotEmpty(itemCommissions)) {
                        for (Map<String, Object> commissionMap : itemCommissions) {
                            commissionMap.put("invoice", invoice);
                            commissionMap.put("appliedFraction", appliedFraction);
                            if (!billFromVendorInvoiceRoles.contains(commissionMap.get("partyIdFrom"))
                                    || !salesRepPartyIds.contains(commissionMap.get("partyIdTo"))) {
                                continue;
                            }
                            String partyIdFromTo = (String) commissionMap.get("partyIdFrom") + (String) commissionMap.get("partyIdTo");
                            if (!commissionParties.containsKey(partyIdFromTo)) {
                                commissionParties.put(partyIdFromTo, UtilMisc.toList(commissionMap));
                            } else {
                                (commissionParties.get(partyIdFromTo)).add(commissionMap);
                            }
                        }
                    }
                }
            }
        }
        Timestamp now = UtilDateTime.nowTimestamp();
        // Create invoice for each commission receiving party
        for (Map.Entry<String, List<Map<String, Object>>> commissionParty : commissionParties.entrySet()) {
            List<GenericValue> toStore = new LinkedList<>();
            List<Map<String, Object>> commList = commissionParty.getValue();
            // get the billing parties
            if (UtilValidate.isEmpty(commList)) {
                continue;
            }
            // From and To are reversed between commission and invoice
            String partyIdBillTo = (String) (commList.get(0)).get("partyIdFrom");
            String partyIdBillFrom = (String) (commList.get(0)).get("partyIdTo");
            GenericValue invoice = (GenericValue) (commList.get(0)).get("invoice");
            BigDecimal appliedFraction = (BigDecimal) (commList.get(0)).get("appliedFraction");
            Long days = (Long) (commList.get(0)).get("days");
            // create the invoice record
            // To and From are in commission's sense, opposite for invoice
            Map<String, Object> createInvoiceMap = new HashMap<>();
            createInvoiceMap.put("partyId", partyIdBillTo);
            createInvoiceMap.put("partyIdFrom", partyIdBillFrom);
            createInvoiceMap.put("invoiceDate", now);
            // if there were days associated with the commission agreement, then set a dueDate for the invoice.
            if (days != null) {
                createInvoiceMap.put("dueDate", UtilDateTime.getDayEnd(now, days));
            }
            createInvoiceMap.put("invoiceTypeId", "COMMISSION_INVOICE");
            // start with INVOICE_IN_PROCESS, in the INVOICE_READY we can't change the invoice (or shouldn't be able to...)
            createInvoiceMap.put("statusId", "INVOICE_IN_PROCESS");
            createInvoiceMap.put("currencyUomId", invoice.getString("currencyUomId"));
            createInvoiceMap.put("userLogin", userLogin);
            // store the invoice first
            Map<String, Object> createInvoiceResult;
            try {
                createInvoiceResult = dispatcher.runSync("createInvoice", createInvoiceMap);
                if (ServiceUtil.isError(createInvoiceResult)) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                            "AccountingInvoiceCommissionError", locale), null, null, null);
                }
            } catch (GenericServiceException e) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                        "AccountingInvoiceCommissionError", locale), null, null, null);
            }
            String invoiceId = (String) createInvoiceResult.get("invoiceId");
            // create the bill-from (or pay-to) contact mech as the primary PAYMENT_LOCATION of the party from the store
            GenericValue partyContactMechPurpose = null;
            try {
                partyContactMechPurpose = EntityQuery.use(delegator).from("PartyContactMechPurpose")
                        .where("partyId", partyIdBillTo, "contactMechPurposeTypeId", "BILLING_LOCATION").queryFirst();
            } catch (GenericEntityException e) {
                return ServiceUtil.returnError(e.getMessage());
            }
            if (partyContactMechPurpose != null) {
                GenericValue invoiceContactMech = delegator.makeValue("InvoiceContactMech", UtilMisc.toMap(
                        "invoiceId", invoiceId,
                        "contactMechId", partyContactMechPurpose.getString("contactMechId"),
                        "contactMechPurposeTypeId", "BILLING_LOCATION"));
                toStore.add(invoiceContactMech);
            }
            try {
                partyContactMechPurpose = EntityQuery.use(delegator).from("PartyContactMechPurpose")
                        .where("partyId", partyIdBillTo, "contactMechPurposeTypeId", "PAYMENT_LOCATION").queryFirst();
            } catch (GenericEntityException e) {
                return ServiceUtil.returnError(e.getMessage());
            }
            if (partyContactMechPurpose != null) {
                GenericValue invoiceContactMech = delegator.makeValue("InvoiceContactMech", UtilMisc.toMap(
                        "invoiceId", invoiceId,
                        "contactMechId", partyContactMechPurpose.getString("contactMechId"),
                        "contactMechPurposeTypeId", "PAYMENT_LOCATION"));
                toStore.add(invoiceContactMech);
            }
            // create the item records
            for (Map<String, Object> commissionMap : commList) {
                BigDecimal elemAmount = ((BigDecimal) commissionMap.get("commission")).multiply(appliedFraction);
                BigDecimal quantity = (BigDecimal) commissionMap.get("quantity");
                String invoiceIdFrom = (String) commissionMap.get("invoiceId");
                String invoiceItemSeqIdFrom = (String) commissionMap.get("invoiceItemSeqId");
                elemAmount = elemAmount.setScale(DECIMALS, ROUNDING);
                Map<String, Object> resMap = null;
                try {
                    resMap = dispatcher.runSync("createInvoiceItem", UtilMisc.toMap("invoiceId", invoiceId,
                            "productId", commissionMap.get("productId"),
                            "invoiceItemTypeId", "COMM_INV_ITEM",
                            "quantity", quantity,
                            "amount", elemAmount,
                            "userLogin", userLogin));
                    if (ServiceUtil.isError(resMap)) {
                        return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                                "AccountingInvoiceCommissionErrorItem", locale), null, null, resMap);
                    }
                    resMap = dispatcher.runSync("createInvoiceItemAssoc", UtilMisc.toMap("invoiceIdFrom", invoiceIdFrom,
                            "invoiceItemSeqIdFrom", invoiceItemSeqIdFrom,
                            "invoiceIdTo", invoiceId,
                            "invoiceItemSeqIdTo", resMap.get("invoiceItemSeqId"),
                            "invoiceItemAssocTypeId", "COMMISSION_INVOICE",
                            "partyIdFrom", partyIdBillFrom,
                            "partyIdTo", partyIdBillTo,
                            "quantity", quantity,
                            "amount", elemAmount,
                            "userLogin", userLogin));
                    if (ServiceUtil.isError(resMap)) {
                        return ServiceUtil.returnError(ServiceUtil.getErrorMessage(resMap));
                    }
                } catch (GenericServiceException e) {
                    return ServiceUtil.returnError(e.getMessage());
                }
            }
            // store value objects
            try {
                delegator.storeAll(toStore);
            } catch (GenericEntityException e) {
                Debug.logError(e, "Entity/data problem creating commission invoice: " + e.toString(), MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                        "AccountingInvoiceCommissionEntityDataProblem",
                        UtilMisc.toMap("reason", e.toString()), locale));
            }
            invoicesCreated.add(UtilMisc.<String, String>toMap("commissionInvoiceId", invoiceId, "salesRepresentative ", partyIdBillFrom));
        }
        String invCreated = Integer.toString(invoicesCreated.size());
        Map<String, Object> result = ServiceUtil.returnSuccess(UtilProperties.getMessage(RESOURCE,
                "AccountingCommissionInvoicesCreated",
                UtilMisc.toMap("invoicesCreated", invCreated), locale));
        Debug.logInfo("Created Commission invoices for each commission receiving parties "
                + invCreated, MODULE);
        result.put("invoicesCreated", invoicesCreated);
        return result;
    }

    public static Map<String, Object> readyInvoices(DispatchContext dctx, Map<String, Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = (Locale) context.get("locale");
        // Get invoices to make ready
        List<String> invoicesCreated = UtilGenerics.cast(context.get("invoicesCreated"));
        String nextStatusId = "INVOICE_READY";
        try {
            for (String invoiceId : invoicesCreated) {
                Map<String, Object> setInvoiceStatusResult = dispatcher.runSync("setInvoiceStatus", UtilMisc.<String, Object>toMap("invoiceId",
                        invoiceId, "statusId", nextStatusId, "userLogin", userLogin));
                if (ServiceUtil.isError(setInvoiceStatusResult)) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                            "AccountingInvoiceCommissionError", locale), null, null, setInvoiceStatusResult);
                }
            }
        } catch (GenericServiceException e) {
            Debug.logError(e, "Entity/data problem creating commission invoice: " + e.toString(), MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    "AccountingInvoiceCommissionError",
                    UtilMisc.toMap("reason", e.toString()), locale));
        }
        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> createInvoicesFromShipment(DispatchContext dctx, Map<String, Object> context) {
        //Delegator delegator = dctx.getDelegator();
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        String shipmentId = (String) context.get("shipmentId");
        Locale locale = (Locale) context.get("locale");
        List<String> invoicesCreated;
        Map<String, Object> response = ServiceUtil.returnSuccess();
        GenericValue orderShipment = null;
        String invoicePerShipment = null;

        try {
            orderShipment = EntityQuery.use(delegator).from("OrderShipment").where("shipmentId", shipmentId).queryFirst();
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(e.getMessage());
        }

        if (orderShipment != null) {
            String orderId = orderShipment.getString("orderId");
            try {
                GenericValue orderHeader = EntityQuery.use(delegator).from("OrderHeader").where("orderId", orderId).queryOne();
                invoicePerShipment = orderHeader.getString("invoicePerShipment");
            } catch (GenericEntityException e) {
                return ServiceUtil.returnError(e.getMessage());
            }
        }

        // Either no orderShipment exists, or there's a null invoicePerShipment in the OrderHeader.
        // In either case, use the default value from the properties
        if (invoicePerShipment == null) {
            invoicePerShipment = EntityUtilProperties.getPropertyValue("accounting", "create.invoice.per.shipment", delegator);
        }

        if ("Y".equals(invoicePerShipment)) {
            Map<String, Object> serviceContext = UtilMisc.toMap("shipmentIds", UtilMisc.toList(shipmentId), "eventDate", context.get("eventDate"),
                    "userLogin", context.get("userLogin"));
            try {
                Map<String, Object> result = dispatcher.runSync("createInvoicesFromShipments", serviceContext);
                if (ServiceUtil.isError(result)) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                            "AccountingTroubleCallingCreateInvoicesFromShipmentService",
                            UtilMisc.toMap("shipmentId", shipmentId), locale));
                }
                invoicesCreated = UtilGenerics.cast(result.get("invoicesCreated"));
            } catch (GenericServiceException e) {
                Debug.logError(e, "Trouble calling createInvoicesFromShipment service; invoice not created for shipment [" + shipmentId + "]",
                        MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                        "AccountingTroubleCallingCreateInvoicesFromShipmentService",
                        UtilMisc.toMap("shipmentId", shipmentId), locale));
            }
            response.put("invoicesCreated", invoicesCreated);
        }
        return response;
    }

    public static Map<String, Object> setInvoicesToReadyFromShipment(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        String shipmentId = (String) context.get("shipmentId");
        Locale locale = (Locale) context.get("locale");
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        // 1. Find all the orders for this shipment
        // 2. For every order check the invoice
        // 2.a If the invoice is in In-Process status, then move its status to ready and capture the payment.
        // 2.b If the invoice is in status other then IN-Process, skip this. These would be already paid and captured.
        GenericValue shipment = null;
        try {
            shipment = EntityQuery.use(delegator).from("Shipment").where("shipmentId", shipmentId).queryOne();
        } catch (GenericEntityException e) {
            Debug.logError(e, "Trouble getting Shipment entity for shipment " + shipmentId, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    "AccountingTroubleGettingShipmentEntity",
                    UtilMisc.toMap("shipmentId", shipmentId), locale));
        }
        if (shipment == null) {
            Debug.logError(UtilProperties.getMessage(RESOURCE, "AccountingShipmentNotFound", locale), MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "AccountingShipmentNotFound", locale));
        }

        List<GenericValue> itemIssuances;
        try {
            itemIssuances = EntityQuery.use(delegator).select("orderId", "shipmentId")
                    .from("ItemIssuance").where("shipmentId", shipmentId).orderBy("orderId").distinct().queryList();
        } catch (GenericEntityException e) {
            Debug.logError(e, "Problem getting issued items from shipments", MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    "AccountingProblemGettingItemsFromShipments", locale));
        }
        if (itemIssuances.isEmpty()) {
            Debug.logInfo("No items issued for shipments", MODULE);
            return ServiceUtil.returnSuccess();
        }
        // The orders can now be placed in separate groups, each for
        // 1. The group of orders for which payment is already captured. No grouping and action required.
        // 2. The group of orders for which invoice is IN-Process status.
        Map<String, GenericValue> ordersWithInProcessInvoice = new HashMap<>();

        for (GenericValue itemIssuance : itemIssuances) {
            String orderId = itemIssuance.getString("orderId");
            Map<String, Object> billFields = new HashMap<>();
            billFields.put("orderId", orderId);

            GenericValue orderItemBilling = null;
            try {
                orderItemBilling = EntityQuery.use(delegator).from("OrderItemBilling").where(billFields).queryFirst();
            } catch (GenericEntityException e) {
                Debug.logError(e, "Problem looking up OrderItemBilling records for " + billFields, MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                        "AccountingProblemLookingUpOrderItemBilling",
                        UtilMisc.toMap("billFields", billFields), locale));
            }
            // if none found, the order does not have any invoice
            if (orderItemBilling != null) {
                // orders already have an invoice
                GenericValue invoice = null;
                try {
                    invoice = orderItemBilling.getRelatedOne("Invoice", false);
                } catch (GenericEntityException e) {
                    Debug.logError(e, MODULE);
                    return ServiceUtil.returnError(e.getMessage());
                }
                if (invoice != null) {
                    if ("INVOICE_IN_PROCESS".equals(invoice.getString("statusId"))) {
                        ordersWithInProcessInvoice.put(orderId, invoice);
                    }
                }
            }
        }

        // For In-Process invoice, move the status to ready and capture the payment
        for (GenericValue invoice : ordersWithInProcessInvoice.values()) {
            String invoiceId = invoice.getString("invoiceId");
            Map<String, Object> setInvoiceStatusResult;
            try {
                setInvoiceStatusResult = dispatcher.runSync("setInvoiceStatus", UtilMisc.<String, Object>toMap("invoiceId", invoiceId, "statusId",
                        "INVOICE_READY", "userLogin", userLogin));
            } catch (GenericServiceException e) {
                Debug.logError(e, MODULE);
                return ServiceUtil.returnError(e.getMessage());
            }
            if (ServiceUtil.isError(setInvoiceStatusResult)) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                        "AccountingErrorCreatingInvoiceFromOrder", locale), null, null, setInvoiceStatusResult);
            }
        }
        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> createSalesInvoicesFromDropShipment(DispatchContext dctx, Map<String, Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        String shipmentId = (String) context.get("shipmentId");
        Locale locale = (Locale) context.get("locale");

        Map<String, Object> serviceContext = UtilMisc.toMap("shipmentIds", UtilMisc.toList(shipmentId), "createSalesInvoicesForDropShipments",
                Boolean.TRUE, "userLogin", context.get("userLogin"));

        Map<String, Object> serviceResult;
        try {
            serviceResult = dispatcher.runSync("createInvoicesFromShipments", serviceContext);
            if (ServiceUtil.isError(serviceResult)) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                        "AccountingTroubleCallingCreateInvoicesFromShipmentService",
                        UtilMisc.toMap("shipmentId", shipmentId), locale));
            }
        } catch (GenericServiceException e) {
            Debug.logError(e, "Trouble calling createInvoicesFromShipment service; invoice not created for shipment " + shipmentId, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    "AccountingTroubleCallingCreateInvoicesFromShipmentService",
                    UtilMisc.toMap("shipmentId", shipmentId), locale));
        }

        return serviceResult;
    }

    public static Map<String, Object> createInvoicesFromShipments(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        List<String> shipmentIds = UtilGenerics.cast(context.get("shipmentIds"));
        Locale locale = (Locale) context.get("locale");
        Boolean createSalesInvoicesForDropShipments = (Boolean) context.get("createSalesInvoicesForDropShipments");
        if (UtilValidate.isEmpty(createSalesInvoicesForDropShipments)) {
            createSalesInvoicesForDropShipments = Boolean.FALSE;
        }

        boolean salesShipmentFound = false;
        boolean purchaseShipmentFound = false;
        boolean dropShipmentFound = false;

        List<String> invoicesCreated = new LinkedList<>();

        //DEJ20060520: not used? planned to be used? List shipmentIdList = new LinkedList();
        for (String tmpShipmentId : shipmentIds) {
            try {
                GenericValue shipment = EntityQuery.use(delegator).from("Shipment").where("shipmentId", tmpShipmentId).queryOne();
                if ((shipment.getString("shipmentTypeId") != null) && ("PURCHASE_SHIPMENT".equals(shipment.getString("shipmentTypeId")))) {
                    purchaseShipmentFound = true;
                } else if ((shipment.getString("shipmentTypeId") != null) && ("DROP_SHIPMENT".equals(shipment.getString("shipmentTypeId")))) {
                    dropShipmentFound = true;
                } else {
                    salesShipmentFound = true;
                }
                if (purchaseShipmentFound && salesShipmentFound && dropShipmentFound) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                            "AccountingShipmentsOfDifferentTypes",
                            UtilMisc.toMap("tmpShipmentId", tmpShipmentId, "shipmentTypeId", shipment.getString("shipmentTypeId")),
                            locale));
                }
            } catch (GenericEntityException e) {
                Debug.logError(e, "Trouble getting Shipment entity for shipment " + tmpShipmentId, MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                        "AccountingTroubleGettingShipmentEntity",
                        UtilMisc.toMap("tmpShipmentId", tmpShipmentId), locale));
            }
        }
        EntityQuery shipmentQuery =
                EntityQuery.use(delegator).where(EntityCondition.makeCondition("shipmentId", EntityOperator.IN, shipmentIds)).orderBy("shipmentId");
        // check the status of the shipment

        // get the items of the shipment.  They can come from ItemIssuance if the shipment were from a sales order, ShipmentReceipt
        // if it were a purchase order or from the order items of the (possibly linked) orders if the shipment is a drop shipment
        List<GenericValue> items = null;
        List<GenericValue> orderItemAssocs = null;
        try {
            if (purchaseShipmentFound) {
                items = shipmentQuery.from("ShipmentReceipt").queryList();
                // filter out items which have been received but are not actually owned by an internal organization, so they should not be on a
                // purchase invoice
                Iterator<GenericValue> itemsIter = items.iterator();
                while (itemsIter.hasNext()) {
                    GenericValue item = itemsIter.next();
                    GenericValue inventoryItem = item.getRelatedOne("InventoryItem", false);
                    GenericValue ownerPartyRole = EntityQuery.use(delegator).from("PartyRole")
                            .where("partyId", inventoryItem.get("ownerPartyId"), "roleTypeId", "INTERNAL_ORGANIZATIO").cache().queryOne();
                    if (UtilValidate.isEmpty(ownerPartyRole)) {
                        itemsIter.remove();
                    }
                }
            } else if (dropShipmentFound) {

                List<GenericValue> shipments = shipmentQuery.from("Shipment").queryList();

                // Get the list of purchase order IDs related to the shipments
                List<String> purchaseOrderIds = EntityUtil.getFieldListFromEntityList(shipments, "primaryOrderId", true);

                if (createSalesInvoicesForDropShipments) {

                    // If a sales invoice is being created for a drop shipment, we have to reference the original sales order items
                    // Get the list of the linked orderIds (original sales orders)
                    orderItemAssocs = EntityQuery.use(delegator).from("OrderItemAssoc")
                            .where(EntityCondition.makeCondition("toOrderId", EntityOperator.IN, purchaseOrderIds)).queryList();

                    // Get only the order items which are indirectly related to the purchase order - this limits the list to the drop ship group(s)
                    items = EntityUtil.getRelated("FromOrderItem", null, orderItemAssocs, false);
                } else {

                    // If it's a purchase invoice being created, the order items for that purchase orders can be used directly
                    items = EntityQuery.use(delegator).from("OrderItem")
                            .where(EntityCondition.makeCondition("orderId", EntityOperator.IN, purchaseOrderIds)).queryList();
                }
            } else {
                items = shipmentQuery.from("ItemIssuance").queryList();
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, "Problem getting issued items from shipments", MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    "AccountingProblemGettingItemsFromShipments", locale));
        }
        if (items.isEmpty()) {
            Debug.logInfo("No items issued for shipments", MODULE);
            return ServiceUtil.returnSuccess();
        }

        // group items by order
        Map<String, List<GenericValue>> shippedOrderItems = new HashMap<>();
        for (GenericValue item : items) {
            String orderId = item.getString("orderId");
            String orderItemSeqId = item.getString("orderItemSeqId");
            List<GenericValue> itemsByOrder = shippedOrderItems.get(orderId);
            if (itemsByOrder == null) {
                itemsByOrder = new LinkedList<>();
            }

            // check and make sure we haven't already billed for this issuance or shipment receipt
            List<EntityCondition> billFields = new LinkedList<>();
            billFields.add(EntityCondition.makeCondition("orderId", orderId));
            billFields.add(EntityCondition.makeCondition("orderItemSeqId", orderItemSeqId));
            billFields.add(EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "INVOICE_CANCELLED"));

            if (dropShipmentFound) {

                // Drop shipments have neither issuances nor receipts, so this check is meaningless
                itemsByOrder.add(item);
                shippedOrderItems.put(orderId, itemsByOrder);
                continue;
            } else if ("ItemIssuance".equals(item.getEntityName())) {
                billFields.add(EntityCondition.makeCondition("itemIssuanceId", item.get("itemIssuanceId")));
            } else if ("ShipmentReceipt".equals(item.getEntityName())) {
                billFields.add(EntityCondition.makeCondition("shipmentReceiptId", item.getString("receiptId")));
            }
            List<GenericValue> itemBillings = null;
            try {
                itemBillings = EntityQuery.use(delegator).from("OrderItemBillingAndInvoiceAndItem").where(billFields).queryList();
            } catch (GenericEntityException e) {
                Debug.logError(e, "Problem looking up OrderItemBilling records for " + billFields, MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                        "AccountingProblemLookingUpOrderItemBilling",
                        UtilMisc.toMap("billFields", billFields), locale));
            }

            // if none found, then okay to bill
            if (itemBillings.isEmpty()) {
                itemsByOrder.add(item);
            }

            // update the map with modified list
            shippedOrderItems.put(orderId, itemsByOrder);
        }

        // make sure we aren't billing items already invoiced i.e. items billed as digital (FINDIG)
        for (Entry<String, List<GenericValue>> order : shippedOrderItems.entrySet()) {
            String orderId = order.getKey();
            // we'll only use this list to figure out which ones to send
            List<GenericValue> billItems = order.getValue();

            // a new list to be used to pass to the create invoice service
            List<GenericValue> toBillItems = new LinkedList<>();

            // map of available quantities so we only have to calc once
            Map<String, BigDecimal> itemQtyAvail = new HashMap<>();

            // now we will check each issuance and make sure it hasn't already been billed
            for (GenericValue issue : billItems) {
                BigDecimal issueQty = BigDecimal.ZERO;

                if ("ShipmentReceipt".equals(issue.getEntityName())) {
                    issueQty = issue.getBigDecimal("quantityAccepted");
                } else {
                    issueQty = issue.getBigDecimal("quantity");
                }

                BigDecimal billAvail = itemQtyAvail.get(issue.getString("orderItemSeqId"));
                if (billAvail == null) {
                    List<EntityCondition> lookup = new LinkedList<>();
                    lookup.add(EntityCondition.makeCondition("orderId", orderId));
                    lookup.add(EntityCondition.makeCondition("orderItemSeqId", issue.get("orderItemSeqId")));
                    lookup.add(EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "INVOICE_CANCELLED"));
                    GenericValue orderItem = null;
                    List<GenericValue> billed = null;
                    BigDecimal orderedQty = null;
                    try {
                        orderItem = "OrderItem".equals(issue.getEntityName()) ? issue : issue.getRelatedOne("OrderItem", false);

                        // total ordered
                        orderedQty = orderItem.getBigDecimal("quantity");

                        if (dropShipmentFound && createSalesInvoicesForDropShipments) {

                            // Override the issueQty with the quantity from the purchase order item
                            GenericValue orderItemAssoc = EntityUtil.getFirst(EntityUtil.filterByAnd(orderItemAssocs, UtilMisc.toMap("orderId",
                                    issue.getString("orderId"), "orderItemSeqId", issue.getString("orderItemSeqId"))));
                            GenericValue purchaseOrderItem = orderItemAssoc.getRelatedOne("ToOrderItem", false);
                            orderItem.set("quantity", purchaseOrderItem.getBigDecimal("quantity"));
                            issueQty = purchaseOrderItem.getBigDecimal("quantity");
                        }
                        billed = EntityQuery.use(delegator).from("OrderItemBillingAndInvoiceAndItem").where(lookup).queryList();
                    } catch (GenericEntityException e) {
                        Debug.logError(e, "Problem getting OrderItem/OrderItemBilling records " + lookup, MODULE);
                        return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                                "AccountingProblemGettingOrderItemOrderItemBilling",
                                UtilMisc.toMap("lookup", lookup), locale));
                    }


                    // add up the already billed total
                    if (!billed.isEmpty()) {
                        BigDecimal billedQuantity = BigDecimal.ZERO;
                        for (GenericValue oib : billed) {
                            BigDecimal qty = oib.getBigDecimal("quantity");
                            if (qty != null) {
                                billedQuantity = billedQuantity.add(qty).setScale(DECIMALS, ROUNDING);
                            }
                        }
                        BigDecimal leftToBill = orderedQty.subtract(billedQuantity).setScale(DECIMALS, ROUNDING);
                        billAvail = leftToBill;
                    } else {
                        billAvail = orderedQty;
                    }
                }

                // no available means we cannot bill anymore
                if (billAvail != null && billAvail.signum() == 1) { // this checks if billAvail is a positive non-zero number
                    if (issueQty != null && issueQty.compareTo(billAvail) > 0) {
                        // can only bill some of the issuance; others have been billed already
                        if ("ShipmentReceipt".equals(issue.getEntityName())) {
                            issue.set("quantityAccepted", billAvail);
                        } else {
                            issue.set("quantity", billAvail);
                        }
                        billAvail = BigDecimal.ZERO;
                    } else {
                        // now have been billed
                        if (issueQty == null) {
                            issueQty = BigDecimal.ZERO;
                        }
                        billAvail = billAvail.subtract(issueQty).setScale(DECIMALS, ROUNDING);
                    }

                    // okay to bill these items; but none else
                    toBillItems.add(issue);
                }

                // update the available to bill quantity for the next pass
                itemQtyAvail.put(issue.getString("orderItemSeqId"), billAvail);
            }

            OrderReadHelper orh = new OrderReadHelper(delegator, orderId);

            GenericValue productStore = orh.getProductStore();
            String prorateShipping = productStore != null ? productStore.getString("prorateShipping") : "N";

            // If shipping charges are not prorated, the shipments need to be examined for additional shipping charges
            if ("N".equalsIgnoreCase(prorateShipping)) {

                // Get the set of filtered shipments
                List<GenericValue> invoiceableShipments = null;
                try {
                    if (dropShipmentFound) {

                        List<String> invoiceablePrimaryOrderIds = null;
                        if (createSalesInvoicesForDropShipments) {

                            // If a sales invoice is being created for the drop shipment, we need to reference back to the original purchase order IDs

                            // Get the IDs for orders which have billable items
                            List<String> invoiceableLinkedOrderIds = EntityUtil.getFieldListFromEntityList(toBillItems, "orderId", true);

                            // Get back the IDs of the purchase orders - this will be a list of the purchase order items which are billable by
                            // virtue of not having been
                            //  invoiced in a previous sales invoice
                            List<GenericValue> reverseOrderItemAssocs = EntityUtil.filterByCondition(orderItemAssocs,
                                    EntityCondition.makeCondition("orderId", EntityOperator.IN, invoiceableLinkedOrderIds));
                            invoiceablePrimaryOrderIds = EntityUtil.getFieldListFromEntityList(reverseOrderItemAssocs, "toOrderId", true);

                        } else {

                            // If a purchase order is being created for a drop shipment, the purchase order IDs can be used directly
                            invoiceablePrimaryOrderIds = EntityUtil.getFieldListFromEntityList(toBillItems, "orderId", true);

                        }

                        // Get the list of shipments which are associated with the filtered purchase orders
                        if (!UtilValidate.isEmpty(invoiceablePrimaryOrderIds)) {
                            invoiceableShipments = EntityQuery.use(delegator).from("Shipment").where(
                                    UtilMisc.toList(
                                            EntityCondition.makeCondition("primaryOrderId", EntityOperator.IN, invoiceablePrimaryOrderIds),
                                            EntityCondition.makeCondition("shipmentId", EntityOperator.IN, shipmentIds))).queryList();
                        }
                    } else {
                        List<String> invoiceableShipmentIds = EntityUtil.getFieldListFromEntityList(toBillItems, "shipmentId", true);
                        if (UtilValidate.isNotEmpty(invoiceableShipmentIds)) {
                            invoiceableShipments = EntityQuery.use(delegator).from("Shipment").where(EntityCondition.makeCondition("shipmentId",
                                    EntityOperator.IN, invoiceableShipmentIds)).queryList();
                        }
                    }
                } catch (GenericEntityException e) {
                    Debug.logError(e, "Trouble calling createInvoicesFromShipments service", MODULE);
                    return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                            "AccountingTroubleCallingCreateInvoicesFromShipmentsService", locale));
                }

                // Total the additional shipping charges for the shipments
                Map<GenericValue, BigDecimal> additionalShippingCharges = new HashMap<>();
                BigDecimal totalAdditionalShippingCharges = BigDecimal.ZERO;
                if (UtilValidate.isNotEmpty(invoiceableShipments)) {
                    for (GenericValue shipment : invoiceableShipments) {
                        if (shipment.get("additionalShippingCharge") == null) {
                            continue;
                        }
                        BigDecimal shipmentAdditionalShippingCharges = shipment.getBigDecimal("additionalShippingCharge").setScale(DECIMALS,
                                ROUNDING);
                        additionalShippingCharges.put(shipment, shipmentAdditionalShippingCharges);
                        totalAdditionalShippingCharges = totalAdditionalShippingCharges.add(shipmentAdditionalShippingCharges);
                    }
                }

                // If the additional shipping charges are greater than zero, process them
                if (totalAdditionalShippingCharges.signum() == 1) {

                    // Add an OrderAdjustment to the order for each additional shipping charge
                    for (Map.Entry<GenericValue, BigDecimal> entry : additionalShippingCharges.entrySet()) {
                        GenericValue shipment = entry.getKey();
                        BigDecimal additionalShippingCharge = entry.getValue();
                        String shipmentId = shipment.getString("shipmentId");
                        Map<String, Object> createOrderAdjustmentContext = new HashMap<>();
                        createOrderAdjustmentContext.put("orderId", orderId);
                        createOrderAdjustmentContext.put("orderAdjustmentTypeId", "SHIPPING_CHARGES");
                        String addtlChargeDescription = shipment.getString("addtlShippingChargeDesc");
                        if (UtilValidate.isEmpty(addtlChargeDescription)) {
                            addtlChargeDescription = UtilProperties.getMessage(RESOURCE, "AccountingAdditionalShippingChargeForShipment",
                                    UtilMisc.toMap("shipmentId", shipmentId), locale);
                        }
                        createOrderAdjustmentContext.put("description", addtlChargeDescription);
                        createOrderAdjustmentContext.put("sourceReferenceId", shipmentId);
                        createOrderAdjustmentContext.put("amount", additionalShippingCharge);
                        createOrderAdjustmentContext.put("userLogin", context.get("userLogin"));
                        String shippingOrderAdjustmentId = null;
                        try {
                            Map<String, Object> createOrderAdjustmentResult = dispatcher.runSync("createOrderAdjustment",
                                    createOrderAdjustmentContext);
                            if (ServiceUtil.isError(createOrderAdjustmentResult)) {
                                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(createOrderAdjustmentResult));
                            }
                            shippingOrderAdjustmentId = (String) createOrderAdjustmentResult.get("orderAdjustmentId");
                        } catch (GenericServiceException e) {
                            Debug.logError(e, "Trouble calling createOrderAdjustment service", MODULE);
                            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                                    "AccountingTroubleCallingCreateOrderAdjustmentService", locale));
                        }

                        // Obtain a list of OrderAdjustments due to tax on the shipping charges, if any
                        GenericValue billToParty = orh.getBillToParty();
                        GenericValue payToParty = orh.getBillFromParty();
                        GenericValue destinationContactMech = null;
                        try {
                            destinationContactMech = shipment.getRelatedOne("DestinationPostalAddress", false);
                        } catch (GenericEntityException e) {
                            Debug.logError(e, "Trouble calling createInvoicesFromShipment service; invoice not created for shipment " + shipmentId,
                                    MODULE);
                            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                                    "AccountingTroubleCallingCreateInvoicesFromShipmentService", locale));
                        }

                        List<Object> emptyList = new LinkedList<>();
                        Map<String, Object> calcTaxContext = new HashMap<>();
                        calcTaxContext.put("productStoreId", orh.getProductStoreId());
                        calcTaxContext.put("payToPartyId", payToParty.getString("partyId"));
                        calcTaxContext.put("billToPartyId", billToParty.getString("partyId"));
                        calcTaxContext.put("orderShippingAmount", totalAdditionalShippingCharges);
                        calcTaxContext.put("shippingAddress", destinationContactMech);

                        // These parameters don't matter if we're only worried about adjustments on the shipping charges
                        calcTaxContext.put("itemProductList", emptyList);
                        calcTaxContext.put("itemAmountList", emptyList);
                        calcTaxContext.put("itemPriceList", emptyList);
                        calcTaxContext.put("itemQuantityList", emptyList);
                        calcTaxContext.put("itemShippingList", emptyList);

                        Map<String, Object> calcTaxResult = null;
                        try {
                            calcTaxResult = dispatcher.runSync("calcTax", calcTaxContext);
                            if (ServiceUtil.isError(calcTaxResult)) {
                                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                                        "AccountingTroubleCallingCalcTaxService", locale));
                            }
                        } catch (GenericServiceException e) {
                            Debug.logError(e, "Trouble calling calcTaxService", MODULE);
                            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                                    "AccountingTroubleCallingCalcTaxService", locale));
                        }
                        List<GenericValue> orderAdjustments = UtilGenerics.cast(calcTaxResult.get("orderAdjustments"));

                        // If we have any OrderAdjustments due to tax on shipping, store them and add them to the total
                        if (orderAdjustments != null) {
                            for (GenericValue orderAdjustment : orderAdjustments) {
                                totalAdditionalShippingCharges =
                                        totalAdditionalShippingCharges.add(orderAdjustment.getBigDecimal("amount").setScale(DECIMALS, ROUNDING));
                                orderAdjustment.set("orderAdjustmentId", delegator.getNextSeqId("OrderAdjustment"));
                                orderAdjustment.set("orderId", orderId);
                                orderAdjustment.set("orderItemSeqId", "_NA_");
                                orderAdjustment.set("shipGroupSeqId", shipment.getString("primaryShipGroupSeqId"));
                                orderAdjustment.set("originalAdjustmentId", shippingOrderAdjustmentId);
                            }
                            try {
                                delegator.storeAll(orderAdjustments);
                            } catch (GenericEntityException e) {
                                Debug.logError(e, "Problem storing OrderAdjustments: " + orderAdjustments, MODULE);
                                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                                        "AccountingProblemStoringOrderAdjustments",
                                        UtilMisc.toMap("orderAdjustments", orderAdjustments), locale));
                            }
                        }

                        // If part of the order was paid via credit card, try to charge it for the additional shipping
                        List<GenericValue> orderPaymentPreferences = null;
                        try {
                            orderPaymentPreferences = EntityQuery.use(delegator).from("OrderPaymentPreference")
                                    .where("orderId", orderId, "paymentMethodTypeId", "CREDIT_CARD").queryList();
                        } catch (GenericEntityException e) {
                            Debug.logError(e, "Problem getting OrderPaymentPreference records", MODULE);
                            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                                    "AccountingProblemGettingOrderPaymentPreferences", locale));
                        }

                        //  Use the first credit card we find, for the sake of simplicity
                        String paymentMethodId = null;
                        GenericValue cardOrderPaymentPref = EntityUtil.getFirst(orderPaymentPreferences);
                        if (cardOrderPaymentPref != null) {
                            paymentMethodId = cardOrderPaymentPref.getString("paymentMethodId");
                        }

                        if (paymentMethodId != null) {

                            // Release all outstanding (not settled or cancelled) authorizations, while keeping a running
                            //  total of their amounts so that the total plus the additional shipping charges can be authorized again
                            //  all at once.
                            BigDecimal totalNewAuthAmount = totalAdditionalShippingCharges.setScale(DECIMALS, ROUNDING);
                            for (GenericValue orderPaymentPreference : orderPaymentPreferences) {
                                if (!("PAYMENT_SETTLED".equals(orderPaymentPreference.getString("statusId")) || "PAYMENT_CANCELLED"
                                        .equals(orderPaymentPreference.getString("statusId")))) {
                                    GenericValue authTransaction = PaymentGatewayServices.getAuthTransaction(orderPaymentPreference);
                                    if (authTransaction != null && authTransaction.get("amount") != null) {

                                        // Update the total authorized amount
                                        totalNewAuthAmount = totalNewAuthAmount.add(authTransaction.getBigDecimal("amount").setScale(DECIMALS,
                                                ROUNDING));

                                        // Release the authorization for the OrderPaymentPreference
                                        Map<String, Object> prefReleaseResult = null;
                                        try {
                                            prefReleaseResult = dispatcher.runSync("releaseOrderPaymentPreference", UtilMisc.toMap(
                                                    "orderPaymentPreferenceId", orderPaymentPreference.getString("orderPaymentPreferenceId"),
                                                    "userLogin", context.get("userLogin")));
                                        } catch (GenericServiceException e) {
                                            Debug.logError(e, "Trouble calling releaseOrderPaymentPreference service", MODULE);
                                            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                                                    "AccountingTroubleCallingReleaseOrderPaymentPreferenceService", locale));
                                        }
                                        if (ServiceUtil.isError(prefReleaseResult) || ServiceUtil.isFailure(prefReleaseResult)) {
                                            String errMsg = ServiceUtil.getErrorMessage(prefReleaseResult);
                                            Debug.logError(errMsg, MODULE);
                                            return ServiceUtil.returnError(errMsg);
                                        }
                                    }
                                }
                            }

                            // Create a new OrderPaymentPreference for the order to handle the new (totalled) charge. Don't
                            //  set the maxAmount so that it doesn't interfere with other authorizations
                            Map<String, Object> serviceContext = UtilMisc.toMap("orderId", orderId, "paymentMethodId", paymentMethodId,
                                    "paymentMethodTypeId", "CREDIT_CARD", "userLogin", context.get("userLogin"));
                            String orderPaymentPreferenceId = null;
                            try {
                                Map<String, Object> result = dispatcher.runSync("createOrderPaymentPreference", serviceContext);
                                if (ServiceUtil.isError(result)) {
                                    return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                                            "AccountingTroubleCallingCreateOrderPaymentPreferenceService", locale));
                                }
                                orderPaymentPreferenceId = (String) result.get("orderPaymentPreferenceId");
                            } catch (GenericServiceException e) {
                                Debug.logError(e, "Trouble calling createOrderPaymentPreference service", MODULE);
                                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                                        "AccountingTroubleCallingCreateOrderPaymentPreferenceService", locale));
                            }

                            // Attempt to authorize the new orderPaymentPreference
                            Map<String, Object> authResult = null;
                            try {
                                // Use an overrideAmount because the maxAmount wasn't set on the OrderPaymentPreference
                                authResult = dispatcher.runSync("authOrderPaymentPreference", UtilMisc.toMap("orderPaymentPreferenceId",
                                        orderPaymentPreferenceId, "overrideAmount", totalNewAuthAmount, "userLogin", context.get("userLogin")));
                                if (ServiceUtil.isError(authResult)) {
                                    return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                                            "AccountingTroubleCallingAuthOrderPaymentPreferenceService", locale));
                                }
                            } catch (GenericServiceException e) {
                                Debug.logError(e, "Trouble calling authOrderPaymentPreference service", MODULE);
                                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                                        "AccountingTroubleCallingAuthOrderPaymentPreferenceService", locale));
                            }

                            // If the authorization fails, create the invoice anyway, but make a note of it
                            boolean authFinished = (Boolean) authResult.get("finished");
                            boolean authErrors = (Boolean) authResult.get("errors");
                            if (authErrors || !authFinished) {
                                String errMsg = UtilProperties.getMessage(RESOURCE, "AccountingUnableToAuthAdditionalShipCharges", UtilMisc.toMap(
                                        "shipmentId", shipmentId, "paymentMethodId", paymentMethodId, "orderPaymentPreferenceId",
                                        orderPaymentPreferenceId), locale);
                                Debug.logError(errMsg, MODULE);
                            }

                        }
                    }
                }
            } else {
                Debug.logInfo(UtilProperties.getMessage(RESOURCE, "AccountingIgnoringAdditionalShipCharges", UtilMisc.toMap("productStoreId",
                        orh.getProductStoreId()), locale), MODULE);
            }

            String invoiceId = null;
            GenericValue shipmentItemBilling = null;
            String shipmentId = shipmentIds.get(0);
            try {
                shipmentItemBilling = EntityQuery.use(delegator).from("ShipmentItemBilling").where("shipmentId", shipmentId).queryFirst();
            } catch (GenericEntityException e) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                        "AccountingProblemGettingShipmentItemBilling", locale));
            }
            if (shipmentItemBilling != null) {
                invoiceId = shipmentItemBilling.getString("invoiceId");
            }

            // call the createInvoiceForOrder service for each order
            Map<String, Object> serviceContext = UtilMisc.toMap("orderId", orderId, "billItems", toBillItems, "invoiceId", invoiceId, "eventDate",
                    context.get("eventDate"), "userLogin", context.get("userLogin"));
            try {
                Map<String, Object> result = dispatcher.runSync("createInvoiceForOrder", serviceContext);
                if (ServiceUtil.isError(result)) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                            "AccountingTroubleCallingCreateInvoiceForOrderService", locale));
                }
                invoicesCreated.add((String) result.get("invoiceId"));
            } catch (GenericServiceException e) {
                Debug.logError(e, "Trouble calling createInvoiceForOrder service; invoice not created for shipment", MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                        "AccountingTroubleCallingCreateInvoiceForOrderService", locale));
            }
        }

        Map<String, Object> response = ServiceUtil.returnSuccess();
        response.put("invoicesCreated", invoicesCreated);
        return response;
    }

    private static String getInvoiceItemType(Delegator delegator, String key1, String key2, String invoiceTypeId, String defaultValue) {
        GenericValue itemMap = null;
        try {
            if (UtilValidate.isNotEmpty(key1)) {
                itemMap =
                        EntityQuery.use(delegator).from("InvoiceItemTypeMap").where("invoiceItemMapKey", key1, "invoiceTypeId",
                                invoiceTypeId).cache().queryOne();
            }
            if (itemMap == null && UtilValidate.isNotEmpty(key2)) {
                itemMap =
                        EntityQuery.use(delegator).from("InvoiceItemTypeMap").where("invoiceItemMapKey", key2, "invoiceTypeId",
                                invoiceTypeId).cache().queryOne();
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, "Trouble getting InvoiceItemTypeMap entity record", MODULE);
            return defaultValue;
        }
        if (itemMap != null) {
            return itemMap.getString("invoiceItemTypeId");
        }
        return defaultValue;
    }

    public static Map<String, Object> createInvoicesFromReturnShipment(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Locale locale = (Locale) context.get("locale");

        String shipmentId = (String) context.get("shipmentId");
        String errorMsg = UtilProperties.getMessage(RESOURCE, "AccountingErrorCreatingInvoiceForShipment",
                UtilMisc.toMap("shipmentId", shipmentId), locale);
        boolean salesReturnFound = false;
        boolean purchaseReturnFound = false;

        List<String> invoicesCreated = new LinkedList<>();
        try {

            // get the shipment and validate that it is a sales return
            GenericValue shipment = EntityQuery.use(delegator).from("Shipment").where("shipmentId", shipmentId).queryOne();
            if (shipment == null) {
                return ServiceUtil.returnError(errorMsg + UtilProperties.getMessage(RESOURCE,
                        "AccountingShipmentNotFound", locale));
            }
            if ("SALES_RETURN".equals(shipment.getString("shipmentTypeId"))) {
                salesReturnFound = true;
            } else if ("PURCHASE_RETURN".equals(shipment.getString("shipmentTypeId"))) {
                purchaseReturnFound = true;
            }
            if (!(salesReturnFound || purchaseReturnFound)) {
                return ServiceUtil.returnError(errorMsg + UtilProperties.getMessage(RESOURCE,
                        "AccountingShipmentNotSalesReturnAndPurchaseReturn", locale));
            }
            // get the items of the shipment. They can come from ItemIssuance if the shipment were from a purchase return, ShipmentReceipt if it
            // were from a sales return
            List<GenericValue> shippedItems = null;
            if (salesReturnFound) {
                shippedItems = shipment.getRelated("ShipmentReceipt", null, null, false);
            } else if (purchaseReturnFound) {
                shippedItems = shipment.getRelated("ItemIssuance", null, null, false);
            }
            if (shippedItems == null) {
                Debug.logInfo("No items issued for shipments", MODULE);
                return ServiceUtil.returnSuccess();
            }

            // group the shipments by returnId (because we want a seperate itemized invoice for each return)
            Map<String, List<GenericValue>> itemsShippedGroupedByReturn = new HashMap<>();

            for (GenericValue item : shippedItems) {
                String returnId = null;
                String returnItemSeqId = null;
                if ("ShipmentReceipt".equals(item.getEntityName())) {
                    returnId = item.getString("returnId");
                } else if ("ItemIssuance".equals(item.getEntityName())) {
                    GenericValue returnItemShipment = EntityQuery.use(delegator).from("ReturnItemShipment")
                            .where("shipmentId", item.get("shipmentId"), "shipmentItemSeqId", item.get("shipmentItemSeqId"))
                            .queryFirst();
                    returnId = returnItemShipment.getString("returnId");
                    returnItemSeqId = returnItemShipment.getString("returnItemSeqId");
                }

                // see if there are ReturnItemBillings for this item
                Long billingCount = 0L;
                if ("ShipmentReceipt".equals(item.getEntityName())) {
                    billingCount = EntityQuery.use(delegator).from("ReturnItemBilling")
                            .where("shipmentReceiptId", item.get("receiptId"),
                                    "returnId", returnId,
                                    "returnItemSeqId", item.get("returnItemSeqId"))
                            .queryCount();
                } else if ("ItemIssuance".equals(item.getEntityName())) {
                    billingCount = EntityQuery.use(delegator).from("ReturnItemBilling").where("returnId", returnId, "returnItemSeqId",
                            returnItemSeqId).queryCount();
                }
                // if there are billings, we have already billed the item, so skip it
                if (billingCount > 0) {
                    continue;
                }

                // get the List of items shipped to/from this returnId
                List<GenericValue> billItems = itemsShippedGroupedByReturn.get(returnId);
                if (billItems == null) {
                    billItems = new LinkedList<>();
                }

                // add our item to the group and put it back in the map
                billItems.add(item);
                itemsShippedGroupedByReturn.put(returnId, billItems);
            }

            // loop through the returnId keys in the map and invoke the createInvoiceFromReturn service for each
            for (Map.Entry<String, List<GenericValue>> entry : itemsShippedGroupedByReturn.entrySet()) {
                String returnId = entry.getKey();
                List<GenericValue> billItems = entry.getValue();
                if (Debug.verboseOn()) {
                    Debug.logVerbose("Creating invoice for return [" + returnId + "] with items: " + billItems.toString(), MODULE);
                }
                Map<String, Object> input = UtilMisc.toMap("returnId", returnId, "billItems", billItems, "userLogin", context.get("userLogin"));
                Map<String, Object> serviceResults = dispatcher.runSync("createInvoiceFromReturn", input);
                if (ServiceUtil.isError(serviceResults)) {
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResults));
                }

                // put the resulting invoiceId in the return list
                invoicesCreated.add((String) serviceResults.get("invoiceId"));
            }
        } catch (GenericServiceException | GenericEntityException e) {
            Debug.logError(e, errorMsg + e.getMessage(), MODULE);
            return ServiceUtil.returnError(errorMsg + e.getMessage());
        }

        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put("invoicesCreated", invoicesCreated);
        return result;
    }

    public static Map<String, Object> createInvoiceFromReturn(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = (Locale) context.get("locale");

        String returnId = (String) context.get("returnId");
        List<GenericValue> billItems = UtilGenerics.cast(context.get("billItems"));
        String errorMsg = UtilProperties.getMessage(RESOURCE, "AccountingErrorCreatingInvoiceForReturn", UtilMisc.toMap("returnId", returnId),
                locale);
        // List invoicesCreated = new ArrayList();
        try {
            String invoiceTypeId;
            String description;
            // get the return header
            GenericValue returnHeader = EntityQuery.use(delegator).from("ReturnHeader").where("returnId", returnId).queryOne();
            if (returnHeader == null || returnHeader.get("returnHeaderTypeId") == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "AccountingReturnTypeCannotBeNull", locale));
            }

            if (returnHeader.getString("returnHeaderTypeId").startsWith("CUSTOMER_")) {
                invoiceTypeId = "CUST_RTN_INVOICE";
                description = "Return Invoice for Customer Return #" + returnId;
            } else {
                invoiceTypeId = "PURC_RTN_INVOICE";
                description = "Return Invoice for Vendor Return #" + returnId;
            }

            List<GenericValue> returnItems = returnHeader.getRelated("ReturnItem", null, null, false);
            if (!returnItems.isEmpty()) {
                for (GenericValue returnItem : returnItems) {
                    if ("RETURN_COMPLETED".equals(returnItem.getString("statusId"))) {
                        GenericValue product = returnItem.getRelatedOne("Product", false);
                        if (!ProductWorker.isPhysical(product)) {
                            boolean isNonPhysicalItemToReturn = false;
                            List<GenericValue> returnItemBillings = returnItem.getRelated("ReturnItemBilling", null, null, false);

                            if (!returnItemBillings.isEmpty()) {
                                GenericValue invoice = EntityUtil.getFirst(returnItemBillings).getRelatedOne("Invoice", false);
                                if ("INVOICE_CANCELLED".equals(invoice.getString("statusId"))) {
                                    isNonPhysicalItemToReturn = true;
                                }
                            } else {
                                isNonPhysicalItemToReturn = true;
                            }

                            if (isNonPhysicalItemToReturn) {
                                if (UtilValidate.isEmpty(billItems)) {
                                    billItems = new ArrayList<>();
                                }

                                billItems.add(returnItem);
                            }
                        }
                    }
                }
            }

            Map<String, Object> results = ServiceUtil.returnSuccess();
            if (UtilValidate.isNotEmpty(billItems)) {
                // set the invoice data
                Map<String, Object> input = UtilMisc.<String, Object>toMap("invoiceTypeId", invoiceTypeId, "statusId", "INVOICE_IN_PROCESS");
                input.put("partyId", returnHeader.get("toPartyId"));
                input.put("partyIdFrom", returnHeader.get("fromPartyId"));
                input.put("currencyUomId", returnHeader.get("currencyUomId"));
                input.put("invoiceDate", UtilDateTime.nowTimestamp());
                input.put("description", description);
                input.put("billingAccountId", returnHeader.get("billingAccountId"));
                input.put("userLogin", userLogin);

                // call the service to create the invoice
                Map<String, Object> serviceResults = dispatcher.runSync("createInvoice", input);
                if (ServiceUtil.isError(serviceResults)) {
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResults));
                }
                String invoiceId = (String) serviceResults.get("invoiceId");

                // keep track of the invoice total vs the promised return total (how much the customer promised to return)
                BigDecimal invoiceTotal = BigDecimal.ZERO;
                BigDecimal promisedTotal = BigDecimal.ZERO;

                // loop through shipment receipts to create invoice items and return item billings for each item and adjustment
                int invoiceItemSeqNum = 1;
                String invoiceItemSeqId = UtilFormatOut.formatPaddedNumber(invoiceItemSeqNum, INVOICE_ITEM_SEQUENCE_ID_DIGITS);

                for (GenericValue item : billItems) {
                    boolean shipmentReceiptFound = false;
                    boolean itemIssuanceFound = false;
                    GenericValue returnItem = null;
                    BigDecimal quantity = BigDecimal.ZERO;

                    if ("ShipmentReceipt".equals(item.getEntityName())) {
                        shipmentReceiptFound = true;
                    } else if ("ItemIssuance".equals(item.getEntityName())) {
                        itemIssuanceFound = true;
                    } else if ("ReturnItem".equals(item.getEntityName())) {
                        quantity = item.getBigDecimal("returnQuantity");
                        returnItem = item;
                    } else {
                        Debug.logError("Unexpected entity " + item + " of type " + item.getEntityName(), MODULE);
                    }
                    // we need the related return item and product
                    if (shipmentReceiptFound) {
                        returnItem = item.getRelatedOne("ReturnItem", true);
                    } else if (itemIssuanceFound) {
                        GenericValue shipmentItem = item.getRelatedOne("ShipmentItem", true);
                        GenericValue returnItemShipment = EntityUtil.getFirst(shipmentItem.getRelated("ReturnItemShipment", null, null, false));
                        returnItem = returnItemShipment.getRelatedOne("ReturnItem", true);
                    }
                    if (returnItem == null) {
                        continue; // Just to prevent NPE
                    }
                    GenericValue product = returnItem.getRelatedOne("Product", true);

                    // extract the return price as a big decimal for convenience
                    BigDecimal returnPrice = returnItem.getBigDecimal("returnPrice");

                    // determine invoice item type from the return item type
                    String invoiceItemTypeId = getInvoiceItemType(delegator, returnItem.getString("returnItemTypeId"), null, invoiceTypeId, null);
                    if (invoiceItemTypeId == null) {
                        return ServiceUtil.returnError(errorMsg + UtilProperties.getMessage(RESOURCE,
                                "AccountingNoKnownInvoiceItemTypeReturnItemType",
                                UtilMisc.toMap("returnItemTypeId", returnItem.getString("returnItemTypeId")), locale));
                    }
                    if (shipmentReceiptFound) {
                        quantity = item.getBigDecimal("quantityAccepted");
                    } else if (itemIssuanceFound) {
                        quantity = item.getBigDecimal("quantity");
                    }

                    // create the invoice item for this shipment receipt
                    input = UtilMisc.toMap("invoiceId", invoiceId, "invoiceItemTypeId", invoiceItemTypeId, "quantity", quantity);
                    input.put("invoiceItemSeqId", "" + invoiceItemSeqId); // turn the int into a string with ("" + int) hack
                    input.put("amount", returnItem.get("returnPrice"));
                    input.put("productId", returnItem.get("productId"));
                    input.put("taxableFlag", product.get("taxable"));
                    input.put("description", returnItem.get("description"));
                    // TODO: what about the productFeatureId?
                    input.put("userLogin", userLogin);
                    serviceResults = dispatcher.runSync("createInvoiceItem", input);
                    if (ServiceUtil.isError(serviceResults)) {
                        return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResults));
                    }

                    // copy the return item information into ReturnItemBilling
                    input = UtilMisc.toMap("returnId", returnId, "returnItemSeqId", returnItem.get("returnItemSeqId"),
                            "invoiceId", invoiceId);
                    input.put("invoiceItemSeqId", "" + invoiceItemSeqId); // turn the int into a string with ("" + int) hack
                    input.put("quantity", quantity);
                    input.put("amount", returnItem.get("returnPrice"));
                    input.put("userLogin", userLogin);
                    if (shipmentReceiptFound) {
                        input.put("shipmentReceiptId", item.get("receiptId"));
                    }
                    serviceResults = dispatcher.runSync("createReturnItemBilling", input);
                    if (ServiceUtil.isError(serviceResults)) {
                        return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResults));
                    }
                    if (Debug.verboseOn()) {
                        Debug.logVerbose("Creating Invoice Item with amount " + returnPrice + " and quantity " + quantity
                                + " for shipment [" + item.getString("shipmentId") + ":" + item.getString("shipmentItemSeqId") + "]", MODULE);
                    }

                    String parentInvoiceItemSeqId = invoiceItemSeqId;
                    // increment the seqId counter after creating the invoice item and return item billing
                    invoiceItemSeqNum += 1;
                    invoiceItemSeqId = UtilFormatOut.formatPaddedNumber(invoiceItemSeqNum, INVOICE_ITEM_SEQUENCE_ID_DIGITS);

                    // keep a running total (note: a returnItem may have many receipts. hence, the promised total quantity is the receipt
                    // quantityAccepted + quantityRejected)
                    BigDecimal cancelQuantity = BigDecimal.ZERO;
                    if (shipmentReceiptFound) {
                        cancelQuantity = item.getBigDecimal("quantityRejected");
                    } else if (itemIssuanceFound) {
                        cancelQuantity = item.getBigDecimal("cancelQuantity");
                    }
                    if (cancelQuantity == null) {
                        cancelQuantity = BigDecimal.ZERO;
                    }
                    BigDecimal actualAmount = returnPrice.multiply(quantity).setScale(DECIMALS, ROUNDING);
                    BigDecimal promisedAmount = returnPrice.multiply(quantity.add(cancelQuantity)).setScale(DECIMALS, ROUNDING);
                    invoiceTotal = invoiceTotal.add(actualAmount).setScale(DECIMALS, ROUNDING);
                    promisedTotal = promisedTotal.add(promisedAmount).setScale(DECIMALS, ROUNDING);

                    // for each adjustment related to this ReturnItem, create a separate invoice item
                    List<GenericValue> adjustments = returnItem.getRelated("ReturnAdjustment", null, null, true);
                    for (GenericValue adjustment : adjustments) {

                        if (adjustment.get("amount") == null) {
                            Debug.logWarning("Return adjustment [" + adjustment.get("returnAdjustmentId")
                                            + "] has null amount and will be skipped", MODULE);
                            continue;
                        }

                        // determine invoice item type from the return item type
                        invoiceItemTypeId = getInvoiceItemType(delegator, adjustment.getString("returnAdjustmentTypeId"), null, invoiceTypeId, null);
                        if (invoiceItemTypeId == null) {
                            return ServiceUtil.returnError(errorMsg + UtilProperties.getMessage(RESOURCE,
                                    "AccountingNoKnownInvoiceItemTypeReturnAdjustmentType",
                                    UtilMisc.toMap("returnAdjustmentTypeId", adjustment.getString("returnAdjustmentTypeId")), locale));
                        }

                        // prorate the adjustment amount by the returned amount; do not round ratio
                        BigDecimal ratio = quantity.divide(returnItem.getBigDecimal("returnQuantity"), 100, ROUNDING);
                        BigDecimal amount = adjustment.getBigDecimal("amount");
                        amount = amount.multiply(ratio).setScale(DECIMALS, ROUNDING);
                        if (Debug.verboseOn()) {
                            Debug.logVerbose("Creating Invoice Item with amount " + adjustment.getBigDecimal("amount") + " prorated to " + amount
                                    + " for return adjustment [" + adjustment.getString("returnAdjustmentId") + "]", MODULE);
                        }

                        // prepare invoice item data for this adjustment
                        input = UtilMisc.toMap("invoiceId", invoiceId, "invoiceItemTypeId", invoiceItemTypeId, "quantity", BigDecimal.ONE);
                        input.put("amount", amount);
                        input.put("invoiceItemSeqId", "" + invoiceItemSeqId); // turn the int into a string with ("" + int) hack
                        input.put("productId", returnItem.get("productId"));
                        input.put("description", adjustment.get("description"));
                        input.put("overrideGlAccountId", adjustment.get("overrideGlAccountId"));
                        input.put("parentInvoiceId", invoiceId);
                        input.put("parentInvoiceItemSeqId", parentInvoiceItemSeqId);
                        input.put("taxAuthPartyId", adjustment.get("taxAuthPartyId"));
                        input.put("taxAuthGeoId", adjustment.get("taxAuthGeoId"));
                        input.put("userLogin", userLogin);

                        // only set taxable flag when the adjustment is not a tax
                        // TODO: Note that we use the value of Product.taxable here. This is not an ideal solution. Instead, use returnAdjustment
                        // .includeInTax
                        if ("RET_SALES_TAX_ADJ".equals(adjustment.get("returnAdjustmentTypeId"))) {
                            input.put("taxableFlag", "N");
                        }

                        // create the invoice item
                        serviceResults = dispatcher.runSync("createInvoiceItem", input);
                        if (ServiceUtil.isError(serviceResults)) {
                            return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResults));
                        }

                        // increment the seqId counter
                        invoiceItemSeqNum += 1;
                        invoiceItemSeqId = UtilFormatOut.formatPaddedNumber(invoiceItemSeqNum, INVOICE_ITEM_SEQUENCE_ID_DIGITS);

                        // keep a running total (promised adjustment in this case is the same as the invoice adjustment)
                        invoiceTotal = invoiceTotal.add(amount).setScale(DECIMALS, ROUNDING);
                        promisedTotal = promisedTotal.add(amount).setScale(DECIMALS, ROUNDING);
                    }
                }

                // ratio of the invoice total to the promised total so far or zero if the amounts were zero
                BigDecimal actualToPromisedRatio = BigDecimal.ZERO;
                if (invoiceTotal.signum() != 0) {
                    actualToPromisedRatio = invoiceTotal.divide(promisedTotal, 100, ROUNDING);  // do not round ratio
                }

                // loop through return-wide adjustments and create invoice items for each
                List<GenericValue> adjustments = returnHeader.getRelated("ReturnAdjustment", UtilMisc.toMap("returnItemSeqId", "_NA_"), null, true);
                for (GenericValue adjustment : adjustments) {

                    // determine invoice item type from the return item type
                    String invoiceItemTypeId = getInvoiceItemType(delegator, adjustment.getString("returnAdjustmentTypeId"), null, invoiceTypeId,
                            null);
                    if (invoiceItemTypeId == null) {
                        return ServiceUtil.returnError(errorMsg + UtilProperties.getMessage(RESOURCE,
                                "AccountingNoKnownInvoiceItemTypeReturnAdjustmentType",
                                UtilMisc.toMap("returnAdjustmentTypeId", adjustment.getString("returnAdjustmentTypeId")), locale));
                    }

                    // prorate the adjustment amount by the actual to promised ratio
                    BigDecimal amount = adjustment.getBigDecimal("amount").multiply(actualToPromisedRatio).setScale(DECIMALS, ROUNDING);
                    if (Debug.verboseOn()) {
                        Debug.logVerbose("Creating Invoice Item with amount " + adjustment.getBigDecimal("amount") + " prorated to " + amount
                                + " for return adjustment [" + adjustment.getString("returnAdjustmentId") + "]", MODULE);
                    }

                    // prepare the invoice item for the return-wide adjustment
                    input = UtilMisc.toMap("invoiceId", invoiceId, "invoiceItemTypeId", invoiceItemTypeId, "quantity", BigDecimal.ONE);
                    input.put("amount", amount);
                    input.put("invoiceItemSeqId", "" + invoiceItemSeqId); // turn the int into a string with ("" + int) hack
                    input.put("description", adjustment.get("description"));
                    input.put("overrideGlAccountId", adjustment.get("overrideGlAccountId"));
                    input.put("taxAuthPartyId", adjustment.get("taxAuthPartyId"));
                    input.put("taxAuthGeoId", adjustment.get("taxAuthGeoId"));
                    input.put("userLogin", userLogin);

                    // XXX TODO Note: we need to implement ReturnAdjustment.includeInTax for this to work properly
                    input.put("taxableFlag", adjustment.get("includeInTax"));

                    // create the invoice item
                    serviceResults = dispatcher.runSync("createInvoiceItem", input);
                    if (ServiceUtil.isError(serviceResults)) {
                        return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResults));
                    }

                    // increment the seqId counter
                    invoiceItemSeqNum += 1;
                    invoiceItemSeqId = UtilFormatOut.formatPaddedNumber(invoiceItemSeqNum, INVOICE_ITEM_SEQUENCE_ID_DIGITS);
                }

                // Set the invoice to READY
                serviceResults = dispatcher.runSync("setInvoiceStatus", UtilMisc.<String, Object>toMap("invoiceId", invoiceId, "statusId",
                        "INVOICE_READY", "userLogin", userLogin));
                if (ServiceUtil.isError(serviceResults)) {
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResults));
                }

                // return the invoiceId
                results.put("invoiceId", invoiceId);
            }
            return results;
        } catch (GenericServiceException | GenericEntityException e) {
            Debug.logError(e, errorMsg + e.getMessage(), MODULE);
            return ServiceUtil.returnError(errorMsg + e.getMessage());
        }
    }

    public static Map<String, Object> checkInvoicePaymentApplications(DispatchContext ctx, Map<String, Object> context) {
        Delegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = (Locale) context.get("locale");

        if (DECIMALS == -1 || ROUNDING == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    "AccountingAritmeticPropertiesNotConfigured", locale));
        }

        String invoiceId = (String) context.get("invoiceId");
        GenericValue invoice = null;
        try {
            invoice = EntityQuery.use(delegator).from("Invoice").where("invoiceId", invoiceId).queryOne();
        } catch (GenericEntityException e) {
            Debug.logError(e, "Problem getting Invoice for Invoice ID" + invoiceId, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    "AccountingInvoiceNotFound", UtilMisc.toMap("invoiceId", invoiceId), locale));
        }

        // Ignore invoices that aren't ready yet
        if (!"INVOICE_READY".equals(invoice.getString("statusId"))) {
            return ServiceUtil.returnSuccess();
        }

        // Get the payment applications that can be used to pay the invoice
        List<GenericValue> paymentAppl = null;
        try {
            paymentAppl = EntityQuery.use(delegator).from("PaymentAndApplication").where("invoiceId", invoiceId).queryList();
            // For each payment application, select only those that are RECEIVED or SENT based on whether the payment is a RECEIPT or DISBURSEMENT
            // respectively
            for (Iterator<GenericValue> iter = paymentAppl.iterator(); iter.hasNext();) {
                GenericValue payment = iter.next();
                if ("PMNT_RECEIVED".equals(payment.get("statusId")) && UtilAccounting.isReceipt(payment)) {
                    continue; // keep
                }
                if ("PMNT_SENT".equals(payment.get("statusId")) && UtilAccounting.isDisbursement(payment)) {
                    continue; // keep
                }
                // all other cases, remove the payment application
                iter.remove();
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, "Problem getting PaymentApplication(s) for Invoice ID " + invoiceId, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    "AccountingProblemGettingPaymentApplication",
                    UtilMisc.toMap("invoiceId", invoiceId), locale));
        }

        Map<String, BigDecimal> payments = new HashMap<>();
        Timestamp paidDate = null;
        for (GenericValue payAppl : paymentAppl) {
            payments.put(payAppl.getString("paymentId"), payAppl.getBigDecimal("amountApplied"));

            // paidDate will be the last date (chronologically) of all the Payments applied to this invoice
            Timestamp paymentDate = payAppl.getTimestamp("effectiveDate");
            if (paymentDate != null) {
                if ((paidDate == null) || (paidDate.before(paymentDate))) {
                    paidDate = paymentDate;
                }
            }
        }

        BigDecimal totalPayments = BigDecimal.ZERO;
        for (BigDecimal amount : payments.values()) {
            if (amount == null) {
                amount = BigDecimal.ZERO;
            }
            totalPayments = totalPayments.add(amount).setScale(DECIMALS, ROUNDING);
        }

        if (totalPayments.signum() == 1) {
            BigDecimal invoiceTotal = InvoiceWorker.getInvoiceTotal(delegator, invoiceId);
            if (Debug.verboseOn()) {
                Debug.logVerbose("Invoice #" + invoiceId + " total: " + invoiceTotal, MODULE);
                Debug.logVerbose("Total payments : " + totalPayments, MODULE);
            }
            if (totalPayments.compareTo(invoiceTotal) >= 0) { // this checks that totalPayments is greater than or equal to invoiceTotal
                // this invoice is paid
                Map<String, Object> svcCtx = UtilMisc.toMap("statusId", "INVOICE_PAID", "invoiceId", invoiceId,
                        "paidDate", paidDate, "userLogin", userLogin);
                try {
                    Map<String, Object> serviceResults = dispatcher.runSync("setInvoiceStatus", svcCtx);
                    if (ServiceUtil.isError(serviceResults)) {
                        return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                                "AccountingProblemChangingInvoiceStatusTo",
                                UtilMisc.toMap("newStatus", "INVOICE_PAID"), locale));
                    }
                } catch (GenericServiceException e) {
                    Debug.logError(e, "Problem changing invoice status to INVOICE_PAID" + svcCtx, MODULE);
                    return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                            "AccountingProblemChangingInvoiceStatusTo",
                            UtilMisc.toMap("newStatus", "INVOICE_PAID"), locale));
                }
            }
        } else {
            Debug.logInfo("No payments found for Invoice #" + invoiceId, MODULE);
        }

        return ServiceUtil.returnSuccess();
    }

    private static BigDecimal calcHeaderAdj(Delegator delegator, GenericValue adj, String invoiceTypeId, String invoiceId, String invoiceItemSeqId,
                                            BigDecimal divisor, BigDecimal multiplier, BigDecimal baseAmount, int decimals, RoundingMode rounding,
                                            GenericValue userLogin, LocalDispatcher dispatcher, Locale locale) {
        BigDecimal adjAmount = BigDecimal.ZERO;
        if (adj.get("amount") != null) {

            // pro-rate the amount
            BigDecimal amount = BigDecimal.ZERO;
            if ("DONATION_ADJUSTMENT".equals(adj.getString("orderAdjustmentTypeId"))) {
                amount = baseAmount;
            } else if (divisor.signum() != 0) { // make sure the divisor is not 0 to avoid NaN problems; just leave the amount as 0 and skip it in
                // essense
                // multiply first then divide to avoid rounding errors
                amount = baseAmount.multiply(multiplier).divide(divisor, decimals, rounding);
            }
            if (amount.signum() != 0) {
                Map<String, Object> createInvoiceItemContext = new HashMap<>();
                createInvoiceItemContext.put("invoiceId", invoiceId);
                createInvoiceItemContext.put("invoiceItemSeqId", invoiceItemSeqId);
                createInvoiceItemContext.put("invoiceItemTypeId", getInvoiceItemType(delegator, adj.getString("orderAdjustmentTypeId"), null,
                        invoiceTypeId, "INVOICE_ADJ"));
                createInvoiceItemContext.put("description", adj.get("description"));
                createInvoiceItemContext.put("quantity", BigDecimal.ONE);
                createInvoiceItemContext.put("amount", amount);
                createInvoiceItemContext.put("overrideGlAccountId", adj.get("overrideGlAccountId"));
                createInvoiceItemContext.put("taxAuthPartyId", adj.get("taxAuthPartyId"));
                createInvoiceItemContext.put("taxAuthGeoId", adj.get("taxAuthGeoId"));
                createInvoiceItemContext.put("taxAuthorityRateSeqId", adj.get("taxAuthorityRateSeqId"));
                createInvoiceItemContext.put("userLogin", userLogin);

                Map<String, Object> createInvoiceItemResult = null;
                try {
                    createInvoiceItemResult = dispatcher.runSync("createInvoiceItem", createInvoiceItemContext);
                } catch (GenericServiceException e) {
                    Debug.logError(e, "Service/other problem creating InvoiceItem from order header adjustment", MODULE);
                    return adjAmount;
                }
                if (ServiceUtil.isError(createInvoiceItemResult)) {
                    return adjAmount;
                }

                // Create the OrderAdjustmentBilling record
                Map<String, Object> createOrderAdjustmentBillingContext = new HashMap<>();
                createOrderAdjustmentBillingContext.put("orderAdjustmentId", adj.getString("orderAdjustmentId"));
                createOrderAdjustmentBillingContext.put("invoiceId", invoiceId);
                createOrderAdjustmentBillingContext.put("invoiceItemSeqId", invoiceItemSeqId);
                createOrderAdjustmentBillingContext.put("amount", amount);
                createOrderAdjustmentBillingContext.put("userLogin", userLogin);

                try {
                    Map<String, Object> result = dispatcher.runSync("createOrderAdjustmentBilling", createOrderAdjustmentBillingContext);
                    if (ServiceUtil.isError(result)) {
                        return adjAmount;
                    }
                } catch (GenericServiceException e) {
                    return adjAmount;
                }

            }
            amount = amount.setScale(decimals, rounding);
            adjAmount = amount;
        } else if (adj.get("sourcePercentage") != null) {
            // pro-rate the amount
            BigDecimal percent = adj.getBigDecimal("sourcePercentage");
            percent = percent.divide(new BigDecimal(100), 100, rounding);
            BigDecimal amount = BigDecimal.ZERO;
            // make sure the divisor is not 0 to avoid NaN problems; just leave the amount as 0 and skip it in essense
            if (divisor.signum() != 0) {
                // multiply first then divide to avoid rounding errors
                amount = percent.multiply(divisor);
            }
            if (amount.signum() != 0) {
                Map<String, Object> createInvoiceItemContext = new HashMap<>();
                createInvoiceItemContext.put("invoiceId", invoiceId);
                createInvoiceItemContext.put("invoiceItemSeqId", invoiceItemSeqId);
                createInvoiceItemContext.put("invoiceItemTypeId", getInvoiceItemType(delegator, adj.getString("orderAdjustmentTypeId"), null,
                        invoiceTypeId, "INVOICE_ADJ"));
                createInvoiceItemContext.put("description", adj.get("description"));
                createInvoiceItemContext.put("quantity", BigDecimal.ONE);
                createInvoiceItemContext.put("amount", amount);
                createInvoiceItemContext.put("overrideGlAccountId", adj.get("overrideGlAccountId"));
                createInvoiceItemContext.put("taxAuthPartyId", adj.get("taxAuthPartyId"));
                createInvoiceItemContext.put("taxAuthGeoId", adj.get("taxAuthGeoId"));
                createInvoiceItemContext.put("taxAuthorityRateSeqId", adj.get("taxAuthorityRateSeqId"));
                createInvoiceItemContext.put("userLogin", userLogin);

                Map<String, Object> createInvoiceItemResult = null;
                try {
                    createInvoiceItemResult = dispatcher.runSync("createInvoiceItem", createInvoiceItemContext);
                } catch (GenericServiceException e) {
                    Debug.logError(e, "Service/other problem creating InvoiceItem from order header adjustment", MODULE);
                    return adjAmount;
                }
                if (ServiceUtil.isError(createInvoiceItemResult)) {
                    return adjAmount;
                }

                // Create the OrderAdjustmentBilling record
                Map<String, Object> createOrderAdjustmentBillingContext = new HashMap<>();
                createOrderAdjustmentBillingContext.put("orderAdjustmentId", adj.getString("orderAdjustmentId"));
                createOrderAdjustmentBillingContext.put("invoiceId", invoiceId);
                createOrderAdjustmentBillingContext.put("invoiceItemSeqId", invoiceItemSeqId);
                createOrderAdjustmentBillingContext.put("amount", amount);
                createOrderAdjustmentBillingContext.put("userLogin", userLogin);

                try {
                    Map<String, Object> result = dispatcher.runSync("createOrderAdjustmentBilling", createOrderAdjustmentBillingContext);
                    if (ServiceUtil.isError(result)) {
                        return adjAmount;
                    }
                } catch (GenericServiceException e) {
                    return adjAmount;
                }

            }
            amount = amount.setScale(decimals, rounding);
            adjAmount = amount;
        }

        Debug.logInfo("adjAmount: " + adjAmount + ", divisor: " + divisor + ", multiplier: " + multiplier
                + ", invoiceTypeId: " + invoiceTypeId + ", invoiceId: " + invoiceId + ", itemSeqId: " + invoiceItemSeqId
                + ", decimals: " + decimals + ", rounding: " + rounding + ", adj: " + adj, MODULE);
        return adjAmount;
    }

    /* Creates InvoiceTerm entries for a list of terms, which can be BillingAccountTerms, OrderTerms, etc. */
    private static void createInvoiceTerms(Delegator delegator, LocalDispatcher dispatcher, String invoiceId, List<GenericValue> terms,
                                           GenericValue userLogin, Locale locale) {
        if (terms != null) {
            for (GenericValue term : terms) {

                Map<String, Object> createInvoiceTermContext = new HashMap<>();
                createInvoiceTermContext.put("invoiceId", invoiceId);
                createInvoiceTermContext.put("invoiceItemSeqId", "_NA_");
                createInvoiceTermContext.put("termTypeId", term.get("termTypeId"));
                createInvoiceTermContext.put("termValue", term.get("termValue"));
                createInvoiceTermContext.put("termDays", term.get("termDays"));
                if (!"BillingAccountTerm".equals(term.getEntityName())) {
                    createInvoiceTermContext.put("textValue", term.get("textValue"));
                    createInvoiceTermContext.put("description", term.get("description"));
                }
                createInvoiceTermContext.put("uomId", term.get("uomId"));
                createInvoiceTermContext.put("userLogin", userLogin);

                Map<String, Object> createInvoiceTermResult = null;
                try {
                    createInvoiceTermResult = dispatcher.runSync("createInvoiceTerm", createInvoiceTermContext);
                } catch (GenericServiceException e) {
                    Debug.logError(e, "Service/other problem creating InvoiceItem from order header adjustment", MODULE);
                }
                if (ServiceUtil.isError(createInvoiceTermResult)) {
                    Debug.logError("Service/other problem creating InvoiceItem from order header adjustment", MODULE);
                }
            }
        }
    }

    /**
     * Service to add payment application records to indicate which invoices
     * have been paid/received. For invoice processing, this service works on
     * the invoice level when 'invoiceProcessing' parameter is set to "Y" else
     * it works on the invoice item level.
     */
    public static Map<String, Object> updatePaymentApplication(DispatchContext dctx, Map<String, Object> context) {
        if (!context.containsKey("useHighestAmount")) {
            context.put("useHighestAmount", "N");
        }
        BigDecimal amountApplied = (BigDecimal) context.get("amountApplied");
        if (amountApplied != null) {
            context.put("amountApplied", amountApplied);
        } else {
            context.put("amountApplied", BigDecimal.ZERO);
        }

        return updatePaymentApplicationDefBd(dctx, context);
    }

    /**
     * Service to add payment application records to indicate which invoices
     * have been paid/received. For invoice processing, this service works on
     * the invoice level when 'invoiceProcessing' parameter is set to "Y" else
     * it works on the invoice item level.
     * <p>
     * This version will apply as much as possible when no amountApplied is provided.
     */
    public static Map<String, Object> updatePaymentApplicationDef(DispatchContext dctx, Map<String, Object> context) {
        if (!context.containsKey("useHighestAmount")) {
            context.put("useHighestAmount", "Y");
        }
        return updatePaymentApplication(dctx, context);
    }

    public static Map<String, Object> updatePaymentApplicationDefBd(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get("locale");

        if (DECIMALS == -1 || ROUNDING == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    "AccountingAritmeticPropertiesNotConfigured", locale));
        }

        if (!context.containsKey("useHighestAmount")) {
            context.put("useHighestAmount", "Y");
        }

        String defaultInvoiceProcessing = EntityUtilProperties.getPropertyValue("accounting", "invoiceProcessing", delegator);

        boolean debug = true; // show processing messages in the log..or not....

        // a 'y' in invoiceProssesing will reverse the default processing
        String changeProcessing = (String) context.get("invoiceProcessing");
        String invoiceId = (String) context.get("invoiceId");
        String invoiceItemSeqId = (String) context.get("invoiceItemSeqId");
        String paymentId = (String) context.get("paymentId");
        String toPaymentId = (String) context.get("toPaymentId");
        String paymentApplicationId = (String) context.get("paymentApplicationId");
        BigDecimal amountApplied = (BigDecimal) context.get("amountApplied");
        String billingAccountId = (String) context.get("billingAccountId");
        String taxAuthGeoId = (String) context.get("taxAuthGeoId");
        String useHighestAmount = (String) context.get("useHighestAmount");

        List<String> errorMessageList = new LinkedList<>();

        if (debug) {
            Debug.logInfo("updatePaymentApplicationDefBd input parameters..."
                    + " defaultInvoiceProcessing: " + defaultInvoiceProcessing
                    + " changeDefaultInvoiceProcessing: " + changeProcessing
                    + " useHighestAmount: " + useHighestAmount
                    + " paymentApplicationId: " + paymentApplicationId
                    + " PaymentId: " + paymentId
                    + " InvoiceId: " + invoiceId
                    + " InvoiceItemSeqId: " + invoiceItemSeqId
                    + " BillingAccountId: " + billingAccountId
                    + " toPaymentId: " + toPaymentId
                    + " amountApplied: " + amountApplied
                    + " TaxAuthGeoId: " + taxAuthGeoId, MODULE);
        }

        if (changeProcessing == null) {
            changeProcessing = "N";    // not provided, so no change
        }

        boolean invoiceProcessing = true;
        if ("YY".equals(defaultInvoiceProcessing)) {
            invoiceProcessing = true;
        } else if ("NN".equals(defaultInvoiceProcessing)) {
            invoiceProcessing = false;
        } else if ("Y".equals(defaultInvoiceProcessing)) {
            invoiceProcessing = !"Y".equals(changeProcessing);
        } else if ("N".equals(defaultInvoiceProcessing)) {
            invoiceProcessing = "Y".equals(changeProcessing);
        }

        // on a new paymentApplication check if only billing or invoice or tax
        // id is provided not 2,3... BUT a combination of billingAccountId and invoiceId is permitted - that's how you use a
        // Billing Account to pay for an Invoice
        if (paymentApplicationId == null) {
            int count = 0;
            if (invoiceId != null) {
                count++;
            }
            if (toPaymentId != null) {
                count++;
            }
            if (billingAccountId != null) {
                count++;
            }
            if (taxAuthGeoId != null) {
                count++;
            }
            if ((billingAccountId != null) && (invoiceId != null)) {
                count--;
            }
            if (count != 1) {
                errorMessageList.add(UtilProperties.getMessage(RESOURCE, "AccountingSpecifyInvoiceToPaymentBillingAccountTaxGeoId", locale));
            }
        }

        // avoid null pointer exceptions.
        if (amountApplied == null) {
            amountApplied = BigDecimal.ZERO;
        }
        // makes no sense to have an item numer without an invoice number
        if (invoiceId == null) {
            invoiceItemSeqId = null;
        }

        // retrieve all information and perform checking on the retrieved info.....

        // Payment.....
        BigDecimal paymentApplyAvailable = BigDecimal.ZERO;
        // amount available on the payment reduced by the already applied amounts
        GenericValue payment = null;
        String currencyUomId = null;
        if (paymentId == null || "".equals(paymentId)) {
            errorMessageList.add(UtilProperties.getMessage(RESOURCE, "AccountingPaymentIdBlankNotSupplied", locale));
        } else {
            try {
                payment = EntityQuery.use(delegator).from("Payment").where("paymentId", paymentId).queryOne();
            } catch (GenericEntityException e) {
                return ServiceUtil.returnError(e.getMessage());
            }
            if (payment == null) {
                errorMessageList.add(UtilProperties.getMessage(RESOURCE,
                        "AccountingPaymentRecordNotFound", UtilMisc.toMap("paymentId", paymentId), locale));
                return ServiceUtil.returnError(errorMessageList);
            }
            paymentApplyAvailable = payment.getBigDecimal("amount").subtract(PaymentWorker.getPaymentApplied(payment)).setScale(DECIMALS, ROUNDING);

            if ("PMNT_CANCELLED".equals(payment.getString("statusId"))) {
                errorMessageList.add(UtilProperties.getMessage(RESOURCE,
                        "AccountingPaymentCancelled", UtilMisc.toMap("paymentId", paymentId), locale));
            }
            if ("PMNT_CONFIRMED".equals(payment.getString("statusId"))) {
                errorMessageList.add(UtilProperties.getMessage(RESOURCE,
                        "AccountingPaymentConfirmed", UtilMisc.toMap("paymentId", paymentId), locale));
            }

            currencyUomId = payment.getString("currencyUomId");

        }

        // the "TO" Payment.....
        BigDecimal toPaymentApplyAvailable = BigDecimal.ZERO;
        GenericValue toPayment = null;
        if (toPaymentId != null && !"".equals(toPaymentId)) {
            try {
                toPayment = EntityQuery.use(delegator).from("Payment").where("paymentId", toPaymentId).queryOne();
            } catch (GenericEntityException e) {
                return ServiceUtil.returnError(e.getMessage());
            }
            if (toPayment == null) {
                errorMessageList.add(UtilProperties.getMessage(RESOURCE,
                        "AccountingPaymentRecordNotFound", UtilMisc.toMap("paymentId", toPaymentId), locale));
                return ServiceUtil.returnError(errorMessageList);
            }
            toPaymentApplyAvailable = toPayment.getBigDecimal("amount").subtract(PaymentWorker.getPaymentApplied(toPayment)).setScale(DECIMALS,
                    ROUNDING);

            if ("PMNT_CANCELLED".equals(toPayment.getString("statusId"))) {
                errorMessageList.add(UtilProperties.getMessage(RESOURCE,
                        "AccountingPaymentCancelled", UtilMisc.toMap("paymentId", paymentId), locale));
            }
            if ("PMNT_CONFIRMED".equals(toPayment.getString("statusId"))) {
                errorMessageList.add(UtilProperties.getMessage(RESOURCE,
                        "AccountingPaymentConfirmed", UtilMisc.toMap("paymentId", paymentId), locale));
            }

            if (paymentApplicationId == null) {
                // only check for new application records, update on existing records is checked in the paymentApplication section
                if (toPaymentApplyAvailable.signum() == 0) {
                    errorMessageList.add(UtilProperties.getMessage(RESOURCE,
                            "AccountingPaymentAlreadyApplied", UtilMisc.toMap("paymentId", toPaymentId), locale));
                } else {
                    // check here for too much application if a new record is
                    // added (paymentApplicationId == null)
                    if (amountApplied.compareTo(toPaymentApplyAvailable) > 0) {
                        errorMessageList.add(UtilProperties.getMessage(RESOURCE,
                                "AccountingPaymentLessRequested",
                                UtilMisc.<String, Object>toMap("paymentId", toPaymentId,
                                        "paymentApplyAvailable", toPaymentApplyAvailable,
                                        "amountApplied", amountApplied, "isoCode", currencyUomId), locale));
                    }
                }
            }

            // check if at least one send is the same as one receiver on the other payment
            if (!payment.getString("partyIdFrom").equals(toPayment.getString("partyIdTo"))
                    && !payment.getString("partyIdTo").equals(toPayment.getString("partyIdFrom"))) {
                errorMessageList.add(UtilProperties.getMessage(RESOURCE,
                        "AccountingFromPartySameToParty", locale));
            }

            if (debug) {
                Debug.logInfo("toPayment info retrieved and checked...", MODULE);
            }
        }

        // assign payment to billing account if the invoice is assigned to this billing account
        if (invoiceId != null) {
            GenericValue invoice = null;
            try {
                invoice = EntityQuery.use(delegator).from("Invoice").where("invoiceId", invoiceId).queryOne();
            } catch (GenericEntityException e) {
                return ServiceUtil.returnError(e.getMessage());
            }

            if (invoice == null) {
                errorMessageList.add(UtilProperties.getMessage(RESOURCE,
                        "AccountingInvoiceNotFound", UtilMisc.toMap("invoiceId", invoiceId), locale));
            } else {
                if (invoice.getString("billingAccountId") != null) {
                    billingAccountId = invoice.getString("billingAccountId");
                }
            }
        }

        // billing account
        GenericValue billingAccount = null;
        if (billingAccountId != null && !"".equals(billingAccountId)) {
            try {
                billingAccount = EntityQuery.use(delegator).from("BillingAccount").where("billingAccountId", billingAccountId).queryOne();
            } catch (GenericEntityException e) {
                return ServiceUtil.returnError(e.getMessage());
            }
            if (billingAccount == null) {
                errorMessageList.add(UtilProperties.getMessage(RESOURCE,
                        "AccountingBillingAccountNotFound", UtilMisc.toMap("billingAccountId", billingAccountId), locale));
                return ServiceUtil.returnError(errorMessageList);
            }
            // check the currency
            if (billingAccount.get("accountCurrencyUomId") != null && currencyUomId != null
                    && !billingAccount.getString("accountCurrencyUomId").equals(currencyUomId)) {
                errorMessageList.add(UtilProperties.getMessage(RESOURCE, "AccountingBillingAccountCurrencyProblem",
                        UtilMisc.toMap("billingAccountId", billingAccountId,
                                "accountCurrencyUomId", billingAccount.getString("accountCurrencyUomId"),
                                "paymentId", paymentId, "paymentCurrencyUomId", currencyUomId), locale));
            }

            if (debug) {
                Debug.logInfo("Billing Account info retrieved and checked...", MODULE);
            }
        }

        // get the invoice (item) information
        BigDecimal invoiceApplyAvailable = BigDecimal.ZERO;
        // amount available on the invoice reduced by the already applied amounts
        BigDecimal invoiceItemApplyAvailable = BigDecimal.ZERO;
        // amount available on the invoiceItem reduced by the already applied amounts
        GenericValue invoice = null;
        GenericValue invoiceItem = null;
        if (invoiceId != null) {
            try {
                invoice = EntityQuery.use(delegator).from("Invoice").where("invoiceId", invoiceId).queryOne();
            } catch (GenericEntityException e) {
                return ServiceUtil.returnError(e.getMessage());
            }

            if (invoice == null) {
                errorMessageList.add(UtilProperties.getMessage(RESOURCE,
                        "AccountingInvoiceNotFound", UtilMisc.toMap("invoiceId", invoiceId), locale));
            } else { // check the invoice and when supplied the invoice item...

                if ("INVOICE_CANCELLED".equals(invoice.getString("statusId"))) {
                    errorMessageList.add(UtilProperties.getMessage(RESOURCE,
                            "AccountingInvoiceCancelledCannotApplyTo", UtilMisc.toMap("invoiceId", invoiceId), locale));
                }

                // check the currency
                if (currencyUomId != null && invoice.get("currencyUomId") != null
                        && !currencyUomId.equals(invoice.getString("currencyUomId"))) {
                    Debug.logInfo(UtilProperties.getMessage(RESOURCE, "AccountingInvoicePaymentCurrencyProblem",
                            UtilMisc.toMap("invoiceCurrency", invoice.getString("currencyUomId"), "paymentCurrency", payment.getString(
                                    "currencyUomId")), locale), MODULE);
                    Debug.logInfo("will try to apply payment on the actualCurrency amount on payment", MODULE);

                    if (payment.get("actualCurrencyAmount") == null || payment.get("actualCurrencyUomId") == null) {
                        errorMessageList.add("Actual amounts are required in the currency of the invoice to make this work....");
                    } else {
                        currencyUomId = payment.getString("actualCurrencyUomId");
                        if (!currencyUomId.equals(invoice.getString("currencyUomId"))) {
                            errorMessageList.add("actual currency on payment (" + currencyUomId + ") not the same as original invoice currency ("
                                    + invoice.getString("currencyUomId") + ")");
                        }
                    }
                    paymentApplyAvailable =
                            payment.getBigDecimal("actualCurrencyAmount").subtract(PaymentWorker.getPaymentApplied(payment))
                                    .setScale(DECIMALS, ROUNDING);
                }

                // check if the invoice already covered by payments
                BigDecimal invoiceTotal = InvoiceWorker.getInvoiceTotal(invoice);
                invoiceApplyAvailable = InvoiceWorker.getInvoiceNotApplied(invoice);

                if (invoiceTotal.signum() == 0) {
                    errorMessageList.add(UtilProperties.getMessage(RESOURCE,
                            "AccountingInvoiceTotalZero", UtilMisc.toMap("invoiceId", invoiceId), locale));
                } else if (paymentApplicationId == null) {
                    // only check for new records here...updates are checked in the paymentApplication section
                    if (invoiceApplyAvailable.signum() == 0) {
                        errorMessageList.add(UtilProperties.getMessage(RESOURCE,
                                "AccountingInvoiceCompletelyApplied", UtilMisc.toMap("invoiceId", invoiceId), locale));
                        // check here for too much application if a new record(s) are
                        // added (paymentApplicationId == null)
                    } else if (amountApplied.compareTo(invoiceApplyAvailable) > 0) {
                        errorMessageList.add(UtilProperties.getMessage(RESOURCE, "AccountingInvoiceLessRequested",
                                UtilMisc.<String, Object>toMap("invoiceId", invoiceId,
                                        "invoiceApplyAvailable", invoiceApplyAvailable,
                                        "amountApplied", amountApplied,
                                        "isoCode", invoice.getString("currencyUomId")), locale));
                    }
                }

                // check if at least one sender is the same as one receiver on the invoice
                if (!payment.getString("partyIdFrom").equals(invoice.getString("partyId"))
                        && !payment.getString("partyIdTo").equals(invoice.getString("partyIdFrom"))) {
                    errorMessageList.add(UtilProperties.getMessage(RESOURCE,
                            "AccountingFromPartySameToParty", locale));
                }

                if (debug) {
                    Debug.logInfo("Invoice info retrieved and checked ...", MODULE);
                }
            }

            // if provided check the invoice item.
            if (invoiceItemSeqId != null) {
                // when itemSeqNr not provided delay checking on invoiceItemSeqId
                try {
                    invoiceItem = EntityQuery.use(delegator).from("InvoiceItem").where("invoiceId", invoiceId, "invoiceItemSeqId",
                            invoiceItemSeqId).queryOne();
                } catch (GenericEntityException e) {
                    return ServiceUtil.returnError(e.getMessage());
                }

                if (invoiceItem == null) {
                    errorMessageList.add(UtilProperties.getMessage(RESOURCE,
                            "AccountingInvoiceItemNotFound",
                            UtilMisc.toMap("invoiceId", invoiceId, "invoiceItemSeqId", invoiceItemSeqId), locale));
                } else {
                    if (invoice.get("currencyUomId") != null && currencyUomId != null && !invoice.getString("currencyUomId").equals(currencyUomId)) {
                        errorMessageList.add(UtilProperties.getMessage(RESOURCE,
                                "AccountingInvoicePaymentCurrencyProblem",
                                UtilMisc.toMap("paymentCurrencyId", currencyUomId,
                                        "itemCurrency", invoice.getString("currencyUomId")), locale));
                    }

                    // get the invoice item applied value
                    BigDecimal quantity = null;
                    if (invoiceItem.get("quantity") == null) {
                        quantity = BigDecimal.ONE;
                    } else {
                        quantity = invoiceItem.getBigDecimal("quantity").setScale(DECIMALS, ROUNDING);
                    }
                    invoiceItemApplyAvailable =
                            invoiceItem.getBigDecimal("amount").multiply(quantity).setScale(DECIMALS, ROUNDING)
                                    .subtract(InvoiceWorker.getInvoiceItemApplied(invoiceItem));
                    // check here for too much application if a new record is added
                    if (paymentApplicationId == null && amountApplied.compareTo(invoiceItemApplyAvailable) > 0) {
                        // new record
                        errorMessageList.add("Invoice(" + invoiceId + ") item(" + invoiceItemSeqId + ") has  " + invoiceItemApplyAvailable + " to "
                                + "apply but " + amountApplied + " is requested\n");
                        String uomId = invoice.getString("currencyUomId");
                        errorMessageList.add(UtilProperties.getMessage(RESOURCE, "AccountingInvoiceItemLessRequested",
                                UtilMisc.<String, Object>toMap("invoiceId", invoiceId, "invoiceItemSeqId", invoiceItemSeqId,
                                        "invoiceItemApplyAvailable", invoiceItemApplyAvailable,
                                        "amountApplied", amountApplied, "isoCode", uomId), locale));
                    }
                }
                if (debug) {
                    Debug.logInfo("InvoiceItem info retrieved and checked against the Invoice (currency and amounts) ...", MODULE);
                }
            }
        }

        // check this at the end because the invoice can change the currency.......
        if (paymentApplicationId == null) {
            // only check for new application records, update on existing records is checked in the paymentApplication section
            if (paymentApplyAvailable.signum() == 0) {
                errorMessageList.add(UtilProperties.getMessage(RESOURCE,
                        "AccountingPaymentAlreadyApplied", UtilMisc.toMap("paymentId", paymentId), locale));
            } else {
                // check here for too much application if a new record is
                // added (paymentApplicationId == null)
                if (amountApplied.compareTo(paymentApplyAvailable) > 0) {
                    errorMessageList.add(UtilProperties.getMessage(RESOURCE, "AccountingPaymentLessRequested",
                            UtilMisc.<String, Object>toMap("paymentId", paymentId,
                                    "paymentApplyAvailable", paymentApplyAvailable,
                                    "amountApplied", amountApplied, "isoCode", currencyUomId), locale));
                }
            }
        }


        // get the application record if the applicationId is supplied if not
        // create empty record.
        BigDecimal newInvoiceApplyAvailable = invoiceApplyAvailable;
        // amount available on the invoice taking into account if the invoiceItemnumber has changed
        BigDecimal newInvoiceItemApplyAvailable = invoiceItemApplyAvailable;
        // amount available on the invoiceItem taking into account if the itemnumber has changed
        BigDecimal newToPaymentApplyAvailable = toPaymentApplyAvailable;
        BigDecimal newPaymentApplyAvailable = paymentApplyAvailable;
        GenericValue paymentApplication = null;
        if (paymentApplicationId == null) {
            paymentApplication = delegator.makeValue("PaymentApplication");
            // prepare for creation
        } else { // retrieve existing paymentApplication
            try {
                paymentApplication =
                        EntityQuery.use(delegator).from("PaymentApplication").where("paymentApplicationId", paymentApplicationId).queryOne();
            } catch (GenericEntityException e) {
                return ServiceUtil.returnError(e.getMessage());
            }

            if (paymentApplication == null) {
                errorMessageList.add(UtilProperties.getMessage(RESOURCE,
                        "AccountingPaymentApplicationNotFound",
                        UtilMisc.toMap("paymentApplicationId", paymentApplicationId), locale));
                paymentApplicationId = null;
            } else {

                // if both invoiceId and BillingId is entered there was
                // obviously a change
                // only take the newly entered item, same for tax authority and toPayment
                if (paymentApplication.get("invoiceId") == null && invoiceId != null) {
                    billingAccountId = null;
                    taxAuthGeoId = null;
                    toPaymentId = null;
                } else if (paymentApplication.get("toPaymentId") == null && toPaymentId != null) {
                    invoiceId = null;
                    invoiceItemSeqId = null;
                    taxAuthGeoId = null;
                    billingAccountId = null;
                } else if (paymentApplication.get("billingAccountId") == null && billingAccountId != null) {
                    invoiceId = null;
                    invoiceItemSeqId = null;
                    toPaymentId = null;
                    taxAuthGeoId = null;
                } else if (paymentApplication.get("taxAuthGeoId") == null && taxAuthGeoId != null) {
                    invoiceId = null;
                    invoiceItemSeqId = null;
                    toPaymentId = null;
                    billingAccountId = null;
                }

                // check if the payment for too much application if an existing
                // application record is changed
                if (paymentApplyAvailable.compareTo(BigDecimal.ZERO) == 0) {
                    newPaymentApplyAvailable =
                            paymentApplyAvailable.add(paymentApplication.getBigDecimal("amountApplied")).subtract(amountApplied).setScale(DECIMALS,
                                    ROUNDING);
                } else {
                    newPaymentApplyAvailable = paymentApplyAvailable.add(paymentApplyAvailable).subtract(amountApplied).setScale(DECIMALS, ROUNDING);
                }
                if (newPaymentApplyAvailable.compareTo(BigDecimal.ZERO) < 0) {
                    errorMessageList.add(UtilProperties.getMessage(RESOURCE,
                            "AccountingPaymentNotEnough",
                            UtilMisc.<String, Object>toMap("paymentId", paymentId,
                                    "paymentApplyAvailable", paymentApplyAvailable.add(paymentApplication.getBigDecimal("amountApplied")),
                                    "amountApplied", amountApplied), locale));
                }

                if (invoiceId != null) {
                    // only when we are processing an invoice on existing paymentApplication check invoice item for to much application if the invoice
                    // number did not change
                    if (invoiceId.equals(paymentApplication.getString("invoiceId"))) {
                        // check if both the itemNumbers are null then this is a
                        // record for the whole invoice
                        if (invoiceItemSeqId == null && paymentApplication.get("invoiceItemSeqId") == null) {
                            newInvoiceApplyAvailable =
                                    invoiceApplyAvailable.add(paymentApplication.getBigDecimal("amountApplied")).subtract(amountApplied)
                                            .setScale(DECIMALS, ROUNDING);
                            if (invoiceApplyAvailable.compareTo(BigDecimal.ZERO) < 0) {
                                errorMessageList.add(UtilProperties.getMessage(RESOURCE,
                                        "AccountingInvoiceNotEnough",
                                        UtilMisc.<String, Object>toMap("tooMuch", newInvoiceApplyAvailable.negate(),
                                                "invoiceId", invoiceId), locale));
                            }
                        } else if (invoiceItemSeqId == null && paymentApplication.get("invoiceItemSeqId") != null) {
                            // check if the item number changed from a real Item number to a null value
                            newInvoiceApplyAvailable =
                                    invoiceApplyAvailable.add(paymentApplication.getBigDecimal("amountApplied"))
                                            .subtract(amountApplied).setScale(DECIMALS, ROUNDING);
                            if (invoiceApplyAvailable.compareTo(BigDecimal.ZERO) < 0) {
                                errorMessageList.add(UtilProperties.getMessage(RESOURCE,
                                        "AccountingInvoiceNotEnough",
                                        UtilMisc.<String, Object>toMap("tooMuch", newInvoiceApplyAvailable.negate(),
                                                "invoiceId", invoiceId), locale));
                            }
                        } else if (paymentApplication.get("invoiceItemSeqId") == null) {
                            // check if the item number changed from a null value to
                            // a real Item number
                            newInvoiceItemApplyAvailable = invoiceItemApplyAvailable.subtract(amountApplied).setScale(DECIMALS, ROUNDING);
                            if (newInvoiceItemApplyAvailable.compareTo(BigDecimal.ZERO) < 0) {
                                errorMessageList.add(UtilProperties.getMessage(RESOURCE,
                                        "AccountingItemInvoiceNotEnough",
                                        UtilMisc.<String, Object>toMap("tooMuch", newInvoiceItemApplyAvailable.negate(),
                                                "invoiceId", invoiceId,
                                                "invoiceItemSeqId", invoiceItemSeqId), locale));
                            }
                        } else if (invoiceItemSeqId.equals(paymentApplication.getString("invoiceItemSeqId"))) {
                            // check if the real item numbers the same
                            // item number the same numeric value
                            newInvoiceItemApplyAvailable =
                                    invoiceItemApplyAvailable.add(paymentApplication.getBigDecimal("amountApplied"))
                                            .subtract(amountApplied).setScale(DECIMALS, ROUNDING);
                            if (newInvoiceItemApplyAvailable.compareTo(BigDecimal.ZERO) < 0) {
                                errorMessageList.add(UtilProperties.getMessage(RESOURCE,
                                        "AccountingItemInvoiceNotEnough",
                                        UtilMisc.<String, Object>toMap("tooMuch", newInvoiceItemApplyAvailable.negate(),
                                                "invoiceId", invoiceId,
                                                "invoiceItemSeqId", invoiceItemSeqId), locale));
                            }
                        } else {
                            // item number changed only check new item
                            newInvoiceItemApplyAvailable = invoiceItemApplyAvailable.add(amountApplied).setScale(DECIMALS, ROUNDING);
                            if (newInvoiceItemApplyAvailable.compareTo(BigDecimal.ZERO) < 0) {
                                errorMessageList.add(UtilProperties.getMessage(RESOURCE,
                                        "AccountingItemInvoiceNotEnough",
                                        UtilMisc.<String, Object>toMap("tooMuch", newInvoiceItemApplyAvailable.negate(),
                                                "invoiceId", invoiceId,
                                                "invoiceItemSeqId", invoiceItemSeqId), locale));
                            }
                        }

                        // if the amountApplied = 0 give it the higest possible
                        // value
                        if (amountApplied.signum() == 0) {
                            if (newInvoiceItemApplyAvailable.compareTo(newPaymentApplyAvailable) < 0) {
                                amountApplied = newInvoiceItemApplyAvailable;
                                // from the item number
                            } else {
                                amountApplied = newPaymentApplyAvailable;
                                // from the payment
                            }
                        }

                        // check the invoice
                        newInvoiceApplyAvailable =
                                invoiceApplyAvailable.add(paymentApplication.getBigDecimal("amountApplied")
                                        .subtract(amountApplied)).setScale(DECIMALS, ROUNDING);
                        if (newInvoiceApplyAvailable.compareTo(BigDecimal.ZERO) < 0) {
                            errorMessageList.add(UtilProperties.getMessage(RESOURCE,
                                    "AccountingInvoiceNotEnough",
                                    UtilMisc.<String, Object>toMap("tooMuch", invoiceApplyAvailable.add(paymentApplication.getBigDecimal(
                                            "amountApplied")).subtract(amountApplied),
                                            "invoiceId", invoiceId), locale));
                        }
                    }
                }

                // check the toPayment account when only the amountApplied has
                // changed,
                if (toPaymentId != null && toPaymentId.equals(paymentApplication.getString("toPaymentId"))) {
                    newToPaymentApplyAvailable =
                            toPaymentApplyAvailable.subtract(paymentApplication.getBigDecimal("amountApplied"))
                                    .add(amountApplied).setScale(DECIMALS, ROUNDING);
                    if (newToPaymentApplyAvailable.compareTo(BigDecimal.ZERO) < 0) {
                        errorMessageList.add(UtilProperties.getMessage(RESOURCE,
                                "AccountingPaymentNotEnough",
                                UtilMisc.<String, Object>toMap("paymentId", toPaymentId,
                                        "paymentApplyAvailable", newToPaymentApplyAvailable,
                                        "amountApplied", amountApplied), locale));
                    }
                } else if (toPaymentId != null) {
                    // billing account entered number has changed so we have to
                    // check the new billing account number.
                    newToPaymentApplyAvailable = toPaymentApplyAvailable.add(amountApplied).setScale(DECIMALS, ROUNDING);
                    if (newToPaymentApplyAvailable.compareTo(BigDecimal.ZERO) < 0) {
                        errorMessageList.add(UtilProperties.getMessage(RESOURCE,
                                "AccountingPaymentNotEnough",
                                UtilMisc.<String, Object>toMap("paymentId", toPaymentId,
                                        "paymentApplyAvailable", newToPaymentApplyAvailable,
                                        "amountApplied", amountApplied), locale));
                    }

                }
            }
            if (debug) {
                Debug.logInfo("paymentApplication record info retrieved and checked...", MODULE);
            }
        }

        // show the maximumus what can be added in the payment application file.
        String toMessage = null;  // prepare for success message
        if (debug) {
            String extra = "";
            if (invoiceItemSeqId != null) {
                extra = " Invoice item(" + invoiceItemSeqId + ") amount not yet applied: " + newInvoiceItemApplyAvailable;
            }
            Debug.logInfo("checking finished, start processing with the following data... ", MODULE);
            if (invoiceId != null) {
                Debug.logInfo(" Invoice(" + invoiceId + ") amount not yet applied: " + newInvoiceApplyAvailable + extra + " Payment("
                        + paymentId + ") amount not yet applied: " + newPaymentApplyAvailable + " Requested amount to apply:" + amountApplied,
                        MODULE);
                toMessage = UtilProperties.getMessage(RESOURCE,
                        "AccountingApplicationToInvoice",
                        UtilMisc.toMap("invoiceId", invoiceId), locale);
                if (!extra.isEmpty()) {
                    toMessage = UtilProperties.getMessage(RESOURCE,
                            "AccountingApplicationToInvoiceItem",
                            UtilMisc.toMap("invoiceId", invoiceId, "invoiceItemSeqId", invoiceItemSeqId), locale);
                }
            }
            if (toPaymentId != null) {
                Debug.logInfo(" toPayment(" + toPaymentId + ") amount not yet applied: " + newToPaymentApplyAvailable + " Payment(" + paymentId
                        + ") amount not yet applied: " + newPaymentApplyAvailable + " Requested amount to apply:" + amountApplied, MODULE);
                toMessage = UtilProperties.getMessage(RESOURCE,
                        "AccountingApplicationToPayment",
                        UtilMisc.toMap("paymentId", toPaymentId), locale);
            }
            if (taxAuthGeoId != null) {
                Debug.logInfo(" taxAuthGeoId(" + taxAuthGeoId + ")  Payment(" + paymentId + ") amount not yet applied: " + newPaymentApplyAvailable
                        + " Requested amount to apply:" + amountApplied, MODULE);
                toMessage = UtilProperties.getMessage(RESOURCE,
                        "AccountingApplicationToTax",
                        UtilMisc.toMap("taxAuthGeoId", taxAuthGeoId), locale);
            }
        }
        // if the amount to apply was not provided or was zero fill it with the maximum possible and provide information to the user
        if (amountApplied.signum() == 0 && "Y".equals(useHighestAmount)) {
            amountApplied = newPaymentApplyAvailable;
            if (invoiceId != null && newInvoiceApplyAvailable.compareTo(amountApplied) < 0) {
                amountApplied = newInvoiceApplyAvailable;
                toMessage = UtilProperties.getMessage(RESOURCE,
                        "AccountingApplicationToInvoice",
                        UtilMisc.toMap("invoiceId", invoiceId), locale);
            }
            if (toPaymentId != null && newToPaymentApplyAvailable.compareTo(amountApplied) < 0) {
                amountApplied = newToPaymentApplyAvailable;
                toMessage = UtilProperties.getMessage(RESOURCE,
                        "AccountingApplicationToPayment",
                        UtilMisc.toMap("paymentId", toPaymentId), locale);
            }
        }

        String successMessage = null;
        if (amountApplied.signum() == 0) {
            errorMessageList.add(UtilProperties.getMessage(RESOURCE, "AccountingNoAmount", locale));
        } else {
            successMessage = UtilProperties.getMessage(RESOURCE,
                    "AccountingApplicationSuccess",
                    UtilMisc.<String, Object>toMap("amountApplied", amountApplied,
                            "paymentId", paymentId,
                            "isoCode", currencyUomId,
                            "toMessage", toMessage), locale);
        }
        // report error messages if any
        if (!errorMessageList.isEmpty()) {
            return ServiceUtil.returnError(errorMessageList);
        }

        // ============ start processing ======================
        // if the application is specified it is easy, update the existing record only
        if (paymentApplicationId != null) {
            // record is already retrieved previously
            if (debug) {
                Debug.logInfo("Process an existing paymentApplication record: " + paymentApplicationId, MODULE);
            }
            // update the current record
            paymentApplication.set("invoiceId", invoiceId);
            paymentApplication.set("invoiceItemSeqId", invoiceItemSeqId);
            paymentApplication.set("paymentId", paymentId);
            paymentApplication.set("toPaymentId", toPaymentId);
            paymentApplication.set("amountApplied", amountApplied);
            paymentApplication.set("billingAccountId", billingAccountId);
            paymentApplication.set("taxAuthGeoId", taxAuthGeoId);
            return storePaymentApplication(delegator, paymentApplication, locale);
        }

        // if no invoice sequence number is provided it assumed the requested paymentAmount will be
        // spread over the invoice starting with the lowest sequence number if
        // itemprocessing is on otherwise create one record
        if (invoiceId != null && paymentId != null && (invoiceItemSeqId == null)) {
            if (invoiceProcessing) {
                // create only a single record with a null seqId
                if (debug) {
                    Debug.logInfo("Try to allocate the payment to the invoice as a whole", MODULE);
                }
                paymentApplication.set("paymentId", paymentId);
                paymentApplication.set("toPaymentId", null);
                paymentApplication.set("invoiceId", invoiceId);
                paymentApplication.set("invoiceItemSeqId", null);
                paymentApplication.set("toPaymentId", null);
                paymentApplication.set("amountApplied", amountApplied);
                paymentApplication.set("billingAccountId", billingAccountId);
                paymentApplication.set("taxAuthGeoId", null);
                if (debug) {
                    Debug.logInfo("creating new paymentapplication", MODULE);
                }
                return storePaymentApplication(delegator, paymentApplication, locale);
            }
            if (debug) {
                Debug.logInfo("Try to allocate the payment to the itemnumbers of the invoice", MODULE);
            }
            // get the invoice items
            List<GenericValue> invoiceItems = null;
            try {
                invoiceItems = EntityQuery.use(delegator).from("InvoiceItem").where("invoiceId", invoiceId).queryList();
            } catch (GenericEntityException e) {
                return ServiceUtil.returnError(e.getMessage());
            }
            if (invoiceItems.isEmpty()) {
                errorMessageList.add(UtilProperties.getMessage(RESOURCE, "AccountingNoInvoiceItemsFoundForInvoice", UtilMisc.toMap("invoiceId",
                        invoiceId), locale));
                return ServiceUtil.returnError(errorMessageList);
            }
            // check if the user want to apply a smaller amount than the maximum possible on the payment
            if (amountApplied.signum() != 0 && amountApplied.compareTo(paymentApplyAvailable) < 0) {
                paymentApplyAvailable = amountApplied;
            }
            for (GenericValue currentInvoiceItem : invoiceItems) {
                if (paymentApplyAvailable.compareTo(BigDecimal.ZERO) > 0) {
                    break;
                }
                if (debug) {
                    Debug.logInfo("Start processing item: " + currentInvoiceItem.getString("invoiceItemSeqId"), MODULE);
                }
                BigDecimal itemQuantity = BigDecimal.ONE;
                if (currentInvoiceItem.get("quantity") != null && currentInvoiceItem.getBigDecimal("quantity").signum() != 0) {
                    itemQuantity = new BigDecimal(currentInvoiceItem.getString("quantity")).setScale(DECIMALS, ROUNDING);
                }
                BigDecimal itemAmount = currentInvoiceItem.getBigDecimal("amount").setScale(DECIMALS, ROUNDING);
                BigDecimal itemTotal = itemAmount.multiply(itemQuantity).setScale(DECIMALS, ROUNDING);

                // get the application(s) already allocated to this
                // item, if available
                List<GenericValue> paymentApplications = null;
                try {
                    paymentApplications = currentInvoiceItem.getRelated("PaymentApplication", null, null, false);
                } catch (GenericEntityException e) {
                    return ServiceUtil.returnError(e.getMessage());
                }
                BigDecimal tobeApplied = BigDecimal.ZERO;
                // item total amount - already applied (if any)
                BigDecimal alreadyApplied = BigDecimal.ZERO;
                if (UtilValidate.isNotEmpty(paymentApplications)) {
                    // application(s) found, add them all together
                    Iterator<GenericValue> p = paymentApplications.iterator();
                    while (p.hasNext()) {
                        paymentApplication = p.next();
                        alreadyApplied = alreadyApplied.add(paymentApplication.getBigDecimal("amountApplied").setScale(DECIMALS, ROUNDING));
                    }
                    tobeApplied = itemTotal.subtract(alreadyApplied).setScale(DECIMALS, ROUNDING);
                } else {
                    // no application connected yet
                    tobeApplied = itemTotal;
                }
                if (debug) {
                    Debug.logInfo("tobeApplied:(" + tobeApplied + ") = " + "itemTotal(" + itemTotal + ") - alreadyApplied(" + alreadyApplied + ") "
                            + "but not more then (nonapplied) paymentAmount(" + paymentApplyAvailable + ")", MODULE);
                }

                if (tobeApplied.signum() == 0) {
                    // invoiceItem already fully applied so look at the next one....
                    continue;
                }

                if (paymentApplyAvailable.compareTo(tobeApplied) > 0) {
                    paymentApplyAvailable = paymentApplyAvailable.subtract(tobeApplied);
                } else {
                    tobeApplied = paymentApplyAvailable;
                    paymentApplyAvailable = BigDecimal.ZERO;
                }

                // create application payment record but check currency
                // first if supplied
                if (invoice.get("currencyUomId") != null && currencyUomId != null && !invoice.getString("currencyUomId").equals(currencyUomId)) {
                    errorMessageList.add("Payment currency (" + currencyUomId + ") and invoice currency(" + invoice.getString("currencyUomId") + ")"
                            + " not the same\n");
                } else {
                    paymentApplication.set("paymentApplicationId", null);
                    // make sure we get a new record
                    paymentApplication.set("invoiceId", invoiceId);
                    paymentApplication.set("invoiceItemSeqId", currentInvoiceItem.getString("invoiceItemSeqId"));
                    paymentApplication.set("paymentId", paymentId);
                    paymentApplication.set("toPaymentId", toPaymentId);
                    paymentApplication.set("amountApplied", tobeApplied);
                    paymentApplication.set("billingAccountId", billingAccountId);
                    paymentApplication.set("taxAuthGeoId", taxAuthGeoId);
                    storePaymentApplication(delegator, paymentApplication, locale);
                }

            }

            if (!errorMessageList.isEmpty()) {
                return ServiceUtil.returnError(errorMessageList);
            }
            if (successMessage != null) {
                return ServiceUtil.returnSuccess(successMessage);
            }
            return ServiceUtil.returnSuccess();
        }

        // if no paymentApplicationId supplied create a new record with the data
        // supplied...
        paymentApplication.set("paymentApplicationId", paymentApplicationId);
        paymentApplication.set("invoiceId", invoiceId);
        paymentApplication.set("invoiceItemSeqId", invoiceItemSeqId);
        paymentApplication.set("paymentId", paymentId);
        paymentApplication.set("toPaymentId", toPaymentId);
        paymentApplication.set("amountApplied", amountApplied);
        paymentApplication.set("billingAccountId", billingAccountId);
        paymentApplication.set("taxAuthGeoId", taxAuthGeoId);
        return storePaymentApplication(delegator, paymentApplication, locale);

    }

    public static Map<String, Object> calculateInvoicedAdjustmentTotal(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get("locale");
        GenericValue orderAdjustment = (GenericValue) context.get("orderAdjustment");
        Map<String, Object> result = ServiceUtil.returnSuccess();

        BigDecimal invoicedTotal = BigDecimal.ZERO;
        List<GenericValue> invoicedAdjustments = null;
        try {
            invoicedAdjustments = EntityQuery.use(delegator).from("OrderAdjustmentBilling").where("orderAdjustmentId",
                    orderAdjustment.get("orderAdjustmentId")).queryList();
        } catch (GenericEntityException e) {
            Debug.logError(e, "Accounting trouble calling calculateInvoicedAdjustmentTotal service", MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    "AccountingTroubleCallingCalculateInvoicedAdjustmentTotalService" + ": " + e.getMessage(), locale));
        }
        for (GenericValue invoicedAdjustment : invoicedAdjustments) {
            invoicedTotal = invoicedTotal.add(invoicedAdjustment.getBigDecimal("amount").setScale(DECIMALS, ROUNDING));
        }
        result.put("invoicedTotal", invoicedTotal);
        return result;
    }

    /**
     * Update/add to the paymentApplication table and making sure no duplicate
     * record exist
     * @param delegator
     * @param paymentApplication
     * @return map results
     */
    private static Map<String, Object> storePaymentApplication(Delegator delegator, GenericValue paymentApplication, Locale locale) {
        Map<String, Object> results = ServiceUtil.returnSuccess(UtilProperties.getMessage(RESOURCE,
                "AccountingSuccessful", locale));
        boolean debug = true;
        if (debug) {
            Debug.logInfo("Start updating the paymentApplication table ", MODULE);
        }

        if (DECIMALS == -1 || ROUNDING == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    "AccountingAritmeticPropertiesNotConfigured", locale));
        }

        // check if a record already exists with this data
        List<GenericValue> checkAppls = null;
        try {
            checkAppls = EntityQuery.use(delegator).from("PaymentApplication")
                    .where("invoiceId", paymentApplication.get("invoiceId"),
                            "invoiceItemSeqId", paymentApplication.get("invoiceItemSeqId"),
                            "billingAccountId", paymentApplication.get("billingAccountId"),
                            "paymentId", paymentApplication.get("paymentId"),
                            "toPaymentId", paymentApplication.get("toPaymentId"),
                            "taxAuthGeoId", paymentApplication.get("taxAuthGeoId"))
                    .queryList();
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(e.getMessage());
        }
        if (!checkAppls.isEmpty()) {
            if (debug) {
                Debug.logInfo(checkAppls.size() + " records already exist", MODULE);
            }
            // 1 record exists just update and if different ID delete other record and add together.
            GenericValue checkAppl = checkAppls.get(0);
            // if new record  add to the already existing one.
            if (paymentApplication.get("paymentApplicationId") == null) {
                // add 2 amounts together
                checkAppl.set("amountApplied", paymentApplication.getBigDecimal("amountApplied")
                        .add(checkAppl.getBigDecimal("amountApplied")).setScale(DECIMALS, ROUNDING));
                if (debug) {
                    Debug.logInfo("Update paymentApplication record: " + checkAppl.getString("paymentApplicationId") + " with appliedAmount:"
                            + checkAppl.getBigDecimal("amountApplied"), MODULE);
                }
                try {
                    checkAppl.store();
                } catch (GenericEntityException e) {
                    return ServiceUtil.returnError(e.getMessage());
                }
            } else if (paymentApplication.getString("paymentApplicationId").equals(checkAppl.getString("paymentApplicationId"))) {
                // update existing record in-place
                checkAppl.set("amountApplied", paymentApplication.getBigDecimal("amountApplied"));
                if (debug) {
                    Debug.logInfo("Update paymentApplication record: " + checkAppl.getString("paymentApplicationId") + " with appliedAmount:"
                            + checkAppl.getBigDecimal("amountApplied"), MODULE);
                }
                try {
                    checkAppl.store();
                } catch (GenericEntityException e) {
                    return ServiceUtil.returnError(e.getMessage());
                }
            } else { // two existing records, an updated one added to the existing one
                // add 2 amounts together
                checkAppl.set("amountApplied", paymentApplication.getBigDecimal("amountApplied")
                        .add(checkAppl.getBigDecimal("amountApplied")).setScale(DECIMALS, ROUNDING));
                // delete paymentApplication record and update the checkAppls one.
                if (debug) {
                    Debug.logInfo("Delete paymentApplication record: " + paymentApplication.getString("paymentApplicationId") + " with "
                            + "appliedAmount:" + paymentApplication.getBigDecimal("amountApplied"), MODULE);
                }
                try {
                    paymentApplication.remove();
                } catch (GenericEntityException e) {
                    return ServiceUtil.returnError(e.getMessage());
                }
                // update amount existing record
                if (debug) {
                    Debug.logInfo("Update paymentApplication record: " + checkAppl.getString("paymentApplicationId") + " with appliedAmount:"
                            + checkAppl.getBigDecimal("amountApplied"), MODULE);
                }
                try {
                    checkAppl.store();
                } catch (GenericEntityException e) {
                    return ServiceUtil.returnError(e.getMessage());
                }
            }
        } else {
            if (debug) {
                Debug.logInfo("No records found with paymentId, invoiceid..etc probaly changed one of them...", MODULE);
            }
            // create record if ID null;
            if (paymentApplication.get("paymentApplicationId") == null) {
                paymentApplication.set("paymentApplicationId", delegator.getNextSeqId("PaymentApplication"));
                if (debug) {
                    Debug.logInfo("Create new paymentAppication record: " + paymentApplication.getString("paymentApplicationId") + " with "
                            + "appliedAmount:" + paymentApplication.getBigDecimal("amountApplied"), MODULE);
                }
                try {
                    paymentApplication.create();
                } catch (GenericEntityException e) {
                    return ServiceUtil.returnError(e.getMessage());
                }
            } else {
                // update existing record (could not be found because a non existing combination of paymentId/invoiceId/invoiceSeqId/ etc... was
                // provided
                if (debug) {
                    Debug.logInfo("Update existing paymentApplication record: " + paymentApplication.getString("paymentApplicationId") + " with "
                            + "appliedAmount:" + paymentApplication.getBigDecimal("amountApplied"), MODULE);
                }
                try {
                    paymentApplication.store();
                } catch (GenericEntityException e) {
                    return ServiceUtil.returnError(e.getMessage());
                }
            }
        }
        return results;
    }

    public static Map<String, Object> checkPaymentInvoices(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        String paymentId = (String) context.get("paymentId");
        try {
            GenericValue payment = EntityQuery.use(delegator).from("Payment").where("paymentId", paymentId).queryOne();
            if (payment == null) {
                throw new GenericServiceException("Payment with ID [" + paymentId + "] not found!");
            }

            List<GenericValue> paymentApplications = payment.getRelated("PaymentApplication", null, null, false);
            if (UtilValidate.isEmpty(paymentApplications)) {
                return ServiceUtil.returnSuccess();
            }

            // TODO: this is inefficient -- instead use HashSet to construct a distinct Set of invoiceIds, then iterate over it and call
            // checkInvoicePaymentAppls
            for (GenericValue paymentApplication : paymentApplications) {
                String invoiceId = paymentApplication.getString("invoiceId");
                if (invoiceId != null) {
                    Map<String, Object> serviceResult = dispatcher.runSync("checkInvoicePaymentApplications", UtilMisc.<String, Object>toMap(
                            "invoiceId", invoiceId, "userLogin", userLogin));
                    if (ServiceUtil.isError(serviceResult)) {
                        return serviceResult;
                    }
                }
            }
            return ServiceUtil.returnSuccess();
        } catch (GenericServiceException | GenericEntityException se) {
            Debug.logError(se, se.getMessage(), MODULE);
            return ServiceUtil.returnError(se.getMessage());
        }
    }

    public static Map<String, Object> importInvoice(DispatchContext dctx, Map<String, Object> context) {
        Locale locale = (Locale) context.get("locale");
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        ByteBuffer fileBytes = (ByteBuffer) context.get("uploadedFile");
        if (fileBytes == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "AccountingUploadedFileDataNotFound", locale));
        }
        String organizationPartyId = (String) context.get("organizationPartyId");
        String encoding = System.getProperty("file.encoding");
        String csvString = Charset.forName(encoding).decode(fileBytes).toString();
        final BufferedReader csvReader = new BufferedReader(new StringReader(csvString));
        CSVFormat fmt = CSVFormat.DEFAULT.withHeader();
        List<String> errMsgs = new LinkedList<>();
        List<String> newErrMsgs;
        String lastInvoiceId = null;
        String currentInvoiceId = null;
        String newInvoiceId = null;
        int invoicesCreated = 0;

        try {
            for (final CSVRecord rec : fmt.parse(csvReader)) {
                currentInvoiceId = rec.get("invoiceId");
                if (lastInvoiceId == null || !currentInvoiceId.equals(lastInvoiceId)) {
                    newInvoiceId = null;
                    Map<String, Object> invoice = UtilMisc.toMap(
                            "invoiceTypeId", rec.get("invoiceTypeId"),
                            "partyIdFrom", rec.get("partyIdFrom"),
                            "partyId", rec.get("partyId"),
                            "invoiceDate", rec.get("invoiceDate"),
                            "dueDate", rec.get("dueDate"),
                            "currencyUomId", rec.get("currencyUomId"),
                            "description", rec.get("description"),
                            "referenceNumber", rec.get("referenceNumber") + "   Imported: orginal InvoiceId: " + currentInvoiceId,
                            "userLogin", userLogin);

                    // replace values if required
                    if (UtilValidate.isNotEmpty(rec.get("partyIdFromTrans"))) {
                        invoice.put("partyIdFrom", rec.get("partyIdFromTrans"));
                    }
                    if (UtilValidate.isNotEmpty(rec.get("partyIdTrans"))) {
                        invoice.put("partyId", rec.get("partyIdTrans"));
                    }

                    // invoice validation
                    newErrMsgs = new LinkedList<>();
                    try {
                        if (UtilValidate.isEmpty(invoice.get("partyIdFrom"))) {
                            newErrMsgs.add("Line number " + rec.getRecordNumber() + ": Mandatory Party Id From and Party Id From Trans missing for "
                                    + "invoice: " + currentInvoiceId);
                        } else if (EntityQuery.use(delegator).from("Party").where("partyId", invoice.get("partyIdFrom")).queryOne() == null) {
                            newErrMsgs.add("Line number " + rec.getRecordNumber() + ": partyIdFrom: " + invoice.get("partyIdFrom") + " not found "
                                    + "for invoice: " + currentInvoiceId);
                        }
                        if (UtilValidate.isEmpty(invoice.get("partyId"))) {
                            newErrMsgs.add("Line number " + rec.getRecordNumber() + ": Mandatory Party Id and Party Id Trans missing for invoice: "
                                    + currentInvoiceId);
                        } else if (EntityQuery.use(delegator).from("Party").where("partyId", invoice.get("partyId")).queryOne() == null) {
                            newErrMsgs.add("Line number " + rec.getRecordNumber() + ": partyId: " + invoice.get("partyId") + " not found for "
                                    + "invoice: " + currentInvoiceId);
                        }
                        if (UtilValidate.isEmpty(invoice.get("invoiceTypeId"))) {
                            newErrMsgs.add("Line number " + rec.getRecordNumber() + ": Mandatory Invoice Type missing for invoice: "
                                    + currentInvoiceId);
                        } else if (EntityQuery.use(delegator).from("InvoiceType").where("invoiceTypeId",
                                invoice.get("invoiceTypeId")).queryOne() == null) {
                            newErrMsgs.add("Line number " + rec.getRecordNumber() + ": InvoiceItem type id: " + invoice.get("invoiceTypeId") + " "
                                    + "not found for invoice: " + currentInvoiceId);
                        }

                        boolean isPurchaseInvoice = EntityTypeUtil.hasParentType(delegator, "InvoiceType", "invoiceTypeId", (String) invoice.get(
                                "invoiceTypeId"), "parentTypeId", "PURCHASE_INVOICE");
                        boolean isSalesInvoice = EntityTypeUtil.hasParentType(delegator, "InvoiceType", "invoiceTypeId", (String) invoice.get(
                                "invoiceTypeId"), "parentTypeId", "SALES_INVOICE");
                        if (isPurchaseInvoice && !invoice.get("partyId").equals(organizationPartyId)) {
                            newErrMsgs.add("Line number " + rec.getRecordNumber() + ": A purchase type invoice should have the partyId 'To' being "
                                    + "the organizationPartyId(=" + organizationPartyId + ")! however is " + invoice.get("partyId") + "! invoice: "
                                    + currentInvoiceId);
                        }
                        if (isSalesInvoice && !invoice.get("partyIdFrom").equals(organizationPartyId)) {
                            newErrMsgs.add("Line number " + rec.getRecordNumber() + ": A sales type invoice should have the partyId 'from' being "
                                    + "the organizationPartyId(=" + organizationPartyId + ")! however is " + invoice.get("partyIdFrom")
                                    + "! invoice: " + currentInvoiceId);
                        }


                    } catch (GenericEntityException e) {
                        Debug.logError("Valication checking problem against database. due to " + e.getMessage(), MODULE);
                    }

                    if (!newErrMsgs.isEmpty()) {
                        errMsgs.addAll(newErrMsgs);
                    } else {
                        Map<String, Object> invoiceResult = null;
                        try {
                            invoiceResult = dispatcher.runSync("createInvoice", invoice);
                            if (ServiceUtil.isError(invoiceResult)) {
                                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(invoiceResult));
                            }
                        } catch (GenericServiceException e) {
                            csvReader.close();
                            Debug.logError(e, MODULE);
                            return ServiceUtil.returnError(e.getMessage());
                        }
                        newInvoiceId = (String) invoiceResult.get("invoiceId");
                        invoicesCreated++;
                    }
                    lastInvoiceId = currentInvoiceId;
                }


                if (newInvoiceId != null) {
                    Map<String, Object> invoiceItem = UtilMisc.toMap(
                            "invoiceId", newInvoiceId,
                            "invoiceItemSeqId", rec.get("invoiceItemSeqId"),
                            "invoiceItemTypeId", rec.get("invoiceItemTypeId"),
                            "productId", rec.get("productId"),
                            "description", rec.get("itemDescription"),
                            "amount", rec.get("amount"),
                            "quantity", rec.get("quantity"),
                            "userLogin", userLogin);

                    if (UtilValidate.isNotEmpty(rec.get("productIdTrans"))) {
                        invoiceItem.put("productId", rec.get("productIdTrans"));
                    }
                    // invoice item validation
                    newErrMsgs = new LinkedList<>();
                    try {
                        if (UtilValidate.isEmpty(invoiceItem.get("invoiceItemSeqId"))) {
                            newErrMsgs.add("Line number " + rec.getRecordNumber() + ": Mandatory item sequence Id missing for invoice: "
                                    + currentInvoiceId);
                        }
                        if (UtilValidate.isEmpty(invoiceItem.get("invoiceItemTypeId"))) {
                            newErrMsgs.add("Line number " + rec.getRecordNumber() + ": Mandatory invoice item type missing for invoice: "
                                    + currentInvoiceId);
                        } else if (EntityQuery.use(delegator).from("InvoiceItemType").where("invoiceItemTypeId", invoiceItem.get("invoiceItemTypeId"
                        )).queryOne() == null) {
                            newErrMsgs.add("Line number " + rec.getRecordNumber() + ": InvoiceItem Item type id: " + invoiceItem.get(
                                    "invoiceItemTypeId") + " not found for invoice: " + currentInvoiceId + " Item seqId:" + invoiceItem.get(
                                            "invoiceItemSeqId"));
                        }
                        if (UtilValidate.isEmpty(invoiceItem.get("productId")) && UtilValidate.isEmpty(invoiceItem.get("description"))) {
                            newErrMsgs.add("Line number " + rec.getRecordNumber() + ": no Product Id given, no description given");
                        }
                        if (UtilValidate.isNotEmpty(invoiceItem.get("productId")) && EntityQuery.use(delegator).from("Product").where("productId",
                                invoiceItem.get("productId")).queryOne() == null) {
                            newErrMsgs.add("Line number " + rec.getRecordNumber() + ": Product Id: " + invoiceItem.get("productId") + " not found "
                                    + "for invoice: " + currentInvoiceId + " Item seqId:" + invoiceItem.get("invoiceItemSeqId"));
                        }
                        if (UtilValidate.isEmpty(invoiceItem.get("amount")) && UtilValidate.isEmpty(invoiceItem.get("quantity"))) {
                            newErrMsgs.add("Line number " + rec.getRecordNumber() + ": Either or both quantity and amount is required for invoice: "
                                    + currentInvoiceId + " Item seqId:" + invoiceItem.get("invoiceItemSeqId"));
                        }
                    } catch (GenericEntityException e) {
                        Debug.logError("Validation checking problem against database. due to " + e.getMessage(), MODULE);
                    }

                    if (!newErrMsgs.isEmpty()) {
                        errMsgs.addAll(newErrMsgs);
                    } else {
                        try {
                            Map<String, Object> result = dispatcher.runSync("createInvoiceItem", invoiceItem);
                            if (ServiceUtil.isError(result)) {
                                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(result));
                            }
                        } catch (GenericServiceException e) {
                            csvReader.close();
                            Debug.logError(e, MODULE);
                            return ServiceUtil.returnError(e.getMessage());
                        }
                    }
                }
            }

        } catch (IOException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }

        if (!errMsgs.isEmpty()) {
            return ServiceUtil.returnError(errMsgs);
        }

        Map<String, Object> result = ServiceUtil.returnSuccess(UtilProperties.getMessage(RESOURCE, "AccountingNewInvoicesCreated",
                UtilMisc.toMap("invoicesCreated", invoicesCreated), locale));
        result.put("organizationPartyId", organizationPartyId);
        return result;
    }
}
