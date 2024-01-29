package com.fidelissd.zcp.xcommon.models.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.sql.Timestamp;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Client {
    @JsonProperty("id")
    private String id;

    @NotNull(message = "Client name is required")
    @NotBlank(message = "Client name is required")
    @JsonProperty("name")
    private String clientName;

    @JsonProperty("contact")
    private Employee clientEmployee;

    @JsonProperty("domain")
    private String domain;

    @JsonProperty("comments")
    private String comments;

    @JsonProperty("domain_login_enabled")
    private Boolean domainLoginEnabled;

    @JsonProperty("tenant_id")
    private String tenantId;

    @JsonProperty("disabled")
    private Boolean disabled;

    @JsonProperty("created_at")
    private Timestamp createdAt;

    @JsonProperty("created_by")
    private Timestamp createdBy;

    @JsonProperty("last_modified_at")
    private Timestamp lastModifiedAt;

    @JsonProperty("last_modified_by")
    private Timestamp lastModifiedBy;

    @JsonProperty("country_code")
    private String countryCode;

    @JsonProperty("country")
    private String countryName;

    @JsonProperty("city")
    private String city;

    @JsonProperty("referring_url")
    private String referringUrl;

    @JsonProperty("employees_count")
    private long numberOfEmployees;

    @JsonProperty("website")
    private String website;

    @JsonProperty("linkedin")
    private String linkedin;

    @JsonProperty("facebook")
    private String facebook;

    @JsonProperty("instagram")
    private String instagram;

    @JsonProperty("twitter")
    private String twitter;

    @Email(message = "Please provide a valid email address")
    @JsonProperty("email")
    private String email;

    @JsonProperty("phone_country_code")
    private String phoneCountryCode;

    @JsonProperty("phone")
    private String phone;

    @JsonProperty("phone_formatted")
    private String phoneFormatted;

    @JsonProperty("alias")
    private String alias;

    @JsonProperty("licenses_purchased")
    private int licensesPurchased;

    @JsonProperty("lifetime_value")
    private BigDecimal lifetimeValue;

}
