package com.simbaquartz.xparty.services.recent;

/**
 * Created by mande on 4/27/2021.
 */
public enum RecentlyViewedResourceTypesEnum {
  TASK ("Task", "TYPE_TASK"),
  TASK_TEMPLATE("Task template", "TYPE_TASK_TMPLT"),
  PROJECT ("Project", "TYPE_PROJECT"),
  CONTACT ("Contact", "TYPE_CONTACT"),
  LEAD ("Lead", "TYPE_LEAD"),
  DEAL("Deal", "TYPE_DEAL");

  private final String description;
  private final String typeId;

  RecentlyViewedResourceTypesEnum(String value, String partyTypeId)
  {
    this.description = value;
    this.typeId = partyTypeId;
  }

  public String getDescription() {
    return description;
  }
  public String getTypeId()
  {
    return typeId;
  }
}
