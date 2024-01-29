package com.simbaquartz.xapi.connect.models.product;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import javax.validation.constraints.NotEmpty;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class VProduct {

    @NotEmpty(message = "Please provide product name")
    @JsonProperty("internalName")
    private  String internalName = null;

    @JsonProperty("productName")
    private  String productName = null;

    @JsonProperty("sku")
    private  String sku = null;

    @JsonProperty("description")
    private  String description = null;

    @JsonProperty("longDescription")
    private  String longDescription = null;

    @JsonProperty("currencyUomId")
    private String currencyUomId = null;

    @JsonProperty("customerPrice")
    private BigDecimal customerPrice = null;

    @JsonProperty("customerPriceWholesale")
    private BigDecimal customerPriceWholesale = null;

    @JsonProperty("costPrice")
    private BigDecimal costPrice = null;

    @JsonProperty("isVirtual")
    private String isVirtual = null;

    @JsonProperty("gender")
    private String gender = null;

    @JsonProperty("age")
    private String age = null;

    @JsonProperty("rhodiumPlated")
    private boolean rhodiumPlated = false;

    @JsonProperty("productStoreId")
    private String productStoreId = null;

    @JsonProperty("isVariant")
    private String isVariant = null;

    @JsonProperty("categories")
    private List<String> categories = null;

    @JsonProperty("virtualProductId")
    private String virtualProductId = null;

    @JsonProperty("productFeatures")
    private List<String> productFeatures = null;

    @JsonProperty("metalType")
    private List<String> metalType = null;
    @JsonProperty("ringSize")
    private List<String> ringSize = null;
    @JsonProperty("diamondShape")
    private List<String> diamondShape = null;

    @JsonProperty("productTypeId")
    private String productTypeId = null;

    @JsonProperty("primaryProductCategoryId")
    private String primaryProductCategoryId = null;

    @JsonProperty("weightUomId")
    private String weightUomId = null;

    @JsonProperty("productWeight")
    private BigDecimal productWeight = null;

    @JsonProperty("productFeatureFromDate")
    private Timestamp productFeatureFromDate = null;

    @JsonProperty("productFeatureThruDate")
    private Timestamp productFeatureThruDate = null;

    @JsonProperty("productId")
    private String productId = null;

    @JsonProperty("error_message")
    private String errorMessage = null;

    @JsonProperty("compatibleStones")
    private List<ProductCompatible> compatibleStones = null;

    @JsonProperty("nonCustomizationInfo")
    private List<NonCustomizationInformation> nonCustomizationInformation = null;

    @JsonProperty("attributes")
    private List<ProductAttributes> attributes = null;

    @JsonProperty("inventory")
    private Inventory inventory = null;
}