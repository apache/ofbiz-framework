package com.simbaquartz.xparty.hierarchy.orderentity.role;

import com.simbaquartz.xparty.hierarchy.interfaces.EnumHierarchyRoles;

// Roles required to View Order Entities
public enum OrderEntityUpdaterRoles implements EnumHierarchyRoles
{
    SALES_REP,
    REVIEWER,
    REQ_TAKER,
    ORDER_CLERK;

    // If we ever upgrade to Java 8 (https://issues.apache.org/jira/browse/OFBIZ-6458) this can be moved to the interface.
    public static String[] roleTypeIds() {
        return EnumHierarchyRoles.roleTypeIds(OrderEntityUpdaterRoles.values());
    }
}
