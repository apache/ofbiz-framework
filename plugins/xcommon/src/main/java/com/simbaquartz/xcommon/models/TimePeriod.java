package com.simbaquartz.xcommon.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.sql.Timestamp;
import lombok.Data;

/** Model definition for TimePeriod. */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TimePeriod {

  /** The (inclusive) start of the time period. The value may be {@code null}. */
  @JsonProperty("start")
  private Timestamp start = null;

  /** The (exclusive) end of the time period. The value may be {@code null}. */
  @JsonProperty("end")
  private Timestamp end = null;
}
