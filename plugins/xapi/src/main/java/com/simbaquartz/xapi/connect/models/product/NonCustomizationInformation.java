package com.simbaquartz.xapi.connect.models.product;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class NonCustomizationInformation {

    @JsonProperty("id")
    private String id;

    @JsonProperty("typeId")
    private String typeId;

    @JsonProperty("name")
    private String name;

    @JsonProperty("categoryId")
    private String categoryId;

    @JsonProperty("quantity")
    private BigDecimal quantity;

    @JsonProperty("shape")
    private String shape;

    @JsonProperty("weight")
    private Double weight;

    @JsonProperty("avgColor")
    private String avgColor;

    @JsonProperty("avgClarity")
    private String avgClarity;

    @JsonProperty("minColor")
    private String minColor;

    @JsonProperty("minClarity")
    private String minClarity;
}
