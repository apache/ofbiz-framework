package com.simbaquartz.xapi.connect.models.agreement;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import javax.validation.constraints.NotEmpty;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@javax.annotation.Generated(value = "class io.swagger.codegen.languages.JavaResteasyServerCodegen", date = "2017-05-09T10:47:15.854-07:00")
public class Term {

    @NotEmpty(message = "Please provide a Term Name")
    @JsonProperty("term_name")
    private String termName = null;

    @JsonProperty("term_text")
    private String termText = null;

    @JsonProperty("party_content_type_id")
    private String partyContentTypeId = null;

    @JsonProperty("content_ids")
    private List<String> contentIds  = null;

}