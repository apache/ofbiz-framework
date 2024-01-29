package com.simbaquartz.xapi.connect.models.product;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.sql.Timestamp;
import java.util.Objects;


@javax.annotation.Generated(value = "class io.swagger.codegen.languages.JavaResteasyServerCodegen", date = "2017-06-30T20:56:17.408-07:00")
public class ProductConfiguration {

    private String configItemid = null;
    private String productId = null;
    private String description = null;
    private String longDescription = null;
    private String configurationTypeId= null;
    private String defaultConfigurationOptionId = null;
    private Long sequenceNum = null;
    private Boolean isMandatory = null;
    private Timestamp fromDate = null;
    private Timestamp thruDate = null;

    /**
     * Unique identifier representing a specific configuration for a given item.
     **/

    @JsonProperty("config_item_id")
    public String getConfigItemid() {
        return configItemid;
    }

    public void setConfigItemid(String configItemid) {
        this.configItemid = configItemid;
    }
    /**
     *
     **/

    @JsonProperty("product_id")
    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    /**
     * The description of the product Coonfiguration.
     **/

    @JsonProperty("description")
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * The long description of the product Coonfiguration.
     **/

    @JsonProperty("long_description")
    public String getLongDescription() {
        return longDescription;
    }

    public void setLongDescription(String longDescription) {
        this.longDescription = longDescription;
    }

    /**
     * Representing a specific configuration type for a given product.
     **/

    @JsonProperty("configuration_type")
    public String getConfigurationTypeId() {
        return configurationTypeId;
    }

    public void setConfigurationTypeId(String configurationTypeId) {
        this.configurationTypeId= configurationTypeId;
    }
   /**
     * Representing a default configuration option for a given product.
     **/

    @JsonProperty("default_configuration_option_id")
    public String getDefaultConfigOptionId() {
        return defaultConfigurationOptionId;
    }

    public void setDefaultConfigOptionId(String defaultConfigurationOptionId) {
        this.defaultConfigurationOptionId= defaultConfigurationOptionId;
    }

    /**
     * Representing weather the configuration is mandatory or not.
     **/

    @JsonProperty("is_mandatory")
    public Boolean getIsMandatory() {
        return isMandatory;
    }

    public void setIsAvailable(Boolean isAvailable) {
        this.isMandatory = isAvailable;
    }

    /**
     * Represents the sequence number for the different configurations of a same product.
     **/

    @JsonProperty("sequence_num")
    public Long getSequenceNum() {
        return sequenceNum;
    }

    public void setSequenceNum(Long sequenceNum) {
        this.sequenceNum = sequenceNum;
    }

    /**
    * Represents the date on which the configuration starts applying on the product
    */


    @JsonProperty("from_date")
    public Timestamp getFromDate() {
        return fromDate;
    }

    public void setFromDate(Timestamp fromDate) {
        this.fromDate = fromDate;
    }

    /**
     * Represents the date on which the configuration stops applying on the product
     */

    @JsonProperty("thru_date")
    public Timestamp getThruDate() {
        return thruDate;
    }

    public void setThruDate(Timestamp thruDate) {
        this.thruDate = thruDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ProductConfiguration productConfiguration = (ProductConfiguration) o;
        return Objects.equals(configItemid, productConfiguration.configItemid) &&
                Objects.equals(description, productConfiguration.description) &&
                Objects.equals(longDescription, productConfiguration.longDescription) &&
                Objects.equals(configurationTypeId, productConfiguration.configurationTypeId) &&
                Objects.equals(defaultConfigurationOptionId, productConfiguration.defaultConfigurationOptionId) &&
                Objects.equals(sequenceNum, productConfiguration.sequenceNum) &&
                Objects.equals(isMandatory, productConfiguration.isMandatory) &&
                Objects.equals(fromDate, productConfiguration.fromDate) &&
                Objects.equals(thruDate, productConfiguration.thruDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(configItemid, description, longDescription, configurationTypeId, defaultConfigurationOptionId, sequenceNum, isMandatory, fromDate, thruDate);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class ProductConfuguration {\n");

        sb.append("    configItemid: ").append(toIndentedString(configItemid)).append("\n");
        sb.append("    description: ").append(toIndentedString(description)).append("\n");
        sb.append("    longDescription: ").append(toIndentedString(longDescription)).append("\n");
        sb.append("    configurationTypeId: ").append(toIndentedString(configurationTypeId)).append("\n");
        sb.append("    defaultConfigurationOptionId: ").append(toIndentedString(defaultConfigurationOptionId)).append("\n");
        sb.append("    sequenceNum: ").append(toIndentedString(sequenceNum)).append("\n");
        sb.append("    isMandatory: ").append(toIndentedString(isMandatory)).append("\n");
        sb.append("    fromDate: ").append(toIndentedString(fromDate)).append("\n");
        sb.append("    thruDate: ").append(toIndentedString(thruDate)).append("\n");
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

