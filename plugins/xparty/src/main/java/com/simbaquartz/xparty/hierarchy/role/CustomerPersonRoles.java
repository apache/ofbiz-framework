
package com.simbaquartz.xparty.hierarchy.role;

import com.simbaquartz.xparty.hierarchy.interfaces.HierarchyRolesEnum;

// Roles required to View Order Entities
public enum CustomerPersonRoles implements HierarchyRolesEnum {
    //CONST("Description", "partyRelationshipTypeId", Boolean employmentRole)
    CONTRACTING_OFFICER ("Contracting Officer", "CONTRACTING_OFFICER", true),
    MANAGER ("Manager", "EMPLOYMENT", true),
    SHIPMENT_CLERK ("Shipping Contact", "EMPLOYMENT", true),
    END_USER_CUSTOMER ("End User Contact", "EMPLOYMENT", true),
    CONTACT ("Contact", "CONTACT_REL", false);

    private final String description;
    private final String partyRelationshipTypeId;
    public final boolean employmentRole;

    CustomerPersonRoles(String value, String partyRelationshipTypeId, boolean employmentRole)
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
