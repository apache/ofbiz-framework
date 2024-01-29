package com.fidelissd.zcp.xcommon.enums;

/**
 * Represents various types of tasks, task, template, milestone, goal
 */
public enum TaskTypesEnum {
  TASK("TASK_TYPE_TASK", "Basic task"),
  ACTION("TASK_TYPE_ACTION", "Action triggered by an event (call or something)"),
  ACTIVITY("TASK_TYPE_ACTIVITY", "Activity, activities can be used to measure time spent, example reading book, training etc."),
  MILESTONE("TASK_TYPE_MLSTN", "Milestone task"),
  TEMPLATE("TASK_TYPE_TEMPLATE", "Task template");

  private String typeId;
  private String description;

  TaskTypesEnum(String typeId, String description) {
    this.typeId = typeId;
    this.description = description;
  }

  public String getTypeId() {
    return typeId;
  }

  public void setTypeId(String typeId) {
    this.typeId = typeId;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  @Override
  public String toString() {
    return typeId;
  }
}