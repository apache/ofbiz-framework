package com.simbaquartz.xapi.connect.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;
import com.fidelissd.zcp.xcommon.collections.FastList;
import lombok.Data;

/**
 * Represents a Job Role object.
 **/
@Data
@javax.annotation.Generated(value = "class io.swagger.codegen.languages.JavaResteasyServerCodegen", date = "2018-05-11T11:44:45.432+05:30")
public class JobRole {

  @JsonProperty("role_id")
  private String id = null; //jobRoleId

  @JsonProperty("parent_type_id")
  private String parentTypeId = null; //jobRole parentTypeId

  @JsonProperty("project_id")
  private String projectId = null;

  @JsonProperty("role_name")
  private String roleName = null;

  @JsonProperty("role_responsibilities")
  private String roleResponsibilities = null;

  @JsonProperty("daily_hours")
  private BigDecimal dailyHours = null;

  @JsonProperty("weekly_hours")
  private BigDecimal weeklyHours = null;

  @JsonProperty("billable_hours")
  private BigDecimal billableHours = null;

  @JsonProperty("nonBillable_hours")
  private BigDecimal nonBillableHours = null;

  @JsonProperty("assigned_hours")
  private BigDecimal assignedHours = null;

  @JsonProperty("unAssigned_hours")
  private BigDecimal unAssignedHours = null;

  @JsonProperty("monthly_hours")
  private BigDecimal monthlyHours = null;

  @JsonProperty("hourly_charges")
  private BigDecimal hourlyCharges = null;

  @JsonProperty("time_in_milli_sec")
  private Long timeInMilliSec = null;

  @JsonProperty("plan_date")
  private Timestamp planDate = null;

  @JsonProperty("description")
  private String description  = null;

  @JsonProperty("work_schedules")
  private List<String> workSchedules  = null;
}

