package com.simbaquartz.xcommon.models.client.billing;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import lombok.Getter;
import org.apache.ofbiz.base.util.UtilMisc;

import java.math.BigDecimal;
import java.util.List;

/**
 * Get the billing plan id from https://dashboard.stripe.com/test/products/prod_JUcsEFu87v0rQX
 */
public enum BillingPlans {
  MMO_STARTER_PLAN(
      "price_1Irdd9K4AlhimjG5PoIj8h0I",
      "MMO Starter Plan (Free)",
      "Good for you.",
      BigDecimal.ZERO,
      UtilMisc.toList(
          "Standard support",
          "Up to 5 users",
          "5GB of storage",
          "Task management"
      ),
      UtilMisc.toMap(
          "storageInGb", 5,
          "seatsAvailable", 5
      ),
      true),
  MMO_BASIC_PLAN(
      "price_1Irdd9K4AlhimjG5wy05Ud5f",
      "MMO Basic Plan",
      "Perfect starter for your small team.",
      new BigDecimal(12),
      UtilMisc.toList(
          "Upto 5 users",
          "20 GB storage space",
          "Professional looking PDFs",
          "30+ Employees Support"
      ),
      UtilMisc.toMap(
          "storageInGb", 20,
          "seatsAvailable", 10
      ),
      false),
  MMO_ESSENTIAL_PLAN(
      "price_1Irdd9K4AlhimjG5T8pMxJFk",
      "MMO Essential Plan",
      "Tailored partnership for growing businesses looking for a scalable solution",
      new BigDecimal(16),
      UtilMisc.toList(
          "Standard support",
          "Quote management"
      ),
      UtilMisc.toMap(
          "storageInGb", 50,
          "seatsAvailable", 5
      ),
      false);

  @JsonProperty("id")
  private String planId;
  @JsonProperty("plan_name")
  private String planName;
  @JsonProperty("plan_description")
  private String planDetails;
  @JsonProperty("plan_price")
  private BigDecimal planPrice;

  /**
   * Returns string representation of available features to be shown on the UI
   */
  @JsonProperty("plan_features")
  private List<String> features;

  /**
   * For managing plan limits, like storage limit, seats limit etc.
   */
  @JsonProperty("plan_limits")
  private Map<String, ? extends Object> planLimits;

  @JsonProperty("is_available")
  private Boolean availableForSelection;

  @JsonProperty("upgradeToPlanId")
  @Getter
  private String upgradeToPlanId;
  @JsonProperty("maxUsers")
  @Getter
  private Long maxUsers;

  BillingPlans(String planId, String planName, String planDetails, BigDecimal planPrice,
      List<String> features, Map planLimits, Boolean availableForSelection) {
    this.planId = planId;
    this.planName = planName;
    this.planDetails = planDetails;
    this.planPrice = planPrice;
    this.features = features;
    this.planLimits = planLimits;
    this.availableForSelection = availableForSelection;
  }

  @Override
  public String toString() {
    return String.valueOf(planId);
  }

  @JsonCreator
  public static BillingPlans fromValue(String text) {
    for (BillingPlans b : BillingPlans.values()) {
      if (String.valueOf(b.planId).equals(text)) {
        return b;
      }
    }
    return null;
  }

  public String getPlanId() {
    return this.planId;
  }

  public String getPlanName() {
    return this.planName;
  }

  public String getPlanDetails() {
    return this.planDetails;
  }

  public BigDecimal getPlanPrice() {
    return this.planPrice;
  }

  public List<String> getFeatures() {
    return this.features;
  }

  public Map<String, ? extends Object> getPlanLimits() {
    return planLimits;
  }

  public Boolean isAvailableForSelection() {
    return this.availableForSelection;
  }

  public enum BillingFrequency {
    PER_MONTH("MONTHLY"),
    PER_YEAR("ANNUALLY"),
    PER_DAY("DAY");

    private String billingFrequency;

    BillingFrequency(String billingFrequency) {
      this.billingFrequency = billingFrequency;
    }

    public String getBillingFrequency() {
      return billingFrequency;
    }

    public void setBillingFrequency(String billingFrequency) {
      this.billingFrequency = billingFrequency;
    }
  }

}
