package com.simbaquartz.xcommon.models.company;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class LeaveType {

  // leave type
  @JsonProperty("id")
  private String id;

  // leave type description
  @JsonProperty("leaveType")
  private String leaveType;
}
