package com.simbaquartz.xapi.connect.models.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/** Represents progress that can be used to track progress for tasks/goals/projects etc. */
@Data
public class Progress {
  /** Percentage of tasks done in the project, tasks. */
  @JsonProperty("percentDone")
  private Double percentDone = 0d;

  /** Total done count for the progress. */
  @JsonProperty("doneCount")
  private Long doneCount = 0l;

  /** Total count for the progress. */
  @JsonProperty("totalCount")
  private Long totalCount = 0l;
}
