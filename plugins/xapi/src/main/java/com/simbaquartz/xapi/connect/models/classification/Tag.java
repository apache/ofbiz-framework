package com.simbaquartz.xapi.connect.models.classification;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import javax.validation.constraints.NotEmpty;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@javax.annotation.Generated(value = "class io.swagger.codegen.languages.JavaResteasyServerCodegen", date = "2017-05-09T10:47:15.854-07:00")
public class Tag {

    @NotEmpty(message = "Please provide a Tag Name")
    @JsonProperty("tag_name")
    private String tagName = null;

    @JsonProperty("tag_color")
    private String tagColor = null;

}