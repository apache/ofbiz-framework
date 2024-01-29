package com.simbaquartz.xapi.connect.models.product;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.Date;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class LooseGemstone {

    @JsonProperty("id")
    private String id;

    @JsonProperty("sku")
    private String sku;

    @JsonProperty("inventoryCount")
    private String inventoryCount;

    @JsonProperty("type")
    private String type;

    @JsonProperty("shape")
    private String shape;

    @JsonProperty("carat")
    private Float carat;

    @JsonProperty("height")
    private Float height;

    @JsonProperty("width")
    private Float width;

    @JsonProperty("depth")
    private Float depth;

    @JsonProperty("heatTreated")
    private Boolean isHeatTreated;

    @JsonProperty("certificate")
    private String certificate;

    @JsonProperty("retailPrice")
    private Float retailPrice;

    @JsonProperty("wholesalePrice")
    private Float wholesalePrice;

    @JsonProperty("purchasePrice")
    private Float buyPrice;

    @JsonProperty("location")
    private String location;

    @JsonProperty("buyFromVendor")
    private String buyFromVendor;

    @JsonProperty("buyFromLocation")
    private String buyFromLocation;

    @JsonProperty("buyDate")
    private Date buyDate;

    @JsonProperty("viewCount")
    private Integer viewCount;

    @JsonProperty("packetNumber")
    private String packetNumber;

    @JsonProperty("gemstoneMineCountry")
    private String gemstoneMineCountry;

}
