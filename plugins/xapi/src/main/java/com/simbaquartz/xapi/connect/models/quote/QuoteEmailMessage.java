package com.simbaquartz.xapi.connect.models.quote;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.simbaquartz.xapi.connect.models.email.EmailMessage;
import lombok.Data;

import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper=false)
@JsonIgnoreProperties(ignoreUnknown = true)
public class QuoteEmailMessage extends EmailMessage {
    @JsonProperty("preview_email_url")
    private String previewEmailUrl = null;

    @JsonProperty("preview_quote_pdf_url")
    private String previewQuotePdfUrl = null;

    @JsonProperty("data_resource_id")
    private String dataResourceId = null;

    @JsonProperty("send_cc_detail")
    private List<Map> sendCcDetail = null;

    @JsonProperty("send_bcc_detail")
    private List<Map> sendBccDetail = null;

}
