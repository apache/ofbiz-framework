package com.simbaquartz.xapi.connect.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.simbaquartz.xapi.connect.models.note.Note;
import com.simbaquartz.xapi.connect.models.product.Product;
import com.fidelissd.zcp.xcommon.models.company.Company;
import com.fidelissd.zcp.xcommon.models.geo.PostalAddress;
import com.fidelissd.zcp.xcommon.models.people.Person;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 * Represents a Get Quote Response object.
 **/
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GetQuoteResponse {

  @JsonProperty("id")
  private String id = null;

  @JsonProperty("name")
  private String name = null;

  @JsonProperty("solicitation_number")
  private String solicitationNumber = null;

  @JsonProperty("sub_total")
  private BigDecimal subTotal = BigDecimal.ZERO;

  @JsonProperty("total")
  private BigDecimal total = BigDecimal.ZERO;

  @JsonProperty("discount_price")
  private BigDecimal discountPrice = BigDecimal.ZERO;

  @JsonProperty("shipping_price")
  private BigDecimal shippingPrice = BigDecimal.ZERO;

  @JsonProperty("cost_total")
  private BigDecimal costTotal = BigDecimal.ZERO;

  @JsonProperty("revenue_percent")
  private BigDecimal revenuePercent = BigDecimal.ZERO;

  @JsonProperty("profit_total")
  private BigDecimal profitTotal = BigDecimal.ZERO;

  @JsonProperty("status_id")
  private String statusId = null;

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

  @JsonProperty("quote_status_name")
  private String quoteStatusName = null;

  @JsonProperty("quote_type_name")
  private String quoteTypeName = null;

  @JsonProperty("notes")
  private List<Note> notes = new ArrayList<>();

  @JsonProperty("quote_tags")
  private List<QuoteTag> quoteTags = new ArrayList<>();

  @JsonProperty("created_at")
  private Timestamp createdAt = null;

  @JsonProperty("updated_at")
  private Timestamp updatedAt = null;

  @JsonProperty("created_by")
  private Person createdBy = null;

  @JsonProperty("last_updated_by")
  private Person lastUpdatedBy = null;

  @JsonProperty("form_2237_number")
  private String form2237Number = null;

  @JsonProperty("quote_customer_shipping_addresses")
  private List<PostalAddress> quoteCustomerShippingAddresses = null;

  @JsonProperty("quote_vendor_shipping_addresses")
  private List<PostalAddress> quoteVendorShippingAddresses = null;

  @JsonProperty("is_treat_adjustment_as_line_item")
  private boolean treatAdjustmentsAsLineItem = false;


}

