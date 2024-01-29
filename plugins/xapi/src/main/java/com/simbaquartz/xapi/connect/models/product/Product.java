package com.simbaquartz.xapi.connect.models.product;

import com.fasterxml.jackson.annotation.JsonProperty;

import com.fidelissd.zcp.xcommon.models.people.Person;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import javax.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class Product {
  @NotEmpty(message = "Please provide a Product Id")
  @JsonProperty("productId")
  private String productId = null;

  @NotEmpty(message = "Please provide a Product Name")
  @JsonProperty("name")
  private String name = null;

  @JsonProperty("displayName")
  private String displayName = null;

  @JsonProperty("price")
  private BigDecimal price = null;

  @JsonProperty("formattedPrice")
  private String formattedPrice = null;

  @JsonProperty("description")
  private String description = null;

  @JsonProperty("longDescription")
  private String longDescription = null;

  @JsonProperty("images")
  private List<ProductImage> images = null;

  // List of custom field values
  /*@JsonProperty("customValues")
  private List<CustomFieldValue> customValues = null;*/

  @JsonProperty("productTags")
  private List<String> productTags = null;

  @JsonProperty("productVariants")
  private List<Product> productVariants = null;

  /** Any product associated media, photos, documents etc. */
  @JsonProperty("attachments")
  private List<ProductAttachment> attachments = null;

  @JsonProperty("variants")
  private List<Product> variants = null;

  @JsonProperty("productFeatureIds")
  private String productFeatureIds = null;

  @JsonProperty("descriptionHtml")
  private String descriptionHtml = null;

  @JsonProperty("projectId")
  private String projectId = null;

  @JsonProperty("currencyUomId")
  private String currencyUomId = null;

  @JsonProperty("storeId")
  private String storeId = null;

  @JsonProperty("productType")
  private String productType = null;

  @JsonProperty("unitPrice")
  private BigDecimal unitPrice = null;

  @JsonProperty("unitListPrice")
  private BigDecimal unitListPrice = null;

  @JsonProperty("unitCost")
  private BigDecimal unitCost = null;

  @JsonProperty("priceType")
  private String priceType = null;

  @JsonProperty("positionIndex")
  private Integer positionIndex = null;

  @JsonProperty("barcode")
  private String barcode = null;

  @JsonProperty("sku")
  private String sku = null;

  @JsonProperty("quantity")
  private BigDecimal quantity = null;

  @JsonProperty("minimumOrderQuantity")
  private BigDecimal minimumOrderQuantity = null;

  @JsonProperty("unitAverageCost")
  private BigDecimal unitAverageCost = null;

  @JsonProperty("shippingPrice")
  private BigDecimal shippingPrice = null;

  @JsonProperty("shippingCost")
  private BigDecimal shippingCost = null;

  @JsonProperty("uomId")
  private String uomId = null;

  @JsonProperty("stockCount")
  private BigDecimal stockCount = null;

  @JsonProperty("margin")
  private BigDecimal margin = null;

  @JsonProperty("compareAtPrice")
  private BigDecimal compareAtPrice = null;

  @JsonProperty("taxable")
  private Boolean taxable = null;

  @JsonProperty("gst")
  private BigDecimal gst = null;

  @JsonProperty("trackInventory")
  private Boolean trackInventory = null;

  @JsonProperty("continueAfterStockOut")
  private Boolean continueAfterStockOut = null;

  @JsonProperty("isPhysical")
  private Boolean isPhysical = null;

  @JsonProperty("hsn")
  private String hsn = null;

  @JsonProperty("productAttributes")
  private List<Map> productAttributes = null;

  @JsonProperty("updateCollection")
  private Boolean updateCollection = null;

  @JsonProperty("updateTag")
  private Boolean updateTag = null;

  @JsonProperty("updateVariant")
  private Boolean updateVariant = null;

  @JsonProperty("weight")
  private BigDecimal weight = null;

  @JsonProperty("weightUnit")
  private String weightUnit = null;

  @JsonProperty("urlHandle")
  private String urlHandle = null;

  @JsonProperty("vendor")
  private String vendor = null;

  @JsonProperty("isAvailable")
  private Boolean isAvailable = null;

  @JsonProperty("isConfigurable")
  private Boolean isConfigurable = null;

  @JsonProperty("publishedAt")
  private Timestamp publishedAt = null;

  @JsonProperty("createdAt")
  private Timestamp createdAt = null;

  @JsonProperty("createdAtPretty")
  private String createdAtPretty = null;

  @JsonProperty("updatedAt")
  private Timestamp updatedAt = null;

  @JsonProperty("proxyEnable")
  private String proxyEnable = null;

  @JsonProperty("desiredMarkup")
  private BigDecimal desiredMarkup = null;

  @JsonProperty("updatedAtPretty")
  private String updatedAtPretty = null;

  @JsonProperty("standardLeadTimeDays")
  private BigDecimal standardLeadTimeDays = null;

  @JsonProperty("supplierLastPrice")
  private BigDecimal supplierLastPrice = null;

  @JsonProperty("supplierShippingPrice")
  private BigDecimal supplierShippingPrice = null;

  @JsonProperty("supplierProductId")
  private String supplierProductId = null;

  @JsonProperty("quantityUomId")
  private String quantityUomId = null;

  @JsonProperty("endOfLife")
  private String endOfLife = null;

  @JsonProperty("categoryName")
  private String categoryName = null;

  @JsonProperty("categoryIds")
  private List<String> categoryIds = null;

  @JsonProperty("categoryId")
  private String categoryId = null;

  @JsonProperty("sequenceNum")
  private Long sequenceNum = null;

  @JsonProperty("supplierProductName")
  private String supplierProductName = null;

  @JsonProperty("thruDate")
  private Timestamp supplierProductAvailableThruDate = null;

  @JsonProperty("supplierProductAvailableFromDate")
  private Timestamp supplierProductAvailableFromDate = null;

  @JsonProperty("salesDiscontinuationDate")
  private Timestamp salesDiscontinuationDate = null;

  @JsonProperty("productCategories")
  private List<String> productCategories = null;

  @JsonProperty("productCollections")
  private List<String> productCollections = null;

  @JsonProperty("startIndex")
  private Integer startIndex = null;

  @JsonProperty("viewSize")
  private Integer viewSize = null;

  @JsonProperty("sortBy")
  private String sortBy = null;

  @JsonProperty("keyword")
  private String keyword = null;

  @JsonProperty("contentId")
  private String contentId = null;

  @JsonProperty("productContentTypeId")
  private String productContentTypeId = null;

  @JsonProperty("fromDate")
  private Timestamp fromDate = null;

  @JsonProperty("supplierId")
  private String supplierId = null;

  @JsonProperty("supplierName")
  private String supplierName = null;

  @JsonProperty("statusId")
  private String statusId = null;

  @JsonProperty("status")
  private String status = null;

  @JsonProperty("isVariant")
  private Boolean isVariant = null;

  @JsonProperty("createdBy")
  private Person createdBy = null;

  @JsonProperty("updatedBy")
  private Person updatedBy = null;

  @JsonProperty("variantCount")
  private Long variantCount = null;

  @JsonProperty("quantityOnHandTotal")
  private BigDecimal quantityOnHandTotal = null;

  @JsonProperty("availableToPromiseTotal")
  private BigDecimal availableToPromiseTotal = null;

  @JsonProperty("serviceMode")
  private String serviceMode = null;

  @JsonProperty("serviceModeId")
  private String serviceModeId = null;

  @JsonProperty("percentPrice")
  private BigDecimal percentPrice = null;

}
