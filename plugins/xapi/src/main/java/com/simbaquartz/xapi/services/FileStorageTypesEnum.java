package com.simbaquartz.xapi.services;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum FileStorageTypesEnum {
  TOTAL("totalStorageUsed", "Total storage used"),
  TASK("taskStorage", "Task related documents storage"),
  LEAD("leadStorage", "Lead related documents storage"),
  MEMBER("memberStorage", "Member related documents storage");

  private String typeId;
  private String description;

  FileStorageTypesEnum(String typeId, String description) {
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

  public static List<String> getTypeIds() {
    return Stream.of(FileStorageTypesEnum.values())
        .map(FileStorageTypesEnum::getTypeId)
        .collect(Collectors.toList());
  }
}
