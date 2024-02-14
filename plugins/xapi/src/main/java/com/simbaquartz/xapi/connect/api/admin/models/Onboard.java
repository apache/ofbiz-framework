package com.simbaquartz.xapi.connect.api.admin.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fidelissd.zcp.xcommon.models.Phone;
import com.fidelissd.zcp.xcommon.models.geo.PostalAddress;
import lombok.*;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Onboard {
    @JsonProperty("admin")
    private OrgAdminInfo admin;

    @JsonProperty("organizationName")
    private String organizationName;

    @JsonProperty("organizationAddress")
    private PostalAddress organizationAddress;

    @JsonProperty("orgPhone")
    private Phone orgPhone;

    @JsonProperty("orgEmail")
    private String orgEmail;

    @JsonProperty("orgWebSite")
    private String orgWebSite;

    @JsonProperty("industry")
    private String industry;

    @JsonProperty("cage")
    private String cage;

    @JsonProperty("duns")
    private String duns;

    @JsonProperty("taxId")
    private String taxId;

    @JsonProperty("accountNumber")
    private String accountNumber;

    @JsonProperty("bankName")
    private String bankName;

    @JsonProperty("routingNumber")
    private String routingNumber;

    @JsonProperty("businessLocation")
    private String businessLocation;

    @JsonProperty("organizationCustomer")
    private String organizationCustomer;

    @JsonProperty("shouldLoadGovtData")
    private boolean shouldLoadGovtData;

    @JsonProperty("acceptsMarketing")
    private boolean acceptsMarketing;

    @Builder
    @Data
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class OrgAdminInfo {
        @JsonProperty("email")
        private String email;

        @JsonProperty("firstName")
        private String firstName;

        @JsonProperty("lastName")
        private String lastName;

        @JsonProperty("password")
        private String password;
    }
}
