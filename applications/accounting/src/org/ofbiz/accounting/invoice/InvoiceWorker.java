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
import java.util.Iterator;
import java.util.List;
import javolution.util.FastList;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilNumber;
import org.ofbiz.entity.condition.EntityConditionList;
import org.ofbiz.entity.condition.EntityExpr;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.util.EntityUtil;

/**
 * InvoiceWorker - Worker methods of invoices
 */
public class InvoiceWorker {
    
    public static String module = InvoiceWorker.class.getName();
    private static BigDecimal ZERO = new BigDecimal("0");
    private static int decimals = UtilNumber.getBigDecimalScale("invoice.decimals");
    private static int rounding = UtilNumber.getBigDecimalRoundingMode("invoice.rounding");

    /**
     * Method to return the total amount of an invoice
     * @param invoice GenericValue object of the Invoice
     * @return the invoice total as double
     */
    public static double getInvoiceTotal(GenericDelegator delegator, String invoiceId) {
        return getInvoiceTotalBd(delegator, invoiceId).doubleValue();
    }

    public static BigDecimal getInvoiceTotalBd(GenericDelegator delegator, String invoiceId) {
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
        
        return getInvoiceTotalBd(invoice);
    }

    /** Method to get the taxable invoice item types as a List of invoiceItemTypeIds.  These are identified in Enumeration with enumTypeId TAXABLE_INV_ITM_TY. */
    public static List getTaxableInvoiceItemTypeIds(GenericDelegator delegator) throws GenericEntityException {
        List typeIds = FastList.newInstance();
        List invoiceItemTaxTypes = delegator.findByAndCache("Enumeration", UtilMisc.toMap("enumTypeId", "TAXABLE_INV_ITM_TY"));
        for (Iterator iter = invoiceItemTaxTypes.iterator(); iter.hasNext(); ) {
            GenericValue invoiceItemTaxType = (GenericValue) iter.next();
            typeIds.add(invoiceItemTaxType.get("enumId"));
        }
        return typeIds;
    }

