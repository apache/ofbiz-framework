package com.simbaquartz.xcommon.models.store;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;


/**
 * Store&#39;s configuration e.g. chargeTaxes, allowDiscounts
 **/

@javax.annotation.Generated(value = "class io.swagger.codegen.languages.JavaResteasyServerCodegen", date = "2017-05-09T10:47:15.854-07:00")
public class StoreSettings {

    private String id = null;
    private Boolean chargeTaxes = null;
    private String weightUnit = null;
    private String subscriptionPlan = null;
    private Boolean hasDiscounts = null;
    private String moneyFormat = null;
    private Boolean hasGiftCards = null;
    private Boolean isLive = null;
    private String taxShipping = null;
    private Boolean taxesIncluded = null;
    private String timeZone = null;
    private Boolean setupRequired = null;
    private Boolean isOpen = null;


    /**
       * store is open/close
       * */
    @JsonProperty("is_open")
    public Boolean getIsOpen() {
        return isOpen;
    }

    public void setIsOpen(Boolean isOpen) {
        this.isOpen = isOpen;
    }


    /**
     * check if the setup is required for the store.
     * *
     */

    @JsonProperty("setup_required")
    public Boolean getSetupRequired() {
        return setupRequired;
    }

    public void setSetupRequired(Boolean setupRequired) {
        this.setupRequired = setupRequired;
    }

    /**
     * Time zone preffered by the store.
     * *
     */

    @JsonProperty("time_zone")
    public String getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }


    /**
     * are the taxes included
     * *
     */

    @JsonProperty("taxes_included")
    public Boolean getTaxesIncluded() {
        return taxesIncluded;
    }

    public void setTaxesIncluded(Boolean taxesIncluded) {
        this.taxesIncluded = taxesIncluded;
    }

    /**
     * tax on shipping
     * *
     */

    @JsonProperty("tax_shipping")
    public String getTaxShipping() {
        return taxShipping;
    }

    public void setTaxShipping(String taxShipping) {
        this.taxShipping = taxShipping;
    }


    /**
     * Weather the store is live or not
     * *
     */

    @JsonProperty("is_live")
    public Boolean getIsLive() {
        return isLive;
    }

    public void setIsLive(Boolean isLive) {
        this.isLive = isLive;
    }

    /**
     * Weather the store has discounts or not
     * *
     */

    @JsonProperty("has_gift_cards")
    public Boolean getHasGiftCards() {
        return hasDiscounts;
    }

    public void setHasGiftCards(Boolean hasDiscounts) {
        this.hasDiscounts = hasDiscounts;
    }

    /**
     * What are the money formats that store follows.
     * *
     */

    @JsonProperty("money_format")
    public String getMoneyFormat() {
        return moneyFormat;
    }

    public void setMoneyFormat(String moneyFormat) {
        this.moneyFormat = moneyFormat;
    }

    /**
     * Weather the store has discounts or not
     * *
     */

    @JsonProperty("has_discounts")
    public Boolean getHasDiscounts() {
        return hasDiscounts;
    }

    public void setHasDiscounts(Boolean hasDiscounts) {
        this.hasDiscounts = hasDiscounts;
    }

    /**
     * What are the subscription plans that store offers.
     * *
     */

    @JsonProperty("subscription_plan")
    public String getSubscriptionPlan() {
        return subscriptionPlan;
    }

    public void setSubscriptionPlan(String subscriptionPlan) {
        this.subscriptionPlan = subscriptionPlan;
    }

    /**
     * Default unit of measure for weights for the store.?
     */

    @JsonProperty("weight_unit")
    public String getWeightUnit() {
        return weightUnit;
    }

    public void setWeightUnit(String weightUnit) {
        this.weightUnit = weightUnit;
    }


    /**
     * Unique identifier of a StoreSettings. For example, storeX may have a sequential system generated store id much like 100001.
     **/

    @JsonProperty("id")
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    /**
     * Whether the store needs to charge taxes.
     **/

    @JsonProperty("charge_taxes")
    public Boolean getChargeTaxes() {
        return chargeTaxes;
    }

    public void setChargeTaxes(Boolean chargeTaxes) {
        this.chargeTaxes = chargeTaxes;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        StoreSettings storeConfig = (StoreSettings) o;
        return Objects.equals(id, storeConfig.id) &&
                Objects.equals(chargeTaxes, storeConfig.chargeTaxes) &&
                Objects.equals(weightUnit, storeConfig.weightUnit) &&
                Objects.equals(subscriptionPlan, storeConfig.subscriptionPlan) &&
                Objects.equals(hasDiscounts, storeConfig.hasDiscounts) &&
                Objects.equals(moneyFormat, storeConfig.moneyFormat) &&
                Objects.equals(hasGiftCards, storeConfig.hasGiftCards) &&
                Objects.equals(isLive, storeConfig.isLive) &&
                Objects.equals(taxShipping, storeConfig.taxShipping) &&
                Objects.equals(taxesIncluded, storeConfig.taxesIncluded) &&
                Objects.equals(timeZone, storeConfig.timeZone) &&
                Objects.equals(setupRequired, storeConfig.setupRequired);
    }

//        private String id = null;
//        private Boolean chargeTaxes = null;
//        private String weightUnit = null;
//        private String subscriptionPlan = null;
//        private Boolean hasDiscounts = null;
//        private String moneyFormat = null;
//        private Boolean hasGiftCards = null;
//        private Boolean isLive = null;
//        private String taxShipping = null;
//        private Boolean taxesIncluded = null;
//        private String timeZone = null;
//        private Boolean setupRequired = null;
        /**
         * Convert the given object to string with each line indented by 4 spaces
         * (except the first line).
         */

    private String toIndentedString (Object o){
        if (o == null) {
            return "null";
        }
        return o.toString().replace("\n", "\n    ");
    }
}

