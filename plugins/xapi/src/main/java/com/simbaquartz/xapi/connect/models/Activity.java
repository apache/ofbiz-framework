package com.simbaquartz.xapi.connect.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.simbaquartz.xapi.connect.models.common.Author;
import lombok.Data;

import java.sql.Timestamp;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Activity   {
  /**
   * Type of activity, email, phone, message etc.
   */
  @JsonProperty("type")
  private String activityType = null;

  @JsonProperty("uuid")
  private String uuid = null;

  @JsonProperty("access_token")
  private String accessToken = null;

  @JsonProperty("start_time")
  private Timestamp startTime;

  @JsonProperty("startTime")
  private Timestamp startDateTime;

  @JsonProperty("long_description")
  private String longDescription;

  @JsonProperty("longDescription")
  private String longDesc;

  @JsonProperty("description")
  private String description;

  @JsonProperty("author")
  private Author author;
}

