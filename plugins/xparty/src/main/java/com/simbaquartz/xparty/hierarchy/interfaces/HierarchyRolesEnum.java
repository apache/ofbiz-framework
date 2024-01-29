

package com.simbaquartz.xparty.hierarchy.interfaces;

public interface HierarchyRolesEnum
{
    String description = null;
    String partyRelationshipTypeId = null;
    boolean employmentRole = false;

    String getDescription();
    String getPartyRelationshipTypeId();
    boolean isEmploymentRole();
}