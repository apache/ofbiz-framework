package com.simbaquartz.xapi.connect.models;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a Client Search object.
 **/

public class ClientSearch{
  
  private String clientName = null;
  private String billingStatus = null;

  private Integer startIndex = null;
  private Integer viewSize = null;
  private String sortBy = null;

  @JsonProperty("time_zone")
  public String getClientName() {return clientName;}

  public void setClientName(String clientName) {
    this.clientName = clientName;
  }

  @JsonProperty("billing_status")
  public String getBillingStatus() {
    return billingStatus;
  }

  public void setBillingStatus(String billingStatus) {
    this.billingStatus = billingStatus;
  }

  @JsonProperty("start_index")
  public Integer getStartIndex() {
    return startIndex;
  }

  public void setStartIndex(Integer startIndex) {
    this.startIndex = startIndex;
  }

  @JsonProperty("view_size")
  public Integer getViewSize() {
    return viewSize;
  }

  public void setViewSize(Integer viewSize) {
    this.viewSize = viewSize;
  }

  @JsonProperty("sort_by")
  public String getSortBy() {
    return sortBy;
  }

  public void setSortBy(String sortBy) {
    this.sortBy = sortBy;
  }
}

