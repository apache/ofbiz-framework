package com.simbaquartz.xapi.connect.models.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fidelissd.zcp.xcommon.models.people.Person;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** For displaying author information for entities (Notes, documents etc.) */
@Data
@Builder
@EqualsAndHashCode(callSuper = false)
public class Author extends Person {

  /** A plain text displayable name for this user. */
  @JsonProperty("displayName")
  private String displayName;

  /** A link to the user's profile photo, if available. */
  @JsonProperty("photoUrl")
  private String photoUrl;

  /**
   * Whether the user corresponds to the user requesting details (logged in user). The default is
   * False.
   */
  @JsonProperty("self")
  private boolean self;

  /** The email address of the user. */
  @JsonProperty("email")
  private String email;

  /** A link to the user's profile photo, if available. */
  @JsonProperty("photoLink")
  private String photoLink;
}
