package com.simbaquartz.xapi.connect.models.quote;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import com.fidelissd.zcp.xcommon.models.media.File;
import lombok.Data;

import java.sql.Timestamp;

import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
@JsonIgnoreProperties(ignoreUnknown = true)
public class QuoteAttachment extends File {
    @JsonProperty("contentId")
    private String contentId;

    @JsonProperty("contentTypeId")
    private String contentTypeId = null;

    @JsonProperty("isQuoteDocument")
    private boolean quoteDocument = false;

    @JsonProperty("isCompanyDocument")
    private boolean companyDocument = false;

    @JsonProperty("isProductDocument")
    private boolean productDocument = false;

    @JsonProperty("isAttached")
    private boolean attached = false;

    @JsonProperty("fromDate")
    private Timestamp fromDate = null;

    @JsonProperty("attachable")
    private boolean attachable = false;
}
