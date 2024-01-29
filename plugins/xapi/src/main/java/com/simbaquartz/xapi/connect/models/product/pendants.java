package com.simbaquartz.xapi.connect.models.product;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Date;
import java.util.List;

public class pendants {

    @JsonProperty("id")
    private String id;

    @JsonProperty("sku")
    private String sku;

    @JsonProperty("inventoryCount")
    private Integer inventoryCount;

    @JsonProperty("collectionName")
    private String collectionName;

    @JsonProperty("gender")
    private String gender;

    @JsonProperty("suitableForAge")
    private String suitableForAge;

    @JsonProperty("styleId")
    private String styleId;

    @JsonProperty("styleValue")
    private String styleValue;

    @JsonProperty("rhodiumPlated")
    private Boolean isRhodiumPlated;

    @JsonProperty("width")
    private Float width;

    @JsonProperty("length")
    private Float length;

    @JsonProperty("chainLength")
    private Float chainLength;

    @JsonProperty("chainType")
    private String chainType;

    @JsonProperty("engravingAvailable")
    private Boolean isEngravingAvailable;

    @JsonProperty("buyFromVendor")
    private String buyFromVendor;

    @JsonProperty("buyFromLocation")
    private String buyFromLocation;

    @JsonProperty("viewCount")
    private Integer viewCount;

    @JsonProperty("metal")
    private List<String> metal;

    @JsonProperty("retailPrice")
    private Float retailPrice;

    @JsonProperty("wholesalePrice")
    private Float wholesalePrice;

    @JsonProperty("buyPrice")
    private Float buyPrice;

    @JsonProperty("buyDate")
    private Date buyDate;

    @JsonProperty("non_customization_information")
    private List<NonCustomizationInformation> nonCustomizationInformation;
}
