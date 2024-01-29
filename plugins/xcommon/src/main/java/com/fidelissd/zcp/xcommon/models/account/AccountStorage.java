package com.fidelissd.zcp.xcommon.models.account;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

import com.fidelissd.zcp.xcommon.models.client.billing.BillingPlans;
import lombok.Data;

/** Represents the account level storage details. */
@Data
public class AccountStorage {

  /**
   * Total account level storage available, usually configured in the selected billing plan.
   *
   * @see BillingPlans
   */
  @JsonProperty("availableInGb")
  private BigDecimal available = BigDecimal.ZERO;

  /** How much of available storage is used (In Bytes) */
  @JsonProperty("used")
  private BigDecimal used = BigDecimal.ZERO;

  /** How much of available storage is used (In GB) */
  @JsonProperty("usedInGb")
  private BigDecimal usedInGb = BigDecimal.ZERO;

  /** How much of available storage is used for storing formatted, e.g. 55 MB */
  @JsonProperty("usedFormatted")
  private String usedFormatted = "0 KB";

  /** How much of %age out of 100 of available storage is used for storing all documents, e.g. 15 */
  @JsonProperty("usedPercentage")
  private Short usedPercentage = 0;

  /** How much of available storage is used for storing task documents (task attachments) */
  @JsonProperty("taskStorage")
  private BigDecimal taskStorage = BigDecimal.ZERO;

  /** How much of available task storage is used for storing formatted, e.g. 55 MB */
  @JsonProperty("taskStorageFormatted")
  private String taskStorageFormatted = "0 KB";

  /** How much of %age out of 100 of available storage is used for storing task documents, e.g. 15 */
  @JsonProperty("taskUsedPercentage")
  private Short taskUsedPercentage = 0;
}
