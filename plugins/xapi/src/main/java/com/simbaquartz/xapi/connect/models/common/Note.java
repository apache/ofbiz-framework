package com.simbaquartz.xapi.connect.models.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fidelissd.zcp.xcommon.models.account.User;
import java.sql.Timestamp;
import java.util.List;
import lombok.Data;

/** Represents a Simple Note, can be extended to make task, project person notes etc. */
@Data
public class Note {
  @JsonProperty("id")
  private String id = null;

  /** Rich HTML supported format of the note. */
  @JsonProperty("noteHtml")
  private String noteInfo = null;

  /** Used to store the state of a rich editor with Mentions and other rich media support. */
  @JsonProperty("jsonText")
  private String jsonText = null;

  /** Creation date of the note. */
  @JsonProperty("createdAt")
  private Timestamp noteDateTime = null;

  /** Author of the note */
  @JsonProperty("author")
  private User author = null;
}
