
package com.simbaquartz.xparty.hierarchy.role;

import com.simbaquartz.xparty.hierarchy.interfaces.HierarchyRolesEnum;

// Roles required to View Order Entities
public enum PartyGroupTypes implements HierarchyRolesEnum {
    //CONST("Description", "partyRelationshipTypeId", Boolean employmentRole)
    INTERNAL_ORGANIZATIO ("Internal Organization", "INTERNAL_ORGANIZATIO", false),
    ORGANIZATION_ROLE ("Organization", "ORGANIZATION_ROLE", false),
    SUPPLIER ("Supplier", "SUPPLIER_REL", false),
    PARTNER ("Partner", "PARTNERSHIP", false),
    CUSTOMER("Customer","CUSTOMER", false);

    private final String description;
    private final String partyRelationshipTypeId;
    public final boolean employmentRole;

    PartyGroupTypes(String value, String partyRelationshipTypeId, boolean employmentRole)
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