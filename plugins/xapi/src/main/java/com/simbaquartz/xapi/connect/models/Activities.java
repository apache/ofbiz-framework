package com.simbaquartz.xapi.connect.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Activities   {
  @JsonProperty("offset")
  private Integer offset = null;
  @JsonProperty("limit")
  private Integer limit = null;
  @JsonProperty("count")
  private Integer count = null;
  @JsonProperty("history")
  private List<Activity> history = new ArrayList<Activity>();

}

