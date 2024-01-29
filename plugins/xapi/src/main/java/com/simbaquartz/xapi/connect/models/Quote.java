package com.simbaquartz.xapi.connect.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fidelissd.zcp.xcommon.models.geo.PostalAddress;
import com.simbaquartz.xapi.connect.models.product.Product;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;



/**
 * Represents a Quote object.
 **/

@javax.annotation.Generated(value = "class io.swagger.codegen.languages.JavaResteasyServerCodegen", date = "2018-05-11T11:44:45.432+05:30")
public class Quote   {

  private String id = null;
  private String name = null;
  private String supplierId = null;
  private String supplierName = null;
  private PostalAddress billingAddress = null;
  private PostalAddress shippingAddress = null;
  private Customer customer = null;
  private List<Product> products = null;
  private Timestamp awardDate = null;
  private String solicitationNumber = null;
  private String purchaseObligationNumber = null;
  private String requisitionNumber = null;
  private String orderNumber = null;
  private String purchaseOrderNumber = null;
  private String contractReferenceNumber = null;
  private String otherReferenceNumber = null;
  private String invoicePerShipment = null;
  private String completeOrderFlag = null;
  private BigDecimal subTotal = null;
  private BigDecimal total = null;
  private BigDecimal discountPrice = null;
  private BigDecimal shippingPrice = null;
  private BigDecimal costTotol = null;
  private BigDecimal revenuePercent = null;
  private BigDecimal profitTotal = null;
  private String statusId = null;
  private String typeId = null;
  private String currencyUomId = null;
  private List<List<QuoteRole>> quoteRoles = new ArrayList<>();
  private Customer billingCustomer = null;
  private String quality = null;
  private Timestamp issueDate = null;
  private Timestamp validFromDate = null;
  private Timestamp validThruDate = null;
  private String createdAt = null;
  private String salesChannelEnumId = null;
  private String updatedAt = null;
  private String createdBy = null;
  private String lastUpdatedBy = null;
  private String statusChangeReason = null;
  private String tagId = null;
  private String tagName = null;
  private String tagColorCode = null;
  private String estimatedAwardMonth  = null;
  private String estimatedAwardQuarter  = null;
  private String estimatedAwardYear  = null;
  private String percentageConfidence  = null;
  private String salesPipelineDataNotes  = null;
  private String attrName  = null;
  private String attrValue  = null;
  private String awardServiceTracked = null;
  private String defaultTermTypeId = null;
  private String description = null;
  private Timestamp endOfWarrantyService = null;
  private String partyId = null;
  private String productStoreId = null;
  private long quoteQualityScore;
  private Timestamp startOfWarrantyService = null;
  private boolean treatAdjustmentsAsLineItem;

  private String projectId = null;


  /**
   * Unique identifier representing a specific quote id for the quote.
   **/

  @JsonProperty("id")
  public String getId() {
    return id;
  }
  public void setId(String id) {
    this.id = id;
  }

  /**
   * The quote name associated with this quote.
   **/

  @JsonProperty("name")
  public String getName() {
    return name;
  }
  public void setName(String name) {
    this.name = name;
  }

  /**
   * The quote salesChannelEnumId associated with this quote.
   **/

  @JsonProperty("SalesChannelEnumId")
  public String getSalesChannelEnumId() {
    return salesChannelEnumId;
  }
  public void setSalesChannelEnumId(String salesChannelEnumId) {
    this.salesChannelEnumId = salesChannelEnumId;
  }

  /**
   * The id of the supplier associated with this quote.
   **/

  @JsonProperty("supplier_id")
  public String getSupplierId() {
    return supplierId;
  }
  public void setSupplierId(String supplierId) {
    this.supplierId = supplierId;
  }

  /**
   * The supplier name associated with this quote.
   **/

  @JsonProperty("supplier_name")
  public String getSupplierName() {
    return supplierName;
  }
  public void setSupplierName(String supplierName) {
    this.supplierName = supplierName;
  }

  /**
   **/

