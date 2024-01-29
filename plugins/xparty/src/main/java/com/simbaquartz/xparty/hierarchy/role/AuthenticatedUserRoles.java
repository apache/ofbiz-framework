
package com.simbaquartz.xparty.hierarchy.role;

import com.simbaquartz.xparty.hierarchy.interfaces.EnumHierarchyRoles;
import com.simbaquartz.xparty.hierarchy.interfaces.HierarchyRolesEnum;

// Roles required to View Order Entities
public enum AuthenticatedUserRoles implements HierarchyRolesEnum {
    //CONST("Description", "partyRelationshipTypeId", Boolean employmentRole)
    GUEST ("AuthenticatedUser","AUTHENTICATED_USER", true),
    EMPLOYEE ("AuthenticatedUser","AUTHENTICATED_USER", true);

    private final String description;
    private final String partyRelationshipTypeId;
    public final boolean employmentRole;

    AuthenticatedUserRoles(String value, String partyRelationshipTypeId, boolean employmentRole)
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
