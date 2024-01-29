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
public class ProductCompatible {

    @JsonProperty("shapeId")
    private  String shapeId = null;

    @JsonProperty("caratTo")
    private  Double caratTo = null;

    @JsonProperty("caratFrom")
    private  Double caratFrom = null;
}