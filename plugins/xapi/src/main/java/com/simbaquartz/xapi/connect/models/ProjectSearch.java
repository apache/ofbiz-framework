package com.simbaquartz.xapi.connect.models;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.sql.Timestamp;
import java.util.Objects;
import com.fidelissd.zcp.xcommon.collections.FastList;
import lombok.Data;

import java.util.List;

@Data
@javax.annotation.Generated(value = "class io.swagger.codegen.languages.JavaResteasyServerCodegen", date = "2017-06-30T20:56:17.408-07:00")
public class ProjectSearch {

  @JsonProperty("id")
  private String id = null;

  @JsonProperty("parent_project_id")
  private String parentProjectId = null;

  @JsonProperty("project_type_id")
  private String projectTypeId = null;

  @JsonProperty("name")
  private String name = null;

  @JsonProperty("description")
  private String description = null;

  @JsonProperty("status_id")
  private String statusId = null;

  @JsonProperty("owner_ids")
  private List<String> ownerIds = null;

  @JsonProperty("start_date")
  private String startDate = null;

  @JsonProperty("end_date")
  private String endDate = null;

  @JsonProperty("created_from_date")
  private String createdFromDate = null;

  @JsonProperty("created_to_date")
  private String createdToDate = null;

  @JsonProperty("is_billable")
  private Boolean isBillable = false;

  //task filters
  @JsonProperty("is_task_filter")
  private String isTaskFilter =  null;

  @JsonProperty("task_assignments")
  private List<String> taskAssignments = null;

  @JsonProperty("task_status_ids")
  private List<String> taskStatusIds = null;

  @JsonProperty("task_status_type_ids")
  private List<String> statusTypeIds = null;

 @JsonProperty("task_type_ids")
  private List<String> taskTypeIds = null;

 @JsonProperty("task_sub_type_ids")
  private List<String> taskSubTypeIds = null;

 @JsonProperty("task_priorities")
  private List<String> taskPriorities = null;

 @JsonProperty("reporters")
  private List<String> reporters = null;

 @JsonProperty("task_category_ids")
  private List<String> taskCategoryIds = null;

 @JsonProperty("task_source_page_ids")
  private List<String> taskSourcePageIds = null;

  @JsonProperty("task_start_from_date")
  private String taskStartFromDate = null;

  @JsonProperty("task_start_to_date")
  private String taskStartToDate = null;

  @JsonProperty("from_open_date")
  private String fromOpenDate = null;

  @JsonProperty("to_open_date")
  private String toOpenDate = null;

  @JsonProperty("due_date_from")
  private String dueDateFrom = null;

  @JsonProperty("due_date_to")
  private String dueDateTo = null;
}

