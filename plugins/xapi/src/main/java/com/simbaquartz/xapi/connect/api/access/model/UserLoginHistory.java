package com.simbaquartz.xapi.connect.api.access.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Represents the login history for a user account.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserLoginHistory {
  /**
   * Log in id of the user.
   */
  @JsonProperty("id")
  private String id;

  /**
   * Device type, mobile/desktop etc.
   */
  @JsonProperty("deviceType")
  private String deviceType;


}
