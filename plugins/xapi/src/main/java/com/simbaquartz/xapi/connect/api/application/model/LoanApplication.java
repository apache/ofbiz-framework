package com.simbaquartz.xapi.connect.api.application.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fidelissd.zcp.xcommon.models.CreatedModifiedBy;
import com.simbaquartz.xapi.connect.models.classification.Tag;
import com.fidelissd.zcp.xcommon.models.account.User;
import com.fidelissd.zcp.xcommon.models.media.File;
import com.fidelissd.zcp.xcommon.models.people.Person;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** Model definition for Loan application. */
@Data
@EqualsAndHashCode(callSuper = false)
public class LoanApplication extends CreatedModifiedBy {

  /**
   * Unique Identifier of the application.
   */
  @JsonProperty("id")
  private String id;

  /**
   * Name for the application, usually auto generated but can be user overridden.
   */
  @JsonProperty("name")
  private String name;

  /**
   * Owner (manager) of the application.
   */
  @JsonProperty("owner")
  private User owner;

  /**
   * Primary loan applicant.
   */
  @JsonProperty("applicant")
  private Person applicant;

  /**
   * Loan application followers
   */
  @JsonProperty("followers")
  private List<User> followers;

  /**
   * Application related documents.
   */
  @JsonProperty("documents")
  private List<File> documents;


  /**
   * Application related tags.
   */
  @JsonProperty("tags")
  private List<Tag> tags;


  /** Application status text */
  @JsonProperty("status")
  private String status = null;

  /** Application status id */
  @JsonProperty("statusId")
  private String statusId = null;
}
