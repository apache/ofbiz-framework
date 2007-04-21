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
package org.ofbiz.accounting.invoice;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javolution.util.FastList;

import javolution.util.FastMap;

import org.ofbiz.accounting.payment.BillingAccountWorker;
import org.ofbiz.accounting.payment.PaymentWorker;
import org.ofbiz.accounting.payment.PaymentGatewayServices;
import org.ofbiz.accounting.util.UtilAccounting;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilFormatOut;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilNumber;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.condition.EntityExpr;
import org.ofbiz.entity.condition.EntityConditionList;
import org.ofbiz.order.order.OrderReadHelper;
import org.ofbiz.product.product.ProductWorker;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceUtil;

/**
 * InvoiceServices - Services for creating invoices
 *
 * Note that throughout this file we use BigDecimal to do arithmetic. It is 
 * critical to understand the way BigDecimal works if you wish to modify the 
 * computations in this file. The most important things to keep in mind:
 *
 * Critically important: BigDecimal arithmetic methods like add(), 
 * multiply(), divide() do not modify the BigDecimal itself. Instead, they 
 * return a new BigDecimal. For example, to keep a running total of an 
 * amount, make sure you do this:
 *
 *      amount = amount.add(subAmount);
 *
 * and not this,
 *
 *      amount.add(subAmount);
 *
 * Use .setScale(scale, roundingMode) after every computation to scale and 
 * round off the decimals. Check the code to see how the scale and 
 * roundingMode are obtained and how the function is used.
 *
 * use .compareTo() to compare big decimals
 *
 *      ex.  (amountOne.compareTo(amountTwo) == 1) 
 *           checks if amountOne is greater than amountTwo
 *
 * Use .signum() to test if value is negative, zero, or positive
 *
 *      ex.  (amountOne.signum() == 1) 
 *           checks if the amount is a positive non-zero number
 *
 * Never use the .equals() function becaues it considers 2.0 not equal to 2.00 (the scale is different)
 * Instead, use .compareTo() or .signum(), which handles scale correctly.
 *
 * For reference, check the official Sun Javadoc on java.math.BigDecimal.
 */
public class InvoiceServices {

    public static String module = InvoiceServices.class.getName();

    // set some BigDecimal properties
    private static BigDecimal ZERO = new BigDecimal("0");
    private static int decimals = UtilNumber.getBigDecimalScale("invoice.decimals");
    private static int rounding = UtilNumber.getBigDecimalRoundingMode("invoice.rounding");
    private static int taxDecimals = UtilNumber.getBigDecimalScale("salestax.calc.decimals");
    private static int taxRounding = UtilNumber.getBigDecimalScale("salestax.rounding");
    public static final int taxCalcScale = UtilNumber.getBigDecimalScale("salestax.calc.decimals");
    private static final int INVOICE_ITEM_SEQUENCE_ID_DIGITS = 5; // this is the number of digits used for invoiceItemSeqId: 00001, 00002...

    public static final String resource = "AccountingUiLabels";

    // service to create an invoice for a complete order by the system userid
    public static Map createInvoiceForOrderAllItems(DispatchContext dctx, Map context) {
        GenericDelegator delegator = dctx.getDelegator();
        try {
            List orderItems = delegator.findByAnd("OrderItem", UtilMisc.toMap("orderId", (String) context.get("orderId")));
            if (orderItems != null && orderItems.size() > 0) {
                context.put("billItems", orderItems);
            }
            // get the system userid and store in context otherwise the invoice add service does not work
            GenericValue userLogin = delegator.findByPrimaryKey("UserLogin", UtilMisc.toMap("userLoginId", "system"));
            if (userLogin != null) {
                context.put("userLogin", userLogin);
            }
            
        } catch (GenericEntityException e) {
            String errMsg = UtilProperties.getMessage(resource,"AccountingEntityDataProblemCreatingInvoiceFromOrderItems",UtilMisc.toMap("reason",e.toString()),(Locale) context.get("locale"));
            Debug.logError(e, errMsg, module);
            return ServiceUtil.returnError(errMsg);
        }
        return createInvoiceForOrder(dctx, context);
    }

