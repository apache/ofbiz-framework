
package com.simbaquartz.xparty.hierarchy.role;

import com.simbaquartz.xparty.hierarchy.interfaces.HierarchyRolesEnum;

// Roles required to View Order Entities
public enum CustomerRoles implements HierarchyRolesEnum {
    //CONST("Description", "partyRelationshipTypeId", Boolean employmentRole)
    GOVERNMENT_AGENCY ("Government Agency", "ORG_ROLLUP", false),
    GOVERNMENT_ORG ("Government Organization", "ORG_ROLLUP", false),
    GOVERNMENT_LOC ("Government Location", "ORG_ROLLUP", false);

    private final String description;
    private final String partyRelationshipTypeId;
    public final boolean employmentRole;

    CustomerRoles(String value, String partyRelationshipTypeId, boolean employmentRole)
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