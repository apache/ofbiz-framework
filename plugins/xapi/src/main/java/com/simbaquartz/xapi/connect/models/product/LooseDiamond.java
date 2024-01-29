package com.simbaquartz.xapi.connect.models.product;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.apache.commons.net.ntp.TimeStamp;

import javax.xml.soap.Text;
import java.util.Date;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class LooseDiamond {

    @JsonProperty("id")
    private String id;

    @JsonProperty("sku")
    private String sku;

    @JsonProperty("inventoryCount")
    private Integer inventoryCount;

    @JsonProperty("shape")
    private String shape;

    @JsonProperty("cut")
    private String cut;

    @JsonProperty("clarity")
    private String clarity;

    @JsonProperty("color")
    private String color;

    @JsonProperty("carat")
    private Float carat;

    @JsonProperty("fluorescence")
    private String fluorescence;

    @JsonProperty("width")
    private Float width;

    @JsonProperty("height")
    private Float height;

    @JsonProperty("depth")
    private Float depth;

    @JsonProperty("tablePercentage")
    private Float tablePercentage;

    @JsonProperty("depthPercentage")
    private Float depthPercentage;

    @JsonProperty("polish")
    private String polish;

    @JsonProperty("symmetry")
    private String symmetry;

    @JsonProperty("girdle")
    private String girdle;

    @JsonProperty("certificate")
    private String certificate;

    @JsonProperty("retailPrice")
    private Float retailPrice;

    @JsonProperty("wholesalePrice")
    private Float wholesalePrice;

    @JsonProperty("buyPrice")
    private Float buyPrice;

    @JsonProperty("location")
    private String location;

    @JsonProperty("buyFromVendor")
    private String buyFromVendor;

    @JsonProperty("buyFromLocation")
    private String buyFromLocation;

    @JsonProperty("purchaseDate")
    private Date purchaseDate;

    @JsonProperty("viewCount")
    private Integer viewCount;

    @JsonProperty("rapBracketPrice")
    private Integer rapBracketPrice;

    @JsonProperty("rapPrice")
    private Integer rapPrice;

    @JsonProperty("rapCarat")
    private Integer rapCarat;

    @JsonProperty("diamondType")
    private String diamondType;

    @JsonProperty("packetNumber")
    private String packetNumber;

    @JsonProperty("rapDate")
    private Date rapDate;

    @JsonProperty("percentageRetailPriceBelowRap")
    private Float percentageRetailPriceBelowRap;

    @JsonProperty("percentageWholesalePriceBelowRap")
    private Float percentageWholesalePriceBelowRap;

    @JsonProperty("percentageBuyPriceBelowRap")
    private Float percentageBuyPriceBelowRap;

}
