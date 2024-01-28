package com.simbaquartz.xcommon.enums;

/**
 * Types of locations (addresses)
 */
public enum PostalAddressTypesEnum {
  HOME("home","HOME_ADDRESS","Home address"),
  WORK("work","WORK_ADDRESS","Work address"),
  SHIP_FROM("shipFrom","SHIP_ORIG_LOCATION","Ship from address"),
  SHIP_TO("shipTo","SHIPPING_LOCATION","Ship to address"),
  RETURN_TO("returnTo","PUR_RET_LOCATION","Address to be used for returning purchased merchandise"),
  BILLING("billing","BILLING_LOCATION","Bill to address, usually used for mailing invoices to the address."),
  PAYMENT("payment","PAYMENT_LOCATION","Pay to address for receiving payments, usually bank address."),
  PREVIOUS("previous","PREVIOUS_LOCATION","To track old addresses."),
  GENERAL("general","GENERAL_LOCATION","General location."),
  PRIMARY("primary","PRIMARY_LOCATION","Primary address");

  private String label;
  private String typeId;
  private String description;

  PostalAddressTypesEnum(String label, String typeId, String description) {
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
