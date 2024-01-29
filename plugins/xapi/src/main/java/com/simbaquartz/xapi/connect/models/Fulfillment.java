package com.simbaquartz.xapi.connect.models;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;


/**
 * Represents a Fulfilment object.
 **/

@javax.annotation.Generated(value = "class io.swagger.codegen.languages.JavaResteasyServerCodegen", date = "2017-07-07T13:00:22.061+05:30")
public class Fulfillment   {
  
  private String id = null;
  private String orderId = null;
  private String createdAt = null;
  private String status = null;
  private String trackingCompany = null;
  private String trackingNumber = null;
  private String updatedAt = null;

  /**
   * The unique numeric identifier for the fulfillment.
   **/
  
  @JsonProperty("id")
  public String getId() {
    return id;
  }
  public void setId(String id) {
    this.id = id;
  }

  /**
   * The unique numeric identifier for the order.
   **/
  
  @JsonProperty("order_id")
  public String getOrderId() {
    return orderId;
  }
  public void setOrderId(String orderId) {
    this.orderId = orderId;
  }

  /**
   * The date and time when the fulfillment was created.
   **/
  
  @JsonProperty("created_at")
  public String getCreatedAt() {
    return createdAt;
  }
  public void setCreatedAt(String createdAt) {
    this.createdAt = createdAt;
  }

  /**
   * The status of the fulfillment.
   **/
  
  @JsonProperty("status")
  public String getStatus() {
    return status;
  }
  public void setStatus(String status) {
    this.status = status;
  }

  /**
   * The name of the shipping company.
   **/
  
  @JsonProperty("tracking_company")
  public String getTrackingCompany() {
    return trackingCompany;
  }
  public void setTrackingCompany(String trackingCompany) {
    this.trackingCompany = trackingCompany;
  }

  /**
   * The shipping number, provided by the shipping company.
   **/
  
  @JsonProperty("tracking_number")
  public String getTrackingNumber() {
    return trackingNumber;
  }
  public void setTrackingNumber(String trackingNumber) {
    this.trackingNumber = trackingNumber;
  }

  /**
   * The date and time when the fulfillment was last modified.
   **/
  
  @JsonProperty("updated_at")
  public String getUpdatedAt() {
    return updatedAt;
  }
  public void setUpdatedAt(String updatedAt) {
    this.updatedAt = updatedAt;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Fulfillment fulfillment = (Fulfillment) o;
    return Objects.equals(id, fulfillment.id) &&
        Objects.equals(orderId, fulfillment.orderId) &&
        Objects.equals(createdAt, fulfillment.createdAt) &&
        Objects.equals(status, fulfillment.status) &&
        Objects.equals(trackingCompany, fulfillment.trackingCompany) &&
        Objects.equals(trackingNumber, fulfillment.trackingNumber) &&
        Objects.equals(updatedAt, fulfillment.updatedAt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, orderId, createdAt, status, trackingCompany, trackingNumber, updatedAt);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class Fulfillment {\n");
    
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    orderId: ").append(toIndentedString(orderId)).append("\n");
    sb.append("    createdAt: ").append(toIndentedString(createdAt)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    trackingCompany: ").append(toIndentedString(trackingCompany)).append("\n");
    sb.append("    trackingNumber: ").append(toIndentedString(trackingNumber)).append("\n");
    sb.append("    updatedAt: ").append(toIndentedString(updatedAt)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}

