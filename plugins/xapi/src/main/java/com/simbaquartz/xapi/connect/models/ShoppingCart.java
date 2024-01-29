package com.simbaquartz.xapi.connect.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fidelissd.zcp.xcommon.models.geo.PostalAddress;

import java.sql.Timestamp;
import java.util.Objects;


/**
 * Represents a Shooping Cart object.
 **/

@javax.annotation.Generated(value = "class io.swagger.codegen.languages.JavaResteasyServerCodegen", date = "2017-08-08T14:31:04.908+05:30")
public class ShoppingCart   {
  
  private String id = null;
  private String currency = null;
  private String customerId = null;
  private String email = null;
  private String phone = null;
  private PostalAddress shippingAddress = null;
  private String subtotalPrice = null;
  private String token = null;
  private String totalPrice = null;
  private String totalTax = null;
  private Timestamp createdAt = null;
  private Timestamp updatedAt = null;
  private String createdBy = null;
  private String lastUpdatedBy = null;

  /**
   * Unique identifier representing a specific id for the cart.
   **/
  
  @JsonProperty("id")
  public String getId() {
    return id;
  }
  public void setId(String id) {
    this.id = id;
  }

  /**
   * The three letter code for the currency used for the payment.
   **/
  
  @JsonProperty("currency")
  public String getCurrency() {
    return currency;
  }
  public void setCurrency(String currency) {
    this.currency = currency;
  }

  /**
   * The id of the customer associated with this cart.
   **/
  
  @JsonProperty("customer_id")
  public String getCustomerId() {
    return customerId;
  }
  public void setCustomerId(String customerId) {
    this.customerId = customerId;
  }

  /**
   * The customer's email address.
   **/
  
  @JsonProperty("email")
  public String getEmail() {
    return email;
  }
  public void setEmail(String email) {
    this.email = email;
  }

  /**
   * The customer's phone number.
   **/
  
  @JsonProperty("phone")
  public String getPhone() {
    return phone;
  }
  public void setPhone(String phone) {
    this.phone = phone;
  }

  /**
   **/
  
  @JsonProperty("shipping_address")
  public PostalAddress getShippingAddress() {
    return shippingAddress;
  }
  public void setShippingAddress(PostalAddress shippingAddress) {
    this.shippingAddress = shippingAddress;
  }

  /**
   * Price of the checkout before shipping and taxes
   **/
  
  @JsonProperty("subtotal_price")
  public String getSubtotalPrice() {
    return subtotalPrice;
  }
  public void setSubtotalPrice(String subtotalPrice) {
    this.subtotalPrice = subtotalPrice;
  }

  /**
   * Unique identifier for a particular checkout.
   **/
  
  @JsonProperty("token")
  public String getToken() {
    return token;
  }
  public void setToken(String token) {
    this.token = token;
  }

  /**
   * The sum of all the prices of all the items in the checkout,taxes and discounts included.
   **/
  
  @JsonProperty("total_price")
  public String getTotalPrice() {
    return totalPrice;
  }
  public void setTotalPrice(String totalPrice) {
    this.totalPrice = totalPrice;
  }

  /**
   * The sum of all the taxes applied to the line items in the checkout.
   **/
  
  @JsonProperty("total_tax")
  public String getTotalTax() {
    return totalTax;
  }
  public void setTotalTax(String totalTax) {
    this.totalTax = totalTax;
  }

  /**
   * The date and time when the cart was created.
   **/
  
  @JsonProperty("created_at")
  public Timestamp getCreatedAt() {
    return createdAt;
  }
  public void setCreatedAt(Timestamp createdAt) {
    this.createdAt = createdAt;
  }

  /**
   * The date and time when the checkout was last modified.
   **/
  
  @JsonProperty("updated_at")
  public Timestamp getUpdatedAt() {
    return updatedAt;
  }
  public void setUpdatedAt(Timestamp updatedAt) {
    this.updatedAt = updatedAt;
  }

  /**
   * The name who create the cart.
   **/
  
  @JsonProperty("created_by")
  public String getCreatedBy() {
    return createdBy;
  }
  public void setCreatedBy(String createdBy) {
    this.createdBy = createdBy;
  }

  /**
   * The name who last update the cart.
   **/
  
  @JsonProperty("last_updated_by")
  public String getLastUpdatedBy() {
    return lastUpdatedBy;
  }
  public void setLastUpdatedBy(String lastUpdatedBy) {
    this.lastUpdatedBy = lastUpdatedBy;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ShoppingCart shoppingCart = (ShoppingCart) o;
    return Objects.equals(id, shoppingCart.id) &&
        Objects.equals(currency, shoppingCart.currency) &&
        Objects.equals(customerId, shoppingCart.customerId) &&
        Objects.equals(email, shoppingCart.email) &&
        Objects.equals(phone, shoppingCart.phone) &&
        Objects.equals(shippingAddress, shoppingCart.shippingAddress) &&
        Objects.equals(subtotalPrice, shoppingCart.subtotalPrice) &&
        Objects.equals(token, shoppingCart.token) &&
        Objects.equals(totalPrice, shoppingCart.totalPrice) &&
        Objects.equals(totalTax, shoppingCart.totalTax) &&
        Objects.equals(createdAt, shoppingCart.createdAt) &&
        Objects.equals(updatedAt, shoppingCart.updatedAt) &&
        Objects.equals(createdBy, shoppingCart.createdBy) &&
        Objects.equals(lastUpdatedBy, shoppingCart.lastUpdatedBy);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, currency, customerId, email, phone, shippingAddress, subtotalPrice, token, totalPrice, totalTax, createdAt, updatedAt, createdBy, lastUpdatedBy);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ShoppingCart {\n");
    
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    currency: ").append(toIndentedString(currency)).append("\n");
    sb.append("    customerId: ").append(toIndentedString(customerId)).append("\n");
    sb.append("    email: ").append(toIndentedString(email)).append("\n");
    sb.append("    phone: ").append(toIndentedString(phone)).append("\n");
    sb.append("    shippingAddress: ").append(toIndentedString(shippingAddress)).append("\n");
    sb.append("    subtotalPrice: ").append(toIndentedString(subtotalPrice)).append("\n");
    sb.append("    token: ").append(toIndentedString(token)).append("\n");
    sb.append("    totalPrice: ").append(toIndentedString(totalPrice)).append("\n");
    sb.append("    totalTax: ").append(toIndentedString(totalTax)).append("\n");
    sb.append("    createdAt: ").append(toIndentedString(createdAt)).append("\n");
    sb.append("    updatedAt: ").append(toIndentedString(updatedAt)).append("\n");
    sb.append("    createdBy: ").append(toIndentedString(createdBy)).append("\n");
    sb.append("    lastUpdatedBy: ").append(toIndentedString(lastUpdatedBy)).append("\n");
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

