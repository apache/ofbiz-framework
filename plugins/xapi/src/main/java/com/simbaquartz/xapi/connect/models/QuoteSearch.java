package com.simbaquartz.xapi.connect.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.simbaquartz.xapi.connect.models.product.Product;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;


@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class QuoteSearch {

  @JsonProperty("id")
  private String id = null;

  @JsonProperty("sort_by")
  private String sortBy = null;

  @JsonProperty("name")
  private String name = null;

  @JsonProperty("supplier_id")
  private String supplierId = null;

  @JsonProperty("supplier_name")
  private String supplierName = null;

  @JsonProperty("customer")
  private Customer customer = null;

  @JsonProperty("products")
  private List<Product> products = null;

  @JsonProperty("solicitation_number")
  private String solicitationNumber = null;

  @JsonProperty("purchase_obligation_number")
  private String purchaseObligationNumber = null;

  @JsonProperty("requisition_number")
  private String requisitionNumber = null;

  @JsonProperty("order_number")
  private String orderNumber = null;

  @JsonProperty("purchase_order_number")
  private String purchaseOrderNumber = null;

  @JsonProperty("contract_reference_number")
  private String contractReferenceNumber = null;

  @JsonProperty("other_reference_number")
  private String otherReferenceNumber = null;

  @JsonProperty("form_2237_number")
  private String form2237Number = null;

  @JsonProperty("status_id")
  private List<String> statusId = null;

  @JsonProperty("type_id")
  private String typeId = null;

  @JsonProperty("billing_customer")
  private Customer billingCustomer = null;

  @JsonProperty("tag_id")
  private String tagId = null;

  @JsonProperty("tag_name")
  private String tagName = null;

  @JsonProperty("percentage_confidence")
  private String percentageConfidence  = null;

  @JsonProperty("end_of_warranty_service")
  private Timestamp endOfWarrantyService = null;

  @JsonProperty("party_id")
  private String partyId = null;

  @JsonProperty("start_of_warranty_service")
  private Timestamp startOfWarrantyService = null;

  @NotNull(message = "Please provide a Customer Id")
  @JsonProperty("customer_id")
  private String customerId = null;

  @JsonProperty("sales_rep_party_id")
  private String salesRepPartyId = null;

  @JsonProperty("total")
  private BigDecimal total = null;

  @JsonProperty("probability")
  private String probability = null;

  @JsonProperty("date_range_from")
  private String dateRangeFrom = null;

  @JsonProperty("date_range_to")
  private String dateRangeTo = null;

  @JsonProperty("quote_price_from")
  private BigDecimal quotePriceFrom = null;

  @JsonProperty("quote_price_to")
  private BigDecimal quotePriceTo = null;

  @JsonProperty("shipping_state")
  private String shippingState = null;

  @JsonProperty("include_quote_contact_details")
  private Boolean quoteContactsDetailsRequired = false;
}

