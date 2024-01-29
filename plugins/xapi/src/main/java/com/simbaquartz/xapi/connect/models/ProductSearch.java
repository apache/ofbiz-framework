package com.simbaquartz.xapi.connect.models;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Objects;

/**
 * Created by Admin on 8/19/17.
 */


@javax.annotation.Generated(value = "class io.swagger.codegen.languages.JavaResteasyServerCodegen", date = "2017-06-30T20:56:17.408-07:00")
public class ProductSearch {

    private String createdAt = null;
    private String descriptionHtml = null;
    private String id = null;
    private String name = null;
    private String productType = null;
    private String barcode = null;
    private String sku = null;
    private BigDecimal price = null;
    private BigDecimal compareAtPrice = null;
    private Boolean taxable = null;
    private BigDecimal weight = null;
    private String weightUnit = null;
    private String urlHandle = null;
    private String vendor = null;
    private String publishedAt = null;
    private String createdAfter = null;
    private String createdBefore = null;
    private Integer startIndex = null;
    private Integer viewSize = null;
    private String sortBy = null;

    /**
     * Sort by Field name
     * @return
     */
    public String getSortBy() {return sortBy;}
    public void setSortBy(String sortBy) {this.sortBy = sortBy;}

    /**
     * Unique identifier representing a specific product type for a given store.
     **/

    @JsonProperty("product_type_id")
    public String getProductType() {
        return productType;
    }

    public void setProductType(String productType) {
        this.productType = productType;
    }


    /**
     * Unique identifier representing a specific product for a given store.
     **/

    @JsonProperty("id")
    public String getProductId() {
        return id;
    }

    public void setProductId(String id) {
        this.id = id;
    }


    /**
     * The name of the product.
     **/

    @JsonProperty("product_name")
    public String getProductName() {
        return name;
    }

    public void setProductName(String name) {
        this.name = name;
    }

    /**
     * The description of the product, complete with HTML formatting.
     **/

    @JsonProperty("long_description")
    public String getDescriptionHtml() {
        return descriptionHtml;
    }

    public void setDescriptionHtml(String descriptionHtml) {
        this.descriptionHtml = descriptionHtml;
    }

    /**
     * The name of the vendor of the product. For example Nike, Apple etc.
     **/

    @JsonProperty("brand_name")
    public String getVendor() {
        return vendor;
    }

    public void setVendor(String vendor) {
        this.vendor = vendor;
    }

    /**
     * The date and time when the product was published to the Online Store channel. The API returns this value in RFC 3339 format. A value of null indicates that the product is not published to Online Store.
     **/

    @JsonProperty("release_date")
    public String getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(String publishedAt) {
        this.publishedAt = publishedAt;
    }

    /**
     * A human-friendly unique string for the Product automatically generated from its title.
     **/

    @JsonProperty("product_url")
    public String getUrlHandle() {
        return urlHandle;
    }

    public void setUrlHandle(String urlHandle) {
        this.urlHandle = urlHandle;
    }

    /**
     *
     **/

    @JsonProperty("barcode")
    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    /**
     * The product's created after date.
     **/

    @JsonProperty("created_after")
    public String getCreatedAfter() {
        return createdAfter;
    }

    public void setCreatedAfter(String createdAfter) {
        this.createdAfter = createdAfter;
    }

    /**
     * The product's created before date.
     **/

    @JsonProperty("created_before")
    public String getCreatedBefore() {
        return createdBefore;
    }

    public void setCreatedBefore(String createdBefore) {
        this.createdBefore = createdBefore;
    }

    /**
     *
     **/

    @JsonProperty("sku")
    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    /**
     * Default price of product
     **/

    @JsonProperty("product_price")
    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    /**
     *LIST PRICE  of product
     **/

    @JsonProperty("product_list_price")
    public BigDecimal getCompareAtPrice() {
        return compareAtPrice;
    }

    public void setCompareAtPrice(BigDecimal compareAtPrice) {
        this.compareAtPrice = compareAtPrice;
    }

    /**
     *product weight
     **/

    @JsonProperty("product_weight")
    public BigDecimal getWeight() {
        return weight;
    }

    public void setWeight(BigDecimal weight) {
        this.weight = weight;
    }

    /**
     * product weight unit
     **/

    @JsonProperty("weight_uom_id")
    public String getWeightUnit() {
        return weightUnit;
    }

    public void setWeightUnit(String weightUnit) {
        this.weightUnit = weightUnit;
    }

    /**
     *
     **/

    @JsonProperty("taxable")
    public Boolean getTaxable() {
        return taxable;
    }

    public void setTaxable(Boolean taxable) {
        this.taxable = taxable;
    }

    /**
     * start index
     **/

    @JsonProperty("start_index")
    public Integer getStartIndex() {
        return startIndex;
    }

    public void setStartIndex(Integer startIndex) {
        this.startIndex = startIndex;
    }

    /**
     * Customer list size
     **/

    @JsonProperty("view_size")
    public Integer getViewSize() {
        return viewSize;
    }

    public void setViewSize(Integer viewSize) {
        this.viewSize = viewSize;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ProductSearch product = (ProductSearch) o;
        return Objects.equals(id, product.id) &&
                Objects.equals(name, product.name) &&
                Objects.equals(descriptionHtml, product.descriptionHtml) &&
                Objects.equals(vendor, product.vendor) &&
                Objects.equals(publishedAt, product.publishedAt) &&
                Objects.equals(urlHandle, product.urlHandle) &&
                Objects.equals(productType, product.productType) &&
                Objects.equals(barcode, product.barcode) &&
                Objects.equals(sku, product.sku) &&
                Objects.equals(price, product.price) &&
                Objects.equals(compareAtPrice, product.compareAtPrice) &&
                Objects.equals(taxable, product.taxable) &&
                Objects.equals(weight, product.weight) &&
                Objects.equals(createdAt, product.createdAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, descriptionHtml, vendor, publishedAt, taxable, weight, barcode, sku, price, compareAtPrice, productType, urlHandle, createdAt);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class Product {\n");

        sb.append("    id: ").append(toIndentedString(id)).append("\n");
        sb.append("    name: ").append(toIndentedString(name)).append("\n");
        sb.append("    descriptionHtml: ").append(toIndentedString(descriptionHtml)).append("\n");
        sb.append("    vendor: ").append(toIndentedString(vendor)).append("\n");
        sb.append("    publishedAt: ").append(toIndentedString(publishedAt)).append("\n");
        sb.append("    urlHandle: ").append(toIndentedString(urlHandle)).append("\n");
        sb.append("    productType: ").append(toIndentedString(productType)).append("\n");
        sb.append("    barcode: ").append(toIndentedString(barcode)).append("\n");
        sb.append("    sku: ").append(toIndentedString(sku)).append("\n");
        sb.append("    price: ").append(toIndentedString(price)).append("\n");
        sb.append("    compareAtPrice: ").append(toIndentedString(compareAtPrice)).append("\n");
        sb.append("    taxable: ").append(toIndentedString(taxable)).append("\n");
        sb.append("    weight: ").append(toIndentedString(weight)).append("\n");
        sb.append("    createdAt: ").append(toIndentedString(createdAt)).append("\n");
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

