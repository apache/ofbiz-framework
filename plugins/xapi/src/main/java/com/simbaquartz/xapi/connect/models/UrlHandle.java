package com.simbaquartz.xapi.connect.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class UrlHandle {
    @JsonProperty("url_handle_id")
    private String urlHandleId;

    @JsonProperty("url_handle")
    private String urlHandle;

    @JsonProperty("url")
    private String url;

    @JsonProperty("url_type")
    private String urlType;

    @JsonProperty("type_id")
    private String typeId;

    @JsonProperty("render_screen")
    private String renderScreen;

    @JsonProperty("description")
    private String description;
}
