package com.simbaquartz.xapi.connect.models.product;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.ArrayList;
import javax.validation.constraints.NotEmpty;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConfigurableSetting {

    @JsonProperty("id")
    private String id;

    @JsonProperty("sku")
    private String sku;

    @JsonProperty("collection_name")
    private String collectionName;

    @JsonProperty("gender")
    private String gender;

    @JsonProperty("suitable_for_age")
    private String suitableForAge;

    @JsonProperty("style_id")
    private String styleId;

    @JsonProperty("style_value")
    private String styleValue;

    @JsonProperty("available_sizes")
    private List<String> availableSizes;

    @JsonProperty("rhodium_plated")
    private Boolean isRhodiumPlated;

    @JsonProperty("width")
    private Float width;

    @JsonProperty("height")
    private Float height;

    @JsonProperty("matching_wedding_band_sku_number")
    private String matchingWeddingBandSkuNumber;

    @JsonProperty("engraving_available")
    private Boolean isEngravingAvailable;

    @JsonProperty("buy_from_vendor")
    private String buyFromVendor;

    @JsonProperty("buy_from_location")
    private String buyFromLocation;

    @JsonProperty("view_count")
    private String viewCount;

    @JsonProperty("metal")
    private List<String> metal;

    @JsonProperty("retail_price")
    private Float retailPrice;

    @JsonProperty("wholesale_price")
    private Float wholesalePrice;

    @JsonProperty("buy_price")
    private Float buyPrice;

    @JsonProperty("buy_date")
    private Timestamp buyDate;

    @JsonProperty("can_be_set_with_shape")
    private String canBeSetWithShape;

    @JsonProperty("can_be_set_with_carat_size")
    private String canBeSetWithCaratSize;

    @JsonProperty("non_customization_information")
    private List<NonCustomizationInformation> nonCustomizationInformation;

}