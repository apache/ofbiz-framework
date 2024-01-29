package com.simbaquartz.xapi.connect.models;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Objects;


/**
 * Represents a Discount object.
 **/

@javax.annotation.Generated(value = "class io.swagger.codegen.languages.JavaResteasyServerCodegen", date = "2017-07-07T13:00:22.061+05:30")
public class DiscountSearch   {

    private String id;

    private String name;
    private String code;
    private BigDecimal value;

    private String discountType;

    private Timestamp startsAt;
    private Timestamp endsAt;
    private BigDecimal minimumOrderAmount;
    private Long usageLimit;
    private Boolean appliesOnce;
    private Boolean appliesOncePerCustomer;
    private Boolean appliesToRegisteredCustomersOnly;

    private String appliesToProductIds;
    private String appliesToCategoryIds;
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
     * The unique numeric identifier for the discount.
     **/

    @JsonProperty("id")
    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }

    /**
     * The name for your dicount, e.g. 50% Off Storewide
     **/
    @JsonProperty("name")
    public String getName() {return name;}
    public void setName(String name) {
        this.name = name;
    }

    /**
     * The case-insensitive discount code that customers use at checkout.
     **/

    @JsonProperty("code")
    public String getCode() {
        return code;
    }
    public void setCode(String code) {
        this.code = code;
    }

    /**
     * The value of the discount.
     **/

    @JsonProperty("value")
    public BigDecimal getValue() {
        return value;
    }
    public void setValue(BigDecimal value) {
        this.value = value;
    }

    /**
     * Specify how the discount's value will be applied to the order. Valid values are:
     * fixed_amount: The value as a unit of the store's currency will be discounted. E.g. If value is 30 and store's currency is USD, then $30 is deducted.
     * percentage: The percent amount to discount the order, e.g. 15% off.
     * shipping: Applies a free shipping discount on orders that have a shipping rate less than or equal to the amount specified in the value property. For example, free shipping for any shipping rate that is less than or equal to $70.
     **/
    @JsonProperty("discountType")
    public String getDiscountType() {
        return discountType;
    }
    public void setDiscountType(String discountType) {
        this.discountType = discountType;
    }

    /**
     * The date when the discount code becomes disabled.
     **/

    @JsonProperty("ends_at")
    public Timestamp getEndsAt() {
        return endsAt;
    }
    public void setEndsAt(Timestamp endsAt) {
        this.endsAt = endsAt;
    }

    /**
     * The date the discount becomes valid for use during checkout.
     **/

    @JsonProperty("starts_at")
    public Timestamp getStartsAt() {
        return startsAt;
    }
    public void setStartsAt(Timestamp startsAt) {
        this.startsAt = startsAt;
    }

    /**
     * The minimum value an order must reach for the discount to be allowed during checkout.
     **/

    @JsonProperty("minimum_order_amount")
    public BigDecimal getMinimumOrderAmount() {
        return minimumOrderAmount;
    }
    public void setMinimumOrderAmount(BigDecimal minimumOrderAmount) {
        this.minimumOrderAmount = minimumOrderAmount;
    }

    /**
     * The number of times this discount code can be redeemed.
     **/

    @JsonProperty("usage_limit")
    public Long getUsageLimit() {
        return usageLimit;
    }
    public void setUsageLimit(Long usageLimit) {
        this.usageLimit = usageLimit;
    }

    /**
     * The id of a collection or product that this discount code is restricted to.
     **/

    @JsonProperty("applies_to_product_ids")
    public String getAppliesToProductIds() {
        return appliesToProductIds;
    }
    public void setAppliesToProductIds(String appliesToProductIds) {
        this.appliesToProductIds = appliesToProductIds;
    }

    /**
     * When a discount applies to a product or collection resource, applies_once determines whether  the discount should be applied once per order, or to every applicable item in the cart.
     **/

    @JsonProperty("applies_once")
    public Boolean getAppliesOnce() {
        return appliesOnce;
    }
    public void setAppliesOnce(Boolean appliesOnce) {
        this.appliesOnce = appliesOnce;
    }

    /**
     * Determines whether the discount should be applied once, or any number of times per customer.
     **/

    @JsonProperty("applies_once_per_customer")
    public Boolean getAppliesOncePerCustomer() {
        return appliesOncePerCustomer;
    }
    public void setAppliesOncePerCustomer(Boolean appliesOncePerCustomer) {
        this.appliesOncePerCustomer = appliesOncePerCustomer;
    }

    /**
     * The discount code can be set to apply to only a product, smart_collection, customersavedsearch  or custom_collection. If applies_to_resource is set, then applies_to_id should also be set.
     **/

    @JsonProperty("applies_to_category_ids")
    public String getAppliesToCategoryIds() {
        return appliesToCategoryIds;
    }
    public void setAppliesToCategoryIds(String appliesToCategoryIds) {
        this.appliesToCategoryIds = appliesToCategoryIds;
    }

    /**
     * Determines whether the discount should be applied for registered users once.
     */
    @JsonProperty("applies_to_registered_only")
    public Boolean getAppliesToRegisteredCustomersOnly() {return appliesToRegisteredCustomersOnly;}
    public void setAppliesToRegisteredCustomersOnly(Boolean appliesToRegisteredCustomersOnly) {
        this.appliesToRegisteredCustomersOnly = appliesToRegisteredCustomersOnly;
    }
    /**
     * The customer's created after date.
     **/

    @JsonProperty("created_after")
    public String getCreatedAfter() {
        return createdAfter;
    }

    public void setCreatedAfter(String createdAfter) {
        this.createdAfter = createdAfter;
    }

    /**
     * The customer's created before date.
     **/

    @JsonProperty("created_before")
    public String getCreatedBefore() {
        return createdBefore;
    }

    public void setCreatedBefore(String createdBefore) {
        this.createdBefore = createdBefore;
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
        DiscountSearch discountSearch = (DiscountSearch) o;
        return Objects.equals(id, discountSearch.id) &&
                Objects.equals(code, discountSearch.code) &&
                Objects.equals(value, discountSearch.value) &&
                Objects.equals(discountType, discountSearch.discountType) &&
                Objects.equals(endsAt, discountSearch.endsAt) &&
                Objects.equals(startsAt, discountSearch.startsAt) &&
                Objects.equals(minimumOrderAmount, discountSearch.minimumOrderAmount) &&
                Objects.equals(usageLimit, discountSearch.usageLimit) &&
                Objects.equals(appliesToProductIds, discountSearch.appliesToProductIds) &&
                Objects.equals(appliesOnce, discountSearch.appliesOnce) &&
                Objects.equals(appliesOncePerCustomer, discountSearch.appliesOncePerCustomer) &&
                Objects.equals(appliesToCategoryIds, discountSearch.appliesToCategoryIds);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, code, value, discountType, endsAt, startsAt, minimumOrderAmount, usageLimit, appliesToProductIds, appliesOnce, appliesOncePerCustomer, appliesToCategoryIds);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class Discount {\n");

        sb.append("    id: ").append(toIndentedString(id)).append("\n");
        sb.append("    code: ").append(toIndentedString(code)).append("\n");
        sb.append("    value: ").append(toIndentedString(value)).append("\n");
        sb.append("    discountType: ").append(toIndentedString(discountType)).append("\n");
        sb.append("    endsAt: ").append(toIndentedString(endsAt)).append("\n");
        sb.append("    startsAt: ").append(toIndentedString(startsAt)).append("\n");
        sb.append("    minimumOrderAmount: ").append(toIndentedString(minimumOrderAmount)).append("\n");
        sb.append("    usageLimit: ").append(toIndentedString(usageLimit)).append("\n");
        sb.append("    appliesToId: ").append(toIndentedString(appliesToProductIds)).append("\n");
        sb.append("    appliesOnce: ").append(toIndentedString(appliesOnce)).append("\n");
        sb.append("    appliesOncePerCustomer: ").append(toIndentedString(appliesOncePerCustomer)).append("\n");
        sb.append("    appliesToResource: ").append(toIndentedString(appliesToCategoryIds)).append("\n");
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

