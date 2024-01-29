package com.fidelissd.zcp.xcommon.models.account;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import lombok.Data;

/**
 * Represents an application account (MMO in this case).
 * This is the account that manages subscription and licenses to use mmo services.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ApplicationAccount {
    /**
     * Unique account ID.
     */
    @JsonProperty("id")
    private String id;

    /**
     * Primary email address associated with the account
     */
    @NotNull(message = "Email is required")
    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    @JsonProperty("email")
    private String email;

    /**
     * Change the role of the account members
     */
    @JsonProperty("role")
    String role;

    /**
     * Whether user opts in to marketing, advise, help emails or not
     */
    @JsonProperty("acceptsMarketing")
    private Boolean acceptsMarketing = false;

    /**
     * Full name of the person who is the owner of the organization
     */
    @NotNull(message = "Full name is required")
    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 100, message
            = "Full name must be between 2 and 100 characters long")
    @JsonProperty("fullName")
    private String fullName;

    /**
     * Account owner's login password.
     */
    @JsonProperty("password")
    private String password;

    /**
     * Name of the organization, this is optional if not passed it'll be auto
     * generated in the format "Full Name's Company"
     */
    @JsonProperty("organizationName")
    private String organizationName;

    /**
     * If not passed this will be auto extracted from the email address
     * this will be used to allow auto on-boardings from this domain.
     */
    @JsonProperty("domain")
    private String domain;

    /**
     * Number of employees in the organization.
     */
    @JsonProperty("numberOfEmployees")
    private int numberOfEmployees;

}