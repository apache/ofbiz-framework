package com.simbaquartz.xapi.connect.models.product;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import javax.validation.constraints.NotEmpty;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class KitProduct implements Serializable {

    @NotEmpty(message = "Please provide a Stone Product Id")
    @JsonProperty("stone_product_id")
    private String stoneProductId = null;

    @NotEmpty(message = "Please provide a Setting Product Id")
    @JsonProperty("setting_product_id")
    private String settingProductId = null;

    @NotEmpty(message = "Please provide a Kit Product Name")
    @JsonProperty("kit_product_name")
    private String internalName = null;

    @JsonProperty("kit_product_price")
    private BigDecimal price = null;

    @JsonProperty("currency_uom_id")
    private String currencyUomId = null;

    @JsonProperty("kit_product_id")
    private String kitProductId = null;
}

