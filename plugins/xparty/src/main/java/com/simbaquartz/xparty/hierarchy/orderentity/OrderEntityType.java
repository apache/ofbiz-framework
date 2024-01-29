

package com.simbaquartz.xparty.hierarchy.orderentity;

import com.google.common.base.CaseFormat;

// Enum of all the Order Entities
public enum OrderEntityType
{
    // Entity Name, Role Entity, Primary Key, Customer PartyId Column, contentEntityName, itemEntityName, itemEntitySequenceIdName
    CUST_REQUEST("CustRequest", "CustRequestParty", "custRequestId", "fromPartyId", "CustRequestContent", "CustRequestItem", "sequenceNum"),
    QUOTE("Quote", "QuoteRole", "quoteId", "partyId", "FsdQuoteContent", "QuoteItem", "quoteItemSeqId"),
    ORDER("OrderHeader", "OrderRole", "orderId", "", "OrderContent", "OrderItem", "orderItemSeqId"), // TODO Figure out how to handle Customer Party Id
    INVOICE("Invoice", "InvoiceRole", "invoiceId", "partyId", "InvoiceContent", "InvoiceItem", "invoiceItemSeqId"),
    SHIPMENT("Shipment", "", "shipmentId", "", null, "OrderItem", "orderItemSeqId"); // TODO Figure out how to handle Customer Party Id

    public final String EntityName;
    public final String fieldPrefix;
    public final String EntityRoleName;
    public final String primaryKeyName;
    public final String customerPartyIdName;
    public final String contentEntityName;
    public final String itemEntityName;
    public final String itemEntitySequenceIdName;

    OrderEntityType(String EntityName, String EntityRoleName, String primaryKeyName, String customerPartyIdName, String contentEntityName, String itemEntityName, String itemEntitySequenceIdName)
    {
        this.EntityName = EntityName;
        this.EntityRoleName = EntityRoleName;
        this.primaryKeyName = primaryKeyName;
        this.customerPartyIdName = customerPartyIdName;
        this.contentEntityName = contentEntityName;
        this.itemEntityName = itemEntityName;
        this.itemEntitySequenceIdName = itemEntitySequenceIdName;

        fieldPrefix = CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, name());
    }

    public String getEntityName()
    {
        return EntityName;
    }

    public String getEntityRoleName()
    {
        return EntityRoleName;
    }

    public String getPrimaryKeyName()
    {
        return primaryKeyName;
    }

    public String getCustomerPartyIdName()
    {
        return customerPartyIdName;
    }

    public String getContentEntityName()
    {
        return contentEntityName;
    }

    public String getItemEntityName()
    {
        return itemEntityName;
    }

    public String getUnitPriceFieldName()
    {
        switch (this)
        {
            case CUST_REQUEST:
            case QUOTE:
                return fieldPrefix + "UnitPrice";
            case ORDER:
                return "unitPrice";
            case INVOICE:
                return "amount";
            case SHIPMENT:
            default:
                return null;
        }
    }

    public String getUnitListPriceFieldName()
    {
        switch (this)
        {
            case CUST_REQUEST:
            case QUOTE:
                return fieldPrefix + "UnitListPrice";
            case ORDER:
                return "unitListPrice";
            case INVOICE:
            case SHIPMENT:
            default:
                return null;
        }
    }

    public String getUnitCostFieldName()
    {
        return fieldPrefix + "UnitCost";
    }

    public String getUnitAverageCostFieldName()
    {
        switch (this)
        {
            case QUOTE:
                return fieldPrefix + "UnitAverageCost";
            case ORDER:
                return "unitAverageCost";
            case CUST_REQUEST:
            case INVOICE:
            case SHIPMENT:
            default:
                return null;
        }
    }

    public String getUnitShippingCostFieldName()
    {
        return fieldPrefix + "UnitShippingCost";
    }

    public String getUnitShippingPriceFieldName()
    {
        return fieldPrefix + "UnitShippingPrice";
    }

    public String getItemEntitySequenceIdName()
    {
        return fieldPrefix + "ItemSeqId";
    }

    public String getAdjustmentEntityName()
    {
        switch (this)
        {
            case QUOTE:
            case ORDER:
                return CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, name()) + "Adjustment";
            case CUST_REQUEST:
            case INVOICE:
            case SHIPMENT:
            default:
                return null;
        }
    }

    public String getAdjustmentTypeIdName()
    {
        switch (this)
        {
            case QUOTE:
            case ORDER:
                return fieldPrefix + "AdjustmentTypeId";
            case CUST_REQUEST:
            case INVOICE:
            case SHIPMENT:
            default:
                return null;
        }
    }

    public String getEntityUserDefinedNameFieldName()
    {
        switch (this)
        {
            case CUST_REQUEST:
            case QUOTE:
            case ORDER:
                return CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, name()) + "Name";
            case INVOICE:
            case SHIPMENT:
            default:
                return null;
        }
    }

    public String getItemShipGroupAssocEntityName()
    {
        switch (this)
        {
            case CUST_REQUEST:
            case QUOTE:
                return "QuoteItemShipGroupAssoc";
            case ORDER:
                return "OrderItemShipGroupAssoc";
            case INVOICE:
            case SHIPMENT:
            default:
                return null;
        }
    }
}
