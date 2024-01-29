package com.simbaquartz.xapi.connect.models.product;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.sql.Timestamp;
import java.util.Objects;


@javax.annotation.Generated(value = "class io.swagger.codegen.languages.JavaResteasyServerCodegen", date = "2017-06-30T20:56:17.408-07:00")
public class ProductConfigurationItem {

    private String id = null;
    private String configurationItemTypeId= null;
    private String name = null;
    private String description = null;
    private String longDescription = null;
    private String imageUrl = null;
    private Timestamp fromDate = null;
    private Timestamp thruDate = null;
    private Long sequenceNum = null;


    /**
     * Unique identifier representing a specific configuration for a given item.
     **/

    @JsonProperty("config_id")
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    /**
     * The name of the product configuration.
     **/

    @JsonProperty("name")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    @JsonProperty("configuration_item_type")
    public String getConfigurationItemTypeId() {
        return configurationItemTypeId;
    }

    public void setConfigurationItemTypeId(String configurationItemTypeId) {
        this.configurationItemTypeId= configurationItemTypeId;
    }
    /**
     * Representing a image url for a given product.
     **/

    @JsonProperty("image_url")
    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
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
        ProductConfigurationItem productConfigurationItem = (ProductConfigurationItem) o;
        return Objects.equals(id, productConfigurationItem.id) &&
                Objects.equals(name, productConfigurationItem.name) &&
                Objects.equals(description, productConfigurationItem.description) &&
                Objects.equals(longDescription, productConfigurationItem.longDescription) &&
                Objects.equals(configurationItemTypeId, productConfigurationItem.configurationItemTypeId) &&
                Objects.equals(imageUrl, productConfigurationItem.imageUrl) &&
                Objects.equals(fromDate, productConfigurationItem.fromDate) &&
                Objects.equals(thruDate, productConfigurationItem.thruDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, description, longDescription, configurationItemTypeId, imageUrl, fromDate, thruDate);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class ProductConfuguration {\n");

        sb.append("    id: ").append(toIndentedString(id)).append("\n");
        sb.append("    name: ").append(toIndentedString(name)).append("\n");
        sb.append("    description: ").append(toIndentedString(description)).append("\n");
        sb.append("    longDescription: ").append(toIndentedString(longDescription)).append("\n");
        sb.append("    configurationTypeId: ").append(toIndentedString(configurationItemTypeId)).append("\n");
        sb.append("    imageUrl: ").append(toIndentedString(imageUrl)).append("\n");
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

