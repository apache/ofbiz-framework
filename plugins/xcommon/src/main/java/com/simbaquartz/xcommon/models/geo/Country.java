package com.simbaquartz.xcommon.models.geo;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Objects;


/**
 * Represents a Country object.
 **/

@javax.annotation.Generated(value = "class io.swagger.codegen.languages.JavaResteasyServerCodegen", date = "2017-07-06T12:32:14.657+05:30")
public class Country   {
  
  private String id = null;
  private String code = null;
  private String name = null;
  private List<Province> provinces = null;
  private String tax = null;

  /**
   * The unique numeric identifier for the country.
   **/
  
  @JsonProperty("id")
  public String getId() {
    return id;
  }
  public void setId(String id) {
    this.id = id;
  }

  /**
   * The ISO 3166-1 alpha-2 two-letter country code for the country.
   **/
  
  @JsonProperty("code")
  public String getCode() {
    return code;
  }
  public void setCode(String code) {
    this.code = code;
  }

  /**
   * The full name of the country, in English.
   **/
  
  @JsonProperty("name")
  public String getName() {
    return name;
  }
  public void setName(String name) {
    this.name = name;
  }

  /**
   **/

  @JsonProperty("provinces")
  public List<Province> getProvinces() {
    return provinces;
  }
  public void setProvinces(List<Province> provinces) {
    this.provinces = provinces;
  }


  /**
   * The national sales tax rate to be applied to orders made by customers from that country.
   **/
  
  @JsonProperty("tax")
  public String getTax() {
    return tax;
  }
  public void setTax(String tax) {
    this.tax = tax;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Country country = (Country) o;
    return Objects.equals(id, country.id) &&
        Objects.equals(code, country.code) &&
        Objects.equals(name, country.name) &&
        Objects.equals(provinces, country.provinces) &&
        Objects.equals(tax, country.tax);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, code, name, provinces, tax);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class Country {\n");
    
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    code: ").append(toIndentedString(code)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    provinces: ").append(toIndentedString(provinces)).append("\n");
    sb.append("    tax: ").append(toIndentedString(tax)).append("\n");
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

