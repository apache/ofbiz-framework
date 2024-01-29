package com.simbaquartz.xapi.connect.models.product;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

import java.math.BigDecimal;
import java.util.Objects;


@javax.annotation.Generated(value = "class io.swagger.codegen.languages.JavaResteasyServerCodegen", date = "2017-07-03T09:29:13.884+05:30")
public class ProductVariant   {
  
  private String id = null;
  private String productId = null;
  private String title = null;
  private Integer positionIndex = null;
  private String barcode = null;
  private String sku = null;
  private BigDecimal price = null;
  private BigDecimal compareAtPrice = null;
  private Boolean taxable = null;
  private BigDecimal weight = null;

  /**
   * The unit system that the product variant's weight is measure in. The weight_unit can be either \"g\", \"kg, \"oz\", or \"lb\".
   */
  public enum WeightUnitEnum {
    G("g"),

        KG("kg"),

        OZ("oz"),

        LB("lb");
    private String value;

    WeightUnitEnum(String value) {
      this.value = value;
    }

    @Override
    @JsonValue
    public String toString() {
      return String.valueOf(value);
    }
  }

  private WeightUnitEnum weightUnit = null;
  private ProductImage imageId = null;
  private Boolean inventoryTracked = null;
  private Boolean inventoryRequired = null;
  private Integer inventoryQuantity = null;
  private String option = null;
  private String createdAt = null;
  private String updatedAt = null;

  /**
   * The unique numeric identifier for the product variant.
   **/
  
  @JsonProperty("id")
  public String getId() {
    return id;
  }
  public void setId(String id) {
    this.id = id;
  }

  /**
   * The unique numeric identifier for the product.
   **/
  
  @JsonProperty("product_id")
  public String getProductId() {
    return productId;
  }
  public void setProductId(String productId) {
    this.productId = productId;
  }

  /**
   * The title of the product variant.
   **/
  
  @JsonProperty("title")
  public String getTitle() {
    return title;
  }
  public void setTitle(String title) {
    this.title = title;
  }

  /**
   * The order of the product image in the list. The first product image is at position 1 and is the \"main\" image for the product.
   **/
  
  @JsonProperty("position_index")
  public Integer getPositionIndex() {
    return positionIndex;
  }
  public void setPositionIndex(Integer positionIndex) {
    this.positionIndex = positionIndex;
  }

  /**
   * The barcode, UPC or ISBN number for the product.
   **/
  
  @JsonProperty("barcode")
  public String getBarcode() {
    return barcode;
  }
  public void setBarcode(String barcode) {
    this.barcode = barcode;
  }

  /**
   * A unique identifier for the product in the shop.
   **/
  
  @JsonProperty("sku")
  public String getSku() {
    return sku;
  }
  public void setSku(String sku) {
    this.sku = sku;
  }

  /**
   * The price of the product variant. E.g. 192.00
   **/
  
  @JsonProperty("price")
  public BigDecimal getPrice() {
    return price;
  }
  public void setPrice(BigDecimal price) {
    this.price = price;
  }

  /**
   * The competitors prices for the same item.
   **/
  
  @JsonProperty("compare_at_price")
  public BigDecimal getCompareAtPrice() {
    return compareAtPrice;
  }
  public void setCompareAtPrice(BigDecimal compareAtPrice) {
    this.compareAtPrice = compareAtPrice;
  }

  /**
   * Specifies whether or not a tax is charged when the product variant is sold.
   **/
  
  @JsonProperty("taxable")
  public Boolean getTaxable() {
    return taxable;
  }
  public void setTaxable(Boolean taxable) {
    this.taxable = taxable;
  }

  /**
   * The weight of the product variant in weight_unit or store's default weight UOM.
   **/
  
  @JsonProperty("weight")
  public BigDecimal getWeight() {
    return weight;
  }
  public void setWeight(BigDecimal weight) {
    this.weight = weight;
  }

  /**
   * The unit system that the product variant's weight is measure in. The weight_unit can be either \"g\", \"kg, \"oz\", or \"lb\".
   **/
  
  @JsonProperty("weight_unit")
  public WeightUnitEnum getWeightUnit() {
    return weightUnit;
  }
  public void setWeightUnit(WeightUnitEnum weightUnit) {
    this.weightUnit = weightUnit;
  }

  /**
   **/
  
  @JsonProperty("image_id")
  public ProductImage getImageId() {
    return imageId;
  }
  public void setImageId(ProductImage imageId) {
    this.imageId = imageId;
  }

