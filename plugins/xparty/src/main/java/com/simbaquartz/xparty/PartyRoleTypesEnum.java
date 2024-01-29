package com.simbaquartz.xparty;

/** Roles a party can have. */
public enum PartyRoleTypesEnum {
  CUSTOMER("CUSTOMER", "Customer"),
  LEAD("LEAD", "Lead"),
  _NA_("_NA_", "Not Applicable");

  private final String typeId;
  private final String description;

  PartyRoleTypesEnum(String typeId, String description) {
    this.typeId = typeId;
    this.description = description;
  }

  public String getDescription() {
    return description;
  }

  public String getTypeId() {
    return typeId;
  }
}
