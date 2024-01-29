package com.simbaquartz.xapi.connect.models.product;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;

/**
 * Created by Admin on 9/23/17.
 */
public class ProductConfigurations {

    private String productId = null;
    private String id = null;
    private String typeId = null;
    private String name = null;
    private String description = null;
    private List<ProductConfigurationOption> options = null;
    private List<ProductConfigurationProduct> configProducts = null;
    private Long sequence = null;
    private String configProductId = null;
    private BigDecimal quantity = null;
    private Timestamp fromDate = null;
    private Timestamp thruDate = null;


    /**
     * product id for adding configuration.
     **/

    @JsonProperty("product_id")
    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    /**
         * product id for adding configuration.
         **/

        @JsonProperty("config_product_id")
        public String getConfigProductId() {
            return configProductId;
        }

        public void setConfigProductId(String configProductId) {
            this.configProductId = configProductId;
        }

    /**
     * Unique identifier representing a specific configuration for a given item.
     **/

    @JsonProperty("id")
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
     * Representing a specific configuration type for a given product.
     **/

    @JsonProperty("type_id")
    public String getTypeId() {
        return typeId;
    }

    public void setTypeId(String typeId) {
        this.typeId = typeId;
    }

    /**
     * The list of the product option configuration .
     **/

    @JsonProperty("options")
    public List<ProductConfigurationOption> getOptions() {
        return options;
    }

    public void setOptions(List<ProductConfigurationOption> options) {
        this.options = options;
    }

    /**
     * The list of the product configuration products .
     **/

    @JsonProperty("config_products")
    public List<ProductConfigurationProduct> getConfigProducts() {
        return configProducts;
    }

    public void setConfigProducts(List<ProductConfigurationProduct> configProducts) {
        this.configProducts = configProducts;
    }



    /**
     * Represents the sequence number for the different configurations of a same product.
     **/

    @JsonProperty("sequence")
    public Long getSequence() {
        return sequence;
    }

    public void setSequence(Long sequence) {
        this.sequence = sequence;
    }

    /**
     * Represents the quantity for the different configurations.
     **/

    @JsonProperty("quantity")
    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
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
}
