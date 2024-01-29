package com.simbaquartz.xapi.connect.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.simbaquartz.xapi.connect.models.product.Product;
import com.fidelissd.zcp.xcommon.models.company.Company;
import com.fidelissd.zcp.xcommon.models.people.Person;
import lombok.Data;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a Search Quote Response object.
 **/
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SearchQuoteResponse {

  @JsonProperty("id")
  private String id = null;

  @JsonProperty("name")
  private String name = null;

  @JsonProperty("solicitation_number")
  private String solicitationNumber = null;

  @JsonProperty("sub_total")
  private BigDecimal subTotal = null;

  @JsonProperty("total")
  private BigDecimal total = null;

  @JsonProperty("discount_price")
  private BigDecimal discountPrice = null;

  @JsonProperty("shipping_price")
  private BigDecimal shippingPrice = null;

  @JsonProperty("cost_total")
  private BigDecimal costTotal = null;

  @JsonProperty("revenue_percent")
  private BigDecimal revenuePercent = null;

  @JsonProperty("profit_total")
  private BigDecimal profitTotal = null;

  @JsonProperty("status_id")
  private String statusId = null;

  @JsonProperty("status")
  private String status = null;

  @JsonProperty("type_id")
  private String typeId = null;

  @JsonProperty("currency_uom_id")
  private String currencyUomId = null;

  @JsonProperty("issue_date")
  private Timestamp issueDate = null;

  @JsonProperty("valid_from_date")
  private Timestamp validFromDate = null;

  @JsonProperty("valid_thru_date")
  private Timestamp validThruDate = null;

  @JsonProperty("created_at")
  private Timestamp createdAt = null;

  @JsonProperty("updated_at")
  private Timestamp updatedAt = null;

  @JsonProperty("created_by")
  private Person createdBy = null;

  @JsonProperty("last_updated_by")
  private Person lastUpdatedBy = null;

  @JsonProperty("quote_quality_score")
  private long quoteQualityScore;

  @JsonProperty("SalesChannelEnumId")
  private String salesChannelEnumId = null;

  @JsonProperty("customer")
  private Company customer = null;

  @JsonProperty("vendor")
  private Company vendor = null;

  @JsonProperty("products")
  private List<Product> products = null;

  @JsonProperty("customer_contact")
  private List<Person> customerContact = new ArrayList<>();

  @JsonProperty("vendor_contact")
  private List<Person> vendorContact = new ArrayList<>();

  @JsonProperty("company_contact")
  private List<Person> companyContact = new ArrayList<>();

}

