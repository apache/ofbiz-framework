package com.fidelissd.zcp.xcommon.models.geo;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;


/**
 * Represents a Provinces object.
 **/

@javax.annotation.Generated(value = "class io.swagger.codegen.languages.JavaResteasyServerCodegen", date = "2017-07-06T12:32:14.657+05:30")
public class Province {
  
  private String id = null;
  private String code = null;
  private String name = null;
  private String countryId = null;
  private String tax = null;
  private String taxName = null;
  private String taxType = null;
  private String taxPercentage = null;

  /**
   * The unique numeric identifier for the particular province or state.
   **/
  
  @JsonProperty("id")
  public String getId() {
    return id;
  }
  public void setId(String id) {
    this.id = id;
  }

  /**
   * The two letter province or state code.
   **/
  
  @JsonProperty("code")
  public String getCode() {
    return code;
  }
  public void setCode(String code) {
    this.code = code;
  }

  /**
   * The name of the province or state.
   **/
  
  @JsonProperty("name")
  public String getName() {
    return name;
  }
  public void setName(String name) {
    this.name = name;
  }

  /**
   * The unique numeric identifier for the country.
   **/
  
  @JsonProperty("country_id")
  public String getCountryId() {
    return countryId;
  }
  public void setCountryId(String countryId) {
    this.countryId = countryId;
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

  /**
   * The name of the tax as it is referred to in the applicable province/state.
   **/
  
  @JsonProperty("tax_name")
  public String getTaxName() {
    return taxName;
  }
  public void setTaxName(String taxName) {
    this.taxName = taxName;
  }

  /**
   * A tax_type is applied for a compounded sales tax.
   **/
  
  @JsonProperty("tax_type")
  public String getTaxType() {
    return taxType;
  }
  public void setTaxType(String taxType) {
    this.taxType = taxType;
  }

  /**
   * The tax value in percent format.
   **/
  
  @JsonProperty("tax_percentage")
  public String getTaxPercentage() {
    return taxPercentage;
  }
  public void setTaxPercentage(String taxPercentage) {
    this.taxPercentage = taxPercentage;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Province province = (Province) o;
    return Objects.equals(id, province.id) &&
        Objects.equals(code, province.code) &&
        Objects.equals(name, province.name) &&
        Objects.equals(countryId, province.countryId) &&
        Objects.equals(tax, province.tax) &&
        Objects.equals(taxName, province.taxName) &&
        Objects.equals(taxType, province.taxType) &&
        Objects.equals(taxPercentage, province.taxPercentage);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, code, name, countryId, tax, taxName, taxType, taxPercentage);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class Province {\n");
    
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    code: ").append(toIndentedString(code)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    countryId: ").append(toIndentedString(countryId)).append("\n");
    sb.append("    tax: ").append(toIndentedString(tax)).append("\n");
    sb.append("    taxName: ").append(toIndentedString(taxName)).append("\n");
    sb.append("    taxType: ").append(toIndentedString(taxType)).append("\n");
    sb.append("    taxPercentage: ").append(toIndentedString(taxPercentage)).append("\n");
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