    public static double getInvoiceTaxTotal(GenericValue invoice) {
        BigDecimal invoiceTaxTotal = ZERO;
        BigDecimal ONE = new BigDecimal("1");

        if (invoice == null)
           throw new IllegalArgumentException("The invoiceId passed does not match an existing invoice"); 
        List invoiceTaxItems = null;
        try {
            GenericDelegator delegator = invoice.getDelegator();
            EntityConditionList condition = new EntityConditionList( UtilMisc.toList(
                    new EntityExpr("invoiceId", EntityOperator.EQUALS, invoice.get("invoiceId")),
                    new EntityExpr("invoiceItemTypeId", EntityOperator.IN, getTaxableInvoiceItemTypeIds(delegator))
                    ), EntityOperator.AND);
            invoiceTaxItems = delegator.findByCondition("InvoiceItem", condition, null, null);
        } catch (GenericEntityException e) {
            Debug.logError(e, "Trouble getting InvoiceItem list", module);            
        }
        if (invoiceTaxItems != null && invoiceTaxItems.size() > 0) {
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
        return invoiceTaxTotal.setScale(decimals, rounding).doubleValue();

    }
    
    public static double getInvoiceNoTaxTotal(GenericValue invoice) {
        return getInvoiceTotalBd(invoice).doubleValue() - getInvoiceTaxTotal(invoice);
    }
    
    /**
     * Method to return the total amount of an invoice
     * @param invoice GenericValue object of the Invoice
     * @return the invoice total as double
     */
        public static double getInvoiceTotal(GenericValue invoice) {
            return getInvoiceTotalBd(invoice).doubleValue();
        }
        
        public static BigDecimal getInvoiceTotalBd(GenericValue invoice) {
        BigDecimal invoiceTotal = ZERO;
        List invoiceItems = null;
        try {
            invoiceItems = invoice.getRelated("InvoiceItem");
        } catch (GenericEntityException e) {
            Debug.logError(e, "Trouble getting InvoiceItem list", module);            
        }
        if (invoiceItems != null && invoiceItems.size() > 0) {
            Iterator invoiceItemsIter = invoiceItems.iterator();
            while (invoiceItemsIter.hasNext()) {
                GenericValue invoiceItem = (GenericValue) invoiceItemsIter.next();
                BigDecimal amount = invoiceItem.getBigDecimal("amount");
                BigDecimal quantity = invoiceItem.getBigDecimal("quantity");
                if (amount == null)
                    amount = ZERO;
                if (quantity == null)
                    quantity = new BigDecimal("1");
                invoiceTotal = invoiceTotal.add( amount.multiply(quantity)).setScale(decimals,rounding);
            }
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
        GenericDelegator delegator = invoice.getDelegator();
        List locations = null;
        // first try InvoiceContactMech to see if we can find the address needed
        try {
            locations = invoice.getRelated("InvoiceContactMech", UtilMisc.toMap("contactMechPurposeTypeId", contactMechPurposeTypeId), null);
        } catch (GenericEntityException e) {
            Debug.logError("Touble getting InvoiceContactMech entity list", module);           
        }

        if (locations == null || locations.size() == 0)    {
            // if no locations found get it from the PartyAndContactMech using the from and to party on the invoice
            String destinationPartyId = null;
            if (invoice.getString("invoiceTypeId").equals("SALES_INVOICE"))
                destinationPartyId = invoice.getString("partyId");
            if (invoice.getString("invoiceTypeId").equals("PURCHASE_INVOICE"))
                destinationPartyId = "partyFrom";
            try {
                locations = EntityUtil.filterByDate(delegator.findByAnd("PartyContactMechPurpose", 
                        UtilMisc.toMap("partyId", destinationPartyId, "contactMechPurposeTypeId", contactMechPurposeTypeId)));
            } catch (GenericEntityException e) {
                Debug.logError("Trouble getting contact party purpose list", module);           
            }
            //if still not found get it from the general location
            if (locations == null || locations.size() == 0)    {
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
        if (locations != null && locations.size() > 0) {
            try {
                contactMech = ((GenericValue) locations.get(0)).getRelatedOne("ContactMech");
            } catch (GenericEntityException e) {
                Debug.logError(e, "Trouble getting Contact for contactMechId: " + contactMech.getString("contactMechId"), module);
            }
            
            if (contactMech.getString("contactMechTypeId").equals("POSTAL_ADDRESS"))    {
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
     * @return the invoice total as double
     */
    public static BigDecimal getInvoiceNotApplied(GenericDelegator delegator, String invoiceId) {
        return InvoiceWorker.getInvoiceTotalBd(delegator, invoiceId).subtract(getInvoiceAppliedBd(delegator, invoiceId));
    }
    public static BigDecimal getInvoiceNotApplied(GenericValue invoice) {
        return InvoiceWorker.getInvoiceTotalBd(invoice).subtract(getInvoiceAppliedBd(invoice));
    }
    /**
     * Returns amount not applied (ie, still outstanding) of an invoice at an asOfDate, based on Payment.effectiveDate <= asOfDateTime
     * 
     * @param invoice
     * @param asOfDateTime
     * @return
     */
    public static BigDecimal getInvoiceNotApplied(GenericValue invoice, Timestamp asOfDateTime) {
        return InvoiceWorker.getInvoiceTotalBd(invoice).subtract(getInvoiceAppliedBd(invoice, asOfDateTime));
    }

    
    /**
     * Method to return the total amount of an invoice which is applied to a payment
     * @param invoice GenericValue object of the Invoice
     * @return the invoice total as double
     */
    public static double getInvoiceApplied(GenericDelegator delegator, String invoiceId) {
        return getInvoiceAppliedBd(delegator, invoiceId).doubleValue();
    }

    public static BigDecimal getInvoiceAppliedBd(GenericDelegator delegator, String invoiceId) {
        return getInvoiceAppliedBd(delegator, invoiceId, UtilDateTime.nowTimestamp());
    }
    
    /**
     * Returns amount applied to invoice before an asOfDateTime, based on Payment.effectiveDate <= asOfDateTime
     * 
     * @param delegator
     * @param invoiceId
     * @param asOfDateTime - a Timestamp
     * @return
     */
    public static BigDecimal getInvoiceAppliedBd(GenericDelegator delegator, String invoiceId, Timestamp asOfDateTime) {
        if (delegator == null) {
            throw new IllegalArgumentException("Null delegator is not allowed in this method");
        }
        
        BigDecimal invoiceApplied = ZERO;
        List paymentApplications = null;
        
        // lookup payment applications which took place before the asOfDateTime for this invoice
        EntityConditionList dateCondition = new EntityConditionList(UtilMisc.toList(
                new EntityExpr("effectiveDate", EntityOperator.EQUALS, null),
                new EntityExpr("effectiveDate", EntityOperator.LESS_THAN_EQUAL_TO, asOfDateTime)), EntityOperator.OR);
        EntityConditionList conditions = new EntityConditionList(UtilMisc.toList(
                dateCondition,
                new EntityExpr("invoiceId", EntityOperator.EQUALS, invoiceId)),
                EntityOperator.AND); 

        try {
            paymentApplications = delegator.findByCondition("PaymentAndApplication", conditions, null, UtilMisc.toList("effectiveDate"));
        } catch (GenericEntityException e) {
            Debug.logError(e, "Trouble getting paymentApplicationlist", module);            
        }
        if (paymentApplications != null && paymentApplications.size() > 0) {
            Iterator p = paymentApplications.iterator();
            while (p.hasNext()) {
                GenericValue paymentApplication = (GenericValue) p.next();
                invoiceApplied = invoiceApplied.add(paymentApplication.getBigDecimal("amountApplied")).setScale(decimals,rounding);
            }
        }
        return invoiceApplied;
    }
    /**
     * Method to return the total amount of an invoice which is applied to a payment
     * @param invoice GenericValue object of the Invoice
     * @return the applied total as double
     */
    public static double getInvoiceApplied(GenericValue invoice) {
        return getInvoiceAppliedBd(invoice).doubleValue();
    }
    /**
     * Big decimal version of getInvoiceApplied
     * 
     * @param delegator
     * @param invoiceId
     * @param invoiceItemSeqId
     * @return
     */
    public static BigDecimal getInvoiceAppliedBd(GenericValue invoice, Timestamp asOfDateTime) {
        return getInvoiceAppliedBd(invoice.getDelegator(), invoice.getString("invoiceId"), asOfDateTime);
    }
    public static BigDecimal getInvoiceAppliedBd(GenericValue invoice) {
        return getInvoiceAppliedBd(invoice, UtilDateTime.nowTimestamp());
    }
    
    /**
     * Method to return the amount of an invoiceItem which is applied to a payment
     * @param invoice GenericValue object of the Invoice
     * @return the invoice total as double
     */
    public static double getInvoiceItemApplied(GenericDelegator delegator, String invoiceId, String invoiceItemSeqId) {
        return getInvoiceItemAppliedBd(delegator, invoiceId, invoiceItemSeqId).doubleValue();
    }
    
    /**
     * Big decimal version of getInvoiceApplied
     * 
     * @param delegator
     * @param invoiceId
     * @param invoiceItemSeqId
     * @return
     */
    public static BigDecimal getInvoiceItemAppliedBd(GenericDelegator delegator, String invoiceId, String invoiceItemSeqId) {
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
        
        return getInvoiceItemAppliedBd(invoiceItem);
    }
    
    /**
     * Method to return the total amount of an invoiceItem which is applied to a payment
     * @param invoice GenericValue object of the Invoice
     * @return the applied total as double
     */
    public static double getInvoiceItemApplied(GenericValue invoiceItem) {
        return getInvoiceItemAppliedBd(invoiceItem).doubleValue();
    }
    public static BigDecimal getInvoiceItemAppliedBd(GenericValue invoiceItem) {
        BigDecimal invoiceItemApplied = ZERO;
        List paymentApplications = null;
        try {
            paymentApplications = invoiceItem.getRelated("PaymentApplication");
        } catch (GenericEntityException e) {
            Debug.logError(e, "Trouble getting paymentApplicationlist", module);            
        }
        if (paymentApplications != null && paymentApplications.size() > 0) {
            Iterator p = paymentApplications.iterator();
            while (p.hasNext()) {
                GenericValue paymentApplication = (GenericValue) p.next();
                invoiceItemApplied = invoiceItemApplied.add(paymentApplication.getBigDecimal("amountApplied")).setScale(decimals,rounding);
            }
        }
        return invoiceItemApplied;        
    }
    
   
}