  @JsonProperty("billing_address")
  public PostalAddress getBillingAddress() {
    return billingAddress;
  }
  public void setBillingAddress(PostalAddress billingAddress) {
    this.billingAddress = billingAddress;
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
   **/

  @JsonProperty("customer")
  public Customer getCustomer() {
    return customer;
  }
  public void setCustomer(Customer customer) {
    this.customer = customer;
  }

  /**
   **/

  @JsonProperty("products")
  public List<Product> getProduct() {
    return products;
  }
  public void setProduct(List<Product> products) {
    this.products = products;
  }

  /**
   * The solicitation number associated with this quote.
   **/

  @JsonProperty("solicitation_number")
  public String getSolicitationNumber() {
    return solicitationNumber;
  }
  public void setSolicitationNumber(String solicitationNumber) {
    this.solicitationNumber = solicitationNumber;
  }

  /**
   * The purchaseObligationNumber number associated with this quote.
   **/

  @JsonProperty("purchase_obligation_number")
  public String getPurchaseObligationNumber() {
    return purchaseObligationNumber;
  }
  public void setPurchaseObligationNumber(String purchaseObligationNumber) {
    this.purchaseObligationNumber = purchaseObligationNumber;
  }

  /**
   * The requisitionNumber number associated with this quote.
   **/

  @JsonProperty("requisition_number")
  public String getRequisitionNumber() {
    return requisitionNumber;
  }
  public void setRequisitionNumber(String requisitionNumber) {
    this.requisitionNumber = requisitionNumber;
  }

  /**
   * The orderNumber number associated with this quote.
   **/

  @JsonProperty("order_number")
  public String getOrderNumber() {
    return orderNumber;
  }
  public void setOrderNumber(String orderNumber) {
    this.orderNumber = orderNumber;
  }

  /**
   * The purchaseOrderNumber number associated with this quote.
   **/

  @JsonProperty("purchase_order_number")
  public String getPurchaseOrderNumber() {
    return purchaseOrderNumber;
  }
  public void setPurchaseOrderNumber(String purchaseOrderNumber) {
    this.purchaseOrderNumber = purchaseOrderNumber;
  }

  /**
   * The contractReferenceNumber number associated with this quote.
   **/

  @JsonProperty("contract_reference_number")
  public String getContractReferenceNumber() {
    return contractReferenceNumber;
  }
  public void setContractReferenceNumber(String contractReferenceNumber) {
    this.contractReferenceNumber = contractReferenceNumber;
  }

  /**
   * The sub total of the quote. E.g. 192.00
   **/

  @JsonProperty("sub_total")
  public BigDecimal getSubTotal() {
    return subTotal;
  }
  public void setSubTotal(BigDecimal subTotal) {
    this.subTotal = subTotal;
  }

  /**
   * The total price of the quote. E.g. 192.00
   **/

  @JsonProperty("total")
  public BigDecimal getTotal() {
    return total;
  }
  public void setTotal(BigDecimal total) {
    this.total = total;
  }

  /**
   * The total amount of the discounts to be applied to the price of the quote.
   **/

  @JsonProperty("discount_price")
  public BigDecimal getDiscountPrice() {
    return discountPrice;
  }
  public void setDiscountPrice(BigDecimal discountPrice) {
    this.discountPrice = discountPrice;
  }

  /**
   * The price of the shipping for the quote. E.g. 192.00
   **/

  @JsonProperty("shipping_price")
  public BigDecimal getShippingPrice() {
    return shippingPrice;
  }
  public void setShippingPrice(BigDecimal shippingPrice) {
    this.shippingPrice = shippingPrice;
  }

  /**
   * Total cost of the quote. E.g. 192.00
   **/

  @JsonProperty("cost_totol")
  public BigDecimal getCostTotol() {
    return costTotol;
  }
  public void setCostTotol(BigDecimal costTotol) {
    this.costTotol = costTotol;
  }

  /**
   * Profit % for the quote. E.g. 192.00
   **/

  @JsonProperty("revenue_percent")
  public BigDecimal getRevenuePercent() {
    return revenuePercent;
  }
  public void setRevenuePercent(BigDecimal revenuePercent) {
    this.revenuePercent = revenuePercent;
  }

  /**
   * Profit total price for the quote. E.g. 192.00
   **/

  @JsonProperty("profit_total")
  public BigDecimal getProfitTotal() {
    return profitTotal;
  }
  public void setProfitTotal(BigDecimal profitTotal) {
    this.profitTotal = profitTotal;
  }

  /**
   * Status of the quote.
   **/

  @JsonProperty("status_id")
  public String getStatusId() {
    return statusId;
  }
  public void setStatusId(String statusId) {
    this.statusId = statusId;
  }

  /**
   * Type of the quote.
   **/

  @JsonProperty("type_id")
  public String getTypeId() {
    return typeId;
  }
  public void setTypeId(String typeId) {
    this.typeId = typeId;
  }

  /**
   * Displays default Currency UOM id set for the quote. For example, USD for US dollars.
   **/

  @JsonProperty("currency_uom_id")
  public String getCurrencyUomId() {
    return currencyUomId;
  }
  public void setCurrencyUomId(String currencyUomId) {
    this.currencyUomId = currencyUomId;
  }

  /**
   **/

  @JsonProperty("quote_roles")
  public List<List<QuoteRole>> getQuoteRoles() {
    return quoteRoles;
  }
  public void setQuoteRoles(List<List<QuoteRole>> quoteRoles) {
    this.quoteRoles = quoteRoles;
  }

  /**
   **/

  @JsonProperty("billing_customer")
  public Customer getBillingCustomer() {
    return billingCustomer;
  }
  public void setBillingCustomer(Customer billingCustomer) {
    this.billingCustomer = billingCustomer;
  }

  /**
   * Quote quality out of 100.
   **/

  @JsonProperty("quality")
  public String getQuality() {
    return quality;
  }
  public void setQuality(String quality) {
    this.quality = quality;
  }

  /**
   * The date and time when the quote was created.
   **/

  @JsonProperty("issue_date")
  public Timestamp getIssueDate() {
    return issueDate;
  }
  public void setIssueDate(Timestamp issueDate) {
    this.issueDate = issueDate;
  }

  /**
   * The date and time when the quote was awarded.
   **/

  @JsonProperty("award_date")
  public Timestamp getAwardDate() {
    return awardDate;
  }
  public void setAwardDate(Timestamp awardDate) {
    this.awardDate = awardDate;
  }

  /**
   * The quote valid from date and time.
   **/

  @JsonProperty("valid_from_date")
  public Timestamp getValidFromDate() {
    return validFromDate;
  }
  public void setValidFromDate(Timestamp validFromDate) {
    this.validFromDate = validFromDate;
  }

  /**
   * The quote valid thru date and time.
   **/

  @JsonProperty("valid_thru_date")
  public Timestamp getValidThruDate() {
    return validThruDate;
  }
  public void setValidThruDate(Timestamp validThruDate) {
    this.validThruDate = validThruDate;
  }

  /**
   * The date and time when the quote was created.
   **/

  @JsonProperty("created_at")
  public String getCreatedAt() {
    return createdAt;
  }
  public void setCreatedAt(String createdAt) {
    this.createdAt = createdAt;
  }

  /**
   * The date and time when the quote was last modified.
   **/

  @JsonProperty("updated_at")
  public String getUpdatedAt() {
    return updatedAt;
  }
  public void setUpdatedAt(String updatedAt) {
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

  @JsonProperty("status_change_reason")
  public String getStatusChangeReason() {
    return statusChangeReason;
  }

  public void setStatusChangeReason(String statusChangeReason) {
    this.statusChangeReason = statusChangeReason;
  }

  @JsonProperty("tag_name")
  public String getTagName() {
    return tagName;
  }

  public void setTagName(String tagName) {
    this.tagName = tagName;
  }

  @JsonProperty("tag_color_code")
  public String getTagColorCode() {
    return tagColorCode;
  }



  public void setTagColorCode(String tagColorCode) {
    this.tagColorCode = tagColorCode;
  }

  @JsonProperty("estimated_award_month")
    public String getEstimatedAwardMonth() {
      return estimatedAwardMonth;
    }

  public void setEstimatedAwardMonth(String estimatedAwardMonth) {
    this.estimatedAwardMonth = estimatedAwardMonth;
  }

  @JsonProperty("estimated_award_quarter")
  public String getEstimatedAwardQuarter() {
    return estimatedAwardQuarter;
  }

  public void setEstimatedAwardQuarter(String estimatedAwardQuarter) {
    this.estimatedAwardQuarter = estimatedAwardQuarter;
  }
  @JsonProperty("estimated_award_year")
  public String getEstimatedAwardYear() {
    return estimatedAwardYear;
  }

  public void setEstimatedAwardYear(String estimatedAwardYear) {
    this.estimatedAwardYear = estimatedAwardYear;
  }

  @JsonProperty("percentage_confidence")
  public String getPercentageConfidence() {
    return percentageConfidence;
  }

  public void setPercentageConfidence(String percentageConfidence) {
    this.percentageConfidence = percentageConfidence;
  }

  @JsonProperty("sales_pipeline_data_notes")
  public String getSalesPipelineDataNotes() {
    return salesPipelineDataNotes;
  }

  public void setSalesPipelineDataNotes(String salesPipelineDataNotes) {
    this.salesPipelineDataNotes = salesPipelineDataNotes;
  }
  @JsonProperty("attr_name")
  public String getAttrName() {
    return attrName;
  }

  public void setAttrName(String attrName) {
    this.attrName = attrName;
  }
  @JsonProperty("attr_value")
  public String getAttrValue() {
    return attrValue;
  }

  public void setAttrValue(String attrValue) {
    this.attrValue = attrValue;

  }

  @JsonProperty("award_service_tracked")
  public String getAwardServiceTracked() {
    return awardServiceTracked;
  }
  public void setAwardServiceTracked(String awardServiceTracked) {
    this.awardServiceTracked = awardServiceTracked;
  }

  @JsonProperty("default_term_type_id")
  public String getDefaultTermTypeId() {
    return defaultTermTypeId;
  }
  public void setDefaultTermTypeId(String defaultTermTypeId) {
    this.defaultTermTypeId = defaultTermTypeId;
  }

  @JsonProperty("description")
  public String getDescription() {
    return description;
  }
  public void setDescription(String description) {
    this.description = description;
  }

  @JsonProperty("end_of_warranty_service")
  public Timestamp getEndOfWarrantyService() {
    return endOfWarrantyService;
  }
  public void setEndOfWarrantyService(Timestamp endOfWarrantyService) {
    this.endOfWarrantyService = endOfWarrantyService;
  }

  @JsonProperty("other_reference_number")
  public String getOtherReferenceNumber() {
    return otherReferenceNumber;
  }
  public void setOtherReferenceNumber(String otherReferenceNumber) {
    this.otherReferenceNumber = otherReferenceNumber;
  }

  @JsonProperty("invoice_per_shipment")
  public String getInvoicePerShipment() {
    return invoicePerShipment;
  }
  public void setInvoicePerShipment(String invoicePerShipment) {
    this.invoicePerShipment = invoicePerShipment;
  }

  @JsonProperty("complete_order_flag")
  public String getCompleteOrderFlag() {
    return completeOrderFlag;
  }
  public void setCompleteOrderFlag(String completeOrderFlag) {
    this.completeOrderFlag = completeOrderFlag;
  }

  @JsonProperty("party_id")
  public String getPartyId() {
    return partyId;
  }
  public void setPartyId(String partyId) {
    this.partyId = partyId;
  }

  @JsonProperty("product_store_id")
  public String getProductStoreId() {
    return productStoreId;
  }
  public void setProductStoreId(String productStoreId) {
    this.productStoreId = productStoreId;
  }

  @JsonProperty("quote_quality_score")
  public long getQuoteQualityScore() {
    return quoteQualityScore;
  }
  public void setQuoteQualityScore(long quoteQualityScore) {
    this.quoteQualityScore = quoteQualityScore;
  }

  @JsonProperty("start_of_warranty_service")
  public Timestamp getStartOfWarrantyService() {
    return startOfWarrantyService;
  }
  public void setStartOfWarrantyService(Timestamp startOfWarrantyService) {
    this.startOfWarrantyService = startOfWarrantyService;
  }

  @JsonProperty("treat_adjustment_as_line_item")
  public boolean getTreatAdjustmentsAsLineItem() {
    return treatAdjustmentsAsLineItem;
  }
  public void setTreatAdjustmentsAsLineItem(boolean treatAdjustmentsAsLineItem) {
    this.treatAdjustmentsAsLineItem = treatAdjustmentsAsLineItem;
  }

  @JsonProperty("tag_id")
  public String getTagId() {
    return tagId;
  }
  public void setTagId(String tagId) {
    this.tagId = tagId;
  }

  @JsonProperty("project_id")
  public String getProjectId() {
    return projectId;
  }

  public void setProjectId(String projectId) {
    this.projectId = projectId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Quote quote = (Quote) o;
    return Objects.equals(id, quote.id) &&
        Objects.equals(name, quote.name) &&
        Objects.equals(supplierId, quote.supplierId) &&
        Objects.equals(supplierName, quote.supplierName) &&
        Objects.equals(billingAddress, quote.billingAddress) &&
        Objects.equals(shippingAddress, quote.shippingAddress) &&
        Objects.equals(customer, quote.customer) &&
        Objects.equals(solicitationNumber, quote.solicitationNumber) &&
        Objects.equals(subTotal, quote.subTotal) &&
        Objects.equals(total, quote.total) &&
        Objects.equals(discountPrice, quote.discountPrice) &&
        Objects.equals(shippingPrice, quote.shippingPrice) &&
        Objects.equals(costTotol, quote.costTotol) &&
        Objects.equals(revenuePercent, quote.revenuePercent) &&
        Objects.equals(profitTotal, quote.profitTotal) &&
        Objects.equals(statusId, quote.statusId) &&
        Objects.equals(typeId, quote.typeId) &&
        Objects.equals(currencyUomId, quote.currencyUomId) &&
        Objects.equals(quoteRoles, quote.quoteRoles) &&
        Objects.equals(billingCustomer, quote.billingCustomer) &&
        Objects.equals(quality, quote.quality) &&
        Objects.equals(issueDate, quote.issueDate) &&
        Objects.equals(validFromDate, quote.validFromDate) &&
        Objects.equals(validThruDate, quote.validThruDate) &&
        Objects.equals(createdAt, quote.createdAt) &&
        Objects.equals(updatedAt, quote.updatedAt) &&
        Objects.equals(createdBy, quote.createdBy) &&
        Objects.equals(lastUpdatedBy, quote.lastUpdatedBy)&&
        Objects.equals(tagId, quote.tagId)&&
        Objects.equals(tagName, quote.tagName)&&
        Objects.equals(tagColorCode, quote.tagColorCode)&&
        Objects.equals(statusChangeReason, quote.statusChangeReason)&&
            Objects.equals(name, quote.name) &&
            Objects.equals(supplierId, quote.supplierId) &&
            Objects.equals(supplierName, quote.supplierName) &&
            Objects.equals(billingAddress, quote.billingAddress) &&
            Objects.equals(shippingAddress, quote.shippingAddress) &&
            Objects.equals(customer, quote.customer) &&
            Objects.equals(solicitationNumber, quote.solicitationNumber) &&
            Objects.equals(subTotal, quote.subTotal) &&
            Objects.equals(total, quote.total) &&
            Objects.equals(discountPrice, quote.discountPrice) &&
            Objects.equals(shippingPrice, quote.shippingPrice) &&
            Objects.equals(costTotol, quote.costTotol) &&
            Objects.equals(revenuePercent, quote.revenuePercent) &&
            Objects.equals(profitTotal, quote.profitTotal) &&
            Objects.equals(statusId, quote.statusId) &&
            Objects.equals(typeId, quote.typeId) &&
            Objects.equals(currencyUomId, quote.currencyUomId) &&
            Objects.equals(quoteRoles, quote.quoteRoles) &&
            Objects.equals(billingCustomer, quote.billingCustomer) &&
            Objects.equals(quality, quote.quality) &&
            Objects.equals(issueDate, quote.issueDate) &&
            Objects.equals(validFromDate, quote.validFromDate) &&
            Objects.equals(validThruDate, quote.validThruDate) &&
            Objects.equals(createdAt, quote.createdAt) &&
            Objects.equals(updatedAt, quote.updatedAt) &&
            Objects.equals(createdBy, quote.createdBy) &&
            Objects.equals(lastUpdatedBy, quote.lastUpdatedBy) &&
            Objects.equals(awardServiceTracked, quote.awardServiceTracked) &&
            Objects.equals(defaultTermTypeId, quote.defaultTermTypeId) &&
            Objects.equals(description, quote.description) &&
            Objects.equals(endOfWarrantyService, quote.endOfWarrantyService) &&
            Objects.equals(otherReferenceNumber, quote.otherReferenceNumber) &&
            Objects.equals(partyId, quote.partyId) &&
            Objects.equals(productStoreId, quote.productStoreId) &&
            Objects.equals(quoteQualityScore, quote.quoteQualityScore) &&
            Objects.equals(startOfWarrantyService, quote.startOfWarrantyService) &&
            Objects.equals(treatAdjustmentsAsLineItem, quote.treatAdjustmentsAsLineItem) &&
            Objects.equals(statusChangeReason, quote.statusChangeReason)&&
        Objects.equals(estimatedAwardMonth , quote.estimatedAwardMonth )&&
        Objects.equals(estimatedAwardQuarter , quote.estimatedAwardQuarter )&&
        Objects.equals(estimatedAwardYear , quote.estimatedAwardYear )&&
        Objects.equals(percentageConfidence , quote.percentageConfidence )&&
        Objects.equals(lastUpdatedBy, quote.lastUpdatedBy)&&
        Objects.equals(attrName, quote.attrName)&&
        Objects.equals(attrValue, quote.attrValue)&&
        Objects.equals(salesPipelineDataNotes , quote.salesPipelineDataNotes )&&
        Objects.equals(name, quote.name) &&
        Objects.equals(supplierId, quote.supplierId) &&
        Objects.equals(supplierName, quote.supplierName) &&
        Objects.equals(billingAddress, quote.billingAddress) &&
        Objects.equals(shippingAddress, quote.shippingAddress) &&
        Objects.equals(customer, quote.customer) &&
        Objects.equals(solicitationNumber, quote.solicitationNumber) &&
        Objects.equals(subTotal, quote.subTotal) &&
        Objects.equals(total, quote.total) &&
        Objects.equals(discountPrice, quote.discountPrice) &&
        Objects.equals(shippingPrice, quote.shippingPrice) &&
        Objects.equals(costTotol, quote.costTotol) &&
        Objects.equals(revenuePercent, quote.revenuePercent) &&
        Objects.equals(profitTotal, quote.profitTotal) &&
        Objects.equals(statusId, quote.statusId) &&
        Objects.equals(typeId, quote.typeId) &&
        Objects.equals(currencyUomId, quote.currencyUomId) &&
        Objects.equals(quoteRoles, quote.quoteRoles) &&
        Objects.equals(billingCustomer, quote.billingCustomer) &&
        Objects.equals(quality, quote.quality) &&
        Objects.equals(issueDate, quote.issueDate) &&
        Objects.equals(validFromDate, quote.validFromDate) &&
        Objects.equals(validThruDate, quote.validThruDate) &&
        Objects.equals(createdAt, quote.createdAt) &&
        Objects.equals(updatedAt, quote.updatedAt) &&
        Objects.equals(createdBy, quote.createdBy) &&
        Objects.equals(lastUpdatedBy, quote.lastUpdatedBy) &&
        Objects.equals(awardServiceTracked, quote.awardServiceTracked) &&
        Objects.equals(defaultTermTypeId, quote.defaultTermTypeId) &&
        Objects.equals(description, quote.description) &&
        Objects.equals(endOfWarrantyService, quote.endOfWarrantyService) &&
        Objects.equals(otherReferenceNumber, quote.otherReferenceNumber) &&
        Objects.equals(partyId, quote.partyId) &&
        Objects.equals(productStoreId, quote.productStoreId) &&
        Objects.equals(quoteQualityScore, quote.quoteQualityScore) &&
        Objects.equals(startOfWarrantyService, quote.startOfWarrantyService) &&
        Objects.equals(treatAdjustmentsAsLineItem, quote.treatAdjustmentsAsLineItem) &&
        Objects.equals(statusChangeReason, quote.statusChangeReason);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name, supplierId, supplierName, billingAddress, shippingAddress, customer, solicitationNumber, subTotal, total, discountPrice, shippingPrice, costTotol, revenuePercent, profitTotal, statusId, typeId, currencyUomId, quoteRoles, billingCustomer, quality, issueDate, validFromDate, validThruDate, createdAt, updatedAt, createdBy, lastUpdatedBy,statusChangeReason,awardServiceTracked,defaultTermTypeId,description,endOfWarrantyService,otherReferenceNumber,partyId,productStoreId,quoteQualityScore,startOfWarrantyService,treatAdjustmentsAsLineItem,tagId,tagName,tagColorCode,estimatedAwardMonth,estimatedAwardQuarter,estimatedAwardYear,percentageConfidence,
            salesPipelineDataNotes,attrValue,attrName);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class Quote {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    supplierId: ").append(toIndentedString(supplierId)).append("\n");
    sb.append("    supplierName: ").append(toIndentedString(supplierName)).append("\n");
    sb.append("    billingAddress: ").append(toIndentedString(billingAddress)).append("\n");
    sb.append("    shippingAddress: ").append(toIndentedString(shippingAddress)).append("\n");
    sb.append("    customer: ").append(toIndentedString(customer)).append("\n");
    sb.append("    solicitationNumber: ").append(toIndentedString(solicitationNumber)).append("\n");
    sb.append("    subTotal: ").append(toIndentedString(subTotal)).append("\n");
    sb.append("    total: ").append(toIndentedString(total)).append("\n");
    sb.append("    discountPrice: ").append(toIndentedString(discountPrice)).append("\n");
    sb.append("    shippingPrice: ").append(toIndentedString(shippingPrice)).append("\n");
    sb.append("    costTotol: ").append(toIndentedString(costTotol)).append("\n");
    sb.append("    revenuePercent: ").append(toIndentedString(revenuePercent)).append("\n");
    sb.append("    profitTotal: ").append(toIndentedString(profitTotal)).append("\n");
    sb.append("    statusId: ").append(toIndentedString(statusId)).append("\n");
    sb.append("    typeId: ").append(toIndentedString(typeId)).append("\n");
    sb.append("    currencyUomId: ").append(toIndentedString(currencyUomId)).append("\n");
    sb.append("    quoteRoles: ").append(toIndentedString(quoteRoles)).append("\n");
    sb.append("    billingCustomer: ").append(toIndentedString(billingCustomer)).append("\n");
    sb.append("    quality: ").append(toIndentedString(quality)).append("\n");
    sb.append("    issueDate: ").append(toIndentedString(issueDate)).append("\n");
    sb.append("    validFromDate: ").append(toIndentedString(validFromDate)).append("\n");
    sb.append("    validThruDate: ").append(toIndentedString(validThruDate)).append("\n");
    sb.append("    createdAt: ").append(toIndentedString(createdAt)).append("\n");
    sb.append("    updatedAt: ").append(toIndentedString(updatedAt)).append("\n");
    sb.append("    createdBy: ").append(toIndentedString(createdBy)).append("\n");
    sb.append("    lastUpdatedBy: ").append(toIndentedString(lastUpdatedBy)).append("\n");
    sb.append("    statusChangeReason: ").append(toIndentedString(statusChangeReason)).append("\n");
    sb.append("    tagId: ").append(toIndentedString(tagId)).append("\n");
    sb.append("    tagName: ").append(toIndentedString(tagName)).append("\n");
    sb.append("    tagColorCode: ").append(toIndentedString(tagColorCode)).append("\n");
    sb.append("    estimatedAwardMonth: ").append(toIndentedString(estimatedAwardMonth)).append("\n");
    sb.append("    estimatedAwardQuarter: ").append(toIndentedString(estimatedAwardQuarter)).append("\n");
    sb.append("    estimatedAwardYear: ").append(toIndentedString(estimatedAwardYear)).append("\n");
    sb.append("    percentageConfidence: ").append(toIndentedString(percentageConfidence)).append("\n");
    sb.append("    salesPipelineDataNotes: ").append(toIndentedString(salesPipelineDataNotes)).append("\n");
    sb.append("    attrName: ").append(toIndentedString(attrName)).append("\n");
    sb.append("    attrValue: ").append(toIndentedString(attrValue)).append("\n");
    sb.append("    awardServiceTracked: ").append(toIndentedString(awardServiceTracked)).append("\n");
    sb.append("    defaultTermTypeId: ").append(toIndentedString(defaultTermTypeId)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    endOfWarrantyService: ").append(toIndentedString(endOfWarrantyService)).append("\n");
    sb.append("    otherReferenceNumber: ").append(toIndentedString(otherReferenceNumber)).append("\n");
    sb.append("    partyId: ").append(toIndentedString(partyId)).append("\n");
    sb.append("    productStoreId: ").append(toIndentedString(productStoreId)).append("\n");
    sb.append("    quoteQualityScore: ").append(toIndentedString(quoteQualityScore)).append("\n");
    sb.append("    startOfWarrantyService: ").append(toIndentedString(startOfWarrantyService)).append("\n");
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

