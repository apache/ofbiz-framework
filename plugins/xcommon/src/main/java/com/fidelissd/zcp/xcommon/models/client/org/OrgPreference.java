package com.fidelissd.zcp.xcommon.models.client.org;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrgPreference {
    @JsonProperty("default_email_from_address")
    private String defaultEmailFromAddress;
    @JsonProperty("default_quote_email_cc_address")
    private String defaultQuoteEmailCcAddress;
    @JsonProperty("default_quote_email_bcc_address")
    private String defaultQuoteEmailBccAddress;
    @JsonProperty("quote_id_prefix")
    private String defaultQuoteIdPrefix;
    @JsonProperty("default_quote_email_message")
    private String defaultQuoteEmailMessage;
    @JsonProperty("email_reply_to_address")
    private String emailReplyToAddress;
    @JsonProperty("email_type")
    private String emailType;
    @JsonProperty("quote_pdf_footer_text")
    private String quotePdfFooterText;
}
