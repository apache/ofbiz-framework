package com.simbaquartz.xparty.hierarchy.orderentity.role;

import com.simbaquartz.xparty.hierarchy.interfaces.EnumHierarchyRoles;

// Roles required to Create Order Entities
public enum OrderEntityCreatorRoles implements EnumHierarchyRoles
{
    SALES_REP;
    //REQ_TAKER // Because CustRequestParty, QuoteRole, OrderRole, InvoiceRole are related to PartyRole, all SALES_REPs also have to have REQ_TAKER role.  Stupid limitation of framework.

    // If we ever upgrade to Java 8 (https://issues.apache.org/jira/browse/OFBIZ-6458) this can be moved to the interface.
    public static String[] roleTypeIds() {
        return EnumHierarchyRoles.roleTypeIds(OrderEntityCreatorRoles.values());
    }
}
