package com.simbaquartz.xapi.connect.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fidelissd.zcp.xcommon.collections.FastList;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Data
@javax.annotation.Generated(value = "class io.swagger.codegen.languages.JavaResteasyServerCodegen", date = "2017-06-30T20:56:17.408-07:00")
public class ProjectItem {

  @JsonProperty("project_id")
  private String projectId = null;

  @JsonProperty("item_type_id")
  private String itemTypeId = null;

  @JsonProperty("task_id")
  private String taskId = null;

  @JsonProperty("name")
  private String name = null;

  @JsonProperty("description")
  private String description = null;

  @JsonProperty("status_id")
  private String statusId = null;

  @JsonProperty("reported_by_party_id")
  private String reportedByPartyId = null;

  @JsonProperty("assigned_to_party_id")
  private String assignedToPartyId = null;

  @JsonProperty("start_date")
  private Timestamp startDate = null;

  @JsonProperty("end_date")
  private Timestamp endDate = null;

  @JsonProperty("reported_date")
  private Timestamp reportedDate = null;

  @JsonProperty("from_start_date")
  private String fromStartDate = null;

  @JsonProperty("to_start_date")
  private String toStartDate = null;

  @JsonProperty("from_open_date")
  private String fromOpenDate = null;

  @JsonProperty("to_open_date")
  private String toOpenDate = null;

  @JsonProperty("from_closed_date")
  private String fromClosedDate = null;

  @JsonProperty("to_closed_date")
  private String toClosedDate = null;

  @JsonProperty("priority")
  private String priority = null;

  @JsonProperty("component")
  private String component = null;

  @JsonProperty("tag")
  private String tag = null;

  @JsonProperty("epic")
  private String epic = null;

  @JsonProperty("epic_name")
  private String epicName = null;

  @JsonProperty("planned_hours")
  private String plannedHours = null;

  @JsonProperty("source_page")
  private String sourcePage = null;

  @JsonProperty("sort_by")
  private String sortBy = null;

  @JsonProperty("versions")
  private List<String> versions = null;

  @JsonProperty("start_index")
  private Integer startIndex = null;

  @JsonProperty("view_size")
  private Integer viewSize = null;

  @JsonProperty("project_items_list")
  private List<String> projectItemsList = FastList.newInstance();

  @JsonProperty("content_ids")
  private List<String> contentIds = FastList.newInstance();

  @JsonProperty("components")
  private List<Map> components = FastList.newInstance();

  @JsonProperty("componentIds")
  private List<String> componentIds = FastList.newInstance();

  @JsonProperty("tags")
  private List<Map> tags = FastList.newInstance();

  @JsonProperty("tagNames")
  private List<String> tagNames = FastList.newInstance();

  @JsonProperty("from_backlog_page")
  private String fromBacklogPage = "";
}

