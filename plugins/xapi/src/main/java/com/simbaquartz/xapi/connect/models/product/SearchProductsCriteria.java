package com.simbaquartz.xapi.connect.models.product;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SearchProductsCriteria {
    @JsonProperty("product_id")
    private String productId;

    @JsonProperty("supplier_id")
    private String supplierId;

    @JsonProperty("collection_id")
    private String collectionId;

    @JsonProperty("customer_id")
    private String customerId;

}
