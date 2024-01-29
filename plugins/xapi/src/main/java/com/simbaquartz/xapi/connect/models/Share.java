package com.simbaquartz.xapi.connect.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@javax.annotation.Generated(value = "class io.swagger.codegen.languages.JavaResteasyServerCodegen", date = "2017-06-30T20:56:17.408-07:00")
public class Share {

  @JsonProperty("sharePreferenceId")
  private String sharePreferenceId = null;

  @JsonProperty("sharePreferenceTypeId")
  private String sharePreferenceTypeId = null;

  @JsonProperty("taskId")
  private String taskId = null;

  @JsonProperty("projectId")
  private String projectId = null;

  @JsonProperty("folderId")
  private String folderId = null;

  @JsonProperty("workspaceId")
  private String workspaceId = null;

  @JsonProperty("description")
  private String description = null;

  @JsonProperty("accessType")
  private String accessType = null;

  @JsonProperty("shareItems")
  private List<Map> shareItems = null;

  @JsonProperty("partyIds")
  private List<String> partyIds = null;
}

