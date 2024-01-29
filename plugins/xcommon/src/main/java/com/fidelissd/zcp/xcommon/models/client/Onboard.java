package com.fidelissd.zcp.xcommon.models.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fidelissd.zcp.xcommon.models.Phone;
import com.fidelissd.zcp.xcommon.models.geo.PostalAddress;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Onboard {
    @JsonProperty("admin_email")
    private String adminEmail;

    @JsonProperty("admin_firstname")
    private String adminFirstName;

    @JsonProperty("admin_lastname")
    private String adminLastName;

    @JsonProperty("admin_password")
    private String adminPassword;

    @JsonProperty("org_name")
    private String organizationName;

    @JsonProperty("org_address")
    private PostalAddress organizationAddress;

    @JsonProperty("org_contact")
    private Phone phoneNumber;

    @JsonProperty("org_email")
    private String orgEmail;

    @JsonProperty("industry")
    private String industry;

    @JsonProperty("org_website")
    private String organizationWebSite;

    @JsonProperty("cage")
    private String cage;

    @JsonProperty("duns")
    private String duns;

    @JsonProperty("tax-id")
    private String taxId;

    @JsonProperty("industry_type")
    private String industryType;

    @JsonProperty("account_number")
    private String accountNumber;

    @JsonProperty("bank_name")
    private String bankName;

    @JsonProperty("routing_number")
    private String routingNumber;

    @JsonProperty("business_location")
    private String businessLocation;

    @JsonProperty("org_customer")
    private String organizationCustomer;

    @JsonProperty("should_load_govt_data")
    private boolean shouldLoadGovtData;
}
