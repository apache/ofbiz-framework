
package com.simbaquartz.xparty.hierarchy.role;

import com.simbaquartz.xparty.hierarchy.interfaces.HierarchyRolesEnum;

// Roles required to View Order Entities
public enum AccountRoles implements HierarchyRolesEnum {
    //CONST("Description", "partyRelationshipTypeId", Boolean employmentRole, helpText)
    OWNER ("Owner","OWNER", true, "Owner of the account."),
    MEMBER("Account Member", "MEMBER", true, "A member of the account, can represent employees and any other point of contacts."),
    REPORTS_TO("Reports To", "REPORTS_TO", true, "Reporting manager for the org member."),
    EMPLOYMENT("Employment (Other)", "EMPLOYMENT", true, "A person in employement role."),
    EMPLOYEE("Employee (Other)", "EMPLOYEE", true, "Any other person who is an employee, that doesn't fit into the other roles."),
    INTERNAL_ORG("Internal organization", "INTERNAL_ORGANIZATIO", true, "Represents the internal organization represented by the account."),
    CONTACT("Contact", "CONTACT_REL", false, "A Contact is a person who doesn't fall into any of the other roles."),
    CONTACT_OWNER("Contact owner", "CONTACT_OWNER", false, "Contact owner account, owns contact"),
    _NA_("Not Applicable", "CONTACT_REL", false, "Use NA for a person to which non of the other categories apply."),
    DEPARTMENT ("DEPARTMENT","DEPARTMENT", true, "Department of the organization."),
    SUB_DEPARTMENT ("SUB_DEPARTMENT","SUB_DEPARTMENT", true, "Sub Department of the Department."),
    HOD ("HOD","HOD", true, "Head of the Department."),
    DEPARTMENT_MEMBER ("DEPT_MEMBER","DEPT_MEMBER", true, "Member of the Department."),
    ORGANIZATION_ROLE ("ORGANIZATION_ROLE","ORGANIZATION_ROLE", true, "Organization Role."),
    APP_CLIENT ("Application Client (Organization)","SUBSCRIBER", true, "Organization is the client for the app.");

    private final String description;
    private final String partyRelationshipTypeId;
    private final boolean employmentRole;
    private final String helpText;

    AccountRoles(String value, String partyRelationshipTypeId, boolean employmentRole, String helpText)
    {
        this.description = value;
        this.partyRelationshipTypeId = partyRelationshipTypeId;
        this.employmentRole = employmentRole;
        this.helpText = helpText;
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

    public String getHelpText()
    {
        return helpText;
    }
}