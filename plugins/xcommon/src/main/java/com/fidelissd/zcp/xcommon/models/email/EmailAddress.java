package com.fidelissd.zcp.xcommon.models.email;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotEmpty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class EmailAddress {

  /** Unique identifier representing an email address. */
  @JsonProperty("id")
  private String id = null;

  /** Valid email address like user@example.com */
  @NotEmpty(message = "Please provide a email")
  @Email(message = "Invalid format for email, please use user@example.org format.")
  @JsonProperty("emailAddress")
  private String emailAddress = null;

  /** Whether the email address is verified or not. Defaults to false. */
  @JsonProperty("verified")
  private Boolean verified = false;

  /** True if the email address represents primary address. Defaults to true. */
  @JsonProperty("isPrimary")
  private Boolean isPrimary = true;

  /**
   * Whether the email owner accepts marketing emails or not, defaults to false.
   */
  @JsonProperty("acceptsMarketing")
  private Boolean acceptsMarketing = false;

  @JsonProperty("label")
  private String label = null;

  @JsonProperty("other")
  private String other = null;

  @JsonProperty("deleted")
  private Boolean deleted = null;

  @JsonProperty("labelId")
  private String contactMechPurposeTypeId = null;
}
