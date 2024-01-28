package com.simbaquartz.xcommon.models.client;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.simbaquartz.xcommon.models.Fax;
import com.simbaquartz.xcommon.models.Phone;
import com.simbaquartz.xcommon.models.email.EmailAddress;
import com.simbaquartz.xcommon.models.geo.PostalAddress;
import java.util.List;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class Employee {

  @JsonProperty("id")
  private String id = null;

  @JsonProperty("title")
  private String title = null;

  @JsonProperty("suffix")
  private String suffix = null;

  @JsonProperty("displayName")
  private String displayName = null;

  @NotEmpty(message = "Please provide the First Name")
  @JsonProperty("firstName")
  private String firstName = null;

  @JsonProperty("middleName")
  private String middleName = null;

  @NotEmpty(message = "Please provide the Last Name")
  @JsonProperty("lastName")
  private String lastName = null;

  @NotEmpty(message = "Please provide a valid role for the Employee")
  @JsonProperty("role")
  private String role = null;

  @JsonProperty("accountLeadPartyId")
  private String accountLeadPartyId = null;

  @JsonProperty("phoneNumber")
  private Phone phoneNumber = null;

  @JsonProperty("faxNumber")
  private Fax faxNumber = null;

  @JsonProperty("address")
  private PostalAddress address = null;

  @Email(message = "Invalid format for email, please use user@example.org format.")
  @JsonProperty("email")
  private String email = null;

  /** Photo Url of the employee */
  @JsonProperty("photoUrl")
  private String photoUrl = null;

  @JsonProperty("webAddress")
  private String webAddress = null;

  @JsonProperty("phone")
  private String phone = null;

  @JsonProperty("createdAt")
  private String createdAt = null;

  @JsonProperty("updatedAt")
  private String updatedAt = null;

  @JsonProperty("createdBy")
  private String createdBy = null;

  @JsonProperty("lastUpdatedBy")
  private String lastUpdatedBy = null;

  @JsonProperty("designation")
  private String designation = null;

  @JsonProperty("department")
  private String department = null;

  @JsonProperty("emails")
  private List<EmailAddress> emails = null;

  @JsonProperty("phoneNumbers")
  private List<Phone> phoneNumbers = null;
}
