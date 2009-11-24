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
import java.math.MathContext;
import java.sql.Timestamp;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javolution.util.FastList;
import javolution.util.FastMap;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilNumber;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityConditionList;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityUtil;

/**
 * InvoiceWorker - Worker methods of invoices
 */
public class InvoiceWorker {

    public static String module = InvoiceWorker.class.getName();
    private static BigDecimal ZERO = BigDecimal.ZERO;
    private static int decimals = UtilNumber.getBigDecimalScale("invoice.decimals");
    private static int rounding = UtilNumber.getBigDecimalRoundingMode("invoice.rounding");
    private static int taxDecimals = UtilNumber.getBigDecimalScale("salestax.calc.decimals");
    private static int taxRounding = UtilNumber.getBigDecimalRoundingMode("salestax.rounding");

    /**
     * Return the total amount of the invoice (including tax) using the the invoiceId as input.
     * @param delegator
     * @param invoiceId
     * @return
     */
    public static BigDecimal getInvoiceTotal(Delegator delegator, String invoiceId) {
        return getInvoiceTotal(delegator, invoiceId, Boolean.TRUE);
    }

    /**
     * Return the total amount of the invoice (including tax) using the the invoiceId as input.
     * with the ability to specify if the actual currency is required.
     * @param delegator
     * @param invoiceId
     * @param actualCurrency true: provide the actual currency of the invoice (could be different from the system currency)
     *                       false: if required convert the actual currency into the system currency.
     * @return
     */
    public static BigDecimal getInvoiceTotal(Delegator delegator, String invoiceId, Boolean actualCurrency) {
        if (delegator == null) {
            throw new IllegalArgumentException("Null delegator is not allowed in this method");
        }

        GenericValue invoice = null;
        try {
            invoice = delegator.findByPrimaryKey("Invoice", UtilMisc.toMap("invoiceId", invoiceId));
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
     * @param invoice GenericValue object of the Invoice
     * @return the invoice total as BigDecimal
     */
    public static BigDecimal getInvoiceItemTotal(GenericValue invoiceItem) {
        BigDecimal quantity = invoiceItem.getBigDecimal("quantity");
        if (quantity == null) {
            quantity = BigDecimal.ONE;
        }
        return quantity.multiply(invoiceItem.getBigDecimal("amount")).setScale(decimals, rounding);
    }

    /** Method to get the taxable invoice item types as a List of invoiceItemTypeIds.  These are identified in Enumeration with enumTypeId TAXABLE_INV_ITM_TY. */
    public static List getTaxableInvoiceItemTypeIds(Delegator delegator) throws GenericEntityException {
        List typeIds = FastList.newInstance();
        List invoiceItemTaxTypes = delegator.findByAndCache("Enumeration", UtilMisc.toMap("enumTypeId", "TAXABLE_INV_ITM_TY"));
        for (Iterator iter = invoiceItemTaxTypes.iterator(); iter.hasNext();) {
            GenericValue invoiceItemTaxType = (GenericValue) iter.next();
            typeIds.add(invoiceItemTaxType.getString("enumId"));
        }
        return typeIds;
    }

    public static BigDecimal getInvoiceTaxTotal(GenericValue invoice) {
        BigDecimal invoiceTaxTotal = ZERO;
        BigDecimal ONE = BigDecimal.ONE;

        if (invoice == null)
           throw new IllegalArgumentException("The invoiceId passed does not match an existing invoice");
        List invoiceTaxItems = null;
        try {
            Delegator delegator = invoice.getDelegator();
            EntityConditionList condition = EntityCondition.makeCondition(UtilMisc.toList(
                    EntityCondition.makeCondition("invoiceId", invoice.getString("invoiceId")),
                    EntityCondition.makeCondition("invoiceItemTypeId", EntityOperator.IN, getTaxableInvoiceItemTypeIds(delegator))),
                    EntityOperator.AND);
            invoiceTaxItems = delegator.findList("InvoiceItem", condition, null, null, null, false);
        } catch (GenericEntityException e) {
            Debug.logError(e, "Trouble getting InvoiceItem list", module);
        }
        if (UtilValidate.isNotEmpty(invoiceTaxItems)) {
            Iterator invoiceItemsIter = invoiceTaxItems.iterator();
            while (invoiceItemsIter.hasNext()) {
                GenericValue invoiceItem = (GenericValue) invoiceItemsIter.next();
                BigDecimal amount = invoiceItem.getBigDecimal("amount");
                BigDecimal quantity = invoiceItem.getBigDecimal("quantity");
                if (amount == null)
                    amount = ZERO;
                if (quantity == null)
                    quantity = ONE;
                invoiceTaxTotal = invoiceTaxTotal.add(amount.multiply(quantity)).setScale(decimals + 1, rounding);
            }
        }
        return invoiceTaxTotal.setScale(decimals, rounding);

    }

    public static BigDecimal getInvoiceNoTaxTotal(GenericValue invoice) {
        return getInvoiceTotal(invoice, Boolean.TRUE).subtract(getInvoiceTaxTotal(invoice));
    }

    /**
     * Method to return the total amount of an invoice
     * @param invoice GenericValue object of the Invoice
     * @return the invoice total as BigDecimal
     */
     public static BigDecimal getInvoiceTotal(GenericValue invoice) {
        return getInvoiceTotal(invoice, Boolean.TRUE);
    }

     /**
      * 
      * Return the total amount of the invoice (including tax) using the the invoice GenericValue as input.
      * with the ability to specify if the actual currency is required.
      * @param invoice
      * @param actualCurrency true: provide the actual currency of the invoice (could be different from the system currency)
      *                       false: if required convert the actual currency into the system currency.
      * @return
      */
     public static BigDecimal getInvoiceTotal(GenericValue invoice, Boolean actualCurrency) {
        BigDecimal invoiceTotal = ZERO;
        BigDecimal invoiceTaxTotal = ZERO;
        Map invoiceTaxByTaxAuthGeoAndPartyResult = getInvoiceTaxByTaxAuthGeoAndParty(invoice);
        List taxByTaxAuthGeoAndPartyList = (List) invoiceTaxByTaxAuthGeoAndPartyResult.get("taxByTaxAuthGeoAndPartyList");
        invoiceTaxTotal = (BigDecimal) invoiceTaxByTaxAuthGeoAndPartyResult.get("taxGrandTotal");

        List invoiceItems = null;
        try {
            invoiceItems = invoice.getRelated("InvoiceItem");
            if ("SALES_INVOICE".equals(invoice.getString("invoiceTypeId"))) {
                invoiceItems = EntityUtil.filterByAnd(
                        invoiceItems, UtilMisc.toList(
                                EntityCondition.makeCondition("invoiceItemTypeId", EntityOperator.NOT_EQUAL, "INV_SALES_TAX"), 
                                EntityCondition.makeCondition("invoiceItemTypeId", EntityOperator.NOT_EQUAL, "ITM_SALES_TAX")));
            } else if (("PURCHASE_INVOICE".equals(invoice.getString("invoiceTypeId")))) {
                invoiceItems = EntityUtil.filterByAnd(
                        invoiceItems, UtilMisc.toList(
                                EntityCondition.makeCondition("invoiceItemTypeId", EntityOperator.NOT_EQUAL, "PINV_SALES_TAX"), 
                                EntityCondition.makeCondition("invoiceItemTypeId", EntityOperator.NOT_EQUAL, "PITM_SALES_TAX")));
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, "Trouble getting InvoiceItem list", module);
        }
        if (UtilValidate.isNotEmpty(invoiceItems)) {
            Iterator invoiceItemsIter = invoiceItems.iterator();
            while (invoiceItemsIter.hasNext()) {
                GenericValue invoiceItem = (GenericValue) invoiceItemsIter.next();
                BigDecimal amount = invoiceItem.getBigDecimal("amount");
                BigDecimal quantity = invoiceItem.getBigDecimal("quantity");
                if (amount == null)
                    amount = ZERO;
                if (quantity == null)
                    quantity = BigDecimal.ONE;
                invoiceTotal = invoiceTotal.add( amount.multiply(quantity)).setScale(decimals,rounding);
            }
        }
        invoiceTotal = invoiceTotal.add(invoiceTaxTotal).setScale(decimals, rounding);
        if (UtilValidate.isNotEmpty(invoiceTotal) && !actualCurrency) {
            invoiceTotal = invoiceTotal.multiply(getInvoiceCurrencyConversionRate(invoice)).setScale(decimals,rounding);
        }
        return invoiceTotal;
    }

    /**
     * Method to obtain the bill to party for an invoice. Note that invoice.partyId is the bill to party.
     * @param invoice GenericValue object of the Invoice
     * @return GenericValue object of the Party
     */
    public static GenericValue getBillToParty(GenericValue invoice) {
        try {
            GenericValue billToParty = invoice.getRelatedOne("Party");
            if (billToParty != null) {
                return billToParty;
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, "Trouble getting Party from Invoice", module);
        }

        // remaining code is the old method, which we leave here for compatibility purposes
        List billToRoles = null;
        try {
            billToRoles = invoice.getRelated("InvoiceRole", UtilMisc.toMap("roleTypeId", "BILL_TO_CUSTOMER"),
                UtilMisc.toList("-datetimePerformed"));
        } catch (GenericEntityException e) {
            Debug.logError(e, "Trouble getting InvoiceRole list", module);
        }

        if (billToRoles != null) {
            GenericValue role = EntityUtil.getFirst(billToRoles);
            GenericValue party = null;
            try {
                party = role.getRelatedOne("Party");
            } catch (GenericEntityException e) {
                Debug.logError(e, "Trouble getting Party from InvoiceRole", module);
            }
            if (party != null)
                return party;
        }
        return null;
    }

    /** Convenience method to obtain the bill from party for an invoice. Note that invoice.partyIdFrom is the bill from party. */
    public static GenericValue getBillFromParty(GenericValue invoice) {
        try {
            return invoice.getRelatedOne("FromParty");
        } catch (GenericEntityException e) {
            Debug.logError(e, "Trouble getting FromParty from Invoice", module);
        }
        return null;
    }

    /**
      * Method to obtain the send from party for an invoice
      * @param invoice GenericValue object of the Invoice
      * @return GenericValue object of the Party
      */
    public static GenericValue getSendFromParty(GenericValue invoice) {
        GenericValue billFromParty = getBillFromParty(invoice);
        if (billFromParty != null) {
            return billFromParty;
        }

        // remaining code is the old method, which we leave here for compatibility purposes
        List sendFromRoles = null;
        try {
            sendFromRoles = invoice.getRelated("InvoiceRole", UtilMisc.toMap("roleTypeId", "BILL_FROM_VENDOR"),
                UtilMisc.toList("-datetimePerformed"));
        } catch (GenericEntityException e) {
            Debug.logError(e, "Trouble getting InvoiceRole list", module);
        }

        if (sendFromRoles != null) {
            GenericValue role = EntityUtil.getFirst(sendFromRoles);
            GenericValue party = null;
            try {
                party = role.getRelatedOne("Party");
            } catch (GenericEntityException e) {
                Debug.logError(e, "Trouble getting Party from InvoiceRole", module);
            }
            if (party != null)
                return party;
        }
        return null;
    }

    /**
      * Method to obtain the billing address for an invoice
      * @param invoice GenericValue object of the Invoice
      * @return GenericValue object of the PostalAddress
      */
    public static GenericValue getBillToAddress(GenericValue invoice) {
        return getInvoiceAddressByType(invoice, "BILLING_LOCATION");
    }

    /**
      * Method to obtain the sending address for an invoice
      * @param invoice GenericValue object of the Invoice
      * @return GenericValue object of the PostalAddress
      */
    public static GenericValue getSendFromAddress(GenericValue invoice) {
        return getInvoiceAddressByType(invoice, "PAYMENT_LOCATION");
    }

    public static GenericValue getInvoiceAddressByType(GenericValue invoice, String contactMechPurposeTypeId) {
        Delegator delegator = invoice.getDelegator();
        List<GenericValue> locations = null;
        // first try InvoiceContactMech to see if we can find the address needed
        try {
            locations = invoice.getRelated("InvoiceContactMech", UtilMisc.toMap("contactMechPurposeTypeId", contactMechPurposeTypeId), null);
        } catch (GenericEntityException e) {
            Debug.logError("Touble getting InvoiceContactMech entity list", module);
        }

        if (UtilValidate.isEmpty(locations))    {
            // if no locations found get it from the PartyAndContactMech using the from and to party on the invoice
            String destinationPartyId = null;
            if (invoice.getString("invoiceTypeId").equals("SALES_INVOICE"))
                destinationPartyId = invoice.getString("partyId");
            if (invoice.getString("invoiceTypeId").equals("PURCHASE_INVOICE"))
                destinationPartyId = invoice.getString("partyId");
            try {
                locations = EntityUtil.filterByDate(delegator.findByAnd("PartyContactMechPurpose",
                        UtilMisc.toMap("partyId", destinationPartyId, "contactMechPurposeTypeId", contactMechPurposeTypeId)));
            } catch (GenericEntityException e) {
                Debug.logError("Trouble getting contact party purpose list", module);
            }
            //if still not found get it from the general location
            if (UtilValidate.isEmpty(locations))    {
                try {
                    locations = EntityUtil.filterByDate(delegator.findByAnd("PartyContactMechPurpose",
                            UtilMisc.toMap("partyId", destinationPartyId, "contactMechPurposeTypeId", "GENERAL_LOCATION")));
                } catch (GenericEntityException e) {
                    Debug.logError("Trouble getting contact party purpose list", module);
                }
            }
        }

        // now return the first PostalAddress from the locations
        GenericValue postalAddress = null;
        GenericValue contactMech = null;
        if (UtilValidate.isNotEmpty(locations)) {
            try {
                contactMech = locations.get(0).getRelatedOne("ContactMech");
            } catch (GenericEntityException e) {
                Debug.logError(e, "Trouble getting Contact for contactMechId: " + locations.get(0).getString("contactMechId"), module);
            }

            if (contactMech != null && contactMech.getString("contactMechTypeId").equals("POSTAL_ADDRESS"))    {
                try {
                    postalAddress = contactMech.getRelatedOne("PostalAddress");
                    return postalAddress;
                } catch (GenericEntityException e) {
                    Debug.logError(e, "Trouble getting PostalAddress for contactMechId: " + contactMech.getString("contactMechId"), module);
                }
            }
        }
        return contactMech;
    }

    private static GenericValue getAddressFromParty(GenericValue party, String purposeTypeId) {
        if (party == null) return null;

        GenericValue contactMech = null;
        GenericValue postalAddress = null;
        try {
            List mecs = party.getRelated("PartyContactMechPurpose",
                UtilMisc.toMap("contactMechPurposeTypeId", purposeTypeId), null);
            if (mecs != null) {
                List filteredMecs = EntityUtil.filterByDate(mecs);
                GenericValue mecPurpose = EntityUtil.getFirst(filteredMecs);
                if (mecPurpose != null)
                    contactMech = mecPurpose.getRelatedOne("ContactMech");
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, "Trouble getting current ContactMech for Party/Purpose", module);
        }

        if (contactMech != null) {
            if (contactMech.getString("contactMechTypeId").equals("POSTAL_ADDRESS")) {
                try {
                    postalAddress = contactMech.getRelatedOne("PostalAddress");
                } catch (GenericEntityException e) {
                    Debug.logError(e, "Trouble getting PostalAddress from ContactMech", module);
                }
            }
        }

        if (postalAddress != null)
            return postalAddress;
        return null;
    }

    /**
     * Method to return the total amount of an invoice which is not yet applied to a payment
     * @param invoice GenericValue object of the Invoice
     * @return the invoice total as BigDecimal
     */
    public static BigDecimal getInvoiceNotApplied(Delegator delegator, String invoiceId, Boolean actualCurrency) {
        return InvoiceWorker.getInvoiceTotal(delegator, invoiceId, actualCurrency).subtract(getInvoiceApplied(delegator, invoiceId,  UtilDateTime.nowTimestamp(), actualCurrency));
    }
    public static BigDecimal getInvoiceNotApplied(Delegator delegator, String invoiceId) {
        return InvoiceWorker.getInvoiceTotal(delegator, invoiceId).subtract(getInvoiceApplied(delegator, invoiceId));
    }
    public static BigDecimal getInvoiceNotApplied(GenericValue invoice) {
        return InvoiceWorker.getInvoiceTotal(invoice, Boolean.TRUE).subtract(getInvoiceApplied(invoice));
    }
    public static BigDecimal getInvoiceNotApplied(GenericValue invoice, Boolean actualCurrency) {
        return InvoiceWorker.getInvoiceTotal(invoice, actualCurrency).subtract(getInvoiceApplied(invoice, actualCurrency));
    }
    /**
     * Returns amount not applied (ie, still outstanding) of an invoice at an asOfDate, based on Payment.effectiveDate <= asOfDateTime
     *
     * @param invoice
     * @param asOfDateTime
     * @return
     */
    public static BigDecimal getInvoiceNotApplied(GenericValue invoice, Timestamp asOfDateTime) {
        return InvoiceWorker.getInvoiceTotal(invoice, Boolean.TRUE).subtract(getInvoiceApplied(invoice, asOfDateTime));
    }


    /**
     * Method to return the total amount of an invoice which is applied to a payment
     * @param invoice GenericValue object of the Invoice
     * @return the invoice total as BigDecimal
     */
    public static BigDecimal getInvoiceApplied(Delegator delegator, String invoiceId) {
        return getInvoiceApplied(delegator, invoiceId, UtilDateTime.nowTimestamp(), Boolean.TRUE);
    }

    /**
     * Returns amount applied to invoice before an asOfDateTime, based on Payment.effectiveDate <= asOfDateTime
     *
     * @param delegator
     * @param invoiceId
     * @param asOfDateTime - a Timestamp
     * @return
     */
    public static BigDecimal getInvoiceApplied(Delegator delegator, String invoiceId, Timestamp asOfDateTime, Boolean actualCurrency) {
        if (delegator == null) {
            throw new IllegalArgumentException("Null delegator is not allowed in this method");
        }

        BigDecimal invoiceApplied = ZERO;
        List paymentApplications = null;

        // lookup payment applications which took place before the asOfDateTime for this invoice
        EntityConditionList dateCondition = EntityCondition.makeCondition(UtilMisc.toList(
                EntityCondition.makeCondition("effectiveDate", EntityOperator.EQUALS, null),
                EntityCondition.makeCondition("effectiveDate", EntityOperator.LESS_THAN_EQUAL_TO, asOfDateTime)), EntityOperator.OR);
        EntityConditionList conditions = EntityCondition.makeCondition(UtilMisc.toList(
                dateCondition,
                EntityCondition.makeCondition("invoiceId", EntityOperator.EQUALS, invoiceId)),
                EntityOperator.AND);

        try {
            paymentApplications = delegator.findList("PaymentAndApplication", conditions, null, UtilMisc.toList("effectiveDate"), null, false);
        } catch (GenericEntityException e) {
            Debug.logError(e, "Trouble getting paymentApplicationlist", module);
        }
        if (UtilValidate.isNotEmpty(paymentApplications)) {
            Iterator p = paymentApplications.iterator();
            while (p.hasNext()) {
                GenericValue paymentApplication = (GenericValue) p.next();
                invoiceApplied = invoiceApplied.add(paymentApplication.getBigDecimal("amountApplied")).setScale(decimals,rounding);
            }
        }
        if (UtilValidate.isNotEmpty(invoiceApplied) && !actualCurrency) {
            invoiceApplied = invoiceApplied.multiply(getInvoiceCurrencyConversionRate(delegator, invoiceId)).setScale(decimals,rounding);
        }
        return invoiceApplied;
    }
    /**
     * Method to return the total amount of an invoice which is applied to a payment
     * @param invoice GenericValue object of the Invoice
     * @return the applied total as BigDecimal
     */
    public static BigDecimal getInvoiceApplied(GenericValue invoice) {
        return getInvoiceApplied(invoice, UtilDateTime.nowTimestamp());
    }

    /**
     * @param delegator
     * @param invoiceId
     * @param invoiceItemSeqId
     * @return
     */
    public static BigDecimal getInvoiceApplied(GenericValue invoice, Boolean actualCurrency) {
        return getInvoiceApplied(invoice.getDelegator(), invoice.getString("invoiceId"), UtilDateTime.nowTimestamp(), actualCurrency);
    }
    public static BigDecimal getInvoiceApplied(GenericValue invoice, Timestamp asOfDateTime) {
        return getInvoiceApplied(invoice.getDelegator(), invoice.getString("invoiceId"), asOfDateTime, Boolean.TRUE);
    }
    /**
     * Method to return the amount of an invoiceItem which is applied to a payment
     * @param invoice GenericValue object of the Invoice
     * @return the invoice total as BigDecimal
     */
    public static BigDecimal getInvoiceItemApplied(Delegator delegator, String invoiceId, String invoiceItemSeqId) {
        if (delegator == null) {
            throw new IllegalArgumentException("Null delegator is not allowed in this method");
        }

        GenericValue invoiceItem = null;
        try {
            invoiceItem = delegator.findByPrimaryKey("Invoice", UtilMisc.toMap("invoiceId", invoiceId,"invoiceItemSeqId", invoiceItemSeqId));
        } catch (GenericEntityException e) {
            Debug.logError(e, "Problem getting InvoiceItem", module);
        }

        if (invoiceItem == null) {
            throw new IllegalArgumentException("The invoiceId/itemSeqId passed does not match an existing invoiceItem");
        }

        return getInvoiceItemApplied(invoiceItem);
    }

    /**
     * Method to return the total amount of an invoiceItem which is applied to a payment
     * @param invoice GenericValue object of the Invoice
     * @return the applied total as BigDecimal
     */
    public static BigDecimal getInvoiceItemApplied(GenericValue invoiceItem) {
        BigDecimal invoiceItemApplied = ZERO;
        List paymentApplications = null;
        try {
            paymentApplications = invoiceItem.getRelated("PaymentApplication");
        } catch (GenericEntityException e) {
            Debug.logError(e, "Trouble getting paymentApplicationlist", module);
        }
        if (UtilValidate.isNotEmpty(paymentApplications)) {
            Iterator p = paymentApplications.iterator();
            while (p.hasNext()) {
                GenericValue paymentApplication = (GenericValue) p.next();
                invoiceItemApplied = invoiceItemApplied.add(paymentApplication.getBigDecimal("amountApplied")).setScale(decimals,rounding);
            }
        }
        return invoiceItemApplied;
    }
    public static BigDecimal getInvoiceCurrencyConversionRate(GenericValue invoice) {
        BigDecimal conversionRate = null;
        Delegator delegator = invoice.getDelegator();
        String otherCurrencyUomId = null;
        // find the organization party currencyUomId which different from the invoice currency
        try {
            GenericValue party  = delegator.findByPrimaryKey("PartyAcctgPreference", UtilMisc.toMap("partyId", invoice.getString("partyIdFrom")));
            if (UtilValidate.isEmpty(party) || party.getString("baseCurrencyUomId").equals(invoice.getString("currencyUomId"))) {
                party  = delegator.findByPrimaryKey("PartyAcctgPreference", UtilMisc.toMap("partyId", invoice.getString("partyId")));
            }
            if (UtilValidate.isNotEmpty(party) && party.getString("baseCurrencyUomId") != null) {
                otherCurrencyUomId = party.getString("baseCurrencyUomId");
            } else {
                otherCurrencyUomId = UtilProperties.getPropertyValue("general", "currency.uom.id.default");
            }
            if (otherCurrencyUomId == null) {
                otherCurrencyUomId = "USD"; // final default
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, "Trouble getting database records....", module);
        }
        if (invoice.getString("currencyUomId").equals(otherCurrencyUomId)) {
            return BigDecimal.ONE;  // organization party has the same currency so conversion not required.
        }

        try {
            // check if the invoice is posted and get the conversion from there
            List acctgTransEntries = invoice.getRelated("AcctgTrans");
            if (UtilValidate.isNotEmpty(acctgTransEntries)) {
                GenericValue acctgTransEntry = ((GenericValue) acctgTransEntries.get(0)).getRelated("AcctgTransEntry").get(0);
                conversionRate = acctgTransEntry.getBigDecimal("amount").divide(acctgTransEntry.getBigDecimal("origAmount"), new MathContext(100)).setScale(decimals,rounding);
            }
            // check if a payment is applied and use the currency conversion from there
            if (UtilValidate.isEmpty(conversionRate)) {
                List paymentAppls = invoice.getRelated("PaymentApplication");
                Iterator ii = paymentAppls.iterator();
                while (ii.hasNext()) {
                    GenericValue paymentAppl = (GenericValue) ii.next();
                    GenericValue payment = paymentAppl.getRelatedOne("Payment");
                    if (UtilValidate.isNotEmpty(payment.getBigDecimal("actualCurrencyAmount"))) {
                        if (UtilValidate.isEmpty(conversionRate)) {
                            conversionRate = payment.getBigDecimal("amount").divide(payment.getBigDecimal("actualCurrencyAmount"),new MathContext(100)).setScale(decimals,rounding);
                        } else {
                            conversionRate = conversionRate.add(payment.getBigDecimal("amount").divide(payment.getBigDecimal("actualCurrencyAmount"),new MathContext(100))).divide(new BigDecimal("2"),new MathContext(100)).setScale(decimals,rounding);
                        }
                    }
                }
            }
            // use the dated conversion entity
            if (UtilValidate.isEmpty(conversionRate)) {
                List rates = EntityUtil.filterByDate(delegator.findByAnd("UomConversionDated", UtilMisc.toMap("uomIdTo", invoice.getString("currencyUomId"), "uomId", otherCurrencyUomId)), invoice.getTimestamp("invoiceDate"));
                if (UtilValidate.isNotEmpty(rates)) {
                    conversionRate = (BigDecimal.ONE).divide(((GenericValue) rates.get(0)).getBigDecimal("conversionFactor"), new MathContext(100)).setScale(decimals,rounding);
                } else {
                    Debug.logError("Could not find conversionrate for invoice: " + invoice.getString("invoiceId"), module);
                    return new BigDecimal("1");
                }
            }

        } catch (GenericEntityException e) {
            Debug.logError(e, "Trouble getting database records....", module);
        }
        return(conversionRate);
    }

    public static BigDecimal getInvoiceCurrencyConversionRate(Delegator delegator, String invoiceId) {
        if (delegator == null) {
            throw new IllegalArgumentException("Null delegator is not allowed in this method");
        }

        GenericValue invoice = null;
        try {
            invoice = delegator.findByPrimaryKey("Invoice", UtilMisc.toMap("invoiceId", invoiceId));
        } catch (GenericEntityException e) {
            Debug.logError(e, "Problem getting Invoice", module);
        }

        if (invoice == null) {
            throw new IllegalArgumentException("The invoiceId passed does not match an existing invoice");
        }

        return getInvoiceCurrencyConversionRate(invoice);
    }

    /**
     * Return a list of taxes separated by Geo and party and return the tax grandtotal
     * @param invoice Generic Value
     * @return  Map: taxByTaxAuthGeoAndPartyList(List) and taxGrandTotal(BigDecimal)
     */
    public static Map<String, Object> getInvoiceTaxByTaxAuthGeoAndParty(GenericValue invoice) {
        BigDecimal taxGrandTotal = ZERO;
        List<Map<String, Object>> taxByTaxAuthGeoAndPartyList = FastList.newInstance();
        List<GenericValue> invoiceItems = null;
        if (UtilValidate.isNotEmpty(invoice)) {
            try {
                invoiceItems = invoice.getRelated("InvoiceItem");
            } catch (GenericEntityException e) {
                Debug.logError(e, "Trouble getting InvoiceItem list", module);
            }
            if ("SALES_INVOICE".equals(invoice.getString("invoiceTypeId"))) {
                invoiceItems = EntityUtil.filterByOr(
                        invoiceItems, UtilMisc.toList(
                                EntityCondition.makeCondition("invoiceItemTypeId", EntityOperator.EQUALS, "INV_SALES_TAX"), 
                                EntityCondition.makeCondition("invoiceItemTypeId", EntityOperator.EQUALS, "ITM_SALES_TAX")));
            } else if (("PURCHASE_INVOICE".equals(invoice.getString("invoiceTypeId")))) {
                invoiceItems = EntityUtil.filterByOr(
                        invoiceItems, UtilMisc.toList(
                                EntityCondition.makeCondition("invoiceItemTypeId", EntityOperator.EQUALS, "PINV_SALES_TAX"), 
                                EntityCondition.makeCondition("invoiceItemTypeId", EntityOperator.EQUALS, "PITM_SALES_TAX")));
            } else {
                invoiceItems = null;
            }
            if (UtilValidate.isNotEmpty(invoiceItems)) {
                invoiceItems = EntityUtil.orderBy(invoiceItems, UtilMisc.toList("taxAuthGeoId","taxAuthPartyId"));
                // get the list of all distinct taxAuthGeoId and taxAuthPartyId. It is for getting the number of taxAuthGeoId and taxAuthPartyId in invoiceItems.
                List<String> distinctTaxAuthGeoIdList = EntityUtil.getFieldListFromEntityList(invoiceItems, "taxAuthGeoId", true);
                List<String> distinctTaxAuthPartyIdList = EntityUtil.getFieldListFromEntityList(invoiceItems, "taxAuthPartyId", true);
                for (String taxAuthGeoId : distinctTaxAuthGeoIdList ) {
                    for (String taxAuthPartyId : distinctTaxAuthPartyIdList) {
                        //get all records for invoices filtered by taxAuthGeoId and taxAurhPartyId
                        List<GenericValue> invoiceItemsByTaxAuthGeoAndPartyIds = EntityUtil.filterByAnd(invoiceItems, UtilMisc.toMap("taxAuthGeoId", taxAuthGeoId, "taxAuthPartyId", taxAuthPartyId));
                        if (UtilValidate.isNotEmpty(invoiceItemsByTaxAuthGeoAndPartyIds)) {
                            BigDecimal totalAmount = ZERO;
                            //Now for each invoiceItem record get and add amount.
                            for (GenericValue invoiceItem : invoiceItemsByTaxAuthGeoAndPartyIds) {
                                BigDecimal amount = invoiceItem.getBigDecimal("amount");
                                if (amount == null) {
                                    amount = ZERO;
                                }
                                totalAmount = totalAmount.add(amount).setScale(taxDecimals, taxRounding);
                            }
                            totalAmount = totalAmount.setScale(UtilNumber.getBigDecimalScale("salestax.calc.decimals"), UtilNumber.getBigDecimalRoundingMode("salestax.rounding"));
                            taxByTaxAuthGeoAndPartyList.add(UtilMisc.<String, Object>toMap("taxAuthPartyId", taxAuthPartyId, "taxAuthGeoId", taxAuthGeoId, "totalAmount", totalAmount));
                            taxGrandTotal = taxGrandTotal.add(totalAmount);
                        }
                    }
                }
            }
        }
        Map<String, Object> result = FastMap.newInstance();
        result.put("taxByTaxAuthGeoAndPartyList", taxByTaxAuthGeoAndPartyList);
        result.put("taxGrandTotal", taxGrandTotal);
        return result;
    }
}
