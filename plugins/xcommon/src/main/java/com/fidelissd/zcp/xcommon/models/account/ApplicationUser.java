package com.fidelissd.zcp.xcommon.models.account;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fidelissd.zcp.xcommon.models.Workspace;
import com.fidelissd.zcp.xcommon.models.client.billing.Subscription;
import com.fidelissd.zcp.xcommon.models.geo.PostalAddress;
import com.fidelissd.zcp.xcommon.models.store.Store;
import com.fidelissd.zcp.xcommon.models.people.Person;
import java.sql.Timestamp;
import lombok.Data;

/** Represents an application user that can access the application and perform allowed actions. */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ApplicationUser {
  @JsonProperty("id")
  private String id = null;

  /** Personal details of the user, name, email, organization details etc. */
  @JsonProperty("personalDetails")
  private Person personalDetails = null;

  /** First name of the user. */
  @JsonProperty("firstName")
  private String firstName = null;

  /** Last name of the user */
  @JsonProperty("lastName")
  private String lastName = null;

  /** Display name of the user. */
  @JsonProperty("displayName")
  private String displayName = null;

  /** Primary email of the application user. */
  @JsonProperty("email")
  private String email = null;

  /** Whether email of the application user is verified or not. */
  @JsonProperty("emailVerified")
  private Boolean emailVerified = null;

  /**
   * Use this to track any additional external reference ids, like employee number, customer,
   * account number etc.
   */
  @JsonProperty("externalId")
  private String externalId = null;

  /** Publicly accesible photo Url of the user. */
  @JsonProperty("photoUrl")
  private String photoUrl = null;

  /** Date and time when the user was created. */
  @JsonProperty("createdAt")
  private Timestamp createdAt = null;

  /** Date and time when the user last logged into the application. */
  @JsonProperty("lastLoggedInAt")
  private Timestamp lastLoggedInAt = null;

  /** Date and time when the user profile was last modified. */
  @JsonProperty("lastModifiedAt")
  private Timestamp lastModifiedAt = null;

  @JsonProperty("subscription")
  private Subscription subscription = null;

  @JsonProperty("isTourCompleted")
  private Boolean isTourCompleted;

  @JsonProperty("role")
  private String role;

  /** Host url of the application, useful for building relative paths. */
  @JsonProperty("hostUrl")
  private String hostUrl;

  /** Last used workspace info. */
  @JsonProperty("workspace")
  private Workspace workspace = null;

  /**
   * Whether the user corresponds to the user requesting details (logged in user). The default is
   * False.
   */
  @JsonProperty("self")
  private Boolean self = false;

  /**
   * Whether the user corresponds to the user requesting details (logged in user). The default is
   * False.
   */
  @JsonProperty("isAssumed")
  private Boolean isAssumed = false;

  /**
   * Whether the user corresponds to the user requesting details (logged in user). The default is
   * False.
   */
  @JsonProperty("assumedBy")
  private String assumedBy = null;

  /**
   * Default location of the logged in user.
   */
  @JsonProperty("location")
  private PostalAddress location = null;

  /**
   * Last timezone of the logged in user.
   */
  @JsonProperty("timezone")
  private String timezone = null;

  /**
   * Reporting Manager
   */
  @JsonProperty("reportingManager")
  private User reportingManager = null;

  /**
   * Store details
   */
  @JsonProperty("store")
  private Store store = null;

  /**
   * Represents whether the member account is enabled or not. Once a member is removed this will return true.
   */
  @JsonProperty("disabled")
  private Boolean disabled = false;

  @JsonProperty("connectedAccounts")
  private ConnectedAccounts connectedAccounts;
}
