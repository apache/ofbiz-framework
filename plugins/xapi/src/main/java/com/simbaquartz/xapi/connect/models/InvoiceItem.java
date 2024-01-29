package com.simbaquartz.xapi.connect.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;

/**
 * Represents a Invoice object.
 **/

@javax.annotation.Generated(value = "class io.swagger.codegen.languages.JavaResteasyServerCodegen", date = "2018-05-18T14:37:04.465+05:30")
@Data
public class InvoiceItem {

    @JsonProperty("id")
    private String id = null;
    @JsonProperty("invoiceItemTypeId")
    private String invoiceItemTypeId = null;
    @JsonProperty("quantity")
    private BigDecimal quantity = null;
    @JsonProperty("amount")
    private BigDecimal amount = null;
    @JsonProperty("productId")
    private String productId = null;
    @JsonProperty("description")
    private String description = null;
}

