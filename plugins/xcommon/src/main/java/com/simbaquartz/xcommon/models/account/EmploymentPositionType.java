package com.simbaquartz.xcommon.models.account;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/** Represents the employment position type for an account. */
@Data
public class EmploymentPositionType {

  /** Unique identifier for the position type. */
  @JsonProperty("id")
  private String id;

  /** Position title description. */
  @JsonProperty("description")
  private String description;

  public EmploymentPositionType() {}

  public EmploymentPositionType(String id, String description) {
    this.id = id;
    this.description = description;
  }
}
