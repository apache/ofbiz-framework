package com.fidelissd.zcp.xcommon.models.client.billing;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Date;
import lombok.Data;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Subscription {

    @JsonProperty("subscriptionId")
    private String subscriptionId;

    @JsonProperty("customerId")
    private String customerId;

    @JsonProperty("planId")
    private String billingPlanId;

    // This is used to process a credit card information used only when creating a subscription for an existing customer
    // UI injects an iframe using chargifyjs and generates a token after authenticating user's card number and
    // billing address details. https://developer.chargify.com/content/chargify-js/chargify-js.html
    // This token is then passed to the api to create a subscription
    // https://reference.chargify.com/v1/subscriptions/subscription-request-examples#existing-customers
    @JsonProperty("chargifyToken")
    private String chargifyPaymentToken;

    @JsonProperty("billingFirstName")
    private String billingFirstName;

    @JsonProperty("billingLastName")
    private String billingLastName;

    @JsonProperty("billingAddress1")
    private String billingAddress1;

    @JsonProperty("billingAddress2")
    private String billingAddress2;

    @JsonProperty("billingCity")
    private String billingCity;

    @JsonProperty("billingState")
    private String billingState;

    @JsonProperty("billingCountry")
    private String billingCountry;

    @JsonProperty("billingZip")
    private String billingZip;

    @JsonProperty("billingPlanName")
    private String billingPlanName;

    @JsonProperty("billingPlanDetails")
    private String billingPlanDetails;

    @JsonProperty("billingPlanPrice")
    private BigDecimal billingPlanPrice;

    @JsonProperty("billingPlanfeatures")
    private List<String> billingPlanfeatures;

    @JsonProperty("billingFrequency")
    private String billingFrequency; // /mo or /yr

    @JsonProperty("status")
    private String subscriptionStatus;//active/expired/past-due

    @JsonProperty("isActive")
    private boolean isActive;

    @JsonProperty("isSubscriptionNeeded")
    private boolean needsSubscription;

    @JsonProperty("promptUpgrade")
    private boolean promptUpgrade = false;

    @JsonProperty("promptUpgradeToPlan")
    private String promptUpgradeToPlanId = "";

    @JsonProperty("upgradeLink")
    private String upgradeLink;

    @JsonProperty("startDate")
    private Timestamp subscriptionStartDate;

    @JsonProperty("endDate")
    private Timestamp subscriptionEndDate;

    @JsonProperty("nextBillingDate")
    private Timestamp nextBillingDate;

    @JsonProperty("nextBillingAmount")
    private BigDecimal nextBillingAmount;

    @JsonProperty("dueInvoices")
    private List<BillingInvoice> dueInvoices;

    @JsonProperty("availablePlans")
    private List<BillingPlans> availablePlans;

    @JsonProperty("allPlans")
    private List<BillingPlans> allPlans;

    @JsonProperty("cancelAtEndOfPeriod")
    private Boolean cancelAtEndOfPeriod;

    @JsonProperty("delayedCancelAt")
    private Date delayedCancelAt;

    @JsonProperty("isTrialing")
    private Boolean isTrialing;

    @JsonProperty("trialStartDate")
    private Date trialStartDate;

    @JsonProperty("trialEndDate")
    private Date trialEndDate;

    @JsonProperty("maxUsersAllowed")
    private Long maxUsersAllowed;

    @JsonProperty("maxStorageLimitInGb")
    private BigDecimal maxStorageLimitInGb;

    @JsonProperty("hasActiveLicense")
    private Boolean hasActiveLicense;
}
