package com.simbaquartz.xapi.connect.models.product;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import javax.validation.constraints.NotEmpty;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductAttributes {

    @JsonProperty("attrName")
    private  String attrName = null;

    @JsonProperty("attrValue")
    private  String attrValue = null;

    @JsonProperty("productId")
    private  String productId = null;

}