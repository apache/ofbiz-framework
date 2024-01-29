package com.simbaquartz.xapi.connect.models;

import com.fasterxml.jackson.annotation.JsonProperty;


@javax.annotation.Generated(value = "class io.swagger.codegen.languages.JavaResteasyServerCodegen", date = "2017-06-30T20:56:17.408-07:00")
public class Userlogin {

  private String firstName = null;
  private String lastName = null;
  private String emailAddress = null;
  private String password = null;
  private String confirmPassword = null;
  private String oldPassword = null;
  private String organizationName = null;
  private String inviteToken = null;
  private String partyId = null;
  private String app = "admin";

  @JsonProperty("confirm_password")
  public String getConfirmPassword() {
    return confirmPassword;
  }

  public void setConfirmPassword(String confirmPassword) {
    this.confirmPassword = confirmPassword;
  }

  @JsonProperty("old_password")
  public String getOldPassword() {
    return oldPassword;
  }

  public void setOldPassword(String oldPassword) {
    this.oldPassword = oldPassword;
  }

  @JsonProperty("invite_token")
  public String getInviteToken() {
    return inviteToken;
  }

  public void setInviteToken(String inviteToken) {
    this.inviteToken = inviteToken;
  }

  @JsonProperty("first_name")
  public String getFirstName() {
    return firstName;
  }

  public void setFirstName(String firstName) {
    this.firstName = firstName;
  }

  @JsonProperty("last_name")
  public String getLastName() {
    return lastName;
  }

  public void setLastName(String lastName) {
    this.lastName = lastName;
  }

  @JsonProperty("email_address")
  public String getEmailAddress() {
    return emailAddress;
  }

  public void setEmailAddress(String emailAddress) {
    this.emailAddress = emailAddress;
  }

  @JsonProperty("password")
  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  @JsonProperty("organization_name")
  public String getOrganizationName() {
    return organizationName;
  }

  public void setOrganizationName(String organizationName) {
    this.organizationName = organizationName;
  }

  @JsonProperty("party_id")
  public String getPartyId() {
    return partyId;
  }

  public void setPartyId(String partyId) {
    this.partyId = partyId;
  }

  @JsonProperty("app")
  public String getApp() {
    return app;
  }

  public void setApp(String app) {
    this.app = app;
  }
}

