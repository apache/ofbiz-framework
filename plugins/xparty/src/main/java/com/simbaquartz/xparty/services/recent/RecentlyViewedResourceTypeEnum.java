package com.simbaquartz.xparty.services.recent;

/**
 * When adding a new entry, please update seed data enumeration as well here.
 * plugins/xparty/data/XpartyTypeData.xml Example <Enumeration description="Deal" enumCode="DEAL"
 * enumId="TYPE_DEAL" sequenceId="05" enumTypeId="RCNT_RSRC_TYPE"/> - Update description, enumCode,
 * enumId - Update sequenceId (+1 from last one)
 */
public enum RecentlyViewedResourceTypeEnum {
  LEAD("TYPE_LEAD", "Lead"),
  DEAL("TYPE_DEAL", "Deal or Opportunity"),
  CONTACT("TYPE_CONTACT", "Contact"),
  PROJECT("TYPE_PROJECT", "Project"),
  TASK_TEMPLATE("TYPE_TASK_TMPLT", "Task template"),
  TASK("TYPE_TASK", "Task");

  private String typeId;
  private String description;

  RecentlyViewedResourceTypeEnum(String typeId, String description) {
    this.typeId = typeId;
    this.description = description;
  }

  public String getTypeId() {
    return typeId;
  }

  public String getDescription() {
    return description;
  }
}
