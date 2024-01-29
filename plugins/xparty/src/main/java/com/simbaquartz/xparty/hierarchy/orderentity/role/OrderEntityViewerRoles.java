package com.simbaquartz.xparty.hierarchy.orderentity.role;

import com.simbaquartz.xparty.hierarchy.interfaces.EnumHierarchyRoles;

// Roles required to View Order Entities
public enum OrderEntityViewerRoles implements EnumHierarchyRoles
{
    CONTRACTING_OFFICER,
    SALES_REP,
    REQ_TAKER; //Request Taker can create an OrderEntity for someone else, they still need to be able to view/update the OrderEntity.

    // If we ever upgrade to Java 8 (https://issues.apache.org/jira/browse/OFBIZ-6458) this can be moved to the interface.
    public static String[] roleTypeIds() {
        return EnumHierarchyRoles.roleTypeIds(OrderEntityViewerRoles.values());
    }
}
