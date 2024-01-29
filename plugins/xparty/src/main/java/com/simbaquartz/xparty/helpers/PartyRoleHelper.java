package com.simbaquartz.xparty.helpers;

import com.simbaquartz.xparty.PartyRoleTypesEnum;
import org.apache.ofbiz.entity.GenericValue;

import java.util.List;

/** Roles helper utility for common roles. */
public class PartyRoleHelper {
  /**
   * Returns true if the passed party has EMPLOYEE roles.
   *
   * @param partyRole
   * @return
   */
  public static Boolean isEmployee(GenericValue partyRole) {
    String roleTypeId = partyRole.getString("roleTypeId");
    if (roleTypeId.equalsIgnoreCase("PARTNER")) return true;
    else return false;
  }

  public static Boolean isPartner(List<GenericValue> partyRoles) {
    for (GenericValue partyRole : partyRoles) {
      if (isPartner(partyRole)) return true;
    }
    return false;
  }

  public static Boolean isPartner(GenericValue partyRole) {
    String roleTypeId = partyRole.getString("roleTypeId");
    if (roleTypeId.equalsIgnoreCase("PARTNER")) return true;
    else return false;
  }

  public static Boolean isSupplierOrVendor(List<GenericValue> partyRoles) {
    for (GenericValue partyRole : partyRoles) {
      if (isSupplierOrVendor(partyRole)) return true;
    }
    return false;
  }

  public static Boolean isSupplierOrVendor(GenericValue partyRole) {
    String roleTypeId = partyRole.getString("roleTypeId");
    if (roleTypeId.equalsIgnoreCase("SUPPLIER")) return true;
    else return false;
  }

  public static Boolean isCustomer(List<GenericValue> partyRoles) {
    for (GenericValue partyRole : partyRoles) {
      if (isCustomer(partyRole)) return true;
    }
    return false;
  }

  public static Boolean isCustomer(GenericValue partyRole) {
    String roleTypeId = partyRole.getString("roleTypeId");
    if (roleTypeId.equalsIgnoreCase("CUSTOMER")) return true;
    else return false;
  }

  public static Boolean isGovernmentLocation(List<GenericValue> partyRoles) {
    for (GenericValue partyRole : partyRoles) {
      if (isGovernmentLocation(partyRole)) return true;
    }
    return false;
  }

  public static Boolean isGovernmentLocation(GenericValue partyRole) {
    String roleTypeId = partyRole.getString("roleTypeId");
    if (roleTypeId.equalsIgnoreCase("GOVERNMENT_LOC")) return true;
    else return false;
  }

  public static Boolean isGovernmentOrganization(List<GenericValue> partyRoles) {
    for (GenericValue partyRole : partyRoles) {
      if (isGovernmentOrganization(partyRole)) return true;
    }
    return false;
  }

  public static Boolean isGovernmentOrganization(GenericValue partyRole) {
    String roleTypeId = partyRole.getString("roleTypeId");
    if (roleTypeId.equalsIgnoreCase("GOVERNMENT_ORG")) return true;
    else return false;
  }

  public static Boolean isLead(GenericValue partyRole) {
    String roleTypeId = partyRole.getString("roleTypeId");
    return isLead(roleTypeId);
  }

  public static Boolean isLead(String roleTypeId) {
    return roleTypeId.equalsIgnoreCase(PartyRoleTypesEnum.LEAD.getTypeId());
  }
}
