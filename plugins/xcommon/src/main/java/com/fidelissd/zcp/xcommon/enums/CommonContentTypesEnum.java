package com.fidelissd.zcp.xcommon.enums;

/**
 * For tracking types of content supported.
 */
public enum CommonContentTypesEnum {
  PHOTO("photo","LGOIMGURL","Represents Logo/profile photo for a resource."),
  PUBLIC_URL("publicUrl","PUBLIC_RESOURCE_URL","Publicly accessible url");

  private String label;
  private String typeId;
  private String description;

  CommonContentTypesEnum(String label,String typeId, String description) {
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
