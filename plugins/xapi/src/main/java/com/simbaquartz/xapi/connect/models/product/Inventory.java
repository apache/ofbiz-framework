package com.simbaquartz.xapi.connect.models.product;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)

public class Inventory {
    @JsonProperty("productId")
    private  String productId = null;

    @JsonProperty("quantityAccepted")
    private BigDecimal quantityAccepted = BigDecimal.ZERO;

    @JsonProperty("quantityRejected")
    private BigDecimal quantityRejected = BigDecimal.ZERO;

    @JsonProperty("reasonEnumId")
    private String reasonEnumId = null;

    @JsonProperty("unitCost")
    private BigDecimal unitCost = BigDecimal.ZERO;

    @JsonProperty("ownerPartyId")
    private String ownerPartyId = "dos.store";

    @JsonProperty("currencyUomId")
    private String currencyUomId = "USD";

    @JsonProperty("facilityId")
    private String facilityId = "dos.store.facility";

    @JsonProperty("inventoryItemTypeId")
    private String inventoryItemTypeId = "SERIALIZED_INV_ITEM";

    @JsonProperty("locationSeqId")
    private String locationSeqId = null;

    @JsonProperty("itemDescription")
    private String itemDescription = null;

    @JsonProperty("inventoryItemId")
    private String inventoryItemId = null;
    
    @JsonProperty("error_message")
    private String errorMessage = null;
}