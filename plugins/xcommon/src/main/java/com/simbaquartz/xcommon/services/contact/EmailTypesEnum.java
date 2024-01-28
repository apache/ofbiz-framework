package com.simbaquartz.xcommon.services.contact;

public enum EmailTypesEnum {

  HOME("home","EMAIL_HOME","Home email"),
  WORK("work","EMAIL_WORK","Work email"),
  PRIMARY("primary","PRIMARY_EMAIL","Primary email"),
  OTHER("other","OTHER_EMAIL","Other email"),
  OFFICE("office","EMAIL_WORK_SEC","Office email");

  private String label;
  private String typeId;
  private String description;

  EmailTypesEnum(String label,String typeId, String description) {
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
