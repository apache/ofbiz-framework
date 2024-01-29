package com.simbaquartz.xapi.connect.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fidelissd.zcp.xcommon.models.geo.PostalAddress;

import java.util.Objects;


@javax.annotation.Generated(value = "class io.swagger.codegen.languages.JavaResteasyServerCodegen", date = "2017-06-30T20:56:17.408-07:00")
public class Shipment   {
  
  private String id = null;
  private PostalAddress origin = null;
  private PostalAddress destination = null;
  private String items = null;

  /**
   * Unique identifier for the Shipment.
   **/
  
  @JsonProperty("id")
  public String getId() {
    return id;
  }
  public void setId(String id) {
    this.id = id;
  }

  /**
   **/
  
  @JsonProperty("origin")
  public PostalAddress getOrigin() {
    return origin;
  }
  public void setOrigin(PostalAddress origin) {
    this.origin = origin;
  }

  /**
   **/
  
  @JsonProperty("destination")
  public PostalAddress getDestination() {
    return destination;
  }
  public void setDestination(PostalAddress destination) {
    this.destination = destination;
  }

  /**
   * TODO--create items model and add reference here as an array.
   **/
  
  @JsonProperty("items")
  public String getItems() {
    return items;
  }
  public void setItems(String items) {
    this.items = items;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Shipment shipment = (Shipment) o;
    return Objects.equals(id, shipment.id) &&
        Objects.equals(origin, shipment.origin) &&
        Objects.equals(destination, shipment.destination) &&
        Objects.equals(items, shipment.items);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, origin, destination, items);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class Shipment {\n");
    
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    origin: ").append(toIndentedString(origin)).append("\n");
    sb.append("    destination: ").append(toIndentedString(destination)).append("\n");
    sb.append("    items: ").append(toIndentedString(items)).append("\n");
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

