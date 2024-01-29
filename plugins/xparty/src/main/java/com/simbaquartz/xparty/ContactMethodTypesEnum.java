package com.simbaquartz.xparty;

/** Types of method to contact someone (person/company). */
public enum ContactMethodTypesEnum {
  EMAIL("email", "EMAIL_ADDRESS", "Email address"),
  WEB_SITE("website", "WEB_ADDRESS", "Web site address"),
  DOMAIN("domain", "DOMAIN_NAME", "Domain name"),
  PHONE("work", "TELECOM_NUMBER", "Phone number"),
  ADDRESS("address", "POSTAL_ADDRESS", "Postal Address"),
  LOCATION("location", "POSTAL_ADDRESS", "Location, same as address");

  private String id;
  private String typeId;
  private String description;

  ContactMethodTypesEnum(String id, String typeId, String description) {
    this.id = id;
    this.typeId = typeId;
    this.description = description;
  }

  public String getId() {
    return id;
  }

  public String getTypeId() {
    return typeId;
  }

  public String getDescription() {
    return description;
  }
}
