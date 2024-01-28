package com.simbaquartz.xcommon.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.sql.Timestamp;
import lombok.Data;

@Data
@javax.annotation.Generated(value = "class io.swagger.codegen.languages.JavaResteasyServerCodegen", date = "2017-06-30T20:56:17.408-07:00")
public class Workspace {

  @JsonProperty("id")
  private String workspaceId = null;

  @JsonProperty("name")
  private String name = null;

  @JsonProperty("description")
  private String description = null;

  @JsonProperty("assoc_id")
  private String assocId = null;

  @JsonProperty("avatar_url")
  private String avatarUrl = null;

  @JsonProperty("access_type")
  private String accessType = null;

  @JsonProperty("archived")
  private String archived = null;

  @JsonProperty("assoc_type_id")
  private String assocTypeId = null;

  @JsonProperty("createdAt")
  private Timestamp createdAt = null;

}

