
package com.simbaquartz.xparty.hierarchy.role;

import com.simbaquartz.xparty.hierarchy.interfaces.HierarchyRolesEnum;

// Roles required to View Order Entities
public enum InternalPersonRoles implements HierarchyRolesEnum {
    //CONST("Description", "partyRelationshipTypeId", Boolean employmentRole)
    SALES_REP ("Sales Representative","EMPLOYMENT", true),
    MANAGER ("Manager","EMPLOYMENT", true),
    ACCOUNT_LEAD ("Account Lead","ACCOUNT", false),
    AGENT("Agent", "AGENT", true),
    OWNER("Owner","OWNER", false),
    ORDER_CLERK("Order Clerk", "ORDER_CLERK", true);

    private final String description;
    private final String partyRelationshipTypeId;
    public final boolean employmentRole;

    InternalPersonRoles(String value, String partyRelationshipTypeId, boolean employmentRole)
    {
        this.description = value;
        this.partyRelationshipTypeId = partyRelationshipTypeId;
        this.employmentRole = employmentRole;
    }

    public String getDescription() {
        return description;
    }

    public String getPartyRelationshipTypeId()
    {
        return partyRelationshipTypeId;
    }

    public boolean isEmploymentRole() {
        return employmentRole;
    }
}
