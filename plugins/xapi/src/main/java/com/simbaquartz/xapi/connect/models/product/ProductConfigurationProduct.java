package com.simbaquartz.xapi.connect.models.product;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.Objects;


@javax.annotation.Generated(value = "class io.swagger.codegen.languages.JavaResteasyServerCodegen", date = "2017-06-30T20:56:17.408-07:00")
public class ProductConfigurationProduct {

    private String productId = null;
    private String configItemId = null;
    private String configOptionId = null;
    private BigDecimal quantity = null;


    /**
     * Unique identifier representing a specific configuration for a given item.
     **/

    @JsonProperty("config_item_id")
    public String getConfigItemId() {
        return configItemId;
    }

    public void setConfigItemId(String configItemId) {
        this.configItemId = configItemId;
    }

    /**
     * Identifier representing a option for a given item.
     **/

    @JsonProperty("config_option_id")
    public String getConfigOptionId() {
        return configOptionId;
    }

    public void setConfigOptionId(String configOptionId) {
        this.configOptionId = configOptionId;
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
     * Represents the sequence number for the different configurations of a same product.
     **/

    @JsonProperty("quantity")
    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }



    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ProductConfigurationProduct productConfigurationProduct = (ProductConfigurationProduct) o;
        return Objects.equals(configItemId, productConfigurationProduct.configItemId) &&
                Objects.equals(configOptionId, productConfigurationProduct.configOptionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(configItemId, configOptionId, configItemId);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class ProductConfuguration {\n");

        sb.append("    configItemId: ").append(toIndentedString(configItemId)).append("\n");
        sb.append("    configOptionId: ").append(toIndentedString(configOptionId)).append("\n");
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

