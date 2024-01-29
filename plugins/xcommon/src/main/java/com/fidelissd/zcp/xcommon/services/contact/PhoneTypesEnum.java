package com.fidelissd.zcp.xcommon.services.contact;

/**
 * The type of the phone number. The type can be custom or one of these predefined values: home work
 * mobile homeFax workFax otherFax pager workMobile workPager main other
 */
public enum PhoneTypesEnum {
  HOME("home", "PHONE_HOME", "Home phone"),
  WORK("work", "PHONE_WORK", "Work phone"),
  PRIMARY("primary", "PRIMARY_PHONE", "Primary phone");

  private String label;
  private String typeId;
  private String description;

  PhoneTypesEnum(String label, String typeId, String description) {
    this.label = label;
    this.typeId = typeId;
    this.description = description;
  }

  public String getLabel() {
    return label;
  }

  public String getTypeId() {
    return typeId;
  }

  public String getDescription() {
    return description;
  }
}
