package com.fidelissd.zcp.xcommon.models.people;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fidelissd.zcp.xcommon.models.Phone;
import com.fidelissd.zcp.xcommon.models.WebAddress;
import com.fidelissd.zcp.xcommon.models.account.ConnectedAccounts;
import com.fidelissd.zcp.xcommon.models.email.EmailAddress;
import com.fidelissd.zcp.xcommon.models.geo.PostalAddress;
import java.util.List;
import lombok.Data;

import java.util.Date;

/**
 * A person's past or current organization. Overlapping date ranges are permitted.
 * Ref: https://developers.google.com/people/api/rest/v1/people#organization
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Organization {

    /**
     * Unique identifier of the organization
     */
    @JsonProperty("id")
    private String id = null;//party group party id

    /**
     * Name of the organization
     */
    @JsonProperty("name")
    private String organizationName = null;

    /**
     * The type of the organization. The type can be custom or one of these predefined values:
     *  - work
     *  - school
     */
    @JsonProperty("type")
    private String type = null;

    /**
     * The start date when the person joined the organization.
     */
    @JsonProperty("start_date")
    private Date startDate = null;

    /**
     * The end date when the person left the organization.
     */
    @JsonProperty("end_date")
    private Date endDate = null;

    /**
     * True if the organization is the person's current organization; false if the organization is a past organization.
     */
    @JsonProperty("is_current")
    private Boolean current = null;

    /**
     * The person's department at the organization.
     */
    @JsonProperty("department")
    private String department = null;

    /**
     * The person's job title at the organization.
     */
    @JsonProperty("title")
    private String title = null;

    /**
     * The person's job description at the organization.
     */
    @JsonProperty("job_description")
    private String jobDescription = null;

    /**
     * The domain name associated with the organization.
     */
    @JsonProperty("domain")
    private String domain = null;

    @JsonProperty("industry_type")
    private String industryType = null;

    @JsonProperty("cage")
    private String cage = null;

    @JsonProperty("duns")
    private String duns = null;

    @JsonProperty("taxId")
    private String taxId = null;

    @JsonProperty("site_name")
    private String officeSiteName = null;

    @JsonProperty("annual_revenue")
    private String annualRevenue = null;

    @JsonProperty("bank")
    private String bank = null;

    @JsonProperty("routing")
    private String routing = null;

    @JsonProperty("account_number")
    private String accountNumber = null;

    @JsonProperty("email")
    private String email = null;

    @JsonProperty("party_initials")
    private String partyInitials = null;

    @JsonProperty("logo_image_url")
    private String logoImageUrl = null;

    @JsonProperty("phone")
    private List<Phone> phone = null;

    @JsonProperty("emailAddress")
    private List<EmailAddress> emailAddress = null;

    @JsonProperty("webAddress")
    private List<WebAddress> webAddress = null;

    @JsonProperty("address")
    private List<PostalAddress> address = null;

    @JsonProperty("quote_id_prefix")
    private String quoteIdPrefix = null;

    @JsonProperty("primaryAddress")
    private PostalAddress primaryAddress = null;

    @JsonProperty("primaryPhone")
    private Phone primaryPhone = null;

    @JsonProperty("connectedAccounts")
    private ConnectedAccounts connectedAccounts;
}
