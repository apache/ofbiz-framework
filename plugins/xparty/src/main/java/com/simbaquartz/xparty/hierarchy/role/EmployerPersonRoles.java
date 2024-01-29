
package com.simbaquartz.xparty.hierarchy.role;

import com.simbaquartz.xparty.hierarchy.interfaces.HierarchyRolesEnum;

// Roles required to View Order Entities
public enum EmployerPersonRoles implements HierarchyRolesEnum {
    //CONST("Description", "partyRelationshipTypeId", Boolean employmentRole, helpText)
    OWNER ("OWNER","OWNER", true, "Owner of the account."),
    SALES_REP ("Sales Representative","EMPLOYMENT", true, "A Sales Representative is a salesperson who is employed by the Supplier/Partner."),
    MANAGER ("Manager","EMPLOYMENT", true, "A Sales Manager, Director, or Executive is a person who is employed by the Supplier/Partner and manages the Supplier/Partner's staff."),
    AGENT("Sales Agent", "AGENT", true, "A Sales Agent is a self employed salesperson. The Sales Agent obtains orders for the Supplier/Partner and is paid commission on those orders."),
    ACCOUNTANT("Accountant", "EMPLOYMENT", true, "An Account Receivable/Payable person who is employed by the Supplier/Partner."),
    ORDER_CLERK("Order Clerk", "EMPLOYMENT", true, "An Order Clerk is a person who is responsible for receiving and processing order information."),
    SHIPMENT_CLERK("Shipment Clerk", "EMPLOYMENT", true, "A Shipment Clerk is a person who is responsible for shipping and providing shipment information about an order."),
    CONTACT("Contact", "CONTACT_REL", false, "A Contact is a person who doesn't fall into any of the other roles."),
    CUSTOMER("Customer", "CONTACT_OWNER", true, "Applicant / Customer of the account."),
    EMPLOYEE("Employee (Other)", "EMPLOYMENT", true, "Any other person who is an employee, that doesn't fit into the other roles."),
    SUPPLIER_POC("Supplier Point of Contact", "EMPLOYMENT", true, "A supplier point of contact is a person who is employed by a Supplier and liaisons with a Customer on behalf of FSD."),
    _NA_("Not Applicable", "CONTACT_REL", false, "Use NA for a person to which non of the other categories apply.");




    private final String description;
    private final String partyRelationshipTypeId;
    private final boolean employmentRole;
    private final String helpText;

    EmployerPersonRoles(String value, String partyRelationshipTypeId, boolean employmentRole, String helpText)
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