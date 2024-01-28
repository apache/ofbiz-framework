package com.simbaquartz.xcommon.models.company;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.simbaquartz.xcommon.models.Phone;
import com.simbaquartz.xcommon.models.WebAddress;
import com.simbaquartz.xcommon.models.email.EmailAddress;
import com.simbaquartz.xcommon.models.geo.PostalAddress;
import com.simbaquartz.xcommon.models.people.Person;
import java.util.List;
import lombok.Data;

/**
 * Represents a company object. A company is a legal entity formed by a group of individuals to
 * engage in and operate a business—commercial or industrial—enterprise.
 *
 * <p><a href="https://www.investopedia.com/terms/c/company.asp>See more...</a>
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Company {

  @JsonProperty("id")
  private String id = null;

  @JsonProperty("name")
  private String name = null;

  /**
   * Primary email address of the company
   */
  @JsonProperty("email")
  private String email = null;

  /**
   * Web site of the company
   */
  @JsonProperty("webSite")
  private String webSite = null;

  @JsonProperty("photoUrl")
  private String photoUrl = null;

  @JsonProperty("address")
  private List<PostalAddress> address = null;

  /**
   * Point of contacts for the company.
   */
  @JsonProperty("contacts")
  private List<Person> contacts = null;

  @JsonProperty("shippingAddress")
  private List<PostalAddress> shippingAddress = null;

  @JsonProperty("billing_address")
  private List<PostalAddress> billingAddress = null;

  @JsonProperty("phone")
  private List<Phone> phone = null;

  @JsonProperty("webAddress")
  private List<WebAddress> webAddress = null;

  @JsonProperty("emailAddress")
  private List<EmailAddress> emailAddress = null;

  @JsonProperty("totalEmployees")
  private int totalEmployees;

  @JsonProperty("businessType")
  private String businessType = null;

  @JsonProperty("businessRole")
  private String businessRole = null;


  /**
   * Company locations (subsidiaries)
   */
  @JsonProperty("locations")
  private List<PostalAddress> locations = null;

}
