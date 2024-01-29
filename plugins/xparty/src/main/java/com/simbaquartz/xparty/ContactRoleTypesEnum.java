package com.simbaquartz.xparty;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Roles a Contact can have. For example a contact can be a customer, lead, vendor */
public enum ContactRoleTypesEnum {
  CUSTOMER("CUSTOMER", "Customer"),
  VENDOR("VENDOR", "Vendor"),
  LEAD("LEAD", "Lead");

  private final String typeId;
  private final String description;

  ContactRoleTypesEnum(String typeId, String description) {
    this.typeId = typeId;
    this.description = description;
  }

  public String getDescription() {
    return description;
  }

  public String getTypeId() {
    return typeId;
  }

  public static List<String> getTypeIds() {
    return Stream.of(ContactRoleTypesEnum.values()).map(ContactRoleTypesEnum::getTypeId).collect(Collectors.toList());
  }
}
