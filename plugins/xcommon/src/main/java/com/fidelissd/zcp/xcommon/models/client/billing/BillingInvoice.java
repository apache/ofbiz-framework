package com.fidelissd.zcp.xcommon.models.client.billing;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BillingInvoice {
    @JsonProperty("id")
    private String invoiceId;

    @JsonProperty("is_past_due")
    private boolean isPastDue;

    @JsonProperty("invoice_total")
    private BigDecimal invoiceTotal;

    @JsonProperty("invoice_currency")
    private String invoiceCurrency;
}