    /* Service to create an invoice for an order */
    public static Map createInvoiceForOrder(DispatchContext dctx, Map context) {
        GenericDelegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = (Locale) context.get("locale");

        if (decimals == -1 || rounding == -1) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource,"AccountingAritmeticPropertiesNotConfigured",locale));
        }

        String orderId = (String) context.get("orderId");
        List billItems = (List) context.get("billItems");
        boolean previousInvoiceFound = false;

        if (billItems == null || billItems.size() == 0) {
            Debug.logVerbose("No order items to invoice; not creating invoice; returning success", module);
            return ServiceUtil.returnSuccess(UtilProperties.getMessage(resource,"AccountingNoOrderItemsToInvoice",locale));
        }

        try {
            GenericValue orderHeader = delegator.findByPrimaryKey("OrderHeader", UtilMisc.toMap("orderId", orderId));
            if (orderHeader == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resource,"AccountingNoOrderHeader",locale));
            }

            // get list of previous invoices for the order
            List billedItems = delegator.findByAnd("OrderItemBilling", UtilMisc.toMap("orderId", orderId));
            if (billedItems != null && billedItems.size() > 0) {
                boolean nonDigitalInvoice = false;
                Iterator bii = billedItems.iterator();
                while (bii.hasNext() && !nonDigitalInvoice) {
                    GenericValue orderItemBilling = (GenericValue) bii.next();
                    GenericValue invoiceItem = orderItemBilling.getRelatedOne("InvoiceItem");
                    if (invoiceItem != null) {
                        String invoiceItemType = invoiceItem.getString("invoiceItemTypeId");
                        if (invoiceItemType != null) {
                            if ("INV_FPROD_ITEM".equals(invoiceItemType) || "INV_PROD_FEATR_ITEM".equals(invoiceItemType)) {
                                nonDigitalInvoice = true;
                            }
                        }
                    }
                }
                if (nonDigitalInvoice) {
                    previousInvoiceFound = true;
                }
            }

            // figure out the invoice type
            String invoiceType = null;

            String orderType = orderHeader.getString("orderTypeId");
            if (orderType.equals("SALES_ORDER")) {
                invoiceType = "SALES_INVOICE";
            } else if (orderType.equals("PURCHASE_ORDER")) {
                invoiceType = "PURCHASE_INVOICE";
            }

            // Make an order read helper from the order
            OrderReadHelper orh = new OrderReadHelper(orderHeader);

            // get the product store
            GenericValue productStore = delegator.findByPrimaryKey("ProductStore", UtilMisc.toMap("productStoreId", orh.getProductStoreId()));

            // get the shipping adjustment mode (Y = Pro-Rate; N = First-Invoice)
            String prorateShipping = productStore.getString("prorateShipping");
            if (prorateShipping == null) {
                prorateShipping = "Y";
            }

            // get the billing parties
            String billToCustomerPartyId = orh.getBillToParty().getString("partyId");
            String billFromVendorPartyId = orh.getBillFromParty().getString("partyId");

            // get some quantity totals
            BigDecimal totalItemsInOrder = orh.getTotalOrderItemsQuantityBd();

            // get some price totals
            BigDecimal shippableAmount = orh.getShippableTotalBd(null);
            BigDecimal orderSubTotal = orh.getOrderItemsSubTotalBd();

            // these variables are for pro-rating order amounts across invoices, so they should not be rounded off for maximum accuracy
            BigDecimal invoiceShipProRateAmount = ZERO;
            BigDecimal invoiceSubTotal = ZERO;
            BigDecimal invoiceQuantity = ZERO;

            GenericValue billingAccount = orderHeader.getRelatedOne("BillingAccount");
            String billingAccountId = billingAccount != null ? billingAccount.getString("billingAccountId") : null;

            // TODO: ideally this should be the same time as when a shipment is sent and be passed in as a parameter 
            Timestamp invoiceDate = UtilDateTime.nowTimestamp();
            // TODO: perhaps consider billing account net days term as well?
            Long orderTermNetDays = orh.getOrderTermNetDays();
            Timestamp dueDate = null;
            if (orderTermNetDays != null) {
                dueDate = UtilDateTime.getDayEnd(invoiceDate, orderTermNetDays.intValue());
            }
            
            // create the invoice record
            Map createInvoiceContext = FastMap.newInstance();
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
            Map createInvoiceResult = dispatcher.runSync("createInvoice", createInvoiceContext);
            if (ServiceUtil.isError(createInvoiceResult)) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resource,"AccountingErrorCreatingInvoiceFromOrder",locale), null, null, createInvoiceResult);
            }
            
            // call service, not direct entity op: delegator.create(invoice);
            String invoiceId = (String) createInvoiceResult.get("invoiceId");

            // order roles to invoice roles
            List orderRoles = orderHeader.getRelated("OrderRole");
            if (orderRoles != null) {
                Iterator orderRolesIt = orderRoles.iterator();
                Map createInvoiceRoleContext = FastMap.newInstance();
                createInvoiceRoleContext.put("invoiceId", invoiceId);
                createInvoiceRoleContext.put("userLogin", userLogin);
                while (orderRolesIt.hasNext()) {
                    GenericValue orderRole = (GenericValue)orderRolesIt.next();
                    createInvoiceRoleContext.put("partyId", orderRole.getString("partyId"));
                    createInvoiceRoleContext.put("roleTypeId", orderRole.getString("roleTypeId"));
                    Map createInvoiceRoleResult = dispatcher.runSync("createInvoiceRole", createInvoiceRoleContext);
                    if (ServiceUtil.isError(createInvoiceRoleResult)) {
                        return ServiceUtil.returnError(UtilProperties.getMessage(resource,"AccountingErrorCreatingInvoiceFromOrder",locale), null, null, createInvoiceRoleResult);
                    }
                }
            }

            // order terms to invoice terms.  Implemented for purchase orders, although it may be useful
            // for sales orders as well.  Later it might be nice to filter OrderTerms to only copy over financial terms.
            List orderTerms = orh.getOrderTerms();
            createInvoiceTerms(delegator, dispatcher, invoiceId, orderTerms, userLogin, locale);

            // billing accounts
            List billingAccountTerms = null;
            // for billing accounts we will use related information
            if (billingAccount != null) {
                // get the billing account terms
                billingAccountTerms = billingAccount.getRelated("BillingAccountTerm");

                // set the invoice terms as defined for the billing account
                createInvoiceTerms(delegator, dispatcher, invoiceId, billingAccountTerms, userLogin, locale);

                // set the invoice bill_to_customer from the billing account
                List billToRoles = billingAccount.getRelated("BillingAccountRole", UtilMisc.toMap("roleTypeId", "BILL_TO_CUSTOMER"), null);
                Iterator billToIter = billToRoles.iterator();
                while (billToIter.hasNext()) {
                    GenericValue billToRole = (GenericValue) billToIter.next();
                    if (!(billToRole.getString("partyId").equals(billToCustomerPartyId))) {
                        Map createInvoiceRoleContext = UtilMisc.toMap("invoiceId", invoiceId, "partyId", billToRole.get("partyId"), 
                                                                           "roleTypeId", "BILL_TO_CUSTOMER", "userLogin", userLogin);
                        Map createInvoiceRoleResult = dispatcher.runSync("createInvoiceRole", createInvoiceRoleContext);
                        if (ServiceUtil.isError(createInvoiceRoleResult)) {
                            return ServiceUtil.returnError(UtilProperties.getMessage(resource,"AccountingErrorCreatingInvoiceRoleFromOrder",locale), null, null, createInvoiceRoleResult);
                        }
                    }
                }

                // set the bill-to contact mech as the contact mech of the billing account
                if (UtilValidate.isNotEmpty(billingAccount.getString("contactMechId"))) {
                    Map createBillToContactMechContext = UtilMisc.toMap("invoiceId", invoiceId, "contactMechId", billingAccount.getString("contactMechId"), 
                                                                       "contactMechPurposeTypeId", "BILLING_LOCATION", "userLogin", userLogin);
                    Map createBillToContactMechResult = dispatcher.runSync("createInvoiceContactMech", createBillToContactMechContext);
                    if (ServiceUtil.isError(createBillToContactMechResult)) {
                        return ServiceUtil.returnError(UtilProperties.getMessage(resource,"AccountingErrorCreatingInvoiceContactMechFromOrder",locale), null, null, createBillToContactMechResult);
                    }
                }
            } else {
                List billingLocations = orh.getBillingLocations();
                if (billingLocations != null) {
                    Iterator bli = billingLocations.iterator();
                    while (bli.hasNext()) {
                        GenericValue ocm = (GenericValue) bli.next();
                        Map createBillToContactMechContext = UtilMisc.toMap("invoiceId", invoiceId, "contactMechId", ocm.getString("contactMechId"), 
                                                                           "contactMechPurposeTypeId", "BILLING_LOCATION", "userLogin", userLogin);
                        Map createBillToContactMechResult = dispatcher.runSync("createInvoiceContactMech", createBillToContactMechContext);
                        if (ServiceUtil.isError(createBillToContactMechResult)) {
                            return ServiceUtil.returnError(UtilProperties.getMessage(resource,"AccountingErrorCreatingInvoiceContactMechFromOrder",locale), null, null, createBillToContactMechResult);
                        }
                    }
                }
            }

            // get a list of the payment method types
            //DEJ20050705 doesn't appear to be used: List paymentPreferences = orderHeader.getRelated("OrderPaymentPreference");

            // create the bill-from (or pay-to) contact mech as the primary PAYMENT_LOCATION of the party from the store
            GenericValue payToAddress = null;
            if (invoiceType.equals("PURCHASE_INVOICE")) {
                // for purchase orders, the pay to address is the BILLING_LOCATION of the vendor
                GenericValue billFromVendor = orh.getPartyFromRole("BILL_FROM_VENDOR");
                if (billFromVendor != null) {
                    List billingContactMechs = billFromVendor.getRelatedOne("Party").getRelatedByAnd("PartyContactMechPurpose",
                            UtilMisc.toMap("contactMechPurposeTypeId", "BILLING_LOCATION"));
                    if ((billingContactMechs != null) && (billingContactMechs.size() > 0)) {
                        payToAddress = (GenericValue) billingContactMechs.get(0);
                    }
                }
            } else {
                // for sales orders, it is the payment address on file for the store
                payToAddress = PaymentWorker.getPaymentAddress(delegator, productStore.getString("payToPartyId"));
            }
            if (payToAddress != null) {
                Map createPayToContactMechContext = UtilMisc.toMap("invoiceId", invoiceId, "contactMechId", payToAddress.getString("contactMechId"), 
                                                                   "contactMechPurposeTypeId", "PAYMENT_LOCATION", "userLogin", userLogin);
                Map createPayToContactMechResult = dispatcher.runSync("createInvoiceContactMech", createPayToContactMechContext);
                if (ServiceUtil.isError(createPayToContactMechResult)) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(resource,"AccountingErrorCreatingInvoiceContactMechFromOrder",locale), null, null, createPayToContactMechResult);
                }
            }

            // sequence for items - all OrderItems or InventoryReservations + all Adjustments
            int invoiceItemSeqNum = 1;
            String invoiceItemSeqId = UtilFormatOut.formatPaddedNumber(invoiceItemSeqNum, INVOICE_ITEM_SEQUENCE_ID_DIGITS);

            // create the item records
            if (billItems != null) {
                Iterator itemIter = billItems.iterator();
                while (itemIter.hasNext()) {
                    GenericValue itemIssuance = null;
                    GenericValue orderItem = null;
                    GenericValue shipmentReceipt = null;
                    GenericValue currentValue = (GenericValue) itemIter.next();
                    if ("ItemIssuance".equals(currentValue.getEntityName())) {
                        itemIssuance = currentValue;
                    } else if ("OrderItem".equals(currentValue.getEntityName())) {
                        orderItem = currentValue;
                    } else if ("ShipmentReceipt".equals(currentValue.getEntityName())) {
                        shipmentReceipt = currentValue;
                    } else {
                        Debug.logError("Unexpected entity " + currentValue + " of type " + currentValue.getEntityName(), module);
                    }

                    if (orderItem == null && itemIssuance != null) {
                        orderItem = itemIssuance.getRelatedOne("OrderItem");
                    } else if ((orderItem == null) && (shipmentReceipt != null)) {
                        orderItem = shipmentReceipt.getRelatedOne("OrderItem");
                    } else if ((orderItem == null) && (itemIssuance == null) && (shipmentReceipt == null)) {
                        Debug.logError("Cannot create invoice when orderItem, itemIssuance, and shipmentReceipt are all null", module);
                        return ServiceUtil.returnError(UtilProperties.getMessage(resource,"AccountingIllegalValuesPassedToCreateInvoiceService",locale));
                    }
                    GenericValue product = null;
                    if (orderItem.get("productId") != null) {
                        product = orderItem.getRelatedOne("Product");
                    }

                    // get some quantities
                    BigDecimal orderedQuantity = orderItem.getBigDecimal("quantity");
                    BigDecimal billingQuantity = null;
                    if (itemIssuance != null) {
                        billingQuantity = itemIssuance.getBigDecimal("quantity");
                    } else if (shipmentReceipt != null) {
                        billingQuantity = shipmentReceipt.getBigDecimal("quantityAccepted");
                    } else {
                        billingQuantity = orderedQuantity;
                    }
                    if (orderedQuantity == null) orderedQuantity = ZERO;
                    if (billingQuantity == null) billingQuantity = ZERO;

                    // check if shipping applies to this item.  Shipping is calculated for sales invoices, not purchase invoices.
                    boolean shippingApplies = false;
                    if ((product != null) && (ProductWorker.shippingApplies(product)) && (invoiceType.equals("SALES_INVOICE"))) {
                        shippingApplies = true;
                    }

                    BigDecimal billingAmount = orderItem.getBigDecimal("unitPrice").setScale(decimals, rounding);
                    
                    Map createInvoiceItemContext = FastMap.newInstance();
                    createInvoiceItemContext.put("invoiceId", invoiceId);
                    createInvoiceItemContext.put("invoiceItemSeqId", invoiceItemSeqId);
                    createInvoiceItemContext.put("invoiceItemTypeId", getInvoiceItemType(delegator, (orderItem == null ? null : orderItem.getString("orderItemTypeId")), (product == null ? null : product.getString("productTypeId")), invoiceType, "INV_FPROD_ITEM"));
                    createInvoiceItemContext.put("description", orderItem.get("itemDescription"));
                    createInvoiceItemContext.put("quantity", new Double(billingQuantity.doubleValue()));
                    createInvoiceItemContext.put("amount", new Double(billingAmount.doubleValue()));
                    createInvoiceItemContext.put("productId", orderItem.get("productId"));
                    createInvoiceItemContext.put("productFeatureId", orderItem.get("productFeatureId"));
                    createInvoiceItemContext.put("overrideGlAccountId", orderItem.get("overrideGlAccountId"));
                    //createInvoiceItemContext.put("uomId", "");
                    createInvoiceItemContext.put("userLogin", userLogin);

                    String itemIssuanceId = null;
                    if (itemIssuance != null && itemIssuance.get("inventoryItemId") != null) {
                        itemIssuanceId = itemIssuance.getString("itemIssuanceId");
                        createInvoiceItemContext.put("inventoryItemId", itemIssuance.get("inventoryItemId"));
                    }
                    // similarly, tax only for purchase invoices
                    if ((product != null) && (invoiceType.equals("SALES_INVOICE"))) {
                        createInvoiceItemContext.put("taxableFlag", product.get("taxable"));
                    }

                    Map createInvoiceItemResult = dispatcher.runSync("createInvoiceItem", createInvoiceItemContext);
                    if (ServiceUtil.isError(createInvoiceItemResult)) {
                        return ServiceUtil.returnError(UtilProperties.getMessage(resource,"AccountingErrorCreatingInvoiceItemFromOrder",locale), null, null, createInvoiceItemResult);
                    }

                    // this item total
                    BigDecimal thisAmount = billingAmount.multiply(billingQuantity).setScale(decimals, rounding);

                    // add to the ship amount only if it applies to this item
                    if (shippingApplies) {
                        invoiceShipProRateAmount = invoiceShipProRateAmount.add(thisAmount).setScale(decimals, rounding);
                    }

                    // increment the invoice subtotal
                    invoiceSubTotal = invoiceSubTotal.add(thisAmount).setScale(100, rounding);

                    // increment the invoice quantity
                    invoiceQuantity = invoiceQuantity.add(billingQuantity).setScale(decimals, rounding);

                    // create the OrderItemBilling record
                    Map createOrderItemBillingContext = FastMap.newInstance();
                    createOrderItemBillingContext.put("invoiceId", invoiceId);
                    createOrderItemBillingContext.put("invoiceItemSeqId", invoiceItemSeqId);
                    createOrderItemBillingContext.put("orderId", orderItem.get("orderId"));
                    createOrderItemBillingContext.put("orderItemSeqId", orderItem.get("orderItemSeqId"));
                    createOrderItemBillingContext.put("itemIssuanceId", itemIssuanceId);
                    createOrderItemBillingContext.put("quantity", new Double(billingQuantity.doubleValue()));
                    createOrderItemBillingContext.put("amount", new Double(billingAmount.doubleValue()));
                    createOrderItemBillingContext.put("userLogin", userLogin);
                    if ((shipmentReceipt != null) && (shipmentReceipt.getString("receiptId") != null)) {
                        createOrderItemBillingContext.put("shipmentReceiptId", shipmentReceipt.getString("receiptId"));
                    }

                    Map createOrderItemBillingResult = dispatcher.runSync("createOrderItemBilling", createOrderItemBillingContext);
                    if (ServiceUtil.isError(createOrderItemBillingResult)) {
                        return ServiceUtil.returnError(UtilProperties.getMessage(resource,"AccountingErrorCreatingOrderItemBillingFromOrder",locale), null, null, createOrderItemBillingResult);
                    }

                    String parentInvoiceItemSeqId = invoiceItemSeqId;
                    // increment the counter
                    invoiceItemSeqNum++;
                    invoiceItemSeqId = UtilFormatOut.formatPaddedNumber(invoiceItemSeqNum, INVOICE_ITEM_SEQUENCE_ID_DIGITS);

                    // Get the original order item from the DB, in case the quantity has been overridden
                    GenericValue originalOrderItem = delegator.findByPrimaryKey("OrderItem", UtilMisc.toMap("orderId", orderId, "orderItemSeqId", orderItem.getString("orderItemSeqId")));

                    // create the item adjustment as line items
                    List itemAdjustments = OrderReadHelper.getOrderItemAdjustmentList(orderItem, orh.getAdjustments());
                    Iterator itemAdjIter = itemAdjustments.iterator();
                    while (itemAdjIter.hasNext()) {
                        GenericValue adj = (GenericValue) itemAdjIter.next();
                        
                        // Check against OrderAdjustmentBilling to see how much of this adjustment has already been invoiced
                        BigDecimal adjAlreadyInvoicedAmount = null;
                        try {
                            Map checkResult = dispatcher.runSync("calculateInvoicedAdjustmentTotal", UtilMisc.toMap("orderAdjustment", adj));
                            adjAlreadyInvoicedAmount = (BigDecimal) checkResult.get("invoicedTotal");
                        } catch (GenericServiceException e) {
                            String errMsg = UtilProperties.getMessage(resource, "AccountingTroubleCallingCalculateInvoicedAdjustmentTotalService", locale);
                            Debug.logError(e, errMsg, module);
                            return ServiceUtil.returnError(errMsg);
                        }
        
                        // If the absolute invoiced amount >= the abs of the adjustment amount, the full amount has already been invoiced,
                        //  so skip this adjustment
                        if (null == adj.get("amount")) { // JLR 17/4/7 : fix a bug coming from POS in case of use of a discount (on item(s) or sale, item(s) here) and a cash amount higher than total (hence issuing change)
                            continue;
                        }                        
                        if (adjAlreadyInvoicedAmount.abs().compareTo(adj.getBigDecimal("amount").setScale(decimals, rounding).abs()) > 0) {
                            continue;
                        }
        
                        BigDecimal amount = ZERO;
                        if (adj.get("amount") != null) { 
                            // pro-rate the amount
                            // set decimals = 100 means we don't round this intermediate value, which is very important
                            amount = adj.getBigDecimal("amount").divide(originalOrderItem.getBigDecimal("quantity"), 100, rounding);
                            amount = amount.multiply(billingQuantity);
                            amount = amount.setScale(decimals, rounding);
                        }
                        else if (adj.get("sourcePercentage") != null) { 
                            // pro-rate the amount
                            // set decimals = 100 means we don't round this intermediate value, which is very important
                            BigDecimal percent = adj.getBigDecimal("sourcePercentage");
                            percent = percent.divide(new BigDecimal(100), 100, rounding);
                            amount = billingAmount.multiply(percent); 
                            amount = amount.divide(originalOrderItem.getBigDecimal("quantity"), 100, rounding);
                            amount = amount.multiply(billingQuantity);
                            amount = amount.setScale(decimals, rounding);
                        }
                        if (amount.signum() != 0) {                      
                            Map createInvoiceItemAdjContext = FastMap.newInstance();
                            createInvoiceItemAdjContext.put("invoiceId", invoiceId);
                            createInvoiceItemAdjContext.put("invoiceItemSeqId", invoiceItemSeqId);
                            createInvoiceItemAdjContext.put("invoiceItemTypeId", getInvoiceItemType(delegator, adj.getString("orderAdjustmentTypeId"), null, invoiceType, "INVOICE_ITM_ADJ"));
                            createInvoiceItemAdjContext.put("description", adj.get("description"));
                            createInvoiceItemAdjContext.put("quantity", new Double(1));
                            createInvoiceItemAdjContext.put("amount", new Double(amount.doubleValue()));
                            createInvoiceItemAdjContext.put("productId", orderItem.get("productId"));
                            createInvoiceItemAdjContext.put("productFeatureId", orderItem.get("productFeatureId"));
                            createInvoiceItemAdjContext.put("overrideGlAccountId", adj.get("overrideGlAccountId"));
                            createInvoiceItemAdjContext.put("parentInvoiceId", invoiceId);
                            createInvoiceItemAdjContext.put("parentInvoiceItemSeqId", parentInvoiceItemSeqId);
                            //createInvoiceItemAdjContext.put("uomId", "");
                            createInvoiceItemAdjContext.put("userLogin", userLogin);
                            createInvoiceItemAdjContext.put("taxAuthPartyId", adj.get("taxAuthPartyId"));
                            createInvoiceItemAdjContext.put("taxAuthGeoId", adj.get("taxAuthGeoId"));
                            createInvoiceItemAdjContext.put("taxAuthorityRateSeqId", adj.get("taxAuthorityRateSeqId"));
        
                            // invoice items for sales tax are not taxable themselves
                            // TODO: This is not an ideal solution. Instead, we need to use OrderAdjustment.includeInTax when it is implemented
                            if (!(adj.getString("orderAdjustmentTypeId").equals("SALES_TAX"))) {
                                createInvoiceItemAdjContext.put("taxableFlag", product.get("taxable"));    
                            }
        
                            Map createInvoiceItemAdjResult = dispatcher.runSync("createInvoiceItem", createInvoiceItemAdjContext);
                            if (ServiceUtil.isError(createInvoiceItemAdjResult)) {
                                return ServiceUtil.returnError(UtilProperties.getMessage(resource,"AccountingErrorCreatingInvoiceItemFromOrder",locale), null, null, createInvoiceItemAdjResult);
                            }

                            // Create the OrderAdjustmentBilling record
                            Map createOrderAdjustmentBillingContext = FastMap.newInstance();
                            createOrderAdjustmentBillingContext.put("orderAdjustmentId", adj.getString("orderAdjustmentId"));
                            createOrderAdjustmentBillingContext.put("invoiceId", invoiceId);
                            createOrderAdjustmentBillingContext.put("invoiceItemSeqId", invoiceItemSeqId);
                            createOrderAdjustmentBillingContext.put("amount", new Double(amount.doubleValue()));
                            createOrderAdjustmentBillingContext.put("userLogin", userLogin);

                            Map createOrderAdjustmentBillingResult = dispatcher.runSync("createOrderAdjustmentBilling", createOrderAdjustmentBillingContext);
                            if (ServiceUtil.isError(createOrderAdjustmentBillingResult)) {
                                return ServiceUtil.returnError(UtilProperties.getMessage(resource,"AccountingErrorCreatingOrderAdjustmentBillingFromOrder",locale), null, null, createOrderAdjustmentBillingContext);
                            }

                            // this adjustment amount
                            BigDecimal thisAdjAmount = new BigDecimal(amount.doubleValue()).setScale(decimals, rounding);
    
                            // adjustments only apply to totals when they are not tax or shipping adjustments
                            if (!"SALES_TAX".equals(adj.getString("orderAdjustmentTypeId")) &&
                                    !"SHIPPING_ADJUSTMENT".equals(adj.getString("orderAdjustmentTypeId"))) {
                                // increment the invoice subtotal
                                invoiceSubTotal = invoiceSubTotal.add(thisAdjAmount).setScale(100, rounding);
    
                                // add to the ship amount only if it applies to this item
                                if (shippingApplies) {
                                    invoiceShipProRateAmount = invoiceShipProRateAmount.add(thisAdjAmount).setScale(decimals, rounding);
                                }
                            }
    
                            // increment the counter
                            invoiceItemSeqNum++;
                            invoiceItemSeqId = UtilFormatOut.formatPaddedNumber(invoiceItemSeqNum, INVOICE_ITEM_SEQUENCE_ID_DIGITS);
                        }
                    }
                }
            }

            // create header adjustments as line items -- always to tax/shipping last
            Map shipAdjustments = new HashMap();
            Map taxAdjustments = new HashMap();

            List headerAdjustments = orh.getOrderHeaderAdjustments();
            Iterator headerAdjIter = headerAdjustments.iterator();
            while (headerAdjIter.hasNext()) {
                GenericValue adj = (GenericValue) headerAdjIter.next();

                // Check against OrderAdjustmentBilling to see how much of this adjustment has already been invoiced
                BigDecimal adjAlreadyInvoicedAmount = null;
                try {
                    Map checkResult = dispatcher.runSync("calculateInvoicedAdjustmentTotal", UtilMisc.toMap("orderAdjustment", adj));
                    adjAlreadyInvoicedAmount = ((BigDecimal) checkResult.get("invoicedTotal")).setScale(decimals, rounding);
                } catch (GenericServiceException e) {
                    String errMsg = UtilProperties.getMessage(resource, "AccountingTroubleCallingCalculateInvoicedAdjustmentTotalService", locale);
                    Debug.logError(e, errMsg, module);
                    return ServiceUtil.returnError(errMsg);
                }

                // If the absolute invoiced amount >= the abs of the adjustment amount, the full amount has already been invoiced,
                //  so skip this adjustment
                if (null == adj.get("amount")) { // JLR 17/4/7 : fix a bug coming from POS in case of use of a discount (on item(s) or sale, sale here) and a cash amount higher than total (hence issuing change)
                    continue;
                }
                if (adjAlreadyInvoicedAmount.abs().compareTo(adj.getBigDecimal("amount").setScale(decimals, rounding).abs()) > 0) {
                    continue;
                }

                if ("SHIPPING_CHARGES".equals(adj.getString("orderAdjustmentTypeId"))) {
                    shipAdjustments.put(adj, adjAlreadyInvoicedAmount);
                } else if ("SALES_TAX".equals(adj.getString("orderAdjustmentTypeId"))) {
                    taxAdjustments.put(adj, adjAlreadyInvoicedAmount);
                } else {
                    // these will effect the shipping pro-rate (unless commented)
                    // other adjustment type
                    BigDecimal adjAmount = calcHeaderAdj(delegator, adj, invoiceType, invoiceId, invoiceItemSeqId, 
                            orderSubTotal, invoiceSubTotal, adj.getBigDecimal("amount").setScale(decimals, rounding), decimals, rounding, userLogin, dispatcher, locale);
                    // invoiceShipProRateAmount += adjAmount;
                    // do adjustments compound or are they based off subtotal? Here we will (unless commented)
                    // invoiceSubTotal += adjAmount;

                    // increment the counter
                    invoiceItemSeqNum++;
                    invoiceItemSeqId = UtilFormatOut.formatPaddedNumber(invoiceItemSeqNum, INVOICE_ITEM_SEQUENCE_ID_DIGITS);
                }
            }

            // next do the shipping adjustments.  Note that we do not want to add these to the invoiceSubTotal or orderSubTotal for pro-rating tax later, as that would cause
            // numerator/denominator problems when the shipping is not pro-rated but rather charged all on the first invoice
            Iterator shipAdjIter = shipAdjustments.keySet().iterator();
            while (shipAdjIter.hasNext()) {
                GenericValue adj = (GenericValue) shipAdjIter.next();
                BigDecimal adjAlreadyInvoicedAmount = (BigDecimal) shipAdjustments.get(adj);
                
                if ("N".equalsIgnoreCase(prorateShipping)) {

                    // Set the divisor and multiplier to 1 to avoid prorating
                    BigDecimal divisor = new BigDecimal("1");
                    BigDecimal multiplier = new BigDecimal("1");
                    
                    // The base amount in this case is the adjustment amount minus the total already invoiced for that adjustment, since
                    //  it won't be prorated
                    BigDecimal baseAmount = adj.getBigDecimal("amount").setScale(decimals, rounding).subtract(adjAlreadyInvoicedAmount);
                    BigDecimal adjAmount = calcHeaderAdj(delegator, adj, invoiceType, invoiceId, invoiceItemSeqId, 
                            divisor, multiplier, baseAmount, decimals, rounding, userLogin, dispatcher, locale);
                } else {

                    // Pro-rate the shipping amount based on shippable information
                    BigDecimal divisor = shippableAmount;
                    BigDecimal multiplier = invoiceShipProRateAmount;
                    
                    // The base amount in this case is the adjustment amount, since we want to prorate based on the full amount
                    BigDecimal baseAmount = adj.getBigDecimal("amount").setScale(decimals, rounding);
                    BigDecimal adjAmount = calcHeaderAdj(delegator, adj, invoiceType, invoiceId, invoiceItemSeqId, 
                            divisor, multiplier, baseAmount, decimals, rounding, userLogin, dispatcher, locale);
                }

                // Increment the counter
                invoiceItemSeqNum++;
                invoiceItemSeqId = UtilFormatOut.formatPaddedNumber(invoiceItemSeqNum, INVOICE_ITEM_SEQUENCE_ID_DIGITS);
            }

            // last do the tax adjustments
            String prorateTaxes = productStore.getString("prorateTaxes");
            if (prorateTaxes == null) {
                prorateTaxes = "Y";
            }            
            Iterator taxAdjIter = taxAdjustments.keySet().iterator();
            while (taxAdjIter.hasNext()) {
                GenericValue adj = (GenericValue) taxAdjIter.next();
                BigDecimal adjAlreadyInvoicedAmount = (BigDecimal) taxAdjustments.get(adj);
                BigDecimal adjAmount = null;
                
                if ("N".equalsIgnoreCase(prorateTaxes)) {

                    // Set the divisor and multiplier to 1 to avoid prorating
                    BigDecimal divisor = new BigDecimal("1");
                    BigDecimal multiplier = new BigDecimal("1");
                    
                    // The base amount in this case is the adjustment amount minus the total already invoiced for that adjustment, since
                    //  it won't be prorated
                    //  Note this should use invoice decimals & rounding instead of taxDecimals and taxRounding for tax adjustments, because it will be added to the invoice 
                    BigDecimal baseAmount = adj.getBigDecimal("amount").setScale(decimals, rounding).subtract(adjAlreadyInvoicedAmount);
                    adjAmount = calcHeaderAdj(delegator, adj, invoiceType, invoiceId, invoiceItemSeqId, 
                             divisor, multiplier, baseAmount, decimals, rounding, userLogin, dispatcher, locale);
                } else {

                    // Pro-rate the tax amount based on shippable information
                    BigDecimal divisor = orderSubTotal;
                    BigDecimal multiplier = invoiceSubTotal;
                    
                    // The base amount in this case is the adjustment amount, since we want to prorate based on the full amount
                    //  Note this should use invoice decimals & rounding instead of taxDecimals and taxRounding for tax adjustments, because it will be added to the invoice 
                    BigDecimal baseAmount = adj.getBigDecimal("amount").setScale(decimals, rounding);
                    adjAmount = calcHeaderAdj(delegator, adj, invoiceType, invoiceId, invoiceItemSeqId, 
                            divisor, multiplier, baseAmount, decimals, rounding, userLogin, dispatcher, locale);
                }
                invoiceSubTotal = invoiceSubTotal.add(adjAmount).setScale(decimals, rounding);                

                // Increment the counter
                invoiceItemSeqNum++;
                invoiceItemSeqId = UtilFormatOut.formatPaddedNumber(invoiceItemSeqNum, INVOICE_ITEM_SEQUENCE_ID_DIGITS);
            }

            // check for previous order payments
            List orderPaymentPrefs = delegator.findByAnd("OrderPaymentPreference", UtilMisc.toMap("orderId", orderId));
            if (orderPaymentPrefs != null) {
                List currentPayments = new ArrayList();
                Iterator opi = orderPaymentPrefs.iterator();
                while (opi.hasNext()) {
                    GenericValue paymentPref = (GenericValue) opi.next();
                    List payments = paymentPref.getRelated("Payment");
                    currentPayments.addAll(payments);
                }
                if (currentPayments.size() > 0) {
                    // apply these payments to the invoice; only if they haven't already been applied
                    Iterator cpi = currentPayments.iterator();
                    while (cpi.hasNext()) {
                        GenericValue payment = (GenericValue) cpi.next();
                        List currentApplications = null;
                        currentApplications = payment.getRelated("PaymentApplication");
                        if (currentApplications == null || currentApplications.size() == 0) {
                            // no applications; okay to apply
                            Map appl = new HashMap();
                            appl.put("paymentId", payment.get("paymentId"));
                            appl.put("invoiceId", invoiceId);
                            appl.put("billingAccountId", billingAccountId);
                            appl.put("amountApplied", payment.get("amount"));
                            appl.put("userLogin", userLogin);
                            Map createPayApplResult = dispatcher.runSync("createPaymentApplication", appl); 
                            if (ServiceUtil.isError(createPayApplResult)) {
                                return ServiceUtil.returnError(UtilProperties.getMessage(resource,"AccountingErrorCreatingInvoiceFromOrder",locale), null, null, createPayApplResult);
                            }
                        }
                    }
                }
            }

            // Should all be in place now. Depending on the ProductStore.autoApproveInvoice setting, set status to INVOICE_READY (unless it's a purchase 
            //  invoice, which we set to INVOICE_IN_PROCESS) 
            boolean autoApproveInvoice = UtilValidate.isEmpty(productStore.get("autoApproveInvoice")) || "Y".equals(productStore.getString("autoApproveInvoice"));
            if(autoApproveInvoice) {
                String nextStatusId = "INVOICE_READY";
                if (invoiceType.equals("PURCHASE_INVOICE")) {
                    nextStatusId = "INVOICE_IN_PROCESS";
                }
                Map setInvoiceStatusResult = dispatcher.runSync("setInvoiceStatus", UtilMisc.toMap("invoiceId", invoiceId, "statusId", nextStatusId, "userLogin", userLogin));
                if (ServiceUtil.isError(setInvoiceStatusResult)) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(resource,"AccountingErrorCreatingInvoiceFromOrder",locale), null, null, setInvoiceStatusResult);
                }
            }

            // check to see if we are all paid up
            Map checkResp = dispatcher.runSync("checkInvoicePaymentApplications", UtilMisc.toMap("invoiceId", invoiceId, "userLogin", userLogin));
            if (ServiceUtil.isError(checkResp)) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resource,"AccountingErrorCreatingInvoiceFromOrderCheckPaymentAppl",locale), null, null, checkResp);
            }

            Map resp = ServiceUtil.returnSuccess();
            resp.put("invoiceId", invoiceId);
            return resp;
        } catch (GenericEntityException e) {
            String errMsg = UtilProperties.getMessage(resource,"AccountingEntityDataProblemCreatingInvoiceFromOrderItems",UtilMisc.toMap("reason",e.toString()),locale);
            Debug.logError(e, errMsg, module);
            return ServiceUtil.returnError(errMsg);
        } catch (GenericServiceException e) {
            String errMsg = UtilProperties.getMessage(resource,"AccountingServiceOtherProblemCreatingInvoiceFromOrderItems",UtilMisc.toMap("reason",e.toString()),locale);
            Debug.logError(e, errMsg, module);
            return ServiceUtil.returnError(errMsg);
        }
    }
    
    // Service for creating commission invoices
    public static Map createCommissionInvoices(DispatchContext dctx, Map context) {
        GenericDelegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = (Locale) context.get("locale");
        List invoicesCreated = FastList.newInstance();
        
        String invoiceIdIn = (String) context.get("invoiceId");
        String invoiceItemSeqIdIn = (String) context.get("invoiceItemSeqId");
        BigDecimal amountTotal = InvoiceWorker.getInvoiceTotalBd(delegator, invoiceIdIn);
        // never use equals for BigDecimal - use either signum or compareTo 
        if (amountTotal.signum() == 0) {
            Debug.logWarning("Invoice [" + invoiceIdIn + "] has an amount total of [" + amountTotal + "], so no commission invoice will be created", module);
            return ServiceUtil.returnSuccess(UtilProperties.getMessage(resource,"AccountingInvoiceCommissionZeroInvoiceAmount",locale));
        }
        
        try {
            // Change this when amountApplied is BigDecimal, 18 digit scale to keep all the precision
            BigDecimal appliedFraction = new BigDecimal(((Double)context.get("amountApplied")).doubleValue()).divide(amountTotal, 12, rounding);
            Map inMap = UtilMisc.toMap("invoiceId", invoiceIdIn);
            GenericValue invoice = delegator.findByPrimaryKey("Invoice", inMap);
            String invoiceTypeId = invoice.getString("invoiceTypeId");
           
            // Determine sales or return
            boolean isReturn = false;
            if ("SALES_INVOICE".equals(invoiceTypeId)) {
                isReturn = false;
            } else if ("CUST_RTN_INVOICE".equals(invoiceTypeId)) {
                isReturn = true;
            } else {
                Debug.logWarning("This type of invoice has no commission; returning success", module);
                return ServiceUtil.returnSuccess(UtilProperties.getMessage(resource,"AccountingInvoiceCommissionInvalid",locale));
            }
            
            if (invoiceItemSeqIdIn != null) {
                inMap.put("invoiceItemSeqId", invoiceItemSeqIdIn);
            }
            List invoiceItems = delegator.findByAnd("InvoiceItem", inMap);
            
            // Map of commission Lists (of Maps) for each party
            Map commissionParties = FastMap.newInstance();
            // Determine commissions for various parties
            Iterator itemIter = invoiceItems.iterator();
            while (itemIter.hasNext()) {
                GenericValue invoiceItem = (GenericValue) itemIter.next();
                BigDecimal amount = ZERO;
                BigDecimal quantity = ZERO;
                quantity = invoiceItem.getBigDecimal("quantity");
                amount = invoiceItem.getBigDecimal("amount");
                amount = isReturn ? amount.negate() : amount;
                String productId = invoiceItem.getString("productId");
                
                // Determine commission parties for this invoiceItem
                if (productId != null && productId.length() > 0) {
                    Map outMap = dispatcher.runSync("getCommissionForProduct", UtilMisc.toMap(
                            "productId", productId,
                            "invoiceItemTypeId", invoiceItem.getString("invoiceItemTypeId"),
                            "amount", amount,
                            "quantity", quantity,
                            "userLogin", userLogin));
                    if (ServiceUtil.isError(outMap)) {
                        return outMap;
                    }
                    
                    // build a Map of partyIds (both to and from) in a commission and the amounts
                    // Note that getCommissionForProduct returns a List of Maps with a lot values.  See services.xml definition for reference.
                    List itemComms = (List) outMap.get("commissions");
                    if (itemComms != null && itemComms.size() > 0) {
                        Iterator it = itemComms.iterator();
                        while (it.hasNext()) {
                            Map commMap = (Map)it.next();
                            String partyIdFromTo = (String) commMap.get("partyIdFrom") + (String) commMap.get("partyIdTo");
                            if (!commissionParties.containsKey(partyIdFromTo)) {
                                commissionParties.put(partyIdFromTo, UtilMisc.toList(commMap));
                            } else {
                                ((List)commissionParties.get(partyIdFromTo)).add(commMap);
                            }
                        }
                    }
                }
            }
            
            String invoiceType = "COMMISSION_INVOICE";
            Timestamp now = UtilDateTime.nowTimestamp();
            
            // Create invoice for each commission receiving party
            Iterator it = commissionParties.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pair = (Map.Entry)it.next();
                List toStore = FastList.newInstance();
                List commList = (List)pair.getValue();
                // get the billing parties
                if (UtilValidate.isEmpty(commList)) {
                    continue;
                }
               
                // From and To are reversed between commission and invoice
                String partyIdBillTo = (String) ((Map)commList.get(0)).get("partyIdFrom");
                String partyIdBillFrom = (String) ((Map)commList.get(0)).get("partyIdTo");
                Long days = (Long) ((Map)commList.get(0)).get("days");
                
                // create the invoice record
                // To and From are in commission's sense, opposite for invoice
                Map createInvoiceContext = FastMap.newInstance();
                createInvoiceContext.put("partyId", partyIdBillTo);
                createInvoiceContext.put("partyIdFrom", partyIdBillFrom);
                createInvoiceContext.put("invoiceDate", now);
                // if there were days associated with the commission agreement, then set a dueDate for the invoice.
                if (days != null) {
                    createInvoiceContext.put("dueDate", UtilDateTime.getDayEnd(now, days.intValue()));
                }
                createInvoiceContext.put("invoiceTypeId", invoiceType);
                // start with INVOICE_IN_PROCESS, in the INVOICE_READY we can't change the invoice (or shouldn't be able to...)
                createInvoiceContext.put("statusId", "INVOICE_IN_PROCESS");
                createInvoiceContext.put("currencyUomId", invoice.getString("currencyUomId"));
                createInvoiceContext.put("userLogin", userLogin);
                
                // store the invoice first
                Map createInvoiceResult = dispatcher.runSync("createInvoice", createInvoiceContext);
                if (ServiceUtil.isError(createInvoiceResult)) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(resource,"AccountingInvoiceCommissionError",locale), null, null, createInvoiceResult);
                }
                String invoiceId = (String) createInvoiceResult.get("invoiceId");
                
                // create the bill-from (or pay-to) contact mech as the primary PAYMENT_LOCATION of the party from the store
                List contactMechs = delegator.findByAnd("PartyContactMechPurpose", UtilMisc.toMap("partyId", partyIdBillTo, "contactMechPurposeTypeId", "BILLING_LOCATION"));
                if ((contactMechs != null) && (contactMechs.size() > 0)) {
                    GenericValue address = (GenericValue) contactMechs.get(0);
                    GenericValue payToCm = delegator.makeValue("InvoiceContactMech", UtilMisc.toMap(
                            "invoiceId", invoiceId,
                            "contactMechId", address.getString("contactMechId"),
                            "contactMechPurposeTypeId", "BILLING_LOCATION"));
                    toStore.add(payToCm);
                }
                contactMechs = delegator.findByAnd("PartyContactMechPurpose", UtilMisc.toMap("partyId", partyIdBillFrom, "contactMechPurposeTypeId", "PAYMENT_LOCATION"));
                if ((contactMechs != null) && (contactMechs.size() > 0)) {
                    GenericValue address = (GenericValue) contactMechs.get(0);
                    GenericValue payToCm = delegator.makeValue("InvoiceContactMech", UtilMisc.toMap(
                            "invoiceId", invoiceId,
                            "contactMechId", address.getString("contactMechId"),
                            "contactMechPurposeTypeId", "PAYMENT_LOCATION"));
                    toStore.add(payToCm);
                }
                
                // create the item records
                Iterator itt = commList.iterator();
                while (itt.hasNext()) {
                    Map elem = (Map) itt.next();
                    BigDecimal elemAmount = ((BigDecimal)elem.get("commission")).multiply(appliedFraction);
                    elemAmount = elemAmount.setScale(decimals, rounding);
                    Map resMap = dispatcher.runSync("createInvoiceItem", UtilMisc.toMap(
                            "invoiceId", invoiceId,
                            "productId", elem.get("productId"),
                            "invoiceItemTypeId", "COMM_INV_ITEM",
                            "amount", new Double(elemAmount.doubleValue()),
                            "userLogin", userLogin));
                    if (ServiceUtil.isError(resMap)) {
                        return ServiceUtil.returnError(UtilProperties.getMessage(resource,"AccountingInvoiceCommissionErrorItem",locale), null, null, resMap);
                    }
                }
                // store value objects
                delegator.storeAll(toStore);
                invoicesCreated.add(invoiceId);
            }
            Map resp = ServiceUtil.returnSuccess();
            resp.put("invoicesCreated", invoicesCreated);
            return resp;
        } catch (GenericEntityException e) {
            String errMsg = UtilProperties.getMessage(resource,"AccountingInvoiceCommissionEntityDataProblem",UtilMisc.toMap("reason",e.toString()),locale);
            Debug.logError(e, errMsg, module);
            return ServiceUtil.returnError(errMsg);
        } catch (GenericServiceException e) {
            String errMsg = UtilProperties.getMessage(resource,"AccountingInvoiceCommissionEntityDataProblem",UtilMisc.toMap("reason",e.toString()),locale);
            Debug.logError(e, errMsg, module);
            return ServiceUtil.returnError(errMsg);
        }
    }
    
    public static Map readyInvoices(DispatchContext dctx, Map context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = (Locale) context.get("locale");
        // Get invoices to make ready
        List invoicesCreated = (List) context.get("invoicesCreated");
        String nextStatusId = "INVOICE_READY";
        Iterator it = invoicesCreated.iterator();
        try {
            while (it.hasNext()) {
                String invoiceId = (String) it.next();
                Map setInvoiceStatusResult = dispatcher.runSync("setInvoiceStatus", UtilMisc.toMap("invoiceId", invoiceId, "statusId", nextStatusId, "userLogin", userLogin));
                if (ServiceUtil.isError(setInvoiceStatusResult)) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(resource,"AccountingInvoiceCommissionError",locale), null, null, setInvoiceStatusResult);
                }
            }
        } catch (GenericServiceException e) {
            String errMsg = UtilProperties.getMessage(resource,"AccountingInvoiceCommissionEntityDataProblem",UtilMisc.toMap("reason",e.toString()),locale);
            Debug.logError(e, errMsg, module);
            return ServiceUtil.returnError(errMsg);
        }
        return ServiceUtil.returnSuccess();
    }
    
    public static Map createInvoicesFromShipment(DispatchContext dctx, Map context) {
        //GenericDelegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        String shipmentId = (String) context.get("shipmentId");
        Locale locale = (Locale) context.get("locale");
        List invoicesCreated = new ArrayList();
        
        Map serviceContext = UtilMisc.toMap("shipmentIds", UtilMisc.toList(shipmentId), "userLogin", context.get("userLogin"));
        try {
            Map result = dispatcher.runSync("createInvoicesFromShipments", serviceContext);
            invoicesCreated = (List) result.get("invoicesCreated");
        } catch (GenericServiceException e) {
            Debug.logError(e, "Trouble calling createInvoicesFromShipment service; invoice not created for shipment [" + shipmentId + "]", module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource,"AccountingTroubleCallingCreateInvoicesFromShipmentService",UtilMisc.toMap("shipmentId",shipmentId),locale));
        }
        Map response = ServiceUtil.returnSuccess();
        response.put("invoicesCreated", invoicesCreated);
        return response;
    }
    
    public static Map createSalesInvoicesFromDropShipment(DispatchContext dctx, Map context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        String shipmentId = (String) context.get("shipmentId");
        Locale locale = (Locale) context.get("locale");

        Map serviceContext = UtilMisc.toMap("shipmentIds", UtilMisc.toList(shipmentId), "createSalesInvoicesForDropShipments", Boolean.TRUE, "userLogin", context.get("userLogin"));

        Map serviceResult;
        try {
            serviceResult = dispatcher.runSync("createInvoicesFromShipments", serviceContext);
        } catch (GenericServiceException e) {
            String errorMessage = UtilProperties.getMessage(resource, "AccountingTroubleCallingCreateInvoicesFromShipmentService", UtilMisc.toMap("shipmentId", shipmentId), locale);
            Debug.logError(e, errorMessage, module);
            return ServiceUtil.returnError(errorMessage);
        }
        
        return serviceResult;
    }
        
    public static Map createInvoicesFromShipments(DispatchContext dctx, Map context) {
        GenericDelegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        List shipmentIds = (List) context.get("shipmentIds");
        Locale locale = (Locale) context.get("locale");
        Boolean createSalesInvoicesForDropShipments = (Boolean) context.get("createSalesInvoicesForDropShipments");
        if (UtilValidate.isEmpty(createSalesInvoicesForDropShipments)) createSalesInvoicesForDropShipments = Boolean.FALSE;

        boolean salesShipmentFound = false;
        boolean purchaseShipmentFound = false;
        boolean dropShipmentFound = false;
        
        List invoicesCreated = new ArrayList();

        //DEJ20060520: not used? planned to be used? List shipmentIdList = new LinkedList();
        for (int i = 0; i < shipmentIds.size(); i++) {
            String tmpShipmentId = (String)shipmentIds.get(i);
            try {
                GenericValue shipment = delegator.findByPrimaryKey("Shipment", UtilMisc.toMap("shipmentId", tmpShipmentId));
                if ((shipment.getString("shipmentTypeId") != null) && (shipment.getString("shipmentTypeId").equals("PURCHASE_SHIPMENT"))) {
                    purchaseShipmentFound = true;
                } else if ((shipment.getString("shipmentTypeId") != null) && (shipment.getString("shipmentTypeId").equals("DROP_SHIPMENT"))) {
                    dropShipmentFound = true;
                } else {
                    salesShipmentFound = true;
                }
                if (purchaseShipmentFound && salesShipmentFound && dropShipmentFound) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(resource,"AccountingShipmentsOfDifferentTypes",UtilMisc.toMap("tmpShipmentId",tmpShipmentId,"shipmentTypeId",shipment.getString("shipmentTypeId")),locale));
                }
            } catch (GenericEntityException e) {
                String errMsg = UtilProperties.getMessage(resource, "AccountingTroubleGettingShipmentEntity",UtilMisc.toMap("tmpShipmentId",tmpShipmentId), locale);
                Debug.logError(e, errMsg, module);
                return ServiceUtil.returnError(errMsg);
            }
        }
        EntityCondition shipmentIdsCond = new EntityExpr("shipmentId", EntityOperator.IN, shipmentIds);
        // check the status of the shipment

        // get the items of the shipment.  They can come from ItemIssuance if the shipment were from a sales order, ShipmentReceipt
        // if it were a purchase order or from the order items of the (possibly linked) orders if the shipment is a drop shipment
        List items = null;
        List orderItemAssocs = null;
        try {
            if (purchaseShipmentFound) {
                items = delegator.findByCondition("ShipmentReceipt", shipmentIdsCond, null, UtilMisc.toList("shipmentId"));
            } else if (dropShipmentFound) {

                List shipments = delegator.findByCondition("Shipment", shipmentIdsCond, null, null);
                
                // Get the list of purchase order IDs related to the shipments
                List purchaseOrderIds = EntityUtil.getFieldListFromEntityList(shipments, "primaryOrderId", true);
    
                if (createSalesInvoicesForDropShipments.booleanValue()) {
                
                    // If a sales invoice is being created for a drop shipment, we have to reference the original sales order items
                    // Get the list of the linked orderIds (original sales orders)
                    orderItemAssocs = delegator.findByCondition("OrderItemAssoc", new EntityExpr("toOrderId", EntityOperator.IN, purchaseOrderIds), null, null);
    
                    // Get only the order items which are indirectly related to the purchase order - this limits the list to the drop ship group(s)
                    items = EntityUtil.getRelated("FromOrderItem", orderItemAssocs);
                } else {

                    // If it's a purchase invoice being created, the order items for that purchase orders can be used directly
                    items = delegator.findByCondition("OrderItem", new EntityExpr("orderId", EntityOperator.IN, purchaseOrderIds), null, null);
                }
            } else {
                items = delegator.findByCondition("ItemIssuance", shipmentIdsCond, null, UtilMisc.toList("shipmentId"));
            }
        } catch (GenericEntityException e) {
            String errMsg = UtilProperties.getMessage(resource, "AccountingProblemGettingItemsFromShipments", locale);
            Debug.logError(e, errMsg, module);
            return ServiceUtil.returnError(errMsg);
        }
        if (items == null) {
            Debug.logInfo("No items issued for shipments", module);
            return ServiceUtil.returnSuccess();
        }

        // group items by order
        Map shippedOrderItems = new HashMap();
        Iterator itemsIter = items.iterator();
        while (itemsIter.hasNext()) {
            GenericValue item = (GenericValue) itemsIter.next();
            String orderId = item.getString("orderId");
            String orderItemSeqId = item.getString("orderItemSeqId");
            List itemsByOrder = (List) shippedOrderItems.get(orderId);
            if (itemsByOrder == null) {
                itemsByOrder = new ArrayList();
            }

            // check and make sure we haven't already billed for this issuance or shipment receipt
            Map billFields = UtilMisc.toMap("orderId", orderId, "orderItemSeqId", orderItemSeqId);
            if (dropShipmentFound) {

                // Drop shipments have neither issuances nor receipts, so this check is meaningless
                itemsByOrder.add(item);
                shippedOrderItems.put(orderId, itemsByOrder);
                continue;
            } else if (item.getEntityName().equals("ItemIssuance")) {
                billFields.put("itemIssuanceId", item.get("itemIssuanceId"));
            } else if (item.getEntityName().equals("ShipmentReceipt")) {
                billFields.put("shipmentReceiptId", item.getString("receiptId"));
            }
            List itemBillings = null;
            try {
                itemBillings = delegator.findByAnd("OrderItemBilling", billFields);
            } catch (GenericEntityException e) {
                String errMsg = UtilProperties.getMessage(resource, "AccountingProblemLookingUpOrderItemBilling",UtilMisc.toMap("billFields",billFields), locale);
                Debug.logError(e, errMsg, module);
                return ServiceUtil.returnError(errMsg);
            }

            // if none found, then okay to bill
            if (itemBillings == null || itemBillings.size() == 0) {
                itemsByOrder.add(item);
            }

            // update the map with modified list
            shippedOrderItems.put(orderId, itemsByOrder);
        }

        // make sure we aren't billing items already invoiced i.e. items billed as digital (FINDIG)
        Set orders = shippedOrderItems.keySet();
        Iterator ordersIter = orders.iterator();
        while (ordersIter.hasNext()) {
            String orderId = (String) ordersIter.next();

            // we'll only use this list to figure out which ones to send
            List billItems = (List) shippedOrderItems.get(orderId);

            // a new list to be used to pass to the create invoice service
            List toBillItems = new ArrayList();

            // map of available quantities so we only have to calc once
            Map itemQtyAvail = new HashMap();

            // now we will check each issuance and make sure it hasn't already been billed
            Iterator billIt = billItems.iterator();
            while (billIt.hasNext()) {
                GenericValue issue = (GenericValue) billIt.next();
                BigDecimal issueQty = ZERO;

                if (issue.getEntityName().equals("ShipmentReceipt")) {
                    issueQty = issue.getBigDecimal("quantityAccepted");
                } else {
                    issueQty = issue.getBigDecimal("quantity");
                }

                BigDecimal billAvail = (BigDecimal) itemQtyAvail.get(issue.getString("orderItemSeqId"));
                if (billAvail == null) {
                    Map lookup = UtilMisc.toMap("orderId", orderId, "orderItemSeqId", issue.get("orderItemSeqId"));
                    GenericValue orderItem = null;
                    List billed = null;
                    BigDecimal orderedQty = null;
                    try {
                        orderItem = issue.getEntityName().equals("OrderItem") ? issue : issue.getRelatedOne("OrderItem");

                        // total ordered
                        orderedQty = orderItem.getBigDecimal("quantity");

                        if (dropShipmentFound && createSalesInvoicesForDropShipments.booleanValue()) {
                            
                            // Override the issueQty with the quantity from the purchase order item
                            GenericValue orderItemAssoc = EntityUtil.getFirst(EntityUtil.filterByAnd(orderItemAssocs, UtilMisc.toMap("orderId", issue.getString("orderId"), "orderItemSeqId", issue.getString("orderItemSeqId"))));
                            GenericValue purchaseOrderItem = orderItemAssoc.getRelatedOne("ToOrderItem");
                            orderItem.set("quantity", purchaseOrderItem.getDouble("quantity"));
                            issueQty = purchaseOrderItem.getBigDecimal("quantity");
                        }

                        billed = delegator.findByAnd("OrderItemBilling", lookup);
                    } catch (GenericEntityException e) {
                        String errMsg = UtilProperties.getMessage(resource, "AccountingProblemGettingOrderItemOrderItemBilling",UtilMisc.toMap("lookup",lookup), locale);
                        Debug.logError(e, errMsg, module);
                        return ServiceUtil.returnError(errMsg);
                    }


                    // add up the already billed total
                    if (billed != null && billed.size() > 0) {
                        BigDecimal billedQuantity = ZERO;
                        Iterator bi = billed.iterator();
                        while (bi.hasNext()) {
                            GenericValue oib = (GenericValue) bi.next();
                            BigDecimal qty = oib.getBigDecimal("quantity");
                            if (qty != null) {
                                billedQuantity = billedQuantity.add(qty).setScale(decimals, rounding);
                            }
                        }
                        BigDecimal leftToBill = orderedQty.subtract(billedQuantity).setScale(decimals, rounding);
                        billAvail = leftToBill;
                    } else {
                        billAvail = orderedQty;
                    }
                }

                // no available means we cannot bill anymore
                if (billAvail != null && billAvail.signum() == 1) { // this checks if billAvail is a positive non-zero number
                    if (issueQty != null && issueQty.doubleValue() > billAvail.doubleValue()) {
                        // can only bill some of the issuance; others have been billed already
                        issue.set("quantity", new Double(billAvail.doubleValue()));
                        billAvail = ZERO;
                    } else {
                        // now have been billed
                        billAvail = billAvail.subtract(issueQty).setScale(decimals, rounding);
                    }

                    // okay to bill these items; but none else
                    toBillItems.add(issue);
                }

                // update the available to bill quantity for the next pass
                itemQtyAvail.put(issue.getString("orderItemSeqId"), billAvail);
            }

            OrderReadHelper orh = new OrderReadHelper(delegator, orderId);
            GenericValue productStore = orh.getProductStore();

            // If shipping charges are not prorated, the shipments need to be examined for additional shipping charges
            if (productStore.getString("prorateShipping").equals("N")) {
    
                // Get the set of filtered shipments
                List invoiceableShipments = null;
                try {
                    if (dropShipmentFound) {
                        
                        List invoiceablePrimaryOrderIds = null;
                        if (createSalesInvoicesForDropShipments.booleanValue()) {
                        
                            // If a sales invoice is being created for the drop shipment, we need to reference back to the original purchase order IDs

                            // Get the IDs for orders which have billable items
                            List invoiceableLinkedOrderIds = EntityUtil.getFieldListFromEntityList(toBillItems, "orderId", true);

                            // Get back the IDs of the purchase orders - this will be a list of the purchase order items which are billable by virtue of not having been
                            //  invoiced in a previous sales invoice
                            List reverseOrderItemAssocs = EntityUtil.filterByCondition(orderItemAssocs, new EntityExpr("orderId", EntityOperator.IN, invoiceableLinkedOrderIds));
                            invoiceablePrimaryOrderIds = EntityUtil.getFieldListFromEntityList(reverseOrderItemAssocs, "toOrderId", true);
                            
                        } else {
        
                            // If a purchase order is being created for a drop shipment, the purchase order IDs can be used directly
                            invoiceablePrimaryOrderIds = EntityUtil.getFieldListFromEntityList(toBillItems, "orderId", true);

                        }

                        // Get the list of shipments which are associated with the filtered purchase orders
                        if (! UtilValidate.isEmpty(invoiceablePrimaryOrderIds)) {
                            List invoiceableShipmentConds = UtilMisc.toList(
                                    new EntityExpr("primaryOrderId", EntityOperator.IN, invoiceablePrimaryOrderIds),
                                    new EntityExpr("shipmentId", EntityOperator.IN, shipmentIds));
                            invoiceableShipments = delegator.findByCondition("Shipment", new EntityConditionList(invoiceableShipmentConds, EntityOperator.AND), null, null);
                        }
                    } else {
                        List invoiceableShipmentIds = EntityUtil.getFieldListFromEntityList(toBillItems, "shipmentId", true);
                        if (! UtilValidate.isEmpty(invoiceableShipmentIds)) {
                            invoiceableShipments = delegator.findByCondition("Shipment", new EntityExpr("shipmentId", EntityOperator.IN, invoiceableShipmentIds), null, null);
                        }
                    }
                } catch( GenericEntityException e ) {
                    String errMsg = UtilProperties.getMessage(resource, "AccountingTroubleCallingCreateInvoicesFromShipmentsService", locale);
                    Debug.logError(e, errMsg, module);
                    return ServiceUtil.returnError(errMsg);
                }
                
                // Total the additional shipping charges for the shipments
                Map additionalShippingCharges = new HashMap();
                BigDecimal totalAdditionalShippingCharges = ZERO;
                if (! UtilValidate.isEmpty(invoiceableShipments)) {
                    Iterator isit = invoiceableShipments.iterator();
                    while(isit.hasNext()) {
                        GenericValue shipment = (GenericValue) isit.next();
                        if (shipment.get("additionalShippingCharge") == null) continue;
                        BigDecimal shipmentAdditionalShippingCharges = shipment.getBigDecimal("additionalShippingCharge").setScale(decimals, rounding);
                        additionalShippingCharges.put(shipment, shipmentAdditionalShippingCharges);
                        totalAdditionalShippingCharges = totalAdditionalShippingCharges.add(shipmentAdditionalShippingCharges);
                    }
                }
                
                // If the additional shipping charges are greater than zero, process them
                if (totalAdditionalShippingCharges.signum() == 1) {

                    // Add an OrderAdjustment to the order for each additional shipping charge
                    Iterator ascit = additionalShippingCharges.keySet().iterator();
                    while (ascit.hasNext()) {
                        GenericValue shipment = (GenericValue) ascit.next();
                        String shipmentId = shipment.getString("shipmentId");
                        BigDecimal additionalShippingCharge = (BigDecimal) additionalShippingCharges.get(shipment);
                        Map createOrderAdjustmentContext = new HashMap();
                        createOrderAdjustmentContext.put("orderId", orderId);
                        createOrderAdjustmentContext.put("orderAdjustmentTypeId", "SHIPPING_CHARGES");
                        createOrderAdjustmentContext.put("description", UtilProperties.getMessage(resource, "AccountingAdditionalShippingChargeForShipment", locale) + " #" + shipmentId);
                        createOrderAdjustmentContext.put("sourceReferenceId", shipmentId);
                        createOrderAdjustmentContext.put("amount", new Double(additionalShippingCharge.doubleValue()));
                        createOrderAdjustmentContext.put("userLogin", context.get("userLogin"));
                        String shippingOrderAdjustmentId = null;
                        try {
                            Map createOrderAdjustmentResult = dispatcher.runSync("createOrderAdjustment", createOrderAdjustmentContext);
                            shippingOrderAdjustmentId = (String) createOrderAdjustmentResult.get("orderAdjustmentId");
                        } catch (GenericServiceException e) {
                            String errMsg = UtilProperties.getMessage(resource, "AccountingTroubleCallingCreateOrderAdjustmentService", locale);
                            Debug.logError(e, errMsg, module);
                            return ServiceUtil.returnError(errMsg);
                        }

                        // Obtain a list of OrderAdjustments due to tax on the shipping charges, if any
                        GenericValue billToParty = orh.getBillToParty();
                        GenericValue payToParty = orh.getBillFromParty();
                        GenericValue destinationContactMech = null;
                        try {
                            destinationContactMech = shipment.getRelatedOne("DestinationPostalAddress");
                        } catch( GenericEntityException e ) {
                            String errMsg = UtilProperties.getMessage(resource, "AccountingTroubleCallingCreateInvoicesFromShipmentService", locale);
                            Debug.logError(e, errMsg, module);
                            return ServiceUtil.returnError(errMsg);
                        }
                        
                        List emptyList = new ArrayList();
                        Map calcTaxContext = new HashMap();
                        calcTaxContext.put("productStoreId", orh.getProductStoreId());
                        calcTaxContext.put("payToPartyId", payToParty.getString("partyId"));
                        calcTaxContext.put("billToPartyId", billToParty.getString("partyId"));
                        calcTaxContext.put("orderShippingAmount", totalAdditionalShippingCharges);
                        calcTaxContext.put("shippingAddress", destinationContactMech);

                        // These parameters don't matter if we're only worried about adjustments on the shipping charges
                        calcTaxContext.put("itemProductList", emptyList);
                        calcTaxContext.put("itemAmountList", emptyList);
                        calcTaxContext.put("itemPriceList", emptyList);
                        calcTaxContext.put("itemShippingList", emptyList);

                        List orderAdjustments = null;
                        Map calcTaxResult = null;
                        try {
                            calcTaxResult = dispatcher.runSync("calcTax", calcTaxContext);
                        } catch (GenericServiceException e) {
                            String errMsg = UtilProperties.getMessage(resource, "AccountingTroubleCallingCalcTaxService", locale);
                            Debug.logError(e, errMsg, module);
                            return ServiceUtil.returnError(errMsg);
                        }
                        orderAdjustments = (List) calcTaxResult.get("orderAdjustments");

                        // If we have any OrderAdjustments due to tax on shipping, store them and add them to the total
                        if (calcTaxResult != null && orderAdjustments != null) {
                            Iterator oait = orderAdjustments.iterator();
                            while (oait.hasNext()) {
                                GenericValue orderAdjustment = (GenericValue) oait.next();
                                totalAdditionalShippingCharges = totalAdditionalShippingCharges.add(orderAdjustment.getBigDecimal("amount").setScale(decimals, rounding));
                                orderAdjustment.set("orderAdjustmentId", delegator.getNextSeqId("OrderAdjustment"));
                                orderAdjustment.set("orderId", orderId);
                                orderAdjustment.set("orderItemSeqId", "_NA_");
                                orderAdjustment.set("shipGroupSeqId", shipment.getString("primaryShipGroupSeqId"));
                                orderAdjustment.set("originalAdjustmentId", shippingOrderAdjustmentId);                                
                            }
                            try {
                                delegator.storeAll(orderAdjustments);
                            } catch( GenericEntityException e ) {
                                String errMsg = UtilProperties.getMessage(resource, "AccountingProblemStoringOrderAdjustments", UtilMisc.toMap("orderAdjustments", orderAdjustments), locale);
                                Debug.logError(e, errMsg, module);
                                return ServiceUtil.returnError(errMsg);
                            }
                        }

                        // If part of the order was paid via credit card, try to charge it for the additional shipping
                        List orderPaymentPreferences = new ArrayList();
                        try {
                            orderPaymentPreferences = delegator.findByAnd("OrderPaymentPreference", UtilMisc.toMap("orderId", orderId, "paymentMethodTypeId", "CREDIT_CARD"));
                        } catch( GenericEntityException e ) {
                            String errMsg = UtilProperties.getMessage(resource, "AccountingProblemGettingOrderPaymentPreferences", locale);
                            Debug.logError(e, errMsg, module);
                            return ServiceUtil.returnError(errMsg);
                        }

                        //  Use the first credit card we find, for the sake of simplicity
                        String paymentMethodId = null;
                        GenericValue cardOrderPaymentPref = EntityUtil.getFirst(orderPaymentPreferences);
                        if (cardOrderPaymentPref != null) {
                            paymentMethodId = cardOrderPaymentPref.getString("paymentMethodId");
                        }
                        
                        if (paymentMethodId != null ) {

                            // Release all outstanding (not settled or cancelled) authorizations, while keeping a running
                            //  total of their amounts so that the total plus the additional shipping charges can be authorized again
                            //  all at once.
                            BigDecimal totalNewAuthAmount = new BigDecimal(totalAdditionalShippingCharges.doubleValue()).setScale(decimals, rounding);
                            Iterator oppit = orderPaymentPreferences.iterator();
                            while (oppit.hasNext()) {
                                GenericValue orderPaymentPreference = (GenericValue) oppit.next();
                                if (! (orderPaymentPreference.getString("statusId").equals("PAYMENT_SETTLED") || orderPaymentPreference.getString("statusId").equals("PAYMENT_CANCELLED"))) {
                                    GenericValue authTransaction = PaymentGatewayServices.getAuthTransaction(orderPaymentPreference);
                                    if (authTransaction != null && authTransaction.get("amount") != null) {

                                        // Update the total authorized amount
                                        totalNewAuthAmount = totalNewAuthAmount.add(authTransaction.getBigDecimal("amount").setScale(decimals, rounding));

                                        // Release the authorization for the OrderPaymentPreference
                                        Map prefReleaseResult = null;
                                        try {
                                            prefReleaseResult = dispatcher.runSync("releaseOrderPaymentPreference", UtilMisc.toMap("orderPaymentPreferenceId", orderPaymentPreference.getString("orderPaymentPreferenceId"), "userLogin", context.get("userLogin")));
                                        } catch( GenericServiceException e ) {
                                            String errMsg = UtilProperties.getMessage(resource, "AccountingTroubleCallingReleaseOrderPaymentPreferenceService", locale);
                                            Debug.logError(e, errMsg, module);
                                            return ServiceUtil.returnError(errMsg);
                                        }
                                        if (ServiceUtil.isError(prefReleaseResult) || ServiceUtil.isFailure(prefReleaseResult)) {
                                            String errMsg = ServiceUtil.getErrorMessage(prefReleaseResult);
                                            Debug.logError(errMsg, module);
                                            return ServiceUtil.returnError(errMsg);
                                        }
                                    }
                                }
                            }
                            
                            // Create a new OrderPaymentPreference for the order to handle the new (totalled) charge. Don't
                            //  set the maxAmount so that it doesn't interfere with other authorizations
                            Map serviceContext = UtilMisc.toMap("orderId", orderId, "paymentMethodId", paymentMethodId, "paymentMethodTypeId", "CREDIT_CARD", "userLogin", context.get("userLogin"));
                            String orderPaymentPreferenceId = null;
                            try {
                                Map result = dispatcher.runSync("createOrderPaymentPreference", serviceContext);
                                orderPaymentPreferenceId = (String) result.get("orderPaymentPreferenceId");
                            } catch (GenericServiceException e) {
                                String errMsg = UtilProperties.getMessage(resource, "AccountingTroubleCallingCreateOrderPaymentPreferenceService", locale);
                                Debug.logError(e, errMsg, module);
                                return ServiceUtil.returnError(errMsg);
                            }

                            // Attempt to authorize the new orderPaymentPreference
                            Map authResult = null;
                            try {

                                // Use an overrideAmount because the maxAmount wasn't set on the OrderPaymentPreference
                                authResult = dispatcher.runSync("authOrderPaymentPreference", UtilMisc.toMap("orderPaymentPreferenceId", orderPaymentPreferenceId, "overrideAmount", new Double(totalNewAuthAmount.doubleValue()), "userLogin", context.get("userLogin")));
                            } catch (GenericServiceException e) {
                                String errMsg = UtilProperties.getMessage(resource, "AccountingTroubleCallingAuthOrderPaymentPreferenceService", locale);
                                Debug.logError(e, errMsg, module);
                                return ServiceUtil.returnError(errMsg);
                            }

                            // If the authorization fails, create the invoice anyway, but make a note of it
                            boolean authFinished = ( (Boolean) authResult.get("finished") ).booleanValue();
                            boolean authErrors = ( (Boolean) authResult.get("errors") ).booleanValue();
                            if (authErrors || ! authFinished) {
                                String errMsg = UtilProperties.getMessage(resource, "AccountingUnableToAuthAdditionalShipCharges", UtilMisc.toMap("shipmentId", shipmentId, "paymentMethodId", paymentMethodId, "orderPaymentPreferenceId", orderPaymentPreferenceId), locale);
                                Debug.logError(errMsg, module);
                            }
                            
                        } 
                    } 
                }
            }

            // call the createInvoiceForOrder service for each order
            Map serviceContext = UtilMisc.toMap("orderId", orderId, "billItems", toBillItems, "userLogin", context.get("userLogin"));
            try {
                Map result = dispatcher.runSync("createInvoiceForOrder", serviceContext);
                invoicesCreated.add(result.get("invoiceId"));
            } catch (GenericServiceException e) {
                String errMsg = UtilProperties.getMessage(resource, "AccountingTroubleCallingCreateInvoiceForOrderService", locale);
                Debug.logError(e, errMsg, module);
                return ServiceUtil.returnError(errMsg);
            }
        }

        Map response = ServiceUtil.returnSuccess();
        response.put("invoicesCreated", invoicesCreated);
        return response;
    }

    private static String getInvoiceItemType(GenericDelegator delegator, String key1, String key2, String invoiceTypeId, String defaultValue) {
        GenericValue itemMap = null;
        try {
            if (UtilValidate.isNotEmpty(key1)) {
                itemMap = delegator.findByPrimaryKeyCache("InvoiceItemTypeMap", UtilMisc.toMap("invoiceItemMapKey", key1, "invoiceTypeId", invoiceTypeId));
            }
            if (itemMap == null && UtilValidate.isNotEmpty(key2)) {
                itemMap = delegator.findByPrimaryKeyCache("InvoiceItemTypeMap", UtilMisc.toMap("invoiceItemMapKey", key2, "invoiceTypeId", invoiceTypeId));
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, "Trouble getting InvoiceItemTypeMap entity record", module);
            return defaultValue;
        }
        if (itemMap != null) {
            return itemMap.getString("invoiceItemTypeId");
        } else {
            return defaultValue;
        }
    }

    public static Map createInvoicesFromReturnShipment(DispatchContext dctx, Map context) {
        GenericDelegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Locale locale = (Locale) context.get("locale");

        String shipmentId = (String) context.get("shipmentId");
        String errorMsg = UtilProperties.getMessage(resource, "AccountingErrorCreatingInvoiceForShipment",UtilMisc.toMap("shipmentId",shipmentId), locale);


        List invoicesCreated = new ArrayList();
        try {

            // get the shipment and validate that it is a sales return
            GenericValue shipment = delegator.findByPrimaryKey("Shipment", UtilMisc.toMap("shipmentId", shipmentId));
            if (shipment == null) {
                return ServiceUtil.returnError(errorMsg + UtilProperties.getMessage(resource, "AccountingShipmentNotFound",locale));
            }
            if (!shipment.getString("shipmentTypeId").equals("SALES_RETURN")) {
                return ServiceUtil.returnError(errorMsg + UtilProperties.getMessage(resource, "AccountingShipmentNotSalesReturn",locale));
            }

            // get the list of ShipmentReceipt for this shipment
            List shipmentReceipts = shipment.getRelated("ShipmentReceipt");

            // group the shipments by returnId (because we want a seperate itemized invoice for each return)
            Map receiptsGroupedByReturn = new HashMap();
            for (Iterator iter = shipmentReceipts.iterator(); iter.hasNext(); ) {
                GenericValue receipt = (GenericValue) iter.next();
                String returnId = receipt.getString("returnId");

                // see if there are ReturnItemBillings for this item
                List billings = delegator.findByAnd("ReturnItemBilling", UtilMisc.toMap("shipmentReceiptId", receipt.getString("receiptId"), "returnId", returnId, 
                            "returnItemSeqId", receipt.get("returnItemSeqId")));

                // if there are billings, we have already billed the item, so skip it
                if (billings.size() > 0) continue;

                // get the List of receipts keyed to this returnId or create a new one
                List receipts = (List) receiptsGroupedByReturn.get(returnId);
                if (receipts == null) {
                    receipts = new ArrayList();
                }

                // add our item to the group and put it back in the map
                receipts.add(receipt);
                receiptsGroupedByReturn.put(returnId, receipts);
            }

            // loop through the returnId keys in the map and invoke the createInvoiceFromReturn service for each
            for (Iterator iter = receiptsGroupedByReturn.keySet().iterator(); iter.hasNext(); ) {
                String returnId = (String) iter.next();
                List receipts = (List) receiptsGroupedByReturn.get(returnId);
                if (Debug.verboseOn()) {
                    Debug.logVerbose("Creating invoice for return [" + returnId + "] with receipts: " + receipts.toString(), module);
                }
                Map input = UtilMisc.toMap("returnId", returnId, "shipmentReceiptsToBill", receipts, "userLogin", context.get("userLogin"));
                Map serviceResults = dispatcher.runSync("createInvoiceFromReturn", input);
                if (ServiceUtil.isError(serviceResults)) {
                    return ServiceUtil.returnError(errorMsg, null, null, serviceResults);
                }

                // put the resulting invoiceId in the return list
                invoicesCreated.add(serviceResults.get("invoiceId"));
            }
        } catch (GenericServiceException e) {
            Debug.logError(e, errorMsg + e.getMessage(), module);
            return ServiceUtil.returnError(errorMsg + e.getMessage());
        } catch (GenericEntityException e) {
            Debug.logError(e, errorMsg + e.getMessage(), module);
            return ServiceUtil.returnError(errorMsg + e.getMessage());
        }

        Map result = ServiceUtil.returnSuccess();
        result.put("invoicesCreated", invoicesCreated);
        return result;
    }

    public static Map createInvoiceFromReturn(DispatchContext dctx, Map context) {
        GenericDelegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = (Locale) context.get("locale");

        String returnId= (String) context.get("returnId");
        List receipts = (List) context.get("shipmentReceiptsToBill");
        String errorMsg = UtilProperties.getMessage(resource, "AccountingErrorCreatingInvoiceForReturn",UtilMisc.toMap("returnId",returnId),locale);
        // List invoicesCreated = new ArrayList();
        try {
            // get the return header
            GenericValue returnHeader = delegator.findByPrimaryKey("ReturnHeader", UtilMisc.toMap("returnId", returnId));

            // set the invoice data
            Map input = UtilMisc.toMap("invoiceTypeId", "CUST_RTN_INVOICE", "statusId", "INVOICE_IN_PROCESS");
            input.put("partyId", returnHeader.get("toPartyId"));
            input.put("partyIdFrom", returnHeader.get("fromPartyId"));
            input.put("currencyUomId", returnHeader.get("currencyUomId"));
            input.put("invoiceDate", UtilDateTime.nowTimestamp());
            input.put("description", "Return Invoice for Customer Return #" + returnId); 
            input.put("billingAccountId", returnHeader.get("billingAccountId"));
            input.put("userLogin", userLogin);

            // call the service to create the invoice
            Map serviceResults = dispatcher.runSync("createInvoice", input);
            if (ServiceUtil.isError(serviceResults)) {
                return ServiceUtil.returnError(errorMsg, null, null, serviceResults);
            }
            String invoiceId = (String) serviceResults.get("invoiceId");

            // keep track of the invoice total vs the promised return total (how much the customer promised to return)
            BigDecimal invoiceTotal = ZERO;
            BigDecimal promisedTotal = ZERO;

            // loop through shipment receipts to create invoice items and return item billings for each item and adjustment
            int invoiceItemSeqNum = 1;
            String invoiceItemSeqId = UtilFormatOut.formatPaddedNumber(invoiceItemSeqNum, INVOICE_ITEM_SEQUENCE_ID_DIGITS);
            for (Iterator iter = receipts.iterator(); iter.hasNext(); ) {
                GenericValue receipt = (GenericValue) iter.next();

                // we need the related return item and product
                GenericValue returnItem = receipt.getRelatedOneCache("ReturnItem");
                GenericValue product = returnItem.getRelatedOneCache("Product");

                // extract the return price as a big decimal for convenience
                BigDecimal returnPrice = returnItem.getBigDecimal("returnPrice");

                // determine invoice item type from the return item type
                String invoiceItemTypeId = getInvoiceItemType(delegator, returnItem.getString("returnItemTypeId"), null, "CUST_RTN_INVOICE", null);
                if (invoiceItemTypeId == null) {
                    return ServiceUtil.returnError(errorMsg + UtilProperties.getMessage(resource, "AccountingNoKnownInvoiceItemTypeReturnItemType",UtilMisc.toMap("returnItemTypeId",returnItem.getString("returnItemTypeId")),locale));
                }

                // create the invoice item for this shipment receipt
                input = UtilMisc.toMap("invoiceId", invoiceId, "invoiceItemTypeId", invoiceItemTypeId, "quantity", receipt.get("quantityAccepted"));
                input.put("invoiceItemSeqId", "" + invoiceItemSeqId); // turn the int into a string with ("" + int) hack
                input.put("amount", returnItem.get("returnPrice")); // this service requires Double
                input.put("productId", returnItem.get("productId"));
                input.put("taxableFlag", product.get("taxable"));
                input.put("description", returnItem.get("description"));
                // TODO: what about the productFeatureId?
                input.put("userLogin", userLogin);
                serviceResults = dispatcher.runSync("createInvoiceItem", input);
                if (ServiceUtil.isError(serviceResults)) {
                    return ServiceUtil.returnError(errorMsg, null, null, serviceResults);
                }

                // copy the return item information into ReturnItemBilling
                input = UtilMisc.toMap("returnId", returnId, "returnItemSeqId", returnItem.get("returnItemSeqId"), 
                        "invoiceId", invoiceId);
                input.put("invoiceItemSeqId", "" + invoiceItemSeqId); // turn the int into a string with ("" + int) hack
                input.put("shipmentReceiptId", receipt.get("receiptId"));
                input.put("quantity", receipt.get("quantityAccepted"));
                input.put("amount", returnItem.get("returnPrice")); // this service requires Double
                input.put("userLogin", userLogin);
                serviceResults = dispatcher.runSync("createReturnItemBilling", input);
                if (ServiceUtil.isError(serviceResults)) {
                    return ServiceUtil.returnError(errorMsg, null, null, serviceResults);
                }
                if (Debug.verboseOn()) {
                    Debug.logVerbose("Creating Invoice Item with amount " + returnPrice + " and quantity " + receipt.getBigDecimal("quantityAccepted") 
                            + " for shipment receipt [" + receipt.getString("receiptId") + "]", module);
                }

                // increment the seqId counter after creating the invoice item and return item billing
                invoiceItemSeqNum += 1;  
                invoiceItemSeqId = UtilFormatOut.formatPaddedNumber(invoiceItemSeqNum, INVOICE_ITEM_SEQUENCE_ID_DIGITS);

                // keep a running total (note: a returnItem may have many receipts. hence, the promised total quantity is the receipt quantityAccepted + quantityRejected)
                BigDecimal actualAmount = returnPrice.multiply(receipt.getBigDecimal("quantityAccepted")).setScale(decimals, rounding);
                BigDecimal promisedAmount = returnPrice.multiply(receipt.getBigDecimal("quantityAccepted").add(receipt.getBigDecimal("quantityRejected"))).setScale(decimals, rounding);
                invoiceTotal = invoiceTotal.add(actualAmount).setScale(decimals, rounding);
                promisedTotal = promisedTotal.add(promisedAmount).setScale(decimals, rounding);

                // for each adjustment related to this ReturnItem, create a separate invoice item
                List adjustments = returnItem.getRelatedCache("ReturnAdjustment");
                for (Iterator adjIter = adjustments.iterator(); adjIter.hasNext(); ) {
                    GenericValue adjustment = (GenericValue) adjIter.next();

                    if (adjustment.get("amount") == null) {
                         Debug.logWarning("Return adjustment [" + adjustment.get("returnAdjustmentId") + "] has null amount and will be skipped", module);
                         continue;
                    }

                    // determine invoice item type from the return item type
                    invoiceItemTypeId = getInvoiceItemType(delegator, adjustment.getString("returnAdjustmentTypeId"), null, "CUST_RTN_INVOICE", null);
                    if (invoiceItemTypeId == null) {
                        return ServiceUtil.returnError(errorMsg + "No known invoice item type for the return adjustment type [" 
                                +  adjustment.getString("returnAdjustmentTypeId") + "]");
                    }

                    // prorate the adjustment amount by the returned amount; do not round ratio
                    BigDecimal ratio = receipt.getBigDecimal("quantityAccepted").divide(returnItem.getBigDecimal("returnQuantity"), 100, rounding);
                    BigDecimal amount = adjustment.getBigDecimal("amount");
                    amount = amount.multiply(ratio).setScale(decimals, rounding);
                    if (Debug.verboseOn()) {
                        Debug.logVerbose("Creating Invoice Item with amount " + adjustment.getBigDecimal("amount") + " prorated to " + amount 
                                + " for return adjustment [" + adjustment.getString("returnAdjustmentId") + "]", module);
                    }

                    // prepare invoice item data for this adjustment
                    input = UtilMisc.toMap("invoiceId", invoiceId, "invoiceItemTypeId", invoiceItemTypeId, "quantity", new Double(1.0));
                    input.put("amount", new Double(amount.doubleValue()));
                    input.put("invoiceItemSeqId", "" + invoiceItemSeqId); // turn the int into a string with ("" + int) hack
                    input.put("productId", returnItem.get("productId"));
                    input.put("description", adjustment.get("description"));
                    input.put("overrideGlAccountId", adjustment.get("overrideGlAccountId"));
                    input.put("taxAuthPartyId", adjustment.get("taxAuthPartyId"));
                    input.put("taxAuthGeoId", adjustment.get("taxAuthGeoId"));
                    input.put("userLogin", userLogin);

                    // only set taxable flag when the adjustment is not a tax 
                    // TODO: Note that we use the value of Product.taxable here. This is not an ideal solution. Instead, use returnAdjustment.includeInTax
                    if (adjustment.get("returnAdjustmentTypeId").equals("RET_SALES_TAX_ADJ")) {
                        input.put("taxableFlag", "N");
                    }

                    // create the invoice item
                    serviceResults = dispatcher.runSync("createInvoiceItem", input);
                    if (ServiceUtil.isError(serviceResults)) {
                        return ServiceUtil.returnError(errorMsg, null, null, serviceResults);
                    }

                    // increment the seqId counter
                    invoiceItemSeqNum += 1;  
                    invoiceItemSeqId = UtilFormatOut.formatPaddedNumber(invoiceItemSeqNum, INVOICE_ITEM_SEQUENCE_ID_DIGITS);

                    // keep a running total (promised adjustment in this case is the same as the invoice adjustment)
                    invoiceTotal = invoiceTotal.add(amount).setScale(decimals, rounding);
                    promisedTotal = promisedTotal.add(amount).setScale(decimals, rounding);
                }
            }

            // ratio of the invoice total to the promised total so far or zero if the amounts were zero
            BigDecimal actualToPromisedRatio = ZERO;
            if (invoiceTotal.signum() != 0) {
                actualToPromisedRatio = invoiceTotal.divide(promisedTotal, 100, rounding);  // do not round ratio
            }

            // loop through return-wide adjustments and create invoice items for each
            List adjustments = returnHeader.getRelatedByAndCache("ReturnAdjustment", UtilMisc.toMap("returnItemSeqId", "_NA_"));
            for (Iterator iter = adjustments.iterator(); iter.hasNext(); ) {
                GenericValue adjustment = (GenericValue) iter.next();

                // determine invoice item type from the return item type
                String invoiceItemTypeId = getInvoiceItemType(delegator, adjustment.getString("returnAdjustmentTypeId"), null, "CUST_RTN_INVOICE", null);
                if (invoiceItemTypeId == null) {
                    return ServiceUtil.returnError(errorMsg + UtilProperties.getMessage(resource, "AccountingNoKnownInvoiceItemTypeReturnAdjustmentType",
                            UtilMisc.toMap("returnAdjustmentTypeId",adjustment.getString("returnAdjustmentTypeId")),locale));
                }

                // prorate the adjustment amount by the actual to promised ratio
                BigDecimal amount = adjustment.getBigDecimal("amount").multiply(actualToPromisedRatio).setScale(decimals, rounding);
                if (Debug.verboseOn()) {
                    Debug.logVerbose("Creating Invoice Item with amount " + adjustment.getBigDecimal("amount") + " prorated to " + amount 
                            + " for return adjustment [" + adjustment.getString("returnAdjustmentId") + "]", module);
                }

                // prepare the invoice item for the return-wide adjustment
                input = UtilMisc.toMap("invoiceId", invoiceId, "invoiceItemTypeId", invoiceItemTypeId, "quantity", new Double(1.0));
                input.put("amount", new Double(amount.doubleValue()));
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
                    return ServiceUtil.returnError(errorMsg, null, null, serviceResults);
                }

                // increment the seqId counter
                invoiceItemSeqNum += 1;  
                invoiceItemSeqId = UtilFormatOut.formatPaddedNumber(invoiceItemSeqNum, INVOICE_ITEM_SEQUENCE_ID_DIGITS);
            }
        
            // Set the invoice to READY
            serviceResults = dispatcher.runSync("setInvoiceStatus", UtilMisc.toMap("invoiceId", invoiceId, "statusId", "INVOICE_READY", "userLogin", userLogin));
            if (ServiceUtil.isError(serviceResults)) {
                return ServiceUtil.returnError(errorMsg, null, null, serviceResults);
            }

            // return the invoiceId
            Map results = ServiceUtil.returnSuccess();
            results.put("invoiceId", invoiceId);
            return results;
        } catch (GenericServiceException e) {
            Debug.logError(e, errorMsg + e.getMessage(), module);
            return ServiceUtil.returnError(errorMsg + e.getMessage());
        } catch (GenericEntityException e) {
            Debug.logError(e, errorMsg + e.getMessage(), module);
            return ServiceUtil.returnError(errorMsg + e.getMessage());
        }
    }

    public static Map checkInvoicePaymentApplications(DispatchContext ctx, Map context) {
        GenericDelegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = (Locale) context.get("locale");

        if (decimals == -1 || rounding == -1) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource,"AccountingAritmeticPropertiesNotConfigured",locale));
        }

        String invoiceId = (String) context.get("invoiceId");
        GenericValue invoice = null ;
        try {
            invoice = delegator.findByPrimaryKey("Invoice", UtilMisc.toMap("invoiceId", invoiceId));
        } catch( GenericEntityException e ) {
            Debug.logError(e, "Problem getting Invoice for Invoice ID" + invoiceId, module);
            return ServiceUtil.returnError("Problem getting Invoice for Invoice ID" + invoiceId);
        }
        
        // Ignore invoices that aren't ready yet
        if (! invoice.getString("statusId").equals("INVOICE_READY")) {
            return ServiceUtil.returnSuccess();
        }
        
        // Get the payment applications that can be used to pay the invoice
        List paymentAppl = null;
        try {
            paymentAppl = delegator.findByAnd("PaymentAndApplication", UtilMisc.toMap("invoiceId", invoiceId));
            if (paymentAppl != null) {

                // For each payment application, select only those that are RECEIVED or SENT based on whether the payment is a RECEIPT or DISBURSEMENT respectively
                for (Iterator iter = paymentAppl.iterator(); iter.hasNext(); ) {
                    GenericValue payment = (GenericValue) iter.next();
                    if ("PMNT_RECEIVED".equals(payment.get("statusId")) && UtilAccounting.isReceipt(payment)) {
                        continue; // keep
                    }
                    if ("PMNT_SENT".equals(payment.get("statusId")) && UtilAccounting.isDisbursement(payment)) {
                        continue; // keep
                    }
                    // all other cases, remove the payment applicaition
                    iter.remove();
                }
            }
        } catch (GenericEntityException e) {
            String errMsg = UtilProperties.getMessage(resource, "AccountingProblemGettingPaymentApplication",UtilMisc.toMap("invoiceId",invoiceId), locale);
            Debug.logError(e, errMsg, module);
            return ServiceUtil.returnError(errMsg);
        }

        Map payments = new HashMap();
        Timestamp paidDate = null;
        if (paymentAppl != null) {
            Iterator pai = paymentAppl.iterator();
            while (pai.hasNext()) {
                GenericValue payAppl = (GenericValue) pai.next();
                payments.put(payAppl.getString("paymentId"), payAppl.getBigDecimal("amountApplied"));

                // paidDate will be the last date (chronologically) of all the Payments applied to this invoice
                Timestamp paymentDate = payAppl.getTimestamp("effectiveDate");
                if (paymentDate != null) {
                    if ((paidDate == null) || (paidDate.before(paymentDate))) {
                        paidDate = paymentDate;
                    }
                }    
            }
        }

        BigDecimal totalPayments = ZERO;
        Iterator pi = payments.keySet().iterator();
        while (pi.hasNext()) {
            String paymentId = (String) pi.next();
            BigDecimal amount = (BigDecimal) payments.get(paymentId);
            if (amount == null) amount = ZERO;
            totalPayments = totalPayments.add(amount).setScale(decimals, rounding);
        }

        if (totalPayments.signum() == 1) {
            BigDecimal invoiceTotal = InvoiceWorker.getInvoiceTotalBd(delegator, invoiceId);
            if (Debug.verboseOn()) {
                Debug.logVerbose("Invoice #" + invoiceId + " total: " + invoiceTotal, module);
                Debug.logVerbose("Total payments : " + totalPayments, module);
            }
            if (totalPayments.compareTo(invoiceTotal) >= 0) { // this checks that totalPayments is greater than or equal to invoiceTotal
                // this invoice is paid
                Map svcCtx = UtilMisc.toMap("statusId", "INVOICE_PAID", "invoiceId", invoiceId, 
                        "paidDate", paidDate, "userLogin", userLogin);
                try {
                    dispatcher.runSync("setInvoiceStatus", svcCtx);
                } catch (GenericServiceException e) {
                    String errMsg = UtilProperties.getMessage(resource, "AccountingProblemChangingInvoiceStatusTo",UtilMisc.toMap("newStatus","INVOICE_PAID"), locale);
                    Debug.logError(e, errMsg + svcCtx, module);
                    return ServiceUtil.returnError(errMsg);
                }
            }
        } else {
            Debug.log("No payments found for Invoice #" + invoiceId, module);
        }

        return ServiceUtil.returnSuccess();
    }

    private static BigDecimal calcHeaderAdj(GenericDelegator delegator, GenericValue adj, String invoiceTypeId, String invoiceId, String invoiceItemSeqId, 
            BigDecimal divisor, BigDecimal multiplier, BigDecimal baseAmount, int decimals, int rounding, GenericValue userLogin, LocalDispatcher dispatcher, Locale locale) {
        BigDecimal adjAmount = ZERO;
        if (adj.get("amount") != null) {

            // pro-rate the amount
            BigDecimal amount = ZERO;
            // make sure the divisor is not 0 to avoid NaN problems; just leave the amount as 0 and skip it in essense
            if (divisor.signum() != 0) {
                // multiply first then divide to avoid rounding errors
                amount = baseAmount.multiply(multiplier).divide(divisor, decimals, rounding);
            }
            if (amount.signum() != 0) {
                Map createInvoiceItemContext = FastMap.newInstance();
                createInvoiceItemContext.put("invoiceId", invoiceId);
                createInvoiceItemContext.put("invoiceItemSeqId", invoiceItemSeqId);
                createInvoiceItemContext.put("invoiceItemTypeId", getInvoiceItemType(delegator, adj.getString("orderAdjustmentTypeId"), null, invoiceTypeId, "INVOICE_ADJ"));
                createInvoiceItemContext.put("description", adj.get("description"));
                createInvoiceItemContext.put("quantity", new Double(1));
                createInvoiceItemContext.put("amount", new Double(amount.doubleValue()));
                createInvoiceItemContext.put("overrideGlAccountId", adj.get("overrideGlAccountId"));
                //createInvoiceItemContext.put("productId", orderItem.get("productId"));
                //createInvoiceItemContext.put("productFeatureId", orderItem.get("productFeatureId"));
                //createInvoiceItemContext.put("uomId", "");
                //createInvoiceItemContext.put("taxableFlag", product.get("taxable"));
                createInvoiceItemContext.put("taxAuthPartyId", adj.get("taxAuthPartyId"));
                createInvoiceItemContext.put("taxAuthGeoId", adj.get("taxAuthGeoId"));
                createInvoiceItemContext.put("taxAuthorityRateSeqId", adj.get("taxAuthorityRateSeqId"));
                createInvoiceItemContext.put("userLogin", userLogin);

                Map createInvoiceItemResult = null;
                try {
                    createInvoiceItemResult = dispatcher.runSync("createInvoiceItem", createInvoiceItemContext);
                } catch( GenericServiceException e ) {
                    String errMsg = UtilProperties.getMessage(resource,"AccountingServiceErrorCreatingInvoiceItemFromOrder",locale) + ": " + e.toString();
                    Debug.logError(e, errMsg, module);
                    ServiceUtil.returnError(errMsg);
                }
                if (ServiceUtil.isError(createInvoiceItemResult)) {
                    ServiceUtil.returnError(UtilProperties.getMessage(resource,"AccountingErrorCreatingInvoiceItemFromOrder",locale), null, null, createInvoiceItemResult);
                }

                // Create the OrderAdjustmentBilling record
                Map createOrderAdjustmentBillingContext = FastMap.newInstance();
                createOrderAdjustmentBillingContext.put("orderAdjustmentId", adj.getString("orderAdjustmentId"));
                createOrderAdjustmentBillingContext.put("invoiceId", invoiceId);
                createOrderAdjustmentBillingContext.put("invoiceItemSeqId", invoiceItemSeqId);
                createOrderAdjustmentBillingContext.put("amount", new Double(amount.doubleValue()));
                createOrderAdjustmentBillingContext.put("userLogin", userLogin);

                try {
                    Map createOrderAdjustmentBillingResult = dispatcher.runSync("createOrderAdjustmentBilling", createOrderAdjustmentBillingContext);
                } catch( GenericServiceException e ) {
                    ServiceUtil.returnError(UtilProperties.getMessage(resource,"AccountingErrorCreatingOrderAdjustmentBillingFromOrder",locale), null, null, createOrderAdjustmentBillingContext);
                }

            }
            amount = amount.setScale(decimals, rounding);
            adjAmount = amount;
        }
        else if (adj.get("sourcePercentage") != null) {
            // pro-rate the amount
            BigDecimal percent = adj.getBigDecimal("sourcePercentage");
            percent = percent.divide(new BigDecimal(100), 100, rounding);            
            BigDecimal amount = ZERO;
            // make sure the divisor is not 0 to avoid NaN problems; just leave the amount as 0 and skip it in essense
            if (divisor.signum() != 0) {
                // multiply first then divide to avoid rounding errors
                amount = percent.multiply(divisor);
            }
            if (amount.signum() != 0) {
                Map createInvoiceItemContext = FastMap.newInstance();
                createInvoiceItemContext.put("invoiceId", invoiceId);
                createInvoiceItemContext.put("invoiceItemSeqId", invoiceItemSeqId);
                createInvoiceItemContext.put("invoiceItemTypeId", getInvoiceItemType(delegator, adj.getString("orderAdjustmentTypeId"), null, invoiceTypeId, "INVOICE_ADJ"));
                createInvoiceItemContext.put("description", adj.get("description"));
                createInvoiceItemContext.put("quantity", new Double(1));
                createInvoiceItemContext.put("amount", new Double(amount.doubleValue()));
                createInvoiceItemContext.put("overrideGlAccountId", adj.get("overrideGlAccountId"));
                //createInvoiceItemContext.put("productId", orderItem.get("productId"));
                //createInvoiceItemContext.put("productFeatureId", orderItem.get("productFeatureId"));
                //createInvoiceItemContext.put("uomId", "");
                //createInvoiceItemContext.put("taxableFlag", product.get("taxable"));
                createInvoiceItemContext.put("taxAuthPartyId", adj.get("taxAuthPartyId"));
                createInvoiceItemContext.put("taxAuthGeoId", adj.get("taxAuthGeoId"));
                createInvoiceItemContext.put("taxAuthorityRateSeqId", adj.get("taxAuthorityRateSeqId"));
                createInvoiceItemContext.put("userLogin", userLogin);

                Map createInvoiceItemResult = null;
                try {
                    createInvoiceItemResult = dispatcher.runSync("createInvoiceItem", createInvoiceItemContext);
                } catch( GenericServiceException e ) {
                    String errMsg = UtilProperties.getMessage(resource,"AccountingServiceErrorCreatingInvoiceItemFromOrder",locale) + ": " + e.toString();
                    Debug.logError(e, errMsg, module);
                    ServiceUtil.returnError(errMsg);
                }
                if (ServiceUtil.isError(createInvoiceItemResult)) {
                    ServiceUtil.returnError(UtilProperties.getMessage(resource,"AccountingErrorCreatingInvoiceItemFromOrder",locale), null, null, createInvoiceItemResult);
                }

                // Create the OrderAdjustmentBilling record
                Map createOrderAdjustmentBillingContext = FastMap.newInstance();
                createOrderAdjustmentBillingContext.put("orderAdjustmentId", adj.getString("orderAdjustmentId"));
                createOrderAdjustmentBillingContext.put("invoiceId", invoiceId);
                createOrderAdjustmentBillingContext.put("invoiceItemSeqId", invoiceItemSeqId);
                createOrderAdjustmentBillingContext.put("amount", new Double(amount.doubleValue()));
                createOrderAdjustmentBillingContext.put("userLogin", userLogin);

                try {
                    Map createOrderAdjustmentBillingResult = dispatcher.runSync("createOrderAdjustmentBilling", createOrderAdjustmentBillingContext);
                } catch( GenericServiceException e ) {
                    ServiceUtil.returnError(UtilProperties.getMessage(resource,"AccountingErrorCreatingOrderAdjustmentBillingFromOrder",locale), null, null, createOrderAdjustmentBillingContext);
                }

            }
            amount = amount.setScale(decimals, rounding);
            adjAmount = amount;
        }

        Debug.logInfo("adjAmount: " + adjAmount + ", divisor: " + divisor + ", multiplier: " + multiplier + 
                ", invoiceTypeId: " + invoiceTypeId + ", invoiceId: " + invoiceId + ", itemSeqId: " + invoiceItemSeqId + 
                ", decimals: " + decimals + ", rounding: " + rounding + ", adj: " + adj, module);
        return adjAmount;
    }

    /* Creates InvoiceTerm entries for a list of terms, which can be BillingAccountTerms, OrderTerms, etc. */
    private static void createInvoiceTerms(GenericDelegator delegator, LocalDispatcher dispatcher, String invoiceId, List terms, GenericValue userLogin, Locale locale) {
        List invoiceTerms = new LinkedList();
        if ((terms != null) && (terms.size() > 0)) {
            for (Iterator termsIter = terms.iterator(); termsIter.hasNext(); ) {
                GenericValue term = (GenericValue) termsIter.next();

                Map createInvoiceTermContext = FastMap.newInstance();
                createInvoiceTermContext.put("invoiceId", invoiceId);
                createInvoiceTermContext.put("invoiceItemSeqId", "_NA_");
                createInvoiceTermContext.put("termTypeId", term.get("termTypeId"));
                createInvoiceTermContext.put("termValue", term.get("termValue"));
                createInvoiceTermContext.put("termDays", term.get("termDays"));
                createInvoiceTermContext.put("description", term.get("description"));
                createInvoiceTermContext.put("uomId", term.get("uomId"));
                createInvoiceTermContext.put("userLogin", userLogin);

                Map createInvoiceTermResult = null;
                try {
                    createInvoiceTermResult = dispatcher.runSync("createInvoiceTerm", createInvoiceTermContext);
                } catch( GenericServiceException e ) {
                    String errMsg = UtilProperties.getMessage(resource,"AccountingServiceErrorCreatingInvoiceTermFromOrder",locale) + ": " + e.toString();
                    Debug.logError(e, errMsg, module);
                    ServiceUtil.returnError(errMsg);
                }
                if (ServiceUtil.isError(createInvoiceTermResult)) {
                    ServiceUtil.returnError(UtilProperties.getMessage(resource,"AccountingErrorCreatingInvoiceTermFromOrder",locale), null, null, createInvoiceTermResult);
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
    public static Map updatePaymentApplication(DispatchContext dctx, Map context) {
        Double amountApplied = (Double) context.get("amountApplied");
        if (amountApplied != null) {
            BigDecimal amountAppliedBd = new BigDecimal(amountApplied.toString());
            context.put("amountApplied", amountAppliedBd);
        } else {
            BigDecimal amountAppliedBd = ZERO;
            context.put("amountApplied", amountAppliedBd);
        }

        return updatePaymentApplicationBd(dctx, context);
    }

    /**
     * Service to add payment application records to indicate which invoices
     * have been paid/received. For invoice processing, this service works on
     * the invoice level when 'invoiceProcessing' parameter is set to "Y" else
     * it works on the invoice item level.
     * 
     * This version will apply as much as possible when no amountApplied is provided. 
     */
    public static Map updatePaymentApplicationDef(DispatchContext dctx, Map context) {
        if (!context.containsKey("useHighestAmount")) {
            context.put("useHighestAmount","Y");
        }
        Double amountApplied = (Double) context.get("amountApplied");
        if (amountApplied != null) {
            BigDecimal amountAppliedBd = new BigDecimal(amountApplied.toString());
            context.put("amountApplied", amountAppliedBd);
        } else {
            BigDecimal amountAppliedBd = ZERO;
            context.put("amountApplied", amountAppliedBd);
        }

        return updatePaymentApplicationBd(dctx, context);
    }

    public static Map updatePaymentApplicationBd(DispatchContext dctx, Map context) {
        if (!context.containsKey("useHighestAmount")) {
            context.put("useHighestAmount","N");
        }
        return updatePaymentApplicationDefBd(dctx, context);
    }

    private static String successMessage = null;
    public static Map updatePaymentApplicationDefBd(DispatchContext dctx, Map context) {
            GenericDelegator delegator = dctx.getDelegator();
            Locale locale = (Locale) context.get("locale");

        if (decimals == -1 || rounding == -1) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource,"AccountingAritmeticPropertiesNotConfigured",locale));
        }

        if (!context.containsKey("useHighestAmount")) {
            context.put("useHighestAmount","Y");
        }

        String defaultInvoiceProcessing = UtilProperties.getPropertyValue("AccountingConfig","invoiceProcessing");
        
        boolean debug = true; // show processing messages in the log..or not....

        // a 'y' in invoiceProssesing wil reverse the default processing
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

        List errorMessageList = new LinkedList();

        if (debug) Debug.logInfo("updatePaymentApplicationDefBd input parameters..." + 
                " defaultInvoiceProcessing: " + defaultInvoiceProcessing + 
                " changeDefaultInvoiceProcessing: " + changeProcessing +
                " useHighestAmount: " + useHighestAmount +
                " paymentApplicationId: " + paymentApplicationId + 
                " PaymentId: " + paymentId + 
                " InvoiceId: " + invoiceId + 
                " InvoiceItemSeqId: " + invoiceItemSeqId + 
                " BillingAccountId: " + billingAccountId + 
                " toPaymentId: " + toPaymentId + 
                " amountApplied: " + amountApplied + 
                " TaxAuthGeoId: " + taxAuthGeoId, module);

        if (changeProcessing == null) changeProcessing = "N";    // not provided, so no change
        
        boolean invoiceProcessing = true;
        if (defaultInvoiceProcessing.equals("YY")) invoiceProcessing = true;

        else if (defaultInvoiceProcessing.equals("NN")) invoiceProcessing = false;

        else if (defaultInvoiceProcessing.equals("Y")) {
            if (changeProcessing.equals("Y")) invoiceProcessing = false;
            else invoiceProcessing = true;
        }

        else if (defaultInvoiceProcessing.equals("N")) {
            if (changeProcessing.equals("Y")) invoiceProcessing = true;
            else invoiceProcessing = false;
        }

        // on a new paymentApplication check if only billing or invoice or tax
        // id is provided not 2,3... BUT a combination of billingAccountId and invoiceId is permitted - that's how you use a
        // Billing Account to pay for an Invoice
        if (paymentApplicationId == null) {
            int count = 0;
            if (invoiceId != null) count++;
            if (toPaymentId != null) count++;
            if (billingAccountId != null) count++;
            if (taxAuthGeoId != null) count++;
            if ((billingAccountId != null) && (invoiceId != null)) count--;
            if (count != 1) {
                errorMessageList.add(UtilProperties.getMessage(resource, "AccountingSpecifyInvoiceToPaymentBillingAccountTaxGeoId", locale));
            }
        }

        // avoid null pointer exceptions.
        if (amountApplied == null) amountApplied = ZERO; 
        // makes no sense to have an item numer without an invoice number
        if (invoiceId == null) invoiceItemSeqId = null; 

        // retrieve all information and perform checking on the retrieved info.....

        // Payment.....
        BigDecimal paymentApplyAvailable = ZERO; 
        // amount available on the payment reduced by the already applied amounts
        BigDecimal amountAppliedMax = ZERO; 
        // the maximum that can be applied taking payment,invoice,invoiceitem,billing account in concideration
        // if maxApplied is missing, this value can be used
        GenericValue payment = null;
        if (paymentId == null || paymentId.equals("")) {
            errorMessageList.add(UtilProperties.getMessage(resource, "AccountingPaymentIdBlankNotSupplied",locale));
        } else {
            try {
                payment = delegator.findByPrimaryKey("Payment", UtilMisc.toMap("paymentId", paymentId));
            } catch (GenericEntityException e) {
                ServiceUtil.returnError(e.getMessage());
            }
            if (payment == null) {
                errorMessageList.add(UtilProperties.getMessage(resource, "AccountingPaymentRecordNotFound",UtilMisc.toMap("paymentId",paymentId),locale));
            }
            paymentApplyAvailable = payment.getBigDecimal("amount").subtract(PaymentWorker.getPaymentAppliedBd(payment)).setScale(decimals,rounding);

            if (payment.getString("statusId").equals("PMNT_CANCELLED")) {
                errorMessageList.add(UtilProperties.getMessage(resource, "AccountingPaymentCancelled", UtilMisc.toMap("paymentId",paymentId), locale));
            }
            if (payment.getString("statusId").equals("PMNT_CONFIRMED")) {
                errorMessageList.add(UtilProperties.getMessage(resource, "AccountingPaymentConfirmed", UtilMisc.toMap("paymentId",paymentId), locale));
            }

            // if the amount to apply is 0 give it amount the payment still need
            // to apply
            if (amountApplied.signum() == 0) {
                amountAppliedMax = paymentApplyAvailable;
            }

            if (paymentApplicationId == null) { 
                // only check for new application records, update on existing records is checked in the paymentApplication section
                if (paymentApplyAvailable.signum() == 0) {
                    errorMessageList.add(UtilProperties.getMessage(resource, "AccountingPaymentAlreadyApplied",UtilMisc.toMap("paymentId",paymentId), locale));
                } else {
                    // check here for too much application if a new record is
                    // added (paymentApplicationId == null)
                    if (amountApplied.compareTo(paymentApplyAvailable) > 0) {
                        errorMessageList.add(UtilProperties.getMessage(resource, "AccountingPaymentLessRequested",
                                UtilMisc.toMap("paymentId",paymentId, 
                                            "paymentApplyAvailable",paymentApplyAvailable,
                                            "amountApplied",amountApplied,"isoCode",payment.getString("currencyUomId")),locale));
                    }
                }
            }

            if (debug) Debug.logInfo("Payment info retrieved and checked...", module);
        }

        // the "TO" Payment.....
        BigDecimal toPaymentApplyAvailable = ZERO; 
        GenericValue toPayment = null;
        if (toPaymentId != null && !toPaymentId.equals("")) {
            try {
                toPayment = delegator.findByPrimaryKey("Payment", UtilMisc.toMap("paymentId", toPaymentId));
            } catch (GenericEntityException e) {
                ServiceUtil.returnError(e.getMessage());
            }
            if (toPayment == null) {
                errorMessageList.add(UtilProperties.getMessage(resource, "AccountingPaymentRecordNotFound",UtilMisc.toMap("paymentId",toPaymentId),locale));
            }
            toPaymentApplyAvailable = toPayment.getBigDecimal("amount").subtract(PaymentWorker.getPaymentAppliedBd(toPayment)).setScale(decimals,rounding);

            if (toPayment.getString("statusId").equals("PMNT_CANCELLED")) {
                errorMessageList.add(UtilProperties.getMessage(resource, "AccountingPaymentCancelled", UtilMisc.toMap("paymentId",paymentId), locale));
            }
            if (toPayment.getString("statusId").equals("PMNT_CONFIRMED")) {
                errorMessageList.add(UtilProperties.getMessage(resource, "AccountingPaymentConfirmed", UtilMisc.toMap("paymentId",paymentId), locale));
            }

            // if the amount to apply is less then required by the payment reduce it
            if (amountAppliedMax.compareTo(toPaymentApplyAvailable) > 0) {
                amountAppliedMax = toPaymentApplyAvailable;
            }

            if (paymentApplicationId == null) { 
                // only check for new application records, update on existing records is checked in the paymentApplication section
                if (toPaymentApplyAvailable.signum() == 0) {
                    errorMessageList.add(UtilProperties.getMessage(resource, "AccountingPaymentAlreadyApplied",UtilMisc.toMap("paymentId",toPaymentId), locale));
                } else {
                    // check here for too much application if a new record is
                    // added (paymentApplicationId == null)
                    if (amountApplied.compareTo(toPaymentApplyAvailable) > 0) {
                            errorMessageList.add(UtilProperties.getMessage(resource, "AccountingPaymentLessRequested",
                                    UtilMisc.toMap("paymentId",toPaymentId, 
                                                "paymentApplyAvailable",toPaymentApplyAvailable,
                                                "amountApplied",amountApplied,"isoCode",payment.getString("currencyUomId")),locale));
                    }
                }
            }
            
            // check if at least one send is the same as one receiver on the other payment
            if (!payment.getString("partyIdFrom").equals(toPayment.getString("partyIdTo")) && 
                    !payment.getString("partyIdFrom").equals(toPayment.getString("partyIdTo")))    {
                errorMessageList.add(UtilProperties.getMessage(resource, "AccountingFromPartySameToParty", locale));
            }

            if (debug) Debug.logInfo("toPayment info retrieved and checked...", module);
        }

        // billing account
        GenericValue billingAccount = null;
        BigDecimal billingAccountApplyAvailable = ZERO;
        if (billingAccountId != null && !billingAccountId.equals("")) {
            try {
                billingAccount = delegator.findByPrimaryKey("BillingAccount", UtilMisc.toMap("billingAccountId", billingAccountId));
            } catch (GenericEntityException e) {
                ServiceUtil.returnError(e.getMessage());
            }
            if (billingAccount == null) {
                errorMessageList.add(UtilProperties.getMessage(resource, "AccountingBillingAccountNotFound",UtilMisc.toMap("billingAccountId",billingAccountId), locale));
            }
            
            // Get the available balance, which is how much can be used, rather than the regular balance, which is how much has already been charged
            try {
                billingAccountApplyAvailable = BillingAccountWorker.availableToCapture(billingAccount);
            } catch (GenericEntityException e) {
                errorMessageList.add(UtilProperties.getMessage(resource, "AccountingBillingAccountBalanceNotFound",UtilMisc.toMap("billingAccountId",billingAccountId), locale));
                ServiceUtil.returnError(e.getMessage());
            }

            if (paymentApplicationId == null) { 
                // when creating a new PaymentApplication, check if there is sufficient balance in the billing account, but only if the invoiceId is not null
                // If you create a PaymentApplication with both billingAccountId and invoiceId, then you're applying a billing account towards an invoice
                // If you create a PaymentApplication just with billingAccountId and no invoiceId, then you're adding value to billing account, so it should not matter
                // what the previous available balance is
                if (invoiceId != null) {
                    if (billingAccountApplyAvailable.signum() <= 0)  {
                        errorMessageList.add(UtilProperties.getMessage(resource, "AccountingBillingAccountBalanceProblem",UtilMisc.toMap("billingAccountId",billingAccountId,"isoCode",billingAccount.getString("accountCurrencyUomId")), locale));
                    } else {
                        // check here for too much application if a new record is
                        // added (paymentApplicationId == null)
                        if (amountApplied.compareTo(billingAccountApplyAvailable) == 1) {
                            errorMessageList.add(UtilProperties.getMessage(resource, "AccountingBillingAccountLessRequested",
                                        UtilMisc.toMap("billingAccountId",billingAccountId, 
                                            "billingAccountApplyAvailable",billingAccountApplyAvailable,
                                            "amountApplied",amountApplied,"isoCode",billingAccount.getString("accountCurrencyUomId")),locale));
                        }
                    }
                }
            }

            // check the currency
            if (billingAccount.get("accountCurrencyUomId") != null && payment.get("currencyUomId") != null && 
                    !billingAccount.getString("accountCurrencyUomId").equals(payment.getString("currencyUomId"))) {
                errorMessageList.add(UtilProperties.getMessage(resource, "AccountingBillingAccountCurrencyProblem",
                        UtilMisc.toMap("billingAccountId",billingAccountId,"accountCurrencyUomId",billingAccount.getString("accountCurrencyUomId"),
                                "paymentId",paymentId,"paymentCurrencyUomId", payment.getString("currencyUomId")),locale));
            }

            if (debug) Debug.logInfo("Billing Account info retrieved and checked...", module);
        }

        // get the invoice (item) information
        BigDecimal invoiceApplyAvailable = ZERO;
        // amount available on the invoice reduced by the already applied amounts
        BigDecimal invoiceItemApplyAvailable = ZERO;
        // amount available on the invoiceItem reduced by the already applied amounts
        GenericValue invoice = null;
        GenericValue invoiceItem = null;
        if (invoiceId != null) {
            try {
                invoice = delegator.findByPrimaryKey("Invoice", UtilMisc.toMap("invoiceId", invoiceId));
            } catch (GenericEntityException e) {
                ServiceUtil.returnError(e.getMessage());
            }
            
            if (invoice == null) {
                errorMessageList.add(UtilProperties.getMessage(resource, "AccountingInvoiceNotFound",UtilMisc.toMap("invoiceId",invoiceId),locale));
            }
            else { // check the invoice and when supplied the invoice item...
                
                if (invoice.getString("statusId").equals("INVOICE_CANCELLED")) {
                    errorMessageList.add(UtilProperties.getMessage(resource,"AccountingInvoiceCancelledCannotApplyTo",UtilMisc.toMap("invoiceId",invoiceId),locale));
                }
                
                // check if the invoice already covered by payments
                BigDecimal invoiceTotal = InvoiceWorker.getInvoiceTotalBd(invoice);
                invoiceApplyAvailable = InvoiceWorker.getInvoiceNotApplied(invoice);
                
                // adjust the amountAppliedMax value if required....
                if (invoiceApplyAvailable.compareTo(amountAppliedMax) < 0) {
                    amountAppliedMax = invoiceApplyAvailable;
                }
                
                if (invoiceTotal.signum() == 0) {
                    errorMessageList.add(UtilProperties.getMessage(resource,"AccountingInvoiceTotalZero",UtilMisc.toMap("invoiceId",invoiceId),locale));
                } else if (paymentApplicationId == null) { 
                    // only check for new records here...updates are checked in the paymentApplication section
                    if (invoiceApplyAvailable.signum() == 0) {
                        errorMessageList.add(UtilProperties.getMessage(resource,"AccountingInvoiceCompletelyApplied",UtilMisc.toMap("invoiceId",invoiceId),locale));
                    }
                    // check here for too much application if a new record(s) are
                    // added (paymentApplicationId == null)
                    else if (amountApplied.compareTo(invoiceApplyAvailable) > 0) {
                        errorMessageList.add(UtilProperties.getMessage(resource, "AccountingInvoiceLessRequested",
                                UtilMisc.toMap("invoiceId",invoiceId, 
                                            "invoiceApplyAvailable",invoiceApplyAvailable,
                                            "amountApplied",amountApplied,"isoCode",invoice.getString("currencyUomId")),locale));
                    }
                }
                
                // check if at least one sender is the same as one receiver on the invoice
                if (!payment.getString("partyIdFrom").equals(invoice.getString("partyId")) && 
                        !payment.getString("partyIdTo").equals(invoice.getString("partyIdFrom")))    {
                    errorMessageList.add(UtilProperties.getMessage(resource, "AccountingFromPartySameToParty", locale));
                }
                
                if (debug) Debug.logInfo("Invoice info retrieved and checked ...", module);
            }
            
            // if provided check the invoice item.
            if (invoiceItemSeqId != null) { 
                // when itemSeqNr not provided delay checking on invoiceItemSeqId
                try {
                    invoiceItem = delegator.findByPrimaryKey("InvoiceItem", UtilMisc.toMap("invoiceId", invoiceId, "invoiceItemSeqId", invoiceItemSeqId));
                } catch (GenericEntityException e) {
                    ServiceUtil.returnError(e.getMessage());
                }
                
                if (invoiceItem == null) {
                    errorMessageList.add(UtilProperties.getMessage(resource,"AccountingInvoiceItemNotFound",UtilMisc.toMap("invoiceId",invoiceId,"invoiceItemSeqId",invoiceItemSeqId),locale));
                } else {
                    if (invoiceItem.get("uomId") != null && payment.get("currencyUomId") != null && !invoiceItem.getString("uomId").equals(payment.getString("currencyUomId"))) {
                        errorMessageList.add(UtilProperties.getMessage(resource,"AccountingInvoiceItemPaymentCurrencyProblem",UtilMisc.toMap("paymentCurrencyId",payment.getString("currencyUomId"),"itemCurrency",invoiceItem.getString("uomId")) ,locale));
                    } else if (invoice.get("currencyUomId") != null && payment.get("currencyUomId") != null && !invoice.getString("currencyUomId").equals(payment.getString("currencyUomId"))) {
                        errorMessageList.add(UtilProperties.getMessage(resource,"AccountingInvoicePaymentCurrencyProblem",UtilMisc.toMap("paymentCurrencyId",payment.getString("currencyUomId"),"itemCurrency",invoice.getString("currencyUomId")) ,locale));
                    }
                    
                    // get the invoice item applied value
                    BigDecimal quantity = null;
                    if (invoiceItem.get("quantity") == null) {
                        quantity = new BigDecimal("1");
                    } else {
                        quantity = invoiceItem.getBigDecimal("quantity").setScale(decimals,rounding);
                    }
                    invoiceItemApplyAvailable = invoiceItem.getBigDecimal("amount").multiply(quantity).setScale(decimals,rounding).subtract(InvoiceWorker.getInvoiceItemAppliedBd(invoiceItem));
                    // check here for too much application if a new record is added
                    // (paymentApplicationId == null)
                    if (paymentApplicationId == null && amountApplied.compareTo(invoiceItemApplyAvailable) > 0) { 
                        // new record
                        errorMessageList.add("Invoice(" + invoiceId + ") item(" + invoiceItemSeqId + ") has  " + invoiceItemApplyAvailable + " to apply but " + amountApplied + " is requested\n");
                        String uomId = null;
                        if (invoiceItem.getString("uomId") != null) {
                            uomId = invoiceItem.getString("uomId");
                        }
                        else {
                            uomId = invoice.getString("currencyUomId");
                        }
                        errorMessageList.add(UtilProperties.getMessage(resource, "AccountingInvoiceItemLessRequested",
                                UtilMisc.toMap("invoiceId",invoiceId, "invoiceItemSeqId", invoiceItemSeqId,
                                            "invoiceItemApplyAvailable",invoiceItemApplyAvailable,
                                            "amountApplied",amountApplied,"isoCode",uomId),locale));
                    }
                }
                if (debug) Debug.logInfo("InvoiceItem info retrieved and checked against the Invoice (currency and amounts) ...", module);
            }
        }

        // get the application record if the applicationId is supplied if not
        // create empty record.
        BigDecimal newInvoiceApplyAvailable = invoiceApplyAvailable; 
        // amount available on the invoice taking into account if the invoiceItemnumber has changed
        BigDecimal newInvoiceItemApplyAvailable = invoiceItemApplyAvailable; 
        // amount available on the invoiceItem taking into account if the itemnumber has changed
        BigDecimal newToPaymentApplyAvailable = toPaymentApplyAvailable; 
        // amount available on the Billing Account taking into account if the billing account number has changed
        BigDecimal newBillingAccountApplyAvailable = billingAccountApplyAvailable; 
        // amount available on the Billing Account taking into account if the billing account number has changed
        BigDecimal newPaymentApplyAvailable = paymentApplyAvailable;
        GenericValue paymentApplication = null;
        if (paymentApplicationId == null) {
            paymentApplication = delegator.makeValue("PaymentApplication", null); 
            // prepare for creation
        } else { // retrieve existing paymentApplication
            try {
                paymentApplication = delegator.findByPrimaryKey("PaymentApplication", UtilMisc.toMap("paymentApplicationId", paymentApplicationId));
            } catch (GenericEntityException e) {
                ServiceUtil.returnError(e.getMessage());
            }

            if (paymentApplication == null) {
                errorMessageList.add(UtilProperties.getMessage(resource, "AccountingPaymentApplicationNotFound", UtilMisc.toMap("paymentApplicationId",paymentApplicationId), locale));
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
                newPaymentApplyAvailable = paymentApplyAvailable.add(paymentApplication.getBigDecimal("amountApplied")).subtract(amountApplied).setScale(decimals, rounding);
                if (newPaymentApplyAvailable.compareTo(ZERO) < 0) {
                    errorMessageList.add(UtilProperties.getMessage(resource, "AccountingPaymentNotEnough", UtilMisc.toMap("paymentId",paymentId,"paymentApplyAvailable",paymentApplyAvailable.add(paymentApplication.getBigDecimal("amountApplied")),"amountApplied",amountApplied),locale));
                }

                if (invoiceId != null) { 
                    // only when we are processing an invoice on existing paymentApplication check invoice item for to much application if the invoice
                    // number did not change
                    if (invoiceId.equals(paymentApplication .getString("invoiceId"))) {
                        // check if both the itemNumbers are null then this is a
                        // record for the whole invoice
                        if (invoiceItemSeqId == null && paymentApplication.get("invoiceItemSeqId") == null) {
                            newInvoiceApplyAvailable = invoiceApplyAvailable.add(paymentApplication.getBigDecimal("amountApplied")).subtract(amountApplied).setScale(decimals, rounding);
                            if (invoiceApplyAvailable.compareTo(ZERO) < 0) {
                                errorMessageList.add(UtilProperties.getMessage(resource, "AccountingInvoiceNotEnough",UtilMisc.toMap("tooMuch",newInvoiceApplyAvailable.negate(),"invoiceId",invoiceId),locale));
                            }
                        } else if (invoiceItemSeqId == null && paymentApplication.get("invoiceItemSeqId") != null) {
                            // check if the item number changed from a real Item number to a null value
                            newInvoiceApplyAvailable = invoiceApplyAvailable.add(paymentApplication.getBigDecimal("amountApplied")).subtract(amountApplied).setScale(decimals, rounding);
                            if (invoiceApplyAvailable.compareTo(ZERO) < 0) {
                                errorMessageList.add(UtilProperties.getMessage(resource, "AccountingInvoiceNotEnough",UtilMisc.toMap("tooMuch",newInvoiceApplyAvailable.negate(),"invoiceId",invoiceId),locale));
                            }
                        } else if (invoiceItemSeqId != null && paymentApplication.get("invoiceItemSeqId") == null) {
                            // check if the item number changed from a null value to
                            // a real Item number
                            newInvoiceItemApplyAvailable = invoiceItemApplyAvailable.subtract(amountApplied).setScale(decimals, rounding);
                            if (newInvoiceItemApplyAvailable.compareTo(ZERO) < 0) {
                                errorMessageList.add(UtilProperties.getMessage(resource, "AccountingItemInvoiceNotEnough",UtilMisc.toMap("tooMuch",newInvoiceItemApplyAvailable.negate(),"invoiceId",invoiceId,"invoiceItemSeqId",invoiceItemSeqId),locale));
                            }
                        } else if (invoiceItemSeqId.equals(paymentApplication.getString("invoiceItemSeqId"))) {
                            // check if the real item numbers the same
                            // item number the same numeric value
                            newInvoiceItemApplyAvailable = invoiceItemApplyAvailable.add(paymentApplication.getBigDecimal("amountApplied")).subtract(amountApplied).setScale(decimals, rounding);
                            if (newInvoiceItemApplyAvailable.compareTo(ZERO) < 0) {
                                errorMessageList.add(UtilProperties.getMessage(resource, "AccountingItemInvoiceNotEnough",UtilMisc.toMap("tooMuch",newInvoiceItemApplyAvailable.negate(),"invoiceId",invoiceId,"invoiceItemSeqId",invoiceItemSeqId),locale));
                            }
                        } else {
                            // item number changed only check new item
                            newInvoiceItemApplyAvailable = invoiceItemApplyAvailable.add(amountApplied).setScale(decimals, rounding);
                            if (newInvoiceItemApplyAvailable.compareTo(ZERO) < 0) {
                                errorMessageList.add(UtilProperties.getMessage(resource, "AccountingItemInvoiceNotEnough",UtilMisc.toMap("tooMuch",newInvoiceItemApplyAvailable.negate(),"invoiceId",invoiceId,"invoiceItemSeqId",invoiceItemSeqId),locale));
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
                        newInvoiceApplyAvailable = invoiceApplyAvailable.add(paymentApplication.getBigDecimal("amountApplied").subtract(amountApplied)).setScale(decimals, rounding);
                        if (newInvoiceApplyAvailable.compareTo(ZERO) < 0) {
                            errorMessageList.add(UtilProperties.getMessage(resource, "AccountingInvoiceNotEnough",UtilMisc.toMap("tooMuch",invoiceApplyAvailable.add(paymentApplication.getBigDecimal("amountApplied")).subtract(amountApplied),"invoiceId",invoiceId),locale));
                        }
                    }
                }

                // check the toPayment account when only the amountApplied has
                // changed,
                if (toPaymentId != null && toPaymentId.equals(paymentApplication.getString("toPaymentId"))) {
                    newToPaymentApplyAvailable = toPaymentApplyAvailable.subtract(paymentApplication.getBigDecimal("amountApplied")).add(amountApplied).setScale(decimals, rounding);
                    if (newToPaymentApplyAvailable.compareTo(ZERO) < 0) {
                        errorMessageList.add(UtilProperties.getMessage(resource, "AccountingPaymentNotEnough", UtilMisc.toMap("paymentId",toPaymentId,"paymentApplyAvailable",newToPaymentApplyAvailable,"amountApplied",amountApplied),locale));
                    }
                } else if (toPaymentId != null) {
                    // billing account entered number has changed so we have to
                    // check the new billing account number.
                    newToPaymentApplyAvailable = toPaymentApplyAvailable.add(amountApplied).setScale(decimals, rounding);
                    if (newToPaymentApplyAvailable.compareTo(ZERO) < 0) {
                        errorMessageList.add(UtilProperties.getMessage(resource, "AccountingPaymentNotEnough", UtilMisc.toMap("paymentId",toPaymentId,"paymentApplyAvailable",newToPaymentApplyAvailable,"amountApplied",amountApplied),locale));
                    }

                }
                // check the billing account when only the amountApplied has
                // changed,
                // change in account number is already checked in the billing
                // account section
                if (billingAccountId != null && billingAccountId.equals(paymentApplication.getString("billingAccountId"))) {
                    newBillingAccountApplyAvailable = billingAccountApplyAvailable.subtract(paymentApplication.getBigDecimal("amountApplied")).add(amountApplied).setScale(decimals, rounding);
                    if (newBillingAccountApplyAvailable.compareTo(ZERO) < 0) {
                        errorMessageList.add(UtilProperties.getMessage(resource, "AccountingBillingAccountNotEnough",UtilMisc.toMap("billingAccountId",billingAccountId,"newBillingAccountApplyAvailable",newBillingAccountApplyAvailable,"amountApplied",amountApplied,"isoCode", billingAccount.getString("accountCurrencyUomId")),locale));
                    }
                } else if (billingAccountId != null) {
                    // billing account entered number has changed so we have to
                    // check the new billing account number.
                    newBillingAccountApplyAvailable = billingAccountApplyAvailable.add(amountApplied).setScale(decimals, rounding);
                    if (newBillingAccountApplyAvailable.compareTo(ZERO) < 0) {
                        errorMessageList.add(UtilProperties.getMessage(resource, "AccountingBillingAccountNotEnough",UtilMisc.toMap("billingAccountId",billingAccountId,"newBillingAccountApplyAvailable",newBillingAccountApplyAvailable,"amountApplied",amountApplied,"isoCode", billingAccount.getString("accountCurrencyUomId")),locale));
                    }

                }
            }
            if (debug) Debug.logInfo("paymentApplication record info retrieved and checked...", module);
        }

        // show the maximumus what can be added in the payment application file.
        String toMessage = null;  // prepare for success message
        if (debug) {
            String extra = "";
            if (invoiceItemSeqId != null) {
                extra = " Invoice item(" + invoiceItemSeqId + ") amount not yet applied: " + newInvoiceItemApplyAvailable;
            }
            Debug.logInfo("checking finished, start processing with the following data... ", module);
            if (invoiceId != null) {
                Debug.logInfo(" Invoice(" + invoiceId + ") amount not yet applied: " + newInvoiceApplyAvailable + extra + " Payment(" + paymentId + ") amount not yet applied: " + newPaymentApplyAvailable +  " Requested amount to apply:" + amountApplied, module);
                toMessage = UtilProperties.getMessage(resource, "AccountingApplicationToInvoice",UtilMisc.toMap("invoiceId",invoiceId),locale);
                if(extra.length() > 0) toMessage = UtilProperties.getMessage(resource, "AccountingApplicationToInvoiceItem",UtilMisc.toMap("invoiceId",invoiceId,"invoiceItemSeqId",invoiceItemSeqId),locale);
            }
            if (toPaymentId != null) {
                Debug.logInfo(" toPayment(" + toPaymentId + ") amount not yet applied: " + newToPaymentApplyAvailable + " Payment(" + paymentId + ") amount not yet applied: " + newPaymentApplyAvailable + " Requested amount to apply:" + amountApplied, module);
                toMessage = UtilProperties.getMessage(resource, "AccountingApplicationToPayment",UtilMisc.toMap("paymentId",toPaymentId),locale);
            }
            if (billingAccountId != null) {
                Debug.logInfo(" billingAccount(" + billingAccountId + ") amount not yet applied: " + newBillingAccountApplyAvailable + " Payment(" + paymentId + ") amount not yet applied: " + newPaymentApplyAvailable + " Requested amount to apply:" + amountApplied, module);
                toMessage = UtilProperties.getMessage(resource, "AccountingApplicationToBillingAccount",UtilMisc.toMap("billingAccountId",billingAccountId),locale);
            }
            if (taxAuthGeoId != null) {
                Debug.logInfo(" taxAuthGeoId(" + taxAuthGeoId + ")  Payment(" + paymentId + ") amount not yet applied: " + newPaymentApplyAvailable + " Requested amount to apply:" + amountApplied, module);
                toMessage = UtilProperties.getMessage(resource, "AccountingApplicationToTax",UtilMisc.toMap("taxAuthGeoId",taxAuthGeoId),locale);
            }
        }
        // if the amount to apply was not provided or was zero fill it with the maximum possible and provide information to the user
        if (amountApplied.signum() == 0 &&  useHighestAmount.equals("Y")) {
            amountApplied = newPaymentApplyAvailable;
            if (invoiceId != null && newInvoiceApplyAvailable.compareTo(amountApplied) < 0) {
                amountApplied = newInvoiceApplyAvailable;
                toMessage = UtilProperties.getMessage(resource, "AccountingApplicationToInvoice",UtilMisc.toMap("invoiceId",invoiceId),locale);
            }
            if (toPaymentId != null && newToPaymentApplyAvailable.compareTo(amountApplied) == 1) {
                amountApplied = newToPaymentApplyAvailable;
                toMessage = UtilProperties.getMessage(resource, "AccountingApplicationToPayment",UtilMisc.toMap("paymentId",toPaymentId),locale);
            }
        }
        
        if (amountApplied.signum() == 0) {
            errorMessageList.add(UtilProperties.getMessage(resource, "AccountingNoAmount",locale));
        }
        else {
            successMessage = UtilProperties.getMessage(resource, "AccountingApplicationSuccess",UtilMisc.toMap("amountApplied",amountApplied,"paymentId",paymentId,"isoCode", payment.getString("currencyUomId"),"toMessage",toMessage),locale);
        }
        
        // report error messages if any
        if (errorMessageList.size() > 0) {
            return ServiceUtil.returnError(errorMessageList);
        }

        // ============ start processing ======================
           // if the application is specified it is easy, update the existing record only
        if (paymentApplicationId != null) { 
            // record is already retrieved previously
            if (debug) Debug.logInfo("Process an existing paymentApplication record: " + paymentApplicationId, module);
            // update the current record
            paymentApplication.set("invoiceId", invoiceId);
            paymentApplication.set("invoiceItemSeqId", invoiceItemSeqId);
            paymentApplication.set("paymentId", paymentId);
            paymentApplication.set("toPaymentId", toPaymentId);
            paymentApplication.set("amountApplied", new Double(amountApplied.doubleValue()));
            paymentApplication.set("billingAccountId", billingAccountId);
            paymentApplication.set("taxAuthGeoId", taxAuthGeoId);
            return storePaymentApplication(delegator, paymentApplication,locale);
        }

        // if no invoice sequence number is provided it assumed the requested paymentAmount will be
        // spread over the invoice starting with the lowest sequence number if
        // itemprocessing is on otherwise creat one record
        if (invoiceId != null && paymentId != null && (invoiceItemSeqId == null)) {
            if (invoiceProcessing) { 
                // create only a single record with a null seqId
                if (debug) Debug.logInfo("Try to allocate the payment to the invoice as a whole", module);
                paymentApplication.set("paymentId", paymentId);
                paymentApplication.set("toPaymentId",null);
                paymentApplication.set("invoiceId", invoiceId);
                paymentApplication.set("invoiceItemSeqId", null);
                paymentApplication.set("toPaymentId", null);
                paymentApplication.set("amountApplied", new Double(amountApplied.doubleValue()));
                paymentApplication.set("billingAccountId", null);
                paymentApplication.set("taxAuthGeoId", null);
                if (debug) Debug.logInfo("creating new paymentapplication", module);
                return storePaymentApplication(delegator, paymentApplication,locale);
            } else { // spread the amount over every single item number
                if (debug) Debug.logInfo("Try to allocate the payment to the itemnumbers of the invoice", module);
                // get the invoice items
                List invoiceItems = null;
                try {
                    invoiceItems = delegator.findByAnd("InvoiceItem", UtilMisc.toMap("invoiceId", invoiceId));
                } catch (GenericEntityException e) {
                    ServiceUtil.returnError(e.getMessage());
                }
                if (invoiceItems == null || invoiceItems.size() == 0) {
                    errorMessageList.add("No invoice items found for invoice " + invoiceId + " to match payment against...\n");
                    return ServiceUtil.returnError(errorMessageList);
                } else { // we found some invoice items, start processing....
                    Iterator i = invoiceItems.iterator();
                    // check if the user want to apply a smaller amount than the maximum possible on the payment
                    if (amountApplied.signum() != 0 && amountApplied.compareTo(paymentApplyAvailable) < 0)    {
                        paymentApplyAvailable = amountApplied;
                    }
                    while (i.hasNext() && paymentApplyAvailable.compareTo(ZERO) > 0) {
                        // get the invoiceItem
                        invoiceItem = (GenericValue) i.next();
                        if (debug) Debug.logInfo("Start processing item: " + invoiceItem.getString("invoiceItemSeqId"), module);
                        BigDecimal itemQuantity = new BigDecimal("1");
                        if (invoiceItem.get("quantity") != null && invoiceItem.getBigDecimal("quantity").signum() != 0) {
                            itemQuantity = new BigDecimal(invoiceItem.getString("quantity")).setScale(decimals,rounding);
                        }
                        BigDecimal itemAmount = invoiceItem.getBigDecimal("amount").setScale(decimals,rounding);
                        BigDecimal itemTotal = itemAmount.multiply(itemQuantity).setScale(decimals,rounding);

                        // get the application(s) already allocated to this
                        // item, if available
                        List paymentApplications = null;
                        try {
                            paymentApplications = invoiceItem.getRelated("PaymentApplication");
                        } catch (GenericEntityException e) {
                            ServiceUtil.returnError(e.getMessage());
                        }
                        BigDecimal tobeApplied = ZERO; 
                        // item total amount - already applied (if any)
                        BigDecimal alreadyApplied = ZERO;
                        if (paymentApplications != null && paymentApplications.size() > 0) { 
                            // application(s) found, add them all together
                            Iterator p = paymentApplications.iterator();
                            while (p.hasNext()) {
                                paymentApplication = (GenericValue) p.next();
                                alreadyApplied = alreadyApplied.add(paymentApplication.getBigDecimal("amountApplied").setScale(decimals,rounding));
                            }
                            tobeApplied = itemTotal.subtract(alreadyApplied).setScale(decimals,rounding);
                        } else { 
                            // no application connected yet
                            tobeApplied = itemTotal;
                        }
                        if (debug) Debug.logInfo("tobeApplied:(" + tobeApplied + ") = " + "itemTotal(" + itemTotal + ") - alreadyApplied(" + alreadyApplied + ") but not more then (nonapplied) paymentAmount(" + paymentApplyAvailable + ")", module);

                        if (tobeApplied.signum() == 0) { 
                            // invoiceItem already fully applied so look at the next one....
                            continue;
                        }

                        if (paymentApplyAvailable.compareTo(tobeApplied) > 0) {
                            paymentApplyAvailable = paymentApplyAvailable.subtract(tobeApplied);
                        } else {
                            tobeApplied = paymentApplyAvailable;
                            paymentApplyAvailable = ZERO;
                        }

                        // create application payment record but check currency
                        // first if supplied
                        if (invoiceItem.get("uomId") != null && payment.get("currencyUomId") != null && !invoiceItem.getString("uomId").equals(payment.getString("currencyUomId"))) {
                            errorMessageList.add("Payment currency (" + payment.getString("currencyUomId") + ") and invoice Item(" + invoiceItem.getString("invoiceItemSeqId") + ") currency(" + invoiceItem.getString("uomId") + ") not the same\n");
                        } else if (invoice.get("currencyUomId") != null && payment.get("currencyUomId") != null && !invoice.getString("currencyUomId").equals( payment.getString("currencyUomId"))) {
                            errorMessageList.add("Payment currency (" + payment.getString("currencyUomId") + ") and invoice currency(" + invoice.getString("currencyUomId") + ") not the same\n");
                        } else {
                            paymentApplication.set("paymentApplicationId", null);
                            // make sure we get a new record
                            paymentApplication.set("invoiceId", invoiceId);
                            paymentApplication.set("invoiceItemSeqId", invoiceItem.getString("invoiceItemSeqId"));
                            paymentApplication.set("paymentId", paymentId);
                            paymentApplication.set("toPaymentId", toPaymentId);
                            paymentApplication.set("amountApplied", new Double( tobeApplied.doubleValue()));
                            paymentApplication.set("billingAccountId", billingAccountId);
                            paymentApplication.set("taxAuthGeoId", taxAuthGeoId);
                            storePaymentApplication(delegator, paymentApplication,locale);
                        }

                        // check if either the invoice or the payment is fully
                        // applied, when yes change the status to paid
                        // which triggers the ledger routines....
                        /*
                         * if
                         * (InvoiceWorker.getInvoiceTotalBd(invoice).equals(InvoiceWorker.getInvoiceAppliedBd(invoice))) {
                         * try { dispatcher.runSync("setInvoiceStatus",
                         * UtilMisc.toMap("invoiceId",invoiceId,"statusId","INVOICE_PAID")); }
                         * catch (GenericServiceException e1) {
                         * Debug.logError(e1, "Error updating invoice status",
                         * module); } }
                         * 
                         * if
                         * (payment.getBigDecimal("amount").equals(PaymentWorker.getPaymentAppliedBd(payment))) {
                         * GenericValue appliedPayment = (GenericValue)
                         * delegator.makeValue("Payment",
                         * UtilMisc.toMap("paymentId",paymentId,"statusId","INVOICE_PAID"));
                         * try { appliedPayment.store(); } catch
                         * (GenericEntityException e) {
                         * ServiceUtil.returnError(e.getMessage()); } }
                         */
                    }

                    if (errorMessageList.size() > 0) {
                        return ServiceUtil.returnError(errorMessageList);
                    } else {
                        if (successMessage != null) {
                            return ServiceUtil.returnSuccess(successMessage);
                        }
                        else {
                            return ServiceUtil.returnSuccess();
                        }
                    }
                }
            }
        }
        
        // if no paymentApplicationId supplied create a new record with the data
        // supplied...
        if (paymentApplicationId == null && amountApplied != null) {
            paymentApplication.set("paymentApplicationId", paymentApplicationId);
            paymentApplication.set("invoiceId", invoiceId);
            paymentApplication.set("invoiceItemSeqId", invoiceItemSeqId);
            paymentApplication.set("paymentId", paymentId);
            paymentApplication.set("toPaymentId", toPaymentId);
            paymentApplication.set("amountApplied", new Double(amountApplied.doubleValue()));
            paymentApplication.set("billingAccountId", billingAccountId);
            paymentApplication.set("taxAuthGeoId", taxAuthGeoId);
            return storePaymentApplication(delegator, paymentApplication,locale);
        }

        // should never come here...
        errorMessageList.add("??unsuitable parameters passed...?? This message.... should never be shown\n");
        errorMessageList.add("--Input parameters...InvoiceId:" + invoiceId + " invoiceItemSeqId:" + invoiceItemSeqId + " PaymentId:" + paymentId + " toPaymentId:" + toPaymentId + "\n  paymentApplicationId:" + paymentApplicationId + " amountApplied:" + amountApplied);
        return ServiceUtil.returnError(errorMessageList);
    }

    public static Map calculateInvoicedAdjustmentTotalBd(DispatchContext dctx, Map context) {
        GenericDelegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get("locale");
        GenericValue orderAdjustment = (GenericValue) context.get("orderAdjustment");
        Map result = ServiceUtil.returnSuccess();
        
        BigDecimal invoicedTotal = ZERO;
        List invoicedAdjustments = null;
        try {
            invoicedAdjustments = delegator.findByAnd("OrderAdjustmentBilling", UtilMisc.toMap("orderAdjustmentId", orderAdjustment.getString("orderAdjustmentId")));
        } catch( GenericEntityException e ) {
            String errMsg = UtilProperties.getMessage(resource, "AccountingTroubleCallingCalculateInvoicedAdjustmentTotalService" + ": " + e.getMessage(), locale);
            Debug.logError(e, errMsg, module);
            return ServiceUtil.returnError(errMsg);
        }
        Iterator iait = invoicedAdjustments.iterator();
        while (iait.hasNext()) {
            GenericValue invoicedAdjustment = (GenericValue) iait.next();
            invoicedTotal = invoicedTotal.add(invoicedAdjustment.getBigDecimal("amount").setScale(decimals, rounding));
        }
        result.put("invoicedTotal", invoicedTotal);
        return result;
    }

    /**
     * Update/add to the paymentApplication table and making sure no duplicate
     * record exist
     * 
     * @param delegator
     * @param paymentApplication
     * @return map results
     */
    private static Map storePaymentApplication(GenericDelegator delegator, GenericValue paymentApplication,Locale locale) {
        Map results = ServiceUtil.returnSuccess(successMessage);
        boolean debug = true;
        if (debug) Debug.logInfo("Start updating the paymentApplication table ", module);

        if (decimals == -1 || rounding == -1) {
            return ServiceUtil.returnError("Arithmetic properties for Invoice services not configured properly. Cannot proceed.");
        }
        
        // check if a record already exists with this data
        List checkAppls = null;
        try {
            checkAppls = delegator.findByAnd("PaymentApplication", UtilMisc.toMap(
                    "invoiceId", paymentApplication.get("invoiceId"),
                    "invoiceItemSeqId", paymentApplication.get("invoiceItemSeqId"),
                    "billingAccountId", paymentApplication.get("billingAccountId"), 
                    "paymentId", paymentApplication.get("paymentId"), 
                    "toPaymentId", paymentApplication.get("toPaymentId"), 
                    "taxAuthGeoId", paymentApplication.get("taxAuthGeoId")));
        } catch (GenericEntityException e) {
            ServiceUtil.returnError(e.getMessage());
        }
        if (checkAppls != null && checkAppls.size() > 0) {
            if (debug) Debug.logInfo(checkAppls.size() + " records already exist", module);
            // 1 record exists just update and if diffrent ID delete other record and add together.
            GenericValue checkAppl = (GenericValue) checkAppls.get(0);
            // if new record  add to the already existing one.
            if ( paymentApplication.get("paymentApplicationId") == null)    {
                // add 2 amounts together
                checkAppl.set("amountApplied", new Double(paymentApplication.getBigDecimal("amountApplied").
                        add(checkAppl.getBigDecimal("amountApplied")).setScale(decimals,rounding).doubleValue()));
                if (debug)     Debug.logInfo("Update paymentApplication record: " + checkAppl.getString("paymentApplicationId") + " with appliedAmount:" + checkAppl.getBigDecimal("amountApplied"), module);
                try {
                    checkAppl.store();
                } catch (GenericEntityException e) {
                    ServiceUtil.returnError(e.getMessage());
                }
            } else if (paymentApplication.getString("paymentApplicationId").equals(checkAppl.getString("paymentApplicationId"))) {
                // update existing record inplace
                checkAppl.set("amountApplied", new Double(paymentApplication.getBigDecimal("amountApplied").doubleValue()));
                if (debug)     Debug.logInfo("Update paymentApplication record: " + checkAppl.getString("paymentApplicationId") + " with appliedAmount:" + checkAppl.getBigDecimal("amountApplied"), module);
                try {
                    checkAppl.store();
                } catch (GenericEntityException e) {
                    ServiceUtil.returnError(e.getMessage());
                }
            } else    { // two existing records, an updated one added to the existing one
                // add 2 amounts together
                checkAppl.set("amountApplied", new Double(paymentApplication.getBigDecimal("amountApplied").
                        add(checkAppl.getBigDecimal("amountApplied")).setScale(decimals,rounding).doubleValue()));
                // delete paymentApplication record and update the checkAppls one.
                if (debug) Debug.logInfo("Delete paymentApplication record: " + paymentApplication.getString("paymentApplicationId") + " with appliedAmount:" + paymentApplication.getBigDecimal("amountApplied"), module);
                try {
                    paymentApplication.remove();
                } catch (GenericEntityException e) {
                    ServiceUtil.returnError(e.getMessage());
                }
                // update amount existing record
                if (debug)     Debug.logInfo("Update paymentApplication record: " + checkAppl.getString("paymentApplicationId") + " with appliedAmount:" + checkAppl.getBigDecimal("amountApplied"), module);
                try {
                    checkAppl.store();
                } catch (GenericEntityException e) {
                    ServiceUtil.returnError(e.getMessage());
                }
            }
        } else {
            if (debug) Debug.logInfo("No records found with paymentId,invoiceid..etc probaly changed one of them...", module);
            // create record if ID null;
            if (paymentApplication.get("paymentApplicationId") == null) {
                paymentApplication.set("paymentApplicationId", delegator.getNextSeqId("PaymentApplication"));
                    if (debug) Debug.logInfo("Create new paymentAppication record: " + paymentApplication.getString("paymentApplicationId") + " with appliedAmount:" + paymentApplication.getBigDecimal("amountApplied"), module);
                try {
                    paymentApplication.create();
                } catch (GenericEntityException e) {
                    ServiceUtil.returnError(e.getMessage());
                }
            } else {
                // update existing record (could not be found because a non existing combination of paymentId/invoiceId/invoiceSeqId/ etc... was provided
                if (debug) Debug.logInfo("Update existing paymentApplication record: " + paymentApplication.getString("paymentApplicationId") + " with appliedAmount:" + paymentApplication.getBigDecimal("amountApplied"), module);
                try {
                    paymentApplication.store();
                } catch (GenericEntityException e) {
                    ServiceUtil.returnError(e.getMessage());
                }
            }
        }
        successMessage = successMessage.concat(UtilProperties.getMessage(resource, "AccountingSuccessFull",locale));
        return results;
    }

    public static Map checkPaymentInvoices(DispatchContext dctx, Map context) {
        GenericDelegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        String paymentId = (String) context.get("paymentId");
        try {
            GenericValue payment = delegator.findByPrimaryKey("Payment", UtilMisc.toMap("paymentId", paymentId));
            if (payment == null) throw new GenericServiceException("Payment with ID [" + paymentId  + "] not found!");

            List paymentApplications = payment.getRelated("PaymentApplication");
            if (UtilValidate.isEmpty(paymentApplications)) return ServiceUtil.returnSuccess();

            // TODO: this is inefficient -- instead use HashSet to construct a distinct Set of invoiceIds, then iterate over it and call checkInvoicePaymentAppls
            Iterator iter = paymentApplications.iterator();
            while (iter.hasNext()) {
                GenericValue paymentApplication = (GenericValue) iter.next();
                String invoiceId = paymentApplication.getString("invoiceId");
                if (invoiceId != null) {
                    Map serviceResult = dispatcher.runSync("checkInvoicePaymentApplications", UtilMisc.toMap("invoiceId", invoiceId, "userLogin", userLogin));
                    if (ServiceUtil.isError(serviceResult)) return serviceResult;
                }
            }
            return ServiceUtil.returnSuccess();
        } catch (GenericServiceException se) {
            Debug.logError(se, se.getMessage(), module);
            return ServiceUtil.returnError(se.getMessage());
        } catch (GenericEntityException ee) {
            Debug.logError(ee, ee.getMessage(), module);
            return ServiceUtil.returnError(ee.getMessage());
        }
    }
}
