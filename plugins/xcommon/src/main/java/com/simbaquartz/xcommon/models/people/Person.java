package com.simbaquartz.xcommon.models.people;

import com.fasterxml.jackson.annotation.JsonProperty;

import com.simbaquartz.xcommon.models.*;
import com.simbaquartz.xcommon.models.email.EmailAddress;
import com.simbaquartz.xcommon.models.geo.PostalAddress;
import com.simbaquartz.xcommon.models.geo.Timezone;
import java.util.Date;
import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** Represents a person with name, contact details etc. */
@Data
@EqualsAndHashCode(callSuper = false)
public class Person extends CreatedModifiedBy {
  @JsonProperty("id")
  private String id = null;

  /** Full name including any prefix, suffix. Example Dr. John Doe Sr. MD */
  @JsonProperty("fullName")
  private String fullName = null;

  /**
   * Display name for a person, this varies from region to region. Some call it John Doe, some call
   * it Doe, John
   */
  @JsonProperty("displayName")
  private String displayName = null;

  /** First name of the person */
  @JsonProperty("firstName")
  private String firstName = null;

  /** Middle name of the person */
  @JsonProperty("middleName")
  private String middleName = null;

  /** Last name of the person */
  @JsonProperty("lastName")
  private String lastName = null;

  /** Initials for the person. Example JD for John Doe */
  @JsonProperty("initials")
  private String partyInitials = null;

  /**
   * Email of the person. Example john.doe@example.com
   */
  @JsonProperty("email")
  private String email = null;

  @JsonProperty("photoUrl")
  private String photoUrl = null;

  /** Profile photo of the person, usually their avatar image. */
  @JsonProperty("photo")
  private Photo photo = null;

  @JsonProperty("phone")
  private Phone primaryPhone = null;

  /** A person's location. For example London, UK */
  @JsonProperty("location")
  private String location = null;

  /** A person's name prefix. For example Dr, Col */
  @JsonProperty("prefix")
  private String prefix = null;

  /** A person's name suffix. For example Jr, MD */
  @JsonProperty("suffix")
  private String suffix = null;

  @JsonProperty("birthday")
  private Date birthday = null;

  /** A person's Salutation. For example Col, Maj */
  @JsonProperty("salutation")
  private String salutation = null;

  /** Gender of the person, male, female, other */
  @JsonProperty("gender")
  private String gender = null;

  /**
   * Title of the person with reference to an organization or team. Example President, Team Leader
   * etc.
   */
  @JsonProperty("title")
  private String title = null;

  @JsonProperty("enabled")
  private Boolean partyEnabled = null;

  @JsonProperty("requiresPasswordChange")
  private Boolean requirePasswordChange = null;

  @JsonProperty("designation")
  private String designation = null;

  /** Person's current organization. */
  @JsonProperty("organization")
  private Organization organization = null;

  /** Person's past or current organization. */
  @JsonProperty("organizations")
  private List<Organization> organizations = null;

  @JsonProperty("companyName")
  private String companyName = null;

  @JsonProperty("companyId")
  private String companyPartyId = null;

  @JsonProperty("jobTitle")
  private String companyDesignation = null;

  @JsonProperty("department")
  private String companyDepartment = null;

  @JsonProperty("parentId")
  private String parentPartyId = null;

  @JsonProperty("parentName")
  private String parentPartyName = null;

  @JsonProperty("parentEmail")
  private String parentPartyEmail = null;

  @JsonProperty("defaultCurrency")
  private String preferredCurrencyUomId = null;

  @JsonProperty("address")
  private PostalAddress primaryAddress = null;

  @JsonProperty("phones")
  private List<Phone> phones = null;

  @JsonProperty("addresses")
  private List<PostalAddress> addresses = null;

  @JsonProperty("isManager")
  private Boolean isManager = null;

  @JsonProperty("logoContentId")
  private String logoContentId = null;

  @JsonProperty("linkedInAddress")
  private String linkedInAddress = null;

  @JsonProperty("groupName")
  private String groupName = null;

  @JsonProperty("nickName")
  private String nickName = null;

  @JsonProperty("phoneList")
  private List<Phone> phoneList = null;

  @JsonProperty("emailList")
  private List<Map<String, Object>> emailList = null;

  @JsonProperty("emails")
  private List<EmailAddress> emailAddress = null;

  @JsonProperty("webAddresses")
  private List<WebAddress> webAddress = null;

  @JsonProperty("role")
  private String roleTypeId = null;

  @JsonProperty("publicUrl")
  private String publicResourceUrl = null;

  @JsonProperty("thumbnailUrl")
  private String thumbNailUrl = null;

  @JsonProperty("roles")
  private List<String> roles = null;

  @JsonProperty("partyIds")
  private List<String> partyIds = null;

  /** About section for the person, hobbies, family history etc. */
  @JsonProperty("about")
  private String aboutText = null;

  @JsonProperty("linkedinAddresses")
  private List<LinkedInAddress> linkedInAddresses;

  @JsonProperty("profile")
  private String profile = null;

  @JsonProperty("timezoneId")
  private String timezoneId = null;

  @JsonProperty("timezone")
  private Timezone timezone = null;

  /** Use this to get/set person specific attributes, example features enabled, feature seen etc. */
  @JsonProperty("attributes")
  private List<Map> attributes = null;

  /**
   * Race details
   */
  @JsonProperty("race")
  private List<String> raceList = null;
}