  /**
   * Specifies whether or not inventory is tracked for the product. If set to true, system will track inventory for this product item.
   **/
  
  @JsonProperty("inventory_tracked")
  public Boolean getInventoryTracked() {
    return inventoryTracked;
  }
  public void setInventoryTracked(Boolean inventoryTracked) {
    this.inventoryTracked = inventoryTracked;
  }

  /**
   * Specifies whether or not inventory is required for the product. If set to true, item can not be purchased if stock is 0.
   **/
  
  @JsonProperty("inventory_required")
  public Boolean getInventoryRequired() {
    return inventoryRequired;
  }
  public void setInventoryRequired(Boolean inventoryRequired) {
    this.inventoryRequired = inventoryRequired;
  }

  /**
   * The number of items in stock for this product variant.
   **/
  
  @JsonProperty("inventory_quantity")
  public Integer getInventoryQuantity() {
    return inventoryQuantity;
  }
  public void setInventoryQuantity(Integer inventoryQuantity) {
    this.inventoryQuantity = inventoryQuantity;
  }

  /**
   * Custom properties that a store owner can use to define product variants. Multiple options can exist. Options are represented as option1, option2. The default value is 'Default Title'.
   **/
  
  @JsonProperty("option")
  public String getOption() {
    return option;
  }
  public void setOption(String option) {
    this.option = option;
  }

  /**
   * The time when the product variant was created, in RFC 3339 format.
   **/
  
  @JsonProperty("created_at")
  public String getCreatedAt() {
    return createdAt;
  }
  public void setCreatedAt(String createdAt) {
    this.createdAt = createdAt;
  }

  /**
   * The time when the product variant was last updated, in RFC 3339 format.
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
    ProductVariant productVariant = (ProductVariant) o;
    return Objects.equals(id, productVariant.id) &&
        Objects.equals(productId, productVariant.productId) &&
        Objects.equals(title, productVariant.title) &&
        Objects.equals(positionIndex, productVariant.positionIndex) &&
        Objects.equals(barcode, productVariant.barcode) &&
        Objects.equals(sku, productVariant.sku) &&
        Objects.equals(price, productVariant.price) &&
        Objects.equals(compareAtPrice, productVariant.compareAtPrice) &&
        Objects.equals(taxable, productVariant.taxable) &&
        Objects.equals(weight, productVariant.weight) &&
        Objects.equals(weightUnit, productVariant.weightUnit) &&
        Objects.equals(imageId, productVariant.imageId) &&
        Objects.equals(inventoryTracked, productVariant.inventoryTracked) &&
        Objects.equals(inventoryRequired, productVariant.inventoryRequired) &&
        Objects.equals(inventoryQuantity, productVariant.inventoryQuantity) &&
        Objects.equals(option, productVariant.option) &&
        Objects.equals(createdAt, productVariant.createdAt) &&
        Objects.equals(updatedAt, productVariant.updatedAt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, productId, title, positionIndex, barcode, sku, price, compareAtPrice, taxable, weight, weightUnit, imageId, inventoryTracked, inventoryRequired, inventoryQuantity, option, createdAt, updatedAt);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ProductVariant {\n");
    
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    productId: ").append(toIndentedString(productId)).append("\n");
    sb.append("    title: ").append(toIndentedString(title)).append("\n");
    sb.append("    positionIndex: ").append(toIndentedString(positionIndex)).append("\n");
    sb.append("    barcode: ").append(toIndentedString(barcode)).append("\n");
    sb.append("    sku: ").append(toIndentedString(sku)).append("\n");
    sb.append("    price: ").append(toIndentedString(price)).append("\n");
    sb.append("    compareAtPrice: ").append(toIndentedString(compareAtPrice)).append("\n");
    sb.append("    taxable: ").append(toIndentedString(taxable)).append("\n");
    sb.append("    weight: ").append(toIndentedString(weight)).append("\n");
    sb.append("    weightUnit: ").append(toIndentedString(weightUnit)).append("\n");
    sb.append("    imageId: ").append(toIndentedString(imageId)).append("\n");
    sb.append("    inventoryTracked: ").append(toIndentedString(inventoryTracked)).append("\n");
    sb.append("    inventoryRequired: ").append(toIndentedString(inventoryRequired)).append("\n");
    sb.append("    inventoryQuantity: ").append(toIndentedString(inventoryQuantity)).append("\n");
    sb.append("    option: ").append(toIndentedString(option)).append("\n");
    sb.append("    createdAt: ").append(toIndentedString(createdAt)).append("\n");
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

