package com.fidelissd.zcp.xcommon.models.email;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class EmailSetting   {

    @JsonProperty("attachments")
    private List<Map> attachments = new ArrayList<>();
}
