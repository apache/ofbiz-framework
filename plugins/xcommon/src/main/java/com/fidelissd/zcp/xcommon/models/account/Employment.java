package com.fidelissd.zcp.xcommon.models.account;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fidelissd.zcp.xcommon.models.company.Company;
import com.fidelissd.zcp.xcommon.models.people.Person;

import java.sql.Timestamp;
import lombok.Data;

/**
 * Represents the employment an account member has with the account organization. There can be
 * multiple employments that may exist for a member since employees get promoted and their
 * employment changes.
 */
@Data
public class Employment {

  /** Unique identifier for the employment */
  @JsonProperty("id")
  private String id;

  /** Person holding the employment */
  @JsonProperty("employee")
  private Person employee;

  /** Employer offering this employment. */
  @JsonProperty("employer")
  private Company employer;

  /** Position title for the employment, example 'Chief Marketing Officer'. */
  @JsonProperty("position")
  private EmploymentPositionType employmentPositionType;

  /** Employment from date, date when this employment was started, can not be empty. */
  @JsonProperty("fromDate")
  private Timestamp fromDate;

  /**
   * Employment thru date, date when this employment was ended, can be empty in case of an
   * ongoing/active/current employment.
   */
  @JsonProperty("thruDate")
  private Timestamp thruDate;

  /**
   * Indicates if the current employment is the most recent/active/ongoing employment. Defaults to
   * false
   */
  @JsonProperty("isCurrent")
  private boolean isCurrent = false;

  /** Indicates if the current employment is full time. */
  @JsonProperty("isFullTime")
  private boolean isFullTime = false;

  /** Indicates if the employment type is temporary or part time. */
  @JsonProperty("isTemporary")
  private boolean isTemporary = false;

  /** Indicates if the position is remote. */
  @JsonProperty("isRemote")
  private boolean isRemote = false;
}
