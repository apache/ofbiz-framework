package com.simbaquartz.xapi.connect.models.product;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

import java.sql.Date;



@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CurrencyRate {

    @JsonProperty("currencyFrom")
    private String currencyFromUomId = null;

    @JsonProperty("currencyTo")
    private String currencyToUomId = null;

    @JsonProperty("effectiveDate")
    private Date effectiveDate = null;

    @JsonProperty("factor")
    private Double factor = null;

}


